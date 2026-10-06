package mattonfire.dnd.classes.Client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import mattonfire.dnd.classes.DnDClasses;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.ScreenshotRecorder;

/**
 * Dev-only automation, so the client can be driven without anyone at the keyboard.
 * <p>
 * Auto-join: with {@code -Ddnd.autojoin=<world folder>} (set by build.gradle's client run from
 * {@code -PdevWorld}, default "New World") the client opens that singleplayer world as soon as the
 * title screen appears. 1.19.4 has no {@code --quickPlaySingleplayer}; that only exists from 1.20.
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
 * <li>{@code quit} closes the game cleanly</li>
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
    }

    private static void registerAutoJoin() {
        String world = System.getProperty("dnd.autojoin");
        if (world == null || world.isBlank()) {
            return;
        }
        boolean[] tried = {false};
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (tried[0] || !(client.currentScreen instanceof TitleScreen)) {
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
            case "quit" -> client.scheduleStop();
            default -> DnDClasses.LOGGER.warn("[DevScript] {}: unknown step '{}'", lineNumber, line);
        }
    }
}
