package mattonfire.dnd.dungeon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import mattonfire.dnd.world.gen.dungeon.DungeonTheme;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;

/**
 * Everything the server remembers about one dungeon, keyed by its structure start's chunk
 * ({@code StructureStart.getPos().toLong()}, the same key bounties and settlements use). Changes go
 * through the setters so the {@link DungeonRegistry} gets saved.
 */
public class DungeonState {
    public static final String[] TIER_NUMERALS = {"I", "II", "III", "IV"};

    private final long startKey;
    private final DungeonTheme theme;
    private final BlockBox bounds;
    private final BlockPos entrance;
    private final int floorY;
    private final List<Room> rooms;
    private final int place;
    private final int nameIndex;
    private int tier;
    private boolean bossDefeated;
    private long clearedAt = -1L;
    private int clears;
    private long lastOccupied;
    /** How many times it has been reset; traps re-arm when this moves past the count they were spent at. */
    private int resets;
    private final Set<UUID> visitors = new HashSet<>();
    DungeonRegistry owner;

    public DungeonState(long startKey, DungeonTheme theme, int tier, BlockBox bounds, BlockPos entrance, int floorY, List<Room> rooms) {
        this.startKey = startKey;
        this.theme = theme;
        this.tier = tier;
        this.bounds = bounds;
        this.entrance = entrance;
        this.floorY = floorY;
        this.rooms = rooms;
        ChunkPos chunk = new ChunkPos(startKey);
        long hash = MathHelper.hashCode(chunk.x, 0x5EED, chunk.z);
        this.place = (int) Math.floorMod(hash, (long) theme.placeCount());
        this.nameIndex = (int) Math.floorMod(hash >>> 16, (long) theme.nameCount());
    }

    public long startKey() {
        return this.startKey;
    }

    public DungeonTheme theme() {
        return this.theme;
    }

    /** Challenge tier, 1-4. */
    public int tier() {
        return this.tier;
    }

    public void setTier(int tier) {
        this.tier = MathHelper.clamp(tier, 1, 4);
        this.changed();
    }

    public Text tierText() {
        return Text.translatable("dungeon.dndclasses.challenge", TIER_NUMERALS[this.tier - 1]);
    }

    /** "The Barrow of Ashmoor": picked from the theme's word lists, seeded by the start position. */
    public Text name() {
        String base = this.theme.translationKey();
        return Text.translatable("dungeon.dndclasses.name", Text.translatable(base + ".place." + this.place),
                Text.translatable(base + ".name." + this.nameIndex));
    }

    /** Every piece's box together, entrance included. */
    public BlockBox bounds() {
        return this.bounds;
    }

    /** The top of the entrance stair, at the surface. */
    public BlockPos entrance() {
        return this.entrance;
    }

    /** y of the dungeon's floor blocks. */
    public int floorY() {
        return this.floorY;
    }

    public List<Room> rooms() {
        return Collections.unmodifiableList(this.rooms);
    }

    public Room room(int id) {
        for (Room room : this.rooms) {
            if (room.id == id) {
                return room;
            }
        }
        return null;
    }

    /** The room whose box holds {@code pos}, or null (corridors and the surface aren't rooms). */
    public Room roomAt(BlockPos pos) {
        for (Room room : this.rooms) {
            if (room.box.contains(pos)) {
                return room;
            }
        }
        return null;
    }

    public boolean contains(BlockPos pos) {
        return this.bounds.contains(pos);
    }

    /** Cleared means the boss is dead (or an admin said so). */
    public boolean isCleared() {
        return this.bossDefeated;
    }

    public boolean bossDefeated() {
        return this.bossDefeated;
    }

    /** Game time of the last clear, or -1. */
    public long clearedAt() {
        return this.clearedAt;
    }

    public int clears() {
        return this.clears;
    }

    public long lastOccupied() {
        return this.lastOccupied;
    }

    /** Bumped by every {@link #reset()}: anything spent since the last reset (traps) comes back. */
    public int resets() {
        return this.resets;
    }

    public void setRoomState(Room room, RoomState state) {
        if (room.state != state) {
            room.state = state;
            this.changed();
        }
    }

    /** Marks the dungeon cleared at {@code time}: the boss is down and every room opens. */
    public void markCleared(long time) {
        for (Room room : this.rooms) {
            room.state = RoomState.CLEARED;
        }
        this.bossDefeated = true;
        this.clearedAt = time;
        this.clears++;
        this.changed();
    }

