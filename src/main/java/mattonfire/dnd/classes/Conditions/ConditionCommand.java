package mattonfire.dnd.classes.Conditions;

import java.util.Arrays;
import java.util.Collection;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import mattonfire.dnd.classes.Abilities.Ability;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * {@code /dndcondition} (op level 2), also under {@code /dm condition}:
 * <ul>
 * <li>{@code apply <targets> <condition> [<ticks>] [from <source> | save <ability> <dc> <every> [escalate <condition>
 * <ticks>]]}</li>
 * <li>{@code remove <targets> <condition>|all}</li>
 * <li>{@code list <target>}</li>
 * <li>{@code immune <target> <condition>}: prints the reason; fails when not immune</li>
 * </ul>
 * The result is the number of targets it landed on, for {@code execute store} and "Test passed/failed" checks.
 */
public final class ConditionCommand {
    private static final DynamicCommandExceptionType UNKNOWN = new DynamicCommandExceptionType(
            id -> Text.literal("Unknown condition " + id));
    private static final DynamicCommandExceptionType UNKNOWN_ABILITY = new DynamicCommandExceptionType(
            id -> Text.literal("Unknown ability " + id));

    private static final SuggestionProvider<ServerCommandSource> CONDITIONS = (context, builder) -> CommandSource
            .suggestMatching(Arrays.stream(Condition.values()).map(Condition::id), builder);
    private static final SuggestionProvider<ServerCommandSource> CONDITIONS_OR_ALL = (context, builder) -> CommandSource
            .suggestMatching(java.util.stream.Stream.concat(Arrays.stream(Condition.values()).map(Condition::id),
                    java.util.stream.Stream.of("all")), builder);
    private static final SuggestionProvider<ServerCommandSource> ABILITIES = (context, builder) -> CommandSource
            .suggestMatching(Arrays.stream(Ability.values()).map(Ability::id), builder);

