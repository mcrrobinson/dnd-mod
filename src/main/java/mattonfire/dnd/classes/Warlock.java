package mattonfire.dnd.classes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import mattonfire.dnd.classes.Damages.ModDamageTypes;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/**
 * Warlock class abilities:
 * - Right click with an empty main hand to throw a slow small fireball
 * (client sends C2S_WARLOCK_FIREBALL, see WarlockFireballMixin).
 * - Immune to fire, lava and fireball damage (DamageTypeTags.IS_FIRE).
 * - Hurt by water and rain (1 damage every 4s, never below 2 health).
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

        int now = server.getTicks();
        Integer readyTick = FIREBALL_READY_TICK.get(player.getUuid());
        if (readyTick != null && now < readyTick) {
            return;
        }
        FIREBALL_READY_TICK.put(player.getUuid(), now + FIREBALL_COOLDOWN_TICKS);

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
}
