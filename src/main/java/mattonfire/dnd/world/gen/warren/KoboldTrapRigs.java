package mattonfire.dnd.world.gen.warren;

import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.classes.Blocks.CrumblingFloorBlock;
import mattonfire.dnd.classes.Blocks.DartLauncherBlock;
import mattonfire.dnd.classes.Blocks.KoboldTriplineBlock;
import mattonfire.dnd.classes.Blocks.SpikeTrapBlock;
import mattonfire.dnd.classes.Blocks.TrapBlocks;
import mattonfire.dnd.classes.Blocks.TrapKind;
import mattonfire.dnd.classes.Blocks.TrapTriggerBlockEntity;
import mattonfire.dnd.faction.FactionIds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.PointedDripstoneBlock;
import net.minecraft.block.enums.Thickness;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

/**
 * Kobold traps (design 10, 4.1-4.2): {@link #setup} writes one into its trigger's block entity with {@code Faction:
 * dndclasses:kobolds}, for the warren pieces and for {@code /dndhome koboldtrap}, which builds a test rig in front
 * of you ({@link #testRig}).
 */
public final class KoboldTrapRigs {
    private KoboldTrapRigs() {
    }

    /**
     * Puts down a kobold trap's trigger at {@code trigger} (world) and records the trap in it, if the trigger is in
     * {@code box} (null: anywhere). The trap's other blocks are placed by the caller.
     */
    public static void setup(WorldAccess world, @Nullable BlockBox box, BlockPos trigger, TrapKind kind, int tier,
                             List<BlockPos> linked, List<BlockPos> crumbles, List<BlockPos> rubble, @Nullable BlockPos ladder,
                             Direction ladderFacing, CrumblingFloorBlock.Covering covering, boolean buried) {
        if (box != null && !box.contains(trigger)) {
            return;
        }
        world.setBlockState(trigger, TrapBlocks.TRAP_TRIGGER.getDefaultState(), Block.NOTIFY_LISTENERS);
        if (world.getBlockEntity(trigger) instanceof TrapTriggerBlockEntity trap) {
            trap.setup(kind, 0L, tier, linked, crumbles, ladder, ladderFacing);
            trap.setFaction(FactionIds.KOBOLDS);
            trap.setBuried(buried);
            trap.setCovering(covering);
            trap.setRubble(rubble);
        }
    }

