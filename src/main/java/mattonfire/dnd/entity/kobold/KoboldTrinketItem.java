package mattonfire.dnd.entity.kobold;

import java.util.List;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

/**
 * A Kobold Trinket: one of {@value #KINDS} odds and ends a kobold treasures (NBT {@code Trinket}, 0-11), each with
 * its own tooltip line. One without a kind gets a random one the first time it's in an inventory. Gnome Tinkers buy
 * them, and a Dragon Tribute Bowl takes them.
 */
public class KoboldTrinketItem extends Item {
    public static final int KINDS = 12;
    public static final String KEY = "Trinket";

    public KoboldTrinketItem(Settings settings) {
        super(settings);
    }

    /** The trinket's kind, or -1 before it has one. */
    public static int kind(ItemStack stack) {
        return stack.getNbt() != null && stack.getNbt().contains(KEY) ? Math.floorMod(stack.getNbt().getInt(KEY), KINDS) : -1;
    }

    public static ItemStack of(int kind) {
        ItemStack stack = new ItemStack(Kobolds.KOBOLD_TRINKET);
        stack.getOrCreateNbt().putInt(KEY, Math.floorMod(kind, KINDS));
        return stack;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient && kind(stack) < 0) {
            stack.getOrCreateNbt().putInt(KEY, world.getRandom().nextInt(KINDS));
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        int kind = kind(stack);
        if (kind >= 0) {
            tooltip.add(Text.translatable("item.dndclasses.kobold_trinket." + kind).formatted(Formatting.GRAY, Formatting.ITALIC));
        }
    }
}
