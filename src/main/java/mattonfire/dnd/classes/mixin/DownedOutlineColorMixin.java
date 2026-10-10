package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.Client.Hud.DownedHud;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/** A Downed party member's outline ({@link DownedOutlineMixin}) is red (gold once stable), not the team colour. */
@Mixin(Entity.class)
public abstract class DownedOutlineColorMixin {
    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void dndclasses$downedOutlineColor(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity) (Object) this;
        if (DownedHud.outlined(self)) {
            cir.setReturnValue(DownedHud.outlineColor((PlayerEntity) self));
        }
    }
}
