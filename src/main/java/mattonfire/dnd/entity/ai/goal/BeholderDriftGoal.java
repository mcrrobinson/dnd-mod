package mattonfire.dnd.entity.ai.goal;

import java.util.EnumSet;

import mattonfire.dnd.entity.BeholderEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Out of a fight the Beholder drifts slowly round its home, never far from it. */
public class BeholderDriftGoal extends Goal {
    private static final int RANGE = 10;

    private final BeholderEntity beholder;
    private Vec3d destination;
    private int ticks;

    public BeholderDriftGoal(BeholderEntity beholder) {
        this.beholder = beholder;
        this.setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (this.beholder.getTarget() != null || this.beholder.getRandom().nextInt(60) != 0) {
            return false;
        }
        BlockPos home = this.beholder.getHome() != null ? this.beholder.getHome() : this.beholder.getBlockPos();
        for (int attempt = 0; attempt < 8; attempt++) {
            Vec3d spot = Vec3d.ofBottomCenter(home).add(
                    this.beholder.getRandom().nextBetween(-RANGE, RANGE), this.beholder.getRandom().nextBetween(-2, 4),
                    this.beholder.getRandom().nextBetween(-RANGE, RANGE));
            Box box = this.beholder.getType().getDimensions().getBoxAt(spot);
            if (this.beholder.world.isSpaceEmpty(this.beholder, box)) {
                this.destination = spot;
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.beholder.getMoveControl().moveTo(this.destination.x, this.destination.y, this.destination.z, 0.4);
    }

    @Override
    public boolean shouldContinue() {
        return this.beholder.getTarget() == null && this.beholder.getMoveControl().isMoving() && ++this.ticks < 200;
    }

    @Override
    public void tick() {
        this.beholder.getLookControl().lookAt(this.destination.x, this.destination.y + 1.2, this.destination.z);
    }
}
