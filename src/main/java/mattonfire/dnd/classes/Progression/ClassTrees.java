package mattonfire.dnd.classes.Progression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.ClassInfo;
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
 * tier: 8 points for the whole tree. The two branches are the class's
 * {@link Subclass}es: right (A) then left (B). Only the chosen one's rows 2 and
 * 1 can be unlocked; both first nodes stay open. Ranks above 1 ({@link Ranks}) cost a point
 * each too, so with {@link ClassProgress#MAX_LEVEL} points you can't take everything.
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
    private static final Map<DndCharacter, List<Subclass>> SUBCLASSES = new EnumMap<>(DndCharacter.class);
    private static final Map<String, Subclass> SUBCLASS_BY_ID = new HashMap<>();
    /** Locked node id to the only subclass that can unlock it. */
    private static final Map<String, Subclass> SUBCLASS_OF_NODE = new HashMap<>();

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
        Ranks.validate();
        for (ClassSkills skills : ALL) {
            buildSubclasses(skills);
        }
    }

    /**
     * Works out each subclass's nodes from the tree's shape: its column, plus any
     * middle-column node that only hangs off that column (the Rogue's Danger Sense).
     * A middle node that only needs the root (the Paladin's Circle of Healing) is
     * shared. Throws if a tree doesn't fit.
     */
    private static void buildSubclasses(ClassSkills skills) {
        DndCharacter dndClass = skills.dndClass();
        List<SkillNode> nodes = TREES.get(dndClass);
        List<String> ids = skills.subclassIds();
        if (nodes.isEmpty())
            return;
        if (ids.size() != 2)
            throw new IllegalStateException(dndClass + " needs exactly 2 subclasses, has " + ids);
        SkillNode root = nodes.get(0);
        List<Subclass> subclasses = new ArrayList<>();
        Set<String> claimed = new HashSet<>();
        int[] cols = { 2, 0 };
        for (int i = 0; i < 2; i++) {
            int col = cols[i];
            String id = ids.get(i);
            if (!id.startsWith(root.id().substring(0, root.id().indexOf('.') + 1)))
                throw new IllegalStateException("Subclass " + id + " should start with its class's skill prefix");
            if (ClassInfo.subclass(id) == null)
                throw new IllegalStateException("Subclass " + id + " has no entry in class_info.json");

            List<SkillNode> firsts = nodes.stream()
                    .filter(n -> n.col() == col && n.row() == 3 && n.requires().equals(List.of(root.id()))).toList();
            if (firsts.size() != 1)
                throw new IllegalStateException(id + " needs one first (row 3) node in column " + col);
            String dabble = firsts.get(0).id();

            Set<String> branch = new LinkedHashSet<>();
            nodes.stream().filter(n -> n.col() == col && !n.isRoot() && n.row() != 0)
                    .forEach(n -> branch.add(n.id()));
            boolean grew = true;
            while (grew) {
                grew = false;
                for (SkillNode node : nodes) {
                    if (!node.isRoot() && node.row() != 0 && !branch.contains(node.id())
                            && branch.containsAll(node.requires())) {
                        branch.add(node.id());
                        grew = true;
                    }
                }
            }
            branch.remove(dabble);
            for (String nodeId : branch) {
                if (!claimed.add(nodeId))
                    throw new IllegalStateException(nodeId + " is in both of " + dndClass + "'s subclasses");
            }
            Subclass subclass = new Subclass(id, dndClass, col, dabble, Collections.unmodifiableSet(branch));
            subclasses.add(subclass);
            if (SUBCLASS_BY_ID.put(id, subclass) != null)
                throw new IllegalStateException("Duplicate subclass id " + id);
            branch.forEach(nodeId -> SUBCLASS_OF_NODE.put(nodeId, subclass));
        }
        for (SkillNode node : nodes) {
            boolean branchNode = claimed.contains(node.id())
                    || subclasses.stream().anyMatch(sub -> sub.dabble().equals(node.id()));
            if (!node.isRoot() && node.row() != 0 && !branchNode && !node.requires().equals(List.of(root.id())))
                throw new IllegalStateException(node.id() + " is in neither of " + dndClass
                        + "'s subclasses and isn't a shared node off the root");
        }
        SUBCLASSES.put(dndClass, List.copyOf(subclasses));
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

    /** The class's two subclasses, right branch first; empty without a tree. */
    public static List<Subclass> subclasses(DndCharacter dndClass) {
        return dndClass == null ? List.of() : SUBCLASSES.getOrDefault(dndClass, List.of());
    }

    @Nullable
    public static Subclass subclass(String id) {
        return id == null ? null : SUBCLASS_BY_ID.get(id);
    }

    /** The subclass that alone can unlock this node, or null if anyone can (root, first nodes, capstone, shared). */
    @Nullable
    public static Subclass subclassOf(String nodeId) {
        return SUBCLASS_OF_NODE.get(nodeId);
    }

    /**
     * A node in neither branch that still needs some subclass: the middle column
     * off the root, like the Paladin's Circle of Healing.
     */
    public static boolean isShared(SkillNode node) {
        return node != null && !node.isRoot() && node.row() != 0 && node.row() != 3
                && subclassOf(node.id()) == null;
    }

    /** True if the node belongs to this class's tree. */
    public static boolean belongsTo(SkillNode node, DndCharacter dndClass) {
        return node != null && get(dndClass).contains(node);
    }
}
