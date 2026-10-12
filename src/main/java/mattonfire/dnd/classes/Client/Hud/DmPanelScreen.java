package mattonfire.dnd.classes.Client.Hud;

import java.util.ArrayList;
import java.util.List;

import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.dm.DmPanelPackets;
import mattonfire.dnd.dm.DmRollCalls;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * The DM panel (key K): the {@code /dm} toolkit as buttons, so the DM doesn't type commands mid-session.
 * <ul>
 * <li>Toggles: DM mode, the veil, and freeze all (freezes everything within 24 blocks / unfreezes everything).</li>
 * <li>Encounters: the encounter files; Spawn puts the selected one at the block under the crosshair, optionally
 * frozen. Clear all removes every live encounter.</li>
 * <li>Roll call: check (flat, a save or a skill), DC, normal/advantage/disadvantage and secret, rolled by every
 * player not in DM mode ({@code /dm roll @a ...}).</li>
 * <li>Narrate: a line of narration to everyone ({@code /dm narrate}).</li>
 * </ul>
 * The screen only sends requests ({@link DmPanelPackets}); the server checks the player may use {@code /dm} and runs
 * the matching command, then sends back the new state and the command's feedback, shown at the bottom. Pressing K
 * asks the server first, so it opens only for DMs. Doesn't pause the game.
 */
public class DmPanelScreen extends Screen {
    public static final String KEY = "key.dnd-classes.dm-panel";

    private static final int PANEL_MAX_WIDTH = 400;
    private static final int PANEL_MAX_HEIGHT = 232;
    private static final int PAD = 8;
    private static final int LINE = 10;
    private static final int ROW = 10;
    private static final int LIST_WIDTH = 160;
    private static final int BUTTON = 16;
    private static final int BACKGROUND = 0xE0141018;
    private static final int BORDER = 0xFFB08A3E;
    private static final int DIVIDER = 0x60B08A3E;
    private static final int SELECTED = 0x50B08A3E;
    private static final int HOVER = 0x30FFFFFF;
    private static final int GOLD = 0xFFE0B050;

    /** One encounter file, as the server listed it. */
    public record EncounterInfo(Identifier id, String name, String difficulty, String size) {
    }

    /** What the server last told us. */
    public record State(boolean dm, boolean veiled, int frozen, List<EncounterInfo> encounters, List<String> live,
            List<Text> feedback) {
    }

    private static State state = new State(false, false, 0, List.of(), List.of(), List.of());

    // Kept between openings.
    private static Identifier selectedEncounter;
    private static boolean spawnFrozen;
    private static String check = "flat";
    private static String dc = "10";
    private static Mode mode = Mode.NORMAL;
    private static boolean secret;
    private static String narration = "";

    private enum Mode {
        NORMAL("Normal", null), ADVANTAGE("Advantage", "advantage"), DISADVANTAGE("Disadvantage", "disadvantage");

        final String label;
        final String option;

        Mode(String label, String option) {
            this.label = label;
            this.option = option;
        }
    }

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int listTop;
    private int listBottom;
    private int listScroll;
    private int rightX;
    private int rightWidth;
    private int feedbackTop;
    private boolean waiting;

    private ButtonWidget dmButton;
    private ButtonWidget veilButton;
    private ButtonWidget freezeButton;
    private ButtonWidget spawnButton;
    private CheckboxWidget frozenBox;
    private CheckboxWidget secretBox;
    private TextFieldWidget dcField;
    private TextFieldWidget narrateField;

    public DmPanelScreen() {
        super(Text.translatable("dm_panel.dndclasses.title"));
    }

