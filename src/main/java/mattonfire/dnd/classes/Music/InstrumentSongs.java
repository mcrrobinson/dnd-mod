package mattonfire.dnd.classes.Music;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.DnDClasses;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/**
 * Tells clients in earshot of an instrument song how long it lasts, so MusicStings can duck the
 * background music under it the way it does for a class special's sting.
 * Packet: the song length in ticks.
 */
public final class InstrumentSongs {
    public static final Identifier S2C_INSTRUMENT_SONG = Identifier.of(DnDClasses.MOD_ID, "instrument_song");

    /** The song files are 4 s long. */
    public static final int SONG_TICKS = 80;

    /** How far away a song is heard (and ducks the music); the sound plays at volume 2, so 32 blocks. */
    public static final double HEARING_RANGE = 32.0;

    private InstrumentSongs() {
    }

    public static void sendDuck(ServerWorld world, Vec3d pos) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.squaredDistanceTo(pos) <= HEARING_RANGE * HEARING_RANGE) {
                PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                buf.writeVarInt(SONG_TICKS);
                ServerPlayNetworking.send(player, S2C_INSTRUMENT_SONG, buf);
            }
        }
    }
}
