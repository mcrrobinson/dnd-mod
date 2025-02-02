package mattonfire.dnd.classes;

import java.util.HashSet;
import java.util.Set;

import mattonfire.dnd.classes.Registry.ModEnchantments;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class GridMiner {
    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (world instanceof ServerWorld) {
                if (hasGridMiner(player)) {
                    breakConnectedBlocks((ServerWorld) world, pos, player, 10, new HashSet<>());
                }
            }
            return true;
        });
    }

    private static boolean hasGridMiner(PlayerEntity player) {
        ItemStack mainHand = player.getMainHandStack();
        if (EnchantmentHelper.getLevel(ModEnchantments.GRID_MINER_ENCHANTMENT, mainHand) > 0) {
            Item item = mainHand.getItem();
            if (item instanceof net.minecraft.item.PickaxeItem || item instanceof net.minecraft.item.ShovelItem) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBreakableBlock(BlockState state, PlayerEntity player) {
        ItemStack tool = player.getMainHandStack();
        if (tool.getItem() instanceof ToolItem) {
            ToolItem toolItem = (ToolItem) tool.getItem();
            return toolItem.isSuitableFor(state);
        }
        return false;
    }

    private static void breakConnectedBlocks(ServerWorld world, BlockPos pos, PlayerEntity player, int depth,
            Set<BlockPos> visited) {
        if (depth <= 0 || visited.contains(pos)) {
            return;
        }
        visited.add(pos);

        if (isBreakableBlock(world.getBlockState(pos), player)) {
            world.breakBlock(pos, true, player);

            for (BlockPos adjacent : new BlockPos[] {
                    pos.up(), pos.down(), pos.north(), pos.south(), pos.east(), pos.west(),
                    pos.add(1, 1, 0), pos.add(1, -1, 0), pos.add(-1, 1, 0), pos.add(-1, -1, 0),
                    pos.add(0, 1, 1), pos.add(0, -1, 1), pos.add(0, 1, -1), pos.add(0, -1, -1),
                    pos.add(1, 0, 1), pos.add(-1, 0, 1), pos.add(1, 0, -1), pos.add(-1, 0, -1)
            }) {
                breakConnectedBlocks(world, adjacent, player, depth - 1, visited);
            }
        }
    }
}
