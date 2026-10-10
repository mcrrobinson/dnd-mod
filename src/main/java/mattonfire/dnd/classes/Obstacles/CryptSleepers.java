package mattonfire.dnd.classes.Obstacles;

import java.util.List;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Party.PartyManager;
import mattonfire.dnd.classes.Progression.Progression;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.Nullable;

/**
 * Crypt Sleepers: dormant undead laid in alcoves. A sleeper is any mob with the command tag
 * {@value #TAG} and its AI off ({@code NoAI}). It wakes, for good, when:
 * <ul>
 * <li>a living player who isn't undead-allied comes within {@value #WAKE_RADIUS} blocks;</li>
 * <li>something attacks it (sunlight or drowning don't count);</li>
 * <li>an obstacle alarm ({@link ObstacleEvents#ALARM}) goes off within the alarm's radius.</li>
 * </ul>
 * <b>Deathly Escort:</b> a Necromancer is undead-allied, and so is every member of their party within
 * {@value #ESCORT_RADIUS} blocks of them, so a Necromancer can lead the party past sleeping crypt undead.
 * Creative and spectator players never wake them.
 *
 * <p>Put a mob to sleep with {@link #sleep} (structures), {@code /dndobstacle sleep [radius]}, or
 * summon it with {@code {NoAI:1b,Tags:["dndclasses.sleeper"]}}.
 */
public final class CryptSleepers {
    public static final String TAG = "dndclasses.sleeper";
    public static final int WAKE_RADIUS = 5;
    public static final int ESCORT_RADIUS = 6;
    private static final int CHECK_INTERVAL = 10;

    private CryptSleepers() {
    }

    static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getTime() % CHECK_INTERVAL == 0) {
                tick(world);
            }
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof MobEntity mob && isSleeper(mob) && source.getAttacker() != null
                    && entity.getWorld() instanceof ServerWorld world) {
                wake(world, mob, source.getAttacker());
            }
            return true;
        });
        ObstacleEvents.ALARM.register((world, pos, radius, cause) -> {
            for (MobEntity mob : world.getEntitiesByClass(MobEntity.class, new Box(pos).expand(radius),
                    CryptSleepers::isSleeper)) {
                wake(world, mob, cause);
            }
        });
    }

    public static boolean isSleeper(Entity entity) {
        return entity instanceof MobEntity && entity.getCommandTags().contains(TAG);
    }

    /** Lays {@code mob} down as a sleeper: no AI, never despawns. */
    public static void sleep(MobEntity mob) {
        mob.addCommandTag(TAG);
        mob.setAiDisabled(true);
        mob.setTarget(null);
        mob.setPersistent();
    }

    /** Wakes a sleeper and sets it on {@code target} if that's a player. */
    public static void wake(ServerWorld world, MobEntity mob, @Nullable Entity target) {
        if (!mob.removeScoreboardTag(TAG)) {
            return;
        }
        mob.setAiDisabled(false);
        if (target instanceof PlayerEntity player && !player.isCreative() && !player.isSpectator()) {
            mob.setTarget(player);
        }
        world.spawnParticles(ParticleTypes.SOUL, mob.getX(), mob.getBodyY(0.6), mob.getZ(), 8, 0.3, 0.4, 0.3, 0.02);
        world.playSound(null, mob.getBlockPos(), SoundEvents.ENTITY_ZOMBIE_AMBIENT, SoundCategory.HOSTILE, 1.0f, 0.6f);
        DnDClasses.LOGGER.info("[Obstacle] crypt sleeper {} woke at {}", mob.getType().getUntranslatedName(),
                mob.getBlockPos().toShortString());
    }

    /**
     * Whether sleepers treat the player as one of their own: a Necromancer, or a party member within
     * {@value #ESCORT_RADIUS} blocks of one (Deathly Escort).
     */
    public static boolean undeadAllied(ServerPlayerEntity player) {
        if (Progression.classOf(player) == DndCharacter.NECROMANCER) {
            return true;
        }
        for (ServerPlayerEntity member : PartyManager.nearbyMembers(player, ESCORT_RADIUS)) {
            if (Progression.classOf(member) == DndCharacter.NECROMANCER) {
                return true;
            }
        }
        return false;
    }

    private static void tick(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isCreative() || player.isSpectator() || !player.isAlive()) {
                continue;
            }
            List<MobEntity> near = world.getEntitiesByClass(MobEntity.class,
                    player.getBoundingBox().expand(WAKE_RADIUS),
                    mob -> isSleeper(mob) && mob.squaredDistanceTo(player) <= WAKE_RADIUS * WAKE_RADIUS);
            if (near.isEmpty() || undeadAllied(player)) {
                continue;
            }
            for (MobEntity mob : near) {
                wake(world, mob, player);
            }
        }
    }
}
