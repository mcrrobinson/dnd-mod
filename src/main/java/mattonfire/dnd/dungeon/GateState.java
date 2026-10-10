package mattonfire.dnd.dungeon;

import java.util.List;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * A class-check gate's blocks, kept in its room's ward (written at worldgen) so the gate can be rebuilt
 * when the dungeon repopulates: the gateway blocks (obstacle, rubble or door), the iron door's lower half
 * and the key chest for a {@link GateKind#LOCKED_DOOR}, and the obstacle group seed and tier.
 */
public class GateState {
    private final GateKind kind;
    private final List<BlockPos> blocks;
    @Nullable
    private final BlockPos door;
    @Nullable
    private final BlockPos keyChest;
    private final long groupSeed;
    private final String tier;
    private int resetsSeen;

    public GateState(GateKind kind, List<BlockPos> blocks, @Nullable BlockPos door, @Nullable BlockPos keyChest, long groupSeed,
                     String tier) {
        this.kind = kind;
        this.blocks = List.copyOf(blocks);
        this.door = door;
        this.keyChest = keyChest;
        this.groupSeed = groupSeed;
        this.tier = tier;
    }

    public GateKind kind() {
        return this.kind;
    }

    public List<BlockPos> blocks() {
        return this.blocks;
    }

    @Nullable
    public BlockPos door() {
        return this.door;
    }

    @Nullable
    public BlockPos keyChest() {
        return this.keyChest;
    }

    public long groupSeed() {
        return this.groupSeed;
    }

    /** The obstacle tier name (see {@code Obstacles.Tier}). */
    public String tier() {
        return this.tier;
    }

    public int resetsSeen() {
        return this.resetsSeen;
    }

    void setResetsSeen(int resets) {
        this.resetsSeen = resets;
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("Kind", this.kind.name());
        nbt.put("Blocks", PuzzleState.positions(this.blocks));
        if (this.door != null) {
            nbt.put("Door", NbtHelper.fromBlockPos(this.door));
        }
        if (this.keyChest != null) {
            nbt.put("KeyChest", NbtHelper.fromBlockPos(this.keyChest));
        }
        nbt.putLong("GroupSeed", this.groupSeed);
        nbt.putString("Tier", this.tier);
        nbt.putInt("ResetsSeen", this.resetsSeen);
        return nbt;
    }

    public static GateState fromNbt(NbtCompound nbt) {
        GateState state = new GateState(GateKind.byName(nbt.getString("Kind")),
                PuzzleState.positions(nbt.getList("Blocks", NbtElement.COMPOUND_TYPE)),
                nbt.contains("Door") ? NbtHelper.toBlockPos(nbt.getCompound("Door")) : null,
                nbt.contains("KeyChest") ? NbtHelper.toBlockPos(nbt.getCompound("KeyChest")) : null,
                nbt.getLong("GroupSeed"), nbt.getString("Tier"));
        state.resetsSeen = nbt.getInt("ResetsSeen");
        return state;
    }
}
