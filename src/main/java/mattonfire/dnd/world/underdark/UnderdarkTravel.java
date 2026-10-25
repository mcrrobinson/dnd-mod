package mattonfire.dnd.world.underdark;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.IEntityDataSaver;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import mattonfire.dnd.world.underdark.rift.RiftChasmPiece;
import mattonfire.dnd.world.underdark.rift.RiftLandingPiece;
import mattonfire.dnd.world.underdark.rift.UnderdarkRiftStructures;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;

/**
 * Moving players into and out of the Underdark (design 9, 2.6.1). The command half is {@code /underdark go} and
 * {@code back} ({@link #descend}, {@link #back}); the rift half is {@link #riftDown} (through a Rift Veil) and
 * {@link #riftUp} (a Rift Anchor's pull), with a {@link #COOLDOWN_TICKS} cooldown between rift trips.
 * <p>
 * Down: onto the east side of the anchor of a rift landing that starts in this chunk or the 8 round it, or else
 * onto a floor {@link UnderdarkLanding#findOrMake} finds (with an anchor placed beside it) or carves. Up: onto the
 * rubble ledge at the bottom of an Overworld rift starting in this chunk or the 8 round it, or else onto the
 * Overworld's surface at the same x and z (the nearest dry column within {@link #DRY_SEARCH} blocks).
 * <p>
 * Going down remembers where the player came from, in their persistent data under {@code dndUnderdark.Return}
 * (dimension, position, facing), so {@link #back} can return them there.
 */
public final class UnderdarkTravel {
    /** Persistent-data compound holding the Underdark travel state. */
    public static final String DATA = "dndUnderdark";
    /** Where the player was before going down: {@code Dim}, {@code X}, {@code Y}, {@code Z}, {@code Yaw}, {@code Pitch}. */
    public static final String RETURN = "Return";
    /** The Overworld time (ticks) until which rift travel is on cooldown. */
    public static final String COOLDOWN = "Cooldown";
    public static final int COOLDOWN_TICKS = 100;
    /** How far from a wet surface column {@link #riftUp} looks for dry ground. */
    public static final int DRY_SEARCH = 16;

    /**
     * Fired after a player has gone down into the Underdark or come back up (by command now, through rifts later).
     * Hook for followers: the Hirelings ticket teleports a player's hirelings along with them from here.
     */
    public static final Event<AfterTravel> AFTER_TRAVEL = EventFactory.createArrayBacked(AfterTravel.class,
            listeners -> (player, from, to) -> {
                for (AfterTravel listener : listeners) {
                    listener.afterTravel(player, from, to);
                }
            });

    @FunctionalInterface
    public interface AfterTravel {
        /** {@code player} has just arrived in {@code to} from {@code from}. */
        void afterTravel(ServerPlayerEntity player, ServerWorld from, ServerWorld to);
    }

    private UnderdarkTravel() {
    }

    /**
     * Sends {@code player} down into the Underdark at their x and z (onto a found floor, or a carved pocket), and
     * remembers where they came from unless they're already in the Underdark. Returns the spot, or null if this
     * world has no Underdark.
     */
    public static UnderdarkLanding.Spot descend(ServerPlayerEntity player) {
        ServerWorld underdark = Underdark.world(player.getServer());
        if (underdark == null) {
            return null;
        }
        if (!Underdark.isUnderdark(player.getWorld())) {
            remember(player);
        }
        UnderdarkLanding.Spot spot = UnderdarkLanding.findOrMake(underdark, player.getBlockX(), player.getBlockZ());
        teleport(player, underdark, spot.feet(), player.getYaw(), player.getPitch());
        DnDClasses.LOGGER.info("[Underdark] {} descends at {} {} {} ({})", player.getName().getString(),
                spot.feet().getX(), spot.feet().getY(), spot.feet().getZ(), spot.kind().id());
        return spot;
    }

    /**
     * Moves {@code player} (already in the Underdark or not) to {@code feet} in the Underdark facing {@code yaw},
     * remembering where they came from.
     */
    public static void sendTo(ServerPlayerEntity player, BlockPos feet, float yaw, float pitch) {
        ServerWorld underdark = Underdark.world(player.getServer());
        if (!Underdark.isUnderdark(player.getWorld())) {
            remember(player);
        }
        teleport(player, underdark, feet, yaw, pitch);
    }

