package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.classes.Obstacles.StructureObstacles;
import mattonfire.dnd.world.gen.Desolation;
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
 *
 * A desolate settlement ({@link Desolation}) also places no entities while its pieces go down, and is withered
 * to its level once they're all in.
 */
@Mixin(StructureStart.class)
public abstract class StructureStartPlacingMixin {
    @Inject(method = "place", at = @At("HEAD"))
    private void dndclasses$startPlacing(StructureWorldAccess world, StructureAccessor structureAccessor,
            ChunkGenerator chunkGenerator, Random random, BlockBox chunkBox, ChunkPos chunkPos, CallbackInfo ci) {
        StructureStart start = (StructureStart) (Object) this;
        StructureObstacles.setPlacing(start);
        Desolation.begin(world, start);
    }

    @Inject(method = "place", at = @At("RETURN"))
    private void dndclasses$stopPlacing(StructureWorldAccess world, StructureAccessor structureAccessor,
            ChunkGenerator chunkGenerator, Random random, BlockBox chunkBox, ChunkPos chunkPos, CallbackInfo ci) {
        StructureObstacles.setPlacing(null);
        Desolation.end(world, (StructureStart) (Object) this, chunkBox, chunkGenerator);
    }
}
