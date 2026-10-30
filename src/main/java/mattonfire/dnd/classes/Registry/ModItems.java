package mattonfire.dnd.classes.Registry;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Items.AssassinArmorItem;
import mattonfire.dnd.classes.Items.BloodHunterArmorItem;
import mattonfire.dnd.classes.Items.ExtendedSwordItem;
import mattonfire.dnd.classes.Items.GoldenHornsArmorItem;
import mattonfire.dnd.classes.Items.HolyArmorArmorItem;
import mattonfire.dnd.classes.Items.InstrumentItem;
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
import net.minecraft.entity.effect.StatusEffects;
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

        // Bard instruments: the song buffs nearby players when a Bard plays it (InstrumentItem)
        public static final Item LUTE = registerItem("lute",
                new InstrumentItem(ModSounds.SONG_LUTE, StatusEffects.REGENERATION, new FabricItemSettings().maxCount(1)));
        public static final Item DRUM = registerItem("drum",
                new InstrumentItem(ModSounds.SONG_DRUM, StatusEffects.STRENGTH, new FabricItemSettings().maxCount(1)));
        public static final Item FLUTE = registerItem("flute",
                new InstrumentItem(ModSounds.SONG_FLUTE, StatusEffects.SPEED, new FabricItemSettings().maxCount(1)));

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

        public static final Item GOBLIN_WARLORD_SPAWN_EGG = registerItem("goblin_warlord_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.GOBLIN_WARLORD, 0x4A6B2A, 0xE0A82E, new FabricItemSettings()));

        public static final Item MAGMAMUNCHER_ALPHA_SPAWN_EGG = registerItem("magmamuncher_alpha_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.MAGMAMUNCHER_ALPHA, 0x3A1A12, 0xFF6A00, new FabricItemSettings()));

        public static final Item MOUNTAIN_DWARF_SPAWN_EGG = registerItem("mountain_dwarf_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.MOUNTAIN_DWARF, 0x5A5F66, 0x9C4A1E, new FabricItemSettings()));

        public static final Item WOOD_ELF_SPAWN_EGG = registerItem("wood_elf_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.WOOD_ELF, 0x5E8F4A, 0xE9E2C8, new FabricItemSettings()));

        public static final Item ELF_WARDEN_SPAWN_EGG = registerItem("elf_warden_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.ELF_WARDEN, 0x2F5D2A, 0xC9B98A, new FabricItemSettings()));

        public static final Item ELF_SPEAKER_SPAWN_EGG = registerItem("elf_speaker_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.ELF_SPEAKER, 0xE8ECEF, 0x9FB8C8, new FabricItemSettings()));

        public static final Item ELF_FLETCHER_SPAWN_EGG = registerItem("elf_fletcher_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.ELF_FLETCHER, 0x6B4E2E, 0x7FAF5A, new FabricItemSettings()));

        public static final Item DRAGONBORN_TEMPLAR_SPAWN_EGG = registerItem("dragonborn_templar_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.DRAGONBORN_TEMPLAR, 0x8E2A1E, 0xD4A73A, new FabricItemSettings()));

        public static final Item DRAGONBORN_ELDER_SPAWN_EGG = registerItem("dragonborn_elder_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.DRAGONBORN_ELDER, 0xC9A24A, 0x3A2A4A, new FabricItemSettings()));

        public static final Item TIEFLING_EXILE_SPAWN_EGG = registerItem("tiefling_exile_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.TIEFLING_EXILE, 0x8A1F2C, 0x2A1A2E, new FabricItemSettings()));

        public static final Item TIEFLING_PACTBROKER_SPAWN_EGG = registerItem("tiefling_pactbroker_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.TIEFLING_PACTBROKER, 0x5B2A6E, 0xC9A24A, new FabricItemSettings()));

        public static final Item MIMIC_SPAWN_EGG = registerItem("mimic_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.MIMIC, 0xA0692B, 0x3A2A1A, new FabricItemSettings()));

        public static final Item EMBER_WYVERN_SPAWN_EGG = registerItem("ember_wyvern_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.EMBER_WYVERN, 0x2A0E0C, 0xFF7A1A, new FabricItemSettings()));

        public static final Item CLASS_GUIDEBOOK = registerItem("class_guidebook",
                        new mattonfire.dnd.classes.Items.ClassGuidebookItem(new FabricItemSettings().maxCount(1)));

        public static final Item OWLBEAR_SPAWN_EGG = registerItem("owlbear_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.OWLBEAR, 0x5C3A20, 0xE6D6AE, new FabricItemSettings()));
        public static final Item TROLL_SPAWN_EGG = registerItem("troll_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.TROLL, 0x4E5A3A, 0x8A9A6A, new FabricItemSettings()));

        public static final Item LICH_SPAWN_EGG = registerItem("lich_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.LICH, 0x2A1838, 0x7FE0FF, new FabricItemSettings()));

        public static final Item MEDUSA_SPAWN_EGG = registerItem("medusa_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.MEDUSA, 0x6E8A5A, 0xE2D8BC, new FabricItemSettings()));

        public static final Item BEHOLDER_SPAWN_EGG = registerItem("beholder_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.BEHOLDER, 0x7A4860, 0x5AC850, new FabricItemSettings()));

        public static final Item WYVERN_SPAWN_EGG = registerItem("wyvern_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.WYVERN, 0x3E6B2E, 0xB8C46A, new FabricItemSettings()));

        public static final Item LIGHTNING_CHASER_SPAWN_EGG = registerItem("lightning_chaser_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.LIGHTNING_CHASER, 0x2E3A5C, 0xF2E85A, new FabricItemSettings()));
        public static final Item FROST_DRAKE_SPAWN_EGG = registerItem("frost_drake_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.FROST_DRAKE, 0xDCEBFA, 0x2F5FC8, new FabricItemSettings()));
        public static final Item ANCIENT_RED_DRAGON_SPAWN_EGG = registerItem("ancient_red_dragon_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.ANCIENT_RED_DRAGON, 0x8C1A12, 0xF2902A, new FabricItemSettings()));

        public static final Item RIVER_PIKEHORN_SPAWN_EGG = registerItem("river_pikehorn_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.RIVER_PIKEHORN, 0x3C6E78, 0xC9D8A0, new FabricItemSettings()));

        public static final Item MAGMAMUNCHER_SPAWN_EGG = registerItem("magmamuncher_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.MAGMAMUNCHER, 0x2E2622, 0xE0581A, new FabricItemSettings()));

        public static final Item GOBLIN_WARRIOR_SPAWN_EGG = registerItem("goblin_warrior_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.GOBLIN_WARRIOR, 0x4A6B2A, 0x8A8A8A, new FabricItemSettings()));

        public static final Item GELATINOUS_CUBE_SPAWN_EGG = registerItem("gelatinous_cube_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.GELATINOUS_CUBE, 0x7FD46A, 0x3C8A2E, new FabricItemSettings()));
        public static final Item RUST_MONSTER_SPAWN_EGG = registerItem("rust_monster_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.RUST_MONSTER, 0xB25626, 0x707C8E, new FabricItemSettings()));

        public static final Item MIND_FLAYER_SPAWN_EGG = registerItem("mind_flayer_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.MIND_FLAYER, 0x6E4A7E, 0xC9A3D6, new FabricItemSettings()));

        /** Magic item (attunement): +1 armor and +1 toughness for its bonded wearer, anywhere in the inventory. */
        public static final Item CLOAK_OF_PROTECTION = registerItem("cloak_of_protection",
                        new Item(new FabricItemSettings().maxCount(1)));

        /** Common scroll: identifies the unidentified magic item in the other hand. */
        public static final Item SCROLL_OF_IDENTIFY = registerItem("scroll_of_identify",
                        new mattonfire.dnd.magic.items.ScrollItem(mattonfire.dnd.magic.items.ScrollItem.Spell.IDENTIFY,
                                        new FabricItemSettings().maxCount(16)));

        /** Rare scroll: Remove Curse on yourself. */
        public static final Item SCROLL_OF_REMOVE_CURSE = registerItem("scroll_of_remove_curse",
                        new mattonfire.dnd.magic.items.ScrollItem(mattonfire.dnd.magic.items.ScrollItem.Spell.REMOVE_CURSE,
                                        new FabricItemSettings().maxCount(16)));

        // Wondrous and utility magic items (design 5, section 3.7); tiers in magic/items/WondrousItems
        public static final Item WAND_OF_MAGIC_MISSILES = registerItem("wand_of_magic_missiles",
                        new mattonfire.dnd.magic.items.WandOfMagicMissilesItem(new FabricItemSettings().maxCount(1)));
        public static final Item BAG_OF_HOLDING = registerItem("bag_of_holding",
                        new mattonfire.dnd.magic.items.BagOfHoldingItem(new FabricItemSettings().maxCount(1)));
        public static final Item DECANTER_OF_ENDLESS_WATER = registerItem("decanter_of_endless_water",
                        new mattonfire.dnd.magic.items.DecanterOfEndlessWaterItem(new FabricItemSettings().maxCount(1)));
        public static final Item IMMOVABLE_ROD = registerItem("immovable_rod",
                        new mattonfire.dnd.magic.items.ImmovableRodItem(new FabricItemSettings().maxCount(1)));
        public static final Item PERIAPT_OF_WOUND_CLOSURE = registerItem("periapt_of_wound_closure",
                        new Item(new FabricItemSettings().maxCount(1)));
        public static final Item RING_OF_PROTECTION = registerItem("ring_of_protection",
                        new Item(new FabricItemSettings().maxCount(1)));
        public static final Item AMULET_OF_HEALTH = registerItem("amulet_of_health",
                        new Item(new FabricItemSettings().maxCount(1)));
        public static final Item DOSS_LUTE = registerItem("doss_lute",
                        new mattonfire.dnd.magic.items.DossLuteItem(ModSounds.SONG_LUTE, StatusEffects.REGENERATION,
                                        new FabricItemSettings().maxCount(1)));
        public static final Item TOME_OF_CLEAR_THOUGHT = registerItem("tome_of_clear_thought",
                        new mattonfire.dnd.magic.items.TomeOfClearThoughtItem(new FabricItemSettings().maxCount(16)));

        // Boss and monster magic items (design 5, section 3.7, items 10-18 and 20); defs and effects in magic/items/BossItems
        public static final Item WARLORDS_WARHORN = registerItem("warlords_warhorn",
                        new mattonfire.dnd.magic.items.WarlordsWarhornItem(new FabricItemSettings().maxCount(1)));
        public static final Item HEART_OF_THE_FURNACE = registerItem("heart_of_the_furnace",
                        new mattonfire.dnd.magic.items.DescribedItem(new FabricItemSettings().maxCount(1).fireproof()));
        /** Diamond boots' stats (3 armor, 2 toughness, diamond durability). */
        public static final Item STORM_CHASER_BOOTS = registerItem("storm_chaser_boots",
                        new mattonfire.dnd.magic.items.DescribedItem.Armor(net.minecraft.item.ArmorMaterials.DIAMOND,
                                        net.minecraft.item.ArmorItem.Type.BOOTS, new FabricItemSettings()));
        public static final Item WYVERN_SCALE_CLOAK = registerItem("wyvern_scale_cloak",
                        new mattonfire.dnd.magic.items.DescribedItem(new FabricItemSettings().maxCount(1)));
        public static final Item BELT_OF_OWLBEAR_MIGHT = registerItem("belt_of_owlbear_might",
                        new mattonfire.dnd.magic.items.DescribedItem(new FabricItemSettings().maxCount(1)));
        /** 8 attack damage (1 + iron's 2 + 5), 1.4 attack speed (4 - 2.6). */
        public static final Item MACE_OF_DISRUPTION = registerItem("mace_of_disruption",
                        new mattonfire.dnd.magic.items.NamedWeaponItem(net.minecraft.item.ToolMaterials.IRON, 5, -2.6F,
                                        "item.dndclasses.mace_of_disruption.tooltip", true, new FabricItemSettings()));
        /** 9 attack damage (1 + diamond's 3 + 5), 1.6 attack speed (4 - 2.4). */
        public static final Item FROSTBRAND = registerItem("frostbrand",
                        new mattonfire.dnd.magic.items.NamedWeaponItem(net.minecraft.item.ToolMaterials.DIAMOND, 5, -2.4F,
                                        "item.dndclasses.frostbrand.tooltip", false, new FabricItemSettings()));
        public static final Item PHYLACTERY_LOCKET = registerItem("phylactery_locket",
                        new mattonfire.dnd.magic.items.PhylacteryLocketItem(new FabricItemSettings().maxCount(1)));
        public static final Item BURGLARS_RING = registerItem("burglars_ring",
                        new mattonfire.dnd.magic.items.BurglarsRingItem(new FabricItemSettings().maxCount(1)));
        public static final Item EYE_OF_THE_TYRANT = registerItem("eye_of_the_tyrant",
                        new mattonfire.dnd.magic.items.EyeOfTheTyrantItem(new FabricItemSettings().maxCount(1)));
        /** The Medusa's drop (design 8, section 6.5). */
        public static final Item GORGONS_GAZE = registerItem("gorgons_gaze",
                        new mattonfire.dnd.magic.items.GorgonsGazeItem(new FabricItemSettings().maxCount(1)));
        /** The Matron Mother's drop (design 9, 9.4): 7 attack damage (1 + iron's 2 + 4), 1.6 attack speed, reach +1. */
        public static final Item MATRONS_SCOURGE = registerItem("matrons_scourge",
                        new mattonfire.dnd.magic.items.MatronsScourgeItem(new FabricItemSettings()));
        /** Piwafwi, the Cloak of the Drow (design 9, 9.4): a chestplate-slot cloak, 2 armour. */
        public static final Item PIWAFWI = registerItem("piwafwi",
                        new mattonfire.dnd.magic.items.DescribedItem.Armor(mattonfire.dnd.magic.items.DrowItems.PIWAFWI_MATERIAL,
                                        ArmorItem.Type.CHESTPLATE, new FabricItemSettings()));
        /** The Ancient Red Dragon's drop (design 10, 5.11): 8 armour, 3 toughness, fireproof. */
        public static final Item RED_DRAGON_SCALE_MAIL = registerItem("red_dragon_scale_mail",
                        new mattonfire.dnd.magic.items.RedDragonScaleMailItem(new FabricItemSettings().fireproof()));
        /** Shed by an Ancient Red Dragon: sold to the Dragonborn Elder, brewed into Fire Resistance. */
        public static final Item ANCIENT_RED_SCALE = registerItem("ancient_red_scale",
                        new Item(new FabricItemSettings().fireproof()));

        // Signature magic items (design 10, 8.1-8.2): defs and effects in magic/items/VorpalSwordItem and SendingStones
        /** 8 attack damage (1 + netherite's 4 + 3), 1.6 attack speed, a built-in +3, unbreakable. */
        public static final Item VORPAL_SWORD = registerItem("vorpal_sword",
                        new mattonfire.dnd.magic.items.VorpalSwordItem(new FabricItemSettings().fireproof()));
        /** A pair of Sending Stones is one stack of 2 sharing a Pair id. */
        public static final Item SENDING_STONE = registerItem("sending_stone",
                        new mattonfire.dnd.magic.items.SendingStoneItem(new FabricItemSettings().maxCount(2)));


        // ---- Racial gear (kin stock): classes/Items/racial, docs/items/armor.md and racial-weapons.md ----
        public static final Item ELVEN_CHAIN_HELMET = registerItem("elven_chain_helmet",
                new mattonfire.dnd.classes.Items.racial.ElvenChainArmorItem(ArmorItem.Type.HELMET, FAArmorAttributes.builder()
                        .armor(2.0)
                        .armorToughness(1.0)
                        .build()));
        public static final Item ELVEN_CHAIN_CHESTPLATE = registerItem("elven_chain_chestplate",
                new mattonfire.dnd.classes.Items.racial.ElvenChainArmorItem(ArmorItem.Type.CHESTPLATE, FAArmorAttributes.builder()
                        .armor(5.0)
                        .armorToughness(1.0)
                        .build()));
        public static final Item ELVEN_CHAIN_LEGGINGS = registerItem("elven_chain_leggings",
                new mattonfire.dnd.classes.Items.racial.ElvenChainArmorItem(ArmorItem.Type.LEGGINGS, FAArmorAttributes.builder()
                        .armor(4.0)
                        .armorToughness(1.0)
                        .build()));
        public static final Item ELVEN_CHAIN_BOOTS = registerItem("elven_chain_boots",
                new mattonfire.dnd.classes.Items.racial.ElvenChainArmorItem(ArmorItem.Type.BOOTS, FAArmorAttributes.builder()
                        .armor(1.0)
                        .armorToughness(1.0)
                        .build()));
        public static final Item DWARVEN_PLATE_HELMET = registerItem("dwarven_plate_helmet",
                new mattonfire.dnd.classes.Items.racial.DwarvenPlateArmorItem(ArmorItem.Type.HELMET, FAArmorAttributes.builder()
                        .armor(3.0)
                        .armorToughness(2.0)
                        .knockbackResistance(0.1)
                        .build()));
        public static final Item DWARVEN_PLATE_CHESTPLATE = registerItem("dwarven_plate_chestplate",
                new mattonfire.dnd.classes.Items.racial.DwarvenPlateArmorItem(ArmorItem.Type.CHESTPLATE, FAArmorAttributes.builder()
                        .armor(8.0)
                        .armorToughness(2.0)
                        .knockbackResistance(0.1)
                        .build()));
        public static final Item DWARVEN_PLATE_LEGGINGS = registerItem("dwarven_plate_leggings",
                new mattonfire.dnd.classes.Items.racial.DwarvenPlateArmorItem(ArmorItem.Type.LEGGINGS, FAArmorAttributes.builder()
                        .armor(6.0)
                        .armorToughness(2.0)
                        .knockbackResistance(0.1)
                        .build()));
        public static final Item DWARVEN_PLATE_BOOTS = registerItem("dwarven_plate_boots",
                new mattonfire.dnd.classes.Items.racial.DwarvenPlateArmorItem(ArmorItem.Type.BOOTS, FAArmorAttributes.builder()
                        .armor(3.0)
                        .armorToughness(2.0)
                        .knockbackResistance(0.1)
                        .build()));
        public static final Item ORCISH_WARPLATE_HELMET = registerItem("orcish_warplate_helmet",
                new mattonfire.dnd.classes.Items.racial.OrcishWarplateArmorItem(ArmorItem.Type.HELMET, FAArmorAttributes.builder()
                        .armor(3.0)
                        .armorToughness(2.0)
                        .knockbackResistance(0.1)
                        .build()));
        public static final Item ORCISH_WARPLATE_CHESTPLATE = registerItem("orcish_warplate_chestplate",
                new mattonfire.dnd.classes.Items.racial.OrcishWarplateArmorItem(ArmorItem.Type.CHESTPLATE, FAArmorAttributes.builder()
                        .armor(7.0)
                        .armorToughness(2.0)
                        .knockbackResistance(0.1)
                        .build()));
        public static final Item ORCISH_WARPLATE_LEGGINGS = registerItem("orcish_warplate_leggings",
                new mattonfire.dnd.classes.Items.racial.OrcishWarplateArmorItem(ArmorItem.Type.LEGGINGS, FAArmorAttributes.builder()
                        .armor(6.0)
                        .armorToughness(2.0)
                        .knockbackResistance(0.1)
                        .build()));
        public static final Item ORCISH_WARPLATE_BOOTS = registerItem("orcish_warplate_boots",
                new mattonfire.dnd.classes.Items.racial.OrcishWarplateArmorItem(ArmorItem.Type.BOOTS, FAArmorAttributes.builder()
                        .armor(2.0)
                        .armorToughness(2.0)
                        .knockbackResistance(0.1)
                        .build()));
        public static final Item DRACONIC_SCALE_HELMET = registerItem("draconic_scale_helmet",
                new mattonfire.dnd.classes.Items.racial.DraconicScaleArmorItem(ArmorItem.Type.HELMET, FAArmorAttributes.builder()
                        .armor(3.0)
                        .armorToughness(3.0)
                        .build()));
        public static final Item DRACONIC_SCALE_CHESTPLATE = registerItem("draconic_scale_chestplate",
                new mattonfire.dnd.classes.Items.racial.DraconicScaleArmorItem(ArmorItem.Type.CHESTPLATE, FAArmorAttributes.builder()
                        .armor(8.0)
                        .armorToughness(3.0)
                        .build()));
        public static final Item DRACONIC_SCALE_LEGGINGS = registerItem("draconic_scale_leggings",
                new mattonfire.dnd.classes.Items.racial.DraconicScaleArmorItem(ArmorItem.Type.LEGGINGS, FAArmorAttributes.builder()
                        .armor(6.0)
                        .armorToughness(3.0)
                        .build()));
        public static final Item DRACONIC_SCALE_BOOTS = registerItem("draconic_scale_boots",
                new mattonfire.dnd.classes.Items.racial.DraconicScaleArmorItem(ArmorItem.Type.BOOTS, FAArmorAttributes.builder()
                        .armor(3.0)
                        .armorToughness(3.0)
                        .build()));
        public static final Item INFERNAL_REGALIA_HELMET = registerItem("infernal_regalia_helmet",
                new mattonfire.dnd.classes.Items.racial.InfernalRegaliaArmorItem(ArmorItem.Type.HELMET, FAArmorAttributes.builder()
                        .armor(3.0)
                        .armorToughness(2.0)
                        .build()));
        public static final Item INFERNAL_REGALIA_CHESTPLATE = registerItem("infernal_regalia_chestplate",
                new mattonfire.dnd.classes.Items.racial.InfernalRegaliaArmorItem(ArmorItem.Type.CHESTPLATE, FAArmorAttributes.builder()
                        .armor(7.0)
                        .armorToughness(2.0)
                        .build()));
        public static final Item INFERNAL_REGALIA_LEGGINGS = registerItem("infernal_regalia_leggings",
                new mattonfire.dnd.classes.Items.racial.InfernalRegaliaArmorItem(ArmorItem.Type.LEGGINGS, FAArmorAttributes.builder()
                        .armor(5.0)
                        .armorToughness(2.0)
                        .build()));
        public static final Item INFERNAL_REGALIA_BOOTS = registerItem("infernal_regalia_boots",
                new mattonfire.dnd.classes.Items.racial.InfernalRegaliaArmorItem(ArmorItem.Type.BOOTS, FAArmorAttributes.builder()
                        .armor(2.0)
                        .armorToughness(2.0)
                        .build()));
        /** 10 attack damage (1 + diamond's 3 + 6), 0.9 attack speed (4 - 3.1); +50% to goblins (RacialGear). */
        public static final Item DWARVEN_WARAXE = registerItem("dwarven_waraxe",
                new mattonfire.dnd.classes.Items.racial.RacialAxeItem(ToolMaterials.DIAMOND, 6.0F, -3.1F,
                        java.util.Set.of(mattonfire.dnd.classes.Race.DndRace.DWARF), new FabricItemSettings()));
        /** 11 attack damage (1 + diamond's 3 + 7), 0.8 attack speed (4 - 3.2). */
        public static final Item ORCISH_GREATAXE = registerItem("orcish_greataxe",
                new mattonfire.dnd.classes.Items.racial.RacialAxeItem(ToolMaterials.DIAMOND, 7.0F, -3.2F,
                        java.util.Set.of(mattonfire.dnd.classes.Race.DndRace.HALFORC), new FabricItemSettings()));
        /** +20% arrow speed, Power I built in. */
        public static final Item ELVEN_LONGBOW = registerItem("elven_longbow",
                new mattonfire.dnd.classes.Items.racial.ElvenLongbowItem(new FabricItemSettings().maxDamage(480)));
        /** Slings a cobblestone for 4 damage, 0.8 s cooldown. */
        public static final Item HALFLING_SLING = registerItem("halfling_sling",
                new mattonfire.dnd.classes.Items.racial.HalflingSlingItem(new FabricItemSettings().maxDamage(250)));
        /** A crossbow; the Tinkers sell it with Quick Charge III and Multishot (GnomishRepeaterItem.create). */
        public static final Item GNOMISH_REPEATER = registerItem("gnomish_repeater",
                new mattonfire.dnd.classes.Items.racial.GnomishRepeaterItem(new FabricItemSettings().maxDamage(520)));
        /** Shows what's inside a container you look at (TinkersGoggles). */
        public static final Item TINKERS_GOGGLES = registerItem("tinkers_goggles",
                new mattonfire.dnd.classes.Items.racial.TinkersGogglesItem(FAArmorAttributes.builder()
                        .armor(1.0)
                        .build()));
        public static final Item GNOME_TINKER_SPAWN_EGG = registerItem("gnome_tinker_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.GNOME_TINKER, 0x4E7A3E, 0xD6A840, new FabricItemSettings()));
        public static final Item CLOCKWORK_SENTINEL_SPAWN_EGG = registerItem("clockwork_sentinel_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.CLOCKWORK_SENTINEL, 0xC46C4E, 0x4EA484, new FabricItemSettings()));

        /** Dropped by goblin warriors (25%); the orc Quartermaster buys 3 for 2 emeralds. */
        public static final Item GOBLIN_WAR_TOKEN = registerItem("goblin_war_token", new Item(new FabricItemSettings()));
        /** Strength I for 60 s, Nausea for 5 s; sold by the orc Quartermaster. */
        // --- Basilisk (design 8, section 7) ---
        public static final Item BASILISK_SPAWN_EGG = registerItem("basilisk_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.BASILISK, 0x42543A, 0xFFD628, new FabricItemSettings()));
        public static final Item BASILISK_EYE = registerItem("basilisk_eye", new Item(new FabricItemSettings()));
        public static final Item SALVE_OF_STONE_TO_FLESH = registerItem("salve_of_stone_to_flesh",
                        new mattonfire.dnd.classes.Items.SalveOfStoneToFleshItem(new FabricItemSettings()));

        public static final Item GROG = registerItem("grog",
                new mattonfire.dnd.classes.Items.GrogItem(new FabricItemSettings()));
        public static final Item ORC_WARRIOR_SPAWN_EGG = registerItem("orc_warrior_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.ORC_WARRIOR, 0x5E7A3A, 0x6B4A2E, new FabricItemSettings()));
        public static final Item ORC_ARCHER_SPAWN_EGG = registerItem("orc_archer_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.ORC_ARCHER, 0x5E7A3A, 0x8E2B1F, new FabricItemSettings()));
        public static final Item ORC_QUARTERMASTER_SPAWN_EGG = registerItem("orc_quartermaster_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.ORC_QUARTERMASTER, 0x5E7A3A, 0x3A3A3A, new FabricItemSettings()));
        public static final Item ORC_CHAMPION_SPAWN_EGG = registerItem("orc_champion_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.ORC_CHAMPION, 0x4A6230, 0xC9C9C9, new FabricItemSettings()));
        public static final Item ORC_WAR_CHIEF_SPAWN_EGG = registerItem("orc_war_chief_spawn_egg",
                        new SpawnEggItem(mattonfire.dnd.entity.ModEntityTypes.ORC_WAR_CHIEF, 0x3E5228, 0xE0A82E, new FabricItemSettings()));

        /** The Dungeon Master's wand: encounters, freeze, inspect and blink on right-click (DmWand). */
        public static final Item DM_WAND = registerItem("dm_wand",
                new mattonfire.dnd.classes.Items.DmWandItem(new FabricItemSettings().maxCount(1).rarity(net.minecraft.util.Rarity.EPIC)));

        private static Item registerItem(String name, Item item) {
                return Registry.register(Registries.ITEM, new Identifier(DnDClasses.MOD_ID, name), item);
        }

        public static void registerModItems() {
                DnDClasses.LOGGER.info("Registering Mod Items for " + DnDClasses.MOD_ID);
        }
}