package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import mattonfire.dnd.classes.Progression.Classes.WizardSkills;
import net.minecraft.entity.Entity;
import net.minecraft.world.explosion.Explosion;

/**
 * Evocation Wizard's Sculpt Spells: their blasts treat party members and pets as immune to explosions,
 * which skips both the damage and the knockback (see {@link WizardSkills#sculpts}).
 */
@Mixin(Explosion.class)
public abstract class ExplosionSculptMixin {
    @WrapOperation(method = "collectBlocksAndDamageEntities", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;isImmuneToExplosion()Z"))
    private boolean dnd$sculptSpells(Entity target, Operation<Boolean> original) {
        return WizardSkills.sculpts((Explosion) (Object) this, target) || original.call(target);
    }
}
