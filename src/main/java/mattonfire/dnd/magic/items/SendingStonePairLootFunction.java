package mattonfire.dnd.magic.items;

import java.util.UUID;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;

import mattonfire.dnd.classes.Registry.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.function.ConditionalLootFunction;
import net.minecraft.loot.function.LootFunctionType;

/**
 * Loot function {@code dndclasses:sending_stone_pair}: makes a rolled Sending Stone a pair, a stack of 2 sharing a
 * new {@code Pair} id. Other items pass through unchanged. {@code { "function": "dndclasses:sending_stone_pair" }}
 */
public class SendingStonePairLootFunction extends ConditionalLootFunction {
    SendingStonePairLootFunction(LootCondition[] conditions) {
        super(conditions);
    }

    public static ConditionalLootFunction.Builder<?> builder() {
        return builder(SendingStonePairLootFunction::new);
    }

    @Override
    public LootFunctionType getType() {
        return SendingStones.PAIR_FUNCTION;
    }

    @Override
    protected ItemStack process(ItemStack stack, LootContext context) {
        if (!stack.isOf(ModItems.SENDING_STONE))
            return stack;
        stack.setCount(2);
        SendingStoneItem.setPair(stack, UUID.randomUUID());
        return stack;
    }

    public static class Serializer extends ConditionalLootFunction.Serializer<SendingStonePairLootFunction> {
        @Override
        public SendingStonePairLootFunction fromJson(JsonObject json, JsonDeserializationContext context,
                LootCondition[] conditions) {
            return new SendingStonePairLootFunction(conditions);
        }
    }
}
