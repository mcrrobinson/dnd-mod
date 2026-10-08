package mattonfire.dnd.classes;

import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.entity.player.PlayerEntity;

import mattonfire.dnd.classes.Registry.ModEnchantments;

/**
 * Tree Feller: breaking a log with an axe also fells the connected logs (faces only), up to
 * {@link #MAX_STEPS} steps and {@link #MAX_LOGS} logs. Each log is broken as the player, so
 * Fortune, protection, creative and durability work as for normal mining.
 */
public class TreeFeller {
    public static final int MAX_STEPS = 10;
    public static final int MAX_LOGS = 64;

    private static final BlockPos[] NEIGHBOURS = {
            new BlockPos(0, 1, 0), new BlockPos(0, -1, 0), new BlockPos(0, 0, -1), new BlockPos(0, 0, 1),
            new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0) };

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerWorld serverWorld) || !(player instanceof ServerPlayerEntity serverPlayer)
                    || GridMiner.isBreakingExtra())
                return;
            if (isLog(state) && hasTreeFeller(player)) {
                GridMiner.breakAsPlayer(serverPlayer,
                        GridMiner.findConnected(serverWorld, pos, NEIGHBOURS, MAX_STEPS, MAX_LOGS, TreeFeller::isLog),
                        ModEnchantments.TREE_FELLER_ENCHANTMENT);
            }
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
        return mainHand.getItem() instanceof AxeItem
                && EnchantmentHelper.getLevel(ModEnchantments.TREE_FELLER_ENCHANTMENT, mainHand) > 0;
    }
}
