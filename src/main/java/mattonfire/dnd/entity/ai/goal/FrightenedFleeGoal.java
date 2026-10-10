package mattonfire.dnd.entity.ai.goal;

import mattonfire.dnd.classes.Registry.ModEffects;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;

/** While Frightened (the Warlord's Warhorn), the mob runs from nearby players instead of fighting. */
public class FrightenedFleeGoal extends FleeEntityGoal<PlayerEntity> {
    public FrightenedFleeGoal(PathAwareEntity mob, float distance, double slowSpeed, double fastSpeed) {
        super(mob, PlayerEntity.class, distance, slowSpeed, fastSpeed);
    }

    private boolean frightened() {
        return ModEffects.FRIGHTENED != null && this.mob.hasStatusEffect(ModEffects.FRIGHTENED);
    }

    @Override
    public boolean canStart() {
        return frightened() && super.canStart();
    }

    @Override
    public boolean shouldContinue() {
        return frightened() && super.shouldContinue();
    }
}
