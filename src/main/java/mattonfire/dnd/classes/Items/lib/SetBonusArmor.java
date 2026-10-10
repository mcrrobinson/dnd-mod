package mattonfire.dnd.classes.Items.lib;

import java.util.List;
import java.util.Set;

import mattonfire.dnd.classes.DndCharacter;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Armor that grants effects while a full set of it is worn.
 *
 * A wearer whose class is one of {@link #getMatchingClasses()} gets a boosted
 * bonus: every {@link #getFullSetEffects()} effect one level higher, plus the
 * {@link #getMatchingClassEffects()} extras. A wearer whose race is one of
 * {@link #getMatchingRaces()} (racial gear) gets the {@link #getMatchingRaceEffects()}
 * extras. Anyone else gets the normal bonus.
 */
public interface SetBonusArmor extends RacialGearItem {
    /** Effects for anyone wearing the full set. */
    List<StatusEffectInstance> getFullSetEffects();

    /**
     * The full-set effects for this wearer right now. Defaults to {@link #getFullSetEffects()}; a set whose
     * bonus depends on the wearer's state (e.g. low health) filters it here.
     */
    default List<StatusEffectInstance> getFullSetEffects(PlayerEntity wearer) {
        return getFullSetEffects();
    }

    /** Classes this set is made for. */
    default Set<DndCharacter> getMatchingClasses() {
        return Set.of();
    }

    /** Extra effects only for a wearer of a matching class. */
    default List<StatusEffectInstance> getMatchingClassEffects() {
        return List.of();
    }

    /** Extra effects only for a wearer of a matching race (see {@link #getMatchingRaces()}). */
    default List<StatusEffectInstance> getMatchingRaceEffects(PlayerEntity wearer) {
        return List.of();
    }
}
