package mattonfire.dnd.classes.Misc;

import mattonfire.dnd.classes.Registry.ModEnchantments;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;

/**
 * Returning enchantment: sends a thrown item back to the player who threw it.
 * Called from {@code TridentEntityMixin} and {@code ProjectileEntityMixin}.
 */
public class Returning {
    public static boolean hasReturning(ItemStack stack) {
        return !stack.isEmpty() && EnchantmentHelper.getLevel(ModEnchantments.RETURNING_ENCHANTMENT, stack) > 0;
    }

    /**
     * The player a projectile should return to: its owner, if that's a living,
     * non-spectator player in the same world. Null otherwise, in which case the
     * projectile behaves as usual.
     */
    public static PlayerEntity returnTarget(Entity projectile, Entity owner) {
        if (projectile.world.isClient || !(owner instanceof PlayerEntity player) || !player.isAlive()
                || player.isSpectator() || player.world != projectile.world) {
            return null;
        }
        return player;
    }

    /**
     * Puts one {@code stack} back in the player's inventory (or at their feet if
     * it's full) and plays {@code sound}.
     *
     * @param giveItem false when the player shouldn't get a copy back, e.g. a
     *                 creative-mode throw that never used the item up.
     */
    public static void giveBack(PlayerEntity player, ItemStack stack, boolean giveItem, SoundEvent sound,
            float volume, float pitch) {
        if (giveItem && !stack.isEmpty()) {
            ItemStack copy = stack.copy();
            copy.setCount(1);
            if (!player.getInventory().insertStack(copy) && !copy.isEmpty()) {
                player.dropItem(copy, false);
            }
        }
        player.world.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundCategory.PLAYERS,
                volume, pitch);
    }
}
