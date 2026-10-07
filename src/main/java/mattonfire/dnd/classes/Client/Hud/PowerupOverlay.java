package mattonfire.dnd.classes.Client.Hud;

import com.mojang.blaze3d.systems.RenderSystem;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.IEntityDataSaver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Draws the mana bar. Called from {@code InGameHudMixin} at the end of
 * {@code InGameHud.renderStatusBars}, so it shares the health/food bars'
 * visibility (hidden in creative and spectator) and draws beneath chat.
 */
public final class PowerupOverlay {
    private static final Identifier FULL_POWER = new Identifier(DnDClasses.MOD_ID, "textures/power/full.png");
    private static final Identifier EMPTY_POWER = new Identifier(DnDClasses.MOD_ID, "textures/power/empty.png");

    private PowerupOverlay() {
    }

    public static void render(MatrixStack matrices) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        int x = client.getWindow().getScaledWidth() / 2;
        int y = client.getWindow().getScaledHeight();
        int mana = ((IEntityDataSaver) client.player).getPersistentData().getInt("mana");

        RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, EMPTY_POWER);
        for (int i = 0; i < DnDClasses.MANA_ICONS; i++) {
            DrawableHelper.drawTexture(matrices, x + (i * 9) + 10, y - 48, 0, 0, 9, 9, 9, 9);
        }

        RenderSystem.setShaderTexture(0, FULL_POWER);
        for (int i = 0; i < Math.min(mana, DnDClasses.MANA_ICONS); i++) {
            DrawableHelper.drawTexture(matrices, x + (i * 9) + 10, y - 48, 0, 0, 9, 9, 9, 9);
        }

        // Vanilla keeps drawing (mount health) with the GUI icons texture it bound earlier.
        RenderSystem.setShaderTexture(0, DrawableHelper.GUI_ICONS_TEXTURE);
    }
}
