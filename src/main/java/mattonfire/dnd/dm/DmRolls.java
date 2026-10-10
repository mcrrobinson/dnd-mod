package mattonfire.dnd.dm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.Abilities.Advantage;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.SkillChecks.D20;
import mattonfire.dnd.classes.SkillChecks.SaveResult;
import mattonfire.dnd.classes.SkillChecks.SavingThrow;
import mattonfire.dnd.classes.SkillChecks.SkillCheck;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * DM-called checks and saves:
 *
 * <pre>
 * /dm check &lt;players&gt; &lt;skill&gt; &lt;dc&gt; [adv|dis] [silent]    skill check on the main d20 panel
 * /dm save &lt;targets&gt; &lt;ability&gt; &lt;dc&gt; [adv|dis] [silent]  saving throw on the save lane (mobs roll +1, silently)
 * </pre>
 *
 * Each roll is reported to the caller and logged as a {@code [D20]} line. {@code silent} rolls without showing the
 * targets anything. For command blocks and functions: the result is the total (one target) or the number of
 * successes (several), and a roll nobody succeeds at makes the command fail, so
 * {@code execute store success ... run dm save @p dex 14} stores 1 or 0.
 */
final class DmRolls {
    private static final DynamicCommandExceptionType UNKNOWN_SKILL = new DynamicCommandExceptionType(
            s -> Text.literal("Unknown skill " + s));
    private static final DynamicCommandExceptionType UNKNOWN_ABILITY = new DynamicCommandExceptionType(
            s -> Text.literal("Unknown ability " + s + " (STR, DEX, CON, INT, WIS or CHA)"));
    private static final DynamicCommandExceptionType FAILED = new DynamicCommandExceptionType(
            what -> Text.literal("Failed: " + what));

    private DmRolls() {
    }

    @FunctionalInterface
    private interface Run {
        int run(CommandContext<ServerCommandSource> context, Advantage mode, boolean silent)
                throws CommandSyntaxException;
    }

    static LiteralArgumentBuilder<ServerCommandSource> check() {
        return CommandManager.literal("check")
                .then(CommandManager.argument("targets", EntityArgumentType.players())
                        .then(CommandManager.argument("skill", StringArgumentType.word())
                                .suggests((context, builder) -> CommandSource.suggestMatching(
                                        Arrays.stream(Skill.values()).map(Skill::id), builder))
                                .then(options(CommandManager.argument("dc", IntegerArgumentType.integer(1, 40)),
                                        DmRolls::runCheck))));
    }

    static LiteralArgumentBuilder<ServerCommandSource> save() {
        return CommandManager.literal("save")
                .then(CommandManager.argument("targets", EntityArgumentType.entities())
                        .then(CommandManager.argument("ability", StringArgumentType.word())
                                .suggests((context, builder) -> CommandSource.suggestMatching(
                                        Arrays.stream(Ability.values()).map(Ability::id), builder))
                                .then(options(CommandManager.argument("dc", IntegerArgumentType.integer(1, 40)),
                                        DmRolls::runSave))));
    }

    /** {@code <dc>} then optional {@code adv|dis} and {@code silent}, in that order. */
    private static <T extends ArgumentBuilder<ServerCommandSource, T>> T options(T dc, Run run) {
        dc.executes(c -> run.run(c, Advantage.NORMAL, false))
                .then(CommandManager.literal("silent").executes(c -> run.run(c, Advantage.NORMAL, true)));
        for (Advantage mode : new Advantage[] { Advantage.ADVANTAGE, Advantage.DISADVANTAGE }) {
            dc.then(CommandManager.literal(mode == Advantage.ADVANTAGE ? "adv" : "dis")
                    .executes(c -> run.run(c, mode, false))
                    .then(CommandManager.literal("silent").executes(c -> run.run(c, mode, true))));
        }
        return dc;
    }

    private static int runCheck(CommandContext<ServerCommandSource> context, Advantage mode, boolean silent)
            throws CommandSyntaxException {
        Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "targets");
        String skillText = StringArgumentType.getString(context, "skill");
        Skill skill = Skill.byId(skillText);
        if (skill == null) {
            throw UNKNOWN_SKILL.create(skillText);
        }
        int dc = IntegerArgumentType.getInteger(context, "dc");
        ServerCommandSource source = context.getSource();
        List<D20.Roll> rolls = new ArrayList<>();
        for (ServerPlayerEntity player : players) {
            D20.Roll roll = SkillCheck.builder(player, skill, dc, "dm").mode(mode)
                    .display(silent ? D20.Display.SILENT : D20.Display.MAIN).roll();
            D20.show(player, roll, Text.translatable("dm.dndclasses.roll.called", source.getName()));
            report(source, player, roll);
            rolls.add(roll);
        }
        return result(rolls, Text.translatable(skill.translationKey()).getString() + " vs DC " + dc);
    }

    private static int runSave(CommandContext<ServerCommandSource> context, Advantage mode, boolean silent)
            throws CommandSyntaxException {
        Collection<? extends Entity> targets = EntityArgumentType.getEntities(context, "targets");
        String abilityText = StringArgumentType.getString(context, "ability");
        Ability ability = Ability.byId(abilityText);
        if (ability == null) {
            throw UNKNOWN_ABILITY.create(abilityText);
        }
        int dc = IntegerArgumentType.getInteger(context, "dc");
        ServerCommandSource source = context.getSource();
        List<D20.Roll> rolls = new ArrayList<>();
        for (Entity entity : targets) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            SavingThrow.Builder b = SavingThrow.of(living, ability, dc).source(source.getEntity()).tags("dm")
                    .mode(mode);
            SaveResult result = (silent ? b.silent() : b).roll();
            report(source, living, result.roll());
            rolls.add(result.roll());
        }
        return result(rolls, Text.translatable(ability.saveTranslationKey()).getString() + " vs DC " + dc);
    }

    private static void report(ServerCommandSource source, Entity who, D20.Roll roll) {
        StringBuilder line = new StringBuilder(who.getName().getString()).append(": ")
                .append(roll.label().getString()).append(' ').append(roll.natural());
        if (roll.natural2() > 0) {
            line.append(" (").append(roll.natural2()).append(roll.mode() == Advantage.ADVANTAGE ? " adv)" : " dis)");
        }
        line.append(' ').append(String.format("%+d", roll.modifier())).append(" = ").append(roll.total())
                .append(" vs DC ").append(roll.dc()).append(" -> ").append(roll.outcome());
        source.sendFeedback(Text.literal(line.toString()), false);
    }

    /** The total for one roll, the number of successes for several; fails if nobody succeeded. */
    private static int result(List<D20.Roll> rolls, String what) throws CommandSyntaxException {
        int successes = 0;
        for (D20.Roll roll : rolls) {
            if (roll.outcome().succeeded()) {
                successes++;
            }
        }
        if (successes == 0) {
            throw FAILED.create(what);
        }
        return rolls.size() == 1 ? rolls.get(0).total() : successes;
    }
}
