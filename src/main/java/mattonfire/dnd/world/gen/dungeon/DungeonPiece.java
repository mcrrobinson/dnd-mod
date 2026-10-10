package mattonfire.dnd.world.gen.dungeon;

import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.classes.Blocks.DungeonWardBlockEntity;
import mattonfire.dnd.classes.Blocks.HoardCofferBlock;
import mattonfire.dnd.classes.Blocks.HoardCofferBlockEntity;
import mattonfire.dnd.dungeon.DungeonLoot;
import mattonfire.dnd.classes.Registry.ModBlocks;
import mattonfire.dnd.dungeon.RoomRole;
import mattonfire.dnd.entity.MimicEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.state.property.Properties;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.jetbrains.annotations.Nullable;

/**
 * Base for every dungeon piece. Pieces are built in local coordinates with no rotation: x and z
 * count from the box's north-west corner, and y = 0 is the dungeon's floor level ({@link #floorY()}),
 * so y = -1 is the waterproof layer under the floor. Directions are real world directions.
 *
 * A room fills its whole box: a 2-block wall (an outer skin of the theme's waterproof
 * {@link DungeonTheme.Palette#shell()} and an inner wall), the same under the floor and over the
 * ceiling, so caves, aquifers and lava can't get in. Rooms record a ward (the block entity that
 * tracks the room at runtime) and spawn points for later encounter code.
 *
 * A piece is built once per chunk it overlaps, so all its randomness comes from its own seed.
 */
public abstract class DungeonPiece extends StructurePiece {
    /** How far down a floor gets propped up over caves. */
    private static final int MAX_FOUNDATION = 16;
    /** Doorways between rooms and corridors: 3 wide, 3 high. */
    public static final int DOOR_WIDTH = 3;
    public static final int DOOR_HEIGHT = 3;
    /** Vanilla dungeon loot, for chests that don't have a dungeon table yet; rooms use {@link #loot(String)}. */
    protected static final Identifier PLACEHOLDER_LOOT = new Identifier("minecraft", "chests/simple_dungeon");

    /** What every piece of one dungeon shares. */
    public record Info(long startKey, DungeonTheme theme, int tier, int floorY) {
        void write(NbtCompound nbt) {
            nbt.putLong("StartKey", this.startKey);
            nbt.putString("Theme", this.theme.id());
            nbt.putInt("Tier", this.tier);
            nbt.putInt("FloorY", this.floorY);
        }

        static Info read(NbtCompound nbt) {
            return new Info(nbt.getLong("StartKey"), DungeonTheme.byId(nbt.getString("Theme")), nbt.getInt("Tier"), nbt.getInt("FloorY"));
        }
    }

    /**
     * An opening in a room's wall on {@code side}, centred on world coordinate {@code along} (x for
     * north/south walls, z for east/west ones). A hidden door is bricked up from the inside.
     */
    public record Door(Direction side, int along, boolean hidden) {
        NbtCompound write() {
            NbtCompound nbt = new NbtCompound();
            nbt.putInt("Side", this.side.getId());
            nbt.putInt("Along", this.along);
            nbt.putBoolean("Hidden", this.hidden);
            return nbt;
        }

        static Door read(NbtCompound nbt) {
            return new Door(Direction.byId(nbt.getInt("Side")), nbt.getInt("Along"), nbt.getBoolean("Hidden"));
        }
    }

    protected final Info info;
    private final long seed;
    private final int roomId;
    @Nullable
    private final RoomRole role;
    /** Inner (air) height of a room; 0 for pieces that aren't rooms. */
    protected final int height;
    protected final List<Door> doors;
    /** The whole dungeon as planned (a fresh {@code DungeonState} in NBT), handed to the ward. */
    @Nullable
    private NbtCompound summary;

    protected DungeonPiece(StructurePieceType type, Info info, BlockBox box, long seed, int roomId, @Nullable RoomRole role,
                           int height, List<Door> doors) {
        super(type, 0, box);
        this.info = info;
        this.seed = seed;
        this.roomId = roomId;
        this.role = role;
        this.height = height;
        this.doors = List.copyOf(doors);
    }

