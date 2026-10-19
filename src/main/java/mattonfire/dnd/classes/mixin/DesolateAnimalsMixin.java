package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.world.gen.Desolation;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;

/** A new chunk a desolate settlement covers gets none of the animals vanilla adds to new chunks ({@link Desolation#covers}). */
@Mixin(NoiseChunkGenerator.class)
public abstract class DesolateAnimalsMixin {
    @Inject(method = "populateEntities", at = @At("HEAD"), cancellable = true)
    private void dndclasses$noDesolateAnimals(ChunkRegion region, CallbackInfo ci) {
        if (Desolation.covers(region)) {
            ci.cancel();
        }
    }
}
