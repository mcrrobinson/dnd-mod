package mattonfire.dnd.classes.Misc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Blood Hunter special: take control of the mob you are looking at (within 30
 * blocks) for 20 seconds. The player teleports to the mob and takes its shape
 * (via the optional Identity mod); the mob is removed and comes back, with its
 * original NBT (health, name, equipment, ...), where the player stands when the
 * time runs out.
 */
public final class BloodHunterControl {
    public static final double RANGE = 30.0D;
    public static final int DURATION_TICKS = 20 * 20;

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

    /** @return true if a mob was taken over (mana is only spent then). */
    public static boolean takeControl(ServerPlayerEntity player) {
        if (!isIdentityLoaded()) {
            player.sendMessage(Text.of("Taking control of mobs needs the Identity mod."), true);
            return false;
        }
        if (ACTIVE.containsKey(player.getUuid())) {
            return false;
        }

        MobEntity target = findTarget(player);
        if (target == null) {
            return false;
        }

        ServerWorld world = player.getWorld();
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
                player.getServer().getTicks() + DURATION_TICKS, target.getType(), saved));
        return true;
    }

    private static MobEntity findTarget(ServerPlayerEntity player) {
        Vec3d eye = player.getCameraPosVec(1.0F);
        Vec3d look = player.getRotationVec(1.0F);

        // Don't take control through walls.
        double range = RANGE;
        HitResult blockHit = player.raycast(RANGE, 1.0F, false);
        if (blockHit.getType() != HitResult.Type.MISS) {
            range = blockHit.getPos().distanceTo(eye);
        }

        Vec3d end = eye.add(look.multiply(range));
        Box box = player.getBoundingBox().stretch(look.multiply(range)).expand(1.0D);
        // The last argument is a squared distance.
        EntityHitResult hit = ProjectileUtil.raycast(player, eye, end, box,
                entity -> entity instanceof MobEntity && entity.isAlive()
                        && !(entity instanceof EnderDragonEntity) && !(entity instanceof WitherEntity),
                range * range);
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
