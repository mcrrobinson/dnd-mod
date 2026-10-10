package mattonfire.dnd.tavern;

import java.util.List;
import java.util.Optional;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.dungeon.DungeonEvents;
import mattonfire.dnd.dungeon.DungeonMaps;
import mattonfire.dnd.dungeon.DungeonRegistry;
import mattonfire.dnd.dungeon.DungeonState;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Dungeon clearance bounties ({@link Bounty#isDungeon()}). Taking one binds the notice to the
 * nearest uncleared dungeon of its theme within {@link #SEARCH_RADIUS} blocks and hands over a
 * Dungeon Map to it; the notice is done when that dungeon is cleared ({@link DungeonEvents#CLEARED})
 * with its holder inside. The reward grows with the dungeon's Challenge tier.
 *
 * Works for any dungeon theme: a theme with no registered structure just never gets posted.
 */
public final class DungeonBounties {
    /** How far from the board (blocks) a dungeon may be. */
    public static final int SEARCH_RADIUS = 2000;

    // Notice NBT, set when the notice is bound to a dungeon
    static final String START_KEY = "StartKey";
    static final String DIMENSION = "Dimension";
    static final String NAME = "DungeonName";
    static final String TIER = "DungeonTier";

    private DungeonBounties() {
    }

    public static void register() {
        DungeonEvents.CLEARED.register(DungeonBounties::onCleared);
    }

    /**
     * Binds {@code notice} to the nearest uncleared dungeon of its theme near {@code pos} and returns
     * a map to it, or null (and leaves the notice unbound) if there's none in range.
     */
    @Nullable
    public static ItemStack bind(ServerWorld world, BlockPos pos, ItemStack notice) {
        Bounty bounty = BountyNoticeItem.bounty(notice);
        if (bounty == null || !bounty.isDungeon()) {
            return null;
        }
        Optional<DungeonState> found = DungeonRegistry.nearest(world, pos, bounty.dungeon, true, SEARCH_RADIUS);
        if (found.isEmpty()) {
            DnDClasses.LOGGER.info("[Bounty] No uncleared {} within {} blocks of {}", bounty.dungeon.id(), SEARCH_RADIUS,
                    pos.toShortString());
            return null;
        }
        DungeonState dungeon = found.get();
        NbtCompound nbt = notice.getOrCreateNbt();
        nbt.putLong(START_KEY, dungeon.startKey());
        nbt.putString(DIMENSION, world.getRegistryKey().getValue().toString());
        nbt.putString(NAME, Text.Serializer.toJson(dungeon.name()));
        nbt.putInt(TIER, dungeon.tier());
        DnDClasses.LOGGER.info("[Bounty] {} bound to dungeon {} ({}, tier {}) at {}", bounty.id, dungeon.startKey(),
                dungeon.theme().id(), dungeon.tier(), dungeon.entrance().toShortString());
        return DungeonMaps.create(world, dungeon);
    }

    /** Whether the notice has been bound to a dungeon yet. */
    public static boolean isBound(ItemStack notice) {
        NbtCompound nbt = notice.getNbt();
        return nbt != null && nbt.contains(START_KEY);
    }

    /** The bound dungeon's name, or null. */
    @Nullable
    public static Text dungeonName(ItemStack notice) {
        NbtCompound nbt = notice.getNbt();
        if (nbt == null || !nbt.contains(NAME)) {
            return null;
        }
        try {
            return Text.Serializer.fromJson(nbt.getString(NAME));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** The bound dungeon's Challenge tier (1-4), or 1 if it isn't bound. */
    public static int tier(ItemStack notice) {
        NbtCompound nbt = notice.getNbt();
        return nbt == null || !nbt.contains(TIER) ? 1 : Math.max(1, Math.min(4, nbt.getInt(TIER)));
    }

    /** Emeralds paid for {@code notice}: tier-scaled for a dungeon clearance. */
    public static int emeralds(Bounty bounty, ItemStack notice) {
        return bounty.isDungeon() ? bounty.emeraldsFor(tier(notice)) : bounty.emeralds;
    }

    /** Every participant's unfinished notices for this dungeon are done. */
    private static void onCleared(ServerWorld world, DungeonState dungeon, List<ServerPlayerEntity> participants) {
        String dimension = world.getRegistryKey().getValue().toString();
        for (ServerPlayerEntity player : participants) {
            // Dungeon Masters earn no bounty credit.
            if (mattonfire.dnd.dm.DungeonMaster.isDm(player)) {
                continue;
            }
            PlayerInventory inventory = player.getInventory();
            for (int i = 0; i < inventory.size(); i++) {
                ItemStack stack = inventory.getStack(i);
                Bounty bounty = BountyNoticeItem.bounty(stack);
                NbtCompound nbt = stack.getNbt();
                if (bounty == null || !bounty.isDungeon() || BountyNoticeItem.isComplete(stack) || nbt == null
                        || !nbt.contains(START_KEY) || nbt.getLong(START_KEY) != dungeon.startKey()
                        || !dimension.equals(nbt.getString(DIMENSION))) {
                    continue;
                }
                BountyNoticeItem.setProgress(stack, bounty.count);
                DnDClasses.LOGGER.info("[Bounty] {} completed {} by clearing {}", player.getEntityName(), bounty.id,
                        dungeon.startKey());
                player.sendMessage(Text.translatable("bounty.dndclasses.completed", bounty.title()).formatted(Formatting.GREEN), false);
                player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 0.6F, 1.0F);
            }
        }
    }

    /** Gives a freshly taken notice its dungeon and map. False (with a message) if there's no dungeon to send them to. */
    static boolean bindTaken(ServerPlayerEntity player, ServerWorld world, BlockPos pos, ItemStack notice) {
        ItemStack map = bind(world, pos, notice);
        if (map == null) {
            player.sendMessage(Text.translatable("bounty.dndclasses.dungeon.none").formatted(Formatting.GRAY), true);
            return false;
        }
        if (!player.giveItemStack(map)) {
            player.dropItem(map, false);
        }
        Text name = dungeonName(notice);
        if (name != null) {
            player.sendMessage(Text.translatable("bounty.dndclasses.dungeon.bound", name,
                    Text.translatable("dungeon.dndclasses.challenge", DungeonState.TIER_NUMERALS[tier(notice) - 1]))
                    .formatted(Formatting.GRAY, Formatting.ITALIC), false);
        }
        return true;
    }
}
