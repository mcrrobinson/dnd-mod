package mattonfire.dnd.classes;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectType;

public class FreezeEffect extends StatusEffect {
    public FreezeEffect() {
        super(StatusEffectType.HARMFUL, 0xADD8E6); // Light blue color for freezing effect
    }

    @Override
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
        entity.setVelocity(0, 0, 0); // Stops motion
        entity.velocityDirty = true; // Updates the velocity
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true; // Applies the effect every tick
    }
}
