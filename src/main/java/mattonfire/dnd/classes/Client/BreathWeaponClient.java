package mattonfire.dnd.classes.Client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.systems.RenderSystem;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Race.BreathWeapon;
import mattonfire.dnd.classes.Race.DragonAncestry;
import mattonfire.dnd.particle.ModParticles;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

/**
 * Client side of the Dragonborn Breath Weapon: the racial key (R), the cone of flame, frost or sparks drawn
 * for every breath nearby, and the cooldown icon right of the mana bar.
 */
public final class BreathWeaponClient {
    /** Ticks a breath keeps puffing particles. */
    private static final int PUFF_TICKS = 8;
    private static final double CONE_TAN = Math.tan(Math.toRadians(BreathWeapon.HALF_ANGLE_DEG));
    private static final Identifier[] ICONS = {
            null,
            new Identifier(DnDClasses.MOD_ID, "textures/power/breath_ember.png"),
            new Identifier(DnDClasses.MOD_ID, "textures/power/breath_frost.png"),
            new Identifier(DnDClasses.MOD_ID, "textures/power/breath_storm.png"),
    };

    public static KeyBinding RACIAL_KEY;

    private record Puff(ClientWorld world, Vec3d origin, Vec3d aim, DragonAncestry ancestry, int[] ticksLeft) {
    }

    private static final List<Puff> PUFFS = new ArrayList<>();

    // The HUD state from S2C_COOLDOWN
    private static boolean usable;
    private static DragonAncestry ancestry = DragonAncestry.NONE;
    private static long readyAt;
    private static int total = BreathWeapon.COOLDOWN_TICKS;

    private BreathWeaponClient() {
    }

