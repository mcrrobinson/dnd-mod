package mattonfire.dnd.world.gen.sanctum;

import static mattonfire.dnd.world.gen.PieceBlocks.*;

import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.classes.Registry.SanctumBlocks;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The Refuge, south-west of the square: a dormitory of brimstone bricks and crimson planks, its door facing the
 * square, lit by soulfire sconces. Beds explode in the Nether, so its sleepers bind to {@link #ANCHORS} respawn
 * anchors along the back wall instead, framed in cut hellbrass, each with a red carpet before it. They're generated empty: anyone may charge one with glowstone,
 * and a Tiefling binds to one for free and finds it refilled every day ({@link SanctumRefuge}).
 */
public class RefugePiece extends BrimstonePiece {
    static final int WEST = -13;
    static final int EAST = -6;
    static final int BACK = 2;
    static final int FRONT = 10;
    static final int HEIGHT = 5;
    static final int ANCHORS = 4;

    public RefugePiece(BlockPos center, Direction gate, long seed, long sanctum) {
        super(BrimstoneSanctumStructures.REFUGE, center, gate, seed, sanctum, WEST, 0, BACK, EAST + 1, HEIGHT + 1, FRONT);
    }

    public RefugePiece(NbtCompound nbt) {
        super(BrimstoneSanctumStructures.REFUGE, nbt);
    }

    /** The anchors' local b. */
    private static int anchorB(int i) {
        return BACK + 1 + 2 * i;
    }

    /** The respawn anchors, in the world. */
    public List<BlockPos> anchors() {
        List<BlockPos> anchors = new ArrayList<>();
        for (int i = 0; i < ANCHORS; i++) {
            anchors.add(this.at(WEST + 1, 1, anchorB(i)));
        }
        return anchors;
    }

    /** The floor in front of the first anchor, in the world. */
    public BlockPos beforeAnchor() {
        return this.at(WEST + 3, 1, anchorB(0));
    }

    @Override
    protected void build(Builder b) {
        for (int x = WEST; x <= EAST; x++) {
            for (int z = BACK; z <= FRONT; z++) {
                boolean edge = x == WEST || x == EAST || z == BACK || z == FRONT;
                boolean post = (x == WEST || x == EAST) && (z == BACK || z == FRONT || (z - BACK) % 4 == 0)
                        || (z == BACK || z == FRONT) && (x - WEST) % 4 == 0;
                b.set(x, 0, z, edge ? ModBlockFamilies.BRIMSTONE_BRICKS.base() : Blocks.CRIMSON_PLANKS);
                for (int y = 1; y < HEIGHT; y++) {
                    if (post && edge) {
                        b.set(x, y, z, ModBlockFamilies.BRIMSTONE_BRICKS.base());
                    } else if (edge) {
                        boolean window = y == 2 && z == FRONT && x % 2 == 0;
                        b.set(x, y, z, window ? Blocks.RED_STAINED_GLASS_PANE : Blocks.CRIMSON_PLANKS);
                    } else {
                        b.set(x, y, z, Blocks.AIR);
                    }
                }
                b.set(x, HEIGHT, z, edge ? slab(ModBlockFamilies.BRIMSTONE_BRICKS, false) : Blocks.CRIMSON_PLANKS.getDefaultState());
            }
        }
        // The door, facing the square.
        b.air(EAST, 1, 6, EAST, 2, 6);
        b.fill(EAST + 1, 1, 7, EAST + 1, 2, 7, wall(ModBlockFamilies.BRIMSTONE_BRICKS));
        b.set(EAST + 1, 3, 7, soulLantern(false));

        // The anchors along the back wall in a cut hellbrass frame (posts between them, a slab over each), a red
        // carpet before each.
        for (int i = 0; i < ANCHORS; i++) {
            int z = anchorB(i);
            b.set(WEST + 1, 1, z, Blocks.RESPAWN_ANCHOR);
            b.set(WEST + 1, 2, z, slab(ModBlockFamilies.CUT_HELLBRASS, false));
            b.set(WEST + 2, 1, z, Blocks.RED_CARPET);
            if (i > 0) {
                b.fill(WEST + 1, 1, z - 1, WEST + 1, 2, z - 1, ModBlockFamilies.CUT_HELLBRASS.base());
            }
        }
        // Soulfire sconces on the inside of the front wall.
        b.set(WEST + 2, 3, FRONT - 1, facing(SanctumBlocks.SOULFIRE_SCONCE, Direction.NORTH));
        b.set(EAST - 2, 3, FRONT - 1, facing(SanctumBlocks.SOULFIRE_SCONCE, Direction.NORTH));
        b.container(EAST - 1, 1, BACK + 1, facing(Blocks.CHEST, Direction.WEST), REFUGE_LOOT);
        b.set(EAST - 1, 1, FRONT - 1, facing(Blocks.BARREL, Direction.UP));
        b.set((WEST + EAST) / 2, HEIGHT - 1, BACK + 2, soulLantern(true));
        b.set((WEST + EAST) / 2, HEIGHT - 1, FRONT - 2, soulLantern(true));
    }
}
