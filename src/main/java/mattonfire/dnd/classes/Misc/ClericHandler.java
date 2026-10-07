package mattonfire.dnd.classes.Misc;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Registry.ModEffects;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Cleric passives: permanent Haste and Night Vision, and the circle of ignoring
 * mobs while MOB_REPEL is active.
 */
public class ClericHandler {

    // Night vision starts flickering under 200 ticks, so refresh before that.
    private static final int REFRESH_BELOW = 220;
    private static final int EFFECT_DURATION = 400;
    public static final double REPEL_RADIUS = 16;

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(ClericHandler::onWorldTick);
    }

    public static boolean isCleric(@Nullable LivingEntity entity) {
        return entity instanceof PlayerEntityExt ext && ext.getDndClass() == DndCharacter.CLERIC;
    }

    /**
     * True if mobs should ignore this entity: a player with MOB_REPEL (a Cleric
     * using their power, or a party member inside their circle).
     */
    public static boolean isRepellingMobs(@Nullable LivingEntity entity) {
        return entity instanceof PlayerEntity && entity.hasStatusEffect(ModEffects.MOB_REPEL);
    }

    private static void onWorldTick(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!isCleric(player) && isRepellingMobs(player) && player.isAlive()) {
                // Party member sharing a Cleric's circle
                clearTargets(world, player);
            }
            if (!isCleric(player) || !player.isAlive()) {
                continue;
            }

            refreshEffect(player, StatusEffects.HASTE, 2);
            refreshEffect(player, StatusEffects.NIGHT_VISION, 0);

            if (player.hasStatusEffect(ModEffects.MOB_REPEL)) {
                DnDClasses.createParticleRing(world, player.getPos(), REPEL_RADIUS, 100);

                clearTargets(world, player);
            }
        }
    }

    // Mobs already chasing the player lose interest.
    private static void clearTargets(ServerWorld world, ServerPlayerEntity player) {
        if (world.getTime() % 10 == 0) {
            for (MobEntity mob : world.getEntitiesByClass(MobEntity.class,
                    player.getBoundingBox().expand(REPEL_RADIUS * 2), mob -> mob.getTarget() == player)) {
                mob.setTarget(null);
            }
        }
    }

    private static void refreshEffect(PlayerEntity player, StatusEffect effect, int amplifier) {
        StatusEffectInstance existing = player.getStatusEffect(effect);
        if (existing == null || existing.getDuration() < REFRESH_BELOW) {
            player.addStatusEffect(new StatusEffectInstance(effect, EFFECT_DURATION, amplifier, false, false, true));
        }
    }
}
