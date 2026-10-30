package mattonfire.dnd.world.gen.warren;

import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.classes.Blocks.CrumblingFloorBlock;
import mattonfire.dnd.classes.Blocks.SpikeTrapBlock;
import mattonfire.dnd.classes.Blocks.TrapBlocks;
import mattonfire.dnd.classes.Blocks.TrapKind;
import mattonfire.dnd.classes.Registry.ModBannerPatterns;
import mattonfire.dnd.entity.kobold.Kobolds;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SkullBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The warren's cave mouth (design 10, 4.3): 5 wide and 4 high, cut into the slope, with a rough stone and coarse-dirt
 * lip, bone piles, two crude red-dragon banners just inside, and a hidden spike strip ({@link TrapKind#SPIKES}, two
 * coarse-dirt tiles) {@value #SPIKE_B} blocks in. Its middle (local 0, 0, 0) is the mouth floor at the foot of the
 * slope; the tunnel goes on from b = -{@value KoboldWarrenPlanner#TUNNEL_START}. A kobold slinger keeps watch, two in a
 * great warren.
 */
public class WarrenMouthPiece extends WarrenPiece {
    /** The spike strip's row, in from the front. */
    public static final int SPIKE_B = -3;
    private final boolean great;

    public WarrenMouthPiece(BlockPos center, Direction front, long seed, int tier, boolean great) {
        super(KoboldWarrens.MOUTH, center, front, seed, tier, -4, -2, -6, 4, 7, 4);
        this.great = great;
    }

    public WarrenMouthPiece(NbtCompound nbt) {
        super(KoboldWarrens.MOUTH, nbt);
        this.great = nbt.getBoolean("Great");
    }

    @Override
    protected void writeNbt(net.minecraft.structure.StructureContext context, NbtCompound nbt) {
        super.writeNbt(context, nbt);
        nbt.putBoolean("Great", this.great);
    }

    public boolean great() {
        return this.great;
    }

    /** The spike strip's trigger, buried under the middle tile. */
    public BlockPos spikeTrigger() {
        return this.at(0, -1, SPIKE_B);
    }

    @Override
    protected void build(Builder b) {
        b.seal(-4, -1, -6, 4, 6, 3);
        // The cut: clear the slope in front of and over the mouth
        for (int bb = 1; bb <= 3; bb++) {
            b.carve(-3, 1, bb, 3, 6, bb);
        }
        for (int bb = -6; bb <= 3; bb++) {
            for (int a = -2; a <= 2; a++) {
                b.floor(a, 0, bb, bb == SPIKE_B ? Blocks.COARSE_DIRT.getDefaultState() : b.dirtFloor());
            }
            if (bb <= 0) {
                // Walls and roof where the slope doesn't give them: rough stone, the odd coarse dirt
                for (int y = 1; y <= 5; y++) {
                    this.solid(b, -3, y, bb);
                    this.solid(b, 3, y, bb);
                }
                this.solid(b, -2, 5, bb);
                this.solid(b, -1, 5, bb);
                this.solid(b, 0, 5, bb);
                this.solid(b, 1, 5, bb);
                this.solid(b, 2, 5, bb);
                b.carve(-2, 1, bb, 2, 3, bb);
                b.carve(-1, 4, bb, 1, 4, bb);
                this.solid(b, -2, 4, bb);
                this.solid(b, 2, 4, bb);
            }
        }
        // The lip: rough stone and coarse dirt round the opening
        for (int y = 1; y <= 4; y++) {
            b.set(-3, y, 1, b.chance(0.5F) ? Blocks.COBBLESTONE : Blocks.MOSSY_COBBLESTONE);
            b.set(3, y, 1, b.chance(0.5F) ? Blocks.COBBLESTONE : Blocks.COARSE_DIRT);
        }
        for (int a = -2; a <= 2; a++) {
            b.set(a, 5, 1, b.chance(0.6F) ? Blocks.COBBLESTONE : Blocks.COARSE_DIRT);
        }
        // Bone piles either side of the way in
        BlockState bone = Blocks.BONE_BLOCK.getDefaultState();
        b.set(-2, 1, 0, bone);
        b.set(2, 1, -1, bone);
        if (b.chance(0.6F)) {
            b.set(-2, 2, 0, Blocks.SKELETON_SKULL.getDefaultState().with(SkullBlock.ROTATION, b.random.nextInt(16)));
        }
        b.set(2, 1, 1, b.chance(0.5F) ? bone : Blocks.AIR.getDefaultState());
        // Crude red-dragon banners just inside
        b.banner(-2, 3, -1, Direction.EAST, DyeColor.RED, ModBannerPatterns.GREEN_DRAGON.in(DyeColor.BLACK));
        b.banner(2, 3, -1, Direction.WEST, DyeColor.RED, ModBannerPatterns.GREEN_DRAGON.in(DyeColor.BLACK));

        // The spike strip: the middle tile and one beside it
        int side = b.chance(0.5F) ? -1 : 1;
        BlockState spike = TrapBlocks.SPIKE_TRAP.getDefaultState().with(SpikeTrapBlock.FLOOR, SpikeTrapBlock.Floor.COARSE_DIRT);
        b.set(0, 0, SPIKE_B, spike);
        b.set(side, 0, SPIKE_B, spike);
        b.set(0, -1, SPIKE_B, Blocks.STONE);
        List<BlockPos> tiles = new ArrayList<>(List.of(this.at(0, 0, SPIKE_B), this.at(side, 0, SPIKE_B)));
        b.trap(this.spikeTrigger(), TrapKind.SPIKES, tiles, List.of(), List.of(), null, Direction.NORTH,
                CrumblingFloorBlock.Covering.STONE, true);

        b.kobold(1, 1, -5, Kobolds.KOBOLD_SLINGER, 8);
        if (this.great) {
            b.kobold(-1, 1, -5, Kobolds.KOBOLD, 8);
        }
    }

    /** Rock wherever the slope left air, water or plants (one random draw either way). */
    private void solid(Builder b, int a, int y, int bb) {
        boolean dirt = b.chance(0.2F);
        BlockState state = b.get(a, y, bb);
        if (state != null && (state.isAir() || state.isReplaceable() || !state.getFluidState().isEmpty())) {
            b.set(a, y, bb, dirt ? Blocks.COARSE_DIRT : Blocks.STONE);
        }
    }
}
