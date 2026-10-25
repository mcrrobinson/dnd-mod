package mattonfire.dnd.entity;

import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.SkillChecks.MobSaveInfo;
import mattonfire.dnd.classes.SkillChecks.Study;
import net.minecraft.text.Text;

/**
 * Saving throws against the Medusa ({@link MedusaEntity}): her Petrifying Gaze and her snake hair's poison. The
 * entries are registered with {@link MobSaveInfo} so creature study lists them, and each call site rolls from its
 * entry. The gaze is DC {@value #GAZE_DC}, one higher once she's enraged.
 */
public final class MedusaSaves {
    public static final int GAZE_DC = 14;
    public static final int ENRAGED_GAZE_DC = GAZE_DC + 1;
    public static final int SNAKES_DC = 13;

    /** One gaze save per player (and per mob) per this many ticks. */
    public static final int GAZE_WINDOW = 100;
    /** A failed gaze save: a repeat CON save this often while turning to stone. */
    public static final int PETRIFY_REPEAT_TICKS = 40;
    /** How long a player stays Petrified (a statue mob stays one for good). */
    public static final int PETRIFIED_TICKS = 300;
    /** Failing the gaze save by this much or more skips straight to Petrified. */
    public static final int FAIL_BY_PETRIFIES = 5;
    /** Snake bite: Poisoned (and vanilla Poison I) for this long on a failed save. */
    public static final int SNAKE_POISON_TICKS = 100;

    public static final MobSaveInfo.Entry GAZE = MobSaveInfo.register(ModEntityTypes.MEDUSA,
            "save.dndclasses.medusa_gaze", Ability.CON, GAZE_DC, false, "save.dndclasses.effect.medusa_gaze");
    public static final MobSaveInfo.Entry SNAKES = MobSaveInfo.register(ModEntityTypes.MEDUSA,
            "save.dndclasses.medusa_snakes", Ability.CON, SNAKES_DC, false, "save.dndclasses.effect.medusa_snakes");

    private MedusaSaves() {
    }

    /** Registers the entries above with {@link MobSaveInfo} and her Study weakness; call once after the entity types. */
    public static void register() {
        Study.registerWeakness(ModEntityTypes.MEDUSA, Text.translatable("study.dndclasses.weakness.medusa"));
    }
}
