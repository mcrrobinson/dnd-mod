package mattonfire.dnd.world.gen.dungeon;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mattonfire.dnd.dungeon.DungeonState;
import mattonfire.dnd.dungeon.GateKind;
import mattonfire.dnd.dungeon.RoomRole;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructurePiece;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.gen.structure.Structure;

/**
 * Lays out a dungeon on a 7x7 grid of 16x16 cells (one chunk each) centred on the structure's
 * start chunk, which keeps every piece inside the 8-chunk radius structure starts are looked up in.
 *
 * The main path is a random walk from the entrance (the middle cell) through the rooms in the
 * order of {@link RoomRole}: entrance, antechamber, small encounter, trap corridor, large
 * encounter, gate, puzzle, (sometimes another small encounter), mid-boss, then the 2x2-cell boss
 * room and the vault behind it. The trap corridor and the gate room are walked straight through.
 * Then 1-2 side rooms branch off: always a secret room, half the time a side vault.
 *
 * Every room sits in the middle of its cell(s); corridors fill the gaps between neighbouring rooms'
 * walls. Everything is on one floor, {@link #DEPTH} blocks below the lowest ground around.
 */
public final class DungeonPlanner {
    public static final int CELL = 16;
    public static final int GRID = 7;
    private static final int MIDDLE = GRID / 2;
    /** The floor is this far below the lowest ground (or sea level, if lower) sampled over the grid. */
    public static final int DEPTH = 20;
    /** Beyond this distance from (0, 0) any theme can roll tier IV. */
    public static final int FAR_OUT = 4500;
    /** Blocks of distance from (0, 0) per tier. */
    public static final int TIER_STEP = 1500;
    private static final int ATTEMPTS = 64;

    /** Outer size and inner height of each kind of room (a single cell is 16 across). */
    record Size(int width, int height) {
    }

    private DungeonPlanner() {
    }

    static Size size(RoomRole role) {
        return switch (role) {
            case ENTRANCE -> new Size(StairPiece.SIZE, 0);
            case ANTECHAMBER, VAULT -> new Size(13, 5);
            case ENCOUNTER_SMALL -> new Size(13, 6);
            case TRAP_CORRIDOR -> new Size(7, 4);
            case ENCOUNTER_LARGE, PUZZLE -> new Size(15, 7);
            case GATE, SIDE_VAULT -> new Size(11, 5);
            case CHAMPION -> new Size(15, 8);
            case BOSS -> new Size(2 * CELL - 1, 12);
            case SECRET -> new Size(9, 4);
        };
    }

    /**
     * {@code 1 + distance / 1500} from (0, 0), and a quarter of dungeons one higher or lower, within
     * the theme's limits (the Crypt stops at III unless it's {@link #FAR_OUT}; the Ruin starts at II).
     */
    public static int tier(DungeonTheme theme, int x, int z, Random random) {
        double distance = Math.sqrt((double) x * x + (double) z * z);
        int tier = 1 + (int) (distance / TIER_STEP);
        if (random.nextFloat() < 0.25F) {
            tier += random.nextBoolean() ? 1 : -1;
        }
        int max = distance >= FAR_OUT ? 4 : theme.maxNearTier();
        return MathHelper.clamp(MathHelper.clamp(tier, theme.minTier(), max), 1, 4);
    }

    /** The main path's roles, in order, up to the mid-boss (the boss and vault are placed after). */
    static List<RoomRole> mainPath(Random random) {
        List<RoomRole> roles = new ArrayList<>(List.of(RoomRole.ENTRANCE, RoomRole.ANTECHAMBER, RoomRole.ENCOUNTER_SMALL,
                RoomRole.TRAP_CORRIDOR, RoomRole.ENCOUNTER_LARGE, RoomRole.GATE, RoomRole.PUZZLE));
        if (random.nextBoolean()) {
            roles.add(RoomRole.ENCOUNTER_SMALL);
        }
        roles.add(RoomRole.CHAMPION);
        return roles;
    }

    /**
     * The class-check gate for a gate room or a side vault. The check that keeps the route open: a gate
     * on the main path must let at least two classes through or have a bypass anyone can use (see
     * {@link GateKind#mainPathSafe()}); only optional side rooms get strict gates.
     */
    static GateKind gateFor(RoomRole role, Random random) {
        GateKind kind = GateKind.pick(role, random);
        if (!role.branch() && !kind.mainPathSafe()) {
            throw new IllegalStateException("Gate " + kind + " would leave the main path to one class");
        }
        return kind;
    }

