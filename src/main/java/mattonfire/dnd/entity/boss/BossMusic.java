package mattonfire.dnd.entity.boss;

import io.netty.buffer.Unpooled;
import java.util.UUID;
import mattonfire.dnd.classes.DnDClasses;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Tells a client which fight track a boss bar it can see wants (or that the bar is gone), so
 * EventMusic can play it while the fight lasts.
 * Packet: bar UUID, then a boolean "has track" and the track's sound id.
 */
public final class BossMusic {
    public static final Identifier S2C_BOSS_MUSIC = Identifier.of(DnDClasses.MOD_ID, "boss_music");

    private BossMusic() {
    }

    public static void send(ServerPlayerEntity player, UUID bar, @Nullable SoundEvent track) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeUuid(bar);
        buf.writeBoolean(track != null);
        if (track != null) {
            buf.writeIdentifier(track.getId());
        }
        ServerPlayNetworking.send(player, S2C_BOSS_MUSIC, buf);
    }
}
