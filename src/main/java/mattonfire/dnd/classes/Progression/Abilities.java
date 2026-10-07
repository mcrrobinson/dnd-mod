package mattonfire.dnd.classes.Progression;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mattonfire.dnd.classes.Misc.PowerUpEffect;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * The active skills fired with the power-up key. The root of each tree is
 * the class's original power-up in {@link PowerUpEffect}.
 */
public final class Abilities {
    private static final int SHADOWSTEP_RANGE = 12;
    private static final int WOLF_LIFETIME_TICKS = 60 * 20;

    private static final String SUMMON_TAG = "dndclasses.summon";

    /** Summoned wolves and the world time they vanish at. */
    private static final Map<UUID, Long> SUMMONS = new HashMap<>();

    private Abilities() {
    }

    public static void register() {
        // Summons left over from before a restart.
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
     * Fires an active skill.
     *
     * @return false if nothing happened, so no mana is spent
     */
    public static boolean activate(ServerPlayerEntity player, SkillNode node) {
        if (node.isRoot()) {
            return PowerUpEffect.play(player.getServer(), player, Progression.classOf(player));
        }
        ServerWorld world = ((ServerWorld) player.getWorld());
        switch (node.id()) {
            case "barbarian.war_cry" -> {
                for (LivingEntity mob : hostilesNear(player, 8)) {
                    mob.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 120, 0), player);
                    mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 1), player);
                }
                effects(player, SoundEvents.ENTITY_RAVAGER_ROAR, ParticleTypes.ANGRY_VILLAGER, 20);
            }
            case "barbarian.ground_slam" -> {
                for (LivingEntity mob : enemiesNear(player, 5)) {
                    mob.damage(world.getDamageSources().playerAttack(player), 6.0F);
                    Vec3d push = mob.getPos().subtract(player.getPos()).multiply(1, 0, 1).normalize();
                    mob.takeKnockback(1.5, -push.x, -push.z);
                    mob.addVelocity(0, 0.4, 0);
                    mob.velocityModified = true;
                }
                world.spawnParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY(), player.getZ(), 6, 2.0,
                        0.2, 2.0, 0);
                effects(player, SoundEvents.ENTITY_GENERIC_EXPLODE, ParticleTypes.CLOUD, 30);
            }
            case "barbarian.titan" -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 400, 2));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 400, 0));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 400, 0));
                effects(player, SoundEvents.ENTITY_IRON_GOLEM_REPAIR, ParticleTypes.CRIT, 30);
            }
            case "druid.thorn_burst" -> {
                for (LivingEntity mob : hostilesNear(player, 6)) {
                    mob.damage(world.getDamageSources().thorns(player), 3.0F);
                    mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 3), player);
                }
                effects(player, SoundEvents.BLOCK_SWEET_BERRY_BUSH_PLACE, ParticleTypes.COMPOSTER, 40);
            }
            case "druid.regrowth" -> {
                Box box = player.getBoundingBox().expand(8);
                UUID owner = player.getUuid();
                for (LivingEntity ally : world.getEntitiesByClass(LivingEntity.class, box,
                        e -> e instanceof PlayerEntity
                                || e instanceof Tameable t && owner.equals(t.getOwnerUuid()))) {
                    ally.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 160, 1), player);
                    world.spawnParticles(ParticleTypes.HEART, ally.getX(), ally.getY() + 1, ally.getZ(), 3, 0.4,
                            0.4, 0.4, 0);
                }
                effects(player, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, ParticleTypes.HAPPY_VILLAGER, 20);
            }
            case "druid.call_of_the_wild" -> {
                for (int i = 0; i < 3; i++) {
                    WolfEntity wolf = EntityType.WOLF.create(world);
                    if (wolf == null)
                        continue;
                    double angle = i * Math.PI * 2 / 3;
                    wolf.refreshPositionAndAngles(player.getX() + Math.cos(angle) * 1.5, player.getY(),
                            player.getZ() + Math.sin(angle) * 1.5, player.getYaw(), 0);
                    wolf.setOwner(player);
                    wolf.addCommandTag(SUMMON_TAG);
                    SUMMONS.put(wolf.getUuid(), world.getTime() + WOLF_LIFETIME_TICKS);
                    world.spawnEntity(wolf);
                }
                effects(player, SoundEvents.ENTITY_WOLF_HOWL, ParticleTypes.POOF, 20);
            }
            case "rogue.shadowstep" -> {
                Vec3d target = shadowstepTarget(player);
                if (target == null)
                    return false;
                effects(player, SoundEvents.ENTITY_ENDERMAN_TELEPORT, ParticleTypes.LARGE_SMOKE, 20);
                player.requestTeleport(target.x, target.y, target.z);
                player.fallDistance = 0;
                effects(player, SoundEvents.ENTITY_ENDERMAN_TELEPORT, ParticleTypes.PORTAL, 30);
            }
            case "rogue.smoke_bomb" -> {
                for (LivingEntity mob : hostilesNear(player, 6)) {
                    mob.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 100, 0), player);
                    mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1), player);
                    if (mob instanceof MobEntity m && m.getTarget() == player) {
                        m.setTarget(null);
                    }
                }
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 60, 0));
                world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getY() + 1,
                        player.getZ(), 40, 2.0, 1.0, 2.0, 0.01);
                effects(player, SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, ParticleTypes.LARGE_SMOKE, 30);
            }
            case "rogue.death_mark" -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 200, 0));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 200, 1));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 200, 1));
                effects(player, SoundEvents.ENTITY_WITHER_AMBIENT, ParticleTypes.SMOKE, 30);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private static List<LivingEntity> hostilesNear(PlayerEntity player, double radius) {
        return player.getWorld().getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(radius),
                e -> e instanceof Monster && e.isAlive());
    }

    /** Hostile mobs, plus anything else that isn't a player or one of the player's pets. */
    private static List<LivingEntity> enemiesNear(PlayerEntity player, double radius) {
        UUID owner = player.getUuid();
        return player.getWorld().getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(radius),
                e -> e.isAlive() && !(e instanceof PlayerEntity)
                        && !(e instanceof Tameable t && owner.equals(t.getOwnerUuid())));
    }

    /** Where Shadowstep lands: the furthest free spot along the look direction, or null if none. */
    private static Vec3d shadowstepTarget(ServerPlayerEntity player) {
        Vec3d eye = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0F);
        HitResult hit = player.getWorld().raycast(new RaycastContext(eye, eye.add(look.multiply(SHADOWSTEP_RANGE)),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
        double distance = hit.getPos().distanceTo(eye) - 0.5;

        Vec3d feetOffset = player.getPos().subtract(eye);
        for (double d = distance; d > 1.0; d -= 0.5) {
            Vec3d feet = eye.add(look.multiply(d)).add(feetOffset);
            Box box = player.getBoundingBox().offset(feet.subtract(player.getPos()));
            if (player.getWorld().isSpaceEmpty(player, box)) {
                return feet;
            }
        }
        return null;
    }

    private static void effects(ServerPlayerEntity player, SoundEvent sound,
            net.minecraft.particle.ParticleEffect particle, int count) {
        player.getWorld().playSound(null, player.getBlockPos(), sound, SoundCategory.PLAYERS, 1.0F, 1.0F);
        ((ServerWorld) player.getWorld()).spawnParticles(particle, player.getX(), player.getY() + 1, player.getZ(), count,
                1.0, 0.6, 1.0, 0.05);
    }
}
