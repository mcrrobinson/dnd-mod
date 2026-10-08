package mattonfire.dnd.classes.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

/**
 * Blocks a staff has cleared or frozen, waiting to be put back. Kept per world as a
 * {@link PersistentState}, so pending restores survive a server stop and never leak
 * into another world or save.
 */
public class ScheduledBlockRestore extends PersistentState {
    private static final String ID = "dndclasses_staff_restores";

    /**
     * Flags for staff block changes: tell clients, but don't update neighbours. Nothing
     * around reacts (no doors or torches popping off as items, no sand falling in), so
     * putting the blocks back leaves the area exactly as it was.
     */
    public static final int FLAGS = Block.NOTIFY_LISTENERS | Block.FORCE_STATE | Block.SKIP_DROPS;

    private final List<Task> tasks = new ArrayList<>();

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> get(world).tick(world));
    }

    public static ScheduledBlockRestore get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(ScheduledBlockRestore::fromNbt, ScheduledBlockRestore::new, ID);
    }

    /**
     * Restores {@code blocks} after {@code delayTicks}, spread over {@code restoreTicks}.
     * A block is only put back if its spot still holds {@code placeholder} or something
     * replaceable (air, fire, water), so anything placed there in the meantime is kept.
     */
    public static void schedule(ServerWorld world, Map<BlockPos, BlockState> blocks, BlockState placeholder,
            int delayTicks, int restoreTicks) {
        if (blocks.isEmpty())
            return;
        Task task = new Task();
        task.blocks.addAll(blocks.entrySet());
        Collections.shuffle(task.blocks);
        task.placeholder = placeholder;
        task.ticks = delayTicks;
        task.blocksPerTick = Math.max(1, (task.blocks.size() + restoreTicks - 1) / Math.max(1, restoreTicks));
        ScheduledBlockRestore state = get(world);
        state.tasks.add(task);
        state.markDirty();
    }

    private void tick(ServerWorld world) {
        if (tasks.isEmpty())
            return;
        Iterator<Task> it = tasks.iterator();
        while (it.hasNext()) {
            if (it.next().tick(world))
                it.remove();
        }
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        for (Task task : tasks) {
            NbtCompound t = new NbtCompound();
            t.putInt("Ticks", task.ticks);
            t.putInt("PerTick", task.blocksPerTick);
            t.put("Placeholder", NbtHelper.fromBlockState(task.placeholder));
            NbtList blocks = new NbtList();
            for (Map.Entry<BlockPos, BlockState> e : task.blocks) {
                NbtCompound b = new NbtCompound();
                b.put("Pos", NbtHelper.fromBlockPos(e.getKey()));
                b.put("State", NbtHelper.fromBlockState(e.getValue()));
                blocks.add(b);
            }
            t.put("Blocks", blocks);
            list.add(t);
        }
        nbt.put("Tasks", list);
        return nbt;
    }

    private static ScheduledBlockRestore fromNbt(NbtCompound nbt) {
        ScheduledBlockRestore state = new ScheduledBlockRestore();
        var blockLookup = Registries.BLOCK.getReadOnlyWrapper();
        NbtList list = nbt.getList("Tasks", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound t = list.getCompound(i);
            Task task = new Task();
            task.ticks = t.getInt("Ticks");
            task.blocksPerTick = Math.max(1, t.getInt("PerTick"));
            task.placeholder = NbtHelper.toBlockState(blockLookup, t.getCompound("Placeholder"));
            NbtList blocks = t.getList("Blocks", NbtElement.COMPOUND_TYPE);
            for (int j = 0; j < blocks.size(); j++) {
                NbtCompound b = blocks.getCompound(j);
                task.blocks.add(Map.entry(NbtHelper.toBlockPos(b.getCompound("Pos")),
                        NbtHelper.toBlockState(blockLookup, b.getCompound("State"))));
            }
            state.tasks.add(task);
        }
        return state;
    }

    private static class Task {
        final List<Map.Entry<BlockPos, BlockState>> blocks = new ArrayList<>();
        BlockState placeholder;
        int ticks;
        int blocksPerTick;

        /** Counts down, then restores a batch per tick. Returns true when finished. */
        boolean tick(ServerWorld world) {
            if (--ticks > 0)
                return false;
            for (int i = 0; i < blocksPerTick && !blocks.isEmpty(); i++) {
                Map.Entry<BlockPos, BlockState> entry = blocks.remove(blocks.size() - 1);
                BlockState current = world.getBlockState(entry.getKey());
                if (current == placeholder || current.getMaterial().isReplaceable())
                    world.setBlockState(entry.getKey(), entry.getValue(), FLAGS);
            }
            return blocks.isEmpty();
        }
    }
}
