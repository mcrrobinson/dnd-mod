package mattonfire.dnd.entity.ai.goal;

import mattonfire.dnd.entity.LightningChaserEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import java.util.EnumSet;

public class LightningChaserAttackGoal extends Goal {
    private final LightningChaserEntity entity;
    private int cooldown;
    private int seeTime;
    private boolean breatheNext = true;

    public LightningChaserAttackGoal(LightningChaserEntity entity) {
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
    }

    @Override
    public void stop() {
        this.seeTime = 0;
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

        this.entity.getNavigation().startMovingTo(target, 1.0);

        if (this.cooldown > 0) {
            this.cooldown--;
        }

        if (this.cooldown <= 0 && this.seeTime >= 10 && distanceSq <= this.maxAttackDistance()) {
             if (distanceSq > 25.0) { // Range attack (> 5 blocks): fire breath and lightning storm in turn
                 // Starts a bit inside the flame's reach, since the target keeps moving
                 double breathRange = this.entity.getFireBreath().range - 2.0;
                 if (this.breatheNext && distanceSq <= breathRange * breathRange) {
                     this.entity.getFireBreath().start(target);
                     this.breatheNext = false;
                 } else {
                     // Out of the flame's reach the storm comes down instead, and the breath waits its turn
                     this.entity.shoot(target);
                     this.breatheNext = true;
                 }
                 // For a breath the cooldown starts once it's over (ticking pauses while breathing)
                 this.cooldown = 60;
             } else { // Melee
                 this.entity.tryAttack(target);
                 this.cooldown = 20;
             }
        }
    }
    
    private double maxAttackDistance() {
        return 512.0; 
    }
}
