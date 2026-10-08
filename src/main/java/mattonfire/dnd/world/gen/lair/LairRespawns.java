package mattonfire.dnd.world.gen.lair;

import java.util.HashMap;
import java.util.Map;
import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

/**
 * When each dragon lair's Lightning Chaser was last killed, so the lair's spawn override waits
 * {@link #RESPAWN_COOLDOWN} ticks before it sends a new one instead of one every creature spawn cycle.
 */
public class LairRespawns extends PersistentState {
    private static final String ID = DnDClasses.MOD_ID + "_lair_respawns";
    /** Three in-game days. */
    public static final long RESPAWN_COOLDOWN = 24000L * 3;

    /** Lair centre (BlockPos.asLong) -> game time its chaser was killed. */
    private final Map<Long, Long> lastKill = new HashMap<>();

    public static LairRespawns get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(LairRespawns::fromNbt, LairRespawns::new, ID);
    }

    public void onChaserKilled(BlockPos lair, long time) {
        this.lastKill.put(lair.asLong(), time);
        this.markDirty();
    }

    /** False while the lair is still waiting after its last chaser was killed. */
    public boolean canRespawn(BlockPos lair, long time) {
        Long killed = this.lastKill.get(lair.asLong());
        return killed == null || time - killed >= RESPAWN_COOLDOWN || time < killed;
    }

    private static LairRespawns fromNbt(NbtCompound nbt) {
        LairRespawns state = new LairRespawns();
        NbtList list = nbt.getList("Lairs", NbtCompound.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            state.lastKill.put(entry.getLong("Pos"), entry.getLong("Killed"));
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        this.lastKill.forEach((pos, time) -> {
            NbtCompound entry = new NbtCompound();
            entry.putLong("Pos", pos);
            entry.putLong("Killed", time);
            list.add(entry);
        });
        nbt.put("Lairs", list);
        return nbt;
    }
}
