package mattonfire.dnd.world.gen.warren;

import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.classes.Blocks.CrumblingFloorBlock;
import mattonfire.dnd.classes.Blocks.KoboldTriplineBlock;
import mattonfire.dnd.classes.Blocks.TrapBlocks;
import mattonfire.dnd.classes.Blocks.TrapKind;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The warren's tunnel (design 10, 4.3): 3 wide and 3 high, running {@link #length} blocks into the hill from the
 * mouth and dropping one block every 3. Torches every 6 blocks (kobolds see in the dark; these are their "dragon
 * fires"). Halfway down, a {@link TrapKind#ROCKFALL}: a tripline across the tunnel under a 3 x 3 of loose ceiling.
 * Laid out round the mouth (local b = -{@value KoboldWarrenPlanner#TUNNEL_START} - k for step k).
 */
public class WarrenTunnelPiece extends WarrenPiece {
    private final int length;

    public WarrenTunnelPiece(BlockPos mouth, Direction front, long seed, int tier, int length) {
        super(KoboldWarrens.TUNNEL, mouth, front, seed, tier, -3, -(length - 1) / 3 - 2, -(KoboldWarrenPlanner.TUNNEL_START
                + length), 3, 5, -KoboldWarrenPlanner.TUNNEL_START + 1);
        this.length = length;
    }

    public WarrenTunnelPiece(NbtCompound nbt) {
        super(KoboldWarrens.TUNNEL, nbt);
        this.length = nbt.getInt("Length");
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        super.writeNbt(context, nbt);
        nbt.putInt("Length", this.length);
    }

    public int length() {
        return this.length;
    }

    static int floorY(int k) {
        return -(k / 3);
    }

    static int localB(int k) {
        return -KoboldWarrenPlanner.TUNNEL_START - k;
    }

    /** The middle row of the rockfall (its three rows share one floor). */
    public int rockfallStep() {
        int m = Math.max(1, (this.length / 3) / 2);
        return 3 * m + 1;
    }

    /** The rockfall's buried trigger. */
    public BlockPos rockfallTrigger() {
        int k = this.rockfallStep();
        return this.at(0, floorY(k) - 1, localB(k));
    }

    /** The floor block in the middle of the rockfall (stand on top of it). */
    public BlockPos rockfallFloor() {
        int k = this.rockfallStep();
        return this.at(0, floorY(k), localB(k));
    }

    @Override
    protected void build(Builder b) {
        int kr = this.rockfallStep();
        for (int k = 0; k < this.length; k++) {
            int y = floorY(k);
            int bb = localB(k);
            b.seal(-2, y - 1, bb, 2, y + 5, bb);
            for (int a = -2; a <= 2; a++) {
                for (int dy = 0; dy <= 4; dy++) {
                    boolean inside = Math.abs(a) <= 1 && dy >= 1 && dy <= 3;
                    if (!inside) {
                        this.solid(b, a, y + dy, bb);
                    }
                }
            }
            for (int a = -1; a <= 1; a++) {
                b.floor(a, y, bb, b.dirtFloor());
            }
            b.carve(-1, y + 1, bb, 1, y + 3, bb);
            boolean rockfall = Math.abs(k - kr) <= 1;
            if (k % 6 == 3 && !rockfall) {
                boolean west = (k / 6) % 2 == 0;
                b.wallTorch(west ? -1 : 1, y + 2, bb, west ? Direction.EAST : Direction.WEST);
            }
        }

        // The rockfall: loose ceiling over the three rows, a tripline across the middle one
        List<BlockPos> linked = new ArrayList<>();
        List<BlockPos> rubble = new ArrayList<>();
        int y = floorY(kr);
        BlockState line = TrapBlocks.KOBOLD_TRIPLINE.getDefaultState().with(KoboldTriplineBlock.AXIS, Direction.Axis.X);
        for (int a = -1; a <= 1; a++) {
            b.set(a, y + 1, localB(kr), line);
            linked.add(this.at(a, y + 1, localB(kr)));
            for (int k = kr - 1; k <= kr + 1; k++) {
                b.set(a, y + 4, localB(k), TrapBlocks.LOOSE_CEILING.getDefaultState());
                linked.add(this.at(a, y + 4, localB(k)));
                rubble.add(this.at(a, y + 1, localB(k)));
            }
        }
        b.trap(this.rockfallTrigger(), TrapKind.ROCKFALL, linked, List.of(), rubble, null, Direction.NORTH,
                CrumblingFloorBlock.Covering.STONE, true);
    }

    private void solid(Builder b, int a, int y, int bb) {
        BlockState state = b.get(a, y, bb);
        if (state != null && (state.isAir() || state.isReplaceable() || !state.getFluidState().isEmpty())) {
            b.set(a, y, bb, Blocks.STONE);
        }
    }
}
