package mattonfire.dnd.world.gen.village;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * The middle of the village: a round green with the party tree, long tables laden with food all
 * around it, a striped party pavilion, ale barrels and a bonfire, ringed by a path that the
 * village's lanes run into.
 */
public class VillageGreenPiece extends HobbitPiece {
    public static final int RADIUS = 10;
    private static final int SIZE = 2 * RADIUS + 1;
    private static final int HEIGHT = 16;
    private static final int C = RADIUS;

    private static final Block[] AWNINGS = {Blocks.YELLOW_WOOL, Blocks.GREEN_WOOL, Blocks.RED_WOOL, Blocks.LIGHT_BLUE_WOOL};

    public VillageGreenPiece(Random random, int x, int y, int z) {
        super(HobbitVillageStructures.GREEN, centeredBox(x, y, z, Direction.NORTH, SIZE, HEIGHT, SIZE),
                Direction.Type.HORIZONTAL.random(random), random.nextLong());
    }

    public VillageGreenPiece(NbtCompound nbt) {
        super(HobbitVillageStructures.GREEN, nbt);
    }

    private static double dist(int x, int z) {
        return Math.sqrt((x - C) * (x - C) + (z - C) * (z - C));
    }

    @Override
    protected void build(Builder b) {
        // Lawn with a ring path round the edge, scattered with flowers.
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                double d = dist(x, z);
                if (d > RADIUS + 0.4D) {
                    continue;
                }
                boolean ring = d >= RADIUS - 1.5D && d < RADIUS - 0.4D;
                b.ground(x, z, ring ? (b.chance(0.85F) ? Blocks.DIRT_PATH : Blocks.COARSE_DIRT) : Blocks.GRASS_BLOCK);
                b.air(x, 1, z, x, HEIGHT - 1, z);
                if (!ring) {
                    float flowers = d < 4.0D ? 0.45F : 0.12F;
                    if (b.chance(flowers)) {
                        b.flower(x, 1, z);
                    } else if (b.chance(0.15F)) {
                        b.set(x, 1, z, Blocks.GRASS);
                    }
                }
            }
        }

        this.partyTree(b);

        // Long tables on three sides, and one under the pavilion.
        this.tableAlongX(b, 7, 13, 16);
        this.tableAlongZ(b, 4, 7, 13);
        this.tableAlongZ(b, 16, 7, 13);
        this.pavilion(b);
        this.tableAlongX(b, 7, 13, 4);

        // Ale and seats in one corner, a bonfire in another, cakes and harvest in the others.
        b.barrel(15, 1, 15, Direction.UP, ALE);
        b.barrel(14, 1, 15, Direction.UP, ALE);
        b.barrel(15, 2, 15, Direction.UP, ALE);
        b.plate(14, 2, 15);
        b.set(15, 1, 14, Blocks.HAY_BLOCK);
        b.set(5, 1, 15, Blocks.CAMPFIRE);
        b.set(4, 1, 15, Blocks.HAY_BLOCK);
        b.set(6, 1, 16, Blocks.HAY_BLOCK);
        b.set(5, 1, 14, stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false));
        b.barrel(15, 1, 5, Direction.UP, HARVEST);
        b.set(15, 2, 5, Blocks.PUMPKIN);
        b.set(16, 1, 6, Blocks.MELON);
        b.set(5, 1, 5, Blocks.HAY_BLOCK);
        b.set(5, 2, 5, Blocks.CAKE);
        b.barrel(4, 1, 6, Direction.UP, PANTRY);
        b.set(4, 2, 6, Blocks.CAKE);

        // Lamp posts where the lanes come in.
        for (int[] p : new int[][]{{3, 3}, {17, 3}, {3, 17}, {17, 17}}) {
            b.set(p[0], 1, p[1], Blocks.OAK_FENCE);
            b.set(p[0], 2, p[1], lantern(false));
        }

        b.hobbit(8, 1, 8);
        b.hobbit(13, 1, 13);
        b.hobbit(8, 1, 13);
        b.hobbit(10, 1, 15);
        if (b.chance(0.5F)) {
            b.hobbit(13, 1, 8);
        }
    }

    private void partyTree(Builder b) {
        // Canopy first, so the trunk and boughs cut through it.
        for (int dx = -6; dx <= 7; dx++) {
            for (int dz = -6; dz <= 7; dz++) {
                for (int dy = -3; dy <= 3; dy++) {
                    double fx = (dx - 0.5D) / 6.2D;
                    double fz = (dz - 0.5D) / 6.2D;
                    double fy = dy / 3.3D;
                    double e = fx * fx + fy * fy + fz * fz;
                    boolean keep = e <= 1.0D && (dy <= 0 || b.random.nextFloat() > (e - 0.6D) * 1.5D);
                    if (keep) {
                        Block leaves = b.chance(0.15F) ? Blocks.FLOWERING_AZALEA_LEAVES : Blocks.OAK_LEAVES;
                        b.set(C + dx, 10 + dy, C + dz, leaves(leaves));
                    }
                }
            }
        }
        b.fill(C, 1, C, C + 1, 9, C + 1, log(Blocks.OAK_LOG, Direction.Axis.Y));
        // Roots
        b.set(C - 1, 1, C, log(Blocks.OAK_LOG, Direction.Axis.X));
        b.set(C + 2, 1, C + 1, log(Blocks.OAK_LOG, Direction.Axis.X));
        b.set(C + 1, 1, C - 1, log(Blocks.OAK_LOG, Direction.Axis.Z));
        b.set(C, 1, C + 2, log(Blocks.OAK_LOG, Direction.Axis.Z));
        // Boughs
        b.fill(C + 2, 7, C, C + 4, 8, C, log(Blocks.OAK_LOG, Direction.Axis.X));
        b.fill(C - 3, 8, C + 1, C - 1, 8, C + 1, log(Blocks.OAK_LOG, Direction.Axis.X));
        b.fill(C + 1, 8, C + 2, C + 1, 8, C + 4, log(Blocks.OAK_LOG, Direction.Axis.Z));
        b.fill(C, 7, C - 3, C, 7, C - 1, log(Blocks.OAK_LOG, Direction.Axis.Z));

        // Lanterns hanging from the underside of the canopy.
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI / 6.0D + b.random.nextFloat() * 0.3D;
            double r = 3.5D + b.random.nextFloat() * 2.0D;
            int dx = (int) Math.round(Math.cos(angle) * r);
            int dz = (int) Math.round(Math.sin(angle) * r);
            double fx = (dx - 0.5D) / 6.2D;
            double fz = (dz - 0.5D) / 6.2D;
            double rest = 1.0D - fx * fx - fz * fz;
            if (rest <= 0.0D) {
                continue;
            }
            int bottom = 10 - (int) Math.floor(3.3D * Math.sqrt(rest));
            b.set(C + dx, bottom - 1, C + dz, lantern(true));
        }
    }

    private void tableAlongX(Builder b, int x0, int x1, int z) {
        for (int x = x0; x <= x1; x++) {
            b.set(x, 1, z, topSlab(Blocks.SPRUCE_SLAB));
            b.set(x, 2, z, Blocks.AIR);
            b.tableFood(x, 2, z);
            if ((x - x0) % 2 == 0) {
                b.set(x, 1, z - 1, stairs(Blocks.OAK_STAIRS, OUT, false));
                b.set(x, 1, z + 1, stairs(Blocks.OAK_STAIRS, IN, false));
            } else {
                b.set(x, 1, z - 1, Blocks.AIR);
                b.set(x, 1, z + 1, Blocks.AIR);
            }
        }
    }

    private void tableAlongZ(Builder b, int x, int z0, int z1) {
        for (int z = z0; z <= z1; z++) {
            b.set(x, 1, z, topSlab(Blocks.SPRUCE_SLAB));
            b.set(x, 2, z, Blocks.AIR);
            b.tableFood(x, 2, z);
            if ((z - z0) % 2 == 0) {
                b.set(x - 1, 1, z, stairs(Blocks.OAK_STAIRS, Direction.WEST, false));
                b.set(x + 1, 1, z, stairs(Blocks.OAK_STAIRS, Direction.EAST, false));
            } else {
                b.set(x - 1, 1, z, Blocks.AIR);
                b.set(x + 1, 1, z, Blocks.AIR);
            }
        }
    }

    /** A striped awning on four posts over the south table, peaked along its middle. */
    private void pavilion(Builder b) {
        Block stripe = b.pick(AWNINGS);
        for (int x = 6; x <= 14; x++) {
            Block wool = x % 2 == 0 ? Blocks.WHITE_WOOL : stripe;
            for (int z = 2; z <= 6; z++) {
                b.set(x, 4, z, wool);
            }
            b.set(x, 5, 4, wool);
        }
        for (int[] p : new int[][]{{6, 2}, {14, 2}, {6, 6}, {14, 6}}) {
            b.fill(p[0], 1, p[1], p[0], 3, p[1], Blocks.SPRUCE_FENCE);
        }
        b.set(8, 3, 4, lantern(true));
        b.set(12, 3, 4, lantern(true));
    }
}
