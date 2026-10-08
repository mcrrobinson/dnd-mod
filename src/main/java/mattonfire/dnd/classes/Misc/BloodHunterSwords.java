package mattonfire.dnd.classes.Misc;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Blood Hunter nerf: swords cannot be dropped. Covers the player throwing a sword
 * away (Q in the hotbar, Q over a slot, or clicking it outside the inventory). Swords
 * still drop on death, and a sword that doesn't fit back into a full inventory when a
 * screen closes still lands on the ground, like any other item.
 */
public final class BloodHunterSwords {
    private BloodHunterSwords() {
    }

    /** True (and tells the player why in the action bar) if this drop must be blocked. */
    public static boolean blockDrop(PlayerEntity player, ItemStack stack) {
        if (!(player instanceof PlayerEntityExt ext) || ext.getDndClass() != DndCharacter.BLOODHUNTER
                || !(stack.getItem() instanceof SwordItem) || player.isCreative()) {
            return false;
        }
        // Hotbar Q is cancelled on the client before the server hears of it, so both
        // sides show the message (the client one stays local).
        player.sendMessage(Text.literal("Blood Hunters cannot drop their swords!").formatted(Formatting.RED), true);
        return true;
    }
}
