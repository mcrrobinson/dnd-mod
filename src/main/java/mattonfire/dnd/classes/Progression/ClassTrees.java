package mattonfire.dnd.classes.Progression;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Progression.Classes.AlchemistSkills;
import mattonfire.dnd.classes.Progression.Classes.ArtificerSkills;
import mattonfire.dnd.classes.Progression.Classes.BarbarianSkills;
import mattonfire.dnd.classes.Progression.Classes.BardSkills;
import mattonfire.dnd.classes.Progression.Classes.BloodHunterSkills;
import mattonfire.dnd.classes.Progression.Classes.ClericSkills;
import mattonfire.dnd.classes.Progression.Classes.DruidSkills;
import mattonfire.dnd.classes.Progression.Classes.FighterSkills;
import mattonfire.dnd.classes.Progression.Classes.MonkSkills;
import mattonfire.dnd.classes.Progression.Classes.NecromancerSkills;
import mattonfire.dnd.classes.Progression.Classes.PaladinSkills;
import mattonfire.dnd.classes.Progression.Classes.RangerSkills;
import mattonfire.dnd.classes.Progression.Classes.RogueSkills;
import mattonfire.dnd.classes.Progression.Classes.WarlockSkills;
import mattonfire.dnd.classes.Progression.Classes.WizardSkills;

/**
 * Every class's skill tree. Each tree has the same shape:
 *
 * <pre>
 *        [Capstone]          row 0
 *      /            \
 *   [B3]            [A3]     row 1
 *   [B2]            [A2]     row 2
 *   [B1]  [ Root ]  [A1]     row 3
 * </pre>
 *
 * The root is the class's original power-up and is always unlocked. Tier
 * nodes cost a point each and the capstone two, and it needs either third
 * tier, so with {@link ClassProgress#MAX_LEVEL} points you can't take everything.
 */
public final class ClassTrees {
    private static final List<ClassSkills> ALL = List.of(
            new BarbarianSkills(), new BardSkills(), new ClericSkills(), new DruidSkills(), new FighterSkills(),
            new MonkSkills(), new PaladinSkills(), new RangerSkills(), new RogueSkills(), new NecromancerSkills(),
            new WarlockSkills(), new WizardSkills(), new ArtificerSkills(), new BloodHunterSkills(),
            new AlchemistSkills());

    private static final Map<DndCharacter, ClassSkills> SKILLS = new EnumMap<>(DndCharacter.class);
    private static final Map<DndCharacter, List<SkillNode>> TREES = new EnumMap<>(DndCharacter.class);
    private static final Map<String, SkillNode> NODES = new HashMap<>();

    static {
        for (ClassSkills skills : ALL) {
            List<SkillNode> nodes = List.copyOf(skills.nodes());
            SKILLS.put(skills.dndClass(), skills);
            TREES.put(skills.dndClass(), nodes);
            for (SkillNode node : nodes) {
                if (NODES.put(node.id(), node) != null) {
                    throw new IllegalStateException("Duplicate skill id " + node.id());
                }
            }
        }
    }

    private ClassTrees() {
    }

    public static List<ClassSkills> all() {
        return ALL;
    }

    /** The class's skills, or null for no class. */
    public static ClassSkills skills(DndCharacter dndClass) {
        return dndClass == null ? null : SKILLS.get(dndClass);
    }

    /** The class's nodes, root first; empty if the class has no tree yet. */
    public static List<SkillNode> get(DndCharacter dndClass) {
        return dndClass == null ? List.of() : TREES.getOrDefault(dndClass, List.of());
    }

    public static boolean has(DndCharacter dndClass) {
        return !get(dndClass).isEmpty();
    }

    public static SkillNode node(String id) {
        return NODES.get(id);
    }

    public static SkillNode root(DndCharacter dndClass) {
        List<SkillNode> nodes = get(dndClass);
        return nodes.isEmpty() ? null : nodes.get(0);
    }

    /** True if the node belongs to this class's tree. */
    public static boolean belongsTo(SkillNode node, DndCharacter dndClass) {
        return node != null && get(dndClass).contains(node);
    }
}
