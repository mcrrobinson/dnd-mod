package mattonfire.dnd.world.gen.dungeon;

import java.util.List;
import mattonfire.dnd.classes.Blocks.adventuring.SignpostBlock;
import mattonfire.dnd.classes.Blocks.adventuring.SignpostBlockEntity;
import mattonfire.dnd.classes.Registry.AdventuringBlocks;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.dungeon.DungeonState;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

/**
 * What shows of a dungeon at the surface. Not a room.
 *
 * <ul>
 * <li>The Crypt: a <b>barrow</b>, a grassy burial mound heaped over the top of the stair and roofed
 * over it, with a stone-lined passage (3 wide, under a stone lintel, with two tall door stones and a
 * capstone at its mouth) cut into one side, and a ring of {@value #BARROW_STONES} weathered standing
 * stones round it, some toppled.</li>
 * <li>The Goblin Warren: a <b>cave mouth</b>, a rocky knoll heaped over the top of the stair, with a
 * timber-shored tunnel into one side, a crude palisade of stakes round it (the goblin camp's, from
 * {@code GoblinCampPiece}) with skull-topped gate posts where the trail comes in, and goblin totems.</li>
 * <li>The Dwarven Ruin: a <b>collapsed dwarven gatehouse</b> in the fortress's stone (polished
 * blackstone and Fallen Runic Bricks, a glint of gilded blackstone), a 13x13 hall round the top of the
 * stair with broken walls, Collapsed Rubble drifted over its floor, four corner towers (some fallen), a
 * great doorway between Tarnished Gilding pillars under a broken lintel, a paved approach and rubble
 * and fallen pillars all round.</li>
 * <li>Other themes: a ring of standing stones round the shaft top.</li>
 * </ul>
 */
public class EntrancePiece extends DungeonPiece {
    /** The plain ring's radius (other themes). */
    private static final int RADIUS = 5;
    private static final int[][] STONES = {{5, 0}, {-5, 0}, {0, 5}, {0, -5}, {4, 4}, {4, -4}, {-4, 4}, {-4, -4}};
    /** The barrow piece's radius: the standing stones stand {@value #STONE_RING} out. */
    private static final int BARROW_RADIUS = 13;
    private static final int STONE_RING = 12;
    private static final int BARROW_STONES = 9;
    /** The mound reaches this far from the middle of the shaft. */
    private static final double MOUND_RADIUS = 9.5D;
    /** The mound's height over the shaft (its roof). */
    private static final int MOUND_TOP = 4;
    /** The shaft (the stair piece) is 7x7: Chebyshev distance 3 from the middle. */
    private static final int SHAFT = 3;
    /** The passage runs from the shaft out to here. */
    private static final int PASSAGE_END = 10;
    /** The passage is roofed (a stone lintel under the mound) up to here; past the door stones it's an open cut. */
    private static final int LINTEL_END = 7;
    private static final int PASSAGE_HEIGHT = 3;

    private final int top;

    public EntrancePiece(Info info, int cx, int cz, int top, long seed) {
        super(DungeonStructures.ENTRANCE, info, box(info, cx, cz, top), seed, -1, null, 0, List.of());
        this.top = top;
    }

    public EntrancePiece(NbtCompound nbt) {
        super(DungeonStructures.ENTRANCE, nbt);
        this.top = nbt.getInt("Top");
    }

    private static BlockBox box(Info info, int cx, int cz, int top) {
        if (info.theme() == DungeonTheme.CRYPT || info.theme() == DungeonTheme.GOBLIN_WARREN
                || info.theme() == DungeonTheme.DWARVEN_RUIN) {
            return new BlockBox(cx - BARROW_RADIUS, top - 8, cz - BARROW_RADIUS, cx + BARROW_RADIUS, top + 8, cz + BARROW_RADIUS);
        }
        return new BlockBox(cx - RADIUS, top - 2, cz - RADIUS, cx + RADIUS, top + 5, cz + RADIUS);
    }

    @Override
    protected void writeNbt(net.minecraft.structure.StructureContext context, NbtCompound nbt) {
        super.writeNbt(context, nbt);
        nbt.putInt("Top", this.top);
    }

    @Override
    protected void build(Builder b) {
        if (this.theme() == DungeonTheme.CRYPT && this.width() == 2 * BARROW_RADIUS + 1) {
            this.barrow(b);
        } else if (this.theme() == DungeonTheme.GOBLIN_WARREN && this.width() == 2 * BARROW_RADIUS + 1) {
            this.caveMouth(b);
        } else if (this.theme() == DungeonTheme.DWARVEN_RUIN && this.width() == 2 * BARROW_RADIUS + 1) {
            this.gatehouse(b);
        } else {
            this.ring(b);
        }
    }

    // ---------------------------------------------------------------- other themes

