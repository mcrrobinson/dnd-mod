package mattonfire.dnd.classes.Client.Hud;

import com.mojang.blaze3d.systems.RenderSystem;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.ModSounds;
import mattonfire.dnd.classes.SkillChecks.D20;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/**
 * Shows a d20 skill check just below the crosshair: the die tumbles through numbers for a moment
 * (with a rattle), then lands on the natural roll and shows the sum, the DC and what came of it,
 * with a sound for the outcome.
 */
public final class DiceRollHud {
    private static final Identifier D20_TEXTURE = new Identifier(DnDClasses.MOD_ID, "textures/gui/d20.png");
    private static final int ROLL_TICKS = 14;
    private static final int SHOW_TICKS = 60;
    private static final int FADE_TICKS = 10;
    private static final int DIE_SIZE = 32;

    private static D20.Roll roll;
    private static Text detail;
    private static int age;
    private static int tumbleFace = 1;

    private DiceRollHud() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(D20.S2C_ROLL, (client, handler, buf, sender) -> {
            D20.Roll received = D20.Roll.read(buf);
            Text receivedDetail = buf.readText();
            client.execute(() -> start(client, received, receivedDetail));
        });
        ClientTickEvents.END_CLIENT_TICK.register(DiceRollHud::tick);
        HudRenderCallback.EVENT.register(DiceRollHud::render);
    }

    private static void start(MinecraftClient client, D20.Roll newRoll, Text newDetail) {
        roll = newRoll;
        detail = newDetail;
        age = 0;
        play(client, ModSounds.DICE_ROLL, 1.0f);
    }

    private static void tick(MinecraftClient client) {
        if (roll == null || client.isPaused()) {
            return;
        }
        age++;
        if (age < ROLL_TICKS && age % 2 == 0) {
            int next = 1 + client.world.random.nextInt(19);
            tumbleFace = next >= tumbleFace ? next + 1 : next; // never the same face twice
        }
        if (age == ROLL_TICKS) {
            play(client, switch (roll.outcome()) {
                case SUCCESS -> ModSounds.DICE_SUCCESS;
                case FAILURE -> ModSounds.DICE_FAILURE;
                case CRITICAL -> ModSounds.DICE_CRITICAL;
                case FUMBLE -> ModSounds.DICE_FUMBLE;
            }, 1.0f);
        }
        if (age > ROLL_TICKS + SHOW_TICKS) {
            roll = null;
        }
    }

    private static void play(MinecraftClient client, SoundEvent sound, float pitch) {
        client.getSoundManager().play(PositionedSoundInstance.master(sound, pitch));
    }

    private static int color(D20.Outcome outcome) {
        return switch (outcome) {
            case SUCCESS -> 0x55FF55;
            case FAILURE -> 0xFF6655;
            case CRITICAL -> 0xFFD23F;
            case FUMBLE -> 0xC02030;
        };
    }

    private static void render(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (roll == null || client.player == null) {
            return;
        }
        TextRenderer text = client.textRenderer;
        boolean rolling = age < ROLL_TICKS;
        float life = age + tickDelta;
        float fade = MathHelper.clamp((ROLL_TICKS + SHOW_TICKS - life) / FADE_TICKS, 0.0f, 1.0f);
        int alpha = Math.max(8, (int) (fade * 255)) << 24;

        Text title = Text.translatable(roll.skill().translationKey()).formatted(Formatting.BOLD);
        Text sum;
        Text outcome;
        if (rolling) {
            sum = Text.translatable("skill.dndclasses.rolling");
            outcome = Text.empty();
        } else {
            MutableText line = Text.literal(roll.natural() + " " + (roll.modifier() < 0 ? "- " : "+ ")
                    + Math.abs(roll.modifier()) + " = " + roll.total());
            if (roll.dc() > 0) {
                line.append(Text.translatable("skill.dndclasses.vs_dc", roll.dc()));
            }
            sum = line;
            outcome = Text.translatable(roll.outcome().translationKey())
                    .append(Text.literal(" - ").append(detail));
        }

        int textWidth = Math.max(text.getWidth(title), Math.max(text.getWidth(sum), text.getWidth(outcome)));
        int width = DIE_SIZE + 6 + textWidth;
        int left = (client.getWindow().getScaledWidth() - width) / 2;
        int top = client.getWindow().getScaledHeight() / 2 + 20;

        // Translucent backing so the text reads against any scene
        DrawableHelper.fill(matrices, left - 4, top - 3, left + width + 4, top + DIE_SIZE + 3,
                ((int) (fade * 0x90) << 24));

        // The die: tumbles while rolling, then settles
        matrices.push();
        float cx = left + DIE_SIZE / 2.0f;
        float cy = top + DIE_SIZE / 2.0f;
        matrices.translate(cx, cy, 0);
        if (rolling) {
            float spin = (ROLL_TICKS - life) / ROLL_TICKS;
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(spin * spin * 540.0f));
            float bounce = 1.0f + 0.12f * MathHelper.sin(life * 1.6f) * spin;
            matrices.scale(bounce, bounce, 1.0f);
        } else {
            float pop = Math.max(0.0f, 1.0f - (life - ROLL_TICKS) / 4.0f);
            matrices.scale(1.0f + 0.25f * pop, 1.0f + 0.25f * pop, 1.0f);
        }
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, fade);
        RenderSystem.setShaderTexture(0, D20_TEXTURE);
        DrawableHelper.drawTexture(matrices, -DIE_SIZE / 2, -DIE_SIZE / 2, DIE_SIZE, DIE_SIZE, 0, 0, 64, 64, 64, 64);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        String face = Integer.toString(rolling ? tumbleFace : roll.natural());
        int faceColor = rolling || roll.natural() != 20 && roll.natural() != 1 ? 0xFFFFFF
                : roll.natural() == 20 ? 0xFFE066 : 0x202020;
        text.drawWithShadow(matrices, face, -text.getWidth(face) / 2.0f + 0.5f, -2.0f, faceColor | alpha);
        matrices.pop();

        int textLeft = left + DIE_SIZE + 6;
        text.drawWithShadow(matrices, title, textLeft, top + 1, 0xFFFFFF | alpha);
        text.drawWithShadow(matrices, sum, textLeft, top + 12, 0xD0D0D0 | alpha);
        if (!rolling) {
            text.drawWithShadow(matrices, outcome, textLeft, top + 23, color(roll.outcome()) | alpha);
        }
        RenderSystem.disableBlend();
    }
}
