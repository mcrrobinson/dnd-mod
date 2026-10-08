package mattonfire.dnd.entity.boss;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.GoblinWarlordEntity;
import mattonfire.dnd.entity.LichEntity;
import mattonfire.dnd.entity.ModEntityTypes;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MarkerEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.ServerWorldAccess;

/**
 * Bosses that live in generated structures: the stronghold library's Lich and the Nether Fortress
 * Warlord. A boss can't exist on Peaceful, so a structure generated on Peaceful gets an invisible
 * marker where the boss would stand instead. Once the difficulty is above Peaceful, a loaded
 * marker raises its boss and goes away, so switching back from Peaceful doesn't leave the
 * structure empty for good.
 */
public final class StructureBosses {
    private static final String PENDING = "dndclasses.pending_boss";
    private static final String LICH = "dndclasses.pending_boss.lich";
    private static final String WARLORD = "dndclasses.pending_boss.warlord";
    /** Followed by "x,y,z": where the Lich's phylactery goes. */
    private static final String PHYLACTERY = "dndclasses.phylactery=";
    private static final int CHECK_INTERVAL = 20;

    /** Loaded pending-boss markers (server thread only). */
    private static final Set<MarkerEntity> MARKERS = new HashSet<>();

    private StructureBosses() {
    }

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof MarkerEntity marker && marker.getCommandTags().contains(PENDING)) {
                MARKERS.add(marker);
            }
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof MarkerEntity marker) {
                MARKERS.remove(marker);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(StructureBosses::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> MARKERS.clear());
    }

    private static void tick(MinecraftServer server) {
        if (MARKERS.isEmpty() || server.getTicks() % CHECK_INTERVAL != 0) {
            return;
        }
        for (MarkerEntity marker : List.copyOf(MARKERS)) {
            if (marker.isRemoved() || !(marker.world instanceof ServerWorld world)) {
                MARKERS.remove(marker);
                continue;
            }
            if (world.getDifficulty() == Difficulty.PEACEFUL) {
                continue;
            }
            BlockPos pos = marker.getBlockPos();
            Set<String> tags = marker.getCommandTags();
            if (tags.contains(LICH)) {
                spawnLich(world, pos, marker.getYaw(), phylacterySpot(tags, pos));
            } else if (tags.contains(WARLORD)) {
                spawnWarlord(world, pos, marker.getYaw());
            }
            DnDClasses.LOGGER.info("[StructureBosses] raised a boss held back by Peaceful at {}", pos.toShortString());
            marker.discard();
            MARKERS.remove(marker);
        }
    }

    /** Places the library's Lich, or a marker for it on Peaceful. */
    public static void placeLich(ServerWorldAccess world, BlockPos pos, float yaw, BlockPos phylacterySpot) {
        if (world.getDifficulty() == Difficulty.PEACEFUL) {
            placeMarker(world, pos, yaw, LICH, PHYLACTERY + phylacterySpot.getX() + "," + phylacterySpot.getY() + ","
                    + phylacterySpot.getZ());
        } else {
            spawnLich(world, pos, yaw, phylacterySpot);
        }
    }

    /** Places the fortress's Warlord, or a marker for it on Peaceful. */
    public static void placeWarlord(ServerWorldAccess world, BlockPos pos, float yaw) {
        if (world.getDifficulty() == Difficulty.PEACEFUL) {
            placeMarker(world, pos, yaw, WARLORD, null);
        } else {
            spawnWarlord(world, pos, yaw);
        }
    }

    private static void spawnLich(ServerWorldAccess world, BlockPos pos, float yaw, BlockPos phylacterySpot) {
        LichEntity lich = ModEntityTypes.LICH.create(world.toServerWorld());
        if (lich == null) {
            return;
        }
        lich.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw, 0.0F);
        lich.initialize(world, world.getLocalDifficulty(pos), SpawnReason.STRUCTURE, null, null);
        lich.setPhylacterySpot(phylacterySpot);
        lich.setPersistent();
        world.spawnEntityAndPassengers(lich);
    }

    private static void spawnWarlord(ServerWorldAccess world, BlockPos pos, float yaw) {
        GoblinWarlordEntity warlord = ModEntityTypes.GOBLIN_WARLORD.create(world.toServerWorld());
        if (warlord == null) {
            return;
        }
        warlord.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw, 0.0F);
        warlord.initialize(world, world.getLocalDifficulty(pos), SpawnReason.STRUCTURE, null, null);
        warlord.setGuardPos(pos);
        warlord.setPersistent();
        world.spawnEntityAndPassengers(warlord);
    }

    private static void placeMarker(ServerWorldAccess world, BlockPos pos, float yaw, String kind, String extra) {
        MarkerEntity marker = EntityType.MARKER.create(world.toServerWorld());
        if (marker == null) {
            return;
        }
        marker.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw, 0.0F);
        marker.addCommandTag(PENDING);
        marker.addCommandTag(kind);
        if (extra != null) {
            marker.addCommandTag(extra);
        }
        world.spawnEntity(marker);
    }

    private static BlockPos phylacterySpot(Set<String> tags, BlockPos fallback) {
        for (String tag : new ArrayList<>(tags)) {
            if (!tag.startsWith(PHYLACTERY)) {
                continue;
            }
            String[] parts = tag.substring(PHYLACTERY.length()).split(",");
            try {
                return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            } catch (RuntimeException e) {
                break;
            }
        }
        return fallback;
    }
}
