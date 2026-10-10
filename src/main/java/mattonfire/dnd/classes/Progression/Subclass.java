package mattonfire.dnd.classes.Progression;

import java.util.Set;

import mattonfire.dnd.classes.ClassInfo;
import mattonfire.dnd.classes.DndCharacter;

/**
 * One of a class's two subclasses: one branch of its skill tree, chosen at
 * class level {@link ClassProgress#SUBCLASS_LEVEL} at an Attunement Table.
 * Built by {@link ClassTrees} from the ids in {@link ClassSkills#subclassIds()};
 * its text (name, flavour, feature) is in {@code class_info.json}.
 *
 * @param id     e.g. "barbarian.berserker"
 * @param col    the branch's tree column: 2 (right, listed first) or 0 (left)
 * @param dabble the branch's first (row 3) node, open to both subclasses
 * @param nodes  the rest of the branch (rows 2 and 1, plus nodes that hang off it such as the
 *               Rogue's Danger Sense): only this subclass can unlock them
 */
public record Subclass(String id, DndCharacter dndClass, int col, String dabble, Set<String> nodes) {

    public ClassInfo.SubclassInfo info() {
        ClassInfo.SubclassInfo info = ClassInfo.subclass(id);
        return info != null ? info : new ClassInfo.SubclassInfo(id, id, "", "", "", "", false);
    }

    /** "Path of the Berserker". */
    public String name() {
        return info().name();
    }

    /** "Berserker Barbarian", as in "Matt the Berserker Barbarian". */
    public String title() {
        return info().title();
    }

    public String flavour() {
        return info().flavour();
    }

    /** "Frenzy". */
    public String featureName() {
        return info().featureName();
    }

    public String featureDescription() {
        return info().featureDescription();
    }

    /** Whether the feature's effect is in the game yet; until then the tree says so. */
    public boolean featureReady() {
        return info().featureReady();
    }
}
