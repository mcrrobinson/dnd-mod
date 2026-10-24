package mattonfire.dnd.classes.Registry;

import mattonfire.dnd.classes.DnDClasses;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroup {
    public static ItemGroup DND_CLASSES_ITEMGROUP = FabricItemGroup
            .builder(new Identifier(DnDClasses.MOD_ID, "dndclasses"))
            .displayName(Text.translatable("itemgroup.dndclasses"))
            .icon(() -> new ItemStack(ModItems.MONK_STAFF)).build();

    /** Decorative building materials ({@link BlockFamily}), each family's shapes together. */
    public static ItemGroup BUILDING_BLOCKS_ITEMGROUP = FabricItemGroup
            .builder(new Identifier(DnDClasses.MOD_ID, "building_blocks"))
            .displayName(Text.translatable("itemgroup.dndclasses.building_blocks"))
            .icon(() -> new ItemStack(ModBlockFamilies.RUNIC_DEEPSLATE_BRICKS.base())).build();

    public static void registerItemGroups() {
        ItemGroupEvents.modifyEntriesEvent(BUILDING_BLOCKS_ITEMGROUP).register(entries -> {
            for (BlockFamily family : BlockFamily.all()) {
                family.blocks().forEach(entries::add);
            }
            // --- Brimstone Sanctums ---
            SanctumBlocks.all().forEach(entries::add);
            // --- Dwarven Fortresses ---
            DwarvenBlocks.all().forEach(entries::add);
            // --- Orc Strongholds ---
            OrcBuildingBlocks.all().forEach(entries::add);
            // --- Crypt dungeons ---
            CryptBlocks.all().forEach(entries::add);
            // --- Goblin Camps & Warrens ---
            GoblinBlocks.all().forEach(entries::add);
        });
        // --- Dragon Temples ---
        ItemGroupEvents.modifyEntriesEvent(BUILDING_BLOCKS_ITEMGROUP)
                .register(entries -> ModTempleBlocks.tabBlocks().forEach(entries::add));
        // Example of adding to existing Item Group
        ItemGroupEvents.modifyEntriesEvent(DND_CLASSES_ITEMGROUP).register(entries -> {
            entries.add(ModItems.CLASS_GUIDEBOOK);
            entries.add(ModBlocks.ATTUNEMENT_TABLE);
            entries.add(ModItems.STAFF_OF_ICE);
            entries.add(ModItems.STAFF_OF_FIRE);
            entries.add(ModItems.STAFF_OF_LIGHTNING);
            entries.add(ModItems.MONK_STAFF);
            entries.add(ModItems.LUTE);
            entries.add(ModItems.DRUM);
            entries.add(ModItems.FLUTE);
            entries.add(ModItems.CLOAK_OF_PROTECTION);
            entries.add(ModItems.WAND_OF_MAGIC_MISSILES);
            entries.add(ModItems.BAG_OF_HOLDING);
            entries.add(ModItems.DECANTER_OF_ENDLESS_WATER);
            entries.add(ModItems.IMMOVABLE_ROD);
            entries.add(ModItems.PERIAPT_OF_WOUND_CLOSURE);
            entries.add(ModItems.RING_OF_PROTECTION);
            entries.add(ModItems.AMULET_OF_HEALTH);
            entries.add(ModItems.DOSS_LUTE);
            entries.add(ModItems.TOME_OF_CLEAR_THOUGHT);
            entries.add(ModItems.WARLORDS_WARHORN);
            entries.add(ModItems.HEART_OF_THE_FURNACE);
            entries.add(ModItems.STORM_CHASER_BOOTS);
            entries.add(ModItems.WYVERN_SCALE_CLOAK);
            entries.add(ModItems.BELT_OF_OWLBEAR_MIGHT);
            entries.add(ModItems.MACE_OF_DISRUPTION);
            entries.add(ModItems.FROSTBRAND);
            entries.add(ModItems.PHYLACTERY_LOCKET);
            entries.add(ModItems.BURGLARS_RING);
            entries.add(ModItems.EYE_OF_THE_TYRANT);
            entries.add(ModItems.GORGONS_GAZE);
            entries.add(ModItems.SCROLL_OF_IDENTIFY);
            entries.add(ModItems.SCROLL_OF_REMOVE_CURSE);
            entries.add(ModItems.MUSIC_DISC_STEEL_ON_STEEL);
            entries.add(ModItems.MUSIC_DISC_AWAKE_CART);
            entries.add(ModItems.MUSIC_DISC_TOOTH_AND_CLAW);
            entries.add(ModItems.MUSIC_DISC_SILENT_FOOTSTEPS);
            entries.add(ModItems.ASSASSIN_BOOTS);
            entries.add(ModItems.ASSASSIN_CHESTPLATE);
            entries.add(ModItems.ASSASSIN_HELMET);
            entries.add(ModItems.ASSASSIN_LEGGINGS);
            entries.add(ModItems.ROGUE_HELMET);
            entries.add(ModItems.ROGUE_CHESTPLATE);
            entries.add(ModItems.ROGUE_LEGGINGS);
            entries.add(ModItems.ROGUE_BOOTS);
            entries.add(ModItems.WIZARD_HELMET);
            entries.add(ModItems.WIZARD_CHESTPLATE);
            entries.add(ModItems.WIZARD_LEGGINGS);
            entries.add(ModItems.WIZARD_BOOTS);
            entries.add(ModItems.CLERIC_HELMET);
            entries.add(ModItems.CLERIC_CHESTPLATE);
            entries.add(ModItems.CLERIC_LEGGINGS);
            entries.add(ModItems.CLERIC_BOOTS);
            entries.add(ModItems.BLOOD_HUNTER_HELMET);
            entries.add(ModItems.BLOOD_HUNTER_CHESTPLATE);
            entries.add(ModItems.BLOOD_HUNTER_LEGGINGS);
            entries.add(ModItems.BLOOD_HUNTER_BOOTS);
            entries.add(ModItems.ASSASSIN_HELMET);
            entries.add(ModItems.ASSASSIN_CHESTPLATE);
            entries.add(ModItems.ASSASSIN_LEGGINGS);
            entries.add(ModItems.ASSASSIN_BOOTS);
            entries.add(ModItems.GOLDEN_HORNS_HELMET);
            entries.add(ModItems.GOLDEN_HORNS_CHESTPLATE);
            entries.add(ModItems.GOLDEN_HORNS_LEGGINGS);
            entries.add(ModItems.GOLDEN_HORNS_BOOTS);
            entries.add(ModItems.HOLY_ARMOR_HELMET);
            entries.add(ModItems.HOLY_ARMOR_CHESTPLATE);
            entries.add(ModItems.HOLY_ARMOR_LEGGINGS);
            entries.add(ModItems.HOLY_ARMOR_BOOTS);
            entries.add(ModItems.KNIGHT_HELMET);
            entries.add(ModItems.KNIGHT_CHESTPLATE);
            entries.add(ModItems.KNIGHT_LEGGINGS);
            entries.add(ModItems.KNIGHT_BOOTS);
            entries.add(ModItems.PRISMARINE_HELMET);
            entries.add(ModItems.PRISMARINE_CHESTPLATE);
            entries.add(ModItems.PRISMARINE_LEGGINGS);
            entries.add(ModItems.PRISMARINE_BOOTS);
            entries.add(ModItems.ROBE_HELMET);
            entries.add(ModItems.ROBE_CHESTPLATE);
            entries.add(ModItems.ROBE_LEGGINGS);
            entries.add(ModItems.ROBE_BOOTS);
            entries.add(ModItems.STEAMPUNK_HELMET);
            entries.add(ModItems.STEAMPUNK_CHESTPLATE);
            entries.add(ModItems.STEAMPUNK_LEGGINGS);
            entries.add(ModItems.STEAMPUNK_BOOTS);
            entries.add(ModItems.WARRIOR_HELMET);
            entries.add(ModItems.WARRIOR_CHESTPLATE);
            entries.add(ModItems.WARRIOR_LEGGINGS);
            entries.add(ModItems.WARRIOR_BOOTS);
            entries.add(ModItems.WITHER_HELMET);
            entries.add(ModItems.WITHER_CHESTPLATE);
            entries.add(ModItems.WITHER_LEGGINGS);
            entries.add(ModItems.WOODEN_HELMET);
            entries.add(ModItems.WOODEN_CHESTPLATE);
            entries.add(ModItems.WOODEN_LEGGINGS);
            entries.add(ModItems.WOODEN_BOOTS);
            entries.add(ModItems.ELVEN_CHAIN_HELMET);
            entries.add(ModItems.ELVEN_CHAIN_CHESTPLATE);
            entries.add(ModItems.ELVEN_CHAIN_LEGGINGS);
            entries.add(ModItems.ELVEN_CHAIN_BOOTS);
            entries.add(ModItems.DWARVEN_PLATE_HELMET);
            entries.add(ModItems.DWARVEN_PLATE_CHESTPLATE);
            entries.add(ModItems.DWARVEN_PLATE_LEGGINGS);
            entries.add(ModItems.DWARVEN_PLATE_BOOTS);
            entries.add(ModItems.ORCISH_WARPLATE_HELMET);
            entries.add(ModItems.ORCISH_WARPLATE_CHESTPLATE);
            entries.add(ModItems.ORCISH_WARPLATE_LEGGINGS);
            entries.add(ModItems.ORCISH_WARPLATE_BOOTS);
            entries.add(ModItems.DRACONIC_SCALE_HELMET);
            entries.add(ModItems.DRACONIC_SCALE_CHESTPLATE);
            entries.add(ModItems.DRACONIC_SCALE_LEGGINGS);
            entries.add(ModItems.DRACONIC_SCALE_BOOTS);
            entries.add(ModItems.INFERNAL_REGALIA_HELMET);
            entries.add(ModItems.INFERNAL_REGALIA_CHESTPLATE);
            entries.add(ModItems.INFERNAL_REGALIA_LEGGINGS);
            entries.add(ModItems.INFERNAL_REGALIA_BOOTS);
            entries.add(ModItems.ELVEN_LONGBOW);
            entries.add(ModItems.DWARVEN_WARAXE);
            entries.add(ModItems.ORCISH_GREATAXE);
            entries.add(ModItems.GROG);
            entries.add(ModItems.GOBLIN_WAR_TOKEN);
            entries.add(ModItems.HALFLING_SLING);
            entries.add(ModItems.GNOMISH_REPEATER);
            entries.add(ModItems.TINKERS_GOGGLES);
            entries.add(ModItems.HOBBIT_SPAWN_EGG);
            entries.add(ModItems.MOUNTAIN_DWARF_SPAWN_EGG);
            entries.add(ModItems.WOOD_ELF_SPAWN_EGG);
            entries.add(ModItems.ELF_WARDEN_SPAWN_EGG);
            entries.add(ModItems.ELF_SPEAKER_SPAWN_EGG);
            entries.add(ModItems.ELF_FLETCHER_SPAWN_EGG);
            entries.add(ModItems.DRAGONBORN_TEMPLAR_SPAWN_EGG);
            entries.add(ModItems.DRAGONBORN_ELDER_SPAWN_EGG);
            entries.add(ModItems.TIEFLING_EXILE_SPAWN_EGG);
            entries.add(ModItems.TIEFLING_PACTBROKER_SPAWN_EGG);
            entries.add(ModItems.GNOME_TINKER_SPAWN_EGG);
            entries.add(ModItems.CLOCKWORK_SENTINEL_SPAWN_EGG);
            entries.add(ModItems.ORC_WARRIOR_SPAWN_EGG);
            entries.add(ModItems.ORC_ARCHER_SPAWN_EGG);
            entries.add(ModItems.ORC_QUARTERMASTER_SPAWN_EGG);
            entries.add(ModItems.ORC_CHAMPION_SPAWN_EGG);
            entries.add(ModItems.ORC_WAR_CHIEF_SPAWN_EGG);
            entries.add(ModItems.GOBLIN_WARLORD_SPAWN_EGG);
            entries.add(ModItems.MAGMAMUNCHER_ALPHA_SPAWN_EGG);
            entries.add(ModItems.EMBER_WYVERN_SPAWN_EGG);
            entries.add(ModItems.MIMIC_SPAWN_EGG);
            entries.add(ModItems.OWLBEAR_SPAWN_EGG);
            entries.add(ModItems.LICH_SPAWN_EGG);
            entries.add(ModItems.MEDUSA_SPAWN_EGG);
            entries.add(ModItems.BEHOLDER_SPAWN_EGG);
            entries.add(ModItems.WYVERN_SPAWN_EGG);
            entries.add(ModItems.LIGHTNING_CHASER_SPAWN_EGG);
            entries.add(ModItems.FROST_DRAKE_SPAWN_EGG);
            entries.add(ModItems.RIVER_PIKEHORN_SPAWN_EGG);
            entries.add(ModItems.MAGMAMUNCHER_SPAWN_EGG);
            entries.add(ModItems.GOBLIN_WARRIOR_SPAWN_EGG);
            entries.add(ModItems.GELATINOUS_CUBE_SPAWN_EGG);
        });
        // --- Racial banner patterns ---
        ItemGroupEvents.modifyEntriesEvent(BUILDING_BLOCKS_ITEMGROUP).register(entries ->
                ModBannerPatterns.all().forEach(pattern -> entries.add(pattern.item())));
    }
}