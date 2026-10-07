package mattonfire.dnd.classes.Registry;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Items.AssassinArmorItem;
import mattonfire.dnd.classes.Items.BloodHunterArmorItem;
import mattonfire.dnd.classes.Items.ExtendedSwordItem;
import mattonfire.dnd.classes.Items.GoldenHornsArmorItem;
import mattonfire.dnd.classes.Items.HolyArmorArmorItem;
import mattonfire.dnd.classes.Items.KnightArmorItem;
import mattonfire.dnd.classes.Items.MonkStaff;
import mattonfire.dnd.classes.Items.PrismarineArmorItem;
import mattonfire.dnd.classes.Items.RobeArmorItem;
import mattonfire.dnd.classes.Items.ClericArmorItem;
import mattonfire.dnd.classes.Items.RogueArmorItem;
import mattonfire.dnd.classes.Items.SteamPunkArmorItem;
import mattonfire.dnd.classes.Items.WarriorArmorItem;
import mattonfire.dnd.classes.Items.WitherArmorItem;
import mattonfire.dnd.classes.Items.WizardArmorItem;
import mattonfire.dnd.classes.Items.WoodenArmorItem;
import mattonfire.dnd.classes.Items.lib.FAArmorAttributes;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
        public static final Item STAFF_OF_ICE = registerItem("staff_of_ice",
                new ExtendedSwordItem(ToolMaterials.DIAMOND, new FabricItemSettings()));

        public static final Item STAFF_OF_FIRE = registerItem("staff_of_fire",
                new ExtendedSwordItem(ToolMaterials.DIAMOND, new FabricItemSettings()));

        public static final Item STAFF_OF_LIGHTNING = registerItem("staff_of_lightning",
                new ExtendedSwordItem(ToolMaterials.DIAMOND, new FabricItemSettings()));

        public static final Item MONK_STAFF = registerItem("monk_staff",
                new MonkStaff(ToolMaterials.DIAMOND, new FabricItemSettings()));

        public static final Item MUSIC_DISC_STEEL_ON_STEEL = registerItem("music_disc_steel_on_steel",
                new MusicDiscItem(6, ModSounds.STEEL_ON_STEEL, new FabricItemSettings().maxCount(1), 98));
        public static final Item MUSIC_DISC_AWAKE_CART = registerItem("music_disc_awake_cart",
                new MusicDiscItem(6, ModSounds.AWAKE_CART, new FabricItemSettings().maxCount(1), 88));
        public static final Item MUSIC_DISC_TOOTH_AND_CLAW = registerItem("music_disc_tooth_and_claw",
                new MusicDiscItem(6, ModSounds.TOOTH_AND_CLAW, new FabricItemSettings().maxCount(1), 105));
        public static final Item MUSIC_DISC_SILENT_FOOTSTEPS = registerItem("music_disc_silent_footsteps",
                new MusicDiscItem(6, ModSounds.SILENT_FOOTSTEPS, new FabricItemSettings().maxCount(1), 170));
        public static final Item MUSIC_BOX_MUSIC_DISC = registerItem("music_box_music_disc",
                new MusicDiscItem(6, ModSounds.MUSIC_BOX, new FabricItemSettings().maxCount(1), 16));

        public static final Item ROGUE_HELMET = registerItem("rogue_helmet", new RogueArmorItem(ArmorItem.Type.HELMET, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .luck(1.0)
                .build()));

        public static final Item ROGUE_CHESTPLATE = registerItem("rogue_chestplate", new RogueArmorItem(ArmorItem.Type.CHESTPLATE, FAArmorAttributes.builder()
                .armor(8.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .build()));

        public static final Item ROGUE_LEGGINGS = registerItem("rogue_leggings", new RogueArmorItem(ArmorItem.Type.LEGGINGS, FAArmorAttributes.builder()
                .armor(6.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.1)
                .build()));

        public static final Item ROGUE_BOOTS = registerItem("rogue_boots", new RogueArmorItem(ArmorItem.Type.BOOTS, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.05)
                .build()));

        public static final Item WIZARD_HELMET = registerItem("wizard_helmet", new WizardArmorItem(ArmorItem.Type.HELMET, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .luck(1.0)
                .build()));

        public static final Item WIZARD_CHESTPLATE = registerItem("wizard_chestplate", new WizardArmorItem(ArmorItem.Type.CHESTPLATE, FAArmorAttributes.builder()
                .armor(8.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .build()));

        public static final Item WIZARD_LEGGINGS = registerItem("wizard_leggings", new WizardArmorItem(ArmorItem.Type.LEGGINGS, FAArmorAttributes.builder()
                .armor(6.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.1)
                .build()));

        public static final Item WIZARD_BOOTS = registerItem("wizard_boots", new WizardArmorItem(ArmorItem.Type.BOOTS, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.05)
                .build()));

        public static final Item CLERIC_HELMET = registerItem("cleric_helmet", new ClericArmorItem(ArmorItem.Type.HELMET, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .luck(1.0)
                .build()));

        public static final Item CLERIC_CHESTPLATE = registerItem("cleric_chestplate", new ClericArmorItem(ArmorItem.Type.CHESTPLATE, FAArmorAttributes.builder()
                .armor(8.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .build()));

        public static final Item CLERIC_LEGGINGS = registerItem("cleric_leggings", new ClericArmorItem(ArmorItem.Type.LEGGINGS, FAArmorAttributes.builder()
                .armor(6.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.1)
                .build()));

        public static final Item CLERIC_BOOTS = registerItem("cleric_boots", new ClericArmorItem(ArmorItem.Type.BOOTS, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.05)
                .build()));

        public static final Item BLOOD_HUNTER_HELMET = registerItem("blood_hunter_helmet", new BloodHunterArmorItem(ArmorItem.Type.HELMET, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .luck(1.0)
                .build()));

        public static final Item BLOOD_HUNTER_CHESTPLATE = registerItem("blood_hunter_chestplate", new BloodHunterArmorItem(ArmorItem.Type.CHESTPLATE, FAArmorAttributes.builder()
                .armor(8.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .build()));

        public static final Item BLOOD_HUNTER_LEGGINGS = registerItem("blood_hunter_leggings", new BloodHunterArmorItem(ArmorItem.Type.LEGGINGS, FAArmorAttributes.builder()
                .armor(6.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.1)
                .build()));

        public static final Item BLOOD_HUNTER_BOOTS = registerItem("blood_hunter_boots", new BloodHunterArmorItem(ArmorItem.Type.BOOTS, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.05)
                .build()));

        public static final Item ASSASSIN_HELMET = registerItem("assassin_helmet", new AssassinArmorItem(ArmorItem.Type.HELMET, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .luck(1.0)
                .build()));

        public static final Item ASSASSIN_CHESTPLATE = registerItem("assassin_chestplate", new AssassinArmorItem(ArmorItem.Type.CHESTPLATE, FAArmorAttributes.builder()
                .armor(8.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .build()));

        public static final Item ASSASSIN_LEGGINGS = registerItem("assassin_leggings", new AssassinArmorItem(ArmorItem.Type.LEGGINGS, FAArmorAttributes.builder()
                .armor(6.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.1)
                .build()));

        public static final Item ASSASSIN_BOOTS = registerItem("assassin_boots", new AssassinArmorItem(ArmorItem.Type.BOOTS, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.05)
                .build()));

        public static final Item GOLDEN_HORNS_HELMET = registerItem("golden_horns_helmet", new GoldenHornsArmorItem(ArmorItem.Type.HELMET, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .luck(1.0)
                .build()));

        public static final Item GOLDEN_HORNS_CHESTPLATE = registerItem("golden_horns_chestplate", new GoldenHornsArmorItem(ArmorItem.Type.CHESTPLATE, FAArmorAttributes.builder()
                .armor(8.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .build()));

        public static final Item GOLDEN_HORNS_LEGGINGS = registerItem("golden_horns_leggings", new GoldenHornsArmorItem(ArmorItem.Type.LEGGINGS, FAArmorAttributes.builder()
                .armor(6.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.1)
                .build()));

        public static final Item GOLDEN_HORNS_BOOTS = registerItem("golden_horns_boots", new GoldenHornsArmorItem(ArmorItem.Type.BOOTS, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.05)
                .build()));

        public static final Item HOLY_ARMOR_HELMET = registerItem("holy_armor_helmet", new HolyArmorArmorItem(ArmorItem.Type.HELMET, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .luck(1.0)
                .build()));

        public static final Item HOLY_ARMOR_CHESTPLATE = registerItem("holy_armor_chestplate", new HolyArmorArmorItem(ArmorItem.Type.CHESTPLATE, FAArmorAttributes.builder()
                .armor(8.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .build()));

        public static final Item HOLY_ARMOR_LEGGINGS = registerItem("holy_armor_leggings", new HolyArmorArmorItem(ArmorItem.Type.LEGGINGS, FAArmorAttributes.builder()
                .armor(6.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.1)
                .build()));
        
        public static final Item HOLY_ARMOR_BOOTS = registerItem("holy_armor_boots", new HolyArmorArmorItem(ArmorItem.Type.BOOTS, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.05)
                .build()));

        public static final Item KNIGHT_HELMET = registerItem("knight_helmet", new KnightArmorItem(ArmorItem.Type.HELMET, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .luck(1.0)
                .build()));

        public static final Item KNIGHT_CHESTPLATE = registerItem("knight_chestplate", new KnightArmorItem(ArmorItem.Type.CHESTPLATE, FAArmorAttributes.builder()
                .armor(8.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .build()));

        public static final Item KNIGHT_LEGGINGS = registerItem("knight_leggings", new KnightArmorItem(ArmorItem.Type.LEGGINGS, FAArmorAttributes.builder()
                .armor(6.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.1)
                .build()));

        public static final Item KNIGHT_BOOTS = registerItem("knight_boots", new KnightArmorItem(ArmorItem.Type.BOOTS, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.05)
                .build()));

        public static final Item PRISMARINE_HELMET = registerItem("prismarine_helmet", new PrismarineArmorItem(ArmorItem.Type.HELMET, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .luck(1.0)
                .build()));

        public static final Item PRISMARINE_CHESTPLATE = registerItem("prismarine_chestplate", new PrismarineArmorItem(ArmorItem.Type.CHESTPLATE, FAArmorAttributes.builder()
                .armor(8.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .build()));

        public static final Item PRISMARINE_LEGGINGS = registerItem("prismarine_leggings", new PrismarineArmorItem(ArmorItem.Type.LEGGINGS, FAArmorAttributes.builder()
                .armor(6.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.1)
                .build()));

        public static final Item PRISMARINE_BOOTS = registerItem("prismarine_boots", new PrismarineArmorItem(ArmorItem.Type.BOOTS, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.05)
                .build()));

        public static final Item ROBE_HELMET = registerItem("robe_helmet", new RobeArmorItem(ArmorItem.Type.HELMET, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .luck(1.0)
                .build()));

        public static final Item ROBE_CHESTPLATE = registerItem("robe_chestplate", new RobeArmorItem(ArmorItem.Type.CHESTPLATE, FAArmorAttributes.builder()
                .armor(8.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .build()));

        public static final Item ROBE_LEGGINGS = registerItem("robe_leggings", new RobeArmorItem(ArmorItem.Type.LEGGINGS, FAArmorAttributes.builder()
                .armor(6.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.1)
                .build()));

        public static final Item ROBE_BOOTS = registerItem("robe_boots", new RobeArmorItem(ArmorItem.Type.BOOTS, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.05)
                .build()));

        public static final Item STEAMPUNK_HELMET = registerItem("steampunk_helmet", new SteamPunkArmorItem(ArmorItem.Type.HELMET, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .luck(1.0)
                .build()));

        public static final Item STEAMPUNK_CHESTPLATE = registerItem("steampunk_chestplate", new SteamPunkArmorItem(ArmorItem.Type.CHESTPLATE, FAArmorAttributes.builder()
                .armor(8.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .build()));

        public static final Item STEAMPUNK_LEGGINGS = registerItem("steampunk_leggings", new SteamPunkArmorItem(ArmorItem.Type.LEGGINGS, FAArmorAttributes.builder()
                .armor(6.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.1)
                .build()));

        public static final Item STEAMPUNK_BOOTS = registerItem("steampunk_boots", new SteamPunkArmorItem(ArmorItem.Type.BOOTS, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.05)
                .build()));

        public static final Item WARRIOR_HELMET = registerItem("warrior_helmet", new WarriorArmorItem(ArmorItem.Type.HELMET, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .luck(1.0)
                .build()));

        public static final Item WARRIOR_CHESTPLATE = registerItem("warrior_chestplate", new WarriorArmorItem(ArmorItem.Type.CHESTPLATE, FAArmorAttributes.builder()
                .armor(8.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .build()));

        public static final Item WARRIOR_LEGGINGS = registerItem("warrior_leggings", new WarriorArmorItem(ArmorItem.Type.LEGGINGS, FAArmorAttributes.builder()
                .armor(6.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.1)
                .build()));

        public static final Item WARRIOR_BOOTS = registerItem("warrior_boots", new WarriorArmorItem(ArmorItem.Type.BOOTS, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.05)
                .build()));

        public static final Item WITHER_HELMET = registerItem("wither_helmet", new WitherArmorItem(ArmorItem.Type.HELMET, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .luck(1.0)
                .build()));

        public static final Item WITHER_CHESTPLATE = registerItem("wither_chestplate", new WitherArmorItem(ArmorItem.Type.CHESTPLATE, FAArmorAttributes.builder()
                .armor(8.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .build()));

        public static final Item WITHER_LEGGINGS = registerItem("wither_leggings", new WitherArmorItem(ArmorItem.Type.LEGGINGS, FAArmorAttributes.builder()
                .armor(6.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.1)
                .build()));

        public static final Item WITHER_BOOTS = registerItem("wither_boots", new WitherArmorItem(ArmorItem.Type.BOOTS, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.05)
                .build()));

        public static final Item WOODEN_HELMET = registerItem("wooden_helmet", new WoodenArmorItem(ArmorItem.Type.HELMET, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .luck(1.0)
                .build()));

        public static final Item WOODEN_CHESTPLATE = registerItem("wooden_chestplate", new WoodenArmorItem(ArmorItem.Type.CHESTPLATE, FAArmorAttributes.builder()
                .armor(8.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .build()));

        public static final Item WOODEN_LEGGINGS = registerItem("wooden_leggings", new WoodenArmorItem(ArmorItem.Type.LEGGINGS, FAArmorAttributes.builder()
                .armor(6.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.1)
                .build()));

        public static final Item WOODEN_BOOTS = registerItem("wooden_boots", new WoodenArmorItem(ArmorItem.Type.BOOTS, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.05)
                .build()));
                

        public static final Item HOBBIT_SPAWN_EGG = registerItem("hobbit_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.HOBBIT, 0x5C7A2E, 0xD9A877, new FabricItemSettings()));

        private static Item registerItem(String name, Item item) {
                return Registry.register(Registries.ITEM, new Identifier(DnDClasses.MOD_ID, name), item);
        }

        public static void registerModItems() {
                DnDClasses.LOGGER.info("Registering Mod Items for " + DnDClasses.MOD_ID);
        }
}