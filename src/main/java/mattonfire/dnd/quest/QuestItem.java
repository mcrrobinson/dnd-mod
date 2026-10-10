package mattonfire.dnd.quest;

import java.util.List;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * A story item a quest hands out or asks for (Goblin War Orders, Cursed Bone): a named item with an
 * italic lore line ({@code <translation key>.tooltip}) and "Quest item" under it. Quests match it by
 * id, so each one is its own item and they never stack with each other.
 */
public class QuestItem extends Item {
    public QuestItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable(this.getTranslationKey() + ".tooltip").formatted(Formatting.GRAY, Formatting.ITALIC));
        tooltip.add(Text.translatable("item.dndclasses.quest_item").formatted(Formatting.GOLD));
    }
}
