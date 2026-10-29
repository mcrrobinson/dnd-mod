package mattonfire.dnd.entity.kobold;

import mattonfire.dnd.faction.FactionIds;
import mattonfire.dnd.faction.ReputationTier;
import mattonfire.dnd.faction.TierEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.Difficulty;

/**
 * Who the Kobold Tribes fight, by the player's standing with them (design 10, 3.5; the same shape as
 * {@code OrcHostility}):
 * <ul>
 * <li>Hostile: kobolds go for you on sight (brave ones close in, craven ones harass from range).</li>
 * <li>Unfriendly: only inside their home (a warren, a lair), within {@link TierEffects#PARLEY_RANGE} blocks, or
 * once provoked. Otherwise they watch and hiss.</li>
 * <li>Neutral and up: only once provoked (you hit one of them, or wronged the pack).</li>
 * <li>Honored: warren traps spare you ({@link #sparedByTraps}, read by K2's traps).</li>
 * <li>Exalted: kobolds within {@value #ALLY_RANGE} blocks join your fights.</li>
 * </ul>
 * Lair guards ({@link #isLairGuard}, the Ancient's kobolds, D2) treat everyone below Exalted as Hostile.
 */
public final class KoboldHostility {
    /** Command tag on the kobolds guarding an Ancient's lair (D2's guard posts). */
    public static final String LAIR_GUARD_TAG = "dndclasses.lair_guard";
    /** Exalted friends get kobolds from this far joining their fights. */
    public static final double ALLY_RANGE = 16.0D;
    /** How far a kobold notices a player it would attack on sight. */
    public static final double SIGHT_RANGE = 16.0D;

    private KoboldHostility() {
    }

    public static boolean isLairGuard(Entity kobold) {
        return kobold.getCommandTags().contains(LAIR_GUARD_TAG);
    }

    /** {@code player}'s standing as {@code kobold} sees it: a lair guard treats anyone below Exalted as Hostile. */
    public static ReputationTier tier(KoboldEntity kobold, PlayerEntity player) {
        ReputationTier tier = TierEffects.tierWith(player, FactionIds.KOBOLDS);
        if (isLairGuard(kobold) && tier != ReputationTier.EXALTED) {
            return ReputationTier.HOSTILE;
        }
        return tier;
    }

    /** Whether {@code kobold} goes for {@code player} on sight (its target goal). */
    public static boolean attacksOnSight(KoboldEntity kobold, PlayerEntity player) {
        if (player.isCreative() || player.isSpectator() || kobold.world.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        if (kobold.squaredDistanceTo(player) > SIGHT_RANGE * SIGHT_RANGE) {
            return false;
        }
        return switch (tier(kobold, player)) {
            case HOSTILE -> true;
            case UNFRIENDLY -> KoboldCourage.atHome(kobold)
                    || kobold.squaredDistanceTo(player) <= TierEffects.PARLEY_RANGE * TierEffects.PARLEY_RANGE;
            default -> false;
        };
    }

    /**
     * Whether {@code kobold} may fight {@code player} at all: provoked (a grudge, or the player struck a kobold in
     * the last 10 s), or on sight. {@code engaged} skips the sight and parley ranges, for a kobold already fighting.
     */
    public static boolean mayFight(KoboldEntity kobold, PlayerEntity player, boolean engaged) {
        if (player.isCreative() || player.isSpectator() || kobold.world.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        if (kobold.provokedBy(player) || TierEffects.provoked(kobold, player)) {
            return true;
        }
        if (engaged) {
            ReputationTier tier = tier(kobold, player);
            return tier == ReputationTier.HOSTILE || tier == ReputationTier.UNFRIENDLY;
        }
        return attacksOnSight(kobold, player);
    }

    /** Honored and up: kobold traps (NBT {@code Faction: dndclasses:kobolds}, K2) don't go off under you. */
    public static boolean sparedByTraps(PlayerEntity player) {
        return TierEffects.tierWith(player, FactionIds.KOBOLDS).atLeast(ReputationTier.HONORED);
    }

    /** Exalted: kobolds fight beside you ({@code FactionAllyGoal}). */
    public static boolean fightsBeside(ReputationTier tier) {
        return tier == ReputationTier.EXALTED;
    }

    /** Friendly and up: Trapmakers trade. */
    public static boolean trades(PlayerEntity player) {
        return TierEffects.tierWith(player, FactionIds.KOBOLDS).atLeast(ReputationTier.FRIENDLY);
    }
}