    /**
     * A test rig of {@code kind} on the floor at {@code floor} (the block you stand on), running {@code forward}: a
     * 3-wide strip of floor 7 long is laid (stone), and the trap sits 3 blocks ahead. Returns the trigger.
     * <ul>
     * <li>rockfall: a tripline across the strip 3 ahead, a 3 x 3 loose ceiling 4 above the floor over it.</li>
     * <li>spikes: a row of 3 spike tiles 3 ahead.</li>
     * <li>pit: 3 x 2 straw-lidded crumbling floor 3-4 ahead over a 4-deep dripstone pit, a ladder at the far end.</li>
     * <li>dart: a visible trigger tile 3 ahead and a launcher in a post beside it.</li>
     * </ul>
     */
    public static BlockPos testRig(ServerWorld world, BlockPos floor, Direction forward, TrapKind kind, int tier) {
        Direction side = forward.rotateYClockwise();
        BlockPos at = floor.offset(forward, 3);
        for (int f = -1; f <= 6; f++) {
            for (int s = -2; s <= 2; s++) {
                BlockPos p = floor.offset(forward, f).offset(side, s);
                world.setBlockState(p, Blocks.STONE.getDefaultState());
                for (int y = 1; y <= 5; y++) {
                    world.setBlockState(p.up(y), Blocks.AIR.getDefaultState());
                }
                for (int y = 1; y <= 5; y++) {
                    world.setBlockState(p.down(y), Blocks.STONE.getDefaultState());
                }
            }
        }
        List<BlockPos> linked = new ArrayList<>();
        List<BlockPos> crumbles = new ArrayList<>();
        List<BlockPos> rubble = new ArrayList<>();
        BlockPos trigger = at.down();
        BlockPos ladder = null;
        Direction ladderFacing = Direction.NORTH;
        boolean buried = true;
        CrumblingFloorBlock.Covering covering = CrumblingFloorBlock.Covering.STONE;
        switch (kind) {
            case ROCKFALL -> {
                BlockState line = TrapBlocks.KOBOLD_TRIPLINE.getDefaultState().with(KoboldTriplineBlock.AXIS, side.getAxis());
                for (int s = -1; s <= 1; s++) {
                    BlockPos p = at.offset(side, s).up();
                    world.setBlockState(p, line);
                    linked.add(p);
                }
                for (int f = -1; f <= 1; f++) {
                    for (int s = -1; s <= 1; s++) {
                        BlockPos tile = at.offset(forward, f).offset(side, s);
                        BlockPos ceiling = tile.up(4);
                        world.setBlockState(ceiling, TrapBlocks.LOOSE_CEILING.getDefaultState());
                        linked.add(ceiling);
                        rubble.add(tile.up());
                    }
                }
            }
            case SPIKES -> {
                for (int s = -1; s <= 1; s++) {
                    BlockPos p = at.offset(side, s);
                    world.setBlockState(p, TrapBlocks.SPIKE_TRAP.getDefaultState().with(SpikeTrapBlock.FLOOR, SpikeTrapBlock.Floor.STONE));
                    linked.add(p);
                }
            }
            case PIT -> {
                covering = CrumblingFloorBlock.Covering.STRAW;
                trigger = floor.offset(forward, 2).down();
                ladder = at.offset(forward, 1);
                ladderFacing = forward.getOpposite();
                pit(world, null, at, forward, side, crumbles, ladder, ladderFacing);
            }
            default -> {
                buried = false;
                trigger = at;
                BlockPos post = at.offset(side, 2);
                world.setBlockState(post.up(), TrapBlocks.DART_LAUNCHER.getDefaultState()
                        .with(DartLauncherBlock.FACING, side.getOpposite()));
                linked.add(post.up());
                kind = TrapKind.DART;
            }
        }
        setup(world, null, trigger, kind, tier, linked, crumbles, rubble, ladder, ladderFacing, covering, buried);
        return trigger;
    }

    /**
     * A covered pit 3 wide and 2 long, its near row at {@code near} (the floor level): straw-lidded crumbling floor over
     * a 4-deep shaft lined in stone, pointed dripstone on its floor and a ladder up the far wall at {@code ladder}'s
     * column. The crumbling tiles go into {@code crumbles}. {@code box} limits it to one chunk (null: anywhere).
     */
    static void pit(WorldAccess world, @Nullable BlockBox box, BlockPos near, Direction forward, Direction side,
                    List<BlockPos> crumbles, BlockPos ladder, Direction ladderFacing) {
        BlockState lid = TrapBlocks.CRUMBLING_FLOOR.getDefaultState()
                .with(CrumblingFloorBlock.COVERING, CrumblingFloorBlock.Covering.STRAW);
        BlockState spike = Blocks.POINTED_DRIPSTONE.getDefaultState()
                .with(PointedDripstoneBlock.VERTICAL_DIRECTION, Direction.UP).with(PointedDripstoneBlock.THICKNESS, Thickness.TIP);
        BlockState rung = Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, ladderFacing);
        for (int f = -1; f <= 2; f++) {
            for (int s = -2; s <= 2; s++) {
                BlockPos column = near.offset(forward, f).offset(side, s);
                boolean open = f >= 0 && f <= 1 && Math.abs(s) <= 1;
                for (int y = -5; y <= -1; y++) {
                    BlockPos p = column.up(y);
                    BlockState state;
                    if (!open || y == -5) {
                        state = Blocks.STONE.getDefaultState();
                    } else if (column.getX() == ladder.getX() && column.getZ() == ladder.getZ()) {
                        state = rung;
                    } else {
                        state = y == -4 ? spike : Blocks.AIR.getDefaultState();
                    }
                    put(world, box, p, state);
                }
                if (open) {
                    put(world, box, column, lid);
                    crumbles.add(column.toImmutable());
                }
            }
        }
    }

    private static void put(WorldAccess world, @Nullable BlockBox box, BlockPos pos, BlockState state) {
        if (box == null || box.contains(pos)) {
            world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
        }
    }
}
