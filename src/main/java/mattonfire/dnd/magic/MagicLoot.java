package mattonfire.dnd.magic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.ModItems;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.item.Item;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.KilledByPlayerLootCondition;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.entry.LootTableEntry;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.util.Identifier;

/**
 * Where random magic items turn up (design 5, section 3.8; docs/systems/magic-loot.md). Each source table gets
 * extra pools through {@link LootTableEvents#MODIFY}: one roll on the tier tables {@code dndclasses:magic/<tier>}
 * (unidentified, with the tier's curse chance) and the named items that table is a source of. The mod's own tables
 * stay as they are (like {@code BossItemDrops}, which places the boss trophies); vanilla tables are only extended
 * when they come from the game itself, not a datapack that replaces them.
 *
 * <p>Entity tables need a player kill. Bounty rewards come out identified (known goods, no random curse).
 */
public final class MagicLoot {
    /** One extra pool: a chance, then one weighted roll among its entries. */
    record Pool(float chance, List<Entry> entries, boolean playerKill, boolean identified) {
    }

    /** A tier table ({@code item == null}) or a named item, with its weight inside the pool. */
    record Entry(MagicTier tier, Item item, int weight, int min, int max, boolean plain) {
    }

    private static final Map<Identifier, List<Pool>> PLACEMENTS = new HashMap<>();

    private MagicLoot() {
    }

    // ---- placement helpers ----

    static Entry tier(MagicTier tier, int weight) {
        return new Entry(tier, null, weight, 1, 1, false);
    }

    static Entry named(Item item) {
        return new Entry(null, item, 1, 1, 1, false);
    }

    /** A mundane extra (scrolls): no magic function, so it isn't made unidentified. */
    static Entry plain(Item item, int min, int max) {
        return new Entry(null, item, 1, min, max, true);
    }

    private static void add(Identifier table, Pool pool) {
        PLACEMENTS.computeIfAbsent(table, k -> new ArrayList<>()).add(pool);
    }

    private static void chest(String table, float chance, Entry... entries) {
        add(mod("chests/" + table), new Pool(chance, List.of(entries), false, false));
    }

    private static void entity(String table, float chance, Entry... entries) {
        add(mod("entities/" + table), new Pool(chance, List.of(entries), true, false));
    }

    private static void gameplay(String table, float chance, boolean identified, Entry... entries) {
        add(mod("gameplay/" + table), new Pool(chance, List.of(entries), false, identified));
    }

    private static void vanilla(String table, float chance, Entry... entries) {
        add(new Identifier("minecraft", "chests/" + table), new Pool(chance, List.of(entries), false, false));
    }

    private static Identifier mod(String path) {
        return new Identifier(DnDClasses.MOD_ID, path);
    }

    public static Identifier tierTable(MagicTier tier) {
        return mod("magic/" + tier.id);
    }

    // ---- the placement table (design 3.7 sources and 3.8 pools) ----

