package mattonfire.dnd.world.gen.sanctum;

import static mattonfire.dnd.world.gen.PieceBlocks.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mattonfire.dnd.entity.ModEntityTypes;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The sanctum's platform, wall and gate, and the Exiles who keep it:
 * <ul>
 * <li>A disc of radius {@link BrimstoneSanctumStructure#PLATFORM}, two blocks thick (blackstone over basalt),
 * with everything up to {@link #CLEAR} blocks above it cut away. Gaps of up to {@link #FILL_GAP} blocks under
 * it are filled solid; under that it stands on basalt pillars every {@link #PILLAR_SPACING} blocks, down to
 * the Nether floor.</li>
 * <li>A polished blackstone wall {@link #WALL_HEIGHT} high round radius {@link BrimstoneSanctumStructure#WALL},
 * with chiselled posts carrying soul lanterns, and a three-wide gate under an iron-bar portcullis.</li>
 * <li>A polished road from the gate to the Pact Hall, two soul-fire braziers by it, and 4-6 Tiefling Exiles.</li>
 * </ul>
 */
public class PlatformPiece extends BrimstonePiece {
    /** Height cut clear above the platform. */
    static final int CLEAR = 15;
    static final int WALL_HEIGHT = 5;
    static final int PILLAR_SPACING = 6;
    /** Gaps under the platform this deep or less are filled solid. */
    static final int FILL_GAP = 3;
    private static final int DEPTH = BrimstoneSanctumStructure.MAX_DROP + BrimstoneSanctumStructure.MAX_BUMP + 2;
    /** Exiles keep within this distance of the middle. */
    public static final int EXILE_ROAM = 15;
    private static final int[][] EXILE_SPOTS = {{-3, 2}, {3, 2}, {-4, 12}, {4, 12}, {0, 9}, {6, -1}, {-3, -1}, {5, 8}};

    public PlatformPiece(BlockPos center, Direction gate, long seed, long sanctum) {
        super(BrimstoneSanctumStructures.PLATFORM, center, gate, seed, sanctum, -BrimstoneSanctumStructure.PLATFORM, -DEPTH,
                -BrimstoneSanctumStructure.PLATFORM, BrimstoneSanctumStructure.PLATFORM, CLEAR, BrimstoneSanctumStructure.PLATFORM);
    }

    public PlatformPiece(NbtCompound nbt) {
        super(BrimstoneSanctumStructures.PLATFORM, nbt);
    }

    /** The road from the gate to the Pact Hall door. */
    static boolean onRoad(int a, int b) {
        return Math.abs(a) <= 1 && b >= -2;
    }

    static boolean inGate(int a, int b) {
        return Math.abs(a) <= 1 && b > 0;
    }

    @Override
    protected void build(Builder b) {
        int r = BrimstoneSanctumStructure.PLATFORM;
        int wall = BrimstoneSanctumStructure.WALL;
        BlockState basalt = pillar(Blocks.BASALT, Direction.Axis.Y);
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                float roll = b.random.nextFloat();
                double d = Math.sqrt(x * x + z * z);
                if (d > r + 0.5D) {
                    continue;
                }
                b.air(x, 1, z, x, CLEAR, z);
                b.set(x, 0, z, this.floor(d, roll, onRoad(x, z)));
                b.set(x, -1, z, basalt);
                boolean grid = Math.floorMod(x, PILLAR_SPACING) == 0 && Math.floorMod(z, PILLAR_SPACING) == 0;
                int gap = b.inChunkGap(x, -1, z, DEPTH);
                if (gap >= 0 && gap <= FILL_GAP || grid) {
                    b.foundation(x, -1, z, grid ? basalt : Blocks.BLACKSTONE.getDefaultState(), DEPTH);
                }
                if (d > wall - 0.5D && d <= wall + 0.5D && !inGate(x, z)) {
                    for (int y = 1; y < WALL_HEIGHT; y++) {
                        b.set(x, y, z, masonry(b));
                    }
                    b.set(x, WALL_HEIGHT, z, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
                }
            }
        }

        // Posts with soul lanterns round the wall, every 45 degrees but the gate's.
        for (int k = 0; k < 8; k++) {
            if (k == 2) {
                continue;
            }
            double angle = k * Math.PI / 4.0D;
            int x = (int) Math.round(Math.cos(angle) * wall);
            int z = (int) Math.round(Math.sin(angle) * wall);
            b.fill(x, 1, z, x, WALL_HEIGHT, z, Blocks.CHISELED_POLISHED_BLACKSTONE);
            b.set(x, WALL_HEIGHT + 1, z, soulLantern(false));
        }

        // The gate: posts either side, an iron-bar portcullis raised under the lintel.
        int g = wall;
        for (int side = -1; side <= 1; side += 2) {
            b.fill(side * 2, 1, g, side * 2, WALL_HEIGHT + 1, g, Blocks.CHISELED_POLISHED_BLACKSTONE);
            b.set(side * 2, WALL_HEIGHT + 2, g, soulLantern(false));
        }
        b.fill(-1, 4, g, 1, 4, g, Blocks.IRON_BARS);
        b.fill(-1, WALL_HEIGHT, g, 1, WALL_HEIGHT, g, Blocks.POLISHED_BLACKSTONE_BRICKS);
        b.set(0, WALL_HEIGHT + 1, g, Blocks.GILDED_BLACKSTONE);
        b.set(-1, 3, g, Blocks.IRON_BARS);
        b.set(1, 3, g, Blocks.IRON_BARS);

        // Soul-fire braziers either side of the road.
        for (int side = -1; side <= 1; side += 2) {
            int x = side * 3;
            b.set(x, 1, 8, Blocks.POLISHED_BLACKSTONE);
            b.set(x, 2, 8, Blocks.SOUL_CAMPFIRE.getDefaultState().with(CampfireBlock.FACING, Direction.SOUTH));
        }

        // The Exiles.
        List<int[]> spots = new ArrayList<>(List.of(EXILE_SPOTS));
        Collections.shuffle(spots, new java.util.Random(b.random.nextLong()));
        int exiles = 4 + b.random.nextInt(3);
        for (int i = 0; i < exiles; i++) {
            b.tiefling(spots.get(i)[0], 1, spots.get(i)[1], ModEntityTypes.TIEFLING_EXILE, EXILE_ROAM, npc -> npc.setHome(this.at(0, 1, 0), EXILE_ROAM));
        }
    }

    /** The platform's top block {@code d} blocks from the middle. */
    private BlockState floor(double d, float roll, boolean road) {
        if (road) {
            return (roll < 0.85F ? Blocks.POLISHED_BLACKSTONE : Blocks.POLISHED_BLACKSTONE_BRICKS).getDefaultState();
        }
        if (d > BrimstoneSanctumStructure.WALL + 0.5D) {
            return pillar(Blocks.POLISHED_BASALT, Direction.Axis.Y);
        }
        return (roll < 0.55F ? Blocks.BLACKSTONE : roll < 0.75F ? Blocks.POLISHED_BLACKSTONE : roll < 0.9F ? Blocks.SMOOTH_BASALT
                : roll < 0.97F ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS : Blocks.GILDED_BLACKSTONE).getDefaultState();
    }
}