    /** Rooms the path has to go straight through. */
    private static boolean straight(RoomRole role) {
        return role == RoomRole.TRAP_CORRIDOR || role == RoomRole.GATE;
    }

    /** Rooms a side room may open off. */
    private static boolean branchHost(RoomRole role) {
        return role == RoomRole.ANTECHAMBER || role == RoomRole.ENCOUNTER_SMALL || role == RoomRole.ENCOUNTER_LARGE
                || role == RoomRole.PUZZLE || role == RoomRole.CHAMPION;
    }

    /**
     * Plans the rooms for {@code site}. Empty (no dungeon here) in the rare case no attempt fits the
     * route into the grid.
     */
    public static Optional<Layout> plan(Random random, Site site, DungeonTheme theme) {
        int tier = tier(theme, site.entrance().getX(), site.entrance().getZ(), random);
        DungeonPiece.Info info = new DungeonPiece.Info(site.startKey(), theme, tier, site.floorY());
        List<RoomRole> path = mainPath(random);
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            Grid grid = new Grid();
            if (grid.walk(random, path) && grid.placeBoss(random) && grid.placeVault(random) && grid.placeBranches(random)) {
                return Optional.of(new Layout(info, site, grid, random));
            }
        }
        return Optional.empty();
    }

    /** One room on the grid: its role and the cells it covers (the boss room covers 2x2). */
    static final class Room {
        final int id;
        final RoomRole role;
        final int x;
        final int z;
        final int span;
        final List<DungeonPiece.Door> doors = new ArrayList<>();
        BlockBox box;

        Room(int id, RoomRole role, int x, int z, int span) {
            this.id = id;
            this.role = role;
            this.x = x;
            this.z = z;
            this.span = span;
        }
    }

    /** A doorway between two rooms, from cell (ax, az) of {@code a} to the next cell over, {@code dir}. */
    record Link(Room a, Room b, int ax, int az, Direction dir, boolean hidden) {
    }

    /** The cell grid while it's being filled in. */
    static final class Grid {
        final Room[][] cells = new Room[GRID][GRID];
        final List<Room> rooms = new ArrayList<>();
        final List<Link> links = new ArrayList<>();
        private int lastX;
        private int lastZ;
        private Direction lastDir;

        private static boolean inside(int x, int z) {
            return x >= 0 && z >= 0 && x < GRID && z < GRID;
        }

        boolean free(int x, int z) {
            return inside(x, z) && this.cells[x][z] == null;
        }

        private Room add(RoomRole role, int x, int z, int span) {
            Room room = new Room(this.rooms.size(), role, x, z, span);
            this.rooms.add(room);
            for (int i = 0; i < span; i++) {
                for (int j = 0; j < span; j++) {
                    this.cells[x + i][z + j] = room;
                }
            }
            return room;
        }

        private int freeNeighbours(int x, int z) {
            int n = 0;
            for (Direction d : Direction.Type.HORIZONTAL) {
                if (this.free(x + d.getOffsetX(), z + d.getOffsetZ())) {
                    n++;
                }
            }
            return n;
        }

        boolean walk(Random random, List<RoomRole> path) {
            Room previous = this.add(path.get(0), MIDDLE, MIDDLE, 1);
            this.lastX = MIDDLE;
            this.lastZ = MIDDLE;
            this.lastDir = null;
            for (int i = 1; i < path.size(); i++) {
                RoomRole role = path.get(i);
                List<Direction> options = new ArrayList<>();
                for (Direction d : Direction.Type.HORIZONTAL) {
                    int nx = this.lastX + d.getOffsetX();
                    int nz = this.lastZ + d.getOffsetZ();
                    if (!this.free(nx, nz)) {
                        continue;
                    }
                    if (straight(previous.role) && d != this.lastDir) {
                        continue;
                    }
                    // A room walked straight through needs the cell beyond it free too.
                    if (straight(role) && !this.free(nx + d.getOffsetX(), nz + d.getOffsetZ())) {
                        continue;
                    }
                    // Don't walk into a dead end before the path is done (the mid-boss needs room for the boss).
                    this.cells[nx][nz] = previous;
                    int exits = this.freeNeighbours(nx, nz);
                    this.cells[nx][nz] = null;
                    if (exits == 0) {
                        continue;
                    }
                    options.add(d);
                }
                if (options.isEmpty()) {
                    return false;
                }
                // Mostly keep going the same way, so the path wanders rather than coils.
                Direction d = this.lastDir != null && options.contains(this.lastDir) && random.nextFloat() < 0.4F
                        ? this.lastDir : options.get(random.nextInt(options.size()));
                int nx = this.lastX + d.getOffsetX();
                int nz = this.lastZ + d.getOffsetZ();
                Room room = this.add(role, nx, nz, 1);
                this.links.add(new Link(previous, room, this.lastX, this.lastZ, d, false));
                previous = room;
                this.lastX = nx;
                this.lastZ = nz;
                this.lastDir = d;
            }
            return true;
        }

        /** The 2x2 boss room, through a door in the mid-boss room's wall. */
        boolean placeBoss(Random random) {
            Room champion = this.rooms.get(this.rooms.size() - 1);
            List<int[]> options = new ArrayList<>();
            for (Direction d : Direction.Type.HORIZONTAL) {
                int ex = champion.x + d.getOffsetX();
                int ez = champion.z + d.getOffsetZ();
                for (int shift = -1; shift <= 0; shift++) {
                    // The block's corner, so that (ex, ez) is one of its cells on the champion's side
                    int bx = d.getAxis() == Direction.Axis.X ? (d.getOffsetX() > 0 ? ex : ex - 1) : ex + shift;
                    int bz = d.getAxis() == Direction.Axis.Z ? (d.getOffsetZ() > 0 ? ez : ez - 1) : ez + shift;
                    if (this.free(bx, bz) && this.free(bx + 1, bz) && this.free(bx, bz + 1) && this.free(bx + 1, bz + 1)) {
                        options.add(new int[]{bx, bz, d.getId()});
                    }
                }
            }
            if (options.isEmpty()) {
                return false;
            }
            int[] pick = options.get(random.nextInt(options.size()));
            Room boss = this.add(RoomRole.BOSS, pick[0], pick[1], 2);
            this.links.add(new Link(champion, boss, champion.x, champion.z, Direction.byId(pick[2]), false));
            return true;
        }

        /** The vault, off any side of the boss room except the way in. */
        boolean placeVault(Random random) {
            Room boss = this.rooms.get(this.rooms.size() - 1);
            List<int[]> options = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    int cx = boss.x + i;
                    int cz = boss.z + j;
                    for (Direction d : Direction.Type.HORIZONTAL) {
                        if (this.free(cx + d.getOffsetX(), cz + d.getOffsetZ())) {
                            options.add(new int[]{cx, cz, d.getId()});
                        }
                    }
                }
            }
            if (options.isEmpty()) {
                return false;
            }
            int[] pick = options.get(random.nextInt(options.size()));
            Direction d = Direction.byId(pick[2]);
            Room vault = this.add(RoomRole.VAULT, pick[0] + d.getOffsetX(), pick[1] + d.getOffsetZ(), 1);
            this.links.add(new Link(boss, vault, pick[0], pick[1], d, false));
            return true;
        }

        /** A secret room (needed) and, half the time, a side vault, each off a room on the main path. */
        boolean placeBranches(Random random) {
            List<RoomRole> branches = new ArrayList<>(List.of(RoomRole.SECRET));
            if (random.nextBoolean()) {
                branches.add(RoomRole.SIDE_VAULT);
            }
            for (RoomRole role : branches) {
                List<Link> options = new ArrayList<>();
                for (Room host : List.copyOf(this.rooms)) {
                    if (!branchHost(host.role)) {
                        continue;
                    }
                    for (Direction d : Direction.Type.HORIZONTAL) {
                        if (this.free(host.x + d.getOffsetX(), host.z + d.getOffsetZ())) {
                            options.add(new Link(host, null, host.x, host.z, d, role == RoomRole.SECRET));
                        }
                    }
                }
                if (options.isEmpty()) {
                    if (role == RoomRole.SECRET) {
                        return false;
                    }
                    continue;
                }
                Link pick = options.get(random.nextInt(options.size()));
                Room room = this.add(role, pick.ax() + pick.dir().getOffsetX(), pick.az() + pick.dir().getOffsetZ(), 1);
                this.links.add(new Link(pick.a(), room, pick.ax(), pick.az(), pick.dir(), pick.hidden()));
            }
            return true;
        }
    }

    /** A finished plan: the pieces to build, made once and handed to the structure start. */
    public static final class Layout {
        private final List<DungeonPiece> pieces = new ArrayList<>();
        private final List<Room> rooms;

        Layout(DungeonPiece.Info info, Site site, Grid grid, Random random) {
            this.rooms = grid.rooms;
            // Doors first: a room's shape (the trap corridor's direction) depends on them.
            for (Link link : grid.links) {
                int along = link.dir().getAxis() == Direction.Axis.Z ? site.cellCentreX(link.ax()) : site.cellCentreZ(link.az());
                link.a().doors.add(new DungeonPiece.Door(link.dir(), along, link.hidden()));
                link.b().doors.add(new DungeonPiece.Door(link.dir().getOpposite(), along, false));
            }
            for (Room room : grid.rooms) {
                this.pieces.add(this.roomPiece(info, site, room, random));
                if (room.role == RoomRole.ENTRANCE) {
                    this.pieces.add(new EntrancePiece(info, site.cellCentreX(room.x), site.cellCentreZ(room.z), site.top(), random.nextLong()));
                }
            }
            for (Link link : grid.links) {
                int along = link.dir().getAxis() == Direction.Axis.Z ? site.cellCentreX(link.ax()) : site.cellCentreZ(link.az());
                DungeonCorridorPiece corridor = DungeonCorridorPiece.between(info, link.a().box, link.b().box, link.dir().getAxis(),
                        along, random.nextLong());
                if (corridor != null) {
                    this.pieces.add(corridor);
                }
            }
            // Every room carries the whole plan, so its ward can record the dungeon on its own.
            NbtCompound summary = this.summary(info, site);
            for (DungeonPiece piece : this.pieces) {
                if (piece.roomId() >= 0) {
                    piece.setSummary(summary);
                }
            }
        }

        private NbtCompound summary(DungeonPiece.Info info, Site site) {
            List<DungeonState.Room> rooms = new ArrayList<>();
            for (Room room : this.rooms) {
                rooms.add(new DungeonState.Room(room.id, room.role, room.box));
            }
            BlockBox bounds = StructurePiece.boundingBox(this.pieces.stream().map(piece -> (StructurePiece) piece));
            return new DungeonState(info.startKey(), info.theme(), info.tier(), bounds, site.entrance(), info.floorY(), rooms).toNbt();
        }

        private DungeonPiece roomPiece(DungeonPiece.Info info, Site site, Room room, Random random) {
            long seed = random.nextLong();
            int cx = site.cellCentreX(room.x);
            int cz = site.cellCentreZ(room.z);
            if (room.role == RoomRole.ENTRANCE) {
                StairPiece stair = new StairPiece(info, cx, cz, site.top(), seed, room.id, room.doors.get(0).side());
                room.box = stair.getBoundingBox();
                return stair;
            }
            Size size = size(room.role);
            int width = size.width();
            int depth = size.width();
            if (room.span == 2) {
                cx = site.cellMinX(room.x) + CELL - 1;
                cz = site.cellMinZ(room.z) + CELL - 1;
            }
            if (room.role == RoomRole.TRAP_CORRIDOR) {
                boolean alongX = room.doors.get(0).side().getAxis() == Direction.Axis.X;
                width = alongX ? CELL : size.width();
                depth = alongX ? size.width() : CELL;
            }
            BlockBox box = DungeonPiece.roomBox(cx, cz, width, depth, info.floorY(), size.height());
            room.box = box;
            int h = size.height();
            return switch (room.role) {
                case ANTECHAMBER -> new AntechamberPiece(info, box, seed, room.id, room.role, h, room.doors);
                case ENCOUNTER_SMALL, ENCOUNTER_LARGE -> new EncounterRoomPiece(info, box, seed, room.id, room.role, h, room.doors);
                case TRAP_CORRIDOR -> new TrapCorridorPiece(info, box, seed, room.id, room.role, h, room.doors);
                case GATE -> new GateRoomPiece(info, box, seed, room.id, room.role, h, room.doors);
                case PUZZLE -> new PuzzleRoomPiece(info, box, seed, room.id, room.role, h, room.doors);
                case CHAMPION -> new ChampionRoomPiece(info, box, seed, room.id, room.role, h, room.doors);
                case BOSS -> new BossRoomPiece(info, box, seed, room.id, room.role, h, room.doors);
                case VAULT, SIDE_VAULT -> new VaultPiece(info, box, seed, room.id, room.role, h, room.doors);
                case SECRET -> new SecretRoomPiece(info, box, seed, room.id, room.role, h, room.doors);
                case ENTRANCE -> throw new IllegalStateException();
            };
        }

        public void addPieces(StructurePiecesCollector collector) {
            this.pieces.forEach(collector::addPiece);
        }

        public int roomCount() {
            return this.rooms.size();
        }

        /** Room roles in id order, for checks and tests. */
        public List<RoomRole> roles() {
            return this.rooms.stream().map(room -> room.role).toList();
        }
    }

    /**
     * Where a dungeon goes: the grid's north-west corner, the ground at the entrance (the middle of
     * the grid), and the floor level.
     */
    public record Site(long startKey, int gridMinX, int gridMinZ, int top, int floorY) {
        int cellMinX(int cell) {
            return this.gridMinX + cell * CELL;
        }

        int cellMinZ(int cell) {
            return this.gridMinZ + cell * CELL;
        }

        int cellCentreX(int cell) {
            return this.cellMinX(cell) + CELL / 2 - 1;
        }

        int cellCentreZ(int cell) {
            return this.cellMinZ(cell) + CELL / 2 - 1;
        }

        /** The top of the entrance stair. */
        public BlockPos entrance() {
            return new BlockPos(this.cellCentreX(MIDDLE), this.top + 1, this.cellCentreZ(MIDDLE));
        }
    }

    /** The terrain over the grid, from the noise generator before anything is built. */
    static final class Terrain {
        /** Ground samples per side (5 x 5 over the grid). */
        private static final int SAMPLES = 5;
        /** At most this many samples may be under water (or too shallow); the rest must be dry land. */
        private static final int MAX_WET = 2;

        private final Structure.Context context;

        Terrain(Structure.Context context) {
            this.context = context;
        }

        private int height(int x, int z, Heightmap.Type type) {
            return this.context.chunkGenerator().getHeight(x, z, type, this.context.world(), this.context.noiseConfig()) - 1;
        }

        Optional<Site> findSite(DungeonTheme theme) {
            ChunkPos chunk = this.context.chunkPos();
            int gridMinX = chunk.getStartX() - MIDDLE * CELL;
            int gridMinZ = chunk.getStartZ() - MIDDLE * CELL;
            int ex = gridMinX + MIDDLE * CELL + CELL / 2 - 1;
            int ez = gridMinZ + MIDDLE * CELL + CELL / 2 - 1;
            int seaLevel = this.context.chunkGenerator().getSeaLevel();
            // The entrance: dry land in an allowed biome
            int top = this.height(ex, ez, Heightmap.Type.WORLD_SURFACE_WG);
            if (top != this.height(ex, ez, Heightmap.Type.OCEAN_FLOOR_WG) || top < seaLevel) {
                return Optional.empty();
            }
            RegistryEntry<Biome> biome = this.context.biomeSource().getBiome(BiomeCoords.fromBlock(ex), BiomeCoords.fromBlock(top),
                    BiomeCoords.fromBlock(ez), this.context.noiseConfig().getMultiNoiseSampler());
            if (!this.context.biomePredicate().test(biome)) {
                return Optional.empty();
            }
            // The whole footprint has to be well under dry ground
            int lowest = top;
            int wet = 0;
            int step = (GRID * CELL - 8) / (SAMPLES - 1);
            for (int i = 0; i < SAMPLES; i++) {
                for (int j = 0; j < SAMPLES; j++) {
                    int x = gridMinX + 4 + i * step;
                    int z = gridMinZ + 4 + j * step;
                    int surface = this.height(x, z, Heightmap.Type.WORLD_SURFACE_WG);
                    int floor = this.height(x, z, Heightmap.Type.OCEAN_FLOOR_WG);
                    if (floor < surface) {
                        wet++;
                    }
                    lowest = Math.min(lowest, floor);
                }
            }
            if (wet > MAX_WET) {
                return Optional.empty();
            }
            int floorY = Math.min(lowest, seaLevel) - DEPTH;
            if (floorY - 4 <= this.context.world().getBottomY()) {
                return Optional.empty();
            }
            return Optional.of(new Site(chunk.toLong(), gridMinX, gridMinZ, top, floorY));
        }
    }
}
