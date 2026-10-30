package mattonfire.dnd.entity.kobold;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.faction.Faction;
import mattonfire.dnd.faction.FactionIds;
import mattonfire.dnd.faction.Factions;
import mattonfire.dnd.faction.Reputation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/**
 * How the Kobold Tribes judge you by dragons (design 10, 3.2 and 3.5).
 * <ul>
 * <li><b>Tribute</b>: gold left in a Dragon Tribute Bowl: a gold ingot +{@value #INGOT}, a gold block
 * +{@value #BLOCK}, a Kobold Trinket +{@value #TRINKET}, up to {@value #DAILY_CAP} a day
 * ({@code Reputation.Cap.TRIBUTE}). The item is used up; at the cap the bowl refuses it.</li>
 * <li><b>Dragon-slayer dread</b>: killing a dragon in {@code #dndclasses:kobold_dread} (the Lightning Chaser, the
 * Frost Drake, the Ancients) is remembered for {@value #DREAD_TICKS} ticks (3 days, {@code dndKobold.SlewAt} in the
 * player's data): kobolds then need a pack of 4 others to be brave against you, and warn each other. The reputation
 * loss itself comes from the faction file ({@code monster_kill_overrides}).</li>
 * </ul>
 */
public final class KoboldTribute {
    public static final int INGOT = 2;
    public static final int BLOCK = 18;
    public static final int TRINKET = 1;
    public static final int DAILY_CAP = 40;
    public static final long DREAD_TICKS = 72000L;
    static final String SLEW_AT_KEY = "dndKobold.SlewAt";

    private KoboldTribute() {
    }

    public static boolean isTribute(ItemStack stack) {
        return value(stack) > 0;
    }

    private static int value(ItemStack stack) {
        if (stack.isOf(Items.GOLD_INGOT)) {
            return INGOT;
        }
        if (stack.isOf(Items.GOLD_BLOCK)) {
            return BLOCK;
        }
        return stack.isOf(Kobolds.KOBOLD_TRINKET) ? TRINKET : 0;
    }

    /** {@code player} drops one of {@code stack} in the bowl at {@code pos}. */
    public static void offer(ServerPlayerEntity player, ItemStack stack, BlockPos pos) {
        Faction kobolds = Factions.get(FactionIds.KOBOLDS);
        int value = value(stack);
        if (kobolds == null || value <= 0) {
            return;
        }
        int before = Reputation.get(player, kobolds);
        Reputation.change(player, Reputation.Source.TRIBUTE).add(kobolds, value, Reputation.Cap.TRIBUTE, DAILY_CAP).apply();
        int gained = Reputation.get(player, kobolds) - before;
        ServerWorld world = player.getWorld();
        if (gained <= 0 && !Reputation.ignored.test(player)) {
            player.sendMessage(Text.translatable("block.dndclasses.dragon_tribute_bowl.full").formatted(Formatting.GRAY), true);
            DnDClasses.LOGGER.info("[Kobold] {} offers {} at the tribute bowl: the bowl is heaped (daily cap)",
                    player.getEntityName(), stack.getItem());
            return;
        }
        if (!player.getAbilities().creativeMode) {
            stack.decrement(1);
        }
        world.playSound(null, pos, SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.BLOCKS, 1.0F, 1.4F);
        world.spawnParticles(ParticleTypes.WAX_ON, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 6, 0.25D,
                0.1D, 0.25D, 0.0D);
        player.sendMessage(Text.translatable("block.dndclasses.dragon_tribute_bowl.accepted", gained,
                kobolds.displayName()).formatted(Formatting.GOLD), true);
        DnDClasses.LOGGER.info("[Kobold] {} pays tribute ({}): +{} Kobold Tribes", player.getEntityName(),
                stack.getItem(), gained);
    }

    // ---- Dragon-slayer dread ----

    static void markDragonSlayer(ServerPlayerEntity player, LivingEntity dragon) {
        data(player).putLong(SLEW_AT_KEY, player.getWorld().getTime());
        DnDClasses.LOGGER.info("[Kobold] {} slew {}: kobolds dread them for 3 days", player.getEntityName(),
                dragon.getType().getUntranslatedName());
    }

    /** Clears the dread ({@code /rep <player> dndclasses:kobolds reset}). */
    public static void forgetDragonSlayer(ServerPlayerEntity player) {
        data(player).remove(SLEW_AT_KEY);
    }

    /** Killed a dread dragon in the last 3 in-game days. */
    public static boolean isDreaded(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return false;
        }
        NbtCompound data = data(serverPlayer);
        if (!data.contains(SLEW_AT_KEY)) {
            return false;
        }
        long since = serverPlayer.getWorld().getTime() - data.getLong(SLEW_AT_KEY);
        return since >= 0 && since < DREAD_TICKS;
    }

    private static NbtCompound data(ServerPlayerEntity player) {
        return ((IEntityDataSaver) player).getPersistentData();
    }
}
