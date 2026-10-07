package mattonfire.dnd.classes.Party;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * A group of players. The first member is the leader; if the leader leaves, the
 * next member in join order takes over.
 */
public class Party {
    private final LinkedHashSet<UUID> members = new LinkedHashSet<>();

    Party(UUID leader) {
        members.add(leader);
    }

    public UUID getLeader() {
        return members.iterator().next();
    }

    public boolean isLeader(UUID player) {
        return !members.isEmpty() && getLeader().equals(player);
    }

    public Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public boolean contains(UUID player) {
        return members.contains(player);
    }

    public int size() {
        return members.size();
    }

    void add(UUID player) {
        members.add(player);
    }

    void remove(UUID player) {
        members.remove(player);
    }

    /** Members that are currently online, leader first. */
    public List<ServerPlayerEntity> getOnlineMembers(MinecraftServer server) {
        List<ServerPlayerEntity> online = new ArrayList<>();
        for (UUID uuid : members) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player != null) {
                online.add(player);
            }
        }
        return online;
    }
}
