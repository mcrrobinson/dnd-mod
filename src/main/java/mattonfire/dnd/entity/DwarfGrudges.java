package mattonfire.dnd.entity;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/**
 * Dwarves defend their fortress the way piglins guard their gold: open a chest or barrel, or break
 * a gold block, where a dwarf can see you and it and its kin turn on you (see
 * {@link MountainDwarfEntity#witness}). Help them drive off a goblin raid, though, and every dwarf
 * of the fortress forgives you ({@link #forgive}).
 */
public final class DwarfGrudges {
    private DwarfGrudges() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!world.isClient
                    && world.getBlockEntity(hit.getBlockPos()) instanceof LootableContainerBlockEntity) {
                MountainDwarfEntity.witness(player, hit.getBlockPos());
            }
            return ActionResult.PASS;
        });
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (state.isIn(BlockTags.GUARDED_BY_PIGLINS)) {
                MountainDwarfEntity.witness(player, pos);
            }
        });
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
