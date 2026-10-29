package mattonfire.dnd.world.gen.warren;

import mattonfire.dnd.entity.kobold.Kobolds;
import mattonfire.dnd.world.gen.PieceBlocks;
import net.minecraft.block.Blocks;
import net.minecraft.block.TripwireHookBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The trap workshop (design 10, 4.3), back west of the den: a room 7 x 5 with a workbench, a chest of trap parts
 * ({@code chests/kobold_workshop}), tripwire hooks on the walls, cobwebs and 1-2 Kobold Trapmakers, who go out to re-arm
 * the warren's traps. Room a = -12..-6, b = -10..-6 in the den's frame, through a corridor from the den's back.
 */
public class WarrenWorkshopPiece extends WarrenPiece {
    public WarrenWorkshopPiece(BlockPos den, Direction front, long seed, int tier) {
        super(KoboldWarrens.WORKSHOP, den, front, seed, tier, -13, -2, -11, -2, 6, -2);
    }

    public WarrenWorkshopPiece(NbtCompound nbt) {
        super(KoboldWarrens.WORKSHOP, nbt);
    }

    public BlockPos middle() {
        return this.at(-9, 1, -8);
    }

    @Override
    protected void build(Builder b) {
        b.seal(-13, -1, -11, -2, 5, -2);
        for (int a = -5; a <= -3; a++) {
            for (int bb = -8; bb <= -3; bb++) {
                b.floor(a, 0, bb, b.dirtFloor());
            }
        }
        b.carve(-5, 1, -8, -3, 3, -3);
        for (int a = -12; a <= -6; a++) {
            for (int bb = -10; bb <= -6; bb++) {
                b.floor(a, 0, bb, b.dirtFloor());
            }
        }
        b.carve(-12, 1, -10, -6, 4, -6);
        b.set(-12, 1, -10, Blocks.CRAFTING_TABLE);
        b.set(-11, 1, -10, Blocks.FLETCHING_TABLE);
        b.chest(-12, 1, -8, Direction.EAST, WORKSHOP);
        b.barrel(-12, 1, -6, Direction.UP, WORKSHOP);
        b.set(-10, 2, -10, PieceBlocks.facing(Blocks.TRIPWIRE_HOOK, Direction.SOUTH).with(TripwireHookBlock.ATTACHED, false));
        b.set(-8, 2, -10, PieceBlocks.facing(Blocks.TRIPWIRE_HOOK, Direction.SOUTH).with(TripwireHookBlock.ATTACHED, false));
        b.set(-6, 4, -10, Blocks.COBWEB);
        b.set(-12, 4, -6, Blocks.COBWEB);
        b.set(-7, 1, -10, Blocks.LANTERN);
        b.kobold(-9, 1, -8, Kobolds.KOBOLD_TRAPMAKER, 20);
        if (b.chance(0.5F)) {
            b.kobold(-8, 1, -7, Kobolds.KOBOLD_TRAPMAKER, 20);
        }
    }
}