    static void placements() {
        MagicTier u = MagicTier.UNCOMMON, r = MagicTier.RARE, vr = MagicTier.VERY_RARE, l = MagicTier.LEGENDARY;

        // Goblin camps: the Burglar's Ring was "stolen from a hobbit"
        chest("goblin_camp", 0.25F, tier(u, 1));
        chest("goblin_camp", 0.03F, named(ModItems.WAND_OF_MAGIC_MISSILES));
        chest("goblin_camp", 0.01F, named(ModItems.BURGLARS_RING));
        chest("goblin_camp_supplies", 0.10F, tier(u, 1));

        // Dwarven fortresses
        for (String room : new String[] { "forge", "mine", "barracks", "brewhall" })
            chest("dwarven_fortress_" + room, 0.15F, tier(u, 75), tier(r, 25));
        chest("dwarven_fortress_forge", 0.03F, named(ModItems.IMMOVABLE_ROD));
        chest("dwarven_fortress_barracks", 0.03F, named(ModItems.PERIAPT_OF_WOUND_CLOSURE));
        chest("dwarven_fortress_brewhall", 0.03F, named(ModItems.DOSS_LUTE));
        chest("dwarven_fortress_treasury", 0.60F, tier(u, 50), tier(r, 45), tier(vr, 5));
        chest("dwarven_fortress_treasury", 0.03F, named(ModItems.BAG_OF_HOLDING));
        chest("dwarven_fortress_treasury", 0.03F, named(ModItems.AMULET_OF_HEALTH));

        // Lairs: one guaranteed roll on top of the existing loot (the lairs' staffs stay)
        chest("dragon_lair", 1.0F, tier(r, 70), tier(vr, 30));
        chest("dragon_lair", 0.06F, named(ModItems.STORM_CHASER_BOOTS));
        chest("frost_lair", 1.0F, tier(r, 70), tier(vr, 30));
        chest("frost_lair", 0.08F, named(ModItems.FROSTBRAND));
        chest("beholder_lair", 1.0F, tier(r, 40), tier(vr, 45), tier(l, 15));
        chest("beholder_lair", 0.04F, named(ModItems.IMMOVABLE_ROD));
        chest("beholder_lair", 0.02F, named(ModItems.EYE_OF_THE_TYRANT));

        // Dragon Temples: a small chance in the monks' cells; the reliquary (locked at DC 15, guarded) always rolls,
        // mostly Rare. Its staff chances stay in the JSON.
        chest("dragon_temple_cell", 0.08F, tier(u, 1));
        chest("dragon_temple_reliquary", 1.0F, tier(r, 90), tier(vr, 10));

        // Orc Strongholds
        chest("orc_stronghold_yurt", 0.05F, tier(u, 1));
        chest("orc_stronghold_armory", 0.15F, tier(u, 75), tier(r, 25));
        chest("orc_stronghold_hoard", 0.60F, tier(u, 40), tier(r, 50), tier(vr, 10));

        // Brimstone Sanctums: the offering chest behind the Pact Hall altar is locked at DC 15 and guarded
        chest("brimstone_sanctum_refuge", 0.05F, tier(u, 1));
        chest("brimstone_sanctum_market", 0.05F, tier(u, 1));
        chest("brimstone_sanctum_den", 0.12F, tier(u, 80), tier(r, 20));
        chest("brimstone_sanctum_altar", 0.60F, tier(u, 40), tier(r, 50), tier(vr, 10));

        // Cogsdeep: the clocktower strongbox is locked at DC 15 and guarded by the Sentinels
        chest("cogsdeep_commons", 0.05F, tier(u, 1));
        chest("cogsdeep_workshop", 0.05F, tier(u, 1));
        chest("cogsdeep_gems", 0.12F, tier(u, 80), tier(r, 20));
        chest("cogsdeep_vault", 0.60F, tier(u, 40), tier(r, 50), tier(vr, 10));

        // The Underdark's drow: the outpost shrine's reliquary (its 20% Rare roll is in the JSON) and the Drow Spire
        // (the treasure loft behind the Arcane Seal always rolls; the priestesses' cells now and then)
        chest("drow_shrine", 0.03F, named(ModItems.PIWAFWI));
        chest("drow_spire_cells", 0.10F, tier(u, 70), tier(r, 30));
        chest("drow_spire_loft", 1.0F, tier(r, 60), tier(vr, 40));
        chest("drow_spire_loft", 0.15F, named(ModItems.PIWAFWI));

        // Hobbit harvest chests
        chest("hobbit_harvest", 0.02F, named(ModItems.DECANTER_OF_ENDLESS_WATER));

        // Monsters and bosses (player kill). Their own trophies are in BossItemDrops; every Mimic also rolls
        // entities/mimic_magic (JSON, 25% Uncommon + 10% Bag of Holding) on top of the chest it replaced.
        entity("goblin_warlord", 0.50F, tier(r, 1));
        entity("magmamuncher_alpha", 0.50F, tier(r, 1));
        entity("orc_war_chief", 0.50F, tier(r, 1));
        entity("lich", 1.0F, tier(vr, 80), tier(l, 20));
        entity("lich", 0.10F, named(ModItems.TOME_OF_CLEAR_THOUGHT));
        entity("beholder", 1.0F, tier(vr, 80), tier(l, 20));
        entity("beholder", 0.10F, named(ModItems.TOME_OF_CLEAR_THOUGHT));
        entity("drow_matron", 0.15F, tier(l, 1));
        entity("river_pikehorn", 0.05F, named(ModItems.DECANTER_OF_ENDLESS_WATER));
        entity("kobold_dragonshield", 0.15F, tier(u, 1));
        // The Ancient Red's hoard (D2's table; a no-op until it exists): the kobolds' offerings
        chest("ancient_red_hoard", 1.0F, plain(mattonfire.dnd.entity.kobold.Kobolds.KOBOLD_TRINKET, 1, 3));

        // Bounties: known goods, identified
        gameplay("bounty_minor", 0.10F, true, tier(u, 1));
        gameplay("bounty_minor", 0.05F, true, named(ModItems.WAND_OF_MAGIC_MISSILES));
        gameplay("bounty_major", 0.35F, true, tier(u, 60), tier(r, 40));
        gameplay("bounty_major", 0.03F, true, named(ModItems.PERIAPT_OF_WOUND_CLOSURE));
        gameplay("bounty_major", 0.03F, true, named(ModItems.DOSS_LUTE));

        // Goblin raid rewards (the defenders' share of what the goblins carried)
        for (String raid : new String[] { "hobbit_village", "dwarven_fortress", "elven_enclave", "dragon_temple",
                "wilds" })
            gameplay("goblin_raid_" + raid, 0.20F, false, tier(u, 1));
        gameplay("goblin_raid_hobbit_village", 0.02F, false, named(ModItems.BURGLARS_RING));

        // Vanilla structures
        for (String table : new String[] { "simple_dungeon", "abandoned_mineshaft", "desert_pyramid",
                "jungle_temple" })
            vanilla(table, 0.08F, tier(u, 1));
        vanilla("simple_dungeon", 0.02F, named(ModItems.WAND_OF_MAGIC_MISSILES));
        vanilla("abandoned_mineshaft", 0.02F, named(ModItems.WAND_OF_MAGIC_MISSILES));
        for (String table : new String[] { "stronghold_library", "woodland_mansion", "bastion_treasure",
                "ancient_city" }) {
            vanilla(table, 0.20F, tier(r, 70), tier(vr, 30));
            vanilla(table, 0.25F, plain(ModItems.SCROLL_OF_IDENTIFY, 1, 2));
        }
        vanilla("stronghold_library", 0.10F, plain(ModItems.SCROLL_OF_REMOVE_CURSE, 1, 1));
        vanilla("stronghold_library", 0.04F, named(ModItems.AMULET_OF_HEALTH));
        vanilla("stronghold_library", 0.03F, named(ModItems.MACE_OF_DISRUPTION));
        vanilla("stronghold_library", 0.02F, named(ModItems.TOME_OF_CLEAR_THOUGHT));
        vanilla("end_city_treasure", 0.15F, tier(vr, 85), tier(l, 15));
    }

