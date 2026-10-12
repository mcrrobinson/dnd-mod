package mattonfire.dnd.classes.Client.Hud;

import com.mojang.blaze3d.systems.RenderSystem;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Abilities.Advantage;
import mattonfire.dnd.classes.Abilities.RollKind;
import mattonfire.dnd.classes.Config.SaveRollsMode;
import mattonfire.dnd.classes.Registry.ModSounds;
import mattonfire.dnd.classes.SkillChecks.D20;
import io.netty.buffer.Unpooled;
import mattonfire.dnd.dm.DmRollCalls;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.glfw.GLFW;

/**
 * Shows a d20 skill check just below the crosshair: the die tumbles through numbers for a moment
 * (with a rattle), then lands on the natural roll and shows the sum, the DC and what came of it,
 * with a sound for the outcome. With advantage or disadvantage the dropped die is shown greyed after
 * the kept one. Secret rolls ({@link D20#FLAG_SECRET}) show the total but not the DC or outcome.
 * Saving throws go to the compact {@link SaveLaneHud} instead, so they never replace a check here (with
 * {@code saveRolls: full} they use this panel, but only when it isn't showing a check).
 *
 * A DM roll call ({@link DmRollCalls}) shows a prompt here first: the idle die, "DM calls for a Dexterity Save
 * (DC 14)" and "[Y] Roll" with a countdown (the roll key defaults to Y: R is Identity's ability key, and only one binding fires per key). The roll key sends the answer; the server rolls and the result
 * replaces the prompt. While a prompt is up it takes the panel over any other roll.
 */
public final class DiceRollHud {
    private static final Identifier D20_TEXTURE = new Identifier(DnDClasses.MOD_ID, "textures/gui/d20.png");
    private static final int ROLL_TICKS = 14;
    private static final int SHOW_TICKS = 60;
    private static final int FADE_TICKS = 10;
    private static final int DIE_SIZE = 32;
    /** The dropped die of an advantage/disadvantage roll, drawn greyed beside the kept one. */
    private static final int SMALL_DIE_SIZE = 22;

    public static final KeyBinding ROLL_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.dnd-classes.roll", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Y, "category.dnd-classes.dnd-classes"));

    /** A DM roll call waiting for the roll key: what to roll, who called it, DC (0 hidden), mode. */
    private record Prompt(Text label, Text caller, int dc, Advantage mode) {
    }

    private static Prompt prompt;
    /** Ticks left on the prompt's timer (the server rolls for us at 0). */
    private static int promptTicks;
    /** The key was pressed; waiting for the server's roll. */
    private static boolean promptAnswered;
    private static int promptAge;

    private static D20.Roll roll;
    private static Text detail;
    private static int age;
    private static int tumbleFace = 1;
    private static final Random RANDOM = Random.create();

