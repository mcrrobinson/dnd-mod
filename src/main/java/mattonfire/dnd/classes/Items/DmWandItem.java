package mattonfire.dnd.classes.Items;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.dm.DmWand;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * The Dungeon Master's wand: the common {@code /dm} actions on right-click. Clicks on blocks and entities are claimed
 * by {@link DmWand#register()}'s callbacks; this handles clicks on nothing within reach. All the work happens in
 * {@link DmWand} on the server, from a 64-block raycast.
 */
public class DmWandItem extends Item {
    public DmWandItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user instanceof ServerPlayerEntity player) {
            DmWand.use(player, stack);
        }
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public Text getName(ItemStack stack) {
        return DmWand.name(stack, super.getName(stack));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal("Dungeon Masters only").formatted(Formatting.GOLD));
        tooltip.add(Text.literal("Sneak + right-click air: next mode").formatted(Formatting.GRAY));
        tooltip.add(Text.literal(DmWand.mode(stack).label + " - right-click " + DmWand.mode(stack).hint).formatted(Formatting.GRAY));
    }
}
