package mattonfire.dnd.world.gen.stronghold;

import static mattonfire.dnd.world.gen.PieceBlocks.*;

import java.util.function.Consumer;
import mattonfire.dnd.classes.Registry.ModBannerPatterns;
import mattonfire.dnd.entity.OrcEntity;
import mattonfire.dnd.world.gen.OrientedPiece;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SkullBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;

/**
 * Base for the Orc Stronghold pieces. Everything is laid out in the hold's own frame round its centre: local
 * {@code a} runs across (east when the gate faces south), local {@code b} runs from the back towards the gate,
 * and y = 0 is the levelled floor. Block states are written as if the gate faced south and turned with
 * {@link BlockState#rotate}, so the whole hold can face any way.
 *
 * <p>A piece is built once per chunk it overlaps, so all of its randomness comes from its own seed, and every
 * draw happens whether or not its block lands in the chunk. Each piece also carries its hold's key (the start
 * chunk, {@link ChunkPos#toLong}) so the orcs it places know which hold they belong to.
 */
public abstract class StrongholdPiece extends OrientedPiece<StrongholdPiece.Builder> {
    public static final Identifier YURT_LOOT = loot("orc_stronghold_yurt");
    public static final Identifier ARMORY_LOOT = loot("orc_stronghold_armory");
    public static final Identifier HOARD_LOOT = loot("orc_stronghold_hoard");

    protected final long hold;

    protected StrongholdPiece(StructurePieceType type, BlockPos center, Direction gate, long seed, long hold,
                              int a1, int y1, int b1, int a2, int y2, int b2) {
        super(type, "Gate", center, gate, seed, a1, y1, b1, a2, y2, b2);
        this.hold = hold;
    }

    protected StrongholdPiece(StructurePieceType type, NbtCompound nbt) {
        super(type, "Gate", nbt);
        this.hold = nbt.getLong("Hold");
    }

    public long hold() {
        return this.hold;
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        super.writeNbt(context, nbt);
        nbt.putLong("Hold", this.hold);
    }

    @Override
    protected Builder builder(StructureWorldAccess world, BlockBox chunkBox, Random random) {
        return new Builder(world, chunkBox, random);
    }

    /** A floor-standing skull or head, turned {@code rotation} (0-15). */
    protected static BlockState skull(Random random) {
        float r = random.nextFloat();
        Block skull = r < 0.6F ? Blocks.SKELETON_SKULL : r < 0.85F ? Blocks.ZOMBIE_HEAD : r < 0.97F ? Blocks.CREEPER_HEAD
                : Blocks.WITHER_SKELETON_SKULL;
        return skull.getDefaultState().with(SkullBlock.ROTATION, random.nextInt(16));
    }

    /** Places blocks, containers and orcs for one chunk's slice of a piece, in local coordinates. */
    protected class Builder extends OrientedPiece<Builder>.Builder {
        Builder(StructureWorldAccess world, BlockBox chunkBox, Random random) {
            super(world, chunkBox, random);
        }

        /** Props local a/b up with {@code fill} from below y down to solid ground (at most {@code max} blocks). */
        void foundation(int a, int y, int b, BlockState fill, int max) {
            this.propUp(a, y, b, fill, max, true);
        }

        /** A red war banner with the black Bloodtusk and a black border (a wall banner on the block behind it). */
        void warBanner(int a, int y, int b, Direction facing) {
            this.banner(a, y, b, facing, DyeColor.RED, new ModBannerPatterns.Layer("bo", DyeColor.BLACK),
                    ModBannerPatterns.BLOODTUSK.in(DyeColor.BLACK));
        }

        <E extends OrcEntity> void orc(int a, int y, int b, EntityType<E> type, BlockPos home, int range) {
            this.orc(a, y, b, type, home, range, orc -> {
            });
        }

        /**
         * An orc of {@code type} of this hold, keeping within {@code range} of {@code home}. {@code setup} runs
         * before it's added to the world: worldgen saves entities straight away, so later changes would be lost.
         */
        <E extends OrcEntity> void orc(int a, int y, int b, EntityType<E> type, BlockPos home, int range, Consumer<E> setup) {
            this.npc(a, y, b, type, orc -> {
                orc.setHome(home, range);
                orc.setHold(StrongholdPiece.this.hold);
                setup.accept(orc);
            });
        }
    }
}