    private void ring(Builder b) {
        int ground = this.top - this.info.floorY();
        int r = this.width() / 2;
        this.signpost(b, r, Direction.fromHorizontal((int) Math.floorMod(this.seed, 4L)), RADIUS - 1, 2);
        for (int[] stone : STONES) {
            int x = r + stone[0];
            int z = r + stone[1];
            int height = 2 + b.random.nextInt(2);
            boolean fallen = b.chance(0.2F);
            // Footing, so a stone on a slope doesn't float
            b.set(x, ground - 1, z, b.palette.surface(b.random));
            b.set(x, ground, z, b.palette.surface(b.random));
            int standing = fallen ? 1 : height;
            for (int y = 1; y <= standing; y++) {
                b.set(x, ground + y, z, y == standing ? b.palette.accent() : b.palette.surface(b.random));
            }
            if (!fallen && b.chance(0.4F)) {
                b.set(x, ground + standing + 1, z, Blocks.SKELETON_SKULL.getDefaultState());
            }
        }
    }

    // ---------------------------------------------------------------- the signpost

    /**
     * An Adventurer's Signpost naming the dungeon and its Challenge tier, standing on the ground {@code out}
     * blocks from the middle of the shaft along {@code way} (the way in) and {@code side} to its right, with its
     * arrow pointing back along the way in.
     */
    private void signpost(Builder b, int c, Direction way, int out, int side) {
        Direction right = way.rotateYClockwise();
        int x = c + way.getOffsetX() * out + right.getOffsetX() * side;
        int z = c + way.getOffsetZ() * out + right.getOffsetZ() * side;
        int ground = this.groundAt(b, x, z);
        if (ground == Integer.MIN_VALUE) {
            return;
        }
        b.set(x, ground + 1, z, AdventuringBlocks.SIGNPOST.getDefaultState().with(SignpostBlock.FACING, way.getOpposite()));
        b.set(x, ground + 2, z, Blocks.AIR.getDefaultState());
        if (b.world.getBlockEntity(b.pos(x, ground + 1, z)) instanceof SignpostBlockEntity post) {
            post.setDungeon(this.info.startKey(), DungeonState.name(this.info.startKey(), this.theme()),
                    DungeonState.tierText(this.info.tier()));
        }
    }

    // ---------------------------------------------------------------- the Crypt's barrow

    private void barrow(Builder b) {
        int ground = this.top - this.info.floorY();
        int c = BARROW_RADIUS;
        // Everything the layout depends on is drawn first: later draws differ from chunk to chunk
        // (the mound only fills air in the chunk being built), and only change block textures.
        Direction passage = Direction.fromHorizontal(b.random.nextInt(4));
        double ringOffset = b.random.nextDouble() * Math.PI * 2.0D;
        long stoneSeed = b.random.nextLong();
        this.mound(b, ground, c);
        this.dromos(b, ground, c, passage);
        this.stoneRing(b, c, ringOffset, stoneSeed);
        this.signpost(b, c, passage, PASSAGE_END + 1, 2);
    }

    /** Height of the mound at distance {@code r} from the middle of the shaft (0 outside it). */
    static int moundHeight(double r) {
        if (r <= SHAFT + 0.5D) {
            return MOUND_TOP;
        }
        if (r > MOUND_RADIUS) {
            return 0;
        }
        double t = (r - SHAFT - 0.5D) / (MOUND_RADIUS - SHAFT - 0.5D);
        return (int) Math.round((MOUND_TOP - 1) * (1.0D - t * t)) + (t < 0.2D ? 1 : 0);
    }

    /**
     * The grassy mound: earth heaped from a little below the ground up over every column within
     * {@link #MOUND_RADIUS} (only into air, plants and water, so it never fills the shaft or cuts into
     * a hill), and a roof of stone and turf over the shaft, so the stair is reached through the passage.
     */
    private void mound(Builder b, int ground, int c) {
        for (int x = 0; x < this.width(); x++) {
            for (int z = 0; z < this.depth(); z++) {
                int dx = x - c;
                int dz = z - c;
                boolean shaft = Math.max(Math.abs(dx), Math.abs(dz)) <= SHAFT;
                if (shaft) {
                    b.set(x, ground + MOUND_TOP, z, b.palette.shell());
                    b.set(x, ground + MOUND_TOP + 1, z, Blocks.GRASS_BLOCK.getDefaultState());
                    continue;
                }
                int h = moundHeight(Math.sqrt(dx * dx + dz * dz));
                if (h <= 0) {
                    continue;
                }
                for (int y = ground - 4; y <= ground + h; y++) {
                    BlockState now = b.get(x, y, z);
                    if (now == null) {
                        break;
                    }
                    if (now.isAir() || now.isReplaceable() || !now.getFluidState().isEmpty()) {
                        BlockState earth = y == ground + h ? Blocks.GRASS_BLOCK.getDefaultState()
                                : b.random.nextFloat() < 0.85F ? Blocks.DIRT.getDefaultState() : Blocks.COARSE_DIRT.getDefaultState();
                        b.set(x, y, z, earth);
                    }
                }
            }
        }
    }

