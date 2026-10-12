package mattonfire.dnd.dm;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.dm.encounter.Encounter;
import mattonfire.dnd.dm.encounter.Encounters;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * The DM panel's network side (key K, {@code DmPanelScreen}). The panel has no powers of its own: every button
 * sends an action that the server permission-checks ({@link DungeonMaster#canUse}, the same test as {@code /dm})
 * and then runs as the matching {@code /dm} command, as the player, so the commands' own checks, feedback and
 * log lines all apply. A refused packet is logged ({@code [DmPanel] refused ...}) and answered with a chat line.
 *
 * <pre>
 * C2S open                          ask for the panel; the server answers with a state packet (open = true)
 * C2S action &lt;name&gt; &lt;args...&gt;     spawn &lt;encounter&gt; [frozen] | clear | roll &lt;check&gt; &lt;dc&gt; [advantage|disadvantage]
 *                                   [secret] | narrate &lt;text&gt; | veil on|off | dm on|off | freeze on|off
 * S2C state                         open flag, DM mode, veiled, frozen count, encounters, live encounters, and the
 *                                   feedback of the last action
 * </pre>
 */
public final class DmPanelPackets {
    public static final Identifier C2S_OPEN = new Identifier(DnDClasses.MOD_ID, "dm_panel_open");
    public static final Identifier C2S_ACTION = new Identifier(DnDClasses.MOD_ID, "dm_panel_action");
    public static final Identifier S2C_STATE = new Identifier(DnDClasses.MOD_ID, "dm_panel_state");

    /** Longest narration the panel sends (the chat box's own limit). */
    public static final int MAX_TEXT = 256;
    /** Most string arguments one action packet may carry. */
    private static final int MAX_ARGS = 8;
    /** Feedback lines kept for the panel's status area. */
    private static final int MAX_FEEDBACK = 4;

    private DmPanelPackets() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(C2S_OPEN,
                (server, player, handler, buf, sender) -> server.execute(() -> open(player)));
        ServerPlayNetworking.registerGlobalReceiver(C2S_ACTION, (server, player, handler, buf, sender) -> {
            String action = buf.readString(32);
            int count = Math.min(buf.readVarInt(), MAX_ARGS);
            List<String> args = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                args.add(buf.readString(MAX_TEXT * 4));
            }
            server.execute(() -> action(player, action, args));
        });
    }

    private static boolean allowed(ServerPlayerEntity player, String what) {
        if (DungeonMaster.canUse(player.getCommandSource())) {
            return true;
        }
        DnDClasses.LOGGER.warn("[DmPanel] refused {} from {}: not allowed to use /dm", what, player.getEntityName());
        player.sendMessage(Text.literal("Only the Dungeon Master can use the DM panel.").formatted(Formatting.RED),
                false);
        return false;
    }

    private static void open(ServerPlayerEntity player) {
        if (allowed(player, "open")) {
            DnDClasses.LOGGER.info("[DmPanel] {} opened the panel", player.getEntityName());
            sendState(player, true, List.of());
        }
    }

    private static void action(ServerPlayerEntity player, String action, List<String> args) {
        if (!allowed(player, action + " " + String.join(" ", args))) {
            return;
        }
        String command = toCommand(action, args);
        if (command == null) {
            DnDClasses.LOGGER.warn("[DmPanel] bad action from {}: {} {}", player.getEntityName(), action, args);
            sendState(player, false, List.of(Text.literal("Bad panel action: " + action).formatted(Formatting.RED)));
            return;
        }
        DnDClasses.LOGGER.info("[DmPanel] {} runs /{}", player.getEntityName(), command);
        Capture capture = new Capture(player);
        MinecraftServer server = player.getServer();
        server.getCommandManager().executeWithPrefix(player.getCommandSource().withOutput(capture), command);
        sendState(player, false, capture.lines);
    }

    /** The {@code /dm} command for a panel action (without the slash), or null if the arguments are bad. */
    static String toCommand(String action, List<String> args) {
        switch (action) {
            case "spawn" -> {
                if (args.isEmpty()) {
                    return null;
                }
                Identifier id = Identifier.tryParse(args.get(0).contains(":") ? args.get(0)
                        : DnDClasses.MOD_ID + ":" + args.get(0));
                if (id == null || Encounters.get(id) == null) {
                    return null;
                }
                boolean frozen = args.size() > 1 && args.get(1).equals("frozen");
                return "dm encounter spawn " + id + (frozen ? " frozen" : "");
            }
            case "clear" -> {
                return "dm encounter clear all";
            }
            case "roll" -> {
                if (args.size() < 2 || DmRollCalls.Check.parse(args.get(0)) == null) {
                    return null;
                }
                int dc;
                try {
                    dc = Integer.parseInt(args.get(1).trim());
                } catch (NumberFormatException e) {
                    return null;
                }
                if (dc < 1 || dc > 40) {
                    return null;
                }
                StringBuilder command = new StringBuilder("dm roll @a ")
                        .append(args.get(0).toLowerCase(Locale.ROOT)).append(' ').append(dc);
                for (String option : args.subList(2, args.size())) {
                    if (!List.of("advantage", "disadvantage", "secret").contains(option)) {
                        return null;
                    }
                    command.append(' ').append(option);
                }
                return command.toString();
            }
            case "narrate" -> {
                String text = args.isEmpty() ? "" : clean(args.get(0));
                return text.isEmpty() ? null : "dm narrate " + text;
            }
            case "veil", "dm" -> {
                if (args.isEmpty() || !(args.get(0).equals("on") || args.get(0).equals("off"))) {
                    return null;
                }
                return action.equals("veil") ? "dm veil " + args.get(0) : "dm " + args.get(0);
            }
            case "freeze" -> {
                if (args.isEmpty()) {
                    return null;
                }
                return switch (args.get(0)) {
                    case "on" -> "dm freeze radius";
                    case "off" -> "dm unfreeze all";
                    default -> null;
                };
            }
            default -> {
                return null;
            }
        }
    }

    /** One line of plain text: no control characters or line breaks, trimmed, at most {@link #MAX_TEXT}. */
    static String clean(String text) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length() && out.length() < MAX_TEXT; i++) {
            char c = text.charAt(i);
            out.append(Character.isISOControl(c) || c == '§' ? ' ' : c);
        }
        return out.toString().trim();
    }

    /** Sends the panel what it shows: toggles, encounter files, live encounters and the last action's feedback. */
    public static void sendState(ServerPlayerEntity player, boolean open, List<Text> feedback) {
        MinecraftServer server = player.getServer();
        DungeonMaster state = DungeonMaster.get(server);
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(open);
        buf.writeBoolean(state.isDm(player.getUuid()));
        buf.writeBoolean(state.isVeiled(player.getUuid()));
        buf.writeVarInt(frozenCount(server));
        List<Encounter> encounters = new ArrayList<>(Encounters.all());
        encounters.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        buf.writeVarInt(encounters.size());
        for (Encounter encounter : encounters) {
            buf.writeIdentifier(encounter.id());
            buf.writeString(encounter.name());
            buf.writeString(encounter.difficulty() == null ? "" : encounter.difficulty());
            buf.writeString(encounter.sizeLabel());
        }
        List<DungeonMaster.EncounterRecord> live = state.getEncounters();
        buf.writeVarInt(live.size());
        for (DungeonMaster.EncounterRecord record : live) {
            buf.writeVarInt(record.number());
            buf.writeString(record.name());
        }
        int from = Math.max(0, feedback.size() - MAX_FEEDBACK);
        buf.writeVarInt(feedback.size() - from);
        for (Text line : feedback.subList(from, feedback.size())) {
            buf.writeText(line);
        }
        ServerPlayNetworking.send(player, S2C_STATE, buf);
    }

    private static int frozenCount(MinecraftServer server) {
        int count = 0;
        for (ServerWorld world : server.getWorlds()) {
            for (Entity entity : world.iterateEntities()) {
                if (entity.getCommandTags().contains(DmFreeze.TAG)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Passes command output on to the player as usual, and keeps a copy for the panel. {@code DmPanelFeedbackMixin}
     * unwraps it when feedback is broadcast to ops, so the player isn't sent their own "[Name: ...]" line.
     */
    public static final class Capture implements CommandOutput {
        private final ServerPlayerEntity player;
        private final List<Text> lines = new ArrayList<>();

        Capture(ServerPlayerEntity player) {
            this.player = player;
        }

        public ServerPlayerEntity player() {
            return this.player;
        }

        @Override
        public void sendMessage(Text message) {
            this.lines.add(message);
            this.player.sendMessage(message);
        }

        @Override
        public boolean shouldReceiveFeedback() {
            return this.player.shouldReceiveFeedback();
        }

        @Override
        public boolean shouldTrackOutput() {
            return this.player.shouldTrackOutput();
        }

        @Override
        public boolean shouldBroadcastConsoleToOps() {
            return this.player.shouldBroadcastConsoleToOps();
        }
    }
}
