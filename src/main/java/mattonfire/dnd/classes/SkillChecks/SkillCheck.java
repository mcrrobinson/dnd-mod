package mattonfire.dnd.classes.SkillChecks;

import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.Abilities.AbilityScores;
import mattonfire.dnd.classes.Abilities.CharacterSheet;
import mattonfire.dnd.classes.Abilities.RollKind;
import mattonfire.dnd.classes.Abilities.RollQuery;
import mattonfire.dnd.classes.Abilities.Skill;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Checks and saves that use the player's character sheet: the modifier is the ability modifier plus
 * proficiency plus flat bonuses, and the sheet's advantage sources are applied. Add per-roll extras on
 * the builder before rolling:
 *
 * <pre>{@code
 * D20.Roll roll = SkillCheck.builder(player, Skill.ATHLETICS, 15, "obstacle")
 *         .bonus("obstacle.dndclasses.assist", 2)   // shown as "+2 assist"
 *         .advantageIf(inspired)
 *         .roll();
 * D20.show(player, roll, Text.translatable("obstacle.dndclasses.portcullis.success"));
 * }</pre>
 *
 * Tags (e.g. "obstacle", "poison", "trap") let advantage filters on the sheet pick out rolls.
 */
public final class SkillCheck {
    private SkillCheck() {
    }

    /** Rolls a skill check against a DC (not shown; pass it to {@link D20#show}). */
    public static D20.Roll check(PlayerEntity player, Skill skill, int dc) {
        return builder(player, skill, dc).roll();
    }

    /** A skill check: d20 + the sheet's bonus for the skill. */
    public static D20.Builder builder(PlayerEntity player, Skill skill, int dc, String... tags) {
        CharacterSheet sheet = AbilityScores.sheet(player);
        return D20.roll(player).label(skill.translationKey()).ability(skill.ability()).kind(RollKind.CHECK)
                .modifier(sheet.check(skill)).dc(dc).rerollNaturalOnes(sheet.rerollNaturalOnes())
                .sheetAdvantage(RollQuery.check(skill, tags));
    }

    /** A plain ability check (no skill), e.g. a STR check to force a door. */
    public static D20.Builder abilityCheck(PlayerEntity player, Ability ability, int dc, String... tags) {
        CharacterSheet sheet = AbilityScores.sheet(player);
        return D20.roll(player).label(ability.translationKey()).ability(ability).kind(RollKind.CHECK)
                .modifier(sheet.abilityCheck(ability)).dc(dc).rerollNaturalOnes(sheet.rerollNaturalOnes())
                .sheetAdvantage(RollQuery.abilityCheck(ability, tags));
    }

    /**
     * A bare saving throw: d20 + the sheet's save bonus, on the save lane. Monster and trap saves should
     * use the SavingThrow API (exposure cache, half damage) once it exists; this is the roll underneath.
     */
    public static D20.Builder save(PlayerEntity player, Ability ability, int dc, String... tags) {
        CharacterSheet sheet = AbilityScores.sheet(player);
        return D20.roll(player).label(ability.saveTranslationKey()).ability(ability).kind(RollKind.SAVE)
                .modifier(sheet.save(ability)).dc(dc).rerollNaturalOnes(sheet.rerollNaturalOnes())
                .sheetAdvantage(RollQuery.save(ability, tags)).display(D20.Display.SAVE_LANE);
    }

    /** A plain d20 with no sheet bonus (DM "flat" rolls, death saves pass kind DEATH). */
    public static D20.Builder flat(PlayerEntity player, int dc) {
        return D20.roll(player).label("skill.dndclasses.flat").kind(RollKind.FLAT).dc(dc);
    }
}
