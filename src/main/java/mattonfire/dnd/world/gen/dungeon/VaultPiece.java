package mattonfire.dnd.world.gen.dungeon;

import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.classes.Obstacles.ObstacleType;
import mattonfire.dnd.classes.Obstacles.Tier;
import mattonfire.dnd.dungeon.DungeonGates;
import mattonfire.dnd.dungeon.GateKind;
import mattonfire.dnd.dungeon.GateState;
import mattonfire.dnd.dungeon.RoomRole;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The treasure vault behind the boss ({@link RoomRole#VAULT}), or a side vault off the main path
 * ({@link RoomRole#SIDE_VAULT}), with one thing on a plinth in the middle. The treasure vault holds
 * the Hoard Coffer (a roll per player once the dungeon is cleared); the side vault a locked chest of
 * {@code side_vault} loot (lock DC +2) behind a strict class gate in its doorway (a Greater Arcane Seal:
 * Wizards only, one obstacle tier harder than the main path's; see {@link DungeonPlanner#gateFor}).
 */
public class VaultPiece extends DungeonPiece {
    public VaultPiece(Info info, BlockBox box, long seed, int roomId, RoomRole role, int height, List<Door> doors) {
        super(DungeonStructures.VAULT, info, box, seed, roomId, role, height, doors);
    }

    public VaultPiece(NbtCompound nbt) {
        super(DungeonStructures.VAULT, nbt);
    }

    @Override
    protected void build(Builder b) {
        this.room(b);
        int mx = this.width() / 2;
        int mz = this.depth() / 2;
        Direction toDoor = this.doors.isEmpty() ? Direction.NORTH : this.doors.get(0).side();
        b.set(mx, 1, mz, b.palette.accent());
        if (this.role() == RoomRole.VAULT) {
            b.hoardCoffer(mx, 2, mz, toDoor);
        } else {
            b.chest(mx, 2, mz, toDoor, this.loot("side_vault"));
        }
        b.light(mx - 2, this.height, mz, true);
        b.light(mx + 2, this.height, mz, true);
        if (this.role() == RoomRole.SIDE_VAULT && !this.doors.isEmpty()) {
            this.gate(b, this.doors.get(0));
        }
    }

    /** Bars the doorway's inner wall layer with the side vault's strict gate. */
    private void gate(Builder b, Door door) {
        GateKind kind = DungeonPlanner.gateFor(RoomRole.SIDE_VAULT, b.random);
        long groupSeed = b.random.nextLong();
        Tier tier = DungeonGates.obstacleTier(this.tier(), true);
        ObstacleType obstacle = kind.obstacle();
        if (obstacle == null) {
            return;
        }
        List<BlockPos> blocks = new ArrayList<>();
        for (int[] column : b.doorwayColumns(door, 1)) {
            for (int y = 1; y <= DOOR_HEIGHT; y++) {
                blocks.add(b.obstacle(column[0], y, column[1], obstacle, tier, false, groupSeed));
            }
        }
        b.gate = new GateState(kind, blocks, null, null, groupSeed, tier.asString());
    }
}
