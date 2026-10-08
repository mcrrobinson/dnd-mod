package mattonfire.dnd.entity;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.Nullable;

/**
 * Dwarves defend their fortress the way piglins guard their gold: open or break a chest or barrel, or
 * break a gold block, where a dwarf can see you and it and its kin turn on you (see
 * {@link MountainDwarfEntity#witness}). Help them drive off a goblin raid, though, and every dwarf
 * of the fortress forgives you ({@link #forgive}).
 */
public final class DwarfGrudges {
    private DwarfGrudges() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            // Sneaking with an item in hand places it against the chest instead of opening it
            boolean opens = !(player.shouldCancelInteraction() && !player.getStackInHand(hand).isEmpty());
            if (!world.isClient && opens && isHoard(world.getBlockEntity(hit.getBlockPos()))) {
                MountainDwarfEntity.witness(player, hit.getBlockPos());
            }
            return ActionResult.PASS;
        });
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (state.isOf(Blocks.GOLD_BLOCK) || isHoard(blockEntity)) {
                MountainDwarfEntity.witness(player, pos);
            }
        });
    }

    /** Chests (trapped ones too) and barrels; not hoppers, dispensers, shulker boxes and the like. */
    private static boolean isHoard(@Nullable BlockEntity blockEntity) {
        return blockEntity instanceof ChestBlockEntity || blockEntity instanceof BarrelBlockEntity;
    }

    /** Every dwarf within {@code range} of {@code pos} drops its grudge against {@code player}. */
    public static void forgive(PlayerEntity player, BlockPos pos, double range) {
        for (MountainDwarfEntity dwarf : player.world.getEntitiesByClass(MountainDwarfEntity.class,
                new Box(pos).expand(range), dwarf -> dwarf.isAlive())) {
            if (dwarf.shouldAngerAt(player) || dwarf.getTarget() == player) {
                dwarf.stopAnger();
            }
        }
    }
}
