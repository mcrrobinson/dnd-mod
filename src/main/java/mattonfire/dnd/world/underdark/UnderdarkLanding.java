package mattonfire.dnd.world.underdark;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LanternBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Finds somewhere to stand in the Underdark at a given x and z, or carves a pocket there (design 9, 2.6).
 * <p>
 * The search looks for a solid, dry floor with two blocks of air above it between y {@link #HIGHEST} and
 * {@link #LOWEST}. It prefers floors in an open cavern ({@link #HEADROOM} blocks of air), then those nearest
 * y {@link #PREFERRED}, trying the column itself, then rings of columns out to {@link #SEARCH_RADIUS} blocks; a
 * cramped floor in the column (or the nearest ring that has one) is the fallback. If none has a floor it carves a 5x5x3 pocket at y {@link #CARVE_Y} with a
 * deepslate floor and a lantern, sealing any fluid round it so the pocket stays dry. (The Rift Anchor that later
 * stands in carved pockets comes with the rifts ticket.)
 */
public final class UnderdarkLanding {
    public static final int HIGHEST = -75;
    public static final int LOWEST = -250;
    public static final int PREFERRED = -90;
    public static final int SEARCH_RADIUS = 8;
    public static final int CARVE_Y = -150;
    /** Blocks of air over the feet that make a floor a cavern worth landing in rather than a crawlway. */
    public static final int HEADROOM = 6;

    /** Where to stand (the feet's block) and how it was found. */
    public record Spot(BlockPos feet, Kind kind) {
    }

    public enum Kind {
        FOUND, CARVED;

        public String id() {
            return this.name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private UnderdarkLanding() {
    }

    public static Spot findOrMake(ServerWorld world, int x, int z) {
        BlockPos cramped = null;
        for (int r = 0; r <= SEARCH_RADIUS; r++) {
            BlockPos roomy = null;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    BlockPos[] found = floors(world, x + dx, z + dz);
                    roomy = nearer(roomy, found[0]);
                    if (r == 0 || cramped == null) {
                        cramped = nearer(cramped, found[1]);
                    }
                }
            }
            if (roomy != null) {
                return new Spot(roomy, Kind.FOUND);
            }
        }
        if (cramped != null) {
            return new Spot(cramped, Kind.FOUND);
        }
        return new Spot(carve(world, x, z), Kind.CARVED);
    }

    private static BlockPos nearer(BlockPos a, BlockPos b) {
        if (a == null || b == null) {
            return a == null ? b : a;
        }
        return Math.abs(b.getY() - PREFERRED) < Math.abs(a.getY() - PREFERRED) ? b : a;
    }

    /**
     * The feet positions over this column's standable floors nearest {@link #PREFERRED}: [0] one with at least
     * {@link #HEADROOM} blocks of air above (a cavern, not a crawlway), [1] any. Either may be null.
     */
    static BlockPos[] floors(ServerWorld world, int x, int z) {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        BlockPos any = null;
        for (int d = 0;; d++) {
            int above = PREFERRED + d;
            int below = PREFERRED - d;
            if (above > HIGHEST && below < LOWEST) {
                break;
            }
            for (int y : d == 0 ? new int[] { above } : new int[] { above, below }) {
                if (y > HIGHEST || y < LOWEST || !standable(world, pos.set(x, y, z))) {
                    continue;
                }
                if (roomy(world, pos)) {
                    return new BlockPos[] { pos.toImmutable(), any == null ? pos.toImmutable() : any };
                }
                if (any == null) {
                    any = pos.toImmutable();
                }
            }
        }
        return new BlockPos[] { null, any };
    }

    private static boolean roomy(ServerWorld world, BlockPos feet) {
        for (int dy = 2; dy < HEADROOM; dy++) {
            if (!world.getBlockState(feet.up(dy)).isAir()) {
                return false;
            }
        }
        return true;
    }

    /** Whether a player can stand with their feet at {@code feet}: two blocks of air on a dry, solid floor. */
    public static boolean standable(ServerWorld world, BlockPos feet) {
        BlockState floor = world.getBlockState(feet.down());
        return world.getBlockState(feet).isAir() && world.getBlockState(feet.up()).isAir()
                && floor.isSolidBlock(world, feet.down()) && floor.getFluidState().isEmpty()
                && !floor.isOf(Blocks.MAGMA_BLOCK) && !floor.isOf(Blocks.BEDROCK);
    }

    /** The yaw (of 16) that looks down the longest stretch of open air at eye level from {@code feet}. */
    public static float openYaw(ServerWorld world, BlockPos feet) {
        float bestYaw = 0;
        int bestRun = -1;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int i = 0; i < 16; i++) {
            float yaw = i * 22.5F;
            double dx = -Math.sin(Math.toRadians(yaw));
            double dz = Math.cos(Math.toRadians(yaw));
            int run = 0;
            while (run < 48) {
                pos.set(feet.getX() + 0.5 + dx * (run + 1), feet.getY() + 1, feet.getZ() + 0.5 + dz * (run + 1));
                if (!world.getBlockState(pos).isAir()) {
                    break;
                }
                run++;
            }
            if (run > bestRun) {
                bestRun = run;
                bestYaw = yaw;
            }
        }
        return bestYaw;
    }

    private static BlockPos carve(ServerWorld world, int x, int z) {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        // A shell first, so no water or lava from a neighbouring cave pours in.
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -2; dy <= 3; dy++) {
                    pos.set(x + dx, CARVE_Y + dy, z + dz);
                    if (!world.getFluidState(pos).isEmpty() || world.getBlockState(pos).isAir()) {
                        world.setBlockState(pos, Blocks.DEEPSLATE.getDefaultState());
                    }
                }
            }
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.setBlockState(pos.set(x + dx, CARVE_Y - 1, z + dz), Blocks.DEEPSLATE_TILES.getDefaultState());
                for (int dy = 0; dy < 3; dy++) {
                    world.setBlockState(pos.set(x + dx, CARVE_Y + dy, z + dz), Blocks.AIR.getDefaultState());
                }
            }
        }
        world.setBlockState(new BlockPos(x + 2, CARVE_Y, z + 2), Blocks.LANTERN.getDefaultState()
                .with(LanternBlock.HANGING, false));
        return new BlockPos(x, CARVE_Y, z);
    }
}