    /**
     * The passage into the mound: 3 wide and {@value #PASSAGE_HEIGHT} high from the shaft out to
     * {@value #PASSAGE_END} blocks, floored and walled with mossy stone, roofed with a stone lintel
     * up to {@value #LINTEL_END}, then two tall door stones under a capstone, then an open cut through
     * the foot of the mound.
     */
    private void dromos(Builder b, int ground, int c, Direction dir) {
        Direction side = dir.rotateYClockwise();
        for (int t = SHAFT - 1; t <= PASSAGE_END; t++) {
            for (int l = -2; l <= 2; l++) {
                int x = c + dir.getOffsetX() * t + side.getOffsetX() * l;
                int z = c + dir.getOffsetZ() * t + side.getOffsetZ() * l;
                boolean inside = Math.abs(l) <= 1;
                if (inside) {
                    if (t > SHAFT) {
                        b.set(x, ground, z, b.palette.surface(b.random));
                    }
                    for (int y = 1; y <= PASSAGE_HEIGHT; y++) {
                        b.set(x, ground + y, z, Blocks.AIR.getDefaultState());
                    }
                } else if (t > SHAFT && t <= LINTEL_END) {
                    for (int y = 0; y <= PASSAGE_HEIGHT; y++) {
                        b.set(x, ground + y, z, b.palette.surface(b.random));
                    }
                }
                if (t > SHAFT && t <= LINTEL_END) {
                    b.set(x, ground + PASSAGE_HEIGHT + 1, z, b.palette.surface(b.random));
                } else if (inside && t > LINTEL_END + 1) {
                    for (int y = PASSAGE_HEIGHT + 1; y <= MOUND_TOP + 1; y++) {
                        b.set(x, ground + y, z, Blocks.AIR.getDefaultState());
                    }
                }
            }
        }
        // The door stones and their capstone
        int t = LINTEL_END + 1;
        for (int l = -2; l <= 2; l++) {
            int x = c + dir.getOffsetX() * t + side.getOffsetX() * l;
            int z = c + dir.getOffsetZ() * t + side.getOffsetZ() * l;
            if (Math.abs(l) == 2) {
                for (int y = 0; y <= PASSAGE_HEIGHT + 1; y++) {
                    b.set(x, ground + y, z, b.palette.accent());
                }
            } else {
                b.set(x, ground + PASSAGE_HEIGHT + 1, z, b.palette.accent());
            }
            b.set(x, ground + PASSAGE_HEIGHT + 2, z, Blocks.POLISHED_DEEPSLATE_SLAB.getDefaultState());
        }
        // A soul lantern hangs from the roof where the passage meets the stair
        int x = c + dir.getOffsetX() * SHAFT + side.getOffsetX() * 2;
        int z = c + dir.getOffsetZ() * SHAFT + side.getOffsetZ() * 2;
        b.set(x, ground + PASSAGE_HEIGHT, z, b.palette.light(true));
    }

    /**
     * {@value #BARROW_STONES} standing stones round the mound, each footed on the ground where it
     * stands; a quarter have toppled over. Each stone draws from its own random, so a stone looks
     * the same whichever chunk builds it.
     */
    private void stoneRing(Builder b, int c, double ringOffset, long seed) {
        for (int i = 0; i < BARROW_STONES; i++) {
            Random random = Random.create(seed + i);
            double angle = ringOffset + i * Math.PI * 2.0D / BARROW_STONES;
            double ox = Math.cos(angle);
            double oz = Math.sin(angle);
            int x = c + (int) Math.round(ox * STONE_RING);
            int z = c + (int) Math.round(oz * STONE_RING);
            int height = 2 + random.nextInt(3);
            boolean fallen = random.nextFloat() < 0.25F;
            boolean skull = random.nextFloat() < 0.35F;
            int ground = this.groundAt(b, x, z);
            if (ground == Integer.MIN_VALUE) {
                continue;
            }
            b.set(x, ground - 1, z, b.palette.surface(random));
            b.set(x, ground, z, b.palette.surface(random));
            if (fallen) {
                // A low line of stone on the grass, toppled in towards the mound
                for (int k = 0; k < height; k++) {
                    b.set(x - (int) Math.round(ox * k), ground + 1, z - (int) Math.round(oz * k), b.palette.surface(random));
                }
                continue;
            }
            for (int y = 1; y <= height; y++) {
                b.set(x, ground + y, z, y == height ? b.palette.accent() : b.palette.surface(random));
            }
            if (skull) {
                b.set(x, ground + height + 1, z, Blocks.SKELETON_SKULL.getDefaultState());
            }
        }
    }

