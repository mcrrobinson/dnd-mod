package mattonfire.dnd.entity.drow;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import mattonfire.dnd.classes.Conditions.Condition;
import mattonfire.dnd.classes.Conditions.Conditions;
import mattonfire.dnd.classes.DnDClasses;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * The Drow Mage's Darkness: a sphere of magical darkness, radius {@value #RADIUS}, for {@value #DURATION} ticks.
 * Every {@value #PULSE} ticks everything inside but drow and spiders is Blinded (the condition, so mobs lose
 * targets past 4 blocks and players get the blindness fog) and gets vanilla Darkness, for a little longer than a
 * pulse, so it wears off soon after you step out. Black smoke marks the edge.
 */
public final class DrowDarkness {
    public static final double RADIUS = 4.0D;
    public static final int DURATION = 160;
    public static final int PULSE = 10;
    /** Condition key, so the Darkness refreshes its own Blinded and leaves others alone. */
    public static final String KEY = "drow_darkness";

    private record Zone(ServerWorld world, Vec3d center, long endsAt, @Nullable LivingEntity caster) {
    }

    private static final List<Zone> ZONES = new ArrayList<>();

    private DrowDarkness() {
    }

    static void register() {
        ServerTickEvents.END_WORLD_TICK.register(DrowDarkness::tick);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> ZONES.clear());
    }

    /** Casts the Darkness at {@code center}. */
    public static void cast(ServerWorld world, Vec3d center, @Nullable LivingEntity caster) {
        ZONES.add(new Zone(world, center, world.getTime() + DURATION, caster));
        DnDClasses.LOGGER.info("[Drow] Darkness at {} {} {}", Math.round(center.x), Math.round(center.y), Math.round(center.z));
    }

    /** Whether {@code pos} is inside a Darkness in {@code world}. */
    public static boolean isInside(ServerWorld world, Vec3d pos) {
        for (Zone zone : ZONES) {
            if (zone.world == world && zone.center.squaredDistanceTo(pos) <= RADIUS * RADIUS) {
                return true;
            }
        }
        return false;
    }

    private static void tick(ServerWorld world) {
        long now = world.getTime();
        Iterator<Zone> it = ZONES.iterator();
        while (it.hasNext()) {
            Zone zone = it.next();
            if (zone.world != world) {
                continue;
            }
            if (now >= zone.endsAt) {
                it.remove();
                continue;
            }
            if (now % 4 == 0) {
                edge(world, zone.center);
            }
            if (now % PULSE == 0) {
                pulse(world, zone);
            }
        }
    }

    private static void pulse(ServerWorld world, Zone zone) {
        Box box = new Box(zone.center, zone.center).expand(RADIUS);
        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, box, e -> e.isAlive()
                && !DrowAllies.isAlly(e) && e.getBoundingBox().getCenter().squaredDistanceTo(zone.center) <= RADIUS * RADIUS)) {
            if (entity instanceof PlayerEntity player && (player.isCreative() || player.isSpectator())) {
                continue;
            }
            int ticks = PULSE + 15;
            Conditions.of(entity, Condition.BLINDED).source(zone.caster).key(KEY).ticks(ticks).silent().apply();
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, ticks + 20, 0, false, false, true));
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, ticks, 0, false, false, true));
        }
    }

    /** Black smoke scattered over the sphere's surface. */
    private static void edge(ServerWorld world, Vec3d center) {
        for (int i = 0; i < 24; i++) {
            double u = world.random.nextDouble() * 2.0D - 1.0D;
            double theta = world.random.nextDouble() * Math.PI * 2.0D;
            double r = Math.sqrt(1.0D - u * u);
            double x = center.x + RADIUS * r * Math.cos(theta);
            double y = center.y + RADIUS * u;
            double z = center.z + RADIUS * r * Math.sin(theta);
            world.spawnParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        world.spawnParticles(ParticleTypes.SQUID_INK, center.x, center.y, center.z, 6, RADIUS * 0.4D, RADIUS * 0.4D,
                RADIUS * 0.4D, 0.0D);
    }
}
