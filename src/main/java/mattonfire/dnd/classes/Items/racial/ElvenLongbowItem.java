package mattonfire.dnd.classes.Items.racial;

import java.util.List;
import java.util.Set;

import mattonfire.dnd.classes.Config.FAConfig;
import mattonfire.dnd.classes.Items.lib.ArmorTooltips;
import mattonfire.dnd.classes.Items.lib.RacialGearItem;
import mattonfire.dnd.classes.Race.DndRace;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;

/**
 * The Elven Longbow: a bow whose arrows fly 20% faster (and so hit harder) and that has Power I built in,
 * on top of any Power it's enchanted with. The speed and damage are applied in {@code BowItemMixin}, where
 * the vanilla bow code shoots the arrow.
 */
public class ElvenLongbowItem extends BowItem implements RacialGearItem {
    /** Arrow speed multiplier. */
    public static final float ARROW_SPEED = 1.2F;
    /** Built-in Power levels, added to the enchantment's. */
    public static final int BUILT_IN_POWER = 1;

    public ElvenLongbowItem(Settings settings) {
        super(settings);
    }

    @Override
    public Set<DndRace> getMatchingRaces() {
        return Set.of(DndRace.ELF);
    }

    /** The arrow speed for a shot from {@code bow}. */
    public static float arrowSpeed(ItemStack bow, float speed) {
        return bow.getItem() instanceof ElvenLongbowItem ? speed * ARROW_SPEED : speed;
    }

    /**
     * Adds the built-in Power to an arrow just shot from {@code bow}. Vanilla Power gives +0.5 damage per
     * level +0.5, so one more level is +1.0 on an unenchanted bow and +0.5 on an enchanted one.
     */
    public static void applyBuiltInPower(ItemStack bow, PersistentProjectileEntity arrow) {
        if (!(bow.getItem() instanceof ElvenLongbowItem)) {
            return;
        }
        int enchanted = EnchantmentHelper.getLevel(Enchantments.POWER, bow);
        double vanilla = enchanted > 0 ? enchanted * 0.5D + 0.5D : 0.0D;
        int total = enchanted + BUILT_IN_POWER;
        arrow.setDamage(arrow.getDamage() + (total * 0.5D + 0.5D) - vanilla);
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        ArmorTooltips.appendRaces(getMatchingRaces(), tooltip);
        if (FAConfig.getValues().showDescriptions()) {
            ArmorTooltips.appendDescription(this.getTranslationKey(), tooltip);
        }
    }
}
