package mattonfire.dnd.world.gen.enclave;

import mattonfire.dnd.world.gen.TerrainColumns;
import net.minecraft.world.gen.structure.Structure;

/** Terrain heights from the noise generator, before anything (trees included) is built, cached per column. */
final class EnclaveTerrain {
    /** Rings of samples round the middle: radius 8, 16 and 24. */
    private static final int RING_STEP = 8;
    private static final int RINGS = 3;
    /** How far a sample may rise or fall from the middle. */
    static final int MAX_RISE = 6;
    /** How many of the 24 samples may be wet, too steep or out of the forest. */
    private static final int MAX_BAD = 4;

    private final TerrainColumns columns;

    EnclaveTerrain(Structure.Context context) {
        this.columns = new TerrainColumns(context);
    }

    boolean biomeAllowed(int x, int z) {
        return this.columns.biomeAllowed(x, z);
    }

    /** y of the top block (water surface included). */
    int ground(int x, int z) {
        return this.columns.ground(x, z);
    }

    boolean water(int x, int z) {
        return this.columns.water(x, z);
    }

    /** Dry, fairly level (within {@link #MAX_RISE}) and forested out to radius 24. */
    boolean isGoodSite(int x, int z) {
        if (this.water(x, z) || !this.biomeAllowed(x, z)) {
            return false;
        }
        int y = this.ground(x, z);
        int bad = 0;
        for (int ring = 1; ring <= RINGS; ring++) {
            int radius = ring * RING_STEP;
            for (int i = 0; i < 8; i++) {
                double angle = (i + ring * 0.5D) * Math.PI / 4.0D;
                int sx = x + (int) Math.round(Math.cos(angle) * radius);
                int sz = z + (int) Math.round(Math.sin(angle) * radius);
                if (this.water(sx, sz) || Math.abs(this.ground(sx, sz) - y) > MAX_RISE || !this.biomeAllowed(sx, sz)) {
                    if (++bad > MAX_BAD) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** The median ground height over a square of half-size {@code r} round x/z, or MIN_VALUE if wet or steeper than {@code maxSlope}. */
    int siteHeight(int x, int z, int r, int maxSlope) {
        int[] heights = new int[9];
        int i = 0;
        for (int dx = -r; dx <= r; dx += r) {
            for (int dz = -r; dz <= r; dz += r) {
                if (this.water(x + dx, z + dz)) {
                    return Integer.MIN_VALUE;
                }
                heights[i++] = this.ground(x + dx, z + dz);
            }
        }
        java.util.Arrays.sort(heights);
        return heights[8] - heights[0] > maxSlope ? Integer.MIN_VALUE : heights[4];
    }
}
