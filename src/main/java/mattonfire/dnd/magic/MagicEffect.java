package mattonfire.dnd.magic;

import java.util.List;
import java.util.UUID;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * What a magic item does while it's active for its holder (see {@link Attunement#isActive}), with the same
 * hook shape as {@code ClassSkills}. Registered per item with {@link MagicEffects#register}; every hook is
 * optional. Hooks only run for a stack that's in place: weapons and staffs in the main hand, armor worn,
 * anything else anywhere in the inventory.
 */
public interface MagicEffect {
    /** An attribute modifier the item gives while active, kept on the player by {@link MagicEffects}. */
    record Bonus(EntityAttribute attribute, UUID uuid, double amount, EntityAttributeModifier.Operation operation) {
    }

    /** Attribute modifiers held while the item is active (temporary, so they're never saved). */
    default List<Bonus> attributeBonuses() {
        return List.of();
    }

    /** Damage the holder deals, before armor. */
    default float modifyDealtDamage(PlayerEntity player, ItemStack stack, LivingEntity target, DamageSource source,
            float amount) {
        return amount;
    }

    /** Damage the holder takes, before armor. */
    default float modifyTakenDamage(PlayerEntity player, ItemStack stack, DamageSource source, float amount) {
        return amount;
    }

    /** Once a second while active. */
    default void secondTick(ServerPlayerEntity player, ItemStack stack) {
    }

    /** The holder killed something. */
    default void onKill(ServerPlayerEntity player, ItemStack stack, LivingEntity killed, DamageSource source) {
    }
}
