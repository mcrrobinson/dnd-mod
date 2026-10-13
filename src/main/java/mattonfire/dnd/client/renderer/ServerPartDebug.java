package mattonfire.dnd.client.renderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.DragonPart;
import mattonfire.dnd.entity.MultipartDragon;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Dev-only check that the server's dragon part shapes match the animated model: in single player it
 * copies the integrated server's dragons so the hitbox overlay can draw their shapes too (red,
 * next to the client's green), and logs how far apart the two sets are. Off unless a DevScript
 * ({@code serverhitboxes on|measure}) or {@code -Ddndclasses.serverHitboxes=true} turns it on.
 * <p>
 * The server's entities are only read on the server thread: at the end of each server tick (while on) it
 * publishes a snapshot of every dragon's part shapes, which the render and client threads read.
 */
@Environment(EnvType.CLIENT)
public final class ServerPartDebug {
    public static volatile boolean enabled = Boolean.getBoolean("dndclasses.serverHitboxes");

    /** One server dragon at the end of a server tick: each part's shapes, position, body yaw and world time. */
    public record ServerDragon(List<List<Box>> parts, Vec3d pos, float bodyYaw, long time) {
    }

    /** The last snapshot, by world and entity id (the integrated server shares entity ids with the client). */
    private static volatile Map<RegistryKey<World>, Map<Integer, ServerDragon>> snapshot = Map.of();

    private ServerPartDebug() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!enabled || !(server instanceof IntegratedServer)) {
                if (!snapshot.isEmpty()) {
                    snapshot = Map.of();
                }
                return;
            }
            Map<RegistryKey<World>, Map<Integer, ServerDragon>> worlds = new HashMap<>();
            for (ServerWorld world : server.getWorlds()) {
                Map<Integer, ServerDragon> dragons = new HashMap<>();
                for (Entity entity : world.iterateEntities()) {
                    if (entity instanceof MultipartDragon dragon) {
                        dragons.put(entity.getId(), copy(world, entity, dragon));
                    }
                }
                if (!dragons.isEmpty()) {
                    worlds.put(world.getRegistryKey(), Map.copyOf(dragons));
                }
            }
            snapshot = Map.copyOf(worlds);
        });
    }

    private static ServerDragon copy(ServerWorld world, Entity entity, MultipartDragon dragon) {
        DragonPart[] parts = dragon.getParts();
        List<List<Box>> shapes = new ArrayList<>(parts.length);
        for (DragonPart part : parts) {
            shapes.add(List.copyOf(part.getShapes()));
        }
        float yaw = entity instanceof LivingEntity living ? living.bodyYaw : entity.getYaw();
        return new ServerDragon(List.copyOf(shapes), entity.getPos(), yaw, world.getTime());
    }

    /**
     * The integrated server's copy of {@code clientEntity} from the last server tick's snapshot, or null
     * (multiplayer, turned off, not a dragon, not loaded).
     */
    @Nullable
    public static ServerDragon serverCopy(Entity clientEntity) {
        Map<Integer, ServerDragon> dragons = snapshot.get(clientEntity.world.getRegistryKey());
        return dragons == null ? null : dragons.get(clientEntity.getId());
    }

    /** Logs, for every dragon the client has, how far each server part is from the client part. */
    public static void measure(MinecraftClient client) {
        if (client.world == null) {
            return;
        }
        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof MultipartDragon dragon)) {
                continue;
            }
            ServerDragon server = serverCopy(entity);
            if (server == null) {
                continue;
            }
            DragonPart[] clientParts = dragon.getParts();
            List<List<Box>> serverParts = server.parts();
            double sum = 0, max = 0;
            String worst = "";
            int count = 0;
            for (int i = 0; i < Math.min(clientParts.length, serverParts.size()); i++) {
                Box c = union(clientParts[i].getShapes());
                Box s = union(serverParts.get(i));
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
            float clientYaw = entity instanceof LivingEntity living ? living.bodyYaw : entity.getYaw();
            DnDClasses.LOGGER.info("[ServerPartDebug] {} client time {} server time {}: part centres {} apart on average, worst {} ({} blocks);"
                            + " dragon {} blocks apart, body yaw {} vs {}",
                    entity.getType().getUntranslatedName(), client.world.getTime(), server.time(),
                    String.format("%.3f", count == 0 ? 0 : sum / count), worst, String.format("%.3f", max),
                    String.format("%.3f", entity.getPos().distanceTo(server.pos())),
                    String.format("%.1f", clientYaw), String.format("%.1f", server.bodyYaw()));
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
