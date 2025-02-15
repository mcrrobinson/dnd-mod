package mattonfire.dnd.classes.Goals;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

public class FollowSummonerGoal extends Goal {
    private final PathAwareEntity mob;
    private final PlayerEntity summoner;
    private final double speed;
    private final float minDistance;
    private final float maxDistance;

    public FollowSummonerGoal(PathAwareEntity mob, PlayerEntity summoner, double speed, float minDistance,
            float maxDistance) {
        this.mob = mob;
        this.summoner = summoner;
        this.speed = speed;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (summoner == null || !summoner.isAlive()) {
            return false;
        }
        return mob.squaredDistanceTo(summoner) > (minDistance * minDistance);
    }

    @Override
    public void start() {
        moveToSummoner();
    }

    @Override
    public boolean shouldContinue() {
        return mob.squaredDistanceTo(summoner) > (minDistance * minDistance);
    }

    @Override
    public void tick() {
        moveToSummoner();
    }

    private void moveToSummoner() {
        if (summoner != null && summoner.isAlive()) {
            if (mob.squaredDistanceTo(summoner) < (maxDistance * maxDistance)) {
                mob.getNavigation().startMovingTo(summoner, speed);
            } else {
                Vec3d teleportPos = summoner.getPos().add(0, 0, 0);
                mob.refreshPositionAndAngles(teleportPos.getX(), teleportPos.getY(), teleportPos.getZ(), mob.getYaw(),
                        mob.getPitch());
            }
        }
    }
}
