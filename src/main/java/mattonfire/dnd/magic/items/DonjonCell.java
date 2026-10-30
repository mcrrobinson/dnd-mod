package mattonfire.dnd.magic.items;

import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/**
 * The Deck of Many Things' Donjon: the drawer vanishes into a sealed {@value #INSIDE}x{@value #INSIDE}x{@value
 * #INSIDE} cell of polished blackstone, walls {@value #WALL} thick, {@value #DEPTH} blocks below them (never lower
 * than the world's floor + 5). A soul lantern burns inside; they get 3 bread and a stone pickaxe to dig out with.
 */
public final class DonjonCell {
    public static final int INSIDE = 3;
    public static final int WALL = 2;
    public static final int DEPTH = 24;

    private DonjonCell() {
    }

    /** Builds the cell under the player and puts them in it. Returns the cell's floor centre. */
    public static BlockPos build(ServerPlayerEntity player) {
        ServerWorld world = player.getWorld();
        int floorY = Math.max(player.getBlockY() - DEPTH, world.getBottomY() + 5);
        BlockPos center = new BlockPos(player.getBlockX(), floorY, player.getBlockZ());
        BlockState wall = Blocks.POLISHED_BLACKSTONE.getDefaultState();
        int r = INSIDE / 2 + WALL;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -WALL; dy < INSIDE + WALL; dy++) {
                    BlockPos pos = center.add(dx, dy, dz);
                    BlockState current = world.getBlockState(pos);
                    // Never through bedrock or a block entity (a chest, a spawner)
                    if (current.getHardness(world, pos) < 0 || current.hasBlockEntity())
                        continue;
                    boolean inside = Math.abs(dx) <= INSIDE / 2 && Math.abs(dz) <= INSIDE / 2 && dy >= 0 && dy < INSIDE;
                    world.setBlockState(pos, inside ? Blocks.AIR.getDefaultState() : wall);
                }
            }
        }
        world.setBlockState(center.add(1, 0, 1), Blocks.SOUL_LANTERN.getDefaultState());
        if (player.hasVehicle())
            player.stopRiding();
        player.teleport(world, center.getX() + 0.5, floorY, center.getZ() + 0.5, player.getYaw(), player.getPitch());
        player.fallDistance = 0;
        player.getInventory().offerOrDrop(new ItemStack(Items.BREAD, 3));
        player.getInventory().offerOrDrop(new ItemStack(Items.STONE_PICKAXE));
        world.playSound(null, center, SoundEvents.BLOCK_IRON_DOOR_CLOSE, SoundCategory.PLAYERS, 1.0F, 0.5F);
        player.sendMessage(Text.translatable("deck.dndclasses.donjon.done").formatted(Formatting.DARK_GRAY), false);
        DnDClasses.LOGGER.info("[Deck] Donjon: {} sealed in a cell at {}", player.getEntityName(),
                center.toShortString());
        return center;
    }
}
