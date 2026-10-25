package mattonfire.dnd.classes.mixin;

import mattonfire.dnd.world.gen.temple.GorgonTemple;
import net.minecraft.structure.DesertTempleGenerator;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** One in three desert pyramids is a Gorgon's Temple: the Medusa in the main hall, statues round her. */
@Mixin(DesertTempleGenerator.class)
public abstract class DesertTempleGeneratorMixin extends StructurePiece {
    protected DesertTempleGeneratorMixin(StructurePieceType type, int length, BlockBox box) {
        super(type, length, box);
    }

    @Inject(method = "generate", at = @At("TAIL"))
    private void dnd$gorgonTemple(StructureWorldAccess world, StructureAccessor structureAccessor,
            ChunkGenerator chunkGenerator, Random random, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot,
            CallbackInfo ci) {
        GorgonTemple.decorate(world, chunkBox, this.getBoundingBox(), (x, y, z) -> this.offsetPos(x, y, z), GorgonTemple.DESERT_PYRAMID);
    }
}
