package mattonfire.dnd.classes.Commands;

import java.util.Arrays;
import java.util.stream.Stream;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.Feats.Feat;
import mattonfire.dnd.classes.Feats.FeatChoice;
import mattonfire.dnd.classes.Feats.Feats;
import mattonfire.dnd.classes.Progression.ClassProgress;
import mattonfire.dnd.classes.Progression.Progression;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * {@code /dndclass feat <player> add <feat> [ability]} (no table or points; paid as a free feat, which may go past the
 * slots), {@code remove <feat|all> [ability]} (refunds) and {@code list} (with what paid for each; returns the
 * count) and {@code toggle on|off} (sets the N toggle). Works on the player's current class.
 */
final class FeatCommand {
    private FeatCommand() {
    }

    static LiteralArgumentBuilder<ServerCommandSource> node() {
        return CommandManager.literal("feat")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .then(CommandManager.literal("list").executes(FeatCommand::list))
                        .then(CommandManager.literal("toggle")
                                .then(CommandManager.literal("on").executes(context -> toggle(context, true)))
                                .then(CommandManager.literal("off").executes(context -> toggle(context, false))))
                        .then(CommandManager.literal("add")
                                .then(featArgument(false)
                                        .executes(context -> add(context, false))
                                        .then(abilityArgument().executes(context -> add(context, true)))))
                        .then(CommandManager.literal("remove")
                                .then(featArgument(true)
                                        .executes(context -> remove(context, false))
                                        .then(abilityArgument().executes(context -> remove(context, true))))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<ServerCommandSource, String> featArgument(
            boolean all) {
        return CommandManager.argument("feat", StringArgumentType.word())
                .suggests((context, builder) -> CommandSource.suggestMatching(Stream.concat(
                        all ? Stream.of("all") : Stream.empty(), Arrays.stream(Feat.values()).map(Feat::id)),
                        builder));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<ServerCommandSource, String> abilityArgument() {
        return CommandManager.argument("ability", StringArgumentType.word())
                .suggests((context, builder) -> CommandSource.suggestMatching(
                        Arrays.stream(Ability.values()).map(Ability::id), builder));
    }

    private static Ability ability(CommandContext<ServerCommandSource> context, boolean given) {
        return given ? Ability.byId(StringArgumentType.getString(context, "ability")) : null;
    }

    private static int add(CommandContext<ServerCommandSource> context, boolean withAbility)
            throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        String id = StringArgumentType.getString(context, "feat");
        Feat feat = Feat.byId(id);
        if (feat == null) {
            context.getSource().sendError(Text.literal("Unknown feat " + id));
            return 0;
        }
        Ability ability = ability(context, withAbility);
        if (feat.perAbility() && ability == null) {
            context.getSource().sendError(Text.literal(feat.displayName() + " needs an ability (str, dex, con, int,"
                    + " wis, cha)"));
            return 0;
        }
        if (Progression.classOf(player) == DndCharacter.NONE) {
            context.getSource().sendError(Text.literal(player.getEntityName() + " has no class"));
            return 0;
        }
        if (!Progression.chooseFeat(player, feat, ability, true)) {
            context.getSource().sendError(Text.literal("Couldn't add " + feat.displayName() + " (already taken?)"));
            return 0;
        }
        return list(context);
    }

    private static int remove(CommandContext<ServerCommandSource> context, boolean withAbility)
            throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        String id = StringArgumentType.getString(context, "feat");
        Feat feat = "all".equalsIgnoreCase(id) ? null : Feat.byId(id);
        if (feat == null && !"all".equalsIgnoreCase(id)) {
            context.getSource().sendError(Text.literal("Unknown feat " + id));
            return 0;
        }
        int removed = Progression.removeFeats(player, feat, ability(context, withAbility));
        if (removed == 0) {
            context.getSource().sendError(Text.literal(player.getEntityName() + " doesn't have "
                    + (feat == null ? "any feats" : feat.displayName())));
            return 0;
        }
        context.getSource().sendFeedback(Text.literal("Removed " + removed + " feat" + (removed == 1 ? "" : "s")
                + " from " + player.getEntityName() + " (refunded)"), true);
        return list(context);
    }

    /** Sets Power Attack / Steady Aim (N) outright, e.g. so a test script doesn't depend on the last run. */
    private static int toggle(CommandContext<ServerCommandSource> context, boolean on) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        Feats.setToggle(player, on);
        context.getSource().sendFeedback(Text.literal(player.getEntityName() + "'s Power Attack / Steady Aim: "
                + (on ? "on" : "off")), true);
        return on ? 1 : 0;
    }

    private static int list(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        ClassProgress progress = Progression.current(player);
        StringBuilder line = new StringBuilder(player.getEntityName() + "'s " + Progression.name(progress.dndClass)
                + " feats (" + progress.feats.size() + "):");
        if (progress.feats.isEmpty()) {
            line.append(" none");
        }
        for (FeatChoice choice : progress.feats) {
            line.append(" ").append(choice.label()).append(" [").append(choice.source().id()).append("]");
        }
        line.append(". Free feat slots left ").append(progress.freeFeatSlotsLeft()).append(", ability points ")
                .append(progress.asiPoints());
        if (progress.hasFeat(Feat.LUCKY)) {
            line.append(", luck points ").append(Feats.luckPoints(player));
        }
        if (progress.hasFeat(Feat.GREAT_WEAPON_MASTER) || progress.hasFeat(Feat.SHARPSHOOTER)) {
            line.append(", toggle ").append(Feats.toggleOn(player) ? "on" : "off");
        }
        String text = line.toString();
        context.getSource().sendFeedback(Text.literal(text), false);
        return progress.feats.size();
    }
}
