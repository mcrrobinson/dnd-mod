package mattonfire.dnd.tavern;

import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.quest.QuestDefinition;
import mattonfire.dnd.quest.QuestManager;
import mattonfire.dnd.quest.Quests;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * A Rumour: a scrap of gossip taken from a bounty board's third slot. It names a quest (NBT
 * {@code Quest}, e.g. {@code dndclasses:whispering_library/1}); right-click it to start that quest
 * without finding its giver. The quest's other requirements (finished quests, standing) still apply.
 */
public class QuestLeadItem extends Item {
    public static final String QUEST = "Quest";
    private static final String TITLE = "Title";

    public QuestLeadItem(Settings settings) {
        super(settings);
    }

    /** A rumour for {@code quest}; {@code title} (shown in the name) may be null. */
    public static ItemStack create(Identifier quest, @Nullable Text title) {
        ItemStack stack = new ItemStack(mattonfire.dnd.quest.QuestItems.QUEST_LEAD);
        stack.getOrCreateNbt().putString(QUEST, quest.toString());
        if (title != null) {
            stack.getOrCreateNbt().putString(TITLE, Text.Serializer.toJson(title));
        }
        return stack;
    }

    @Nullable
    public static Identifier quest(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt == null || !nbt.contains(QUEST) ? null : Identifier.tryParse(nbt.getString(QUEST));
    }

    /**
     * A quest to post as a rumour for {@code player}: one with a {@code rumour_weight} they could start
     * now, picked by weight; null if there's none.
     */
    @Nullable
    public static QuestDefinition pick(ServerPlayerEntity player, Random random) {
        QuestManager manager = QuestManager.get(player.getServer());
        List<QuestDefinition> open = new ArrayList<>();
        int total = 0;
        for (QuestDefinition quest : Quests.all()) {
            if (quest.rumourWeight() > 0 && manager.whyCantStart(player, quest) == null) {
                open.add(quest);
                total += quest.rumourWeight();
            }
        }
        if (total <= 0) {
            return null;
        }
        int roll = random.nextInt(total);
        for (QuestDefinition quest : open) {
            roll -= quest.rumourWeight();
            if (roll < 0) {
                return quest;
            }
        }
        return open.get(open.size() - 1);
    }

    /** Whether any loaded quest can be posted as a rumour at all. */
    public static boolean anyRumours() {
        return Quests.all().stream().anyMatch(quest -> quest.rumourWeight() > 0);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient || !(user instanceof ServerPlayerEntity player)) {
            return TypedActionResult.success(stack, world.isClient);
        }
        Identifier id = quest(stack);
        QuestDefinition quest = id == null ? null : Quests.get(id);
        if (quest == null) {
            player.sendMessage(Text.translatable("item.dndclasses.quest_lead.stale").formatted(Formatting.GRAY), true);
            return TypedActionResult.fail(stack);
        }
        try {
            // The giver requirement is waived (start never checks it); everything else applies.
            QuestManager.get(player.getServer()).start(player, quest, false);
        } catch (QuestManager.QuestException e) {
            player.sendMessage(e.text().copy().formatted(Formatting.RED), false);
            return TypedActionResult.fail(stack);
        }
        DnDClasses.LOGGER.info("[Quests] {} followed a rumour to {}", player.getEntityName(), quest.id());
        world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1.0F, 0.8F);
        if (!player.getAbilities().creativeMode) {
            stack.decrement(1);
        }
        return TypedActionResult.success(stack);
    }

    @Override
    public Text getName(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains(TITLE)) {
            Text title = Text.Serializer.fromJson(nbt.getString(TITLE));
            if (title != null) {
                return Text.translatable(this.getTranslationKey() + ".named", title);
            }
        }
        return super.getName(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable(this.getTranslationKey() + ".tooltip").formatted(Formatting.GRAY, Formatting.ITALIC));
        tooltip.add(Text.translatable(this.getTranslationKey() + ".use").formatted(Formatting.YELLOW));
    }
}
