package mattonfire.dnd.classes.client.Hud;

import com.mojang.blaze3d.systems.RenderSystem;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.IEntityDataSaver;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class PowerupOverlay implements HudRenderCallback {
    private static final Identifier FULL_POWER = new Identifier(DnDClasses.MOD_ID, "textures/power/full.png");
    private static final Identifier EMPTY_POWER = new Identifier(DnDClasses.MOD_ID, "textures/power/empty.png");

    @Override
    public void onHudRender(MatrixStack matrices, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        int x = 0;
        int y = 0;
        if (client == null || client.player == null) {
            return;
        }

        // Check if the player is in Creative Mode
        if (client.player.isCreative()) {
            return;
        }

        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        x = width / 2;
        y = height;

        RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, EMPTY_POWER);
        for (int i = 0; i < DnDClasses.MANA_ICONS; i++) {
            DrawableHelper.drawTexture(matrices, x + (i * 9) + 10, y - 48, 0, 0, 9, 9, 9, 9);
        }

        RenderSystem.setShaderTexture(0, FULL_POWER);
        for (int i = 0; i < DnDClasses.MANA_ICONS; i++) {
            if (((IEntityDataSaver) MinecraftClient.getInstance().player).getPersistentData().getInt("mana") > i) {
                DrawableHelper.drawTexture(matrices, x + (i * 9) + 10, y - 48, 0, 0, 9, 9,
                        9, 9);
            } else {
                break;
            }
        }
    }
}
