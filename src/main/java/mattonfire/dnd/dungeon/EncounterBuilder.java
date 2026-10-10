package mattonfire.dnd.dungeon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Party.PartyManager;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.dm.DungeonMaster;
import mattonfire.dnd.dm.encounter.Encounter;
import mattonfire.dnd.dm.encounter.EncounterSpawner;
import mattonfire.dnd.world.gen.dungeon.DungeonTheme;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;

/**
 * Builds a room's encounter when the party walks in: a threat budget from the room, the dungeon's
 * tier, the party's size and its class levels, filled with weighted picks from the theme's
 * {@link DungeonEncounterPools pool}; some picks become Veterans. Mobs are spawned through the DM
 * toolkit's {@link EncounterSpawner} at the room's spawn points, persistent and tagged
 * {@link #TAG} and {@link #roomTag}, which is how the room's ward and the death listener
 * ({@link DungeonCombat}) follow them.
 *
 * The numbers are on {@code docs/systems/dungeon-encounters.md}.
 */
public final class EncounterBuilder {
    /** Every dungeon encounter mob (and boss, champion and phylactery). */
    public static final String TAG = "dndclasses.dungeon";
    /** Followed by {@code <startKey>/<roomId>}: which room a mob belongs to. */
    public static final String ROOM_TAG_PREFIX = "dndclasses.dungeon=";
    /** Veterans: hook for their extra loot roll (dungeons ticket 5). */
    public static final String ELITE_TAG = "dndclasses.dungeon_elite";

    public static final double[] TIER_MULT = {1.0, 1.4, 1.9, 2.5};
    public static final double[] ELITE_SHARE = {0.0, 0.10, 0.25, 0.40};
    public static final double ELITE_COST = 1.5;
    public static final double ELITE_HEALTH = 0.5;
    public static final double ELITE_DAMAGE = 0.25;
    public static final int ELITE_GLOW_TICKS = 40;
    public static final int MAX_MOBS = 12;
    public static final int MAX_MOBS_CHAMPION_ROOM = 16;
    public static final int MAX_PARTY = 8;

    private static final UUID ELITE_HEALTH_ID = UUID.fromString("9b0f6a52-2f1d-4d6e-8c3a-71e0a1f4b201");
    private static final UUID ELITE_DAMAGE_ID = UUID.fromString("9b0f6a52-2f1d-4d6e-8c3a-71e0a1f4b202");

    private EncounterBuilder() {
    }

    /** Who a room's fight is scaled for, and how strong they are. */
    public record Party(List<ServerPlayerEntity> players, int size, double averageLevel) {
    }

    /** A planned pick: which entry, and whether it's a Veteran. */
    public record Pick(DungeonEncounterPools.Entry entry, boolean elite) {
        public double cost() {
            return this.elite ? this.entry.threat() * ELITE_COST : this.entry.threat();
        }
    }

    /** What a room spawned, for the ward and the log. */
    public record Spawned(List<Entity> mobs, double budget, double spent) {
    }

    // ---------------------------------------------------------------- the numbers

    /** A room's base threat before scaling: small 6, large 10, the mid-boss room's minions 5. */
    public static double roomBase(RoomRole role) {
        return switch (role) {
            case ENCOUNTER_LARGE -> 10.0;
            case CHAMPION -> 5.0;
            default -> 6.0;
        };
    }

    /** 1 player 1.0, 2: 1.6, 3: 2.1, 4: 2.6, then +0.4 each (8: 4.2). */
    public static double partyMult(int n) {
        n = MathHelper.clamp(n, 1, MAX_PARTY);
        return switch (n) {
            case 1 -> 1.0;
            case 2 -> 1.6;
            case 3 -> 2.1;
            default -> 2.6 + 0.4 * (n - 4);
        };
    }

    /** 0.8 + 0.05 per average class level (level 1: 0.85, 10: 1.3). */
    public static double levelMult(double averageLevel) {
        return 0.8 + 0.05 * averageLevel;
    }

    public static double difficultyMult(Difficulty difficulty) {
        return switch (difficulty) {
            case PEACEFUL -> 0.0;
            case EASY -> 0.75;
            case HARD -> 1.25;
            default -> 1.0;
        };
    }

    public static double budget(RoomRole role, int tier, int partySize, double averageLevel, Difficulty difficulty) {
        return roomBase(role) * TIER_MULT[MathHelper.clamp(tier, 1, 4) - 1] * partyMult(partySize)
                * levelMult(averageLevel) * difficultyMult(difficulty);
    }

