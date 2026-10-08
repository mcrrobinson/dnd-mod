package mattonfire.dnd.classes.Effects;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public class MobRepelEffect extends StatusEffect {
    public MobRepelEffect(StatusEffectCategory category, int color) {
        super(category, color); // Gold color
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true; // This effect is always active
    }
}
