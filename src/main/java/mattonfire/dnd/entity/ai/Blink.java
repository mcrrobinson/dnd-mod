package mattonfire.dnd.entity.ai;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Short-range teleports for casters (the Lich, the Mind Flayer): a random spot {@code min}-{@code max} blocks
 * round a centre, on solid ground within a few blocks of the centre's height ({@code LivingEntity.teleport}), with a
 * puff of particles
 * and a mirror-move sound at both ends.
 */
public final class Blink {
    private static final int ATTEMPTS = 16;
    /** A spot whose floor is further below the try than this doesn't count. */
    private static final double MAX_DROP = 3.0D;

    private Blink() {
    }

    /**
     * Teleports {@code mob} to a random spot {@code min}-{@code max} blocks round {@code center} (within two
     * blocks up or down). With {@code mustSee}, only a spot with a line of sight to it counts. Returns whether it
     * moved. Server side only.
     */
    public static boolean blink(MobEntity mob, BlockPos center, double min, double max, @Nullable LivingEntity mustSee,
            ParticleEffect particle) {
        ServerWorld world = (ServerWorld) mob.getWorld();
        Vec3d from = mob.getPos();
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            float angle = mob.getRandom().nextFloat() * MathHelper.TAU;
            double distance = min + mob.getRandom().nextDouble() * (max - min);
            double x = center.getX() + 0.5D + MathHelper.cos(angle) * distance;
            double z = center.getZ() + 0.5D + MathHelper.sin(angle) * distance;
            double y = center.getY() + mob.getRandom().nextInt(5) - 2;
            if (!mob.teleport(x, y, z, false)) {
                continue;
            }
            // teleport() drops to the first floor below the spot: off a ledge that can be far down
            if (Math.abs(mob.getY() - y) > MAX_DROP || mustSee != null && !mob.canSee(mustSee)) {
                mob.requestTeleport(from.x, from.y, from.z);
                continue;
            }
            world.spawnParticles(particle, from.x, from.y + 1.2D, from.z, 25, 0.3D, 0.8D, 0.3D, 0.02D);
            world.spawnParticles(particle, mob.getX(), mob.getY() + 1.2D, mob.getZ(), 25, 0.3D, 0.8D, 0.3D, 0.02D);
            world.playSound(null, from.x, from.y, from.z, SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE,
                    SoundCategory.HOSTILE, 1.5F, 0.8F);
            mob.playSound(SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.5F, 0.8F);
            mob.getNavigation().stop();
            return true;
        }
        return false;
    }
}
