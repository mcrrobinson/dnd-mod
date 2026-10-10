package mattonfire.dnd.classes.Abilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * What the {@link AbilityContributor}s add to a sheet, collected in registration order and turned into a
 * {@link CharacterSheet} by {@link #build}. Every call takes a source text, which the sheet's breakdown
 * (tooltips, {@code /dndclass sheet}) shows.
 *
 * Scores: base (the class) + {@link #add} is capped at {@link #NATURAL_CAP}; {@link #setAtLeast} (items
 * like Gauntlets of Ogre Power) can go past it, and {@link #override} (admin) replaces the result.
 */
public final class Contribution {
    /** Race and ability score improvements can't push a score past this. */
    public static final int NATURAL_CAP = 20;
    public static final int MIN_SCORE = 1;
    public static final int MAX_SCORE = 30;

    private static final int ABILITIES = Ability.values().length;
    private static final int SKILLS = Skill.values().length;

    final int[] base = filled(ABILITIES, 10);
    final int[] adds = new int[ABILITIES];
    final int[] atLeast = new int[ABILITIES];
    final int[] overrides = filled(ABILITIES, -1);
    final int[] saveFlat = new int[ABILITIES];
    final int[] checkFlat = new int[ABILITIES];
    final int[] skillFlat = new int[SKILLS];
    final Proficiency[] saveProf = filledProf(ABILITIES);
    final Proficiency[] skillProf = filledProf(SKILLS);
    Proficiency checkProfFloor = Proficiency.NONE;
    final List<CharacterSheet.AdvantageSource> advantages = new ArrayList<>();
    boolean rerollNaturalOnes;
    int critRange = 20;
    final List<CharacterSheet.Line> lines = new ArrayList<>();

    Contribution() {
    }

    private static int[] filled(int n, int value) {
        int[] a = new int[n];
        Arrays.fill(a, value);
        return a;
    }

    private static Proficiency[] filledProf(int n) {
        Proficiency[] a = new Proficiency[n];
        Arrays.fill(a, Proficiency.NONE);
        return a;
    }

    private static String signed(int n) {
        return n < 0 ? Integer.toString(n) : "+" + n;
    }

    private void line(String source, String target, String detail) {
        lines.add(new CharacterSheet.Line(source, target, detail));
    }

    /** Sets the starting score (the class's array). */
    public Contribution base(Ability ability, int score, String source) {
        base[ability.ordinal()] = score;
        line(source, ability.shortKey(), Integer.toString(score));
        return this;
    }

    /** Adds to a score (race +2, an ability score improvement +1). Capped at {@link #NATURAL_CAP} with the base. */
    public Contribution add(Ability ability, int amount, String source) {
        adds[ability.ordinal()] += amount;
        line(source, ability.shortKey(), signed(amount));
        return this;
    }

    /** The score is at least this (Gauntlets of Ogre Power: STR 19). Not capped. */
    public Contribution setAtLeast(Ability ability, int score, String source) {
        atLeast[ability.ordinal()] = Math.max(atLeast[ability.ordinal()], score);
        line(source, ability.shortKey(), "at least " + score);
        return this;
    }

    /** Replaces the score outright (admin testing). The last override wins. */
    public Contribution override(Ability ability, int score, String source) {
        overrides[ability.ordinal()] = score;
        line(source, ability.shortKey(), "set to " + score);
        return this;
    }

    /** Flat bonus to one save, or to every save when ability is null (Cloak of Protection +1). */
    public Contribution saveBonus(@Nullable Ability ability, int amount, String source) {
        for (Ability a : ability == null ? Ability.values() : new Ability[] { ability }) {
            saveFlat[a.ordinal()] += amount;
        }
        line(source, ability == null ? "save:ALL" : "save:" + ability.shortKey(), signed(amount));
        return this;
    }

    /** Flat bonus to checks with one skill. */
    public Contribution skillBonus(Skill skill, int amount, String source) {
        skillFlat[skill.ordinal()] += amount;
        line(source, "skill:" + skill.name(), signed(amount));
        return this;
    }

    /** Flat bonus to every check (skill or plain) using an ability, or every check when ability is null. */
    public Contribution checkBonus(@Nullable Ability ability, int amount, String source) {
        for (Ability a : ability == null ? Ability.values() : new Ability[] { ability }) {
            checkFlat[a.ordinal()] += amount;
        }
        line(source, ability == null ? "check:ALL" : "check:" + ability.shortKey(), signed(amount));
        return this;
    }

    /** Proficiency in a skill. The best level from any source counts. */
    public Contribution proficiency(Skill skill, Proficiency level, String source) {
        skillProf[skill.ordinal()] = skillProf[skill.ordinal()].max(level);
        line(source, "skill:" + skill.name(), level.name().toLowerCase());
        return this;
    }

    /** Proficiency in a saving throw. */
    public Contribution saveProficiency(Ability ability, Proficiency level, String source) {
        saveProf[ability.ordinal()] = saveProf[ability.ordinal()].max(level);
        line(source, "save:" + ability.shortKey(), level.name().toLowerCase());
        return this;
    }

    /** At least this proficiency on every check, skill or not (Jack of All Trades: HALF). */
    public Contribution checkProficiencyFloor(Proficiency level, String source) {
        checkProfFloor = checkProfFloor.max(level);
        line(source, "check:ALL", level.name().toLowerCase() + " proficiency");
        return this;
    }

    /** Advantage on matching rolls (Rage: STR saves; a Help action: one obstacle check). */
    public Contribution advantage(RollFilter filter, String description, String source) {
        advantages.add(new CharacterSheet.AdvantageSource(filter, true, description, source));
        line(source, "adv", description);
        return this;
    }

    /** Disadvantage on matching rolls (heavy armour on Stealth, exhaustion). */
    public Contribution disadvantage(RollFilter filter, String description, String source) {
        advantages.add(new CharacterSheet.AdvantageSource(filter, false, description, source));
        line(source, "dis", description);
        return this;
    }

    /** Reroll a natural 1 once on checks and saves (Halfling Lucky). */
    public Contribution rerollNaturalOnes(String source) {
        rerollNaturalOnes = true;
        line(source, "reroll", "reroll natural 1s");
        return this;
    }

    /** Attacks crit on this natural roll or higher. The lowest from any source counts. */
    public Contribution critRange(int lowest, String source) {
        critRange = Math.min(critRange, Math.max(2, lowest));
        line(source, "crit", "crit on " + lowest + "-20");
        return this;
    }

    CharacterSheet build(mattonfire.dnd.classes.DndCharacter dndClass, int level, int proficiencyBonus) {
        int[] scores = new int[ABILITIES];
        for (int i = 0; i < ABILITIES; i++) {
            int natural = base[i] + adds[i];
            if (natural > NATURAL_CAP) {
                natural = Math.max(base[i], NATURAL_CAP);
            }
            int score = Math.max(natural, atLeast[i]);
            if (overrides[i] >= 0) {
                score = overrides[i];
            }
            scores[i] = Math.max(MIN_SCORE, Math.min(MAX_SCORE, score));
        }
        return new CharacterSheet(dndClass, level, scores, proficiencyBonus, saveProf.clone(), saveFlat.clone(),
                skillProf.clone(), skillFlat.clone(), checkFlat.clone(), checkProfFloor, critRange,
                rerollNaturalOnes, List.copyOf(advantages), List.copyOf(lines));
    }
}
