package mattonfire.dnd.classes.Party;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentState;

/**
 * Server-side party registry. Parties are saved with the world (in the
 * overworld's data folder), invites only live in memory and expire.
 */
public class PartyManager extends PersistentState {
    private static final String SAVE_ID = "dndclasses_parties";

    public static final int MAX_PARTY_SIZE = 8;
    public static final int INVITE_TIMEOUT_TICKS = 20 * 60;
    /** How close (in blocks, same dimension) members must be to share XP. */
    public static final double SHARE_RADIUS = 48;

    private final List<Party> parties = new ArrayList<>();
    private final Map<UUID, Party> byMember = new HashMap<>();
    private final Map<UUID, String> names = new HashMap<>();
    /** invitee -> (inviter -> expiry server tick) */
    private final Map<UUID, LinkedHashMap<UUID, Integer>> invites = new HashMap<>();

    public static PartyManager get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager()
                .getOrCreate(PartyManager::fromNbt, PartyManager::new, SAVE_ID);
    }

    /** True if both entities are players in the same party (and not the same player). */
    public static boolean areInSameParty(@Nullable Entity a, @Nullable Entity b) {
        if (!(a instanceof PlayerEntity pa) || !(b instanceof PlayerEntity pb) || a == b) {
            return false;
        }
        MinecraftServer server = pa.getServer();
        if (server == null) {
            return false;
        }
        Party party = get(server).getParty(pa.getUuid());
        return party != null && party.contains(pb.getUuid());
    }

    /**
     * Online party members (excluding the player) in the same dimension, alive,
     * not spectating and within {@code radius} blocks.
     */
    public static List<ServerPlayerEntity> nearbyMembers(ServerPlayerEntity player, double radius) {
        List<ServerPlayerEntity> result = new ArrayList<>();
        Party party = get(player.getServer()).getParty(player.getUuid());
        if (party == null) {
            return result;
        }
        double radiusSq = radius * radius;
        for (ServerPlayerEntity member : party.getOnlineMembers(player.getServer())) {
            if (member != player && member.getWorld() == player.getWorld() && member.isAlive()
                    && !member.isSpectator() && member.squaredDistanceTo(player) <= radiusSq) {
                result.add(member);
            }
        }
        return result;
    }

    @Nullable
    public Party getParty(UUID player) {
        return byMember.get(player);
    }

    public String getName(UUID player) {
        return names.getOrDefault(player, player.toString().substring(0, 8));
    }

    public void rememberName(ServerPlayerEntity player) {
        String name = player.getEntityName();
        if (!name.equals(names.get(player.getUuid())) && byMember.containsKey(player.getUuid())) {
            names.put(player.getUuid(), name);
            markDirty();
        }
    }

    public Party create(ServerPlayerEntity leader) {
        Party party = new Party(leader.getUuid());
        parties.add(party);
        byMember.put(leader.getUuid(), party);
        names.put(leader.getUuid(), leader.getEntityName());
        invites.remove(leader.getUuid());
        markDirty();
        return party;
    }

    public void join(Party party, ServerPlayerEntity player) {
        party.add(player.getUuid());
        byMember.put(player.getUuid(), party);
        names.put(player.getUuid(), player.getEntityName());
        invites.remove(player.getUuid());
        markDirty();
    }

    /** Removes the player from their party; empty parties are disbanded. Returns the old party. */
    @Nullable
    public Party leave(UUID player) {
        Party party = byMember.remove(player);
        if (party == null) {
            return null;
        }
        party.remove(player);
        if (party.size() == 0) {
            parties.remove(party);
        }
        names.remove(player);
        markDirty();
        return party;
    }

    public void invite(ServerPlayerEntity inviter, ServerPlayerEntity invitee) {
        invites.computeIfAbsent(invitee.getUuid(), k -> new LinkedHashMap<>())
                .put(inviter.getUuid(), inviter.getServer().getTicks() + INVITE_TIMEOUT_TICKS);
    }

    /** Inviters with a live invite for this player, oldest first. */
    public List<UUID> pendingInvites(ServerPlayerEntity invitee) {
        LinkedHashMap<UUID, Integer> pending = invites.get(invitee.getUuid());
        List<UUID> result = new ArrayList<>();
        if (pending == null) {
            return result;
        }
        int now = invitee.getServer().getTicks();
        Iterator<Map.Entry<UUID, Integer>> it = pending.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            Party inviterParty = byMember.get(entry.getKey());
            // Expired, or the inviter has since left their party
            if (entry.getValue() < now || inviterParty == null) {
                it.remove();
            } else {
                result.add(entry.getKey());
            }
        }
        if (pending.isEmpty()) {
            invites.remove(invitee.getUuid());
        }
        return result;
    }

    public void removeInvite(UUID invitee, UUID inviter) {
        LinkedHashMap<UUID, Integer> pending = invites.get(invitee);
        if (pending != null) {
            pending.remove(inviter);
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        for (Party party : parties) {
            NbtList members = new NbtList();
            for (UUID uuid : party.getMembers()) {
                NbtCompound member = new NbtCompound();
                member.putUuid("Id", uuid);
                member.putString("Name", getName(uuid));
                members.add(member);
            }
            NbtCompound partyNbt = new NbtCompound();
            partyNbt.put("Members", members);
            list.add(partyNbt);
        }
        nbt.put("Parties", list);
        return nbt;
    }

    private static PartyManager fromNbt(NbtCompound nbt) {
        PartyManager manager = new PartyManager();
        for (NbtElement element : nbt.getList("Parties", NbtElement.COMPOUND_TYPE)) {
            Party party = null;
            for (NbtElement memberElement : ((NbtCompound) element).getList("Members", NbtElement.COMPOUND_TYPE)) {
                NbtCompound member = (NbtCompound) memberElement;
                UUID uuid = member.getUuid("Id");
                if (manager.byMember.containsKey(uuid)) {
                    continue;
                }
                if (party == null) {
                    party = new Party(uuid);
                    manager.parties.add(party);
                } else {
                    party.add(uuid);
                }
                manager.byMember.put(uuid, party);
                manager.names.put(uuid, member.getString("Name"));
            }
        }
        return manager;
    }
}
