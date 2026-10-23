package mattonfire.dnd.world.gen;

import mattonfire.dnd.entity.BeholderEntity;
import mattonfire.dnd.entity.ElfEntity;
import mattonfire.dnd.entity.HobbitEntity;
import mattonfire.dnd.world.gen.enclave.ElvenEnclaveStructures;
import mattonfire.dnd.world.gen.enclave.EnclavePiece;
import mattonfire.dnd.entity.FrostDrakeEntity;
import mattonfire.dnd.entity.LairDragonEntity;
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
import mattonfire.dnd.world.gen.village.HobbitPiece;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.Difficulty;
import net.minecraft.server.world.ServerWorld;
import mattonfire.dnd.world.gen.lair.LairRespawns;

public class ModSpawns {
    private static final int MAX_HOBBITS_NEARBY = 16;
    private static final RegistryKey<Structure> HOBBIT_VILLAGE = RegistryKey.of(RegistryKeys.STRUCTURE,
            new Identifier(mattonfire.dnd.classes.DnDClasses.MOD_ID, "hobbit_village"));
    private static final int MAX_DWARVES_NEARBY = 24;
    private static final double MAX_ALPHA_DISTANCE = 96.0D;

    public static void addSpawns() {
        // Wyverns, rarely and alone, in the mountains (not the plains, where they piled up round
        // hobbit villages). Wild ones despawn far from players (WyvernEntity.canImmediatelyDespawn).
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.MEADOW, BiomeKeys.STONY_PEAKS, BiomeKeys.JAGGED_PEAKS),
                SpawnGroup.CREATURE, ModEntityTypes.WYVERN, 2, 1, 1);
        
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

        // Owlbears prowl the dark and old-growth forests (monsters: in the dark, so under the dark
        // forest canopy by day and anywhere in those woods at night).
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.DARK_FOREST, BiomeKeys.OLD_GROWTH_BIRCH_FOREST,
                        BiomeKeys.OLD_GROWTH_PINE_TAIGA, BiomeKeys.OLD_GROWTH_SPRUCE_TAIGA),
                SpawnGroup.MONSTER, ModEntityTypes.OWLBEAR, 12, 1, 1);


        // Gelatinous Cubes ooze through dark caves and dungeons anywhere in the Overworld, well below sea level.
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(),
                SpawnGroup.MONSTER, ModEntityTypes.GELATINOUS_CUBE, 5, 1, 1);
        SpawnRestriction.register(ModEntityTypes.GELATINOUS_CUBE, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, mattonfire.dnd.entity.GelatinousCubeEntity::canSpawn);

        // Rust Monsters burrow through deep caves (below y=0) in the Overworld, alone or in pairs.
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(),
                SpawnGroup.MONSTER, ModEntityTypes.RUST_MONSTER, 4, 1, 2);
        SpawnRestriction.register(ModEntityTypes.RUST_MONSTER, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, mattonfire.dnd.entity.RustMonsterEntity::canSpawn);

        // Goat rules (grass, stone, snow, packed ice or gravel in daylight): the animal rule only allows
        // grass, so wyverns never turned up on the bare stony and jagged peaks they're listed for.
        SpawnRestriction.register(ModEntityTypes.WYVERN, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (type, world, reason, pos, random) ->
                        // Wild wyverns leave in peaceful, so don't spawn them there
                        world.getDifficulty() != Difficulty.PEACEFUL && GoatEntity.canSpawn(type, world, reason, pos, random));
        // Lightning Chasers only live in their lairs on the mountain peaks (the structure's
        // spawn_overrides), which get a new one now and then once the old one's dead.
        SpawnRestriction.register(ModEntityTypes.LIGHTNING_CHASER, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (type, world, reason, pos, random) ->
                        canLairDragonSpawn(type, world, reason, pos, random, LightningChaserEntity.LAIR_STRUCTURE));
        // Frost Drakes likewise, only in their frost lairs on Frozen Peaks.
        SpawnRestriction.register(ModEntityTypes.FROST_DRAKE, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (type, world, reason, pos, random) ->
                        canLairDragonSpawn(type, world, reason, pos, random, FrostDrakeEntity.LAIR_STRUCTURE));
        SpawnRestriction.register(ModEntityTypes.EMBER_WYVERN, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, mattonfire.dnd.entity.EmberWyvernEntity::canSpawn);
        SpawnRestriction.register(ModEntityTypes.RIVER_PIKEHORN, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, AnimalEntity::isValidNaturalSpawn);
        SpawnRestriction.register(ModEntityTypes.MAGMAMUNCHER, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, MagmamuncherEntity::canSpawnInNether);

        SpawnRestriction.register(ModEntityTypes.MAGMAMUNCHER_ALPHA, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ModSpawns::canMagmamuncherAlphaSpawn);

        SpawnRestriction.register(ModEntityTypes.OWLBEAR, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);

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

        // Elves only spawn inside elven enclaves (the structure's spawn_overrides), in the trees, the
        // glade, the gardens and by the Moonwell, at most MAX_ELVES_NEARBY of them.
        SpawnRestriction.register(ModEntityTypes.WOOD_ELF, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ModSpawns::canElfSpawn);
        SpawnRestriction.register(ModEntityTypes.ELF_WARDEN, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ModSpawns::canElfSpawn);

        // Templars only spawn inside dragon temples (the structure's spawn_overrides), on the terrace,
        // at most MAX_TEMPLARS_NEARBY of them.
        SpawnRestriction.register(ModEntityTypes.DRAGONBORN_TEMPLAR, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ModSpawns::canTemplarSpawn);

        // Orc Warriors only spawn inside orc strongholds (the structure's spawn_overrides), inside the
        // palisade, at most MAX_ORCS_NEARBY orcs within 48 blocks.
        SpawnRestriction.register(ModEntityTypes.ORC_WARRIOR, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ModSpawns::canOrcSpawn);

        // Tiefling Exiles only spawn inside brimstone sanctums (the structure's spawn_overrides), on the
        // platform inside the wall, at most MAX_EXILES_NEARBY of them within 48 blocks.
        SpawnRestriction.register(ModEntityTypes.TIEFLING_EXILE, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ModSpawns::canExileSpawn);

        // Gnome Tinkers and Clockwork Sentinels only spawn inside Cogsdeep (the structure's spawn_overrides), on
        // the burrow's floor in its rooms (not the stair or the observatory), at most MAX_GNOMES_NEARBY of them
        // within 48 blocks.
        SpawnRestriction.register(ModEntityTypes.GNOME_TINKER, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ModSpawns::canCogsdeepSpawn);
        SpawnRestriction.register(ModEntityTypes.CLOCKWORK_SENTINEL, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ModSpawns::canCogsdeepSpawn);

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

    private static boolean canLairDragonSpawn(EntityType<? extends LairDragonEntity> type, ServerWorldAccess world,
                                              SpawnReason reason, BlockPos pos, Random random, RegistryKey<Structure> lairStructure) {
        if (!MobEntity.canMobSpawn(type, world, reason, pos, random)) {
            return false;
        }
        if (reason != SpawnReason.NATURAL) {
            return true;
        }
        // Natural spawns only come from a lair's spawn override: not in peaceful, not while the lair is
        // still waiting after its last dragon was killed, and never near another of its kind.
        if (world.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        ServerWorld serverWorld = world.toServerWorld();
        BlockPos lair = LairDragonEntity.findLair(serverWorld, pos, lairStructure);
        return lair != null && LairRespawns.get(serverWorld).canRespawn(lair, serverWorld.getTime())
                && world.getEntitiesByClass(LairDragonEntity.class, new Box(pos).expand(64.0D), e -> e.getType() == type).isEmpty();
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

    public static final int MAX_ELVES_NEARBY = 12;
    public static final int MAX_TEMPLARS_NEARBY = 7;

    private static boolean canTemplarSpawn(EntityType<mattonfire.dnd.entity.DragonbornTemplarEntity> type, ServerWorldAccess world,
                                           SpawnReason reason, BlockPos pos, Random random) {
        if (!MobEntity.canMobSpawn(type, world, reason, pos, random)) {
            return false;
        }
        // Natural spawns and the chunk's first animals both come from the structure's spawn override.
        return reason != SpawnReason.NATURAL && reason != SpawnReason.CHUNK_GENERATION
                || inTemplePiece(world, pos) && world.getEntitiesByClass(mattonfire.dnd.entity.DragonbornTemplarEntity.class,
                new Box(pos).expand(48.0D), e -> true).size() < MAX_TEMPLARS_NEARBY;
    }

    public static final int MAX_ORCS_NEARBY = 12;

    private static boolean canOrcSpawn(EntityType<mattonfire.dnd.entity.OrcWarriorEntity> type, ServerWorldAccess world,
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
        StructureStart start = world.toServerWorld().getStructureAccessor().getStructureContaining(pos,
                mattonfire.dnd.world.gen.stronghold.OrcStrongholdStructures.KEY);
        if (!start.hasChildren()) {
            return false;
        }
        BlockPos center = start.getBoundingBox().getCenter();
        double dx = pos.getX() - center.getX();
        double dz = pos.getZ() - center.getZ();
        double wall = mattonfire.dnd.world.gen.stronghold.OrcStrongholdStructure.PALISADE - 1;
        return dx * dx + dz * dz <= wall * wall
                && world.getEntitiesByClass(mattonfire.dnd.entity.OrcEntity.class, new Box(pos).expand(48.0D), e -> true).size() < MAX_ORCS_NEARBY;
    }

    public static final int MAX_GNOMES_NEARBY = 10;

    private static <T extends MobEntity> boolean canCogsdeepSpawn(EntityType<T> type, ServerWorldAccess world,
                                                                 SpawnReason reason, BlockPos pos, Random random) {
        if (!MobEntity.canMobSpawn(type, world, reason, pos, random)) {
            return false;
        }
        if (reason != SpawnReason.NATURAL && reason != SpawnReason.CHUNK_GENERATION) {
            return true;
        }
        StructureStart start = world.toServerWorld().getStructureAccessor().getStructureContaining(pos,
                mattonfire.dnd.world.gen.burrow.CogsdeepStructures.KEY);
        if (!start.hasChildren()) {
            return false;
        }
        boolean inRoom = false;
        for (net.minecraft.structure.StructurePiece piece : start.getChildren()) {
            if (piece instanceof mattonfire.dnd.world.gen.burrow.CogsdeepPiece room
                    && !(piece instanceof mattonfire.dnd.world.gen.burrow.ObservatoryPiece)
                    && !(piece instanceof mattonfire.dnd.world.gen.burrow.ShaftPiece)
                    && piece.getBoundingBox().contains(pos) && pos.getY() == room.center().getY()) {
                inRoom = true;
                break;
            }
        }
        return inRoom && world.getEntitiesByClass(MobEntity.class, new Box(pos).expand(48.0D),
                e -> e instanceof mattonfire.dnd.entity.GnomeTinkerEntity || e instanceof mattonfire.dnd.entity.ClockworkSentinelEntity)
                .size() < MAX_GNOMES_NEARBY;
    }

    public static final int MAX_EXILES_NEARBY = 8;

    private static boolean canExileSpawn(EntityType<mattonfire.dnd.entity.TieflingExileEntity> type, ServerWorldAccess world,
                                         SpawnReason reason, BlockPos pos, Random random) {
        if (!MobEntity.canMobSpawn(type, world, reason, pos, random)) {
            return false;
        }
        if (reason != SpawnReason.NATURAL && reason != SpawnReason.CHUNK_GENERATION) {
            return true;
        }
        StructureStart start = world.toServerWorld().getStructureAccessor().getStructureContaining(pos,
                mattonfire.dnd.world.gen.sanctum.BrimstoneSanctumStructures.KEY);
        if (!start.hasChildren()) {
            return false;
        }
        // On the platform (its top is the start's y) and inside the wall.
        BlockPos center = start.getChildren().get(0) instanceof mattonfire.dnd.world.gen.sanctum.BrimstonePiece piece
                ? piece.center() : start.getBoundingBox().getCenter();
        double dx = pos.getX() - center.getX();
        double dz = pos.getZ() - center.getZ();
        double wall = mattonfire.dnd.world.gen.sanctum.BrimstoneSanctumStructure.WALL - 1;
        return pos.getY() == center.getY() + 1 && dx * dx + dz * dz <= wall * wall
                && world.getEntitiesByClass(mattonfire.dnd.entity.TieflingEntity.class, new Box(pos).expand(48.0D), e -> true).size() < MAX_EXILES_NEARBY;
    }

    /** Whether {@code pos} is inside one of a dragon temple's pieces. */
    private static boolean inTemplePiece(ServerWorldAccess world, BlockPos pos) {
        StructureStart start = world.toServerWorld().getStructureAccessor().getStructureContaining(pos,
                mattonfire.dnd.world.gen.temple.DragonTempleStructures.KEY);
        if (!start.hasChildren()) {
            return false;
        }
        for (StructurePiece piece : start.getChildren()) {
            // The slope round the terrace is only ground.
            if (!(piece instanceof mattonfire.dnd.world.gen.temple.SlopePiece) && piece.getBoundingBox().contains(pos)) {
                return true;
            }
        }
        return false;
    }

    private static <T extends ElfEntity> boolean canElfSpawn(EntityType<T> type, ServerWorldAccess world, SpawnReason reason,
                                                            BlockPos pos, Random random) {
        if (!MobEntity.canMobSpawn(type, world, reason, pos, random)) {
            return false;
        }
        // Natural spawns and the chunk's first animals both come from the structure's spawn override.
        return reason != SpawnReason.NATURAL && reason != SpawnReason.CHUNK_GENERATION
                || inEnclavePiece(world, pos)
                && world.getEntitiesByClass(ElfEntity.class, new Box(pos).expand(48.0D), e -> true).size() < MAX_ELVES_NEARBY;
    }

    /** Whether {@code pos} is inside one of an elven enclave's real pieces (not the deep grounds piece). */
    private static boolean inEnclavePiece(ServerWorldAccess world, BlockPos pos) {
        StructureStart start = world.toServerWorld().getStructureAccessor().getStructureContaining(pos, ElvenEnclaveStructures.KEY);
        if (!start.hasChildren()) {
            return false;
        }
        for (StructurePiece piece : start.getChildren()) {
            if (piece instanceof EnclavePiece && piece.getBoundingBox().contains(pos)) {
                return true;
            }
        }
        return false;
    }

    private static boolean canHobbitSpawn(EntityType<HobbitEntity> type, ServerWorldAccess world, SpawnReason reason,
                                          BlockPos pos, Random random) {
        if (!MobEntity.canMobSpawn(type, world, reason, pos, random)) {
            return false;
        }
        return reason != SpawnReason.NATURAL
                || inHobbitBuilding(world, pos)
                && world.getEntitiesByClass(HobbitEntity.class, new Box(pos).expand(48.0D), e -> true).size() < MAX_HOBBITS_NEARBY;
    }

    /**
     * Whether {@code pos} is inside one of a hobbit village's real pieces (smials, gardens, the
     * green and so on). The lanes and grounds pieces have boxes deep under the village, so a cave
     * down there also counts as "in the village" for the structure's spawn override.
     */
    private static boolean inHobbitBuilding(ServerWorldAccess world, BlockPos pos) {
        StructureStart start = world.toServerWorld().getStructureAccessor().getStructureContaining(pos, HOBBIT_VILLAGE);
        if (!start.hasChildren()) {
            return false;
        }
        for (StructurePiece piece : start.getChildren()) {
            if (piece instanceof HobbitPiece && piece.getBoundingBox().contains(pos)) {
                return true;
            }
        }
        return false;
    }
}
