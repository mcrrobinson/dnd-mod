package mattonfire.dnd.dungeon;

import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapIcon;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * Dungeon Maps: vanilla explorer-style filled maps centred on a dungeon's entrance, with a red X
 * on it and named after the dungeon. Bounties hand them out; quests can too.
 */
public final class DungeonMaps {
    /** Map NBT: the start key of the dungeon it leads to. */
    public static final String START_KEY = "DungeonStartKey";
    private static final byte SCALE = 2;

    private DungeonMaps() {
    }

    public static ItemStack create(ServerWorld world, DungeonState dungeon) {
        BlockPos entrance = dungeon.entrance();
        ItemStack map = FilledMapItem.createMap(world, entrance.getX(), entrance.getZ(), SCALE, true, true);
        FilledMapItem.fillExplorationMap(world, map);
        MapState.addDecorationsNbt(map, entrance, "dungeon", MapIcon.Type.RED_X);
        map.setCustomName(Text.translatable("item.dndclasses.dungeon_map", dungeon.name())
                .styled(style -> style.withItalic(false)));
        NbtCompound nbt = map.getOrCreateNbt();
        nbt.putLong(START_KEY, dungeon.startKey());
        return map;
    }

    /** The start key of the dungeon {@code stack} is a map to, or null if it isn't a dungeon map. */
    public static Long startKey(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.contains(START_KEY) ? nbt.getLong(START_KEY) : null;
    }
}
