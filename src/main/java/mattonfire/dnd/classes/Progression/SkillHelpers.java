package mattonfire.dnd.classes.Progression;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;

/** Shared bits for skills: finding targets, effects and temporary summons. */
public final class SkillHelpers {
    private static final String SUMMON_TAG = "dndclasses.summon";

    /** Summoned entities and the world time they vanish at. */
    private static final Map<UUID, Long> SUMMONS = new HashMap<>();

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
}
