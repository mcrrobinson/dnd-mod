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
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.structure.Structure;

/**
 * Tells each client whether its player is in a dungeon, so it can play the dungeon music.
 * A dungeon is any structure in the dndclasses:music_dungeons tag, or anywhere near a mob spawner
 * (vanilla monster rooms aren't structures).
 */
public class DungeonMusic {
    public static final Identifier S2C_IN_DUNGEON = Identifier.of(DnDClasses.MOD_ID, "music_in_dungeon");
    private static final TagKey<Structure> DUNGEONS = TagKey.of(RegistryKeys.STRUCTURE,
            Identifier.of(DnDClasses.MOD_ID, "music_dungeons"));
    private static final int CHECK_INTERVAL_TICKS = 40;
    private static final int SPAWNER_RANGE = 16;

    private static final Set<UUID> IN_DUNGEON = new HashSet<>();

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getTime() % CHECK_INTERVAL_TICKS != 0) {
                return;
            }
            for (ServerPlayerEntity player : world.getPlayers()) {
                boolean inDungeon = isInDungeon(world, player.getBlockPos());
                boolean wasInDungeon = IN_DUNGEON.contains(player.getUuid());
                if (inDungeon != wasInDungeon) {
                    if (inDungeon) {
                        IN_DUNGEON.add(player.getUuid());
                    } else {
                        IN_DUNGEON.remove(player.getUuid());
                    }
                    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                    buf.writeBoolean(inDungeon);
                    ServerPlayNetworking.send(player, S2C_IN_DUNGEON, buf);
                }
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> IN_DUNGEON.remove(handler.player.getUuid()));
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
