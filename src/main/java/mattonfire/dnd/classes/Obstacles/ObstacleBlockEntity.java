package mattonfire.dnd.classes.Obstacles;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Shared by every obstacle block. NBT:
 * <ul>
 * <li>{@code Tier}: easy, medium, hard or very_hard</li>
 * <li>{@code Critical}: on a structure's main path (its type must have a solo fallback)</li>
 * <li>{@code ResealTicks}: reseals this long after opening (0 = never); {@code OpenedAt}: world time it opened</li>
 * <li>{@code GroupSeed}: touching blocks of the same type and seed open together</li>
 * <li>{@code Data}: per-type state for later obstacle kinds</li>
 * </ul>
 * Synced to clients for the crosshair hint.
 */
public class ObstacleBlockEntity extends BlockEntity {
    private Tier tier = Tier.MEDIUM;
    private boolean critical;
    private int resealTicks;
    private long openedAt;
    private long groupSeed;
    private NbtCompound data = new NbtCompound();

    public ObstacleBlockEntity(BlockPos pos, BlockState state) {
        super(ObstacleTypes.BLOCK_ENTITY, pos, state);
    }

    public Tier tier() {
        return tier;
    }

    public boolean critical() {
        return critical;
    }

    public int resealTicks() {
        return resealTicks;
    }

    public long openedAt() {
        return openedAt;
    }

    public long groupSeed() {
        return groupSeed;
    }

    public NbtCompound data() {
        return data;
    }

    public void configure(Tier tier, boolean critical, long groupSeed) {
        this.tier = tier;
        this.critical = critical;
        this.groupSeed = groupSeed;
        changed();
    }

    public void setTier(Tier tier) {
        this.tier = tier;
        changed();
    }

    public void setResealTicks(int ticks) {
        this.resealTicks = Math.max(0, ticks);
        changed();
    }

    void setOpenedAt(long time) {
        this.openedAt = time;
        markDirty();
    }

    private void changed() {
        markDirty();
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    @Override
    public void setWorld(World world) {
        super.setWorld(world);
        if (world instanceof ServerWorld serverWorld) {
            ObstacleIndex.addLater(serverWorld, pos);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putString("Tier", tier.asString());
        nbt.putBoolean("Critical", critical);
        nbt.putInt("ResealTicks", resealTicks);
        nbt.putLong("OpenedAt", openedAt);
        nbt.putLong("GroupSeed", groupSeed);
        nbt.put("Data", data);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        tier = Tier.byName(nbt.getString("Tier"), Tier.MEDIUM);
        critical = nbt.getBoolean("Critical");
        resealTicks = nbt.getInt("ResealTicks");
        openedAt = nbt.getLong("OpenedAt");
        groupSeed = nbt.getLong("GroupSeed");
        data = nbt.getCompound("Data");
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    /** Server ticker: reseals the group once its reseal time is up. */
    static void tick(World world, BlockPos pos, BlockState state, ObstacleBlockEntity be) {
        if (be.resealTicks <= 0 || state.get(ObstacleBlock.STATE) != ObstacleState.OPEN
                || (world.getTime() + pos.asLong()) % 20 != 0) {
            return;
        }
        if (world.getTime() >= be.openedAt + be.resealTicks && world instanceof ServerWorld serverWorld) {
            ObstacleGroups.seal(serverWorld, pos);
        }
    }
}
