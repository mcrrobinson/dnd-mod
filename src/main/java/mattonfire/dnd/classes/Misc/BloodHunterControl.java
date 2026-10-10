package mattonfire.dnd.classes.Misc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import mattonfire.dnd.classes.Progression.Classes.BloodHunterSkills;
import mattonfire.dnd.entity.boss.Boss;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Blood Hunter special: take control of the mob you are looking at. The player
 * teleports to the mob and takes its shape (via the optional Identity mod); the
 * mob is removed and comes back, with its original NBT (health, name,
 * equipment, ...), where the player stands when the time runs out.
 *
 * <p>Duration, range and success chance come from the special's rank
 * ({@link BloodHunterSkills#BLOOD_CONTROL}). Mobs with more max health than a
 * player resist more often. A failed attempt still spends the mana and costs
 * the Blood Hunter a heart (the blood price). Bosses (the Ender Dragon, the
 * Wither and the mod's own {@link Boss}es) can't be controlled at all.
 */
public final class BloodHunterControl {
    /** Max health at which the strong-mob penalty starts (a player's). */
    private static final float PENALTY_FREE_HEALTH = 20.0F;
    /** Success chance lost per point of max health above that, in percentage points. */
    private static final double PENALTY_PER_HEALTH = 0.25;
    /** The most the strong-mob penalty takes off, in percentage points. */
    private static final double MAX_PENALTY = 30.0;
    /** Damage the Blood Hunter takes when the mob resists (one heart). */
    private static final float BLOOD_PRICE = 2.0F;

    private record Controlled(int expiryTick, EntityType<?> type, NbtCompound nbt) {
    }

    private static final Map<UUID, Controlled> ACTIVE = new HashMap<>();

    private BloodHunterControl() {
    }

    public static boolean isIdentityLoaded() {
        return FabricLoader.getInstance().isModLoaded("identity");
    }

    /**
     * Whether the player has attacked a mob recently (Identity's hostility). Always false without
     * Identity. Mixins must call this instead of Identity directly: Mixin resolves every class a mixin
     * method calls when it applies it, so a direct call crashes the game when Identity is missing.
     */
    public static boolean hasIdentityHostility(PlayerEntity player) {
        return isIdentityLoaded() && BloodHunterIdentityCompat.hasHostility(player);
    }

    /**
     * Whether the player has taken a shape through Identity (Druid Wild Shape, Blood Hunter control).
     * Always false without Identity. Identity then supplies the hitbox and model, so racial sizes skip it.
     */
    public static boolean hasIdentityForm(PlayerEntity player) {
        return isIdentityLoaded() && BloodHunterIdentityCompat.hasForm(player);
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int now = server.getTicks();
            Iterator<Map.Entry<UUID, Controlled>> it = ACTIVE.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Controlled> entry = it.next();
                if (now < entry.getValue().expiryTick()) {
                    continue;
                }
                it.remove();
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
                if (player != null) {
                    release(player, entry.getValue());
                }
            }
        });

        // Don't leave the player stuck as the mob (Identity saves it) or lose the mob.
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> releaseNow(handler.player));
        ServerLifecycleEvents.SERVER_STOPPING.register(BloodHunterControl::releaseAll);
    }

    /** @return true if the mana is spent: the mob was taken over, or it resisted. */
    public static boolean takeControl(ServerPlayerEntity player) {
        if (!isIdentityLoaded()) {
            player.sendMessage(Text.of("Taking control of mobs needs the Identity mod."), true);
            return false;
        }
        if (ACTIVE.containsKey(player.getUuid())) {
            return false;
        }

        MobEntity target = findTarget(player, BloodHunterSkills.BLOOD_CONTROL.get(player, "Range"));
        if (target == null) {
            return false;
        }
        if (isBoss(target)) {
            player.sendMessage(Text.literal(target.getDisplayName().getString() + " is too powerful to control.")
                    .formatted(Formatting.RED), true);
            return false;
        }

        ServerWorld world = player.getWorld();
        if (player.getRandom().nextDouble() >= successChance(player, target)) {
            resist(player, target, world);
            return true;
        }

        NbtCompound saved = target.writeNbt(new NbtCompound());

        // Separate copy used only as the player's shape, so the original state
        // stays untouched for when the mob is released.
        Entity created = target.getType().create(world);
        if (!(created instanceof LivingEntity shape)) {
            return false;
        }
        NbtCompound shapeNbt = saved.copy();
        shapeNbt.remove("UUID");
        shape.readNbt(shapeNbt);

        if (!BloodHunterIdentityCompat.morph(player, shape)) {
            return false;
        }

        // A server-side position change has to go through requestTeleport
        // (which teleport() does), or the client never hears about it and the
        // player rubber-bands/slides back.
        player.teleport(world, target.getX(), target.getY(), target.getZ(), target.getYaw(), player.getPitch());
        player.setVelocity(Vec3d.ZERO);
        player.fallDistance = 0.0F;

        target.discard();
        ACTIVE.put(player.getUuid(), new Controlled(
                player.getServer().getTicks() + BloodHunterSkills.BLOOD_CONTROL.ticks(player, "Duration"),
                target.getType(), saved));
        return true;
    }

    /**
     * Chance (0 to 1) that the player takes over the target: the rank's success
     * chance, less 0.25 percentage points per point of max health above 20 (at
     * most 30). A zombie has no penalty, an iron golem or ravager (100) -20.
     */
    public static double successChance(PlayerEntity player, LivingEntity target) {
        double penalty = Math.min(MAX_PENALTY,
                Math.max(0.0, (target.getMaxHealth() - PENALTY_FREE_HEALTH) * PENALTY_PER_HEALTH));
        return Math.max(0.0, BloodHunterSkills.BLOOD_CONTROL.get(player, "Success") - penalty) / 100.0;
    }

    private static boolean isBoss(Entity entity) {
        return entity instanceof Boss || entity instanceof EnderDragonEntity || entity instanceof WitherEntity;
    }

    /** The mob shakes it off: a message, smoke and blood on the mob, and the blood price for the player. */
    private static void resist(ServerPlayerEntity player, MobEntity target, ServerWorld world) {
        player.sendMessage(Text.literal(target.getDisplayName().getString() + " resists your Blood Control!")
                .formatted(Formatting.DARK_RED), true);
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, target.getX(), target.getBodyY(0.5), target.getZ(),
                20, 0.4, 0.5, 0.4, 0.02);
        world.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, player.getX(), player.getBodyY(0.5), player.getZ(),
                8, 0.3, 0.4, 0.3, 0.1);
        world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_EVOKER_PREPARE_ATTACK,
                SoundCategory.PLAYERS, 1.0F, 0.6F);
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_PLAYER_HURT,
                SoundCategory.PLAYERS, 1.0F, 1.0F);
        player.damage(player.getDamageSources().magic(), BLOOD_PRICE);
    }

    private static MobEntity findTarget(ServerPlayerEntity player, double maxRange) {
        Vec3d eye = player.getCameraPosVec(1.0F);
        Vec3d look = player.getRotationVec(1.0F);

        // Don't take control through walls.
        double range = maxRange;
        HitResult blockHit = player.raycast(maxRange, 1.0F, false);
        if (blockHit.getType() != HitResult.Type.MISS) {
            range = blockHit.getPos().distanceTo(eye);
        }

        Vec3d end = eye.add(look.multiply(range));
        Box box = player.getBoundingBox().stretch(look.multiply(range)).expand(1.0D);
        // The last argument is a squared distance. Bosses are hit too, so the player is told why it failed.
        EntityHitResult hit = ProjectileUtil.raycast(player, eye, end, box,
                entity -> entity instanceof MobEntity && entity.isAlive(), range * range);
        return hit == null ? null : (MobEntity) hit.getEntity();
    }

    private static void releaseNow(ServerPlayerEntity player) {
        Controlled controlled = ACTIVE.remove(player.getUuid());
        if (controlled != null) {
            release(player, controlled);
        }
    }

    private static void releaseAll(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            releaseNow(player);
        }
        ACTIVE.clear();
    }

    private static void release(ServerPlayerEntity player, Controlled controlled) {
        if (isIdentityLoaded()) {
            BloodHunterIdentityCompat.unmorph(player);
        }

        ServerWorld world = player.getWorld();
        Entity mob = controlled.type().create(world);
        if (mob == null) {
            return;
        }
        mob.readNbt(controlled.nbt());
        if (world.getEntity(mob.getUuid()) != null) {
            mob.setUuid(UUID.randomUUID());
        }
        mob.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), 0.0F);
        mob.setVelocity(Vec3d.ZERO);
        mob.fallDistance = 0.0F;
        world.spawnEntity(mob);
    }
}
