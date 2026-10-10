package mattonfire.dnd.classes.Client.Hud;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Client.DndClassesClient;
import mattonfire.dnd.classes.Downed.Downed;
import mattonfire.dnd.classes.Downed.DownedHudSync;
import mattonfire.dnd.classes.Downed.Stabilise;
import mattonfire.dnd.classes.Registry.ModSounds;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * What being Downed looks like for the local player, and for a player helping one:
 * <ul>
 * <li>a red vignette that pulses with a heartbeat sound (faster with each fail, slow once stable)</li>
 * <li>a panel above the crosshair: "DOWNED" (or "STABLE"), three pass gems and three fail skulls, "Next save
 * in 4 s" (or "Standing up in 22 s"), and "Hold Z to give up" (a bar while holding it)</li>
 * <li>a stabilise progress bar on both players' panels while someone holds use on a Downed player</li>
 * <li>the hearts row drawn empty ({@code DownedHudMixin})</li>
 * </ul>
 * The death save rolls themselves show on {@link DiceRollHud} below the crosshair, which drops every other
 * main-panel roll while Downed. Everything hides with F1. Also spawns the red particles over Downed players
 * outside your party (party members get an outline instead, see {@code DownedOutlineMixin}).
 * <p>
 * Data: {@link DownedHudSync#S2C_DOWNED_HUD} every few ticks; countdowns run on locally in between.
 */
public final class DownedHud {
    private static final Identifier PIPS = new Identifier(DnDClasses.MOD_ID, "textures/gui/death_saves.png");
    private static final Identifier VIGNETTE = new Identifier("textures/misc/vignette.png");
    private static final int PIP = 9;
    private static final int LINE = 11;
    private static final int BAR_WIDTH = 90;
    /** The packet comes every 5 ticks: anything older than this is from a state that has ended. */
    private static final int STALE_TICKS = 30;
    /** Party members are outlined this far away; everyone else gets particles out to the same range. */
    public static final double ALLY_RANGE = 64.0D;

    private static int flags;
    private static int passes;
    private static int fails;
    private static int ticksLeft;
    private static int giveUpHeld;
    private static String helperName = "";
    private static int helperTicks;
    private static String targetName = "";
    private static boolean targetStable;
    private static int helpingTicks;
    private static int sincePacket = STALE_TICKS;

    private static int nextBeat;
    /** Client ticks since the last heartbeat, for the vignette pulse. */
    private static int sinceBeat = 100;

    private DownedHud() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(DownedHudSync.S2C_DOWNED_HUD, (client, handler, buf, sender) -> {
            int newFlags = buf.readByte();
            int newPasses = 0;
            int newFails = 0;
            int newTicksLeft = 0;
            int newGiveUp = 0;
            String newHelper = "";
            int newHelperTicks = 0;
            String newTarget = "";
            boolean newTargetStable = false;
            int newHelpingTicks = 0;
            if ((newFlags & DownedHudSync.FLAG_DOWNED) != 0) {
                newPasses = buf.readByte();
                newFails = buf.readByte();
                newTicksLeft = buf.readVarInt();
                newGiveUp = buf.readVarInt();
            }
            if ((newFlags & DownedHudSync.FLAG_HELPED) != 0) {
                newHelper = buf.readString();
                newHelperTicks = buf.readVarInt();
            }
            if ((newFlags & DownedHudSync.FLAG_HELPING) != 0) {
                newTarget = buf.readString();
                newTargetStable = buf.readBoolean();
                newHelpingTicks = buf.readVarInt();
            }
            int f = newFlags, p = newPasses, fl = newFails, t = newTicksLeft, g = newGiveUp, ht = newHelperTicks,
                    hg = newHelpingTicks;
            String h = newHelper, tg = newTarget;
            boolean ts = newTargetStable;
            client.execute(() -> {
                flags = f;
                passes = p;
                fails = fl;
                ticksLeft = t;
                giveUpHeld = g;
                helperName = h;
                helperTicks = ht;
                targetName = tg;
                targetStable = ts;
                helpingTicks = hg;
                sincePacket = 0;
            });
        });
        ClientTickEvents.END_CLIENT_TICK.register(DownedHud::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            flags = 0;
            sincePacket = STALE_TICKS;
        }));
        HudRenderCallback.EVENT.register((matrices, tickDelta) -> render(matrices, tickDelta));
    }

    /** The local player is Downed (synced bit) and the server has sent the tally. */
    public static boolean showing() {
        MinecraftClient client = MinecraftClient.getInstance();
        return Downed.is(client.player) && (flags & DownedHudSync.FLAG_DOWNED) != 0 && sincePacket < STALE_TICKS;
    }

    private static boolean helping() {
        return (flags & DownedHudSync.FLAG_HELPING) != 0 && sincePacket < STALE_TICKS;
    }

    private static boolean stable() {
        return (flags & DownedHudSync.FLAG_STABLE) != 0;
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null || client.isPaused()) {
            return;
        }
        sincePacket++;
        if (ticksLeft > 0) {
            ticksLeft--;
        }
        if ((flags & DownedHudSync.FLAG_HELPED) != 0 && helperTicks < Stabilise.HOLD_TICKS) {
            helperTicks++;
        }
        if (helping() && helpingTicks < Stabilise.HOLD_TICKS) {
            helpingTicks++;
        }
        if ((flags & DownedHudSync.FLAG_DOWNED) != 0 && giveUpHeld > 0 && giveUpHeld < Downed.GIVE_UP_TICKS) {
            giveUpHeld++;
        }

        sinceBeat++;
        if (Downed.is(client.player)) {
            if (--nextBeat <= 0) {
                nextBeat = stable() ? 50 : Math.max(18, 34 - 6 * fails);
                sinceBeat = 0;
                client.getSoundManager().play(PositionedSoundInstance.master(ModSounds.DOWNED_HEARTBEAT, 1.0F,
                        stable() ? 0.4F : 0.7F));
            }
        } else {
            nextBeat = 0;
        }

        if (client.player.age % 20 == 0) {
            markStrangers(client);
        }
    }

    /** Red particles over Downed players who aren't in your party (party members are outlined). */
    private static void markStrangers(MinecraftClient client) {
        for (PlayerEntity other : client.world.getPlayers()) {
            if (other == client.player || !Downed.is(other) || PartyHud.isMember(other.getUuid())
                    || other.squaredDistanceTo(client.player) > ALLY_RANGE * ALLY_RANGE) {
                continue;
            }
            for (int i = 0; i < 3; i++) {
                client.world.addParticle(ParticleTypes.DAMAGE_INDICATOR,
                        other.getX() + (client.world.random.nextDouble() - 0.5D) * 0.8D, other.getY() + 0.5D,
                        other.getZ() + (client.world.random.nextDouble() - 0.5D) * 0.8D, 0.0D, 0.1D, 0.0D);
            }
        }
    }

    /** Outline colour of a Downed party member (red), or a stable one (gold). */
    public static int outlineColor(PlayerEntity player) {
        return Downed.isStable(player) ? 0xFFC030 : 0xFF2020;
    }

    /** Whether this entity is a Downed party member within {@value #ALLY_RANGE} blocks: drawn outlined through walls. */
    public static boolean outlined(Entity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        return entity instanceof PlayerEntity player && entity.world.isClient && client.player != null
                && player != client.player && Downed.is(player) && PartyHud.isMember(player.getUuid())
                && player.squaredDistanceTo(client.player) <= ALLY_RANGE * ALLY_RANGE;
    }

    /** Whether the hearts row should be drawn empty ({@code DownedHudMixin}). */
    public static boolean emptyHearts() {
        return Downed.is(MinecraftClient.getInstance().player);
    }

    /**
     * The red vignette, drawn first so the rest of the HUD sits on top. Same blend as vanilla's world-border
     * warning: the texture darkens green and blue towards the screen edges, leaving red.
     */
    public static void renderVignette(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden || !Downed.is(client.player)) {
            return;
        }
        float beat = Math.max(0.0F, 1.0F - (sinceBeat + tickDelta) / 8.0F);
        float strength = MathHelper.clamp((stable() ? 0.35F : 0.6F + 0.08F * fails) + 0.25F * beat, 0.0F, 1.0F);
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE_MINUS_SRC_COLOR,
                GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        RenderSystem.setShaderColor(0.0F, strength, strength, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, VIGNETTE);
        DrawableHelper.drawTexture(matrices, 0, 0, 0.0F, 0.0F, width, height, width, height);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static void render(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) {
            return;
        }
        if (showing()) {
            renderDowned(client, matrices);
        } else if (helping()) {
            renderHelping(client, matrices);
        }
    }

    /** The Downed player's panel, above the crosshair (the d20 panel is below it). */
    private static void renderDowned(MinecraftClient client, MatrixStack matrices) {
        TextRenderer text = client.textRenderer;
        boolean stable = stable();
        Text title = Text.translatable(stable ? "hud.dndclasses.downed.stable" : "hud.dndclasses.downed.title")
                .formatted(stable ? Formatting.YELLOW : Formatting.RED, Formatting.BOLD);
        int seconds = (ticksLeft + 19) / 20;
        Text timer = Text.translatable(stable ? "hud.dndclasses.downed.standing_up" : "hud.dndclasses.downed.next_save",
                seconds).formatted(Formatting.GRAY);
        boolean givingUp = giveUpHeld > 0;
        Text giveUp = givingUp ? Text.translatable("hud.dndclasses.downed.giving_up").formatted(Formatting.DARK_RED)
                : Text.translatable("hud.dndclasses.downed.give_up", giveUpKey()).formatted(Formatting.DARK_GRAY);
        boolean helped = (flags & DownedHudSync.FLAG_HELPED) != 0;
        Text help = helped ? Text.translatable(stable ? "hud.dndclasses.downed.helped_up" : "hud.dndclasses.downed.helped",
                helperName).formatted(Formatting.YELLOW) : null;

        int pipsWidth = 6 * (PIP + 1) + 4;
        int width = Math.max(BAR_WIDTH, Math.max(text.getWidth(title), Math.max(pipsWidth,
                Math.max(text.getWidth(timer), text.getWidth(giveUp)))));
        if (help != null) {
            width = Math.max(width, text.getWidth(help));
        }
        int height = LINE + (stable ? 0 : PIP + 3) + LINE + LINE + (givingUp ? 4 : 0) + (help != null ? LINE + 4 : 0);
        int screenWidth = client.getWindow().getScaledWidth();
        int centre = screenWidth / 2;
        int left = centre - width / 2;
        // Above the crosshair, clear of the boss bar at the top and the d20 panel below
        int top = Math.max(24, client.getWindow().getScaledHeight() / 2 - 16 - height);
        DrawableHelper.fill(matrices, left - 4, top - 3, left + width + 4, top + height + 1, 0x90000000);

        int y = top;
        drawCentred(matrices, text, title, centre, y);
        y += LINE;
        if (!stable) {
            int x = centre - pipsWidth / 2;
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, PIPS);
            for (int i = 0; i < 3; i++) {
                DrawableHelper.drawTexture(matrices, x + i * (PIP + 1), y, i < passes ? 0 : PIP, 0, PIP, PIP,
                        4 * PIP, PIP);
            }
            x += 3 * (PIP + 1) + 4;
            for (int i = 0; i < 3; i++) {
                DrawableHelper.drawTexture(matrices, x + i * (PIP + 1), y, i < fails ? 2 * PIP : 3 * PIP, 0, PIP, PIP,
                        4 * PIP, PIP);
            }
            RenderSystem.disableBlend();
            y += PIP + 3;
        }
        drawCentred(matrices, text, timer, centre, y);
        y += LINE;
        drawCentred(matrices, text, giveUp, centre, y);
        y += LINE;
        if (givingUp) {
            bar(matrices, centre, y - 1, giveUpHeld, Downed.GIVE_UP_TICKS, 0xFF801010);
            y += 4;
        }
        if (help != null) {
            drawCentred(matrices, text, help, centre, y);
            bar(matrices, centre, y + LINE - 1, helperTicks, Stabilise.HOLD_TICKS, 0xFF40C040);
        }
    }

    /** A helper's progress on the Downed player they're holding use on, above the crosshair. */
    private static void renderHelping(MinecraftClient client, MatrixStack matrices) {
        TextRenderer text = client.textRenderer;
        Text label = Text.translatable(targetStable ? "hud.dndclasses.downed.helping_up" : "hud.dndclasses.downed.helping",
                targetName).formatted(Formatting.YELLOW);
        int width = Math.max(BAR_WIDTH, text.getWidth(label));
        int centre = client.getWindow().getScaledWidth() / 2;
        int top = client.getWindow().getScaledHeight() / 2 - 16 - (LINE + 4);
        DrawableHelper.fill(matrices, centre - width / 2 - 4, top - 3, centre + width / 2 + 4, top + LINE + 5,
                0x90000000);
        drawCentred(matrices, text, label, centre, top);
        bar(matrices, centre, top + LINE - 1, helpingTicks, Stabilise.HOLD_TICKS, 0xFF40C040);
    }

    private static void drawCentred(MatrixStack matrices, TextRenderer text, Text line, int centre, int y) {
        text.drawWithShadow(matrices, line, centre - text.getWidth(line) / 2.0F, y, 0xFFFFFFFF);
    }

    private static void bar(MatrixStack matrices, int centre, int y, int value, int total, int color) {
        int left = centre - BAR_WIDTH / 2;
        int filled = BAR_WIDTH * MathHelper.clamp(value, 0, total) / total;
        DrawableHelper.fill(matrices, left, y, left + BAR_WIDTH, y + 3, 0xFF303030);
        DrawableHelper.fill(matrices, left, y, left + filled, y + 3, color);
    }

    private static Text giveUpKey() {
        return DndClassesClient.POWER_UP_KEY != null ? DndClassesClient.POWER_UP_KEY.getBoundKeyLocalizedText()
                : Text.literal("Z");
    }
}
