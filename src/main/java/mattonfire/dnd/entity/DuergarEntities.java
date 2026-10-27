package mattonfire.dnd.entity;

import java.util.List;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.ModItemGroup;
import mattonfire.dnd.world.underdark.Underdark;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.Heightmap;
import net.minecraft.world.ServerWorldAccess;

/**
 * Everything the Grey Clans register (design 9 T5), kept together so the Underdark tickets built in parallel touch
 * the shared registries as little as possible: the four duergar entity types (the mountain dwarf's 0.7 x 1.4 box,
 * 1.5x while Enlarged), their attributes, spawn eggs (in the DnD Classes tab), the Grey Reaches patrols and the
 * spawn rule ({@link #canDuergarSpawn}). Call {@link #register()} once from the mod initialiser.
 */
public final class DuergarEntities {
    public static final float WIDTH = 0.7F;
    public static final float HEIGHT = 1.4F;
    /** Natural spawns stop when this many duergar are within {@link #CROWD_RANGE} blocks. */
    public static final int MAX_NEARBY = 16;
    public static final double CROWD_RANGE = 48.0D;
    /** Grey Reaches patrols: weight and group size. */
    public static final int PATROL_WEIGHT = 8;
    public static final int PATROL_MIN = 2;
    public static final int PATROL_MAX = 3;

    public static final EntityType<DuergarSoldierEntity> DUERGAR_SOLDIER = type("duergar_soldier", DuergarSoldierEntity::new);
    public static final EntityType<DuergarArbalesterEntity> DUERGAR_ARBALESTER = type("duergar_arbalester", DuergarArbalesterEntity::new);
    public static final EntityType<DuergarFactorEntity> DUERGAR_FACTOR = type("duergar_factor", DuergarFactorEntity::new);
    public static final EntityType<DuergarTaskmasterEntity> DUERGAR_TASKMASTER = type("duergar_taskmaster", DuergarTaskmasterEntity::new);

    public static final Item DUERGAR_SOLDIER_SPAWN_EGG = egg("duergar_soldier", DUERGAR_SOLDIER, 0x6E6A66, 0x3A3836);
    public static final Item DUERGAR_ARBALESTER_SPAWN_EGG = egg("duergar_arbalester", DUERGAR_ARBALESTER, 0x6E6A66, 0x5A3A22);
    public static final Item DUERGAR_FACTOR_SPAWN_EGG = egg("duergar_factor", DUERGAR_FACTOR, 0x6E6A66, 0xC9A24A);
    public static final Item DUERGAR_TASKMASTER_SPAWN_EGG = egg("duergar_taskmaster", DUERGAR_TASKMASTER, 0x4A4744, 0xB33A1E);

    private DuergarEntities() {
    }

    /** Every duergar type. */
    public static List<EntityType<? extends DuergarEntity>> all() {
        return List.of(DUERGAR_SOLDIER, DUERGAR_ARBALESTER, DUERGAR_FACTOR, DUERGAR_TASKMASTER);
    }

    // Monsters for the mob cap (the patrols spawn like monsters), guards by behaviour.
    private static <T extends DuergarEntity> EntityType<T> type(String name, EntityType.EntityFactory<T> factory) {
        return Registry.register(Registries.ENTITY_TYPE, new Identifier(DnDClasses.MOD_ID, name),
                FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, factory)
                        .dimensions(EntityDimensions.fixed(WIDTH, HEIGHT))
                        .build());
    }

    private static Item egg(String name, EntityType<? extends MobEntity> type, int primary, int secondary) {
        return Registry.register(Registries.ITEM, new Identifier(DnDClasses.MOD_ID, name + "_spawn_egg"),
                new SpawnEggItem(type, primary, secondary, new FabricItemSettings()));
    }

    public static void register() {
        FabricDefaultAttributeRegistry.register(DUERGAR_SOLDIER, DuergarSoldierEntity.createSoldierAttributes());
        FabricDefaultAttributeRegistry.register(DUERGAR_ARBALESTER, DuergarArbalesterEntity.createArbalesterAttributes());
        FabricDefaultAttributeRegistry.register(DUERGAR_FACTOR, DuergarFactorEntity.createFactorAttributes());
        FabricDefaultAttributeRegistry.register(DUERGAR_TASKMASTER, DuergarTaskmasterEntity.createTaskmasterAttributes());
        DuergarEntity.registerStudy();

        ItemGroupEvents.modifyEntriesEvent(ModItemGroup.DND_CLASSES_ITEMGROUP).register(entries -> {
            entries.add(DUERGAR_SOLDIER_SPAWN_EGG);
            entries.add(DUERGAR_ARBALESTER_SPAWN_EGG);
            entries.add(DUERGAR_FACTOR_SPAWN_EGG);
            entries.add(DUERGAR_TASKMASTER_SPAWN_EGG);
        });

        // Patrols of soldiers in the Grey Reaches. Natural ones despawn; a Hold's own (T8) are persistent.
        BiomeModifications.addSpawn(BiomeSelectors.tag(Underdark.HABITAT_DUERGAR), SpawnGroup.MONSTER, DUERGAR_SOLDIER,
                PATROL_WEIGHT, PATROL_MIN, PATROL_MAX);
        for (EntityType<? extends DuergarEntity> type : all()) {
            SpawnRestriction.register(type, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                    DuergarEntities::canDuergarSpawn);
        }
    }

    /**
     * Natural and chunk spawns: not on Peaceful, in the dark (a monster's light rule), and at most {@link #MAX_NEARBY}
     * duergar within {@link #CROWD_RANGE} blocks. Structure spawn overrides (a Duergar Hold's) use this rule too, and inside a hold the light doesn't matter.
     */
    public static <T extends MobEntity> boolean canDuergarSpawn(EntityType<T> type, ServerWorldAccess world,
                                                               SpawnReason reason, BlockPos pos, Random random) {
        if (!MobEntity.canMobSpawn(type, world, reason, pos, random)) {
            return false;
        }
        if (reason != SpawnReason.NATURAL && reason != SpawnReason.CHUNK_GENERATION) {
            return true;
        }
        if (world.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        // A Duergar Hold's spawn_overrides top its garrison up whatever the light (its braziers light it all).
        boolean inHold = mattonfire.dnd.world.underdark.UnderdarkGarrisons.inside(world, pos,
                mattonfire.dnd.world.underdark.duergar.DuergarHolds.KEY);
        return (inHold || HostileEntity.isSpawnDark(world, pos, random))
                && world.getEntitiesByClass(DuergarEntity.class, new Box(pos).expand(CROWD_RANGE), e -> true).size() < MAX_NEARBY;
    }
}
