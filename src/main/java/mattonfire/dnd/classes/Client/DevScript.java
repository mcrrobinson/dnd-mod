package mattonfire.dnd.classes.Client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.mixin.MinecraftClientInvoker;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.RenameItemC2SPacket;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Dev-only automation, so the client can be driven without anyone at the keyboard.
 * <p>
 * Auto-join: with {@code -Ddnd.autojoin=<world folder>} (set by build.gradle's client run from
 * {@code -PdevWorld}, default "New World") the client opens that singleplayer world as soon as the
 * title screen appears. 1.19.4 has no {@code --quickPlaySingleplayer}; that only exists from 1.20.
 * With {@code -Ddnd.autoconnect=<host:port>} (from {@code -PdevServer}) it joins that server or LAN game
 * instead, e.g. a second client joining a world opened with {@code /publish false survival <port>}.
 * <p>
 * Scripts: start the client with {@code ./gradlew runClient -PdevScript=devscripts/<script>.txt}. Once the
 * player has joined the world, each line of the script runs on its own client tick:
 * <ul>
 * <li>{@code /<command>} runs a command as the player (e.g. {@code /summon ...})</li>
 * <li>{@code wait <ticks>} pauses the script (20 ticks = 1 second)</li>
 * <li>{@code screenshot <name>} saves {@code run/screenshots/<name>.png}</li>
 * <li>{@code hitboxes on|off} toggles hitbox rendering (F3+B)</li>
 * <li>{@code hud on|off} toggles the HUD (F1)</li>
 * <li>{@code closescreen} closes any open screen (e.g. the class picker shown on join)</li>
 * <li>{@code respawn} respawns the player if it's dead (a world saved mid-death loads dead)</li>
 * <li>{@code quit} closes the game cleanly</li>
 * </ul>
 * Input steps act inside the game, so they work while the window is hidden or unfocused:
 * <ul>
 * <li>{@code look <yaw> <pitch>} turns the player (yaw 0 = south)</li>
 * <li>{@code use} / {@code attack} press the use (right) / attack (left) button once, at the crosshair</li>
 * <li>{@code hotbar <0-8>} selects a hotbar slot</li>
 * <li>{@code press <key>} presses a key binding once, by translation key (e.g. {@code key.dnd-classes.power-up})</li>
 * <li>{@code perspective first|back|front} sets the camera (F5)</li>
 * <li>{@code slot <index> [action] [button]} clicks a slot of the open screen; action is a
 * {@link SlotActionType} name (default {@code pickup}; {@code quick_move} = shift-click, {@code swap} with
 * button 0-8 = number key, {@code throw} = Q)</li>
 * <li>{@code slots} logs every non-empty slot of the open screen (or the inventory)</li>
 * <li>{@code button <id>} clicks a screen button such as an enchanting option (0-2)</li>
 * <li>{@code rename <text>} sets the item name in an open anvil</li>
 * </ul>
 * Blank lines and lines starting with {@code #} are skipped. Progress is logged with a [DevScript]
 * prefix, and command feedback appears as [CHAT] lines in run/logs/latest.log.
 */
public final class DevScript {
    // Give chunks and entities a moment to load after joining before the first step
    private static final int START_DELAY_TICKS = 40;

    private final List<String> lines;
    private int next = 0;
    private int waitTicks = START_DELAY_TICKS;

    private DevScript(List<String> lines) {
        this.lines = lines;
    }

    public static void register() {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
            return;
        }
        registerAutoJoin();
        registerScript();
        // Dev clients have no real Mojang login, so a world opened to LAN must not check logins,
        // otherwise a second test client (-PdevServer) gets kicked
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.getServer() != null && client.getServer().isOnlineMode()) {
                client.getServer().setOnlineMode(false);
            }
        });
    }

    private static void registerAutoJoin() {
        String server = System.getProperty("dnd.autoconnect");
        if (server != null && !server.isBlank()) {
            registerAutoConnect(server);
            return;
        }
        String world = System.getProperty("dnd.autojoin");
        if (world == null || world.isBlank()) {
            return;
        }
        boolean[] tried = {false};
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Wait for resources (shaders) to finish loading, or joining crashes the renderer
            if (tried[0] || !(client.currentScreen instanceof TitleScreen) || client.getOverlay() != null) {
                return;
            }
            tried[0] = true;
            if (!client.getLevelStorage().levelExists(world)) {
                DnDClasses.LOGGER.error("[DevScript] Can't auto-join: no world folder '{}' in run/saves", world);
                return;
            }
            DnDClasses.LOGGER.info("[DevScript] Auto-joining world '{}'", world);
            client.createIntegratedServerLoader().start(new TitleScreen(), world);
        });
    }

    private static void registerAutoConnect(String server) {
        boolean[] tried = {false};
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Wait for resources (shaders) to finish loading, or joining crashes the renderer
            if (tried[0] || !(client.currentScreen instanceof TitleScreen) || client.getOverlay() != null) {
                return;
            }
            tried[0] = true;
            DnDClasses.LOGGER.info("[DevScript] Auto-connecting to {}", server);
            ConnectScreen.connect(new TitleScreen(), client, ServerAddress.parse(server), new ServerInfo("DevScript", server, false));
        });
    }

    private static void registerScript() {
        String path = System.getProperty("dnd.devscript");
        if (path == null) {
            return;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(Path.of(path));
        } catch (IOException e) {
            DnDClasses.LOGGER.error("[DevScript] Couldn't read {}", path, e);
            return;
        }
        DnDClasses.LOGGER.info("[DevScript] Loaded {} ({} lines)", path, lines.size());
        DevScript script = new DevScript(lines);
        ClientTickEvents.END_CLIENT_TICK.register(script::tick);
    }

    private void tick(MinecraftClient client) {
        if (client.player == null || client.world == null || this.next >= this.lines.size()) {
            return;
        }
        // An unfocused window would otherwise open the pause menu and freeze the integrated server
        client.options.pauseOnLostFocus = false;
        if (this.waitTicks > 0) {
            this.waitTicks--;
            return;
        }

        int lineNumber = this.next + 1;
        String line = this.lines.get(this.next++).trim();
        if (line.isEmpty() || line.startsWith("#")) {
            return;
        }
        DnDClasses.LOGGER.info("[DevScript] {}: {}", lineNumber, line);

        if (line.startsWith("/")) {
            client.player.networkHandler.sendChatCommand(line.substring(1));
            return;
        }
        String[] words = line.split("\\s+", 2);
        String argument = words.length > 1 ? words[1] : "";
        switch (words[0]) {
            case "wait" -> this.waitTicks = Integer.parseInt(argument);
            case "screenshot" -> ScreenshotRecorder.saveScreenshot(client.runDirectory, argument + ".png", client.getFramebuffer(),
                    message -> DnDClasses.LOGGER.info("[DevScript] {}", message.getString()));
            case "hitboxes" -> client.getEntityRenderDispatcher().setRenderHitboxes(argument.equals("on"));
            case "hud" -> client.options.hudHidden = argument.equals("off");
            case "closescreen" -> client.setScreen(null);
            case "respawn" -> {
                if (client.player.isDead()) {
                    client.player.requestRespawn();
                    client.setScreen(null);
                }
            }
            case "quit" -> client.scheduleStop();
            case "look" -> {
                String[] angles = argument.split("\\s+");
                client.player.setYaw(Float.parseFloat(angles[0]));
                client.player.setPitch(Float.parseFloat(angles[1]));
            }
            case "use" -> ((MinecraftClientInvoker) client).invokeDoItemUse();
            case "attack" -> ((MinecraftClientInvoker) client).invokeDoAttack();
            case "hotbar" -> client.player.getInventory().selectedSlot = Integer.parseInt(argument);
            case "press" -> press(client, argument, lineNumber);
            case "perspective" -> client.options.setPerspective(switch (argument) {
                case "back" -> Perspective.THIRD_PERSON_BACK;
                case "front" -> Perspective.THIRD_PERSON_FRONT;
                default -> Perspective.FIRST_PERSON;
            });
            case "slot" -> clickSlot(client, argument.split("\\s+"));
            case "slots" -> logSlots(client.player.currentScreenHandler);
            case "button" -> client.interactionManager.clickButton(client.player.currentScreenHandler.syncId,
                    Integer.parseInt(argument));
            case "rename" -> {
                if (client.player.currentScreenHandler instanceof AnvilScreenHandler anvil) {
                    anvil.setNewItemName(argument);
                    client.player.networkHandler.sendPacket(new RenameItemC2SPacket(argument));
                } else {
                    DnDClasses.LOGGER.warn("[DevScript] {}: rename needs an open anvil", lineNumber);
                }
            }
            default -> DnDClasses.LOGGER.warn("[DevScript] {}: unknown step '{}'", lineNumber, line);
        }
    }

    private static void press(MinecraftClient client, String translationKey, int lineNumber) {
        for (KeyBinding binding : client.options.allKeys) {
            if (binding.getTranslationKey().equals(translationKey)) {
                KeyBinding.onKeyPressed(KeyBindingHelper.getBoundKeyOf(binding));
                return;
            }
        }
        DnDClasses.LOGGER.warn("[DevScript] {}: no key binding '{}'", lineNumber, translationKey);
    }

    private static void clickSlot(MinecraftClient client, String[] args) {
        int index = Integer.parseInt(args[0]);
        SlotActionType action = args.length > 1 ? SlotActionType.valueOf(args[1].toUpperCase()) : SlotActionType.PICKUP;
        int button = args.length > 2 ? Integer.parseInt(args[2]) : 0;
        ScreenHandler handler = client.player.currentScreenHandler;
        client.interactionManager.clickSlot(handler.syncId, index, button, action, client.player);
    }

    private static void logSlots(ScreenHandler handler) {
        DnDClasses.LOGGER.info("[DevScript] slots of {} (cursor: {})", handler.getClass().getSimpleName(),
                describe(handler.getCursorStack()));
        for (int i = 0; i < handler.slots.size(); i++) {
            ItemStack stack = handler.slots.get(i).getStack();
            if (!stack.isEmpty()) {
                DnDClasses.LOGGER.info("[DevScript] slot {}: {}", i, describe(stack));
            }
        }
    }

    private static String describe(ItemStack stack) {
        if (stack.isEmpty()) {
            return "empty";
        }
        return stack.getCount() + " " + stack.getItem() + (stack.hasNbt() ? " " + stack.getNbt() : "");
    }
}
