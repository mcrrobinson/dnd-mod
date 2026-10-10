package mattonfire.dnd.faction;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

/**
 * A faction loaded from {@code data/<ns>/factions/<id>.json}. Everything a faction does is in its
 * file, so a new faction (an elven court, an orc horde) is a data file and two tags, no code. See
 * {@code docs/systems/factions.md} for the format.
 *
 * <p>Entity "matchers" (members, kill overrides) are an entity type id ({@code dndclasses:hobbit}),
 * an entity type tag ({@code #dndclasses:faction/hobbits}) or a command tag
 * ({@code @dndclasses.role.dwarf_king}).
 */
public record Faction(
        Identifier id,
        String name,
        int color,
        Matcher members,
        @Nullable Settlements settlements,
        int start,
        Map<Identifier, Integer> startByRace,
        Map<Identifier, Double> rivals,
        int decayPerDay,
        int hitMember,
        int killMember,
        Map<Matcher, Integer> killMemberOverrides,
        int betrayal,
        Map<Identifier, Integer> enemyKills,
        Map<Matcher, Integer> enemyKillOverrides,
        int killCap,
        int raidWon,
        int raidDefeated,
        int bountyMinor,
        int bountyMajor,
        int trade,
        int tradeCap,
        int theftWitnessed) {

    private static final Set<String> FIELDS = Set.of("name", "color", "members", "settlements", "start",
            "start_by_race", "rivals", "decay_per_day", "hit_member", "kill_member", "kill_member_overrides",
            "betrayal", "enemy_kills", "enemy_kill_overrides", "kill_cap", "raid_won", "raid_defeated",
            "bounties", "trade", "trade_cap", "theft_witnessed");

    public Text displayName() {
        return Text.translatable(this.name).styled(style -> style.withColor(TextColor.fromRgb(this.color)));
    }

    public boolean isMember(Entity entity) {
        return this.members.matches(entity);
    }

    /** Whether a structure (e.g. {@code dndclasses:hobbit_village}) is one of this faction's settlements. */
    public boolean ownsSettlement(ServerWorld world, Identifier structureId) {
        if (this.settlements == null) {
            return false;
        }
        Registry<Structure> registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        return registry.getEntry(RegistryKey.of(RegistryKeys.STRUCTURE, structureId))
                .map(entry -> this.settlements.matches(entry))
                .orElse(false);
    }

    /** Whether {@code pos} is inside one of this faction's settlements. */
    public boolean isInSettlement(ServerWorld world, BlockPos pos) {
        if (this.settlements == null) {
            return false;
        }
        return this.settlements.tag != null
                ? world.getStructureAccessor().getStructureContaining(pos, this.settlements.tag).hasChildren()
                : world.getStructureAccessor().getStructureContaining(pos, this.settlements.key).hasChildren();
    }

    /** What killing {@code entity} (one of ours) costs: an override or the plain kill penalty. */
    public int killPenalty(Entity entity) {
        Integer override = lookup(this.killMemberOverrides, entity);
        return override != null ? override : this.killMember;
    }

    // ---- Parsing ----

    public static Faction parse(Identifier id, JsonObject json) {
        for (String key : json.keySet()) {
            if (!FIELDS.contains(key)) {
                throw new JsonParseException("unknown field \"" + key + "\"");
            }
        }
        String name = string(json, "name", "faction." + id.getNamespace() + "." + id.getPath().replace('/', '.'));
        int color = parseColor(string(json, "color", "#AAAAAA"));
        if (!json.has("members")) {
            throw new JsonParseException("missing \"members\"");
        }
        Matcher members = Matcher.parse(string(json, "members", null));
        if (members.commandTag == null && members.tag == null && members.type == null) {
            throw new JsonParseException("\"members\" matches nothing");
        }
        Settlements settlements = json.has("settlements") ? Settlements.parse(string(json, "settlements", null)) : null;
        int[] bounties = {0, 0};
        if (json.has("bounties")) {
            JsonObject object = object(json, "bounties");
            for (String key : object.keySet()) {
                int value = integer(object, key, 0);
                switch (key) {
                    case "minor" -> bounties[0] = value;
                    case "major" -> bounties[1] = value;
                    default -> throw new JsonParseException("unknown bounty tier \"" + key + "\"");
                }
            }
        }
        Map<Identifier, Double> rivals = new LinkedHashMap<>();
        if (json.has("rivals")) {
            JsonObject object = object(json, "rivals");
            for (String key : object.keySet()) {
                double factor = number(object, key);
                if (factor > 0.0D || factor < -1.0D) {
                    throw new JsonParseException("rival factor for " + key + " must be between -1 and 0");
                }
                rivals.put(identifier(key), factor);
            }
        }
        return new Faction(id, name, color, members, settlements,
                clamp(integer(json, "start", 0)),
                idMap(json, "start_by_race"),
                rivals,
                nonNegative(json, "decay_per_day", 0),
                integer(json, "hit_member", 0),
                integer(json, "kill_member", 0),
                matcherMap(json, "kill_member_overrides"),
                integer(json, "betrayal", 0),
                idMap(json, "enemy_kills"),
                matcherMap(json, "enemy_kill_overrides"),
                nonNegative(json, "kill_cap", 0),
                integer(json, "raid_won", 0),
                integer(json, "raid_defeated", 0),
                bounties[0], bounties[1],
                integer(json, "trade", 0),
                nonNegative(json, "trade_cap", 0),
                integer(json, "theft_witnessed", 0));
    }

    @Nullable
    static <V> V lookup(Map<Matcher, V> map, Entity entity) {
        // Command tags first (a king is still a dwarf), then exact types, then type tags.
        for (int pass = 0; pass < 3; pass++) {
            for (Map.Entry<Matcher, V> entry : map.entrySet()) {
                Matcher matcher = entry.getKey();
                boolean kind = pass == 0 ? matcher.commandTag != null : pass == 1 ? matcher.type != null : matcher.tag != null;
                if (kind && matcher.matches(entity)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private static int clamp(int value) {
        return Math.max(Reputation.MIN, Math.min(Reputation.MAX, value));
    }

    private static int parseColor(String text) {
        try {
            if (text.startsWith("#") && text.length() == 7) {
                return Integer.parseInt(text.substring(1), 16);
            }
        } catch (NumberFormatException ignored) {
        }
        throw new JsonParseException("\"color\" must look like #RRGGBB, got " + text);
    }

    private static Identifier identifier(String text) {
        try {
            return new Identifier(text);
        } catch (InvalidIdentifierException e) {
            throw new JsonParseException("bad id \"" + text + "\"");
        }
    }

    private static String string(JsonObject json, String key, @Nullable String fallback) {
        if (!json.has(key)) {
            return fallback;
        }
        JsonElement element = json.get(key);
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new JsonParseException("\"" + key + "\" must be a string");
        }
        return element.getAsString();
    }

    private static JsonObject object(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (!element.isJsonObject()) {
            throw new JsonParseException("\"" + key + "\" must be an object");
        }
        return element.getAsJsonObject();
    }

    private static double number(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (!element.isJsonPrimitive() || !((JsonPrimitive) element).isNumber()) {
            throw new JsonParseException("\"" + key + "\" must be a number");
        }
        return element.getAsDouble();
    }

    private static int integer(JsonObject json, String key, int fallback) {
        if (!json.has(key)) {
            return fallback;
        }
        double value = number(json, key);
        if (value != Math.rint(value) || Math.abs(value) > 100000) {
            throw new JsonParseException("\"" + key + "\" must be a whole number");
        }
        return (int) value;
    }

    private static int nonNegative(JsonObject json, String key, int fallback) {
        int value = integer(json, key, fallback);
        if (value < 0) {
            throw new JsonParseException("\"" + key + "\" can't be negative");
        }
        return value;
    }

    private static Map<Identifier, Integer> idMap(JsonObject json, String key) {
        Map<Identifier, Integer> map = new LinkedHashMap<>();
        if (json.has(key)) {
            JsonObject object = object(json, key);
            for (String entry : object.keySet()) {
                map.put(identifier(entry), integer(object, entry, 0));
            }
        }
        return Map.copyOf(map);
    }

    private static Map<Matcher, Integer> matcherMap(JsonObject json, String key) {
        Map<Matcher, Integer> map = new LinkedHashMap<>();
        if (json.has(key)) {
            JsonObject object = object(json, key);
            for (String entry : object.keySet()) {
                map.put(Matcher.parse(entry), integer(object, entry, 0));
            }
        }
        return map;
    }

    /** An entity type, entity type tag ({@code #ns:path}) or command tag ({@code @tag}). */
    public record Matcher(@Nullable EntityType<?> type, @Nullable TagKey<EntityType<?>> tag, @Nullable String commandTag) {
        static Matcher parse(String text) {
            if (text.startsWith("@")) {
                if (text.length() < 2) {
                    throw new JsonParseException("empty command tag");
                }
                return new Matcher(null, null, text.substring(1));
            }
            if (text.startsWith("#")) {
                return new Matcher(null, TagKey.of(RegistryKeys.ENTITY_TYPE, identifier(text.substring(1))), null);
            }
            Identifier id = identifier(text);
            if (!Registries.ENTITY_TYPE.containsId(id)) {
                throw new JsonParseException("unknown entity type " + id);
            }
            return new Matcher(Registries.ENTITY_TYPE.get(id), null, null);
        }

        /** Whether every entity of {@code type} matches (command tags never do). */
        public boolean matchesType(EntityType<?> type) {
            if (this.commandTag != null) {
                return false;
            }
            return this.tag != null ? type.isIn(this.tag) : type == this.type;
        }

        public boolean matches(Entity entity) {
            if (this.commandTag != null) {
                return entity.getCommandTags().contains(this.commandTag);
            }
            if (this.tag != null) {
                return entity.getType().isIn(this.tag);
            }
            return entity.getType() == this.type;
        }
    }

    /** A structure id or structure tag ({@code #ns:path}). */
    public record Settlements(@Nullable RegistryKey<Structure> key, @Nullable TagKey<Structure> tag) {
        static Settlements parse(String text) {
            if (text.startsWith("#")) {
                return new Settlements(null, TagKey.of(RegistryKeys.STRUCTURE, identifier(text.substring(1))));
            }
            return new Settlements(RegistryKey.of(RegistryKeys.STRUCTURE, identifier(text)), null);
        }

        boolean matches(RegistryEntry<Structure> entry) {
            return this.tag != null ? entry.isIn(this.tag) : entry.matchesKey(this.key);
        }
    }
}
