package mattonfire.dnd.entity;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Finds standing room for a mob being placed near something: a summoned minion, a raised undead, a
 * phylactery, a raid goblin. Shared by the Lich, the Goblin Warlord, the Magmamuncher Alpha and goblin raids.
 */
public final class StandingRoom {
    private StandingRoom() {
    }

    /**
     * Tries {@code attempts} random spots {@code min}-{@code max} blocks (horizontally) around
     * ({@code x}, {@code y}, {@code z}), each scanned from {@code up} blocks above to {@code down} below
     * (see {@link #inColumn}). Leaves {@code entity} at the first spot that fits.
     */
    public static boolean around(World world, Entity entity, Random random, double x, double y, double z,
            double min, double max, int attempts, int up, int down, boolean dry) {
        for (int attempt = 0; attempt < attempts; attempt++) {
            float angle = random.nextFloat() * MathHelper.TAU;
            double distance = min + random.nextDouble() * (max - min);
            BlockPos column = BlockPos.ofFloored(x + MathHelper.cos(angle) * distance, y,
                    z + MathHelper.sin(angle) * distance);
            if (inColumn(world, entity, random, column, up, down, dry)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Scans the column at {@code column} from {@code up} blocks above it down to {@code down} below it
     * for a spot on a solid floor where {@code entity} fits (and, if {@code dry}, isn't in a fluid).
     * Leaves the entity there, facing a random way, and returns true; false if none fits.
     */
    public static boolean inColumn(World world, Entity entity, Random random, BlockPos column, int up, int down,
            boolean dry) {
        for (int dy = up; dy >= -down; dy--) {
            BlockPos pos = column.up(dy);
            if (!world.getBlockState(pos.down()).isSideSolidFullSquare(world, pos.down(), Direction.UP)) {
                continue;
            }
            entity.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                    random.nextFloat() * 360.0F, 0.0F);
            if (world.isSpaceEmpty(entity) && (!dry || !world.containsFluid(entity.getBoundingBox()))) {
                return true;
            }
        }
        return false;
    }
}
