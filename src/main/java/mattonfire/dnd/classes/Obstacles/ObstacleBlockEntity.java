package mattonfire.dnd.classes.Obstacles;

import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Obstacles.types.HiddenDoorType;
import mattonfire.dnd.classes.SkillChecks.Perceivable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
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
public class ObstacleBlockEntity extends BlockEntity implements Perceivable {
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

    /** Saves and syncs a change to {@link #data()} that clients need (the hint reads it). */
    public void dataChanged() {
        changed();
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

    // ---- Perceivable: a Search (V) can find a hidden obstacle, at its tier's DC ----

    @Override
    public int perceptionDc() {
        return tier.dc;
    }

    @Override
    public Skill perceptionSkill() {
        return Skill.INVESTIGATION;
    }

    @Override
    public Vec3d perceptionPos() {
        return Vec3d.ofCenter(pos);
    }

    @Override
    public boolean hiddenFrom(ServerPlayerEntity player) {
        BlockState state = getCachedState();
        return world != null && ObstacleBlock.isSealed(state)
                && ((ObstacleBlock) state.getBlock()).type().hiddenFrom(world, pos, player);
    }

    @Override
    public void perceive(ServerPlayerEntity player, boolean searched) {
        if (world instanceof ServerWorld serverWorld
                && getCachedState().getBlock() instanceof ObstacleBlock block
                && block.type() instanceof HiddenDoorType door) {
            door.reveal(serverWorld, pos, player, searched ? HiddenDoorType.How.SEARCH : HiddenDoorType.How.PASSIVE);
        }
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
        // Checked every tick so short timers (a raised portcullis) are exact
        if (be.resealTicks <= 0 || state.get(ObstacleBlock.STATE) != ObstacleState.OPEN) {
            return;
        }
        if (world.getTime() >= be.openedAt + be.resealTicks && world instanceof ServerWorld serverWorld) {
            ObstacleGroups.seal(serverWorld, pos);
        }
    }
}
