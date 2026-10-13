package mattonfire.dnd.world.gen;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;

/**
 * A piece of a structure laid out round a centre: local (a, y, b) is written as if the structure's front (its
 * gate or door) faced south, and turned round {@link #center} to face {@link #front}, block states included.
 * Saved as {@code Center}, the front under a subclass's own key, and {@code Seed}.
 */
public abstract class OrientedPiece<B extends OrientedPiece<B>.Builder> extends ModPiece<B> {
    protected final BlockPos center;
    protected final Direction front;
    /** The NBT key of {@link #front} ("Gate", "Door"). */
    private final String frontKey;

    protected OrientedPiece(StructurePieceType type, String frontKey, BlockPos center, Direction front, long seed,
                            int a1, int y1, int b1, int a2, int y2, int b2) {
        super(type, box(center, front, a1, y1, b1, a2, y2, b2), seed);
        this.frontKey = frontKey;
        this.center = center;
        this.front = front;
    }

    protected OrientedPiece(StructurePieceType type, String frontKey, NbtCompound nbt) {
        super(type, nbt);
        this.frontKey = frontKey;
        this.center = NbtHelper.toBlockPos(nbt.getCompound("Center"));
        this.front = Direction.fromHorizontal(nbt.getInt(frontKey));
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        super.writeNbt(context, nbt);
        nbt.put("Center", NbtHelper.fromBlockPos(this.center));
        nbt.putInt(this.frontKey, this.front.getHorizontal());
    }

    /** The rotation that turns "the front faces south" into the real {@code front}. */
    public static BlockRotation rotation(Direction front) {
        for (BlockRotation rotation : BlockRotation.values()) {
            if (rotation.rotate(Direction.SOUTH) == front) {
                return rotation;
            }
        }
        return BlockRotation.NONE;
    }

    /** The world position of local (a, y, b) round {@code center} with the front towards {@code front}. */
    public static BlockPos at(BlockPos center, Direction front, int a, int y, int b) {
        return center.add(new BlockPos(a, y, b).rotate(rotation(front)));
    }

    public static BlockBox box(BlockPos center, Direction front, int a1, int y1, int b1, int a2, int y2, int b2) {
        return BlockBox.create(at(center, front, a1, y1, b1), at(center, front, a2, y2, b2));
    }

    public BlockPos at(int a, int y, int b) {
        return at(this.center, this.front, a, y, b);
    }

    public BlockPos center() {
        return this.center;
    }

    /** Places blocks in local (a, y, b), turning their states with the structure. */
    public abstract class Builder extends ModPiece<B>.Builder {
        private final BlockRotation rotation;

        protected Builder(StructureWorldAccess world, BlockBox chunkBox, Random random) {
            super(world, chunkBox, random);
            this.rotation = rotation(OrientedPiece.this.front);
        }

        @Override
        public BlockPos pos(int a, int y, int b) {
            return OrientedPiece.this.at(a, y, b);
        }

        @Override
        public Direction dir(Direction local) {
            return this.rotation.rotate(local);
        }

        @Override
        protected void place(int a, int y, int b, BlockPos pos, BlockState state) {
            this.world.setBlockState(pos, state.rotate(this.rotation), PieceBlocks.UPDATE);
        }
    }
}
