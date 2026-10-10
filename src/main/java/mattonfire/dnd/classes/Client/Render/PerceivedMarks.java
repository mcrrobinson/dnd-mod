package mattonfire.dnd.classes.Client.Render;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.joml.Vector3f;

import mattonfire.dnd.classes.SkillChecks.Perception;
import mattonfire.dnd.entity.MimicEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/**
 * Things this player perceived ({@link Perception#S2C_PERCEIVED}), drawn for them only: breathing puffs over a
 * dormant mimic they noticed, and a red outline round what a Search found (the entity glow outline, or a box
 * of red dust round a block).
 */
@Environment(EnvType.CLIENT)
public final class PerceivedMarks {
    /** Red of the Search outline. */
    public static final int OUTLINE_COLOR = 0xFF3030;
    private static final DustParticleEffect RED_DUST = new DustParticleEffect(new Vector3f(1.0F, 0.19F, 0.19F), 0.8F);
    /** Ticks between two breaths of a noticed mimic. */
    private static final int BREATH_PERIOD = 40;

    private record Mark(Perception.Mark kind, long until) {
    }

    private static final Map<Integer, Mark> BREATHING = new HashMap<>();
    private static final Map<Integer, Mark> OUTLINED = new HashMap<>();
    private static final Map<BlockPos, Mark> BLOCKS = new HashMap<>();

    private PerceivedMarks() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(Perception.S2C_PERCEIVED, (client, handler, buf, sender) -> {
            Perception.Mark kind = buf.readEnumConstant(Perception.Mark.class);
            boolean entity = buf.readBoolean();
            int id = entity ? buf.readVarInt() : 0;
            BlockPos pos = entity ? null : buf.readBlockPos();
            int ticks = buf.readVarInt();
            client.execute(() -> {
                if (client.world == null) {
                    return;
                }
                long until = ticks <= 0 ? Long.MAX_VALUE : client.world.getTime() + ticks;
                Mark mark = new Mark(kind, until);
                if (pos != null) {
                    BLOCKS.put(pos, mark);
                } else if (kind == Perception.Mark.BREATH) {
                    BREATHING.put(id, mark);
                } else {
                    OUTLINED.put(id, mark);
                }
            });
        });
        ClientTickEvents.END_CLIENT_TICK.register(PerceivedMarks::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(PerceivedMarks::clear));
    }

    /** Whether the entity should get the red Search outline (client entities only). */
    public static boolean outlined(Entity entity) {
        if (!entity.getWorld().isClient) {
            return false;
        }
        Mark mark = OUTLINED.get(entity.getId());
        return mark != null && mark.until() > entity.getWorld().getTime();
    }

    private static void clear() {
        BREATHING.clear();
        OUTLINED.clear();
        BLOCKS.clear();
    }

    private static void tick(MinecraftClient client) {
        ClientWorld world = client.world;
        if (world == null || client.isPaused()) {
            return;
        }
        long now = world.getTime();
        OUTLINED.values().removeIf(m -> m.until() <= now);
        for (Iterator<Map.Entry<Integer, Mark>> it = BREATHING.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, Mark> e = it.next();
            Entity entity = world.getEntityById(e.getKey());
            if (entity == null || entity.isRemoved() || e.getValue().until() <= now) {
                it.remove();
                continue;
            }
            if (entity instanceof MimicEntity mimic && mimic.isDormant() && (now + entity.getId()) % BREATH_PERIOD == 0) {
                breathe(world, entity);
            }
        }
        for (Iterator<Map.Entry<BlockPos, Mark>> it = BLOCKS.entrySet().iterator(); it.hasNext();) {
            Map.Entry<BlockPos, Mark> e = it.next();
            if (e.getValue().until() <= now) {
                it.remove();
            } else if (now % 5 == 0) {
                box(world, new Box(e.getKey()));
            }
        }
    }

    /** One faint breath out of the lid's seam. */
    private static void breathe(ClientWorld world, Entity entity) {
        Box box = entity.getBoundingBox();
        double y = box.minY + box.getYLength() * 0.6;
        for (int i = 0; i < 3; i++) {
            double x = box.minX + world.random.nextDouble() * box.getXLength();
            double z = box.minZ + world.random.nextDouble() * box.getZLength();
            world.addParticle(ParticleTypes.CLOUD, x, y, z, 0.0, 0.015, 0.0);
        }
    }

    private static void box(ClientWorld world, Box box) {
        for (int i = 0; i <= 4; i++) {
            double t = i / 4.0;
            for (double y : new double[] { box.minY, box.maxY }) {
                dust(world, box.minX + t * box.getXLength(), y, box.minZ);
                dust(world, box.minX + t * box.getXLength(), y, box.maxZ);
                dust(world, box.minX, y, box.minZ + t * box.getZLength());
                dust(world, box.maxX, y, box.minZ + t * box.getZLength());
            }
        }
    }

    private static void dust(ClientWorld world, double x, double y, double z) {
        world.addParticle(RED_DUST, x, y, z, 0.0, 0.0, 0.0);
    }
}
