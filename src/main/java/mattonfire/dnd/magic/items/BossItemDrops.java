package mattonfire.dnd.magic.items;

import java.util.Map;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.ModItems;
import mattonfire.dnd.magic.MagicItemLootFunction;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.item.Item;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.KilledByPlayerLootCondition;
import net.minecraft.loot.condition.RandomChanceWithLootingLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.util.Identifier;

/**
 * Where the boss and monster magic items drop (design 5, section 3.7): one extra pool per mob loot table, added
 * through {@link LootTableEvents#MODIFY} so the mob tables (and the Lich's, owned by the Crypt work) stay as they
 * are. Each drop needs a player kill, rolls a chance plus a bonus per Looting level, and comes out unidentified
 * ({@code dndclasses:magic_item}). The Lich's table is the one its phylactery drops on the final kill.
 */
public final class BossItemDrops {
    /** A named item a mob table drops: {@code chance} + {@code perLooting} for each Looting level. */
    public record Drop(Item item, float chance, float perLooting) {
    }

    private static Map<Identifier, Drop[]> drops() {
        // Map.ofEntries, not Map.of: Map.of stops at ten tables
        return Map.ofEntries(
                Map.entry(table("goblin_warlord"), new Drop[] { new Drop(ModItems.WARLORDS_WARHORN, 0.35F, 0.0F) }),
                Map.entry(table("magmamuncher_alpha"), new Drop[] { new Drop(ModItems.HEART_OF_THE_FURNACE, 0.20F, 0.05F) }),
                Map.entry(table("lightning_chaser"), new Drop[] { new Drop(ModItems.STORM_CHASER_BOOTS, 0.12F, 0.03F) }),
                Map.entry(table("wyvern"), new Drop[] { new Drop(ModItems.WYVERN_SCALE_CLOAK, 0.08F, 0.0F) }),
                Map.entry(table("ember_wyvern"), new Drop[] { new Drop(ModItems.WYVERN_SCALE_CLOAK, 0.08F, 0.0F) }),
                Map.entry(table("owlbear"), new Drop[] { new Drop(ModItems.BELT_OF_OWLBEAR_MIGHT, 0.08F, 0.02F) }),
                Map.entry(table("frost_drake"), new Drop[] { new Drop(ModItems.FROSTBRAND, 0.10F, 0.05F) }),
                Map.entry(table("lich"), new Drop[] { new Drop(ModItems.MACE_OF_DISRUPTION, 0.25F, 0.0F),
                        new Drop(ModItems.PHYLACTERY_LOCKET, 0.20F, 0.0F) }),
                Map.entry(table("beholder"), new Drop[] { new Drop(ModItems.EYE_OF_THE_TYRANT, 0.12F, 0.03F) }),
                Map.entry(table("medusa"), new Drop[] { new Drop(ModItems.GORGONS_GAZE, 0.25F, 0.05F) }));
    }

    private BossItemDrops() {
    }

    private static Identifier table(String entity) {
        return new Identifier(DnDClasses.MOD_ID, "entities/" + entity);
    }

    static void register() {
        Map<Identifier, Drop[]> drops = drops();
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
            Drop[] list = drops.get(id);
            if (list == null)
                return;
            for (Drop drop : list) {
                tableBuilder.pool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(KilledByPlayerLootCondition.builder())
                        .conditionally(RandomChanceWithLootingLootCondition.builder(drop.chance(), drop.perLooting()))
                        .with(ItemEntry.builder(drop.item()).apply(MagicItemLootFunction.unidentified())));
            }
        });
    }
}
