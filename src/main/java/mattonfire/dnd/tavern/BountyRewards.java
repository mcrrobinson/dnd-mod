package mattonfire.dnd.tavern;

import mattonfire.dnd.classes.DnDClasses;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Keeps bounty notices up to date (kills while carrying a hunt notice, arriving somewhere while
 * carrying an expedition notice) and pays out finished ones.
 */
public final class BountyRewards {
    /** How often (ticks) players carrying expedition notices are checked against structures. */
    private static final int EXPLORE_CHECK_INTERVAL = 20;

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

    private static void onKill(ServerPlayerEntity player, LivingEntity killed) {
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
                if (world.getStructureAccessor().getStructureContaining(player.getBlockPos(), bounty.destination).hasChildren()) {
                    BountyNoticeItem.setProgress(stack, bounty.count);
                    announceComplete(player, bounty);
                }
            }
        }
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
        grantProgressionXp(player, bounty.classXp);

        player.sendMessage(Text.translatable("bounty.dndclasses.claimed", bounty.title()).formatted(Formatting.GOLD), false);
        world.playSound(null, where.x, where.y, where.z, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.8F, 1.2F);
        return true;
    }

    private static void give(ServerPlayerEntity player, ItemStack stack) {
        if (!player.giveItemStack(stack)) {
            player.dropItem(stack, false);
        }
    }

    // ---- Class progression ----

    private static Method addXp;
    private static boolean addXpLooked;

    /**
     * Grants class-progression XP for a bounty.
     * <p>
     * TODO(class-progression): class progression lives on the {@code feat/class-progression}
     * branch and isn't on main yet. Once it lands, replace this lookup with a direct call to
     * {@code mattonfire.dnd.classes.Progression.Progression.addXp(player, amount)}. Until then the
     * call is looked up by name, so bounties start paying class XP as soon as that code exists,
     * and are a no-op (vanilla XP and loot only) without it.
     */
    static void grantProgressionXp(ServerPlayerEntity player, int amount) {
        if (amount <= 0) {
            return;
        }
        if (!addXpLooked) {
            addXpLooked = true;
            try {
                addXp = Class.forName("mattonfire.dnd.classes.Progression.Progression")
                        .getMethod("addXp", ServerPlayerEntity.class, int.class);
            } catch (ReflectiveOperationException e) {
                DnDClasses.LOGGER.info("Class progression not present; bounties give no class XP");
            }
        }
        if (addXp != null) {
            try {
                addXp.invoke(null, player, amount);
            } catch (ReflectiveOperationException e) {
                DnDClasses.LOGGER.warn("Couldn't grant bounty class XP", e);
            }
        }
    }

}
