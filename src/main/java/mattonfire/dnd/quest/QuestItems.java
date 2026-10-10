package mattonfire.dnd.quest;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.ModItemGroup;
import mattonfire.dnd.tavern.QuestLeadItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

/** Items the launch quest chains use, plus the Rumour notice ({@code quest_lead}). */
public final class QuestItems {
    /** Chain A: taken from goblins in a camp, delivered to the Dwarf King. */
    public static final Item GOBLIN_WAR_ORDERS = register("goblin_war_orders",
            new QuestItem(new FabricItemSettings().maxCount(1).rarity(Rarity.UNCOMMON)));
    /** Chain B: a quest drop from the restless dead, handed in to the hobbit elder. */
    public static final Item CURSED_BONE = register("cursed_bone",
            new QuestItem(new FabricItemSettings().maxCount(16).rarity(Rarity.UNCOMMON)));
    /** A Rumour from a bounty board: right-click to start the quest in its {@code Quest} NBT. */
    public static final Item QUEST_LEAD = register("quest_lead", new QuestLeadItem(new FabricItemSettings().maxCount(1)));

    private QuestItems() {
    }

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(DnDClasses.MOD_ID, name), item);
    }

    static void register() {
        ItemGroupEvents.modifyEntriesEvent(ModItemGroup.DND_CLASSES_ITEMGROUP).register(entries -> {
            entries.add(GOBLIN_WAR_ORDERS);
            entries.add(CURSED_BONE);
            entries.add(QuestLeadItem.create(new Identifier(DnDClasses.MOD_ID, "whispering_library/1"), null));
        });
    }
}
