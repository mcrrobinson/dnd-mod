package mattonfire.dnd.dm.encounter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;

import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.random.Random;

/**
 * A premade group of mobs, loaded from {@code data/<namespace>/encounters/<id>.json}. Shared by the DM toolkit
 * ({@code /dm encounter spawn}) and anything else that wants to drop a group of mobs (dungeon rooms); see
 * {@code docs/systems/dungeon-master.md} for the format.
 *
 * <pre>{@code
 * {
 *   "name": "Goblin Patrol",            // shown in chat; defaults to the id
 *   "difficulty": "medium",             // easy | medium | hard | deadly; informational
 *   "spread": 3,                        // blocks from the centre the mobs may stand; default 3
 *   "loot": true,                       // false: no drops, no XP, no class XP or bounty credit; default true
 *   "spawns": [
 *     {"entity": "dndclasses:goblin_warrior", "count": [2, 3]},          // count: n or [min, max]; default 1
 *     {"entity": "dndclasses:goblin_warlord", "chance": 0.2,             // chance the entry spawns at all; default 1
 *      "nbt": "{PersistenceRequired:1b}",                                // SNBT string or JSON object; optional
 *      "initialize": true,                                              // vanilla spawn setup (gear, variants); default true
 *      "tags": ["my_pack.leader"]}                                      // extra command tags; optional
 *   ],
 *   "chests": [{"loot_table": "dndclasses:chests/dwarven_fortress_treasury", "count": 2}]  // optional
 * }
 * }</pre>
 */
public record Encounter(Identifier id, String name, String difficulty, int spread, boolean loot, List<Spawn> spawns,
        List<Chest> chests) {

    public static final int DEFAULT_SPREAD = 3;

    /** One line of {@code spawns}. */
    public record Spawn(EntityType<?> entity, int min, int max, float chance, NbtCompound nbt, boolean initialize,
            List<String> tags) {
        /** How many to place this time: 0 if the {@code chance} roll fails. */
        public int roll(Random random) {
            if (this.chance < 1.0F && random.nextFloat() >= this.chance) {
                return 0;
            }
            return this.min + (this.max > this.min ? random.nextInt(this.max - this.min + 1) : 0);
        }
    }

    /** One line of {@code chests}: chests with an un-rolled loot table (so Lockpicking treats them as locked). */
    public record Chest(Identifier lootTable, int count) {
    }

    public static Encounter parse(Identifier id, JsonObject json) {
        String name = JsonHelper.getString(json, "name", id.getPath());
        String difficulty = JsonHelper.getString(json, "difficulty", "medium");
        int spread = Math.max(0, JsonHelper.getInt(json, "spread", DEFAULT_SPREAD));
        boolean loot = JsonHelper.getBoolean(json, "loot", true);

        List<Spawn> spawns = new ArrayList<>();
        for (JsonElement element : JsonHelper.getArray(json, "spawns")) {
            spawns.add(parseSpawn(JsonHelper.asObject(element, "spawn")));
        }
        if (spawns.isEmpty()) {
            throw new JsonParseException("encounter has no spawns");
        }
        List<Chest> chests = new ArrayList<>();
        for (JsonElement element : JsonHelper.getArray(json, "chests", new JsonArray())) {
            JsonObject chest = JsonHelper.asObject(element, "chest");
            chests.add(new Chest(new Identifier(JsonHelper.getString(chest, "loot_table")),
                    Math.max(1, JsonHelper.getInt(chest, "count", 1))));
        }
        return new Encounter(id, name, difficulty, spread, loot, List.copyOf(spawns), List.copyOf(chests));
    }

    /** One {@code spawns} entry; dungeon encounter pools ({@code DungeonEncounterPools}) use the same format. */
    public static Spawn parseSpawn(JsonObject json) {
        Identifier entityId = new Identifier(JsonHelper.getString(json, "entity"));
        Optional<EntityType<?>> type = Registries.ENTITY_TYPE.getOrEmpty(entityId);
        if (type.isEmpty()) {
            throw new JsonParseException("unknown entity " + entityId);
        }
        int min = 1;
        int max = 1;
        if (json.has("count")) {
            JsonElement count = json.get("count");
            if (count.isJsonArray()) {
                JsonArray range = count.getAsJsonArray();
                min = range.get(0).getAsInt();
                max = range.size() > 1 ? range.get(1).getAsInt() : min;
            } else {
                min = max = count.getAsInt();
            }
        }
        if (min < 0 || max < min) {
            throw new JsonParseException("bad count for " + entityId);
        }
        float chance = JsonHelper.getFloat(json, "chance", 1.0F);
        NbtCompound nbt = new NbtCompound();
        if (json.has("nbt")) {
            nbt = parseNbt(json.get("nbt"));
        }
        boolean initialize = JsonHelper.getBoolean(json, "initialize", true);
        List<String> tags = new ArrayList<>();
        for (JsonElement tag : JsonHelper.getArray(json, "tags", new JsonArray())) {
            tags.add(tag.getAsString());
        }
        return new Spawn(type.get(), min, max, chance, nbt, initialize, List.copyOf(tags));
    }

    private static NbtCompound parseNbt(JsonElement json) {
        if (json.isJsonPrimitive()) {
            try {
                return StringNbtReader.parse(json.getAsString());
            } catch (CommandSyntaxException e) {
                throw new JsonParseException("bad nbt: " + e.getMessage());
            }
        }
        NbtElement nbt = new Dynamic<>(JsonOps.INSTANCE, json).convert(NbtOps.INSTANCE).getValue();
        if (!(nbt instanceof NbtCompound compound)) {
            throw new JsonParseException("nbt must be an object");
        }
        return compound;
    }

    /** Total mobs at the top of every count range, for listings. */
    public String sizeLabel() {
        int min = 0;
        int max = 0;
        for (Spawn spawn : this.spawns) {
            if (spawn.chance() >= 1.0F) {
                min += spawn.min();
            }
            max += spawn.max();
        }
        return min == max ? Integer.toString(min) : min + "-" + max;
    }
}
