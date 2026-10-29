package mattonfire.dnd.magic.items;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Items.ScheduledBlockRestore;
import mattonfire.dnd.classes.Obstacles.ObstacleInteractions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.PersistentState;

/**
 * Portable Holes laid on walls, per dimension ({@code dndclasses_portable_hole_passages}). Each passage keeps the
 * original block states it replaced. It's saved before any block changes, closes after
 * {@value PortableHoleItem#PASSAGE_TICKS} ticks (or early), puts every block back exactly, and pushes anyone
 * inside out to the nearer end. Passages still open when the server stops are restored when it starts again.
 */
public class PortableHolePassages extends PersistentState {
    private static final String ID = "dndclasses_portable_hole_passages";
    public static final int MAX_DEPTH = 6;

    /** One laid hole. {@code front} is the spot in front of the face it was laid on, {@code back} beyond its end. */
    static final class Passage {
        UUID hole;
        UUID owner;
        long closesAt;
        Vec3d front;
        Vec3d back;
        boolean through;
        final List<BlockPos> positions = new ArrayList<>();
        final List<BlockState> originals = new ArrayList<>();
        final List<Integer> depths = new ArrayList<>();
        int depth;
    }

    /** Why a hole can't be laid, or the opened passage. */
    public record Result(@Nullable Passage passage, @Nullable String refusal) {
    }