    protected DungeonPiece(StructurePieceType type, NbtCompound nbt) {
        super(type, nbt);
        this.info = Info.read(nbt);
        this.seed = nbt.getLong("Seed");
        this.roomId = nbt.contains("RoomId") ? nbt.getInt("RoomId") : -1;
        this.role = nbt.contains("Role") ? RoomRole.byName(nbt.getString("Role")) : null;
        this.height = nbt.getInt("Height");
        List<Door> doors = new ArrayList<>();
        NbtList list = nbt.getList("Doors", NbtCompound.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            doors.add(Door.read(list.getCompound(i)));
        }
        this.doors = doors;
        this.summary = nbt.contains("Dungeon") ? nbt.getCompound("Dungeon") : null;
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        this.info.write(nbt);
        nbt.putLong("Seed", this.seed);
        if (this.roomId >= 0) {
            nbt.putInt("RoomId", this.roomId);
        }
        if (this.role != null) {
            nbt.putString("Role", this.role.name());
        }
        nbt.putInt("Height", this.height);
        NbtList list = new NbtList();
        for (Door door : this.doors) {
            list.add(door.write());
        }
        nbt.put("Doors", list);
        if (this.summary != null && this.roomId >= 0) {
            nbt.put("Dungeon", this.summary);
        }
    }

    @Nullable
    public NbtCompound summary() {
        return this.summary;
    }

    void setSummary(NbtCompound summary) {
        this.summary = summary;
    }

    /** The room's place on the route, or -1 for pieces that aren't rooms (corridors, the surface marker). */
    public int roomId() {
        return this.roomId;
    }

    @Nullable
    public RoomRole role() {
        return this.role;
    }

    public int tier() {
        return this.info.tier();
    }

    public int floorY() {
        return this.info.floorY();
    }

    public DungeonTheme theme() {
        return this.info.theme();
    }

    /**
     * This dungeon's chest table for a kind of room ({@code encounter}, {@code secret},
     * {@code side_vault}) at its tier: {@code dndclasses:chests/dungeon/<theme>_<room>_t<tier>}.
     */
    protected Identifier loot(String room) {
        return DungeonLoot.chestTable(this.info.theme(), room, this.info.tier());
    }

    protected int width() {
        return this.boundingBox.getBlockCountX();
    }

    protected int depth() {
        return this.boundingBox.getBlockCountZ();
    }

    /** A room box of outer size {@code width} x {@code depth}, centred on (cx, cz), {@code height} of air inside. */
    static BlockBox roomBox(int cx, int cz, int width, int depth, int floorY, int height) {
        int minX = cx - (width - 1) / 2;
        int minZ = cz - (depth - 1) / 2;
        return new BlockBox(minX, floorY - 1, minZ, minX + width - 1, floorY + height + 2, minZ + depth - 1);
    }

