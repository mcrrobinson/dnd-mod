package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.world.gen.Desolation;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.ChunkRegion;

/**
 * Drops every entity a desolate settlement's pieces add ({@link Desolation#active()}): worldgen adds them through
 * the {@link ChunkRegion}, {@code /place structure} through the {@link ServerWorld}.
 */
@Mixin({ChunkRegion.class, ServerWorld.class})
public abstract class DesolateSpawnMixin {
    @Inject(method = "spawnEntity", at = @At("HEAD"), cancellable = true)
    private void dndclasses$noDesolateEntities(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (Desolation.active()) {
            Desolation.dropped();
            cir.setReturnValue(false);
        }
    }
}
