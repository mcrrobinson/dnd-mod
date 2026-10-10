package mattonfire.dnd.world.gen.dungeon;

import java.util.List;
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
 * <li>Other themes (until their own tickets dress them): a ring of standing stones round the shaft top.</li>
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
        if (info.theme() == DungeonTheme.CRYPT) {
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
        } else {
            this.ring(b);
        }
    }

    // ---------------------------------------------------------------- other themes

    private void ring(Builder b) {
        int ground = this.top - this.info.floorY();
        int r = this.width() / 2;
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
}