    /**
     * The local height of the top solid block at local (x, z), from the worldgen heightmap; or
     * {@link Integer#MIN_VALUE} when (x, z) isn't in the chunk being built.
     */
    private int groundAt(Builder b, int x, int z) {
        BlockPos pos = b.pos(x, 0, z);
        if (!b.chunkBox.contains(new BlockPos(pos.getX(), b.chunkBox.getMinY(), pos.getZ()))) {
            return Integer.MIN_VALUE;
        }
        return b.world.getTopY(Heightmap.Type.OCEAN_FLOOR_WG, pos.getX(), pos.getZ()) - 1 - this.info.floorY();
    }

    // ---------------------------------------------------------------- the Goblin Warren's cave mouth

    /** Palisade stakes stand this far from the middle of the shaft (the barrow's stone ring). */
    private static final int PALISADE = STONE_RING;
    /** Stakes are left out within this angle (degrees) either side of the trail into the mouth. */
    private static final double TRAIL_GAP = 12.0D;
    /** Wood for the stakes, as in the goblin camp's palisade. */
    private static final net.minecraft.block.Block[] STAKES = {Blocks.SPRUCE_LOG, Blocks.SPRUCE_LOG, Blocks.OAK_LOG, Blocks.STRIPPED_SPRUCE_LOG};

    private void caveMouth(Builder b) {
        int ground = this.top - this.info.floorY();
        int c = BARROW_RADIUS;
        // As for the barrow: everything the layout depends on is drawn first, and every texture
        // comes from a random of its own position, so each chunk builds the same knoll.
        Direction mouth = Direction.fromHorizontal(b.random.nextInt(4));
        long seed = b.random.nextLong();
        this.knoll(b, ground, c, seed);
        this.tunnel(b, ground, c, mouth, seed);
        this.signpost(b, c, mouth, PALISADE + 1, 3);
        this.palisade(b, c, mouth, seed);
        this.totems(b, c, mouth, seed);
    }

    /** A random of its own for one position, so a block looks the same whichever chunk builds it. */
    private static Random at(long seed, int x, int y, int z) {
        return Random.create(seed ^ (x * 341873128712L + y * 9_123_456_789L + z * 132897987541L));
    }

    private static BlockState rock(Random r) {
        float f = r.nextFloat();
        return (f < 0.45F ? Blocks.STONE : f < 0.7F ? Blocks.COBBLESTONE : f < 0.85F ? Blocks.MOSSY_COBBLESTONE
                : Blocks.ANDESITE).getDefaultState();
    }

    /** The knoll's skin: scrubby, stony ground. */
    private static BlockState knollTop(Random r) {
        float f = r.nextFloat();
        return (f < 0.35F ? Blocks.GRASS_BLOCK : f < 0.6F ? Blocks.COARSE_DIRT : f < 0.8F ? Blocks.MOSSY_COBBLESTONE
                : Blocks.STONE).getDefaultState();
    }

    /**
     * A rocky knoll the size of the barrow's mound, a block higher or lower here and there, heaped
     * only into air, plants and water, and roofed over the shaft, so the stair is reached through the tunnel.
     */
    private void knoll(Builder b, int ground, int c, long seed) {
        for (int x = 0; x < this.width(); x++) {
            for (int z = 0; z < this.depth(); z++) {
                int dx = x - c;
                int dz = z - c;
                Random r = at(seed, x, 0, z);
                if (Math.max(Math.abs(dx), Math.abs(dz)) <= SHAFT) {
                    b.set(x, ground + MOUND_TOP, z, b.palette.shell());
                    b.set(x, ground + MOUND_TOP + 1, z, knollTop(r));
                    continue;
                }
                int h = moundHeight(Math.sqrt(dx * dx + dz * dz));
                if (h <= 0) {
                    continue;
                }
                h = Math.max(1, h + r.nextInt(3) - 1);
                for (int y = ground - 4; y <= ground + h; y++) {
                    BlockState now = b.get(x, y, z);
                    if (now == null) {
                        break;
                    }
                    if (now.isAir() || now.isReplaceable() || !now.getFluidState().isEmpty()) {
                        b.set(x, y, z, y == ground + h ? knollTop(r) : r.nextFloat() < 0.7F ? rock(r) : Blocks.DIRT.getDefaultState());
                    }
                }
            }
        }
    }

