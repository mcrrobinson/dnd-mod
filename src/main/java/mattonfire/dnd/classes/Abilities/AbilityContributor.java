package mattonfire.dnd.classes.Abilities;

import net.minecraft.entity.player.PlayerEntity;

/**
 * Adds to a player's {@link CharacterSheet}. Register one with
 * {@link AbilityScores#register(net.minecraft.util.Identifier, AbilityContributor)}: races, magic items,
 * subclasses, conditions and obstacles all plug in here instead of editing the sheet code.
 *
 * Contributors run on the server only. Call {@link AbilityScores#invalidate} when whatever a
 * contributor reads changes (race picked, item attuned), or register it as dynamic to be re-read at
 * most once a second.
 */
@FunctionalInterface
public interface AbilityContributor {
    void contribute(PlayerEntity player, Contribution contribution);
}
