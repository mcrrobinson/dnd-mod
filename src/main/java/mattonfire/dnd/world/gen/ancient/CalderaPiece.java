package mattonfire.dnd.world.gen.ancient;

import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.classes.Registry.LairBlocks;
import mattonfire.dnd.entity.boss.StructureBosses;
import mattonfire.dnd.world.gen.PieceBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.SnowBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

/**
 * The caldera (design 10, 6.2): a volcanic cone of blackstone, basalt and tuff in bands, veined with magma, built up
 * from the terrain. Its foot is radius {@value AncientLairLayout#BASE_RADIUS}, its rim radius
 * {@value AncientLairLayout#RIM_RADIUS}; the crater floor (radius {@value AncientLairLayout#FLOOR_RADIUS}) sits
 * {@value AncientLairLayout#CRATER_DEPTH} below the rim and is open to the sky, so the dragon can fly in and out.
 * <ul>
 * <li>The floor: basalt and smooth basalt with patches of magma, shed red scales and charred bones, three lava pools
 * sunk into it behind basalt kerbs, and four to six obsidian spires.</li>
 * <li>The Perch: a 9x9 plateau on a basalt pillar {@value AncientLairLayout#PERCH_HEIGHT} blocks tall, on a 13x13
 * plinth, on the side away from the mouth. The dragon rests on it. The stairwell to the hoard hall opens in the
 * plinth ({@link HoardHallPiece}).</li>
 * <li>The Mouth: a gash 7 wide through the rim on the downhill side, with a stair down the cone to the ground.</li>
 * </ul>
 * Fire safety: nothing in it burns, and the lava lies in hollows of the floor with solid stone on every side and
 * below, so it never flows.
 * <p>
 * Built a chunk at a time: every choice comes from the layout's seed and the position, never the chunk.
 */
public class CalderaPiece extends StructurePiece {
    /** A lava pool, in the lair's frame. */
    public record Pool(int along, int side, int radius) {
    }

    /** An obsidian spire (2x2), in the lair's frame. */
    public record Spire(int along, int side, int height) {
    }

    /** A kobold guard post on the crater floor. */
    public record Post(int along, int side, String role) {
    }

    private static final int MIN = Integer.MIN_VALUE;
    /** How far above the crater floor the column scan starts: the tallest hills cut away above the crater. */
    private static final int SCAN_UP = 100;
    /** The fill under the cone stops this far down, whatever is below. */
    private static final int MAX_FILL = 90;
    private static final String[] CRATER_ROLES = { "warrior", "warrior", "slinger", "slinger", "trapmaker", "warrior" };

    private final AncientLairLayout layout;
    private final List<Pool> pools = new ArrayList<>();
    private final List<Spire> spires = new ArrayList<>();
    private final List<Post> posts = new ArrayList<>();

    public CalderaPiece(AncientLairLayout layout) {
        super(AncientLairStructures.CALDERA, 0, box(layout));
        this.layout = layout;
        this.plan();
    }

    public CalderaPiece(NbtCompound nbt) {
        super(AncientLairStructures.CALDERA, nbt);
        this.layout = AncientLairLayout.readNbt(nbt);
        this.plan();
    }

    private static BlockBox box(AncientLairLayout layout) {
        BlockPos c = layout.center();
        int r = AncientLairLayout.MOUTH_REACH;
        return new BlockBox(c.getX() - r, layout.base() - 20, c.getZ() - r, c.getX() + r,
                Math.max(layout.rimY() + 4, layout.floorY() + AncientLairLayout.PERCH_HEIGHT + 12), c.getZ() + r);
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        this.layout.writeNbt(nbt);
    }

    public AncientLairLayout layout() {
        return this.layout;
    }

    public List<Pool> pools() {
        return this.pools;
    }

    public List<Spire> spires() {
        return this.spires;
    }

    public List<Post> posts() {
        return this.posts;
    }

    // ---------------------------------------------------------------- the plan

