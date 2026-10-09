package mattonfire.dnd.entity.ai.goal;

import mattonfire.dnd.entity.WyvernEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import java.util.EnumSet;

public class WyvernAttackGoal extends Goal {
    private final WyvernEntity entity;
    private int cooldown;
    private int seeTime;
    private int repathTicks;

    public WyvernAttackGoal(WyvernEntity entity) {
        this.entity = entity;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        LivingEntity target = this.entity.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void start() {
        this.cooldown = 0;
        this.seeTime = 0;
        this.repathTicks = 0;
    }

    @Override
    public void stop() {
        this.seeTime = 0;
    }

    // Cooldowns and seeTime count real ticks; otherwise the goal selector only ticks this every other tick.
    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.entity.getTarget();
        if (target == null) return;

        double distanceSq = this.entity.squaredDistanceTo(target);
        boolean canSee = this.entity.getVisibilityCache().canSee(target);
        
        if (canSee) {
            this.seeTime++;
        } else {
            this.seeTime = 0;
        }

        if (distanceSq <= this.maxAttackDistance() && this.seeTime >= 5) {
            this.entity.getLookControl().lookAt(target, 30.0F, 30.0F);
        }

        if (this.entity.getFireBreath().isBreathing()) {
            // Hold position and keep facing the target while breathing
            this.entity.getNavigation().stop();
            this.entity.getLookControl().lookAt(target, 30.0F, 30.0F);
            return;
        }

        // Re-path now and then (like MeleeAttackGoal), not every tick
        if (--this.repathTicks <= 0 || this.entity.getNavigation().isIdle()) {
            this.repathTicks = 4 + this.entity.getRandom().nextInt(7);
            this.entity.getNavigation().startMovingTo(target, 1.0);
        }

        if (this.cooldown > 0) {
            this.cooldown--;
        }

        if (this.cooldown <= 0 && this.seeTime >= 10 && distanceSq <= this.maxAttackDistance()) {
             double biteRange = this.entity.getBiteRange();
             if (distanceSq > biteRange * biteRange) { // Fire breath (> 5 blocks)
                 // Starts a bit inside the flame's reach, since the target keeps moving
                 double breathRange = this.entity.getFireBreath().range - 2.0;
                 if (distanceSq <= breathRange * breathRange) {
                     this.entity.getFireBreath().start(target);
                     // The cooldown starts once the breath is over (ticking pauses while breathing)
                     this.cooldown = this.entity.getBreathCooldown();
                 }
             } else { // Melee
                 this.entity.tryAttack(target);
                 this.cooldown = this.entity.getMeleeCooldown();
             }
        }
    }
    
    private double maxAttackDistance() {
        return 512.0; // 22 blocks
    }
}
