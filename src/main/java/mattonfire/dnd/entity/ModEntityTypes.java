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
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, WyvernEntity::new)
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

    public static final EntityType<EmberWyvernEntity> EMBER_WYVERN = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "ember_wyvern"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, EmberWyvernEntity::new)
                    .dimensions(EntityDimensions.fixed(1.5f, 1.5f))
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

    public static void registerEntityTypes() {
        FabricDefaultAttributeRegistry.register(WYVERN, WyvernEntity.createWyvernAttributes());
        FabricDefaultAttributeRegistry.register(LIGHTNING_CHASER, LightningChaserEntity.createLightningChaserAttributes());
        FabricDefaultAttributeRegistry.register(EMBER_WYVERN, EmberWyvernEntity.createEmberWyvernAttributes());
        FabricDefaultAttributeRegistry.register(RIVER_PIKEHORN, RiverPikehornEntity.createRiverPikehornAttributes());
        FabricDefaultAttributeRegistry.register(MAGMAMUNCHER, MagmamuncherEntity.createMagmamuncherAttributes());
        FabricDefaultAttributeRegistry.register(MAGMAMUNCHER_ALPHA, MagmamuncherAlphaEntity.createMagmamuncherAlphaAttributes());
        FabricDefaultAttributeRegistry.register(GOBLIN_WARRIOR, GoblinWarriorEntity.createGoblinWarriorAttributes());
        FabricDefaultAttributeRegistry.register(GOBLIN_WARLORD, GoblinWarlordEntity.createGoblinWarlordAttributes());
        FabricDefaultAttributeRegistry.register(HOBBIT, HobbitEntity.createHobbitAttributes());
        FabricDefaultAttributeRegistry.register(MOUNTAIN_DWARF, MountainDwarfEntity.createMountainDwarfAttributes());
        FabricDefaultAttributeRegistry.register(MIMIC, MimicEntity.createMimicAttributes());
        FabricDefaultAttributeRegistry.register(OWLBEAR, OwlbearEntity.createOwlbearAttributes());
        FabricDefaultAttributeRegistry.register(GELATINOUS_CUBE, GelatinousCubeEntity.createGelatinousCubeAttributes());
    }
}
