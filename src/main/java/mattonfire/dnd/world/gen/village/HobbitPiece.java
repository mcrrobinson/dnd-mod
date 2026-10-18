package mattonfire.dnd.world.gen.village;

import static mattonfire.dnd.world.gen.PieceBlocks.*;

import java.util.function.Consumer;
import mattonfire.dnd.classes.Blocks.hobbit.HobbitBlocks;
import mattonfire.dnd.classes.Registry.BlockFamily;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.entity.HobbitEntity;
import mattonfire.dnd.entity.ModEntityTypes;
import mattonfire.dnd.world.gen.FacingPiece;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CandleBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;

/**
 * Base for the hobbit village pieces. Pieces are designed in local coordinates: x runs across the
 * front, z runs from the front edge (z = 0, where the entrance is) to the back, and y = 0 is the
 * ground layer. Block states are written as if the piece faced north, so "out of the front" is
 * {@link Direction#SOUTH}, and get rotated to the piece's real facing.
 *
 * A piece is built once per chunk it overlaps, so all of its randomness comes from its own seed
 * (never the per-chunk random), which keeps every chunk's slice of it in agreement.
 */
public abstract class HobbitPiece extends FacingPiece<HobbitPiece.Builder> {
    protected static final Direction OUT = Direction.SOUTH;
    protected static final Direction IN = Direction.NORTH;

    protected static final Identifier PANTRY = loot("hobbit_pantry");
    protected static final Identifier LARDER = loot("hobbit_larder");
    protected static final Identifier HARVEST = loot("hobbit_harvest");
    protected static final Identifier ALE = loot("hobbit_ale");

    protected HobbitPiece(StructurePieceType type, BlockBox box, Direction facing, long seed) {
        super(type, box, facing, seed);
    }

    protected HobbitPiece(StructurePieceType type, NbtCompound nbt) {
        super(type, nbt);
    }

    /**
     * The ground layer (local y = 0) sits one below the box: terrain adaptation levels the land
     * so its top block is just under a piece's box, which is then exactly our ground layer.
     */
    @Override
    protected int applyYTransform(int y) {
        return super.applyYTransform(y) - 1;
    }

    /** A box of the given local size (width across the front, depth front to back), centred on x/z. */
    protected static BlockBox centeredBox(int x, int y, int z, Direction facing, int width, int height, int depth) {
        int sizeX = facing.getAxis() == Direction.Axis.Z ? width : depth;
        int sizeZ = facing.getAxis() == Direction.Axis.Z ? depth : width;
        int minX = x - sizeX / 2;
        int minZ = z - sizeZ / 2;
        return new BlockBox(minX, y, minZ, minX + sizeX - 1, y + height - 1, minZ + sizeZ - 1);
    }

    /** Local x of the entrance on the front edge; paths lead to the block just in front of it. */
    protected int entranceX() {
        return this.width() / 2;
    }

    public BlockPos entrance() {
        int x = this.entranceX();
        return new BlockPos(this.applyXTransform(x, -1), this.applyYTransform(0), this.applyZTransform(x, -1));
    }

    /**
     * Thatch for local x/y/z: plain, or Mossy Thatch for about one block in five. Hashed from the position and
     * the piece's seed rather than drawn from its random, so it's the same in every chunk and leaves the rest of
     * the piece's draws alone.
     */
    protected BlockFamily thatch(int x, int y, int z) {
        long h = (MathHelper.hashCode(x, y, z) ^ this.seed) * 0x9E3779B97F4A7C15L;
        return Math.floorMod(h >>> 33, 5L) == 0 ? ModBlockFamilies.MOSSY_THATCH : ModBlockFamilies.THATCH;
    }

    @Override
    protected Builder builder(StructureWorldAccess world, BlockBox chunkBox, Random random) {
        return new Builder(world, chunkBox, random);
    }

    protected static BlockState ripe(Block crop) {
        return ((CropBlock) crop).withAge(((CropBlock) crop).getMaxAge());
    }

