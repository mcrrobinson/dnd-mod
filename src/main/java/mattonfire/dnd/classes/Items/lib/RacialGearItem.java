package mattonfire.dnd.classes.Items.lib;

import java.util.Set;

import mattonfire.dnd.classes.Race.DndRace;

/**
 * Gear made by and for certain races (the racial armor sets and weapons sold as kin stock). The races are
 * listed in the tooltip ({@link ArmorTooltips#appendRaces}); a set's race bonus is in {@link SetBonusArmor}.
 */
public interface RacialGearItem {
    /** Races this item is made for. Empty for gear that isn't racial. */
    default Set<DndRace> getMatchingRaces() {
        return Set.of();
    }
}
