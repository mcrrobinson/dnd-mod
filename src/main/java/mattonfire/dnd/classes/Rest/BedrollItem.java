package mattonfire.dnd.classes.Rest;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

/**
 * The Bedroll: right-click a lit campfire with it at night to make camp ({@link PartyCamp}). Reusable,
 * stacks to 1.
 */
public class BedrollItem extends Item {
    public BedrollItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getPlayer() == null || !CampfireRest.isLitCampfire(context.getWorld().getBlockState(
                context.getBlockPos()))) {
            return ActionResult.PASS;
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
    }
}
