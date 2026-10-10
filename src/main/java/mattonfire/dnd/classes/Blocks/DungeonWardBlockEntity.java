package mattonfire.dnd.classes.Blocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Party.PartyEvents;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.dm.encounter.Encounter;
import mattonfire.dnd.dungeon.DungeonChampion;
import mattonfire.dnd.dungeon.DungeonCombat;
import mattonfire.dnd.dungeon.DungeonEncounterPools;
import mattonfire.dnd.dungeon.EncounterBuilder;
import mattonfire.dnd.entity.boss.BossMinions;
import mattonfire.dnd.entity.boss.StructureBosses;
import mattonfire.dnd.world.gen.dungeon.DungeonPiece;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtElement;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Difficulty;
import mattonfire.dnd.classes.Registry.ModBlocks;
import mattonfire.dnd.dungeon.DungeonEvents;
import mattonfire.dnd.dungeon.DungeonRegistry;
import mattonfire.dnd.dungeon.DungeonState;
import mattonfire.dnd.dungeon.RoomRole;
import mattonfire.dnd.dungeon.RoomState;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * A dungeon room's ward: which dungeon and room it belongs to, the room's box and its spawn
 * points, all written at worldgen. Every {@link #CHECK_INTERVAL} ticks it looks for survival or
 * adventure players in the box. The first time it sees each player in the dungeon it fires
 * {@link DungeonEvents#ENTERED} (and shows the dungeon's name and tier).
 *
 * Rooms with no fight count as cleared as soon as someone walks in. Fighting rooms
 * ({@link RoomRole#combat()}) run UNTOUCHED -> ACTIVE -> CLEARED: the first player past the walls
 * (5 blocks in, for the boss room) calls up an encounter scaled to the party
 * ({@link EncounterBuilder}), the champion ({@link DungeonChampion}) or the boss
 * ({@link StructureBosses#spawnBoss}); the doorways fill with {@code dndclasses:arcane_seal}; and they
 * open again when every tracked mob is gone. See {@code docs/systems/dungeon-encounters.md}.
 */
public class DungeonWardBlockEntity extends BlockEntity {
    public static final int CHECK_INTERVAL = 10;
    /** Nobody inside an ACTIVE room for this long (60 s): the fight is abandoned. */
    public static final int ABANDON_TICKS = 20 * 60;
    /** The same for the boss room (30 s): the boss despawns, and comes back fresh next time. */
    public static final int BOSS_ABANDON_TICKS = 20 * 30;
    /** A player has to be this far past the boss room's walls to call up the boss. */
    public static final int BOSS_THRESHOLD = 5;
    public static final double BOSS_HEALTH_PER_PLAYER = 0.35;
    public static final double BOSS_HEALTH_CAP = 3.45;
    public static final double BOSS_TIER_DAMAGE = 0.2;
    public static final float BOSS_T4_HEAL = 0.15F;
    /** Class XP per player for a cleared room, times the tier. */
    public static final int ROOM_XP_PER_TIER = 10;
    public static final String XP_CHANNEL = "dungeon";
    /** Walls are two blocks thick. */
    private static final int WALL = 2;
    private static final int MISSING_CHECKS = 4;
    private static final int RETRY_TICKS = 200;
    private static final String T4_HEAL_TAG = "dndclasses.dungeon_t4_healed";
    private static final UUID BOSS_DAMAGE_ID = UUID.fromString("e2b7c9d4-1f60-4a8b-9c3e-5d7f0a2b4c61");

    private long startKey;
    private int roomId = -1;
    private RoomRole role = RoomRole.ENCOUNTER_SMALL;
    @Nullable
    private BlockBox box;
    private final List<BlockPos> spawnPoints = new ArrayList<>();
    /** The whole dungeon as generated (see {@link DungeonRegistry#getOrCreate(NbtCompound)}). */
    @Nullable
    private NbtCompound summary;
    /** True while this ward has a fight going (seals up, mobs tracked). */
    private boolean engaged;
    private final Set<UUID> mobs = new HashSet<>();
    private final List<BlockPos> seals = new ArrayList<>();
    private long lastSeen;
    private int partySize = 1;
    /** Not saved: checks each tracked mob has been missing for, and when a failed start may retry. */
    private final Map<UUID, Integer> missing = new HashMap<>();
    private long nextTry;

    public DungeonWardBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.DUNGEON_WARD_ENTITY, pos, state);
    }

    public void setup(long startKey, int roomId, @Nullable RoomRole role, BlockBox box, List<BlockPos> spawnPoints,
                      @Nullable NbtCompound summary) {
        this.summary = summary;
        this.startKey = startKey;
        this.roomId = roomId;
        this.role = role == null ? RoomRole.ENCOUNTER_SMALL : role;
        this.box = box;
        this.spawnPoints.clear();
        this.spawnPoints.addAll(spawnPoints);
        this.markDirty();
    }

    public long getStartKey() {
        return this.startKey;
    }

    public int getRoomId() {
        return this.roomId;
    }

    public RoomRole getRole() {
        return this.role;
    }

    @Nullable
    public BlockBox getBox() {
        return this.box;
    }

    /** The whole dungeon as generated, or null for wards placed by hand. */
    @Nullable
    public NbtCompound getSummary() {
        return this.summary;
    }

    public List<BlockPos> getSpawnPoints() {
        return List.copyOf(this.spawnPoints);
    }

    public static void tick(World world, BlockPos pos, BlockState state, DungeonWardBlockEntity ward) {
        if (!(world instanceof ServerWorld server) || ward.roomId < 0 || ward.box == null
                || Math.floorMod(server.getTime() + pos.asLong(), CHECK_INTERVAL) != 0) {
            return;
        }
        BlockBox box = ward.box;
        List<ServerPlayerEntity> inside = server.getPlayers(player -> player.isAlive() && !player.isSpectator()
                && !player.isCreative() && box.contains(player.getBlockPos()));
        if (inside.isEmpty() && !ward.engaged) {
            return;
        }
        DungeonState dungeon = ward.dungeon(server);
        if (dungeon == null) {
            return;
        }
        if (!inside.isEmpty()) {
            DungeonRegistry.occupied(dungeon, server.getTime());
            for (ServerPlayerEntity player : inside) {
                if (DungeonRegistry.firstVisit(dungeon, player)) {
                    showTitle(dungeon, player);
                    DungeonEvents.ENTERED.invoker().onEntered(server, dungeon, player);
                }
            }
        }
        DungeonState.Room room = dungeon.room(ward.roomId);
        if (room == null) {
            return;
        }
        if (!ward.role.combat()) {
            // Rooms with no fight count as cleared once someone walks in.
            if (!inside.isEmpty() && room.state() == RoomState.UNTOUCHED) {
                dungeon.setRoomState(room, RoomState.CLEARED);
                DungeonEvents.ROOM_CLEARED.invoker().onRoomCleared(server, dungeon, room, inside);
            }
            return;
        }
        ward.tickCombat(server, dungeon, room, inside);
    }

    @Nullable
    private DungeonState dungeon(ServerWorld server) {
        DungeonRegistry registry = DungeonRegistry.get(server);
        DungeonState dungeon = registry.get(this.startKey);
        if (dungeon == null) {
            dungeon = this.summary != null ? registry.getOrCreate(this.summary)
                    : registry.getOrCreate(server.getStructureAccessor().getStructureContaining(this.pos, DungeonRegistry.DUNGEONS));
        }
        return dungeon;
    }

    // ---------------------------------------------------------------- the fight

    /**
     * The room's fight, every {@link #CHECK_INTERVAL} ticks: UNTOUCHED until a player is well inside
     * (then spawn and seal: ACTIVE), ACTIVE until every tracked mob is gone (CLEARED) or nobody has
     * been inside for {@link #ABANDON_TICKS} ({@link #BOSS_ABANDON_TICKS} for the boss; back to
     * UNTOUCHED). A state changed from outside (/dungeon clear or reset, the boss's death) is
     * followed: seals open and leftover mobs go.
     */
    private void tickCombat(ServerWorld world, DungeonState dungeon, DungeonState.Room room, List<ServerPlayerEntity> inside) {
        long now = world.getTime();
        RoomState state = room.state();
        if (state != RoomState.ACTIVE) {
            if (this.engaged) {
                DnDClasses.LOGGER.info("[Dungeon] room #{} {} is {} from outside the fight: opening it", this.roomId,
                        this.role.label(), state);
                this.disengage(world);
            }
            if (state == RoomState.UNTOUCHED && world.getDifficulty() != Difficulty.PEACEFUL && now >= this.nextTry) {
                ServerPlayerEntity trigger = this.triggering(inside);
                if (trigger != null) {
                    this.start(world, dungeon, room, trigger);
                }
            }
            return;
        }
        this.engaged = true;
        if (!inside.isEmpty()) {
            this.lastSeen = now;
        }
        if (world.getDifficulty() == Difficulty.PEACEFUL) {
            this.abandon(world, dungeon, room, "Peaceful");
            return;
        }
        this.adopt(world, dungeon);
        this.prune(world);
        this.seal(world);
        if (this.mobs.isEmpty()) {
            if (this.role == RoomRole.BOSS) {
                // The boss went without dying (a /kill on a fled Lich's phylactery is still a death,
                // so this is rare): leave the room ready for a fresh one.
                this.abandon(world, dungeon, room, "boss gone");
            } else {
                this.clearRoom(world, dungeon, room, inside);
            }
            return;
        }
        long abandonAfter = this.role == RoomRole.BOSS ? BOSS_ABANDON_TICKS : ABANDON_TICKS;
        if (inside.isEmpty() && now - this.lastSeen >= abandonAfter) {
            this.abandon(world, dungeon, room, "nobody inside for " + abandonAfter / 20 + " s");
        }
    }

    /** A player far enough in to start the fight: past the walls, or 5 blocks in for the boss room. */
    @Nullable
    private ServerPlayerEntity triggering(List<ServerPlayerEntity> inside) {
        int in = this.role == RoomRole.BOSS ? WALL + BOSS_THRESHOLD : WALL;
        BlockBox box = this.box;
        for (ServerPlayerEntity player : inside) {
            BlockPos p = player.getBlockPos();
            if (p.getX() >= box.getMinX() + in && p.getX() <= box.getMaxX() - in
                    && p.getZ() >= box.getMinZ() + in && p.getZ() <= box.getMaxZ() - in) {
                return player;
            }
        }
        return null;
    }

    /**
     * Starts the room's fight for the party of {@code trigger}: spawns the encounter (or the
     * champion and its minions, or the boss), seals the doorways and marks the room ACTIVE. Also
     * what {@code /dungeon trigger} calls. Returns how many mobs it spawned.
     */
    public int start(ServerWorld world, DungeonState dungeon, DungeonState.Room room, ServerPlayerEntity trigger) {
        if (world.getDifficulty() == Difficulty.PEACEFUL || !this.role.combat()) {
            return 0;
        }
        if (this.engaged) {
            this.disengage(world);
        }
        EncounterBuilder.Party party = EncounterBuilder.partyFor(world, dungeon, trigger);
        this.partySize = party.size();
        List<Entity> spawned = new ArrayList<>();
        String detail;
        if (this.role == RoomRole.BOSS) {
            MobEntity boss = this.spawnBoss(world, dungeon, trigger);
            if (boss != null) {
                spawned.add(boss);
            }
            detail = "boss " + (boss == null ? "none" : boss.getType().getUntranslatedName() + " with "
                    + boss.getMaxHealth() + " max health (x" + bossHealthMult(this.partySize) + ")");
        } else {
            List<String> extra = new ArrayList<>();
            if (this.role == RoomRole.CHAMPION) {
                MobEntity champion = this.spawnChampion(world, dungeon, room);
                if (champion != null) {
                    spawned.add(champion);
                }
            }
            EncounterBuilder.Spawned encounter = EncounterBuilder.spawn(world, dungeon, room, this.spawnPoints, party,
                    trigger.getBlockPos(), extra);
            spawned.addAll(encounter.mobs());
            detail = String.format(java.util.Locale.ROOT, "budget %.2f, spent %.2f on %d mob(s): %s", encounter.budget(),
                    encounter.spent(), encounter.mobs().size(), describe(encounter.mobs()));
        }
        DnDClasses.LOGGER.info("[Dungeon] room #{} {} triggered by {}: party {} (avg level {}), tier {}, {}", this.roomId,
                this.role.label(), trigger.getEntityName(), party.size(),
                String.format(java.util.Locale.ROOT, "%.1f", party.averageLevel()), dungeon.tier(), detail);
        if (spawned.isEmpty()) {
            this.nextTry = world.getTime() + RETRY_TICKS;
            return 0;
        }
        for (Entity mob : spawned) {
            this.mobs.add(mob.getUuid());
        }
        this.engaged = true;
        this.lastSeen = world.getTime();
        this.seals.clear();
        this.seals.addAll(this.findDoorways(world, dungeon));
        dungeon.setRoomState(room, RoomState.ACTIVE);
        this.seal(world);
        DnDClasses.LOGGER.info("[Dungeon] room #{} sealed {} doorway block(s): {}", this.roomId, this.seals.size(),
                this.seals.stream().map(BlockPos::toShortString).toList());
        world.playSound(null, this.box.getCenter(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.5F, 0.6F);
        for (ServerPlayerEntity player : DungeonCombat.playersIn(world, room)) {
            player.sendMessage(Text.translatable("dungeon.dndclasses.sealed").formatted(Formatting.LIGHT_PURPLE), true);
        }
        this.markDirty();
        return spawned.size();
    }

    private static String describe(List<Entity> mobs) {
        java.util.Map<String, Integer> counts = new java.util.TreeMap<>();
        for (Entity mob : mobs) {
            String name = mob.getType().getUntranslatedName() + (mob.getCommandTags().contains(EncounterBuilder.ELITE_TAG) ? " (veteran)" : "");
            counts.merge(name, 1, Integer::sum);
        }
        return counts.toString();
    }

    /** x1 for one player, +0.35 per extra player, at most x3.45 (8 players). */
    public static double bossHealthMult(int partySize) {
        return Math.min(BOSS_HEALTH_CAP, 1.0 + BOSS_HEALTH_PER_PLAYER * (Math.max(1, partySize) - 1));
    }

    @Nullable
    private MobEntity spawnBoss(ServerWorld world, DungeonState dungeon, ServerPlayerEntity trigger) {
        DungeonEncounterPools.Pool pool = DungeonEncounterPools.get(dungeon.theme());
        StructureBosses.BossType type = pool != null ? pool.boss() : StructureBosses.BossType.LICH;
        BlockPos stand = this.standingSpot(world, this.box.getCenter().withY(dungeon.floorY() + 1));
        // The phylactery waits at the spawn point furthest from the way in (the reliquary comes in dungeons ticket 6).
        BlockPos phylactery = this.spawnPoints.stream()
                .max(java.util.Comparator.comparingDouble(p -> p.getSquaredDistance(trigger.getBlockPos())))
                .orElse(stand);
        double mult = bossHealthMult(this.partySize);
        float yaw = (float) (MathHelper.atan2(trigger.getZ() - stand.getZ(), trigger.getX() - stand.getX()) * (180.0D / Math.PI)) - 90.0F;
        return StructureBosses.spawnBoss(world, stand, yaw, type, mult, phylactery, mob -> {
            EncounterBuilder.tags(dungeon.startKey(), this.roomId).forEach(mob::addCommandTag);
            mob.addCommandTag(DungeonCombat.BOSS_TAG);
            this.tierBonus(mob, dungeon.tier());
        });
    }

    @Nullable
    private MobEntity spawnChampion(ServerWorld world, DungeonState dungeon, DungeonState.Room room) {
        DungeonEncounterPools.Pool pool = DungeonEncounterPools.get(dungeon.theme());
        if (pool == null || pool.champion() == null) {
            return null;
        }
        DungeonEncounterPools.Champion champion = pool.champion();
        Encounter.Spawn spawn = new Encounter.Spawn(champion.entity(), 1, 1, 1.0F, new NbtCompound(), true, List.of());
        Entity entity = EncounterBuilder.spawnOne(world, spawn, this.box.getCenter().withY(dungeon.floorY() + 1),
                EncounterBuilder.tags(dungeon.startKey(), room.id()));
        if (!(entity instanceof MobEntity mob)) {
            return null;
        }
        DungeonChampion.make(world, mob, champion, this.partySize);
        return mob;
    }

    /** T3+: bosses hit 20% harder. */
    private void tierBonus(LivingEntity boss, int tier) {
        if (tier >= 3) {
            EntityAttributeInstance damage = boss.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
            if (damage != null && damage.getModifier(BOSS_DAMAGE_ID) == null) {
                damage.addPersistentModifier(new EntityAttributeModifier(BOSS_DAMAGE_ID, "dndclasses.dungeon_tier_damage",
                        BOSS_TIER_DAMAGE, EntityAttributeModifier.Operation.MULTIPLY_BASE));
            }
        }
    }

    /** The first spot going up from {@code from} with two free blocks over a solid one. */
    private BlockPos standingSpot(ServerWorld world, BlockPos from) {
        BlockPos.Mutable pos = from.mutableCopy();
        for (int i = 0; i < 6; i++) {
            if (world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()
                    && world.getBlockState(pos.up()).getCollisionShape(world, pos.up()).isEmpty()) {
                return pos.toImmutable();
            }
            pos.move(0, 1, 0);
        }
        return from;
    }

    /** Takes in tagged mobs that turned up in the room since (a phylactery, a reformed Lich, split cubes). */
    private void adopt(ServerWorld world, DungeonState dungeon) {
        String tag = EncounterBuilder.roomTag(dungeon.startKey(), this.roomId);
        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, Box.from(this.box).expand(1.0D),
                e -> e.isAlive() && e.getCommandTags().contains(tag))) {
            if (this.mobs.add(entity.getUuid())) {
                this.markDirty();
            }
            if (entity.getCommandTags().contains(DungeonCombat.BOSS_TAG)) {
                StructureBosses.scaleHealth(entity, bossHealthMult(this.partySize));
                this.tierBonus(entity, dungeon.tier());
                if (dungeon.tier() >= 4 && !entity.getCommandTags().contains(T4_HEAL_TAG)
                        && entity.getHealth() < entity.getMaxHealth() * 0.5F) {
                    entity.addCommandTag(T4_HEAL_TAG);
                    entity.heal(entity.getMaxHealth() * BOSS_T4_HEAL);
                    world.playSound(null, entity.getBlockPos(), SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 2.0F, 0.5F);
                }
            }
            if (DungeonChampion.isChampion(entity)) {
                DungeonChampion.tick(world, entity);
            }
        }
    }

    /** Drops tracked mobs that are gone: dead, or missing from the room for {@link #MISSING_CHECKS} checks running. */
    private void prune(ServerWorld world) {
        Box room = Box.from(this.box).expand(1.0D);
        Iterator<UUID> it = this.mobs.iterator();
        while (it.hasNext()) {
            UUID id = it.next();
            Entity entity = world.getEntity(id);
            if (entity != null && entity.isAlive() && room.contains(entity.getPos())) {
                this.missing.remove(id);
                continue;
            }
            int count = this.missing.merge(id, 1, Integer::sum);
            if ((entity != null && !entity.isAlive()) || count >= MISSING_CHECKS) {
                it.remove();
                this.missing.remove(id);
                DungeonChampion.removeBar(id);
                this.markDirty();
            }
        }
    }

    /** A tracked mob died (from the death listener in DungeonCombat). */
    public void onMobDied(UUID id) {
        if (this.mobs.remove(id)) {
            this.missing.remove(id);
            this.markDirty();
        }
    }

    public int trackedMobs() {
        return this.mobs.size();
    }

    public boolean isEngaged() {
        return this.engaged;
    }

    private void clearRoom(ServerWorld world, DungeonState dungeon, DungeonState.Room room, List<ServerPlayerEntity> inside) {
        dungeon.setRoomState(room, RoomState.CLEARED);
        this.unseal(world);
        this.engaged = false;
        this.markDirty();
        world.playSound(null, this.box.getCenter(), SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.5F, 1.0F);
        int xp = ROOM_XP_PER_TIER * dungeon.tier();
        for (ServerPlayerEntity player : inside) {
            player.sendMessage(Text.translatable("dungeon.dndclasses.room_cleared").formatted(Formatting.GREEN), true);
            PartyEvents.shareXp(player, xp, XP_CHANNEL, Progression::addXp);
        }
        DnDClasses.LOGGER.info("[Dungeon] room #{} {} cleared, {} player(s) inside, {} class XP each", this.roomId,
                this.role.label(), inside.size(), xp);
        DungeonEvents.ROOM_CLEARED.invoker().onRoomCleared(world, dungeon, room, inside);
    }

    /** Nobody stayed to finish it: the mobs (and the boss) go quietly, with no loot, and the room waits again. */
    private void abandon(ServerWorld world, DungeonState dungeon, DungeonState.Room room, String why) {
        DnDClasses.LOGGER.info("[Dungeon] room #{} {} abandoned ({}): {} mob(s) removed, back to UNTOUCHED", this.roomId,
                this.role.label(), why, this.mobs.size());
        this.disengage(world);
        dungeon.setRoomState(room, RoomState.UNTOUCHED);
    }

    /** Opens the seals and quietly removes what's left of the fight. */
    private void disengage(ServerWorld world) {
        for (UUID id : this.mobs) {
            Entity entity = world.getEntity(id);
            if (entity != null) {
                entity.discard();
            }
            DungeonChampion.removeBar(id);
        }
        if (this.role == RoomRole.BOSS) {
            for (Entity minion : world.getEntitiesByClass(MobEntity.class, Box.from(this.box).expand(1.0D), BossMinions::isMinion)) {
                minion.discard();
            }
        }
        this.mobs.clear();
        this.missing.clear();
        this.unseal(world);
        this.engaged = false;
        this.markDirty();
    }

    // ---------------------------------------------------------------- seals

    /**
     * The room's doorways: the blocks of its inner wall ring, up to the doorway height, that are
     * open right through the outer ring too.
     */
    private List<BlockPos> findDoorways(ServerWorld world, DungeonState dungeon) {
        List<BlockPos> doorways = new ArrayList<>();
        BlockBox b = this.box;
        BlockPos.Mutable inner = new BlockPos.Mutable();
        BlockPos.Mutable outer = new BlockPos.Mutable();
        for (int x = b.getMinX() + 1; x <= b.getMaxX() - 1; x++) {
            for (int z = b.getMinZ() + 1; z <= b.getMaxZ() - 1; z++) {
                boolean west = x == b.getMinX() + 1;
                boolean east = x == b.getMaxX() - 1;
                boolean north = z == b.getMinZ() + 1;
                boolean south = z == b.getMaxZ() - 1;
                if (!(west || east || north || south) || ((west || east) && (north || south))) {
                    continue;
                }
                int ox = west ? x - 1 : east ? x + 1 : x;
                int oz = north ? z - 1 : south ? z + 1 : z;
                for (int y = dungeon.floorY() + 1; y <= dungeon.floorY() + DungeonPiece.DOOR_HEIGHT; y++) {
                    inner.set(x, y, z);
                    outer.set(ox, y, oz);
                    BlockState in = world.getBlockState(inner);
                    if ((in.isAir() || in.isOf(ModBlocks.ARCANE_SEAL)) && world.getBlockState(outer).isAir()) {
                        doorways.add(inner.toImmutable());
                    }
                }
            }
        }
        return doorways;
    }

    /** Fills every doorway with a seal, except where someone's standing (tried again next check). */
    private void seal(ServerWorld world) {
        for (BlockPos pos : this.seals) {
            if (!world.isChunkLoaded(pos) || world.getBlockState(pos).isOf(ModBlocks.ARCANE_SEAL)
                    || !world.getBlockState(pos).isAir()) {
                continue;
            }
            if (!world.getEntitiesByClass(LivingEntity.class, new Box(pos), e -> true).isEmpty()) {
                continue;
            }
            world.setBlockState(pos, ModBlocks.ARCANE_SEAL.getDefaultState(), Block.NOTIFY_ALL);
            world.spawnParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 6, 0.3D, 0.3D, 0.3D, 0.5D);
        }
    }

    private void unseal(ServerWorld world) {
        for (BlockPos pos : this.seals) {
            if (world.isChunkLoaded(pos) && world.getBlockState(pos).isOf(ModBlocks.ARCANE_SEAL)) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                world.spawnParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 4, 0.3D, 0.3D, 0.3D, 0.05D);
            }
        }
        this.seals.clear();
    }

    public List<BlockPos> getSeals() {
        return List.copyOf(this.seals);
    }

    private static void showTitle(DungeonState dungeon, ServerPlayerEntity player) {
        player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 60, 20));
        player.networkHandler.sendPacket(new SubtitleS2CPacket(dungeon.tierText()));
        player.networkHandler.sendPacket(new TitleS2CPacket(dungeon.name()));
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putLong("StartKey", this.startKey);
        nbt.putInt("RoomId", this.roomId);
        nbt.putString("Role", this.role.name());
        if (this.box != null) {
            nbt.put("Box", DungeonState.box(this.box));
        }
        NbtList points = new NbtList();
        for (BlockPos point : this.spawnPoints) {
            points.add(NbtHelper.fromBlockPos(point));
        }
        nbt.put("SpawnPoints", points);
        if (this.summary != null) {
            nbt.put("Dungeon", this.summary);
        }
        nbt.putBoolean("Engaged", this.engaged);
        NbtList mobs = new NbtList();
        for (UUID id : this.mobs) {
            mobs.add(NbtHelper.fromUuid(id));
        }
        nbt.put("Mobs", mobs);
        NbtList seals = new NbtList();
        for (BlockPos seal : this.seals) {
            seals.add(NbtHelper.fromBlockPos(seal));
        }
        nbt.put("Seals", seals);
        nbt.putLong("LastSeen", this.lastSeen);
        nbt.putInt("PartySize", this.partySize);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.startKey = nbt.getLong("StartKey");
        this.roomId = nbt.contains("RoomId") ? nbt.getInt("RoomId") : -1;
        this.role = RoomRole.byName(nbt.getString("Role"));
        this.box = nbt.contains("Box") ? DungeonState.box(nbt.getIntArray("Box")) : null;
        this.summary = nbt.contains("Dungeon") ? nbt.getCompound("Dungeon") : null;
        this.spawnPoints.clear();
        NbtList points = nbt.getList("SpawnPoints", NbtCompound.COMPOUND_TYPE);
        for (int i = 0; i < points.size(); i++) {
            this.spawnPoints.add(NbtHelper.toBlockPos(points.getCompound(i)));
        }
        this.engaged = nbt.getBoolean("Engaged");
        this.mobs.clear();
        NbtList mobs = nbt.getList("Mobs", NbtElement.INT_ARRAY_TYPE);
        for (int i = 0; i < mobs.size(); i++) {
            this.mobs.add(NbtHelper.toUuid(mobs.get(i)));
        }
        this.seals.clear();
        NbtList seals = nbt.getList("Seals", NbtCompound.COMPOUND_TYPE);
        for (int i = 0; i < seals.size(); i++) {
            this.seals.add(NbtHelper.toBlockPos(seals.getCompound(i)));
        }
        this.lastSeen = nbt.getLong("LastSeen");
        this.partySize = Math.max(1, nbt.getInt("PartySize"));
    }
}
