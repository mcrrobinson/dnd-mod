package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import mattonfire.dnd.classes.Enchantments.SmiteDragonsEnchantment;
import mattonfire.dnd.classes.Registry.ModEnchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Adds Smite Dragons' bonus to the enchantment damage PlayerEntity.attack works out, so it scales with
 * the attack cooldown and shows the enchanted-hit particles like Smite. Both getAttackDamage calls are
 * hooked: living targets use the first, dragon parts (not LivingEntity, but with the dragon's type)
 * the second, and only one runs per attack.
 */
@Mixin(PlayerEntity.class)
public abstract class SmiteDragonsMixin {
    @ModifyVariable(method = "attack", ordinal = 1, require = 2, at = @At(value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/enchantment/EnchantmentHelper;getAttackDamage(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EntityGroup;)F"))
    private float dnd$smiteDragons(float enchantDamage, Entity target) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        return enchantDamage + SmiteDragonsEnchantment.getBonus(self.getMainHandStack(), target,
                ModEnchantments.SMITE_DRAGONS_ENCHANTMENT);
    }
}
