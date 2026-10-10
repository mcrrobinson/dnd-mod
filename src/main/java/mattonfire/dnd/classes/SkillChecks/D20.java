package mattonfire.dnd.classes.SkillChecks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.Abilities.AbilityScores;
import mattonfire.dnd.classes.Abilities.Advantage;
import mattonfire.dnd.classes.Abilities.CharacterSheet;
import mattonfire.dnd.classes.Abilities.RollKind;
import mattonfire.dnd.classes.Abilities.RollQuery;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Identifier;

/**
 * d20 rolls: roll a d20 (or two, with advantage or disadvantage), add a modifier and beat a DC. A
 * natural 20 always succeeds and a natural 1 always fails. Most callers go through {@link SkillCheck},
 * which takes the modifier and any advantage from the player's
 * {@link mattonfire.dnd.classes.Abilities.CharacterSheet}; {@link #roll(PlayerEntity)} is the raw builder
 * for anything else (death saves, DM flat rolls, attacks).
 *
 * A roll is shown to the player on the HUD with a sound ({@code DiceRollHud} on the client, sent with
 * {@link #show}) and every roll is logged as a {@code [D20]} line.
 *
 * <ul>
 * <li>{@link Lockpicking}: Rogues pick the locks of dungeon and lair loot chests (Thieves' Tools)</li>
 * <li>{@link Persuasion}: Bards talk villagers into better prices (Persuasion)</li>
 * <li>{@link AttackRolls}: every full-strength melee swing rolls; natural 20 crits, natural 1 fumbles</li>
 * <li>{@code Obstacles.ObstacleInteractions}: class-gated obstacles (Arcane Seals), through
 * {@link SkillCheck}</li>
 * </ul>
 */
public final class D20 {
    public static final Identifier S2C_ROLL = new Identifier(DnDClasses.MOD_ID, "d20_roll");

    /** Labels of the original checks. */
    public static final String LOCKPICKING = "skill.dndclasses.lockpicking";
    public static final String PERSUASION = "skill.dndclasses.persuasion";
    public static final String ATTACK = "skill.dndclasses.attack";

    /** Roll flag: show the player their total but not the DC or the outcome (secret DM rolls). */
    public static final int FLAG_SECRET = 1;

    /** Rigged naturals per player for tests ({@code /dndclass forceroll}); used before any random roll. */
    private static final Map<UUID, Deque<Integer>> FORCED = new HashMap<>();

    private D20() {
    }

    public enum Outcome {
        SUCCESS,
        FAILURE,
        /** Natural 20 (or the top of an extended crit range). */
        CRITICAL,
        /** Natural 1. */
        FUMBLE;

        public boolean succeeded() {
            return this == SUCCESS || this == CRITICAL;
        }

        public String translationKey() {
            return "skill.dndclasses.outcome." + name().toLowerCase();
        }
    }

    /** Where the client draws a roll. */
    public enum Display {
        /** The big panel under the crosshair. */
        MAIN,
        /** The compact saving-throw rows beside the crosshair ({@code SaveLaneHud}). */
        SAVE_LANE,
        /** Logged but never sent (non-player and passive rolls). */
        SILENT
    }

    /** A named part of the modifier, shown on the HUD ("+2 assist"). Already included in {@code modifier}. */
    public record Bonus(Text label, int amount) {
    }

