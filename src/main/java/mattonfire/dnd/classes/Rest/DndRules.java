package mattonfire.dnd.classes.Rest;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

/**
 * The rests and death saves gamerules. All five are registered here; the death
 * save ones are read by the downed state once it exists.
 */
public final class DndRules {
    /** Charges gate major actives and rests restore them. False plays as before charges existed. */
    public static final GameRules.Key<GameRules.BooleanRule> RESTS = GameRuleRegistry.register("dndRests",
            GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(true));
    /** Real minutes alive per free charge; 0 turns the trickle off. */
    public static final GameRules.Key<GameRules.IntRule> CHARGE_TRICKLE_MINUTES = GameRuleRegistry.register(
            "dndChargeTrickleMinutes", GameRules.Category.PLAYER, GameRuleFactory.createIntRule(10, 0, 1440));
    /** 0 off, 1 downed when a party member is near (default), 2 downed when any player is near. */
    public static final GameRules.Key<GameRules.IntRule> DEATH_SAVES = GameRuleRegistry.register("dndDeathSaves",
            GameRules.Category.PLAYER, GameRuleFactory.createIntRule(1, 0, 2));
    /** Solo players get one Last Stand roll instead of dying. */
    public static final GameRules.Key<GameRules.BooleanRule> LAST_STAND = GameRuleRegistry.register("dndLastStand",
            GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(true));
    /** Players downed by a player outside their party go downed instead of dying. */
    public static final GameRules.Key<GameRules.BooleanRule> PVP_DOWNED = GameRuleRegistry.register("dndPvpDowned",
            GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(true));

    private DndRules() {
    }

    /** Loads the class so the rules are registered at startup. */
    public static void register() {
    }

    public static boolean rests(World world) {
        return world.getGameRules().getBoolean(RESTS);
    }

    public static int chargeTrickleMinutes(World world) {
        return world.getGameRules().getInt(CHARGE_TRICKLE_MINUTES);
    }
}
