package mattonfire.dnd.classes.Effects;

import mattonfire.dnd.classes.Conditions.Condition;
import mattonfire.dnd.classes.Conditions.Conditions;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.server.ServerTask;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

/**
 * The old Frightened status effect, kept registered so {@code /effect} and old saves still work. On the server it
 * turns straight into a sourceless Frightened condition ({@link Conditions}) of the same length, and the effect is
 * taken off at the end of the tick: the condition does the work (half-strength blows, fleeing, the movement clamp).
 * The attribute modifier stays so an effect loaded from an old save still lets go of it when it ends.
 */
public class FrightenedEffect extends StatusEffect {
    public FrightenedEffect(StatusEffectCategory category, int color) {
        super(category, color);
        this.addAttributeModifier(EntityAttributes.GENERIC_ATTACK_DAMAGE, "5d7e0c1a-8b2f-4c6d-9e3a-1f0b2c4d6e8a",
                -0.5D, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public void onApplied(LivingEntity entity, AttributeContainer attributes, int amplifier) {
        super.onApplied(entity, attributes, amplifier);
        if (!(entity.getWorld() instanceof ServerWorld world)) {
            return;
        }
        var instance = entity.getStatusEffect(this);
        int ticks = instance == null || instance.isInfinite() ? Conditions.INDEFINITE : instance.getDuration();
        Conditions.of(entity, Condition.FRIGHTENED).sourceName(Text.translatable("effect.dndclasses.frightened"))
                .key("effect").ticks(ticks).apply();
        // Not while the effect map is being changed: the server runs this after the current tick's work
        StatusEffect self = this;
        world.getServer().send(new ServerTask(world.getServer().getTicks(), () -> entity.removeStatusEffect(self)));
    }
}
