package mattonfire.dnd.classes.Client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Config.SaveRollsMode;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.mixin.InGameHudAccessor;
import mattonfire.dnd.classes.mixin.MerchantScreenAccessor;
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
 *     for {@code step2}. Also a barrier after a long command: {@code /say done} right after it only arrives
 *     once the server is free again</li>
 * <li>{@code waitserver <ticks>} pauses until the integrated server has run that many more ticks. {@code wait}
 *     counts client ticks, which keep going while the server is busy (e.g. a long {@code /dndhome ...survey});
 *     {@code waitserver 2} after such a command waits for it to finish. A client joined to another game
 *     ({@code -PdevServer}) has no integrated server, so it falls back to {@code wait}</li>
 * <li>{@code screenshot <name>} saves {@code run/screenshots/<name>.png}</li>
 * <li>{@code hitboxes on|off} toggles hitbox rendering (F3+B)</li>
 * <li>{@code serverhitboxes on|off} also draws the integrated server's dragon part shapes (red);
 *     {@code serverhitboxes measure} logs how far they are from the client's</li>
 * <li>{@code hud on|off} toggles the HUD (F1)</li>
 * <li>{@code debug on|off} shows or hides the debug screen (F3)</li>
 * <li>{@code guiscale <n>} sets the GUI scale (0 = auto) and logs the scale in use. The window size caps it
 *     (854x480 allows 2 at most); start the client with {@code -PdevWidth=1920 -PdevHeight=1080} for 3 or 4</li>
 * <li>{@code actionbar} logs the action bar text now on screen ({@code actionbar: none} if there's none).
 *     Every new action bar message is also logged as it arrives ({@code [DevScript] actionbar: ...})</li>
 * <li>{@code clearchat} clears the chat (F3+D), so it doesn't cover the HUD in a screenshot</li>
 * <li>{@code closescreen} closes any open screen (e.g. the class picker shown on join); a container screen is
 *     closed on the server too, like pressing Esc</li>
 * <li>{@code respawn} respawns the player if it's dead (a world saved mid-death loads dead)</li>
 * <li>{@code quit} closes the game cleanly</li>
 * </ul>
 * Input steps act inside the game, so they work while the window is hidden or unfocused:
 * <ul>
 * <li>{@code look <yaw> <pitch>} turns the player (yaw 0 = south)</li>
 * <li>{@code use} / {@code attack} press the use (right) / attack (left) button once, at the crosshair, and log
 *     what the crosshair is on ({@code use -> entity minecraft:spider}, {@code -> block ...} or {@code -> miss}).
 *     {@code attack} ignores the attack cooldown a missed click or an open screen leaves</li>
 * <li>{@code hotbar <0-8>} selects a hotbar slot</li>
 * <li>{@code sneak on|off} holds or releases the sneak key</li>
 * <li>{@code walk on|off} (or {@code forward on|off}) holds or releases the walk forward key, e.g. to crawl
 *     into a gap or to check movement is locked</li>
 * <li>{@code fov <30-110>} sets the field of view, e.g. 30 to zoom in on a third-person close-up</li>
 * <li>{@code sizes} logs every player's pose, hitbox (width x height), eye height, position and body race as
 * this client sees them, and in singleplayer as the integrated server sees them</li>
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
 * <li>{@code slots} logs every non-empty slot of the open screen (or the inventory); on a merchant screen it
 *     also logs each trade with the price this player is shown, after every modifier ({@code trade <i>: <price>
 *     [+ <second>] -> <result> (base <count>, special <n>)})</li>
 * <li>{@code button <id>} clicks a screen button such as an enchanting option (0-2); in an NPC dialogue it
 *     picks reply {@code <id>} (0 = the first)</li>
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
 *     the server's checks apply; {@code skill asi <ability>} sends a Character tab "+" click (e.g. {@code skill asi
 *     dex})</li>
 * <li>{@code tooltips [hotbar slots...]} opens a screen showing the tooltips of the hotbar items (all, or the
 *     slots listed, e.g. {@code tooltips 0 1 4}) side by side and logs their lines; close it with {@code closescreen}</li>
 * <li>{@code escape} sends the open screen an Escape key press, e.g. to check a picker ignores it</li>
 * <li>{@code key <name>} presses a keyboard key, by GLFW name ({@code j}, {@code tab}, {@code escape},
 *     {@code enter}, {@code f3}, ...): sent to the open screen (e.g. J or Tab in the Journal), or with no screen
 *     open, to the key binding it's bound to (like {@code press}). Logs the screen open afterwards</li>
 * <li>{@code scroll <amount> [dx dy]} scrolls the mouse wheel over the open screen (positive = up), at GUI
 *     coordinates measured from its centre (default: the centre), e.g. the Character tab's skill list</li>
 * <li>{@code trade <index>} picks a trade (from 0, as {@code slots} lists them) on an open merchant screen, like
 *     clicking it on the left; the result slot (2) then holds it, so {@code slot 2 quick_move} buys it</li>
 * <li>{@code target <selector>} logs each matching mob's attack target, as the integrated server sees it
 *     ({@code target <type> <name> at x y z -> <target>|none}), e.g. {@code target @e[type=dndclasses:orc_archer]}</li>
 * <li>{@code racepicker on|off}: while a script runs the race picker stays shut (the dev-world player has a class
 *     but no race, so it would block every script). {@code on} lets it open, straight away if the player has no
 *     race</li>
 * <li>{@code racepick <race> [ancestry]} sends what clicking a race (or a Dragonborn ancestry) in the picker
 *     would, e.g. {@code racepick elf}, {@code racepick dragonborn frost}; the server's one-pick check applies</li>
 * <li>{@code saverolls full|compact|off|config} overrides the {@code saveRolls} client option for the rest of the
 *     run ({@code config} goes back to the config file), to check each save-lane mode in one run</li>
 * </ul>
 * Runs started with {@code -PdevScript} get a fixed player name unless {@code -PdevUser} sets one: {@code Dev}, or
 * {@code DevGuest} for {@code -PdevServer}, plus {@code _<run dir>} outside {@code run/} (e.g. {@code DevGuest_run_2}),
 * so per-player state (quests, reputation, ...) carries over between runs.
 * <p>
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
    /** Server tick a {@code waitserver} step waits for, or -1. */
    private int waitServerUntil = -1;
    /** The last action bar message logged, so each one is logged once. */
    private String lastActionBar;

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
        logActionBarChange(client);
        if (this.waitTicks > 0) {
            this.waitTicks--;
            return;
        }
        if (this.waitServerUntil >= 0) {
            if (client.getServer() != null && client.getServer().getTicks() < this.waitServerUntil) {
                return;
            }
            DnDClasses.LOGGER.info("[DevScript] waitserver done at server tick {}",
                    client.getServer() == null ? "?" : client.getServer().getTicks());
            this.waitServerUntil = -1;
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
            case "waitserver" -> {
                if (client.getServer() != null) {
                    this.waitServerUntil = client.getServer().getTicks() + Integer.parseInt(argument);
                } else {
                    DnDClasses.LOGGER.warn("[DevScript] {}: no integrated server; waitserver waits client ticks", lineNumber);
                    this.waitTicks = Integer.parseInt(argument);
                }
            }
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
            case "debug" -> client.options.debugEnabled = argument.equals("on");
            case "guiscale" -> {
                client.options.getGuiScale().setValue(Integer.parseInt(argument));
                client.onResolutionChanged();
                DnDClasses.LOGGER.info("[DevScript] gui scale {} (window {}x{})", client.getWindow().getScaleFactor(),
                        client.getWindow().getFramebufferWidth(), client.getWindow().getFramebufferHeight());
            }
            case "actionbar" -> {
                InGameHudAccessor hud = (InGameHudAccessor) client.inGameHud;
                DnDClasses.LOGGER.info("[DevScript] actionbar: {}", hud.getOverlayMessage() == null
                        || hud.getOverlayRemaining() <= 0 ? "none" : hud.getOverlayMessage().getString());
            }
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
            case "use" -> {
                logCrosshair(client, "use");
                ((MinecraftClientInvoker) client).invokeDoItemUse();
            }
            case "attack" -> {
                logCrosshair(client, "attack");
                // A missed click or an open screen leaves a cooldown that would swallow this click
                client.attackCooldown = 0;
                ((MinecraftClientInvoker) client).invokeDoAttack();
            }
            case "sneak" -> client.options.sneakKey.setPressed(argument.equals("on"));
            case "walk", "forward" -> client.options.forwardKey.setPressed(argument.equals("on"));
            case "sizes" -> logSizes(client);
            case "fov" -> client.options.getFov().setValue(Integer.parseInt(argument));
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
            case "button" -> {
                // NPC dialogue: the reply with this index (0 = first); otherwise a container screen's button
                if (client.currentScreen instanceof mattonfire.dnd.classes.Client.Hud.DialogueScreen dialogue) {
                    dialogue.choose(Integer.parseInt(argument));
                } else {
                    client.interactionManager.clickButton(client.player.currentScreenHandler.syncId,
                            Integer.parseInt(argument));
                }
            }
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
            case "key" -> key(client, argument, lineNumber);
            case "scroll" -> {
                String[] args = argument.split("\\s+");
                if (client.currentScreen != null) {
                    double x = client.currentScreen.width / 2.0 + (args.length > 2 ? Double.parseDouble(args[1]) : 0);
                    double y = client.currentScreen.height / 2.0 + (args.length > 2 ? Double.parseDouble(args[2]) : 0);
                    client.currentScreen.mouseScrolled(x, y, Double.parseDouble(args[0]));
                } else {
                    DnDClasses.LOGGER.warn("[DevScript] {}: scroll needs an open screen", lineNumber);
                }
            }
            case "trade" -> {
                int index = Integer.parseInt(argument);
                if (client.player.currentScreenHandler instanceof net.minecraft.screen.MerchantScreenHandler merchant
                        && index >= 0 && index < merchant.getRecipes().size()) {
                    if (client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.MerchantScreen screen) {
                        ((MerchantScreenAccessor) screen).setSelectedIndex(index);
                    }
                    merchant.setRecipeIndex(index);
                    merchant.switchTo(index);
                    client.player.networkHandler.sendPacket(
                            new net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket(index));
                } else {
                    DnDClasses.LOGGER.warn("[DevScript] {}: trade needs an open merchant screen with a trade {}",
                            lineNumber, index);
                }
            }
            case "target" -> logTargets(client, argument, lineNumber);
            case "racepicker" -> PickerFlow.setRacePickerAllowed(client, argument.equals("on"));
            case "racepick" -> racePick(argument.split("\\s+"), lineNumber);
            case "saverolls" -> SaveRollsMode.setOverride(SaveRollsMode.byId(argument)); // else: the config file
            default -> DnDClasses.LOGGER.warn("[DevScript] {}: unknown step '{}'", lineNumber, line);
        }
    }

    private void logActionBarChange(MinecraftClient client) {
        InGameHudAccessor hud = (InGameHudAccessor) client.inGameHud;
        String message = hud.getOverlayRemaining() > 0 && hud.getOverlayMessage() != null
                ? hud.getOverlayMessage().getString() : null;
        // Logged once while it shows (some HUDs resend the same text every tick); again once it has faded
        if (message != null && !message.equals(this.lastActionBar)) {
            DnDClasses.LOGGER.info("[DevScript] actionbar: {}", message);
        }
        this.lastActionBar = message;
    }

    private static void logCrosshair(MinecraftClient client, String step) {
        net.minecraft.util.hit.HitResult hit = client.crosshairTarget;
        String what;
        if (hit instanceof net.minecraft.util.hit.EntityHitResult entityHit) {
            what = "entity " + net.minecraft.registry.Registries.ENTITY_TYPE.getId(entityHit.getEntity().getType());
        } else if (hit instanceof net.minecraft.util.hit.BlockHitResult blockHit
                && hit.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
            what = "block " + net.minecraft.registry.Registries.BLOCK.getId(
                    client.world.getBlockState(blockHit.getBlockPos()).getBlock()) + " at "
                    + blockHit.getBlockPos().toShortString();
        } else {
            what = "miss";
        }
        DnDClasses.LOGGER.info("[DevScript] {} -> {}", step, what);
    }

    private static void key(MinecraftClient client, String name, int lineNumber) {
        net.minecraft.client.util.InputUtil.Key key;
        try {
            key = net.minecraft.client.util.InputUtil.fromTranslationKey("key.keyboard." + name.toLowerCase());
        } catch (IllegalArgumentException e) {
            key = net.minecraft.client.util.InputUtil.UNKNOWN_KEY;
        }
        if (key == net.minecraft.client.util.InputUtil.UNKNOWN_KEY) {
            DnDClasses.LOGGER.warn("[DevScript] {}: unknown key '{}'", lineNumber, name);
            return;
        }
        if (client.currentScreen != null) {
            client.currentScreen.keyPressed(key.getCode(), 0, 0);
            client.currentScreen.keyReleased(key.getCode(), 0, 0);
        } else {
            KeyBinding.onKeyPressed(key);
        }
        DnDClasses.LOGGER.info("[DevScript] screen after key {}: {}", name, client.currentScreen == null ? "none"
                : client.currentScreen.getClass().getSimpleName());
    }

    /** Logs the attack target of each mob the selector matches, read on the integrated server's thread. */
    private static void logTargets(MinecraftClient client, String selector, int lineNumber) {
        net.minecraft.server.MinecraftServer server = client.getServer();
        if (server == null) {
            DnDClasses.LOGGER.warn("[DevScript] {}: target needs the integrated server (singleplayer or the LAN host)",
                    lineNumber);
            return;
        }
        java.util.UUID self = client.player.getUuid();
        server.execute(() -> {
            net.minecraft.server.network.ServerPlayerEntity player = server.getPlayerManager().getPlayer(self);
            if (player == null) {
                return;
            }
            try {
                net.minecraft.command.EntitySelector parsed = net.minecraft.command.argument.EntityArgumentType.entities()
                        .parse(new com.mojang.brigadier.StringReader(selector));
                java.util.List<? extends net.minecraft.entity.Entity> found = parsed.getEntities(player.getCommandSource());
                if (found.isEmpty()) {
                    DnDClasses.LOGGER.info("[DevScript] target {}: no entities", selector);
                }
                for (net.minecraft.entity.Entity entity : found) {
                    net.minecraft.entity.Entity target = null;
                    if (entity instanceof net.minecraft.entity.mob.MobEntity mob) {
                        target = mob.getTarget();
                        // Brain-driven mobs (piglins, ...) keep their target in a memory instead
                        if (target == null && mob.getBrain().hasMemoryModule(
                                net.minecraft.entity.ai.brain.MemoryModuleType.ATTACK_TARGET)) {
                            target = mob.getBrain().getOptionalMemory(
                                    net.minecraft.entity.ai.brain.MemoryModuleType.ATTACK_TARGET).orElse(null);
                        }
                    }
                    DnDClasses.LOGGER.info("[DevScript] target {} {} at {} -> {}",
                            net.minecraft.registry.Registries.ENTITY_TYPE.getId(entity.getType()), entity.getName().getString(),
                            entity.getBlockPos().toShortString(), target == null ? "none"
                                    : net.minecraft.registry.Registries.ENTITY_TYPE.getId(target.getType()) + " "
                                    + target.getName().getString() + " at " + target.getBlockPos().toShortString());
                }
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                DnDClasses.LOGGER.warn("[DevScript] {}: bad selector '{}': {}", lineNumber, selector, e.getMessage());
            }
        });
    }

    private static void skill(String[] args, int lineNumber) {
        Identifier packet = switch (args[0]) {
            case "unlock" -> Progression.C2S_UNLOCK;
            case "equip" -> Progression.C2S_EQUIP;
            case "rankup" -> Progression.C2S_RANK_UP;
            case "bestiary" -> Progression.C2S_BESTIARY_UNLOCK;
            case "subclass" -> Progression.C2S_CHOOSE_SUBCLASS;
            case "asi" -> mattonfire.dnd.classes.Abilities.AbilityIncreases.C2S_ABILITY_INCREASE;
            default -> null;
        };
        if (packet == null || args.length < 2) {
            DnDClasses.LOGGER.warn("[DevScript] {}: skill needs unlock|equip|rankup|bestiary|subclass|asi <id>",
                    lineNumber);
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

    private static void logSizes(MinecraftClient client) {
        for (net.minecraft.entity.player.PlayerEntity p : client.world.getPlayers()) {
            logSize("client", p);
        }
        if (client.getServer() != null) {
            for (net.minecraft.entity.player.PlayerEntity p : client.getServer().getPlayerManager().getPlayerList()) {
                logSize("server", p);
            }
        }
    }

    private static void logSize(String side, net.minecraft.entity.player.PlayerEntity p) {
        DnDClasses.LOGGER.info("[DevScript] size {} {} race={} pose={} hitbox={}x{} eye={} pos={} {} {}", side,
                p.getEntityName(), ((mattonfire.dnd.classes.PlayerEntityExt) p).getBodyRace().id(), p.getPose(),
                String.format("%.3f", p.getWidth()), String.format("%.3f", p.getHeight()),
                String.format("%.3f", p.getStandingEyeHeight()), String.format("%.2f", p.getX()),
                String.format("%.2f", p.getY()), String.format("%.2f", p.getZ()));
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
        // A merchant screen also lists its trades, with the prices this player is shown.
        if (handler instanceof net.minecraft.screen.MerchantScreenHandler merchant) {
            net.minecraft.village.TradeOfferList offers = merchant.getRecipes();
            for (int i = 0; i < offers.size(); i++) {
                net.minecraft.village.TradeOffer offer = offers.get(i);
                DnDClasses.LOGGER.info("[DevScript] trade {}: {}{} -> {} (base {}, special {})", i,
                        describe(offer.getAdjustedFirstBuyItem()),
                        offer.getSecondBuyItem().isEmpty() ? "" : " + " + describe(offer.getSecondBuyItem()),
                        describe(offer.getSellItem()), offer.getOriginalFirstBuyItem().getCount(), offer.getSpecialPrice());
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
