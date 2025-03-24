package mattonfire.dnd.classes.Registry;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Items.DarkLordArmorItem;
import mattonfire.dnd.classes.Items.ExtendedSwordItem;
import mattonfire.dnd.classes.Items.ModArmorMaterials;
import mattonfire.dnd.classes.Items.MonkStaff;
import mattonfire.dnd.classes.Items.SilverKnightArmorItem;
import mattonfire.dnd.classes.Items.ThiefArmorItem;
import mattonfire.dnd.classes.Items.WizardArmorItem;
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

        public static final Item PINK_GARNET_HELMET = registerItem("pink_garnet_helmet",
                new ArmorItem(ModArmorMaterials.PINK_GARNET, ArmorItem.Type.HELMET, new Item.Settings()
                        .maxDamage(ModArmorMaterials.PINK_GARNET.getDurability(ArmorItem.Type.HELMET))));
        public static final Item PINK_GARNET_CHESTPLATE = registerItem("pink_garnet_chestplate",
                new ArmorItem(ModArmorMaterials.PINK_GARNET, ArmorItem.Type.CHESTPLATE, new Item.Settings()
                        .maxDamage(ModArmorMaterials.PINK_GARNET.getDurability(ArmorItem.Type.CHESTPLATE))));
        public static final Item PINK_GARNET_LEGGINGS = registerItem("pink_garnet_leggings",
                new ArmorItem(ModArmorMaterials.PINK_GARNET, ArmorItem.Type.LEGGINGS, new Item.Settings()
                        .maxDamage(ModArmorMaterials.PINK_GARNET.getDurability(ArmorItem.Type.LEGGINGS))));
        public static final Item PINK_GARNET_BOOTS = registerItem("pink_garnet_boots",
                new ArmorItem(ModArmorMaterials.PINK_GARNET, ArmorItem.Type.BOOTS, new Item.Settings()
                        .maxDamage(ModArmorMaterials.PINK_GARNET.getDurability(ArmorItem.Type.BOOTS))));

        public static final Item THIEF_HELMET = registerItem("thief_helmet", new ThiefArmorItem(ArmorItem.Type.HELMET, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .luck(1.0)
                .build()));

        public static final Item THIEF_CHESTPLATE = registerItem("thief_chestplate", new ThiefArmorItem(ArmorItem.Type.CHESTPLATE, FAArmorAttributes.builder()
                .armor(8.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .build()));

        public static final Item THIEF_LEGGINGS = registerItem("thief_leggings", new ThiefArmorItem(ArmorItem.Type.LEGGINGS, FAArmorAttributes.builder()
                .armor(6.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.1)
                .build()));

        public static final Item THIEF_BOOTS = registerItem("thief_boots", new ThiefArmorItem(ArmorItem.Type.BOOTS, FAArmorAttributes.builder()
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

        public static final Item SILVER_KNIGHT_HELMET = registerItem("silver_knight_helmet", new SilverKnightArmorItem(ArmorItem.Type.HELMET, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .luck(1.0)
                .build()));

        public static final Item SILVER_KNIGHT_CHESTPLATE = registerItem("silver_knight_chestplate", new SilverKnightArmorItem(ArmorItem.Type.CHESTPLATE, FAArmorAttributes.builder()
                .armor(8.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .build()));

        public static final Item SILVER_KNIGHT_LEGGINGS = registerItem("silver_knight_leggings", new SilverKnightArmorItem(ArmorItem.Type.LEGGINGS, FAArmorAttributes.builder()
                .armor(6.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.1)
                .build()));

        public static final Item SILVER_KNIGHT_BOOTS = registerItem("silver_knight_boots", new SilverKnightArmorItem(ArmorItem.Type.BOOTS, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.05)
                .build()));

        public static final Item DARK_LORD_HELMET = registerItem("dark_lord_helmet", new DarkLordArmorItem(ArmorItem.Type.HELMET, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .luck(1.0)
                .build()));

        public static final Item DARK_LORD_CHESTPLATE = registerItem("dark_lord_chestplate", new DarkLordArmorItem(ArmorItem.Type.CHESTPLATE, FAArmorAttributes.builder()
                .armor(8.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .build()));

        public static final Item DARK_LORD_LEGGINGS = registerItem("dark_lord_leggings", new DarkLordArmorItem(ArmorItem.Type.LEGGINGS, FAArmorAttributes.builder()
                .armor(6.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.1)
                .build()));

        public static final Item DARK_LORD_BOOTS = registerItem("dark_lord_boots", new DarkLordArmorItem(ArmorItem.Type.BOOTS, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.05)
                .build()));

        private static Item registerItem(String name, Item item) {
                return Registry.register(Registries.ITEM, new Identifier(DnDClasses.MOD_ID, name), item);
        }

        public static void registerModItems() {
                DnDClasses.LOGGER.info("Registering Mod Items for " + DnDClasses.MOD_ID);
        }
}