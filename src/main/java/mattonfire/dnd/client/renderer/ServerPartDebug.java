package mattonfire.dnd.client.renderer;

import java.util.List;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.DragonPart;
import mattonfire.dnd.entity.MultipartDragon;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.Nullable;

/**
 * Dev-only check that the server's dragon part shapes match the animated model: in single player it
 * reads the integrated server's copy of a dragon so the hitbox overlay can draw its shapes too (red,
 * next to the client's green), and logs how far apart the two sets are. Off unless a DevScript
 * ({@code serverhitboxes on|measure}) or {@code -Ddndclasses.serverHitboxes=true} turns it on.
 */
@Environment(EnvType.CLIENT)
public final class ServerPartDebug {
    public static boolean enabled = Boolean.getBoolean("dndclasses.serverHitboxes");

    private ServerPartDebug() {
    }

    /** The integrated server's copy of {@code clientEntity}, or null (multiplayer, not loaded). */
    @Nullable
    public static Entity serverCopy(Entity clientEntity) {
        IntegratedServer server = MinecraftClient.getInstance().getServer();
        if (server == null) {
            return null;
        }
        ServerWorld world = server.getWorld(clientEntity.world.getRegistryKey());
        return world == null ? null : world.getEntityById(clientEntity.getId());
    }

    /** Logs, for every dragon the client has, how far each server part is from the client part. */
    public static void measure(MinecraftClient client) {
        if (client.world == null) {
            return;
        }
        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof MultipartDragon dragon) || !(serverCopy(entity) instanceof MultipartDragon serverDragon)) {
                continue;
            }
            DragonPart[] clientParts = dragon.getParts();
            DragonPart[] serverParts = serverDragon.getParts();
            double sum = 0, max = 0;
            String worst = "";
            int count = 0;
            for (int i = 0; i < Math.min(clientParts.length, serverParts.length); i++) {
                Box c = union(clientParts[i].getShapes());
                Box s = union(serverParts[i].getShapes());
                if (c == null || s == null) {
                    continue;
                }
                double d = c.getCenter().distanceTo(s.getCenter());
                sum += d;
                count++;
                if (d > max) {
                    max = d;
                    worst = clientParts[i].name;
                }
            }
            Entity serverEntity = (Entity) serverDragon;
            float clientYaw = entity instanceof net.minecraft.entity.LivingEntity living ? living.bodyYaw : entity.getYaw();
            float serverYaw = serverEntity instanceof net.minecraft.entity.LivingEntity living ? living.bodyYaw : serverEntity.getYaw();
            DnDClasses.LOGGER.info("[ServerPartDebug] {} client time {} server time {}: part centres {} apart on average, worst {} ({} blocks);"
                            + " dragon {} blocks apart, body yaw {} vs {}",
                    entity.getType().getUntranslatedName(), client.world.getTime(), serverEntity.world.getTime(),
                    String.format("%.3f", count == 0 ? 0 : sum / count), worst, String.format("%.3f", max),
                    String.format("%.3f", entity.getPos().distanceTo(serverEntity.getPos())),
                    String.format("%.1f", clientYaw), String.format("%.1f", serverYaw));
        }
    }

    @Nullable
    private static Box union(List<Box> shapes) {
        Box box = null;
        for (Box shape : shapes) {
            box = box == null ? shape : box.union(shape);
        }
        return box;
    }
}
