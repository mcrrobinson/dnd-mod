package mattonfire.dnd.classes.Items.racial;

import java.util.List;
import java.util.Set;

import mattonfire.dnd.classes.Config.FAConfig;
import mattonfire.dnd.classes.Items.lib.ArmorTooltips;
import mattonfire.dnd.classes.Items.lib.RacialGearItem;
import mattonfire.dnd.classes.Race.DndRace;
import mattonfire.dnd.entity.SlingStoneEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * The Halfling Sling: right-click to sling one cobblestone from your inventory (4 damage, 0.8 s cooldown).
 * Creative players need no stones.
 */
public class HalflingSlingItem extends Item implements RacialGearItem {
    public static final int COOLDOWN_TICKS = 16;

    public HalflingSlingItem(Settings settings) {
        super(settings);
    }

    @Override
    public Set<DndRace> getMatchingRaces() {
        return Set.of(DndRace.HALFLING);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return SlingItems.sling(world, user, hand, this, SlingStoneEntity.DAMAGE, COOLDOWN_TICKS);
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