    private static final Block[] FLOWERS = {
            Blocks.POPPY, Blocks.DANDELION, Blocks.CORNFLOWER, Blocks.ALLIUM, Blocks.OXEYE_DAISY, Blocks.AZURE_BLUET,
            Blocks.RED_TULIP, Blocks.ORANGE_TULIP, Blocks.PINK_TULIP, Blocks.WHITE_TULIP, Blocks.LILY_OF_THE_VALLEY
    };
    private static final Block[] TALL_FLOWERS = {Blocks.ROSE_BUSH, Blocks.PEONY, Blocks.LILAC, Blocks.SUNFLOWER};
    private static final Block[] POTTED = {
            Blocks.POTTED_RED_TULIP, Blocks.POTTED_FERN, Blocks.POTTED_FLOWERING_AZALEA_BUSH, Blocks.POTTED_AZALEA_BUSH,
            Blocks.POTTED_POPPY, Blocks.POTTED_CORNFLOWER, Blocks.POTTED_LILY_OF_THE_VALLEY, Blocks.POTTED_DANDELION
    };
    private static final Block[] CANDLES = {Blocks.CANDLE, Blocks.WHITE_CANDLE, Blocks.YELLOW_CANDLE, Blocks.ORANGE_CANDLE};

    /**
     * Places blocks, containers and entities for one chunk's slice of a piece. Everything takes
     * local coordinates; anything outside the chunk is skipped, but random draws happen either way.
     */
    protected class Builder extends FacingPiece<Builder>.Builder {
        Builder(StructureWorldAccess world, BlockBox chunkBox, Random random) {
            super(world, chunkBox, random);
        }

        /** The village never marked fences or chains for post-processing; kept so villages generate as before. */
        @Override
        protected boolean joinsNeighbours(Block block) {
            return block instanceof PaneBlock || block instanceof StairsBlock || block instanceof WallBlock;
        }

        /** The block at local x/y/z, or air outside this chunk. */
        @Override
        public BlockState get(int x, int y, int z) {
            return HobbitPiece.this.getBlockAt(this.world, x, y, z, this.chunkBox);
        }

        /** Sets the ground block at y = 0 and fills any gap beneath it with dirt. */
        void ground(int x, int z, BlockState top) {
            this.set(x, 0, z, top);
            HobbitPiece.this.fillDownwards(this.world, Blocks.DIRT.getDefaultState(), x, -1, z, this.chunkBox);
        }

        void ground(int x, int z, Block top) {
            this.ground(x, z, top.getDefaultState());
        }

