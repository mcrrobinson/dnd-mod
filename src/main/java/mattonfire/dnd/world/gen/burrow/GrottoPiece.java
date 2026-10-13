package mattonfire.dnd.world.gen.burrow;

import net.minecraft.block.BigDripleafBlock;
import net.minecraft.block.BigDripleafStemBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CaveVines;
import net.minecraft.block.GlowLichenBlock;
import net.minecraft.block.SmallDripleafBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The lush grotto beyond the east workshop: a cave of moss round a clay-bottomed pool, carved as a lumpy
 * ellipsoid {@link #RX} by {@link #RY} by {@link #RZ} centred {@link #CENTER} blocks east of the stair, reached by
 * a short tunnel from the workshop's far doorway. Its shell is always built whole (moss and rooted dirt under
 * foot, stone and moss over head), so no water or cave breaks in. Azaleas, moss carpet, grass and small dripleaf
 * on the floor, big dripleaf in the pool, glow berry vines and spore blossoms from the ceiling, hanging roots,
 * glow lichen. The gnomes' own lush cave: real lush caves are rare under forests.
 */
public class GrottoPiece extends CogsdeepPiece {
    public static final int CENTER = 37;
    public static final double RX = 8.5D;
    public static final double RY = 5.0D;
    public static final double RZ = 9.5D;
    /** The ellipsoid's middle height (the floor is near y -1). */
    static final int MID = 3;
    /** Shell thickness, in units of the normalised radius. */
    static final double SHELL = 1.4D;
    /** The tunnel from the workshop runs from here to the cave. */
    static final int TUNNEL_FROM = WorkshopPiece.FAR;

    public GrottoPiece(BlockPos center, Direction door, long seed, long burrow) {
        super(CogsdeepStructures.GROTTO, center, door, seed, burrow, TUNNEL_FROM, -4, -14, CENTER + 12, 11, 14);
    }

    public GrottoPiece(NbtCompound nbt) {
        super(CogsdeepStructures.GROTTO, nbt);
    }

    /** The middle of the pool, in the world. */
    public BlockPos pool() {
        return this.at(CENTER + 2, -1, 0);
    }

    /** Normalised distance from the middle, roughened with a little cheap noise so it's not a perfect egg. */
    private static double distance(int a, int y, int c) {
        double dx = (a - CENTER) / RX;
        double dy = (y - MID) / RY;
        double dz = c / RZ;
        double wobble = 0.08D * Math.sin(a * 0.9D + c * 0.4D) + 0.06D * Math.cos(c * 1.1D - y * 0.7D);
        return Math.sqrt(dx * dx + dy * dy + dz * dz) + wobble;
    }

    private static boolean inPool(int a, int c) {
        int da = a - (CENTER + 2);
        return da * da / 9.0D + c * c / 12.0D <= 1.0D;
    }

