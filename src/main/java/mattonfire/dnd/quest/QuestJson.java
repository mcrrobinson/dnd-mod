package mattonfire.dnd.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

/**
 * Parsing helpers for quest files. Every failure throws {@link IllegalArgumentException} with a
 * message naming the field, which the loader logs next to the file name.
 */
final class QuestJson {
    private QuestJson() {
    }

    static JsonObject object(JsonElement element, String what) {
        if (element == null || !element.isJsonObject()) {
            throw new IllegalArgumentException(what + " must be an object");
        }
        return element.getAsJsonObject();
    }

    static String string(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("missing or non-string field \"" + key + "\"");
        }
        return element.getAsString();
    }

    @Nullable
    static String optString(JsonObject json, String key) {
        return json.has(key) ? string(json, key) : null;
    }

    static int integer(JsonObject json, String key, int fallback, int min, int max) {
        JsonElement element = json.get(key);
        if (element == null) {
            return fallback;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("field \"" + key + "\" must be a number");
        }
        double value = element.getAsDouble();
        if (value != Math.rint(value) || value < min || value > max) {
            throw new IllegalArgumentException("field \"" + key + "\" must be a whole number from " + min + " to " + max);
        }
        return (int) value;
    }

    static double number(JsonObject json, String key, double fallback, double min, double max) {
        JsonElement element = json.get(key);
        if (element == null) {
            return fallback;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("field \"" + key + "\" must be a number");
        }
        double value = element.getAsDouble();
        if (value < min || value > max) {
            throw new IllegalArgumentException("field \"" + key + "\" must be from " + min + " to " + max);
        }
        return value;
    }

    static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement element = json.get(key);
        if (element == null) {
            return fallback;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException("field \"" + key + "\" must be true or false");
        }
        return element.getAsBoolean();
    }

    static Identifier id(String value, String what) {
        try {
            return new Identifier(value);
        } catch (InvalidIdentifierException e) {
            throw new IllegalArgumentException(what + ": bad id \"" + value + "\"");
        }
    }

    static Identifier id(JsonObject json, String key) {
        return id(string(json, key), "field \"" + key + "\"");
    }

    /** A list field; missing means empty. Each element is parsed with {@code parser}. */
    static <T> List<T> list(JsonObject json, String key, Function<JsonElement, T> parser) {
        JsonElement element = json.get(key);
        List<T> result = new ArrayList<>();
        if (element == null) {
            return result;
        }
        if (!element.isJsonArray()) {
            throw new IllegalArgumentException("field \"" + key + "\" must be a list");
        }
        JsonArray array = element.getAsJsonArray();
        for (int i = 0; i < array.size(); i++) {
            try {
                result.add(parser.apply(array.get(i)));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(key + "[" + i + "]: " + e.getMessage());
            }
        }
        return result;
    }

    /**
     * Text in a quest file: a plain string is a translation key that falls back to itself (so DM
     * datapacks can write literal text), an object or list is a full JSON text component.
     */
    static MutableText text(JsonElement element, String what) {
        if (element == null) {
            throw new IllegalArgumentException("missing text \"" + what + "\"");
        }
        if (element.isJsonPrimitive()) {
            String value = element.getAsString();
            return Text.translatableWithFallback(value, value);
        }
        MutableText text;
        try {
            text = Text.Serializer.fromJson(element);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("bad text \"" + what + "\": " + e.getMessage());
        }
        if (text == null) {
            throw new IllegalArgumentException("bad text \"" + what + "\"");
        }
        return text;
    }

    @Nullable
    static MutableText optText(JsonObject json, String key) {
        return json.has(key) ? text(json.get(key), key) : null;
    }

    /** A name for a tag or id path: {@code dndclasses:bounty/undead} becomes "undead". */
    static String shortName(Identifier id) {
        String path = id.getPath();
        return path.substring(path.lastIndexOf('/') + 1).replace('_', ' ');
    }

    /** An entity type id or {@code #tag}. */
    record EntityMatch(@Nullable Identifier id, @Nullable TagKey<EntityType<?>> tag) {
        static EntityMatch parse(String value) {
            if (value.startsWith("#")) {
                return new EntityMatch(null, TagKey.of(RegistryKeys.ENTITY_TYPE, QuestJson.id(value.substring(1), "entity tag")));
            }
            Identifier id = QuestJson.id(value, "entity");
            if (!Registries.ENTITY_TYPE.containsId(id)) {
                throw new IllegalArgumentException("unknown entity \"" + value + "\"");
            }
            return new EntityMatch(id, null);
        }

        boolean matches(EntityType<?> type) {
            return this.tag != null ? type.isIn(this.tag) : Registries.ENTITY_TYPE.getId(type).equals(this.id);
        }

        Text name() {
            if (this.id != null) {
                return Registries.ENTITY_TYPE.get(this.id).getName();
            }
            Identifier tagId = this.tag.id();
            return Text.translatableWithFallback("tag.entity_type." + tagId.getNamespace() + "." + tagId.getPath().replace('/', '.'),
                    shortName(tagId));
        }

        @Override
        public String toString() {
            return this.tag != null ? "#" + this.tag.id() : String.valueOf(this.id);
        }
    }

    /** An item id or {@code #tag}. */
    record ItemMatch(@Nullable Identifier id, @Nullable TagKey<net.minecraft.item.Item> tag) {
        static ItemMatch parse(String value) {
            if (value.startsWith("#")) {
                return new ItemMatch(null, TagKey.of(RegistryKeys.ITEM, QuestJson.id(value.substring(1), "item tag")));
            }
            Identifier id = QuestJson.id(value, "item");
            if (!Registries.ITEM.containsId(id)) {
                throw new IllegalArgumentException("unknown item \"" + value + "\"");
            }
            return new ItemMatch(id, null);
        }

        boolean matches(ItemStack stack) {
            if (stack.isEmpty()) {
                return false;
            }
            return this.tag != null ? stack.isIn(this.tag) : stack.isOf(Registries.ITEM.get(this.id));
        }

        Text name() {
            if (this.id != null) {
                return Registries.ITEM.get(this.id).getName();
            }
            Identifier tagId = this.tag.id();
            return Text.translatableWithFallback("tag.item." + tagId.getNamespace() + "." + tagId.getPath().replace('/', '.'),
                    shortName(tagId));
        }

        @Override
        public String toString() {
            return this.tag != null ? "#" + this.tag.id() : String.valueOf(this.id);
        }
    }

    /**
     * A structure id or {@code #tag}. Structures live in a dynamic registry, so ids are only checked
     * when used, not when the file loads.
     */
    record StructureMatch(@Nullable RegistryKey<Structure> key, @Nullable TagKey<Structure> tag) {
        static StructureMatch parse(String value) {
            if (value.startsWith("#")) {
                return new StructureMatch(null, TagKey.of(RegistryKeys.STRUCTURE, QuestJson.id(value.substring(1), "structure tag")));
            }
            return new StructureMatch(RegistryKey.of(RegistryKeys.STRUCTURE, QuestJson.id(value, "structure")), null);
        }

        /** Whether {@code pos} is inside one of these structures' pieces. */
        boolean contains(ServerWorld world, BlockPos pos) {
            StructureStart start = this.tag != null
                    ? world.getStructureAccessor().getStructureContaining(pos, this.tag)
                    : world.getStructureAccessor().getStructureContaining(pos, this.key);
            return start.hasChildren();
        }

        Identifier id() {
            return this.tag != null ? this.tag.id() : this.key.getValue();
        }

        Text name() {
            Identifier id = this.id();
            return Text.translatableWithFallback("structure." + id.getNamespace() + "." + id.getPath().replace('/', '.'),
                    shortName(id));
        }

        @Override
        public String toString() {
            return this.tag != null ? "#" + this.tag.id() : String.valueOf(this.key.getValue());
        }
    }
}
