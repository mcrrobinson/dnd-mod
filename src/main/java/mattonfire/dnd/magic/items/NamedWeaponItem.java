package mattonfire.dnd.magic.items;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.magic.MagicData;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

/**
 * A named magic weapon (the Mace of Disruption, Frostbrand): a sword-like melee weapon whose effects live in its
 * {@code MagicEffect} ({@code BossItems}). Shows its effect line once identified; {@code glows} gives it the
 * enchantment glint once identified.
 */
public class NamedWeaponItem extends SwordItem {
    private final String tooltipKey;
    private final boolean glows;

    public NamedWeaponItem(ToolMaterial material, int attackDamage, float attackSpeed, String tooltipKey,
            boolean glows, Settings settings) {
        super(material, attackDamage, attackSpeed, settings);
        this.tooltipKey = tooltipKey;
        this.glows = glows;
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return (glows && MagicData.isIdentified(stack)) || super.hasGlint(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (!MagicData.isIdentified(stack))
            return;
        // One tooltip line per newline in the translation, so long descriptions don't run off the screen
        for (String line : Text.translatable(tooltipKey).getString().split("\n"))
            tooltip.add(Text.literal(line).formatted(Formatting.GRAY));
    }
}
