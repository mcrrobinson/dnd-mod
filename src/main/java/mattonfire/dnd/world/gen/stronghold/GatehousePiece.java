package mattonfire.dnd.world.gen.stronghold;

import mattonfire.dnd.entity.ModEntityTypes;
import net.minecraft.block.Blocks;
import net.minecraft.block.LadderBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The gatehouse in the palisade: two 9-high watchtowers of dark oak (a ladder up the inside, an archer
 * platform on top whose rail is open in the middle of each side, so the archers can step up and shoot down)
 * either side of a five-wide gate whose lintel is lined with skulls, and red war banners with a black skull
 * hung on the towers. An Orc Archer keeps watch on each tower.
 */
public class GatehousePiece extends StrongholdPiece {
    static final int TOWER_HEIGHT = 9;
    static final int TOWER_A = 4;

    public GatehousePiece(BlockPos center, Direction gate, long seed, long hold) {
        super(OrcStrongholdStructures.GATEHOUSE, center, gate, seed, hold, -7, 0, OrcStrongholdStructure.PALISADE - 3,
                7, TOWER_HEIGHT + 2, OrcStrongholdStructure.PALISADE + 3);
    }

    public GatehousePiece(NbtCompound nbt) {
        super(OrcStrongholdStructures.GATEHOUSE, nbt);
    }

    @Override
    protected void build(Builder b) {
        int w = OrcStrongholdStructure.PALISADE;
        for (int side = -1; side <= 1; side += 2) {
            int x = side * TOWER_A;
            // The tower: a 3x3 shaft of logs.
            for (int y = 0; y < TOWER_HEIGHT; y++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        boolean corner = Math.abs(dx) == 1 && Math.abs(dz) == 1;
                        b.set(x + dx, y, w + dz, log(corner ? Blocks.DARK_OAK_LOG : Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.Y));
                    }
                }
                b.foundation(x, 0, w, Blocks.TERRACOTTA.getDefaultState(), 16);
            }
            // A ladder up the inside face.
            for (int y = 1; y <= TOWER_HEIGHT; y++) {
                b.set(x, y, w - 2, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.NORTH));
            }
            // The platform, with a hole for the ladder and a rail with a gap in the middle of each side: the
            // archer steps into a gap to shoot down (from behind the rail the platform hides the ground).
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx == 0 && dz == -2) {
                        continue;
                    }
                    b.set(x + dx, TOWER_HEIGHT, w + dz, Blocks.DARK_OAK_PLANKS);
                    boolean edge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                    boolean gap = dx == 0 || dz == 0;
                    if (edge && !gap) {
                        b.set(x + dx, TOWER_HEIGHT + 1, w + dz, Blocks.DARK_OAK_FENCE);
                    }
                }
            }
            b.set(x - 2 * side, TOWER_HEIGHT + 2, w + 2, Blocks.TORCH);
            // War banners: outside, facing the approach, and inside over the yard.
            b.warBanner(x, TOWER_HEIGHT - 2, w + 2, Direction.SOUTH);
            b.warBanner(x + side, TOWER_HEIGHT - 3, w - 2, Direction.NORTH);
            b.orc(x + side, TOWER_HEIGHT + 1, w, ModEntityTypes.ORC_ARCHER, b.pos(x + side, TOWER_HEIGHT + 1, w), 2);
        }
        // The gate: posts, a lintel of logs and a row of skulls.
        for (int y = 1; y <= 5; y++) {
            b.set(-2, y, w, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
            b.set(2, y, w, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
        }
        for (int x = -2; x <= 2; x++) {
            b.set(x, 6, w, log(Blocks.DARK_OAK_LOG, Direction.Axis.X));
        }
        for (int x = -2; x <= 2; x++) {
            b.set(x, 7, w, x == 0 ? Blocks.WITHER_SKELETON_SKULL.getDefaultState() : skull(b.random));
        }
        b.set(-1, 5, w, Blocks.LANTERN.getDefaultState().with(net.minecraft.block.LanternBlock.HANGING, true));
        b.set(1, 5, w, Blocks.LANTERN.getDefaultState().with(net.minecraft.block.LanternBlock.HANGING, true));
    }
}
