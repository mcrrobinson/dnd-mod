package mattonfire.dnd.quest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mattonfire.dnd.classes.Party.Party;
import mattonfire.dnd.classes.Party.PartyManager;
import mattonfire.dnd.faction.Faction;
import mattonfire.dnd.faction.Factions;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * <pre>
 * /quest list                                   your active and finished quests
 * /quest info &lt;quest&gt;                            details of one (any loaded quest, for ops)
 * /quest track|abandon &lt;quest&gt;
 * /quest join [instance]                         join a quest a party member started (newest if no number)
 * /quest admin &lt;players&gt; start|complete|reset &lt;quest&gt;      (op)
 * /quest admin &lt;players&gt; stage &lt;quest&gt; &lt;n&gt;              (op)
 * /quest admin &lt;players&gt; talk &lt;role&gt;   as if they spoke with an NPC with that role (op)
 * /quest admin &lt;players&gt; pass &lt;skill&gt;  as if they passed a dialogue check (op)
 * </pre>
 */
public final class QuestCommand {
    private static final DynamicCommandExceptionType UNKNOWN = new DynamicCommandExceptionType(
            id -> Text.translatable("quest.dndclasses.error.unknown", String.valueOf(id)));

    private static final SuggestionProvider<ServerCommandSource> ALL_QUESTS = (context, builder) ->
            CommandSource.suggestIdentifiers(Quests.all().stream().map(QuestDefinition::id), builder);

