package mattonfire.dnd.world.gen.burrow;

import net.minecraft.block.BellBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.GrindstoneBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.enums.Attachment;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The clocktower room north of the Commons: 13 by 17 and {@link #TOP} high, the tallest room in the burrow. The
 * north wall holds a great clock face of calcite with a copper rim, glowstone marks at the quarters, blackstone
 * hands at ten to twelve and a working clock in an item frame at its hub. A copper pendulum hangs on a long chain,
 * a bell from the ceiling, grindstone gears turn (or would) on the side walls, and a spruce gallery runs along both
 * side walls at y {@link #GALLERY}, reached by a ladder, with the burrow's strongbox (a DC 15 lock) at the far
 * end of the east one. A
 * Clockwork Sentinel keeps the room.
 */
public class ClocktowerPiece extends CogsdeepPiece {
    public static final int HALF = 7;
    public static final int NEAR = -CommonsPiece.HALF;
    public static final int FAR = -26;
    public static final int TOP = 14;
    /** The gallery floor's top (you stand at this y). */
    public static final int GALLERY = 7;
    /** The clock face's hub, on the far wall. */
    static final int HUB_Y = 8;
    static final double FACE = 5.5D;

    public ClocktowerPiece(BlockPos center, Direction door, long seed, long burrow) {
        super(CogsdeepStructures.CLOCKTOWER, center, door, seed, burrow, -HALF, -1, FAR, HALF, TOP, NEAR);
    }

    public ClocktowerPiece(NbtCompound nbt) {
        super(CogsdeepStructures.CLOCKTOWER, nbt);
    }

    /** The strongbox on the gallery, in the world. */
    public BlockPos strongbox() {
        return this.at(HALF - 1, GALLERY, FAR + 1);
    }

    /** The clock's hub, in the world. */
    public BlockPos hub() {
        return this.at(0, HUB_Y, FAR);
    }

    @Override
    protected void build(Builder b) {
        b.room(-HALF, FAR, HALF, NEAR, TOP, Blocks.POLISHED_ANDESITE);
        b.doorway(0, NEAR, true, 3, 4);
        // A checker of andesite and deepslate tiles.
        for (int a = -HALF + 1; a < HALF; a++) {
            for (int c = FAR + 1; c < NEAR; c++) {
                if ((a + c) % 2 == 0) {
                    b.set(a, -1, c, Blocks.DEEPSLATE_TILES);
                }
            }
        }

        // The clock face, set into the far wall.
        for (int a = -6; a <= 6; a++) {
            for (int y = HUB_Y - 6; y <= HUB_Y + 6; y++) {
                double r = Math.sqrt(a * a + (y - HUB_Y) * (y - HUB_Y));
                if (r > FACE) {
                    continue;
                }
                b.set(a, y, FAR, r > FACE - 1.0D ? Blocks.CUT_COPPER : Blocks.CALCITE);
            }
        }
        // Hour marks: glowstone at twelve, three, six and nine.
        for (int quarter = 0; quarter < 4; quarter++) {
            double angle = quarter * Math.PI / 2.0D;
            int a = (int) Math.round(Math.sin(angle) * 4.0D);
            int y = HUB_Y + (int) Math.round(Math.cos(angle) * 4.0D);
            b.set(a, y, FAR, Blocks.GLOWSTONE);
        }
        // The hands: the minute hand to twelve, the hour hand to ten.
        hand(b, 0.0D, 3.2D);
        hand(b, -Math.PI / 3.0D, 2.2D);
        b.set(0, HUB_Y, FAR, Blocks.POLISHED_BLACKSTONE);
        b.frame(0, HUB_Y, FAR + 1, Direction.SOUTH, new ItemStack(Items.CLOCK));

        // The pendulum: a long chain with a copper bob, and a bell from the ceiling.
        b.fill(0, 3, FAR + 4, 0, TOP - 1, FAR + 4, Blocks.CHAIN);
        b.set(0, 2, FAR + 4, Blocks.CUT_COPPER);
        b.set(0, TOP - 1, NEAR - 6, Blocks.BELL.getDefaultState().with(BellBlock.ATTACHMENT, Attachment.CEILING)
                .with(BellBlock.FACING, Direction.SOUTH));

        // Grindstone gears up the side walls.
        for (int c = FAR + 3; c <= NEAR - 3; c += 4) {
            for (int wall : new int[]{-1, 1}) {
                Direction facingIn = wall < 0 ? Direction.EAST : Direction.WEST;
                int y = 2 + Math.floorMod(c, 3);
                b.set(wall * (HALF - 1), y, c, Blocks.GRINDSTONE.getDefaultState().with(GrindstoneBlock.FACE, WallMountLocation.WALL)
                        .with(GrindstoneBlock.FACING, facingIn));
            }
        }

        // The gallery: spruce slabs (top half) two wide along both side walls, railed with fences.
        for (int a = -HALF + 1; a < HALF; a++) {
            if (Math.abs(a) < HALF - 2) {
                continue;
            }
            for (int c = FAR + 1; c < NEAR; c++) {
                b.set(a, GALLERY - 1, c, slab(Blocks.SPRUCE_SLAB, true));
                if (Math.abs(a) == HALF - 2) {
                    b.set(a, GALLERY, c, Blocks.SPRUCE_FENCE);
                }
            }
        }
        // The ladder up to it on the east wall, through a hole in the gallery.
        int ladderC = NEAR - 4;
        for (int y = 0; y <= GALLERY; y++) {
            b.set(HALF - 1, y, ladderC, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.WEST));
        }

        // The strongbox at the gallery's far end, and lanterns on the gallery and on chains.
        b.container(HALF - 1, GALLERY, FAR + 1, facing(Blocks.CHEST, Direction.WEST), VAULT_LOOT);
        b.set(-(HALF - 2), GALLERY, FAR + 1, Blocks.LANTERN);
        b.set(HALF - 1, GALLERY, NEAR - 2, Blocks.LANTERN);
        b.set(-(HALF - 1), GALLERY, NEAR - 2, Blocks.LANTERN);
        b.hangingLantern(-3, TOP - 1, FAR + 8, 4);
        b.hangingLantern(3, TOP - 1, FAR + 8, 4);
        b.hangingLantern(0, TOP - 1, NEAR - 3, 5);

        b.sentinel(0, 0, NEAR - 6, 8);
    }

    /** A clock hand from the hub at {@code angle} (radians clockwise from twelve), {@code length} long. */
    private static void hand(Builder b, double angle, double length) {
        for (double t = 1.0D; t <= length; t += 0.5D) {
            int a = (int) Math.round(Math.sin(angle) * t);
            int y = HUB_Y + (int) Math.round(Math.cos(angle) * t);
            b.set(a, y, FAR, Blocks.POLISHED_BLACKSTONE);
        }
    }
}
