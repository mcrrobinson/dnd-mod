package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;

@Mixin(ActiveTargetGoal.class)
public interface ActiveTargetGoalAccessor {
    @Accessor("targetClass")
    Class<?> getTargetClass();

    @Accessor("targetPredicate")
    TargetPredicate getTargetPredicate();
}
