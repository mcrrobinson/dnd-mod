package mattonfire.dnd.tavern;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Remembers which bounties are pinned to a board today. */
public class BountyBoardBlockEntity extends BlockEntity {
    private long day = Long.MIN_VALUE;
    private final Bounty[] posted = new Bounty[BountyBoardBlock.SLOTS];

    public BountyBoardBlockEntity(BlockPos pos, BlockState state) {
        super(Tavern.BOUNTY_BOARD_ENTITY, pos, state);
    }

    /** Pins up a fresh set of notices if a new day has dawned since the last ones went up. */
    public void refresh(World world) {
        long today = world.getTimeOfDay() / 24000L;
        if (today == this.day) {
            return;
        }
        this.day = today;
        List<Bounty> drawn = Bounty.draw(world.getRandom(), this.posted.length);
        BlockState state = world.getBlockState(this.pos);
        for (int i = 0; i < this.posted.length; i++) {
            this.posted[i] = i < drawn.size() ? drawn.get(i) : null;
            state = state.with(BountyBoardBlock.NOTICES[i], this.posted[i] != null);
        }
        world.setBlockState(this.pos, state);
        this.markDirty();
    }

    /**
     * A board someone has just hung up starts bare, and gets its first notices next morning, so
     * taking a board down and putting it back up doesn't post a fresh set.
     */
    public void startBare(World world) {
        this.day = world.getTimeOfDay() / 24000L;
        BlockState state = world.getBlockState(this.pos);
        for (int i = 0; i < this.posted.length; i++) {
            this.posted[i] = null;
            if (state.contains(BountyBoardBlock.NOTICES[i])) {
                state = state.with(BountyBoardBlock.NOTICES[i], false);
            }
        }
        world.setBlockState(this.pos, state);
        this.markDirty();
    }

    @Nullable
    public Bounty posted(int slot) {
        return this.posted[slot];
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putLong("Day", this.day);
        NbtList list = new NbtList();
        for (Bounty bounty : this.posted) {
            list.add(NbtString.of(bounty == null ? "" : bounty.id));
        }
        nbt.put("Posted", list);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.day = nbt.contains("Day") ? nbt.getLong("Day") : Long.MIN_VALUE;
        NbtList list = nbt.getList("Posted", NbtElement.STRING_TYPE);
        for (int i = 0; i < this.posted.length; i++) {
            this.posted[i] = i < list.size() ? Bounty.byId(list.getString(i)) : null;
        }
    }
}
