package mattonfire.dnd.classes.Downed;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import mattonfire.dnd.classes.DnDClasses;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Feeds the client's {@code DownedHud}: every {@value #INTERVAL} ticks, each player who is Downed or holding
 * use on a Downed ally gets {@link #S2C_DOWNED_HUD}, and one empty packet when they stop.
 * <p>
 * Packet: a flags byte ({@link #FLAG_DOWNED}, {@link #FLAG_STABLE}, {@link #FLAG_HELPED}, {@link #FLAG_HELPING}),
 * then if Downed: passes, fails, ticks left (to the next save or to standing up), give-up ticks held; if helped:
 * the helper's name and their hold ticks; if helping: the target's name, whether they're stable, and the hold
 * ticks. Hold and give-up totals are {@link Stabilise#HOLD_TICKS} and {@link Downed#GIVE_UP_TICKS}.
 */
public final class DownedHudSync {
    public static final Identifier S2C_DOWNED_HUD = new Identifier(DnDClasses.MOD_ID, "downed_hud");
    public static final int INTERVAL = 5;

    public static final int FLAG_DOWNED = 1;
    public static final int FLAG_STABLE = 2;
    public static final int FLAG_HELPED = 4;
    public static final int FLAG_HELPING = 8;

    /** Players who got a non-empty packet last time, so they get one empty packet when it ends. */
    private static final Set<UUID> SHOWING = new HashSet<>();

    private DownedHudSync() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % INTERVAL == 0) {
                sync(server);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SHOWING.remove(handler.getPlayer().getUuid()));
    }

    private static void sync(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PacketByteBuf buf = build(server, player);
            if (buf == null) {
                if (SHOWING.remove(player.getUuid())) {
                    PacketByteBuf empty = PacketByteBufs.create();
                    empty.writeByte(0);
                    ServerPlayNetworking.send(player, S2C_DOWNED_HUD, empty);
                }
                continue;
            }
            SHOWING.add(player.getUuid());
            ServerPlayNetworking.send(player, S2C_DOWNED_HUD, buf);
        }
    }

    /** The player's packet, or null if they're neither Downed nor helping anyone. */
    private static PacketByteBuf build(MinecraftServer server, ServerPlayerEntity player) {
        Downed.Snapshot snapshot = Downed.snapshot(player);
        ServerPlayerEntity helper = null;
        int helperTicks = 0;
        if (snapshot != null) {
            UUID helperId = Stabilise.helperOf(player);
            helper = helperId == null ? null : server.getPlayerManager().getPlayer(helperId);
            if (helper != null) {
                helperTicks = Math.round(Math.max(0.0F, Stabilise.progress(helper)) * Stabilise.HOLD_TICKS);
            }
        }
        UUID targetId = Stabilise.target(player);
        ServerPlayerEntity target = targetId == null ? null : server.getPlayerManager().getPlayer(targetId);
        if (target != null && !Downed.is(target)) {
            target = null;
        }
        if (snapshot == null && target == null) {
            return null;
        }

        int flags = (snapshot != null ? FLAG_DOWNED : 0) | (snapshot != null && snapshot.stable() ? FLAG_STABLE : 0)
                | (helper != null ? FLAG_HELPED : 0) | (target != null ? FLAG_HELPING : 0);
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByte(flags);
        if (snapshot != null) {
            buf.writeByte(snapshot.passes());
            buf.writeByte(snapshot.fails());
            buf.writeVarInt(Math.max(0, snapshot.ticksLeft()));
            buf.writeVarInt(snapshot.giveUpHeld());
        }
        if (helper != null) {
            buf.writeString(helper.getEntityName());
            buf.writeVarInt(helperTicks);
        }
        if (target != null) {
            buf.writeString(target.getEntityName());
            buf.writeBoolean(Downed.isStable(target));
            buf.writeVarInt(Math.round(Math.max(0.0F, Stabilise.progress(player)) * Stabilise.HOLD_TICKS));
        }
        return buf;
    }
}
