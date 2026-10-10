package mattonfire.dnd.classes.Abilities;

/**
 * Which rolls an advantage or disadvantage applies to. Combine with {@link #and} / {@link #or}, e.g.
 * {@code RollFilter.save(Ability.CON).and(RollFilter.tag("poison"))} for a Dwarf's poison resilience.
 */
@FunctionalInterface
public interface RollFilter {
    boolean test(RollQuery query);

    default RollFilter and(RollFilter other) {
        return q -> test(q) && other.test(q);
    }

    default RollFilter or(RollFilter other) {
        return q -> test(q) || other.test(q);
    }

    static RollFilter any() {
        return q -> true;
    }

    static RollFilter kind(RollKind kind) {
        return q -> q.kind() == kind;
    }

    static RollFilter allChecks() {
        return kind(RollKind.CHECK);
    }

    static RollFilter allSaves() {
        return kind(RollKind.SAVE);
    }

    /** Checks with this skill. */
    static RollFilter check(Skill skill) {
        return q -> q.kind() == RollKind.CHECK && q.skill() == skill;
    }

    /** Saving throws with this ability. */
    static RollFilter save(Ability ability) {
        return q -> q.kind() == RollKind.SAVE && q.ability() == ability;
    }

    /** Checks and saves that use this ability (including its skills). */
    static RollFilter ability(Ability ability) {
        return q -> (q.kind() == RollKind.CHECK || q.kind() == RollKind.SAVE) && q.ability() == ability;
    }

    static RollFilter tag(String tag) {
        return q -> q.hasTag(tag);
    }
}
