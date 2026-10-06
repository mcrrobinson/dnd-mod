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

    public static void registerEntityTypes() {
        FabricDefaultAttributeRegistry.register(WYVERN, WyvernEntity.createWyvernAttributes());
        FabricDefaultAttributeRegistry.register(LIGHTNING_CHASER, LightningChaserEntity.createLightningChaserAttributes());
        FabricDefaultAttributeRegistry.register(RIVER_PIKEHORN, RiverPikehornEntity.createRiverPikehornAttributes());
        FabricDefaultAttributeRegistry.register(MAGMAMUNCHER, MagmamuncherEntity.createMagmamuncherAttributes());
        FabricDefaultAttributeRegistry.register(GOBLIN_WARRIOR, GoblinWarriorEntity.createGoblinWarriorAttributes());
    }
}
