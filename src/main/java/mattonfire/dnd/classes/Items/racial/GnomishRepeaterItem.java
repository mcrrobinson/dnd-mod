package mattonfire.dnd.classes.Items.racial;

import java.util.List;
import java.util.Set;

import mattonfire.dnd.classes.Config.FAConfig;
import mattonfire.dnd.classes.Items.lib.ArmorTooltips;
import mattonfire.dnd.classes.Items.lib.RacialGearItem;
import mattonfire.dnd.classes.Race.DndRace;
import mattonfire.dnd.classes.Registry.ModItems;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;

/**
 * The Gnomish Repeater (Gnome kin stock at Cogsdeep): a brass-limbed crossbow with a drum magazine. It works like
 * any crossbow; the Tinkers sell it already enchanted with Quick Charge III and Multishot ({@link #create}), so it
 * reloads in a quarter of a second and fires three bolts at once. The crossbow's model states and the vanilla
 * crossbow's hand poses are given to it on the client ({@code DndClassesClient}, {@code CrossbowRenderMixin}).
 */
public class GnomishRepeaterItem extends CrossbowItem implements RacialGearItem {
    public static final int QUICK_CHARGE = 3;

    public GnomishRepeaterItem(Settings settings) {
        super(settings);
    }

    /** A Repeater as the Tinkers sell it: Quick Charge III and Multishot. */
    public static ItemStack create() {
        ItemStack stack = new ItemStack(ModItems.GNOMISH_REPEATER);
        stack.addEnchantment(Enchantments.QUICK_CHARGE, QUICK_CHARGE);
        stack.addEnchantment(Enchantments.MULTISHOT, 1);
        return stack;
    }

    @Override
    public Set<DndRace> getMatchingRaces() {
        return Set.of(DndRace.GNOME);
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        ArmorTooltips.appendRaces(this.getMatchingRaces(), tooltip);
        if (FAConfig.getValues().showDescriptions()) {
            ArmorTooltips.appendDescription(this.getTranslationKey(), tooltip);
        }
    }
}
