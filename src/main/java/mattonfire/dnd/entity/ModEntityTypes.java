package mattonfire.dnd.entity;

import mattonfire.dnd.classes.DnDClasses;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntityTypes {
    public static final EntityType<WyvernEntity> WYVERN = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "wyvern"),
            FabricEntityTypeBuilder.<WyvernEntity>create(SpawnGroup.CREATURE, WyvernEntity::new)
                    .dimensions(EntityDimensions.fixed(1.5f, 1.5f))
                    .build()
    );

    public static final EntityType<LightningChaserEntity> LIGHTNING_CHASER = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "lightning_chaser"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, LightningChaserEntity::new)
                    .dimensions(EntityDimensions.fixed(1.5f, 1.5f))
                    .build()
    );

    // The Lightning Chaser's model with an icy texture; lives only in frost lairs on Frozen Peaks
    public static final EntityType<FrostDrakeEntity> FROST_DRAKE = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "frost_drake"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, FrostDrakeEntity::new)
                    .dimensions(EntityDimensions.fixed(1.5f, 1.5f))
                    .build()
    );

    public static final EntityType<EmberWyvernEntity> EMBER_WYVERN = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "ember_wyvern"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, EmberWyvernEntity::new)
                    .dimensions(EntityDimensions.fixed(1.5f, 1.5f))
                    .fireImmune()
                    .build()
    );

    // The Necromancer's Raise Dead summon: a Wyvern drawn at BoneWyvernEntity.SCALE
    public static final EntityType<BoneWyvernEntity> BONE_WYVERN = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "bone_wyvern"),
            FabricEntityTypeBuilder.create(SpawnGroup.MISC, BoneWyvernEntity::new)
                    .dimensions(EntityDimensions.fixed(0.8f, 0.7f))
                    .fireImmune()
                    .build()
    );

    public static final EntityType<RiverPikehornEntity> RIVER_PIKEHORN = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "river_pikehorn"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, RiverPikehornEntity::new)
                    .dimensions(EntityDimensions.fixed(1.0f, 1.0f))
                    .build()
    );

    public static final EntityType<MagmamuncherEntity> MAGMAMUNCHER = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "magmamuncher"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, MagmamuncherEntity::new)
                    .dimensions(EntityDimensions.fixed(1.5f, 1.5f))
                    .fireImmune()
                    .build()
    );

    public static final EntityType<MagmamuncherAlphaEntity> MAGMAMUNCHER_ALPHA = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "magmamuncher_alpha"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, MagmamuncherAlphaEntity::new)
                    .dimensions(EntityDimensions.fixed(2.6f, 1.6f))
                    .fireImmune()
                    .build()
    );

    public static final EntityType<GoblinWarriorEntity> GOBLIN_WARRIOR = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "goblin_warrior"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, GoblinWarriorEntity::new)
                    .dimensions(EntityDimensions.fixed(0.8f, 1.5f))
                    .fireImmune()
                    .build()
    );

    public static final EntityType<GoblinWarlordEntity> GOBLIN_WARLORD = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "goblin_warlord"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, GoblinWarlordEntity::new)
                    .dimensions(EntityDimensions.fixed(1.1f, 2.1f))
                    .fireImmune()
                    .build()
    );

    public static final EntityType<HobbitEntity> HOBBIT = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "hobbit"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, HobbitEntity::new)
                    .dimensions(EntityDimensions.fixed(0.5f, 1.1f))
                    .build()
    );

    public static final EntityType<MountainDwarfEntity> MOUNTAIN_DWARF = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "mountain_dwarf"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, MountainDwarfEntity::new)
                    .dimensions(EntityDimensions.fixed(0.7f, 1.4f))
                    .build()
    );

    public static final EntityType<ElfEntity> WOOD_ELF = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "wood_elf"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, ElfEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .build()
    );

    public static final EntityType<ElfWardenEntity> ELF_WARDEN = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "elf_warden"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, ElfWardenEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .build()
    );

    /** The Speaker of an elven enclave, in the Speaker's Hall. */
    public static final EntityType<ElfMerchantEntity> ELF_SPEAKER = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "elf_speaker"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, ElfMerchantEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .build()
    );

    /** The Fletcher of an elven enclave, at the archery glade. */
    public static final EntityType<ElfMerchantEntity> ELF_FLETCHER = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "elf_fletcher"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, ElfMerchantEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .build()
    );

    /** A Dragonborn Templar, the guard of a Dragon Temple: the player model at 1.05x. */
    public static final EntityType<DragonbornTemplarEntity> DRAGONBORN_TEMPLAR = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "dragonborn_templar"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, DragonbornTemplarEntity::new)
                    .dimensions(EntityDimensions.fixed(0.63f, 2.05f))
                    .build()
    );

    /** The Dragonborn Elder of a Dragon Temple, in the sanctum. */
    public static final EntityType<DragonbornElderEntity> DRAGONBORN_ELDER = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "dragonborn_elder"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, DragonbornElderEntity::new)
                    .dimensions(EntityDimensions.fixed(0.63f, 2.05f))
                    .build()
    );

    /** An Orc Warrior of an Orc Stronghold: the player model at 1.1x tall, 1.15x wide. */
    public static final EntityType<OrcWarriorEntity> ORC_WARRIOR = orc("orc_warrior", OrcWarriorEntity::new, 0.69f, 2.0f);
    /** An Orc Archer on a stronghold's gate towers. */
    public static final EntityType<OrcArcherEntity> ORC_ARCHER = orc("orc_archer", OrcArcherEntity::new, 0.69f, 2.0f);
    /** The Orc Quartermaster at a stronghold's smithy. */
    public static final EntityType<OrcQuartermasterEntity> ORC_QUARTERMASTER = orc("orc_quartermaster", OrcQuartermasterEntity::new, 0.69f, 2.0f);
    /** The Orc Champion of the Trial of Strength: 1.15x tall, 1.2x wide. */
    public static final EntityType<OrcChampionEntity> ORC_CHAMPION = orc("orc_champion", OrcChampionEntity::new, 0.72f, 2.07f);
    /** The Orc War Chief, a stronghold's boss: 1.25x tall, 1.3x wide. */
    public static final EntityType<OrcWarChiefEntity> ORC_WAR_CHIEF = orc("orc_war_chief", OrcWarChiefEntity::new, 0.78f, 2.25f);

    /** A Tiefling Exile of a Brimstone Sanctum: the player model with horns and a tail. Fire immune. */
    public static final EntityType<TieflingExileEntity> TIEFLING_EXILE = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "tiefling_exile"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, TieflingExileEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .fireImmune()
                    .build()
    );

    /** The Pactbroker of a Brimstone Sanctum, in the Pact Hall. Fire immune. */
    public static final EntityType<TieflingPactbrokerEntity> TIEFLING_PACTBROKER = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "tiefling_pactbroker"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, TieflingPactbrokerEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .fireImmune()
                    .build()
    );

    /** A Gnome Tinker of Cogsdeep: the player model at the Small scale (0.65), with a big nose. A merchant. */
    public static final EntityType<GnomeTinkerEntity> GNOME_TINKER = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "gnome_tinker"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, GnomeTinkerEntity::new)
                    .dimensions(EntityDimensions.fixed(0.4f, 1.25f))
                    .build()
    );

    /** A Clockwork Sentinel of Cogsdeep: a copper golem at 0.75 of an iron golem's size. */
    public static final EntityType<ClockworkSentinelEntity> CLOCKWORK_SENTINEL = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "clockwork_sentinel"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, ClockworkSentinelEntity::new)
                    .dimensions(EntityDimensions.fixed(1.05f, 2.0f))
                    .trackRangeChunks(10)
                    .build()
    );

    private static <T extends OrcEntity> EntityType<T> orc(String name, EntityType.EntityFactory<T> factory, float width, float height) {
        return Registry.register(Registries.ENTITY_TYPE, new Identifier(DnDClasses.MOD_ID, name),
                FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, factory)
                        .dimensions(EntityDimensions.fixed(width, height))
                        .build());
    }

    public static final EntityType<MimicEntity> MIMIC = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "mimic"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, MimicEntity::new)
                    .dimensions(EntityDimensions.fixed(0.875f, 0.875f))
                    .build()
    );

    public static final EntityType<OwlbearEntity> OWLBEAR = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "owlbear"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, OwlbearEntity::new)
                    .dimensions(EntityDimensions.fixed(1.4f, 1.9f))
                    .build()
    );

    public static final EntityType<GelatinousCubeEntity> GELATINOUS_CUBE = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "gelatinous_cube"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, GelatinousCubeEntity::new)
                    .dimensions(EntityDimensions.fixed(2.0f, 2.0f))
                    .build()
    );

    public static final EntityType<MindFlayerEntity> MIND_FLAYER = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "mind_flayer"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, MindFlayerEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .build()
    );

    public static final EntityType<LichEntity> LICH = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "lich"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, LichEntity::new)
                    .dimensions(EntityDimensions.fixed(0.9f, 2.5f))
                    .build()
    );

    public static final EntityType<PhylacteryEntity> PHYLACTERY = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "phylactery"),
            FabricEntityTypeBuilder.create(SpawnGroup.MISC, PhylacteryEntity::new)
                    .dimensions(EntityDimensions.fixed(0.8f, 1.25f))
                    .fireImmune()
                    .build()
    );

    public static final EntityType<BeholderEntity> BEHOLDER = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "beholder"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, BeholderEntity::new)
                    .dimensions(EntityDimensions.fixed(2.25f, 2.4f))
                    .build()
    );

    // Medusa: the Gorgon's Temple lair boss (design 8, section 6)
    public static final EntityType<MedusaEntity> MEDUSA = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "medusa"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, MedusaEntity::new)
                    // 1.95 high, not 2.1: she has to fit the jungle temple's two-block rooms
                    .dimensions(EntityDimensions.fixed(0.8f, 1.95f))
                    .build()
    );

    // The Halfling Sling's cobblestone (classes/Items/racial/HalflingSlingItem)
    public static final EntityType<SlingStoneEntity> SLING_STONE = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "sling_stone"),
            FabricEntityTypeBuilder.<SlingStoneEntity>create(SpawnGroup.MISC, SlingStoneEntity::new)
                    .dimensions(EntityDimensions.fixed(0.25f, 0.25f))
                    .trackRangeBlocks(4).trackedUpdateRate(10)
                    .build()
    );

    // The invisible seat a player sits on during a campfire short rest (classes/Rest/RestSeatEntity)
    public static final EntityType<mattonfire.dnd.classes.Rest.RestSeatEntity> REST_SEAT = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "rest_seat"),
            FabricEntityTypeBuilder.<mattonfire.dnd.classes.Rest.RestSeatEntity>create(SpawnGroup.MISC,
                            mattonfire.dnd.classes.Rest.RestSeatEntity::new)
                    .dimensions(EntityDimensions.fixed(0.0f, 0.0f))
                    .disableSummon().disableSaving()
                    .trackRangeBlocks(10).trackedUpdateRate(20)
                    .build()
    );

    public static void registerEntityTypes() {
        FabricDefaultAttributeRegistry.register(WYVERN, WyvernEntity.createWyvernAttributes());
        FabricDefaultAttributeRegistry.register(LIGHTNING_CHASER, LairDragonEntity.createLairDragonAttributes());
        FabricDefaultAttributeRegistry.register(FROST_DRAKE, LairDragonEntity.createLairDragonAttributes());
        FabricDefaultAttributeRegistry.register(EMBER_WYVERN, EmberWyvernEntity.createEmberWyvernAttributes());
        FabricDefaultAttributeRegistry.register(BONE_WYVERN, BoneWyvernEntity.createBoneWyvernAttributes());
        FabricDefaultAttributeRegistry.register(RIVER_PIKEHORN, RiverPikehornEntity.createRiverPikehornAttributes());
        FabricDefaultAttributeRegistry.register(MAGMAMUNCHER, MagmamuncherEntity.createMagmamuncherAttributes());
        FabricDefaultAttributeRegistry.register(MAGMAMUNCHER_ALPHA, MagmamuncherAlphaEntity.createMagmamuncherAlphaAttributes());
        FabricDefaultAttributeRegistry.register(GOBLIN_WARRIOR, GoblinWarriorEntity.createGoblinWarriorAttributes());
        FabricDefaultAttributeRegistry.register(GOBLIN_WARLORD, GoblinWarlordEntity.createGoblinWarlordAttributes());
        FabricDefaultAttributeRegistry.register(HOBBIT, HobbitEntity.createHobbitAttributes());
        FabricDefaultAttributeRegistry.register(MOUNTAIN_DWARF, MountainDwarfEntity.createMountainDwarfAttributes());
        FabricDefaultAttributeRegistry.register(MIMIC, MimicEntity.createMimicAttributes());
        FabricDefaultAttributeRegistry.register(WOOD_ELF, ElfEntity.createElfAttributes());
        FabricDefaultAttributeRegistry.register(ELF_WARDEN, ElfWardenEntity.createElfWardenAttributes());
        FabricDefaultAttributeRegistry.register(ELF_SPEAKER, ElfMerchantEntity.createSpeakerAttributes());
        FabricDefaultAttributeRegistry.register(ELF_FLETCHER, ElfMerchantEntity.createFletcherAttributes());
        FabricDefaultAttributeRegistry.register(DRAGONBORN_TEMPLAR, DragonbornTemplarEntity.createTemplarAttributes());
        FabricDefaultAttributeRegistry.register(DRAGONBORN_ELDER, DragonbornElderEntity.createElderAttributes());
        FabricDefaultAttributeRegistry.register(TIEFLING_EXILE, TieflingExileEntity.createExileAttributes());
        FabricDefaultAttributeRegistry.register(TIEFLING_PACTBROKER, TieflingPactbrokerEntity.createPactbrokerAttributes());
        FabricDefaultAttributeRegistry.register(GNOME_TINKER, GnomeTinkerEntity.createTinkerAttributes());
        FabricDefaultAttributeRegistry.register(CLOCKWORK_SENTINEL, ClockworkSentinelEntity.createSentinelAttributes());
        FabricDefaultAttributeRegistry.register(ORC_WARRIOR, OrcWarriorEntity.createOrcWarriorAttributes());
        FabricDefaultAttributeRegistry.register(ORC_ARCHER, OrcArcherEntity.createOrcArcherAttributes());
        FabricDefaultAttributeRegistry.register(ORC_QUARTERMASTER, OrcQuartermasterEntity.createOrcQuartermasterAttributes());
        FabricDefaultAttributeRegistry.register(ORC_CHAMPION, OrcChampionEntity.createOrcChampionAttributes());
        FabricDefaultAttributeRegistry.register(ORC_WAR_CHIEF, OrcWarChiefEntity.createOrcWarChiefAttributes());
        FabricDefaultAttributeRegistry.register(OWLBEAR, OwlbearEntity.createOwlbearAttributes());
        FabricDefaultAttributeRegistry.register(GELATINOUS_CUBE, GelatinousCubeEntity.createGelatinousCubeAttributes());
        FabricDefaultAttributeRegistry.register(MIND_FLAYER, MindFlayerEntity.createMindFlayerAttributes());
        FabricDefaultAttributeRegistry.register(LICH, LichEntity.createLichAttributes());
        FabricDefaultAttributeRegistry.register(PHYLACTERY, PhylacteryEntity.createPhylacteryAttributes());
        LichEntity.registerEvents();
        FabricDefaultAttributeRegistry.register(BEHOLDER, BeholderEntity.createBeholderAttributes());
        FabricDefaultAttributeRegistry.register(MEDUSA, MedusaEntity.createMedusaAttributes());
    }
}