    /**
     * The tunnel into the knoll: 3 wide and {@value #PASSAGE_HEIGHT} high from the shaft out to
     * {@value #PASSAGE_END} blocks, floored with trodden earth and walled and roofed with rock up to
     * {@value #LINTEL_END}; then a timber frame (spruce posts under a log lintel, a skull on each
     * post), then an open cut through the foot of the knoll.
     */
    private void tunnel(Builder b, int ground, int c, Direction dir, long seed) {
        Direction side = dir.rotateYClockwise();
        for (int t = SHAFT - 1; t <= PASSAGE_END; t++) {
            for (int l = -2; l <= 2; l++) {
                int x = c + dir.getOffsetX() * t + side.getOffsetX() * l;
                int z = c + dir.getOffsetZ() * t + side.getOffsetZ() * l;
                Random r = at(seed, x, 1, z);
                boolean inside = Math.abs(l) <= 1;
                if (inside) {
                    if (t > SHAFT) {
                        b.set(x, ground, z, (r.nextFloat() < 0.7F ? Blocks.COARSE_DIRT : Blocks.COBBLESTONE).getDefaultState());
                    }
                    for (int y = 1; y <= PASSAGE_HEIGHT; y++) {
                        b.set(x, ground + y, z, Blocks.AIR.getDefaultState());
                    }
                } else if (t > SHAFT && t <= LINTEL_END) {
                    for (int y = 0; y <= PASSAGE_HEIGHT; y++) {
                        b.set(x, ground + y, z, rock(r));
                    }
                }
                if (t > SHAFT && t <= LINTEL_END) {
                    b.set(x, ground + PASSAGE_HEIGHT + 1, z, rock(r));
                } else if (inside && t > LINTEL_END + 1) {
                    for (int y = PASSAGE_HEIGHT + 1; y <= MOUND_TOP + 1; y++) {
                        b.set(x, ground + y, z, Blocks.AIR.getDefaultState());
                    }
                }
            }
        }
        // The timber frame at the mouth
        int t = LINTEL_END + 1;
        Direction.Axis across = side.getAxis();
        for (int l = -2; l <= 2; l++) {
            int x = c + dir.getOffsetX() * t + side.getOffsetX() * l;
            int z = c + dir.getOffsetZ() * t + side.getOffsetZ() * l;
            if (Math.abs(l) == 2) {
                for (int y = 0; y <= PASSAGE_HEIGHT; y++) {
                    b.set(x, ground + y, z, Blocks.SPRUCE_LOG.getDefaultState());
                }
                b.set(x, ground + PASSAGE_HEIGHT + 1, z, Blocks.SPRUCE_LOG.getDefaultState());
                b.set(x, ground + PASSAGE_HEIGHT + 2, z, Blocks.SKELETON_SKULL.getDefaultState()
                        .with(net.minecraft.block.SkullBlock.ROTATION, dir.getHorizontal() * 4 % 16));
            } else {
                b.set(x, ground + PASSAGE_HEIGHT + 1, z, Blocks.SPRUCE_LOG.getDefaultState()
                        .with(net.minecraft.block.PillarBlock.AXIS, across));
            }
        }
        // A lantern hangs where the tunnel meets the stair
        int x = c + dir.getOffsetX() * SHAFT + side.getOffsetX() * 2;
        int z = c + dir.getOffsetZ() * SHAFT + side.getOffsetZ() * 2;
        b.set(x, ground + PASSAGE_HEIGHT, z, b.palette.light(true));
    }

