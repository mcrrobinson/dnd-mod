package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.Race.RacePassives;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;

/** Keen Senses: a sneaking Elf sees hostile mobs nearby outlined, through walls, for this player only. */
@Mixin(MinecraftClient.class)
public abstract class KeenSensesOutlineMixin {
    @Shadow
    public ClientPlayerEntity player;

    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    private void dndclasses$keenSenses(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (RacePassives.keenSenses(this.player, entity)) {
            cir.setReturnValue(true);
        }
    }
}
