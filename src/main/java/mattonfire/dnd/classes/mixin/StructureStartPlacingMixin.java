package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.classes.Obstacles.StructureObstacles;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

/**
 * Remembers which structure start is placing its pieces on this thread ({@link StructureObstacles#placing()}),
 * so a vanilla piece's mixin can see its sibling pieces (a stronghold corridor's hidden closet must not cut into
 * another room). Works for natural generation and {@code /place structure} alike.
 */
@Mixin(StructureStart.class)
public abstract class StructureStartPlacingMixin {
    @Inject(method = "place", at = @At("HEAD"))
    private void dndclasses$startPlacing(StructureWorldAccess world, StructureAccessor structureAccessor,
            ChunkGenerator chunkGenerator, Random random, BlockBox chunkBox, ChunkPos chunkPos, CallbackInfo ci) {
        StructureObstacles.setPlacing((StructureStart) (Object) this);
    }

    @Inject(method = "place", at = @At("RETURN"))
    private void dndclasses$stopPlacing(StructureWorldAccess world, StructureAccessor structureAccessor,
            ChunkGenerator chunkGenerator, Random random, BlockBox chunkBox, ChunkPos chunkPos, CallbackInfo ci) {
        StructureObstacles.setPlacing(null);
    }
}
