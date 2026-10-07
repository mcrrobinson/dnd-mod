package mattonfire.dnd.tavern;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A notice taken from a tavern bounty board. It remembers which bounty it is and how far along
 * the job is; carry it anywhere in your inventory while you hunt or explore, then hand it back to
 * a bounty board or an innkeeper once it's done for the reward.
 */
public class BountyNoticeItem extends Item {
    private static final String BOUNTY = "Bounty";
    private static final String PROGRESS = "Progress";

    public BountyNoticeItem(Settings settings) {
        super(settings);
    }

    public static ItemStack create(Bounty bounty) {
        ItemStack stack = new ItemStack(Tavern.BOUNTY_NOTICE);
        stack.getOrCreateNbt().putString(BOUNTY, bounty.id);
        return stack;
    }

    @Nullable
    public static Bounty bounty(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (!stack.isOf(Tavern.BOUNTY_NOTICE) || nbt == null) {
            return null;
        }
        return Bounty.byId(nbt.getString(BOUNTY));
    }

    public static int progress(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt == null ? 0 : nbt.getInt(PROGRESS);
    }

    public static void setProgress(ItemStack stack, int progress) {
        stack.getOrCreateNbt().putInt(PROGRESS, progress);
    }

    public static boolean isComplete(ItemStack stack) {
        Bounty bounty = bounty(stack);
        return bounty != null && progress(stack) >= bounty.count;
    }

    @Override
    public Text getName(ItemStack stack) {
        Bounty bounty = bounty(stack);
        if (bounty == null) {
            return super.getName(stack);
        }
        return Text.translatable(this.getTranslationKey() + ".named", bounty.title());
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return isComplete(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        Bounty bounty = bounty(stack);
        if (bounty == null) {
            tooltip.add(Text.translatable("bounty.dndclasses.blank").formatted(Formatting.GRAY));
            return;
        }
        tooltip.add(bounty.description().copy().formatted(Formatting.GRAY, Formatting.ITALIC));
        if (isComplete(stack)) {
            tooltip.add(Text.translatable("bounty.dndclasses.complete").formatted(Formatting.GREEN));
        } else if (bounty.isHunt()) {
            tooltip.add(Text.translatable("bounty.dndclasses.progress.hunt", progress(stack), bounty.count)
                    .formatted(Formatting.YELLOW));
        } else {
            tooltip.add(Text.translatable("bounty.dndclasses.progress.explore").formatted(Formatting.YELLOW));
        }
        tooltip.add(rewardText(bounty).formatted(Formatting.GOLD));
    }

    static net.minecraft.text.MutableText rewardText(Bounty bounty) {
        return Text.translatable("bounty.dndclasses.reward", bounty.emeralds, bounty.classXp);
    }
}