    @Override
    public final void generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator,
                               Random random, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        Builder b = new Builder(world, chunkBox, Random.create(this.seed));
        this.build(b);
        b.placeWard();
    }

    protected abstract void build(Builder b);

    /** Shell, doorways and a ward under the middle of the floor: the start of every room. */
    protected void room(Builder b) {
        b.shell(this.height);
        b.doorways();
        b.ward(this.width() / 2, -1, this.depth() / 2);
    }

    protected static BlockState stairs(Block block, Direction facing, boolean upsideDown) {
        return block.getDefaultState().with(StairsBlock.FACING, facing)
                .with(StairsBlock.HALF, upsideDown ? BlockHalf.TOP : BlockHalf.BOTTOM);
    }

    protected static BlockState facing(Block block, Direction facing) {
        BlockState state = block.getDefaultState();
        if (state.contains(Properties.HORIZONTAL_FACING)) {
            return state.with(Properties.HORIZONTAL_FACING, facing);
        }
        if (state.contains(Properties.FACING)) {
            return state.with(Properties.FACING, facing);
        }
        return state;
    }

    /**
     * Places one chunk's slice of a piece. Everything takes local coordinates; anything outside the
     * chunk is skipped, but random draws happen either way, so every chunk sees the same piece.
     */
    protected class Builder {
        final StructureWorldAccess world;
        final BlockBox chunkBox;
        final Random random;
        final DungeonTheme.Palette palette;
        private BlockPos ward;
        private final List<BlockPos> spawnPoints = new ArrayList<>();

        Builder(StructureWorldAccess world, BlockBox chunkBox, Random random) {
            this.world = world;
            this.chunkBox = chunkBox;
            this.random = random;
            this.palette = DungeonPiece.this.info.theme().palette();
        }

        BlockPos pos(int x, int y, int z) {
            BlockBox box = DungeonPiece.this.boundingBox;
            return new BlockPos(box.getMinX() + x, DungeonPiece.this.info.floorY() + y, box.getMinZ() + z);
        }

        boolean chance(float p) {
            return this.random.nextFloat() < p;
        }

        void set(int x, int y, int z, BlockState state) {
            BlockPos pos = this.pos(x, y, z);
            if (!this.chunkBox.contains(pos)) {
                return;
            }
            this.world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
            Block block = state.getBlock();
            // Shapes that join up with their neighbours get fixed once the chunk is finished.
            if (block instanceof PaneBlock || block instanceof StairsBlock || block instanceof WallBlock
                    || block instanceof FenceBlock || block == Blocks.CHAIN) {
                this.world.getChunk(pos).markBlockForPostProcessing(pos);
            }
        }

        void set(int x, int y, int z, Block block) {
            this.set(x, y, z, block.getDefaultState());
        }

        void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
                    for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                        this.set(x, y, z, state);
                    }
                }
            }
        }

        void air(int x1, int y1, int z1, int x2, int y2, int z2) {
            this.fill(x1, y1, z1, x2, y2, z2, Blocks.AIR.getDefaultState());
        }

        @Nullable
        BlockState get(int x, int y, int z) {
            BlockPos pos = this.pos(x, y, z);
            return this.chunkBox.contains(pos) ? this.world.getBlockState(pos) : null;
        }

        /**
         * Fills the whole box as a room with {@code height} blocks of air inside: the outer ring,
         * the layer under the floor and the one over the ceiling in the waterproof shell block, the
         * next ring and the ceiling in wall, the floor between, and the floor propped up from below.
         */
        void shell(int height) {
            int w = DungeonPiece.this.width();
            int d = DungeonPiece.this.depth();
            for (int x = 0; x < w; x++) {
                for (int z = 0; z < d; z++) {
                    int ring = Math.min(Math.min(x, z), Math.min(w - 1 - x, d - 1 - z));
                    this.foundation(x, z);
                    for (int y = -1; y <= height + 2; y++) {
                        BlockState state;
                        if (y == -1 || y == height + 2 || ring == 0) {
                            state = this.palette.shell();
                        } else if (y == 0) {
                            state = ring == 1 ? this.palette.trim() : this.palette.floor(this.random);
                        } else if (ring == 1 || y == height + 1) {
                            state = y == 1 && ring == 1 ? this.palette.trim() : this.palette.wall(this.random);
                        } else {
                            state = Blocks.AIR.getDefaultState();
                        }
                        this.set(x, y, z, state);
                    }
                }
            }
        }

        /** Props the bottom of the box up with the foundation block wherever there's air or liquid under it. */
        void foundation(int x, int z) {
            BlockPos.Mutable pos = this.pos(x, -2, z).mutableCopy();
            if (!this.chunkBox.contains(pos)) {
                return;
            }
            for (int i = 0; i < MAX_FOUNDATION && pos.getY() > this.world.getBottomY(); i++) {
                BlockState state = this.world.getBlockState(pos);
                if (!state.isAir() && state.getFluidState().isEmpty() && !state.isReplaceable()) {
                    return;
                }
                this.world.setBlockState(pos, this.palette.foundation(), Block.NOTIFY_LISTENERS);
                pos.move(Direction.DOWN);
            }
        }

        /** Cuts every door of this piece through both wall layers, framed in trim. */
        void doorways() {
            for (Door door : DungeonPiece.this.doors) {
                this.doorway(door);
            }
        }

        void doorway(Door door) {
            BlockBox box = DungeonPiece.this.boundingBox;
            int w = DungeonPiece.this.width();
            int d = DungeonPiece.this.depth();
            boolean alongX = door.side().getAxis() == Direction.Axis.Z;
            int centre = alongX ? door.along() - box.getMinX() : door.along() - box.getMinZ();
            int half = DOOR_WIDTH / 2;
            for (int depthIn = 0; depthIn < 2; depthIn++) {
                for (int t = -half - 1; t <= half + 1; t++) {
                    int x;
                    int z;
                    switch (door.side()) {
                        case NORTH -> {
                            x = centre + t;
                            z = depthIn;
                        }
                        case SOUTH -> {
                            x = centre + t;
                            z = d - 1 - depthIn;
                        }
                        case WEST -> {
                            x = depthIn;
                            z = centre + t;
                        }
                        default -> {
                            x = w - 1 - depthIn;
                            z = centre + t;
                        }
                    }
                    boolean opening = Math.abs(t) <= half;
                    for (int y = 1; y <= DOOR_HEIGHT + 1; y++) {
                        if (opening && y <= DOOR_HEIGHT) {
                            boolean bricked = door.hidden() && depthIn == 1;
                            this.set(x, y, z, bricked ? this.palette.secretWall() : Blocks.AIR.getDefaultState());
                        } else if (depthIn == 1) {
                            this.set(x, y, z, this.palette.trim());
                        }
                    }
                }
            }
        }

        /** Where the room's ward goes (local); placed after the build, once its data is complete. */
        void ward(int x, int y, int z) {
            this.ward = this.pos(x, y, z);
        }

        /** A spot encounter monsters can appear at (feet position, local). */
        void spawnPoint(int x, int y, int z) {
            this.spawnPoints.add(this.pos(x, y, z));
        }

        void placeWard() {
            if (this.ward == null || !this.chunkBox.contains(this.ward)) {
                return;
            }
            this.world.setBlockState(this.ward, ModBlocks.DUNGEON_WARD.getDefaultState(), Block.NOTIFY_LISTENERS);
            if (this.world.getBlockEntity(this.ward) instanceof DungeonWardBlockEntity ward) {
                ward.setup(DungeonPiece.this.info.startKey(), DungeonPiece.this.roomId, DungeonPiece.this.role,
                        DungeonPiece.this.boundingBox, this.spawnPoints, DungeonPiece.this.summary);
            }
        }

        /** A pillar of the theme's pillar block from the floor to the ceiling, capped with accent. */
        void column(int x, int z, int top) {
            this.set(x, 1, z, this.palette.trim());
            for (int y = 2; y < top; y++) {
                this.set(x, y, z, DungeonPiece.this.info.theme().palette().pillar());
            }
            this.set(x, top, z, this.palette.accent());
        }

        void light(int x, int y, int z, boolean hanging) {
            this.set(x, y, z, this.palette.light(hanging));
        }

        /** A chest filled from a loot table when first opened, or now and then a mimic holding that loot. */
        void chest(int x, int y, int z, Direction facing, Identifier lootTable) {
            float mimicChance = DungeonPiece.this.info.theme() == DungeonTheme.DWARVEN_RUIN ? 0.15F : 0.08F;
            if (this.chance(mimicChance)) {
                this.mimic(x, y, z, facing, lootTable);
                return;
            }
            long lootSeed = this.random.nextLong();
            this.set(x, y, z, facing(Blocks.CHEST, facing));
            BlockPos pos = this.pos(x, y, z);
            if (this.chunkBox.contains(pos) && this.world.getBlockEntity(pos) instanceof LootableContainerBlockEntity container) {
                container.setLootTable(lootTable, lootSeed);
            }
            if (DungeonPiece.this.info.tier() >= 2 && this.chance(TrapCorridorPiece.NEEDLE_CHANCE)) {
                TrapCorridorPiece.needle(DungeonPiece.this, this, x, y, z);
            }
        }

        /** The vault's Hoard Coffer, tied to this dungeon (see HoardCofferBlockEntity). */
        void hoardCoffer(int x, int y, int z, Direction facing) {
            this.set(x, y, z, HoardCofferBlock.facing(facing));
            BlockPos pos = this.pos(x, y, z);
            if (this.chunkBox.contains(pos) && this.world.getBlockEntity(pos) instanceof HoardCofferBlockEntity coffer) {
                coffer.setup(DungeonPiece.this.info.startKey(), DungeonPiece.this.summary);
            }
        }

        /** A mimic sitting where a chest would be, holding that chest's loot. */
        void mimic(int x, int y, int z, Direction facing, Identifier lootTable) {
            this.set(x, y, z, Blocks.AIR.getDefaultState());
            BlockPos pos = this.pos(x, y, z);
            if (!this.chunkBox.contains(pos)) {
                return;
            }
            MimicEntity mimic = MimicEntity.disguised(this.world.toServerWorld(), pos, facing, lootTable);
            if (mimic != null) {
                this.world.spawnEntity(mimic);
            }
        }
    }
}
