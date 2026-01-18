package mattonfire.dnd.classes.Goals;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;

public class TimedDespawnGoal extends Goal {
    private final MobEntity mob;
    private int tickCount;
    private final int maxTicks;

    public TimedDespawnGoal(MobEntity mob, int maxTicks) {
        this.mob = mob;
        this.maxTicks = maxTicks;
    }

    @Override
    public boolean canStart() {
        return true; // Always runs
    }

    @Override
    public void tick() {
        tickCount++;
        System.out.println(tickCount);
        if (tickCount >= maxTicks) {
            mob.discard(); // Safely removes the entity
        }
    }
}