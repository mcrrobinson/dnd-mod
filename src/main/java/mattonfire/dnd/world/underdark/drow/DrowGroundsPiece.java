package mattonfire.dnd.world.underdark.drow;

import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.classes.Registry.UnderdarkBlocks;
import mattonfire.dnd.world.underdark.UnderdarkPiece;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.structure.StructureContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The ground of a Drow Outpost: the cavern floor in a disc of radius {@link DrowOutpostPlanner#GROUNDS_RADIUS}
 * levelled ({@link UnderdarkPiece.Builder#ground}: up to {@value #FILL} blocks filled under the floor, air above it to
 * {@value #CLEAR} blocks, falling to {@value #RIM_CLEAR} at the rim, liquids round it sealed with deepslate), paths of
 * Polished Faerzress and deepslate tiles from the shrine to every door, Faerie Fire Lanterns on posts along them, a few
 * cobwebs and bluecaps, and 4-8 cave spiders (persistent).
 * <p>
 * Its box is the whole outpost, so the structure's {@code spawn_overrides} cover all of it.
 */
public class DrowGroundsPiece extends UnderdarkPiece {
    /** Blocks filled under the floor where the cave dips. */
    public static final int FILL = 4;
    /** Air cleared above the floor in the middle. */
    public static final int CLEAR = 7;
    /** ...and at the rim. */
    public static final int RIM_CLEAR = 3;
    /** Where the clearance starts falling off. */
    private static final int CLEAR_FALLS_FROM = 20;
    /** Paths start at the shrine's edge. */
    private static final int PATH_FROM = 6;

    private static final int R = DrowOutpostPlanner.GROUNDS_RADIUS;

    /** Entrances the paths lead to (a, b from the centre). */
    private final List<int[]> entrances;

    public DrowGroundsPiece(BlockPos center, long seed, List<BlockPos> entrances) {
        super(DrowOutposts.GROUNDS, center, Direction.SOUTH, seed, -R - 1, -FILL - 1, -R - 1, R + 1, CLEAR + 1, R + 1);
        this.entrances = new ArrayList<>();
        for (BlockPos pos : entrances) {
            this.entrances.add(new int[] {pos.getX() - center.getX(), pos.getZ() - center.getZ()});
        }
    }

    public DrowGroundsPiece(NbtCompound nbt) {
        super(DrowOutposts.GROUNDS, nbt);
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

    /** Air above the floor at {@code r} blocks from the centre. */
    static int clearAt(double r) {
        if (r <= CLEAR_FALLS_FROM) {
            return CLEAR;
        }
        return Math.max(RIM_CLEAR, CLEAR - (int) Math.ceil((r - CLEAR_FALLS_FROM) * (CLEAR - RIM_CLEAR)
                / (double) (R - CLEAR_FALLS_FROM)));
    }

    @Override
    protected void build(Builder b) {
        BlockState fill = Blocks.DEEPSLATE.getDefaultState();
        BlockState[] tops = {Blocks.DEEPSLATE_TILES.getDefaultState(), Blocks.COBBLED_DEEPSLATE.getDefaultState(),
                UnderdarkBlocks.FUNGAL_LOAM.getDefaultState()};
        for (int a = -R - 1; a <= R + 1; a++) {
            for (int c = -R - 1; c <= R + 1; c++) {
                double r = Math.sqrt(a * a + c * c);
                BlockState top = tops[b.random.nextInt(tops.length)];
                if (r <= R + 0.5D) {
                    b.ground(a, c, FILL, clearAt(r), fill, top);
                } else if (r <= R + 1.5D) {
                    // The rim: no liquid may stand next to the cleared ground.
                    for (int y = -FILL; y <= RIM_CLEAR + 1; y++) {
                        b.seal(a, y, c, fill);
                    }
                }
            }
        }
        for (int[] entrance : this.entrances) {
            this.path(b, entrance[0], entrance[1]);
        }
        // Webs and bluecaps here and there between the buildings (they build over these).
        for (int i = 0; i < 40; i++) {
            int a = b.random.nextInt(2 * R - 3) - R + 2;
            int c = b.random.nextInt(2 * R - 3) - R + 2;
            boolean web = b.random.nextFloat() < 0.4F;
            if (a * a + c * c > PATH_FROM * PATH_FROM && a * a + c * c < (R - 2) * (R - 2)) {
                BlockState there = b.get(a, 0, c);
                if (there != null && there.isAir()) {
                    b.set(a, 0, c, web ? Blocks.COBWEB.getDefaultState() : UnderdarkBlocks.BLUECAP.getDefaultState());
                }
            }
        }
        // Cave spiders, the drow's pets, prowling the grounds.
        int spiders = 4 + b.random.nextInt(5);
        for (int i = 0; i < spiders; i++) {
            double angle = b.random.nextDouble() * Math.PI * 2.0D;
            int r = 7 + b.random.nextInt(8);
            int a = (int) Math.round(Math.cos(angle) * r);
            int c = (int) Math.round(Math.sin(angle) * r);
            b.resident(a, 0, c, EntityType.CAVE_SPIDER, 10);
        }
    }

    /**
     * A path from the shrine's edge out to (ea, eb): Polished Faerzress and deepslate tiles, two wide, and a Faerie
     * Fire Lantern on a post beside it every 7 blocks.
     */
    private void path(Builder b, int ea, int eb) {
        double length = Math.sqrt(ea * ea + eb * eb);
        if (length < PATH_FROM + 1) {
            return;
        }
        double ux = ea / length;
        double uz = eb / length;
        int posts = 0;
        for (double t = PATH_FROM; t <= length; t += 0.5D) {
            int a = (int) Math.round(ux * t);
            int c = (int) Math.round(uz * t);
            for (int[] side : new int[][] {{0, 0}, {1, 0}, {0, 1}}) {
                boolean polished = Math.floorMod(a + c + side[0] + side[1], 3) == 0;
                b.set(a + side[0], -1, c + side[1], polished ? ModBlockFamilies.POLISHED_FAERZRESS.base().getDefaultState()
                        : Blocks.DEEPSLATE_TILES.getDefaultState());
                b.clear(a + side[0], 0, c + side[1]);
            }
            int along = (int) t;
            if (along >= PATH_FROM + 3 && along % 7 == 3 && along != posts && length - t > 3) {
                posts = along;
                // Beside the path, on its left.
                int pa = a + (int) Math.round(-uz * 2.5D);
                int pc = c + (int) Math.round(ux * 2.5D);
                b.set(pa, 0, pc, ModBlockFamilies.DROWSTONE_BRICKS.wall().getDefaultState());
                b.set(pa, 1, pc, ModBlockFamilies.DROWSTONE_BRICKS.wall().getDefaultState());
                b.lantern(pa, 2, pc, UnderdarkBlocks.FAERIE_LANTERN);
            }
        }
    }
}
