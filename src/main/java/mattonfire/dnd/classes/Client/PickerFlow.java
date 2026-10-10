package mattonfire.dnd.classes.Client;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Client.Hud.ClassSelectionHud;
import mattonfire.dnd.classes.Client.Hud.RaceSelectionHud;
import mattonfire.dnd.classes.Client.Keybinds.ModKeybinds;
import mattonfire.dnd.classes.Race.DndRace;
import mattonfire.dnd.classes.Race.DragonAncestry;
import mattonfire.dnd.classes.Race.RaceLifecycle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;

/**
 * Decides which character picker the client shows: the race picker first (when the server says the
 * player needs a race), then the class picker (when the class is NONE). The server's class and race
 * queries and approvals all come through here, so the two pickers never fight over the screen.
 * <p>
 * While a DevScript runs, the race picker stays shut unless the script says {@code racepicker on},
 * so existing scripts (whose dev-world player has a class but no race) don't stall on it.
 */
@Environment(EnvType.CLIENT)
public final class PickerFlow {
    /** Last class the server told us, or null before the first query. */
    private static DndCharacter knownClass;
    /** The server wants a race picked. */
    private static boolean racePrompt;
    /** False while a DevScript runs, until it says {@code racepicker on}. */
    static boolean racePickerAllowed = true;

    private PickerFlow() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(RaceLifecycle.S2C_RACE_QUERY, PickerFlow::handleRacePacket);
        ClientPlayNetworking.registerGlobalReceiver(RaceLifecycle.S2C_APPROVE_RACE_PICK,
                PickerFlow::handleRacePacket);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            knownClass = null;
            racePrompt = false;
        }));
    }

    private static void handleRacePacket(MinecraftClient client,
            net.minecraft.client.network.ClientPlayNetworkHandler handler, PacketByteBuf buf,
            net.fabricmc.fabric.api.networking.v1.PacketSender sender) {
        int race = buf.readVarInt();
        buf.readVarInt(); // ancestry: the player's own race comes through the DataTracker
        boolean prompt = buf.readBoolean();
        client.execute(() -> {
            racePrompt = prompt && race == DndRace.NONE.getValue();
            update(client);
        });
    }

    /** The server sent the class (query on join, or approval of a pick). */
    public static void onClass(MinecraftClient client, DndCharacter dndClass) {
        knownClass = dndClass;
        update(client);
    }

    /** DevScript {@code racepicker on|off}. */
    static void setRacePickerAllowed(MinecraftClient client, boolean allowed) {
        racePickerAllowed = allowed;
        update(client);
    }

    public static void update(MinecraftClient client) {
        if (client.player == null) {
            return;
        }
        if (racePrompt && racePickerAllowed) {
            if (!showing(client, RaceSelectionHud.class)) {
                client.setScreen(new ModKeybinds(new RaceSelectionHud()));
            }
            return;
        }
        if (showing(client, RaceSelectionHud.class)) {
            client.setScreen(null);
        }
        if (knownClass == DndCharacter.NONE) {
            if (!showing(client, ClassSelectionHud.class)) {
                client.setScreen(new ModKeybinds(new ClassSelectionHud()));
            }
        }
    }

    public static void showAncestryStep() {
        MinecraftClient.getInstance().setScreen(new ModKeybinds(new RaceSelectionHud(true)));
    }

    public static void showRaceStep() {
        MinecraftClient.getInstance().setScreen(new ModKeybinds(new RaceSelectionHud(false)));
    }

    /** What clicking a race (or an ancestry) sends. The server ignores it unless the race is NONE. */
    public static void sendPick(DndRace race, DragonAncestry ancestry) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(race.getValue());
        buf.writeVarInt(ancestry.getValue());
        ClientPlayNetworking.send(RaceLifecycle.C2S_RACE_PICK, buf);
    }

    private static boolean showing(MinecraftClient client, Class<?> description) {
        return client.currentScreen instanceof ModKeybinds screen && description.isInstance(screen.getDescription());
    }
}
