package mattonfire.dnd.entity;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.ActionResult;

/**
 * Dwarves defend their fortress the way piglins guard their gold: open a chest or barrel, or break
 * a gold block, where a dwarf can see you and it and its kin turn on you (see
 * {@link MountainDwarfEntity#witness}).
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
}
