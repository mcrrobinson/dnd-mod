package mattonfire.dnd.entity.hireling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mattonfire.dnd.entity.HirelingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Every hired hireling, by owner, kept with the overworld's saved data ({@code dndclasses_hirelings}). Each
 * record holds an NBT snapshot of the hireling (refreshed every 100 ticks and when it unloads), where it is,
 * its mode, and a generation number.
 *
 * <p>When the owner logs out, their followers are snapshotted and discarded ({@link Record#stashed}) and the
 * generation goes up; when the owner joins, they're recreated from the snapshot with the new generation. A
 * copy that later loads from disk with an older generation is a stale duplicate and is discarded
 * ({@link Hirelings#onLoad}). Waiting hirelings stay in the world.
 */
public final class HirelingRegistry extends net.minecraft.world.PersistentState {
    private static final String SAVE_ID = "dndclasses_hirelings";

    /** One hired hireling. */
    public static final class Record {
        public final UUID uuid;
        public NbtCompound snapshot = new NbtCompound();
        public RegistryKey<World> dimension = World.OVERWORLD;
        public BlockPos pos = BlockPos.ORIGIN;
        public boolean waiting;
        public int generation;
        /** Discarded at the owner's logout, to be recreated when they join. */
        public boolean stashed;
        /** Display name, kept for the list while it isn't loaded. */
        public String name = "";

        Record(UUID uuid) {
            this.uuid = uuid;
        }

        NbtCompound write() {
            NbtCompound nbt = new NbtCompound();
            nbt.putUuid("uuid", this.uuid);
            nbt.put("snapshot", this.snapshot);
            nbt.putString("dimension", this.dimension.getValue().toString());
            nbt.putLong("pos", this.pos.asLong());
            nbt.putBoolean("waiting", this.waiting);
            nbt.putInt("generation", this.generation);
            nbt.putBoolean("stashed", this.stashed);
            nbt.putString("name", this.name);
            return nbt;
        }

        static Record read(NbtCompound nbt) {
            Record record = new Record(nbt.getUuid("uuid"));
            record.snapshot = nbt.getCompound("snapshot");
            record.dimension = RegistryKey.of(RegistryKeys.WORLD, new Identifier(nbt.getString("dimension")));
            record.pos = BlockPos.fromLong(nbt.getLong("pos"));
            record.waiting = nbt.getBoolean("waiting");
            record.generation = nbt.getInt("generation");
            record.stashed = nbt.getBoolean("stashed");
            record.name = nbt.getString("name");
            return record;
        }
    }

    private final Map<UUID, List<Record>> byOwner = new HashMap<>();
    /** The day each owner last joined: days before it were offline days, which cost no wages. */
    private final Map<UUID, Long> joinDay = new HashMap<>();

    public static HirelingRegistry get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager()
                .getOrCreate(HirelingRegistry::fromNbt, HirelingRegistry::new, SAVE_ID);
    }

    public List<Record> of(UUID owner) {
        return this.byOwner.getOrDefault(owner, List.of());
    }

    public Map<UUID, List<Record>> all() {
        return this.byOwner;
    }

    @Nullable
    public Record find(UUID owner, UUID hireling) {
        for (Record record : this.of(owner)) {
            if (record.uuid.equals(hireling)) {
                return record;
            }
        }
        return null;
    }

    public Record add(UUID owner, HirelingEntity hireling) {
        Record record = new Record(hireling.getUuid());
        this.byOwner.computeIfAbsent(owner, key -> new ArrayList<>()).add(record);
        hireling.setGeneration(record.generation);
        this.update(record, hireling);
        return record;
    }

    public void remove(UUID owner, UUID hireling) {
        List<Record> records = this.byOwner.get(owner);
        if (records != null && records.removeIf(record -> record.uuid.equals(hireling))) {
            if (records.isEmpty()) {
                this.byOwner.remove(owner);
            }
            this.markDirty();
        }
    }

    /** Refreshes the record from the loaded hireling. */
    public void update(Record record, HirelingEntity hireling) {
        NbtCompound nbt = new NbtCompound();
        if (hireling.saveSelfNbt(nbt)) {
            record.snapshot = nbt;
        }
        record.dimension = hireling.getWorld().getRegistryKey();
        record.pos = hireling.getBlockPos();
        record.waiting = hireling.isWaiting();
        record.name = hireling.hirelingName();
        this.markDirty();
    }

    public long joinDay(UUID owner) {
        return this.joinDay.getOrDefault(owner, Long.MIN_VALUE);
    }

    public void setJoinDay(UUID owner, long day) {
        this.joinDay.put(owner, day);
        this.markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList owners = new NbtList();
        for (Map.Entry<UUID, List<Record>> entry : this.byOwner.entrySet()) {
            NbtCompound owner = new NbtCompound();
            owner.putUuid("owner", entry.getKey());
            NbtList records = new NbtList();
            for (Record record : entry.getValue()) {
                records.add(record.write());
            }
            owner.put("hirelings", records);
            owners.add(owner);
        }
        nbt.put("owners", owners);
        NbtList days = new NbtList();
        for (Map.Entry<UUID, Long> entry : this.joinDay.entrySet()) {
            NbtCompound day = new NbtCompound();
            day.putUuid("owner", entry.getKey());
            day.putLong("day", entry.getValue());
            days.add(day);
        }
        nbt.put("joinDays", days);
        return nbt;
    }

    private static HirelingRegistry fromNbt(NbtCompound nbt) {
        HirelingRegistry registry = new HirelingRegistry();
        for (NbtElement element : nbt.getList("owners", NbtElement.COMPOUND_TYPE)) {
            NbtCompound owner = (NbtCompound) element;
            List<Record> records = new ArrayList<>();
            for (NbtElement record : owner.getList("hirelings", NbtElement.COMPOUND_TYPE)) {
                records.add(Record.read((NbtCompound) record));
            }
            if (!records.isEmpty()) {
                registry.byOwner.put(owner.getUuid("owner"), records);
            }
        }
        for (NbtElement element : nbt.getList("joinDays", NbtElement.COMPOUND_TYPE)) {
            NbtCompound day = (NbtCompound) element;
            registry.joinDay.put(day.getUuid("owner"), day.getLong("day"));
        }
        return registry;
    }
}
