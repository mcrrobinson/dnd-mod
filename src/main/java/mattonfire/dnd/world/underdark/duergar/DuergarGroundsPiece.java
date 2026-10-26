package mattonfire.dnd.world.underdark.duergar;

import static mattonfire.dnd.world.underdark.duergar.DuergarHoldPlanner.BACK;
import static mattonfire.dnd.world.underdark.duergar.DuergarHoldPlanner.FRONT;
import static mattonfire.dnd.world.underdark.duergar.DuergarHoldPlanner.HALF;
import static mattonfire.dnd.world.underdark.duergar.DuergarHoldPlanner.INNER;
import static mattonfire.dnd.world.underdark.duergar.DuergarHoldPlanner.LAKE_A1;
import static mattonfire.dnd.world.underdark.duergar.DuergarHoldPlanner.LAKE_A2;
import static mattonfire.dnd.world.underdark.duergar.DuergarHoldPlanner.LAKE_B1;
import static mattonfire.dnd.world.underdark.duergar.DuergarHoldPlanner.LAKE_B2;

import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.entity.DuergarEntities;
import mattonfire.dnd.world.underdark.UnderdarkPiece;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.structure.StructureContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The ground of a Duergar Hold, in the hold's own coordinates (gate towards +b):
 * <ul>
 * <li>the cavern floor in a square of half-size {@link DuergarHoldPlanner#HALF} levelled
 * ({@link UnderdarkPiece.Builder#ground}: up to {@value #FILL} blocks filled with tuff where it dips, air above to
 * {@value #CLEAR} blocks, falling to {@value #RIM_CLEAR} at the rim, liquids round it sealed);</li>
 * <li>the yard inside the wall paved with cobbled deepslate, deepslate tiles and tuff, a road of smooth basalt and
 * polished deepslate from the gate to the lake, and paths from it to every door;</li>
 * <li>the <b>lava lake</b> at the back: a basin {@value #LAKE_W} x {@value #LAKE_D} of lava two deep, sunk a block
 * under the floor, walled and floored with basalt to {@code y = -5}, with a shore of magma blocks round it and a
 * basalt-brick railing along the yard side;</li>
 * <li>Duergar Braziers on posts along the road and at the lake's corners, and 1-2 soldiers on guard in the yard;</li>
 * <li>the cave's springs stopped over and round the hold ({@link #springs}).</li>
 * </ul>
 * Its box is the whole hold, so the structure's {@code spawn_overrides} cover all of it.
 */
public class DuergarGroundsPiece extends UnderdarkPiece {
    /** Blocks filled under the floor where the cave dips. */
    public static final int FILL = 4;
    /** Air cleared above the floor... */
    public static final int CLEAR = 10;
    /** ...falling to this at the rim. */
    public static final int RIM_CLEAR = 4;
    private static final int CLEAR_FALLS_FROM = 26;
    static final int LAKE_W = LAKE_A2 - LAKE_A1 + 1;
    static final int LAKE_D = LAKE_B2 - LAKE_B1 + 1;
    /** The lava's surface, a block under the floor. */
    public static final int LAVA_TOP = -2;
    /** Springs are stopped up to this high over the floor... */
    public static final int SPRINGS = 40;
    /** ...and this far out round the square. */
    public static final int SPRING_RIM = 4;
    /** Where the braziers stand along the road (b), at a = +-3. */
    static final int[] BRAZIERS = {20, 12, 4, -4, -12};

    /** Entrances the paths lead to (hold-local a, b). */
    private final List<int[]> entrances;

    public DuergarGroundsPiece(BlockPos center, Direction front, long seed, List<int[]> entrances) {
        super(DuergarHolds.GROUNDS, center, front, seed, -HALF - SPRING_RIM, -6, -HALF - SPRING_RIM, HALF + SPRING_RIM, SPRINGS,
                HALF + SPRING_RIM);
        this.entrances = new ArrayList<>(entrances);
    }

    public DuergarGroundsPiece(NbtCompound nbt) {
        super(DuergarHolds.GROUNDS, nbt);
        this.entrances = new ArrayList<>();
        int[] flat = nbt.getIntArray("Entrances");
        for (int i = 0; i + 1 < flat.length; i += 2) {
            this.entrances.add(new int[] {flat[i], flat[i + 1]});
        }
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        super.writeNbt(context, nbt);
        int[] flat = new int[this.entrances.size() * 2];
        for (int i = 0; i < this.entrances.size(); i++) {
            flat[i * 2] = this.entrances.get(i)[0];
            flat[i * 2 + 1] = this.entrances.get(i)[1];
        }
        nbt.put("Entrances", new NbtIntArray(flat));
    }

    /** Air above the floor at square distance {@code d} from the centre. */
    static int clearAt(int d) {
        return d <= CLEAR_FALLS_FROM ? CLEAR : Math.max(RIM_CLEAR, CLEAR - (d - CLEAR_FALLS_FROM) * 2);
    }

    /** Whether hold-local a/b is in the lava (inside the shore). */
    public static boolean inLake(int a, int b) {
        return a >= LAKE_A1 && a <= LAKE_A2 && b >= LAKE_B1 && b <= LAKE_B2;
    }

    /** Whether hold-local a/b is on the lake's magma shore. */
    public static boolean onShore(int a, int b) {
        return !inLake(a, b) && a >= LAKE_A1 - 1 && a <= LAKE_A2 + 1 && b >= LAKE_B1 - 1 && b <= LAKE_B2 + 1;
    }

    /** A lava block's world position: hold-local a/b at the lava's surface. */
    public BlockPos lava(int a, int b) {
        return this.at(a, LAVA_TOP, b);
    }

    @Override
    protected void build(Builder b) {
        BlockState fill = Blocks.TUFF.getDefaultState();
        BlockState[] tops = {Blocks.TUFF.getDefaultState(), Blocks.SMOOTH_BASALT.getDefaultState(),
                Blocks.COBBLED_DEEPSLATE.getDefaultState()};
        BlockState[] yard = {Blocks.COBBLED_DEEPSLATE.getDefaultState(), Blocks.COBBLED_DEEPSLATE.getDefaultState(),
                Blocks.DEEPSLATE_TILES.getDefaultState(), Blocks.TUFF.getDefaultState()};
        for (int a = -HALF - 1; a <= HALF + 1; a++) {
            for (int c = -HALF - 1; c <= HALF + 1; c++) {
                int d = Math.max(Math.abs(a), Math.abs(c));
                BlockState top = tops[b.random.nextInt(tops.length)];
                BlockState paving = yard[b.random.nextInt(yard.length)];
                if (d <= HALF) {
                    b.ground(a, c, FILL, clearAt(d), fill, top);
                    if (Math.abs(a) <= INNER && c >= BACK && c <= FRONT) {
                        b.set(a, -1, c, paving);
                    }
                } else {
                    // The rim: no liquid may stand next to the cleared ground.
                    for (int y = -FILL; y <= RIM_CLEAR + 1; y++) {
                        b.seal(a, y, c, fill);
                    }
                }
            }
        }
        this.springs(b);
        this.lake(b);
        // The road from the gate to the lake, and the paths off it.
        for (int c = BACK; c <= FRONT; c++) {
            for (int a = -1; a <= 1; a++) {
                b.set(a, -1, c, road(a, c));
            }
        }
        for (int[] entrance : this.entrances) {
            int ea = entrance[0];
            int ec = entrance[1];
            int step = ea > 0 ? 1 : -1;
            for (int a = 2 * step; a != ea + step; a += step) {
                for (int c = ec - 1; c <= ec + 1; c++) {
                    b.set(a, -1, c, road(a, c));
                    b.clear(a, 0, c);
                }
            }
        }
        // Braziers on posts along the road and at the lake's front corners.
        for (int c : BRAZIERS) {
            this.brazier(b, -3, c);
            this.brazier(b, 3, c);
        }
        this.brazier(b, LAKE_A1 - 1, LAKE_B2 + 2);
        this.brazier(b, LAKE_A2 + 1, LAKE_B2 + 2);
        // The yard guards.
        DuergarHolds.duergar(b, 4, 0, 18, DuergarEntities.DUERGAR_SOLDIER, 14);
        if (b.random.nextBoolean()) {
            DuergarHolds.duergar(b, -4, 0, -10, DuergarEntities.DUERGAR_SOLDIER, 14);
        }
    }

    /**
     * Stops the springs in the cave's walls and roof over the hold: any lava above the cleared air, up to
     * {@value #SPRINGS} over the floor and {@value #SPRING_RIM} blocks round the square, turns to basalt, any water to
     * cobbled deepslate, so nothing pours into the hold from the cavern's walls or roof.
     */
    private void springs(Builder b) {
        BlockState basalt = Blocks.BASALT.getDefaultState();
        BlockState stone = Blocks.COBBLED_DEEPSLATE.getDefaultState();
        for (int a = -HALF - SPRING_RIM; a <= HALF + SPRING_RIM; a++) {
            for (int c = -HALF - SPRING_RIM; c <= HALF + SPRING_RIM; c++) {
                int d = Math.max(Math.abs(a), Math.abs(c));
                for (int y = d <= HALF ? clearAt(d) + 1 : 0; y <= SPRINGS; y++) {
                    BlockState state = b.get(a, y, c);
                    if (state != null && !state.getFluidState().isEmpty()) {
                        b.set(a, y, c, state.getFluidState().isIn(net.minecraft.registry.tag.FluidTags.LAVA) ? basalt : stone);
                    }
                }
            }
        }
    }

    private static BlockState road(int a, int c) {
        return Math.floorMod(a + c, 3) == 0 ? Blocks.POLISHED_DEEPSLATE.getDefaultState()
                : Blocks.SMOOTH_BASALT.getDefaultState();
    }

    /** A Duergar Brazier on a basalt-brick post. */
    private void brazier(Builder b, int a, int c) {
        b.set(a, 0, c, DuergarHolds.bricks());
        b.set(a, 1, c, DuergarHolds.brazier());
    }

    /**
     * The lava lake: a basin of lava two deep ({@code y = -3} to {@value #LAVA_TOP}) with air over it to the floor,
     * floored with basalt two deep; the shore round it is basalt to {@code y = -5} topped with magma blocks; a railing
     * of basalt-brick wall along the yard side. Everything round the lava is solid, so it has nowhere to flow.
     */
    private void lake(Builder b) {
        BlockState basalt = Blocks.BASALT.getDefaultState();
        BlockState lava = Blocks.LAVA.getDefaultState();
        BlockState magma = Blocks.MAGMA_BLOCK.getDefaultState();
        for (int a = LAKE_A1 - 1; a <= LAKE_A2 + 1; a++) {
            for (int c = LAKE_B1 - 1; c <= LAKE_B2 + 1; c++) {
                if (inLake(a, c)) {
                    b.set(a, -5, c, basalt);
                    b.set(a, -4, c, basalt);
                    b.set(a, -3, c, lava);
                    b.set(a, -2, c, lava);
                    b.clear(a, -1, c);
                } else {
                    for (int y = -5; y <= -2; y++) {
                        b.set(a, y, c, basalt);
                    }
                    b.set(a, -1, c, magma);
                }
            }
        }
        for (int a = LAKE_A1 - 1; a <= LAKE_A2 + 1; a++) {
            b.set(a, 0, LAKE_B2 + 1, ModBlockFamilies.BASALT_FORGE_BRICKS.wall().getDefaultState());
        }
    }
}
