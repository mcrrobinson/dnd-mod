package mattonfire.dnd.classes.Misc;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Progression.Classes.ClericSkills;
import mattonfire.dnd.classes.Registry.ModEffects;
import mattonfire.dnd.magic.RemoveCurse;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Cleric passives: permanent Haste and Night Vision, the circle of ignoring
 * mobs while MOB_REPEL is active, and Remove Curse on a player ({@link RemoveCurse}).
 */
public class ClericHandler {

    // Night vision starts flickering under 200 ticks, so refresh before that.
    private static final int REFRESH_BELOW = 220;
    private static final int EFFECT_DURATION = 400;
    /** Mobs within this many blocks that are chasing a repelling player give up. */
    private static final double CHASE_CANCEL_RADIUS = 32;
    /** Ring radius while Sanctuary only covers the Cleric (rank I). */
    private static final double SOLO_RING_RADIUS = 2;
    private static final int BASE_HASTE = 2;
    private static final int DEEP_DELVER_HASTE = 3;
    /** Ticks between Deep Delver checks while Haste is still running. */
    private static final int HASTE_RECHECK = 20;
    /**
     * The Sanctuary ring draws a third of its points each tick (each point every 3 ticks): a third of the
     * particle packets. 3 is the shortest visible life of an enchanted-hit particle, so the ring stays whole.
     */
    private static final int RING_PHASES = 3;

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(ClericHandler::onWorldTick);
        // Remove Curse: sneak + right-click a player with an empty hand (sneak + use on the air is the
        // client's C2S_SELF packet, see RemoveCurse)
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            // The client sends interact-at first (hit set), then plain interact: handle only the latter
            if (hit != null || hand != Hand.MAIN_HAND || !player.isSneaking() || !player.getMainHandStack().isEmpty()
                    || !(entity instanceof PlayerEntity) || !isCleric(player)) {
                return ActionResult.PASS;
            }
            if (player instanceof ServerPlayerEntity cleric && entity instanceof ServerPlayerEntity target) {
                RemoveCurse.cleric(cleric, target);
            }
            return ActionResult.SUCCESS;
        });
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

            // Deep Delver (skill tree) raises Haste III to Haste IV. Reading the passive parses the class
            // progress NBT, so only do it when Haste needs (re)applying, or once a second to pick up a change.
            StatusEffectInstance haste = player.getStatusEffect(StatusEffects.HASTE);
            if (haste == null || haste.getDuration() < REFRESH_BELOW || world.getTime() % HASTE_RECHECK == 0) {
                refreshEffect(player, StatusEffects.HASTE,
                        Progression.hasPassive(player, "cleric.deep_delver") ? DEEP_DELVER_HASTE : BASE_HASTE);
            }
            refreshEffect(player, StatusEffects.NIGHT_VISION, 0);

            if (player.hasStatusEffect(ModEffects.MOB_REPEL)) {
                // The ring shows how far the party share reaches at the Cleric's Sanctuary rank.
                double reach = ClericSkills.sanctuaryPartyReach(player);
                DnDClasses.createParticleRing(world, player.getPos(), reach > 0 ? reach : SOLO_RING_RADIUS,
                        reach > 0 ? 100 : 20, RING_PHASES);

                clearTargets(world, player);
            }
        }
    }

    // Mobs already chasing the player lose interest.
    private static void clearTargets(ServerWorld world, ServerPlayerEntity player) {
        if (world.getTime() % 10 == 0) {
            for (MobEntity mob : world.getEntitiesByClass(MobEntity.class,
                    player.getBoundingBox().expand(CHASE_CANCEL_RADIUS), mob -> mob.getTarget() == player)) {
                mob.setTarget(null);
            }
        }
    }

    private static void refreshEffect(PlayerEntity player, StatusEffect effect, int amplifier) {
        StatusEffectInstance existing = player.getStatusEffect(effect);
        if (existing != null && existing.getAmplifier() != amplifier && !existing.isAmbient()
                && !existing.shouldShowParticles()) {
            // Our own effect at the wrong level (Deep Delver changed): replace it.
            player.removeStatusEffect(effect);
            existing = null;
        }
        if (existing == null || existing.getDuration() < REFRESH_BELOW) {
            player.addStatusEffect(new StatusEffectInstance(effect, EFFECT_DURATION, amplifier, false, false, true));
        }
    }
}
