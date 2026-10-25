package mattonfire.dnd.entity.drow;

import mattonfire.dnd.faction.ReputationTier;
import mattonfire.dnd.faction.TierEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.Difficulty;

/**
 * Who the Spider Houses fight. Drow attack any player below Neutral with the Spider Houses on sight (everyone
 * starts Hostile, at -700, and Elves at -900, so in practice that's every player; a data pack can change the start).
 * Anyone who strikes a drow is fought regardless. Spiders and other drow are allies ({@link DrowAllies}).
 */
public final class DrowHostility {
    private DrowHostility() {
    }

    /** Whether {@code drow} goes for {@code player} on sight. */
    public static boolean targets(DrowEntity drow, PlayerEntity player) {
        if (player.isCreative() || player.isSpectator() || drow.getWorld().getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        return !TierEffects.tierWith(player, drow).atLeast(ReputationTier.NEUTRAL);
    }

    /** Whether {@code drow} may fight {@code player}: on sight, or because the player struck it. */
    public static boolean mayFight(DrowEntity drow, PlayerEntity player) {
        if (player.isCreative() || player.isSpectator()) {
            return false;
        }
        return player == drow.getAttacker() || targets(drow, player);
    }
}
