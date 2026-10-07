package mattonfire.dnd.classes.Progression;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Progression.SkillNode.Kind;

/**
 * The skill tree of every class. Each tree has the same shape:
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
 * tier, so with {@link Progression#MAX_LEVEL} points you can't take everything.
 */
public final class ClassTrees {
    private static final Map<DndCharacter, List<SkillNode>> TREES = new EnumMap<>(DndCharacter.class);
    private static final Map<String, SkillNode> NODES = new HashMap<>();

    static {
        tree(DndCharacter.BARBARIAN,
                active("barbarian.rage", "Rage", "Strength III for 15 seconds.", "minecraft:blaze_powder", 9, 0, 1, 3),
                // Berserker
                passive("barbarian.bloodlust", "Bloodlust", "Heal a heart every time you kill something.",
                        "minecraft:redstone", 1, 2, 3, "barbarian.rage"),
                active("barbarian.war_cry", "War Cry",
                        "Mobs within 8 blocks get Weakness and Slowness for 6 seconds.", "minecraft:goat_horn", 3, 1,
                        2, 2, "barbarian.bloodlust"),
                passive("barbarian.rage_fuelled", "Fuelled by Rage", "Deal 30% more damage below half health.",
                        "minecraft:fire_charge", 1, 2, 1, "barbarian.war_cry"),
                // Juggernaut
                passive("barbarian.thick_skin", "Thick Skin", "+4 armor.", "minecraft:leather_chestplate", 1, 0, 3,
                        "barbarian.rage"),
                active("barbarian.ground_slam", "Ground Slam",
                        "Slam the ground, hurting and throwing back mobs within 5 blocks.", "minecraft:anvil", 5, 1,
                        0, 2, "barbarian.thick_skin"),
                passive("barbarian.unstoppable", "Unstoppable", "Half knockback, and you can't be slowed.",
                        "minecraft:iron_chestplate", 1, 0, 1, "barbarian.ground_slam"),
                active("barbarian.titan", "Titan", "Strength III, Resistance and Regeneration for 20 seconds.",
                        "minecraft:netherite_axe", 9, 2, 1, 0, "barbarian.rage_fuelled", "barbarian.unstoppable"));

        tree(DndCharacter.DRUID,
                active("druid.wild_shape", "Wild Shape", "Turn into an animal you've killed for 30 seconds.",
                        "minecraft:rabbit_foot", 9, 0, 1, 3),
                // Circle of the Moon
                passive("druid.hardy_form", "Hardy Form", "Resistance while in animal form.",
                        "minecraft:turtle_helmet", 1, 2, 3, "druid.wild_shape"),
                active("druid.thorn_burst", "Thorn Burst",
                        "Thorns hurt mobs within 6 blocks and root them for 4 seconds.", "minecraft:sweet_berries",
                        4, 1, 2, 2, "druid.hardy_form"),
                passive("druid.beast_bond", "Beast Bond", "Tamed animals give up to 10 extra hearts instead of 5.",
                        "minecraft:lead", 1, 2, 1, "druid.thorn_burst"),
                // Circle of the Land
                passive("druid.tidecaller", "Tidecaller", "You can swim again.", "minecraft:heart_of_the_sea", 1, 0,
                        3, "druid.wild_shape"),
                active("druid.regrowth", "Regrowth",
                        "Regeneration II for you, nearby players and your animals for 8 seconds.",
                        "minecraft:glow_berries", 4, 1, 0, 2, "druid.tidecaller"),
                passive("druid.photosynthesis", "Photosynthesis", "Bright light also feeds you.",
                        "minecraft:oak_sapling", 1, 0, 1, "druid.regrowth"),
                active("druid.call_of_the_wild", "Call of the Wild", "Three wolves fight for you for a minute.",
                        "minecraft:bone", 9, 2, 1, 0, "druid.beast_bond", "druid.photosynthesis"));

        tree(DndCharacter.ROGUE,
                active("rogue.vanish", "Vanish", "Invisibility for 15 seconds.", "minecraft:fermented_spider_eye", 9,
                        0, 1, 3),
                // Assassin
                passive("rogue.backstab", "Backstab", "Deal 50% more damage while sneaking or invisible.",
                        "minecraft:iron_sword", 1, 2, 3, "rogue.vanish"),
                active("rogue.shadowstep", "Shadowstep", "Teleport up to 12 blocks where you're looking.",
                        "minecraft:ender_pearl", 4, 1, 2, 2, "rogue.backstab"),
                passive("rogue.poisoned_blades", "Poisoned Blades", "Your hits poison.", "minecraft:spider_eye", 1, 2,
                        1, "rogue.shadowstep"),
                // Thief
                passive("rogue.light_feet", "Light Feet", "Take half fall damage.", "minecraft:feather", 1, 0, 3,
                        "rogue.vanish"),
                active("rogue.smoke_bomb", "Smoke Bomb",
                        "Blind and slow mobs within 6 blocks, and they lose track of you.", "minecraft:gunpowder", 3,
                        1, 0, 2, "rogue.light_feet"),
                passive("rogue.fleet", "Fleet", "Move 15% faster.", "minecraft:sugar", 1, 0, 1, "rogue.smoke_bomb"),
                active("rogue.death_mark", "Death Mark",
                        "Invisibility, Strength II and Speed II for 10 seconds.", "minecraft:wither_skeleton_skull", 9,
                        2, 1, 0, "rogue.poisoned_blades", "rogue.fleet"));
    }

    private ClassTrees() {
    }

    private static SkillNode active(String id, String name, String description, String icon, int manaCost,
            int pointCost, int col, int row, String... requires) {
        return new SkillNode(id, name, description, icon, Kind.ACTIVE, manaCost, pointCost, col, row,
                List.of(requires));
    }

    private static SkillNode passive(String id, String name, String description, String icon, int pointCost,
            int col, int row, String... requires) {
        return new SkillNode(id, name, description, icon, Kind.PASSIVE, 0, pointCost, col, row, List.of(requires));
    }

    private static void tree(DndCharacter dndClass, SkillNode... nodes) {
        List<SkillNode> list = new ArrayList<>(List.of(nodes));
        TREES.put(dndClass, list);
        for (SkillNode node : list) {
            NODES.put(node.id(), node);
        }
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