    @Override
    protected void build(Builder b) {
        int minA = TUNNEL_FROM + 1;
        int maxA = CENTER + 12;
        // Carve the cave and its shell. The floor is flattened at y -1, so the ellipsoid's lower cap is solid.
        for (int a = minA; a <= maxA; a++) {
            for (int c = -14; c <= 14; c++) {
                for (int y = -4; y <= 11; y++) {
                    double d = distance(a, y, c);
                    if (d > SHELL) {
                        continue;
                    }
                    if (d <= 1.0D && y >= 0) {
                        b.set(a, y, c, Blocks.AIR);
                    } else if (y <= 0) {
                        b.set(a, y, c, b.chance(0.15F) ? Blocks.ROOTED_DIRT : Blocks.MOSS_BLOCK);
                    } else {
                        float r = b.random.nextFloat();
                        b.set(a, y, c, r < 0.35F ? Blocks.MOSS_BLOCK : r < 0.5F ? Blocks.ROOTED_DIRT
                                : r < 0.6F ? Blocks.CLAY : Blocks.STONE);
                    }
                }
            }
        }
        // The tunnel from the workshop's doorway, three wide and four high, in a shell of masonry.
        for (int a = TUNNEL_FROM + 1; a <= TUNNEL_FROM + 4; a++) {
            for (int c = -2; c <= 2; c++) {
                for (int y = -1; y <= 4; y++) {
                    boolean shell = Math.abs(c) == 2 || y == -1 || y == 4;
                    if (shell && distance(a, y, c) > 1.0D) {
                        b.set(a, y, c, y == -1 ? Blocks.MOSS_BLOCK : masonry(b));
                    } else if (!shell) {
                        b.set(a, y, c, Blocks.AIR);
                    }
                }
            }
        }

        // The floor and ceiling dressing, column by column.
        for (int a = minA; a <= maxA; a++) {
            for (int c = -13; c <= 13; c++) {
                if (distance(a, 0, c) > 1.0D) {
                    continue;
                }
                if (inPool(a, c)) {
                    b.set(a, -2, c, Blocks.CLAY);
                    b.set(a, -1, c, Blocks.WATER);
                    if (b.chance(0.3F)) {
                        // Big dripleaf: a stem standing in the water on the clay, the leaf just above it.
                        b.set(a, -1, c, Blocks.BIG_DRIPLEAF_STEM.getDefaultState().with(BigDripleafStemBlock.FACING, Direction.NORTH)
                                .with(net.minecraft.state.property.Properties.WATERLOGGED, true));
                        b.set(a, 0, c, Blocks.BIG_DRIPLEAF.getDefaultState().with(BigDripleafBlock.FACING, Direction.NORTH));
                    }
                } else {
                    b.set(a, -1, c, Blocks.MOSS_BLOCK);
                    float r = b.random.nextFloat();
                    BlockState plant = r < 0.25F ? Blocks.MOSS_CARPET.getDefaultState()
                            : r < 0.33F ? Blocks.AZALEA.getDefaultState()
                            : r < 0.38F ? Blocks.FLOWERING_AZALEA.getDefaultState()
                            : r < 0.5F ? Blocks.GRASS.getDefaultState() : null;
                    if (plant != null) {
                        b.set(a, 0, c, plant);
                    } else if (r < 0.55F) {
                        b.set(a, 0, c, Blocks.SMALL_DRIPLEAF.getDefaultState().with(SmallDripleafBlock.HALF, DoubleBlockHalf.LOWER)
                                .with(SmallDripleafBlock.FACING, Direction.EAST));
                        b.set(a, 1, c, Blocks.SMALL_DRIPLEAF.getDefaultState().with(SmallDripleafBlock.HALF, DoubleBlockHalf.UPPER)
                                .with(SmallDripleafBlock.FACING, Direction.EAST));
                    }
                }
                // Hang things from the ceiling: the first solid block above the cave's air in this column.
                int ceiling = -1;
                for (int y = 1; y <= 10; y++) {
                    if (distance(a, y, c) > 1.0D) {
                        ceiling = y;
                        break;
                    }
                }
                if (ceiling < 3) {
                    continue;
                }
                float r = b.random.nextFloat();
                if (r < 0.14F) {
                    int length = 1 + b.random.nextInt(Math.min(3, ceiling - 2));
                    for (int i = 1; i <= length; i++) {
                        boolean end = i == length;
                        b.set(a, ceiling - i, c, (end ? Blocks.CAVE_VINES : Blocks.CAVE_VINES_PLANT).getDefaultState()
                                .with(CaveVines.BERRIES, b.chance(0.6F)));
                    }
                } else if (r < 0.18F) {
                    b.set(a, ceiling - 1, c, Blocks.SPORE_BLOSSOM);
                } else if (r < 0.24F) {
                    b.set(a, ceiling, c, Blocks.ROOTED_DIRT);
                    b.set(a, ceiling - 1, c, Blocks.HANGING_ROOTS);
                } else if (r < 0.32F) {
                    b.set(a, ceiling - 1, c, Blocks.GLOW_LICHEN.getDefaultState().with(GlowLichenBlock.getProperty(Direction.UP), true));
                }
            }
        }
        // A rooted azalea tree stump: a few azalea leaves round a log by the pool.
        int ta = CENTER - 3;
        int tc = -4;
        b.set(ta, 0, tc, Blocks.OAK_LOG);
        b.set(ta, 1, tc, Blocks.OAK_LOG);
        for (Direction d : Direction.Type.HORIZONTAL) {
            b.set(ta + d.getOffsetX(), 2, tc + d.getOffsetZ(), Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState()
                    .with(net.minecraft.block.LeavesBlock.PERSISTENT, true));
        }
        b.set(ta, 2, tc, Blocks.AZALEA_LEAVES.getDefaultState().with(net.minecraft.block.LeavesBlock.PERSISTENT, true));
        b.set(ta, -1, tc, Blocks.ROOTED_DIRT);

        b.sentinel(CENTER - 4, 0, 3, 10);
    }
}