    /**
     * A rolled d20.
     *
     * @param label    shown as the HUD title, e.g. {@code Text.translatable(skill.translationKey())}
     * @param ability  the ability behind the roll, or null (attacks, flat rolls)
     * @param natural  the die that counts
     * @param natural2 the other die with advantage or disadvantage (dropped), 0 for a single die
     * @param mode     advantage, disadvantage or neither
     * @param rerolled how many natural 1s were rerolled (Halfling Lucky), 0 if none
     * @param modifier everything added to the natural, bonuses included
     * @param bonuses  named parts of the modifier for display
     * @param dc       0 for rolls without a target number (attack rolls)
     * @param flags    {@link #FLAG_SECRET}
     */
    public record Roll(Text label, @Nullable Ability ability, RollKind kind, int natural, int natural2,
            Advantage mode, int rerolled, int modifier, List<Bonus> bonuses, int dc, Outcome outcome,
            Display display, int flags) {

        public int total() {
            return natural + modifier;
        }

        public boolean secret() {
            return (flags & FLAG_SECRET) != 0;
        }

        public Roll withDisplay(Display newDisplay) {
            return new Roll(label, ability, kind, natural, natural2, mode, rerolled, modifier, bonuses, dc, outcome,
                    newDisplay, flags);
        }

        public void write(PacketByteBuf buf) {
            buf.writeText(label);
            buf.writeByte(ability == null ? -1 : ability.ordinal());
            buf.writeEnumConstant(kind);
            buf.writeVarInt(natural);
            buf.writeVarInt(natural2);
            buf.writeEnumConstant(mode);
            buf.writeVarInt(rerolled);
            buf.writeVarInt(modifier);
            buf.writeCollection(bonuses, (b, bonus) -> {
                b.writeText(bonus.label());
                b.writeVarInt(bonus.amount());
            });
            buf.writeVarInt(dc);
            buf.writeEnumConstant(outcome);
            buf.writeEnumConstant(display);
            buf.writeVarInt(flags);
        }

        public static Roll read(PacketByteBuf buf) {
            Text label = buf.readText();
            int abilityIndex = buf.readByte();
            Ability ability = abilityIndex < 0 ? null : Ability.values()[abilityIndex];
            RollKind kind = buf.readEnumConstant(RollKind.class);
            int natural = buf.readVarInt();
            int natural2 = buf.readVarInt();
            Advantage mode = buf.readEnumConstant(Advantage.class);
            int rerolled = buf.readVarInt();
            int modifier = buf.readVarInt();
            List<Bonus> bonuses = buf.readList(b -> new Bonus(b.readText(), b.readVarInt()));
            int dc = buf.readVarInt();
            Outcome outcome = buf.readEnumConstant(Outcome.class);
            Display display = buf.readEnumConstant(Display.class);
            int flags = buf.readVarInt();
            return new Roll(label, ability, kind, natural, natural2, mode, rerolled, modifier, bonuses, dc, outcome,
                    display, flags);
        }
    }

    /**
     * A raw roll builder. {@link SkillCheck} fills one in from the character sheet. Non-players can roll too
     * (silent mob saves, see {@link SavingThrow}); they have no sheet, so {@link Builder#sheetAdvantage} does
     * nothing for them.
     */
    public static Builder roll(LivingEntity roller) {
        return new Builder(roller);
    }

    public static final class Builder {
        private final LivingEntity player;
        private Text label = Text.literal("d20");
        @Nullable
        private Ability ability;
        private RollKind kind = RollKind.CHECK;
        private int modifier;
        private final List<Bonus> bonuses = new ArrayList<>();
        private int dc;
        private int advantages;
        private int disadvantages;
        private int critRange = 20;
        private boolean rerollNaturalOnes;
        private Display display = Display.MAIN;
        private int flags;
        /** A fixed natural instead of rolling (0: roll). */
        private int taken;

        private Builder(LivingEntity player) {
            this.player = player;
        }

        public Builder label(Text label) {
            this.label = label;
            return this;
        }

        /** A translation key label. */
        public Builder label(String translationKey) {
            return label(Text.translatable(translationKey));
        }

        public Builder ability(@Nullable Ability ability) {
            this.ability = ability;
            return this;
        }

        public Builder kind(RollKind kind) {
            this.kind = kind;
            return this;
        }

        /** The base modifier (replaces any earlier one; bonuses are kept). */
        public Builder modifier(int modifier) {
            this.modifier = modifier;
            return this;
        }

        /** Adds a named bonus on top of the modifier ("+2 assist"). */
        public Builder bonus(Text label, int amount) {
            bonuses.add(new Bonus(label, amount));
            return this;
        }

        public Builder bonus(String translationKey, int amount) {
            return bonus(Text.translatable(translationKey), amount);
        }

