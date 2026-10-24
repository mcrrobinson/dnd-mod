package mattonfire.dnd.classes.Conditions.ai;

import java.util.EnumSet;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.Conditions.Condition;
import mattonfire.dnd.classes.Conditions.Conditions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.math.Vec3d;

/**
 * A Frightened path mob runs from what frightens it: the condition's source, or the nearest player when it's
 * sourceless. Added to every {@code PathAwareEntity} when it loads (replacing the Warhorn-only flee goal).
 */
public class ConditionFleeGoal extends Goal {
    private final PathAwareEntity mob;
    private final float distance;
    private final double slowSpeed;
    private final double fastSpeed;
    @Nullable
    private Entity threat;
    @Nullable
    private Path path;

    public ConditionFleeGoal(PathAwareEntity mob, float distance, double slowSpeed, double fastSpeed) {
        this.mob = mob;
        this.distance = distance;
        this.slowSpeed = slowSpeed;
        this.fastSpeed = fastSpeed;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Nullable
    private Entity findThreat() {
        if (!Conditions.has(this.mob, Condition.FRIGHTENED)) {
            return null;
        }
        Entity source = Conditions.sourceOf(this.mob, Condition.FRIGHTENED);
        if (source != null && source.isAlive() && source.squaredDistanceTo(this.mob) <= this.distance * this.distance) {
            return source;
        }
        if (source == null) {
            return this.mob.getWorld().getClosestPlayer(this.mob.getX(), this.mob.getY(), this.mob.getZ(), this.distance,
                    e -> EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(e) && e instanceof PlayerEntity);
        }
        return null;
    }

    @Override
    public boolean canStart() {
        this.threat = findThreat();
        if (this.threat == null) {
            return false;
        }
        Vec3d away = NoPenaltyTargeting.findFrom(this.mob, 16, 7, this.threat.getPos());
        if (away == null || this.threat.squaredDistanceTo(away.x, away.y, away.z) < this.threat.squaredDistanceTo(this.mob)) {
            return false;
        }
        EntityNavigation nav = this.mob.getNavigation();
        this.path = nav.findPathTo(away.x, away.y, away.z, 0);
        return this.path != null;
    }

    @Override
    public boolean shouldContinue() {
        return Conditions.has(this.mob, Condition.FRIGHTENED) && !this.mob.getNavigation().isIdle();
    }

    @Override
    public void start() {
        this.mob.getNavigation().startMovingAlong(this.path, this.slowSpeed);
        this.mob.setTarget(null);
    }

    @Override
    public void stop() {
        this.threat = null;
    }

    @Override
    public void tick() {
        if (this.threat != null && this.mob.squaredDistanceTo(this.threat) < 49.0D) {
            this.mob.getNavigation().setSpeed(this.fastSpeed);
        } else {
            this.mob.getNavigation().setSpeed(this.slowSpeed);
        }
    }
}
