package mattonfire.dnd.classes.Music;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.DnDClasses;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.structure.StructureStart;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.structure.Structure;

/**
 * Tells each client whether its player is in a dungeon or near a Nether Fortress, so it can play
 * the dungeon or fortress music.
 * A dungeon is any structure in the dndclasses:music_dungeons tag, or anywhere near a mob spawner
 * (vanilla monster rooms aren't structures).
 * A fortress is any structure in the dndclasses:music_nether_fortresses tag; "near" means inside the
 * fortress's overall bounds or within FORTRESS_MARGIN blocks of them (FORTRESS_VERTICAL_MARGIN up/down). Fortresses win over dungeons,
 * so their blaze spawners don't switch to the dungeon music.
 */
public class DungeonMusic {
    public static final Identifier S2C_IN_DUNGEON = Identifier.of(DnDClasses.MOD_ID, "music_in_dungeon");
    public static final Identifier S2C_NEAR_FORTRESS = Identifier.of(DnDClasses.MOD_ID, "music_near_fortress");
    private static final TagKey<Structure> FORTRESSES = TagKey.of(RegistryKeys.STRUCTURE,
            Identifier.of(DnDClasses.MOD_ID, "music_nether_fortresses"));
    private static final int FORTRESS_MARGIN = 16;
    // Fortresses sit buried in netherrack, so count the caves above and below them too
    private static final int FORTRESS_VERTICAL_MARGIN = 32;
    private static final TagKey<Structure> DUNGEONS = TagKey.of(RegistryKeys.STRUCTURE,
            Identifier.of(DnDClasses.MOD_ID, "music_dungeons"));
    private static final int CHECK_INTERVAL_TICKS = 40;
    private static final int SPAWNER_RANGE = 16;

    private static final Set<UUID> IN_DUNGEON = new HashSet<>();
    private static final Set<UUID> NEAR_FORTRESS = new HashSet<>();

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getTime() % CHECK_INTERVAL_TICKS != 0) {
                return;
            }
            for (ServerPlayerEntity player : world.getPlayers()) {
                boolean nearFortress = isNearFortress(world, player.getBlockPos());
                boolean inDungeon = !nearFortress && isInDungeon(world, player.getBlockPos());
                update(player, IN_DUNGEON, inDungeon, S2C_IN_DUNGEON);
                update(player, NEAR_FORTRESS, nearFortress, S2C_NEAR_FORTRESS);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            IN_DUNGEON.remove(handler.player.getUuid());
            NEAR_FORTRESS.remove(handler.player.getUuid());
        });
    }

    /** Sends the new value to the player when it changes. */
    private static void update(ServerPlayerEntity player, Set<UUID> set, boolean value, Identifier packet) {
        boolean changed = value ? set.add(player.getUuid()) : set.remove(player.getUuid());
        if (changed) {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeBoolean(value);
            ServerPlayNetworking.send(player, packet, buf);
        }
    }

    private static boolean isNearFortress(ServerWorld world, BlockPos pos) {
        Registry<Structure> registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        Set<Structure> fortresses = new HashSet<>();
        registry.iterateEntries(FORTRESSES).forEach(entry -> fortresses.add(entry.value()));
        if (fortresses.isEmpty()) {
            return false;
        }
        // Fortresses that reach the chunks around the player, checked against their whole bounds
        // (all pieces) grown by the margins, so the music starts a little before the entrance
        ChunkPos center = new ChunkPos(pos);
        for (int x = center.x - 1; x <= center.x + 1; x++) {
            for (int z = center.z - 1; z <= center.z + 1; z++) {
                if (!world.getChunkManager().isChunkLoaded(x, z)) {
                    continue;
                }
                for (StructureStart start : world.getStructureAccessor().getStructureStarts(new ChunkPos(x, z),
                        fortresses::contains)) {
                    if (start.hasChildren() && isNear(start.getBoundingBox(), pos)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isNear(BlockBox box, BlockPos pos) {
        return pos.getX() >= box.getMinX() - FORTRESS_MARGIN && pos.getX() <= box.getMaxX() + FORTRESS_MARGIN
                && pos.getZ() >= box.getMinZ() - FORTRESS_MARGIN && pos.getZ() <= box.getMaxZ() + FORTRESS_MARGIN
                && pos.getY() >= box.getMinY() - FORTRESS_VERTICAL_MARGIN
                && pos.getY() <= box.getMaxY() + FORTRESS_VERTICAL_MARGIN;
    }

    private static boolean isInDungeon(ServerWorld world, BlockPos pos) {
        return isNearSpawner(world, pos) || world.getStructureAccessor().getStructureContaining(pos, DUNGEONS).hasChildren();
    }

    private static boolean isNearSpawner(ServerWorld world, BlockPos pos) {
        ChunkPos center = new ChunkPos(pos);
        for (int x = center.x - 1; x <= center.x + 1; x++) {
            for (int z = center.z - 1; z <= center.z + 1; z++) {
                if (!world.getChunkManager().isChunkLoaded(x, z)) {
                    continue;
                }
                WorldChunk chunk = world.getChunk(x, z);
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof MobSpawnerBlockEntity
                            && blockEntity.getPos().isWithinDistance(pos, SPAWNER_RANGE)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
