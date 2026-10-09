package mattonfire.dnd.classes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import mattonfire.dnd.classes.Damages.ModDamageTypes;
import mattonfire.dnd.classes.Party.PartyManager;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Progression.Classes.WarlockSkills;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Warlock class abilities:
 * - Right click with an empty main hand to throw a slow small fireball
 * (cooldown halved by the Infernal Fireballs skill)
 * (client sends C2S_WARLOCK_FIREBALL, see WarlockFireballMixin).
 * - Immune to fire, lava and fireball damage (DamageTypeTags.IS_FIRE).
 * - Hurt by water and rain (1 damage every 4s, never below 2 health).
 * - Special: breathes fire (DnDClasses.WARLOCK_FIREBREATH holds the world time it ends at).
 * Burns and hurts mobs in a beam, but not the Warlock's party or pets. Duration, reach,
 * damage and burn time come from WarlockSkills.FIRE_BREATH.
 */
public class Warlock {
    public static final Identifier C2S_WARLOCK_FIREBALL = Identifier.of(DnDClasses.MOD_ID, "warlock_fireball");

    // Ticks between fireballs.
    public static final int FIREBALL_COOLDOWN_TICKS = 20;
    // Acceleration per tick, vanilla is 0.1. Lower = slower fireball.
    private static final double FIREBALL_POWER = 0.05;

    // Ticks between damage while wet, the damage dealt, and the health it never takes you below.
    private static final int WET_DAMAGE_INTERVAL_TICKS = 80;
    private static final float WET_DAMAGE = 1.0F;
    private static final float WET_MIN_HEALTH = 2.0F;

    private static final Map<UUID, Integer> FIREBALL_READY_TICK = new HashMap<>();

    public static boolean isWarlock(PlayerEntity player) {
        return player instanceof PlayerEntityExt ext && ext.getDndClass() == DndCharacter.WARLOCK;
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(C2S_WARLOCK_FIREBALL, Warlock::receiveFireballRequest);

        ServerTickEvents.END_WORLD_TICK.register(Warlock::tickFireBreath);

        ServerPlayConnectionEvents.DISCONNECT
                .register((handler, server) -> FIREBALL_READY_TICK.remove(handler.getPlayer().getUuid()));

        // Fire, lava, burning and fireballs (including your own) don't hurt.
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof PlayerEntity player && isWarlock(player)
                    && source.isIn(DamageTypeTags.IS_FIRE)) {
                return false;
            }
            return true;
        });

        // Water and rain hurt, but never kill.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % WET_DAMAGE_INTERVAL_TICKS != 0) {
                return;
            }
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (isWarlock(player) && player.isAlive() && !player.isCreative() && !player.isSpectator()
                        && player.isTouchingWaterOrRain() && player.getHealth() > WET_MIN_HEALTH) {
                    player.damage(ModDamageTypes.of(player.getWorld(), ModDamageTypes.WARLOCK_WET_DAMAGE_SOURCE),
                            Math.min(WET_DAMAGE, player.getHealth() - WET_MIN_HEALTH));
                }
            }
        });
    }

    private static void receiveFireballRequest(MinecraftServer server, ServerPlayerEntity player,
            ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        server.execute(() -> throwFireball(server, player));
    }

    private static void throwFireball(MinecraftServer server, ServerPlayerEntity player) {
        if (!isWarlock(player) || !player.isAlive() || player.isSpectator()
                || !player.getMainHandStack().isEmpty()) {
            return;
        }
        if (mattonfire.dnd.classes.Effects.AntiMagicEffect.blocks(player)) {
            return;
        }

        int now = server.getTicks();
        Integer readyTick = FIREBALL_READY_TICK.get(player.getUuid());
        if (readyTick != null && now < readyTick) {
            return;
        }
        int cooldown = Progression.hasPassive(player, WarlockSkills.INFERNAL_FIREBALLS)
                ? FIREBALL_COOLDOWN_TICKS / 2
                : FIREBALL_COOLDOWN_TICKS;
        FIREBALL_READY_TICK.put(player.getUuid(), now + cooldown);

        Vec3d look = player.getRotationVec(1.0F);
        SmallFireballEntity fireball = new SmallFireballEntity(player.getWorld(), player, look.x, look.y, look.z);
        fireball.setPosition(player.getX() + look.x, player.getEyeY() - 0.1 + look.y, player.getZ() + look.z);
        fireball.powerX = look.x * FIREBALL_POWER;
        fireball.powerY = look.y * FIREBALL_POWER;
        fireball.powerZ = look.z * FIREBALL_POWER;
        fireball.setVelocity(look.multiply(0.3));
        player.getWorld().spawnEntity(fireball);

        player.swingHand(Hand.MAIN_HAND);
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0F, 1.0F);
    }

    /** Runs once per world; only handles the Warlocks that are in this world. */
    private static void tickFireBreath(ServerWorld world) {
        if (DnDClasses.WARLOCK_FIREBREATH.isEmpty()) {
            return;
        }
        long now = world.getTime();
        DnDClasses.WARLOCK_FIREBREATH.entrySet().removeIf(entry -> {
            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(entry.getKey());
            if (player == null || player.isRemoved()) {
                return true; // Went offline
            }
            if (player.getWorld() != world) {
                return false; // Handled when its own world ticks
            }
            long endTick = entry.getValue();
            if (now > endTick || !player.isAlive()) {
                sendBreathState(player, false, 0, 0);
                return true;
            }
            // Reach is in whole blocks; the beam is checked one block at a time.
            int reach = WarlockSkills.FIRE_BREATH.getInt(player, "Reach");
            sendBreathState(player, true, endTick, reach);
            breatheFire(world, player, reach);
            return false;
        });
    }

    /** Tells the Warlock's client whether to draw its own flames, until when and how far. */
    private static void sendBreathState(ServerPlayerEntity player, boolean active, long endTick, int reach) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(active);
        if (active) {
            buf.writeLong(endTick);
            buf.writeVarInt(reach);
        }
        ServerPlayNetworking.send(player, DnDClasses.S2C_WARLOCK_FIREBREATH, buf);
    }

    private static void breatheFire(ServerWorld world, ServerPlayerEntity player, int reach) {
        float damage = (float) WarlockSkills.FIRE_BREATH.get(player, "Damage");
        int burnSeconds = WarlockSkills.FIRE_BREATH.getInt(player, "Burn");
        Vec3d look = player.getRotationVec(1.0F);
        Vec3d start = player.getEyePos();
        for (int i = 1; i <= reach; i++) {
            Vec3d pos = start.add(look.multiply(i));

            // The Warlock's own client draws its flames.
            for (ServerPlayerEntity other : world.getPlayers()) {
                if (other != player) {
                    world.spawnParticles(other, ParticleTypes.FLAME, false, pos.x, pos.y, pos.z, 8, 0.2, 0.2, 0.2,
                            0.01);
                }
            }

            Box box = new Box(pos.x - 0.5, pos.y - 0.5, pos.z - 0.5, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5);
            for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, box,
                    e -> e != player && e.isAlive() && !isFriendly(player, e))) {
                entity.setOnFireFor(burnSeconds);
                // Credit the Warlock, so kills count and the party's no-friendly-fire rule applies.
                entity.damage(world.getDamageSources().indirectMagic(player, player), damage);
            }
        }
    }

    /** Party members and the Warlock's own pets don't get burned. */
    private static boolean isFriendly(ServerPlayerEntity player, LivingEntity entity) {
        return PartyManager.areInSameParty(player, entity)
                || entity instanceof Tameable pet && player.getUuid().equals(pet.getOwnerUuid());
    }
}
