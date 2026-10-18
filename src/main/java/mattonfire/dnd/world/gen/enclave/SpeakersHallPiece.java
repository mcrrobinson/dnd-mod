package mattonfire.dnd.world.gen.enclave;

import static mattonfire.dnd.world.gen.PieceBlocks.*;

import mattonfire.dnd.classes.Registry.ElvenBlocks;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.entity.ModEntityTypes;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.util.math.Direction;

/**
 * The Speaker's Hall: a round platform 11 across at {@link HeartTreePiece#HALL} on the Heart Tree, under
 * its canopy. Heartwood floor with a ring of Moonstone Bricks inlaid in it and moss carpet, a Starglass Pane
 * balustrade round the edge, the trunk grown over with Living Wood at the hall's height, twelve bookshelves set
 * into it, an enchanting table, a lectern, a loom and the Heart Tree chest (locked, DC 15). The Speaker keeps to it. This piece's box is the
 * elves' hearth ({@link mattonfire.dnd.world.gen.RacialHomes}). Shares its origin with the Heart Tree.
 */
public class SpeakersHallPiece extends EnclavePiece {
    public static final int RADIUS = 5;
    private static final int Y = HeartTreePiece.HALL;

    public SpeakersHallPiece(int x, int y, int z, long seed) {
        super(ElvenEnclaveStructures.HALL, box(x, y, z, -RADIUS - 1, Y, -RADIUS - 1, RADIUS + 1, Y + 3, RADIUS + 1), x, y, z, seed);
    }

    public SpeakersHallPiece(NbtCompound nbt) {
        super(ElvenEnclaveStructures.HALL, nbt);
    }

    static boolean isFloor(int x, int z) {
        int c = Math.max(Math.abs(x), Math.abs(z));
        return c > HeartTreePiece.TRUNK && x * x + z * z <= (RADIUS + 0.9D) * (RADIUS + 0.9D);
    }

    /**
     * Where the stair comes up through the floor (open, so there's headroom over the top steps). Climbing onto a
     * step at y your head reaches into the block at y + 2, over that step and the one you come
     * from, so the floor and the beam under it ({@code Y - 1}) are left out over every step from {@code Y - 4} up.
     */
    private static boolean isStairwell(int x, int z) {
        int i = HeartTreePiece.ringIndex(x, z);
        int y = i >= 0 ? HeartTreePiece.stairY(i) : -1;
        return y >= Y - 4;
    }

    @Override
    protected void build(Builder b) {
        for (int x = -RADIUS - 1; x <= RADIUS + 1; x++) {
            for (int z = -RADIUS - 1; z <= RADIUS + 1; z++) {
                if (!isFloor(x, z)) {
                    continue;
                }
                if (isStairwell(x, z)) {
                    continue;
                }
                // A ring of moonstone inlaid in the floor, round the trunk.
                boolean inlay = Math.abs(Math.sqrt(x * x + z * z) - 4.0D) < 0.5D;
                b.set(x, Y, z, inlay ? ModBlockFamilies.MOONSTONE_BRICKS.base() : ModBlockFamilies.HEARTWOOD_PLANKS.base());
                b.air(x, Y + 1, z, x, Y + 3, z);
                b.set(x, Y - 1, z, ModBlockFamilies.STRIPPED_HEARTWOOD_LOG.base());
                boolean edge = false;
                for (Direction d : Direction.Type.HORIZONTAL) {
                    int nx = x + d.getOffsetX();
                    int nz = z + d.getOffsetZ();
                    if (!isFloor(nx, nz) && Math.max(Math.abs(nx), Math.abs(nz)) > HeartTreePiece.TRUNK) {
                        edge = true;
                    }
                }
                if (edge) {
                    b.set(x, Y + 1, z, ElvenBlocks.STARGLASS_PANE);
                } else if (this.noise(x, Y, z) < 0.45F) {
                    b.set(x, Y + 1, z, Blocks.MOSS_CARPET);
                }
            }
        }
        // The trunk round the hall is grown over with living wood (its rounded corners stay out).
        for (int y = Y + 1; y <= Y + 3; y++) {
            for (int s = -HeartTreePiece.TRUNK; s <= HeartTreePiece.TRUNK; s++) {
                if (Math.abs(s) == HeartTreePiece.TRUNK) {
                    continue;
                }
                b.set(s, y, -HeartTreePiece.TRUNK, ModBlockFamilies.LIVING_WOOD.base());
                b.set(s, y, HeartTreePiece.TRUNK, ModBlockFamilies.LIVING_WOOD.base());
                b.set(-HeartTreePiece.TRUNK, y, s, ModBlockFamilies.LIVING_WOOD.base());
                b.set(HeartTreePiece.TRUNK, y, s, ModBlockFamilies.LIVING_WOOD.base());
            }
        }
        // Twelve bookshelves set into the trunk's faces.
        for (int s = -1; s <= 1; s++) {
            b.set(s, Y + 1, -HeartTreePiece.TRUNK, Blocks.BOOKSHELF);
            b.set(s, Y + 1, HeartTreePiece.TRUNK, Blocks.BOOKSHELF);
            b.set(-HeartTreePiece.TRUNK, Y + 1, s, Blocks.BOOKSHELF);
            b.set(HeartTreePiece.TRUNK, Y + 1, s, Blocks.BOOKSHELF);
        }
        // Two blocks out from the south shelves, with air between, so they power it.
        b.set(0, Y + 1, 3, Blocks.AIR);
        b.set(-1, Y + 1, 3, Blocks.AIR);
        b.set(1, Y + 1, 3, Blocks.AIR);
        b.set(0, Y + 1, 4, Blocks.ENCHANTING_TABLE);
        b.set(0, Y + 1, -4, facing(Blocks.LECTERN, Direction.NORTH));
        b.set(4, Y + 1, 0, facing(Blocks.LOOM, Direction.WEST));
        b.container(-4, Y + 1, 0, facing(Blocks.CHEST, Direction.EAST), HEART_LOOT);
        b.set(-4, Y + 1, 1, Blocks.POTTED_FLOWERING_AZALEA_BUSH);
        b.set(-4, Y + 1, -1, Blocks.POTTED_LILY_OF_THE_VALLEY);
        for (int[] c : new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}}) {
            b.set(c[0] * 4, Y + 1, c[1] * 4, lantern(false));
        }
        b.elf(3, Y + 1, 3, ModEntityTypes.ELF_SPEAKER);
    }
}
