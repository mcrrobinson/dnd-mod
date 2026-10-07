package mattonfire.dnd.classes.mixin;

import mattonfire.dnd.classes.Enchantments.VampiricEnchantment;
import mattonfire.dnd.classes.Registry.ModEnchantments;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vampiric enchantment: when a living entity is hurt by a melee hit from an
 * attacker holding a Vampiric weapon, the attacker heals for a share of the
 * health (and absorption) the target actually lost.
 */
@Mixin(LivingEntity.class)
public abstract class VampiricMixin {
    @Unique
    private float dndclasses$vampiricHealthBefore;

    @Inject(method = "damage", at = @At("HEAD"))
    private void dndclasses$vampiricRecordHealth(DamageSource source, float amount,
            CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        dndclasses$vampiricHealthBefore = self.getHealth() + self.getAbsorptionAmount();
    }

    @Inject(method = "damage", at = @At("RETURN"))
    private void dndclasses$vampiricHeal(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.world.isClient || !cir.getReturnValueZ()) {
            return;
        }
        // Melee only: the attacker must be the direct source (no arrows, tridents, thorns...).
        if (!(source.getAttacker() instanceof LivingEntity attacker) || source.getSource() != attacker
                || attacker == self
                || !(source.isOf(DamageTypes.PLAYER_ATTACK) || source.isOf(DamageTypes.MOB_ATTACK)
                        || source.isOf(DamageTypes.MOB_ATTACK_NO_AGGRO))) {
            return;
        }
        int level = EnchantmentHelper.getLevel(ModEnchantments.VAMPIRIC_ENCHANTMENT, attacker.getMainHandStack());
        if (level <= 0 || !attacker.isAlive()) {
            return;
        }
        float dealt = dndclasses$vampiricHealthBefore - (Math.max(self.getHealth(), 0f) + self.getAbsorptionAmount());
        if (dealt > 0f) {
            attacker.heal(dealt * VampiricEnchantment.healFraction(level));
        }
    }
}
