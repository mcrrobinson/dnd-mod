package mattonfire.dnd.world.gen;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * A piece that knows where its own walls are and plans its own collapse in a desolate settlement, instead of the
 * breaches and holes {@link Withering} aims at its box. {@link Withering} calls {@link #ruin} in every chunk the
 * start reaches, after the per-block withering; the piece plans the same collapse every time from its own data and
 * {@link Ruin#roll}, and the {@link Ruin} carves only the part in this chunk. Nothing a piece plans may depend on the
 * blocks in another chunk.
 */
public interface Ruinable {
    /**
     * Carves this chunk's part of the piece's collapse.
     *
     * @return true if that's the piece's whole collapse (no breaches or holes aimed at its box as well)
     */
    boolean ruin(Ruin ruin);

    /** What a {@link Ruinable} piece knocks its collapse out with. Positions are world positions. */
    interface Ruin {
        DesolationLevel level();

        /** A number in [0, 1) fixed by the start, the piece and the arguments, the same in every chunk. */
        float roll(int a, int b, int c, int k);

        /** Smooth noise in [0, 1) over a/b, in blobs about {@code size} across ({@link Desolation#patches}). */
        float patches(int a, int b, int size, int k);

        /** Whether anything within {@code r} blocks of column x/z is in this chunk. */
        boolean near(int x, int z, int r);

        /**
         * A ragged hole of about {@code radius} round the line from {@code from} to {@code to}, through every built
         * block on the way. Some of what it knocks out is thrown {@code out} up to {@code reach} blocks and lies on
         * the ground.
         */
        void breach(BlockPos from, BlockPos to, float radius, Direction out, int reach);

        /**
         * Knocks column x/z down from {@code top} to {@code bottom}: the built blocks, and anything at all above
         * {@code cover} (rock heaped on top of a wall). Left alone if anything but air stands on {@code top}, so
         * the mountain over a wall never floats. Some of it is thrown {@code out} up to {@code reach} blocks.
         */
        void crumble(int x, int z, int top, int bottom, int cover, Direction out, int reach);

        /**
         * A pillar that broke and fell {@code out}: {@code fallen} (its blocks from the break up) lie end to end on
         * the ground from {@code foot} outwards, laid on their side, some of them smashed to rubble.
         */
        void topple(BlockPos foot, Direction out, BlockState[] fallen);

        /**
         * Debris on the ground in column x/z, looking down from {@code y}: rubble, slabs or whole blocks of
         * {@code materials}, more and bigger the higher {@code amount} (0 for none, 1 for a lot).
         */
        void scree(int x, int y, int z, float amount, BlockState[] materials);

        /** Takes away the hanging and attached blocks in {@code area} (banners, lanterns, torches) left with nothing to hold them. */
        void tidy(BlockBox area);
    }
}