    /** Networking: the server's answer to K, and refreshes after each action. */
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(DmPanelPackets.S2C_STATE, (client, handler, buf, sender) -> {
            boolean open = buf.readBoolean();
            boolean dm = buf.readBoolean();
            boolean veiled = buf.readBoolean();
            int frozen = buf.readVarInt();
            List<EncounterInfo> encounters = new ArrayList<>();
            for (int i = buf.readVarInt(); i > 0; i--) {
                encounters.add(new EncounterInfo(buf.readIdentifier(), buf.readString(), buf.readString(),
                        buf.readString()));
            }
            List<String> live = new ArrayList<>();
            for (int i = buf.readVarInt(); i > 0; i--) {
                live.add("#" + buf.readVarInt() + " " + buf.readString());
            }
            List<Text> feedback = new ArrayList<>();
            for (int i = buf.readVarInt(); i > 0; i--) {
                feedback.add(buf.readText());
            }
            State received = new State(dm, veiled, frozen, encounters, live, feedback);
            client.execute(() -> {
                state = received;
                if (client.currentScreen instanceof DmPanelScreen panel) {
                    panel.waiting = false;
                    panel.refresh();
                } else if (open && client.currentScreen == null && client.player != null) {
                    client.setScreen(new DmPanelScreen());
                }
            });
        });
    }

    /** K pressed: ask the server; it opens the panel only for players who may use {@code /dm}. */
    public static void request() {
        ClientPlayNetworking.send(DmPanelPackets.C2S_OPEN, PacketByteBufs.empty());
    }

    /** Sends one panel action; the server checks it and runs the matching {@code /dm} command. */
    public static void send(String action, List<String> args) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(action);
        buf.writeVarInt(args.size());
        for (String arg : args) {
            buf.writeString(arg);
        }
        ClientPlayNetworking.send(DmPanelPackets.C2S_ACTION, buf);
    }

    private void act(String action, List<String> args) {
        this.waiting = true;
        send(action, args);
    }

    /** Every check the roll caller offers: flat, the six saves, then the skills. */
    private static List<String> checks() {
        List<String> ids = new ArrayList<>();
        ids.add("flat");
        for (Ability ability : Ability.values()) {
            ids.add(ability.id());
        }
        for (Skill skill : Skill.values()) {
            ids.add(skill.id());
        }
        return ids;
    }

    private static Text checkLabel(String id) {
        DmRollCalls.Check parsed = DmRollCalls.Check.parse(id);
        return parsed == null ? Text.literal(id) : parsed.label();
    }

    @Override
    protected void init() {
        this.panelWidth = Math.min(PANEL_MAX_WIDTH, this.width - 8);
        this.panelHeight = Math.min(PANEL_MAX_HEIGHT, this.height - 8);
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;
        int inner = this.panelWidth - 2 * PAD;

        // Toggles
        int toggleY = this.panelY + PAD + LINE + 4;
        int toggleWidth = (inner - 8) / 3;
        int x = this.panelX + PAD;
        this.dmButton = this.addDrawableChild(ButtonWidget.builder(Text.empty(),
                b -> this.act("dm", List.of(state.dm() ? "off" : "on")))
                .dimensions(x, toggleY, toggleWidth, BUTTON).build());
        this.veilButton = this.addDrawableChild(ButtonWidget.builder(Text.empty(),
                b -> this.act("veil", List.of(state.veiled() ? "off" : "on")))
                .dimensions(x + toggleWidth + 4, toggleY, toggleWidth, BUTTON).build());
        this.freezeButton = this.addDrawableChild(ButtonWidget.builder(Text.empty(),
                b -> this.act("freeze", List.of(state.frozen() > 0 ? "off" : "on")))
                .dimensions(x + 2 * (toggleWidth + 4), toggleY, inner - 2 * (toggleWidth + 4), BUTTON).build());

        int contentTop = toggleY + BUTTON + 6 + LINE + 2;
        this.feedbackTop = this.panelY + this.panelHeight - PAD - 2 * LINE;
        int contentBottom = this.feedbackTop - 4;

        // Encounters (left)
        int buttonsY = contentBottom - BUTTON;
        this.spawnButton = this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("dm_panel.dndclasses.spawn"), b -> this.spawn())
                .dimensions(x, buttonsY, 106, BUTTON).build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("dm_panel.dndclasses.clear"),
                b -> this.act("clear", List.of()))
                .dimensions(x + 110, buttonsY, LIST_WIDTH - 110, BUTTON).build());
        this.frozenBox = this.addDrawableChild(new CheckboxWidget(x, buttonsY - 22, LIST_WIDTH, 20,
                Text.translatable("dm_panel.dndclasses.spawn_frozen"), spawnFrozen));
        this.listTop = contentTop;
        this.listBottom = buttonsY - 24;

        // Roll call (right)
        this.rightX = x + LIST_WIDTH + 2 * PAD;
        this.rightWidth = this.panelX + this.panelWidth - PAD - this.rightX;
        int y = contentTop;
        this.addDrawableChild(CyclingButtonWidget.<String>builder(DmPanelScreen::checkLabel)
                .values(checks()).initially(checks().contains(check) ? check : "flat")
                .build(this.rightX, y, this.rightWidth, BUTTON, Text.translatable("dm_panel.dndclasses.check"),
                        (button, value) -> check = value));
        y += BUTTON + 4;
        int dcLabel = this.textRenderer.getWidth("DC") + 4;
        this.dcField = new TextFieldWidget(this.textRenderer, this.rightX + dcLabel + 1, y + 1, 28, BUTTON - 2,
                Text.literal("DC"));
        this.dcField.setTextPredicate(text -> text.matches("\\d{0,2}"));
        this.dcField.setText(dc);
        this.dcField.setChangedListener(text -> dc = text);
        this.addDrawableChild(this.dcField);
        int modeX = this.rightX + dcLabel + 34;
        this.addDrawableChild(CyclingButtonWidget.<Mode>builder(m -> Text.literal(m.label))
                .values(Mode.values()).initially(mode)
                .build(modeX, y, this.rightX + this.rightWidth - modeX, BUTTON, Text.literal("Mode"),
                        (button, value) -> mode = value));
        y += BUTTON + 4;
        this.secretBox = this.addDrawableChild(new CheckboxWidget(this.rightX, y, 80, 20,
                Text.translatable("dm_panel.dndclasses.secret"), secret));
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("dm_panel.dndclasses.call_roll"),
                b -> this.callRoll())
                .dimensions(this.rightX + this.rightWidth - 90, y + 2, 90, BUTTON).build());
        y += 20 + 8 + LINE + 2;

        // Narrate (right, below)
        this.narrateField = new TextFieldWidget(this.textRenderer, this.rightX + 1, y + 1, this.rightWidth - 2,
                BUTTON - 2, Text.translatable("dm_panel.dndclasses.narrate"));
        this.narrateField.setMaxLength(DmPanelPackets.MAX_TEXT);
        this.narrateField.setText(narration);
        this.narrateField.setChangedListener(text -> narration = text);
        this.narrateField.setPlaceholder(Text.translatable("dm_panel.dndclasses.narrate_hint")
                .formatted(Formatting.DARK_GRAY));
        this.addDrawableChild(this.narrateField);
        y += BUTTON + 4;
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("dm_panel.dndclasses.narrate"),
                b -> this.narrate())
                .dimensions(this.rightX + this.rightWidth - 90, y, 90, BUTTON).build());

        this.refresh();
    }

    /** Updates the toggles and the encounter selection after a state packet, keeping what's typed. */
    private void refresh() {
        this.dmButton.setMessage(onOff("dm_panel.dndclasses.dm_mode", state.dm()));
        this.veilButton.setMessage(onOff("dm_panel.dndclasses.veil", state.veiled()));
        this.freezeButton.setMessage(state.frozen() > 0
                ? Text.translatable("dm_panel.dndclasses.unfreeze_all")
                : Text.translatable("dm_panel.dndclasses.freeze_all"));
        if (selectedEncounter == null || state.encounters().stream().noneMatch(e -> e.id().equals(selectedEncounter))) {
            selectedEncounter = state.encounters().isEmpty() ? null : state.encounters().get(0).id();
        }
        this.spawnButton.active = selectedEncounter != null;
        this.listScroll = Math.max(0, Math.min(this.listScroll, this.maxScroll()));
    }

    private static Text onOff(String key, boolean on) {
        return Text.translatable(key).append(": ").append(on
                ? Text.translatable("options.on").formatted(Formatting.GREEN)
                : Text.translatable("options.off").formatted(Formatting.GRAY));
    }

    private void spawn() {
        if (selectedEncounter == null) {
            return;
        }
        spawnFrozen = this.frozenBox.isChecked();
        this.act("spawn", spawnFrozen ? List.of(selectedEncounter.toString(), "frozen")
                : List.of(selectedEncounter.toString()));
    }

    private void callRoll() {
        secret = this.secretBox.isChecked();
        List<String> args = new ArrayList<>(List.of(check, dc.isEmpty() ? "10" : dc));
        if (mode.option != null) {
            args.add(mode.option);
        }
        if (secret) {
            args.add("secret");
        }
        this.act("roll", args);
    }

    private void narrate() {
        String text = this.narrateField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        this.act("narrate", List.of(text));
        this.narrateField.setText("");
    }

    // ---- encounter list ----

    private int visibleRows() {
        return Math.max(1, (this.listBottom - this.listTop) / ROW);
    }

    private int maxScroll() {
        return Math.max(0, state.encounters().size() - this.visibleRows());
    }

    private int rowAt(double mouseX, double mouseY) {
        int x = this.panelX + PAD;
        if (mouseX < x || mouseX >= x + LIST_WIDTH || mouseY < this.listTop || mouseY >= this.listBottom) {
            return -1;
        }
        int index = (int) (mouseY - this.listTop) / ROW + this.listScroll;
        return index < state.encounters().size() && index - this.listScroll < this.visibleRows() ? index : -1;
    }

    /** Selects an encounter by id (also used by DevScript); false if the server didn't list it. */
    public boolean select(String id) {
        for (EncounterInfo encounter : state.encounters()) {
            if (encounter.id().toString().equals(id) || encounter.id().getPath().equals(id)) {
                selectedEncounter = encounter.id();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int row = this.rowAt(mouseX, mouseY);
        if (row >= 0 && button == 0) {
            selectedEncounter = state.encounters().get(row).id();
            this.spawnButton.active = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mouseX < this.panelX + PAD + LIST_WIDTH) {
            this.listScroll = Math.max(0, Math.min(this.maxScroll(), this.listScroll - (int) Math.signum(amount)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter in the narrate box narrates
        if ((keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER)
                && this.getFocused() == this.narrateField) {
            this.narrate();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        this.dcField.tick();
        this.narrateField.tick();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ---- rendering ----

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        int x = this.panelX;
        int y = this.panelY;
        int right = x + this.panelWidth;
        int bottom = y + this.panelHeight;
        fill(matrices, x, y, right, bottom, BACKGROUND);
        fill(matrices, x, y, right, y + 1, BORDER);
        fill(matrices, x, bottom - 1, right, bottom, BORDER);
        fill(matrices, x, y, x + 1, bottom, BORDER);
        fill(matrices, right - 1, y, right, bottom, BORDER);

        drawTextWithShadow(matrices, this.textRenderer, this.title, x + PAD, y + PAD, GOLD);
        Text status = Text.translatable("dm_panel.dndclasses.status", state.live().size(), state.frozen());
        drawTextWithShadow(matrices, this.textRenderer, status,
                right - PAD - this.textRenderer.getWidth(status), y + PAD, 0xFFA0A0A0);

        int headerY = this.listTop - LINE - 2;
        int leftX = x + PAD;
        fill(matrices, leftX, headerY - 4, right - PAD, headerY - 3, DIVIDER);
        drawTextWithShadow(matrices, this.textRenderer, Text.translatable("dm_panel.dndclasses.encounters"), leftX,
                headerY, GOLD);
        drawTextWithShadow(matrices, this.textRenderer, Text.translatable("dm_panel.dndclasses.roll_call"),
                this.rightX, headerY, GOLD);
        fill(matrices, this.rightX - PAD, headerY - 3, this.rightX - PAD + 1, this.feedbackTop - 4, DIVIDER);

        // Encounter rows
        List<EncounterInfo> encounters = state.encounters();
        if (encounters.isEmpty()) {
            drawTextWithShadow(matrices, this.textRenderer, Text.translatable("dm_panel.dndclasses.no_encounters"),
                    leftX, this.listTop, 0xFF808080);
        }
        int hovered = this.rowAt(mouseX, mouseY);
        for (int i = 0; i < this.visibleRows() && i + this.listScroll < encounters.size(); i++) {
            EncounterInfo encounter = encounters.get(i + this.listScroll);
            int rowY = this.listTop + i * ROW;
            if (encounter.id().equals(selectedEncounter)) {
                fill(matrices, leftX, rowY, leftX + LIST_WIDTH, rowY + ROW, SELECTED);
            } else if (i + this.listScroll == hovered) {
                fill(matrices, leftX, rowY, leftX + LIST_WIDTH, rowY + ROW, HOVER);
            }
            String size = encounter.size();
            int sizeWidth = this.textRenderer.getWidth(size);
            String name = this.textRenderer.trimToWidth(encounter.name(), LIST_WIDTH - sizeWidth - 8);
            this.textRenderer.drawWithShadow(matrices, name, leftX + 2, rowY + 1,
                    difficultyColor(encounter.difficulty()));
            this.textRenderer.drawWithShadow(matrices, size, leftX + LIST_WIDTH - 2 - sizeWidth, rowY + 1, 0xFF909090);
        }
        if (this.maxScroll() > 0) {
            String more = (this.listScroll + 1) + "-" + Math.min(encounters.size(), this.listScroll
                    + this.visibleRows()) + "/" + encounters.size();
            this.textRenderer.drawWithShadow(matrices, more, leftX + LIST_WIDTH - this.textRenderer.getWidth(more),
                    headerY, 0xFF808080);
        }

        // DC label and the Narrate header
        drawTextWithShadow(matrices, this.textRenderer, Text.literal("DC"), this.rightX,
                this.dcField.getY() + 3, 0xFFE0E0E0);
        drawTextWithShadow(matrices, this.textRenderer, Text.translatable("dm_panel.dndclasses.narrate"),
                this.rightX, this.narrateField.getY() - LINE - 2, GOLD);

        // Feedback from the last action
        fill(matrices, leftX, this.feedbackTop - 3, right - PAD, this.feedbackTop - 2, DIVIDER);
        List<Text> feedback = state.feedback();
        if (this.waiting) {
            drawTextWithShadow(matrices, this.textRenderer, Text.literal("..."), leftX, this.feedbackTop, 0xFF808080);
        } else {
            int line = 0;
            for (int i = Math.max(0, feedback.size() - 2); i < feedback.size(); i++) {
                String text = this.textRenderer.trimToWidth(feedback.get(i).getString(), this.panelWidth - 2 * PAD);
                drawTextWithShadow(matrices, this.textRenderer, Text.literal(text)
                        .setStyle(feedback.get(i).getStyle()), leftX, this.feedbackTop + line * LINE, 0xFFE0E0E0);
                line++;
            }
        }
        super.render(matrices, mouseX, mouseY, delta);
    }

    private static int difficultyColor(String difficulty) {
        return switch (difficulty) {
            case "easy" -> 0xFF80D080;
            case "medium" -> 0xFFE0E0A0;
            case "hard" -> 0xFFE0A060;
            case "deadly" -> 0xFFE06060;
            default -> 0xFFE0E0E0;
        };
    }
}
