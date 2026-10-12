package mattonfire.dnd.dm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * DM-called checks and saves:
 *
 * <pre>
 * /dm check &lt;players&gt; &lt;skill&gt; &lt;dc&gt; [adv|dis] [silent]    skill check on the main d20 panel
 * /dm save &lt;targets&gt; &lt;ability&gt; &lt;dc&gt; [adv|dis] [silent]  saving throw on the save lane (mobs roll +1, silently)
 * /dm roll &lt;players&gt; &lt;check&gt; &lt;dc&gt; [advantage|disadvantage] [secret] [auto]   roll call ({@link DmRollCalls})
 * /dm apply pass|fail|all &lt;command&gt;              (op only) run a command as and at each player of the last call
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

    private static final DynamicCommandExceptionType UNKNOWN_CHECK = new DynamicCommandExceptionType(
            s -> Text.literal("Unknown check " + s + " (str..cha for a save, a skill, or flat)"));
    private static final DynamicCommandExceptionType UNKNOWN_OPTION = new DynamicCommandExceptionType(
            s -> Text.literal("Unknown option " + s + " (advantage, disadvantage, secret, auto)"));
    private static final SimpleCommandExceptionType NO_PLAYERS = new SimpleCommandExceptionType(
            Text.literal("No players to roll (players in DM mode are skipped unless named alone)"));
    private static final SimpleCommandExceptionType NO_CALL = new SimpleCommandExceptionType(
            Text.literal("No roll call yet"));
    private static final List<String> ROLL_OPTIONS = List.of("advantage", "disadvantage", "secret", "auto");

    private DmRolls() {
    }

    static LiteralArgumentBuilder<ServerCommandSource> roll() {
        return CommandManager.literal("roll")
                .then(CommandManager.argument("targets", EntityArgumentType.players())
                        .then(CommandManager.argument("check", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    List<String> ids = new ArrayList<>();
                                    for (Ability a : Ability.values()) {
                                        ids.add(a.id());
                                    }
                                    for (Skill s : Skill.values()) {
                                        ids.add(s.id());
                                    }
                                    ids.add("flat");
                                    return CommandSource.suggestMatching(ids, builder);
                                })
                                .then(CommandManager.argument("dc", IntegerArgumentType.integer(1, 40))
                                        .executes(c -> runRoll(c, ""))
                                        .then(CommandManager.argument("options", StringArgumentType.greedyString())
                                                .suggests(DmRolls::suggestOptions)
                                                .executes(c -> runRoll(c,
                                                        StringArgumentType.getString(c, "options")))))));
    }

    /** Suggests the options not used yet, after the last space. */
    private static CompletableFuture<Suggestions> suggestOptions(CommandContext<ServerCommandSource> context,
            SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        int space = remaining.lastIndexOf(' ');
        List<String> used = List.of(remaining.substring(0, space + 1).trim().split(" +"));
        SuggestionsBuilder offset = builder.createOffset(builder.getStart() + space + 1);
        List<String> left = new ArrayList<>();
        for (String option : ROLL_OPTIONS) {
            if (!used.contains(option)) {
                left.add(option);
            }
        }
        return CommandSource.suggestMatching(left, offset);
    }

    private static int runRoll(CommandContext<ServerCommandSource> context, String options)
            throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Collection<ServerPlayerEntity> selected = EntityArgumentType.getPlayers(context, "targets");
        String checkText = StringArgumentType.getString(context, "check");
        DmRollCalls.Check check = DmRollCalls.Check.parse(checkText);
        if (check == null) {
            throw UNKNOWN_CHECK.create(checkText);
        }
        int dc = IntegerArgumentType.getInteger(context, "dc");
        Advantage mode = Advantage.NORMAL;
        boolean secret = false;
        boolean auto = false;
        for (String word : options.trim().split(" +")) {
            switch (word.toLowerCase(Locale.ROOT)) {
                case "" -> {
                }
                case "advantage", "adv" -> mode = Advantage.ADVANTAGE;
                case "disadvantage", "dis" -> mode = Advantage.DISADVANTAGE;
                case "secret" -> secret = true;
                case "auto" -> auto = true;
                default -> throw UNKNOWN_OPTION.create(word);
            }
        }
        // The DM isn't rolling with the table (unless they're the only one picked, e.g. @s for a test)
        List<ServerPlayerEntity> players = new ArrayList<>();
        for (ServerPlayerEntity player : selected) {
            if (selected.size() == 1 || !DungeonMaster.isDm(player)) {
                players.add(player);
            }
        }
        if (players.isEmpty()) {
            throw NO_PLAYERS.create();
        }
        DmRollCalls.Call call = DmRollCalls.start(source, players, check, dc, mode, secret, auto);
        if (!auto) {
            source.sendFeedback(Text.literal("Roll call #" + call.number + ": " + call.title()
                    + (mode == Advantage.NORMAL ? "" : mode == Advantage.ADVANTAGE ? " with advantage"
                            : " with disadvantage")
                    + (secret ? " (secret)" : "") + " for " + players.size()
                    + (players.size() == 1 ? " player" : " players") + ", " + DmRollCalls.TIMEOUT_TICKS / 20
                    + " s to roll").formatted(Formatting.GOLD), false);
        }
        return players.size();
    }

    static LiteralArgumentBuilder<ServerCommandSource> apply() {
        LiteralArgumentBuilder<ServerCommandSource> apply = CommandManager.literal("apply")
                .requires(source -> source.hasPermissionLevel(2));
        for (String which : new String[] { "pass", "fail", "all" }) {
            apply.then(CommandManager.literal(which)
                    .then(CommandManager.argument("command", StringArgumentType.greedyString())
                            .executes(c -> runApply(c, which))));
        }
        return apply;
    }

    private static int runApply(CommandContext<ServerCommandSource> context, String which)
            throws CommandSyntaxException {
        DmRollCalls.Call call = DmRollCalls.last();
        if (call == null) {
            throw NO_CALL.create();
        }
        ServerCommandSource source = context.getSource();
        String command = StringArgumentType.getString(context, "command");
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        List<String> names = new ArrayList<>();
        for (UUID uuid : DmRollCalls.playersOf(call, which)) {
            ServerPlayerEntity player = source.getServer().getPlayerManager().getPlayer(uuid);
            if (player == null) {
                continue;
            }
            // Like "execute as <player> at <player> run <command>", keeping the DM's permission level
            ServerCommandSource as = source.withEntity(player).withWorld((ServerWorld) player.getWorld())
                    .withPosition(player.getPos()).withRotation(player.getRotationClient());
            source.getServer().getCommandManager().executeWithPrefix(as, command);
            names.add(player.getEntityName());
        }
        source.sendFeedback(Text.literal("Applied to " + names.size() + " (" + which + ", call #" + call.number
                + ")" + (names.isEmpty() ? "" : ": " + String.join(", ", names))).formatted(Formatting.GOLD),
                true);
        return names.size();
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
