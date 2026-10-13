package mattonfire.dnd.world.gen.burrow;

import static mattonfire.dnd.world.gen.PieceBlocks.*;

import mattonfire.dnd.entity.ModEntityTypes;
import mattonfire.dnd.world.gen.OrientedPiece;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;

/**
 * Base for the Cogsdeep pieces, laid out like the Brimstone Sanctum's: local {@code a} runs across (east when
 * the observatory door faces south), local {@code b} runs from the back towards the door, and {@code (0, 0)} is
 * the axis of the spiral stair. Each piece has its own reference height ({@link #center}'s y): the burrow's
 * floor for the rooms down below (y = 0 is where you stand, the floor blocks are at y = -1), the ground for the
 * observatory. Block states are written as if the door faced south and turned with {@link BlockState#rotate}.
 *
 * <p>A piece is built once per chunk it overlaps, so all of its randomness comes from its own seed, and every
 * draw happens whether or not its block lands in the chunk. Each piece carries its burrow's key (the start
 * chunk, {@link ChunkPos#toLong}).
 */
public abstract class CogsdeepPiece extends OrientedPiece<CogsdeepPiece.Builder> {
    /** The chests and barrels of the two workshops. Not locked. */
    public static final Identifier WORKSHOP_LOOT = loot("cogsdeep_workshop");
    /** The gem-polishing room's chest (DC 10). */
    public static final Identifier GEM_LOOT = loot("cogsdeep_gems");
    /** The strongbox on the clocktower gallery (DC 15). */
    public static final Identifier VAULT_LOOT = loot("cogsdeep_vault");
    /** The chest in the Commons and the observatory's. Not locked. */
    public static final Identifier COMMONS_LOOT = loot("cogsdeep_commons");

    protected final long burrow;

    protected CogsdeepPiece(StructurePieceType type, BlockPos center, Direction door, long seed, long burrow,
                            int a1, int y1, int b1, int a2, int y2, int b2) {
        super(type, "Door", center, door, seed, a1, y1, b1, a2, y2, b2);
        this.burrow = burrow;
    }

    protected CogsdeepPiece(StructurePieceType type, NbtCompound nbt) {
        super(type, "Door", nbt);
        this.burrow = nbt.getLong("Burrow");
    }

    /** The piece's reference point: the stair axis at its floor (burrow rooms) or at the ground (observatory). */
    @Override
    public BlockPos center() {
        return this.center;
    }

    public Direction door() {
        return this.front;
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        super.writeNbt(context, nbt);
        nbt.putLong("Burrow", this.burrow);
    }

    @Override
    protected Builder builder(StructureWorldAccess world, BlockBox chunkBox, Random random) {
        return new Builder(world, chunkBox, random);
    }

    /** The burrow's stonework: stone bricks with some cracked, mossy and tuff blocks. */
    protected static Block masonry(Builder b) {
        float r = b.random.nextFloat();
        return r < 0.07F ? Blocks.CRACKED_STONE_BRICKS : r < 0.13F ? Blocks.MOSSY_STONE_BRICKS
                : r < 0.17F ? Blocks.TUFF : Blocks.STONE_BRICKS;
    }

    /** Copper going green: mostly cut copper, some of it already exposed, weathered or oxidised. */
    protected static Block cutCopper(Builder b) {
        float r = b.random.nextFloat();
        return r < 0.45F ? Blocks.CUT_COPPER : r < 0.7F ? Blocks.EXPOSED_CUT_COPPER
                : r < 0.9F ? Blocks.WEATHERED_CUT_COPPER : Blocks.OXIDIZED_CUT_COPPER;
    }

    /** Places blocks, containers and folk for one chunk's slice of a piece, in local coordinates. */
    protected class Builder extends OrientedPiece<Builder>.Builder {
        Builder(StructureWorldAccess world, BlockBox chunkBox, Random random) {
            super(world, chunkBox, random);
        }

        /** The block at local a/y/b, read even outside this chunk. */
        @Override
        public BlockState get(int a, int y, int b) {
            return this.world.getBlockState(this.pos(a, y, b));
        }

        /**
         * A room's shell: walls, floor and ceiling of masonry from (a1, -1, b1) to (a2, top, b2), and air inside.
         * The shell keeps out the water and gravel round it, so it's always built whole.
         */
        void room(int a1, int b1, int a2, int b2, int top, Block floor) {
            for (int y = -1; y <= top; y++) {
                for (int a = a1; a <= a2; a++) {
                    for (int b = b1; b <= b2; b++) {
                        boolean shell = y == -1 || y == top || a == a1 || a == a2 || b == b1 || b == b2;
                        if (!shell) {
                            this.set(a, y, b, Blocks.AIR);
                        } else if (y == -1 && a != a1 && a != a2 && b != b1 && b != b2) {
                            this.set(a, y, b, floor);
                        } else {
                            this.set(a, y, b, masonry(this));
                        }
                    }
                }
            }
        }

        /** A doorway {@code width} wide (odd) and {@code height} high through the wall at a/b, along the a or b axis. */
        void doorway(int a, int b, boolean alongA, int width, int height) {
            int half = width / 2;
            for (int i = -half; i <= half; i++) {
                for (int y = 0; y < height; y++) {
                    this.set(alongA ? a + i : a, y, alongA ? b : b + i, Blocks.AIR);
                }
            }
        }

        /**
         * Props local a/b up with {@code fill} from below y down to solid ground, through air, water and plants
         * (at most {@code max} blocks).
         */
        void foundation(int a, int y, int b, BlockState fill, int max) {
            this.propUp(a, y, b, fill, max, false);
        }

        /** A lantern hanging on a chain {@code chain} long from the ceiling block above {@code top}. */
        void hangingLantern(int a, int top, int b, int chain) {
            for (int y = top; y > top - chain; y--) {
                this.set(a, y, b, Blocks.CHAIN);
            }
            this.set(a, top - chain, b, lantern(true));
        }

        /** A Gnome Tinker keeping within {@code range} of this spot. */
        void tinker(int a, int y, int b, int range) {
            BlockPos home = this.pos(a, y, b);
            this.npc(a, y, b, ModEntityTypes.GNOME_TINKER, npc -> npc.setHome(home, range));
        }

        /** A Clockwork Sentinel keeping within {@code range} of this spot. */
        void sentinel(int a, int y, int b, int range) {
            BlockPos home = this.pos(a, y, b);
            this.npc(a, y, b, ModEntityTypes.CLOCKWORK_SENTINEL, npc -> npc.setHome(home, range));
        }
    }
}
