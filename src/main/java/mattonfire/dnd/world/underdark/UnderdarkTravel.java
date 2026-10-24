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

/**
 * Moving players into and out of the Underdark. The foundation ticket has the command half ({@code /underdark go}
 * and {@code back}); the rifts ticket (T2) adds Rift Veil and Rift Anchor travel on top of {@link #descend} and
 * {@link #teleport}.
 * <p>
 * Going down remembers where the player came from, in their persistent data under {@code dndUnderdark.Return}
 * (dimension, position, facing), so {@link #back} can return them there.
 */
public final class UnderdarkTravel {
    /** Persistent-data compound holding the Underdark travel state. */
    public static final String DATA = "dndUnderdark";
    /** Where the player was before going down: {@code Dim}, {@code X}, {@code Y}, {@code Z}, {@code Yaw}, {@code Pitch}. */
    public static final String RETURN = "Return";

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
