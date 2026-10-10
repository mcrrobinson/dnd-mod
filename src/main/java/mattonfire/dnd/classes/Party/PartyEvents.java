package mattonfire.dnd.classes.Party;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.dm.DungeonMaster;
import mattonfire.dnd.classes.PlayerEntityExt;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Party gameplay: no friendly fire, shared XP and the party HUD sync.
 */
public class PartyEvents {
    public static final Identifier S2C_PARTY_HUD = new Identifier(DnDClasses.MOD_ID, "party_hud");

    private static final int HUD_SYNC_INTERVAL_TICKS = 10;

    /** XP channel for vanilla experience orbs. */
    public static final String XP_VANILLA = "vanilla";
    /** XP channel for class progression XP from kills. */
    public static final String XP_PROGRESSION = "progression";

    /** Fractional XP left over from splitting, per channel and player, so small orbs aren't lost. */
    private static final Map<String, Map<UUID, Double>> XP_REMAINDER = new HashMap<>();

    public static void register() {
        // No friendly fire: party members (and their pets) can't hurt each other,
        // including with arrows, tridents, fireballs and thrown potions.
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof PlayerEntity)) {
                return true;
            }
            return !PartyManager.areInSameParty(entity, responsiblePlayer(source.getAttacker()));
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % HUD_SYNC_INTERVAL_TICKS == 0) {
                syncHud(server);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            PartyManager.get(server).rememberName(handler.getPlayer());
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.getPlayer().getUuid();
            for (Map<UUID, Double> remainders : XP_REMAINDER.values()) {
                remainders.remove(uuid);
            }
        });
    }

    private static Entity responsiblePlayer(Entity attacker) {
        if (attacker instanceof TameableEntity tameable && tameable.getOwner() != null) {
            return tameable.getOwner();
        }
        return attacker;
    }

    /**
     * Splits {@code amount} XP evenly between the player and their party members
     * within {@link PartyManager#SHARE_RADIUS} blocks. With no party (or nobody
     * nearby) the player gets it all.
     *
     * Vanilla XP orbs go through this with {@link #XP_VANILLA}, and class XP from
     * kills with {@link #XP_PROGRESSION} ({@code ProgressionEvents}). Other XP
     * systems call it with their own channel name and a grant function.
     */
    public static void shareXp(ServerPlayerEntity collector, int amount, String channel,
            BiConsumer<ServerPlayerEntity, Integer> grant) {
        List<ServerPlayerEntity> nearby = PartyManager.nearbyMembers(collector, PartyManager.SHARE_RADIUS);
        // A Dungeon Master takes no share of the party's XP.
        nearby.removeIf(DungeonMaster::isDm);
        if (nearby.isEmpty() || amount <= 0) {
            grant.accept(collector, amount);
            return;
        }
        nearby.add(0, collector);

        double share = (double) amount / nearby.size();
        Map<UUID, Double> remainders = XP_REMAINDER.computeIfAbsent(channel, k -> new HashMap<>());
        for (ServerPlayerEntity member : nearby) {
            double total = share + remainders.getOrDefault(member.getUuid(), 0.0);
            int whole = (int) Math.floor(total);
            remainders.put(member.getUuid(), total - whole);
            if (whole > 0) {
                grant.accept(member, whole);
            }
        }
    }

    /** Sends every online party member the health and class of the rest of their party. */
    public static void syncHud(MinecraftServer server) {
        PartyManager manager = PartyManager.get(server);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            Party party = manager.getParty(player.getUuid());
            if (party == null) {
                continue;
            }
            // Dungeon Masters don't show on the party HUD.
            DungeonMaster dm = DungeonMaster.get(server);
            List<UUID> shown = party.getMembers().stream()
                    .filter(uuid -> !uuid.equals(player.getUuid()) && !dm.isDm(uuid)).toList();
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeVarInt(shown.size());
            for (UUID uuid : shown) {
                ServerPlayerEntity member = server.getPlayerManager().getPlayer(uuid);
                buf.writeString(manager.getName(uuid));
                buf.writeBoolean(party.isLeader(uuid));
                buf.writeBoolean(member != null);
                buf.writeFloat(member != null ? member.getHealth() : 0);
                buf.writeFloat(member != null ? member.getMaxHealth() : 20);
                buf.writeFloat(member != null ? member.getAbsorptionAmount() : 0);
                buf.writeBoolean(member != null && member.getWorld() == player.getWorld()
                        && member.squaredDistanceTo(player) <= PartyManager.SHARE_RADIUS * PartyManager.SHARE_RADIUS);
                // Class id (DndCharacter value, 0 when offline or unknown) for the role icon.
                buf.writeVarInt(member instanceof PlayerEntityExt ext && ext.getDndClass() != null
                        ? ext.getDndClass().getValue() : 0);
            }
            ServerPlayNetworking.send(player, S2C_PARTY_HUD, buf);
        }
    }

    /** Clears the party HUD of a player that is no longer in a party. */
    public static void clearHud(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(0);
        ServerPlayNetworking.send(player, S2C_PARTY_HUD, buf);
    }
}
