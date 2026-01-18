package mattonfire.dnd.world.gen;

import mattonfire.dnd.entity.ModEntityTypes;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.BiomeKeys;

public class ModSpawns {
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
    }
}
