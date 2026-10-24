package mattonfire.dnd.classes.Client.Hud;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.systems.RenderSystem;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Client.ConditionClient;
import mattonfire.dnd.classes.Conditions.Condition;
import mattonfire.dnd.classes.Conditions.Conditions;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * The player's conditions as a row of small icons directly above the save lane (right of the crosshair). Under each
 * icon a 1 px bar shows the time left (none when it lasts until something ends it); a tiny d20 in the corner means
 * a repeat save is coming, flashing in the last 10 ticks before it's rolled. Up to {@value #MAX_ICONS} icons, then
 * "+N". Hidden with F1. A Petrified player also gets a grey stone edge round the screen.
 */
@Environment(EnvType.CLIENT)
public final class ConditionHud {
    static final Identifier ICONS = new Identifier(DnDClasses.MOD_ID, "textures/gui/conditions.png");
    static final int CELL = 16;
    static final int SIZE = 12;
    static final int GAP = 2;
    static final int MAX_ICONS = 6;

    private ConditionHud() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register(ConditionHud::render);
    }

    /** One icon: the condition's longest instance, and whether any instance has a repeat save. */
    private static final class Icon {
        final Condition condition;
        int remaining;
        int total;
        int nextSave = Integer.MAX_VALUE;
        boolean save;

        Icon(Condition condition) {
            this.condition = condition;
        }
    }

    static List<Icon> icons() {
        Map<Condition, Icon> byCondition = new EnumMap<>(Condition.class);
        for (ConditionClient.Entry entry : ConditionClient.entries()) {
            Icon icon = byCondition.get(entry.condition);
            boolean longer = icon == null || icon.remaining >= 0 && (entry.remaining < 0 || entry.remaining > icon.remaining);
            if (icon == null) {
                icon = new Icon(entry.condition);
                byCondition.put(entry.condition, icon);
            }
            if (longer) {
                icon.remaining = entry.remaining;
                icon.total = Math.max(1, entry.total);
            }
            if (entry.saveAbility != null) {
                icon.save = true;
                icon.nextSave = Math.min(icon.nextSave, entry.nextSave);
            }
        }
        return new ArrayList<>(byCondition.values());
    }

    private static void render(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) {
            return;
        }
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        if (Conditions.has(client.player, Condition.PETRIFIED)) {
            stoneEdge(matrices, width, height);
        }
        List<Icon> icons = icons();
        if (icons.isEmpty()) {
            return;
        }
        int left = width / 2 + 22;
        int top = height / 2 - 8 - 15;
        RenderSystem.enableBlend();
        int shown = Math.min(icons.size(), icons.size() > MAX_ICONS ? MAX_ICONS - 1 : MAX_ICONS);
        for (int i = 0; i < shown; i++) {
            Icon icon = icons.get(i);
            int x = left + i * (SIZE + GAP);
            DrawableHelper.fill(matrices, x - 1, top - 1, x + SIZE + 1, top + SIZE + 1, 0x80000000);
            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, ICONS);
            DrawableHelper.drawTexture(matrices, x, top, SIZE, SIZE, icon.condition.ordinal() * CELL, 0, CELL, CELL,
                    CELL * Condition.values().length, CELL);
            if (icon.remaining >= 0 && icon.total > 0) {
                int bar = Math.max(1, Math.round(SIZE * Math.min(1.0F, icon.remaining / (float) icon.total)));
                DrawableHelper.fill(matrices, x, top + SIZE + 1, x + SIZE, top + SIZE + 2, 0xFF303030);
                DrawableHelper.fill(matrices, x, top + SIZE + 1, x + bar, top + SIZE + 2, 0xFFE0E0E0);
            }
            if (icon.save) {
                boolean flash = icon.nextSave <= 10 && (client.player.age / 2) % 2 == 0;
                RenderSystem.setShaderColor(1.0F, flash ? 0.85F : 1.0F, flash ? 0.3F : 1.0F, 1.0F);
                RenderSystem.setShaderTexture(0, DiceRollHud.D20_TEXTURE);
                DrawableHelper.drawTexture(matrices, x + SIZE - 5, top - 2, 6, 6, 0, 0, 64, 64, 64, 64);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
        if (icons.size() > shown) {
            int x = left + shown * (SIZE + GAP);
            client.textRenderer.drawWithShadow(matrices, "+" + (icons.size() - shown), x + 1, top + 2, 0xFFFFFF);
        }
        RenderSystem.disableBlend();
    }

    /** A grey stone vignette round the screen edge. */
    private static void stoneEdge(MatrixStack matrices, int width, int height) {
        int depth = Math.max(12, Math.min(width, height) / 8);
        int steps = 6;
        for (int i = 0; i < steps; i++) {
            int alpha = (int) (0x90 * (1.0F - i / (float) steps));
            int color = alpha << 24 | 0x6E6E6E;
            int inset = depth * i / steps;
            int next = depth * (i + 1) / steps;
            DrawableHelper.fill(matrices, inset, inset, width - inset, next, color);
            DrawableHelper.fill(matrices, inset, height - next, width - inset, height - inset, color);
            DrawableHelper.fill(matrices, inset, next, next, height - next, color);
            DrawableHelper.fill(matrices, width - next, next, width - inset, height - next, color);
        }
    }
}
