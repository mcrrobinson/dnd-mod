package mattonfire.dnd.classes.Rest;

import java.util.List;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.ManaManager;
import mattonfire.dnd.classes.Progression.ClassSkills;
import mattonfire.dnd.classes.Progression.ClassTrees;
import mattonfire.dnd.classes.Progression.Progression;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Short and long rests: the limits ({@link #canShortRest}, {@link #canLongRest})
 * and the benefits ({@link #complete}). The triggers (campfires, beds, tavern
 * rooms, camps) call these; {@code /dndclass rest} calls {@link #complete}
 * directly.
 */
public final class Rests {
    public static final int SHORT_RESTS_PER_LONG = 2;
    /** World ticks between the end of one short rest and the start of the next (3 minutes). */
    public static final int SHORT_REST_COOLDOWN = 3 * 60 * 20;
    public static final long TICKS_PER_DAY = 24000L;

    /** Cleared by a long rest; Bad Omen and curses stay. */
    private static final List<StatusEffect> LONG_REST_CLEARS = List.of(StatusEffects.POISON, StatusEffects.WITHER,
            StatusEffects.HUNGER, StatusEffects.WEAKNESS, StatusEffects.SLOWNESS, StatusEffects.MINING_FATIGUE);

    private Rests() {
    }

    public static void register() {
        DndRules.register();
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 20 != 0) {
                return;
            }
            Charges.trickleSecond(server);
            RestSync.syncChanged(server);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> RestSync.sync(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> RestSync.forget(handler.getPlayer()));
        // ClassLifecycle copies dndRest to the new entity; the client has a new player too.
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> RestSync.sync(newPlayer));
    }

    /** Today's in-game day number, from the overworld clock. */
    public static long day(ServerPlayerEntity player) {
        return player.getServer().getOverworld().getTimeOfDay() / TICKS_PER_DAY;
    }

    private static long now(ServerPlayerEntity player) {
        return player.getServer().getOverworld().getTime();
    }

    /** Why the player can't start a short rest now, or null if they can. */
    public static Text canShortRest(ServerPlayerEntity player) {
        RestState state = RestState.get(player);
        if (state.shortRestsSinceLong >= SHORT_RESTS_PER_LONG) {
            return Text.literal("You've had " + SHORT_RESTS_PER_LONG + " short rests: you need a long rest first.");
        }
        if (state.lastShortRestEnd > 0) {
            long wait = state.lastShortRestEnd + SHORT_REST_COOLDOWN - now(player);
            if (wait > 0) {
                long seconds = (wait + 19) / 20;
                return Text.literal(String.format("You can short rest again in %d:%02d", seconds / 60,
                        seconds % 60));
            }
        }
        return RestEvents.ALLOW_REST.invoker().refuse(player, RestKind.SHORT);
    }

    /** Why the player can't start a long rest now, or null if they can. */
    public static Text canLongRest(ServerPlayerEntity player) {
        if (day(player) <= RestState.get(player).lastLongRestDay) {
            return Text.literal("You've already had a long rest today.");
        }
        return RestEvents.ALLOW_REST.invoker().refuse(player, RestKind.LONG);
    }

    /** Applies a rest's benefits on its own, counting today as the day it started. */
    public static void complete(ServerPlayerEntity player, RestKind kind, RestSource source) {
        complete(player, kind, source, List.of(player), day(player));
    }

    /**
     * Applies a rest's benefits. Doesn't check the limits: call {@link #canShortRest}
     * or {@link #canLongRest} when the rest starts.
     *
     * @param companions everyone resting together, the player included (Song of Rest)
     * @param startedDay the in-game day the rest started, recorded for the one-a-day rule. A bed
     *                   rest that skips the night must pass the evening's day, not the morning's
     */
    public static void complete(ServerPlayerEntity player, RestKind kind, RestSource source,
            List<ServerPlayerEntity> companions, long startedDay) {
        RestState state = RestState.get(player);
        int max = Charges.max(player);
        int chargesBefore = state.charges;
        ClassSkills skills = ClassTrees.skills(Progression.classOf(player));
        Text hitDiceLine = null;
        int hitDiceBack = 0;

        if (kind == RestKind.SHORT) {
            int gained = skills == null ? 0 : skills.shortRestCharges(player, state, max);
            state.charges = Math.min(max, state.charges + Math.max(0, gained));
            hitDiceLine = HitDice.spendOnShortRest(player, state);
            state.shortRestsSinceLong++;
            state.lastShortRestEnd = now(player);
        } else {
            player.setHealth(player.getMaxHealth());
            state.charges = max;
            hitDiceBack = HitDice.restoreOnLongRest(player, state);
            state.shortRestsSinceLong = 0;
            state.wizardRecoveryUsed = false;
            state.lastLongRestDay = Math.max(state.lastLongRestDay, startedDay);
            for (StatusEffect effect : LONG_REST_CLEARS) {
                player.removeStatusEffect(effect);
            }
        }
        state.save(player);

        ManaManager.setMana(player, DnDClasses.MANA_ICONS);
        ManaManager.sync(player);
        if (kind == RestKind.SHORT && skills != null) {
            skills.onShortRest(player, companions);
        }

        MutableText summary = Text.literal(kind == RestKind.SHORT ? "Short rest: " : "Long rest: ")
                .formatted(Formatting.GOLD);
        StringBuilder detail = new StringBuilder(kind == RestKind.LONG ? "health, mana" : "mana restored");
        if (kind == RestKind.LONG) {
            detail.append(max > 0 ? " and charges restored" : " restored");
            if (hitDiceBack > 0) {
                detail.append(", ").append(hitDiceBack).append(" Hit ").append(hitDiceBack == 1 ? "Die" : "Dice")
                        .append(" back");
            }
        } else if (max > 0 && DndRules.rests(player.getWorld())) {
            detail.append(", ").append(state.charges - chargesBefore).append(" charge")
                    .append(state.charges - chargesBefore == 1 ? "" : "s").append(" back");
        }
        player.sendMessage(summary.append(Text.literal(detail.toString()).formatted(Formatting.YELLOW)), false);
        if (hitDiceLine != null) {
            player.sendMessage(hitDiceLine, false);
        }

        RestSync.sync(player);
        RestEvents.AFTER_REST.invoker().afterRest(player, kind, source);
    }
}
