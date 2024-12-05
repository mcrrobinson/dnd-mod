package mattonfire.dnd.classes;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.LivingEntity;

public class FreezeEffect extends StatusEffect {
    public FreezeEffect() {
        super(StatusEffectCategory.HARMFUL, 0xADD8E6); // Light blue color for freezing effect
    }

    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
        // TODO: Currently crashes client.
        // entity.setVelocity(0, entity.prevY, 0); // Stops motion
        // entity.velocityModified = true; // Updates the velocity
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        // Apply effect every tick
        return true;
    }
}
