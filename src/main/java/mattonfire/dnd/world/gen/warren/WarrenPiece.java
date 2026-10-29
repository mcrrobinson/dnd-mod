package mattonfire.dnd.world.gen.warren;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import mattonfire.dnd.classes.Blocks.CrumblingFloorBlock;
import mattonfire.dnd.classes.Blocks.KoboldSnareBlock;
import mattonfire.dnd.classes.Blocks.KoboldSnareBlockEntity;
import mattonfire.dnd.classes.Blocks.TrapKind;
import mattonfire.dnd.entity.kobold.KoboldEntity;
import mattonfire.dnd.entity.kobold.Kobolds;
import mattonfire.dnd.world.gen.OrientedPiece;
import mattonfire.dnd.world.gen.PieceBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import org.jetbrains.annotations.Nullable;

/**
 * Base for the kobold warren pieces ({@link KoboldWarrenPlanner}). Every piece is laid out round a centre with the
 * warren's front (the cave mouth, out of the hill) towards local +b ({@link OrientedPiece}): the mouth and tunnel round
 * the mouth, the rooms round the den's middle. y = 0 is the floor.
 * <p>
 * Pieces are dug into the rock: {@link Builder#seal} first turns any water, lava, sand or gravel round a room into rock
 * (so nothing floods in or falls), then {@link Builder#carve} hollows the room out. Kobolds are persistent and kept
 * near their room ({@link Builder#kobold}). {@link #tier} is the trap tier: 1, or 2 in a great warren.
 */
public abstract class WarrenPiece extends OrientedPiece<WarrenPiece.Builder> {
    static final Identifier HOARD = PieceBlocks.loot("kobold_hoard");
    static final Identifier WORKSHOP = PieceBlocks.loot("kobold_workshop");
    static final Identifier LARDER = PieceBlocks.loot("kobold_larder");
    /** The front: out of the hill. */
    static final Direction OUT = Direction.SOUTH;
    /** Into the hill. */
    static final Direction IN = Direction.NORTH;

    protected final int tier;

    protected WarrenPiece(StructurePieceType type, BlockPos center, Direction front, long seed, int tier,
                          int a1, int y1, int b1, int a2, int y2, int b2) {
        super(type, "Front", center, front, seed, a1, y1, b1, a2, y2, b2);
        this.tier = tier;
    }

    protected WarrenPiece(StructurePieceType type, NbtCompound nbt) {
        super(type, "Front", nbt);
        this.tier = Math.max(1, nbt.getInt("Tier"));
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        super.writeNbt(context, nbt);
        nbt.putInt("Tier", this.tier);
    }

    public int tier() {
        return this.tier;
    }

    public Direction front() {
        return this.front;
    }

    @Override
    protected Builder builder(StructureWorldAccess world, BlockBox chunkBox, Random random) {
        return new Builder(world, chunkBox, random);
    }

    public class Builder extends OrientedPiece<Builder>.Builder {
        Builder(StructureWorldAccess world, BlockBox chunkBox, Random random) {
            super(world, chunkBox, random);
        }

        /** Turns liquids and loose sand or gravel in the box into rock, so the rooms dug inside stay dry and stable. */
        void seal(int a1, int y1, int b1, int a2, int y2, int b2) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int a = Math.min(a1, a2); a <= Math.max(a1, a2); a++) {
                    for (int b = Math.min(b1, b2); b <= Math.max(b1, b2); b++) {
                        BlockState state = this.get(a, y, b);
                        if (state == null) {
                            continue;
                        }
                        if (!state.getFluidState().isEmpty()) {
                            this.set(a, y, b, Blocks.STONE);
                        } else if (state.getBlock() instanceof FallingBlock) {
                            this.set(a, y, b, state.isOf(Blocks.RED_SAND) ? Blocks.RED_SANDSTONE
                                    : state.isOf(Blocks.SAND) ? Blocks.SANDSTONE : Blocks.STONE);
                        }
                    }
                }
            }
        }

        /** Hollows out the box. */
        void carve(int a1, int y1, int b1, int a2, int y2, int b2) {
            this.air(a1, y1, b1, a2, y2, b2);
        }

        /** A rough kobold floor: packed dirt, coarse dirt and the odd stone. */
        BlockState dirtFloor() {
            float r = this.random.nextFloat();
            return (r < 0.45F ? Blocks.COARSE_DIRT : r < 0.7F ? Blocks.PACKED_MUD : r < 0.85F ? Blocks.ROOTED_DIRT
                    : Blocks.COBBLESTONE).getDefaultState();
        }

        /** Lays a floor at y with {@code floor} and props it up over any hole below. */
        void floor(int a, int y, int b, BlockState floor) {
            this.set(a, y, b, floor);
            this.propUp(a, y, b, Blocks.STONE.getDefaultState(), 6, false);
        }

        /** A torch on the wall behind it ({@code facing} points out of the wall, local). */
        void wallTorch(int a, int y, int b, Direction facing) {
            this.set(a, y, b, Blocks.WALL_TORCH.getDefaultState().with(WallTorchBlock.FACING, facing));
        }

        /** A kobold living here: persistent, keeping within {@code range} blocks of where it was placed. */
        <E extends KoboldEntity> void kobold(int a, int y, int b, EntityType<E> type, int range) {
            this.kobold(a, y, b, type, range, kobold -> {
            });
        }

        <E extends KoboldEntity> void kobold(int a, int y, int b, EntityType<E> type, int range, Consumer<? super E> setup) {
            BlockPos home = this.pos(a, y, b);
            this.npc(a, y, b, type, kobold -> {
                kobold.setPersistent();
                kobold.setHome(home, range);
                setup.accept(kobold);
            });
        }

        /** A rank-and-file kobold: 60% a Kobold, 40% a Slinger (one random draw either way). */
        void koboldOrSlinger(int a, int y, int b, int range) {
            boolean slinger = this.chance(0.4F);
            if (slinger) {
                this.kobold(a, y, b, Kobolds.KOBOLD_SLINGER, range);
            } else {
                this.kobold(a, y, b, Kobolds.KOBOLD, range);
            }
        }

        /** A kobold snare set at generation: it spares kobolds and never falls apart. */
        void snare(int a, int y, int b) {
            this.set(a, y, b, Kobolds.KOBOLD_SNARE_BLOCK.getDefaultState().with(KoboldSnareBlock.WATERLOGGED, false));
            BlockPos pos = this.pos(a, y, b);
            if (this.chunkBox.contains(pos) && this.world.getBlockEntity(pos) instanceof KoboldSnareBlockEntity snare) {
                snare.setKoboldOwner(new UUID(0L, 0L), 0L);
            }
        }

        /** Records a kobold trap in its trigger block (if the trigger is in this chunk; positions are world ones). */
        void trap(BlockPos trigger, TrapKind kind, List<BlockPos> linked, List<BlockPos> crumbles, List<BlockPos> rubble,
                  @Nullable BlockPos ladder, Direction ladderFacing, CrumblingFloorBlock.Covering covering, boolean buried) {
            KoboldTrapRigs.setup(this.world, this.chunkBox, trigger, kind, WarrenPiece.this.tier, linked, crumbles, rubble,
                    ladder, ladderFacing, covering, buried);
        }
    }
}
