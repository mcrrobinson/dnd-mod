package mattonfire.dnd.world.gen.dungeon;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.ComposterBlock;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.util.math.Direction;

/**
 * The Goblin Warren's props: hide lean-tos, straw bedrolls, cages, refuse pits, skull totems,
 * hanging chains and fire pits. Like {@link CryptDressing}, rooms call these after building (only
 * for {@link DungeonTheme#GOBLIN_WARREN}) with spots they know are clear of their doors, chests,
 * spawn points and traps; props go only into air ({@link DungeonPiece.Builder#setIfAir}), a refuse
 * pit only replaces plain warren floor with open air over it, and every draw comes from the
 * builder's separate {@code dressing} random, so dressing never moves a room's chests, traps or puzzle.
 */
final class WarrenDressing {
    /** The hides of the goblin camp's tents ({@code GoblinCampPiece}). */
    private static final Block[] HIDES = {Blocks.BROWN_WOOL, Blocks.GREEN_WOOL, Blocks.GRAY_WOOL, Blocks.LIME_WOOL, Blocks.BLACK_WOOL};

    private WarrenDressing() {
    }

    static boolean applies(DungeonPiece piece) {
        return piece.theme() == DungeonTheme.GOBLIN_WARREN;
    }

    private static BlockState hide(DungeonPiece.Builder b) {
        return HIDES[b.dressing.nextInt(HIDES.length)].getDefaultState();
    }

    /** A straw bedroll: a hay bale, sometimes with a brown rug beside it towards {@code side}. */
    static void bedroll(DungeonPiece.Builder b, int x, int z, Direction side) {
        if (b.setIfAir(x, 1, z, Blocks.HAY_BLOCK.getDefaultState()) && b.dressing.nextFloat() < 0.5F) {
            b.setIfAir(x + side.getOffsetX(), 1, z + side.getOffsetZ(), Blocks.BROWN_CARPET.getDefaultState());
        }
    }

    /** A skull on a spruce stake two fences high: the goblins' trophy totem. */
    static void totem(DungeonPiece.Builder b, int x, int z) {
        if (!b.setIfAir(x, 1, z, Blocks.SPRUCE_FENCE.getDefaultState())) {
            return;
        }
        b.setIfAir(x, 2, z, Blocks.SPRUCE_FENCE.getDefaultState());
        Block skull = b.dressing.nextFloat() < 0.2F ? Blocks.ZOMBIE_HEAD : Blocks.SKELETON_SKULL;
        b.setIfAir(x, 3, z, skull.getDefaultState().with(SkullBlock.ROTATION, b.dressing.nextInt(16)));
    }

    /** A crate of stores (an empty barrel), now and then two stacked, or a composter of slops. */
    static void stores(DungeonPiece.Builder b, int x, int z) {
        float r = b.dressing.nextFloat();
        if (r < 0.25F) {
            b.setIfAir(x, 1, z, Blocks.COMPOSTER.getDefaultState().with(ComposterBlock.LEVEL, 1 + b.dressing.nextInt(7)));
            return;
        }
        if (b.setIfAir(x, 1, z, Blocks.BARREL.getDefaultState()) && r > 0.7F) {
            b.setIfAir(x, 2, z, Blocks.BARREL.getDefaultState());
        }
    }

    /** One corner prop: a bedroll, stores, a heap of gnawed bones or a totem. */
    static void cornerProp(DungeonPiece.Builder b, int x, int z, Direction side) {
        switch (b.dressing.nextInt(4)) {
            case 0 -> bedroll(b, x, z, side);
            case 1 -> stores(b, x, z);
            case 2 -> CryptDressing.bonePile(b, x, z);
            default -> totem(b, x, z);
        }
    }

    /** A chain hanging from the ceiling ({@code top}) down to the floor. */
    static void chain(DungeonPiece.Builder b, int x, int z, int top) {
        for (int y = top; y >= 1; y--) {
            if (!b.setIfAir(x, y, z, Blocks.CHAIN.getDefaultState())) {
                return;
            }
        }
    }

    /**
     * A refuse pit 2x2 from (x, z): plain floor dug into stinking mud. Only replaces warren floor
     * (not trim, traps or vents) with air over it, so it never blocks anything.
     */
    static void refusePit(DungeonPiece.Builder b, int x, int z) {
        for (int dx = 0; dx < 2; dx++) {
            for (int dz = 0; dz < 2; dz++) {
                BlockState floor = b.get(x + dx, 0, z + dz);
                BlockState above = b.get(x + dx, 1, z + dz);
                if (floor != null && above != null && above.isAir() && isWarrenFloor(floor)) {
                    b.set(x + dx, 0, z + dz, b.dressing.nextFloat() < 0.8F ? Blocks.MUD.getDefaultState()
                            : Blocks.SOUL_SOIL.getDefaultState());
                }
            }
        }
    }

