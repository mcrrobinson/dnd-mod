package mattonfire.dnd.entity.drow;

import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.SkillChecks.MobSaveInfo;

/**
 * The drow's saves (design 9, section 4), registered with {@link MobSaveInfo} so the Study card lists them.
 * <ul>
 * <li>Drow Poison, CON {@value #POISON_DC}: hand-crossbow bolts (warrior, elite) and the priestess's scourge
 * ({@link DrowPoison}).</li>
 * <li>Dread of the Spider Queen, WIS {@value #DREAD_DC}: the priestess's fear (Frightened for
 * {@value #DREAD_TICKS} ticks).</li>
 * <li>The Matron Mother (design 9, 8.3): her scourge's Drow Poison, CON {@value #MATRON_POISON_DC}; the Lash of the
 * Spider Queen, WIS {@value #MATRON_LASH_DC} (Frightened for {@value #MATRON_LASH_TICKS} ticks); the Web Storm, DEX
 * {@value #WEB_STORM_DC} (webs at your feet and Restrained).</li>
 * </ul>
 */
public final class DrowSaves {
    public static final int POISON_DC = 13;
    public static final int DREAD_DC = 13;
    public static final int DREAD_TICKS = 100;
    public static final int MATRON_POISON_DC = 14;
    public static final int MATRON_LASH_DC = 15;
    public static final int MATRON_LASH_TICKS = 120;
    public static final int WEB_STORM_DC = 14;

    public static final MobSaveInfo.Entry WARRIOR_POISON = MobSaveInfo.register(Drow.DROW_WARRIOR,
            "save.dndclasses.drow_poison", Ability.CON, POISON_DC, false, "save.dndclasses.effect.drow_poison");
    public static final MobSaveInfo.Entry ELITE_POISON = MobSaveInfo.register(Drow.DROW_ELITE,
            "save.dndclasses.drow_poison", Ability.CON, POISON_DC, false, "save.dndclasses.effect.drow_poison");
    public static final MobSaveInfo.Entry PRIESTESS_POISON = MobSaveInfo.register(Drow.DROW_PRIESTESS,
            "save.dndclasses.drow_poison", Ability.CON, POISON_DC, false, "save.dndclasses.effect.drow_poison");
    public static final MobSaveInfo.Entry DREAD = MobSaveInfo.register(Drow.DROW_PRIESTESS,
            "save.dndclasses.drow_dread", Ability.WIS, DREAD_DC, false, "save.dndclasses.effect.drow_dread");
    public static final MobSaveInfo.Entry MATRON_POISON = MobSaveInfo.register(Drow.DROW_MATRON,
            "save.dndclasses.drow_poison", Ability.CON, MATRON_POISON_DC, false, "save.dndclasses.effect.drow_poison");
    public static final MobSaveInfo.Entry MATRON_LASH = MobSaveInfo.register(Drow.DROW_MATRON,
            "save.dndclasses.matron_lash", Ability.WIS, MATRON_LASH_DC, false, "save.dndclasses.effect.matron_lash");
    public static final MobSaveInfo.Entry WEB_STORM = MobSaveInfo.register(Drow.DROW_MATRON,
            "save.dndclasses.web_storm", Ability.DEX, WEB_STORM_DC, false, "save.dndclasses.effect.web_storm");
    /** Drow Poison from a vial (no particular drow); the data-driven rule uses the same DC. */
    public static final MobSaveInfo.Entry DROW_POISON = WARRIOR_POISON;

    private DrowSaves() {
    }

    /** The poison save for a given drow (its own entry, so the label and DC match its card). */
    public static MobSaveInfo.Entry poisonOf(DrowEntity drow) {
        if (drow instanceof DrowEliteEntity) {
            return ELITE_POISON;
        }
        if (drow instanceof DrowMatronEntity) {
            return MATRON_POISON;
        }
        if (drow instanceof DrowPriestessEntity) {
            return PRIESTESS_POISON;
        }
        return WARRIOR_POISON;
    }

    static void register() {
        // Loading the class registers the entries
    }
}
