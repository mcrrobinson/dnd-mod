package mattonfire.dnd.classes.Abilities;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Progression.ClassProgress;
import mattonfire.dnd.classes.Progression.Progression;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Ability score improvements: {@link ClassProgress#ASI_POINTS_PER_LEVEL} ability points at each of
 * {@link ClassProgress#ASI_LEVELS}, each spent for +1 to a score at an Attunement Table (C2S
 * {@link #C2S_ABILITY_INCREASE}, handled by {@link Progression#increaseAbility}). The points are stored per class
 * in {@link ClassProgress#increases} and reach the sheet through the {@link #ASI} contributor.
 */
public final class AbilityIncreases {
    public static final Identifier ASI = new Identifier(DnDClasses.MOD_ID, "asi");
    /** C2S: the ability's short key ("DEX"). */
    public static final Identifier C2S_ABILITY_INCREASE = new Identifier(DnDClasses.MOD_ID, "ability_increase");
    /** Source text of the contribution, as shown in the sheet's breakdown. */
    public static final String SOURCE = "Ability Score Improvement";

    private AbilityIncreases() {
    }

    /** The {@link #ASI} contributor: +1 per point spent in the current class. */
    static void contribute(PlayerEntity player, Contribution c) {
        if (Progression.classOf(player) == DndCharacter.NONE) {
            return;
        }
        ClassProgress progress = Progression.current(player);
        for (Ability a : Ability.values()) {
            int n = progress.increase(a);
            if (n > 0) {
                c.add(a, n, SOURCE);
            }
        }
    }

    /**
     * The score before items and overrides: the base plus everything added to it (race, improvements), uncapped.
     * Worked out from the sheet's breakdown lines, so it works on the client's copy too.
     */
    public static int naturalScore(CharacterSheet sheet, Ability ability) {
        int base = 10;
        int adds = 0;
        for (CharacterSheet.Line line : sheet.linesFor(ability.shortKey())) {
            String detail = line.detail();
            if (detail.matches("\\d+")) {
                base = Integer.parseInt(detail);
            } else if (detail.matches("[+-]\\d+")) {
                adds += Integer.parseInt(detail);
            }
        }
        return base + adds;
    }

    /** Whether one more improvement would still raise the score (the natural cap is {@link Contribution#NATURAL_CAP}). */
    public static boolean belowCap(CharacterSheet sheet, Ability ability) {
        return naturalScore(sheet, ability) < Contribution.NATURAL_CAP;
    }
}