        /** Grass and dirt ground over a whole rectangle, with the air above it cleared. */
        void lawn(int x1, int z1, int x2, int z2, int clearHeight) {
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    this.ground(x, z, Blocks.GRASS_BLOCK);
                    this.air(x, 1, z, x, clearHeight, z);
                }
            }
        }

        void flower(int x, int y, int z) {
            if (this.chance(0.15F)) {
                this.tall(x, y, z, this.pick(TALL_FLOWERS));
            } else {
                this.set(x, y, z, this.pick(FLOWERS));
            }
        }

        void tall(int x, int y, int z, Block block) {
            this.set(x, y, z, block.getDefaultState().with(TallPlantBlock.HALF, DoubleBlockHalf.LOWER));
            this.set(x, y + 1, z, block.getDefaultState().with(TallPlantBlock.HALF, DoubleBlockHalf.UPPER));
        }

        void pot(int x, int y, int z) {
            this.set(x, y, z, this.pick(POTTED));
        }

        void candles(int x, int y, int z) {
            this.set(x, y, z, this.pick(CANDLES).getDefaultState()
                    .with(CandleBlock.CANDLES, 1 + this.random.nextInt(3)).with(CandleBlock.LIT, true));
        }

        /**
         * A round window centred on x/y/z: a plus of glass with its corners rounded off by stairs.
         * {@code alongX} for a wall running along x (constant z), otherwise along z.
         */
        void roundWindow(int x, int y, int z, boolean alongX) {
            int dx = alongX ? 1 : 0;
            int dz = alongX ? 0 : 1;
            Direction low = alongX ? Direction.WEST : Direction.SOUTH;
            Direction high = alongX ? Direction.EAST : Direction.NORTH;
            this.set(x, y, z, Blocks.GLASS_PANE);
            this.set(x - dx, y, z - dz, Blocks.GLASS_PANE);
            this.set(x + dx, y, z + dz, Blocks.GLASS_PANE);
            this.set(x, y - 1, z, Blocks.GLASS_PANE);
            this.set(x, y + 1, z, Blocks.GLASS_PANE);
            this.set(x - dx, y + 1, z - dz, stairs(Blocks.SPRUCE_STAIRS, low, true));
            this.set(x + dx, y + 1, z + dz, stairs(Blocks.SPRUCE_STAIRS, high, true));
            this.set(x - dx, y - 1, z - dz, stairs(Blocks.SPRUCE_STAIRS, low, false));
            this.set(x + dx, y - 1, z + dz, stairs(Blocks.SPRUCE_STAIRS, high, false));
        }

        /**
         * A round door: a coloured 3x3 disc with the real door in its middle, framed by a pale stone arch (Round
         * Door Frame Stone posts, stairs rounding the top corners, a keystone over the door).
         */
        void roundDoor(int x, int y, int z, Block door, Block planks) {
            BlockFamily frame = ModBlockFamilies.ROUND_DOOR_FRAME_STONE;
            this.fill(x - 1, y, z, x + 1, y + 2, z, planks);
            this.door(x, y, z, door, IN);
            this.fill(x - 2, y, z, x - 2, y + 2, z, frame.base());
            this.fill(x + 2, y, z, x + 2, y + 2, z, frame.base());
            this.set(x - 1, y + 3, z, frame.base());
            this.set(x, y + 3, z, ModBlockFamilies.ROUND_DOOR_KEYSTONE.base());
            this.set(x + 1, y + 3, z, frame.base());
            this.set(x - 2, y + 3, z, stairs(frame, Direction.WEST, true));
            this.set(x + 2, y + 3, z, stairs(frame, Direction.EAST, true));
            // A knob on the right-hand panel.
            this.set(x + 1, y + 1, z - 1, Blocks.POLISHED_BLACKSTONE_BUTTON.getDefaultState()
                    .with(net.minecraft.block.WallMountedBlock.FACING, OUT));
        }

        /** A Larder Shelf facing {@code facing}, filled from {@code lootTable} when first opened. */
        void larder(int x, int y, int z, Direction facing, Identifier lootTable) {
            this.container(x, y, z, facing(HobbitBlocks.LARDER_SHELF, facing), lootTable);
        }

        /** Hanging herbs or an onion string under the ceiling at y + 1. */
        void herbs(int x, int y, int z, boolean onions) {
            this.set(x, y, z, onions ? HobbitBlocks.ONION_STRING : HobbitBlocks.HANGING_HERBS);
        }

        void door(int x, int y, int z, Block door, Direction facing) {
            BlockState state = door.getDefaultState().with(DoorBlock.FACING, facing).with(DoorBlock.HINGE, DoorHinge.LEFT);
            this.set(x, y, z, state.with(DoorBlock.HALF, DoubleBlockHalf.LOWER));
            this.set(x, y + 1, z, state.with(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        }

        void gate(int x, int y, int z, Block gate, Direction facing) {
            this.set(x, y, z, gate.getDefaultState().with(FenceGateBlock.FACING, facing));
        }

        /** A plate of food on the table top below. */
        void plate(int x, int y, int z) {
            this.frame(x, y, z, Direction.UP, HobbitFood.plate(this.random));
        }

        /** Something tasty, or at least festive, for a table top. */
        void tableFood(int x, int y, int z) {
            int roll = this.random.nextInt(10);
            if (roll < 5) {
                this.plate(x, y, z);
            } else if (roll < 7) {
                this.set(x, y, z, Blocks.CAKE);
            } else if (roll < 8) {
                this.set(x, y, z, this.pick(new Block[]{Blocks.CANDLE_CAKE, Blocks.RED_CANDLE_CAKE, Blocks.YELLOW_CANDLE_CAKE})
                        .getDefaultState().with(CandleBlock.LIT, true));
            } else if (roll < 9) {
                this.candles(x, y, z);
            } else {
                this.pot(x, y, z);
            }
        }

        void hobbit(int x, int y, int z) {
            this.hobbit(x, y, z, ModEntityTypes.HOBBIT);
        }

        void hobbit(int x, int y, int z, EntityType<? extends HobbitEntity> type) {
            this.hobbit(x, y, z, type, hobbit -> {
            });
        }

        /** Places a hobbit and lets {@code setup} change it before it's added (e.g. the village elder). */
        void hobbit(int x, int y, int z, EntityType<? extends HobbitEntity> type, Consumer<HobbitEntity> setup) {
            BlockPos home = this.pos(x, y, z);
            this.npc(x, y, z, type, hobbit -> {
                hobbit.setHome(home);
                setup.accept(hobbit);
            });
        }
    }
}
