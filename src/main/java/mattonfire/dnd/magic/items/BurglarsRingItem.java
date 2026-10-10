package mattonfire.dnd.magic.items;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.magic.Curse;
import mattonfire.dnd.magic.MagicData;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

/**
 * The Burglar's Ring (Very Rare ring, attunement, always cursed with {@link Curse#RINGS_CALL}). Sneak without moving
 * for a second and you turn invisible until you attack or stop sneaking; mobs lose track of you beyond 4 blocks
 * (see {@code BossItems}). The curse is put on any Ring that lacks it (creative tab, {@code /give}), so there's no
 * uncursed copy.
 */
public class BurglarsRingItem extends Item {
    public BurglarsRingItem(Settings settings) {
        super(settings);
    }

    /** Every Burglar's Ring carries the Ring's Call; it's known once the Ring is identified. */
    public static void ensureCurse(ItemStack stack) {
        if (MagicData.curse(stack).isEmpty()) {
            MagicData.setCurse(stack, Curse.RINGS_CALL.id);
            MagicData.setCurseKnown(stack, MagicData.isIdentified(stack));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient)
            ensureCurse(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (!MagicData.isIdentified(stack))
            return;
        tooltip.add(Text.translatable("item.dndclasses.burglars_ring.tooltip").formatted(Formatting.GRAY));
    }
}
