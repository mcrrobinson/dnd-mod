package mattonfire.dnd.classes.Client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.mixin.MouseAccessor;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.client.gui.widget.PressableWidget;
import mattonfire.dnd.classes.mixin.MinecraftClientInvoker;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.RenameItemC2SPacket;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;

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
 * <li>{@code waitchat <text>} pauses until a chat or game message containing {@code text} arrives (at most 5
 *     minutes), to keep two clients in step: one sends {@code /say step2} or {@code /me step2}, the other waits
 *     for {@code step2}</li>
 * <li>{@code screenshot <name>} saves {@code run/screenshots/<name>.png}</li>
 * <li>{@code hitboxes on|off} toggles hitbox rendering (F3+B)</li>
 * <li>{@code serverhitboxes on|off} also draws the integrated server's dragon part shapes (red);
 *     {@code serverhitboxes measure} logs how far they are from the client's</li>
 * <li>{@code hud on|off} toggles the HUD (F1)</li>
 * <li>{@code clearchat} clears the chat (F3+D), so it doesn't cover the HUD in a screenshot</li>
 * <li>{@code closescreen} closes any open screen (e.g. the class picker shown on join); a container screen is
 *     closed on the server too, like pressing Esc</li>
 * <li>{@code respawn} respawns the player if it's dead (a world saved mid-death loads dead)</li>
 * <li>{@code quit} closes the game cleanly</li>
 * </ul>
 * Input steps act inside the game, so they work while the window is hidden or unfocused:
 * <ul>
 * <li>{@code look <yaw> <pitch>} turns the player (yaw 0 = south)</li>
 * <li>{@code use} / {@code attack} press the use (right) / attack (left) button once, at the crosshair</li>
 * <li>{@code hotbar <0-8>} selects a hotbar slot</li>
 * <li>{@code sneak on|off} holds or releases the sneak key</li>
 * <li>{@code holduse on|off} holds or releases the use (right) button, e.g. to keep drawing a bow</li>
 * <li>{@code mine on|off} keeps breaking the block at the crosshair every tick, like holding the attack
 *     button (which needs a focused window), e.g. {@code mine on}, {@code wait 60}, {@code mine off}</li>
 * <li>{@code press <key>} presses a key binding once, by translation key (e.g. {@code key.dnd-classes.power-up})</li>
 * <li>{@code holdkey <key> on|off} holds or releases a key binding, by translation key (e.g. holding
 *     {@code key.dnd-classes.power-up} for 3 s gives up while Downed)</li>
 * <li>{@code perspective first|back|front} sets the camera (F5)</li>
 * <li>{@code slot <index> [action] [button]} clicks a slot of the open screen; action is a
 * {@link SlotActionType} name (default {@code pickup}; {@code quick_move} = shift-click, {@code swap} with
 * button 0-8 = number key, {@code throw} = Q)</li>
 * <li>{@code slots} logs every non-empty slot of the open screen (or the inventory)</li>
 * <li>{@code button <id>} clicks a screen button such as an enchanting option (0-2)</li>
 * <li>{@code rename <text>} sets the item name in an open anvil</li>
 * <li>{@code click <dx> <dy> [button]} clicks the open screen at GUI coordinates measured from its centre
 *     (button 0 = left, 1 = right), e.g. the skill tree's tabs</li>
 * <li>{@code widget <label>} presses the open screen's button with that label (e.g. {@code Yes} in a confirm
 *     dialog)</li>
 * <li>{@code page <n|last>} turns an open book to page n (from 0) or the last page</li>
 * <li>{@code hover <dx> <dy>} moves the cursor to GUI coordinates measured from the screen's centre, so the open
 *     screen draws the tooltip there (take a screenshot after it)</li>
 * <li>{@code skill unlock|equip|rankup|bestiary|subclass <id>} sends what clicking the skill tree screen would
 *     (left-click, left-click at a table, right-click at a table, a bestiary entry, confirming a subclass), so
 *     the server's checks apply</li>
 * <li>{@code tooltips [hotbar slots...]} opens a screen showing the tooltips of the hotbar items (all, or the
 *     slots listed, e.g. {@code tooltips 0 1 4}) side by side and logs their lines; close it with {@code closescreen}</li>
 * <li>{@code escape} sends the open screen an Escape key press, e.g. to check a picker ignores it</li>
 * <li>{@code racepicker on|off}: while a script runs the race picker stays shut (the dev-world player has a class
 *     but no race, so it would block every script). {@code on} lets it open, straight away if the player has no
 *     race</li>
 * <li>{@code racepick <race> [ancestry]} sends what clicking a race (or a Dragonborn ancestry) in the picker
 *     would, e.g. {@code racepick elf}, {@code racepick dragonborn frost}; the server's one-pick check applies</li>
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
    /** Text a {@code waitchat} step is waiting for, or null. */
    private String waitChat;
    private int waitChatTicks;
    private static final int WAIT_CHAT_TIMEOUT = 20 * 60 * 5;
    /** Messages received since the last {@code waitchat} step finished (so one sent early still counts). */
    private static final List<String> RECEIVED = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
    private boolean mining;

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
        PickerFlow.racePickerAllowed = false;
        ClientTickEvents.END_CLIENT_TICK.register(script::tick);
        net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.GAME
                .register((message, overlay) -> RECEIVED.add(message.getString()));
        net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.CHAT
                .register((message, signed, sender, params, timestamp) -> RECEIVED.add(message.getString()));
    }

    private void tick(MinecraftClient client) {
        if (client.player == null || client.world == null || this.next >= this.lines.size()) {
            return;
        }
        // An unfocused window would otherwise open the pause menu and freeze the integrated server
        client.options.pauseOnLostFocus = false;
        if (this.mining && client.crosshairTarget instanceof net.minecraft.util.hit.BlockHitResult hit
                && hit.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
            client.interactionManager.updateBlockBreakingProgress(hit.getBlockPos(), hit.getSide());
            client.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        }
        if (this.waitTicks > 0) {
            this.waitTicks--;
            return;
        }
        if (this.waitChat != null) {
            boolean seen;
            synchronized (RECEIVED) {
                seen = RECEIVED.stream().anyMatch(message -> message.contains(this.waitChat));
            }
            if (!seen && ++this.waitChatTicks < WAIT_CHAT_TIMEOUT) {
                return;
            }
            DnDClasses.LOGGER.info("[DevScript] waitchat {}: {}", this.waitChat, seen ? "seen" : "timed out");
            this.waitChat = null;
            RECEIVED.clear();
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
            case "waitchat" -> {
                this.waitChat = argument;
                this.waitChatTicks = 0;
            }
            case "screenshot" -> ScreenshotRecorder.saveScreenshot(client.runDirectory, argument + ".png", client.getFramebuffer(),
                    message -> DnDClasses.LOGGER.info("[DevScript] {}", message.getString()));
            case "hitboxes" -> client.getEntityRenderDispatcher().setRenderHitboxes(argument.equals("on"));
            case "serverhitboxes" -> {
                if (argument.equals("measure")) {
                    mattonfire.dnd.client.renderer.ServerPartDebug.measure(client);
                } else {
                    mattonfire.dnd.client.renderer.ServerPartDebug.enabled = argument.equals("on");
                }
            }
            case "hud" -> client.options.hudHidden = argument.equals("off");
            case "clearchat" -> client.inGameHud.getChatHud().clear(false);
            case "closescreen" -> {
                // setScreen(null) alone leaves a container open on the server (brewing stand, chest, ...)
                if (client.currentScreen instanceof HandledScreen<?>) {
                    client.player.closeHandledScreen();
                } else {
                    client.setScreen(null);
                }
            }
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
            case "sneak" -> client.options.sneakKey.setPressed(argument.equals("on"));
            case "holduse" -> client.options.useKey.setPressed(argument.equals("on"));
            case "mine" -> {
                this.mining = argument.equals("on");
                if (!this.mining) {
                    client.interactionManager.cancelBlockBreaking();
                }
            }
            case "hotbar" -> client.player.getInventory().selectedSlot = Integer.parseInt(argument);
            case "press" -> press(client, argument, lineNumber);
            case "holdkey" -> holdKey(client, argument.split("\\s+"), lineNumber);
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
            case "click" -> {
                String[] args = argument.split("\\s+");
                if (client.currentScreen != null) {
                    double x = client.currentScreen.width / 2.0 + Double.parseDouble(args[0]);
                    double y = client.currentScreen.height / 2.0 + Double.parseDouble(args[1]);
                    int button = args.length > 2 ? Integer.parseInt(args[2]) : 0;
                    client.currentScreen.mouseClicked(x, y, button);
                    client.currentScreen.mouseReleased(x, y, button);
                } else {
                    DnDClasses.LOGGER.warn("[DevScript] {}: click needs an open screen", lineNumber);
                }
            }
            case "widget" -> {
                boolean found = false;
                if (client.currentScreen != null) {
                    for (var child : client.currentScreen.children()) {
                        if (child instanceof PressableWidget widget && widget.getMessage().getString().equals(argument)) {
                            widget.onPress();
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    DnDClasses.LOGGER.warn("[DevScript] {}: no button '{}' on the open screen", lineNumber, argument);
                }
            }
            case "page" -> {
                if (client.currentScreen instanceof BookScreen book) {
                    book.setPage(argument.equals("last") ? Integer.MAX_VALUE / 2 : Integer.parseInt(argument));
                } else {
                    DnDClasses.LOGGER.warn("[DevScript] {}: page needs an open book", lineNumber);
                }
            }
            case "hover" -> {
                String[] args = argument.split("\\s+");
                double scale = client.getWindow().getScaleFactor();
                double x = (client.getWindow().getScaledWidth() / 2.0 + Double.parseDouble(args[0])) * scale;
                double y = (client.getWindow().getScaledHeight() / 2.0 + Double.parseDouble(args[1])) * scale;
                ((MouseAccessor) client.mouse).setX(x);
                ((MouseAccessor) client.mouse).setY(y);
            }
            case "skill" -> skill(argument.split("\\s+"), lineNumber);
            case "tooltips" -> {
                java.util.List<ItemStack> hotbar = new java.util.ArrayList<>();
                for (int i = 0; i < 9; i++) {
                    if (!client.player.getInventory().getStack(i).isEmpty()
                            && (argument.isEmpty() || java.util.Arrays.asList(argument.split("\\s+")).contains(String.valueOf(i))))
                        hotbar.add(client.player.getInventory().getStack(i));
                }
                client.setScreen(new TooltipPreviewScreen(hotbar));
            }
            case "escape" -> {
                if (client.currentScreen != null) {
                    client.currentScreen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE, 0, 0);
                }
                DnDClasses.LOGGER.info("[DevScript] screen after escape: {}", client.currentScreen == null ? "none"
                        : client.currentScreen.getClass().getSimpleName());
            }
            case "racepicker" -> PickerFlow.setRacePickerAllowed(client, argument.equals("on"));
            case "racepick" -> racePick(argument.split("\\s+"), lineNumber);
            default -> DnDClasses.LOGGER.warn("[DevScript] {}: unknown step '{}'", lineNumber, line);
        }
    }

    private static void skill(String[] args, int lineNumber) {
        Identifier packet = switch (args[0]) {
            case "unlock" -> Progression.C2S_UNLOCK;
            case "equip" -> Progression.C2S_EQUIP;
            case "rankup" -> Progression.C2S_RANK_UP;
            case "bestiary" -> Progression.C2S_BESTIARY_UNLOCK;
            case "subclass" -> Progression.C2S_CHOOSE_SUBCLASS;
            default -> null;
        };
        if (packet == null || args.length < 2) {
            DnDClasses.LOGGER.warn("[DevScript] {}: skill needs unlock|equip|rankup|bestiary|subclass <id>", lineNumber);
            return;
        }
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(args[1]);
        ClientPlayNetworking.send(packet, buf);
    }

    private static void racePick(String[] args, int lineNumber) {
        mattonfire.dnd.classes.Race.DndRace race = mattonfire.dnd.classes.Race.DndRace.byId(args[0]);
        mattonfire.dnd.classes.Race.DragonAncestry ancestry = args.length > 1
                ? mattonfire.dnd.classes.Race.DragonAncestry.byId(args[1])
                : mattonfire.dnd.classes.Race.DragonAncestry.NONE;
        if (race == null || ancestry == null) {
            DnDClasses.LOGGER.warn("[DevScript] {}: racepick needs <race> [ember|frost|storm]", lineNumber);
            return;
        }
        PickerFlow.sendPick(race, ancestry);
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

    private static void holdKey(MinecraftClient client, String[] args, int lineNumber) {
        for (KeyBinding binding : client.options.allKeys) {
            if (binding.getTranslationKey().equals(args[0])) {
                binding.setPressed(args.length < 2 || args[1].equals("on"));
                return;
            }
        }
        DnDClasses.LOGGER.warn("[DevScript] {}: no key binding '{}'", lineNumber, args[0]);
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
