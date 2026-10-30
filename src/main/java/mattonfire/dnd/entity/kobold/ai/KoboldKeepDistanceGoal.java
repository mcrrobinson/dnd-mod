package mattonfire.dnd.entity.kobold.ai;

import java.util.EnumSet;
import mattonfire.dnd.entity.kobold.KoboldEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.Vec3d;

/**
 * A craven kobold with a target won't close in: it keeps {@value #MIN}-{@value #MAX} blocks away, backing off when
 * the target comes nearer and edging closer when it drifts further, watching it all the while (a slinger shoots
 * from there, {@link KoboldSlingGoal}).
 */
public class KoboldKeepDistanceGoal extends Goal {
    public static final double MIN = 10.0D;
    public static final double MAX = 14.0D;
    private final KoboldEntity kobold;
    private int repath;

    public KoboldKeepDistanceGoal(KoboldEntity kobold) {
        this.kobold = kobold;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        LivingEntity target = this.kobold.getTarget();
        return target != null && target.isAlive() && !this.kobold.wantsMelee();
    }

    @Override
    public void start() {
        this.repath = 0;
    }

    @Override
    public void stop() {
        this.kobold.getNavigation().stop();
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.kobold.getTarget();
        if (target == null) {
            return;
        }
        this.kobold.getLookControl().lookAt(target, 30.0F, 30.0F);
        if (--this.repath > 0) {
            return;
        }
        this.repath = 10;
        double distance = this.kobold.distanceTo(target);
        if (distance < MIN) {
            Vec3d away = NoPenaltyTargeting.findFrom(this.kobold, 16, 7, target.getPos());
            if (away != null) {
                this.kobold.getNavigation().startMovingTo(away.x, away.y, away.z, 1.2D);
            }
        } else if (distance > MAX) {
            this.kobold.getNavigation().startMovingTo(target, 1.0D);
        } else {
            this.kobold.getNavigation().stop();
        }
    }
}
