package mattonfire.dnd.dungeon;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.dm.encounter.Encounter;
import mattonfire.dnd.entity.boss.StructureBosses;
import mattonfire.dnd.world.gen.dungeon.DungeonTheme;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

/**
 * The monsters each dungeon theme's rooms can call up, per Challenge tier, loaded from
 * {@code data/<namespace>/dungeon_encounters/<theme>.json} (reloaded with {@code /reload}). Each
 * pool entry is a DM-toolkit encounter spawn ({@link Encounter#parseSpawn}: {@code entity},
 * {@code nbt}, {@code initialize}, {@code tags}) plus a {@code weight} and an optional
 * {@code threat} that overrides {@link #DEFAULT_THREAT}.
 *
 * <pre>{@code
 * {
 *   "boss": "lich",                                     // StructureBosses.BossType
 *   "champion": {"entity": "dndclasses:gelatinous_cube", "name": "dungeon.dndclasses.champion.ossuary_cube",
 *                "absorbed_loot": "minecraft:chests/simple_dungeon", "absorbed_stacks": 3},
 *   "tiers": {
 *     "1": [{"entity": "minecraft:zombie", "weight": 10}, {"entity": "minecraft:skeleton", "weight": 8}],
 *     "3": [{"entity": "minecraft:wither_skeleton", "weight": 3, "threat": 3}]
 *   }
 * }
 * }</pre>
 * A tier with no list uses the nearest lower one.
 */
public final class DungeonEncounterPools {
    public static final String FOLDER = "dungeon_encounters";

    /** How much of a room's budget one mob uses up, when its pool entry doesn't say. Anything else costs 1. */
    public static final Map<String, Double> DEFAULT_THREAT = Map.ofEntries(
            Map.entry("minecraft:zombie", 1.0), Map.entry("minecraft:husk", 1.0), Map.entry("minecraft:drowned", 1.0),
            Map.entry("minecraft:spider", 1.0), Map.entry("minecraft:cave_spider", 1.0), Map.entry("minecraft:wolf", 1.0),
            Map.entry("minecraft:skeleton", 1.5), Map.entry("minecraft:stray", 1.5),
            Map.entry("minecraft:witch", 2.0),
            Map.entry("minecraft:wither_skeleton", 3.0), Map.entry("minecraft:vindicator", 3.0),
            Map.entry("dndclasses:goblin_warrior", 3.0), Map.entry("dndclasses:mimic", 3.0),
            Map.entry("dndclasses:gelatinous_cube", 4.0),
            Map.entry("dndclasses:owlbear", 6.0));

    /** One monster a room can pick: what to spawn, how likely, and what it costs. */
    public record Entry(Encounter.Spawn spawn, int weight, double threat) {
    }

    /** The mid-boss: an ordinary mob made into a champion by {@link DungeonChampion}. */
    public record Champion(EntityType<?> entity, String nameKey, @Nullable Identifier absorbedLoot, int absorbedStacks) {
    }

    /** One theme's pool. */
    public record Pool(Map<Integer, List<Entry>> tiers, @Nullable Champion champion, StructureBosses.BossType boss) {
        /** The tier's list, or the nearest lower tier's; empty if there's none. */
        public List<Entry> forTier(int tier) {
            for (int t = tier; t >= 1; t--) {
                List<Entry> list = this.tiers.get(t);
                if (list != null && !list.isEmpty()) {
                    return list;
                }
            }
            return List.of();
        }
    }

    private static Map<String, Pool> pools = Map.of();

    private DungeonEncounterPools() {
    }

    public static void register() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return new Identifier(DnDClasses.MOD_ID, FOLDER);
            }

            @Override
            public void reload(ResourceManager manager) {
                load(manager);
            }
        });
    }

    @Nullable
    public static Pool get(DungeonTheme theme) {
        return pools.get(theme.id());
    }

    private static void load(ResourceManager manager) {
        Map<String, Pool> loaded = new TreeMap<>();
        for (Map.Entry<Identifier, Resource> entry : manager
                .findResources(FOLDER, path -> path.getPath().endsWith(".json")).entrySet()) {
            Identifier file = entry.getKey();
            String theme = file.getPath().substring(FOLDER.length() + 1, file.getPath().length() - ".json".length());
            try (Reader reader = entry.getValue().getReader()) {
                loaded.put(theme, parse(JsonHelper.asObject(JsonParser.parseReader(reader), "pool")));
            } catch (Exception e) {
                DnDClasses.LOGGER.error("[Dungeon] couldn't load encounter pool {}: {}", file, e.getMessage());
            }
        }
        pools = loaded;
        DnDClasses.LOGGER.info("[Dungeon] loaded encounter pools for {}", loaded.keySet());
    }

    static Pool parse(JsonObject json) {
        Map<Integer, List<Entry>> tiers = new HashMap<>();
        JsonObject tierJson = JsonHelper.getObject(json, "tiers");
        for (Map.Entry<String, JsonElement> tier : tierJson.entrySet()) {
            int t;
            try {
                t = Integer.parseInt(tier.getKey());
            } catch (NumberFormatException e) {
                throw new JsonParseException("tier keys are 1-4, not " + tier.getKey());
            }
            List<Entry> list = new ArrayList<>();
            for (JsonElement element : JsonHelper.asArray(tier.getValue(), "tier " + t)) {
                JsonObject spawn = JsonHelper.asObject(element, "entry");
                Encounter.Spawn parsed = Encounter.parseSpawn(spawn);
                String id = Registries.ENTITY_TYPE.getId(parsed.entity()).toString();
                double threat = JsonHelper.getDouble(spawn, "threat", DEFAULT_THREAT.getOrDefault(id, 1.0));
                list.add(new Entry(parsed, Math.max(1, JsonHelper.getInt(spawn, "weight", 1)), Math.max(0.1, threat)));
            }
            tiers.put(t, List.copyOf(list));
        }
        Champion champion = null;
        if (json.has("champion")) {
            JsonObject c = JsonHelper.getObject(json, "champion");
            Identifier id = new Identifier(JsonHelper.getString(c, "entity"));
            EntityType<?> type = Registries.ENTITY_TYPE.getOrEmpty(id)
                    .orElseThrow(() -> new JsonParseException("unknown champion " + id));
            champion = new Champion(type, JsonHelper.getString(c, "name", type.getTranslationKey()),
                    c.has("absorbed_loot") ? new Identifier(JsonHelper.getString(c, "absorbed_loot")) : null,
                    JsonHelper.getInt(c, "absorbed_stacks", 0));
        }
        StructureBosses.BossType boss = StructureBosses.BossType.byName(JsonHelper.getString(json, "boss", "lich"));
        return new Pool(tiers, champion, boss);
    }
}
