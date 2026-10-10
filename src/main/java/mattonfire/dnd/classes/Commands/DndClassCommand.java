package mattonfire.dnd.classes.Commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

import mattonfire.dnd.classes.ClassInfo;
import mattonfire.dnd.classes.ClassLifecycle;
import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.Abilities.AbilityScores;
import mattonfire.dnd.classes.Abilities.CharacterSheet;
import mattonfire.dnd.classes.Abilities.Contribution;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.SkillChecks.D20;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Downed.Downed;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Progression.ClassProgress;
import mattonfire.dnd.classes.Progression.ClassTrees;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Progression.SkillNode;
import mattonfire.dnd.classes.Progression.Subclass;
import mattonfire.dnd.classes.Rest.Charges;
import mattonfire.dnd.classes.Rest.HitDice;
import mattonfire.dnd.classes.Rest.RestKind;
import mattonfire.dnd.classes.Rest.RestSnapshot;
import mattonfire.dnd.classes.Rest.RestSource;
import mattonfire.dnd.classes.Rest.RestState;
import mattonfire.dnd.classes.Rest.RestSync;
import mattonfire.dnd.classes.Rest.Rests;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
 * /dndclass rest <player> short|long         (applies the rest's benefits, ignoring its limits)
 * /dndclass charges <player> [n]
 * /dndclass hitdice <player> [n]
 * /dndclass sheet <player>                    (ability scores, saves, skills, passives)
 * /dndclass score <player> <ability> <value>|clear   (admin override of one score)
 * /dndclass forceroll <player> <n...>|clear   (rigs the player's next d20 naturals, for tests)
 * /dndclass down <player>                     (Downed, death saves, even with nobody near)
 * /dndclass stabilise <player>                (a Downed player stops rolling and stands up in 30 s)
 * /dndclass revive <player> [hp]              (a Downed player stands up, default 1 HP)
 * /dndclass downed <player>                   (prints the death save tally)
 * /dndclass laststand <player> [ready]        (Last Stand cooldown; ready clears it)
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
                .then(CommandManager.literal("rest")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.literal("short")
                                        .executes(context -> rest(context, RestKind.SHORT)))
                                .then(CommandManager.literal("long")
                                        .executes(context -> rest(context, RestKind.LONG)))
                                .then(CommandManager.literal("allow")
                                        .executes(context -> {
                                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context,
                                                    "player");
                                            Rests.forgetLongRest(player);
                                            context.getSource().sendFeedback(Text.literal(player.getEntityName()
                                                    + " can take a long rest again today"), true);
                                            return 1;
                                        }))))
                .then(CommandManager.literal("charges")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(DndClassCommand::restInfo)
                                .then(CommandManager.argument("n", IntegerArgumentType.integer(0))
                                        .executes(context -> {
                                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context,
                                                    "player");
                                            Charges.set(player, IntegerArgumentType.getInteger(context, "n"));
                                            return restInfo(context);
                                        }))))
                .then(CommandManager.literal("hitdice")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(DndClassCommand::restInfo)
                                .then(CommandManager.argument("n", IntegerArgumentType.integer(0))
                                        .executes(context -> {
                                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context,
                                                    "player");
                                            HitDice.setRemaining(player, IntegerArgumentType.getInteger(context,
                                                    "n"));
                                            return restInfo(context);
                                        }))))
                .then(CommandManager.literal("sheet")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(DndClassCommand::sheet)))
                .then(CommandManager.literal("score")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.argument("ability", StringArgumentType.word())
                                        .suggests((context, builder) -> CommandSource.suggestMatching(
                                                Arrays.stream(Ability.values()).map(Ability::shortKey), builder))
                                        .then(CommandManager.literal("clear")
                                                .executes(context -> score(context, null)))
                                        .then(CommandManager.argument("value", IntegerArgumentType.integer(
                                                Contribution.MIN_SCORE, Contribution.MAX_SCORE))
                                                .executes(context -> score(context,
                                                        IntegerArgumentType.getInteger(context, "value")))))))
                .then(CommandManager.literal("forceroll")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.literal("clear")
                                        .executes(context -> {
                                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                            int dropped = D20.clearForced(player.getUuid());
                                            context.getSource().sendFeedback(Text.literal("Cleared " + dropped
                                                    + " rigged rolls for " + player.getEntityName()), false);
                                            return dropped;
                                        }))
                                .then(CommandManager.argument("naturals", StringArgumentType.greedyString())
                                        .executes(DndClassCommand::forceRoll))))
                .then(CommandManager.literal("down")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(context -> {
                                    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                    if (Downed.is(player) || !player.isAlive()) {
                                        context.getSource().sendError(Text.literal(player.getEntityName()
                                                + " is already Downed or dead"));
                                        return 0;
                                    }
                                    Downed.down(player, player.getDamageSources().generic());
                                    return downedInfo(context);
                                })))
                .then(CommandManager.literal("stabilise")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(context -> {
                                    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                    if (!Downed.is(player)) {
                                        context.getSource().sendError(Text.literal(player.getEntityName()
                                                + " isn't Downed"));
                                        return 0;
                                    }
                                    Downed.stabilise(player);
                                    return downedInfo(context);
                                })))
                .then(CommandManager.literal("revive")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(context -> revive(context, 1))
                                .then(CommandManager.argument("hp", IntegerArgumentType.integer(1))
                                        .executes(context -> revive(context,
                                                IntegerArgumentType.getInteger(context, "hp"))))))
                .then(CommandManager.literal("laststand")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(context -> lastStand(context, false))
                                .then(CommandManager.literal("ready")
                                        .executes(context -> lastStand(context, true)))))
                .then(CommandManager.literal("downed")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(DndClassCommand::downedInfo)))
                .then(CommandManager.literal("resetprogress")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(DndClassCommand::resetProgress)))
                // Refills (or drains) a player's mana pips, for testing actives without waiting
                .then(CommandManager.literal("mana")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.argument("pips",
                                        IntegerArgumentType.integer(0, mattonfire.dnd.classes.DnDClasses.MANA_ICONS))
                                        .executes(context -> {
                                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context,
                                                    "player");
                                            int pips = IntegerArgumentType.getInteger(context, "pips");
                                            mattonfire.dnd.classes.ManaManager.setMana(player, pips);
                                            mattonfire.dnd.classes.ManaManager.sync(player);
                                            context.getSource().sendFeedback(Text.literal("Set "
                                                    + player.getEntityName() + "'s mana to " + pips), false);
                                            return pips;
                                        })))));
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

    private static int rest(CommandContext<ServerCommandSource> context, RestKind kind)
            throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        Rests.complete(player, kind, RestSource.ADMIN);
        context.getSource().sendFeedback(Text.literal("Gave " + player.getEntityName() + " a " + kind.label()), true);
        return restInfo(context);
    }

    private static int revive(CommandContext<ServerCommandSource> context, int hp) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        if (!Downed.is(player)) {
            context.getSource().sendError(Text.literal(player.getEntityName() + " isn't Downed"));
            return 0;
        }
        Downed.revive(player, hp);
        context.getSource().sendFeedback(Text.literal("Revived " + player.getEntityName() + " with "
                + (int) player.getHealth() + " HP"), true);
        return 1;
    }

    /** Prints (or clears) the Last Stand cooldown; returns the seconds left. */
    private static int lastStand(CommandContext<ServerCommandSource> context, boolean ready)
            throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        RestState state = RestState.get(player);
        if (ready) {
            state.lastStandReadyAt = 0;
            state.save(player);
        }
        long left = Math.max(0, (state.lastStandReadyAt - player.getServer().getOverworld().getTime() + 19) / 20);
        context.getSource().sendFeedback(Text.literal(player.getEntityName() + ": Last Stand "
                + (left == 0 ? "ready" : "in " + left + " s")), false);
        return (int) left;
    }

    /** Prints a player's Downed state; returns 1 if Downed. */
    private static int downedInfo(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        int[] tally = Downed.tally(player);
        if (tally == null) {
            context.getSource().sendFeedback(Text.literal(player.getEntityName() + " isn't Downed"), false);
            return 0;
        }
        boolean stable = Downed.isStable(player);
        context.getSource().sendFeedback(Text.literal(player.getEntityName() + ": Downed"
                + (stable ? ", stable, standing up in " + tally[2] + " s"
                        : ", " + tally[0] + " passes, " + tally[1] + " fails, next save in " + tally[2] + " s")),
                false);
        return 1;
    }

    /** Prints charges, Hit Dice and short rests left; returns the charges. */
    private static int restInfo(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        RestSnapshot rest = RestSync.snapshot(player);
        context.getSource().sendFeedback(Text.literal(player.getEntityName() + ": " + rest.charges() + "/"
                + rest.max() + " charges" + (rest.temp() > 0 ? " (+" + rest.temp() + " temporary)" : "")
                + ", recharge on a " + rest.rechargeGroup().label() + ", " + rest.hitDiceLeft() + "/"
                + rest.hitDiceMax() + " Hit Dice (d" + rest.dieSize() + "), " + rest.shortRestsLeft()
                + " short rests left" + (rest.enabled() ? "" : " (dndRests is off)")), false);
        return rest.charges();
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

    private static String signed(int n) {
        return n < 0 ? Integer.toString(n) : "+" + n;
    }

    private static int sheet(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        CharacterSheet sheet = AbilityScores.sheet(player);
        ServerCommandSource source = context.getSource();
        source.sendFeedback(Text.literal(player.getEntityName() + " (" + name(sheet.dndClass()) + ", level "
                + sheet.level() + "): proficiency " + signed(sheet.proficiencyBonus()) + ", crit "
                + sheet.critRange() + "-20, attack " + signed(sheet.attackBonus())
                + (sheet.rerollNaturalOnes() ? ", rerolls natural 1s" : "")), false);
        MutableText scores = Text.literal("Scores:");
        MutableText saves = Text.literal("Saves:");
        for (Ability a : Ability.values()) {
            scores.append(" ").append(Text.translatable(a.shortTranslationKey()))
                    .append(" " + sheet.score(a) + " (" + signed(sheet.modifier(a)) + ")");
            saves.append(" ").append(Text.translatable(a.shortTranslationKey()))
                    .append(" " + signed(sheet.save(a)) + sheet.saveProficiency(a).symbol());
        }
        source.sendFeedback(scores, false);
        source.sendFeedback(saves, false);
        MutableText skills = Text.literal("Skills:");
        boolean first = true;
        for (Skill s : Skill.values()) {
            skills.append(first ? " " : ", ").append(Text.translatable(s.translationKey()))
                    .append(" " + signed(sheet.check(s)) + sheet.skillProficiency(s).symbol());
            first = false;
        }
        source.sendFeedback(skills, false);
        source.sendFeedback(Text.literal("Passive: ").append(Text.translatable(Skill.PERCEPTION.translationKey()))
                .append(" " + sheet.passive(Skill.PERCEPTION) + ", ")
                .append(Text.translatable(Skill.INSIGHT.translationKey()))
                .append(" " + sheet.passive(Skill.INSIGHT) + ", ")
                .append(Text.translatable(Skill.STEALTH.translationKey()))
                .append(" " + sheet.passive(Skill.STEALTH) + String.format(" (sneaking: noticed at x%.2f range)",
                        0.8 * mattonfire.dnd.classes.SkillChecks.Stealth.factor(sheet.passive(Skill.STEALTH)))),
                false);
        for (CharacterSheet.Line line : sheet.lines()) {
            if (!line.source().equals(displayName(sheet.dndClass()))) {
                source.sendFeedback(Text.literal("  " + line.source() + ": " + line.target() + " " + line.detail())
                        .formatted(Formatting.GRAY), false);
            }
        }
        return sheet.proficiencyBonus();
    }

    /** The class's display name, which the class contributor uses as its source text. */
    private static String displayName(DndCharacter dndClass) {
        ClassInfo info = ClassInfo.get(dndClass);
        return info == null ? "" : info.name();
    }

    private static int score(CommandContext<ServerCommandSource> context, Integer value)
            throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        String abilityName = StringArgumentType.getString(context, "ability");
        Ability ability = Ability.byId(abilityName);
        if (ability == null) {
            context.getSource().sendError(Text.literal("Unknown ability " + abilityName + " (STR, DEX, CON, INT, WIS, CHA)"));
            return 0;
        }
        AbilityScores.setOverride(player, ability, value);
        int score = AbilityScores.score(player, ability);
        context.getSource().sendFeedback(Text.literal((value == null ? "Cleared the override: " : "Set ")
                + player.getEntityName() + "'s " + ability.shortKey() + " to " + score + " ("
                + signed(Ability.modifier(score)) + ")"), true);
        return score;
    }

    private static int forceRoll(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        String text = StringArgumentType.getString(context, "naturals");
        List<Integer> naturals = new ArrayList<>();
        for (String part : text.trim().split("[\\s,]+")) {
            int n;
            try {
                n = Integer.parseInt(part);
            } catch (NumberFormatException e) {
                n = 0;
            }
            if (n < 1 || n > 20) {
                context.getSource().sendError(Text.literal("Not a d20 roll: " + part + " (use 1-20)"));
                return 0;
            }
            naturals.add(n);
        }
        D20.force(player, naturals);
        context.getSource().sendFeedback(Text.literal("Rigged " + player.getEntityName() + "'s next d20 rolls: "
                + naturals), false);
        return naturals.size();
    }

    private static String name(DndCharacter dndClass) {
        return dndClass.name().toLowerCase(Locale.ROOT);
    }
}
