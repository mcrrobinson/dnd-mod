package mattonfire.dnd.entity.ai.goal;

import mattonfire.dnd.entity.TrollEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;

/**
 * The Troll's melee: chases its target like a normal melee mob, and once it's within
 * {@link TrollEntity#COMBO_REACH} blocks starts the two-claw Rend combo ({@link TrollEntity#startCombo}).
 * The troll itself times the second claw and the cooldown; a felled troll doesn't fight.
 */
public class TrollComboGoal extends MeleeAttackGoal {
    private final TrollEntity troll;

    public TrollComboGoal(TrollEntity troll) {
        super(troll, 1.15D, true);
        this.troll = troll;
    }

    @Override
    public boolean canStart() {
        return !this.troll.isFelled() && super.canStart();
    }

    @Override
    public boolean shouldContinue() {
        return !this.troll.isFelled() && super.shouldContinue();
    }

    @Override
    protected void attack(LivingEntity target, double squaredDistance) {
        if (this.troll.canStartCombo(target)) {
            this.troll.startCombo(target);
        }
    }
}
