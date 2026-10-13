package mattonfire.dnd.world.gen;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.util.math.random.Random;

/**
 * A piece turned the vanilla way ({@link #setOrientation}): designed in local coordinates with x across the
 * front, z from the front edge (z = 0) to the back and y = 0 the floor, and block states written as if the
 * piece faced north. {@link Builder#set} turns them to the piece's real facing.
 */
public abstract class FacingPiece<B extends FacingPiece<B>.Builder> extends ModPiece<B> {
    protected FacingPiece(StructurePieceType type, BlockBox box, Direction facing, long seed) {
        super(type, box, seed);
        this.setOrientation(facing);
    }

    protected FacingPiece(StructurePieceType type, NbtCompound nbt) {
        super(type, nbt);
    }

    /**
     * A box of the given local size whose front edge is centred on {@code front}: the block at local
     * (width / 2, 0, 0) lands exactly there.
     */
    protected static BlockBox boxFrom(BlockPos front, Direction facing, int width, int height, int depth) {
        boolean alongZ = facing.getAxis() == Direction.Axis.Z;
        int sizeX = alongZ ? width : depth;
        int sizeZ = alongZ ? depth : width;
        int lx;
        int lz;
        switch (facing) {
            case NORTH -> {
                lx = width / 2;
                lz = sizeZ - 1;
            }
            case SOUTH -> {
                lx = width / 2;
                lz = 0;
            }
            case WEST -> {
                lx = sizeX - 1;
                lz = width / 2;
            }
            default -> {
                lx = 0;
                lz = width / 2;
            }
        }
        int minX = front.getX() - lx;
        int minZ = front.getZ() - lz;
        return new BlockBox(minX, front.getY(), minZ, minX + sizeX - 1, front.getY() + height - 1, minZ + sizeZ - 1);
    }

    /** Local size across the front. */
    protected int width() {
        return this.getFacing().getAxis() == Direction.Axis.Z ? this.boundingBox.getBlockCountX() : this.boundingBox.getBlockCountZ();
    }

    /** Local size from front to back. */
    protected int depth() {
        return this.getFacing().getAxis() == Direction.Axis.Z ? this.boundingBox.getBlockCountZ() : this.boundingBox.getBlockCountX();
    }

    protected int height() {
        return this.boundingBox.getBlockCountY();
    }

    /** The world position of a local spot (which may lie outside the box). */
    public BlockPos worldPos(int x, int y, int z) {
        return this.offsetPos(x, y, z).toImmutable();
    }

    /** Places blocks through {@link #addBlock}, which turns their states to the piece's facing. */
    public abstract class Builder extends ModPiece<B>.Builder {
        protected Builder(StructureWorldAccess world, BlockBox chunkBox, Random random) {
            super(world, chunkBox, random);
        }

        @Override
        public BlockPos pos(int x, int y, int z) {
            return FacingPiece.this.offsetPos(x, y, z);
        }

        /** Turns a direction written facing north into the piece's real direction. */
        @Override
        public Direction dir(Direction local) {
            if (local.getAxis() == Direction.Axis.Y) {
                return local;
            }
            return FacingPiece.this.getRotation().rotate(FacingPiece.this.getMirror().apply(local));
        }

        @Override
        protected void place(int x, int y, int z, BlockPos pos, BlockState state) {
            FacingPiece.this.addBlock(this.world, state, x, y, z, this.chunkBox);
        }
    }
}
