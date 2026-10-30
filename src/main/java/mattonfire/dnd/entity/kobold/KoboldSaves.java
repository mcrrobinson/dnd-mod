package mattonfire.dnd.entity.kobold;

import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.SkillChecks.MobSaveInfo;

/**
 * The kobolds' saves, registered with {@link MobSaveInfo} so the Study card lists them: the Trapmaker's
 * {@link #SNARE} (DEX {@value #SNARE_DC}: a failure is Restrained, see {@link KoboldSnares}).
 */
public final class KoboldSaves {
    public static final int SNARE_DC = 12;

    public static final MobSaveInfo.Entry SNARE = MobSaveInfo.register(Kobolds.KOBOLD_TRAPMAKER,
            "save.dndclasses.kobold_snare", Ability.DEX, SNARE_DC, false, "save.dndclasses.effect.kobold_snare");

    private KoboldSaves() {
    }

    /** Loading the class registers the entries above; call once after the entity types. */
    public static void register() {
    }
}
