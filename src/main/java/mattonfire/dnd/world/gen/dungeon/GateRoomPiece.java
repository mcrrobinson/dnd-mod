package mattonfire.dnd.world.gen.dungeon;

import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.classes.Obstacles.ObstacleType;
import mattonfire.dnd.classes.Obstacles.Tier;
import mattonfire.dnd.dungeon.DungeonGates;
import mattonfire.dnd.dungeon.GateKind;
import mattonfire.dnd.dungeon.GateState;
import mattonfire.dnd.dungeon.RoomRole;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * A room split across the path by a wall with a 3-wide gateway, barred by a class-check gate
 * ({@link GateKind}, picked by {@link DungeonPlanner#gateFor} so the main path never hangs on one class):
 * <ul>
 * <li>a Lesser Arcane Seal (an Area 1 obstacle: Wizards and Warlocks dispel it, anyone can mine it and take
 * the backlash);</li>
 * <li>a rubble cave-in anyone can dig through;</li>
 * <li>an iron door, with its lever "key" in a locked chest on the near side: a Rogue picks the lock, anyone
 * else smashes the chest open. A lever on either wall block next to the door opens it.</li>
 * </ul>
 * The gate is recorded in the room's ward ({@link GateState}) and rebuilt when the dungeon repopulates.
 */
public class GateRoomPiece extends DungeonPiece {
    public GateRoomPiece(Info info, BlockBox box, long seed, int roomId, RoomRole role, int height, List<Door> doors) {
        super(DungeonStructures.GATE_ROOM, info, box, seed, roomId, role, height, doors);
    }

    public GateRoomPiece(NbtCompound nbt) {
        super(DungeonStructures.GATE_ROOM, nbt);
    }

    @Override
    protected void build(Builder b) {
        this.room(b);
        int w = this.width();
        int d = this.depth();
        // The path runs between the two doors; the gate wall stands across it, halfway.
        Direction entry = this.doors.isEmpty() ? Direction.WEST : this.doors.get(0).side();
        boolean alongX = entry.getAxis() == Direction.Axis.X;
        int across = alongX ? d : w;
        int mid = (alongX ? w : d) / 2;
        int centre = across / 2;
        for (int t = 2; t < across - 2; t++) {
            boolean gateway = Math.abs(t - centre) <= 1;
            boolean jamb = Math.abs(t - centre) == 2;
            for (int y = 1; y <= this.height; y++) {
                if (gateway && y <= DOOR_HEIGHT) {
                    continue;
                }
                BlockState state = jamb || (gateway && y == DOOR_HEIGHT + 1) ? b.palette.accent() : b.palette.wall(b.random);
                set(b, alongX, mid, t, y, state);
            }
        }
        b.light(alongX ? mid - 2 : centre, this.height, alongX ? centre : mid - 2, true);
        b.light(alongX ? mid + 2 : centre, this.height, alongX ? centre : mid + 2, true);
        this.gate(b, alongX, mid, centre, entry);
    }

    /** Sets the block {@code along} the path and {@code t} across it. */
    private static void set(Builder b, boolean alongX, int along, int t, int y, BlockState state) {
        if (alongX) {
            b.set(along, y, t, state);
        } else {
            b.set(t, y, along, state);
        }
    }

    private static BlockPos pos(Builder b, boolean alongX, int along, int t, int y) {
        return alongX ? b.pos(along, y, t) : b.pos(t, y, along);
    }

    private void gate(Builder b, boolean alongX, int mid, int centre, Direction entry) {
        GateKind kind = DungeonPlanner.gateFor(RoomRole.GATE, b.random);
        long groupSeed = b.random.nextLong();
        long lootSeed = b.random.nextLong();
        Tier tier = DungeonGates.obstacleTier(this.tier(), false);
        List<BlockPos> blocks = new ArrayList<>();
        BlockPos door = null;
        BlockPos keyChest = null;
        ObstacleType obstacle = kind.obstacle();
        if (obstacle != null) {
            for (int t = centre - 1; t <= centre + 1; t++) {
                for (int y = 1; y <= DOOR_HEIGHT; y++) {
                    blocks.add(alongX ? b.obstacle(mid, y, t, obstacle, tier, kind.mainPathSafe(), groupSeed)
                            : b.obstacle(t, y, mid, obstacle, tier, kind.mainPathSafe(), groupSeed));
                }
            }
        } else if (kind == GateKind.RUBBLE) {
            int i = 0;
            for (int t = centre - 1; t <= centre + 1; t++) {
                for (int y = 1; y <= DOOR_HEIGHT; y++) {
                    set(b, alongX, mid, t, y, DungeonGates.rubble(y == 1, i++));
                    blocks.add(pos(b, alongX, mid, t, y));
                }
            }
        } else {
            // An iron door in the middle of the gateway, wall either side of it and over it.
            Direction facing = entry.getOpposite();
            BlockState iron = Blocks.IRON_DOOR.getDefaultState().with(DoorBlock.FACING, facing);
            set(b, alongX, mid, centre, 1, iron.with(DoorBlock.HALF, DoubleBlockHalf.LOWER));
            set(b, alongX, mid, centre, 2, iron.with(DoorBlock.HALF, DoubleBlockHalf.UPPER));
            set(b, alongX, mid, centre, 3, b.palette.accent());
            door = pos(b, alongX, mid, centre, 1);
            blocks.add(pos(b, alongX, mid, centre, 3));
            for (int t : new int[]{centre - 1, centre + 1}) {
                for (int y = 1; y <= DOOR_HEIGHT; y++) {
                    set(b, alongX, mid, t, y, y == 2 ? b.palette.trim() : b.palette.wall(b.random));
                    blocks.add(pos(b, alongX, mid, t, y));
                }
            }
            // The key chest stands against the side wall on the near side of the gate.
            boolean entryLow = entry == Direction.WEST || entry == Direction.NORTH;
            int chestAlong = mid + (entryLow ? -2 : 2);
            Direction chestFacing = alongX ? Direction.SOUTH : Direction.EAST;
            set(b, alongX, chestAlong, 2, 1, facing(Blocks.CHEST, chestFacing));
            keyChest = pos(b, alongX, chestAlong, 2, 1);
            if (b.chunkBox.contains(keyChest) && b.world.getBlockEntity(keyChest) instanceof LootableContainerBlockEntity chest) {
                chest.setLootTable(DungeonGates.keyTable(this.tier()), lootSeed);
            }
        }
        b.gate = new GateState(kind, blocks, door, keyChest, groupSeed, tier.asString());
    }
}
