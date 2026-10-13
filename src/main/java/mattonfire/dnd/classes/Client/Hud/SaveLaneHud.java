package mattonfire.dnd.classes.Client.Hud;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.mojang.blaze3d.systems.RenderSystem;

import mattonfire.dnd.classes.Abilities.Advantage;
import mattonfire.dnd.classes.Abilities.RollKind;
import mattonfire.dnd.classes.Config.SaveRollsMode;
import mattonfire.dnd.classes.SkillChecks.D20;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/**
 * The save lane: saving throws as compact rows to the right of the crosshair, so they never take the big roll
 * panel ({@link DiceRollHud}) away from a check. Each row is a small die that spins for a moment, then one line,
 * e.g. {@code DEX save 13+4=17 vs 14 ✔ half damage}, held for 2 seconds before it fades. A line too wide for the
 * screen gives up parts until it fits ({@link #fit}).
 * <ul>
 * <li>At most {@link #MAX_ROWS} rows; a new one pushes out the oldest.</li>
 * <li>A save with the same label and outcome as a row still showing adds to that row's "x N" count and
 * refreshes it instead of adding a row (the server also merges saves it throttled).</li>
 * <li>Outcome sounds at 40% volume; natural 20s and 1s at full volume.</li>
 * <li>{@link SaveRollsMode}: {@code compact} draws rows, {@code off} draws nothing (natural 20s and 1s still
 * sound), {@code full} sends saves to the big panel unless it's showing a check.</li>
 * </ul>
 */
public final class SaveLaneHud {
    private static final Identifier D20_TEXTURE = DiceRollHud.D20_TEXTURE;
    static final int MAX_ROWS = 3;
    private static final int SPIN_TICKS = 6;
    private static final int HOLD_TICKS = 40;
    private static final int FADE_TICKS = 10;
    private static final int DIE_SIZE = 16;
    private static final int ROW_HEIGHT = 18;
    private static final float QUIET = 0.4f;

    private static final class Row {
        D20.Roll roll;
        Text detail;
        int count;
        int age;
        /**
         * The fitted line and its width, built when the row arrives or changes (a merged save, the die landing,
         * a new screen width), not every frame. Render thread only.
         */
        Text line;
        int lineWidth;
        boolean lineLanded;
        int lineMaxText = -1;

        Row(D20.Roll roll, Text detail, int count) {
            this.roll = roll;
            this.detail = detail;
            this.count = count;
        }

        boolean matches(D20.Roll other) {
            return roll.outcome() == other.outcome() && roll.ability() == other.ability()
                    && roll.label().getString().equals(other.label().getString());
        }
    }

    private static final List<Row> ROWS = new ArrayList<>();

