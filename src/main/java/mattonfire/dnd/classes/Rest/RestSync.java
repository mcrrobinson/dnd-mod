package mattonfire.dnd.classes.Rest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Progression.Progression;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Sends each player their {@link RestSnapshot}: on change, on join and on
 * respawn, and once a second if anything moved (level-ups, the gamerule).
 */
public final class RestSync {
    public static final Identifier S2C_REST_STATE = new Identifier(DnDClasses.MOD_ID, "rest_state");

    /** The rest in progress per player (kind ordinal + 1, progress, total), set by rest sessions. */
    private static final Map<UUID, int[]> SESSIONS = new HashMap<>();
    private static final Map<UUID, RestSnapshot> LAST_SENT = new HashMap<>();

    private RestSync() {
    }

    /** Shows a rest in progress on the player's HUD; call each time the progress changes. */
    public static void setSession(ServerPlayerEntity player, RestKind kind, int progress, int total) {
        SESSIONS.put(player.getUuid(), new int[] { kind.ordinal() + 1, progress, total });
        sync(player);
    }

    public static void clearSession(ServerPlayerEntity player) {
        if (SESSIONS.remove(player.getUuid()) != null) {
            sync(player);
        }
    }

    public static RestSnapshot snapshot(ServerPlayerEntity player) {
        RestState state = RestState.get(player);
        int[] session = SESSIONS.getOrDefault(player.getUuid(), new int[3]);
        return new RestSnapshot(DndRules.rests(player.getWorld()), state.charges, Charges.max(player),
                state.tempCharges, Charges.group(Progression.classOf(player)).ordinal(),
                HitDice.remaining(player, state), HitDice.max(player), HitDice.dieSize(Progression.classOf(player)),
                Math.max(0, Rests.SHORT_RESTS_PER_LONG - state.shortRestsSinceLong), session[0], session[1],
                session[2]);
    }

    public static void sync(ServerPlayerEntity player) {
        RestSnapshot snapshot = snapshot(player);
        LAST_SENT.put(player.getUuid(), snapshot);
        PacketByteBuf buf = PacketByteBufs.create();
        snapshot.write(buf);
        ServerPlayNetworking.send(player, S2C_REST_STATE, buf);
    }

    static void syncChanged(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!snapshot(player).equals(LAST_SENT.get(player.getUuid()))) {
                sync(player);
            }
        }
    }

    static void forget(ServerPlayerEntity player) {
        SESSIONS.remove(player.getUuid());
        LAST_SENT.remove(player.getUuid());
    }
}
