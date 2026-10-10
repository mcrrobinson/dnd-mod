package mattonfire.dnd.classes.Blocks;

import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.classes.Registry.ModBlocks;
import mattonfire.dnd.dungeon.DungeonEvents;
import mattonfire.dnd.dungeon.DungeonRegistry;
import mattonfire.dnd.dungeon.DungeonState;
import mattonfire.dnd.dungeon.RoomRole;
import mattonfire.dnd.dungeon.RoomState;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * A dungeon room's ward: which dungeon and room it belongs to, the room's box and its spawn
 * points, all written at worldgen. Every {@link #CHECK_INTERVAL} ticks it looks for survival or
 * adventure players in the box. The first time it sees each player in the dungeon it fires
 * {@link DungeonEvents#ENTERED} (and shows the dungeon's name and tier); when its room is
 * untouched it marks it cleared and fires {@link DungeonEvents#ROOM_CLEARED}.
 *
 * Encounters (UNTOUCHED -> ACTIVE -> CLEARED, with seals) replace the instant clear in dungeons ticket 2.
 */
public class DungeonWardBlockEntity extends BlockEntity {
    public static final int CHECK_INTERVAL = 10;

    private long startKey;
    private int roomId = -1;
    private RoomRole role = RoomRole.ENCOUNTER_SMALL;
    @Nullable
    private BlockBox box;
    private final List<BlockPos> spawnPoints = new ArrayList<>();
    /** The whole dungeon as generated (see {@link DungeonRegistry#getOrCreate(NbtCompound)}). */
    @Nullable
    private NbtCompound summary;

    public DungeonWardBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.DUNGEON_WARD_ENTITY, pos, state);
    }

    public void setup(long startKey, int roomId, @Nullable RoomRole role, BlockBox box, List<BlockPos> spawnPoints,
                      @Nullable NbtCompound summary) {
        this.summary = summary;
        this.startKey = startKey;
        this.roomId = roomId;
        this.role = role == null ? RoomRole.ENCOUNTER_SMALL : role;
        this.box = box;
        this.spawnPoints.clear();
        this.spawnPoints.addAll(spawnPoints);
        this.markDirty();
    }

    public long getStartKey() {
        return this.startKey;
    }

    public int getRoomId() {
        return this.roomId;
    }

    public RoomRole getRole() {
        return this.role;
    }

    @Nullable
    public BlockBox getBox() {
        return this.box;
    }

    /** The whole dungeon as generated, or null for wards placed by hand. */
    @Nullable
    public NbtCompound getSummary() {
        return this.summary;
    }

    public List<BlockPos> getSpawnPoints() {
        return List.copyOf(this.spawnPoints);
    }

    public static void tick(World world, BlockPos pos, BlockState state, DungeonWardBlockEntity ward) {
        if (!(world instanceof ServerWorld server) || ward.roomId < 0 || ward.box == null
                || Math.floorMod(server.getTime() + pos.asLong(), CHECK_INTERVAL) != 0) {
            return;
        }
        BlockBox box = ward.box;
        List<ServerPlayerEntity> inside = server.getPlayers(player -> !player.isSpectator() && !player.isCreative()
                && box.contains(player.getBlockPos()));
        if (inside.isEmpty()) {
            return;
        }
        DungeonRegistry registry = DungeonRegistry.get(server);
        DungeonState dungeon = registry.get(ward.startKey);
        if (dungeon == null) {
            dungeon = ward.summary != null ? registry.getOrCreate(ward.summary)
                    : registry.getOrCreate(server.getStructureAccessor().getStructureContaining(pos, DungeonRegistry.DUNGEONS));
            if (dungeon == null) {
                return;
            }
        }
        DungeonRegistry.occupied(dungeon, server.getTime());
        for (ServerPlayerEntity player : inside) {
            if (DungeonRegistry.firstVisit(dungeon, player)) {
                showTitle(dungeon, player);
                DungeonEvents.ENTERED.invoker().onEntered(server, dungeon, player);
            }
        }
        DungeonState.Room room = dungeon.room(ward.roomId);
        if (room != null && room.state() == RoomState.UNTOUCHED) {
            dungeon.setRoomState(room, RoomState.CLEARED);
            DungeonEvents.ROOM_CLEARED.invoker().onRoomCleared(server, dungeon, room, inside);
        }
    }

    private static void showTitle(DungeonState dungeon, ServerPlayerEntity player) {
        player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 60, 20));
        player.networkHandler.sendPacket(new SubtitleS2CPacket(dungeon.tierText()));
        player.networkHandler.sendPacket(new TitleS2CPacket(dungeon.name()));
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putLong("StartKey", this.startKey);
        nbt.putInt("RoomId", this.roomId);
        nbt.putString("Role", this.role.name());
        if (this.box != null) {
            nbt.put("Box", DungeonState.box(this.box));
        }
        NbtList points = new NbtList();
        for (BlockPos point : this.spawnPoints) {
            points.add(NbtHelper.fromBlockPos(point));
        }
        nbt.put("SpawnPoints", points);
        if (this.summary != null) {
            nbt.put("Dungeon", this.summary);
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.startKey = nbt.getLong("StartKey");
        this.roomId = nbt.contains("RoomId") ? nbt.getInt("RoomId") : -1;
        this.role = RoomRole.byName(nbt.getString("Role"));
        this.box = nbt.contains("Box") ? DungeonState.box(nbt.getIntArray("Box")) : null;
        this.summary = nbt.contains("Dungeon") ? nbt.getCompound("Dungeon") : null;
        this.spawnPoints.clear();
        NbtList points = nbt.getList("SpawnPoints", NbtCompound.COMPOUND_TYPE);
        for (int i = 0; i < points.size(); i++) {
            this.spawnPoints.add(NbtHelper.toBlockPos(points.getCompound(i)));
        }
    }
}