    public static void register() {
        RACIAL_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.dnd-classes.racial",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.dnd-classes.dnd-classes"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (RACIAL_KEY.wasPressed()) {
                if (client.player != null && client.getNetworkHandler() != null) {
                    ClientPlayNetworking.send(BreathWeapon.C2S_RACIAL_POWER, PacketByteBufs.empty());
                }
            }
            tickPuffs(client);
        });

        ClientPlayNetworking.registerGlobalReceiver(BreathWeapon.S2C_BREATH, (client, handler, buf, sender) -> {
            buf.readVarInt(); // breather id, for later (mouth position)
            DragonAncestry kind = ancestryOf(buf.readVarInt());
            Vec3d origin = new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
            Vec3d aim = new Vec3d(buf.readFloat(), buf.readFloat(), buf.readFloat()).normalize();
            client.execute(() -> {
                if (client.world != null) {
                    // Start a little ahead of the face, so the breather's own camera isn't inside the flames
                    PUFFS.add(new Puff(client.world, origin.add(aim.multiply(0.6)).add(0, -0.15, 0), aim, kind,
                            new int[] { PUFF_TICKS }));
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(BreathWeapon.S2C_COOLDOWN, (client, handler, buf, sender) -> {
            boolean canBreathe = buf.readBoolean();
            DragonAncestry kind = ancestryOf(buf.readVarInt());
            int left = buf.readVarInt();
            int length = buf.readVarInt();
            client.execute(() -> {
                usable = canBreathe;
                ancestry = kind;
                total = Math.max(1, length);
                readyAt = client.world == null ? 0 : client.world.getTime() + left;
            });
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            PUFFS.clear();
            usable = false;
            ancestry = DragonAncestry.NONE;
            readyAt = 0;
        }));
    }

    private static DragonAncestry ancestryOf(int id) {
        try {
            return DragonAncestry.fromValue(id);
        } catch (IllegalArgumentException e) {
            return DragonAncestry.NONE;
        }
    }

    private static void tickPuffs(MinecraftClient client) {
        if (PUFFS.isEmpty()) {
            return;
        }
        for (Iterator<Puff> it = PUFFS.iterator(); it.hasNext();) {
            Puff puff = it.next();
            if (puff.world != client.world || puff.ticksLeft[0]-- <= 0) {
                it.remove();
                continue;
            }
            spawn(puff);
        }
    }

    /** One tick of a breath's particles, styled like the dragons' FireBreath and FrostBreath. */
    private static void spawn(Puff puff) {
        ClientWorld world = puff.world;
        Random random = world.random;
        Vec3d aim = puff.aim;
        Vec3d side = aim.crossProduct(Math.abs(aim.y) > 0.9 ? new Vec3d(1, 0, 0) : new Vec3d(0, 1, 0)).normalize();
        Vec3d up = side.crossProduct(aim);
        // DRAGON_FLAME/DRAGON_FROST slow down by DRAG each tick for AGE ticks: this many blocks per unit speed
        double reach = (1 - Math.pow(ModParticles.DRAGON_FLAME_DRAG, ModParticles.DRAGON_FLAME_AGE))
                / (1 - ModParticles.DRAGON_FLAME_DRAG);
        double length = BreathWeapon.RANGE - 0.6;
        for (int i = 0; i < 18; i++) {
            double a = random.nextGaussian() * CONE_TAN * 0.5;
            double b = random.nextGaussian() * CONE_TAN * 0.5;
            Vec3d dir = aim.add(side.multiply(a)).add(up.multiply(b)).normalize();
            Vec3d pos = puff.origin;
            switch (puff.ancestry) {
                case FROST -> {
                    Vec3d v = dir.multiply(length / reach * (0.75 + random.nextDouble() * 0.3));
                    world.addParticle(ModParticles.DRAGON_FROST, pos.x, pos.y, pos.z, v.x, v.y, v.z);
                }
                case STORM -> {
                    // Sparks hang along the cone where they appear, so draw the whole cone each tick
                    Vec3d at = pos.add(dir.multiply(random.nextDouble() * length));
                    world.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, dir.x * 0.05, dir.y * 0.05,
                            dir.z * 0.05);
                }
                default -> {
                    Vec3d v = dir.multiply(length / reach * (0.75 + random.nextDouble() * 0.3));
                    world.addParticle(ModParticles.DRAGON_FLAME, pos.x, pos.y, pos.z, v.x, v.y, v.z);
                }
            }
        }
        // A few extras through the cone: snowflakes, end-rod glints, or loose flames
        for (int i = 0; i < 4; i++) {
            double along = length * random.nextDouble();
            double radius = 0.2 + along * CONE_TAN * 0.6;
            Vec3d at = puff.origin.add(aim.multiply(along)).add(side.multiply(random.nextGaussian() * radius))
                    .add(up.multiply(random.nextGaussian() * radius));
            switch (puff.ancestry) {
                case FROST -> world.addParticle(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, aim.x * 0.1, aim.y * 0.1,
                        aim.z * 0.1);
                case STORM -> world.addParticle(ParticleTypes.END_ROD, at.x, at.y, at.z, aim.x * 0.05,
                        aim.y * 0.05, aim.z * 0.05);
                default -> world.addParticle(ParticleTypes.FLAME, at.x, at.y, at.z, aim.x * 0.1, aim.y * 0.1 + 0.03,
                        aim.z * 0.1);
            }
        }
    }

    /**
     * The cooldown icon, right of the mana bar at the same height: the ancestry's glyph, greyed from the top
     * while recharging. Called from {@code PowerupOverlay}.
     */
    public static void renderIcon(MatrixStack matrices, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!usable || ancestry == DragonAncestry.NONE || client.world == null) {
            return;
        }
        RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, ICONS[ancestry.getValue()]);
        DrawableHelper.drawTexture(matrices, x, y, 0, 0, 9, 9, 9, 9);
        long left = readyAt - client.world.getTime();
        if (left > 0) {
            int covered = (int) Math.ceil(9.0 * Math.min(left, total) / total);
            DrawableHelper.fill(matrices, x, y, x + 9, y + covered, 0xB0000000);
        }
    }
}
