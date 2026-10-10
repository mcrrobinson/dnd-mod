package mattonfire.dnd.classes.Abilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.ClassInfo;
import mattonfire.dnd.classes.DndCharacter;

/**
 * A class's starting ability scores, saving throw proficiencies, skill proficiencies and expertise, from
 * the {@code abilities}, {@code saves}, {@code skills} and {@code expertise} fields of
 * {@code class_info.json}.
 */
public record ClassAbilities(int[] scores, Set<Ability> saves, Set<Skill> skills, Set<Skill> expertise) {
    /** The 5e standard array: every class's scores are a permutation of it. */
    public static final List<Integer> STANDARD_ARRAY = List.of(15, 14, 13, 12, 10, 8);
    public static final int MAX_SAVES = 2;

    /** The classless player: 10 in everything, no proficiencies. */
    public static final ClassAbilities NONE = new ClassAbilities(new int[] { 10, 10, 10, 10, 10, 10 },
            Collections.emptySet(), Collections.emptySet(), Collections.emptySet());

    public static ClassAbilities of(@Nullable DndCharacter dndClass) {
        ClassInfo info = ClassInfo.get(dndClass);
        return info == null || info.abilities() == null ? NONE : info.abilities();
    }

    public int score(Ability ability) {
        return scores[ability.ordinal()];
    }

    public Proficiency proficiency(Skill skill) {
        return expertise.contains(skill) ? Proficiency.EXPERTISE
                : skills.contains(skill) ? Proficiency.PROFICIENT : Proficiency.NONE;
    }

    /**
     * Parses and validates one class's fields. Problems are added to errors (and null is returned) rather
     * than thrown, so {@link #validateAll} can report every broken class at once.
     */
    @Nullable
    public static ClassAbilities parse(String classId, @Nullable Map<String, Integer> abilities,
            @Nullable List<String> saves, @Nullable List<String> skills, @Nullable List<String> expertise,
            List<String> errors) {
        int before = errors.size();
        if (abilities == null) {
            errors.add(classId + ": missing \"abilities\"");
            return null;
        }
        int[] scores = new int[Ability.values().length];
        for (Ability a : Ability.values()) {
            Integer v = abilities.get(a.shortKey());
            if (v == null) {
                errors.add(classId + ": abilities is missing " + a.shortKey());
            } else {
                scores[a.ordinal()] = v;
            }
        }
        for (String key : abilities.keySet()) {
            if (Ability.byId(key) == null || !key.equals(key.toUpperCase())) {
                errors.add(classId + ": unknown ability \"" + key + "\"");
            }
        }
        List<Integer> sorted = new ArrayList<>(Arrays.stream(scores).boxed().toList());
        sorted.sort(Collections.reverseOrder());
        if (!sorted.equals(STANDARD_ARRAY)) {
            errors.add(classId + ": abilities " + Arrays.toString(scores) + " aren't a permutation of the standard array "
                    + STANDARD_ARRAY);
        }
        Set<Ability> saveSet = EnumSet.noneOf(Ability.class);
        for (String s : saves == null ? List.<String>of() : saves) {
            Ability a = Ability.byId(s);
            if (a == null) {
                errors.add(classId + ": unknown save \"" + s + "\"");
            } else {
                saveSet.add(a);
            }
        }
        if (saveSet.size() > MAX_SAVES) {
            errors.add(classId + ": " + saveSet.size() + " save proficiencies (at most " + MAX_SAVES + ")");
        }
        Set<Skill> skillSet = parseSkills(classId, "skills", skills, errors);
        Set<Skill> expertiseSet = parseSkills(classId, "expertise", expertise, errors);
        for (Skill s : expertiseSet) {
            if (!skillSet.contains(s)) {
                errors.add(classId + ": expertise in " + s + " but it isn't in skills");
            }
        }
        if (errors.size() > before) {
            return null;
        }
        return new ClassAbilities(scores, Collections.unmodifiableSet(saveSet), Collections.unmodifiableSet(skillSet),
                Collections.unmodifiableSet(expertiseSet));
    }

    private static Set<Skill> parseSkills(String classId, String field, @Nullable List<String> names,
            List<String> errors) {
        Set<Skill> set = EnumSet.noneOf(Skill.class);
        for (String name : names == null ? List.<String>of() : names) {
            Skill skill = Skill.byId(name);
            if (skill == null) {
                errors.add(classId + ": unknown skill \"" + name + "\" in " + field);
            } else {
                set.add(skill);
            }
        }
        return set;
    }

    /** Fails startup if any class's ability data is missing or invalid. */
    public static void validateAll() {
        List<String> errors = new ArrayList<>(ClassInfo.abilityErrors());
        for (DndCharacter c : DndCharacter.values()) {
            if (c != DndCharacter.NONE && ClassInfo.get(c) == null) {
                errors.add(c + ": not in class_info.json");
            }
        }
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Invalid ability data in " + ClassInfo.RESOURCE + ":\n  "
                    + String.join("\n  ", errors));
        }
    }
}
