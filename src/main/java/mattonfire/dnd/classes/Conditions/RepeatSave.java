package mattonfire.dnd.classes.Conditions;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.Abilities.Ability;

/**
 * A repeat save that can end a {@link ConditionInstance}: rolled every {@code every} ticks, the next one at world
 * time {@code nextAt}. A success ends the instance; a failure keeps it, or with {@code escalateTo} swaps it for that
 * condition for {@code escalateTicks}.
 */
public record RepeatSave(Ability ability, int dc, int every, String labelKey, List<String> tags,
        @Nullable Condition escalateTo, int escalateTicks, long nextAt) {

    public RepeatSave withNextAt(long next) {
        return new RepeatSave(ability, dc, every, labelKey, tags, escalateTo, escalateTicks, next);
    }
}
