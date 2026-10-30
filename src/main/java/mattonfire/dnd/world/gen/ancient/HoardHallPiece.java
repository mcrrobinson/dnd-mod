package mattonfire.dnd.world.gen.ancient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mattonfire.dnd.classes.Blocks.TrapBlocks;
import mattonfire.dnd.classes.Blocks.TrapKind;
import mattonfire.dnd.classes.Blocks.TrapTriggerBlockEntity;
import mattonfire.dnd.classes.Registry.DwarvenBlocks;
import mattonfire.dnd.world.gen.PieceBlocks;
import net.minecraft.block.AmethystClusterBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.PointedDripstoneBlock;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.SnowBlock;
import net.minecraft.block.enums.Thickness;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.EulerAngle;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationPropertyHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

/**
 * Everything under the caldera (design 10, 6.3 and 6.4).
 * <ul>
 * <li><b>The stairwell</b>: a door in the perch's plinth opens on a stair, 3 wide and 3 high, that winds round a core
 * under the perch and comes out into the hall {@value AncientLairLayout#HALL_DEPTH} blocks down. The dragon's 4x4 body
 * can't follow. It has two traps (tier {@value #TRAP_TIER}, DC 15): a dart trap on the first landing and a covered
 * pit on the far side.</li>
 * <li><b>Kobold quarters</b>, two rooms off the stair: the barracks (9x7x4: hay beds, a weapon rack) and the trap
 * workshop (7x5x4, a {@code chests/kobold_workshop} chest).</li>
 * <li><b>The hoard hall</b>: a dome of polished blackstone, radius {@value AncientLairLayout#HALL_RADIUS} and
 * {@value AncientLairLayout#HALL_HEIGHT} high, on six basalt pillars. Coins over most of its floor, heaps of gold
 * set with gems, the dragon's sleeping hollow, fallen heroes in gold and chain among the coins, four hoard chests and
 * the Hoard Coffer on a gold dais behind the hollow.</li>
 * </ul>
 * Kobold guard posts (markers, {@link AncientLairGuards}): four in the barracks and two at the foot of the stair.
 * <p>
 * The stair is worked out in the stairwell's frame ({@link AncientLairLayout#stair}): its centre line runs round a
 * square 3 out from the perch's middle, dropping a block a step except on the corner landings and the pit's flat; a
 * step's floor is a stair block whenever the next one is lower, so it walks both ways without jumping.
 */
public class HoardHallPiece extends StructurePiece {
    static final int TRAP_TIER = 3;

    /** What a tunnel floor block is. */
    enum Kind { FULL, STAIR, DART, PIT, CRUMBLE }

    /** The stairwell's own directions: {@code a} is towards the back, {@code b} to the right. */
    enum Way {
        PLUS_A(1, 0), MINUS_A(-1, 0), PLUS_B(0, 1), MINUS_B(0, -1);

        final int da;
        final int db;

        Way(int da, int db) {
            this.da = da;
            this.db = db;
        }

        static Way of(int da, int db) {
            for (Way way : values()) {
                if (way.da == Integer.signum(da) && way.db == Integer.signum(db)) {
                    return way;
                }
            }
            return PLUS_B;
        }

        Way opposite() {
            return of(-this.da, -this.db);
        }

        Direction world(AncientLairLayout layout) {
            return switch (this) {
                case PLUS_A -> layout.back();
                case MINUS_A -> layout.front();
                case PLUS_B -> layout.right();
                case MINUS_B -> layout.right().getOpposite();
            };
        }
    }

    /** One floor block of the tunnel, in the stairwell's frame; {@code up} is the way a stair climbs. */
    record Step(int a, int b, int h, Kind kind, Way up) {
    }

    /** The tunnel's centre line (where the survey walks), in order from the door down. Filled by planTunnel. */
    static final List<int[]> CENTRE_LINE = new ArrayList<>();
    /** The tunnel's floor blocks, in order from the door down. The same for every lair. */
    static final List<Step> TUNNEL = planTunnel();
    /** The dart trap's launcher, in the stairwell wall beside the first landing, and the way it shoots. */
    static final int[] DART_LAUNCHER = { -3, 5, 1 };
    /** The pit's ladder, in the stairwell's frame, and the depth of the pit under its floor. */
    static final int[] PIT_LADDER = { -1, -4, -9 };
    static final int PIT_DEPTH = 4;
    /** The stairwell's solid block: |a|, |b| up to this, from {@value #WELL_BOTTOM} up to the crater floor. */
    static final int WELL_HALF = AncientLairLayout.PLINTH_HALF;
    static final int WELL_BOTTOM = -15;

    private final AncientLairLayout layout;

    public HoardHallPiece(AncientLairLayout layout) {
        super(AncientLairStructures.HOARD_HALL, 0, box(layout));
        this.layout = layout;
    }

    public HoardHallPiece(NbtCompound nbt) {
        super(AncientLairStructures.HOARD_HALL, nbt);
        this.layout = AncientLairLayout.readNbt(nbt);
    }