    /**
     * Fills {@code budget} with weighted picks: each pick must fit what's left (the first always
     * goes in, so a room has at least one mob), up to {@code maxMobs}. Each pick may become a
     * Veteran with chance {@code eliteShare}, if the Veteran's 1.5x cost fits too.
     */
    public static List<Pick> plan(List<DungeonEncounterPools.Entry> pool, double budget, double eliteShare, int maxMobs,
                                  Random random) {
        List<Pick> picks = new ArrayList<>();
        double left = budget;
        while (picks.size() < maxMobs) {
            List<DungeonEncounterPools.Entry> fits = new ArrayList<>();
            int total = 0;
            for (DungeonEncounterPools.Entry entry : pool) {
                if (picks.isEmpty() || entry.threat() <= left + 1.0E-6) {
                    fits.add(entry);
                    total += entry.weight();
                }
            }
            if (fits.isEmpty()) {
                break;
            }
            int roll = random.nextInt(total);
            DungeonEncounterPools.Entry chosen = fits.get(fits.size() - 1);
            for (DungeonEncounterPools.Entry entry : fits) {
                roll -= entry.weight();
                if (roll < 0) {
                    chosen = entry;
                    break;
                }
            }
            boolean elite = eliteShare > 0 && random.nextDouble() < eliteShare
                    && (picks.isEmpty() || chosen.threat() * ELITE_COST <= left + 1.0E-6);
            Pick pick = new Pick(chosen, elite);
            picks.add(pick);
            left -= pick.cost();
        }
        return picks;
    }

    // ---------------------------------------------------------------- the party

