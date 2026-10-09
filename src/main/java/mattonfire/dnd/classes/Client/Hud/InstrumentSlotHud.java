package mattonfire.dnd.classes.Client.Hud;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.systems.RenderSystem;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Music.BardInstrumentSlot;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.option.AttackIndicator;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;

/**
 * The Bard's instrument slot: a 10th hotbar slot, right of the hotbar, that
 * always holds the lute, with the cooldown overlay and the key's name. The
 * "Play instrument" key asks the server to play it ({@link BardInstrumentSlot}).
 */
public final class InstrumentSlotHud {
    private static final Identifier WIDGETS = new Identifier("textures/gui/widgets.png");

    private static final KeyBinding PLAY_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.dnd-classes.play-instrument", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G,
            "category.dnd-classes.dnd-classes"));

    private InstrumentSlotHud() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (PLAY_KEY.wasPressed()) {
                if (shows(client)) {
                    ClientPlayNetworking.send(BardInstrumentSlot.C2S_PLAY, new PacketByteBuf(Unpooled.buffer()));
                }
            }
        });
        HudRenderCallback.EVENT.register((matrices, tickDelta) -> render(matrices));
    }

    private static boolean shows(MinecraftClient client) {
        PlayerEntity player = client.player;
        return player != null && !player.isSpectator() && client.interactionManager != null
                && player instanceof PlayerEntityExt ext && ext.getDndClass() == DndCharacter.BARD;
    }

    private static void render(MatrixStack matrices) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden || !shows(client))
            return;
        PlayerEntity player = client.player;
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        // Right of the hotbar, past the offhand slot or the attack indicator when they're on that side.
        int x = width / 2 + 91 + 4;
        boolean offhandRight = player.getMainArm() == Arm.LEFT && !player.getOffHandStack().isEmpty();
        if (offhandRight) {
            x += 29;
        } else if (player.getMainArm() == Arm.RIGHT
                && client.options.getAttackIndicator().getValue() == AttackIndicator.HOTBAR) {
            x += 22;
        }
        int y = height - 22;

        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, WIDGETS);
        // One hotbar slot: the left 21 pixels of the hotbar and its 1 pixel right border.
        DrawableHelper.drawTexture(matrices, x, y, 0, 0, 21, 22, 256, 256);
        DrawableHelper.drawTexture(matrices, x + 21, y, 181, 0, 1, 22, 256, 256);
        RenderSystem.disableBlend();

        ItemStack lute = new ItemStack(BardInstrumentSlot.instrument());
        client.getItemRenderer().renderInGuiWithOverrides(matrices, lute, x + 3, y + 3);
        client.getItemRenderer().renderGuiItemOverlay(matrices, client.textRenderer, lute, x + 3, y + 3);

        // The key's name, small, in the slot's top left corner.
        String key = PLAY_KEY.getBoundKeyLocalizedText().getString();
        if (key.length() > 3) {
            key = key.substring(0, 3);
        }
        matrices.push();
        matrices.translate(x + 2, y + 2, 300);
        matrices.scale(0.5F, 0.5F, 1.0F);
        client.textRenderer.drawWithShadow(matrices, key, 0, 0, 0xFFFFFF);
        matrices.pop();
    }
}
