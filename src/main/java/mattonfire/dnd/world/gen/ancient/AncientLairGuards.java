package mattonfire.dnd.world.gen.ancient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import mattonfire.dnd.classes.DnDClasses;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MarkerEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.ServerWorldAccess;
import org.jetbrains.annotations.Nullable;

/**
 * The lair's kobold guards (design 10, 6.5), kept as data so the lair needs nothing from the kobold ticket. Its
 * pieces leave guard posts: invisible markers tagged {@value #POST_TAG}, with {@code data:{Role:"warrior"|"slinger"|
 * "trapmaker"|"leader", Lair:{X,Y,Z}}}. When a loaded post's entity type tag {@code #dndclasses:lair_guards/<role>}
 * has anything in it, and the difficulty isn't Peaceful, a random type from the tag takes the post: it's tagged
 * {@value #GUARD_TAG}, made persistent and kept near the post, and the marker goes. Until then (no kobolds yet) the
 * posts wait, so lairs generated before the kobolds arrive still get their guards later.
 * <p>
 * Posts are checked as they load and every {@value #CHECK_INTERVAL} ticks.
 */
public final class AncientLairGuards {
    public static final String POST_TAG = "dndclasses.lair_guard_post";
    public static final String GUARD_TAG = "dndclasses.lair_guard";
    public static final int CHECK_INTERVAL = 200;
    /** How far a guard strays from its post. */
    public static final int GUARD_RANGE = 12;

    private static final Set<MarkerEntity> POSTS = new HashSet<>();
    private static final Set<MarkerEntity> NEW = new HashSet<>();

    private AncientLairGuards() {
    }

    static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof MarkerEntity marker && marker.getCommandTags().contains(POST_TAG)) {
                POSTS.add(marker);
                NEW.add(marker);
            }
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof MarkerEntity marker) {
                POSTS.remove(marker);
                NEW.remove(marker);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(AncientLairGuards::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            POSTS.clear();
            NEW.clear();
        });
    }

    /** The entity type tag a post of {@code role} raises its guard from. */
    public static TagKey<EntityType<?>> tag(String role) {
        return TagKey.of(RegistryKeys.ENTITY_TYPE, new Identifier(DnDClasses.MOD_ID, "lair_guards/" + role));
    }

    /** Leaves a guard post for a {@code role} kobold at {@code pos}, belonging to the lair centred on {@code lair}. */
    public static void placePost(ServerWorldAccess world, BlockPos pos, String role, BlockPos lair) {
        MarkerEntity marker = EntityType.MARKER.create(world.toServerWorld());
        if (marker == null) {
            return;
        }
        marker.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        marker.addCommandTag(POST_TAG);
        NbtCompound data = new NbtCompound();
        data.putString("Role", role);
        data.put("Lair", NbtHelper.fromBlockPos(lair));
        setData(marker, data);
        world.spawnEntity(marker);
    }

    /** A marker's {@code data} compound (MarkerEntity keeps it private; it round-trips through its NBT). */
    public static NbtCompound data(MarkerEntity marker) {
        return marker.writeNbt(new NbtCompound()).getCompound("data");
    }

    private static void setData(MarkerEntity marker, NbtCompound data) {
        NbtCompound nbt = marker.writeNbt(new NbtCompound());
        nbt.put("data", data);
        marker.readNbt(nbt);
    }

    private static void tick(MinecraftServer server) {
        boolean all = server.getTicks() % CHECK_INTERVAL == 0;
        if (POSTS.isEmpty() || !all && NEW.isEmpty()) {
            return;
        }
        List<MarkerEntity> due = new ArrayList<>(all ? POSTS : NEW);
        NEW.clear();
        for (MarkerEntity marker : due) {
            if (marker.isRemoved() || !(marker.world instanceof ServerWorld world)) {
                POSTS.remove(marker);
                continue;
            }
            if (raise(world, marker) != null) {
                POSTS.remove(marker);
            }
        }
    }

    /** Raises the post's guard if it can; returns it, or null if the post has to wait. */
    @Nullable
    public static MobEntity raise(ServerWorld world, MarkerEntity marker) {
        if (world.getDifficulty() == Difficulty.PEACEFUL) {
            return null;
        }
        NbtCompound data = data(marker);
        String role = data.getString("Role");
        Optional<RegistryEntryList.Named<EntityType<?>>> types = Registries.ENTITY_TYPE.getEntryList(tag(role));
        if (types.isEmpty() || types.get().size() == 0) {
            return null;
        }
        RegistryEntryList.Named<EntityType<?>> list = types.get();
        RegistryEntry<EntityType<?>> pick = list.get(world.random.nextInt(list.size()));
        Entity entity = pick.value().create(world);
        BlockPos pos = marker.getBlockPos();
        if (!(entity instanceof MobEntity guard)) {
            DnDClasses.LOGGER.warn("[LairGuards] {} in #dndclasses:lair_guards/{} isn't a mob", pick.getKey()
                    .map(k -> k.getValue().toString()).orElse("?"), role);
            if (entity != null) {
                entity.discard();
            }
            return null;
        }
        guard.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                world.random.nextFloat() * 360.0F, 0.0F);
        guard.initialize(world, world.getLocalDifficulty(pos), SpawnReason.STRUCTURE, null, null);
        guard.addCommandTag(GUARD_TAG);
        guard.setPersistent();
        if (guard instanceof PathAwareEntity walker) {
            walker.setPositionTarget(pos, GUARD_RANGE);
        }
        world.spawnEntityAndPassengers(guard);
        DnDClasses.LOGGER.info("[LairGuards] a {} ({}) takes the guard post at {}", pick.getKey()
                .map(k -> k.getValue().toString()).orElse("?"), role, pos.toShortString());
        marker.discard();
        return guard;
    }
}
