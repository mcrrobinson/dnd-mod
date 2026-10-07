package mattonfire.dnd.world.gen;

import mattonfire.dnd.entity.HobbitEntity;
import mattonfire.dnd.entity.ModEntityTypes;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.biome.BiomeKeys;

public class ModSpawns {
    private static final int MAX_HOBBITS_NEARBY = 16;

    public static void addSpawns() {
        // Wyverns in plains/mountains
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.PLAINS, BiomeKeys.MEADOW, BiomeKeys.STONY_PEAKS, BiomeKeys.JAGGED_PEAKS),
                SpawnGroup.CREATURE, ModEntityTypes.WYVERN, 10, 1, 3);
        
        // Lightning Chasers in high/steep places
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.WINDSWEPT_HILLS, BiomeKeys.WINDSWEPT_GRAVELLY_HILLS, BiomeKeys.SAVANNA_PLATEAU),
                SpawnGroup.CREATURE, ModEntityTypes.LIGHTNING_CHASER, 5, 1, 2);

        // River Pikehorns in rivers and swamps
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.RIVER, BiomeKeys.SWAMP, BiomeKeys.MANGROVE_SWAMP),
                SpawnGroup.CREATURE, ModEntityTypes.RIVER_PIKEHORN, 15, 2, 4);

        // Magmamunchers in Nether
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.BASALT_DELTAS, BiomeKeys.NETHER_WASTES),
                SpawnGroup.MONSTER, ModEntityTypes.MAGMAMUNCHER, 8, 1, 3);


        SpawnRestriction.register(ModEntityTypes.WYVERN, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, AnimalEntity::isValidNaturalSpawn);
        SpawnRestriction.register(ModEntityTypes.LIGHTNING_CHASER, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, AnimalEntity::isValidNaturalSpawn);
        SpawnRestriction.register(ModEntityTypes.RIVER_PIKEHORN, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, AnimalEntity::isValidNaturalSpawn);
        SpawnRestriction.register(ModEntityTypes.MAGMAMUNCHER, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, AnimalEntity::isValidNaturalSpawn); // Or Monster::isValidSpawn? Magmamuncher is Tameable but spawns in Nether.

        // Hobbits only spawn inside hobbit villages (the structure's spawn_overrides), topping them up
        // over time without overcrowding.
        SpawnRestriction.register(ModEntityTypes.HOBBIT, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ModSpawns::canHobbitSpawn);
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
