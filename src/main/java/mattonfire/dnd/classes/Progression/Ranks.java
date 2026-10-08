package mattonfire.dnd.classes.Progression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.PlayerEntity;

/**
 * The values an ability has at each rank, declared once and read on both
 * sides: when it fires ({@code PowerUpEffect}, {@link ClassSkills}) and in the
 * skill tree tooltip, which writes its "Now" and "Next" lines from them.
 *
 * <pre>
 * static final Ranks RAGE = Ranks.of("barbarian.rage").value("Strength", 1, 2, 3).seconds("Duration", 8, 12, 15);
 * int ticks = RAGE.ticks(player, "Duration");
 * int amplifier = RAGE.amplifier(player, "Strength");
 * </pre>
 *
 * Declare them in a static field of the class's {@link ClassSkills} subclass, so
 * they exist once {@link ClassTrees} has loaded. Every list has one value per
 * rank, so its length is the ability's maximum rank. An ability without a
 * {@code Ranks} has one rank and works as before.
 *
 * Each rank above 1 costs a skill point ({@link #POINT_COST}) and needs a class
 * level: by default rank 2 at level 3, rank 3 at 6 and rank 4 at 9. Abilities
 * with more ranks set their own with {@link #levels}.
 */
public final class Ranks {
    /** Skill points each rank above 1 costs. */
    public static final int POINT_COST = 1;
    /** Default class level needed for each rank; the index is the rank. */
    private static final int[] DEFAULT_LEVELS = { 0, 0, 3, 6, 9 };

    private static final Map<String, Ranks> ALL = new HashMap<>();

    private enum Format {
        LEVEL, SECONDS, AMOUNT, PERCENT, TEXT
    }

    private record Stat(String name, Format format, String unit, double[] values, String[] texts) {
    }

    private final String skill;
    private final List<Stat> stats = new ArrayList<>();
    private int maxRank = 0;
    private int[] levels = DEFAULT_LEVELS;

    private Ranks(String skill) {
        this.skill = skill;
    }

    /** Starts the ranks of a skill, by node id. */
    public static Ranks of(String skill) {
        Ranks ranks = new Ranks(skill);
        if (ALL.put(skill, ranks) != null) {
            throw new IllegalStateException("Ranks declared twice for " + skill);
        }
        return ranks;
    }

    /** The skill's ranks, or null if it only has one. */
    public static Ranks get(String skill) {
        return ALL.get(skill);
    }

    public static int maxRank(String skill) {
        Ranks ranks = ALL.get(skill);
        return ranks == null ? 1 : ranks.maxRank;
    }

    /** Class level needed to reach this rank of the skill. */
    public static int levelFor(String skill, int rank) {
        Ranks ranks = ALL.get(skill);
        int[] levels = ranks == null ? DEFAULT_LEVELS : ranks.levels;
        return rank < levels.length ? levels[rank] : Integer.MAX_VALUE;
    }

    // ---- Declaring ----

    /** An effect level, shown in roman numerals ("Strength II"). Read it with {@link #amplifier}. */
    public Ranks value(String name, int... perRank) {
        return add(name, Format.LEVEL, "", Arrays.stream(perRank).asDoubleStream().toArray(), null);
    }

    /** A duration in seconds ("12 s"). Read it in ticks with {@link #ticks}. */
    public Ranks seconds(String name, double... perRank) {
        return add(name, Format.SECONDS, "", perRank, null);
    }

    /** A plain number with an optional unit ("Reach 6 blocks"). Read it with {@link #get}. */
    public Ranks amount(String name, String unit, double... perRank) {
        return add(name, Format.AMOUNT, unit, perRank, null);
    }

    /** A percentage, declared as e.g. 25 for 25%. {@link #get} returns 25; {@link #fraction} 0.25. */
    public Ranks percent(String name, double... perRank) {
        return add(name, Format.PERCENT, "", perRank, null);
    }

    /** Text shown as-is in the tooltip ("Tier II animals"); for anything the class works out itself. */
    public Ranks text(String name, String... perRank) {
        return add(name, Format.TEXT, "", new double[perRank.length], perRank);
    }

    /** A number of ranks with nothing to show, for classes that read {@link #rank} themselves. */
    public Ranks ranks(int count) {
        checkCount(count);
        return this;
    }

