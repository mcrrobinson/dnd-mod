package mattonfire.dnd.classes.Effects;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * Rogue's Danger Sense: projectiles fly through whoever has it. The effect does
 * nothing by itself; {@code ProjectileEntityMixin} checks for it.
 */
public class DangerSenseEffect extends StatusEffect {
    public DangerSenseEffect(StatusEffectCategory category, int color) {
        super(category, color);
    }
}
