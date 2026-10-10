package mattonfire.dnd.entity.boss;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.GoblinWarlordEntity;
import mattonfire.dnd.entity.LichEntity;
import mattonfire.dnd.entity.ModEntityTypes;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.MarkerEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.ServerWorldAccess;
import org.jetbrains.annotations.Nullable;

/**
 * Bosses that live in generated structures: the stronghold library's Lich and the Nether Fortress
 * Warlord. A boss can't exist on Peaceful, so a structure generated on Peaceful gets an invisible
 * marker where the boss would stand instead. Once the difficulty is above Peaceful, a loaded
 * marker raises its boss and goes away, so switching back from Peaceful doesn't leave the
 * structure empty for good.
 *
 * Dungeon boss rooms instead call {@link #spawnBoss} when the party arrives, with the boss's health
 * scaled to the party.
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
                spawnLich(world, pos, marker.getYaw(), phylacterySpot(tags, pos), null);
            } else if (tags.contains(WARLORD)) {
                spawnWarlord(world, pos, marker.getYaw(), null);
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
            spawnLich(world, pos, yaw, phylacterySpot, null);
        }
    }

    /** Places the fortress's Warlord, or a marker for it on Peaceful. */
    public static void placeWarlord(ServerWorldAccess world, BlockPos pos, float yaw) {
        if (world.getDifficulty() == Difficulty.PEACEFUL) {
            placeMarker(world, pos, yaw, WARLORD, null);
        } else {
            spawnWarlord(world, pos, yaw, null);
        }
    }

    /** Bosses a structure (a dungeon's boss room) can call up on demand. */
    public enum BossType {
        LICH,
        GOBLIN_WARLORD;

        public static BossType byName(String name) {
            try {
                return valueOf(name.toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return LICH;
            }
        }
    }

    /** The persistent max-health modifier {@link #spawnBoss} gives a scaled boss (and its phylactery). */
    public static final UUID HEALTH_SCALE_ID = UUID.fromString("5d1c0b8e-7a43-4f0e-9a51-2b6f3c9e8d10");
    private static final String HEALTH_SCALE_NAME = "dndclasses.boss_health_scale";

    /**
     * Spawns a boss now, for structures that decide when (dungeon boss rooms spawn theirs when the
     * party walks in). Its max health is multiplied by {@code healthMult} through a persistent
     * modifier ({@link #scaleHealth}), and {@code extras} runs before it enters the world (tags, a
     * phylactery spot, ...). Returns null on Peaceful, where no boss can exist.
     */
    @Nullable
    public static MobEntity spawnBoss(ServerWorld world, BlockPos pos, float yaw, BossType type, double healthMult,
                                      @Nullable BlockPos phylacterySpot, @Nullable Consumer<MobEntity> extras) {
        if (world.getDifficulty() == Difficulty.PEACEFUL) {
            return null;
        }
        Consumer<MobEntity> setup = mob -> {
            scaleHealth(mob, healthMult);
            if (extras != null) {
                extras.accept(mob);
            }
        };
        return switch (type) {
            case LICH -> spawnLich(world, pos, yaw, phylacterySpot != null ? phylacterySpot : pos, setup);
            case GOBLIN_WARLORD -> spawnWarlord(world, pos, yaw, setup);
        };
    }

    /**
     * Gives {@code mob} a persistent max-health multiplier of {@code mult} (1 = unchanged) and heals
     * it to full. Does nothing if it already has one, so it can be called again safely.
     */
    public static void scaleHealth(LivingEntity mob, double mult) {
        EntityAttributeInstance health = mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (health == null || mult <= 1.0D || health.getModifier(HEALTH_SCALE_ID) != null) {
            return;
        }
        health.addPersistentModifier(new EntityAttributeModifier(HEALTH_SCALE_ID, HEALTH_SCALE_NAME, mult - 1.0D,
                EntityAttributeModifier.Operation.MULTIPLY_BASE));
        mob.setHealth(mob.getMaxHealth());
    }

    @Nullable
    private static LichEntity spawnLich(ServerWorldAccess world, BlockPos pos, float yaw, BlockPos phylacterySpot,
                                        @Nullable Consumer<MobEntity> extras) {
        LichEntity lich = ModEntityTypes.LICH.create(world.toServerWorld());
        if (lich == null) {
            return null;
        }
        lich.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw, 0.0F);
        lich.initialize(world, world.getLocalDifficulty(pos), SpawnReason.STRUCTURE, null, null);
        lich.setPhylacterySpot(phylacterySpot);
        lich.setPersistent();
        if (extras != null) {
            extras.accept(lich);
        }
        world.spawnEntityAndPassengers(lich);
        return lich;
    }

    @Nullable
    private static GoblinWarlordEntity spawnWarlord(ServerWorldAccess world, BlockPos pos, float yaw,
                                                    @Nullable Consumer<MobEntity> extras) {
        GoblinWarlordEntity warlord = ModEntityTypes.GOBLIN_WARLORD.create(world.toServerWorld());
        if (warlord == null) {
            return null;
        }
        warlord.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw, 0.0F);
        warlord.initialize(world, world.getLocalDifficulty(pos), SpawnReason.STRUCTURE, null, null);
        warlord.setGuardPos(pos);
        warlord.setPersistent();
        if (extras != null) {
            extras.accept(warlord);
        }
        world.spawnEntityAndPassengers(warlord);
        return warlord;
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
