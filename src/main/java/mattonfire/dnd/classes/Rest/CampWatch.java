package mattonfire.dnd.classes.Rest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.mojang.datafixers.util.Pair;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Abilities.AbilityScores;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Downed.Downed;
import mattonfire.dnd.classes.SkillChecks.D20;
import mattonfire.dnd.classes.SkillChecks.SkillCheck;
import mattonfire.dnd.entity.ModEntityTypes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.gen.structure.Structure;

/**
 * A camp's watch roll and its ambush. The party's best watcher (highest Perception check bonus on the
 * character sheet) rolls d20 + Perception against {@value #WATCH_DC}, at disadvantage when camping alone.
 * A failure brings 2-4 hostiles 15-45 s into the camp; a natural 1 brings 4-6 with an elite leader.
 *
 * <p>Other systems can reuse {@link #spawnAmbush} for wandering-monster checks.
 */
public final class CampWatch {
    public static final int WATCH_DC = 12;
    /** The ambush comes this many camp ticks in, at the earliest (15 s)... */
    public static final int AMBUSH_EARLIEST = 15 * 20;
    /** ...and at the latest (45 s). */
    public static final int AMBUSH_LATEST = 45 * 20;
    /** Ambushers appear this far (horizontally) from the fire. */
    public static final int SPAWN_MIN = 16;
    public static final int SPAWN_MAX = 24;
    /** Ambushers further than this from the fire no longer count; campers may roam this far in an ambush. */
    public static final double AMBUSH_RADIUS = 48;
    /** Within this many blocks of a goblin camp, the ambushers are goblins. */
    public static final int GOBLIN_RANGE = 400;
    /** Command tag on every ambusher. */
    public static final String AMBUSHER_TAG = "dndclasses.camp_ambusher";
    /** Roll tags for advantage filters on the sheet ({@code RollQuery.check(PERCEPTION, "watch")}). */
    public static final String TAG_WATCH = "watch";
    /** Added when the fire can't see the sky, for a Dwarf's advantage on watches underground. */
    public static final String TAG_UNDERGROUND = "underground";

    private static final RegistryKey<Structure> GOBLIN_CAMP = RegistryKey.of(RegistryKeys.STRUCTURE,
            new Identifier(DnDClasses.MOD_ID, "goblin_camp"));
    /** Biome monsters that make sense as ambushers; anything else in the biome's pool is skipped. */
    private static final Set<EntityType<?>> AMBUSHERS = Set.of(EntityType.ZOMBIE, EntityType.HUSK,
            EntityType.ZOMBIE_VILLAGER, EntityType.SKELETON, EntityType.STRAY, EntityType.SPIDER,
            EntityType.CAVE_SPIDER, EntityType.WITHER_SKELETON, EntityType.HOGLIN, EntityType.BLAZE,
            EntityType.PIGLIN_BRUTE);
    private static final List<EntityType<?>> DEFAULT = List.of(EntityType.ZOMBIE, EntityType.SKELETON,
            EntityType.SPIDER);
    private static final List<EntityType<?>> NETHER_DEFAULT = List.of(EntityType.WITHER_SKELETON,
            EntityType.HOGLIN);

    private CampWatch() {
    }

    /** The camper with the best Perception check bonus. */
    static ServerPlayerEntity bestWatcher(List<ServerPlayerEntity> campers) {
        return campers.stream()
                .max(Comparator.comparingInt(p -> AbilityScores.sheet(p).check(Skill.PERCEPTION)))
                .orElseThrow();
    }

