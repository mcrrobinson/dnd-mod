package mattonfire.dnd.classes.Rest;

import java.util.HashMap;
import java.util.Map;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Progression.ClassSkills;
import mattonfire.dnd.classes.Progression.ClassTrees;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Progression.SkillNode;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Charges: each class's "spell slots", spent by major actives (the root special,
 * the capstone and any other active costing 7+ mana) on top of their mana, and
 * restored by rests. With the gamerule {@code dndRests} off they're ignored.
 */
public final class Charges {
    /** Max charges at class level 0-3, 4-6, 7-9 and 10. */
    private static final int[] SHORT_GROUP_MAX = { 2, 2, 3, 3 };
    private static final int[] LONG_GROUP_MAX = { 3, 4, 5, 6 };
    /** Most gems the HUD can draw, the long-rest max. */
    public static final int MAX_SHOWN = 6;

    /** Charge costs that replace {@link SkillNode#chargeCost()}, by node id (subclasses, items). */
    private static final Map<String, Integer> COST_OVERRIDES = new HashMap<>();

    private Charges() {
    }

    /** Sets a node's charge cost, replacing the derived one. Call at startup, on both sides. */
    public static void overrideCost(String nodeId, int cost) {
        COST_OVERRIDES.put(nodeId, Math.max(0, cost));
    }

    /** Charges an active costs; a class without a tree pays 1 for its power-up. Both sides. */
    public static int cost(SkillNode node) {
        if (node == null) {
            return 1;
        }
        Integer override = COST_OVERRIDES.get(node.id());
        return override != null ? override : node.chargeCost();
    }

    /** The 5e-style recharge group for a class, before any {@link ClassSkills#rechargeGroup()} override. */
    public static RechargeGroup defaultGroup(DndCharacter dndClass) {
        return switch (dndClass) {
            case FIGHTER, MONK, WARLOCK, DRUID, BARD, ROGUE, BLOODHUNTER -> RechargeGroup.SHORT;
            default -> RechargeGroup.LONG;
        };
    }

    public static RechargeGroup group(DndCharacter dndClass) {
        ClassSkills skills = ClassTrees.skills(dndClass);
        return skills != null ? skills.rechargeGroup() : defaultGroup(dndClass);
    }

    /** Max charges for a class at a class level; 0 without a class. */
    public static int max(DndCharacter dndClass, int level) {
        if (dndClass == DndCharacter.NONE) {
            return 0;
        }
        int tier = level >= 10 ? 3 : level >= 7 ? 2 : level >= 4 ? 1 : 0;
        return (group(dndClass) == RechargeGroup.SHORT ? SHORT_GROUP_MAX : LONG_GROUP_MAX)[tier];
    }

    public static int max(ServerPlayerEntity player) {
        DndCharacter dndClass = Progression.classOf(player);
        return max(dndClass, Progression.get(player, dndClass).level());
    }

    /** Whether the player can pay for the active; always true with rests off or no class. */
    public static boolean canAfford(ServerPlayerEntity player, SkillNode node) {
        if (!DndRules.rests(player.getWorld()) || Progression.classOf(player) == DndCharacter.NONE) {
            return true;
        }
        RestState state = RestState.get(player);
        return state.charges + state.tempCharges >= cost(node);
    }

    /** Pays for a fired active, temporary charges first. Does nothing with rests off. */
    public static void spend(ServerPlayerEntity player, SkillNode node) {
        int cost = cost(node);
        if (cost <= 0 || !DndRules.rests(player.getWorld()) || Progression.classOf(player) == DndCharacter.NONE) {
            return;
        }
        RestState state = RestState.get(player);
        int fromTemp = Math.min(cost, state.tempCharges);
        state.tempCharges -= fromTemp;
        state.charges = Math.max(0, state.charges - (cost - fromTemp));
        state.save(player);
        RestSync.sync(player);
    }

    /** Gives back up to n charges, never above the max. */
    public static void restore(ServerPlayerEntity player, int n) {
        RestState state = RestState.get(player);
        state.charges = Math.min(max(player), state.charges + Math.max(0, n));
        state.save(player);
        RestSync.sync(player);
    }

    public static void restoreAll(ServerPlayerEntity player) {
        restore(player, Integer.MAX_VALUE);
    }

    /** Sets the charges, clamped to 0..max. */
    public static void set(ServerPlayerEntity player, int n) {
        RestState state = RestState.get(player);
        state.charges = Math.max(0, Math.min(max(player), n));
        state.save(player);
        RestSync.sync(player);
    }

    /**
     * After a class change: a first pick starts full, a switch keeps the charges
     * but clamps them to the new class's max. Hit Dice reset to full either way.
     */
    public static void onClassChange(ServerPlayerEntity player, DndCharacter oldClass) {
        RestState state = RestState.get(player);
        int max = max(player);
        state.charges = oldClass == DndCharacter.NONE ? max : Math.min(state.charges, max);
        state.hitDiceSpent = 0;
        state.trickleTicks = 0;
        state.save(player);
        RestSync.sync(player);
    }

    /**
     * Once a second: players below their max who are alive count towards a free
     * charge every {@code dndChargeTrickleMinutes}.
     */
    static void trickleSecond(MinecraftServer server) {
        if (!DndRules.rests(server.getOverworld())) {
            return;
        }
        int minutes = DndRules.chargeTrickleMinutes(server.getOverworld());
        if (minutes <= 0) {
            return;
        }
        int needed = minutes * 60 * 20;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.isSpectator() || !player.isAlive() || mattonfire.dnd.classes.Downed.Downed.is(player)) {
                continue;
            }
            int max = max(player);
            RestState state = RestState.get(player);
            if (state.charges >= max) {
                if (state.trickleTicks != 0) {
                    state.trickleTicks = 0;
                    state.save(player);
                }
                continue;
            }
            state.trickleTicks += 20;
            if (state.trickleTicks >= needed) {
                state.trickleTicks = 0;
                state.charges = Math.min(max, state.charges + 1);
            }
            state.save(player);
        }
    }
}
