package mattonfire.dnd.entity;

import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.SkillChecks.MobSaveInfo;
import mattonfire.dnd.classes.SkillChecks.SaveResult;
import mattonfire.dnd.classes.SkillChecks.SavingThrow;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;

/**
 * Saving throws against the Beholder's eye rays and the Lich's spells (see {@link DragonSaves} for the dragons).
 * <p>
 * Bosses are DC {@link #BOSS_DC}, one higher ({@link #LAST_PHASE_DC}) once the Beholder is enraged or the Lich is
 * in its last phase. Every save is one roll per exposure: one ray, one Frost Nova, one Dread Presence; later hits
 * of the same exposure reuse the first roll. The entries are registered with {@link MobSaveInfo} so creature
 * study lists them, and each call site rolls from its entry so the two can't drift apart.
 */
public final class BossSaves {
    /** A boss's save DC. */
    public static final int BOSS_DC = 15;
    /** An enraged / last-phase boss's save DC. */
    public static final int LAST_PHASE_DC = BOSS_DC + 1;

    /** One ray is one exposure; the same eye can't fire again for at least 34 ticks (charge + enraged cooldown). */
    public static final int RAY_WINDOW = 10;
    /** One Frost Nova (a single burst) is one exposure. */
    public static final int NOVA_WINDOW = 20;
    /** Phases entered in the same tick (a huge hit) share one Dread Presence save. */
    public static final int DREAD_WINDOW = 40;

    /** Players this close to the Lich save against its Dread Presence. */
    public static final double DREAD_RADIUS = 24.0;
    /** How long a failed Dread Presence save leaves a player Frightened. */
    public static final int DREAD_FRIGHTENED_TICKS = 160;
    /** How long a failed fear ray save leaves a creature Frightened. */
    public static final int FEAR_RAY_TICKS = 160;
    /** Frightened by a boss: a repeat WIS save to shake it off this often (the entry's DC, +1 in the last phase). */
    public static final int FEAR_REPEAT_TICKS = 60;

    public static final MobSaveInfo.Entry SLOWING_RAY = MobSaveInfo.register(ModEntityTypes.BEHOLDER,
            "save.dndclasses.slowing_ray", Ability.DEX, BOSS_DC, false, "save.dndclasses.effect.slowing_ray");
    public static final MobSaveInfo.Entry TELEKINETIC_RAY = MobSaveInfo.register(ModEntityTypes.BEHOLDER,
            "save.dndclasses.telekinetic_ray", Ability.STR, BOSS_DC, false, "save.dndclasses.effect.telekinetic_ray");
    public static final MobSaveInfo.Entry ENERVATION_RAY = MobSaveInfo.register(ModEntityTypes.BEHOLDER,
            "save.dndclasses.enervation_ray", Ability.CON, BOSS_DC, true, "save.dndclasses.effect.half_damage");
    public static final MobSaveInfo.Entry FEAR_RAY = MobSaveInfo.register(ModEntityTypes.BEHOLDER,
            "save.dndclasses.fear_ray", Ability.WIS, BOSS_DC, false, "save.dndclasses.effect.fear_ray");

    public static final MobSaveInfo.Entry FROST_NOVA = MobSaveInfo.register(ModEntityTypes.LICH,
            "save.dndclasses.frost_nova", Ability.CON, BOSS_DC, true, "save.dndclasses.effect.frost_nova");
    public static final MobSaveInfo.Entry DREAD_PRESENCE = MobSaveInfo.register(ModEntityTypes.LICH,
            "save.dndclasses.dread_presence", Ability.WIS, BOSS_DC, false, "save.dndclasses.effect.dread_presence");

    private BossSaves() {
    }

    /** Registers the entries above with {@link MobSaveInfo}; call once after the entity types. */
    public static void register() {
        // Loading the class registers the entries
    }

    /**
     * The victim's save against a boss attack from its registered entry, one roll per exposure ({@code tag} within
     * {@code window} ticks), at the last-phase DC if {@code lastPhase}. {@code tags} feed advantage filters
     * ("fear").
     */
    public static SavingThrow.Builder save(LivingEntity victim, Entity boss, MobSaveInfo.Entry entry, boolean lastPhase,
            String tag, int window) {
        return SavingThrow.of(victim, entry)
                .dc(lastPhase ? LAST_PHASE_DC : entry.dc())
                .source(boss)
                .exposure(boss, tag, window)
                .onSuccess(Text.translatable(entry.effectKey()));
    }

    /** {@link #save} then roll. */
    public static SaveResult roll(LivingEntity victim, Entity boss, MobSaveInfo.Entry entry, boolean lastPhase,
            String tag, int window) {
        return save(victim, boss, entry, lastPhase, tag, window).roll();
    }
}