    private final List<Passage> passages = new ArrayList<>();

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            PortableHolePassages state = peek(world);
            if (state != null && !state.passages.isEmpty())
                state.tick(world);
        });
        // Passages left open by a stop or a crash: put the walls back at once
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                PortableHolePassages state = get(world);
                if (state.passages.isEmpty())
                    continue;
                DnDClasses.LOGGER.info("[Magic] restoring {} Portable Hole passage(s) in {}", state.passages.size(),
                        world.getRegistryKey().getValue());
                for (Passage passage : List.copyOf(state.passages))
                    state.close(world, passage, "server start");
            }
        });
    }

    public static PortableHolePassages get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(PortableHolePassages::fromNbt, PortableHolePassages::new,
                ID);
    }

    private static @Nullable PortableHolePassages peek(ServerWorld world) {
        return world.getPersistentStateManager().get(PortableHolePassages::fromNbt, ID);
    }

    // ---- Opening ----

    /**
     * Lays the hole on the face {@code side} of {@code clicked}: {@code base} is the passage's lower block in the
     * face (the clicked block or the one under it, so a wall passage is at the player's height).
     */
    public Result open(ServerWorld world, PlayerEntity player, UUID hole, BlockPos base, Direction side) {
        Direction in = side.getOpposite();
        boolean vertical = in.getAxis() == Direction.Axis.Y;
        if (!player.getAbilities().allowModifyWorld)
            return new Result(null, "adventure");
        Passage passage = new Passage();
        passage.hole = hole;
        passage.owner = player.getUuid();
        for (int d = 0; d < MAX_DEPTH; d++) {
            BlockPos lower = base.offset(in, d);
            List<BlockPos> slice = vertical ? List.of(lower) : List.of(lower, lower.up());
            boolean anySolid = false;
            boolean blocked = false;
            for (BlockPos pos : slice) {
                BlockState state = world.getBlockState(pos);
                if (!isSolid(state))
                    continue;
                anySolid = true;
                if (isBlocked(world, player, pos, state))
                    blocked = true;
            }
            if (d == 0 && (!anySolid || blocked))
                return new Result(null, blocked ? "blocked" : "nothing");
            if (!anySolid) {
                passage.through = true;
                break;
            }
            if (blocked)
                break;
            for (BlockPos pos : slice) {
                BlockState state = world.getBlockState(pos);
                if (!isSolid(state))
                    continue;
                passage.positions.add(pos.toImmutable());
                passage.originals.add(state);
                passage.depths.add(d);
            }
            passage.depth = d + 1;
        }
        Vec3d inVec = Vec3d.of(in.getVector());
        Vec3d baseBottom = Vec3d.ofBottomCenter(base);
        passage.front = baseBottom.subtract(inVec);
        passage.back = baseBottom.add(inVec.multiply(passage.depth));
        if (vertical && in == Direction.DOWN) {
            passage.front = baseBottom.add(0, 1, 0);
            passage.back = baseBottom.add(0, -passage.depth, 0);
        }
        passage.closesAt = world.getTime() + PortableHoleItem.PASSAGE_TICKS;
        // Saved before the blocks change, so a crash can't lose the originals
        passages.add(passage);
        markDirty();
        BlockState hollow = PlanarItems.PORTABLE_HOLE_PASSAGE.getDefaultState();
        for (BlockPos pos : passage.positions)
            world.setBlockState(pos, hollow, ScheduledBlockRestore.FLAGS);
        DnDClasses.LOGGER.info("[Magic] {} laid a Portable Hole at {} facing {}: {} blocks, {} deep{}",
                player.getEntityName(), base.toShortString(), side.asString(), passage.positions.size(),
                passage.depth, passage.through ? ", through" : "");
        return new Result(passage, null);
    }

    /** A block the passage replaces: anything solid. Air, plants, torches and liquids stay as they are. */
    static boolean isSolid(BlockState state) {
        return !state.isAir() && state.getMaterial().isSolid() && !state.getMaterial().isReplaceable();
    }

    /** A solid block the passage can't take: unbreakable, a block entity, protected, or not yours to change. */
    static boolean isBlocked(ServerWorld world, PlayerEntity player, BlockPos pos, BlockState state) {
        return state.getHardness(world, pos) < 0 || state.hasBlockEntity() || state.isOf(PlanarItems.PORTABLE_HOLE_PASSAGE)
                || world.isOutOfHeightLimit(pos) || !world.getWorldBorder().contains(pos)
                || ObstacleInteractions.isProtected(world, pos) || !world.canPlayerModifyAt(player, pos)
                || !player.canModifyAt(world, pos);
    }

    // ---- Closing ----

    private void tick(ServerWorld world) {
        long now = world.getTime();
        Iterator<Passage> it = List.copyOf(passages).iterator();
        while (it.hasNext()) {
            Passage passage = it.next();
            if (now >= passage.closesAt)
                close(world, passage, "time");
        }
    }

    /** Closes the passage that has a block at {@code pos}, if any. */
    public boolean closeAt(ServerWorld world, BlockPos pos, String why) {
        for (Passage passage : List.copyOf(passages)) {
            if (passage.positions.contains(pos)) {
                close(world, passage, why);
                return true;
            }
        }
        return false;
    }

    /** Closes the hole's passage in any dimension. Returns whether one was open. */
    public static boolean closeHole(MinecraftServer server, UUID hole, String why) {
        boolean closed = false;
        for (ServerWorld world : server.getWorlds()) {
            PortableHolePassages state = peek(world);
            if (state == null)
                continue;
            for (Passage passage : List.copyOf(state.passages)) {
                if (passage.hole.equals(hole)) {
                    state.close(world, passage, why);
                    closed = true;
                }
            }
        }
        return closed;
    }

    /** Whether the hole is laid somewhere right now. */
    public static boolean isLaid(MinecraftServer server, UUID hole) {
        for (ServerWorld world : server.getWorlds()) {
            PortableHolePassages state = peek(world);
            if (state != null && state.passages.stream().anyMatch(p -> p.hole.equals(hole)))
                return true;
        }
        return false;
    }

    void close(ServerWorld world, Passage passage, String why) {
        if (!passages.remove(passage))
            return;
        pushOut(world, passage);
        int restored = 0;
        for (int i = 0; i < passage.positions.size(); i++) {
            BlockPos pos = passage.positions.get(i);
            if (world.getBlockState(pos).isOf(PlanarItems.PORTABLE_HOLE_PASSAGE)) {
                world.setBlockState(pos, passage.originals.get(i), ScheduledBlockRestore.FLAGS);
                restored++;
            }
        }
        markDirty();
        if (!passage.positions.isEmpty()) {
            BlockPos at = passage.positions.get(0);
            world.playSound(null, at, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, SoundCategory.BLOCKS, 1.0F, 0.6F);
            world.playSound(null, at, SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.BLOCKS, 0.08F, 2.0F);
        }
        DnDClasses.LOGGER.info("[Magic] Portable Hole passage closed ({}): {} blocks restored", why, restored);
    }

    /** Anything alive inside is moved out to the nearer end (the front if the back is a dead end). */
    private static void pushOut(ServerWorld world, Passage passage) {
        if (passage.positions.isEmpty())
            return;
        Box box = new Box(passage.positions.get(0));
        for (BlockPos pos : passage.positions)
            box = box.union(new Box(pos));
        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, box.contract(0.05), e -> e.isAlive())) {
            int depth = 0;
            double best = Double.MAX_VALUE;
            for (int i = 0; i < passage.positions.size(); i++) {
                double d = Vec3d.ofCenter(passage.positions.get(i)).squaredDistanceTo(entity.getPos());
                if (d < best) {
                    best = d;
                    depth = passage.depths.get(i);
                }
            }
            boolean back = passage.through && depth >= passage.depth / 2.0;
            Vec3d to = back ? passage.back : passage.front;
            if (entity.hasVehicle())
                entity.stopRiding();
            entity.requestTeleport(to.x, to.y, to.z);
            DnDClasses.LOGGER.info("[Magic] Portable Hole closing pushes {} out the {}", entity.getName().getString(),
                    back ? "back" : "front");
        }
    }

    // ---- Saving ----

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        for (Passage p : passages) {
            NbtCompound t = new NbtCompound();
            t.putUuid("Hole", p.hole);
            t.putUuid("Owner", p.owner);
            t.putLong("ClosesAt", p.closesAt);
            t.putBoolean("Through", p.through);
            t.putInt("Depth", p.depth);
            t.put("Front", vec(p.front));
            t.put("Back", vec(p.back));
            NbtList blocks = new NbtList();
            for (int i = 0; i < p.positions.size(); i++) {
                NbtCompound b = new NbtCompound();
                b.put("Pos", NbtHelper.fromBlockPos(p.positions.get(i)));
                b.put("State", NbtHelper.fromBlockState(p.originals.get(i)));
                b.putInt("D", p.depths.get(i));
                blocks.add(b);
            }
            t.put("Blocks", blocks);
            list.add(t);
        }
        nbt.put("Passages", list);
        return nbt;
    }

    private static NbtCompound vec(Vec3d v) {
        NbtCompound c = new NbtCompound();
        c.putDouble("X", v.x);
        c.putDouble("Y", v.y);
        c.putDouble("Z", v.z);
        return c;
    }

    private static Vec3d vec(NbtCompound c) {
        return new Vec3d(c.getDouble("X"), c.getDouble("Y"), c.getDouble("Z"));
    }

    private static PortableHolePassages fromNbt(NbtCompound nbt) {
        PortableHolePassages state = new PortableHolePassages();
        var lookup = Registries.BLOCK.getReadOnlyWrapper();
        NbtList list = nbt.getList("Passages", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound t = list.getCompound(i);
            Passage p = new Passage();
            p.hole = t.getUuid("Hole");
            p.owner = t.getUuid("Owner");
            p.closesAt = t.getLong("ClosesAt");
            p.through = t.getBoolean("Through");
            p.depth = t.getInt("Depth");
            p.front = vec(t.getCompound("Front"));
            p.back = vec(t.getCompound("Back"));
            NbtList blocks = t.getList("Blocks", NbtElement.COMPOUND_TYPE);
            for (int j = 0; j < blocks.size(); j++) {
                NbtCompound b = blocks.getCompound(j);
                p.positions.add(NbtHelper.toBlockPos(b.getCompound("Pos")));
                p.originals.add(NbtHelper.toBlockState(lookup, b.getCompound("State")));
                p.depths.add(b.getInt("D"));
            }
            state.passages.add(p);
        }
        return state;
    }
}
