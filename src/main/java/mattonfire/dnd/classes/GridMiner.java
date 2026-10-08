package mattonfire.dnd.classes;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import mattonfire.dnd.classes.Registry.ModEnchantments;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Grid Miner: breaking a block with a pickaxe or shovel also mines the connected blocks the
 * tool is right for, nearest first. Level I mines up to {@link #MAX_BLOCKS_LEVEL_1} extra
 * blocks within {@link #MAX_STEPS_LEVEL_1} steps, level II up to {@link #MAX_BLOCKS_LEVEL_2}
 * within {@link #MAX_STEPS_LEVEL_2}.
 */
public class GridMiner {
    public static final int MAX_STEPS_LEVEL_1 = 2;
    public static final int MAX_BLOCKS_LEVEL_1 = 16;
    public static final int MAX_STEPS_LEVEL_2 = 4;
    public static final int MAX_BLOCKS_LEVEL_2 = 48;

    /** Six faces plus the twelve edge diagonals. */
    private static final BlockPos[] NEIGHBOURS = {
            new BlockPos(0, 1, 0), new BlockPos(0, -1, 0), new BlockPos(0, 0, -1), new BlockPos(0, 0, 1),
            new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0),
            new BlockPos(1, 1, 0), new BlockPos(1, -1, 0), new BlockPos(-1, 1, 0), new BlockPos(-1, -1, 0),
            new BlockPos(0, 1, 1), new BlockPos(0, -1, 1), new BlockPos(0, 1, -1), new BlockPos(0, -1, -1),
            new BlockPos(1, 0, 1), new BlockPos(-1, 0, 1), new BlockPos(1, 0, -1), new BlockPos(-1, 0, -1) };

    /** Set while Grid Miner or Tree Feller is breaking its extra blocks, so they don't chain. */
    private static boolean breakingExtra = false;

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerWorld serverWorld) || !(player instanceof ServerPlayerEntity serverPlayer)
                    || GridMiner.isBreakingExtra())
                return;
            int level = gridMinerLevel(player);
            if (level <= 0)
                return;
            int steps = level >= 2 ? MAX_STEPS_LEVEL_2 : MAX_STEPS_LEVEL_1;
            int max = level >= 2 ? MAX_BLOCKS_LEVEL_2 : MAX_BLOCKS_LEVEL_1;
            ItemStack tool = player.getMainHandStack();
            List<BlockPos> extra = findConnected(serverWorld, pos, NEIGHBOURS, steps, max,
                    s -> !s.isAir() && tool.isSuitableFor(s));
            breakAsPlayer(serverPlayer, extra, ModEnchantments.GRID_MINER_ENCHANTMENT);
        });
    }

    private static int gridMinerLevel(PlayerEntity player) {
        ItemStack mainHand = player.getMainHandStack();
        if (!(mainHand.getItem() instanceof PickaxeItem || mainHand.getItem() instanceof ShovelItem))
            return 0;
        return EnchantmentHelper.getLevel(ModEnchantments.GRID_MINER_ENCHANTMENT, mainHand);
    }

    /**
     * Breadth-first search from {@code origin} (already broken, not included) through
     * {@code neighbours} for blocks matching {@code filter}, nearest first, up to
     * {@code maxSteps} steps and {@code maxBlocks} blocks.
     */
    static List<BlockPos> findConnected(ServerWorld world, BlockPos origin, BlockPos[] neighbours, int maxSteps,
            int maxBlocks, Predicate<BlockState> filter) {
        List<BlockPos> found = new ArrayList<>();
        Map<BlockPos, Integer> steps = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        steps.put(origin, 0);
        queue.add(origin);
        while (!queue.isEmpty() && found.size() < maxBlocks) {
            BlockPos pos = queue.poll();
            int step = steps.get(pos);
            if (step >= maxSteps)
                continue;
            for (BlockPos offset : neighbours) {
                BlockPos next = pos.add(offset);
                if (steps.containsKey(next))
                    continue;
                steps.put(next, step + 1);
                BlockState state = world.getBlockState(next);
                if (state.getHardness(world, next) < 0 || !filter.test(state))
                    continue;
                found.add(next);
                queue.add(next);
                if (found.size() >= maxBlocks)
                    break;
            }
        }
        return found;
    }

    /**
     * Breaks each block as if the player mined it: protection and claim checks (block break
     * events), adventure mode, creative (no drops), Fortune and Silk Touch, tool durability
     * and stats all apply. Stops if the tool breaks or loses the enchantment.
     */
    static boolean isBreakingExtra() {
        return breakingExtra;
    }

    static void breakAsPlayer(ServerPlayerEntity player, List<BlockPos> positions, Enchantment enchantment) {
        breakingExtra = true;
        try {
            for (BlockPos pos : positions) {
                if (EnchantmentHelper.getLevel(enchantment, player.getMainHandStack()) <= 0)
                    break;
                player.interactionManager.tryBreakBlock(pos);
            }
        } finally {
            breakingExtra = false;
        }
    }
}
