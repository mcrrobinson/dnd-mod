package mattonfire.dnd.classes.Obstacles.types;

import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * Undead that claw their way up beside a death obstacle: the Desecrated Altar's fumble and fallback,
 * and the Ossuary Gate's. They spawn on free floor round the obstacle, equipped as natural spawns, and
 * go for the player who set them off (a Necromancer is still ignored by undead).
 */
public final class RisenUndead {
    /** How far from the obstacle to look for room to stand. */
    private static final int SEARCH_RADIUS = 3;

    private RisenUndead() {
    }

    /** Raises {@code count} of {@code type} round {@code pos}. Returns how many stood up. */
    public static int raise(ServerWorld world, BlockPos pos, EntityType<? extends MobEntity> type, int count,
            @Nullable ServerPlayerEntity target) {
        int raised = 0;
        int attempts = 0;
        while (raised < count && attempts++ < count * 12) {
            BlockPos spot = freeSpot(world, pos);
            if (spot == null) {
                break;
            }
            MobEntity mob = type.create(world);
            if (mob == null) {
                break;
            }
            mob.refreshPositionAndAngles(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5,
                    world.random.nextFloat() * 360.0f, 0.0f);
            mob.initialize(world, world.getLocalDifficulty(spot), SpawnReason.EVENT, null, null);
            if (target != null && !target.isCreative() && !target.isSpectator()) {
                mob.setTarget(target);
            }
            world.spawnEntityAndPassengers(mob);
            world.spawnParticles(ParticleTypes.SOUL, spot.getX() + 0.5, spot.getY() + 0.2, spot.getZ() + 0.5,
                    10, 0.3, 0.1, 0.3, 0.02);
            world.spawnParticles(ParticleTypes.LARGE_SMOKE, spot.getX() + 0.5, spot.getY() + 0.5, spot.getZ() + 0.5,
                    6, 0.3, 0.4, 0.3, 0.01);
            raised++;
        }
        if (raised > 0) {
            world.playSound(null, pos, SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.HOSTILE, 1.0f, 0.5f);
        }
        DnDClasses.LOGGER.info("[Obstacle] {} {} rose at {}", raised, EntityType.getId(type), pos.toShortString());
        return raised;
    }

    /** A random spot near {@code pos} with two blocks of room over solid floor, or null. */
    @Nullable
    private static BlockPos freeSpot(ServerWorld world, BlockPos pos) {
        for (int i = 0; i < 16; i++) {
            BlockPos base = pos.add(world.random.nextBetween(-SEARCH_RADIUS, SEARCH_RADIUS), 0,
                    world.random.nextBetween(-SEARCH_RADIUS, SEARCH_RADIUS));
            for (int dy = 1; dy >= -2; dy--) {
                BlockPos feet = base.up(dy);
                if (world.getBlockState(feet).getCollisionShape(world, feet).isEmpty()
                        && world.getBlockState(feet.up()).getCollisionShape(world, feet.up()).isEmpty()
                        && world.getBlockState(feet.down()).isSideSolidFullSquare(world, feet.down(), Direction.UP)) {
                    return feet;
                }
            }
        }
        return null;
    }
}
