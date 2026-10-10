package mattonfire.dnd.magic;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;

import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.function.ConditionalLootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

/**
 * Loot function {@code dndclasses:magic_item}: turns the rolled stack into a magic item.
 *
 * <pre>
 * { "function": "dndclasses:magic_item",
 *   "tier": "rare",          // or "rarity"; optional for registered items (keeps their tier)
 *   "plus": 2,               // optional; weapons and armor default to the tier's +N (1/2/3)
 *   "identified": false,     // optional, default false (random loot is unidentified)
 *   "curse_chance": 0.1,     // optional; read now, rolled by the curses ticket
 *   "theme": "crypt" }       // optional dungeon theme bias (area 2); not used yet
 * </pre>
 */
public class MagicItemLootFunction extends ConditionalLootFunction {
    public static final LootFunctionType TYPE = new LootFunctionType(new Serializer());

    final @Nullable MagicTier tier;
    final @Nullable Integer plus;
    final boolean identified;
    /** Negative = the tier's default chance. Not rolled yet: the curses ticket adds that. */
    final float curseChance;
    final @Nullable String theme;

    MagicItemLootFunction(LootCondition[] conditions, @Nullable MagicTier tier, @Nullable Integer plus,
            boolean identified, float curseChance, @Nullable String theme) {
        super(conditions);
        this.tier = tier;
        this.plus = plus;
        this.identified = identified;
        this.curseChance = curseChance;
        this.theme = theme;
    }

    static void register() {
        Registry.register(Registries.LOOT_FUNCTION_TYPE, new Identifier(DnDClasses.MOD_ID, "magic_item"), TYPE);
    }

    @Override
    public LootFunctionType getType() {
        return TYPE;
    }

    @Override
    protected ItemStack process(ItemStack stack, LootContext context) {
        apply(stack, tier, plus, identified);
        return stack;
    }

    /** Shared with {@code /dndmagic give}: sets the tier, the +N (gear only) and the identified flag. */
    public static ItemStack apply(ItemStack stack, @Nullable MagicTier tier, @Nullable Integer plus,
            boolean identified) {
        if (stack.isEmpty())
            return stack;
        MagicItemDef def = MagicItems.def(stack.getItem());
        MagicTier effective = tier != null ? tier : def != null ? def.tier() : MagicTier.UNCOMMON;
        if (tier != null || def == null)
            MagicData.setTier(stack, effective);
        if (def == null && MagicGear.canHavePlus(stack.getItem())) {
            int n = plus != null ? plus : effective.defaultPlus;
            if (n > 0)
                MagicData.setPlus(stack, Math.min(n, MagicGear.MAX_PLUS));
        }
        MagicData.setIdentified(stack, identified);
        return stack;
    }

    public static class Serializer extends ConditionalLootFunction.Serializer<MagicItemLootFunction> {
        @Override
        public void toJson(JsonObject json, MagicItemLootFunction function, JsonSerializationContext context) {
            super.toJson(json, function, context);
            if (function.tier != null)
                json.addProperty("tier", function.tier.id);
            if (function.plus != null)
                json.addProperty("plus", function.plus);
            json.addProperty("identified", function.identified);
            if (function.curseChance >= 0)
                json.addProperty("curse_chance", function.curseChance);
            if (function.theme != null)
                json.addProperty("theme", function.theme);
        }

        @Override
        public MagicItemLootFunction fromJson(JsonObject json, JsonDeserializationContext context,
                LootCondition[] conditions) {
            MagicTier tier = null;
            String key = json.has("tier") ? "tier" : json.has("rarity") ? "rarity" : null;
            if (key != null) {
                String id = JsonHelper.getString(json, key);
                tier = MagicTier.byId(id);
                if (tier == null)
                    throw new JsonSyntaxException("Unknown magic item tier '" + id + "'");
            }
            Integer plus = json.has("plus") ? JsonHelper.getInt(json, "plus") : null;
            boolean identified = JsonHelper.getBoolean(json, "identified", false);
            float curseChance = JsonHelper.getFloat(json, "curse_chance", -1.0f);
            String theme = json.has("theme") ? JsonHelper.getString(json, "theme") : null;
            return new MagicItemLootFunction(conditions, tier, plus, identified, curseChance, theme);
        }
    }
}