    private DiceRollHud() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(D20.S2C_ROLL, (client, handler, buf, sender) -> {
            D20.Roll received = D20.Roll.read(buf);
            Text receivedDetail = buf.readText();
            int count = buf.isReadable() ? buf.readVarInt() : 1;
            if (received.display() == D20.Display.SILENT) {
                return;
            }
            if (received.display() == D20.Display.SAVE_LANE) {
                client.execute(() -> {
                    if (SaveRollsMode.current() == SaveRollsMode.FULL
                            && (roll == null || roll.display() == D20.Display.SAVE_LANE)) {
                        start(client, received, receivedDetail);
                    } else {
                        SaveLaneHud.accept(client, received, receivedDetail, count);
                    }
                });
                return;
            }
            client.execute(() -> {
                // While Downed only death saves (and Last Stand) take the main panel; other popups would hide them
                if (DownedHud.showing() && received.kind() != RollKind.DEATH) {
                    return;
                }
                start(client, received, receivedDetail);
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(DmRollCalls.S2C_PROMPT, (client, handler, buf, sender) -> {
            Prompt received = new Prompt(buf.readText(), buf.readText(), buf.readVarInt(),
                    buf.readEnumConstant(Advantage.class));
            int ticks = buf.readVarInt();
            client.execute(() -> {
                if (ticks <= 0) {
                    prompt = null;
                    return;
                }
                prompt = received;
                promptTicks = ticks;
                promptAnswered = false;
                promptAge = 0;
                play(client, ModSounds.DICE_ROLL, 0.7f);
            });
        });
        ClientTickEvents.END_CLIENT_TICK.register(DiceRollHud::tick);
        // Drop a roll still animating when leaving the world
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(DiceRollHud::reset));
        HudRenderCallback.EVENT.register(DiceRollHud::render);
    }

    private static void start(MinecraftClient client, D20.Roll newRoll, Text newDetail) {
        roll = newRoll;
        detail = newDetail;
        age = 0;
        play(client, ModSounds.DICE_ROLL, 1.0f);
    }

    private static void reset() {
        roll = null;
        detail = null;
        age = 0;
        prompt = null;
    }

    /** Whether a DM roll call is waiting for this player's roll key. */
    public static boolean prompting() {
        return prompt != null && !promptAnswered;
    }

    private static void tick(MinecraftClient client) {
        while (ROLL_KEY.wasPressed()) {
            if (prompting() && client.player != null) {
                promptAnswered = true;
                ClientPlayNetworking.send(DmRollCalls.C2S_ANSWER, new PacketByteBuf(Unpooled.buffer()));
            }
        }
        if (prompt != null && !client.isPaused()) {
            promptAge++;
            // The server rolls for us when its timer runs out; this only drives the countdown
            if (promptTicks > 0) {
                promptTicks--;
            }
            if (promptTicks <= 0 && promptAge > DmRollCalls.TIMEOUT_TICKS + 100) {
                prompt = null; // never heard back (left the server's view): drop it
            }
            if (promptAnswered && promptAge % 2 == 0) {
                tumbleFace = 1 + RANDOM.nextInt(20);
            }
        }
        if (roll == null || client.isPaused()) {
            return;
        }
        if (client.world == null) {
            reset();
            return;
        }
        age++;
        if (age < ROLL_TICKS && age % 2 == 0) {
            int next = 1 + RANDOM.nextInt(19);
            tumbleFace = next >= tumbleFace ? next + 1 : next; // never the same face twice
        }
        if (age == ROLL_TICKS && !roll.secret()) {
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

    /** Draws the current roll; also called by {@link DialogueScreen} so rolls show over its panel. */
    /** How long the Lucky clover flashes after the die settles. */
    private static final int LUCKY_FLASH_TICKS = 12;

    static void render(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        if (prompt != null) {
            renderPrompt(client, matrices, tickDelta);
            return;
        }
        if (roll == null) {
            return;
        }
        TextRenderer text = client.textRenderer;
        boolean rolling = age < ROLL_TICKS;
        float life = age + tickDelta;
        float fade = MathHelper.clamp((ROLL_TICKS + SHOW_TICKS - life) / FADE_TICKS, 0.0f, 1.0f);
        int alpha = Math.max(8, (int) (fade * 255)) << 24;

        MutableText title = roll.label().copy().formatted(Formatting.BOLD);
        for (D20.Bonus bonus : roll.bonuses()) {
            title.append(Text.literal("  " + (bonus.amount() < 0 ? "" : "+") + bonus.amount() + " ")
                    .append(bonus.label()).formatted(Formatting.GRAY).styled(st -> st.withBold(false)));
        }
        Text sum;
        Text outcome;
        if (rolling) {
            sum = Text.translatable("skill.dndclasses.rolling");
            outcome = Text.empty();
        } else {
            MutableText line = Text.literal(Integer.toString(roll.natural()));
            if (roll.natural2() > 0) {
                line.append(Text.literal(" (" + roll.natural2() + ")").formatted(Formatting.DARK_GRAY))
                        .append(Text.translatable(roll.mode() == Advantage.ADVANTAGE ? "skill.dndclasses.advantage"
                                : "skill.dndclasses.disadvantage").formatted(Formatting.GRAY));
            }
            line.append(" " + (roll.modifier() < 0 ? "- " : "+ ") + Math.abs(roll.modifier()) + " = " + roll.total());
            if (roll.dc() > 0 && !roll.secret()) {
                line.append(Text.translatable("skill.dndclasses.vs_dc", roll.dc()));
            }
            if (roll.rerolled() > 0) {
                // Halfling Lucky: a clover that flashes as the die settles
                boolean flash = age - ROLL_TICKS < LUCKY_FLASH_TICKS && (age / 3) % 2 == 0;
                line.append(Text.translatable("skill.dndclasses.lucky")
                        .formatted(flash ? Formatting.WHITE : Formatting.GREEN));
            }
            sum = line;
            outcome = roll.secret() ? detail.copy()
                    : Text.translatable(roll.outcome().translationKey()).append(Text.literal(" - ").append(detail));
        }

        int textWidth = Math.max(text.getWidth(title), Math.max(text.getWidth(sum), text.getWidth(outcome)));
        boolean twoDice = roll.natural2() > 0;
        int diceWidth = twoDice ? DIE_SIZE + 2 + SMALL_DIE_SIZE : DIE_SIZE;
        int width = diceWidth + 6 + textWidth;
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

        if (twoDice) {
            // The dropped die: smaller and greyed, beside the kept one
            matrices.push();
            matrices.translate(left + DIE_SIZE + 2 + SMALL_DIE_SIZE / 2.0f, top + DIE_SIZE / 2.0f + 3, 0);
            if (rolling) {
                float spin = (ROLL_TICKS - life) / ROLL_TICKS;
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-spin * spin * 480.0f));
            }
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            RenderSystem.setShaderColor(0.45f, 0.45f, 0.45f, fade * 0.8f);
            RenderSystem.setShaderTexture(0, D20_TEXTURE);
            DrawableHelper.drawTexture(matrices, -SMALL_DIE_SIZE / 2, -SMALL_DIE_SIZE / 2, SMALL_DIE_SIZE,
                    SMALL_DIE_SIZE, 0, 0, 64, 64, 64, 64);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            String face2 = Integer.toString(rolling ? 21 - tumbleFace : roll.natural2());
            text.drawWithShadow(matrices, face2, -text.getWidth(face2) / 2.0f + 0.5f, -3.5f, 0x909090 | alpha);
            matrices.pop();
        }

        int textLeft = left + diceWidth + 6;
        text.drawWithShadow(matrices, title, textLeft, top + 1, 0xFFFFFF | alpha);
        text.drawWithShadow(matrices, sum, textLeft, top + 12, 0xD0D0D0 | alpha);
        if (!rolling) {
            text.drawWithShadow(matrices, outcome, textLeft, top + 23,
                    (roll.secret() ? 0xD0D0D0 : color(roll.outcome())) | alpha);
        }
        RenderSystem.disableBlend();
    }

    /** The roll call prompt: the idle die, who calls for what, and "[Y] Roll" with the seconds left. */
    private static void renderPrompt(MinecraftClient client, MatrixStack matrices, float tickDelta) {
        TextRenderer text = client.textRenderer;
        float life = promptAge + tickDelta;
        MutableText title = prompt.dc() > 0
                ? Text.translatable("dm.dndclasses.roll.prompt", prompt.caller(), prompt.label(), prompt.dc())
                : Text.translatable("dm.dndclasses.roll.prompt_secret", prompt.caller(), prompt.label());
        title.formatted(Formatting.BOLD);
        MutableText modeLine = prompt.mode() == Advantage.NORMAL ? Text.empty()
                : Text.translatable(prompt.mode() == Advantage.ADVANTAGE ? "dm.dndclasses.roll.prompt_advantage"
                        : "dm.dndclasses.roll.prompt_disadvantage").formatted(Formatting.GRAY);
        Text action;
        if (promptAnswered) {
            action = Text.translatable("skill.dndclasses.rolling");
        } else {
            int seconds = (promptTicks + 19) / 20;
            boolean flash = seconds <= 5 && (promptAge / 5) % 2 == 0;
            action = Text.translatable("dm.dndclasses.roll.prompt_key",
                    ROLL_KEY.getBoundKeyLocalizedText().getString().toUpperCase(java.util.Locale.ROOT))
                    .formatted(Formatting.YELLOW)
                    .append(Text.literal("  " + seconds + "s").formatted(flash ? Formatting.RED : Formatting.GRAY));
        }
        Text second = modeLine.getString().isEmpty() ? action : modeLine;
        Text third = modeLine.getString().isEmpty() ? Text.empty() : action;
        boolean twoDice = prompt.mode() != Advantage.NORMAL;
        int diceWidth = twoDice ? DIE_SIZE + 2 + SMALL_DIE_SIZE : DIE_SIZE;
        int textWidth = Math.max(text.getWidth(title), Math.max(text.getWidth(second), text.getWidth(third)));
        int width = diceWidth + 6 + textWidth;
        int left = (client.getWindow().getScaledWidth() - width) / 2;
        int top = client.getWindow().getScaledHeight() / 2 + 20;
        DrawableHelper.fill(matrices, left - 4, top - 3, left + width + 4, top + DIE_SIZE + 3, 0x90000000);
        // A soft gold edge marks it as waiting on you
        int edge = promptAnswered ? 0x60FFFFFF : 0xC0FFD23F;
        DrawableHelper.fill(matrices, left - 4, top - 3, left - 3, top + DIE_SIZE + 3, edge);

        RenderSystem.enableBlend();
        for (int die = 0; die < (twoDice ? 2 : 1); die++) {
            // Set every time: drawing the face text switches the shader
            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            RenderSystem.setShaderTexture(0, D20_TEXTURE);
            int size = die == 0 ? DIE_SIZE : SMALL_DIE_SIZE;
            float cx = die == 0 ? left + DIE_SIZE / 2.0f : left + DIE_SIZE + 2 + SMALL_DIE_SIZE / 2.0f;
            matrices.push();
            matrices.translate(cx, top + DIE_SIZE / 2.0f + (die == 0 ? 0 : 3), 0);
            float angle = promptAnswered ? life * 40.0f : 6.0f * MathHelper.sin(life * 0.15f + die);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(die == 0 ? angle : -angle));
            float shade = die == 0 ? 1.0f : 0.7f;
            RenderSystem.setShaderColor(shade, shade, shade, 1.0f);
            DrawableHelper.drawTexture(matrices, -size / 2, -size / 2, size, size, 0, 0, 64, 64, 64, 64);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            String face = promptAnswered ? Integer.toString(die == 0 ? tumbleFace : 21 - tumbleFace) : "?";
            text.drawWithShadow(matrices, face, -text.getWidth(face) / 2.0f + 0.5f, die == 0 ? -2.0f : -3.5f,
                    die == 0 ? 0xFFFFFFFF : 0xFFB0B0B0);
            matrices.pop();
        }
        int textLeft = left + diceWidth + 6;
        text.drawWithShadow(matrices, title, textLeft, top + 1, 0xFFFFD23F);
        text.drawWithShadow(matrices, second, textLeft, top + 12, 0xFFD0D0D0);
        text.drawWithShadow(matrices, third, textLeft, top + 23, 0xFFD0D0D0);
        RenderSystem.disableBlend();
    }
}
