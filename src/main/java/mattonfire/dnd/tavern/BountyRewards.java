package mattonfire.dnd.tavern;

import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.entity.boss.BossMinions;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.structure.StructureStart;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Keeps bounty notices up to date (kills while carrying a hunt notice, arriving somewhere while
 * carrying an expedition notice) and pays out finished ones.
 */
public final class BountyRewards {
    /** How often (ticks) players carrying expedition notices are checked against structures. */
    private static final int EXPLORE_CHECK_INTERVAL = 20;
    /** Player data: the expeditions (bounty and structure) each player has already finished. */
    private static final String VISITS_KEY = "BountyExpeditions";
    private static final int MAX_VISITS = 128;

    private BountyRewards() {
    }

    public static void register() {
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, killer, killed) -> {
            if (killer instanceof ServerPlayerEntity player) {
                onKill(player, killed);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(BountyRewards::tick);
    }

    /** One kill counts towards one notice: the first unfinished one for that kind of creature. */
    private static void onKill(ServerPlayerEntity player, LivingEntity killed) {
        // Dungeon Masters earn no bounty credit.
        if (BossMinions.givesNothing(killed) || mattonfire.dnd.dm.DungeonMaster.isDm(player)) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            Bounty bounty = BountyNoticeItem.bounty(stack);
            if (bounty == null || !bounty.isHunt() || BountyNoticeItem.isComplete(stack)
                    || !killed.getType().isIn(bounty.targets)) {
                continue;
            }
            int progress = BountyNoticeItem.progress(stack) + 1;
            BountyNoticeItem.setProgress(stack, progress);
            if (progress >= bounty.count) {
                announceComplete(player, bounty);
            } else {
                player.sendMessage(Text.translatable("bounty.dndclasses.progress_update", bounty.title(), progress,
                        bounty.count).formatted(Formatting.YELLOW), true);
            }
            return;
        }
    }

    private static void tick(MinecraftServer server) {
        if (server.getTicks() % EXPLORE_CHECK_INTERVAL != 0) {
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerInventory inventory = player.getInventory();
            for (int i = 0; i < inventory.size(); i++) {
                ItemStack stack = inventory.getStack(i);
                Bounty bounty = BountyNoticeItem.bounty(stack);
                if (bounty == null || bounty.destination == null || BountyNoticeItem.isComplete(stack)) {
                    continue;
                }
                ServerWorld world = player.getWorld();
                StructureStart start = world.getStructureAccessor().getStructureContaining(player.getBlockPos(), bounty.destination);
                if (!start.hasChildren()) {
                    continue;
                }
                // Each place counts once per player for each expedition, however many notices they carry.
                String visit = bounty.id + "@" + start.getPos().toLong();
                if (!markVisited(player, visit)) {
                    player.sendMessage(Text.translatable("bounty.dndclasses.explored_already", bounty.title())
                            .formatted(Formatting.GRAY), true);
                    continue;
                }
                BountyNoticeItem.setProgress(stack, bounty.count);
                announceComplete(player, bounty);
            }
        }
    }

    /**
     * Records that {@code player} finished an expedition {@code visit} (bounty and structure). Returns
     * false if they already had. Only the last {@link #MAX_VISITS} are kept.
     */
    private static boolean markVisited(ServerPlayerEntity player, String visit) {
        NbtCompound data = ((IEntityDataSaver) player).getPersistentData();
        NbtList visits = data.getList(VISITS_KEY, NbtElement.STRING_TYPE);
        for (int i = 0; i < visits.size(); i++) {
            if (visits.getString(i).equals(visit)) {
                return false;
            }
        }
        visits.add(NbtString.of(visit));
        while (visits.size() > MAX_VISITS) {
            visits.remove(0);
        }
        data.put(VISITS_KEY, visits);
        return true;
    }

    private static void announceComplete(ServerPlayerEntity player, Bounty bounty) {
        player.sendMessage(Text.translatable("bounty.dndclasses.completed", bounty.title()).formatted(Formatting.GREEN), false);
        player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 0.6F, 1.0F);
    }

    /**
     * Hands in the notice in the player's hand. Pays out and uses it up if it's done; otherwise
     * tells the player what's left. Returns whether the notice was a finished one.
     */
    public static boolean claim(ServerPlayerEntity player, ItemStack notice, Vec3d where) {
        Bounty bounty = BountyNoticeItem.bounty(notice);
        if (bounty == null) {
            return false;
        }
        if (!BountyNoticeItem.isComplete(notice)) {
            player.sendMessage(Text.translatable("bounty.dndclasses.not_done", bounty.title()).formatted(Formatting.RED), true);
            return false;
        }
        notice.decrement(1);
        ServerWorld world = player.getWorld();

        give(player, new ItemStack(Items.EMERALD, bounty.emeralds));
        LootTable table = world.getServer().getLootManager().getTable(bounty.tier.lootTable);
        List<ItemStack> spoils = table.generateLoot(new LootContext.Builder(world)
                .parameter(LootContextParameters.ORIGIN, where)
                .parameter(LootContextParameters.THIS_ENTITY, player)
                .random(player.getRandom())
                .build(LootContextTypes.GIFT));
        spoils.forEach(stack -> give(player, stack));
        player.addExperience(bounty.xp);
        Progression.addXp(player, bounty.classXp);

        player.sendMessage(Text.translatable("bounty.dndclasses.claimed", bounty.title()).formatted(Formatting.GOLD), false);
        world.playSound(null, where.x, where.y, where.z, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.8F, 1.2F);
        return true;
    }

    private static void give(ServerPlayerEntity player, ItemStack stack) {
        if (!player.giveItemStack(stack)) {
            player.dropItem(stack, false);
        }
    }
}
