package mattonfire.dnd.dm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

/**
 * Who is running the session. Saved with the world (overworld data, {@code dndclasses_dm}):
 * <ul>
 * <li>DMs: players in DM mode ({@code /dm on}). They sit outside the game: no party XP or HUD
 * slot, no raid or boss-fight participation, no bounty credit, and mobs never target them.</li>
 * <li>Granted: non-ops allowed to use the safe {@code /dm} subset ({@code /dm grant}).</li>
 * <li>Veiled: DMs hidden from non-DM clients ({@code /dm veil}), see {@link DmVeil}.</li>
 * <li>Live encounters spawned with {@code /dm encounter spawn}, by number.</li>
 * </ul>
 */
public class DungeonMaster extends PersistentState {
    private static final String SAVE_ID = "dndclasses_dm";

    private final Set<UUID> dms = new HashSet<>();
    private final Set<UUID> granted = new HashSet<>();
    private final Set<UUID> veiled = new HashSet<>();
    private final Map<Integer, EncounterRecord> encounters = new LinkedHashMap<>();
    private int nextEncounter = 1;

    /** One spawned encounter: which file it came from and where, for {@code /dm encounter list}. */
    public record EncounterRecord(int number, Identifier encounter, String name, Identifier dimension, BlockPos pos,
            List<UUID> entities) {
    }

    public static DungeonMaster get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager()
                .getOrCreate(DungeonMaster::fromNbt, DungeonMaster::new, SAVE_ID);
    }

    @Nullable
    private static DungeonMaster of(@Nullable Entity entity) {
        if (entity == null || entity.world.isClient || entity.getServer() == null) {
            return null;
        }
        return get(entity.getServer());
    }

    /** True for a player in DM mode (server side only; always false on the client). */
    public static boolean isDm(@Nullable Entity entity) {
        DungeonMaster state = entity instanceof PlayerEntity ? of(entity) : null;
        return state != null && state.dms.contains(entity.getUuid());
    }

    /** True for a veiled DM: hidden from other players, untargetable, invulnerable, flying. */
    public static boolean isVeiled(@Nullable Entity entity) {
        DungeonMaster state = entity instanceof PlayerEntity ? of(entity) : null;
        return state != null && state.veiled.contains(entity.getUuid());
    }

    /** True for a player given the safe {@code /dm} subset with {@code /dm grant}. */
    public static boolean isGranted(@Nullable Entity entity) {
        DungeonMaster state = entity instanceof PlayerEntity ? of(entity) : null;
        return state != null && state.granted.contains(entity.getUuid());
    }

    /** Op (permission level 2) or granted: may run the safe {@code /dm} commands. */
    public static boolean canUse(ServerCommandSource source) {
        return source.hasPermissionLevel(2) || isGranted(source.getEntity());
    }

    public boolean isDm(UUID uuid) {
        return this.dms.contains(uuid);
    }

    public boolean isVeiled(UUID uuid) {
        return this.veiled.contains(uuid);
    }

    /** Turns DM mode on or off. Turning it off also lifts the veil (call {@link DmVeil#apply} after). */
    public boolean setDm(ServerPlayerEntity player, boolean on) {
        boolean changed = on ? this.dms.add(player.getUuid()) : this.dms.remove(player.getUuid());
        if (!on) {
            changed |= this.veiled.remove(player.getUuid());
        }
        this.markDirty();
        return changed;
    }

    /** Veils or unveils a player; veiling also turns DM mode on. */
    public boolean setVeiled(ServerPlayerEntity player, boolean on) {
        boolean changed = on ? this.veiled.add(player.getUuid()) : this.veiled.remove(player.getUuid());
        if (on) {
            this.dms.add(player.getUuid());
        }
        this.markDirty();
        return changed;
    }

    public boolean setGranted(UUID uuid, boolean on) {
        boolean changed = on ? this.granted.add(uuid) : this.granted.remove(uuid);
        this.markDirty();
        return changed;
    }

    public Set<UUID> getDms() {
        return Set.copyOf(this.dms);
    }

    public Set<UUID> getGranted() {
        return Set.copyOf(this.granted);
    }

    public Set<UUID> getVeiled() {
        return Set.copyOf(this.veiled);
    }

    // ------------------------------------------------------------ encounters

    public int nextEncounterNumber() {
        int number = this.nextEncounter++;
        this.markDirty();
        return number;
    }

    public void addEncounter(EncounterRecord record) {
        this.encounters.put(record.number(), record);
        this.markDirty();
    }

    @Nullable
    public EncounterRecord removeEncounter(int number) {
        EncounterRecord record = this.encounters.remove(number);
        this.markDirty();
        return record;
    }

    public List<EncounterRecord> getEncounters() {
        return new ArrayList<>(this.encounters.values());
    }

    public void clearEncounters() {
        this.encounters.clear();
        this.markDirty();
    }

    // ------------------------------------------------------------ save

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.put("Dms", uuids(this.dms));
        nbt.put("Granted", uuids(this.granted));
        nbt.put("Veiled", uuids(this.veiled));
        nbt.putInt("NextEncounter", this.nextEncounter);
        NbtList list = new NbtList();
        for (EncounterRecord record : this.encounters.values()) {
            NbtCompound tag = new NbtCompound();
            tag.putInt("Number", record.number());
            tag.putString("Encounter", record.encounter().toString());
            tag.putString("Name", record.name());
            tag.putString("Dimension", record.dimension().toString());
            tag.put("Pos", NbtHelper.fromBlockPos(record.pos()));
            tag.put("Entities", uuids(record.entities()));
            list.add(tag);
        }
        nbt.put("Encounters", list);
        return nbt;
    }

    private static DungeonMaster fromNbt(NbtCompound nbt) {
        DungeonMaster state = new DungeonMaster();
        readUuids(nbt.getList("Dms", NbtElement.INT_ARRAY_TYPE), state.dms);
        readUuids(nbt.getList("Granted", NbtElement.INT_ARRAY_TYPE), state.granted);
        readUuids(nbt.getList("Veiled", NbtElement.INT_ARRAY_TYPE), state.veiled);
        state.nextEncounter = Math.max(1, nbt.getInt("NextEncounter"));
        for (NbtElement element : nbt.getList("Encounters", NbtElement.COMPOUND_TYPE)) {
            NbtCompound tag = (NbtCompound) element;
            List<UUID> entities = new ArrayList<>();
            readUuids(tag.getList("Entities", NbtElement.INT_ARRAY_TYPE), entities);
            Identifier encounter = Identifier.tryParse(tag.getString("Encounter"));
            Identifier dimension = Identifier.tryParse(tag.getString("Dimension"));
            if (encounter == null || dimension == null) {
                continue;
            }
            EncounterRecord record = new EncounterRecord(tag.getInt("Number"), encounter, tag.getString("Name"),
                    dimension, NbtHelper.toBlockPos(tag.getCompound("Pos")), entities);
            state.encounters.put(record.number(), record);
        }
        return state;
    }

    private static NbtList uuids(java.util.Collection<UUID> uuids) {
        NbtList list = new NbtList();
        uuids.forEach(uuid -> list.add(NbtHelper.fromUuid(uuid)));
        return list;
    }

    private static void readUuids(NbtList list, java.util.Collection<UUID> into) {
        for (NbtElement element : list) {
            into.add(NbtHelper.toUuid(element));
        }
    }
}
