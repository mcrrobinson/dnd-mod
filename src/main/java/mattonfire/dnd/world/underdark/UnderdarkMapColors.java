package mattonfire.dnd.world.underdark;

import net.minecraft.block.MapColor;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

/**
 * How maps of the Underdark get drawn ({@code FilledMapMixin}). Vanilla draws a dimension with a ceiling as stone and
 * dirt noise instead of reading its blocks, and an explorer map's head start only outlines water biomes sampled at
 * y 0, so Underdark maps stayed blank parchment. Here a map held in the Underdark draws the cave floor at the
 * holder's level as they explore, and an explorer map starts with the Darklake's shores outlined like vanilla's
 * oceans.
 */
public final class UnderdarkMapColors {
    /** How far above the holder's feet a column's scan starts: rock there draws as a wall. */
    public static final int SCAN_ABOVE_FEET = 2;
    /** The highest a scan starts: under the roof's bedrock (its bottom is at y -69). */
    public static final int SCAN_TOP = -70;
    /** The depth an explorer map's head start samples biomes at: the median cavern floor. */
    public static final int EXPLORER_BIOME_Y = -180;

    private UnderdarkMapColors() {
    }

    /**
     * The y of the block a map pixel shows in column {@code x z} for {@code holder}: the first block with a map colour
     * at or below a little above the holder's feet. Open cave shows its floor (or the water's surface); rock at that
     * level shows as a wall.
     */
    public static int surfaceY(World world, WorldChunk chunk, Entity holder, int x, int z) {
        int bottom = world.getBottomY();
        int start = MathHelper.clamp(MathHelper.floor(holder.getY()) + SCAN_ABOVE_FEET, bottom + 1, SCAN_TOP);
        BlockPos.Mutable pos = new BlockPos.Mutable(x, start, z);
        for (int y = start; y > bottom; y--) {
            pos.setY(y);
            if (chunk.getBlockState(pos).getMapColor(world, pos) != MapColor.CLEAR) {
                return y;
            }
        }
        return bottom;
    }
}
