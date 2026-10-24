package mattonfire.dnd.classes.Party;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Downed.Downed;
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

    /** The last party HUD payload each online player got, so an unchanged one isn't sent again. */
    private static final Map<UUID, byte[]> LAST_HUD = new HashMap<>();

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
            // The client forgets its HUD on disconnect, so the next join must send it again.
            LAST_HUD.remove(uuid);
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
     * nearby) the player gets it all. Class XP ({@link #XP_PROGRESSION}) grows
     * first with the Balanced party bonus ({@link PartySynergies#balancedMultiplier}).
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
        // Hirelings' roles count for a Balanced party (class XP only); they take no share.
        List<mattonfire.dnd.entity.HirelingEntity> hirelings = XP_PROGRESSION.equals(channel) && amount > 0
                ? PartySynergies.sharingHirelings(collector, PartyManager.SHARE_RADIUS) : List.of();
        if (nearby.isEmpty() && hirelings.isEmpty() || amount <= 0) {
            grant.accept(collector, amount);
            return;
        }
        nearby.add(0, collector);

        double pool = amount;
        if (XP_PROGRESSION.equals(channel)) {
            // Balanced party: 3+ different primary roles among the sharers (and their hirelings) earn more class XP
            int roles = PartySynergies.distinctRoles(nearby, hirelings);
            double multiplier = PartySynergies.balancedMultiplier(roles);
            pool = amount * multiplier;
            DnDClasses.LOGGER.info("[Party] Balanced party: {} class XP shared by {} with {} role(s){}: x{} = {}",
                    amount, nearby.size(), roles, hirelings.isEmpty() ? "" : " (hirelings: "
                            + hirelings.stream().map(h -> h.firstName() + " " + h.role().id()).toList() + ")",
                    multiplier, pool);
        }

        double share = pool / nearby.size();
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

    /**
     * Sends every online party member the health, class and Downed status of the rest of their party,
     * when it differs from what they last got.
     */
    public static void syncHud(MinecraftServer server) {
        PartyManager manager = PartyManager.get(server);
        // Dungeon Masters don't show on the party HUD.
        DungeonMaster dm = DungeonMaster.get(server);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            Party party = manager.getParty(player.getUuid());
            if (party == null) {
                continue;
            }
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
                // Downed status (Downed bits: 1 downed, 2 stable) and the death save tally, for the party HUD
                // and the ally outline (the client matches the UUID against the players it can see).
                buf.writeUuid(uuid);
                Downed.Snapshot downed = member == null ? null : Downed.snapshot(member);
                buf.writeByte(downed == null ? 0 : Downed.BIT_DOWNED | (downed.stable() ? Downed.BIT_STABLE : 0));
                buf.writeByte(downed == null ? 0 : downed.passes());
                buf.writeByte(downed == null ? 0 : downed.fails());
            }
            sendHud(player, buf);
        }
    }

    /** Clears the party HUD of a player that is no longer in a party. */
    public static void clearHud(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(0);
        sendHud(player, buf);
    }

    /** Sends a party HUD payload unless it's the same as the last one this player got. */
    private static void sendHud(ServerPlayerEntity player, PacketByteBuf buf) {
        byte[] payload = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), payload);
        byte[] last = LAST_HUD.put(player.getUuid(), payload);
        if (last == null || !Arrays.equals(last, payload)) {
            ServerPlayNetworking.send(player, S2C_PARTY_HUD, buf);
        }
    }
}
