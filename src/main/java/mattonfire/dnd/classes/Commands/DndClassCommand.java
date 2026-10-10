package mattonfire.dnd.classes.Commands;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Stream;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import mattonfire.dnd.classes.ClassLifecycle;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Progression.ClassProgress;
import mattonfire.dnd.classes.Progression.ClassTrees;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Progression.SkillNode;
import mattonfire.dnd.classes.Progression.Subclass;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * /dndclass get <player>
 * /dndclass set <player> <class>
 *
 * /dndclass progress <player>
 * /dndclass xp <player> add|set <amount>
 * /dndclass resetprogress <player>
 * /dndclass unlock|equip <player> <skill>   (ignores points and the attunement table)
 * /dndclass rank <player> <skill> <n>        (ignores points, level and the attunement table)
 * /dndclass bestiary <player> learn|unlock <entity>
 * /dndclass subclass <player> <id|none>        (ignores level and the attunement table; none refunds)
 *
 * Changes a player's class without them having to die. Setting "none" clears the
 * class and reopens the class picker on their client.
 */
public class DndClassCommand {
    private static final DynamicCommandExceptionType UNKNOWN_CLASS = new DynamicCommandExceptionType(
            name -> Text.literal("Unknown class: " + name));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("dndclass")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("get")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(DndClassCommand::get)))
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.argument("class", StringArgumentType.word())
                                        .suggests((context, builder) -> CommandSource.suggestMatching(
                                                Arrays.stream(DndCharacter.values()).map(DndClassCommand::name),
                                                builder))
                                        .executes(DndClassCommand::set))))
                .then(CommandManager.literal("progress")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(DndClassCommand::progress)))
                .then(CommandManager.literal("xp")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.literal("add")
                                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> xp(context, true))))
                                .then(CommandManager.literal("set")
                                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(context -> xp(context, false))))))
                .then(skillCommand("unlock", true))
                .then(skillCommand("equip", false))
                .then(CommandManager.literal("rank")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(skillArgument()
                                        .then(CommandManager.argument("rank", IntegerArgumentType.integer(1))
                                                .executes(DndClassCommand::rank)))))
                .then(CommandManager.literal("bestiary")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(bestiaryCommand("learn", false))
                                .then(bestiaryCommand("unlock", true))))
                .then(CommandManager.literal("subclass")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.argument("subclass", StringArgumentType.word())
                                        .suggests((context, builder) -> CommandSource.suggestMatching(
                                                Stream.concat(Stream.of("none"), ClassTrees.subclasses(
                                                        Progression.classOf(EntityArgumentType.getPlayer(context,
                                                                "player"))).stream().map(Subclass::id)),
                                                builder))
                                        .executes(DndClassCommand::subclass))))
                .then(CommandManager.literal("resetprogress")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(DndClassCommand::resetProgress))));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> skillCommand(
            String literal, boolean unlock) {
        return CommandManager.literal(literal)
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .then(skillArgument()
                                .executes(context -> {
                                    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                    String skill = StringArgumentType.getString(context, "skill");
                                    boolean done = unlock ? Progression.unlock(player, skill, true)
                                            : Progression.equip(player, skill, true);
                                    if (!done) {
                                        context.getSource().sendError(Text.literal("Couldn't " + literal + " "
                                                + skill));
                                        return 0;
                                    }
                                    return progress(context);
                                })));
    }

    private static RequiredArgumentBuilder<ServerCommandSource, String> skillArgument() {
        return CommandManager.argument("skill", StringArgumentType.word())
                .suggests((context, builder) -> CommandSource.suggestMatching(
                        ClassTrees.get(Progression.classOf(EntityArgumentType.getPlayer(context, "player"))).stream()
                                .map(SkillNode::id),
                        builder));
    }

    private static int rank(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        String skill = StringArgumentType.getString(context, "skill");
        int rank = IntegerArgumentType.getInteger(context, "rank");
        if (!Progression.setRank(player, skill, rank)) {
            SkillNode node = ClassTrees.node(skill);
            context.getSource().sendError(Text.literal("Couldn't set " + skill + " to rank " + rank
                    + (node == null ? "" : " (it must be unlocked; max rank " + node.maxRank() + ")")));
            return 0;
        }
        return progress(context);
    }

    private static LiteralArgumentBuilder<ServerCommandSource> bestiaryCommand(String literal, boolean unlock) {
        return CommandManager.literal(literal)
                .then(CommandManager.argument("entity", IdentifierArgumentType.identifier())
                        .suggests((context, builder) -> CommandSource.suggestIdentifiers(Registries.ENTITY_TYPE.getIds(),
                                builder))
                        .executes(context -> {
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                            Identifier id = IdentifierArgumentType.getIdentifier(context, "entity");
                            if (!Registries.ENTITY_TYPE.containsId(id)) {
                                context.getSource().sendError(Text.literal("Unknown entity " + id));
                                return 0;
                            }
                            boolean done = unlock ? Progression.unlockBestiary(player, id.toString(), true)
                                    : Progression.learn(player, id.toString());
                            if (!done) {
                                context.getSource().sendError(Text.literal("Couldn't " + literal + " " + id
                                        + " (already done, or the class has no bestiary)"));
                                return 0;
                            }
                            return progress(context);
                        }));
    }

    private static int progress(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        ClassProgress progress = Progression.current(player);
        SkillNode active = progress.activeNode();
        context.getSource().sendFeedback(Text.literal(player.getEntityName() + " " + name(progress.dndClass)
                + ": level " + progress.level() + ", " + progress.xp + " xp, " + progress.points() + " points"
                + ", subclass " + (progress.subclass.isEmpty() ? "-" : progress.subclass)
                + ", unlocked " + progress.unlocked + ", active " + (active == null ? "-" : active.id())
                + ", passives " + progress.passives + ", ranks " + progress.ranks
                + (progress.usesBestiary() ? ", learned " + progress.learned + ", bestiary " + progress.bestiary
                        : "")),
                false);
        return progress.level();
    }

    private static int subclass(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        String id = StringArgumentType.getString(context, "subclass");
        if (id.equals("none")) {
            int refunded = Progression.clearSubclass(player);
            if (refunded < 0) {
                context.getSource().sendError(Text.literal(player.getEntityName() + " has no subclass"));
                return 0;
            }
            context.getSource().sendFeedback(Text.literal("Cleared " + player.getEntityName() + "'s subclass, "
                    + refunded + " point" + (refunded == 1 ? "" : "s") + " refunded"), true);
            return progress(context);
        }
        if (!Progression.chooseSubclass(player, id, true)) {
            context.getSource().sendError(Text.literal("Couldn't set subclass " + id + " (it must be one of "
                    + ClassTrees.subclasses(Progression.classOf(player)).stream().map(Subclass::id).toList()
                    + ", and not the current one)"));
            return 0;
        }
        return progress(context);
    }

    private static int xp(CommandContext<ServerCommandSource> context, boolean add) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        int amount = IntegerArgumentType.getInteger(context, "amount");
        if (add) {
            Progression.addXp(player, amount);
        } else {
            Progression.setXp(player, Progression.classOf(player), amount);
        }
        return progress(context);
    }

    private static int resetProgress(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        Progression.reset(player, Progression.classOf(player));
        context.getSource().sendFeedback(Text.literal("Reset " + player.getEntityName() + "'s "
                + name(Progression.classOf(player)) + " progress"), true);
        return 1;
    }

    private static int get(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        DndCharacter dndClass = ((PlayerEntityExt) player).getDndClass();
        if (dndClass == null) {
            dndClass = DndCharacter.NONE;
        }
        String className = name(dndClass);
        context.getSource().sendFeedback(
                Text.literal(player.getEntityName() + " is " + className), false);
        return dndClass.getValue();
    }

    private static int set(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        String className = StringArgumentType.getString(context, "class");
        DndCharacter dndClass = Arrays.stream(DndCharacter.values())
                .filter(c -> name(c).equalsIgnoreCase(className))
                .findFirst()
                .orElseThrow(() -> UNKNOWN_CLASS.create(className));

        ClassLifecycle.change(player, dndClass);
        context.getSource().sendFeedback(
                Text.literal("Set " + player.getEntityName() + "'s class to " + name(dndClass)), true);
        return 1;
    }

    private static String name(DndCharacter dndClass) {
        return dndClass.name().toLowerCase(Locale.ROOT);
    }
}