        public Builder dc(int dc) {
            this.dc = dc;
            return this;
        }

        /** Adds one source of advantage. Advantage and disadvantage cancel out (5e). */
        public Builder advantage() {
            advantages++;
            return this;
        }

        public Builder disadvantage() {
            disadvantages++;
            return this;
        }

        public Builder advantageIf(boolean condition) {
            return condition ? advantage() : this;
        }

        public Builder disadvantageIf(boolean condition) {
            return condition ? disadvantage() : this;
        }

        /** Adds a source of advantage or disadvantage (NORMAL adds nothing). */
        public Builder mode(Advantage mode) {
            return switch (mode) {
                case ADVANTAGE -> advantage();
                case DISADVANTAGE -> disadvantage();
                case NORMAL -> this;
            };
        }

        /** Applies the advantage sources on the player's sheet that match this roll. */
        public Builder sheetAdvantage(RollQuery query) {
            return player.getWorld().isClient || !(player instanceof PlayerEntity p) ? this
                    : mode(AbilityScores.sheet(p).advantage(query));
        }

        /** The lowest natural that is a CRITICAL (attacks: the sheet's crit range). */
        public Builder critRange(int lowest) {
            this.critRange = lowest;
            return this;
        }

        public Builder rerollNaturalOnes(boolean reroll) {
            this.rerollNaturalOnes = reroll;
            return this;
        }

        public Builder display(Display display) {
            this.display = display;
            return this;
        }

        public Builder flags(int flags) {
            this.flags |= flags;
            return this;
        }

        /**
         * "Take" a natural instead of rolling, e.g. 20 for an obstacle you take your time over. No die
         * is rolled (rigged rolls are left alone), advantage and rerolls don't apply, and the taken
         * natural is never a CRITICAL or a FUMBLE: it succeeds or fails on the total alone.
         */
        public Builder take(int natural) {
            this.taken = natural;
            return this;
        }

        public Builder secret() {
            return flags(FLAG_SECRET);
        }

        /** Rolls and logs it. Show it with {@link D20#show}. */
        public Roll roll() {
            if (taken > 0) {
                return taken();
            }
            Advantage mode = Advantage.resolve(advantages, disadvantages);
            int rerolled = 0;
            int a = d20(player);
            if (rerollNaturalOnes && a == 1) {
                a = d20(player);
                rerolled++;
            }
            int natural = a;
            int natural2 = 0;
            if (mode != Advantage.NORMAL) {
                int b = d20(player);
                if (rerollNaturalOnes && b == 1) {
                    b = d20(player);
                    rerolled++;
                }
                natural = mode == Advantage.ADVANTAGE ? Math.max(a, b) : Math.min(a, b);
                natural2 = natural == a ? b : a;
            }
            int total = total();
            Outcome outcome;
            if (natural == 1) {
                outcome = Outcome.FUMBLE;
            } else if (natural >= critRange) {
                outcome = Outcome.CRITICAL;
            } else if (dc > 0) {
                outcome = natural + total >= dc ? Outcome.SUCCESS : Outcome.FAILURE;
            } else {
                outcome = Outcome.SUCCESS;
            }
            return finish(new Roll(label, ability, kind, natural, natural2, mode, rerolled, total,
                    List.copyOf(bonuses), dc, outcome, display, flags));
        }

        private int total() {
            int total = modifier;
            for (Bonus bonus : bonuses) {
                total += bonus.amount();
            }
            return total;
        }

        private Roll taken() {
            int total = total();
            Outcome outcome = dc <= 0 || taken + total >= dc ? Outcome.SUCCESS : Outcome.FAILURE;
            return finish(new Roll(label, ability, kind, taken, 0, Advantage.NORMAL, 0, total, List.copyOf(bonuses),
                    dc, outcome, display, flags));
        }

        private Roll finish(Roll roll) {
            // Every swing rolls an attack; only the ones that do something are worth a log line
            if (roll.kind() != RollKind.ATTACK || roll.outcome() != Outcome.SUCCESS) {
                log(player, roll);
            }
            return roll;
        }
    }

