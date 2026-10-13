package mattonfire.dnd.world.gen.burrow;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.LightningRodBlock;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The observatory on the surface, Cogsdeep's front door: a round tower of stone bricks {@link #RADIUS} blocks
 * across the middle, {@link #WALL_HEIGHT} high with glass windows, under a hemispherical dome of cut copper that
 * weathers green over time, slit open to the north for the telescope (a lightning rod tube on a fence tripod with
 * an end rod eyepiece) and topped with a lightning rod. Inside: a spruce floor, the stairwell in the middle
 * ({@link ShaftPiece}), a cartography desk with a spyglass, a lectern, bookshelves and a chest. Trees and
 * plants round it are cleared; the floor sits on a foundation down to the ground.
 *
 * <p>y = 0 is where you stand on the floor (the ground's first air in the middle).
 */
public class ObservatoryPiece extends CogsdeepPiece {
    public static final double RADIUS = 6.5D;
    static final int WALL_HEIGHT = 5;
    /** The dome's sphere: centre height and radius. */
    static final int DOME_BASE = WALL_HEIGHT;
    static final double DOME = 6.5D;
    /** Trees and plants are cleared out to this radius, this high. */
    static final int CLEAR_RADIUS = 9;
    static final int CLEAR_HEIGHT = 16;
    /** The telescope's tripod, at a/b. */
    static final int SCOPE_B = -5;

    public ObservatoryPiece(BlockPos center, Direction door, long seed, long burrow) {
        super(CogsdeepStructures.OBSERVATORY, center, door, seed, burrow, -CLEAR_RADIUS, -8, -CLEAR_RADIUS - 4,
                CLEAR_RADIUS, CLEAR_HEIGHT, CLEAR_RADIUS + 4);
    }

    public ObservatoryPiece(NbtCompound nbt) {
        super(CogsdeepStructures.OBSERVATORY, nbt);
    }

    /** The telescope's eyepiece, in the world. */
    public BlockPos eyepiece() {
        return this.at(0, 2, SCOPE_B + 1);
    }

    /** Just outside the door, in the world. */
    public BlockPos outside() {
        return this.at(0, 0, 9);
    }

    private static double r(int a, int b) {
        return Math.sqrt(a * a + b * b);
    }

    @Override
    protected void build(Builder b) {
        // Clear trees and plants round the tower (never the ground itself), and everything inside it.
        for (int a = -CLEAR_RADIUS; a <= CLEAR_RADIUS; a++) {
            for (int c = -CLEAR_RADIUS; c <= CLEAR_RADIUS; c++) {
                double r = r(a, c);
                if (r > CLEAR_RADIUS + 0.5D) {
                    continue;
                }
                for (int y = 0; y <= CLEAR_HEIGHT; y++) {
                    if (!b.inChunk(a, y, c)) {
                        continue;
                    }
                    BlockState state = b.get(a, y, c);
                    if (r <= RADIUS || state.isIn(BlockTags.LOGS) || state.isIn(BlockTags.LEAVES) || state.isReplaceable()
                            && state.getFluidState().isEmpty() && !state.isAir()) {
                        b.set(a, y, c, Blocks.AIR);
                    }
                }
            }
        }

        for (int a = -7; a <= 7; a++) {
            for (int c = -7; c <= 7; c++) {
                double r = r(a, c);
                if (r > RADIUS) {
                    continue;
                }
                boolean wall = r > RADIUS - 1.0D;
                // Floor (and a foundation down to the ground).
                b.set(a, -1, c, wall ? Blocks.POLISHED_ANDESITE : r > 4.4D && r <= 5.4D ? Blocks.CUT_COPPER : Blocks.SPRUCE_PLANKS);
                b.foundation(a, -1, c, Blocks.STONE_BRICKS.getDefaultState(), 8);
                if (wall) {
                    for (int y = 0; y < WALL_HEIGHT; y++) {
                        b.set(a, y, c, y == 0 ? Blocks.POLISHED_ANDESITE : masonry(b));
                    }
                }
            }
        }
        // Windows every 45 degrees, but not by the door or the telescope.
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0D + Math.PI / 8.0D;
            int a = (int) Math.round(Math.sin(angle) * 6.0D);
            int c = (int) Math.round(Math.cos(angle) * 6.0D);
            b.set(a, 2, c, Blocks.GLASS_PANE);
            b.set(a, 3, c, Blocks.GLASS_PANE);
        }

        // The dome: a shell of cut copper on the walls, slit open to the north.
        for (int a = -7; a <= 7; a++) {
            for (int c = -7; c <= 7; c++) {
                for (int y = DOME_BASE; y <= DOME_BASE + 7; y++) {
                    double d = Math.sqrt(a * a + c * c + (y - DOME_BASE) * (y - DOME_BASE));
                    if (d > DOME) {
                        continue;
                    }
                    boolean slit = a == 0 && c < 0 && y > DOME_BASE + 1;
                    if (d > DOME - 1.15D && !slit) {
                        b.set(a, y, c, cutCopper(b));
                    } else {
                        b.set(a, y, c, Blocks.AIR);
                    }
                }
            }
        }
        b.set(0, DOME_BASE + 7, 0, Blocks.LIGHTNING_ROD.getDefaultState().with(LightningRodBlock.FACING, Direction.UP));
        b.hangingLantern(0, DOME_BASE + 5, 0, 2);

        // The door, facing out to the south, and a dirt path from it.
        b.set(0, 0, 6, door(DoubleBlockHalf.LOWER));
        b.set(0, 1, 6, door(DoubleBlockHalf.UPPER));
        // On a slope the ground before the door can stand higher than the floor: cut a way through it, and fill
        // a gap under the first steps out.
        for (int c = 7; c <= 12; c++) {
            for (int a = -1; a <= 1; a++) {
                float r = b.random.nextFloat();
                if (!b.inChunk(a, -1, c)) {
                    continue;
                }
                for (int y = 0; y <= 3; y++) {
                    if (!b.get(a, y, c).isAir()) {
                        b.set(a, y, c, Blocks.AIR);
                    }
                }
                if (c <= 8) {
                    b.foundation(a, -1, c, Blocks.COBBLESTONE.getDefaultState(), 3);
                }
                BlockState ground = b.get(a, -1, c);
                if (ground.isIn(BlockTags.DIRT) || ground.isAir() && c <= 8) {
                    b.set(a, -1, c, a == 0 || r < 0.6F ? Blocks.DIRT_PATH : Blocks.COARSE_DIRT);
                }
            }
        }

        // Lanterns on stone posts either side of the door.
        b.set(-1, 2, 7, Blocks.LANTERN);
        b.set(1, 2, 7, Blocks.LANTERN);
        b.set(-1, 1, 7, Blocks.STONE_BRICK_WALL);
        b.set(1, 1, 7, Blocks.STONE_BRICK_WALL);
        b.set(-1, 0, 7, Blocks.STONE_BRICK_WALL);
        b.set(1, 0, 7, Blocks.STONE_BRICK_WALL);

        // The telescope: a copper tube on a fence tripod, aimed out of the slit, with an end rod eyepiece.
        b.set(0, 0, SCOPE_B, Blocks.SPRUCE_FENCE);
        b.set(0, 1, SCOPE_B, Blocks.SPRUCE_FENCE);
        b.set(0, 2, SCOPE_B, Blocks.LIGHTNING_ROD.getDefaultState().with(LightningRodBlock.FACING, Direction.NORTH));
        b.set(0, 2, SCOPE_B + 1, facing(Blocks.END_ROD, Direction.SOUTH));
        b.set(-1, 0, SCOPE_B, stairs(Blocks.SPRUCE_STAIRS, Direction.EAST, false));

        // A cartography desk with a spyglass lying on it, a lectern, bookshelves and the chest.
        b.set(4, 0, -3, Blocks.CARTOGRAPHY_TABLE);
        b.frame(4, 1, -3, Direction.UP, new ItemStack(Items.SPYGLASS));
        b.set(3, 0, -4, stairs(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
        b.set(-4, 0, -3, facing(Blocks.LECTERN, Direction.EAST));
        b.set(-5, 0, 1, Blocks.BOOKSHELF);
        b.set(-5, 1, 1, Blocks.BOOKSHELF);
        b.set(-5, 0, 2, Blocks.BOOKSHELF);
        b.set(-5, 1, 2, Blocks.BOOKSHELF);
        b.set(-5, 2, 2, Blocks.LANTERN);
        b.container(5, 0, 1, facing(Blocks.CHEST, Direction.WEST), COMMONS_LOOT);
        b.set(5, 0, 2, Blocks.BARREL);
        b.set(5, 1, 2, Blocks.LANTERN);
    }

    private static BlockState door(DoubleBlockHalf half) {
        return Blocks.SPRUCE_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.NORTH)
                .with(DoorBlock.HALF, half).with(DoorBlock.HINGE, DoorHinge.LEFT);
    }
}