    /** Rolls the watch for the camp and schedules any ambush. Shows the roll to every camper. */
    static void roll(PartyCamp camp, List<ServerPlayerEntity> campers) {
        if (campers.isEmpty()) {
            return;
        }
        ServerPlayerEntity watcher = bestWatcher(campers);
        boolean solo = campers.size() == 1;
        ServerWorld world = (ServerWorld) watcher.getWorld();
        boolean underground = !world.isSkyVisible(camp.campfire().up());
        String[] tags = underground ? new String[] { TAG_WATCH, TAG_UNDERGROUND } : new String[] { TAG_WATCH };
        D20.Roll roll = SkillCheck.builder(watcher, Skill.PERCEPTION, WATCH_DC, tags)
                .label(Text.translatable("skill.dndclasses.watch", watcher.getEntityName()))
                .disadvantageIf(solo)
                .roll();
        boolean nat1 = roll.outcome() == D20.Outcome.FUMBLE;
        boolean failed = !roll.outcome().succeeded() || PartyCamp.forceAmbush();
        Text detail;
        if (failed) {
            int at = AMBUSH_EARLIEST + world.random.nextInt(AMBUSH_LATEST - AMBUSH_EARLIEST + 1);
            camp.scheduleAmbush(at, nat1);
            detail = Text.translatable(nat1 ? "camp.dndclasses.watch.fumble" : "camp.dndclasses.watch.fail")
                    .formatted(Formatting.RED);
            DnDClasses.LOGGER.info("[Camp] watch by {} ({}{}) failed: {} + {} = {} vs DC {}, ambush{} at tick {}",
                    watcher.getEntityName(), solo ? "solo, disadvantage" : campers.size() + " campers",
                    underground ? ", underground" : "", roll.natural(), roll.modifier(), roll.total(), WATCH_DC,
                    nat1 ? " (natural 1, elite)" : "", at);
        } else {
            detail = Text.translatable("camp.dndclasses.watch.success").formatted(Formatting.GREEN);
            DnDClasses.LOGGER.info("[Camp] watch by {} ({}{}) passed: {} + {} = {} vs DC {}",
                    watcher.getEntityName(), solo ? "solo, disadvantage" : campers.size() + " campers",
                    underground ? ", underground" : "", roll.natural(), roll.modifier(), roll.total(), WATCH_DC);
        }
        for (ServerPlayerEntity camper : campers) {
            D20.show(camper, roll, detail);
        }
    }