    /**
     * A crude palisade of stakes {@value #PALISADE} blocks out, each standing on the ground where it
     * is, with gaps, and a gateway where the trail comes in to the mouth: two tall gate posts, each
     * topped with a skull.
     */
    private void palisade(Builder b, int c, Direction mouth, long seed) {
        double trail = Math.atan2(mouth.getOffsetZ(), mouth.getOffsetX());
        java.util.Set<Long> done = new java.util.HashSet<>();
        for (int i = 0; i < 96; i++) {
            double angle = i * Math.PI * 2.0D / 96;
            int x = c + (int) Math.round(Math.cos(angle) * PALISADE);
            int z = c + (int) Math.round(Math.sin(angle) * PALISADE);
            if (!done.add((long) x << 32 | (z & 0xFFFFFFFFL))) {
                continue;
            }
            double off = Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(Math.toDegrees(angle - trail)));
            Random r = at(seed, x, 2, z);
            float f = r.nextFloat();
            if (off < TRAIL_GAP || f < 0.15F) {
                continue;
            }
            int ground = this.groundAt(b, x, z);
            if (ground == Integer.MIN_VALUE) {
                continue;
            }
            if (f < 0.3F) {
                for (int y = 1; y <= 2; y++) {
                    b.set(x, ground + y, z, Blocks.SPRUCE_FENCE.getDefaultState());
                }
                continue;
            }
            int height = 2 + r.nextInt(2);
            BlockState stake = STAKES[r.nextInt(STAKES.length)].getDefaultState();
            for (int y = 1; y <= height; y++) {
                b.set(x, ground + y, z, stake);
            }
            float topper = r.nextFloat();
            if (topper < 0.06F) {
                b.set(x, ground + height + 1, z, Blocks.TORCH.getDefaultState());
            } else if (topper < 0.35F) {
                b.set(x, ground + height + 1, z, Blocks.SPRUCE_FENCE.getDefaultState());
            }
        }
        // The gate posts either side of the trail
        Direction side = mouth.rotateYClockwise();
        for (int l : new int[]{-2, 2}) {
            int x = c + mouth.getOffsetX() * PALISADE + side.getOffsetX() * l;
            int z = c + mouth.getOffsetZ() * PALISADE + side.getOffsetZ() * l;
            int ground = this.groundAt(b, x, z);
            if (ground == Integer.MIN_VALUE) {
                continue;
            }
            for (int y = 1; y <= 3; y++) {
                b.set(x, ground + y, z, Blocks.SPRUCE_LOG.getDefaultState());
            }
            Random r = at(seed, x, 3, z);
            b.set(x, ground + 4, z, (r.nextFloat() < 0.2F ? Blocks.ZOMBIE_HEAD : Blocks.SKELETON_SKULL).getDefaultState()
                    .with(net.minecraft.block.SkullBlock.ROTATION, r.nextInt(16)));
        }
    }

    /**
     * Goblin totems: one either side of the trail inside the gateway, and two more round the knoll.
     * A totem is a spruce log and a stripped log with stake arms, under a grinning carved pumpkin (or
     * a skull), facing out along the trail.
     */
    private void totems(Builder b, int c, Direction mouth, long seed) {
        Direction side = mouth.rotateYClockwise();
        java.util.List<int[]> spots = new java.util.ArrayList<>();
        for (int l : new int[]{-3, 3}) {
            spots.add(new int[]{c + mouth.getOffsetX() * 11 + side.getOffsetX() * l, c + mouth.getOffsetZ() * 11 + side.getOffsetZ() * l});
        }
        for (int l : new int[]{-10, 10}) {
            spots.add(new int[]{c - mouth.getOffsetX() * 4 + side.getOffsetX() * l, c - mouth.getOffsetZ() * 4 + side.getOffsetZ() * l});
        }
        for (int[] spot : spots) {
            int x = spot[0];
            int z = spot[1];
            int ground = this.groundAt(b, x, z);
            if (ground == Integer.MIN_VALUE) {
                continue;
            }
            Random r = at(seed, x, 4, z);
            b.set(x, ground, z, Blocks.COARSE_DIRT.getDefaultState());
            b.set(x, ground + 1, z, Blocks.SPRUCE_LOG.getDefaultState());
            b.set(x, ground + 2, z, Blocks.STRIPPED_SPRUCE_LOG.getDefaultState());
            for (int l : new int[]{-1, 1}) {
                if (r.nextFloat() < 0.7F) {
                    b.set(x + side.getOffsetX() * l, ground + 2, z + side.getOffsetZ() * l, Blocks.SPRUCE_FENCE.getDefaultState());
                }
            }
            BlockState head = r.nextFloat() < 0.7F
                    ? Blocks.CARVED_PUMPKIN.getDefaultState().with(net.minecraft.block.CarvedPumpkinBlock.FACING, mouth)
                    : Blocks.SKELETON_SKULL.getDefaultState().with(net.minecraft.block.SkullBlock.ROTATION, r.nextInt(16));
            b.set(x, ground + 3, z, head);
        }
    }

    // ---------------------------------------------------------------- the Dwarven Ruin's gatehouse

    /** The gatehouse's walls stand this far (Chebyshev) from the middle of the shaft. */
    private static final int HALL = 6;
    private static final int HALL_WALL = 5;
    private static final int TOWER = 7;
    /** The hall is cleared of rock and earth this high (the top of the piece). */
    private static final int HALL_CLEAR = 8;
    /** Walls are footed this far down where the ground falls away. */
    private static final int HALL_FOOTING = 8;

    /** The gatehouse's stone: the palette's surface stone, with now and then a glint of gilded blackstone. */
    private static BlockState gateStone(Builder b, Random r) {
        return r.nextFloat() < 0.03F ? Blocks.GILDED_BLACKSTONE.getDefaultState() : b.palette.surface(r);
    }

    private void gatehouse(Builder b) {
        int ground = this.top - this.info.floorY();
        int c = BARROW_RADIUS;
        // As for the barrow: layout first, then textures from randoms of their own positions.
        Direction mouth = Direction.fromHorizontal(b.random.nextInt(4));
        long seed = b.random.nextLong();
        boolean[] fallen = new boolean[4];
        int fallenCount = 1 + b.random.nextInt(2);
        for (int i = 0; i < fallenCount; i++) {
            fallen[b.random.nextInt(4)] = true;
        }
        this.hall(b, ground, c, mouth, seed, fallen);
        this.gateway(b, ground, c, mouth, seed);
        this.ruinedSurrounds(b, c, mouth, seed);
        this.signpost(b, c, mouth, BARROW_RADIUS - 2, 3);
    }

    /** Index 0-3 of the corner tower at (dx, dz), or -1 if it isn't in one. */
    private static int towerAt(int dx, int dz) {
        if (Math.abs(dx) < HALL - 1 || Math.abs(dz) < HALL - 1) {
            return -1;
        }
        return (dx > 0 ? 1 : 0) + (dz > 0 ? 2 : 0);
    }

    /**
     * The hall: a paved floor round the shaft footed down to solid ground, cleared up to
     * {@value #HALL_CLEAR}; walls {@value #HALL_WALL} high with gaps knocked in them, four corner towers
     * {@value #TOWER} high (the {@code fallen} ones down to stumps), and a ledge of slabs left of the roof.
     */
    private void hall(Builder b, int ground, int c, Direction mouth, long seed, boolean[] fallen) {
        for (int x = c - HALL; x <= c + HALL; x++) {
            for (int z = c - HALL; z <= c + HALL; z++) {
                int dx = x - c;
                int dz = z - c;
                int ring = Math.max(Math.abs(dx), Math.abs(dz));
                if (ring <= SHAFT) {
                    continue;
                }
                Random r = at(seed, x, 0, z);
                // Footing down to solid ground
                for (int y = ground - 1; y >= ground - HALL_FOOTING; y--) {
                    BlockState now = b.get(x, y, z);
                    if (now == null || !(now.isAir() || now.isReplaceable() || !now.getFluidState().isEmpty())) {
                        break;
                    }
                    b.set(x, y, z, b.palette.foundation());
                }
                b.set(x, ground, z, ring == HALL ? gateStone(b, r) : b.palette.floor(r));
                int tower = towerAt(dx, dz);
                int height;
                if (tower >= 0) {
                    height = fallen[tower] ? 1 + r.nextInt(2) : TOWER + r.nextInt(2);
                } else if (ring == HALL) {
                    height = r.nextFloat() < 0.35F ? r.nextInt(HALL_WALL) : HALL_WALL;
                } else {
                    height = 0;
                }
                // Collapsed Rubble drifted over the hall floor, deepest against the walls
                int rubble = tower < 0 && ring < HALL && ring > SHAFT + 1
                        && r.nextFloat() < (ring == HALL - 1 ? 0.45F : 0.15F) ? 1 + r.nextInt(ring == HALL - 1 ? 4 : 2) : 0;
                for (int y = 1; y <= HALL_CLEAR; y++) {
                    BlockState state;
                    if (y == 1 && rubble > 0) {
                        state = RuinDressing.rubbleLayers(rubble);
                    } else if (y <= height) {
                        state = gateStone(b, r);
                    } else if (tower >= 0 && !fallen[tower] && y == height + 1 && Math.abs(dx) == HALL && Math.abs(dz) == HALL) {
                        state = b.palette.surfaceWall();
                    } else if (tower < 0 && ring == HALL - 1 && y == HALL_WALL && r.nextFloat() < 0.3F) {
                        // What's left of the roof: a ledge along the inside of the walls
                        state = Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.getDefaultState()
                                .with(net.minecraft.block.SlabBlock.TYPE, net.minecraft.block.enums.SlabType.TOP);
                    } else {
                        state = Blocks.AIR.getDefaultState();
                    }
                    b.set(x, ground + y, z, state);
                }
            }
        }
    }

    /**
     * The great doorway in the wall facing {@code dir}: 3 wide and 4 high, between two
     * Tarnished Gilding pillars with chiselled capitals, under a chiselled lintel broken off on one side (the
     * missing piece lies in front); then a paved approach out to the edge of the piece, cleared above.
     */
    private void gateway(Builder b, int ground, int c, Direction dir, long seed) {
        Direction side = dir.rotateYClockwise();
        int brokenSide = at(seed, c, 5, c).nextBoolean() ? 1 : -1;
        for (int l = -2; l <= 2; l++) {
            int x = c + dir.getOffsetX() * HALL + side.getOffsetX() * l;
            int z = c + dir.getOffsetZ() * HALL + side.getOffsetZ() * l;
            if (Math.abs(l) == 2) {
                for (int y = 1; y <= HALL_WALL + 1; y++) {
                    b.set(x, ground + y, z, ModBlockFamilies.TARNISHED_GILDING.base().getDefaultState());
                }
                b.set(x, ground + HALL_WALL + 2, z, b.palette.accent());
                continue;
            }
            for (int y = 1; y <= 4; y++) {
                b.set(x, ground + y, z, Blocks.AIR.getDefaultState());
            }
            b.set(x, ground + 5, z, l == brokenSide ? Blocks.AIR.getDefaultState() : b.palette.accent());
            b.set(x, ground, z, b.palette.trim());
        }
        // The approach
        for (int t = HALL + 1; t <= BARROW_RADIUS; t++) {
            for (int l = -1; l <= 1; l++) {
                int x = c + dir.getOffsetX() * t + side.getOffsetX() * l;
                int z = c + dir.getOffsetZ() * t + side.getOffsetZ() * l;
                Random r = at(seed, x, 6, z);
                for (int y = ground - 1; y >= ground - HALL_FOOTING; y--) {
                    BlockState now = b.get(x, y, z);
                    if (now == null || !(now.isAir() || now.isReplaceable() || !now.getFluidState().isEmpty())) {
                        break;
                    }
                    b.set(x, y, z, b.palette.foundation());
                }
                b.set(x, ground, z, r.nextFloat() < 0.7F ? b.palette.floor(r) : Blocks.COBBLED_DEEPSLATE.getDefaultState());
                for (int y = 1; y <= 4; y++) {
                    b.set(x, ground + y, z, Blocks.AIR.getDefaultState());
                }
            }
        }
        // The fallen piece of the lintel, in front of the doorway
        int x = c + dir.getOffsetX() * (HALL + 2) + side.getOffsetX() * (brokenSide * 2);
        int z = c + dir.getOffsetZ() * (HALL + 2) + side.getOffsetZ() * (brokenSide * 2);
        b.set(x, ground + 1, z, b.palette.accent());
    }

    /**
     * Round the gatehouse: fallen pillar drums lying in the grass, heaps of rubble, and two headless
     * statues of dwarf lords either side of the approach, each standing on the ground where it is.
     */
    private void ruinedSurrounds(Builder b, int c, Direction mouth, long seed) {
        Random layout = at(seed, 0, 7, 0);
        Direction side = mouth.rotateYClockwise();
        for (int i = 0; i < 14; i++) {
            double angle = layout.nextDouble() * Math.PI * 2.0D;
            double dist = HALL + 2 + layout.nextDouble() * (BARROW_RADIUS - HALL - 3);
            int x = c + (int) Math.round(Math.cos(angle) * dist);
            int z = c + (int) Math.round(Math.sin(angle) * dist);
            boolean drum = layout.nextFloat() < 0.3F;
            Direction lying = Direction.fromHorizontal(layout.nextInt(4));
            int lateral = (x - c) * side.getOffsetX() + (z - c) * side.getOffsetZ();
            int ahead = (x - c) * mouth.getOffsetX() + (z - c) * mouth.getOffsetZ();
            if (Math.abs(lateral) <= 3 && ahead > 0) {
                continue; // keep the approach clear
            }
            int ground = this.groundAt(b, x, z);
            if (ground == Integer.MIN_VALUE) {
                continue;
            }
            Random r = at(seed, x, 8, z);
            BlockState here = b.get(x, ground + 1, z);
            if (here == null || !(here.isAir() || here.isReplaceable())) {
                continue;
            }
            if (drum) {
                BlockState fallen = ModBlockFamilies.TARNISHED_GILDING.base().getDefaultState()
                        .with(net.minecraft.block.PillarBlock.AXIS, lying.getAxis());
                b.set(x, ground + 1, z, fallen);
                b.set(x + lying.getOffsetX(), ground + 1, z + lying.getOffsetZ(), fallen);
            } else {
                b.set(x, ground + 1, z, r.nextFloat() < 0.6F ? Blocks.COBBLED_DEEPSLATE.getDefaultState() : gateStone(b, r));
                if (r.nextFloat() < 0.4F) {
                    b.set(x, ground + 2, z, Blocks.COBBLED_DEEPSLATE_SLAB.getDefaultState());
                }
                // and the heap's spill of rubble beside it, where the ground is level
                Direction spill = Direction.fromHorizontal(r.nextInt(4));
                int sx = x + spill.getOffsetX();
                int sz = z + spill.getOffsetZ();
                BlockState beside = b.get(sx, ground + 1, sz);
                BlockState under = b.get(sx, ground, sz);
                if (beside != null && beside.isAir() && under != null && under.isFullCube(net.minecraft.world.EmptyBlockView.INSTANCE, BlockPos.ORIGIN)) {
                    b.set(sx, ground + 1, sz, RuinDressing.rubbleLayers(1 + r.nextInt(3)));
                }
            }
        }
        for (int l : new int[]{-3, 3}) {
            int x = c + mouth.getOffsetX() * (HALL + 4) + side.getOffsetX() * l;
            int z = c + mouth.getOffsetZ() * (HALL + 4) + side.getOffsetZ() * l;
            int ground = this.groundAt(b, x, z);
            if (ground == Integer.MIN_VALUE) {
                continue;
            }
            b.set(x, ground, z, b.palette.trim());
            b.set(x, ground + 1, z, Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState());
            b.set(x, ground + 2, z, Blocks.POLISHED_DEEPSLATE.getDefaultState());
            b.set(x, ground + 3, z, Blocks.CHISELED_DEEPSLATE.getDefaultState());
        }
    }
}
