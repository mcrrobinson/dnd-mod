package mattonfire.dnd.entity.ai.goal;

import java.util.EnumSet;

import mattonfire.dnd.entity.BeholderEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Keeps the Beholder staring at its target from the air: close enough to bite while its central eye
 * gazes (so the anti-magic cone covers the target), hanging back and above while it fires eye rays.
 */
public class BeholderAttackGoal extends Goal {
    private static final double GAZE_DISTANCE = 2.5;
    private static final double RAY_DISTANCE = 9.0;
    private static final int BITE_COOLDOWN = 20;

    private final BeholderEntity beholder;
    private int repositionTicks;
    private int biteCooldown;

    public BeholderAttackGoal(BeholderEntity beholder) {
        this.beholder = beholder;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        LivingEntity target = this.beholder.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void start() {
        this.repositionTicks = 0;
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.beholder.getTarget();
        if (target == null) {
            return;
        }
        this.beholder.getLookControl().lookAt(target, 20.0F, 20.0F);
        if (this.biteCooldown > 0) {
            this.biteCooldown--;
        }
        if (--this.repositionTicks <= 0) {
            this.repositionTicks = 10;
            Vec3d spot = this.hoverSpot(target);
            this.beholder.getMoveControl().moveTo(spot.x, spot.y, spot.z, 1.0);
        }
        if (this.beholder.isGazing() && this.biteCooldown == 0
                && this.beholder.getBoundingBox().expand(1.2).intersects(target.getBoundingBox())) {
            this.beholder.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            this.beholder.tryAttack(target);
            this.biteCooldown = BITE_COOLDOWN;
        }
    }

    /** Somewhere on the line from the target to the Beholder, at the distance its mode wants, with room for it. */
    private Vec3d hoverSpot(LivingEntity target) {
        boolean gazing = this.beholder.isGazing();
        Vec3d away = this.beholder.getPos().subtract(target.getPos()).multiply(1.0, 0.0, 1.0);
        if (away.lengthSquared() < 1.0E-4) {
            away = new Vec3d(1.0, 0.0, 0.0);
        }
        away = away.normalize().multiply(gazing ? GAZE_DISTANCE : RAY_DISTANCE);
        double[] heights = gazing ? new double[] {0.0, 1.0, -0.5} : new double[] {3.0, 1.5, 0.0};
        for (double height : heights) {
            Vec3d spot = target.getPos().add(away).add(0.0, height, 0.0);
            if (this.hasRoom(spot)) {
                return spot;
            }
        }
        // Boxed in: go straight for the target instead
        return target.getPos().add(0.0, 1.0, 0.0);
    }

    private boolean hasRoom(Vec3d spot) {
        Box box = this.beholder.getType().getDimensions().getBoxAt(spot);
        return this.beholder.world.isSpaceEmpty(this.beholder, box);
    }
}
