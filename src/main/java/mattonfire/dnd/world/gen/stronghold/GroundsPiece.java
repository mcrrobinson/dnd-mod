package mattonfire.dnd.world.gen.stronghold;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mattonfire.dnd.entity.ModEntityTypes;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The hold's grounds: the clearing levelled out to radius {@link OrcStrongholdStructure#CLEAR} (cut away
 * {@link #HEADROOM} blocks up, filled with terracotta beneath), a trampled floor of coarse dirt, red sand and
 * packed mud, the palisade (dark oak and stripped dark oak logs 4 high, topped with pointed dripstone stakes,
 * patched here and there with red terracotta and blackstone), 2-4 totem poles and bone heaps, and the 6-9
 * Orc Warriors who roam inside the wall.
 */
public class GroundsPiece extends StrongholdPiece {
    static final int HEADROOM = 32;
    private static final int MAX_FOUNDATION = 16;
    private static final int WALL_HEIGHT = 4;
    /** Warriors keep within this distance of the middle of the hold. */
    public static final int WARRIOR_ROAM = 18;
    /** Spots for totems and bone heaps, between the buildings. */
    private static final int[][] TOTEM_SPOTS = {{-17, 7}, {17, 7}, {-8, -5}, {8, -5}, {-6, 18}, {6, 18}};
    /** Where warriors stand when the hold generates. */
    private static final int[][] WARRIOR_SPOTS = {{-5, -6}, {5, -6}, {-4, 15}, {4, 15}, {-9, 5}, {8, 7},
            {-8, -3}, {0, 17}, {-15, 8}, {15, 8}};

    public GroundsPiece(BlockPos center, Direction gate, long seed, long hold) {
        super(OrcStrongholdStructures.GROUNDS, center, gate, seed, hold, -OrcStrongholdStructure.CLEAR, 0,
                -OrcStrongholdStructure.CLEAR, OrcStrongholdStructure.CLEAR, HEADROOM, OrcStrongholdStructure.CLEAR);
    }

    public GroundsPiece(NbtCompound nbt) {
        super(OrcStrongholdStructures.GROUNDS, nbt);
    }

    /** The three-wide road from the middle out of the gate. */
    static boolean onRoad(int a, int b) {
        return Math.abs(a) <= 1 && b > OrcStrongholdStructure.PIT_B + 8;
    }

    /** Whether a/b is under one of the gate towers (3x3 at a = +-4 on the wall). */
    static boolean underTower(int a, int b) {
        return Math.abs(b - OrcStrongholdStructure.PALISADE) <= 1 && (Math.abs(a - 4) <= 1 || Math.abs(a + 4) <= 1);
    }

    @Override
    protected void build(Builder b) {
        int clear = OrcStrongholdStructure.CLEAR;
        int wall = OrcStrongholdStructure.PALISADE;
        for (int x = -clear; x <= clear; x++) {
            for (int z = -clear; z <= clear; z++) {
                float r = b.random.nextFloat();
                float r2 = b.random.nextFloat();
                double d = Math.sqrt(x * x + z * z);
                if (d > clear + 0.5D) {
                    continue;
                }
                b.air(x, 1, z, x, HEADROOM, z);
                b.foundation(x, 0, z, Blocks.TERRACOTTA.getDefaultState(), MAX_FOUNDATION);
                b.set(x, 0, z, floor(d, r, onRoad(x, z)));
                boolean gateGap = Math.abs(x) <= 2 && z > 0;
                if (d > wall - 0.5D && d <= wall + 0.5D && !gateGap && !underTower(x, z)) {
                    this.palisade(b, x, z, r, r2);
                }
            }
        }

        // Totem poles and bone heaps.
        List<int[]> spots = new ArrayList<>(List.of(TOTEM_SPOTS));
        Collections.shuffle(spots, new java.util.Random(b.random.nextLong()));
        int totems = 2 + b.random.nextInt(3);
        for (int i = 0; i < totems; i++) {
            int[] spot = spots.get(i);
            if (b.random.nextBoolean()) {
                this.totem(b, spot[0], spot[1]);
            } else {
                this.boneHeap(b, spot[0], spot[1]);
            }
        }

        // The warriors.
        List<int[]> stands = new ArrayList<>(List.of(WARRIOR_SPOTS));
        Collections.shuffle(stands, new java.util.Random(b.random.nextLong()));
        int warriors = 6 + b.random.nextInt(4);
        BlockPos home = this.at(0, 1, 0);
        for (int i = 0; i < warriors; i++) {
            b.orc(stands.get(i)[0], 1, stands.get(i)[1], ModEntityTypes.ORC_WARRIOR, home, WARRIOR_ROAM);
        }
    }

    private void palisade(Builder b, int x, int z, float r, float r2) {
        Block log = r2 < 0.08F ? Blocks.RED_TERRACOTTA : r2 < 0.14F ? Blocks.BLACKSTONE
                : r2 < 0.55F ? Blocks.DARK_OAK_LOG : Blocks.STRIPPED_DARK_OAK_LOG;
        for (int y = 1; y <= WALL_HEIGHT; y++) {
            b.set(x, y, z, log == Blocks.DARK_OAK_LOG || log == Blocks.STRIPPED_DARK_OAK_LOG
                    ? log(log, Direction.Axis.Y) : log.getDefaultState());
        }
        if (r < 0.7F) {
            b.set(x, WALL_HEIGHT + 1, z, stake());
        } else if (r < 0.75F) {
            b.set(x, WALL_HEIGHT + 1, z, Blocks.TORCH);
        }
    }

    /** A totem pole: stacked logs with fence "arms", a carved face and a skull on top. */
    private void totem(Builder b, int x, int z) {
        BlockState skull = skull(b.random);
        for (int y = 1; y <= 4; y++) {
            b.set(x, y, z, log(y % 2 == 0 ? Blocks.STRIPPED_DARK_OAK_LOG : Blocks.DARK_OAK_LOG, Direction.Axis.Y));
        }
        b.set(x, 3, z + 1, Blocks.CARVED_PUMPKIN.getDefaultState().with(net.minecraft.block.CarvedPumpkinBlock.FACING, Direction.SOUTH));
        b.set(x - 1, 4, z, Blocks.DARK_OAK_FENCE);
        b.set(x + 1, 4, z, Blocks.DARK_OAK_FENCE);
        b.set(x, 5, z, skull);
        b.set(x, 0, z, Blocks.COARSE_DIRT);
    }

    /** A heap of bones round a skull. */
    private void boneHeap(Builder b, int x, int z) {
        BlockState skull = skull(b.random);
        b.set(x, 1, z, log(Blocks.BONE_BLOCK, Direction.Axis.Y));
        b.set(x + 1, 1, z, log(Blocks.BONE_BLOCK, Direction.Axis.X));
        b.set(x, 1, z + 1, log(Blocks.BONE_BLOCK, Direction.Axis.Z));
        b.set(x, 2, z, log(Blocks.BONE_BLOCK, Direction.Axis.Y));
        b.set(x - 1, 1, z, skull);
        b.set(x, 3, z, Blocks.SKELETON_SKULL.getDefaultState());
    }

    /** The trampled floor d blocks from the middle. */
    private static BlockState floor(double d, float r, boolean road) {
        if (road && r < 0.75F) {
            return Blocks.PACKED_MUD.getDefaultState();
        }
        if (d > OrcStrongholdStructure.PALISADE + 0.5D) {
            return (r < 0.5F ? Blocks.RED_SAND : r < 0.8F ? Blocks.TERRACOTTA : Blocks.COARSE_DIRT).getDefaultState();
        }
        return (r < 0.4F ? Blocks.COARSE_DIRT : r < 0.65F ? Blocks.RED_SAND : r < 0.8F ? Blocks.PACKED_MUD
                : r < 0.9F ? Blocks.TERRACOTTA : Blocks.GRAVEL).getDefaultState();
    }
}
