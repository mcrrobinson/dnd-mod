package mattonfire.dnd.classes.SkillChecks;

import mattonfire.dnd.classes.Abilities.Skill;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Something a perceptive character can notice: a disguised mimic, an armed dungeon trap, a hidden door.
 *
 * Entities and block entities can implement this directly; a Search ({@link Perception#search}) finds every
 * one within {@link PerceptionService#SEARCH_RANGE} blocks. Anything else (a plain block position, a vanilla
 * entity) is registered with {@link PerceptionService#hide}, which wraps it in one of these.
 *
 * Passive noticing is up to the implementer: call {@link PerceptionService#passiveCheck} from its tick (the
 * mimic does), or use {@link PerceptionService#noticesPassively} in its own rule (traps, through
 * {@link TrapSense}). Things registered with {@code hide} get a passive check for free.
 */
public interface Perceivable {
    /** The DC to notice it, passively or with a Search. */
    int perceptionDc();

    /**
     * PERCEPTION (spotting) or INVESTIGATION (working out a hidden mechanism). For INVESTIGATION the player
     * uses the better of the two bonuses.
     */
    default Skill perceptionSkill() {
        return Skill.PERCEPTION;
    }

    /** Where it is, for range checks. */
    Vec3d perceptionPos();

    /** True while there is still something for this player to notice (false once found, awake or disarmed). */
    boolean hiddenFrom(ServerPlayerEntity player);

    /**
     * The player noticed it.
     *
     * @param searched true for an active Search roll (V), false for passive Perception
     */
    void perceive(ServerPlayerEntity player, boolean searched);
}
