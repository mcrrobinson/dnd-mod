package mattonfire.dnd.entity.drow;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import org.jetbrains.annotations.Nullable;

/**
 * Faerie Fire: violet light outlines the target (vanilla Glowing, so it shows through walls) and every hit on it
 * deals +{@value #BONUS_DAMAGE} damage ({@code FaerieFireDamageMixin}). With design 8's attack advantage for mobs
 * this could become "attacks against it have advantage".
 */
public final class FaerieFire {
    public static final float BONUS_DAMAGE = 2.0F;

    private FaerieFire() {
    }

    /** The status effect that marks a creature outlined by Faerie Fire. */
    public static final class Effect extends StatusEffect {
        public Effect() {
            super(StatusEffectCategory.HARMFUL, 0xB57BFF);
        }
    }

    public static void apply(LivingEntity target, @Nullable LivingEntity caster, int ticks) {
        target.addStatusEffect(new StatusEffectInstance(Drow.FAERIE_FIRE, ticks, 0, false, true, true), caster);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, ticks, 0, false, false, false), caster);
    }

    /** The damage {@code target} takes from a hit of {@code amount}: +2 while outlined, for hits that do damage. */
    public static float modify(LivingEntity target, DamageSource source, float amount) {
        if (amount > 0.0F && Drow.FAERIE_FIRE != null && target.hasStatusEffect(Drow.FAERIE_FIRE)
                && source.getAttacker() != null) {
            return amount + BONUS_DAMAGE;
        }
        return amount;
    }
}
