package mattonfire.dnd.tavern;

import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.classes.Effects.WellRestedEffect;
import mattonfire.dnd.classes.Registry.ModEffects;
import mattonfire.dnd.classes.Rest.BedRest;
import mattonfire.dnd.classes.Rest.RestEvents;
import mattonfire.dnd.classes.Rest.RestKind;
import mattonfire.dnd.classes.Rest.RestSource;
import mattonfire.dnd.classes.Rest.RestState;
import mattonfire.dnd.classes.Rest.RestSync;
import mattonfire.dnd.classes.Rest.Rests;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Rented tavern rooms. A player holding a valid {@link RoomKeyItem} can sleep in any bed inside the
 * key's inn at any time of day, with monsters about, and without skipping the day. After
 * {@value #ASLEEP_TICKS} ticks asleep they get a long rest ({@link BedRest#RULE}, source
 * {@link RestSource#TAVERN}), the key is used up, and they wake Well Rested: one temporary charge
 * and Absorption I for 10 minutes.
 * <p>
 * The one-a-day long rest rule still applies: a player who's already had one today can't sleep in
 * by day, and keeps the key.
 */
public final class TavernRooms {
    /** Time asleep in a rented room for the rest (10 s). */
    public static final int ASLEEP_TICKS = 200;
    /** Emeralds for a room, before reputation and kin prices. */
    public static final int PRICE = 3;
    /** Persistent-data key: the in-game day the player last rented a room. */
    private static final String RENTED_DAY = "dndRoomKeyDay";

    private TavernRooms() {
    }

    public static void register() {
        BedRest.RULE.register((player, bed) -> keyFor(player, bed) != null
                ? new BedRest.Rule(RestSource.TAVERN, ASLEEP_TICKS) : null);

        EntitySleepEvents.ALLOW_SLEEP_TIME.register((player, pos, vanilla) -> {
            if (vanilla || !(player instanceof ServerPlayerEntity serverPlayer) || keyFor(serverPlayer, pos) == null) {
                return ActionResult.PASS;
            }
            // Asked every tick while asleep: stay in bed only while the rest is under way. Once it's
            // done (or refused at the end), the day wakes the player as usual.
            if (player.isSleeping()) {
                return BedRest.isResting(serverPlayer) ? ActionResult.SUCCESS : ActionResult.PASS;
            }
            // Getting into bed: only with a rest to take, or the key would let you lie in all day.
            Text refusal = Rests.canLongRest(serverPlayer);
            if (refusal != null) {
                player.sendMessage(refusal.copy().formatted(Formatting.YELLOW), false);
                return ActionResult.PASS;
            }
            return ActionResult.SUCCESS;
        });
        EntitySleepEvents.ALLOW_NEARBY_MONSTERS.register((player, pos, vanilla) ->
                !vanilla && player instanceof ServerPlayerEntity serverPlayer && keyFor(serverPlayer, pos) != null
                        ? ActionResult.SUCCESS : ActionResult.PASS);
        // By day a rented room doesn't skip to the next morning (the player doesn't count as
        // "slept long enough" for the night skip); at night it's an ordinary bed for that.
        EntitySleepEvents.ALLOW_RESETTING_TIME.register(player -> !(player.getWorld().isDay()
                && player instanceof ServerPlayerEntity serverPlayer
                && player.getSleepingPosition().map(bed -> keyFor(serverPlayer, bed) != null).orElse(false)));

        RestEvents.AFTER_REST.register((player, kind, source) -> {
            if (kind == RestKind.LONG && source == RestSource.TAVERN) {
                afterRest(player);
            }
        });
        // Well Rested doesn't survive death, so neither does its charge (RestState is copied over).
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!newPlayer.hasStatusEffect(ModEffects.WELL_RESTED)) {
                WellRestedEffect.clearTempCharges(newPlayer);
            }
        });
    }

    /** A valid key in the player's inventory that opens this bed, or null. */
    @Nullable
    public static ItemStack keyFor(ServerPlayerEntity player, BlockPos bed) {
        long today = Rests.day(player);
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (RoomKeyItem.opens(stack, bed, today)) {
                return stack;
            }
        }
        return null;
    }

    /** The key's used up, and the player's Well Rested. */
    private static void afterRest(ServerPlayerEntity player) {
        BlockPos bed = player.getSleepingPosition().orElse(player.getBlockPos());
        ItemStack key = keyFor(player, bed);
        if (key != null) {
            key.decrement(1);
        }
        // The effect first: re-adding it to a player who still has it "removes" the old one, which
        // clears the temporary charge.
        player.addStatusEffect(new StatusEffectInstance(ModEffects.WELL_RESTED, WellRestedEffect.DURATION, 0));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, WellRestedEffect.DURATION, 0));
        RestState state = RestState.get(player);
        state.tempCharges = Math.max(state.tempCharges, 1);
        state.save(player);
        RestSync.sync(player);
        player.sendMessage(Text.translatable("tavern.dndclasses.room.well_rested").formatted(Formatting.GOLD), false);
    }

    /** True if the player has already rented a room today (one key per player per day). */
    public static boolean rentedToday(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return false;
        }
        var data = ((IEntityDataSaver) player).getPersistentData();
        return data.contains(RENTED_DAY) && data.getLong(RENTED_DAY) == Rests.day(serverPlayer);
    }

    /** Records that the player rented a room today. */
    public static void markRented(ServerPlayerEntity player) {
        ((IEntityDataSaver) player).getPersistentData().putLong(RENTED_DAY, Rests.day(player));
    }
}
