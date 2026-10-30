package mattonfire.dnd.entity.kobold;

import java.util.List;
import mattonfire.dnd.classes.Config.FAConfig;
import mattonfire.dnd.classes.Items.lib.ArmorTooltips;
import mattonfire.dnd.classes.Items.racial.SlingItems;
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
 * The Kobold Sling: a crude hide sling anyone can use. Right-click slings one cobblestone from your inventory
 * ({@value SlingStoneEntity#KOBOLD_DAMAGE} damage, {@value #COOLDOWN_TICKS}-tick cooldown, {@value #DURABILITY}
 * uses). Shares the Halfling Sling's ammo and throw ({@link SlingItems}).
 */
public class KoboldSlingItem extends Item {
    public static final int COOLDOWN_TICKS = 20;
    public static final int DURABILITY = 120;

    public KoboldSlingItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return SlingItems.sling(world, user, hand, this, SlingStoneEntity.KOBOLD_DAMAGE, COOLDOWN_TICKS);
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        if (FAConfig.getValues().showDescriptions()) {
            ArmorTooltips.appendDescription(this.getTranslationKey(), tooltip);
        }
    }
}