    /** Places the pools, spires and guard posts from the layout's seed: the same every time. */
    private void plan() {
        Random random = Random.create(this.layout.seed() ^ 0xCA1DE7AL);
        for (int tries = 0; tries < 200 && this.pools.size() < 3; tries++) {
            int radius = 2 + random.nextInt(3);
            int along = -4 + random.nextInt(25);
            int side = -20 + random.nextInt(41);
            double d = Math.sqrt(along * along + side * side);
            if (d < 8 || d + radius + 2 > AncientLairLayout.FLOOR_RADIUS - 1 || inMouthLane(along, side, radius + 4)) {
                continue;
            }
            boolean clear = true;
            for (Pool other : this.pools) {
                clear &= dist(along, side, other.along, other.side) >= radius + other.radius + 4;
            }
            if (clear) {
                this.pools.add(new Pool(along, side, radius));
            }
        }
        int spireCount = 4 + random.nextInt(3);
        for (int tries = 0; tries < 300 && this.spires.size() < spireCount; tries++) {
            int along = -23 + random.nextInt(47);
            int side = -23 + random.nextInt(47);
            int height = 6 + random.nextInt(7);
            double d = dist(along, side, 0, 0);
            if (d < 8 || d > AncientLairLayout.FLOOR_RADIUS - 3 || nearPerch(along, side, 3)
                    || inMouthLane(along, side, 5) || nearPool(along, side, 4)) {
                continue;
            }
            boolean clear = true;
            for (Spire other : this.spires) {
                clear &= dist(along, side, other.along, other.side) >= 6;
            }
            if (clear) {
                this.spires.add(new Spire(along, side, height));
            }
        }
        for (int tries = 0; tries < 300 && this.posts.size() < CRATER_ROLES.length; tries++) {
            int along = -22 + random.nextInt(45);
            int side = -22 + random.nextInt(45);
            double d = dist(along, side, 0, 0);
            if (d < 10 || d > AncientLairLayout.FLOOR_RADIUS - 4 || nearPerch(along, side, 2) || nearPool(along, side, 3)
                    || nearSpire(along, side, 3)) {
                continue;
            }
            this.posts.add(new Post(along, side, CRATER_ROLES[this.posts.size()]));
        }
    }

    private static double dist(int a1, int s1, int a2, int s2) {
        return Math.sqrt((a1 - a2) * (a1 - a2) + (s1 - s2) * (s1 - s2));
    }

    private static boolean inMouthLane(int along, int side, int margin) {
        return along >= 8 && Math.abs(side) <= AncientLairLayout.MOUTH_HALF + margin;
    }

    /** Within {@code margin} of the perch's plinth. */
    private static boolean nearPerch(int along, int side, int margin) {
        int half = AncientLairLayout.PLINTH_HALF + margin;
        return Math.abs(along + AncientLairLayout.PERCH_OUT) <= half && Math.abs(side) <= half;
    }

    private boolean nearPool(int along, int side, int margin) {
        for (Pool pool : this.pools) {
            if (dist(along, side, pool.along, pool.side) <= pool.radius + margin) {
                return true;
            }
        }
        return false;
    }

    private boolean nearSpire(int along, int side, int margin) {
        for (Spire spire : this.spires) {
            if (dist(along, side, spire.along, spire.side) <= 1 + margin) {
                return true;
            }
        }
        return false;
    }

    /** The stair of the mouth: level with the crater floor to the rim, then down a block a block. */
    public int mouthPath(int along) {
        return along <= AncientLairLayout.RIM_RADIUS ? this.layout.floorY()
                : this.layout.floorY() - (along - AncientLairLayout.RIM_RADIUS);
    }

    public static boolean inMouth(int along, int side) {
        return Math.abs(side) <= AncientLairLayout.MOUTH_HALF && along >= AncientLairLayout.FLOOR_RADIUS - 4
                && along <= AncientLairLayout.MOUTH_REACH;
    }

