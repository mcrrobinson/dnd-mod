package mattonfire.dnd.entity.ai.goal;

import mattonfire.dnd.entity.MountainDwarfEntity;
import mattonfire.dnd.faction.TierEffects;

/**
 * Exalted with the dwarves: a dwarf joins the fight of a friend of the mountain within
 * {@link TierEffects#DWARF_ALLY_RANGE} blocks ({@link FactionAllyGoal}; never against another dwarf).
 */
public class DwarfFriendGoal extends FactionAllyGoal {
    public DwarfFriendGoal(MountainDwarfEntity dwarf) {
        super(dwarf, TierEffects.DWARF_ALLY_RANGE, TierEffects::dwarfFriend, entity -> entity instanceof MountainDwarfEntity);
    }
}
