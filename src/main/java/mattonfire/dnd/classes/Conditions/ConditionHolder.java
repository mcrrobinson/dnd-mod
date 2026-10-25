package mattonfire.dnd.classes.Conditions;

import java.util.List;

/**
 * Internal: what {@code ConditionsTrackerMixin} adds to every {@code LivingEntity}. Use {@link Conditions} instead.
 */
public interface ConditionHolder {
    /** The synced mask (both sides). */
    int dnd$getConditionMask();

    /** Server: sets the synced mask. */
    void dnd$setConditionMask(int mask);

    /** Server: the live instances (mutable, owned by {@link Conditions}). */
    List<ConditionInstance> dnd$conditions();

    /** Server: whether the persistent instances have been read into the live list. */
    boolean dnd$conditionsLoaded();

    void dnd$setConditionsLoaded(boolean loaded);
}
