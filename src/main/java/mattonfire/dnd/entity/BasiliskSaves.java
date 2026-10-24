package mattonfire.dnd.entity;

import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.SkillChecks.MobSaveInfo;

/**
 * The Basilisk's saves (design 8, section 7), registered with {@link MobSaveInfo} so the Study card lists them.
 * <ul>
 * <li>{@link #GAZE}: its passive petrifying gaze, CON {@value #GAZE_DC}. A failure starts turning you to stone
 * (Restrained, a CON repeat save every {@value #GAZE_REPEAT_TICKS} ticks; a failed repeat makes you Petrified for
 * {@value #PETRIFIED_TICKS} ticks); failing by {@value #FAIL_BY} or more petrifies you at once.</li>
 * <li>{@link #BITE}: its venomous bite, CON {@value #BITE_DC}: a failure is Poisoned (and vanilla Poison I) for
 * {@value #POISON_TICKS} ticks.</li>
 * </ul>
 */
public final class BasiliskSaves {
    public static final int GAZE_DC = 12;
    public static final int BITE_DC = 12;
    /** Ticks between the repeat saves while you're turning to stone. */
    public static final int GAZE_REPEAT_TICKS = 40;
    /** How long a player stays Petrified (a solo player isn't stuck: open question 1). */
    public static final int PETRIFIED_TICKS = 200;
    /** Failing the gaze save by this much or more skips Restrained. */
    public static final int FAIL_BY = 5;
    public static final int POISON_TICKS = 100;
    /** The condition key of the gaze's Restrained and Petrified, which the Salve of Stone to Flesh removes. */
    public static final String PETRIFYING_KEY = "petrifying";

    public static final MobSaveInfo.Entry GAZE = MobSaveInfo.register(ModEntityTypes.BASILISK,
            "save.dndclasses.basilisk_gaze", Ability.CON, GAZE_DC, false, "save.dndclasses.effect.basilisk_gaze");
    public static final MobSaveInfo.Entry BITE = MobSaveInfo.register(ModEntityTypes.BASILISK,
            "save.dndclasses.basilisk_bite", Ability.CON, BITE_DC, false, "save.dndclasses.effect.basilisk_bite");

    private BasiliskSaves() {
    }

    /** Registers the entries above with {@link MobSaveInfo}; call once after the entity types. */
    public static void register() {
        // Loading the class registers the entries
    }
}