    private SaveLaneHud() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(SaveLaneHud::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(ROWS::clear));
        HudRenderCallback.EVENT.register(SaveLaneHud::render);
    }

    /** A save-lane roll arrived ({@code count} saves the server merged into it). Client thread. */
    static void accept(MinecraftClient client, D20.Roll roll, Text detail, int count) {
        boolean natural = roll.natural() == 20 || roll.natural() == 1;
        if (SaveRollsMode.current() == SaveRollsMode.OFF) {
            if (natural && !roll.secret()) {
                play(client, roll.outcome().sound(), 1.0f);
            }
            return;
        }
        for (Row row : ROWS) {
            if (row.matches(roll)) {
                row.roll = roll;
                row.detail = detail;
                row.count += count;
                row.age = SPIN_TICKS; // refresh without spinning again
                row.line = null;
                if (!roll.secret()) {
                    play(client, roll.outcome().sound(), natural ? 1.0f : QUIET);
                }
                return;
            }
        }
        if (ROWS.size() >= MAX_ROWS) {
            ROWS.remove(0);
        }
        ROWS.add(new Row(roll, detail, count));
    }

    private static void tick(MinecraftClient client) {
        if (ROWS.isEmpty() || client.isPaused()) {
            return;
        }
        if (client.world == null) {
            ROWS.clear();
            return;
        }
        for (Row row : ROWS) {
            row.age++;
            if (row.age == SPIN_TICKS && !row.roll.secret()) {
                boolean natural = row.roll.natural() == 20 || row.roll.natural() == 1;
                play(client, row.roll.outcome().sound(), natural ? 1.0f : QUIET);
            }
        }
        ROWS.removeIf(row -> row.age > SPIN_TICKS + HOLD_TICKS + FADE_TICKS);
    }

    private static void play(MinecraftClient client, SoundEvent sound, float volume) {
        client.getSoundManager().play(PositionedSoundInstance.master(sound, 1.0f, volume));
    }

    /** "DEX save" for a plain save, "Fire breath (DEX)" for a named one. */
    private static MutableText label(D20.Roll roll) {
        Text label = roll.label();
        if (roll.ability() == null) {
            return label.copy();
        }
        Text shortName = Text.translatable(roll.ability().shortTranslationKey());
        if (roll.kind() == RollKind.SAVE && label.getContent() instanceof TranslatableTextContent t
                && t.getKey().equals(roll.ability().saveTranslationKey())) {
            return Text.translatable("save.dndclasses.lane.save", shortName);
        }
        return label.copy().append(Text.literal(" (").append(shortName).append(")"));
    }

    /**
     * How much a line gives up to fit beside the crosshair. Each level keeps everything the next one drops, so
     * the outcome (tick or cross, in the outcome colour) and the repeat count always stay.
     */
    static final int FULL = 0;
    /** Drops the detail ("half damage"). */
    static final int NO_DETAIL = 1;
    /** Also drops the ability suffix of a named save ("Frost breath (DEX)" becomes "Frost breath"). */
    static final int NO_ABILITY = 2;
    /** Also drops "vs DC" and the second die of advantage or disadvantage. */
    static final int NO_DC = 3;
    /** Also drops the "Nat 20" / "Nat 1" word (the die face still shows it) and the bold label. */
    static final int MINIMAL = 4;
    static final int MOST_COMPACT = MINIMAL;

    /**
     * The line after the die: label, sum, a tick or cross, then the detail ("half damage"; natural 20s and 1s say
     * so) and the repeat count. {@code compact} ({@link #FULL} to {@link #MINIMAL}) leaves parts out when space is
     * short; {@code maxLabelWidth} (pixels, or a negative number for no limit) trims the label with an ellipsis.
     */
    static MutableText line(TextRenderer text, D20.Roll roll, Text detail, int count, boolean landed, int compact,
            int maxLabelWidth) {
        MutableText label = compact >= NO_ABILITY ? roll.label().copy() : label(roll);
        if (compact >= NO_ABILITY && roll.ability() != null && roll.kind() == RollKind.SAVE
                && roll.label().getContent() instanceof TranslatableTextContent t
                && t.getKey().equals(roll.ability().saveTranslationKey())) {
            label = label(roll); // a plain save keeps its short "DEX save" label
        }
        boolean bold = compact < MINIMAL;
        if (maxLabelWidth >= 0) {
            String full = label.getString();
            int width = bold ? text.getWidth(label.copy().formatted(Formatting.BOLD)) : text.getWidth(full);
            if (width > maxLabelWidth) {
                String ellipsis = "\u2026";
                int room = Math.max(0, maxLabelWidth - text.getWidth(ellipsis) - (bold ? full.length() : 0));
                label = Text.literal(text.trimToWidth(full, room).stripTrailing() + ellipsis);
            }
        }
        MutableText line = bold ? label.formatted(Formatting.BOLD) : label;
        if (!landed) {
            return line.append(Text.literal(" ...").formatted(Formatting.GRAY).styled(s -> s.withBold(false)));
        }
        MutableText sum = Text.literal(" " + roll.natural());
        if (roll.natural2() > 0 && compact < NO_DC) {
            sum.append(Text.literal("(" + roll.natural2() + (roll.mode() == Advantage.ADVANTAGE ? " adv)" : " dis)"))
                    .formatted(Formatting.DARK_GRAY));
        }
        sum.append(Text.literal((roll.modifier() < 0 ? "-" : "+") + Math.abs(roll.modifier()) + "=" + roll.total()));
        if (roll.dc() > 0 && !roll.secret() && compact < NO_DC) {
            sum.append(Text.translatable("save.dndclasses.lane.vs", roll.dc()));
        }
        line.append(sum.styled(s -> s.withBold(false).withColor(0xD0D0D0)));
        if (roll.rerolled() > 0) {
            line.append(Text.literal(" \u2618").styled(s -> s.withBold(false).withColor(0x55FF55))); // Halfling Lucky
        }
        if (!roll.secret()) {
            MutableText tail = Text.literal(roll.outcome().succeeded() ? " \u2714" : " \u2718");
            if ((roll.outcome() == D20.Outcome.CRITICAL || roll.outcome() == D20.Outcome.FUMBLE)
                    && compact < MINIMAL) {
                tail.append(" ").append(Text.translatable("save.dndclasses.lane." + roll.outcome().name()
                        .toLowerCase(Locale.ROOT)));
            }
            if (compact < NO_DETAIL && !detail.getString().isEmpty()) {
                tail.append(" ").append(detail);
            }
            line.append(tail.styled(s -> s.withBold(false).withColor(roll.outcome().color())));
        }
        if (count > 1) {
            line.append(Text.literal(" \u00d7" + count).styled(s -> s.withBold(true).withColor(0xFFD23F)));
        }
        return line;
    }

    /**
     * The most detailed line that fits in {@code maxWidth} pixels: drop parts level by level, then trim the label
     * with an ellipsis. If even that is too wide (a tiny window), the caller scales the line down.
     */
    static Text fit(TextRenderer text, D20.Roll roll, Text detail, int count, boolean landed, int maxWidth) {
        Text line = null;
        for (int compact = FULL; compact <= MOST_COMPACT; compact++) {
            line = line(text, roll, detail, count, landed, compact, -1);
            if (text.getWidth(line) <= maxWidth) {
                return line;
            }
        }
        // Everything but the label, measured with the label trimmed to just its ellipsis
        int rest = text.getWidth(line(text, roll, detail, count, landed, MOST_COMPACT, 0)) - text.getWidth("\u2026");
        int room = Math.max(text.getWidth("\u2026") + 12, maxWidth - rest);
        return line(text, roll, detail, count, landed, MOST_COMPACT, room);
    }

    private static void render(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (ROWS.isEmpty() || client.player == null || client.options.hudHidden) {
            return;
        }
        TextRenderer text = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int left = screenWidth / 2 + 22;
        int minX = screenWidth / 2 + 8; // slide left on a narrow screen, but never over the crosshair
        int maxRight = screenWidth - 3;
        int maxText = maxRight - minX - DIE_SIZE - 4;
        int top = client.getWindow().getScaledHeight() / 2 - 8;
        RenderSystem.enableBlend();
        for (int i = 0; i < ROWS.size(); i++) {
            Row row = ROWS.get(i);
            float life = row.age + tickDelta;
            boolean spinning = row.age < SPIN_TICKS;
            float fade = MathHelper.clamp((SPIN_TICKS + HOLD_TICKS + FADE_TICKS - life) / FADE_TICKS, 0.0f, 1.0f);
            int alpha = Math.max(8, (int) (fade * 255)) << 24;
            int y = top + i * ROW_HEIGHT;
            if (row.line == null || row.lineLanded == spinning || row.lineMaxText != maxText) {
                row.line = fit(text, row.roll, row.detail, row.count, !spinning, maxText);
                row.lineWidth = text.getWidth(row.line);
                row.lineLanded = !spinning;
                row.lineMaxText = maxText;
            }
            Text line = row.line;
            int lineWidth = row.lineWidth;
            float scale = lineWidth > maxText && maxText > 0 ? Math.max(0.5f, maxText / (float) lineWidth) : 1.0f;
            int width = DIE_SIZE + 4 + MathHelper.ceil(lineWidth * scale);
            int x = Math.max(minX, Math.min(left, maxRight - width));
            DrawableHelper.fill(matrices, x - 2, y - 1, x + width + 3, y + DIE_SIZE + 1,
                    (int) (fade * 0x80) << 24);

            matrices.push();
            matrices.translate(x + DIE_SIZE / 2.0f, y + DIE_SIZE / 2.0f, 0);
            if (spinning) {
                float spin = (SPIN_TICKS - life) / SPIN_TICKS;
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(spin * 360.0f));
            }
            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, fade);
            RenderSystem.setShaderTexture(0, D20_TEXTURE);
            DrawableHelper.drawTexture(matrices, -DIE_SIZE / 2, -DIE_SIZE / 2, DIE_SIZE, DIE_SIZE, 0, 0, 64, 64, 64,
                    64);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            if (!spinning) {
                String face = Integer.toString(row.roll.natural());
                int faceColor = row.roll.natural() == 20 ? 0xFFE066 : row.roll.natural() == 1 ? 0x202020 : 0xFFFFFF;
                matrices.scale(0.75f, 0.75f, 1.0f);
                text.drawWithShadow(matrices, face, -text.getWidth(face) / 2.0f + 0.5f, -3.0f, faceColor | alpha);
            }
            matrices.pop();

            if (scale < 1.0f) {
                matrices.push();
                matrices.translate(x + DIE_SIZE + 4, y + 8 - 4 * scale, 0);
                matrices.scale(scale, scale, 1.0f);
                text.drawWithShadow(matrices, line, 0, 0, 0xFFFFFF | alpha);
                matrices.pop();
            } else {
                text.drawWithShadow(matrices, line, x + DIE_SIZE + 4, y + 4, 0xFFFFFF | alpha);
            }
        }
        RenderSystem.disableBlend();
    }
}
