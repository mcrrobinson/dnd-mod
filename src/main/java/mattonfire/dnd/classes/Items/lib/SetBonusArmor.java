package mattonfire.dnd.classes.Items.lib;

import java.util.List;
import java.util.Set;

import mattonfire.dnd.classes.DndCharacter;
import net.minecraft.entity.effect.StatusEffectInstance;

/**
 * Armor that grants effects while a full set of it is worn.
 *
 * A wearer whose class is one of {@link #getMatchingClasses()} gets a boosted
 * bonus: every {@link #getFullSetEffects()} effect one level higher, plus the
 * {@link #getMatchingClassEffects()} extras. Anyone else gets the normal bonus.
 */
public interface SetBonusArmor {
    /** Effects for anyone wearing the full set. */
    List<StatusEffectInstance> getFullSetEffects();

    /** Classes this set is made for. */
    default Set<DndCharacter> getMatchingClasses() {
        return Set.of();
    }

    /** Extra effects only for a wearer of a matching class. */
    default List<StatusEffectInstance> getMatchingClassEffects() {
        return List.of();
    }
}
