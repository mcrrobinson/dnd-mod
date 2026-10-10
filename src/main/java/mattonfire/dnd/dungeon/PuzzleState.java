package mattonfire.dnd.dungeon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import mattonfire.dnd.classes.Blocks.RuneBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

/**
 * A rune-pillar puzzle room's answer and progress, kept in the room's ward (written at worldgen from the
 * piece seed). Four pillars each need one glyph ({@link RuneBlock.Glyph}); at Challenge I one of them is
 * {@link #FREE} and takes any glyph. One set pillar's mural is defaced: the lectern's riddle names it.
 * The answer never changes; the pillars are shuffled after a wrong answer and on a dungeon reset.
 */
public class PuzzleState {
    public static final int PILLARS = 4;
    /** A solution slot any glyph satisfies. */
    public static final int FREE = -1;

    private final int[] solution;
    private final int defaced;
    private final List<BlockPos> pillars;
    private final List<BlockPos> seal;
    private final List<BlockPos> vents;
    @Nullable
    private final BlockPos lectern;
    private boolean solved;
    /** {@link DungeonState#resets()} this state was last brought up to date with. */
    private int resetsSeen;
    private int wrongAnswers;
    /** Players who have studied the murals since the last reset (one check each). */
    private final Set<UUID> studied = new HashSet<>();
    /** Not saved: world time each pillar was last turned (0 = not since the last attempt). */
    private final long[] touched = new long[PILLARS];
    /** Not saved: the vents burn until this world time after a wrong answer. */
    long burnUntil;

    public PuzzleState(int[] solution, int defaced, List<BlockPos> pillars, List<BlockPos> seal, List<BlockPos> vents,
                       @Nullable BlockPos lectern) {
        this.solution = solution.clone();
        this.defaced = defaced;
        this.pillars = List.copyOf(pillars);
        this.seal = List.copyOf(seal);
        this.vents = List.copyOf(vents);
        this.lectern = lectern;
    }

