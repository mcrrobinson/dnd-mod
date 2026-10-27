package mattonfire.dnd.world.underdark.duergar;

import mattonfire.dnd.world.underdark.UnderdarkPiece;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;

/**
 * A Duergar Hold building: an {@link UnderdarkPiece} that can also be built mirrored (local a turned to -a, block
 * states mirrored with it), so in a mirrored hold every building is the true mirror image and its wings stay on the
 * same side of the road. It also carries the hold's {@link DuergarMasonry.Style} (roof material, ridge, trim), shared
 * by all the hold's buildings. Saved as {@code Mirror} and {@code Style}.
 */
public abstract class DuergarPiece extends UnderdarkPiece {
    protected final boolean mirror;
    protected final int style;

    protected DuergarPiece(StructurePieceType type, BlockPos center, Direction front, long seed, boolean mirror, int style,
                           int a1, int y1, int b1, int a2, int y2, int b2) {
        super(type, center, front, seed, mirror ? -a2 : a1, y1, b1, mirror ? -a1 : a2, y2, b2);
        this.mirror = mirror;
        this.style = style;
    }

    protected DuergarPiece(StructurePieceType type, NbtCompound nbt) {
        super(type, nbt);
        this.mirror = nbt.getBoolean("Mirror");
        this.style = nbt.getInt("Style");
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        super.writeNbt(context, nbt);
        nbt.putBoolean("Mirror", this.mirror);
        nbt.putInt("Style", this.style);
    }

    @Override
    public BlockPos at(int a, int y, int b) {
        return super.at(this.mirror ? -a : a, y, b);
    }

    protected DuergarMasonry.Style style() {
        return DuergarMasonry.Style.of(this.style);
    }

    @Override
    protected Builder builder(StructureWorldAccess world, BlockBox chunkBox, Random random) {
        return new Mirrored(world, chunkBox, random);
    }

    /** A builder that mirrors block states and directions along with the positions. */
    protected class Mirrored extends Builder {
        protected Mirrored(StructureWorldAccess world, BlockBox chunkBox, Random random) {
            super(world, chunkBox, random);
        }

        @Override
        protected void place(int a, int y, int b, BlockPos pos, BlockState state) {
            super.place(a, y, b, pos, DuergarPiece.this.mirror ? state.mirror(BlockMirror.FRONT_BACK) : state);
        }

        @Override
        public Direction dir(Direction local) {
            return super.dir(DuergarPiece.this.mirror ? BlockMirror.FRONT_BACK.apply(local) : local);
        }
    }
}