    private static boolean isWarrenFloor(BlockState state) {
        return state.isOf(Blocks.COARSE_DIRT) || state.isOf(Blocks.PACKED_MUD) || state.isOf(Blocks.ROOTED_DIRT)
                || state.isOf(Blocks.COBBLESTONE) || state.isOf(Blocks.DIRT);
    }

    /** A fire pit: a lit campfire on a cobblestone hearth. */
    static void firePit(DungeonPiece.Builder b, int x, int z) {
        if (b.setIfAir(x, 1, z, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, true))) {
            b.set(x, 0, z, Blocks.COBBLESTONE.getDefaultState());
        }
    }

    /**
     * A hide lean-to against a wall: two cells along it and two deep. The back row (by the wall) has a
     * straw bed and a rug under hides three blocks up; the front row has a stake at one end, an open
     * end to crawl in at the other, and hides two blocks up.
     */
    static void leanTo(DungeonPiece.Builder b, int x, int z, Direction along, Direction inward) {
        BlockState roof = hide(b);
        for (int i = 0; i < 2; i++) {
            int bx = x + along.getOffsetX() * i;
            int bz = z + along.getOffsetZ() * i;
            int fx = bx + inward.getOffsetX();
            int fz = bz + inward.getOffsetZ();
            BlockState patch = b.dressing.nextFloat() < 0.15F ? hide(b) : roof;
            b.setIfAir(bx, 1, bz, i == 0 ? Blocks.HAY_BLOCK.getDefaultState() : Blocks.BROWN_CARPET.getDefaultState());
            b.setIfAir(bx, 3, bz, patch);
            if (i == 0) {
                b.setIfAir(fx, 1, fz, Blocks.SPRUCE_FENCE.getDefaultState());
            }
            b.setIfAir(fx, 2, fz, patch);
        }
    }

    /**
     * A cage 3x3 from (x, z): iron bars round a cell with a skull and bones in it, under a spruce slab
     * roof. A captive the goblins didn't keep alive.
     */
    static void cage(DungeonPiece.Builder b, int x, int z) {
        BlockState bars = Blocks.IRON_BARS.getDefaultState();
        BlockState roof = Blocks.SPRUCE_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.BOTTOM);
        for (int dx = 0; dx < 3; dx++) {
            for (int dz = 0; dz < 3; dz++) {
                boolean middle = dx == 1 && dz == 1;
                if (!middle) {
                    b.setIfAir(x + dx, 1, z + dz, bars);
                    b.setIfAir(x + dx, 2, z + dz, bars);
                }
                b.setIfAir(x + dx, 3, z + dz, roof);
            }
        }
        if (b.dressing.nextBoolean()) {
            CryptDressing.skull(b, x + 1, 1, z + 1);
        } else {
            b.setIfAir(x + 1, 1, z + 1, Blocks.BONE_BLOCK.getDefaultState());
        }
    }

    /**
     * Along each wall, either side of its middle (where doors are), at {@code from} blocks from each
     * end: a lean-to or (with {@code cages}) sometimes a cage, each with {@code chance}.
     */
    static void wallProps(DungeonPiece.Builder b, int width, int depth, int from, float chance, boolean cages) {
        for (Direction wall : Direction.Type.HORIZONTAL) {
            boolean alongX = wall.getAxis() == Direction.Axis.Z;
            int length = alongX ? width : depth;
            int fixed = switch (wall) {
                case NORTH, WEST -> 2;
                case SOUTH -> depth - 3;
                default -> width - 3;
            };
            Direction inward = wall.getOpposite();
            Direction along = alongX ? Direction.EAST : Direction.SOUTH;
            for (int start : new int[]{from, length - 2 - from}) {
                if (b.dressing.nextFloat() >= chance) {
                    continue;
                }
                int x = alongX ? start : fixed;
                int z = alongX ? fixed : start;
                if (cages && b.dressing.nextFloat() < 0.5F) {
                    // The cage's 3x3 from its wall-side, low corner.
                    int cx = alongX ? start : (wall == Direction.WEST ? fixed : fixed - 2);
                    int cz = alongX ? (wall == Direction.NORTH ? fixed : fixed - 2) : start;
                    cage(b, cx, cz);
                } else {
                    leanTo(b, x, z, along, inward);
                }
            }
        }
    }
}
