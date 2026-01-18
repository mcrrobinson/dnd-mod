package mattonfire.dnd.classes.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ScheduledBlockRestore {
    int ticks;
    List<Map.Entry<BlockPos, BlockState>> blocks;
    RegistryKey<World> worldKey;
    int blocksPerTick;

    ScheduledBlockRestore(ServerWorld world, Map<BlockPos, BlockState> blocks, int ticks) {
        this.worldKey = world.getRegistryKey();
        this.blocks = new ArrayList<>(blocks.entrySet());
        Collections.shuffle(this.blocks); // Randomize the order
        this.ticks = ticks;
        this.blocksPerTick = Math.max(1, this.blocks.size() / ticks); // Calculate blocks per tick
    }

    // Alternative constructor with custom blocks per tick
    ScheduledBlockRestore(ServerWorld world, Map<BlockPos, BlockState> blocks, int ticks, int blocksPerTick) {
        this.worldKey = world.getRegistryKey();
        this.blocks = new ArrayList<>(blocks.entrySet());
        Collections.shuffle(this.blocks);
        this.ticks = ticks;
        this.blocksPerTick = blocksPerTick;
    }

    boolean restore(MinecraftServer server) {
        ServerWorld world = server.getWorld(worldKey);
        if (world == null) {
            return true; // Mark as complete if world doesn't exist
        }

        // Restore a batch of blocks
        int blocksToRestore = Math.min(blocksPerTick, blocks.size());
        for (int i = 0; i < blocksToRestore; i++) {
            if (!blocks.isEmpty()) {
                Map.Entry<BlockPos, BlockState> entry = blocks.remove(blocks.size() - 1);
                world.setBlockState(entry.getKey(), entry.getValue());
            }
        }

        // Return true if all blocks are restored
        boolean isComplete = blocks.isEmpty();
        if (isComplete) {
            System.out.println("[BEAM] All blocks restored.");
        }

        return isComplete;
    }
}