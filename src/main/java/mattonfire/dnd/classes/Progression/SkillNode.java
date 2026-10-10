package mattonfire.dnd.classes.Progression;

import java.util.List;

/**
 * One node of a class skill tree.
 *
 * @param id          unique id, e.g. "barbarian.war_cry"
 * @param icon        item id drawn in the tree, e.g. "minecraft:goat_horn"
 * @param kind        an active (fired with the power-up key) or a passive
 * @param manaCost    mana pips an active uses; 0 for passives
 * @param pointCost   skill points to unlock; 0 for the root, which every class starts with. Ranks above 1
 *                    cost more, see {@link Ranks}
 * @param col         tree column, 0 (left) to 2 (right)
 * @param row         tree row, 0 (top) to 3 (bottom)
 * @param requires    unlocking needs any one of these nodes unlocked
 */
public record SkillNode(String id, String name, String description, String icon, Kind kind, int manaCost,
        int pointCost, int col, int row, List<String> requires) {

    public enum Kind {
        ACTIVE, PASSIVE
    }

    public boolean isActive() {
        return kind == Kind.ACTIVE;
    }

    public boolean isRoot() {
        return requires.isEmpty();
    }

    /**
     * Charges it costs on top of its mana when rests are on (see {@code Rest.Charges}, which can
     * override this per node): the root special 1, the row-0 capstone 2, any other active
     * costing 7+ mana 1, everything else 0.
     */
    public int chargeCost() {
        if (!isActive())
            return 0;
        if (isRoot())
            return 1;
        if (row == 0)
            return 2;
        return manaCost >= 7 ? 1 : 0;
    }

    /** How many ranks it has, from its {@link Ranks}; 1 if it has none. */
    public int maxRank() {
        return Ranks.maxRank(id);
    }

    /** Its values per rank, or null if it only has one rank. */
    public Ranks ranks() {
        return Ranks.get(id);
    }
}