    /**
     * A fresh answer: four different glyphs, one pillar free at Challenge I, and one of the set pillars'
     * murals defaced.
     */
    public static int[] rollSolution(Random random, int tier) {
        List<Integer> glyphs = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5));
        int[] solution = new int[PILLARS];
        for (int i = 0; i < PILLARS; i++) {
            solution[i] = glyphs.remove(random.nextInt(glyphs.size()));
        }
        if (tier <= 1) {
            solution[random.nextInt(PILLARS)] = FREE;
        }
        return solution;
    }

    /** One of the pillars that has a glyph set, at random. */
    public static int rollDefaced(Random random, int[] solution) {
        List<Integer> set = new ArrayList<>();
        for (int i = 0; i < solution.length; i++) {
            if (solution[i] != FREE) {
                set.add(i);
            }
        }
        return set.get(random.nextInt(set.size()));
    }

    /** Glyphs to start the pillars on (or shuffle them to): random, but never already the answer. */
    public int[] shuffled(Random random) {
        int[] glyphs = new int[PILLARS];
        do {
            for (int i = 0; i < PILLARS; i++) {
                glyphs[i] = random.nextInt(6);
            }
        } while (this.matches(glyphs));
        return glyphs;
    }

    public boolean matches(int[] glyphs) {
        for (int i = 0; i < PILLARS; i++) {
            if (this.solution[i] != FREE && glyphs[i] != this.solution[i]) {
                return false;
            }
        }
        return true;
    }

    public int[] solution() {
        return this.solution.clone();
    }

    public int defaced() {
        return this.defaced;
    }

    public RuneBlock.Glyph defacedGlyph() {
        return RuneBlock.Glyph.of(this.solution[this.defaced]);
    }

    public List<BlockPos> pillars() {
        return this.pillars;
    }

    /** Index of the pillar whose rune is at {@code pos}, or -1. */
    public int pillarAt(BlockPos pos) {
        return this.pillars.indexOf(pos);
    }

    public List<BlockPos> seal() {
        return this.seal;
    }

    public List<BlockPos> vents() {
        return this.vents;
    }

    @Nullable
    public BlockPos lectern() {
        return this.lectern;
    }

    public boolean solved() {
        return this.solved;
    }

    void setSolved(boolean solved) {
        this.solved = solved;
    }

    public int resetsSeen() {
        return this.resetsSeen;
    }

    void setResetsSeen(int resets) {
        this.resetsSeen = resets;
    }

    public int wrongAnswers() {
        return this.wrongAnswers;
    }

    void addWrongAnswer() {
        this.wrongAnswers++;
    }

    /** True the first time a player studies the murals since the last reset. */
    boolean study(UUID player) {
        return this.studied.add(player);
    }

    void clearStudied() {
        this.studied.clear();
    }

    void touch(int pillar, long now) {
        this.touched[pillar] = now;
    }

    void clearTouches() {
        Arrays.fill(this.touched, 0L);
    }

    /** Latest turn of any pillar, or 0. */
    long lastTouch() {
        long last = 0L;
        for (long t : this.touched) {
            last = Math.max(last, t);
        }
        return last;
    }

    /** True when every pillar has been turned within {@code window} ticks of the latest turn. */
    boolean allTouchedWithin(long window) {
        long last = this.lastTouch();
        for (long t : this.touched) {
            if (t == 0L || last - t > window) {
                return false;
            }
        }
        return true;
    }

    /** "sun, moon, (any), skull" for /dungeon info. */
    public static String describe(int[] glyphs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < glyphs.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(glyphs[i] == FREE ? "(any)" : RuneBlock.Glyph.of(glyphs[i]).id());
        }
        return sb.toString();
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putIntArray("Solution", this.solution);
        nbt.putInt("Defaced", this.defaced);
        nbt.put("Pillars", positions(this.pillars));
        nbt.put("Seal", positions(this.seal));
        nbt.put("Vents", positions(this.vents));
        if (this.lectern != null) {
            nbt.put("Lectern", NbtHelper.fromBlockPos(this.lectern));
        }
        nbt.putBoolean("Solved", this.solved);
        nbt.putInt("ResetsSeen", this.resetsSeen);
        nbt.putInt("WrongAnswers", this.wrongAnswers);
        NbtList studied = new NbtList();
        for (UUID id : this.studied) {
            studied.add(NbtHelper.fromUuid(id));
        }
        nbt.put("Studied", studied);
        return nbt;
    }

    public static PuzzleState fromNbt(NbtCompound nbt) {
        int[] solution = nbt.getIntArray("Solution");
        if (solution.length != PILLARS) {
            solution = new int[]{0, 1, 2, 3};
        }
        PuzzleState state = new PuzzleState(solution, nbt.getInt("Defaced"), positions(nbt.getList("Pillars", NbtElement.COMPOUND_TYPE)),
                positions(nbt.getList("Seal", NbtElement.COMPOUND_TYPE)), positions(nbt.getList("Vents", NbtElement.COMPOUND_TYPE)),
                nbt.contains("Lectern") ? NbtHelper.toBlockPos(nbt.getCompound("Lectern")) : null);
        state.solved = nbt.getBoolean("Solved");
        state.resetsSeen = nbt.getInt("ResetsSeen");
        state.wrongAnswers = nbt.getInt("WrongAnswers");
        NbtList studied = nbt.getList("Studied", NbtElement.INT_ARRAY_TYPE);
        for (int i = 0; i < studied.size(); i++) {
            state.studied.add(NbtHelper.toUuid(studied.get(i)));
        }
        return state;
    }

    static NbtList positions(List<BlockPos> list) {
        NbtList nbt = new NbtList();
        for (BlockPos pos : list) {
            nbt.add(NbtHelper.fromBlockPos(pos));
        }
        return nbt;
    }

    static List<BlockPos> positions(NbtList nbt) {
        List<BlockPos> list = new ArrayList<>();
        for (int i = 0; i < nbt.size(); i++) {
            list.add(NbtHelper.toBlockPos(nbt.getCompound(i)));
        }
        return list;
    }
}
