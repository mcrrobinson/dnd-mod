package mattonfire.dnd.classes.Obstacles;

import java.util.List;

import mattonfire.dnd.classes.Obstacles.types.GuardianBeastType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.StructureWorldAccess;
import org.jetbrains.annotations.Nullable;

/**
 * Worldgen helpers for the obstacles placed in the mod's and vanilla structures (on top of {@link ObstaclePlacer}).
 * Like the placer, everything here only writes inside {@code chunkBox}, so a structure built once per chunk
 * writes each block once.
 */
public final class StructureObstacles {
    private static final ThreadLocal<StructureStart> PLACING = new ThreadLocal<>();

    private StructureObstacles() {
    }

    /**
     * The structure start whose pieces this thread is generating right now, or null. Set by
     * {@code StructureStartPlacingMixin} round {@code StructureStart.place}.
     */
    @Nullable
    public static StructureStart placing() {
        return PLACING.get();
    }

    public static void setPlacing(@Nullable StructureStart start) {
        if (start == null) {
            PLACING.remove();
        } else {
            PLACING.set(start);
        }
    }

    /** A group seed that only depends on where the group is, so every chunk of a structure agrees on it. */
    public static long seedAt(BlockPos pos) {
        return MathHelper.hashCode(pos.getX(), pos.getY(), pos.getZ()) * 0x9E3779B97F4A7C15L + 0x0B57AC1EL;
    }

    /**
     * A Guardian Beast post at {@code post} with {@code beasts} beasts of {@code type} beside it, already bound to
     * it (tagged and listed in the post's {@code Data.Beasts}), so they can't wander off unbound before a player
     * first comes near. The beasts are spawned from the chunk that holds the post, at the first {@code spots} that
     * chunk holds too, else on top of the post. Returns whether the post was in this chunk.
     */
    public static boolean guardianPost(StructureWorldAccess world, BlockBox chunkBox, BlockPos post,
            EntityType<? extends MobEntity> type, List<BlockPos> spots, int beasts, Tier tier, boolean critical) {
        if (!ObstaclePlacer.place(world, chunkBox, post, NatureSongObstacles.GUARDIAN_BEAST, tier, critical,
                seedAt(post))) {
            return false;
        }
        String tag = GuardianBeastType.TAG_PREFIX + post.getX() + "_" + post.getY() + "_" + post.getZ();
        NbtList bound = new NbtList();
        int spot = 0;
        for (int i = 0; i < beasts; i++) {
            BlockPos at = post.up();
            while (spot < spots.size()) {
                BlockPos candidate = spots.get(spot++);
                if (chunkBox.contains(candidate)) {
                    at = candidate;
                    break;
                }
            }
            MobEntity beast = type.create(world.toServerWorld());
            if (beast == null) {
                continue;
            }
            beast.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5,
                    world.getRandom().nextFloat() * 360.0f, 0.0f);
            beast.initialize(world, world.getLocalDifficulty(at), SpawnReason.STRUCTURE, null, null);
            beast.addCommandTag(tag);
            beast.setPersistent();
            world.spawnEntityAndPassengers(beast);
            bound.add(NbtHelper.fromUuid(beast.getUuid()));
        }
        if (world.getBlockEntity(post) instanceof ObstacleBlockEntity be && !bound.isEmpty()) {
            be.data().put(GuardianBeastType.BEASTS_KEY, bound);
            be.markDirty();
        }
        return true;
    }
}