    /**
     * Takes {@code player} back to where they were before going down, or to the Overworld's spawn if nothing is
     * remembered (or the remembered world is gone). Returns where they arrived.
     */
    public static BlockPos back(ServerPlayerEntity player) {
        NbtCompound data = data(player);
        ServerWorld target = null;
        BlockPos pos = null;
        float yaw = player.getYaw();
        float pitch = player.getPitch();
        if (data.contains(RETURN)) {
            NbtCompound ret = data.getCompound(RETURN);
            Identifier dim = Identifier.tryParse(ret.getString("Dim"));
            target = dim == null ? null : player.getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD, dim));
            pos = new BlockPos(ret.getInt("X"), ret.getInt("Y"), ret.getInt("Z"));
            yaw = ret.getFloat("Yaw");
            pitch = ret.getFloat("Pitch");
        }
        if (target == null) {
            target = player.getServer().getOverworld();
            BlockPos spawn = target.getSpawnPos();
            pos = target.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, spawn);
        }
        data.remove(RETURN);
        teleport(player, target, pos, yaw, pitch);
        DnDClasses.LOGGER.info("[Underdark] {} returns to {} {} {} in {}", player.getName().getString(), pos.getX(),
                pos.getY(), pos.getZ(), target.getRegistryKey().getValue());
        return pos;
    }

    /** Teleports {@code player} to the middle of the block {@code feet} in {@code to}, then fires {@link #AFTER_TRAVEL}. */
    public static void teleport(ServerPlayerEntity player, ServerWorld to, BlockPos feet, float yaw, float pitch) {
        ServerWorld from = player.getWorld();
        player.fallDistance = 0;
        player.teleport(to, feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, yaw, pitch);
        if (from != to) {
            AFTER_TRAVEL.invoker().afterTravel(player, from, to);
        }
    }

    /** Where a rift trip arrives: the feet's block, the facing, and how it was chosen (for the log line). */
    public record Arrival(BlockPos feet, float yaw, String kind) {
    }

    /** Whether {@code player} went through a rift too recently to go again. */
    public static boolean onCooldown(ServerPlayerEntity player) {
        return now(player) < data(player).getLong(COOLDOWN);
    }

    private static long now(ServerPlayerEntity player) {
        return player.getServer().getOverworld().getTime();
    }

    /**
     * Sends {@code player} down through a Rift Veil: to the rift landing below, or a found or carved floor. Slow
     * Falling for 5 s, a 1 s Darkness pulse, the "The Underdark" title, and the cooldown. Returns where they
     * arrived, or null if there's no Underdark.
     */
    public static Arrival riftDown(ServerPlayerEntity player) {
        ServerWorld underdark = Underdark.world(player.getServer());
        if (underdark == null) {
            return null;
        }
        Arrival arrival = downTarget(underdark, player.getBlockX(), player.getBlockZ());
        if (!Underdark.isUnderdark(player.getWorld())) {
            remember(player);
        }
        teleport(player, underdark, arrival.feet(), arrival.yaw(), 0.0F);
        data(player).putLong(COOLDOWN, now(player) + COOLDOWN_TICKS);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 100, 0, false, false, true));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 20, 0, false, false, false));
        player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 60, 20));
        player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.translatable("underdark.dndclasses.arrive.subtitle")));
        player.networkHandler.sendPacket(new TitleS2CPacket(Text.translatable("underdark.dndclasses.arrive.title")
                .formatted(Formatting.DARK_PURPLE)));
        underdark.playSound(null, arrival.feet(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.8F, 0.5F);
        DnDClasses.LOGGER.info("[Underdark] {} descends at {} {} {} ({})", player.getName().getString(),
                arrival.feet().getX(), arrival.feet().getY(), arrival.feet().getZ(), arrival.kind());
        return arrival;
    }

    /**
     * Sends a mob (or any other non-player entity) that fell into a Rift Veil down the way a player would go.
     * Returns the moved entity, or null.
     */
    public static Entity riftDown(Entity entity) {
        ServerWorld underdark = Underdark.world(entity.getServer());
        if (underdark == null) {
            return null;
        }
        Arrival arrival = downTarget(underdark, entity.getBlockX(), entity.getBlockZ());
        Entity moved = FabricDimensions.teleport(entity, underdark, new TeleportTarget(Vec3d.ofBottomCenter(arrival.feet()),
                Vec3d.ZERO, arrival.yaw(), 0.0F));
        DnDClasses.LOGGER.info("[Underdark] {} falls through a rift to {} {} {} ({})", entity.getName().getString(),
                arrival.feet().getX(), arrival.feet().getY(), arrival.feet().getZ(), arrival.kind());
        return moved;
    }

    /**
     * Pulls {@code player} up out of the Underdark (a Rift Anchor's pull): to the rift above, or the surface. Starts
     * the cooldown and forgets the {@code /underdark back} spot. Returns where they arrived.
     */
    public static Arrival riftUp(ServerPlayerEntity player) {
        ServerWorld overworld = player.getServer().getOverworld();
        Arrival arrival = upTarget(overworld, player.getBlockX(), player.getBlockZ());
        data(player).remove(RETURN);
        teleport(player, overworld, arrival.feet(), arrival.yaw(), 0.0F);
        data(player).putLong(COOLDOWN, now(player) + COOLDOWN_TICKS);
        overworld.playSound(null, arrival.feet(), SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundCategory.PLAYERS,
                0.8F, 0.7F);
        DnDClasses.LOGGER.info("[Underdark] {} ascends to {} {} {} ({})", player.getName().getString(),
                arrival.feet().getX(), arrival.feet().getY(), arrival.feet().getZ(), arrival.kind());
        return arrival;
    }

    /** Where a rift drops something at (x, z): the landing below (rule 1), or a found or carved floor (rule 2). */
    public static Arrival downTarget(ServerWorld underdark, int x, int z) {
        RiftLandingPiece landing = UnderdarkRifts.nearbyPiece(underdark, UnderdarkRiftStructures.LANDING_KEY,
                RiftLandingPiece.class, x, z);
        if (landing != null) {
            BlockPos feet = landing.arrival();
            underdark.getChunk(feet.getX() >> 4, feet.getZ() >> 4); // generated, so the dome is there
            return new Arrival(feet, Direction.EAST.asRotation(), "landing");
        }
        UnderdarkLanding.Spot spot = UnderdarkLanding.findOrMake(underdark, x, z);
        if (spot.kind() == UnderdarkLanding.Kind.FOUND) {
            UnderdarkRifts.ensureAnchorNear(underdark, spot.feet());
        }
        return new Arrival(spot.feet(), UnderdarkLanding.openYaw(underdark, spot.feet()), spot.kind().id());
    }

    /** Where an anchor at (x, z) pulls you up to: the rift above (rule 1), or the Overworld's surface (rule 2). */
    public static Arrival upTarget(ServerWorld overworld, int x, int z) {
        RiftChasmPiece chasm = UnderdarkRifts.nearbyPiece(overworld, UnderdarkRiftStructures.RIFT_KEY, RiftChasmPiece.class,
                x, z);
        if (chasm != null) {
            BlockPos feet = chasm.ledge();
            overworld.getChunk(feet.getX() >> 4, feet.getZ() >> 4);
            return new Arrival(feet, chasm.yawToPool(), "rift");
        }
        BlockPos best = null;
        for (int r = 0; r <= DRY_SEARCH && best == null; r++) {
            for (int dx = -r; dx <= r && best == null; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    BlockPos top = overworld.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                            new BlockPos(x + dx, 0, z + dz));
                    if (overworld.getFluidState(top.down()).isEmpty() && overworld.getFluidState(top).isEmpty()) {
                        best = top;
                        break;
                    }
                }
            }
        }
        if (best == null) {
            best = overworld.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        }
        return new Arrival(best, 0.0F, "surface");
    }

    private static void remember(ServerPlayerEntity player) {
        NbtCompound ret = new NbtCompound();
        World world = player.getWorld();
        ret.putString("Dim", world.getRegistryKey().getValue().toString());
        ret.putInt("X", player.getBlockX());
        ret.putInt("Y", player.getBlockY());
        ret.putInt("Z", player.getBlockZ());
        ret.putFloat("Yaw", player.getYaw());
        ret.putFloat("Pitch", player.getPitch());
        data(player).put(RETURN, ret);
    }

    private static NbtCompound data(ServerPlayerEntity player) {
        NbtCompound all = ((IEntityDataSaver) player).getPersistentData();
        if (!all.contains(DATA)) {
            all.put(DATA, new NbtCompound());
        }
        return all.getCompound(DATA);
    }
}