    private static BlockBox box(AncientLairLayout layout) {
        List<BlockPos> corners = new ArrayList<>();
        int reach = AncientLairLayout.HALL_RADIUS + 3;
        BlockPos hall = layout.hallCenter();
        corners.add(hall.add(-reach, -3, -reach));
        corners.add(hall.add(reach, AncientLairLayout.HALL_HEIGHT + 4, reach));
        corners.add(layout.stair(-WELL_HALF - 12, -WELL_HALF - 9, WELL_BOTTOM - 6));
        corners.add(layout.stair(WELL_HALF + 2, WELL_HALF + 11, AncientLairLayout.PLINTH_HEIGHT));
        return BlockBox.encompassPositions(corners).orElseThrow();
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        this.layout.writeNbt(nbt);
    }

    public AncientLairLayout layout() {
        return this.layout;
    }

    // ---------------------------------------------------------------- the tunnel's plan

    private static boolean corner(int a, int b) {
        return Math.abs(a) == 3 && Math.abs(b) == 3;
    }

    /** Which corner landing (3x3) a centre-line point is on, or 0 for none. */
    private static int square(int a, int b) {
        if (Math.abs(a) >= 2 && Math.abs(b) >= 2) {
            return 1 + (a > 0 ? 1 : 0) + (b > 0 ? 2 : 0);
        }
        return 0;
    }

    private static List<Step> planTunnel() {
        // The centre line: a, b, h, width (3, or 1 for the turn into the exit)
        List<int[]> line = new ArrayList<>();
        line.add(new int[] { -3, -1, 0, 3 });
        line.add(new int[] { -3, 0, 0, 3 });
        line.add(new int[] { -3, 1, 0, 3 });
        int[][] legs = { { 0, 1, 3 }, { 1, 0, 3 }, { 0, -1, -3 }, { -1, 0, -3 }, { 0, 1, 0 } };
        // The far side's first three steps stay level for the pit, and the landing after it too.
        int[][] level = { { 1, -3 }, { 0, -3 }, { -1, -3 }, { -2, -3 } };
        int a = -3;
        int b = 1;
        int h = 0;
        for (int[] leg : legs) {
            while ((leg[0] != 0 ? a : b) != leg[2]) {
                int na = a + leg[0];
                int nb = b + leg[1];
                boolean flat = false;
                for (int[] l : level) {
                    flat |= l[0] == na && l[1] == nb;
                }
                int was = square(a, b);
                if (!flat && (square(na, nb) == 0 || square(na, nb) != was)) {
                    h--;
                }
                a = na;
                b = nb;
                line.add(new int[] { a, b, h, 3 });
            }
        }
        // Out through the front of the stairwell and down into the hall.
        line.add(new int[] { -4, 0, h, 1 });
        for (a = -5; h > -AncientLairLayout.HALL_DEPTH; a--) {
            line.add(new int[] { a, 0, --h, 3 });
        }
        line.add(new int[] { a, 0, h, 3 });
        line.add(new int[] { a - 1, 0, h, 3 });
        CENTRE_LINE.addAll(line);

        Map<Long, Step> cells = new LinkedHashMap<>();
        for (int i = 0; i < line.size(); i++) {
            int[] p = line.get(i);
            int[] prev = i > 0 ? line.get(i - 1) : new int[] { p[0], p[1] - 1, p[2], 3 };
            int[] next = i + 1 < line.size() ? line.get(i + 1) : null;
            Way along = Way.of(p[0] - prev[0], p[1] - prev[1]);
            Kind kind = next != null && next[2] < p[2] ? Kind.STAIR : Kind.FULL;
            Way up = next != null ? Way.of(p[0] - next[0], p[1] - next[1]) : along;
            List<int[]> cross = new ArrayList<>();
            if (p[3] == 1) {
                cross.add(new int[] { p[0], p[1] });
            } else if (corner(p[0], p[1])) {
                for (int da = -1; da <= 1; da++) {
                    for (int db = -1; db <= 1; db++) {
                        cross.add(new int[] { p[0] + da, p[1] + db });
                    }
                }
            } else {
                for (int s = -1; s <= 1; s++) {
                    cross.add(new int[] { p[0] + along.db * s, p[1] + along.da * s });
                }
            }
            for (int[] c : cross) {
                cells.put(BlockPos.asLong(c[0], p[2], c[1]), new Step(c[0], c[1], p[2], kind, up));
            }
        }
        // The traps: a dart plate on the first landing, a pit across the level stretch on the far side.
        cells.put(BlockPos.asLong(-3, -1, 3), new Step(-3, 3, -1, Kind.DART, Way.PLUS_B));
        cells.put(BlockPos.asLong(1, -9, -3), new Step(1, -3, -9, Kind.PIT, Way.PLUS_B));
        for (int ca = -1; ca <= 0; ca++) {
            for (int cb = -4; cb <= -2; cb++) {
                cells.put(BlockPos.asLong(ca, -9, cb), new Step(ca, cb, -9, Kind.CRUMBLE, Way.PLUS_B));
            }
        }
        return List.copyOf(cells.values());
    }

    // ---------------------------------------------------------------- positions

    /** World positions along the tunnel's centre line where you stand, from the door down (for the survey). */
    public List<BlockPos> walk() {
        List<BlockPos> out = new ArrayList<>();
        for (int[] p : CENTRE_LINE) {
            out.add(this.layout.stair(p[0], p[1], p[2] + 1));
        }
        return out;
    }

    /** Where you stand just inside the stairwell's door. */
    public BlockPos door() {
        return this.layout.stair(-3, 0, 1);
    }

    /** Where you stand outside the door, on the crater floor. */
    public BlockPos outsideDoor() {
        return this.layout.stair(-WELL_HALF - 2, 0, 1);
    }