    private static final SuggestionProvider<ServerCommandSource> MY_QUESTS = (context, builder) -> {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) {
            return builder.buildFuture();
        }
        return CommandSource.suggestIdentifiers(QuestManager.get(context.getSource().getServer())
                .instancesOf(player.getUuid()).stream().map(QuestInstance::quest), builder);
    };

    private QuestCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("quest")
                .then(CommandManager.literal("list").executes(QuestCommand::list))
                .then(CommandManager.literal("info")
                        .then(CommandManager.argument("quest", IdentifierArgumentType.identifier()).suggests(MY_QUESTS)
                                .executes(QuestCommand::info)))
                .then(CommandManager.literal("track")
                        .then(CommandManager.argument("quest", IdentifierArgumentType.identifier()).suggests(MY_QUESTS)
                                .executes(QuestCommand::track)))
                .then(CommandManager.literal("abandon")
                        .then(CommandManager.argument("quest", IdentifierArgumentType.identifier()).suggests(MY_QUESTS)
                                .executes(QuestCommand::abandon)))
                .then(CommandManager.literal("join")
                        .executes(QuestCommand::joinLatest)
                        .then(CommandManager.argument("instance", IntegerArgumentType.integer(1))
                                .executes(QuestCommand::join)))
                .then(CommandManager.literal("admin")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                .then(CommandManager.literal("start")
                                        .then(CommandManager.argument("quest", IdentifierArgumentType.identifier())
                                                .suggests(ALL_QUESTS).executes(context -> admin(context, "start"))))
                                .then(CommandManager.literal("complete")
                                        .then(CommandManager.argument("quest", IdentifierArgumentType.identifier())
                                                .suggests(ALL_QUESTS).executes(context -> admin(context, "complete"))))
                                .then(CommandManager.literal("reset")
                                        .then(CommandManager.argument("quest", IdentifierArgumentType.identifier())
                                                .suggests(ALL_QUESTS).executes(context -> admin(context, "reset"))))
                                .then(CommandManager.literal("stage")
                                        .then(CommandManager.argument("quest", IdentifierArgumentType.identifier())
                                                .suggests(ALL_QUESTS)
                                                .then(CommandManager.argument("stage", IntegerArgumentType.integer(1))
                                                        .executes(context -> admin(context, "stage")))))
                                .then(CommandManager.literal("talk")
                                        .then(CommandManager.argument("role", StringArgumentType.word())
                                                .executes(QuestCommand::talk)))
                                .then(CommandManager.literal("pass")
                                        .then(CommandManager.argument("skill", StringArgumentType.word())
                                                .executes(QuestCommand::pass))))));
    }

    private static QuestDefinition quest(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Identifier id = IdentifierArgumentType.getIdentifier(context, "quest");
        QuestDefinition quest = Quests.get(id);
        if (quest == null) {
            throw UNKNOWN.create(id);
        }
        return quest;
    }

    private static int list(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        QuestManager manager = QuestManager.get(source.getServer());
        List<QuestInstance> active = manager.instancesOf(player.getUuid());
        int tracked = manager.tracked(player.getUuid());
        source.sendFeedback(Text.translatable("quest.dndclasses.list.header", active.size(), QuestDefinition.MAX_ACTIVE)
                .formatted(Formatting.GOLD), false);
        for (QuestInstance instance : active) {
            QuestDefinition quest = Quests.get(instance.quest());
            if (quest == null) {
                source.sendFeedback(Text.literal(" #" + instance.id() + " " + instance.quest() + " (not loaded)")
                        .formatted(Formatting.DARK_GRAY), false);
                continue;
            }
            MutableText line = Text.literal(" #" + instance.id() + " ").formatted(Formatting.DARK_GRAY)
                    .append(quest.title().copy().formatted(Formatting.YELLOW))
                    .append(Text.literal(" [" + quest.id() + "] ").formatted(Formatting.DARK_GRAY))
                    .append(Text.translatable("quest.dndclasses.list.stage", instance.stage() + 1, quest.stages().size())
                            .formatted(Formatting.GRAY));
            List<QuestObjective> objectives = quest.stage(instance.stage()).objectives();
            for (int i = 0; i < objectives.size(); i++) {
                QuestObjective objective = objectives.get(i);
                line.append(Text.literal(i == 0 ? ": " : ", ").formatted(Formatting.GRAY))
                        .append(objective.description().copy().formatted(Formatting.WHITE))
                        .append(Text.literal(" " + Math.min(instance.progress(i), objective.count()) + "/" + objective.count())
                                .formatted(Formatting.WHITE));
            }
            if (instance.participants().size() > 1) {
                line.append(Text.literal(" (" + String.join(", ", instance.participants().stream()
                        .map(uuid -> manager.name(source.getServer(), uuid)).toList()) + ")").formatted(Formatting.AQUA));
            }
            if (instance.id() == tracked) {
                line.append(Text.translatable("quest.dndclasses.list.tracked").formatted(Formatting.GREEN));
            }
            source.sendFeedback(line, false);
        }
        Map<Identifier, Long> finished = manager.finished(player.getUuid());
        List<Identifier> unclaimed = manager.unclaimed(player.getUuid());
        if (!finished.isEmpty()) {
            MutableText line = Text.translatable("quest.dndclasses.list.finished").formatted(Formatting.GOLD);
            boolean first = true;
            for (Identifier id : finished.keySet()) {
                line.append(Text.literal(first ? " " : ", ").formatted(Formatting.GRAY));
                first = false;
                line.append(QuestManager.title(id).copy().formatted(Formatting.GRAY));
                if (unclaimed.contains(id)) {
                    line.append(Text.translatable("quest.dndclasses.list.unclaimed").formatted(Formatting.YELLOW));
                }
            }
            source.sendFeedback(line, false);
        }
        return active.size();
    }

    private static int info(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        QuestManager manager = QuestManager.get(source.getServer());
        Identifier id = IdentifierArgumentType.getIdentifier(context, "quest");
        QuestDefinition quest = Quests.get(id);
        QuestInstance instance = manager.instanceOf(player.getUuid(), id);
        if (quest == null || (instance == null && !source.hasPermissionLevel(2) && !manager.hasFinished(player.getUuid(), id))) {
            throw UNKNOWN.create(id);
        }
        source.sendFeedback(quest.title().copy().formatted(Formatting.GOLD, Formatting.BOLD)
                .append(Text.literal(" [" + quest.id() + "]").formatted(Formatting.DARK_GRAY)), false);
        if (quest.description() != null) {
            source.sendFeedback(quest.description().copy().formatted(Formatting.GRAY), false);
        }
        if (quest.giver() != null) {
            source.sendFeedback(Text.translatable("quest.dndclasses.info.giver", QuestObjective.roleName(quest.giver()))
                    .formatted(Formatting.GRAY), false);
        }
        if (instance != null) {
            QuestStage stage = quest.stage(instance.stage());
            source.sendFeedback(Text.translatable("quest.dndclasses.info.stage", instance.stage() + 1, quest.stages().size(),
                    stage.title() != null ? stage.title() : quest.title()).formatted(Formatting.YELLOW), false);
            source.sendFeedback(Text.literal("  ").append(stage.text().copy()).formatted(Formatting.GRAY), false);
            for (int i = 0; i < stage.objectives().size(); i++) {
                source.sendFeedback(QuestManager.objectiveLine(stage.objectives().get(i), instance.progress(i)), false);
            }
            Party party = PartyManager.get(source.getServer()).getParty(player.getUuid());
            MutableText who = Text.translatable("quest.dndclasses.info.participants").formatted(Formatting.AQUA);
            boolean first = true;
            for (UUID uuid : instance.participants()) {
                Party theirs = PartyManager.get(source.getServer()).getParty(uuid);
                boolean leader = theirs != null && theirs == party && theirs.isLeader(uuid);
                who.append(Text.literal((first ? " " : ", ") + (leader ? "★" : "") + manager.name(source.getServer(), uuid))
                        .formatted(Formatting.WHITE));
                first = false;
            }
            who.append(Text.literal(" (#" + instance.id() + ")").formatted(Formatting.DARK_GRAY));
            source.sendFeedback(who, false);
        } else {
            for (int i = 0; i < quest.stages().size(); i++) {
                QuestStage stage = quest.stage(i);
                source.sendFeedback(Text.translatable("quest.dndclasses.info.stage", i + 1, quest.stages().size(),
                        stage.title() != null ? stage.title() : quest.title()).formatted(Formatting.YELLOW), false);
                for (QuestObjective objective : stage.objectives()) {
                    source.sendFeedback(QuestManager.objectiveLine(objective, 0), false);
                }
            }
        }
        if (!quest.rewards().isEmpty()) {
            source.sendFeedback(Text.translatable("quest.dndclasses.info.rewards").formatted(Formatting.GOLD)
                    .append(Text.literal(" ")).append(describe(quest.rewards()).formatted(Formatting.WHITE)), false);
        }
        return 1;
    }

    /** A short summary of reward actions: "5 emeralds, 30 XP, +20 Hobbits of the Shire". */
    static MutableText describe(List<QuestAction> actions) {
        MutableText text = Text.empty();
        boolean first = true;
        for (QuestAction action : actions) {
            MutableText part = null;
            if (action instanceof QuestAction.Emeralds emeralds) {
                part = Text.translatable("quest.dndclasses.reward.emeralds", emeralds.count());
            } else if (action instanceof QuestAction.Xp xp) {
                part = Text.translatable("quest.dndclasses.reward.xp", xp.amount());
            } else if (action instanceof QuestAction.ClassXp xp) {
                part = Text.translatable("quest.dndclasses.reward.class_xp", xp.amount());
            } else if (action instanceof QuestAction.MagicItem item) {
                part = Text.translatable("quest.dndclasses.reward.magic_item",
                        Text.translatableWithFallback("quest.dndclasses.rarity." + item.rarity(), item.rarity()));
            } else if (action instanceof QuestAction.Give give) {
                part = Text.literal(give.count() + " ").append(net.minecraft.registry.Registries.ITEM.get(give.item()).getName());
            } else if (action instanceof QuestAction.Rep rep) {
                part = Text.empty();
                boolean firstRep = true;
                for (Map.Entry<Identifier, Integer> entry : rep.deltas().entrySet()) {
                    Faction faction = Factions.get(entry.getKey());
                    part.append((firstRep ? "" : ", ") + (entry.getValue() > 0 ? "+" : "") + entry.getValue() + " ")
                            .append(faction != null ? faction.displayName() : Text.literal(entry.getKey().toString()));
                    firstRep = false;
                }
            }
            if (part != null) {
                if (!first) {
                    text.append(", ");
                }
                text.append(part);
                first = false;
            }
        }
        return text;
    }

    private static int track(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        Identifier id = IdentifierArgumentType.getIdentifier(context, "quest");
        try {
            QuestManager.get(context.getSource().getServer()).track(player, id);
        } catch (QuestManager.QuestException e) {
            context.getSource().sendError(e.text());
            return 0;
        }
        context.getSource().sendFeedback(Text.translatable("quest.dndclasses.tracking", QuestManager.title(id))
                .formatted(Formatting.GREEN), false);
        return 1;
    }

    private static int abandon(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        Identifier id = IdentifierArgumentType.getIdentifier(context, "quest");
        try {
            Text title = QuestManager.get(context.getSource().getServer()).abandon(player, id);
            context.getSource().sendFeedback(Text.translatable("quest.dndclasses.abandoned", title)
                    .formatted(Formatting.YELLOW), false);
            return 1;
        } catch (QuestManager.QuestException e) {
            context.getSource().sendError(e.text());
            return 0;
        }
    }

    private static int join(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        try {
            QuestManager.get(context.getSource().getServer()).join(player, IntegerArgumentType.getInteger(context, "instance"));
            return 1;
        } catch (QuestManager.QuestException e) {
            context.getSource().sendError(e.text());
            return 0;
        }
    }

    private static int joinLatest(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        QuestManager manager = QuestManager.get(context.getSource().getServer());
        QuestInstance offer = manager.latestOffer(player);
        if (offer == null) {
            context.getSource().sendError(Text.translatable("quest.dndclasses.error.no_offer"));
            return 0;
        }
        try {
            manager.join(player, offer.id());
            return 1;
        } catch (QuestManager.QuestException e) {
            context.getSource().sendError(e.text());
            return 0;
        }
    }

    private static int admin(CommandContext<ServerCommandSource> context, String action) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "players");
        QuestManager manager = QuestManager.get(source.getServer());
        QuestDefinition quest = quest(context);
        int done = 0;
        java.util.Set<UUID> added = new java.util.HashSet<>();
        for (ServerPlayerEntity player : players) {
            try {
                switch (action) {
                    case "start" -> {
                        if (added.contains(player.getUuid())) {
                            // Already added by an earlier target's party.
                            continue;
                        }
                        QuestInstance instance = manager.start(player, quest, true);
                        added.addAll(instance.participants());
                        source.sendFeedback(Text.literal("Started " + quest.id() + " for " + player.getEntityName()
                                + " (#" + instance.id() + ", " + instance.participants().size() + " participants)"), true);
                    }
                    case "complete" -> {
                        manager.complete(player, quest.id());
                        source.sendFeedback(Text.literal("Completed " + quest.id() + " for " + player.getEntityName()), true);
                    }
                    case "reset" -> {
                        manager.reset(player, quest.id());
                        source.sendFeedback(Text.literal("Reset " + quest.id() + " for " + player.getEntityName()), true);
                    }
                    case "stage" -> {
                        int stage = IntegerArgumentType.getInteger(context, "stage");
                        manager.setStage(player, quest.id(), stage);
                        source.sendFeedback(Text.literal("Set " + quest.id() + " to stage " + stage + " for "
                                + player.getEntityName()), true);
                    }
                    default -> throw new IllegalStateException(action);
                }
                done++;
            } catch (QuestManager.QuestException e) {
                source.sendError(Text.literal(player.getEntityName() + ": ").append(e.text()));
            }
        }
        return done;
    }

    private static int talk(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        String role = StringArgumentType.getString(context, "role");
        QuestManager manager = QuestManager.get(source.getServer());
        for (ServerPlayerEntity player : EntityArgumentType.getPlayers(context, "players")) {
            manager.talkedTo(player, role);
            source.sendFeedback(Text.literal(player.getEntityName() + " spoke with the " + role), true);
        }
        return 1;
    }

    private static int pass(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        String skill = StringArgumentType.getString(context, "skill");
        QuestManager manager = QuestManager.get(source.getServer());
        for (ServerPlayerEntity player : EntityArgumentType.getPlayers(context, "players")) {
            manager.checkPassed(player, skill);
            source.sendFeedback(Text.literal(player.getEntityName() + " passed a " + skill + " check"), true);
        }
        return 1;
    }
}
