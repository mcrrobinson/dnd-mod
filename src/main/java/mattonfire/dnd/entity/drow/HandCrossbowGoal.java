package mattonfire.dnd.entity.drow;

import java.util.EnumSet;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;

/**
 * A drow's hand crossbow: while the target is more than {@link DrowEntity#MELEE_RANGE} blocks off, hold at
 * {@link DrowEntity#BOLT_MIN_RANGE}-{@link DrowEntity#BOLT_MAX_RANGE} blocks and fire a bolt every
 * {@link DrowEntity#BOLT_INTERVAL} ticks; walk in when it's further or out of sight, and keep closing in when it's
 * nearer (the melee goal takes over within the melee range).
 */
final class HandCrossbowGoal extends Goal {
    private final DrowEntity drow;
    private int cooldown;
    private int seenTicks;

    HandCrossbowGoal(DrowEntity drow) {
        this.drow = drow;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        LivingEntity target = this.drow.getTarget();
        return target != null && target.isAlive()
                && this.drow.squaredDistanceTo(target) > DrowEntity.MELEE_RANGE * DrowEntity.MELEE_RANGE;
    }

    @Override
    public boolean shouldContinue() {
        return this.canStart();
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void start() {
        // The first bolt comes quickly once it sees you.
        this.cooldown = Math.max(this.cooldown, 20);
    }

    @Override
    public void stop() {
        this.seenTicks = 0;
    }

    @Override
    public void tick() {
        LivingEntity target = this.drow.getTarget();
        if (target == null) {
            return;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
        }
        boolean sees = this.drow.getVisibilityCache().canSee(target);
        this.seenTicks = sees ? this.seenTicks + 1 : 0;
        double distance = Math.sqrt(this.drow.squaredDistanceTo(target));
        if (!sees || distance > DrowEntity.BOLT_MAX_RANGE - 2.0D || distance < DrowEntity.BOLT_MIN_RANGE) {
            this.drow.getNavigation().startMovingTo(target, 1.0D);
        } else {
            this.drow.getNavigation().stop();
        }
        this.drow.getLookControl().lookAt(target, 30.0F, 30.0F);
        if (sees && this.seenTicks >= 5 && this.cooldown <= 0 && distance <= DrowEntity.BOLT_MAX_RANGE) {
            this.drow.shootBolt(target);
            this.cooldown = DrowEntity.BOLT_INTERVAL;
        }
    }
}