    /**
     * The players a fight is for: survival/adventure players inside the dungeon plus the triggering
     * player's party members within {@link PartyManager#SHARE_RADIUS} blocks (Dungeon Masters and
     * creative players don't count), 1-8 of them, and their average class level (a player with no
     * class counts as level 1).
     */
    public static Party partyFor(ServerWorld world, DungeonState dungeon, ServerPlayerEntity trigger) {
        Set<ServerPlayerEntity> players = new LinkedHashSet<>();
        players.add(trigger);
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (counts(player) && dungeon.contains(player.getBlockPos())) {
                players.add(player);
            }
        }
        for (ServerPlayerEntity member : PartyManager.nearbyMembers(trigger, PartyManager.SHARE_RADIUS)) {
            if (counts(member)) {
                players.add(member);
            }
        }
        List<ServerPlayerEntity> list = new ArrayList<>(players);
        double levels = 0;
        for (ServerPlayerEntity player : list) {
            levels += level(player);
        }
        return new Party(list, MathHelper.clamp(list.size(), 1, MAX_PARTY), list.isEmpty() ? 1 : levels / list.size());
    }

    private static boolean counts(ServerPlayerEntity player) {
        return player.isAlive() && !player.isSpectator() && !player.isCreative() && !DungeonMaster.isDm(player);
    }

    private static int level(ServerPlayerEntity player) {
        if (Progression.classOf(player) == DndCharacter.NONE) {
            return 1;
        }
        return Math.max(1, Progression.current(player).level());
    }

    // ---------------------------------------------------------------- spawning

    public static String roomTag(long startKey, int roomId) {
        return ROOM_TAG_PREFIX + startKey + "/" + roomId;
    }

    public static List<String> tags(long startKey, int roomId) {
        return List.of(TAG, roomTag(startKey, roomId));
    }

    /**
     * Spawns a room's encounter: plans it, spreads the mobs over the spawn points (furthest from
     * {@code trigger} first), makes the Veterans, and plays the theme's arrival sound.
     */
    public static Spawned spawn(ServerWorld world, DungeonState dungeon, DungeonState.Room room, List<BlockPos> spawnPoints,
                                Party party, BlockPos trigger, Collection<String> extraTags) {
        DungeonEncounterPools.Pool pool = DungeonEncounterPools.get(dungeon.theme());
        double budget = budget(room.role(), dungeon.tier(), party.size(), party.averageLevel(), world.getDifficulty());
        if (pool == null || budget <= 0) {
            DnDClasses.LOGGER.warn("[Dungeon] no encounter pool for {} (or Peaceful)", dungeon.theme().id());
            return new Spawned(List.of(), budget, 0);
        }
        int max = room.role() == RoomRole.CHAMPION ? MAX_MOBS_CHAMPION_ROOM - 1 : MAX_MOBS;
        List<Pick> picks = plan(pool.forTier(dungeon.tier()), budget, ELITE_SHARE[dungeon.tier() - 1], max, world.getRandom());
        List<BlockPos> points = farthestFirst(spawnPoints, trigger, room);
        List<String> tags = new ArrayList<>(tags(dungeon.startKey(), room.id()));
        tags.addAll(extraTags);
        List<Entity> mobs = new ArrayList<>();
        double spent = 0;
        for (int i = 0; i < picks.size(); i++) {
            Pick pick = picks.get(i);
            Entity mob = spawnOne(world, pick.entry().spawn(), points.get(i % points.size()), tags);
            if (mob == null) {
                continue;
            }
            if (pick.elite() && mob instanceof MobEntity m) {
                makeVeteran(m);
            }
            spent += pick.cost();
            mobs.add(mob);
        }
        arrivalSound(world, dungeon.theme(), room.box().getCenter());
        return new Spawned(mobs, budget, spent);
    }

    /** One mob through {@link EncounterSpawner}, on safe ground next to {@code point}, with a puff of smoke. */
    public static Entity spawnOne(ServerWorld world, Encounter.Spawn spawn, BlockPos point, Collection<String> tags) {
        Encounter.Spawn one = new Encounter.Spawn(spawn.entity(), 1, 1, 1.0F, spawn.nbt(), spawn.initialize(), spawn.tags());
        Identifier id = Registries.ENTITY_TYPE.getId(spawn.entity());
        Encounter encounter = new Encounter(id, id.getPath(), "medium", 1, true, List.of(one), List.of());
        EncounterSpawner.Result result = EncounterSpawner.spawn(world, encounter, point, tags);
        if (result.entities().isEmpty()) {
            return null;
        }
        Entity mob = result.entities().get(0);
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, mob.getX(), mob.getBodyY(0.5D), mob.getZ(), 12, 0.3D, 0.5D, 0.3D, 0.02D);
        return mob;
    }

    /** +50% max health and +25% attack damage (persistent), a "Veteran" name, a brief glow. */
    public static void makeVeteran(MobEntity mob) {
        addModifier(mob, EntityAttributes.GENERIC_MAX_HEALTH, ELITE_HEALTH_ID, "dndclasses.veteran_health", ELITE_HEALTH);
        addModifier(mob, EntityAttributes.GENERIC_ATTACK_DAMAGE, ELITE_DAMAGE_ID, "dndclasses.veteran_damage", ELITE_DAMAGE);
        mob.setHealth(mob.getMaxHealth());
        mob.setCustomName(Text.translatable("dungeon.dndclasses.veteran", mob.getType().getName()));
        mob.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, ELITE_GLOW_TICKS, 0, false, false));
        mob.addCommandTag(ELITE_TAG);
    }

    static void addModifier(LivingEntity mob, net.minecraft.entity.attribute.EntityAttribute attribute, UUID id, String name,
                            double amount) {
        EntityAttributeInstance instance = mob.getAttributeInstance(attribute);
        if (instance != null && instance.getModifier(id) == null) {
            instance.addPersistentModifier(new EntityAttributeModifier(id, name, amount, EntityAttributeModifier.Operation.MULTIPLY_BASE));
        }
    }

    /** Spawn points sorted furthest from the trigger first; the room's centre if it has none. */
    private static List<BlockPos> farthestFirst(List<BlockPos> points, BlockPos trigger, DungeonState.Room room) {
        List<BlockPos> sorted = new ArrayList<>(points);
        if (sorted.isEmpty()) {
            sorted.add(room.box().getCenter());
        }
        sorted.sort((a, b) -> Double.compare(b.getSquaredDistance(trigger), a.getSquaredDistance(trigger)));
        return sorted;
    }

    public static void arrivalSound(ServerWorld world, DungeonTheme theme, BlockPos pos) {
        SoundEvent sound = switch (theme) {
            case GOBLIN_WARREN -> SoundEvents.EVENT_RAID_HORN.value();
            default -> SoundEvents.ENTITY_SKELETON_AMBIENT;
        };
        world.playSound(null, pos, sound, SoundCategory.HOSTILE, 2.0F, 0.6F);
        world.playSound(null, pos, SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 1.0F, 0.8F);
    }

    /** {@code <startKey>/<roomId>} from a mob's room tag, or null if it has none. */
    public static long[] roomOf(Entity entity) {
        for (String tag : entity.getCommandTags()) {
            if (tag.startsWith(ROOM_TAG_PREFIX)) {
                String[] parts = tag.substring(ROOM_TAG_PREFIX.length()).split("/");
                try {
                    return new long[]{Long.parseLong(parts[0]), Integer.parseInt(parts[1])};
                } catch (RuntimeException e) {
                    return null;
                }
            }
        }
        return null;
    }
}
