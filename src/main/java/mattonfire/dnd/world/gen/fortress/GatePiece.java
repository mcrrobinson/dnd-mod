package mattonfire.dnd.world.gen.fortress;

import static mattonfire.dnd.world.gen.PieceBlocks.*;

import mattonfire.dnd.classes.Blocks.dwarven.WaystoneBlock;
import mattonfire.dnd.classes.Registry.AdventuringBlocks;
import mattonfire.dnd.classes.Registry.DwarvenBlocks;
import mattonfire.dnd.classes.Registry.ModBannerPatterns;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.world.gen.Ruinable;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * The great gate: a paved terrace with braziers, guards and a waystone in front of a towering runic
 * brick facade set into the mountainside, flanked by gold-veined pillars and banners, with a chiseled
 * runic doorway under a raised portcullis and the clan crest above it.
 *
 * <p>In a desolate fortress the gate is where it was attacked ({@link #ruin}): the doorway smashed in, holes
 * knocked deep into the facade, the battlements and pillars broken and a scree of masonry down the slope.
 */
public class GatePiece extends FortressPiece implements Ruinable {
    private static final int WIDTH = 21;
    private static final int DEPTH = 10;
    private static final int HEIGHT = 18;
    /** The terrace runs from the front edge up to the facade. */
    static final int FACADE = 4;
    private static final int MID = WIDTH / 2;
    /** The pillars standing out in front of the facade, and how high they go. */
    private static final int[] PILLARS = {4, WIDTH - 5};
    private static final int PILLAR_TOP = 14;
    /** The great hall behind the gate: its inside starts this far in from either side, and goes up to here. */
    private static final int HALL_SIDE = 3;
    private static final int HALL_TOP = 11;
    /** How far out in front of the facade, and past its ends, rubble lies. */
    private static final int SCREE_OUT = 12;
    private static final int SCREE_SIDE = 6;

    public GatePiece(BlockBox box, Direction facing, long seed) {
        super(DwarvenFortressStructures.GATE, box, facing, seed);
    }

    public GatePiece(NbtCompound nbt) {
        super(DwarvenFortressStructures.GATE, nbt);
    }

    public static GatePiece create(Random random, BlockPos front, Direction facing) {
        return new GatePiece(boxFrom(front, facing, WIDTH, HEIGHT, DEPTH), facing, random.nextLong());
    }

    @Override
    protected void build(Builder b) {
        DyeColor banner = b.pick(new DyeColor[]{DyeColor.RED, DyeColor.BLUE, DyeColor.BLACK, DyeColor.PURPLE});

        // Terrace: paved, open to the sky, with a low parapet and a gap for the approach.
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < FACADE; z++) {
                b.foundation(x, z);
                b.set(x, 0, z, z == 0 || x == 0 || x == WIDTH - 1 ? Blocks.DEEPSLATE_TILES : Blocks.POLISHED_DEEPSLATE);
                b.air(x, 1, z, x, HEIGHT - 1, z);
            }
        }
        b.fill(MID - 1, 0, 0, MID + 1, 0, FACADE - 1, RUNIC.base());
        for (int x = 0; x < WIDTH; x++) {
            if (Math.abs(x - MID) > 3) {
                b.set(x, 1, 0, wall(RUNIC));
            }
        }
        for (int z = 1; z < FACADE; z++) {
            b.set(0, 1, z, wall(RUNIC));
            b.set(WIDTH - 1, 1, z, wall(RUNIC));
        }
        // Runic Column posts along the parapet, each with a lantern on top.
        for (int x : new int[]{0, MID - 4, MID + 4, WIDTH - 1}) {
            b.runicColumn(x, 1, 2, 0);
            b.set(x, 3, 0, lantern(false));
        }
        b.brazier(2, 1, 2);
        b.brazier(WIDTH - 3, 1, 2);

        // The facade: solid masonry from the terrace to the back of the piece.
        for (int x = 0; x < WIDTH; x++) {
            for (int z = FACADE; z < DEPTH; z++) {
                b.foundation(x, z);
                b.set(x, 0, z, Blocks.POLISHED_DEEPSLATE);
                for (int y = 1; y < HEIGHT; y++) {
                    b.set(x, y, z, masonry(b));
                }
            }
        }
        // Base course, string course and a gold band.
        b.fill(0, 1, FACADE, WIDTH - 1, 1, FACADE, RUNIC.base());
        b.fill(0, 11, FACADE, WIDTH - 1, 11, FACADE, chiseledRunic());
        b.fill(0, 15, FACADE, WIDTH - 1, 15, FACADE, Blocks.GILDED_BLACKSTONE);
        // A battered plinth: the foot of the facade slopes out onto the terrace, except at the door.
        for (int x = 0; x < WIDTH; x++) {
            if (Math.abs(x - MID) > 3) {
                b.set(x, 1, FACADE - 1, stairs(RUNIC, IN, false));
            }
        }
        // Buttresses up the facade's corners: vertical slabs on the plinth.
        for (int x : new int[]{0, WIDTH - 1}) {
            b.fill(x, 2, FACADE - 1, x, 14, FACADE - 1, facing(DwarvenBlocks.RUNIC_DEEPSLATE_BRICK_VERTICAL_SLAB, OUT));
        }
        // Corbels under the cornice.
        for (int x = 1; x < WIDTH; x += 2) {
            b.corbel(x, 15, FACADE - 1, OUT);
        }
        // Cornice overhanging the terrace, and battlements along the top.
        for (int x = 0; x < WIDTH; x++) {
            b.set(x, 16, FACADE - 1, stairs(Blocks.DEEPSLATE_TILE_STAIRS, OUT, true));
            if (x % 2 == 1) {
                b.set(x, HEIGHT - 1, FACADE, Blocks.AIR);
            } else {
                b.set(x, HEIGHT - 1, FACADE, wall(RUNIC));
            }
        }

        // The passage, five wide and seven tall, with a portcullis drawn up into the arch.
        b.air(MID - 2, 1, FACADE, MID + 2, 7, DEPTH - 1);
        b.fill(MID - 2, 0, FACADE, MID + 2, 0, DEPTH - 1, RUNIC.base());
        b.fill(MID - 1, 1, FACADE, MID + 1, 1, DEPTH - 1, Blocks.RED_CARPET);
        b.fill(MID - 2, 7, FACADE + 1, MID + 2, 7, FACADE + 1, Blocks.IRON_BARS);
        b.archTop(MID - 2, MID + 2, 7, FACADE);
        b.set(MID, 7, DEPTH - 2, lantern(true));
        // Chiseled runic door frame with a gold keystone.
        b.fill(MID - 3, 1, FACADE, MID - 3, 8, FACADE, chiseledRunic());
        b.fill(MID + 3, 1, FACADE, MID + 3, 8, FACADE, chiseledRunic());
        b.fill(MID - 3, 8, FACADE, MID + 3, 8, FACADE, chiseledRunic());
        b.set(MID, 8, FACADE, Blocks.GOLD_BLOCK);

        // The clan crest above the door, in a gilded lozenge.
        b.crest(MID, 13, FACADE, OUT);
        for (int[] p : new int[][]{{-1, 12}, {1, 12}, {-1, 14}, {1, 14}, {-2, 13}, {2, 13}}) {
            b.set(MID + p[0], p[1], FACADE, Blocks.GILDED_BLACKSTONE);
        }
        b.set(MID, 12, FACADE, chiseledRunic());
        b.set(MID, 14, FACADE, chiseledRunic());

        // Pillars standing out in front of the facade, with banners between them and the door.
        for (int x : new int[]{4, WIDTH - 5}) {
            b.column(x, FACADE - 1, 14, IN);
            b.set(x, 15, FACADE - 1, Blocks.GOLD_BLOCK);
        }
        b.banner(MID - 5, 10, FACADE - 1, OUT, banner, ModBannerPatterns.CLAN_ANVIL.in(DyeColor.YELLOW));
        b.banner(MID + 5, 10, FACADE - 1, OUT, banner, ModBannerPatterns.CLAN_ANVIL.in(DyeColor.YELLOW));
        b.set(MID - 5, 6, FACADE - 1, lantern(false));
        b.set(MID - 5, 5, FACADE - 1, wall(RUNIC));
        b.set(MID + 5, 6, FACADE - 1, lantern(false));
        b.set(MID + 5, 5, FACADE - 1, wall(RUNIC));
        b.fill(MID - 5, 1, FACADE - 1, MID - 5, 4, FACADE - 1, wall(RUNIC));
        b.fill(MID + 5, 1, FACADE - 1, MID + 5, 4, FACADE - 1, wall(RUNIC));

        // Arrow slits.
        for (int x : new int[]{2, WIDTH - 3}) {
            b.fill(x, 8, FACADE, x, 9, FACADE, Blocks.IRON_BARS);
            b.corbel(x, 7, FACADE - 1, OUT);
        }

        // Wanted Posters on the facade either side of the door, between the frame and the banner walls.
        b.set(MID - 4, 3, FACADE - 1, AdventuringBlocks.WANTED_POSTER.poster(OUT, 1));
        b.set(MID + 4, 2, FACADE - 1, AdventuringBlocks.WANTED_POSTER.poster(OUT, 3));

        b.dwarf(MID - 3, 1, 2);
        b.dwarf(MID + 3, 1, 2);

        // The waystone beside the approach, naming the hold to whoever comes up to the gate.
        b.set(MID + 2, 1, 1, facing(DwarvenBlocks.WAYSTONE, OUT));
        b.set(MID + 2, 2, 1, facing(DwarvenBlocks.WAYSTONE, OUT).with(WaystoneBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /**
     * The siege of a desolate fortress, scaled by its level's {@link mattonfire.dnd.world.gen.DesolationLevel#siege}:
     * the doorway smashed in, holes knocked deep into the facade (through to the hall behind, some of them), the
     * battlements and top of the facade broken off in a ragged line, the pillars in front toppled and lying on the
     * ground, gaps in the parapet, and the knocked-out masonry thrown out and heaped down the slope in front.
     */
    @Override
    public boolean ruin(Ruin ruin) {
        float siege = ruin.level().siege;
        Direction out = this.worldDirection(0, -1);
        int cover = this.boundingBox.getMaxY();
        int reach = 2 + (int) (4 * siege);
        BlockState[] stone = {RUNIC.base().getDefaultState(), RUNIC.base().getDefaultState(),
                Blocks.DEEPSLATE_BRICKS.getDefaultState(), Blocks.COBBLED_DEEPSLATE.getDefaultState(),
                Blocks.POLISHED_DEEPSLATE.getDefaultState(), ModBlockFamilies.CRACKED_RUNIC_DEEPSLATE_BRICKS.base().getDefaultState()};
        // Where the holes are (local x, radius), for the heaps under them.
        float[][] holes = new float[2 + (int) (1 + 6 * siege)][];
        int count = 0;

        // The doorway smashed in: frame, keystone, arch and portcullis.
        boolean smashed = ruin.roll(0, 0, 0, 100) < 0.25F + siege;
        if (smashed) {
            float x = MID + (ruin.roll(0, 1, 0, 100) - 0.5F) * 3;
            float radius = 2.5F + 1.5F * siege + ruin.roll(0, 2, 0, 100);
            ruin.breach(this.worldPos(Math.round(x), 6, FACADE - 1), this.worldPos(Math.round(x), 5, FACADE + 2), radius, out, reach);
            holes[count++] = new float[]{x, radius};
        }
        // Holes knocked into the facade. Most of them (more the worse the siege) go right through it and the hall's
        // front wall behind, into the dark of the hall; the rest are craters a few blocks deep.
        int breaches = (int) (1 + 6 * siege) + (ruin.roll(1, 0, 0, 101) < (1 + 6 * siege) % 1 ? 1 : 0);
        for (int j = 0; j < breaches; j++) {
            boolean through = ruin.roll(j, 0, 0, 102) < 0.3F + 0.6F * siege;
            int x = through ? HALL_SIDE + (int) (ruin.roll(j, 1, 0, 102) * (WIDTH - 2 * HALL_SIDE))
                    : 1 + (int) (ruin.roll(j, 1, 0, 102) * (WIDTH - 2));
            int y = through ? 2 + (int) (ruin.roll(j, 2, 0, 102) * (HALL_TOP - 3)) : 2 + (int) (ruin.roll(j, 2, 0, 102) * 13);
            float radius = 1.5F + ruin.roll(j, 3, 0, 102) * (0.5F + 2 * siege);
            int depth = through ? DEPTH + 1 - FACADE : 1 + (int) (ruin.roll(j, 4, 0, 102) * 3);
            int dx = (int) ((ruin.roll(j, 5, 0, 102) - 0.5F) * 4);
            int dy = (int) ((ruin.roll(j, 6, 0, 102) - 0.5F) * 3);
            ruin.breach(this.worldPos(x, y, FACADE - 1), this.worldPos(x + dx, Math.max(2, y + dy), FACADE + depth), radius, out, reach);
            if (count < holes.length) {
                holes[count++] = new float[]{x, radius};
            }
        }

        // Pillars toppled: broken off and lying on the terrace and the slope in front.
        boolean[] toppled = new boolean[PILLARS.length];
        for (int i = 0; i < PILLARS.length; i++) {
            int px = PILLARS[i];
            if (ruin.roll(i, 0, 0, 103) >= 0.1F + 0.8F * siege) {
                continue;
            }
            toppled[i] = true;
            int at = 2 + (int) (ruin.roll(i, 1, 0, 103) * 6);
            BlockPos foot = this.worldPos(px, at, FACADE - 1);
            ruin.crumble(foot.getX(), foot.getZ(), this.worldPos(px, HEIGHT + 1, 0).getY(), foot.getY(), cover, out, 1);
            // Its capital's stairs go with it.
            for (int[] side : new int[][]{{px - 1, FACADE - 1}, {px + 1, FACADE - 1}, {px, FACADE - 2}}) {
                BlockPos s = this.worldPos(side[0], PILLAR_TOP - 1, side[1]);
                ruin.crumble(s.getX(), s.getZ(), s.getY(), s.getY(), cover, out, 1);
            }
            BlockState[] fallen = new BlockState[PILLAR_TOP - at + 1];
            for (int y = at; y <= PILLAR_TOP; y++) {
                fallen[y - at] = y == PILLAR_TOP ? chiseledRunic().getDefaultState()
                        : pillar(ModBlockFamilies.GOLD_VEINED_PILLAR, Direction.Axis.Y);
            }
            ruin.topple(this.worldPos(px, at, FACADE - 3), out, fallen);
        }

        // The top broken off in a ragged line: battlements, cornice, gold band and the masonry behind, stepping
        // down towards the front.
        for (int x = 0; x < WIDTH; x++) {
            float n = ruin.patches(x, 0, 4, 104);
            int rows = (int) (n * n * (1 + 9 * siege)) + (ruin.roll(x, 0, 0, 105) < 0.3F * siege ? 1 : 0);
            for (int dz = -1; dz <= 2; dz++) {
                int z = FACADE + dz;
                if (dz == -1 && (x == PILLARS[0] && toppled[0] || x == PILLARS[1] && toppled[1])) {
                    continue;
                }
                int r = dz <= 0 ? rows : rows - 2 * dz + (ruin.roll(x, dz, 0, 106) < 0.5F ? 1 : 0);
                if (r <= 0) {
                    continue;
                }
                BlockPos top = this.worldPos(x, HEIGHT + 1, z);
                ruin.crumble(top.getX(), top.getZ(), top.getY(), this.worldPos(0, HEIGHT - r, 0).getY(), cover, out, reach);
            }
        }

        // Great bites out of the top, right back through the facade.
        int bites = (int) (2 * siege) + (ruin.roll(2, 0, 0, 101) < (2 * siege) % 1 ? 1 : 0);
        for (int j = 0; j < bites; j++) {
            float cx = ruin.roll(j, 0, 0, 109) * (WIDTH - 1);
            float half = 2 + ruin.roll(j, 1, 0, 109) * 3;
            float deep = 4 + ruin.roll(j, 2, 0, 109) * 6 * siege;
            for (int x = (int) Math.floor(cx - half); x <= (int) Math.ceil(cx + half); x++) {
                if (x < 0 || x >= WIDTH) {
                    continue;
                }
                int rows = Math.round(deep * (1 - Math.abs(x - cx) / half)) + (ruin.roll(x, j, 1, 109) < 0.4F ? 1 : 0);
                for (int z = FACADE - 1; z < DEPTH && rows > 0; z++) {
                    BlockPos top = this.worldPos(x, HEIGHT + 1, z);
                    ruin.crumble(top.getX(), top.getZ(), top.getY(), this.worldPos(0, HEIGHT - rows, 0).getY(), cover, out, reach);
                }
            }
        }

        // Gaps in the parapet, and lantern posts knocked down.
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < FACADE; z++) {
                if (z > 0 && x > 0 && x < WIDTH - 1 || ruin.roll(x, z, 0, 107) >= 0.55F * siege) {
                    continue;
                }
                BlockPos top = this.worldPos(x, 3, z);
                ruin.crumble(top.getX(), top.getZ(), top.getY(), this.worldPos(0, 1, 0).getY(), cover, out, 2);
            }
        }

        // A scree of masonry down the slope in front, thickest at the foot of the wall and under the holes.
        for (int x = -SCREE_SIDE; x < WIDTH + SCREE_SIDE; x++) {
            float lateral = x < 0 ? 1 + x / (float) SCREE_SIDE : x >= WIDTH ? 1 - (x - WIDTH + 1) / (float) SCREE_SIDE : 1;
            for (int z = FACADE - SCREE_OUT; z < FACADE; z++) {
                int dist = FACADE - z;
                float under = 0;
                for (int i = 0; i < count; i++) {
                    if (Math.abs(x - holes[i][0]) <= holes[i][1] + 1) {
                        under += 0.5F;
                    }
                }
                float amount = siege * lateral * (1.2F + Math.min(under, 1.0F)) * (float) Math.exp(-(dist - 1) / 3.5F)
                        * (0.35F + ruin.patches(x, z, 3, 108));
                BlockPos at = this.worldPos(x, dist <= 2 ? 9 : HEIGHT + 2, z);
                ruin.scree(at.getX(), at.getY(), at.getZ(), amount, stone);
            }
        }
        // And in the passage, under the smashed doorway.
        if (smashed) {
            for (int x = MID - 2; x <= MID + 2; x++) {
                for (int z = FACADE; z < DEPTH; z++) {
                    float amount = siege * (float) Math.exp(-(z - FACADE) / 2.5F) * (0.35F + ruin.patches(x, z, 3, 108));
                    BlockPos at = this.worldPos(x, 6, z);
                    ruin.scree(at.getX(), at.getY(), at.getZ(), amount, stone);
                }
            }
        }

        BlockBox box = this.boundingBox;
        ruin.tidy(new BlockBox(box.getMinX() - 1, box.getMinY(), box.getMinZ() - 1, box.getMaxX() + 1, box.getMaxY() + 2, box.getMaxZ() + 1));
        return true;
    }
}
