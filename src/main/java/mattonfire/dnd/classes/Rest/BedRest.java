package mattonfire.dnd.classes.Rest;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/**
 * Long rests in beds. A player who gets into bed with a long rest available
 * gets one when either
 * <ul>
 * <li>the night is skipped while they're fully asleep ({@value #FULLY_ASLEEP} ticks, vanilla's
 * {@code isSleepingLongEnough}), or</li>
 * <li>they've been asleep {@link Rule#asleepTicks} in total ({@value #BED_TICKS} ticks, 20 s, for a
 * bed), so a player on a server where not enough people sleep still rests while the night carries
 * on.</li>
 * </ul>
 * Getting up early or being woken by damage gives nothing. The rest counts for the day the player
 * got into bed ({@link Rests#day}), so the night of day N records N and the next night works again.
 * <p>
 * Hooks for later rests: {@link #RULE} lets a system (tavern Room Keys) turn a particular bed into a
 * different kind of rest (source and time asleep), and {@link #onSleepersWoken} is called by
 * {@code ServerWorldMixin} when the world skips the night.
 */
public final class BedRest {
    /** Ticks in bed before a player counts as fully asleep (vanilla's {@code isSleepingLongEnough}). */
    public static final int FULLY_ASLEEP = 100;
    /** Ticks asleep in an ordinary bed that give the rest without the night being skipped (20 s). */
    public static final int BED_TICKS = 20 * 20;
    private static final int HUD_EVERY = 20;

    /** What a bed rest gives: where it came from and how long the player must sleep for it. */
    public record Rule(RestSource source, int asleepTicks) {
        public static final Rule BED = new Rule(RestSource.BED, BED_TICKS);
    }

    /**
     * Asked when a player gets into bed. The first listener to return a {@link Rule} sets the rest
     * that bed gives (a tavern room: {@code new Rule(RestSource.TAVERN, 200)}); null passes to the
     * next, and {@link Rule#BED} is the default.
     */
    public static final Event<BedRule> RULE = EventFactory.createArrayBacked(BedRule.class,
            listeners -> (player, bed) -> {
                for (BedRule listener : listeners) {
                    Rule rule = listener.rule(player, bed);
                    if (rule != null) {
                        return rule;
                    }
                }
                return null;
            });

    @FunctionalInterface
    public interface BedRule {
        /** @return the rest this bed gives the player, or null to leave it to other listeners */
        Rule rule(ServerPlayerEntity player, BlockPos bed);
    }

    /** One player in bed, waiting for their rest. */
    private static final class Session {
        final Rule rule;
        /** The in-game day the player got into bed: the day the rest counts for. */
        final long day;
        int asleepTicks;

        Session(Rule rule, long day) {
            this.rule = rule;
            this.day = day;
        }
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private BedRest() {
    }

    static void register() {
        EntitySleepEvents.START_SLEEPING.register((entity, bed) -> {
            if (entity instanceof ServerPlayerEntity player) {
                start(player, bed);
            }
        });
        EntitySleepEvents.STOP_SLEEPING.register((entity, bed) -> {
            if (entity instanceof ServerPlayerEntity player) {
                stop(player, null);
            }
        });
        // Damage wakes a sleeper (LivingEntity.damage): end the session first, so the wake-up
        // that follows gives nothing and says why.
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (amount > 0 && entity instanceof ServerPlayerEntity player && player.isSleeping()) {
                stop(player, "You were woken up: no long rest.");
            }
            return true;
        });
        ServerTickEvents.END_SERVER_TICK.register(BedRest::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SESSIONS.remove(
                handler.getPlayer().getUuid()));
    }

    /** True while the player is in bed waiting for a long rest. */
    public static boolean isResting(ServerPlayerEntity player) {
        return SESSIONS.containsKey(player.getUuid());
    }

    private static void start(ServerPlayerEntity player, BlockPos bed) {
        SESSIONS.remove(player.getUuid());
        if (player.isSpectator()) {
            return;
        }
        Text refusal = Rests.canLongRest(player);
        if (refusal != null) {
            // Vanilla sleeping (and skipping the night) carries on; only the rest is refused.
            player.sendMessage(refusal.copy().formatted(Formatting.YELLOW), false);
            return;
        }
        Rule rule = RULE.invoker().rule(player, bed);
        SESSIONS.put(player.getUuid(), new Session(rule == null ? Rule.BED : rule, Rests.day(player)));
        RestSync.setSession(player, RestKind.LONG, 0, rule == null ? BED_TICKS : rule.asleepTicks());
    }

    /** Ends the player's session without a rest, telling them why if {@code reason} is set. */
    private static void stop(ServerPlayerEntity player, String reason) {
        Session session = SESSIONS.remove(player.getUuid());
        if (session == null) {
            return;
        }
        RestSync.clearSession(player);
        if (reason == null && session.asleepTicks >= FULLY_ASLEEP) {
            reason = "You got up before you'd rested: no long rest.";
        }
        if (reason != null) {
            player.sendMessage(Text.literal(reason).formatted(Formatting.YELLOW), false);
        }
    }

    private static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Session> entry = it.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null || !player.isSleeping()) {
                it.remove();
                continue;
            }
            Session session = entry.getValue();
            session.asleepTicks++;
            if (session.asleepTicks >= session.rule.asleepTicks()) {
                it.remove();
                finish(player, session);
            } else if (session.asleepTicks % HUD_EVERY == 0) {
                RestSync.setSession(player, RestKind.LONG, session.asleepTicks, session.rule.asleepTicks());
            }
        }
    }

    /**
     * The world is skipping the night and waking everyone in bed (called from
     * {@code ServerWorld.wakeSleepingPlayers}, before anyone wakes): everyone fully asleep rests.
     */
    public static void onSleepersWoken(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            Session session = SESSIONS.get(player.getUuid());
            if (session != null && player.isSleeping() && session.asleepTicks >= FULLY_ASLEEP) {
                SESSIONS.remove(player.getUuid());
                finish(player, session);
            }
        }
    }

    private static void finish(ServerPlayerEntity player, Session session) {
        RestSync.clearSession(player);
        // Re-ask the vetoes (a party member went Downed, a boss fight started) for the day the
        // player got into bed: the morning after a skipped night is already the next day.
        Text refusal = Rests.canLongRest(player, session.day);
        if (refusal != null) {
            player.sendMessage(refusal.copy().formatted(Formatting.YELLOW), false);
            return;
        }
        Rests.complete(player, RestKind.LONG, session.rule.source(), List.of(player), session.day);
    }
}
