package mattonfire.dnd.classes;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import mattonfire.dnd.classes.Registry.ModEnchantments;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Featherfall (Artificer boots enchantment).
 * <ul>
 * <li>Fall damage is cut by 50% / 70% / 90% at levels I / II / III
 * (vanilla Feather Falling IV is about 48%).</li>
 * <li>Level II and up: once a player has fallen far enough (8 blocks at II,
 * 3 at III) they slow-fall until they land. Sneak to drop normally.</li>
 * </ul>
 */
public class Featherfall {
    private static final float[] DAMAGE_REDUCTION = { 0f, 0.5f, 0.7f, 0.9f };
    /** Fall distance (blocks) before slow falling starts, per level. Level I never slow-falls. */
    private static final float[] SLOW_FALL_AFTER = { Float.MAX_VALUE, Float.MAX_VALUE, 8f, 3f };
    private static final int EFFECT_TICKS = 6;

    private static final Set<UUID> SLOW_FALLING = new HashSet<>();

    /** Called on disconnect. */
    public static void forget(UUID player) {
        SLOW_FALLING.remove(player);
    }

    public static int getLevel(LivingEntity entity) {
        return Math.min(EnchantmentHelper.getEquipmentLevel(ModEnchantments.FEATHERFALL_ENCHANTMENT, entity),
                DAMAGE_REDUCTION.length - 1);
    }

    /** Applies the Featherfall reduction to vanilla's computed fall damage. */
    public static int reduceFallDamage(LivingEntity entity, int damage) {
        if (damage <= 0) {
            return damage;
        }
        int level = getLevel(entity);
        if (level <= 0) {
            return damage;
        }
        return (int) Math.floor(damage * (1f - DAMAGE_REDUCTION[level]));
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                tickPlayer(player);
            }
        });
    }

    private static void tickPlayer(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        boolean airborne = !player.isOnGround() && !player.isTouchingWater() && !player.isInLava()
                && !player.isClimbing() && !player.hasVehicle() && !player.isFallFlying()
                && !player.getAbilities().flying && !player.isSpectator();
        int level = getLevel(player);
        if (!airborne || player.isSneaking() || level < 2) {
            SLOW_FALLING.remove(id);
            return;
        }
        // Slow falling resets fallDistance, so remember who is mid-glide until they land.
        if (!SLOW_FALLING.contains(id)) {
            if (player.fallDistance < SLOW_FALL_AFTER[level] || player.getVelocity().y >= 0) {
                return;
            }
            SLOW_FALLING.add(id);
        }
        StatusEffectInstance current = player.getStatusEffect(StatusEffects.SLOW_FALLING);
        if (current == null || current.getDuration() < EFFECT_TICKS / 2) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, EFFECT_TICKS, 0,
                    false, false, false));
        }
    }
}
