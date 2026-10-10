package mattonfire.dnd.magic.items;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.magic.MagicData;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

/** A passive magic item whose effect is in its {@code MagicEffect}; shows one effect line once identified. */
public class DescribedItem extends Item {
    public DescribedItem(Settings settings) {
        super(settings);
    }

    static void describe(ItemStack stack, List<Text> tooltip) {
        if (MagicData.isIdentified(stack))
            tooltip.add(Text.translatable(stack.getItem().getTranslationKey() + ".tooltip").formatted(Formatting.GRAY));
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        describe(stack, tooltip);
    }

    /** Magic armor with the same effect line (the Storm-Chaser Boots). */
    public static class Armor extends ArmorItem {
        public Armor(ArmorMaterial material, Type type, Settings settings) {
            super(material, type, settings);
        }

        @Override
        public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
            describe(stack, tooltip);
        }
    }
}
