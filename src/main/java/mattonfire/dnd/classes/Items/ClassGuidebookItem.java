package mattonfire.dnd.classes.Items;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * A book explaining the reader's current class. Its pages aren't stored on the
 * stack: the client builds them from {@link mattonfire.dnd.classes.ClassInfo}
 * when it's opened, so one book stays right after a class change.
 */
public class ClassGuidebookItem extends Item {
    /** Set by the client initializer; opens the book screen for the local player. */
    public static Consumer<PlayerEntity> clientOpener = player -> {
    };

    public ClassGuidebookItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) {
            clientOpener.accept(user);
        }
        user.incrementStat(Stats.USED.getOrCreateStat(this));
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.dndclasses.class_guidebook.tooltip").formatted(Formatting.GRAY));
    }
}
