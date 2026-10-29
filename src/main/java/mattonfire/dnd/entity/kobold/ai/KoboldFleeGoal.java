package mattonfire.dnd.entity.kobold.ai;

import java.util.EnumSet;
import mattonfire.dnd.entity.kobold.KoboldEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * A kobold runs (at x{@value #SPEED}, cowering): from a player who hit it while it was craven, for
 * {@link KoboldEntity#SCARED_TICKS}; or, below 30% health, towards the nearest other kobold that's further from the
 * threat than it is (its pack), else just away, until it's {@value #SAFE_DISTANCE} blocks clear.
 */
public class KoboldFleeGoal extends Goal {
    public static final double SPEED = 1.3D;
    public static final double SAFE_DISTANCE = 12.0D;
    private final KoboldEntity kobold;
    @Nullable
    private LivingEntity threat;
    private int repath;

    public KoboldFleeGoal(KoboldEntity kobold) {
        this.kobold = kobold;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Nullable
    private LivingEntity findThreat() {
        PlayerEntity scared = this.kobold.scaredOf();
        if (scared != null && scared.isAlive()) {
            return scared;
        }
        LivingEntity target = this.kobold.getTarget();
        if (this.kobold.isLowHealth() && target != null && target.isAlive()
                && this.kobold.squaredDistanceTo(target) < SAFE_DISTANCE * SAFE_DISTANCE) {
            return target;
        }
        return null;
    }

    @Override
    public boolean canStart() {
        this.threat = this.findThreat();
        return this.threat != null;
    }

    @Override
    public boolean shouldContinue() {
        LivingEntity now = this.findThreat();
        if (now == null) {
            return false;
        }
        this.threat = now;
        return true;
    }

    @Override
    public void start() {
        this.repath = 0;
        this.kobold.setCowering(true);
    }

    @Override
    public void stop() {
        this.threat = null;
        this.kobold.setCowering(false);
        this.kobold.getNavigation().stop();
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.threat == null || --this.repath > 0) {
            return;
        }
        this.repath = 10;
        Vec3d goal = null;
        if (this.kobold.isLowHealth()) {
            // Towards the pack: the nearest kobold further from the threat than this one.
            double own = this.kobold.squaredDistanceTo(this.threat);
            KoboldEntity pack = this.kobold.world.getClosestEntity(KoboldEntity.class,
                    net.minecraft.entity.ai.TargetPredicate.createNonAttackable().ignoreVisibility()
                            .setPredicate(other -> other != this.kobold && other.squaredDistanceTo(this.threat) > own + 16.0D),
                    this.kobold, this.kobold.getX(), this.kobold.getY(), this.kobold.getZ(),
                    this.kobold.getBoundingBox().expand(24.0D));
            if (pack != null) {
                goal = pack.getPos();
            }
        }
        for (int tries = 0; goal == null && tries < 4; tries++) {
            goal = NoPenaltyTargeting.findFrom(this.kobold, 16, 7, this.threat.getPos());
        }
        if (goal == null) {
            // Nowhere good found (a narrow ledge, mid-knockback): just run straight away from the threat.
            Vec3d away = this.kobold.getPos().subtract(this.threat.getPos()).multiply(1.0D, 0.0D, 1.0D);
            if (away.lengthSquared() < 1.0E-4D) {
                away = Vec3d.fromPolar(0.0F, this.kobold.getYaw());
            }
            goal = this.kobold.getPos().add(away.normalize().multiply(8.0D));
        }
        if (!this.kobold.getNavigation().startMovingTo(goal.x, goal.y, goal.z, SPEED)) {
            this.repath = 2; // try again shortly rather than standing still
        }
    }
}
