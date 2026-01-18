package mattonfire.dnd.entity.ai.goal;

import mattonfire.dnd.entity.LightningChaserEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.EnumSet;

public class LightningChaserFlyRandomlyGoal extends Goal {
    private final LightningChaserEntity entity;

    public LightningChaserFlyRandomlyGoal(LightningChaserEntity entity) {
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
        // Switch to flight controls and navigation immediately for takeoff
        if (this.entity.isOnGround()) {
            this.entity.switchToFlightMode();
        }
        Vec3d vec3d = this.getRandomLocation();
        if (vec3d != null) {
            this.entity.getNavigation().startMovingTo(vec3d.x, vec3d.y, vec3d.z, 1.0);
        }
    }

    private Vec3d getRandomLocation() {
        Random random = this.entity.getRandom();
        Vec3d pos = this.entity.getPos();
        
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

            BlockPos targetPos = new BlockPos((int)x, (int)y, (int)z);
            if (this.entity.world.isAir(targetPos)) {
                 return new Vec3d(x, y, z);
            }
        }
        return null; // Could not find a valid spot
    }
}
