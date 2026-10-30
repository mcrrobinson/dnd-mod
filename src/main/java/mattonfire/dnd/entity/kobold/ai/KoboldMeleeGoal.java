package mattonfire.dnd.entity.kobold.ai;

import mattonfire.dnd.entity.kobold.KoboldEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.util.Hand;

/** A brave kobold's melee: vanilla's, only while it {@link KoboldEntity#wantsMelee wants to}, with a jab animation. */
public class KoboldMeleeGoal extends MeleeAttackGoal {
    private final KoboldEntity kobold;

    public KoboldMeleeGoal(KoboldEntity kobold, double speed) {
        super(kobold, speed, true);
        this.kobold = kobold;
    }

    @Override
    public boolean canStart() {
        return this.kobold.wantsMelee() && super.canStart();
    }

    @Override
    public boolean shouldContinue() {
        return this.kobold.wantsMelee() && super.shouldContinue();
    }

    @Override
    protected void attack(LivingEntity target, double squaredDistance) {
        if (squaredDistance <= this.getSquaredMaxAttackDistance(target) && this.getCooldown() <= 0) {
            this.resetCooldown();
            this.kobold.swingHand(Hand.MAIN_HAND);
            this.kobold.triggerAnim("action_controller", "attack");
            this.kobold.tryAttack(target);
        }
    }
}
