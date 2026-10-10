package mattonfire.dnd.faction;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Race.DndRace;
import mattonfire.dnd.classes.Race.RaceLifecycle;
import mattonfire.dnd.dm.DungeonMaster;
import mattonfire.dnd.classes.IEntityDataSaver;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Per-player faction reputation, -1000 to 1000 per faction. Stored in the player's persistent data
 * ({@code DndReputation}: faction id to value, {@code DndRepCaps}: today's capped gains), which is
 * saved with the player and copied over on death like the class data.
 *
 * <p>Changes go through {@link #change}: a batch of deltas for one event, applied with daily caps,
 * rivals and clamping, shown as one action-bar line, with a chat line on a tier change and a sync
 * to the client.
 */
public final class Reputation {
    public static final int MIN = -1000;
    public static final int MAX = 1000;
    public static final Identifier SYNC_PACKET_ID = new Identifier(DnDClasses.MOD_ID, "reputation_sync");
    static final String KEY = "DndReputation";
    static final String CAPS_KEY = "DndRepCaps";

    /**
     * The player's race id for {@code start_by_race} ({@code dndclasses:dwarf}), or null without a race
     * or with {@code dndRaces} off.
     */
    public static Function<ServerPlayerEntity, Identifier> raceOf = player -> {
        DndRace race = RaceLifecycle.activeRaceOf(player);
        return race == DndRace.NONE ? null : new Identifier(DnDClasses.MOD_ID, race.id());
    };

    /** Players whose actions don't change reputation: Dungeon Masters. /rep set still works on them. */
    public static Predicate<ServerPlayerEntity> ignored = DungeonMaster::isDm;

    /** What caused a change. Rivals only react to gains from sources that allow it. */
    public enum Source {
        KILL(false), HIT(false), RAID(false), BOUNTY(true), TRADE(true), THEFT(false), QUEST(true), COMMAND(false);

        public final boolean rivals;

        Source(boolean rivals) {
            this.rivals = rivals;
        }
    }

    /** Daily caps on gains (reset each in-game day). */
    public enum Cap {
        NONE, KILL, TRADE
    }

    private Reputation() {
    }

    // ---- Reading ----

    public static int get(ServerPlayerEntity player, Faction faction) {
        NbtCompound stored = data(player).getCompound(KEY);
        String key = faction.id().toString();
        return stored.contains(key) ? stored.getInt(key) : startOf(player, faction);
    }

    public static ReputationTier tier(ServerPlayerEntity player, Faction faction) {
        return ReputationTier.of(get(player, faction));
    }

    /** The first faction {@code entity} belongs to, or null. */
    @Nullable
    public static Faction factionOf(Entity entity) {
        for (Faction faction : Factions.all()) {
            if (faction.isMember(entity)) {
                return faction;
            }
        }
        return null;
    }

    private static int startOf(ServerPlayerEntity player, Faction faction) {
        Identifier race = raceOf.apply(player);
        if (race != null && faction.startByRace().containsKey(race)) {
            return faction.startByRace().get(race);
        }
        return faction.start();
    }

    // ---- Changing ----

    public static Change change(ServerPlayerEntity player, Source source) {
        return new Change(player, source);
    }

    /** One faction, no cap: quests, bounties and the like. */
    public static void add(ServerPlayerEntity player, Faction faction, int delta, Source source) {
        change(player, source).add(faction, delta).apply();
    }

    /** Sets the value outright (the /rep command). */
    public static void set(ServerPlayerEntity player, Faction faction, int value) {
        int before = get(player, faction);
        store(player, faction, value);
        int after = get(player, faction);
        if (after != before) {
            actionBar(player, Map.of(faction, after - before));
            announceTier(player, faction, before, after);
            sync(player);
        }
    }

    /** A batch of deltas from one event. */
    public static final class Change {
        private final ServerPlayerEntity player;
        private final Source source;
        private final List<Entry> entries = new ArrayList<>();

        private Change(ServerPlayerEntity player, Source source) {
            this.player = player;
            this.source = source;
        }

        public Change add(@Nullable Faction faction, int delta) {
            return this.add(faction, delta, Cap.NONE, 0);
        }

        /** {@code cap} limits this kind of gain per faction per day ({@code limit} 0 = none). */
        public Change add(@Nullable Faction faction, int delta, Cap cap, int limit) {
            if (faction != null && delta != 0) {
                this.entries.add(new Entry(faction, delta, cap, limit));
            }
            return this;
        }

        public void apply() {
            if (this.entries.isEmpty() || ignored.test(this.player)) {
                return;
            }
            NbtCompound caps = caps(this.player);
            Map<Faction, Integer> deltas = new LinkedHashMap<>();
            for (Entry entry : this.entries) {
                int delta = entry.delta;
                if (delta > 0 && entry.cap != Cap.NONE && entry.limit > 0) {
                    String capKey = entry.faction.id() + "/" + entry.cap.name().toLowerCase();
                    int used = caps.getInt(capKey);
                    delta = Math.min(delta, entry.limit - used);
                    if (delta <= 0) {
                        continue;
                    }
                    caps.putInt(capKey, used + delta);
                }
                deltas.merge(entry.faction, delta, Integer::sum);
                // Rivals lose a share of a gain; computed from the base delta, so rivals never chain.
                if (delta > 0 && this.source.rivals) {
                    for (Map.Entry<Identifier, Double> rival : entry.faction.rivals().entrySet()) {
                        Faction other = Factions.get(rival.getKey());
                        int loss = (int) Math.round(delta * rival.getValue());
                        if (other != null && loss != 0) {
                            deltas.merge(other, loss, Integer::sum);
                        }
                    }
                }
            }
            Map<Faction, Integer> applied = new LinkedHashMap<>();
            for (Map.Entry<Faction, Integer> entry : deltas.entrySet()) {
                Faction faction = entry.getKey();
                int before = get(this.player, faction);
                store(this.player, faction, before + entry.getValue());
                int after = get(this.player, faction);
                if (after != before) {
                    applied.put(faction, after - before);
                    announceTier(this.player, faction, before, after);
                }
            }
            if (!applied.isEmpty()) {
                actionBar(this.player, applied);
                sync(this.player);
            }
        }

        private record Entry(Faction faction, int delta, Cap cap, int limit) {
        }
    }

    static void store(ServerPlayerEntity player, Faction faction, int value) {
        NbtCompound data = data(player);
        NbtCompound stored = data.getCompound(KEY);
        stored.putInt(faction.id().toString(), Math.max(MIN, Math.min(MAX, value)));
        data.put(KEY, stored);
    }

    // ---- Daily caps and decay ----

    static long today(ServerPlayerEntity player) {
        return player.getServer().getOverworld().getTimeOfDay() / 24000L;
    }

    /** Today's cap counters; reset when the day changes. */
    private static NbtCompound caps(ServerPlayerEntity player) {
        NbtCompound data = data(player);
        NbtCompound caps = data.getCompound(CAPS_KEY);
        long today = today(player);
        if (!caps.contains("Day") || caps.getLong("Day") != today) {
            long decayDay = caps.contains("DecayDay") ? caps.getLong("DecayDay") : today;
            caps = new NbtCompound();
            caps.putLong("Day", today);
            caps.putLong("DecayDay", decayDay);
        }
        data.put(CAPS_KEY, caps);
        return caps;
    }

    /**
     * Grudges fade: each new day moves negative standing up by the faction's {@code decay_per_day},
     * never past 0. Called every few seconds per player.
     */
    static void decay(ServerPlayerEntity player) {
        NbtCompound caps = caps(player);
        long today = today(player);
        long last = caps.getLong("DecayDay");
        caps.putLong("DecayDay", today);
        if (today <= last) {
            // Same day, or the clock was turned back.
            return;
        }
        long days = Math.min(today - last, 400L);
        boolean changed = false;
        for (Faction faction : Factions.all()) {
            int value = get(player, faction);
            if (faction.decayPerDay() > 0 && value < 0) {
                store(player, faction, (int) Math.min(0L, value + days * faction.decayPerDay()));
                changed = true;
            }
        }
        if (changed) {
            sync(player);
        }
    }

    // ---- Feedback and sync ----

    private static void actionBar(ServerPlayerEntity player, Map<Faction, Integer> deltas) {
        MutableText line = Text.empty();
        boolean first = true;
        for (Map.Entry<Faction, Integer> entry : deltas.entrySet()) {
            if (!first) {
                line.append(Text.literal(", ").formatted(Formatting.GRAY));
            }
            first = false;
            int delta = entry.getValue();
            line.append(Text.literal((delta > 0 ? "+" : "") + delta + " ")
                    .formatted(delta > 0 ? Formatting.GREEN : Formatting.RED));
            line.append(entry.getKey().displayName());
        }
        player.sendMessage(line, true);
    }

    private static void announceTier(ServerPlayerEntity player, Faction faction, int before, int after) {
        ReputationTier was = ReputationTier.of(before);
        ReputationTier now = ReputationTier.of(after);
        if (was == now) {
            return;
        }
        player.sendMessage(Text.translatable("faction.dndclasses.tier_change", faction.displayName(), now.displayName()), false);
        boolean up = now.ordinal() > was.ordinal();
        player.playSound(up ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(),
                SoundCategory.PLAYERS, up ? 0.5F : 1.0F, up ? 1.2F : 0.6F);
    }

    /** Sends every faction's name, colour and the player's value. */
    public static void sync(ServerPlayerEntity player) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(Factions.all().size());
        for (Faction faction : Factions.all()) {
            buf.writeIdentifier(faction.id());
            buf.writeString(faction.name());
            buf.writeInt(faction.color());
            buf.writeInt(get(player, faction));
        }
        ServerPlayNetworking.send(player, SYNC_PACKET_ID, buf);
    }

    private static NbtCompound data(ServerPlayerEntity player) {
        return ((IEntityDataSaver) player).getPersistentData();
    }
}