    /** One natural d20: the next rigged value if any ({@code /dndclass forceroll}), else random. */
    public static int d20(LivingEntity player) {
        synchronized (FORCED) {
            Deque<Integer> forced = FORCED.get(player.getUuid());
            if (forced != null && !forced.isEmpty()) {
                int n = forced.poll();
                if (forced.isEmpty()) {
                    FORCED.remove(player.getUuid());
                }
                return n;
            }
        }
        return 1 + player.getRandom().nextInt(20);
    }

    /** Rigs the player's next naturals (each 1-20), in order. */
    public static void force(Entity player, Collection<Integer> naturals) {
        synchronized (FORCED) {
            FORCED.computeIfAbsent(player.getUuid(), k -> new ArrayDeque<>()).addAll(naturals);
        }
    }

    /** Drops any rigged rolls; returns how many were left. */
    public static int clearForced(UUID player) {
        synchronized (FORCED) {
            Deque<Integer> forced = FORCED.remove(player);
            return forced == null ? 0 : forced.size();
        }
    }

    /**
     * Shows the roll on the player's HUD, with a line saying what came of it. SILENT rolls aren't sent.
     * SAVE_LANE rolls go through {@link SavingThrow#send}, which throttles them and merges repeats.
     */
    public static void show(PlayerEntity player, Roll roll, Text detail) {
        if (!(player instanceof ServerPlayerEntity serverPlayer) || roll.display() == Display.SILENT) {
            return;
        }
        if (roll.display() == Display.SAVE_LANE) {
            SavingThrow.send(serverPlayer, roll, detail);
            return;
        }
        send(serverPlayer, roll, detail, 1);
    }

    /**
     * Sends one roll packet as is. {@code count} is how many identical rolls it stands for (the save lane's
     * "x3"); it's written after the detail, and older readers that stop at the detail just ignore it.
     */
    static void send(ServerPlayerEntity player, Roll roll, Text detail, int count) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        roll.write(buf);
        buf.writeText(detail);
        buf.writeVarInt(count);
        ServerPlayNetworking.send(player, S2C_ROLL, buf);
    }

    /**
     * {@code [D20] <player> <label> <natural>[ (<other> adv|dis)] <+mod> = <total>[ vs DC <dc>] -> <outcome>},
     * for logs and DevScript tests.
     */
    public static void log(Entity player, Roll roll) {
        String name = player instanceof PlayerEntity ? player.getEntityName() : player.getName().getString();
        StringBuilder line = new StringBuilder("[D20] ").append(name).append(' ')
                .append(labelForLog(roll.label())).append(' ').append(roll.natural());
        if (roll.natural2() > 0) {
            line.append(" (").append(roll.natural2()).append(' ')
                    .append(roll.mode() == Advantage.ADVANTAGE ? "adv" : "dis").append(')');
        }
        if (roll.rerolled() > 0) {
            line.append(" [rerolled 1]");
        }
        line.append(' ').append(String.format("%+d", roll.modifier())).append(" = ").append(roll.total());
        if (roll.dc() > 0) {
            line.append(" vs DC ").append(roll.dc());
        }
        line.append(" -> ").append(roll.outcome());
        DnDClasses.LOGGER.info(line.toString());
    }

    private static String labelForLog(Text label) {
        String text = label.getString();
        if (label.getContent() instanceof TranslatableTextContent t && text.equals(t.getKey())) {
            return t.getKey(); // no language loaded (dedicated server): the key is still greppable
        }
        return text;
    }

    public static DndCharacter classOf(PlayerEntity player) {
        DndCharacter c = player instanceof PlayerEntityExt ext ? ext.getDndClass() : null;
        return c == null ? DndCharacter.NONE : c;
    }

    /** The sheet behind a player's rolls (server: computed; client: the synced copy). */
    public static CharacterSheet sheet(PlayerEntity player) {
        return AbilityScores.sheet(player);
    }

    public static void register() {
        Lockpicking.register();
        SavingThrow.register();
        Persuasion.register();
        AttackRolls.register();
    }
}
