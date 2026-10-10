package mattonfire.dnd.classes.Client.Hud;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.mojang.blaze3d.systems.RenderSystem;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Abilities.Advantage;
import mattonfire.dnd.classes.Abilities.RollKind;
import mattonfire.dnd.classes.Config.SaveRollsMode;
import mattonfire.dnd.classes.Registry.ModSounds;
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
 * e.g. {@code DEX save 13+4=17 vs 14 ✔ half damage}, held for 2 seconds before it fades.
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
    private static final Identifier D20_TEXTURE = new Identifier(DnDClasses.MOD_ID, "textures/gui/d20.png");
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
                play(client, sound(roll.outcome()), 1.0f);
            }
            return;
        }
        for (Row row : ROWS) {
            if (row.matches(roll)) {
                row.roll = roll;
                row.detail = detail;
                row.count += count;
                row.age = SPIN_TICKS; // refresh without spinning again
                if (!roll.secret()) {
                    play(client, sound(roll.outcome()), natural ? 1.0f : QUIET);
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
                play(client, sound(row.roll.outcome()), natural ? 1.0f : QUIET);
            }
        }
        ROWS.removeIf(row -> row.age > SPIN_TICKS + HOLD_TICKS + FADE_TICKS);
    }

    private static SoundEvent sound(D20.Outcome outcome) {
        return switch (outcome) {
            case SUCCESS -> ModSounds.DICE_SUCCESS;
            case FAILURE -> ModSounds.DICE_FAILURE;
            case CRITICAL -> ModSounds.DICE_CRITICAL;
            case FUMBLE -> ModSounds.DICE_FUMBLE;
        };
    }

    private static void play(MinecraftClient client, SoundEvent sound, float volume) {
        client.getSoundManager().play(PositionedSoundInstance.master(sound, 1.0f, volume));
    }

    private static int color(D20.Outcome outcome) {
        return switch (outcome) {
            case SUCCESS -> 0x55FF55;
            case FAILURE -> 0xFF6655;
            case CRITICAL -> 0xFFD23F;
            case FUMBLE -> 0xC02030;
        };
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
     * The line after the die: label, sum, a tick or cross, then the detail ("half damage"; natural 20s and 1s say
     * so) and the repeat count. {@code withDetail} false leaves the detail out when space is short.
     */
    static MutableText line(D20.Roll roll, Text detail, int count, boolean landed, boolean withDetail) {
        MutableText line = label(roll).formatted(Formatting.BOLD);
        if (!landed) {
            return line.append(Text.literal(" ...").formatted(Formatting.GRAY).styled(s -> s.withBold(false)));
        }
        MutableText sum = Text.literal(" " + roll.natural());
        if (roll.natural2() > 0) {
            sum.append(Text.literal("(" + roll.natural2() + (roll.mode() == Advantage.ADVANTAGE ? " adv)" : " dis)"))
                    .formatted(Formatting.DARK_GRAY));
        }
        sum.append(Text.literal((roll.modifier() < 0 ? "-" : "+") + Math.abs(roll.modifier()) + "=" + roll.total()));
        if (roll.dc() > 0 && !roll.secret()) {
            sum.append(Text.translatable("save.dndclasses.lane.vs", roll.dc()));
        }
        line.append(sum.styled(s -> s.withBold(false).withColor(0xD0D0D0)));
        if (!roll.secret()) {
            MutableText tail = Text.literal(roll.outcome().succeeded() ? " \u2714" : " \u2718");
            if (roll.outcome() == D20.Outcome.CRITICAL || roll.outcome() == D20.Outcome.FUMBLE) {
                tail.append(" ").append(Text.translatable("save.dndclasses.lane." + roll.outcome().name()
                        .toLowerCase(Locale.ROOT)));
            }
            if (withDetail && !detail.getString().isEmpty()) {
                tail.append(" ").append(detail);
            }
            line.append(tail.styled(s -> s.withBold(false).withColor(color(roll.outcome()))));
        }
        if (count > 1) {
            line.append(Text.literal(" \u00d7" + count).styled(s -> s.withBold(true).withColor(0xFFD23F)));
        }
        return line;
    }

    private static void render(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (ROWS.isEmpty() || client.player == null || client.options.hudHidden) {
            return;
        }
        TextRenderer text = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int left = screenWidth / 2 + 22;
        int top = client.getWindow().getScaledHeight() / 2 - 8;
        RenderSystem.enableBlend();
        for (int i = 0; i < ROWS.size(); i++) {
            Row row = ROWS.get(i);
            float life = row.age + tickDelta;
            boolean spinning = row.age < SPIN_TICKS;
            float fade = MathHelper.clamp((SPIN_TICKS + HOLD_TICKS + FADE_TICKS - life) / FADE_TICKS, 0.0f, 1.0f);
            int alpha = Math.max(8, (int) (fade * 255)) << 24;
            int y = top + i * ROW_HEIGHT;
            Text line = line(row.roll, row.detail, row.count, !spinning, true);
            if (left + DIE_SIZE + 4 + text.getWidth(line) > screenWidth - 3) {
                line = line(row.roll, row.detail, row.count, !spinning, false); // narrow screen: drop the detail
            }
            int width = DIE_SIZE + 4 + text.getWidth(line);
            // Still too wide (tiny window): slide left, but never over the crosshair
            int x = Math.max(screenWidth / 2 + 8, Math.min(left, screenWidth - 3 - width));
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

            text.drawWithShadow(matrices, line, x + DIE_SIZE + 4, y + 4, 0xFFFFFF | alpha);
        }
        RenderSystem.disableBlend();
    }
}
