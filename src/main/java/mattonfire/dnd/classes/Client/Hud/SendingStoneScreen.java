package mattonfire.dnd.classes.Client.Hud;

import org.lwjgl.glfw.GLFW;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.magic.items.SendingStones;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

/**
 * The Sending Stone's message screen (design 10, 8.2): one text field ({@value SendingStones#MAX_CHARS} characters),
 * a live word count ("17/25 words", red past {@value SendingStones#MAX_WORDS}) and a Send button. Enter sends. The
 * server checks everything again ({@link SendingStones}).
 */
public class SendingStoneScreen extends Screen {
    private static final int FIELD_WIDTH = 240;

    private final int slot;
    private TextFieldWidget field;
    private ButtonWidget sendButton;

    public SendingStoneScreen(int slot) {
        super(Text.translatable("screen.dndclasses.sending_stone"));
        this.slot = slot;
    }

    /** Opens the screen for the stone in this inventory slot of the local player. */
    public static void open(PlayerEntity player, int slot) {
        MinecraftClient.getInstance().setScreen(new SendingStoneScreen(slot));
    }

    @Override
    protected void init() {
        int width = Math.min(FIELD_WIDTH, this.width - 32);
        int x = (this.width - width) / 2;
        int y = this.height / 2 - 10;
        String previous = this.field != null ? this.field.getText() : "";
        this.field = new TextFieldWidget(this.textRenderer, x, y, width, 20,
                Text.translatable("screen.dndclasses.sending_stone.field"));
        this.field.setMaxLength(SendingStones.MAX_CHARS);
        this.field.setText(previous);
        this.field.setChangedListener(text -> this.updateButton());
        this.addSelectableChild(this.field);
        this.sendButton = this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.dndclasses.sending_stone.send"), button -> this.send())
                .dimensions(this.width / 2 - 50, y + 34, 100, 20).build());
        this.setInitialFocus(this.field);
        this.updateButton();
    }

    private int words() {
        return SendingStones.words(this.field.getText());
    }

    private void updateButton() {
        int words = this.words();
        this.sendButton.active = words > 0 && words <= SendingStones.MAX_WORDS;
    }

    private void send() {
        int words = this.words();
        if (words == 0 || words > SendingStones.MAX_WORDS)
            return;
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(this.slot);
        buf.writeString(this.field.getText().trim());
        ClientPlayNetworking.send(SendingStones.C2S_SEND, buf);
        this.close();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            this.send();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        this.field.tick();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        int y = this.height / 2 - 10;
        drawCenteredTextWithShadow(matrices, this.textRenderer, this.title, this.width / 2, y - 30, 0x7FDFFF);
        drawCenteredTextWithShadow(matrices, this.textRenderer,
                Text.translatable("screen.dndclasses.sending_stone.hint"), this.width / 2, y - 16, 0xA0A0A0);
        this.field.render(matrices, mouseX, mouseY, delta);
        int words = this.words();
        Text count = Text.translatable("screen.dndclasses.sending_stone.words", words, SendingStones.MAX_WORDS);
        int color = words > SendingStones.MAX_WORDS ? 0xFF5555 : 0xA0A0A0;
        this.textRenderer.drawWithShadow(matrices, count, this.field.getX() + this.field.getWidth()
                - this.textRenderer.getWidth(count), y + 23, color);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
