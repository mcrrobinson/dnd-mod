package mattonfire.dnd.classes.Effects;

import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * Artificer special: temporarily reinforces the player's armor. The attribute
 * modifiers are added and removed by vanilla together with the effect, so they
 * also go away on death, milk, or when the effect runs out while logged out.
 */
public class ArmorBuffEffect extends StatusEffect {
    public static final double ARMOR_BONUS = 8.0; // per level
    public static final double TOUGHNESS_BONUS = 4.0; // per level

    public ArmorBuffEffect(StatusEffectCategory category, int color) {
        super(category, color);
        addAttributeModifier(EntityAttributes.GENERIC_ARMOR, "5f3c1d2a-7b4e-4c8a-9e61-0a2f6b9d3e71",
                ARMOR_BONUS, EntityAttributeModifier.Operation.ADDITION);
        addAttributeModifier(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, "8a1e4b7c-2d5f-4e93-b6a0-3c9d7f1e2b54",
                TOUGHNESS_BONUS, EntityAttributeModifier.Operation.ADDITION);
    }
}
