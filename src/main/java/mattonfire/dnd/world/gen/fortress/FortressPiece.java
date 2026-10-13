package mattonfire.dnd.world.gen.fortress;

import static mattonfire.dnd.world.gen.PieceBlocks.*;

import java.util.function.Consumer;
import mattonfire.dnd.classes.Obstacles.ObstaclePlacer;
import mattonfire.dnd.classes.Obstacles.ObstacleType;
import mattonfire.dnd.classes.Obstacles.Tier;
import mattonfire.dnd.entity.ModEntityTypes;
import mattonfire.dnd.entity.MountainDwarfEntity;
import mattonfire.dnd.world.gen.FacingPiece;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CandleBlock;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;

/**
 * Base for the dwarven fortress pieces. Like the hobbit pieces, they're designed in local
 * coordinates: x runs across the front, z runs from the front edge (z = 0, where you come in) to
 * the back, and y = 0 is the floor. Block states are written as if the piece faced north (so
 * "back towards the entrance" is {@link Direction#SOUTH}) and get rotated to its real facing.
 *
 * The pieces are carved out of the mountain: each one fills its whole box, walls and all, and
 * props its floor up wherever the rock falls away beneath it.
 *
 * A piece is built once per chunk it overlaps, so all of its randomness comes from its own seed
 * ({@link mattonfire.dnd.world.gen.ModPiece}).
 */
public abstract class FortressPiece extends FacingPiece<FortressPiece.Builder> {
    /** Chance that a chest in a fortress is really a mimic. */
    private static final float MIMIC_CHANCE = 0.1F;

    protected static final Direction OUT = Direction.SOUTH;
    protected static final Direction IN = Direction.NORTH;

    protected static final Identifier TREASURY = loot("dwarven_fortress_treasury");
    protected static final Identifier FORGE = loot("dwarven_fortress_forge");
    protected static final Identifier BARRACKS = loot("dwarven_fortress_barracks");
    protected static final Identifier BREWHALL = loot("dwarven_fortress_brewhall");
    protected static final Identifier MINE = loot("dwarven_fortress_mine");
    protected static final Identifier REAGENTS = loot("dwarven_fortress_reagents");

    /** How far down a floor gets propped up over caves and cliffs. */
    private static final int MAX_FOUNDATION = 24;

    protected FortressPiece(StructurePieceType type, BlockBox box, Direction facing, long seed) {
        super(type, box, facing, seed);
    }

    protected FortressPiece(StructurePieceType type, NbtCompound nbt) {
        super(type, nbt);
    }

    /** Where the next piece back starts: the floor block just behind the middle of the back wall. */
    public BlockPos backCenter() {
        return this.worldPos(this.width() / 2, 0, this.depth());
    }

    /** The real direction of a step from local (x, z) to (x + dx, z + dz). */
    public Direction worldDirection(int dx, int dz) {
        BlockPos a = this.worldPos(0, 0, 0);
        BlockPos b = this.worldPos(dx, 0, dz);
        return Direction.fromVector(b.getX() - a.getX(), 0, b.getZ() - a.getZ());
    }

    @Override
    protected Builder builder(StructureWorldAccess world, BlockBox chunkBox, Random random) {
        return new Builder(world, chunkBox, random);
    }

    /** The fortress stonework: mostly deepslate bricks, a few cracked or tiled for character. */
    protected static Block masonry(Builder b) {
        float r = b.random.nextFloat();
        return r < 0.08F ? Blocks.CRACKED_DEEPSLATE_BRICKS : r < 0.16F ? Blocks.DEEPSLATE_TILES : Blocks.DEEPSLATE_BRICKS;
    }

    /**
     * Places blocks, containers and entities for one chunk's slice of a piece. Everything takes
     * local coordinates; anything outside the chunk is skipped, but random draws happen either way.
     */
    protected class Builder extends FacingPiece<Builder>.Builder {
        Builder(StructureWorldAccess world, BlockBox chunkBox, Random random) {
            super(world, chunkBox, random);
        }

