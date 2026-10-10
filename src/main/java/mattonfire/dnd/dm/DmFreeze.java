package mattonfire.dnd.dm;

import java.util.UUID;

import mattonfire.dnd.classes.IEntityDataSaver;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;

/**
 * {@code /dm freeze}: pauses mobs and players.
 * <ul>
 * <li>Mobs: {@code NoAI} on (the old value is kept in persistent data and put back), velocity zeroed,
 * {@code NoGravity} while airborne so flyers hang in place, glowing, tagged {@link #TAG}.</li>
 * <li>Players: movement speed x0, held at the spot they were frozen at, attacks, block breaking, block, item and
 * entity use and the power-up key all cancelled, and an action bar line. Saved in the player's persistent data, so
 * reconnecting doesn't escape it.</li>
 * <li>Frozen mobs and players take no damage, so nobody gets free hits during a pause.</li>
 * <li>DMs are never frozen.</li>
 * </ul>
 */
public final class DmFreeze {
    public static final String TAG = "dndclasses.dm_frozen";

    private static final net.minecraft.util.Identifier FIRST = new net.minecraft.util.Identifier("dndclasses", "dm_freeze");
    private static final String DATA_KEY = "DmFreeze";
    private static final UUID SPEED_MODIFIER = UUID.fromString("5f0e6c3a-2d1b-4f6e-9b1e-d3a7c0f1a2b4");
    private static final int MESSAGE_INTERVAL = 40;
    /** A frozen player further than this (squared) from their spot was teleported, not walking. */
    private static final double TELEPORT_SQ = 64.0D;

