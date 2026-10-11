package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.Items.racial.RacialGear;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;

/** Elven Chain: drops spawned during a sneaking Elf's block break are silenced ({@link RacialGear#onEntitySpawned}). */
@Mixin(ServerWorld.class)
public abstract class ElvenChainDropsMixin {
    @Inject(method = "spawnEntity", at = @At("HEAD"))
    private void dnd$silenceElvenDrops(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        RacialGear.onEntitySpawned(entity);
    }
}