        /**
         * The usual room: the whole box walled in fortress masonry, a floor at y = 0 propped up
         * from below, and the inside hollowed out.
         */
        void shell(int width, int height, int depth, BlockState floor) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < depth; z++) {
                    this.foundation(x, z);
                    boolean edge = x == 0 || z == 0 || x == width - 1 || z == depth - 1;
                    this.set(x, 0, z, edge ? Blocks.POLISHED_DEEPSLATE.getDefaultState() : floor);
                    for (int y = 1; y < height; y++) {
                        if (edge || y == height - 1) {
                            this.set(x, y, z, y == 1 && edge ? Blocks.POLISHED_BLACKSTONE_BRICKS : masonry(this));
                        } else {
                            this.set(x, y, z, Blocks.AIR);
                        }
                    }
                }
            }
        }

        /** Props the floor up with cobbled deepslate wherever there's air or liquid under it. */
        void foundation(int x, int z) {
            this.propUp(x, 0, z, Blocks.COBBLED_DEEPSLATE.getDefaultState(), MAX_FOUNDATION, false);
        }

        /** A doorway through a wall at z, centred on x, in a basalt and gilded blackstone frame. */
        void doorway(int cx, int z, int width, int height) {
            int x1 = cx - width / 2;
            int x2 = x1 + width - 1;
            this.air(x1, 1, z, x2, height, z);
            this.fill(x1 - 1, 1, z, x1 - 1, height, z, pillar(Blocks.POLISHED_BASALT, Direction.Axis.Y));
            this.fill(x2 + 1, 1, z, x2 + 1, height, z, pillar(Blocks.POLISHED_BASALT, Direction.Axis.Y));
            this.fill(x1 - 1, height + 1, z, x2 + 1, height + 1, z, Blocks.GILDED_BLACKSTONE);
            if (width >= 3) {
                this.set(x1, height, z, stairs(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.EAST, true));
                this.set(x2, height, z, stairs(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.WEST, true));
            }
        }

        /**
         * Fills the local box (x1, y1, z1) to (x2, y2, z2) with one sealed obstacle group. Only the blocks in this
         * chunk are written ({@link ObstaclePlacer}); the group seed is drawn either way, so every chunk agrees.
         */
        void obstacle(int x1, int y1, int z1, int x2, int y2, int z2, ObstacleType type, Tier tier, boolean critical) {
            long groupSeed = this.random.nextLong();
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
                    for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                        ObstaclePlacer.place(this.world, this.chunkBox, this.pos(x, y, z), type, tier, critical, groupSeed);
                    }
                }
            }
        }

        /** A pillar of polished deepslate on a chiselled base with a gold capital. */
        void column(int x, int z, int top) {
            this.set(x, 1, z, Blocks.CHISELED_DEEPSLATE);
            this.set(x, 2, z, Blocks.GILDED_BLACKSTONE);
            this.fill(x, 3, z, x, top - 2, z, pillar(Blocks.POLISHED_BASALT, Direction.Axis.Y));
            this.set(x, top - 1, z, Blocks.GOLD_BLOCK);
            this.set(x, top, z, Blocks.CHISELED_DEEPSLATE);
        }

        /**
         * A chandelier hanging from the ceiling at y = top: a chain, a gold boss and four arms
         * with candles on top and lanterns beneath.
         */
        void chandelier(int x, int top, int z, int drop) {
            for (int y = top - 1; y > top - drop; y--) {
                this.set(x, y, z, Blocks.CHAIN);
            }
            int y = top - drop;
            this.set(x, y, z, Blocks.GOLD_BLOCK);
            this.set(x, y - 1, z, lantern(true));
            for (Direction d : Direction.Type.HORIZONTAL) {
                int ax = x + d.getOffsetX();
                int az = z + d.getOffsetZ();
                this.set(ax, y, az, bottomSlab(Blocks.POLISHED_BLACKSTONE_SLAB));
                this.set(ax, y + 1, az, this.candleState());
                int ox = x + d.getOffsetX() * 2;
                int oz = z + d.getOffsetZ() * 2;
                this.set(ox, y, oz, bottomSlab(Blocks.POLISHED_BLACKSTONE_SLAB));
                this.set(ox, y - 1, oz, lantern(true));
            }
        }

        BlockState candleState() {
            return Blocks.YELLOW_CANDLE.getDefaultState().with(CandleBlock.CANDLES, 1 + this.random.nextInt(3))
                    .with(CandleBlock.LIT, true);
        }

        /** A brazier: a lit campfire on a gilded plinth. */
        void brazier(int x, int y, int z) {
            this.set(x, y, z, Blocks.GILDED_BLACKSTONE);
            this.set(x, y + 1, z, Blocks.CAMPFIRE);
        }

        @Override
        public void chest(int x, int y, int z, Direction facing, Identifier lootTable) {
            if (this.chance(MIMIC_CHANCE)) {
                this.mimic(x, y, z, facing, lootTable);
                return;
            }
            this.container(x, y, z, facing(Blocks.CHEST, facing), lootTable);
        }

        /** A chest minecart on the rail at x/y/z, filled from a loot table. */
        void chestCart(int x, int y, int z, Identifier lootTable) {
            long lootSeed = this.random.nextLong();
            BlockPos pos = this.pos(x, y, z);
            if (!this.chunkBox.contains(pos)) {
                return;
            }
            ChestMinecartEntity cart = new ChestMinecartEntity(this.world.toServerWorld(), pos.getX() + 0.5D, pos.getY() + 0.0625D, pos.getZ() + 0.5D);
            cart.setLootTable(lootTable, lootSeed);
            this.world.spawnEntity(cart);
        }

        void dwarf(int x, int y, int z) {
            this.dwarf(x, y, z, dwarf -> {
            });
        }

        /**
         * A dwarf living here. {@code setup} runs before it's added to the world: worldgen saves
         * entities straight away, so later changes would be lost.
         */
        void dwarf(int x, int y, int z, Consumer<MountainDwarfEntity> setup) {
            BlockPos home = this.pos(x, y, z);
            this.npc(x, y, z, ModEntityTypes.MOUNTAIN_DWARF, dwarf -> {
                dwarf.setHome(home);
                setup.accept(dwarf);
            });
        }
    }
}
