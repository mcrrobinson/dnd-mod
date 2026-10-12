package mattonfire.dnd.entity;

import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.SkillChecks.Grapple;
import mattonfire.dnd.classes.SkillChecks.MobSaveInfo;
import mattonfire.dnd.classes.SkillChecks.SaveResult;
import mattonfire.dnd.classes.SkillChecks.SavingThrow;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;

/**
 * Saving throws against grabs and shoves: the Mimic's jaws, the Owlbear's hug and charge, and the Gelatinous
 * Cube's engulf. Each grab attempt is one save (no exposure window: the mobs' own cooldowns space them out).
 * The entries are registered with {@link MobSaveInfo} for creature study, and each call site rolls from its entry.
 * <p>
 * Once held by a Mimic or an Owlbear, a player can struggle free with jump ({@link Grapple}) against the same DC.
 */
public final class GrabSaves {
    public static final int MIMIC_GRAB_DC = 12;
    public static final int OWLBEAR_HUG_DC = 13;
    public static final int OWLBEAR_CHARGE_DC = 13;
    public static final int CUBE_ENGULF_DC = 12;
    /** Ticks a cube won't engulf someone who slipped out of it. */
    public static final int CUBE_SPURN_TICKS = 20;

    public static final MobSaveInfo.Entry MIMIC_GRAB = MobSaveInfo.register(ModEntityTypes.MIMIC,
            "save.dndclasses.mimic_grab", Ability.STR, MIMIC_GRAB_DC, false, "save.dndclasses.effect.mimic_grab");
    public static final MobSaveInfo.Entry OWLBEAR_HUG = MobSaveInfo.register(ModEntityTypes.OWLBEAR,
            "save.dndclasses.owlbear_hug", Ability.STR, OWLBEAR_HUG_DC, false, "save.dndclasses.effect.owlbear_hug");
    public static final MobSaveInfo.Entry OWLBEAR_CHARGE = MobSaveInfo.register(ModEntityTypes.OWLBEAR,
            "save.dndclasses.owlbear_charge", Ability.STR, OWLBEAR_CHARGE_DC, false,
            "save.dndclasses.effect.owlbear_charge");
    public static final MobSaveInfo.Entry CUBE_ENGULF = MobSaveInfo.register(ModEntityTypes.GELATINOUS_CUBE,
            "save.dndclasses.cube_engulf", Ability.DEX, CUBE_ENGULF_DC, false, "save.dndclasses.effect.cube_engulf");

    private GrabSaves() {
    }

    /** Registers the entries above with {@link MobSaveInfo}; call once after the entity types. */
    public static void register() {
        // Loading the class registers the entries
    }

    /**
     * The victim's save against a grab or shove from its registered entry, tagged {@value Grapple#TAG} for
     * advantage filters. The lane shows the entry's effect on a success and {@code <effect>.failure} on a failure.
     */
    public static SaveResult roll(LivingEntity victim, Entity attacker, MobSaveInfo.Entry entry) {
        return SavingThrow.of(victim, entry)
                .source(attacker)
                .tags(Grapple.TAG)
                .onSuccess(Text.translatable(entry.effectKey()))
                .onFailure(Text.translatable(entry.effectKey() + ".failure"))
                .roll();
    }
}