    /** Back to fresh: every room untouched and the boss available again. Clears and visitors are kept. */
    public void reset() {
        for (Room room : this.rooms) {
            room.state = RoomState.UNTOUCHED;
        }
        this.bossDefeated = false;
        this.clearedAt = -1L;
        this.resets++;
        this.changed();
    }

    void setLastOccupied(long time) {
        // Saved with the next real change; losing it on a crash only delays a repopulation.
        this.lastOccupied = time;
    }

    /** True the first time a player is seen inside. */
    boolean addVisitor(UUID player) {
        boolean added = this.visitors.add(player);
        if (added) {
            this.changed();
        }
        return added;
    }

    public boolean hasVisited(UUID player) {
        return this.visitors.contains(player);
    }

    private void changed() {
        if (this.owner != null) {
            this.owner.markDirty();
        }
    }

    /** The saved form; a fresh dungeon's is also what generation writes into every ward and piece. */
    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putLong("StartKey", this.startKey);
        nbt.putString("Theme", this.theme.id());
        nbt.putInt("Tier", this.tier);
        nbt.put("Bounds", box(this.bounds));
        nbt.put("Entrance", NbtHelper.fromBlockPos(this.entrance));
        nbt.putInt("FloorY", this.floorY);
        nbt.putBoolean("BossDefeated", this.bossDefeated);
        nbt.putLong("ClearedAt", this.clearedAt);
        nbt.putInt("Clears", this.clears);
        nbt.putLong("LastOccupied", this.lastOccupied);
        nbt.putInt("Resets", this.resets);
        NbtList rooms = new NbtList();
        for (Room room : this.rooms) {
            NbtCompound r = new NbtCompound();
            r.putInt("Id", room.id);
            r.putString("Role", room.role.name());
            r.putString("State", room.state.name());
            r.put("Box", box(room.box));
            rooms.add(r);
        }
        nbt.put("Rooms", rooms);
        NbtList visitors = new NbtList();
        for (UUID uuid : this.visitors) {
            visitors.add(NbtHelper.fromUuid(uuid));
        }
        nbt.put("Visitors", visitors);
        return nbt;
    }

    public static DungeonState fromNbt(NbtCompound nbt) {
        List<Room> rooms = new ArrayList<>();
        NbtList list = nbt.getList("Rooms", NbtCompound.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound r = list.getCompound(i);
            Room room = new Room(r.getInt("Id"), RoomRole.byName(r.getString("Role")), box(r.getIntArray("Box")));
            room.state = RoomState.byName(r.getString("State"));
            rooms.add(room);
        }
        DungeonState state = new DungeonState(nbt.getLong("StartKey"), DungeonTheme.byId(nbt.getString("Theme")),
                MathHelper.clamp(nbt.getInt("Tier"), 1, 4), box(nbt.getIntArray("Bounds")),
                NbtHelper.toBlockPos(nbt.getCompound("Entrance")), nbt.getInt("FloorY"), rooms);
        state.bossDefeated = nbt.getBoolean("BossDefeated");
        state.clearedAt = nbt.getLong("ClearedAt");
        state.clears = nbt.getInt("Clears");
        state.lastOccupied = nbt.getLong("LastOccupied");
        state.resets = nbt.getInt("Resets");
        NbtList visitors = nbt.getList("Visitors", net.minecraft.nbt.NbtElement.INT_ARRAY_TYPE);
        for (int i = 0; i < visitors.size(); i++) {
            state.visitors.add(NbtHelper.toUuid(visitors.get(i)));
        }
        return state;
    }

    public static NbtIntArray box(BlockBox box) {
        return new NbtIntArray(new int[]{box.getMinX(), box.getMinY(), box.getMinZ(), box.getMaxX(), box.getMaxY(), box.getMaxZ()});
    }

    public static BlockBox box(int[] a) {
        return a.length == 6 ? new BlockBox(a[0], a[1], a[2], a[3], a[4], a[5]) : new BlockBox(0, 0, 0, 0, 0, 0);
    }

    /** One room: its place on the route, what it's for and how far its encounter has got. */
    public static final class Room {
        private final int id;
        private final RoomRole role;
        private final BlockBox box;
        private RoomState state = RoomState.UNTOUCHED;

        public Room(int id, RoomRole role, BlockBox box) {
            this.id = id;
            this.role = role;
            this.box = box;
        }

        public int id() {
            return this.id;
        }

        public RoomRole role() {
            return this.role;
        }

        /** The room's whole box, walls included. */
        public BlockBox box() {
            return this.box;
        }

        public RoomState state() {
            return this.state;
        }
    }
}
