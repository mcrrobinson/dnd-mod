package mattonfire.dnd.classes.Obstacles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.SkillChecks.Eligibility;
import org.jetbrains.annotations.Nullable;

/**
 * Who can get past what, read from the obstacle registry ({@link ObstacleTypes}): the guidebook's Obstacle
 * Codex chapter, the docs catalogue table and the "every class has a job" test all come from here.
 *
 * <p>The promise it keeps: every class (except {@link DndCharacter#NONE}) is the sole PRIMARY solver of at
 * least one obstacle type ({@link #classesWithoutExclusive}).
 */
public final class ObstacleCodex {
    private ObstacleCodex() {
    }

    /** The only PRIMARY solver of {@code type}, or null if it has none or several. */
    @Nullable
    public static DndCharacter exclusiveSolver(ObstacleType type) {
        DndCharacter only = null;
        for (DndCharacter c : DndCharacter.values()) {
            if (c != DndCharacter.NONE && type.eligibility(c) == Eligibility.PRIMARY) {
                if (only != null) {
                    return null;
                }
                only = c;
            }
        }
        return only;
    }

    /** The classes with this eligibility for {@code type}, in class order. */
    public static List<DndCharacter> classes(ObstacleType type, Eligibility eligibility) {
        List<DndCharacter> out = new ArrayList<>();
        for (DndCharacter c : DndCharacter.values()) {
            if (c != DndCharacter.NONE && type.eligibility(c) == eligibility) {
                out.add(c);
            }
        }
        return out;
    }

    /** Whether every other class may try it too (untrained). */
    public static boolean anyoneMayTry(ObstacleType type) {
        return !classes(type, Eligibility.UNTRAINED).isEmpty();
    }

    /** For each class, the types it alone is PRIMARY for (in registry order). Classes with none are left out. */
    public static Map<DndCharacter, List<ObstacleType>> exclusives(Collection<? extends ObstacleType> types) {
        Map<DndCharacter, List<ObstacleType>> out = new EnumMap<>(DndCharacter.class);
        for (ObstacleType type : types) {
            DndCharacter only = exclusiveSolver(type);
            if (only != null) {
                out.computeIfAbsent(only, c -> new ArrayList<>()).add(type);
            }
        }
        return out;
    }

    /** Classes (never NONE) that aren't the sole PRIMARY of any of {@code types}. Empty when the promise holds. */
    public static List<DndCharacter> classesWithoutExclusive(Collection<? extends ObstacleType> types) {
        Map<DndCharacter, List<ObstacleType>> exclusives = exclusives(types);
        List<DndCharacter> out = new ArrayList<>();
        for (DndCharacter c : DndCharacter.values()) {
            if (c != DndCharacter.NONE && !exclusives.containsKey(c)) {
                out.add(c);
            }
        }
        return out;
    }

    /**
     * The codex's entries: every registered type, minus look-alike variants (the same kind of obstacle with
     * the same solvers, such as the deepslate and blackstone Hidden Doors), which share the first one's entry.
     */
    public static List<ObstacleType> entries() {
        return entries(ObstacleTypes.all());
    }

    public static List<ObstacleType> entries(Collection<? extends ObstacleType> types) {
        List<ObstacleType> out = new ArrayList<>();
        for (ObstacleType type : types) {
            if (out.stream().noneMatch(seen -> variantOf(type, seen))) {
                out.add(type);
            }
        }
        return out;
    }

    /**
     * A type's fallback line ({@code <key>.fallback}) as a sentence of its own: the hint's "Or smash it..."
     * becomes "Smash it...".
     */
    public static String fallbackSentence(String fallback) {
        if (fallback.startsWith("Or ") && fallback.length() > 3) {
            return Character.toUpperCase(fallback.charAt(3)) + fallback.substring(4);
        }
        return fallback;
    }

    /** The look-alike variants that share {@code type}'s codex entry (not {@code type} itself). */
    public static List<ObstacleType> variants(ObstacleType type) {
        List<ObstacleType> out = new ArrayList<>();
        for (ObstacleType other : ObstacleTypes.all()) {
            if (other != type && variantOf(other, type)) {
                out.add(other);
            }
        }
        return out;
    }

    /** {@code type} is a reskin of {@code other}: the same class, and every class may try them alike. */
    public static boolean variantOf(ObstacleType type, ObstacleType other) {
        if (type.getClass() != other.getClass()) {
            return false;
        }
        for (DndCharacter c : DndCharacter.values()) {
            if (!Objects.equals(type.eligibility(c), other.eligibility(c))) {
                return false;
            }
        }
        return type.skill() == other.skill() && type.breakable() == other.breakable();
    }
}
