package mattonfire.dnd.entity.ai.goal;

import mattonfire.dnd.entity.LichEntity;
import java.util.EnumSet;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.Vec3d;

/**
 * The {@link LichEntity}'s movement. Keeps a caster's distance: backs off when the target gets within
 * 6 blocks, closes in when it's beyond 14 or out of sight, and otherwise stands its ground to cast.
 */
public class CasterMovementGoal extends Goal {
    private final LichEntity lich;
    private int repath;

    public CasterMovementGoal(LichEntity lich) {
        this.lich = lich;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        LivingEntity target = this.lich.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void start() {
        this.repath = 0;
    }

    @Override
    public void stop() {
        this.lich.getNavigation().stop();
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.lich.getTarget();
        if (target == null) {
            return;
        }
        this.lich.getLookControl().lookAt(target, 30.0F, 30.0F);
        if (this.lich.isCasting() || --this.repath > 0) {
            return;
        }
        this.repath = 10;
        double distance = this.lich.squaredDistanceTo(target);
        boolean canSee = this.lich.getVisibilityCache().canSee(target);
        if (distance < 6.0D * 6.0D) {
            Vec3d away = NoPenaltyTargeting.findFrom(this.lich, 10, 4, target.getPos());
            if (away != null) {
                this.lich.getNavigation().startMovingTo(away.x, away.y, away.z, 1.2D);
            }
        } else if (distance > 14.0D * 14.0D || !canSee) {
            this.lich.getNavigation().startMovingTo(target, 1.0D);
        } else {
            this.lich.getNavigation().stop();
        }
    }
}