    /**
     * Spawns an ambush on valid ground {@value #SPAWN_MIN}-{@value #SPAWN_MAX} blocks from {@code centre}, aimed
     * at the nearest of {@code targets}: 2-4 hostiles, or 4-6 with an elite (Strength I and a name) when
     * {@code big}. The mobs suit the biome and dimension, or are goblins near a goblin camp.
     *
     * @return the mobs spawned (empty if there was no room)
     */
    public static List<LivingEntity> spawnAmbush(ServerWorld world, BlockPos centre, List<ServerPlayerEntity> targets,
            boolean big) {
        int count = big ? 4 + world.random.nextInt(3) : 2 + world.random.nextInt(3);
        boolean goblins = nearGoblinCamp(world, centre);
        double baseAngle = world.random.nextDouble() * Math.PI * 2.0;
        List<LivingEntity> spawned = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            EntityType<?> type = goblins ? ModEntityTypes.GOBLIN_WARRIOR : pick(world, centre);
            if (!(type.create(world) instanceof MobEntity mob)) {
                continue;
            }
            BlockPos spot = findSpot(world, centre, baseAngle, mob);
            if (spot == null) {
                mob.discard();
                continue;
            }
            mob.refreshPositionAndAngles(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5,
                    world.random.nextFloat() * 360.0F, 0.0F);
            mob.initialize(world, world.getLocalDifficulty(spot), SpawnReason.EVENT, null, null);
            mob.addCommandTag(AMBUSHER_TAG);
            if (big && spawned.isEmpty()) {
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, StatusEffectInstance.INFINITE, 0));
                mob.setCustomName(Text.translatable("camp.dndclasses.elite", mob.getType().getName())
                        .formatted(Formatting.RED));
                mob.setCustomNameVisible(true);
                mob.setPersistent();
            }
            ServerPlayerEntity target = nearest(mob, targets);
            if (target != null) {
                mob.setTarget(target);
            }
            world.spawnEntityAndPassengers(mob);
            world.spawnParticles(ParticleTypes.LARGE_SMOKE, mob.getX(), mob.getBodyY(0.5), mob.getZ(), 8, 0.4, 0.6,
                    0.4, 0.02);
            spawned.add(mob);
        }
        DnDClasses.LOGGER.info("[Camp] ambush at {}: {}", centre.toShortString(), spawned.stream()
                .map(m -> m.getType().getUntranslatedName() + " " + m.getBlockPos().toShortString() + " "
                        + String.format("%.1f", Math.sqrt(m.getBlockPos().getSquaredDistance(centre.getX(), m.getY(),
                                centre.getZ())))
                        + "m" + (m.hasCustomName() ? " (elite)" : ""))
                .toList());
        return spawned;
    }

    private static ServerPlayerEntity nearest(MobEntity mob, List<ServerPlayerEntity> targets) {
        return targets.stream().filter(p -> p.isAlive() && !Downed.is(p))
                .min(Comparator.comparingDouble(mob::squaredDistanceTo)).orElse(null);
    }

    /** A weighted biome monster from {@link #AMBUSHERS}, or a dimension default. */
    private static EntityType<?> pick(ServerWorld world, BlockPos pos) {
        List<SpawnSettings.SpawnEntry> entries = new ArrayList<>();
        for (SpawnSettings.SpawnEntry entry : world.getBiome(pos).value().getSpawnSettings()
                .getSpawnEntries(SpawnGroup.MONSTER).getEntries()) {
            if (AMBUSHERS.contains(entry.type)) {
                entries.add(entry);
            }
        }
        Optional<SpawnSettings.SpawnEntry> chosen = Pool.of(entries).getOrEmpty(world.random);
        if (chosen.isPresent()) {
            return chosen.get().type;
        }
        List<EntityType<?>> fallback = world.getRegistryKey() == World.NETHER ? NETHER_DEFAULT : DEFAULT;
        return fallback.get(world.random.nextInt(fallback.size()));
    }

    /** Whether a goblin camp starts within {@value #GOBLIN_RANGE} blocks (overworld only). */
    static boolean nearGoblinCamp(ServerWorld world, BlockPos pos) {
        if (world.getRegistryKey() != World.OVERWORLD) {
            return false;
        }
        Optional<RegistryEntry.Reference<Structure>> camp = world.getRegistryManager().get(RegistryKeys.STRUCTURE)
                .getEntry(GOBLIN_CAMP);
        if (camp.isEmpty()) {
            return false;
        }
        // Goblin camps are spaced 28 chunks apart, so a radius of 1 placement cell covers 400 blocks.
        Pair<BlockPos, RegistryEntry<Structure>> found = world.getChunkManager().getChunkGenerator()
                .locateStructure(world, RegistryEntryList.of(camp.get()), pos, 1, false);
        if (found == null) {
            return false;
        }
        BlockPos at = found.getFirst();
        double dx = at.getX() - pos.getX();
        double dz = at.getZ() - pos.getZ();
        return dx * dx + dz * dz <= (double) GOBLIN_RANGE * GOBLIN_RANGE;
    }

    /**
     * Standing room for the mob {@value #SPAWN_MIN}-{@value #SPAWN_MAX} blocks (horizontally) from the centre,
     * near {@code baseAngle}, within 8 blocks up or down of it (so a camp in a cave is ambushed in the cave),
     * on a solid floor and out of fluids. Null if none was found.
     */
    private static BlockPos findSpot(ServerWorld world, BlockPos centre, double baseAngle, MobEntity mob) {
        for (int attempt = 0; attempt < 32; attempt++) {
            double spread = attempt < 16 ? 0.6 : Math.PI;
            double angle = baseAngle + (world.random.nextDouble() * 2.0 - 1.0) * spread;
            double distance = SPAWN_MIN + world.random.nextDouble() * (SPAWN_MAX - SPAWN_MIN);
            int x = MathHelper.floor(centre.getX() + 0.5 + Math.cos(angle) * distance);
            int z = MathHelper.floor(centre.getZ() + 0.5 + Math.sin(angle) * distance);
            if (!world.isChunkLoaded(new BlockPos(x, centre.getY(), z))) {
                continue;
            }
            for (int dy = 8; dy >= -8; dy--) {
                BlockPos pos = new BlockPos(x, centre.getY() + dy, z);
                BlockPos floor = pos.down();
                if (!world.getBlockState(floor).isSideSolidFullSquare(world, floor, Direction.UP)
                        || !world.getFluidState(pos).isEmpty() || !world.getFluidState(pos.up()).isEmpty()) {
                    continue;
                }
                mob.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
                if (world.isSpaceEmpty(mob)) {
                    return pos;
                }
            }
        }
        return null;
    }
}
