package mattonfire.dnd.world.underdark;

import java.util.Locale;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.dungeon.DungeonMaps;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapIcon;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.Structure;

/**
 * Underdark explorer maps (design 9, 3.4): vanilla explorer-style maps to the nearest structure in a destination tag,
 * for the duergar factor's trades and for loot. Each kind's tag ({@code #dndclasses:underdark_maps/<kind>}) starts
 * empty or with optional entries for structures later tickets add (outposts, holds, rift landings), so a map works as
 * soon as its structure lands. Loot tables use the same tags through vanilla's {@code exploration_map} function
 * ({@code loot_tables/underdark/map_to_<kind>.json}).
 */
public final class UnderdarkMaps {
    /** How far (in chunks) a map searches for its destination. */
    public static final int SEARCH_RADIUS = 100;

    public enum Kind {
        DROW_OUTPOST(MapIcon.Type.RED_X),
        DUERGAR_HOLD(MapIcon.Type.RED_X),
        RIFT(MapIcon.Type.TARGET_POINT);

        public final MapIcon.Type icon;
        public final TagKey<Structure> destinations;

        Kind(MapIcon.Type icon) {
            this.icon = icon;
            this.destinations = TagKey.of(RegistryKeys.STRUCTURE, new Identifier(DnDClasses.MOD_ID,
                    "underdark_maps/" + id()));
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** The map's name, "Map to a Drow Outpost" and so on. */
        public Text title() {
            return Text.translatable("item.dndclasses.underdark_map." + id());
        }

        public static Kind byId(String id) {
            for (Kind kind : values()) {
                if (kind.id().equals(id)) {
                    return kind;
                }
            }
            return null;
        }
    }

    private UnderdarkMaps() {
    }

    /**
     * A map from {@code from} to the nearest unexplored {@code kind} destination in {@code world}, or null if there's
     * none within {@value #SEARCH_RADIUS} chunks (or the tag is still empty).
     */
    public static ItemStack create(ServerWorld world, BlockPos from, Kind kind) {
        BlockPos target = world.locateStructure(kind.destinations, from, SEARCH_RADIUS, true);
        if (target == null) {
            DnDClasses.LOGGER.info("[Underdark] no {} for a map near {} {} {}", kind.id(), from.getX(), from.getY(),
                    from.getZ());
            return null;
        }
        DnDClasses.LOGGER.info("[Underdark] map to {} at {} {}", kind.id(), target.getX(), target.getZ());
        return DungeonMaps.explorerMap(world, target, "+", kind.icon,
                kind.title().copy().styled(style -> style.withItalic(false)));
    }
}