    /** Class level needed for ranks 2 and up, replacing the default 3, 6, 9. */
    public Ranks levels(int... forRank2Up) {
        int[] levels = new int[forRank2Up.length + 2];
        System.arraycopy(forRank2Up, 0, levels, 2, forRank2Up.length);
        this.levels = levels;
        return this;
    }

    private Ranks add(String name, Format format, String unit, double[] values, String[] texts) {
        checkCount(values.length);
        stats.add(new Stat(name, format, unit, values, texts));
        return this;
    }

    private void checkCount(int count) {
        if (count < 1 || (maxRank != 0 && count != maxRank)) {
            throw new IllegalArgumentException(skill + ": every rank list needs " + maxRank + " values, got " + count);
        }
        maxRank = count;
    }

    /** Checks the declaration against the tree. Called by {@link ClassTrees} at startup. */
    static void validate() {
        for (Ranks ranks : ALL.values()) {
            if (ClassTrees.node(ranks.skill) == null) {
                throw new IllegalStateException("Ranks declared for unknown skill " + ranks.skill);
            }
            if (ranks.maxRank > 1 && ranks.levels.length <= ranks.maxRank) {
                throw new IllegalStateException(ranks.skill + " has " + ranks.maxRank
                        + " ranks but no level for rank " + ranks.levels.length + "; set them with levels(...)");
            }
        }
    }

    // ---- Reading ----

    public String skill() {
        return skill;
    }

    public int maxRank() {
        return maxRank;
    }

    /** The player's rank in their current class, from 1. Works on both sides. */
    public int rank(PlayerEntity player) {
        return rank(Progression.current(player));
    }

    public int rank(ClassProgress progress) {
        return Math.min(progress.rank(skill), maxRank);
    }

    public double get(PlayerEntity player, String name) {
        return get(rank(player), name);
    }

    public double get(ClassProgress progress, String name) {
        return get(rank(progress), name);
    }

    public double get(int rank, String name) {
        return stat(name).values[Math.max(1, Math.min(rank, maxRank)) - 1];
    }

    public int getInt(PlayerEntity player, String name) {
        return (int) Math.round(get(player, name));
    }

    /** A {@link #seconds} value in ticks. */
    public int ticks(PlayerEntity player, String name) {
        return (int) Math.round(get(player, name) * 20);
    }

    public int ticks(ClassProgress progress, String name) {
        return (int) Math.round(get(progress, name) * 20);
    }

    /** A {@link #value} as a status effect amplifier (level I is amplifier 0). */
    public int amplifier(PlayerEntity player, String name) {
        return (int) get(player, name) - 1;
    }

    public int amplifier(ClassProgress progress, String name) {
        return (int) get(progress, name) - 1;
    }

    /** A {@link #percent} value as a fraction (25% is 0.25). */
    public double fraction(PlayerEntity player, String name) {
        return get(player, name) / 100.0;
    }

    public double fraction(ClassProgress progress, String name) {
        return get(progress, name) / 100.0;
    }

    private Stat stat(String name) {
        for (Stat stat : stats) {
            if (stat.name.equals(name)) {
                return stat;
            }
        }
        throw new IllegalArgumentException(skill + " has no rank value " + name);
    }

    // ---- Tooltip text ----

    /** The values at a rank, e.g. "Strength II, 12 s". Empty if there are none. */
    public String describe(int rank) {
        int index = Math.max(1, Math.min(rank, maxRank)) - 1;
        List<String> parts = new ArrayList<>();
        for (Stat stat : stats) {
            double value = stat.values[index];
            parts.add(switch (stat.format) {
                case LEVEL -> stat.name + " " + roman((int) value);
                case SECONDS -> (stat.name.equals("Duration") ? "" : stat.name + " ") + number(value) + " s";
                case AMOUNT -> stat.name + " " + number(value) + (stat.unit.isEmpty() ? "" : " " + stat.unit);
                case PERCENT -> stat.name + " " + number(value) + "%";
                case TEXT -> stat.texts[index];
            });
        }
        return String.join(", ", parts);
    }

    private static String number(double value) {
        return value == Math.rint(value) ? Integer.toString((int) value) : Double.toString(value);
    }

    private static final String[] ROMAN = { "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X" };

    public static String roman(int value) {
        return value >= 0 && value < ROMAN.length ? ROMAN[value] : Integer.toString(value);
    }
}