    private ConditionCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(node("dndcondition").requires(source -> source.hasPermissionLevel(2)));
    }

    /** The command tree under {@code name} ({@code /dndcondition}, {@code /dm condition}). */
    public static LiteralArgumentBuilder<ServerCommandSource> node(String name) {
        var escalate = CommandManager.literal("escalate")
                .then(CommandManager.argument("next", StringArgumentType.word()).suggests(CONDITIONS)
                        .then(CommandManager.argument("nextTicks", IntegerArgumentType.integer(-1))
                                .executes(c -> apply(c, ticks(c), Mode.ESCALATE))));
        var save = CommandManager.literal("save")
                .then(CommandManager.argument("ability", StringArgumentType.word()).suggests(ABILITIES)
                        .then(CommandManager.argument("dc", IntegerArgumentType.integer(1, 40))
                                .then(CommandManager.argument("every", IntegerArgumentType.integer(1))
                                        .executes(c -> apply(c, ticks(c), Mode.SAVE))
                                        .then(escalate))));
        var from = CommandManager.literal("from")
                .then(CommandManager.argument("source", EntityArgumentType.entity())
                        .executes(c -> apply(c, ticks(c), Mode.FROM)));
        var apply = CommandManager.literal("apply")
                .then(CommandManager.argument("targets", EntityArgumentType.entities())
                        .then(CommandManager.argument("condition", StringArgumentType.word()).suggests(CONDITIONS)
                                .executes(c -> apply(c, null, Mode.PLAIN))
                                .then(CommandManager.argument("ticks", IntegerArgumentType.integer(-1))
                                        .executes(c -> apply(c, ticks(c), Mode.PLAIN))
                                        .then(from)
                                        .then(save))));
        return CommandManager.literal(name)
                .then(apply)
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("targets", EntityArgumentType.entities())
                                .then(CommandManager.argument("condition", StringArgumentType.word())
                                        .suggests(CONDITIONS_OR_ALL)
                                        .executes(ConditionCommand::remove))))
                .then(CommandManager.literal("list")
                        .then(CommandManager.argument("target", EntityArgumentType.entity())
                                .executes(ConditionCommand::list)))
                .then(CommandManager.literal("immune")
                        .then(CommandManager.argument("target", EntityArgumentType.entity())
                                .then(CommandManager.argument("condition", StringArgumentType.word())
                                        .suggests(CONDITIONS)
                                        .executes(ConditionCommand::immune))));
    }

    private enum Mode {
        PLAIN, FROM, SAVE, ESCALATE
    }

    private static Integer ticks(CommandContext<ServerCommandSource> c) {
        return IntegerArgumentType.getInteger(c, "ticks");
    }

    private static Condition condition(CommandContext<ServerCommandSource> c, String arg)
            throws CommandSyntaxException {
        String id = StringArgumentType.getString(c, arg);
        Condition condition = Condition.byId(id);
        if (condition == null) {
            throw UNKNOWN.create(id);
        }
        return condition;
    }

    private static int apply(CommandContext<ServerCommandSource> c, Integer ticks, Mode mode)
            throws CommandSyntaxException {
        Collection<? extends Entity> targets = EntityArgumentType.getEntities(c, "targets");
        Condition condition = condition(c, "condition");
        int length = ticks == null ? condition.defaultTicks() : ticks;
        Entity source = mode == Mode.FROM ? EntityArgumentType.getEntity(c, "source") : null;
        int landed = 0;
        for (Entity entity : targets) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            Conditions.Builder b = Conditions.of(living, condition).ticks(length);
            if (source != null) {
                b.source(source);
            } else {
                b.sourceName(Text.translatable("condition.dndclasses.source.command"));
            }
            if (mode == Mode.SAVE || mode == Mode.ESCALATE) {
                String abilityId = StringArgumentType.getString(c, "ability");
                Ability ability = Ability.byId(abilityId);
                if (ability == null) {
                    throw UNKNOWN_ABILITY.create(abilityId);
                }
                b.repeatSave(ability, IntegerArgumentType.getInteger(c, "dc"),
                        IntegerArgumentType.getInteger(c, "every"));
                if (mode == Mode.ESCALATE) {
                    b.escalateOnFailure(condition(c, "next"), IntegerArgumentType.getInteger(c, "nextTicks"));
                }
            }
            if (b.apply() != null) {
                landed++;
            }
        }
        int n = landed;
        if (n == 0) {
            c.getSource().sendError(Text.literal("No target took " + condition.id()));
        } else {
            c.getSource().sendFeedback(Text.literal("Applied " + condition.id() + " to " + n + " target"
                    + (n == 1 ? "" : "s")), true);
        }
        return n;
    }

    private static int remove(CommandContext<ServerCommandSource> c) throws CommandSyntaxException {
        Collection<? extends Entity> targets = EntityArgumentType.getEntities(c, "targets");
        String id = StringArgumentType.getString(c, "condition");
        Condition condition = id.equals("all") ? null : condition(c, "condition");
        int n = 0;
        for (Entity entity : targets) {
            if (entity instanceof LivingEntity living) {
                boolean ended = condition == null
                        ? Conditions.endMatching(living, inst -> true, ConditionEvents.EndReason.REMOVED, null)
                        : Conditions.remove(living, condition);
                if (ended) {
                    n++;
                }
            }
        }
        int count = n;
        if (count == 0) {
            c.getSource().sendError(Text.literal("Nothing to remove"));
        } else {
            c.getSource().sendFeedback(Text.literal("Removed " + id + " from " + count + " target"
                    + (count == 1 ? "" : "s")), true);
        }
        return count;
    }

    private static int list(CommandContext<ServerCommandSource> c) throws CommandSyntaxException {
        Entity entity = EntityArgumentType.getEntity(c, "target");
        if (!(entity instanceof LivingEntity living)) {
            c.getSource().sendError(Text.literal("Not a living entity"));
            return 0;
        }
        var all = Conditions.all(living);
        String name = Conditions.name(living);
        if (all.isEmpty()) {
            c.getSource().sendFeedback(Text.literal(name + " has no conditions"), false);
            return 0;
        }
        long now = living.getWorld().getTime();
        c.getSource().sendFeedback(Text.literal(name + " conditions (mask " + Conditions.mask(living) + "):")
                .formatted(Formatting.GOLD), false);
        for (ConditionInstance inst : all) {
            StringBuilder line = new StringBuilder(" ").append(inst.condition().id()).append(" (").append(inst.key())
                    .append(") ");
            int left = inst.remainingTicks(living.getWorld());
            line.append(left < 0 ? "indefinite" : left + "t");
            if (inst.sourceName() != null) {
                line.append(" from ").append(inst.sourceName().getString());
            }
            RepeatSave save = inst.save();
            if (save != null) {
                line.append(", save ").append(save.ability().shortKey()).append(' ').append(save.dc())
                        .append(" every ").append(save.every()).append("t (next in ")
                        .append(Math.max(0, save.nextAt() - now)).append("t)");
                if (save.escalateTo() != null) {
                    line.append(" -> ").append(save.escalateTo().id()).append(' ').append(save.escalateTicks())
                            .append('t');
                }
            }
            if (inst.endsWithSource()) {
                line.append(", ends with source");
            }
            if (inst.transientInstance()) {
                line.append(", transient");
            }
            c.getSource().sendFeedback(Text.literal(line.toString()), false);
        }
        return all.size();
    }

    private static int immune(CommandContext<ServerCommandSource> c) throws CommandSyntaxException {
        Entity entity = EntityArgumentType.getEntity(c, "target");
        Condition condition = condition(c, "condition");
        if (!(entity instanceof LivingEntity living)) {
            c.getSource().sendError(Text.literal("Not a living entity"));
            return 0;
        }
        String reason = Conditions.immuneReason(living, condition, null);
        if (reason == null) {
            c.getSource().sendError(Text.literal(Conditions.name(living) + " is not immune to " + condition.id()));
            return 0;
        }
        c.getSource().sendFeedback(Text.literal(Conditions.name(living) + " is immune to " + condition.id() + ": "
                + Conditions.reasonWord(reason) + " (").append(Text.translatable(reason)).append(")"), false);
        return 1;
    }
}
