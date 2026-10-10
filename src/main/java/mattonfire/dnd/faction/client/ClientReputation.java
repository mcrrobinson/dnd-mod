package mattonfire.dnd.faction.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.faction.Reputation;
import mattonfire.dnd.faction.ReputationTier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.Identifier;

/**
 * The local player's standing with each faction, as last sent by the server
 * ({@code dndclasses:reputation_sync}). Read by the Journal's Factions tab.
 */
public final class ClientReputation {
    public record Entry(Identifier id, String nameKey, int color, int value) {
        public ReputationTier tier() {
            return ReputationTier.of(this.value);
        }
    }

    private static volatile List<Entry> entries = List.of();

    private ClientReputation() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(Reputation.SYNC_PACKET_ID, (client, handler, buf, sender) -> {
            int count = buf.readVarInt();
            List<Entry> read = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                read.add(new Entry(buf.readIdentifier(), buf.readString(), buf.readInt(), buf.readInt()));
            }
            List<Entry> result = Collections.unmodifiableList(read);
            client.execute(() -> {
                entries = result;
                DnDClasses.LOGGER.info("[Reputation] client sync: {}", result.stream()
                        .map(entry -> entry.id() + "=" + entry.value()).toList());
            });
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> entries = List.of());
    }

    public static List<Entry> entries() {
        return entries;
    }
}