    /** The cone's surface at distance {@code d} from the middle, or {@link #MIN} beyond its foot. */
    private int coneTop(double d, int x, int z) {
        AncientLairLayout l = this.layout;
        int floor = l.floorY();
        int rim = l.rimY();
        if (d <= AncientLairLayout.FLOOR_RADIUS) {
            return floor;
        }
        double rough = l.smooth(x, z, 5, 1) * 3.0D - 1.0D;
        if (d <= AncientLairLayout.RIM_RADIUS) {
            double t = (d - AncientLairLayout.FLOOR_RADIUS) / (AncientLairLayout.RIM_RADIUS - AncientLairLayout.FLOOR_RADIUS);
            return (int) Math.round(floor + t * (rim - floor) + (t > 0.3D ? rough : 0.0D));
        }
        double foot = AncientLairLayout.BASE_RADIUS + l.smooth(x, z, 7, 2) * 4.0D - 2.0D;
        if (d > foot) {
            return MIN;
        }
        // A volcano's profile: steep under the rim, spreading out at the foot.
        double t = (d - AncientLairLayout.RIM_RADIUS) / (foot - AncientLairLayout.RIM_RADIUS);
        return (int) Math.round(l.base() + (rim - l.base()) * Math.pow(1.0D - t, 1.4D) + rough);
    }

    // ---------------------------------------------------------------- building