    static LootPool.Builder build(Pool pool) {
        LootPool.Builder builder = LootPool.builder().rolls(ConstantLootNumberProvider.create(1));
        if (pool.playerKill())
            builder.conditionally(KilledByPlayerLootCondition.builder());
        if (pool.chance() < 1.0F)
            builder.conditionally(RandomChanceLootCondition.builder(pool.chance()));
        for (Entry e : pool.entries()) {
            if (e.item() == null) {
                LootTableEntry.Builder<?> entry = LootTableEntry.builder(tierTable(e.tier())).weight(e.weight());
                if (pool.identified())
                    entry.apply(MagicItemLootFunction.identified());
                builder.with(entry);
                continue;
            }
            var entry = ItemEntry.builder(e.item()).weight(e.weight());
            if (e.max() > 1)
                entry.apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(e.min(), e.max())));
            if (!e.plain())
                entry.apply(pool.identified() ? MagicItemLootFunction.identified() : MagicItemLootFunction.unidentified());
            builder.with(entry);
        }
        return builder;
    }

    static void register() {
        placements();
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
            List<Pool> pools = PLACEMENTS.get(id);
            if (pools == null)
                return;
            // A datapack that replaces a vanilla table decides its own loot
            if (id.getNamespace().equals("minecraft") && !source.isBuiltin())
                return;
            for (Pool pool : pools)
                tableBuilder.pool(build(pool));
        });
    }
}
