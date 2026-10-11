package mattonfire.dnd.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

/**
 * Forgiveness a fortress owes but couldn't hand out yet. Clearing a Dwarven Ruin forgives the player at
 * every dwarf of the nearest fortress ({@link DwarfGrudges}), but that fortress is usually unloaded while
 * the player is in the Ruin, and an unloaded dwarf's anger doesn't run down. So the clear is recorded
 * here (fortress start, player, game time) and each dwarf checks it when it loads
 * ({@link MountainDwarfEntity}): a grudge it held when it was last saved, against a player forgiven since,
 * is dropped.
 *
 * <p>One entry per fortress and player: a later clear only moves its time forward.
 */
public final class FortressForgiveness extends PersistentState {
    private static final String ID = DnDClasses.MOD_ID + "_fortress_forgiveness";

    private record Entry(BlockPos fortress, UUID player, long time) {
    }

    private final List<Entry> entries = new ArrayList<>();

    public static FortressForgiveness get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(FortressForgiveness::fromNbt, FortressForgiveness::new, ID);
    }

    /** The dwarves of the fortress starting at {@code fortress} forgive {@code player} as of now. */
    public void record(ServerWorld world, BlockPos fortress, UUID player) {
        this.entries.removeIf(entry -> entry.fortress().equals(fortress) && entry.player().equals(player));
        this.entries.add(new Entry(fortress.toImmutable(), player, world.getTime()));
        this.markDirty();
    }

    /**
     * Whether {@code player} was forgiven by the fortress a dwarf at {@code pos} belongs to (within
     * {@link DwarfGrudges#FORTRESS_RADIUS} of its start, horizontally) at or after game time {@code since}.
     */
    public boolean forgivenSince(BlockPos pos, UUID player, long since) {
        for (Entry entry : this.entries) {
            if (entry.player().equals(player) && entry.time() >= since
                    && Math.abs(pos.getX() - entry.fortress().getX()) <= DwarfGrudges.FORTRESS_RADIUS
                    && Math.abs(pos.getZ() - entry.fortress().getZ()) <= DwarfGrudges.FORTRESS_RADIUS) {
                return true;
            }
        }
        return false;
    }

    private static FortressForgiveness fromNbt(NbtCompound nbt) {
        FortressForgiveness state = new FortressForgiveness();
        for (NbtElement element : nbt.getList("Entries", NbtElement.COMPOUND_TYPE)) {
            NbtCompound entry = (NbtCompound) element;
            if (entry.containsUuid("Player")) {
                state.entries.add(new Entry(BlockPos.fromLong(entry.getLong("Fortress")), entry.getUuid("Player"),
                        entry.getLong("Time")));
            }
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        for (Entry entry : this.entries) {
            NbtCompound tag = new NbtCompound();
            tag.putLong("Fortress", entry.fortress().asLong());
            tag.putUuid("Player", entry.player());
            tag.putLong("Time", entry.time());
            list.add(tag);
        }
        nbt.put("Entries", list);
        return nbt;
    }
}
