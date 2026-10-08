package mattonfire.dnd.classes.mixin;

import mattonfire.dnd.entity.boss.BossMinions;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Boss minions drop nothing when they die: no loot, no equipment, no experience. */
@Mixin(LivingEntity.class)
public abstract class BossMinionDropMixin {
    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void dnd$noMinionDrops(DamageSource source, CallbackInfo ci) {
        if (BossMinions.isMinion((LivingEntity) (Object) this)) {
            ci.cancel();
        }
    }
}
