package mattonfire.dnd.classes.Rest;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.block.Block;
import net.minecraft.item.BedItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

/**
 * A Bedroll (any colour): right-click a lit campfire with it at night to make camp ({@link PartyCamp}); reusable,
 * stacks to 1. Used anywhere else it lays the bedroll down, a block you sleep in like a bed
 * ({@link mattonfire.dnd.classes.Blocks.adventuring.BedrollBlock}).
 */
public class BedrollItem extends BedItem {
    public BedrollItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getPlayer() == null || !CampfireRest.isLitCampfire(context.getWorld().getBlockState(
                context.getBlockPos()))) {
            return super.useOnBlock(context);
        }
        if (!context.getWorld().isClient && context.getPlayer() instanceof ServerPlayerEntity player
                && !player.isSpectator()) {
            PartyCamp.use(player, context.getBlockPos());
        }
        return ActionResult.success(context.getWorld().isClient);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.dndclasses.bedroll.how").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.dndclasses.bedroll.party").formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.translatable("item.dndclasses.bedroll.place").formatted(Formatting.GRAY));
    }
}
