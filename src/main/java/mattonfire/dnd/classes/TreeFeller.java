package mattonfire.dnd.classes;

import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashSet;
import java.util.Set;

import mattonfire.dnd.classes.Registry.ModEnchantments;

public class TreeFeller {
    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (world instanceof ServerWorld) {
                if (isLog(state) && hasTreeFeller(player)) {
                    breakConnectedLogs((ServerWorld) world, pos, player, 10, new HashSet<>());
                }
            }
            return true;
        });
    }

    private static boolean isLog(BlockState state) {
        return state.isOf(Blocks.OAK_LOG) || state.isOf(Blocks.SPRUCE_LOG) ||
                state.isOf(Blocks.BIRCH_LOG) || state.isOf(Blocks.JUNGLE_LOG) ||
                state.isOf(Blocks.ACACIA_LOG) || state.isOf(Blocks.DARK_OAK_LOG) ||
                state.isOf(Blocks.MANGROVE_LOG) || state.isOf(Blocks.CHERRY_LOG) ||
                state.isOf(Blocks.CRIMSON_STEM) || state.isOf(Blocks.WARPED_STEM);
    }

    private static boolean hasTreeFeller(PlayerEntity player) {
        ItemStack mainHand = player.getMainHandStack();
        return mainHand.getItem() instanceof net.minecraft.item.AxeItem &&
                EnchantmentHelper.getLevel(ModEnchantments.TREE_FELLER_ENCHANTMENT, mainHand) > 0; // Replace with
                                                                                                   // custom enchantment
        // check
    }

    private static void breakConnectedLogs(ServerWorld world, BlockPos pos, PlayerEntity player, int depth,
            Set<BlockPos> visited) {
        if (depth <= 0 || visited.contains(pos)) {
            return;
        }
        visited.add(pos);

        if (isLog(world.getBlockState(pos))) {
            world.breakBlock(pos, true, player);

            for (BlockPos adjacent : new BlockPos[] {
                    pos.up(), pos.down(), pos.north(), pos.south(), pos.east(), pos.west()
            }) {
                breakConnectedLogs(world, adjacent, player, depth - 1, visited);
            }
        }
    }
}