    @Override
    public void generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator,
                         Random chunkRandom, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        AncientLairLayout l = this.layout;
        int minX = Math.max(this.boundingBox.getMinX(), chunkBox.getMinX());
        int maxX = Math.min(this.boundingBox.getMaxX(), chunkBox.getMaxX());
        int minZ = Math.max(this.boundingBox.getMinZ(), chunkBox.getMinZ());
        int maxZ = Math.min(this.boundingBox.getMaxZ(), chunkBox.getMaxZ());
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                this.column(world, x, z);
            }
        }
        for (Pool pool : this.pools) {
            this.pool(world, chunkBox, pool);
        }
        for (Spire spire : this.spires) {
            this.spire(world, chunkBox, spire);
        }
        this.perch(world, chunkBox);
        for (Post post : this.posts) {
            BlockPos at = l.at(post.along, post.side, l.floorY() + 1);
            if (chunkBox.contains(at)) {
                AncientLairGuards.placePost(world, at, post.role, l.lairCentre());
            }
        }
        BlockPos perch = l.perchTop();
        if (chunkBox.contains(perch)) {
            StructureBosses.placeAncient(world, perch, AncientLairLayout.yaw(l.front()), l.color(), l.lairCentre());
        }
    }

    private static boolean isTerrain(StructureWorldAccess world, BlockPos pos, BlockState state) {
        return state.isOpaqueFullCube(world, pos) && !state.isIn(BlockTags.LOGS) && !state.isIn(BlockTags.LEAVES);
    }

    /** One column of the cone, crater or mouth. */
    private void column(StructureWorldAccess world, int x, int z) {
        AncientLairLayout l = this.layout;
        int along = l.along(x, z);
        int side = l.side(x, z);
        double d = Math.sqrt(along * along + side * side);
        int top = this.coneTop(d, x, z);
        boolean mouth = inMouth(along, side);
        int scanTop = Math.min(world.getTopY() - 1, l.floorY() + SCAN_UP);
        // Scan down once: the first thing in the column, and the solid ground under trees and water.
        BlockPos.Mutable pos = new BlockPos.Mutable(x, scanTop, z);
        int firstSolid = MIN;
        int ground = world.getBottomY();
        for (int y = scanTop; y > world.getBottomY(); y--) {
            pos.setY(y);
            BlockState state = world.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            if (firstSolid == MIN) {
                firstSolid = y;
            }
            if (isTerrain(world, pos, state)) {
                ground = y;
                break;
            }
        }
        if (mouth) {
            int path = this.mouthPath(along);
            if (top == MIN) {
                if (path <= ground) {
                    return;
                }
                top = path;
            } else {
                top = Math.min(top, path);
            }
        } else if (top == MIN) {
            return;
        }
        boolean open = d <= AncientLairLayout.RIM_RADIUS + 0.5D || mouth;
        // Clear above the surface: everything inside the rim and in the mouth, only plants and water on the slopes.
        for (int y = firstSolid; y > top; y--) {
            pos.setY(y);
            BlockState state = world.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            if (!open && isTerrain(world, pos, state)) {
                // A hill higher than the cone here: leave it be.
                return;
            }
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), PieceBlocks.UPDATE);
        }
        // Then the cone, down until it meets the hill (the top few blocks of a hill are dressed too).
        for (int y = top, depth = 0; depth < MAX_FILL && y > world.getBottomY(); y--, depth++) {
            pos.setY(y);
            if (depth > 3 && y <= ground && isTerrain(world, pos, world.getBlockState(pos))) {
                break;
            }
            world.setBlockState(pos, this.material(x, y, z, depth, d, along, side, mouth), PieceBlocks.UPDATE);
        }
        if (d <= AncientLairLayout.FLOOR_RADIUS - 0.5D && top == l.floorY()) {
            BlockState dressing = this.dressing(x, z, along, side);
            if (dressing != null) {
                world.setBlockState(pos.set(x, top + 1, z), dressing, PieceBlocks.UPDATE);
            }
        }
    }

    /** What the cone is made of at depth {@code depth} below its surface. */
    private BlockState material(int x, int y, int z, int depth, double d, int along, int side, boolean mouth) {
        AncientLairLayout l = this.layout;
        float h = l.hash(x, y, z, 11);
        if (depth == 0) {
            if (mouth && along >= AncientLairLayout.RIM_RADIUS) {
                // The mouth's stair: each step's top block a stair up towards the crater.
                return this.mouthPath(along + 1) < this.mouthPath(along)
                        ? PieceBlocks.stairs(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, l.back(), false)
                        : Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
            }
            if (d <= AncientLairLayout.FLOOR_RADIUS + 0.5D && y == l.floorY()) {
                // The crater floor: basalt and smooth basalt, with patches of magma.
                if (l.smooth(x, z, 3, 3) > 0.78F && h < 0.8F) {
                    return Blocks.MAGMA_BLOCK.getDefaultState();
                }
                return h < 0.55F ? Blocks.SMOOTH_BASALT.getDefaultState() : basalt();
            }
            return h < 0.4F ? Blocks.BLACKSTONE.getDefaultState() : h < 0.75F ? basalt()
                    : h < 0.88F ? Blocks.TUFF.getDefaultState() : Blocks.SMOOTH_BASALT.getDefaultState();
        }
        if (h < 0.035F) {
            return Blocks.MAGMA_BLOCK.getDefaultState();
        }
        // Bands of rock that wander up and down the cone.
        int band = Math.floorMod((y + (int) (l.smooth(x, z, 9, 4) * 6.0F)) / 3, 5);
        return switch (band) {
            case 0, 3 -> Blocks.BLACKSTONE.getDefaultState();
            case 1 -> basalt();
            case 2 -> Blocks.TUFF.getDefaultState();
            default -> h < 0.5F ? Blocks.SMOOTH_BASALT.getDefaultState() : Blocks.BLACKSTONE.getDefaultState();
        };
    }

    private static BlockState basalt() {
        return Blocks.BASALT.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Y);
    }

    /** What lies on the crater floor: shed scales and charred bones, here and there. Null for nothing. */
    private BlockState dressing(int x, int z, int along, int side) {
        if (nearPerch(along, side, 1) || this.nearPool(along, side, 2) || this.nearSpire(along, side, 1)) {
            return null;
        }
        float n = this.layout.hash(x, 0, z, 21);
        if (n < 0.05F) {
            return LairBlocks.RED_SCALE_PILE.getDefaultState().with(SnowBlock.LAYERS, 1 + (int) (n * 60.0F));
        }
        if (n < 0.065F) {
            return Blocks.BONE_BLOCK.getDefaultState().with(PillarBlock.AXIS,
                    n < 0.057F ? Direction.Axis.X : Direction.Axis.Z);
        }
        return null;
    }

    /** A lava pool sunk two deep into the floor, ringed by a raised basalt kerb. */
    private void pool(StructureWorldAccess world, BlockBox chunkBox, Pool pool) {
        AncientLairLayout l = this.layout;
        int reach = pool.radius + 2;
        for (int da = -reach; da <= reach; da++) {
            for (int ds = -reach; ds <= reach; ds++) {
                double d = Math.sqrt(da * da + ds * ds);
                BlockPos floor = l.at(pool.along + da, pool.side + ds, l.floorY());
                if (d <= pool.radius + 0.4D) {
                    set(world, chunkBox, floor.up(), Blocks.AIR.getDefaultState());
                    set(world, chunkBox, floor, Blocks.LAVA.getDefaultState());
                    set(world, chunkBox, floor.down(), Blocks.LAVA.getDefaultState());
                    set(world, chunkBox, floor.down(2), Blocks.BASALT.getDefaultState());
                } else if (d <= pool.radius + 1.6D) {
                    set(world, chunkBox, floor, Blocks.SMOOTH_BASALT.getDefaultState());
                    set(world, chunkBox, floor.down(), Blocks.BLACKSTONE.getDefaultState());
                    set(world, chunkBox, floor.up(), Blocks.POLISHED_BASALT.getDefaultState());
                }
            }
        }
    }

    /** An obsidian spire, 2x2, its last two blocks a single point. */
    private void spire(StructureWorldAccess world, BlockBox chunkBox, Spire spire) {
        AncientLairLayout l = this.layout;
        for (int y = 1; y <= spire.height; y++) {
            for (int i = 0; i < 4; i++) {
                if (y > spire.height - 2 && i > 0) {
                    continue;
                }
                BlockPos at = l.at(spire.along + (i & 1), spire.side + (i >> 1), l.floorY() + y);
                float n = l.hash(at.getX(), at.getY(), at.getZ(), 31);
                set(world, chunkBox, at, (n < 0.15F ? Blocks.CRYING_OBSIDIAN : Blocks.OBSIDIAN).getDefaultState());
            }
        }
    }

    /** The Perch: a plinth of blackstone brick, a basalt pillar on it and the 9x9 plateau where the dragon rests. */
    private void perch(StructureWorldAccess world, BlockBox chunkBox) {
        AncientLairLayout l = this.layout;
        int plinth = AncientLairLayout.PLINTH_HALF;
        int half = AncientLairLayout.PERCH_HALF;
        for (int a = -plinth; a <= plinth; a++) {
            for (int b = -plinth; b <= plinth; b++) {
                boolean pillar = Math.abs(a) <= half && Math.abs(b) <= half;
                int top = pillar ? AncientLairLayout.PERCH_HEIGHT : AncientLairLayout.PLINTH_HEIGHT;
                for (int h = 1; h <= top; h++) {
                    BlockPos at = l.stair(a, b, h);
                    float n = l.hash(at.getX(), at.getY(), at.getZ(), 41);
                    BlockState state;
                    if (h <= AncientLairLayout.PLINTH_HEIGHT) {
                        state = h == AncientLairLayout.PLINTH_HEIGHT && !pillar ? Blocks.POLISHED_BLACKSTONE.getDefaultState()
                                : (n < 0.2F ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS : Blocks.POLISHED_BLACKSTONE_BRICKS)
                                .getDefaultState();
                    } else if (h == top) {
                        boolean edge = Math.abs(a) == half || Math.abs(b) == half;
                        state = (edge ? Blocks.SMOOTH_BASALT : Blocks.POLISHED_BLACKSTONE).getDefaultState();
                    } else {
                        state = n < 0.06F ? Blocks.MAGMA_BLOCK.getDefaultState() : n < 0.4F
                                ? Blocks.POLISHED_BASALT.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Y) : basalt();
                    }
                    set(world, chunkBox, at, state);
                }
                // Shed scales round the plateau's edge, clear of the middle where the dragon lies.
                if (pillar && (Math.abs(a) == half || Math.abs(b) == half)) {
                    BlockPos above = l.stair(a, b, AncientLairLayout.PERCH_HEIGHT + 1);
                    float n = l.hash(above.getX(), above.getY(), above.getZ(), 43);
                    set(world, chunkBox, above, n < 0.3F
                            ? LairBlocks.RED_SCALE_PILE.getDefaultState().with(SnowBlock.LAYERS, 1 + (int) (n * 10.0F))
                            : Blocks.AIR.getDefaultState());
                } else if (!pillar) {
                    set(world, chunkBox, l.stair(a, b, AncientLairLayout.PLINTH_HEIGHT + 1), Blocks.AIR.getDefaultState());
                }
            }
        }
    }

    static void set(StructureWorldAccess world, BlockBox chunkBox, BlockPos pos, BlockState state) {
        if (chunkBox.contains(pos)) {
            world.setBlockState(pos, state, PieceBlocks.UPDATE);
        }
    }

    /** Whether {@code state} is one of the stairs the mouth is made of (for the survey). */
    static boolean isStep(BlockState state) {
        Block block = state.getBlock();
        return block instanceof StairsBlock || block == Blocks.POLISHED_BLACKSTONE_BRICKS;
    }
}
