package mattonfire.dnd.entity.ai.goal;

import java.util.EnumSet;

import mattonfire.dnd.entity.FireBreath;
import mattonfire.dnd.entity.FireBreather;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;

/**
 * Breathes fire at the target when it's in reach but not right next to the dragon, for dragons whose
 * other attacks come from separate goals (e.g. MeleeAttackGoal at a lower priority).
 */
public class FireBreathGoal<T extends MobEntity & FireBreather> extends Goal {
    private static final int COOLDOWN = 60;
    private final T dragon;
    private final double minRange;
    /** World time when the next breath may start; kept as a time since canStart only runs every other tick. */
    private long nextBreathTime;

    public FireBreathGoal(T dragon, double minRange) {
        this.dragon = dragon;
        this.minRange = minRange;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    private FireBreath breath() {
        return this.dragon.getFireBreath();
    }

    @Override
    public boolean canStart() {
        if (this.dragon.getWorld().getTime() < this.nextBreathTime) {
            return false;
        }
        LivingEntity target = this.dragon.getTarget();
        if (target == null || !target.isAlive() || !this.dragon.getVisibilityCache().canSee(target)) {
            return false;
        }
        // Starts a bit inside the flame's reach, since the target keeps moving
        double maxRange = this.breath().range - 2.0;
        double distanceSq = this.dragon.squaredDistanceTo(target);
        return distanceSq > this.minRange * this.minRange && distanceSq <= maxRange * maxRange;
    }

    @Override
    public void start() {
        this.dragon.getNavigation().stop();
        this.breath().start(this.dragon.getTarget());
    }

    @Override
    public boolean shouldContinue() {
        return this.breath().isBreathing();
    }

    @Override
    public void stop() {
        this.nextBreathTime = this.dragon.getWorld().getTime() + COOLDOWN;
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.dragon.getTarget();
        if (target != null) {
            this.dragon.getLookControl().lookAt(target, 30.0F, 30.0F);
        }
    }
}
