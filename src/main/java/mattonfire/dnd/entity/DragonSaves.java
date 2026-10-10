package mattonfire.dnd.entity;

import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.SkillChecks.MobSaveInfo;
import mattonfire.dnd.classes.SkillChecks.SaveResult;
import mattonfire.dnd.classes.SkillChecks.SavingThrow;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.text.Text;

/**
 * DEX saves against dragons: their breath and their storms (lightning bolts, hail).
 * <p>
 * One save per exposure: every hit of one breath reuses the first roll ({@link #BREATH_WINDOW}), and the three
 * bolts or hailstones of one storm share one roll ({@link #STORM_WINDOW}). A success halves the damage through
 * {@link SaveResult#damage}, so features that change half-damage saves (Evasion) apply to every dragon at once;
 * what a success does to the after-effects (shorter burn, no Freeze) is up to each attack.
 * <p>
 * The DCs live on the dragons ({@link FireBreather#getBreathSaveDc()} and the constants it returns); the
 * entries here are registered with {@link MobSaveInfo} from those same constants, for creature knowledge.
 */
public final class DragonSaves {
    /** One breath (12 wind-up + 40 flame ticks) is one exposure. */
    public static final int BREATH_WINDOW = 60;
    /** One storm (three bolts or hailstones, all in the same tick) is one exposure; storms are 60+ ticks apart. */
    public static final int STORM_WINDOW = 40;
    /** DEX DC of a Lightning Chaser's or Frost Drake's storm. */
    public static final int STORM_DC = 14;

    public static final String FIRE_BREATH = "save.dndclasses.fire_breath";
    public static final String FROST_BREATH = "save.dndclasses.frost_breath";
    public static final String LIGHTNING_STORM = "save.dndclasses.lightning_storm";
    public static final String HAILSTORM = "save.dndclasses.hailstorm";

    public static final String FIRE_BREATH_EFFECT = "save.dndclasses.effect.fire_breath";
    public static final String FROST_BREATH_EFFECT = "save.dndclasses.effect.frost_breath";
    public static final String LIGHTNING_STORM_EFFECT = "save.dndclasses.effect.lightning_storm";
    public static final String HAILSTORM_EFFECT = "save.dndclasses.effect.hailstorm";

    public static final MobSaveInfo.Entry WYVERN_BREATH = dexSave(ModEntityTypes.WYVERN, FIRE_BREATH,
            WyvernEntity.BREATH_SAVE_DC, FIRE_BREATH_EFFECT);
    public static final MobSaveInfo.Entry EMBER_WYVERN_BREATH = dexSave(ModEntityTypes.EMBER_WYVERN, FIRE_BREATH,
            WyvernEntity.BREATH_SAVE_DC, FIRE_BREATH_EFFECT);
    public static final MobSaveInfo.Entry BONE_WYVERN_BREATH = dexSave(ModEntityTypes.BONE_WYVERN, FIRE_BREATH,
            WyvernEntity.BREATH_SAVE_DC, FIRE_BREATH_EFFECT);
    public static final MobSaveInfo.Entry LIGHTNING_CHASER_BREATH = dexSave(ModEntityTypes.LIGHTNING_CHASER,
            FIRE_BREATH, LairDragonEntity.BREATH_SAVE_DC, FIRE_BREATH_EFFECT);
    public static final MobSaveInfo.Entry LIGHTNING_CHASER_STORM = dexSave(ModEntityTypes.LIGHTNING_CHASER,
            LIGHTNING_STORM, STORM_DC, LIGHTNING_STORM_EFFECT);
    public static final MobSaveInfo.Entry FROST_DRAKE_BREATH = dexSave(ModEntityTypes.FROST_DRAKE, FROST_BREATH,
            LairDragonEntity.BREATH_SAVE_DC, FROST_BREATH_EFFECT);
    public static final MobSaveInfo.Entry FROST_DRAKE_HAIL = dexSave(ModEntityTypes.FROST_DRAKE, HAILSTORM,
            STORM_DC, HAILSTORM_EFFECT);
    public static final MobSaveInfo.Entry PIKEHORN_BREATH = dexSave(ModEntityTypes.RIVER_PIKEHORN, FIRE_BREATH,
            RiverPikehornEntity.BREATH_SAVE_DC, FIRE_BREATH_EFFECT);

    private DragonSaves() {
    }

    private static MobSaveInfo.Entry dexSave(EntityType<?> type, String label, int dc, String effect) {
        return MobSaveInfo.register(type, label, Ability.DEX, dc, true, effect);
    }

    /** Registers the entries above with {@link MobSaveInfo}; call once after the entity types. */
    public static void register() {
        // Loading the class registers the entries
    }

    /**
     * The victim's DEX save against a dragon attack: one roll per exposure ({@code tag} + window), half damage on
     * a success. Players see it on the save lane with {@code effectKey} as the success text; anything else rolls
     * silently.
     */
    public static SaveResult save(LivingEntity victim, Entity dragon, String labelKey, int dc, String effectKey,
            String tag, int window) {
        return SavingThrow.of(victim, Ability.DEX, dc)
                .label(labelKey)
                .source(dragon)
                .exposure(dragon, tag, window)
                .halvesDamage()
                .onSuccess(Text.translatable(effectKey))
                .roll();
    }

    /**
     * True when {@code source} can't hurt the victim at all (fire on a fire-immune mob or under Fire Resistance,
     * freezing on a Frost Drake, invulnerable), so there's nothing to save against and no roll is shown.
     */
    public static boolean isUnaffected(LivingEntity victim, DamageSource source) {
        if (victim.isInvulnerableTo(source)) {
            return true;
        }
        return source.isIn(DamageTypeTags.IS_FIRE)
                && (victim.isFireImmune() || victim.hasStatusEffect(StatusEffects.FIRE_RESISTANCE));
    }
}