    public BlockPos dartTrigger() {
        return this.layout.stair(-3, 3, -1);
    }

    public BlockPos pitTrigger() {
        return this.layout.stair(1, -3, -9);
    }

    /** The hall's floor, at {@code ha} along the mouth's axis from its middle and {@code hs} to the right. */
    BlockPos hall(int ha, int hs, int dy) {
        return this.layout.at(AncientLairLayout.HALL_OUT + ha, hs, this.layout.hallY() + dy);
    }

    /** Where you stand at the foot of the stair, in the hall. */
    public BlockPos stairFoot() {
        int[] last = CENTRE_LINE.get(CENTRE_LINE.size() - 1);
        return this.layout.stair(last[0], last[1], last[2] + 1);
    }

    /** The four hoard chests. */
    public List<BlockPos> hoardChests() {
        List<BlockPos> out = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            double angle = Math.toRadians(45 + 90 * i);
            out.add(this.hall((int) Math.round(Math.cos(angle) * 8), (int) Math.round(Math.sin(angle) * 8), 1));
        }
        return out;
    }

    public BlockPos coffer() {
        return this.hall(COFFER_OUT, 0, 2);
    }

    public BlockPos workshopChest() {
        return this.layout.stair(5, -13, -8);
    }

    /** Where you stand in the barracks and the workshop. */
    public BlockPos barracks() {
        return this.layout.stair(3, 11, -4);
    }

    public BlockPos workshop() {
        return this.layout.stair(3, -10, -8);
    }

    /** Whether {@code pos} is inside the domed hall (a hoard chest's place). */
    public boolean inHall(BlockPos pos) {
        BlockPos c = this.layout.hallCenter();
        double dx = pos.getX() - c.getX();
        double dz = pos.getZ() - c.getZ();
        int dy = pos.getY() - c.getY();
        return dy >= -1 && dy <= AncientLairLayout.HALL_HEIGHT + 1
                && dx * dx + dz * dz <= (AncientLairLayout.HALL_RADIUS + 1) * (AncientLairLayout.HALL_RADIUS + 1);
    }

    // ---------------------------------------------------------------- building

    private static final int PILLAR_RADIUS = 12;
    private static final int COFFER_OUT = 13;
    private static final int HOLLOW = 5;

    @Override
    public void generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator,
                         Random chunkRandom, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        if (!this.boundingBox.intersects(chunkBox)) {
            return;
        }
        this.shells(world, chunkBox);
        this.dome(world, chunkBox);
        this.furnishHall(world, chunkBox);
        this.tunnel(world, chunkBox);
        this.barracksRoom(world, chunkBox);
        this.workshopRoom(world, chunkBox);
    }

    private void set(StructureWorldAccess world, BlockBox chunkBox, BlockPos pos, BlockState state) {
        if (chunkBox.contains(pos)) {
            world.setBlockState(pos, state, PieceBlocks.UPDATE);
        }
    }

    private BlockState brick(BlockPos at) {
        float n = this.layout.hash(at.getX(), at.getY(), at.getZ(), 61);
        return (n < 0.15F ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS : n < 0.3F ? Blocks.BLACKSTONE
                : Blocks.POLISHED_BLACKSTONE_BRICKS).getDefaultState();
    }

    /** Solid blocks round the stairwell, the stair down into the hall and the two rooms, carved out after. */
    private void shells(StructureWorldAccess world, BlockBox chunkBox) {
        this.solid(world, chunkBox, -WELL_HALF, WELL_HALF, -WELL_HALF, WELL_HALF, WELL_BOTTOM, 0);
        this.solid(world, chunkBox, -WELL_HALF - 12, -WELL_HALF - 1, -2, 2, -AncientLairLayout.HALL_DEPTH - 1, -7);
        // Barracks and workshop (the barracks' ceiling is the crater floor)
        this.solid(world, chunkBox, -1, 7, WELL_HALF + 1, WELL_HALF + 10, -6, -1);
        this.solid(world, chunkBox, 0, 6, -WELL_HALF - 8, -WELL_HALF - 1, -10, -4);
    }

    private void solid(StructureWorldAccess world, BlockBox chunkBox, int a0, int a1, int b0, int b1, int h0, int h1) {
        for (int a = a0; a <= a1; a++) {
            for (int b = b0; b <= b1; b++) {
                for (int h = h0; h <= h1; h++) {
                    BlockPos at = this.layout.stair(a, b, h);
                    this.set(world, chunkBox, at, this.brick(at));
                }
            }
        }
    }

    /** The dome: air inside, a shell of blackstone round it, a polished floor. */
    private void dome(StructureWorldAccess world, BlockBox chunkBox) {
        AncientLairLayout l = this.layout;
        BlockPos c = l.hallCenter();
        int reach = AncientLairLayout.HALL_RADIUS + 3;
        int minX = Math.max(c.getX() - reach, chunkBox.getMinX());
        int maxX = Math.min(c.getX() + reach, chunkBox.getMaxX());
        int minZ = Math.max(c.getZ() - reach, chunkBox.getMinZ());
        int maxZ = Math.min(c.getZ() + reach, chunkBox.getMaxZ());
        double rr = AncientLairLayout.HALL_RADIUS * AncientLairLayout.HALL_RADIUS;
        double hh = AncientLairLayout.HALL_HEIGHT * AncientLairLayout.HALL_HEIGHT;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                double r2 = (x - c.getX()) * (x - c.getX()) + (z - c.getZ()) * (z - c.getZ());
                for (int dy = -2; dy <= AncientLairLayout.HALL_HEIGHT + 4; dy++) {
                    double e = r2 / rr + (dy > 0 ? dy * dy / hh : 0.0D);
                    pos.set(x, c.getY() + dy, z);
                    if (dy >= 1 && e <= 1.0D) {
                        world.setBlockState(pos, Blocks.AIR.getDefaultState(), PieceBlocks.UPDATE);
                    } else if (dy == 0 && e <= 1.0D) {
                        float n = l.hash(x, pos.getY(), z, 63);
                        world.setBlockState(pos, (n < 0.12F ? Blocks.GILDED_BLACKSTONE : n < 0.3F
                                ? Blocks.POLISHED_BLACKSTONE_BRICKS : Blocks.POLISHED_BLACKSTONE).getDefaultState(),
                                PieceBlocks.UPDATE);
                    } else if (e <= 1.32D) {
                        world.setBlockState(pos, this.brick(pos), PieceBlocks.UPDATE);
                    }
                }
            }
        }
    }

    /** The hoard: the hollow, the pillars, the coffer's dais, chests, heaps, coins, fallen heroes and lanterns. */
    private void furnishHall(StructureWorldAccess world, BlockBox chunkBox) {
        AncientLairLayout l = this.layout;
        Random random = Random.create(l.seed() ^ 0x40A2D4A11L);
        // Heaps of gold, set with gems, clear of the chests, pillars, dais and the way in.
        List<int[]> heaps = new ArrayList<>();
        int heapCount = 6 + random.nextInt(5);
        for (int tries = 0; tries < 300 && heaps.size() < heapCount; tries++) {
            int ha = -15 + random.nextInt(31);
            int hs = -15 + random.nextInt(31);
            int radius = 2 + random.nextInt(2);
            double d = Math.sqrt(ha * ha + hs * hs);
            if (d < HOLLOW + 3 || d + radius > AncientLairLayout.HALL_RADIUS - 2 || this.blocked(ha, hs, radius + 2)) {
                continue;
            }
            boolean clear = true;
            for (int[] other : heaps) {
                clear &= Math.hypot(ha - other[0], hs - other[1]) >= radius + other[2] + 1;
            }
            if (clear) {
                heaps.add(new int[] { ha, hs, radius });
            }
        }
        int gems = 2 + random.nextInt(3);
        // The floor: coins (or the hollow) everywhere else.
        int reach = AncientLairLayout.HALL_RADIUS;
        for (int ha = -reach; ha <= reach; ha++) {
            for (int hs = -reach; hs <= reach; hs++) {
                double d = Math.sqrt(ha * ha + hs * hs);
                if (d > reach - 0.5D) {
                    continue;
                }
                BlockPos floor = this.hall(ha, hs, 0);
                if (!chunkBox.contains(floor)) {
                    continue;
                }
                if (d <= HOLLOW + 0.5D) {
                    // The sleeping hollow: scooped out, scorched, bare of gold.
                    float n = l.hash(floor.getX(), 0, floor.getZ(), 65);
                    this.set(world, chunkBox, floor, Blocks.AIR.getDefaultState());
                    this.set(world, chunkBox, floor.down(), (n < 0.3F ? Blocks.MAGMA_BLOCK : n < 0.6F ? Blocks.BLACKSTONE
                            : Blocks.SMOOTH_BASALT).getDefaultState());
                    continue;
                }
                if (this.blocked(ha, hs, 0) || inLane(ha, hs)) {
                    continue;
                }
                float cover = l.smooth(floor.getX(), floor.getZ(), 5, 67);
                if (cover < 0.7F) {
                    int layers = 1 + (int) (l.smooth(floor.getX(), floor.getZ(), 4, 69) * 7.99F);
                    this.set(world, chunkBox, floor.up(), coins(layers));
                }
            }
        }
        for (int[] heap : heaps) {
            this.heap(world, chunkBox, heap, gems-- > 0 ? random.nextBoolean() : null);
        }
        this.pillars(world, chunkBox);
        // The coffer's dais, behind the hollow, opposite the way in.
        for (int da = -1; da <= 1; da++) {
            for (int ds = -1; ds <= 1; ds++) {
                this.set(world, chunkBox, this.hall(COFFER_OUT + da, ds, 1),
                        (da == 0 && ds == 0 ? Blocks.GILDED_BLACKSTONE : Blocks.GOLD_BLOCK).getDefaultState());
            }
        }
        long cofferSeed = random.nextLong();
        this.chest(world, chunkBox, this.coffer(), l.back(), AncientLairStructures.cofferLoot(l.color()), cofferSeed,
                Text.translatable("container.dndclasses.hoard_coffer"));
        for (BlockPos chest : this.hoardChests()) {
            long seed = random.nextLong();
            BlockPos c = l.hallCenter();
            Direction facing = Direction.getFacing(c.getX() - chest.getX(), 0, c.getZ() - chest.getZ());
            this.set(world, chunkBox, chest.down(), Blocks.GOLD_BLOCK.getDefaultState());
            this.chest(world, chunkBox, chest, facing, AncientLairStructures.hoardLoot(l.color()), seed, null);
        }
        // Fallen heroes among the coins, each with a skull or bones beside them.
        int heroes = 4 + random.nextInt(3);
        List<int[]> placed = new ArrayList<>();
        for (int tries = 0; tries < 200 && placed.size() < heroes; tries++) {
            int ha = -14 + random.nextInt(29);
            int hs = -14 + random.nextInt(29);
            float yaw = random.nextFloat() * 360.0F;
            boolean gold = random.nextBoolean();
            int armour = random.nextInt(16);
            double d = Math.sqrt(ha * ha + hs * hs);
            if (d < HOLLOW + 2 || d > AncientLairLayout.HALL_RADIUS - 3 || this.blocked(ha, hs, 1) || inLane(ha, hs)) {
                continue;
            }
            boolean clear = true;
            for (int[] other : heaps) {
                clear &= Math.hypot(ha - other[0], hs - other[1]) > other[2] + 1;
            }
            for (int[] other : placed) {
                clear &= Math.hypot(ha - other[0], hs - other[1]) >= 4;
            }
            if (!clear) {
                continue;
            }
            placed.add(new int[] { ha, hs });
            this.set(world, chunkBox, this.hall(ha, hs, 1), coins(1));
            this.hero(world, chunkBox, this.hall(ha, hs, 1), yaw, gold, armour);
            BlockPos beside = this.hall(ha + 1, hs, 1);
            this.set(world, chunkBox, beside, (armour & 1) == 0
                    ? Blocks.SKELETON_SKULL.getDefaultState().with(SkullBlock.ROTATION,
                            RotationPropertyHelper.fromYaw(yaw + 40.0F))
                    : Blocks.BONE_BLOCK.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.X));
        }
        // Lanterns on chains from the dome.
        for (int i = 0; i < 8; i++) {
            int radius = i < 4 ? 6 : 14;
            double angle = Math.toRadians(i * 90 + (i < 4 ? 0 : 45));
            int ha = (int) Math.round(Math.cos(angle) * radius);
            int hs = (int) Math.round(Math.sin(angle) * radius);
            double r = Math.sqrt(ha * ha + hs * hs) / AncientLairLayout.HALL_RADIUS;
            int ceiling = (int) Math.floor(AncientLairLayout.HALL_HEIGHT * Math.sqrt(Math.max(0.0D, 1.0D - r * r)));
            for (int dy = ceiling; dy > ceiling - 3; dy--) {
                this.set(world, chunkBox, this.hall(ha, hs, dy), Blocks.CHAIN.getDefaultState());
            }
            this.set(world, chunkBox, this.hall(ha, hs, ceiling - 3), PieceBlocks.lantern(true));
        }
        // Guard posts at the foot of the stair.
        for (int s = -1; s <= 1; s += 2) {
            BlockPos at = this.hall(-8, 3 * s, 1);
            if (chunkBox.contains(at)) {
                AncientLairGuards.placePost(world, at, s < 0 ? "warrior" : "trapmaker", l.lairCentre());
            }
        }
    }

    /** The way in: from the stair's foot to the hollow, kept clear of gold. */
    private static boolean inLane(int ha, int hs) {
        return Math.abs(hs) <= 2 && ha < -HOLLOW;
    }

    /** Within {@code margin} of a pillar, a hoard chest or the coffer's dais. */
    private boolean blocked(int ha, int hs, int margin) {
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(30 + 60 * i);
            if (Math.hypot(ha - Math.cos(angle) * PILLAR_RADIUS, hs - Math.sin(angle) * PILLAR_RADIUS) <= 2 + margin) {
                return true;
            }
        }
        for (int i = 0; i < 4; i++) {
            double angle = Math.toRadians(45 + 90 * i);
            if (Math.hypot(ha - Math.round(Math.cos(angle) * 8), hs - Math.round(Math.sin(angle) * 8)) <= 1 + margin) {
                return true;
            }
        }
        return Math.abs(ha - COFFER_OUT) <= 1 + margin && Math.abs(hs) <= 1 + margin;
    }

    /** Six basalt pillars, fluted between a square foot and head, with amethyst growing on their sides. */
    private void pillars(StructureWorldAccess world, BlockBox chunkBox) {
        AncientLairLayout l = this.layout;
        int head = AncientLairLayout.HALL_HEIGHT - 4;
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(30 + 60 * i);
            int pa = (int) Math.round(Math.cos(angle) * PILLAR_RADIUS);
            int ps = (int) Math.round(Math.sin(angle) * PILLAR_RADIUS);
            for (int dy = 1; dy <= AncientLairLayout.HALL_HEIGHT; dy++) {
                for (int da = -1; da <= 1; da++) {
                    for (int ds = -1; ds <= 1; ds++) {
                        boolean core = da == 0 || ds == 0;
                        if (!core && dy > 2 && dy < head) {
                            continue;
                        }
                        BlockPos at = this.hall(pa + da, ps + ds, dy);
                        // Up to the dome; the shell takes over above
                        if (chunkBox.contains(at) && (world.getBlockState(at).isAir()
                                || world.getBlockState(at).isOf(DwarvenBlocks.TREASURY_COIN_PILE))) {
                            world.setBlockState(at, (dy == 1 || dy == head ? Blocks.POLISHED_BASALT : Blocks.BASALT)
                                    .getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Y), PieceBlocks.UPDATE);
                        }
                    }
                }
                if (dy <= 2 || dy >= head - 1) {
                    continue;
                }
                // Amethyst on the arms of the fluting
                int[][] arms = { { 2, 0 }, { -2, 0 }, { 0, 2 }, { 0, -2 } };
                Direction[] out = { l.front(), l.back(), l.right(), l.right().getOpposite() };
                for (int k = 0; k < 4; k++) {
                    BlockPos at = this.hall(pa + arms[k][0], ps + arms[k][1], dy);
                    if (l.hash(at.getX(), at.getY(), at.getZ(), 71) < 0.18F && chunkBox.contains(at)
                            && world.getBlockState(at).isAir()) {
                        world.setBlockState(at, Blocks.AMETHYST_CLUSTER.getDefaultState()
                                .with(AmethystClusterBlock.FACING, out[k]), PieceBlocks.UPDATE);
                    }
                }
            }
        }
    }

    /** A heap of gold and raw gold blocks, a mound up to three high; {@code gem} sets an emerald (true) or diamond in it. */
    private void heap(StructureWorldAccess world, BlockBox chunkBox, int[] heap, Boolean gem) {
        AncientLairLayout l = this.layout;
        int radius = heap[2];
        for (int da = -radius; da <= radius; da++) {
            for (int ds = -radius; ds <= radius; ds++) {
                double d = Math.sqrt(da * da + ds * ds);
                if (d > radius + 0.3D) {
                    continue;
                }
                int height = Math.max(1, (int) Math.round(3.0D * (1.0D - d / (radius + 0.8D))));
                for (int dy = 1; dy <= height; dy++) {
                    BlockPos at = this.hall(heap[0] + da, heap[1] + ds, dy);
                    float n = l.hash(at.getX(), at.getY(), at.getZ(), 73);
                    BlockState state = n < 0.3F ? Blocks.RAW_GOLD_BLOCK.getDefaultState() : Blocks.GOLD_BLOCK.getDefaultState();
                    if (gem != null && da == 0 && ds == 0 && dy == height) {
                        state = (gem ? Blocks.EMERALD_BLOCK : Blocks.DIAMOND_BLOCK).getDefaultState();
                    }
                    this.set(world, chunkBox, at, state);
                }
                this.set(world, chunkBox, this.hall(heap[0] + da, heap[1] + ds, height + 1), coins(1 + (int) (d * 2)));
            }
        }
    }

    private static BlockState coins(int layers) {
        return DwarvenBlocks.TREASURY_COIN_PILE.getDefaultState().with(SnowBlock.LAYERS, MathHelper.clamp(layers, 1, 8));
    }

    private void chest(StructureWorldAccess world, BlockBox chunkBox, BlockPos pos, Direction facing,
                       net.minecraft.util.Identifier loot, long seed, Text name) {
        if (!chunkBox.contains(pos)) {
            return;
        }
        world.setBlockState(pos, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, facing), PieceBlocks.UPDATE);
        if (world.getBlockEntity(pos) instanceof LootableContainerBlockEntity container) {
            container.setLootTable(loot, seed);
            if (name != null) {
                container.setCustomName(name);
            }
        }
    }

    /** A fallen hero: an armour stand slumped among the coins in gold or chain. */
    private void hero(StructureWorldAccess world, BlockBox chunkBox, BlockPos pos, float yaw, boolean gold, int pieces) {
        if (!chunkBox.contains(pos)) {
            return;
        }
        ArmorStandEntity stand = EntityType.ARMOR_STAND.create(world.toServerWorld());
        if (stand == null) {
            return;
        }
        stand.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw, 0.0F);
        Item[] armour = gold
                ? new Item[] { Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS }
                : new Item[] { Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS };
        EquipmentSlot[] slots = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
        for (int i = 0; i < 4; i++) {
            // The chestplate always; the rest now and then
            if (i == 1 || (pieces >> i & 1) == 1) {
                stand.equipStack(slots[i], new ItemStack(armour[i]));
            }
        }
        if ((pieces & 4) != 0) {
            stand.equipStack(EquipmentSlot.MAINHAND, new ItemStack(gold ? Items.GOLDEN_SWORD : Items.IRON_SWORD));
        }
        stand.setShowArms(true);
        stand.setHideBasePlate(true);
        // Slumped: head bowed, arms hanging, legs folded under.
        stand.setHeadRotation(new EulerAngle(45.0F, 15.0F, 0.0F));
        stand.setBodyRotation(new EulerAngle(20.0F, 0.0F, 0.0F));
        stand.setLeftArmRotation(new EulerAngle(-10.0F, 0.0F, -12.0F));
        stand.setRightArmRotation(new EulerAngle(-25.0F, 0.0F, 15.0F));
        stand.setLeftLegRotation(new EulerAngle(-90.0F, -10.0F, 0.0F));
        stand.setRightLegRotation(new EulerAngle(-90.0F, 10.0F, 0.0F));
        world.spawnEntity(stand);
    }

    /** Carves the stair: its air, then its floor, the traps and the door. */
    private void tunnel(StructureWorldAccess world, BlockBox chunkBox) {
        AncientLairLayout l = this.layout;
        for (Step step : TUNNEL) {
            for (int dy = 1; dy <= 3; dy++) {
                this.set(world, chunkBox, l.stair(step.a, step.b, step.h + dy), Blocks.AIR.getDefaultState());
            }
        }
        // The door in the plinth
        for (int a = -WELL_HALF; a <= -WELL_HALF + 1; a++) {
            for (int b = -1; b <= 1; b++) {
                for (int h = 1; h <= 3; h++) {
                    this.set(world, chunkBox, l.stair(a, b, h), Blocks.AIR.getDefaultState());
                }
                this.set(world, chunkBox, l.stair(a, b, 0), Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState());
            }
        }
        this.set(world, chunkBox, l.stair(-WELL_HALF, -2, 3), Blocks.CHISELED_POLISHED_BLACKSTONE.getDefaultState());
        this.set(world, chunkBox, l.stair(-WELL_HALF, 2, 3), Blocks.CHISELED_POLISHED_BLACKSTONE.getDefaultState());
        for (Step step : TUNNEL) {
            BlockPos at = l.stair(step.a, step.b, step.h);
            switch (step.kind) {
                case FULL -> this.set(world, chunkBox, at, Blocks.POLISHED_BLACKSTONE.getDefaultState());
                case STAIR -> this.set(world, chunkBox, at,
                        PieceBlocks.stairs(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, step.up.world(l), false));
                case CRUMBLE -> this.set(world, chunkBox, at, TrapBlocks.CRUMBLING_FLOOR.getDefaultState());
                case DART -> this.dartTrap(world, chunkBox, at);
                case PIT -> this.pitTrap(world, chunkBox, at);
            }
            // Down in the hall the stair stands on a solid ramp.
            if (step.h <= -12) {
                for (int h = step.h - 1; h > -AncientLairLayout.HALL_DEPTH; h--) {
                    BlockPos below = l.stair(step.a, step.b, h);
                    if (chunkBox.contains(below) && !world.getBlockState(below).isOpaqueFullCube(world, below)) {
                        world.setBlockState(below, this.brick(below), PieceBlocks.UPDATE);
                    }
                }
            }
        }
        // Lanterns on two landings
        this.set(world, chunkBox, l.stair(4, 4, -4), PieceBlocks.lantern(false));
        this.set(world, chunkBox, l.stair(-4, -4, -8), PieceBlocks.lantern(false));
        this.set(world, chunkBox, l.stair(4, -4, -8), PieceBlocks.lantern(false));
    }

    private void dartTrap(StructureWorldAccess world, BlockBox chunkBox, BlockPos plate) {
        AncientLairLayout l = this.layout;
        BlockPos launcher = l.stair(DART_LAUNCHER[0], DART_LAUNCHER[1], DART_LAUNCHER[2]);
        this.set(world, chunkBox, launcher, TrapBlocks.DART_LAUNCHER.getDefaultState()
                .with(HorizontalFacingBlock.FACING, Way.MINUS_B.world(l)));
        if (!chunkBox.contains(plate)) {
            return;
        }
        world.setBlockState(plate, TrapBlocks.TRAP_TRIGGER.getDefaultState(), PieceBlocks.UPDATE);
        if (world.getBlockEntity(plate) instanceof TrapTriggerBlockEntity trap) {
            trap.setup(TrapKind.DART, 0L, TRAP_TIER, List.of(launcher), List.of(), null, Direction.NORTH);
        }
    }

    private void pitTrap(StructureWorldAccess world, BlockBox chunkBox, BlockPos plate) {
        AncientLairLayout l = this.layout;
        List<BlockPos> crumbles = new ArrayList<>();
        Direction ladderFacing = Way.PLUS_B.world(l);
        BlockState spike = Blocks.POINTED_DRIPSTONE.getDefaultState()
                .with(PointedDripstoneBlock.VERTICAL_DIRECTION, Direction.UP)
                .with(PointedDripstoneBlock.THICKNESS, Thickness.TIP);
        for (int a = -1; a <= 0; a++) {
            for (int b = -4; b <= -2; b++) {
                crumbles.add(l.stair(a, b, -9));
                boolean ladder = a == PIT_LADDER[0] && b == PIT_LADDER[1];
                for (int d = 1; d <= PIT_DEPTH; d++) {
                    BlockState state = ladder ? Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, ladderFacing)
                            : d == PIT_DEPTH ? spike : Blocks.AIR.getDefaultState();
                    this.set(world, chunkBox, l.stair(a, b, -9 - d), state);
                }
                this.set(world, chunkBox, l.stair(a, b, -9 - PIT_DEPTH - 1), Blocks.POLISHED_BLACKSTONE.getDefaultState());
            }
        }
        if (!chunkBox.contains(plate)) {
            return;
        }
        world.setBlockState(plate, TrapBlocks.TRAP_TRIGGER.getDefaultState(), PieceBlocks.UPDATE);
        if (world.getBlockEntity(plate) instanceof TrapTriggerBlockEntity trap) {
            trap.setup(TrapKind.PIT, 0L, TRAP_TIER, List.of(), crumbles,
                    l.stair(PIT_LADDER[0], PIT_LADDER[1], PIT_LADDER[2]), ladderFacing);
        }
    }

    /** The kobolds' barracks, 9x7x4, off the second landing: hay beds along the walls and a weapon rack. */
    private void barracksRoom(StructureWorldAccess world, BlockBox chunkBox) {
        AncientLairLayout l = this.layout;
        int b0 = WELL_HALF + 1;
        for (int a = 0; a <= 6; a++) {
            for (int b = b0; b <= b0 + 8; b++) {
                for (int h = -4; h <= -1; h++) {
                    this.set(world, chunkBox, l.stair(a, b, h), Blocks.AIR.getDefaultState());
                }
                this.set(world, chunkBox, l.stair(a, b, -5), (a + b) % 5 == 0 ? Blocks.SMOOTH_BASALT.getDefaultState()
                        : Blocks.POLISHED_BLACKSTONE.getDefaultState());
            }
        }
        for (int a = 2; a <= 4; a++) {
            for (int b = WELL_HALF - 1; b < b0; b++) {
                for (int h = -4; h <= -2; h++) {
                    this.set(world, chunkBox, l.stair(a, b, h), Blocks.AIR.getDefaultState());
                }
                this.set(world, chunkBox, l.stair(a, b, -5), Blocks.POLISHED_BLACKSTONE.getDefaultState());
            }
        }
        // Hay beds, two by two along both long walls
        for (int b = b0 + 1; b <= b0 + 7; b += 2) {
            for (int a : new int[] { 0, 6 }) {
                this.set(world, chunkBox, l.stair(a, b, -4), Blocks.HAY_BLOCK.getDefaultState()
                        .with(PillarBlock.AXIS, l.back().getAxis()));
            }
        }
        // The weapon rack at the end wall: posts with swords and a bow hung between them
        Direction toRoom = Way.MINUS_B.world(l);
        for (int a = 1; a <= 5; a++) {
            BlockPos at = l.stair(a, b0 + 8, -4);
            this.set(world, chunkBox, at, (a % 2 == 1 ? Blocks.POLISHED_BLACKSTONE_WALL : Blocks.AIR).getDefaultState());
            if (a % 2 == 0 && chunkBox.contains(at)) {
                ItemFrameEntity frame = new ItemFrameEntity(world.toServerWorld(), l.stair(a, b0 + 8, -3), toRoom);
                frame.setHeldItemStack(new ItemStack(a == 2 ? Items.STONE_SWORD : Items.BOW), false);
                world.spawnEntity(frame);
            }
        }
        this.set(world, chunkBox, l.stair(3, b0 + 4, -1), PieceBlocks.lantern(true));
        int[][] posts = { { 1, b0 + 2 }, { 1, b0 + 6 }, { 5, b0 + 2 }, { 5, b0 + 6 } };
        String[] roles = { "warrior", "warrior", "slinger", "leader" };
        for (int i = 0; i < posts.length; i++) {
            BlockPos at = l.stair(posts[i][0], posts[i][1], -4);
            if (chunkBox.contains(at)) {
                AncientLairGuards.placePost(world, at, roles[i], l.lairCentre());
            }
        }
    }

    /** The trap workshop, 7x5x4, off the third landing: benches, barrels, cobwebs and the workshop chest. */
    private void workshopRoom(StructureWorldAccess world, BlockBox chunkBox) {
        AncientLairLayout l = this.layout;
        int b0 = -WELL_HALF - 1;
        for (int a = 1; a <= 5; a++) {
            for (int b = b0; b >= b0 - 6; b--) {
                for (int h = -8; h <= -5; h++) {
                    this.set(world, chunkBox, l.stair(a, b, h), Blocks.AIR.getDefaultState());
                }
                this.set(world, chunkBox, l.stair(a, b, -9), Blocks.POLISHED_BLACKSTONE.getDefaultState());
            }
        }
        for (int a = 2; a <= 4; a++) {
            for (int b = -WELL_HALF + 1; b > b0; b--) {
                for (int h = -8; h <= -6; h++) {
                    this.set(world, chunkBox, l.stair(a, b, h), Blocks.AIR.getDefaultState());
                }
                this.set(world, chunkBox, l.stair(a, b, -9), Blocks.POLISHED_BLACKSTONE.getDefaultState());
            }
        }
        Direction toDoor = Way.PLUS_B.world(l);
        this.set(world, chunkBox, l.stair(1, b0 - 6, -8), Blocks.CRAFTING_TABLE.getDefaultState());
        this.set(world, chunkBox, l.stair(1, b0 - 5, -8), Blocks.FLETCHING_TABLE.getDefaultState());
        this.set(world, chunkBox, l.stair(1, b0 - 4, -8), Blocks.STONECUTTER.getDefaultState()
                .with(HorizontalFacingBlock.FACING, Way.PLUS_A.world(l)));
        this.set(world, chunkBox, l.stair(5, b0, -8), Blocks.BARREL.getDefaultState());
        this.set(world, chunkBox, l.stair(5, b0 - 1, -8), Blocks.BARREL.getDefaultState());
        this.set(world, chunkBox, l.stair(1, b0 - 6, -5), Blocks.COBWEB.getDefaultState());
        this.set(world, chunkBox, l.stair(5, b0 - 6, -5), Blocks.COBWEB.getDefaultState());
        this.set(world, chunkBox, l.stair(3, b0 - 6, -8), Blocks.DISPENSER.getDefaultState()
                .with(net.minecraft.block.DispenserBlock.FACING, toDoor));
        this.set(world, chunkBox, l.stair(3, b0 - 3, -5), PieceBlocks.lantern(true));
        BlockPos chest = this.workshopChest();
        if (chunkBox.contains(chest)) {
            world.setBlockState(chest, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, Way.MINUS_A.world(l)),
                    PieceBlocks.UPDATE);
            if (world.getBlockEntity(chest) instanceof LootableContainerBlockEntity container) {
                container.setLootTable(AncientLairStructures.WORKSHOP_LOOT,
                        this.layout.seed() ^ chest.asLong());
            }
        }
    }
}
