package mattonfire.dnd.entity.ai.goal;

import mattonfire.dnd.entity.WyvernEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.EnumSet;

public class WyvernFlyRandomlyGoal extends Goal {
    private final WyvernEntity wyvern;

    public WyvernFlyRandomlyGoal(WyvernEntity wyvern) {
        this.wyvern = wyvern;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (this.wyvern.isOnGround()) {
            // Much lower chance to take off if on ground (1/200 ticks = ~10 seconds avg)
            // Combined with WanderAroundFarGoal (1/120), this allows walking to occur frequently
            return this.wyvern.getNavigation().isIdle() && this.wyvern.getRandom().nextInt(200) == 0;
        }
        // Normal fly chance if already in air
        return this.wyvern.getNavigation().isIdle() && this.wyvern.getRandom().nextInt(50) == 0;
    }

    @Override
    public boolean shouldContinue() {
        return this.wyvern.getNavigation().isFollowingPath();
    }

    @Override
    public void start() {
        Vec3d vec3d = this.getRandomLocation();
        if (vec3d != null) {
            this.wyvern.getNavigation().startMovingTo(vec3d.x, vec3d.y, vec3d.z, 1.0);
        }
    }

    private Vec3d getRandomLocation() {
        Random random = this.wyvern.getRandom();
        Vec3d pos = this.wyvern.getPos();
        
        // If in air, sometimes try to pick a landing spot (lower altitude)
        boolean tryLand = !this.wyvern.isOnGround() && random.nextInt(5) == 0;

        for (int i = 0; i < 10; ++i) {
            double x = pos.x + (random.nextDouble() * 32.0 - 16.0);
            double y = pos.y + (random.nextDouble() * 10.0 - 5.0);
            double z = pos.z + (random.nextDouble() * 32.0 - 16.0);
            
            // Bias towards slightly higher flight if near ground (take off)
            if (this.wyvern.isOnGround()) {
                y = pos.y + random.nextDouble() * 6.0 + 4.0; 
            } else if (tryLand) {
                // Look for ground below
                y = pos.y - random.nextDouble() * 8.0 - 2.0; 
            }

            BlockPos targetPos = new BlockPos((int)x, (int)y, (int)z);
            if (this.wyvern.world.isAir(targetPos)) {
                 return new Vec3d(x, y, z);
            }
        }
        return null; // Could not find a valid spot
    }
}
