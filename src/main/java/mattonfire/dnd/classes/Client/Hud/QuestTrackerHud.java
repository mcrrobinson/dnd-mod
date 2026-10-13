package mattonfire.dnd.classes.Client.Hud;

import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.quest.client.ClientQuests;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Top-right: the tracked quest's title and up to {@value #MAX_OBJECTIVES} objectives of its current stage
 * with progress. Moves down below vanilla's status effect icons when they show, keeps clear of the boss bar,
 * and hides with F1 and F3 like {@link PartyHud}.
 */
public final class QuestTrackerHud {
    static final int MAX_OBJECTIVES = 3;
    private static final int MARGIN = 4;
    private static final int MAX_WIDTH = 150;
    /** Half the vanilla boss bar's width plus a gap: the tracker never reaches into the centre. */
    private static final int BOSS_BAR_HALF = 91 + 6;
    private static final int LINE = 10;

    /** The quest and width the cached lines were built for (render thread only). */
    private static ClientQuests.Active linesFor;
    private static int linesMaxWidth;
    private static List<Text> cachedLines = List.of();
    private static int[] cachedColors = new int[0];
    private static int cachedWidth;

    private QuestTrackerHud() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register((matrices, tickDelta) -> render(matrices));
    }

    private static void render(MatrixStack matrices) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden || client.options.debugEnabled) {
            return;
        }
        ClientQuests.Active quest = ClientQuests.tracked();
        if (quest == null) {
            return;
        }
        TextRenderer text = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int maxWidth = Math.min(MAX_WIDTH, screenWidth / 2 - BOSS_BAR_HALF - MARGIN);
        if (maxWidth < 60) {
            return;
        }

        if (quest != linesFor || maxWidth != linesMaxWidth) {
            build(text, quest, maxWidth);
        }
        List<Text> lines = cachedLines;
        int[] colors = cachedColors;
        int width = cachedWidth;
        int x = screenWidth - MARGIN - width;
        int y = top(client);
        DrawableHelper.fill(matrices, x - 3, y - 3, screenWidth - MARGIN + 3, y + lines.size() * LINE + 1, 0x80000000);
        for (int i = 0; i < lines.size(); i++) {
            text.drawWithShadow(matrices, lines.get(i), x, y + i * LINE, colors[i]);
        }
    }

    /**
     * Builds and measures the tracker's lines. The quest is a new object each time a quest packet arrives, so
     * the lines are rebuilt then (or when the window width changes), not every frame.
     */
    private static void build(TextRenderer text, ClientQuests.Active quest, int maxWidth) {
        List<Text> lines = new ArrayList<>();
        List<ClientQuests.Objective> objectives = quest.objectives();
        int shown = Math.min(objectives.size(), MAX_OBJECTIVES);
        int[] colors = new int[1 + shown];
        lines.add(fit(text, quest.title().copy().formatted(Formatting.GOLD), maxWidth));
        colors[0] = 0xFFFFFF;
        for (int i = 0; i < shown; i++) {
            ClientQuests.Objective objective = objectives.get(i);
            String count = objective.count() > 1 || objective.done()
                    ? " " + objective.progress() + "/" + objective.count() : "";
            Text line = Text.literal(objective.done() ? "✔ " : "- ")
                    .append(objective.description().copy()).append(count);
            lines.add(fit(text, line, maxWidth));
            colors[1 + i] = objective.done() ? 0x55FF55 : 0xE0E0E0;
        }
        int width = 0;
        for (Text line : lines) {
            width = Math.max(width, text.getWidth(line));
        }
        cachedLines = List.copyOf(lines);
        cachedColors = colors;
        cachedWidth = width;
        linesFor = quest;
        linesMaxWidth = maxWidth;
    }

    /** Below vanilla's status effect icons (good ones at y 1, bad ones a row lower), if any show. */
    static int top(MinecraftClient client) {
        boolean good = false;
        boolean bad = false;
        for (StatusEffectInstance effect : client.player.getStatusEffects()) {
            if (!effect.shouldShowIcon()) {
                continue;
            }
            if (effect.getEffectType().isBeneficial()) {
                good = true;
            } else {
                bad = true;
            }
        }
        return bad ? 1 + 26 + 26 + MARGIN : good ? 1 + 26 + MARGIN : MARGIN + 2;
    }

    /** The text cut to fit, with "..." when it doesn't. */
    static Text fit(TextRenderer text, Text line, int width) {
        if (text.getWidth(line) <= width) {
            return line;
        }
        String trimmed = text.trimToWidth(line.getString(), width - text.getWidth("...")) + "...";
        return Text.literal(trimmed).setStyle(line.getStyle());
    }
}
