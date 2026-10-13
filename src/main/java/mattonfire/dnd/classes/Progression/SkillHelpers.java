package mattonfire.dnd.classes.Progression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * Shared bits for skills: finding targets, effects, temporary summons, timed area effects
 * ({@link #startArea}), thrown things to watch until they land ({@link #onLanding}) and per-player
 * timers ({@link Cooldowns}).
 */
public final class SkillHelpers {
    private static final String SUMMON_TAG = "dndclasses.summon";

    /** Summoned entities and the world time they vanish at. */
    private static final Map<UUID, Long> SUMMONS = new HashMap<>();

    /** Timed area effects still running, in every world. Server thread only. */
    private static final List<TimedArea> AREAS = new ArrayList<>();
    /** Thrown things being watched until they land. Server thread only. */
    private static final List<Tracked> TRACKED = new ArrayList<>();

    private SkillHelpers() {
    }

    static void register() {
        // A summon that leaves a world without being killed (chunk unload, portal) comes back
        // without its AI goals, so forget it and the load check below removes it.
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity.getCommandTags().contains(SUMMON_TAG) && entity.getRemovalReason() != null
                    && !entity.getRemovalReason().shouldDestroy()) {
                SUMMONS.remove(entity.getUuid());
            }
        });
        // Summons left over from before a restart or a chunk reload.
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity.getCommandTags().contains(SUMMON_TAG) && !SUMMONS.containsKey(entity.getUuid())) {
                entity.discard();
            }
        });
        ServerTickEvents.END_WORLD_TICK.register(SkillHelpers::tickAreas);
        // Runs before the class skills' own server ticks (registered after this), so a Transmute cloud
        // spawned by a landing flask is pulsed the same tick.
        ServerTickEvents.END_SERVER_TICK.register(server -> tickTracked());
        // Areas hold their worlds, and cooldowns count server ticks, which restart at 0 with the next
        // singleplayer world: carry neither into the next session.
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            AREAS.clear();
            TRACKED.clear();
            Cooldowns.ALL.forEach(Cooldowns::clear);
        });
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (SUMMONS.isEmpty())
                return;
            long now = world.getTime();
            SUMMONS.entrySet().removeIf(entry -> {
                Entity entity = world.getEntity(entry.getKey());
                if (entity == null)
                    return false; // In another world or unloaded; checked there
                if (now >= entry.getValue() || !entity.isAlive()) {
                    world.spawnParticles(ParticleTypes.POOF, entity.getX(), entity.getY() + 0.5, entity.getZ(), 8,
                            0.3, 0.3, 0.3, 0.02);
                    entity.discard();
                    return true;
                }
                return false;
            });
        });
    }

    /**
     * Spawns an entity that vanishes after {@code lifetimeTicks}, and is
     * removed if it outlives a server restart. Set it up (owner, position)
     * before calling.
     */
    public static void spawnSummon(ServerWorld world, Entity entity, int lifetimeTicks) {
        entity.addCommandTag(SUMMON_TAG);
        SUMMONS.put(entity.getUuid(), world.getTime() + lifetimeTicks);
        world.spawnEntity(entity);
    }

    /** The first living non-player, non-pet in the crosshair within range, not behind blocks. */
    public static LivingEntity lookTarget(PlayerEntity player, double range) {
        Vec3d eye = player.getEyePos();
        Vec3d end = eye.add(player.getRotationVec(1.0F).multiply(range));
        HitResult block = player.getWorld().raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, player));
        if (block.getType() != HitResult.Type.MISS)
            end = block.getPos();
        Box area = player.getBoundingBox().stretch(end.subtract(eye)).expand(1.0);
        UUID owner = player.getUuid();
        EntityHitResult hit = ProjectileUtil.raycast(player, eye, end, area,
                e -> e instanceof LivingEntity && e.isAlive() && !(e instanceof PlayerEntity)
                        && !(e instanceof Tameable t && owner.equals(t.getOwnerUuid())),
                eye.squaredDistanceTo(end));
        return hit != null ? (LivingEntity) hit.getEntity() : null;
    }

    /** Living hostile mobs within the radius. */
    public static List<LivingEntity> hostilesNear(PlayerEntity player, double radius) {
        return player.getWorld().getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(radius),
                e -> e instanceof Monster && e.isAlive());
    }

    /** Anything alive within the radius that isn't a player or one of the player's pets. */
    public static List<LivingEntity> enemiesNear(PlayerEntity player, double radius) {
        UUID owner = player.getUuid();
        return player.getWorld().getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(radius),
                e -> e.isAlive() && !(e instanceof PlayerEntity)
                        && !(e instanceof Tameable t && owner.equals(t.getOwnerUuid())));
    }

    /** Players (including this one) and the player's pets within the radius. */
    public static List<LivingEntity> alliesNear(PlayerEntity player, double radius) {
        UUID owner = player.getUuid();
        return player.getWorld().getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(radius),
                e -> e.isAlive() && (e instanceof PlayerEntity
                        || e instanceof Tameable t && owner.equals(t.getOwnerUuid())));
    }

    /** Plays a sound at the player and puffs particles around them. */
    public static void effects(ServerPlayerEntity player, SoundEvent sound, ParticleEffect particle, int count) {
        player.getWorld().playSound(null, player.getBlockPos(), sound, SoundCategory.PLAYERS, 1.0F, 1.0F);
        ((ServerWorld) player.getWorld()).spawnParticles(particle, player.getX(), player.getY() + 1, player.getZ(),
                count, 1.0, 0.6, 1.0, 0.05);
    }

    // ---- Timed area effects ----

    /** What a timed area does each tick it's up (world time {@code now}). */
    @FunctionalInterface
    public interface AreaTick {
        void tick(ServerWorld world, TimedArea area, long now);
    }

    /**
     * An area effect at a fixed spot for a while (a Rain of Arrows, a Hellgate, a Wither Cloud, a Meteor
     * Swarm). It ticks at the end of its world's tick, from the tick after it starts until world time
     * {@code end} (exclusive), or until its owner leaves that world if {@code endsWithOwner}.
     */
    public record TimedArea(ServerWorld world, UUID owner, Vec3d center, long start, long end, boolean endsWithOwner,
            AreaTick tick) {
        /** Ticks since it started (1 on its first tick). */
        public long age(long now) {
            return now - start;
        }
    }

    /** Starts a timed area effect lasting {@code ticks} ticks; see {@link TimedArea}. */
    public static TimedArea startArea(ServerWorld world, UUID owner, Vec3d center, int ticks, boolean endsWithOwner,
            AreaTick tick) {
        long now = world.getTime();
        TimedArea area = new TimedArea(world, owner, center, now, now + ticks, endsWithOwner, tick);
        AREAS.add(area);
        return area;
    }

    private static void tickAreas(ServerWorld world) {
        if (AREAS.isEmpty())
            return;
        long now = world.getTime();
        AREAS.removeIf(area -> {
            if (area.world() != world)
                return false; // Ticked by its own world
            if (now >= area.end() || area.endsWithOwner() && world.getPlayerByUuid(area.owner()) == null)
                return true;
            area.tick().tick(world, area, now);
            return false;
        });
    }

    // ---- Thrown things ----

    private record Tracked(Entity entity, int maxAge, Consumer<Entity> onLanded) {
    }

    /**
     * Watches a thrown potion (or any projectile that's discarded where it hits) and runs {@code onLanded}
     * where it lands. It's dropped without landing if it's unloaded or still flying after {@code maxAge}
     * ticks.
     */
    public static void onLanding(Entity thrown, int maxAge, Consumer<Entity> onLanded) {
        TRACKED.add(new Tracked(thrown, maxAge, onLanded));
    }

    private static void tickTracked() {
        if (TRACKED.isEmpty())
            return;
        TRACKED.removeIf(tracked -> {
            Entity entity = tracked.entity();
            // A potion is discarded when it shatters
            if (entity.getRemovalReason() == Entity.RemovalReason.DISCARDED) {
                tracked.onLanded().accept(entity);
                return true;
            }
            // Unloaded, or flew off without landing
            return entity.isRemoved() || entity.age > tracked.maxAge();
        });
    }

    // ---- Per-player timers ----

    /**
     * Per-player timers for a skill's cooldown or timed buff, on one clock (server ticks) with one rule: a
     * timer started for {@code n} ticks at tick {@code t} runs while the tick is below {@code t + n}. Every
     * instance is cleared when the server stops. Only read on the server: with no server (the client), a
     * player has no timer running.
     */
    public static final class Cooldowns {
        private static final List<Cooldowns> ALL = new ArrayList<>();

        /** Server tick each player's timer ends at. */
        private final Map<UUID, Integer> ends = new HashMap<>();

        public Cooldowns() {
            ALL.add(this);
        }

        /** Starts (or restarts) the player's timer, running {@code ticks} ticks from now. */
        public void start(PlayerEntity player, int ticks) {
            MinecraftServer server = player.getServer();
            if (server != null) {
                ends.put(player.getUuid(), server.getTicks() + ticks);
            }
        }

        /** Whether the player's timer is running. Forgets it once it's over. */
        public boolean active(PlayerEntity player) {
            Integer end = ends.get(player.getUuid());
            if (end == null)
                return false;
            MinecraftServer server = player.getServer();
            if (server == null)
                return false;
            if (server.getTicks() < end)
                return true;
            ends.remove(player.getUuid());
            return false;
        }

        /** Whether the player is off cooldown: no timer running. */
        public boolean ready(PlayerEntity player) {
            return !active(player);
        }

        /** Ticks left on the player's timer, 0 if none. */
        public int remaining(PlayerEntity player) {
            Integer end = ends.get(player.getUuid());
            MinecraftServer server = player.getServer();
            return end == null || server == null ? 0 : Math.max(0, end - server.getTicks());
        }

        /** Uses up a one-shot timer (a charged strike): forgets it, and returns whether it was still running. */
        public boolean take(PlayerEntity player) {
            boolean running = active(player);
            ends.remove(player.getUuid());
            return running;
        }

        /**
         * Whether the player had a timer that has now run out; forgets it. For buffs that need undoing when
         * they end.
         */
        public boolean finished(PlayerEntity player) {
            return ends.containsKey(player.getUuid()) && !active(player);
        }

        /** Forgets the player's timer. Returns whether there was one. */
        public boolean remove(UUID id) {
            return ends.remove(id) != null;
        }

        /** Forgets every timer that has run out. */
        public void prune(MinecraftServer server) {
            int now = server.getTicks();
            ends.values().removeIf(end -> now >= end);
        }

        /**
         * Forgets every timer that has run out or whose player has logged off, calling {@code onEnd} for the
         * online players whose timer ran out (to undo a temporary modifier, which a log-off drops anyway).
         */
        public void expire(MinecraftServer server, Consumer<ServerPlayerEntity> onEnd) {
            if (ends.isEmpty())
                return;
            int now = server.getTicks();
            ends.entrySet().removeIf(entry -> {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
                if (player != null && now < entry.getValue())
                    return false;
                if (player != null)
                    onEnd.accept(player);
                return true;
            });
        }

        public boolean isEmpty() {
            return ends.isEmpty();
        }

        private void clear() {
            ends.clear();
        }
    }
}
