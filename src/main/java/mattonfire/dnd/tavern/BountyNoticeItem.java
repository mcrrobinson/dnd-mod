package mattonfire.dnd.tavern;

import net.minecraft.client.item.TooltipContext;
import mattonfire.dnd.dungeon.DungeonState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
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

    /**
     * Using a dungeon notice that isn't bound yet (one from the creative tab or {@code /give}) binds
     * it to the nearest uncleared dungeon of its theme and hands over the map, like taking it from a board.
     */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        Bounty bounty = bounty(stack);
        if (bounty == null || !bounty.isDungeon() || DungeonBounties.isBound(stack)) {
            return TypedActionResult.pass(stack);
        }
        if (world instanceof ServerWorld server && user instanceof ServerPlayerEntity player) {
            DungeonBounties.bindTaken(player, server, player.getBlockPos(), stack);
        }
        return TypedActionResult.success(stack, world.isClient);
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
        if (bounty.isDungeon()) {
            Text name = DungeonBounties.dungeonName(stack);
            if (name != null) {
                tooltip.add(Text.translatable("bounty.dndclasses.dungeon.target", name, Text.translatable(
                        "dungeon.dndclasses.challenge", DungeonState.TIER_NUMERALS[DungeonBounties.tier(stack) - 1]))
                        .formatted(Formatting.AQUA));
            }
        }
        if (isComplete(stack)) {
            tooltip.add(Text.translatable("bounty.dndclasses.complete").formatted(Formatting.GREEN));
        } else if (bounty.isDungeon()) {
            tooltip.add(Text.translatable(DungeonBounties.isBound(stack) ? "bounty.dndclasses.progress.dungeon"
                    : "bounty.dndclasses.progress.dungeon_unbound").formatted(Formatting.YELLOW));
        } else if (bounty.isHunt()) {
            tooltip.add(Text.translatable("bounty.dndclasses.progress.hunt", progress(stack), bounty.count)
                    .formatted(Formatting.YELLOW));
        } else {
            tooltip.add(Text.translatable("bounty.dndclasses.progress.explore").formatted(Formatting.YELLOW));
        }
        tooltip.add((bounty.isDungeon() && DungeonBounties.isBound(stack)
                ? Text.translatable("bounty.dndclasses.reward", DungeonBounties.emeralds(bounty, stack), bounty.classXp)
                : rewardText(bounty)).formatted(Formatting.GOLD));
    }

    static net.minecraft.text.MutableText rewardText(Bounty bounty) {
        if (bounty.isDungeon()) {
            return Text.translatable("bounty.dndclasses.reward.dungeon", bounty.emeralds, bounty.classXp);
        }
        return Text.translatable("bounty.dndclasses.reward", bounty.emeralds, bounty.classXp);
    }
}
