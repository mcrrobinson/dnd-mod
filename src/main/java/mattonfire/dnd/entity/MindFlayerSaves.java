package mattonfire.dnd.entity;

import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.SkillChecks.MobSaveInfo;

/**
 * The Mind Flayer's saves, registered with {@link MobSaveInfo} so a Study card lists them.
 * <ul>
 * <li><b>Mind Blast</b>: INT {@value #MIND_BLAST_DC}, half damage on a success, Stunned on a failure (with an INT
 * repeat save at the same DC).</li>
 * <li><b>Dominate</b>: WIS {@value #DOMINATE_DC} for hostile mobs it tries to make its thralls (players are never
 * dominated).</li>
 * </ul>
 */
public final class MindFlayerSaves {
    public static final int MIND_BLAST_DC = 14;
    public static final int DOMINATE_DC = 14;
    /** Escape DC for a player struggling out of its tentacles (jump). */
    public static final int GRAPPLE_DC = 14;

    public static final MobSaveInfo.Entry MIND_BLAST = MobSaveInfo.register(ModEntityTypes.MIND_FLAYER,
            "save.dndclasses.mind_blast", Ability.INT, MIND_BLAST_DC, true, "save.dndclasses.effect.mind_blast");
    public static final MobSaveInfo.Entry DOMINATE = MobSaveInfo.register(ModEntityTypes.MIND_FLAYER,
            "save.dndclasses.mind_flayer_dominate", Ability.WIS, DOMINATE_DC, false,
            "save.dndclasses.effect.mind_flayer_dominate");

    private MindFlayerSaves() {
    }

    /** Registers the entries above with {@link MobSaveInfo}; call once after the entity types. */
    public static void register() {
        // Loading the class registers the entries
    }
}
