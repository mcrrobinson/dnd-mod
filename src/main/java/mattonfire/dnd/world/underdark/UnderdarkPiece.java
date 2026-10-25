package mattonfire.dnd.world.underdark;

import java.util.function.Consumer;
import mattonfire.dnd.world.gen.OrientedPiece;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LanternBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;

/**
 * Base for the Underdark settlements' pieces (design 9, section 8: the Drow Outposts, and next the Duergar Holds and
 * the Drow Spire). Laid out round a centre like the mod's other {@link OrientedPiece}s: local {@code (a, y, b)} with
 * the piece's front towards +b, turned to face {@link #front}; {@code y = 0} is where you stand and the floor block is
 * at {@code y = -1}. Saved as {@code Center}, {@code Front} and {@code Seed}.
 * <p>
 * The builder adds what cavern settlements need: levelling the cave floor ({@link Builder#ground}), clearing air with
 * the liquids round it sealed off ({@link Builder#clear}, {@link Builder#seal}), natural rock ({@link #rock}), and
 * folk: persistent residents kept near their posts ({@link Builder#resident}) and captives ({@link Builder#captive}).
 */
public abstract class UnderdarkPiece extends OrientedPiece<UnderdarkPiece.Builder> {
    protected UnderdarkPiece(StructurePieceType type, BlockPos center, Direction front, long seed,
                             int a1, int y1, int b1, int a2, int y2, int b2) {
        super(type, "Front", center, front, seed, a1, y1, b1, a2, y2, b2);
    }

    protected UnderdarkPiece(StructurePieceType type, NbtCompound nbt) {
        super(type, "Front", nbt);
    }

    public Direction front() {
        return this.front;
    }

    @Override
    protected Builder builder(StructureWorldAccess world, BlockBox chunkBox, Random random) {
        return new Builder(world, chunkBox, random);
    }

    /** Natural Underdark rock for a spire's or wall's rough outside: dripstone, deepslate, cobbled deepslate, tuff. */
    public static Block rock(Builder b) {
        float r = b.random.nextFloat();
        return r < 0.45F ? Blocks.DRIPSTONE_BLOCK : r < 0.75F ? Blocks.DEEPSLATE : r < 0.92F ? Blocks.COBBLED_DEEPSLATE
                : Blocks.TUFF;
    }

    /** A cheap, repeatable hash of a local position, in [0, 1): the same in every chunk that builds the piece. */
    public static float noise(long seed, int a, int y, int b) {
        long h = net.minecraft.util.math.MathHelper.hashCode(a, y, b) ^ seed;
        h = h * 6364136223846793005L + 1442695040888963407L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        return ((h >>> 40) & 0xFFFFFFL) / (float) 0x1000000;
    }

    /** Whether {@code state} is open: air, a liquid, or something replaceable (lichen, small fungi, webs). */
    public static boolean open(BlockState state) {
        return state.isAir() || !state.getFluidState().isEmpty() || state.isReplaceable();
    }

    /** Places blocks, entities and folk for one chunk's slice of an Underdark piece, in local coordinates. */
    public class Builder extends OrientedPiece<Builder>.Builder {
        protected Builder(StructureWorldAccess world, BlockBox chunkBox, Random random) {
            super(world, chunkBox, random);
        }

        /** Whether the block at local a/y/b (in this chunk) holds a liquid. */
        public boolean fluid(int a, int y, int b) {
            BlockState state = this.get(a, y, b);
            return state != null && !state.getFluidState().isEmpty();
        }

        /** Air at local a/y/b. */
        public void clear(int a, int y, int b) {
            this.set(a, y, b, Blocks.AIR);
        }

        /** Air from y1 to y2 in column a/b. */
        public void clear(int a, int b, int y1, int y2) {
            for (int y = y1; y <= y2; y++) {
                this.set(a, y, b, Blocks.AIR);
            }
        }

        /** Replaces a liquid at local a/y/b with {@code with}, so it can't flow into what was cleared next to it. */
        public void seal(int a, int y, int b, BlockState with) {
            if (this.fluid(a, y, b)) {
                this.set(a, y, b, with);
            }
        }

        /**
         * Levels the cave floor in column a/b: everything open from {@code y = -depth} to the floor ({@code y = -1})
         * becomes {@code fill} (a liquid underneath is sealed with it too), the floor block becomes {@code top} if it
         * had to be filled, and the column is cleared from {@code y = 0} up to {@code clearTo}, with a liquid just
         * above sealed with {@code fill}.
         */
        public void ground(int a, int b, int depth, int clearTo, BlockState fill, BlockState top) {
            for (int y = -depth; y <= -1; y++) {
                BlockState state = this.get(a, y, b);
                if (state != null && open(state)) {
                    this.set(a, y, b, y == -1 ? top : fill);
                }
            }
            this.seal(a, -depth - 1, b, fill);
            this.clear(a, b, 0, clearTo);
            this.seal(a, clearTo + 1, b, fill);
        }

        /** Sets {@code state} at local a/y/b only where it's open ({@link UnderdarkPiece#open}). */
        public void solidify(int a, int y, int b, BlockState state) {
            BlockState there = this.get(a, y, b);
            if (there != null && open(there)) {
                this.set(a, y, b, state);
            }
        }

        /** A lantern hanging on a chain {@code chain} long from the ceiling block above {@code top}. */
        public void hangingLantern(int a, int top, int b, int chain, Block lantern) {
            for (int y = top; y > top - chain; y--) {
                this.set(a, y, b, Blocks.CHAIN);
            }
            this.set(a, top - chain, b, lantern.getDefaultState().with(LanternBlock.HANGING, true));
        }

        /** A standing lantern. */
        public void lantern(int a, int y, int b, Block lantern) {
            this.set(a, y, b, lantern.getDefaultState().with(LanternBlock.HANGING, false));
        }

        /**
         * A persistent resident of {@code type} kept within {@code range} blocks of this spot ({@code setup} runs
         * before it's added, as for {@link #npc}).
         */
        public <E extends MobEntity> void resident(int a, int y, int b, EntityType<E> type, int range,
                                                   Consumer<? super E> setup) {
            BlockPos home = this.pos(a, y, b);
            this.npc(a, y, b, type, mob -> {
                mob.setPersistent();
                mob.setPositionTarget(home, range);
                setup.accept(mob);
            });
        }

        public <E extends MobEntity> void resident(int a, int y, int b, EntityType<E> type, int range) {
            this.resident(a, y, b, type, range, mob -> {
            });
        }

        /** A kneeling captive of {@code type} ({@link Captives}), facing {@code facing} (local). */
        public <E extends MobEntity> void captive(int a, int y, int b, EntityType<E> type, Direction facing) {
            float yaw = this.dir(facing).asRotation();
            this.npc(a, y, b, type, mob -> {
                mob.setYaw(yaw);
                mob.setHeadYaw(yaw);
                mob.setBodyYaw(yaw);
                Captives.bind(mob);
            });
        }
    }
}
