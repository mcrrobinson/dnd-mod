package mattonfire.dnd.classes.Client;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.CharacterCreation;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Client.Hud.CharacterCreationHud;
import mattonfire.dnd.classes.Client.Hud.CharacterCreationScreen;
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
 * Decides which character picker the client shows: the character-creation screen (race and class together)
 * when the server says the player needs both, the race picker when they need only a race, and the class picker
 * when they need only a class. The server's class and race queries and approvals all come through here, so the
 * pickers never fight over the screen.
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
    /**
     * Race and class answers still to come for a sent character-creation pick. Until both arrive the screen
     * stays as it is, so the first answer doesn't swap it for the other half's picker.
     */
    private static int awaitingAnswers;

    private PickerFlow() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(RaceLifecycle.S2C_RACE_QUERY, PickerFlow::handleRacePacket);
        ClientPlayNetworking.registerGlobalReceiver(RaceLifecycle.S2C_APPROVE_RACE_PICK,
                PickerFlow::handleRacePacket);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            knownClass = null;
            racePrompt = false;
            awaitingAnswers = 0;
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
            answered();
            update(client);
        });
    }

    /** The server sent the class (query on join, or approval of a pick). */
    public static void onClass(MinecraftClient client, DndCharacter dndClass) {
        knownClass = dndClass;
        answered();
        update(client);
    }

    private static void answered() {
        if (awaitingAnswers > 0) {
            awaitingAnswers--;
        }
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
        if (awaitingAnswers > 0 && showing(client, CharacterCreationHud.class)) {
            return;
        }
        boolean race = racePrompt && racePickerAllowed;
        // Before the first class query the class is unknown; it follows the race query straight away on join.
        boolean dndClass = knownClass == DndCharacter.NONE;
        if (race && knownClass == null) {
            return;
        }
        Class<?> wanted = race && dndClass ? CharacterCreationHud.class
                : race ? RaceSelectionHud.class
                        : dndClass ? ClassSelectionHud.class : null;
        boolean ours = showing(client, CharacterCreationHud.class) || showing(client, RaceSelectionHud.class)
                || showing(client, ClassSelectionHud.class);
        if (wanted == null) {
            if (ours) {
                client.setScreen(null);
            }
            return;
        }
        if (showing(client, wanted)) {
            return;
        }
        if (wanted == CharacterCreationHud.class) {
            client.setScreen(new CharacterCreationScreen(new CharacterCreationHud()));
        } else if (wanted == RaceSelectionHud.class) {
            client.setScreen(new ModKeybinds(new RaceSelectionHud()));
        } else {
            client.setScreen(new ModKeybinds(new ClassSelectionHud()));
        }
    }

    /** Reopens the character-creation screen with the same picks, e.g. for the Dragonborn ancestry step. */
    public static void showCharacterStep(CharacterCreationHud.Choice choice) {
        MinecraftClient.getInstance().setScreen(new CharacterCreationScreen(new CharacterCreationHud(choice)));
    }

    /** What Confirm on the character-creation screen sends. The server answers with the race and the class. */
    public static void sendCharacterPick(DndRace race, DragonAncestry ancestry, DndCharacter dndClass) {
        awaitingAnswers = 2;
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(race.getValue());
        buf.writeVarInt(ancestry.getValue());
        buf.writeVarInt(dndClass.getValue());
        ClientPlayNetworking.send(CharacterCreation.C2S_CHARACTER_PICK, buf);
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
