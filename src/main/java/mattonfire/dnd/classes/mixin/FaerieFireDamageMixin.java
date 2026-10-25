package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import mattonfire.dnd.entity.drow.FaerieFire;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;

/** Faerie Fire: a creature outlined by a Drow Mage takes +2 damage from every hit ({@link FaerieFire#modify}). */
@Mixin(LivingEntity.class)
public abstract class FaerieFireDamageMixin {
    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float dndclasses$faerieFire(float amount, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        return self.getWorld().isClient ? amount : FaerieFire.modify(self, source, amount);
    }
}
