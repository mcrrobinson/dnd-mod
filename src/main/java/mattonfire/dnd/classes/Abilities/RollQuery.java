package mattonfire.dnd.classes.Abilities;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

/**
 * Describes a roll about to be made, so {@link RollFilter}s on the sheet can decide whether their
 * advantage or disadvantage applies. {@code tags} are free-form keywords from the caller, e.g.
 * "poison", "obstacle", "trap", "fear", "grapple".
 */
public record RollQuery(RollKind kind, @Nullable Ability ability, @Nullable Skill skill, Set<String> tags) {
    public static RollQuery check(Skill skill, String... tags) {
        return new RollQuery(RollKind.CHECK, skill.ability(), skill, Set.of(tags));
    }

    public static RollQuery abilityCheck(Ability ability, String... tags) {
        return new RollQuery(RollKind.CHECK, ability, null, Set.of(tags));
    }

    public static RollQuery save(Ability ability, String... tags) {
        return new RollQuery(RollKind.SAVE, ability, null, Set.of(tags));
    }

    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }
}
