package mattonfire.dnd.classes.Items.racial;

import java.util.List;
import java.util.Set;

import mattonfire.dnd.classes.Config.FAConfig;
import mattonfire.dnd.classes.Items.lib.ArmorTooltips;
import mattonfire.dnd.classes.Items.lib.RacialGearItem;
import mattonfire.dnd.classes.Race.DndRace;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.text.Text;
import net.minecraft.world.World;

/**
 * A racial axe: the Dwarven Waraxe (10 damage, 0.9 attack speed, +50% to goblins through
 * {@link RacialGear#modifyDealtDamage}) or the Orcish Greataxe (11 damage, 0.8 attack speed). Anyone can wield
 * one; the tooltip names the race it's made for.
 */
public class RacialAxeItem extends AxeItem implements RacialGearItem {
    private final Set<DndRace> races;

    /**
     * @param attackDamage added to the material's damage and the player's 1, so diamond (3) + 6 = 10
     * @param attackSpeed  added to the player's 4, so -3.1 = 0.9
     */
    public RacialAxeItem(ToolMaterial material, float attackDamage, float attackSpeed, Set<DndRace> races,
            Settings settings) {
        super(material, attackDamage, attackSpeed, settings);
        this.races = Set.copyOf(races);
    }

    @Override
    public Set<DndRace> getMatchingRaces() {
        return races;
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        ArmorTooltips.appendRaces(races, tooltip);
        if (FAConfig.getValues().showDescriptions()) {
            ArmorTooltips.appendDescription(this.getTranslationKey(), tooltip);
        }
    }
}
