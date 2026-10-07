package mattonfire.dnd.classes.Effects;

import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * Struck by a Beholder's fear ray: your blows land at half strength, and the Beholder that
 * frightened you keeps driving you back when you try to close in (see BeholderEntity).
 */
public class FrightenedEffect extends StatusEffect {
    public FrightenedEffect(StatusEffectCategory category, int color) {
        super(category, color);
        this.addAttributeModifier(EntityAttributes.GENERIC_ATTACK_DAMAGE, "5d7e0c1a-8b2f-4c6d-9e3a-1f0b2c4d6e8a",
                -0.5D, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
