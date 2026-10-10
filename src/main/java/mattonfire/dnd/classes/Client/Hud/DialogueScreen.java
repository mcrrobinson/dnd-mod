package mattonfire.dnd.classes.Client.Hud;

import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.quest.dialogue.DialogueManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

/**
 * An NPC's dialogue page (S2C {@code dndclasses:dialogue_open}): the speaker's name, what they say and the
 * player's replies, in a panel along the bottom of the screen so the world and the d20 HUD stay visible.
 * Click a reply or press its number; the choice goes back as C2S {@code dndclasses:dialogue_choose}
 * (the option index) and the server answers with the next page or {@code dialogue_close}. Doesn't pause
 * the game.
 */
public class DialogueScreen extends Screen {
    private static final int PANEL_MAX_WIDTH = 360;
    private static final int PAD = 8;
    private static final int LINE = 10;
    private static final int BACKGROUND = 0xE0141018;
    private static final int BORDER = 0xFFB08A3E;
    private static final int OPTION_HOVER = 0x60B08A3E;

    private final Text speaker;
    private final List<Text> lines;
    private final List<Text> options;
    private final List<OrderedText> wrapped = new ArrayList<>();
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    /** Set once a reply is sent, so a double click can't answer the next page by accident. */
    private boolean waiting;

    public DialogueScreen(Text speaker, List<Text> lines, List<Text> options) {
        super(speaker);
        this.speaker = speaker;
        this.lines = lines;
        this.options = options;
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(DialogueManager.S2C_OPEN, (client, handler, buf, sender) -> {
            Text speaker = buf.readText();
            List<Text> lines = buf.readList(PacketByteBuf::readText);
            List<Text> options = buf.readList(PacketByteBuf::readText);
            client.execute(() -> {
                DnDClasses.LOGGER.info("[Dialogue] client page from {}: {} options {}", speaker.getString(),
                        lines.stream().map(Text::getString).toList(), options.stream().map(Text::getString).toList());
                client.setScreen(new DialogueScreen(speaker, lines, options));
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(DialogueManager.S2C_CLOSE, (client, handler, buf, sender) ->
                client.execute(() -> {
                    if (client.currentScreen instanceof DialogueScreen) {
                        client.setScreen(null);
                    }
                }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (client.currentScreen instanceof DialogueScreen) {
                client.execute(() -> client.setScreen(null));
            }
        });
    }

    @Override
    protected void init() {
        this.panelWidth = Math.min(PANEL_MAX_WIDTH, this.width - 32);
        int inner = this.panelWidth - 2 * PAD;
        this.wrapped.clear();
        for (Text line : this.lines) {
            this.wrapped.addAll(this.textRenderer.wrapLines(line, inner));
        }
        List<OptionWidget> widgets = new ArrayList<>();
        int optionsHeight = 0;
        for (int i = 0; i < this.options.size(); i++) {
            OptionWidget widget = new OptionWidget(i, inner, this.options.get(i));
            widgets.add(widget);
            optionsHeight += widget.getHeight() + 2;
        }
        this.panelHeight = PAD + LINE + 4 + this.wrapped.size() * LINE + 6 + optionsHeight + PAD - 2;
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = Math.max(4, this.height - this.panelHeight - 10);
        int y = this.panelY + PAD + LINE + 4 + this.wrapped.size() * LINE + 6;
        for (OptionWidget widget : widgets) {
            widget.setX(this.panelX + PAD);
            widget.setY(y);
            y += widget.getHeight() + 2;
            this.addDrawableChild(widget);
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        int x0 = this.panelX;
        int y0 = this.panelY;
        int x1 = x0 + this.panelWidth;
        int y1 = y0 + this.panelHeight;
        fill(matrices, x0, y0, x1, y1, BACKGROUND);
        fill(matrices, x0, y0, x1, y0 + 1, BORDER);
        fill(matrices, x0, y1 - 1, x1, y1, BORDER);
        fill(matrices, x0, y0, x0 + 1, y1, BORDER);
        fill(matrices, x1 - 1, y0, x1, y1, BORDER);
        this.textRenderer.drawWithShadow(matrices, this.speaker.copy().formatted(Formatting.GOLD, Formatting.BOLD),
                x0 + PAD, y0 + PAD, 0xFFFFFF);
        int y = y0 + PAD + LINE + 4;
        for (OrderedText line : this.wrapped) {
            this.textRenderer.drawWithShadow(matrices, line, x0 + PAD, y, 0xE8E0D0);
            y += LINE;
        }
        fill(matrices, x0 + PAD, y + 2, x1 - PAD, y + 3, 0x60B08A3E);
        super.render(matrices, mouseX, mouseY, delta);
        // A check's d20 sits under the crosshair, which the panel may cover: draw it again on top.
        DiceRollHud.render(matrices, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9) {
            int index = keyCode - GLFW.GLFW_KEY_1;
            if (index < this.options.size()) {
                this.choose(index);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** Sends the reply with this index (also used by DevScript's {@code button} step). */
    public void choose(int index) {
        if (this.waiting || index < 0 || index >= this.options.size()) {
            return;
        }
        this.waiting = true;
        send(index);
    }

    public int optionCount() {
        return this.options.size();
    }

    private static void send(int index) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(index);
        ClientPlayNetworking.send(DialogueManager.C2S_CHOOSE, buf);
    }

    @Override
    public void close() {
        // Tell the server the talk is over (it may already know, e.g. after "Goodbye").
        if (!this.waiting && MinecraftClient.getInstance().getNetworkHandler() != null) {
            send(-1);
        }
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /** A reply: its number and wrapped text, highlighted on hover. */
    private class OptionWidget extends PressableWidget {
        private final int index;
        private final List<OrderedText> text;

        OptionWidget(int index, int width, Text message) {
            super(0, 0, width, 0, message);
            this.index = index;
            this.text = DialogueScreen.this.textRenderer.wrapLines(
                    Text.literal((index + 1) + ". ").formatted(Formatting.GRAY).append(message), width - 4);
            this.height = this.text.size() * LINE + 4;
        }

        @Override
        public void onPress() {
            DialogueScreen.this.choose(this.index);
        }

        @Override
        public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            if (this.isHovered() || this.isFocused()) {
                fill(matrices, this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, OPTION_HOVER);
            }
            int y = this.getY() + 2;
            for (OrderedText line : this.text) {
                DialogueScreen.this.textRenderer.drawWithShadow(matrices, line, this.getX() + 2, y, 0xFFFFFF);
                y += LINE;
            }
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
            builder.put(NarrationPart.TITLE, this.getMessage());
        }
    }
}
