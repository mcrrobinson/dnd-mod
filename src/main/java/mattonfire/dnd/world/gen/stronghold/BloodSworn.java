package mattonfire.dnd.world.gen.stronghold;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

/**
 * The Trial of Strength's records, per hold (the stronghold's structure start, {@code ChunkPos.toLong}): who
 * is Blood-Sworn there, and the in-game day each player last lost the Trial there (they may try again the
 * next day). Saved with the world as {@code dndclasses_blood_sworn}.
 */
public class BloodSworn extends PersistentState {
    private static final String ID = DnDClasses.MOD_ID + "_blood_sworn";

    private final Map<Long, Set<UUID>> sworn = new HashMap<>();
    private final Map<Long, Map<UUID, Long>> lost = new HashMap<>();

    public static BloodSworn get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(BloodSworn::fromNbt, BloodSworn::new, ID);
    }

    public boolean isSworn(long hold, UUID player) {
        Set<UUID> players = this.sworn.get(hold);
        return players != null && players.contains(player);
    }

    public void setSworn(long hold, UUID player, boolean sworn) {
        if (sworn) {
            this.sworn.computeIfAbsent(hold, k -> new HashSet<>()).add(player);
        } else if (this.sworn.containsKey(hold)) {
            this.sworn.get(hold).remove(player);
        }
        this.markDirty();
    }

    /** Whether {@code player} lost the Trial at this hold on day {@code today}. */
    public boolean lostOn(long hold, UUID player, long today) {
        Map<UUID, Long> days = this.lost.get(hold);
        return days != null && days.containsKey(player) && days.get(player) == today;
    }

    public void setLost(long hold, UUID player, long day) {
        this.lost.computeIfAbsent(hold, k -> new HashMap<>()).put(player, day);
        this.markDirty();
    }

    /** Forgets a loss, so the player may try again today (the dev command). */
    public void clearLost(long hold, UUID player) {
        Map<UUID, Long> days = this.lost.get(hold);
        if (days != null && days.remove(player) != null) {
            this.markDirty();
        }
    }

    private static BloodSworn fromNbt(NbtCompound nbt) {
        BloodSworn state = new BloodSworn();
        NbtList holds = nbt.getList("Holds", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < holds.size(); i++) {
            NbtCompound entry = holds.getCompound(i);
            long hold = entry.getLong("Hold");
            NbtList sworn = entry.getList("Sworn", NbtElement.INT_ARRAY_TYPE);
            for (NbtElement uuid : sworn) {
                state.sworn.computeIfAbsent(hold, k -> new HashSet<>()).add(NbtHelper.toUuid(uuid));
            }
            NbtList lost = entry.getList("Lost", NbtElement.COMPOUND_TYPE);
            for (int j = 0; j < lost.size(); j++) {
                NbtCompound loss = lost.getCompound(j);
                state.lost.computeIfAbsent(hold, k -> new HashMap<>()).put(loss.getUuid("Player"), loss.getLong("Day"));
            }
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        Set<Long> keys = new HashSet<>(this.sworn.keySet());
        keys.addAll(this.lost.keySet());
        NbtList holds = new NbtList();
        for (long hold : keys) {
            NbtCompound entry = new NbtCompound();
            entry.putLong("Hold", hold);
            NbtList sworn = new NbtList();
            for (UUID uuid : this.sworn.getOrDefault(hold, Set.of())) {
                sworn.add(NbtHelper.fromUuid(uuid));
            }
            entry.put("Sworn", sworn);
            NbtList lost = new NbtList();
            this.lost.getOrDefault(hold, Map.of()).forEach((uuid, day) -> {
                NbtCompound loss = new NbtCompound();
                loss.putUuid("Player", uuid);
                loss.putLong("Day", day);
                lost.add(loss);
            });
            entry.put("Lost", lost);
            holds.add(entry);
        }
        nbt.put("Holds", holds);
        return nbt;
    }
}
