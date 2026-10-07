package mattonfire.dnd.classes.Items;

import mattonfire.dnd.classes.Registry.ModItems;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Hands out the class guidebook on a player's first join and when their class changes. */
public final class ClassGuidebook {
    private ClassGuidebook() {
    }

    /** A player who has never left the world before is joining for the first time. */
    public static void onJoin(ServerPlayerEntity player) {
        if (player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.LEAVE_GAME)) == 0) {
            giveIfMissing(player);
        }
    }

    /** Gives the guidebook unless the player already carries one (it always shows their current class). */
    public static void giveIfMissing(ServerPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.getStack(i).isOf(ModItems.CLASS_GUIDEBOOK)) {
                return;
            }
        }
        ItemStack book = new ItemStack(ModItems.CLASS_GUIDEBOOK);
        if (!inventory.insertStack(book)) {
            player.dropItem(book, false);
        }
        player.sendMessage(Text.translatable("message.dndclasses.class_guidebook.received")
                .formatted(Formatting.GOLD), false);
    }
}
