package mattonfire.dnd.dungeon;

import java.util.Set;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.LootConditionType;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameter;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.JsonSerializer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Loot condition {@code dndclasses:dungeon_tier}: passes when the loot is rolled inside a recorded
 * dungeon whose Challenge tier is between {@code min} and {@code max} (both optional, 1-4). The
 * dungeon is looked up from the roll's origin (a chest, the Hoard Coffer, a dying Veteran), so it
 * works in shared tables like {@code chests/dungeon/vault_salvage}. Outside any dungeon the tier
 * counts as I.
 *
 * <pre>{ "condition": "dndclasses:dungeon_tier", "min": 3, "max": 4 }</pre>
 */
public class DungeonTierLootCondition implements LootCondition {
    public static final LootConditionType TYPE = new LootConditionType(new Serializer());

    private final int min;
    private final int max;

    public DungeonTierLootCondition(int min, int max) {
        this.min = min;
        this.max = max;
    }

    static void register() {
        Registry.register(Registries.LOOT_CONDITION_TYPE, new Identifier(DnDClasses.MOD_ID, "dungeon_tier"), TYPE);
    }

    @Override
    public LootConditionType getType() {
        return TYPE;
    }

    @Override
    public Set<LootContextParameter<?>> getRequiredParameters() {
        return Set.of(LootContextParameters.ORIGIN);
    }

    @Override
    public boolean test(LootContext context) {
        Vec3d origin = context.get(LootContextParameters.ORIGIN);
        int tier = origin == null ? 1
                : DungeonRegistry.get(context.getWorld()).containing(BlockPos.ofFloored(origin)).map(DungeonState::tier).orElse(1);
        return tier >= this.min && tier <= this.max;
    }

    public static class Serializer implements JsonSerializer<DungeonTierLootCondition> {
        @Override
        public void toJson(JsonObject json, DungeonTierLootCondition condition, JsonSerializationContext context) {
            json.addProperty("min", condition.min);
            json.addProperty("max", condition.max);
        }

        @Override
        public DungeonTierLootCondition fromJson(JsonObject json, JsonDeserializationContext context) {
            return new DungeonTierLootCondition(JsonHelper.getInt(json, "min", 1), JsonHelper.getInt(json, "max", 4));
        }
    }
}
