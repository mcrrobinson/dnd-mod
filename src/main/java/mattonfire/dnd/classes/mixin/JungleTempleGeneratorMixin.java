package mattonfire.dnd.classes.mixin;

import mattonfire.dnd.world.gen.temple.GorgonTemple;
import net.minecraft.structure.JungleTempleGenerator;
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

/** One in three jungle temples is a Gorgon's Temple: the Medusa in the upper room, statues round her. */
@Mixin(JungleTempleGenerator.class)
public abstract class JungleTempleGeneratorMixin extends StructurePiece {
    protected JungleTempleGeneratorMixin(StructurePieceType type, int length, BlockBox box) {
        super(type, length, box);
    }

    @Inject(method = "generate", at = @At("TAIL"))
    private void dnd$gorgonTemple(StructureWorldAccess world, StructureAccessor structureAccessor,
            ChunkGenerator chunkGenerator, Random random, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot,
            CallbackInfo ci) {
        GorgonTemple.decorate(world, chunkBox, this.getBoundingBox(), (x, y, z) -> this.offsetPos(x, y, z), GorgonTemple.JUNGLE_TEMPLE);
    }
}
