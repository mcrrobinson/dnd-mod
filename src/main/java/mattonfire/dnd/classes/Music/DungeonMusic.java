package mattonfire.dnd.classes.Music;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.dungeon.DungeonEvents;
import mattonfire.dnd.dungeon.DungeonRegistry;
import mattonfire.dnd.dungeon.DungeonState;
import mattonfire.dnd.world.gen.dungeon.DungeonStructure;
import mattonfire.dnd.world.gen.dungeon.DungeonTheme;
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
 * Inside one of the mod's dungeons the client also gets the dungeon's theme (S2C_DUNGEON_THEME, a byte:
 * 0 = generic, otherwise {@link #themeId}), so it can play that theme's loop instead of Silent Footsteps.
 * The room-cleared and dungeon-cleared stings are sent from here too (S2C_DUNGEON_STING).
 */
public class DungeonMusic {
    public static final Identifier S2C_IN_DUNGEON = Identifier.of(DnDClasses.MOD_ID, "music_in_dungeon");
    public static final Identifier S2C_NEAR_FORTRESS = Identifier.of(DnDClasses.MOD_ID, "music_near_fortress");
    public static final Identifier S2C_DUNGEON_THEME = Identifier.of(DnDClasses.MOD_ID, "music_dungeon_theme");
    public static final Identifier S2C_DUNGEON_STING = Identifier.of(DnDClasses.MOD_ID, "music_dungeon_sting");
    /** Theme byte for "no theme": vanilla dungeons and other tagged structures play the generic track. */
    public static final byte GENERIC = 0;
    /** Sting bytes. */
    public static final byte STING_ROOM_CLEARED = 0;
    public static final byte STING_DUNGEON_CLEARED = 1;
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
    private static final Map<UUID, Byte> THEME = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getTime() % CHECK_INTERVAL_TICKS != 0) {
                return;
            }
            for (ServerPlayerEntity player : world.getPlayers()) {
                boolean nearFortress = isNearFortress(world, player.getBlockPos());
                DungeonTheme theme = nearFortress ? null : themeAt(world, player.getBlockPos());
                boolean inDungeon = !nearFortress && (theme != null || isInDungeon(world, player.getBlockPos()));
                // Theme first, so the client already knows it when the dungeon music starts
                updateTheme(player, inDungeon ? themeId(theme) : GENERIC);
                update(player, IN_DUNGEON, inDungeon, S2C_IN_DUNGEON);
                update(player, NEAR_FORTRESS, nearFortress, S2C_NEAR_FORTRESS);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            IN_DUNGEON.remove(handler.player.getUuid());
            NEAR_FORTRESS.remove(handler.player.getUuid());
            THEME.remove(handler.player.getUuid());
        });
        DungeonEvents.ROOM_CLEARED.register((world, dungeon, room, players) -> {
            // The boss room opening after the kill gets the dungeon fanfare instead
            if (!dungeon.isCleared()) {
                sting(players, STING_ROOM_CLEARED);
            }
        });
        DungeonEvents.CLEARED.register((world, dungeon, participants) -> sting(participants, STING_DUNGEON_CLEARED));
    }

    /** The theme byte the client gets: 0 for none or a theme without its own track, else ordinal + 1. */
    public static byte themeId(DungeonTheme theme) {
        return theme == null ? GENERIC : (byte) (theme.ordinal() + 1);
    }

    /** The theme of the mod dungeon whose bounds hold {@code pos}, or null. */
    private static DungeonTheme themeAt(ServerWorld world, BlockPos pos) {
        StructureStart start = world.getStructureAccessor().getStructureContaining(pos, DungeonRegistry.DUNGEONS);
        if (start.hasChildren() && start.getStructure() instanceof DungeonStructure structure) {
            return structure.theme();
        }
        // /place structure leaves no start behind; its wards record the dungeon in the registry
        return DungeonRegistry.get(world).containing(pos).map(DungeonState::theme).orElse(null);
    }

    private static void updateTheme(ServerPlayerEntity player, byte theme) {
        Byte previous = THEME.put(player.getUuid(), theme);
        if (previous == null || previous != theme) {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeByte(theme);
            ServerPlayNetworking.send(player, S2C_DUNGEON_THEME, buf);
        }
    }

    /** Plays a dungeon sting for each of {@code players} (client side, with the music ducked under it). */
    public static void sting(List<ServerPlayerEntity> players, byte sting) {
        for (ServerPlayerEntity player : players) {
            DnDClasses.LOGGER.info("[DungeonMusic] sting {} to {}", sting == STING_DUNGEON_CLEARED ? "dungeon_cleared" : "room_cleared",
                    player.getName().getString());
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeByte(sting);
            ServerPlayNetworking.send(player, S2C_DUNGEON_STING, buf);
        }
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
