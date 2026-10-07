package mattonfire.dnd.world.gen;

import mattonfire.dnd.entity.BeholderEntity;
import mattonfire.dnd.entity.HobbitEntity;
import mattonfire.dnd.entity.LightningChaserEntity;
import mattonfire.dnd.entity.MagmamuncherAlphaEntity;
import mattonfire.dnd.entity.ModEntityTypes;
import mattonfire.dnd.entity.MountainDwarfEntity;
import mattonfire.dnd.entity.MagmamuncherEntity;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.GoatEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.biome.BiomeKeys;

public class ModSpawns {
    private static final int MAX_HOBBITS_NEARBY = 16;
    private static final int MAX_DWARVES_NEARBY = 24;
    private static final double MAX_ALPHA_DISTANCE = 96.0D;

    public static void addSpawns() {
        // Wyverns in plains/mountains
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.PLAINS, BiomeKeys.MEADOW, BiomeKeys.STONY_PEAKS, BiomeKeys.JAGGED_PEAKS),
                SpawnGroup.CREATURE, ModEntityTypes.WYVERN, 10, 1, 3);
        
        // Ember Wyverns (fire-immune glass cannons) across the open Nether biomes
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.NETHER_WASTES, BiomeKeys.CRIMSON_FOREST, BiomeKeys.BASALT_DELTAS, BiomeKeys.SOUL_SAND_VALLEY),
                SpawnGroup.MONSTER, ModEntityTypes.EMBER_WYVERN, 4, 1, 2);

        // River Pikehorns in rivers and swamps
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.RIVER, BiomeKeys.SWAMP, BiomeKeys.MANGROVE_SWAMP),
                SpawnGroup.CREATURE, ModEntityTypes.RIVER_PIKEHORN, 15, 2, 4);

        // Magmamunchers in Nether
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.BASALT_DELTAS, BiomeKeys.NETHER_WASTES),
                SpawnGroup.MONSTER, ModEntityTypes.MAGMAMUNCHER, 8, 1, 3);
        // Rarely, a Magmamuncher Alpha (a boss) instead; at most one in a wide area.
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.BASALT_DELTAS, BiomeKeys.NETHER_WASTES),
                SpawnGroup.MONSTER, ModEntityTypes.MAGMAMUNCHER_ALPHA, 1, 1, 1);


        // Goat rules (grass, stone, snow, packed ice or gravel in daylight): the animal rule only allows
        // grass, so wyverns never turned up on the bare stony and jagged peaks they're listed for.
        SpawnRestriction.register(ModEntityTypes.WYVERN, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, GoatEntity::canSpawn);
        // Lightning Chasers only live in their lairs on the mountain peaks (the structure's
        // spawn_overrides), which get a new one now and then once the old one's dead.
        SpawnRestriction.register(ModEntityTypes.LIGHTNING_CHASER, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ModSpawns::canLightningChaserSpawn);
        SpawnRestriction.register(ModEntityTypes.EMBER_WYVERN, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, mattonfire.dnd.entity.EmberWyvernEntity::canSpawn);
        SpawnRestriction.register(ModEntityTypes.RIVER_PIKEHORN, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, AnimalEntity::isValidNaturalSpawn);
        SpawnRestriction.register(ModEntityTypes.MAGMAMUNCHER, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, MagmamuncherEntity::canSpawnInNether);

        SpawnRestriction.register(ModEntityTypes.MAGMAMUNCHER_ALPHA, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ModSpawns::canMagmamuncherAlphaSpawn);

        // Goblin Warriors join the Nether Fortress spawn pool (see SpawnHelperMixin)
        SpawnRestriction.register(ModEntityTypes.GOBLIN_WARRIOR, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);

        // Hobbits only spawn inside hobbit villages (the structure's spawn_overrides), topping them up
        // over time without overcrowding.
        SpawnRestriction.register(ModEntityTypes.HOBBIT, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ModSpawns::canHobbitSpawn);

        // Mountain dwarves likewise only spawn inside dwarven fortresses, in the dark halls too.
        SpawnRestriction.register(ModEntityTypes.MOUNTAIN_DWARF, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ModSpawns::canDwarfSpawn);

        // Beholders only live in their underground lairs (the structure's spawn_overrides), which
        // get a new one now and then once the old one's dead.
        SpawnRestriction.register(ModEntityTypes.BEHOLDER, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ModSpawns::canBeholderSpawn);
    }

    private static boolean canBeholderSpawn(EntityType<BeholderEntity> type, ServerWorldAccess world,
                                            SpawnReason reason, BlockPos pos, Random random) {
        if (!HostileEntity.canSpawnIgnoreLightLevel(type, world, reason, pos, random)) {
            return false;
        }
        // Natural spawns (only ever inside a lair): rarely, underground, and never near another one.
        return reason != SpawnReason.NATURAL
                || (random.nextInt(20) == 0 && !world.isSkyVisible(pos)
                && world.getEntitiesByClass(BeholderEntity.class, new Box(pos).expand(64.0D), e -> true).isEmpty());
    }

    private static boolean canLightningChaserSpawn(EntityType<LightningChaserEntity> type, ServerWorldAccess world,
                                                   SpawnReason reason, BlockPos pos, Random random) {
        if (!MobEntity.canMobSpawn(type, world, reason, pos, random)) {
            return false;
        }
        return reason != SpawnReason.NATURAL
                || world.getEntitiesByClass(LightningChaserEntity.class, new Box(pos).expand(64.0D), e -> true).isEmpty();
    }

    private static boolean canMagmamuncherAlphaSpawn(EntityType<MagmamuncherAlphaEntity> type, ServerWorldAccess world,
                                                     SpawnReason reason, BlockPos pos, Random random) {
        if (!HostileEntity.canSpawnIgnoreLightLevel(type, world, reason, pos, random)) {
            return false;
        }
        // Natural spawns: a 1 in 4 roll on top of the low weight, and never near another alpha.
        return reason != SpawnReason.NATURAL
                || (random.nextInt(4) == 0
                && world.getEntitiesByClass(MagmamuncherAlphaEntity.class, new Box(pos).expand(MAX_ALPHA_DISTANCE), e -> true).isEmpty());
    }

    private static boolean canDwarfSpawn(EntityType<MountainDwarfEntity> type, ServerWorldAccess world, SpawnReason reason,
                                         BlockPos pos, Random random) {
        if (!MobEntity.canMobSpawn(type, world, reason, pos, random)) {
            return false;
        }
        return reason != SpawnReason.NATURAL
                || world.getEntitiesByClass(MountainDwarfEntity.class, new Box(pos).expand(64.0D), e -> true).size() < MAX_DWARVES_NEARBY;
    }

    private static boolean canHobbitSpawn(EntityType<HobbitEntity> type, ServerWorldAccess world, SpawnReason reason,
                                          BlockPos pos, Random random) {
        if (!MobEntity.canMobSpawn(type, world, reason, pos, random)) {
            return false;
        }
        return reason != SpawnReason.NATURAL
                || world.getEntitiesByClass(HobbitEntity.class, new Box(pos).expand(48.0D), e -> true).size() < MAX_HOBBITS_NEARBY;
    }
}