    private DmFreeze() {
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !isFrozen(entity));
        // An early phase, so a frozen player's swing is stopped before other listeners (attack rolls) see it.
        AttackEntityCallback.EVENT.addPhaseOrdering(FIRST, Event.DEFAULT_PHASE);
        AttackBlockCallback.EVENT.addPhaseOrdering(FIRST, Event.DEFAULT_PHASE);
        UseBlockCallback.EVENT.addPhaseOrdering(FIRST, Event.DEFAULT_PHASE);
        UseEntityCallback.EVENT.addPhaseOrdering(FIRST, Event.DEFAULT_PHASE);
        UseItemCallback.EVENT.addPhaseOrdering(FIRST, Event.DEFAULT_PHASE);
        PlayerBlockBreakEvents.BEFORE.addPhaseOrdering(FIRST, Event.DEFAULT_PHASE);
        AttackEntityCallback.EVENT.register(FIRST, (player, world, hand, entity, hit) -> blocked(player));
        AttackBlockCallback.EVENT.register(FIRST, (player, world, hand, pos, direction) -> blocked(player));
        UseBlockCallback.EVENT.register(FIRST, (player, world, hand, hit) -> blocked(player));
        UseEntityCallback.EVENT.register(FIRST, (player, world, hand, entity, hit) -> blocked(player));
        UseItemCallback.EVENT.register(FIRST, (player, world, hand) -> blocked(player) == ActionResult.FAIL
                ? TypedActionResult.fail(player.getStackInHand(hand))
                : TypedActionResult.pass(player.getStackInHand(hand)));
        PlayerBlockBreakEvents.BEFORE.register(FIRST, (world, player, pos, state, blockEntity) -> !isFrozen(player));
        ServerTickEvents.END_SERVER_TICK.register(DmFreeze::tick);
    }

    private static ActionResult blocked(PlayerEntity player) {
        return !player.world.isClient && isFrozen(player) ? ActionResult.FAIL : ActionResult.PASS;
    }

    public static boolean isFrozen(Entity entity) {
        return entity.getCommandTags().contains(TAG);
    }

    private static NbtCompound data(Entity entity) {
        return ((IEntityDataSaver) entity).getPersistentData();
    }

    /** Freezes a mob or player. Returns false for anything else, a DM, or something already frozen. */
    public static boolean freeze(Entity entity) {
        if (isFrozen(entity) || DungeonMaster.isDm(entity)) {
            return false;
        }
        NbtCompound saved = new NbtCompound();
        if (entity instanceof MobEntity mob) {
            saved.putBoolean("NoAI", mob.isAiDisabled());
            saved.putBoolean("NoGravity", mob.hasNoGravity());
            saved.putBoolean("Glowing", mob.isGlowing());
            mob.setAiDisabled(true);
            mob.setTarget(null);
            mob.getNavigation().stop();
            if (!mob.isOnGround()) {
                mob.setNoGravity(true);
            }
            mob.setGlowing(true);
        } else if (entity instanceof ServerPlayerEntity player) {
            saved.putDouble("X", player.getX());
            saved.putDouble("Y", player.getY());
            saved.putDouble("Z", player.getZ());
            player.sendMessage(Text.literal("The Dungeon Master holds the scene.").formatted(Formatting.AQUA), true);
        } else {
            return false;
        }
        entity.setVelocity(Vec3d.ZERO);
        entity.velocityModified = true;
        data(entity).put(DATA_KEY, saved);
        entity.addCommandTag(TAG);
        return true;
    }

    /** Lifts a freeze and puts back what it changed. Returns false if the entity wasn't frozen. */
    public static boolean unfreeze(Entity entity) {
        if (!isFrozen(entity)) {
            return false;
        }
        entity.removeScoreboardTag(TAG);
        NbtCompound saved = data(entity).getCompound(DATA_KEY);
        data(entity).remove(DATA_KEY);
        if (entity instanceof MobEntity mob) {
            mob.setAiDisabled(saved.getBoolean("NoAI"));
            mob.setNoGravity(saved.getBoolean("NoGravity"));
            mob.setGlowing(saved.getBoolean("Glowing"));
        } else if (entity instanceof ServerPlayerEntity player) {
            EntityAttributeInstance speed = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            if (speed != null) {
                speed.removeModifier(SPEED_MODIFIER);
            }
            player.sendMessage(Text.literal("The scene moves on.").formatted(Formatting.AQUA), true);
        }
        return true;
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (isFrozen(player)) {
                hold(player, server.getTicks());
            }
        }
    }

    /** Keeps a frozen player in place: slowed to nothing, pulled back if they jump, fly or get pushed. */
    private static void hold(ServerPlayerEntity player, int ticks) {
        EntityAttributeInstance speed = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(SPEED_MODIFIER) == null) {
            speed.addTemporaryModifier(new EntityAttributeModifier(SPEED_MODIFIER, "DM freeze", -1.0D,
                    EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        NbtCompound saved = data(player).getCompound(DATA_KEY);
        if (!saved.contains("X")) {
            // Frozen by a tag alone (e.g. /tag add): hold them where they are now.
            saved.putDouble("X", player.getX());
            saved.putDouble("Y", player.getY());
            saved.putDouble("Z", player.getZ());
            data(player).put(DATA_KEY, saved);
        }
        double x = saved.getDouble("X");
        double y = saved.getDouble("Y");
        double z = saved.getDouble("Z");
        double moved = player.squaredDistanceTo(x, y, z);
        if (moved > TELEPORT_SQ) {
            // Too far to have walked in a tick: a /tp or portal. Hold them at the new spot.
            saved.putDouble("X", player.getX());
            saved.putDouble("Y", player.getY());
            saved.putDouble("Z", player.getZ());
            data(player).put(DATA_KEY, saved);
        } else if (moved > 0.01D) {
            player.networkHandler.requestTeleport(x, y, z, player.getYaw(), player.getPitch());
        }
        player.setVelocity(Vec3d.ZERO);
        if (ticks % MESSAGE_INTERVAL == 0) {
            player.sendMessage(Text.literal("The Dungeon Master holds the scene.").formatted(Formatting.AQUA), true);
        }
    }

    /** Mobs and non-DM players in range, for {@code /dm freeze radius}. */
    public static boolean freezable(Entity entity) {
        return (entity instanceof MobEntity || entity instanceof ServerPlayerEntity) && entity instanceof LivingEntity
                && entity.isAlive() && !DungeonMaster.isDm(entity);
    }
}
