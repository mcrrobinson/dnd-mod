package mattonfire.dnd.magic.items;

import mattonfire.dnd.classes.Effects.AntiMagicEffect;
import mattonfire.dnd.magic.Attunement;
import mattonfire.dnd.magic.MagicData;
import mattonfire.dnd.magic.MagicItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Shared checks for magic items that do something when used. */
public final class MagicItemUse {
    private MagicItemUse() {
    }

    /**
     * Whether the item's magic works for the player right now ({@link Attunement#isActive}). If not, tells them
     * why on the action bar: it's unidentified, they're in an anti-magic field, they aren't attuned, or it's
     * made for another class.
     */
    public static boolean ready(PlayerEntity player, ItemStack stack) {
        if (Attunement.isActive(player, stack))
            return true;
        player.sendMessage(whyNot(player, stack), true);
        return false;
    }

    private static Text whyNot(PlayerEntity player, ItemStack stack) {
        if (MagicData.isDormant(stack))
            return Text.translatable("magic.dndclasses.use.dormant").formatted(Formatting.GRAY);
        if (AntiMagicEffect.isSuppressed(player))
            return Text.translatable("effect.dndclasses.anti_magic.blocked");
        MagicItems.Info info = MagicItems.info(stack);
        if (info != null && !Attunement.classAllowed(player, info))
            return Text.translatable("magic.dndclasses.use.wrong_class").formatted(Formatting.RED);
        return Text.translatable("magic.dndclasses.use.attune").formatted(Formatting.RED);
    }
}
