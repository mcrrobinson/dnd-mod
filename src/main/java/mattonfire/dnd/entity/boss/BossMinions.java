package mattonfire.dnd.entity.boss;

import net.minecraft.entity.Entity;

/**
 * Mobs a boss summons mid-fight (the Lich's raised dead, the Goblin Warlord's warriors). Like an
 * evoker's vexes they drop no loot or experience and earn no class XP or bounty progress, so a boss
 * kept alive can't be farmed for them.
 */
public final class BossMinions {
    public static final String TAG = "dndclasses.boss_minion";

    private BossMinions() {
    }

    public static void mark(Entity minion) {
        minion.addCommandTag(TAG);
    }

    public static boolean isMinion(Entity entity) {
        return entity.getCommandTags().contains(TAG);
    }

    /**
     * True for mobs that drop nothing and earn nothing: boss minions and mobs from {@code "loot": false}
     * encounters ({@link mattonfire.dnd.dm.encounter.EncounterSpawner#NO_LOOT_TAG}).
     */
    public static boolean givesNothing(Entity entity) {
        return isMinion(entity) || mattonfire.dnd.dm.encounter.EncounterSpawner.isNoLoot(entity);
    }
}
