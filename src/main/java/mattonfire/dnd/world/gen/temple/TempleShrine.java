package mattonfire.dnd.world.gen.temple;

import java.util.function.BiConsumer;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.classes.Race.BreathWeapon;
import mattonfire.dnd.classes.Race.DndRace;
import mattonfire.dnd.classes.Race.DragonAncestry;
import mattonfire.dnd.classes.Race.RaceLifecycle;
import mattonfire.dnd.world.gen.RacialHomes;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * The Dragonborn shrines: the three breath altars in a Dragon Temple's sanctum ({@link SanctumPiece}).
 * <ul>
 * <li><b>Blessing of the Ancestors</b>: a Dragonborn who right-clicks the altar of their own ancestry (once
 * per in-game day) is blessed for 20 minutes: Breath Weapon cooldown halved and +2 breath damage, through
 * {@link BreathWeapon#MODIFY}.</li>
 * <li><b>Rite of Rebirth</b>: right-clicking another ancestry's altar while holding 10 diamonds consumes them
 * and changes the player's ancestry to that altar's. It's the only way besides {@code /dndrace set}.</li>
 * </ul>
 * Other races get a line saying the altar doesn't answer them. Stored in persistent data as
 * {@code DndShrineDay} (the day of the last blessing) and {@code DndShrineBlessedUntil} (overworld time).
 */
public final class TempleShrine {
    public static final int BLESSING_TICKS = 20 * 60 * 20;
    public static final float BLESSING_DAMAGE = 2.0F;
    public static final int REBIRTH_DIAMONDS = 10;
    private static final String DAY_KEY = "DndShrineDay";
    private static final String UNTIL_KEY = "DndShrineBlessedUntil";

    /** Quest hooks (area 7): called after a blessing, and after a rebirth with the new ancestry. */
    public static BiConsumer<ServerPlayerEntity, DragonAncestry> onBlessing = (player, ancestry) -> {
    };
    public static BiConsumer<ServerPlayerEntity, DragonAncestry> onRebirth = (player, ancestry) -> {
    };

    private TempleShrine() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer) || player.isSpectator()) {
                return ActionResult.PASS;
            }
            BlockPos pos = hit.getBlockPos();
            DragonAncestry altar = SanctumPiece.altarOf(world.getBlockState(pos));
            if (altar == null || !atAltar(serverPlayer.getWorld(), pos)) {
                return ActionResult.PASS;
            }
            if (hand != Hand.MAIN_HAND) {
                // The main hand already had its turn at this altar.
                return ActionResult.PASS;
            }
            return use(serverPlayer, pos, altar);
        });
        BreathWeapon.MODIFY.register((player, ancestry, stats) -> {
            if (!isBlessed(player)) {
                return stats;
            }
            BreathWeapon.Stats blessed = new BreathWeapon.Stats(stats.damage() + BLESSING_DAMAGE, stats.cooldownTicks() / 2);
            DnDClasses.LOGGER.info("[DragonTemple] Blessing on {}'s breath: damage {} -> {}, cooldown {} -> {} ticks",
                    player.getEntityName(), stats.damage(), blessed.damage(), stats.cooldownTicks(), blessed.cooldownTicks());
            return blessed;
        });
    }

    /** Whether {@code pos} is in the sanctum of a Dragon Temple (where the altars are). */
    public static boolean atAltar(ServerWorld world, BlockPos pos) {
        RacialHomes.Visit visit = RacialHomes.visitOf(world, pos, RacialHomes.DRAGON_TEMPLE);
        return visit != null && visit.pieceAt(pos, DragonTempleStructures.SANCTUM) != null;
    }

    private static ActionResult use(ServerPlayerEntity player, BlockPos pos, DragonAncestry altar) {
        if (RaceLifecycle.activeRaceOf(player) != DndRace.DRAGONBORN) {
            hint(player, Text.translatable("message.dndclasses.shrine.not_kin"));
            return ActionResult.PASS;
        }
        DragonAncestry own = RaceLifecycle.ancestryOf(player);
        if (altar == own) {
            return bless(player, pos, altar);
        }
        return rebirth(player, pos, altar);
    }

    private static ActionResult bless(ServerPlayerEntity player, BlockPos pos, DragonAncestry ancestry) {
        NbtCompound data = data(player);
        long today = player.getServer().getOverworld().getTimeOfDay() / 24000L;
        if (data.contains(DAY_KEY) && data.getLong(DAY_KEY) == today) {
            hint(player, Text.translatable("message.dndclasses.shrine.blessed_today"));
            return ActionResult.SUCCESS;
        }
        data.putLong(DAY_KEY, today);
        data.putLong(UNTIL_KEY, now(player) + BLESSING_TICKS);
        ServerWorld world = player.getWorld();
        world.playSound(null, pos, SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.BLOCKS, 1.0F, 1.2F);
        world.spawnParticles(particle(ancestry), pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D, 30, 0.4D, 0.6D, 0.4D, 0.02D);
        player.sendMessage(Text.translatable("message.dndclasses.shrine.blessed", BLESSING_TICKS / 1200)
                .formatted(Formatting.GOLD), false);
        BreathWeapon.sync(player);
        DnDClasses.LOGGER.info("[DragonTemple] {} received the Blessing of the Ancestors ({}) at {}", player.getEntityName(),
                ancestry.id(), pos.toShortString());
        onBlessing.accept(player, ancestry);
        return ActionResult.SUCCESS;
    }

    private static ActionResult rebirth(ServerPlayerEntity player, BlockPos pos, DragonAncestry ancestry) {
        ItemStack held = player.getMainHandStack();
        if (!held.isOf(Items.DIAMOND) || held.getCount() < REBIRTH_DIAMONDS) {
            hint(player, Text.translatable("message.dndclasses.shrine.rebirth_cost", REBIRTH_DIAMONDS,
                    Text.literal(ancestry.displayName())));
            return ActionResult.SUCCESS;
        }
        if (!player.getAbilities().creativeMode) {
            held.decrement(REBIRTH_DIAMONDS);
        }
        DragonAncestry old = RaceLifecycle.ancestryOf(player);
        // The blessing was the old ancestors'.
        data(player).putLong(UNTIL_KEY, 0L);
        RaceLifecycle.change(player, DndRace.DRAGONBORN, ancestry);
        ServerWorld world = player.getWorld();
        world.playSound(null, pos, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.BLOCKS, 0.6F, 1.4F);
        world.spawnParticles(particle(ancestry), player.getX(), player.getBodyY(0.5D), player.getZ(), 60, 0.5D, 1.0D, 0.5D, 0.05D);
        player.sendMessage(Text.translatable("message.dndclasses.shrine.reborn",
                Text.literal(ancestry.displayName())).formatted(Formatting.GOLD), false);
        DnDClasses.LOGGER.info("[DragonTemple] {} underwent the Rite of Rebirth: {} -> {} at {}", player.getEntityName(),
                old.id(), ancestry.id(), pos.toShortString());
        onRebirth.accept(player, ancestry);
        return ActionResult.SUCCESS;
    }

    /** Whether {@code player} carries the Blessing of the Ancestors now. */
    public static boolean isBlessed(ServerPlayerEntity player) {
        return blessingTicksLeft(player) > 0;
    }

    /** Ticks of blessing left, 0 if none. */
    public static long blessingTicksLeft(ServerPlayerEntity player) {
        long left = data(player).getLong(UNTIL_KEY) - now(player);
        // Longer than a blessing can be means the world clock went back (another save).
        return left <= 0 || left > BLESSING_TICKS ? 0 : left;
    }

    /** Ends the blessing now (admin). */
    public static void clear(ServerPlayerEntity player) {
        data(player).putLong(UNTIL_KEY, 0L);
        data(player).remove(DAY_KEY);
    }

    private static ParticleEffect particle(@Nullable DragonAncestry ancestry) {
        return ancestry == DragonAncestry.FROST ? ParticleTypes.SNOWFLAKE
                : ancestry == DragonAncestry.STORM ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.FLAME;
    }

    private static long now(ServerPlayerEntity player) {
        return player.getServer().getOverworld().getTime();
    }

    private static NbtCompound data(ServerPlayerEntity player) {
        return ((IEntityDataSaver) player).getPersistentData();
    }

    private static void hint(ServerPlayerEntity player, Text text) {
        player.sendMessage(text.copy().formatted(Formatting.GRAY), true);
    }
}
