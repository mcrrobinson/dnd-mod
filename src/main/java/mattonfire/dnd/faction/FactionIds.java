package mattonfire.dnd.faction;

import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.util.Identifier;

/**
 * The ids of the built-in factions ({@code data/dndclasses/factions/<id>.json}), for code that names one: racial
 * homes, settlement law, the perks {@link TierEffects} builds in.
 */
public final class FactionIds {
    public static final Identifier HOBBITS = id("hobbits");
    public static final Identifier MOUNTAIN_DWARVES = id("mountain_dwarves");
    public static final Identifier SYLVAN_COURT = id("sylvan_court");
    public static final Identifier DRAGONSWORN = id("dragonsworn");
    public static final Identifier BLOODTUSK_HORDE = id("bloodtusk_horde");
    public static final Identifier BRIMSTONE_COVENANT = id("brimstone_covenant");
    public static final Identifier TINKERS_GUILD = id("tinkers_guild");
    public static final Identifier GOBLINS = id("goblins");

    private FactionIds() {
    }

    private static Identifier id(String path) {
        return new Identifier(DnDClasses.MOD_ID, path);
    }
}
