package mattonfire.dnd.classes.Blocks.adventuring;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.Registry.AdventuringBlocks;
import mattonfire.dnd.dungeon.DungeonRegistry;
import mattonfire.dnd.dungeon.DungeonState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/**
 * What an Adventurer's Signpost says: a few lines of text, drawn on both faces of its board. A signpost put up at a
 * dungeon's entrance also remembers the dungeon ({@link #setDungeon}), and refreshes its Challenge line from the
 * {@link DungeonRegistry} whenever someone reads it, so {@code /dungeon tier} and clears show up on it.
 */
public class SignpostBlockEntity extends BlockEntity {
    private static final String LINES = "Lines";
    private static final String DUNGEON = "Dungeon";

    private final List<Text> lines = new ArrayList<>();
    @Nullable
    private Long dungeon;

    public SignpostBlockEntity(BlockPos pos, BlockState state) {
        super(AdventuringBlocks.SIGNPOST_ENTITY, pos, state);
    }

    public List<Text> lines() {
        return List.copyOf(this.lines);
    }

    public void setLines(List<? extends Text> lines) {
        this.lines.clear();
        this.lines.addAll(lines);
        this.changed();
    }

    /** Points this signpost at a dungeon: its name, then "Challenge II" in red. */
    public void setDungeon(long startKey, Text name, Text tier) {
        this.dungeon = startKey;
        this.setLines(List.of(name, tier.copy().formatted(Formatting.DARK_RED)));
    }

    /** The lines for the dungeon as the server knows it now: name, tier and "Cleared" once its boss is dead. */
    public void refresh(ServerWorld world) {
        if (this.dungeon == null) {
            return;
        }
        DungeonState state = DungeonRegistry.get(world).get(this.dungeon);
        if (state == null) {
            return;
        }
        List<Text> fresh = new ArrayList<>();
        fresh.add(state.name());
        fresh.add(state.tierText().copy().formatted(Formatting.DARK_RED));
        if (state.isCleared()) {
            fresh.add(Text.translatable("block.dndclasses.adventurers_signpost.cleared").formatted(Formatting.DARK_GREEN));
        }
        if (!fresh.equals(this.lines)) {
            this.setLines(fresh);
        }
    }

    /** All the lines on one line, for the action bar. */
    public Text joined() {
        MutableText out = Text.empty();
        for (int i = 0; i < this.lines.size(); i++) {
            if (i > 0) {
                out.append(Text.literal(" - ").formatted(Formatting.GRAY));
            }
            out.append(this.lines.get(i));
        }
        return out;
    }

    private void changed() {
        this.markDirty();
        if (this.world != null && !this.world.isClient) {
            this.world.updateListeners(this.pos, this.getCachedState(), this.getCachedState(), Block.NOTIFY_ALL);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        NbtList list = new NbtList();
        for (Text line : this.lines) {
            list.add(NbtString.of(Text.Serializer.toJson(line)));
        }
        nbt.put(LINES, list);
        if (this.dungeon != null) {
            nbt.putLong(DUNGEON, this.dungeon);
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.lines.clear();
        NbtList list = nbt.getList(LINES, NbtElement.STRING_TYPE);
        for (int i = 0; i < list.size(); i++) {
            Text line = Text.Serializer.fromLenientJson(list.getString(i));
            if (line != null) {
                this.lines.add(line);
            }
        }
        this.dungeon = nbt.contains(DUNGEON) ? nbt.getLong(DUNGEON) : null;
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return this.createNbt();
    }
}
