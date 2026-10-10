package mattonfire.dnd.entity.ai.goal;

import mattonfire.dnd.entity.LairDragonEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.EnumSet;

public class LairDragonFlyRandomlyGoal extends Goal {
    /** How far it wanders from its lair before turning back. */
    private static final double LAIR_RANGE = 24.0;

    private final LairDragonEntity entity;

    public LairDragonFlyRandomlyGoal(LairDragonEntity entity) {
        this.entity = entity;
        this.setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (this.entity.isOnGround()) {
            // Increased chance to take off if on ground (1/100 ticks = ~5 seconds avg), regardless of current navigation state
            return this.entity.getRandom().nextInt(100) == 0;
        }
        // Normal fly chance if already in air
        return this.entity.getNavigation().isIdle() && this.entity.getRandom().nextInt(50) == 0;
    }

    @Override
    public boolean shouldContinue() {
        return this.entity.getNavigation().isFollowingPath();
    }

    @Override
    public void start() {
        Vec3d vec3d = this.getRandomLocation();
        if (vec3d != null) {
            this.entity.getNavigation().startMovingTo(vec3d.x, vec3d.y, vec3d.z, 1.0);
        }
    }

    private Vec3d getRandomLocation() {
        Random random = this.entity.getRandom();
        Vec3d pos = this.entity.getPos();

        // Strayed too far from its lair: head back and circle above it.
        BlockPos lair = this.entity.getLair();
        if (lair != null && Vec3d.ofBottomCenter(lair).subtract(pos).horizontalLengthSquared() > LAIR_RANGE * LAIR_RANGE) {
            pos = Vec3d.ofBottomCenter(lair).add(0.0, 8.0, 0.0);
            return new Vec3d(pos.x + random.nextDouble() * 16.0 - 8.0, pos.y + random.nextDouble() * 6.0,
                    pos.z + random.nextDouble() * 16.0 - 8.0);
        }

        // If in air, sometimes try to pick a landing spot (lower altitude)
        boolean tryLand = !this.entity.isOnGround() && random.nextInt(5) == 0;

        for (int i = 0; i < 10; ++i) {
            double x = pos.x + (random.nextDouble() * 32.0 - 16.0);
            double y = pos.y + (random.nextDouble() * 10.0 - 5.0);
            double z = pos.z + (random.nextDouble() * 32.0 - 16.0);
            
            // Bias towards slightly higher flight if near ground (take off)
            if (this.entity.isOnGround()) {
                y = pos.y + random.nextDouble() * 6.0 + 4.0; 
            } else if (tryLand) {
                // Look for ground below
                y = pos.y - random.nextDouble() * 8.0 - 2.0; 
            }

            BlockPos targetPos = BlockPos.ofFloored(x, y, z);
            if (this.entity.world.isAir(targetPos)) {
                 return new Vec3d(x, y, z);
            }
        }
        return null; // Could not find a valid spot
    }
}
