package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.entity.LivingEntity;

/** Great Weapon Master's Cleave sets the attack cooldown so the next swing is full strength at once. */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("lastAttackedTicks")
    void dnd$setLastAttackedTicks(int ticks);
}
