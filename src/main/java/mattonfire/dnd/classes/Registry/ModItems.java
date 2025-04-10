package mattonfire.dnd.classes.Registry;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Items.BloodHunterArmorItem;
import mattonfire.dnd.classes.Items.ExtendedSwordItem;
import mattonfire.dnd.classes.Items.ModArmorMaterials;
import mattonfire.dnd.classes.Items.MonkStaff;
import mattonfire.dnd.classes.Items.ClericArmorItem;
import mattonfire.dnd.classes.Items.RogueArmorItem;
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

        public static final Item blood_hunter_HELMET = registerItem("blood_hunter_helmet", new BloodHunterArmorItem(ArmorItem.Type.HELMET, FAArmorAttributes.builder()
                .armor(3.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .luck(1.0)
                .build()));

        public static final Item blood_hunter_CHESTPLATE = registerItem("blood_hunter_chestplate", new BloodHunterArmorItem(ArmorItem.Type.CHESTPLATE, FAArmorAttributes.builder()
                .armor(8.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.05)
                .attackSpeed(0.1)
                .build()));

        public static final Item blood_hunter_LEGGINGS = registerItem("blood_hunter_leggings", new BloodHunterArmorItem(ArmorItem.Type.LEGGINGS, FAArmorAttributes.builder()
                .armor(6.0)
                .armorToughness(3.0)
                .knockbackResistance(0.1)
                .movementSpeed(0.1)
                .attackSpeed(0.1)
                .build()));

        public static final Item blood_hunter_BOOTS = registerItem("blood_hunter_boots", new BloodHunterArmorItem(ArmorItem.Type.BOOTS, FAArmorAttributes.builder()
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