package mattonfire.dnd.classes.Client;

import org.lwjgl.glfw.GLFW;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.SkillChecks.Perception;
import mattonfire.dnd.entity.DragonPart;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * The Search/Study key (V): sends the creature under the crosshair (up to {@link Perception#STUDY_RANGE}
 * blocks, not through walls) or none; the server decides between a Study and a Search ({@link Perception}).
 */
@Environment(EnvType.CLIENT)
public final class SearchKeyClient {
    public static final KeyBinding STUDY_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.dnd-classes.study", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.dnd-classes.dnd-classes"));

    private SearchKeyClient() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (STUDY_KEY.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    press(client);
                }
            }
        });
    }

    /** Sends a key press (also used by DevScript-style callers). */
    public static void press(MinecraftClient client) {
        Entity target = crosshairCreature(client);
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(target == null ? -1 : target.getId());
        ClientPlayNetworking.send(Perception.C2S_STUDY, buf);
    }

    private static Entity crosshairCreature(MinecraftClient client) {
        Entity camera = client.getCameraEntity();
        if (camera == null || client.world == null) {
            return null;
        }
        double range = Perception.STUDY_RANGE;
        Vec3d eye = camera.getCameraPosVec(1.0F);
        Vec3d look = camera.getRotationVec(1.0F);
        HitResult block = camera.raycast(range, 1.0F, false);
        if (block.getType() != HitResult.Type.MISS) {
            range = block.getPos().distanceTo(eye);
        }
        Vec3d end = eye.add(look.multiply(range));
        Box box = camera.getBoundingBox().stretch(look.multiply(range)).expand(1.0);
        EntityHitResult hit = ProjectileUtil.raycast(camera, eye, end, box,
                e -> (e instanceof LivingEntity || e instanceof DragonPart) && !e.isSpectator() && e.isAlive(),
                range * range);
        if (hit == null) {
            return null;
        }
        // A big dragon's hit shapes stand for the dragon
        return hit.getEntity() instanceof DragonPart part ? part.owner : hit.getEntity();
    }
}
