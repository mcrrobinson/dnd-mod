package mattonfire.dnd.entity.raid;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.ModSounds;
import mattonfire.dnd.entity.SettlementGrudges;
import mattonfire.dnd.entity.GoblinWarlordEntity;
import mattonfire.dnd.entity.GoblinWarriorEntity;
import mattonfire.dnd.entity.HobbitEntity;
import mattonfire.dnd.entity.ModEntityTypes;
import mattonfire.dnd.entity.MountainDwarfEntity;
import mattonfire.dnd.entity.boss.BossMusic;
import mattonfire.dnd.faction.TierEffects;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.Heightmap;
import org.jetbrains.annotations.Nullable;

/**
 * One goblin war party attacking a settlement. It comes in waves of Goblin Warriors, the last one
 * led by a Goblin Warlord; between waves there is a short lull with a horn call. Players near the
 * settlement see the raid's bar (counting down to the next wave, then the war party's remaining
 * health) and hear the raid music. Clearing every wave wins the raid and rewards everyone who took
 * part; if no one stays to defend it, the war party withdraws with its plunder.
 */
public class GoblinRaid {
    /** Players within this many blocks (horizontally) of the rally point are in the raid. */
    public static final double RADIUS = 96.0;
    private static final int FIRST_WAVE_DELAY = 200;
    private static final int WAVE_DELAY = 300;
    private static final int VICTORY_TICKS = 100;
    /** With nobody defending for this long, the war party withdraws. */
    private static final int ABANDON_TICKS = 20 * 120;
    /** A raider not loaded for this long (while defenders are about) is counted as gone. */
    private static final int MISSING_TICKS = 200;
    /** The last few raiders of a wave start glowing after this long, so they can be found. */
    private static final int GLOW_AFTER_TICKS = 20 * 30;
    private static final int GLOW_MAX_RAIDERS = 2;
    private static final double SPAWN_DISTANCE = 32.0;
    private static final double DEFENDER_RANGE = 16.0;
    private static final Identifier ADVANCEMENT = new Identifier(DnDClasses.MOD_ID, "goblin_raid_defended");
    /**
     * Every raider carries this tag. A tagged goblin that loads while no raid in its world claims
     * it (the raid ended, or forgot it, while it was unloaded) is sent away; see {@link GoblinRaids}.
     */
    public static final String RAIDER_TAG = "dndclasses.goblin_raider";

    public enum State {
        /** Counting down to the next wave. */
        APPROACHING,
        FIGHTING,
        VICTORY,
        /** Over; the manager drops it. */
        DONE
    }

    private static final class Raider {
        float health;
        float maxHealth;
        int missingTicks;

        Raider(float health, float maxHealth) {
            this.health = health;
            this.maxHealth = maxHealth;
        }
    }

    private final int id;
    private final Settlement.Kind kind;
    private final long settlementKey;
    private final BlockPos rally;
    @Nullable
    private final Direction outward;
    private final int totalWaves;

    private State state = State.APPROACHING;
    private int wave;
    private int stateTicks;
    private int absentTicks;
    private final Map<UUID, Raider> raiders = new LinkedHashMap<>();
    private final Set<UUID> participants = new HashSet<>();
    @Nullable
    private BlockPos waveSpawn;

    private final ServerBossBar bar = new ServerBossBar(Text.empty(), BossBar.Color.RED, BossBar.Style.NOTCHED_10);

    GoblinRaid(int id, Settlement settlement, Difficulty difficulty) {
        this.id = id;
        this.kind = settlement.kind();
        this.settlementKey = settlement.key();
        this.rally = settlement.rally();
        this.outward = settlement.outward();
        this.totalWaves = switch (difficulty) {
            case PEACEFUL, EASY -> 2;
            case NORMAL -> 3;
            case HARD -> 4;
        };
        this.stateTicks = FIRST_WAVE_DELAY;
    }

    private GoblinRaid(NbtCompound nbt) {
        this.id = nbt.getInt("Id");
        this.kind = Settlement.Kind.byId(nbt.getString("Kind"));
        this.settlementKey = nbt.getLong("Settlement");
        this.rally = NbtHelper.toBlockPos(nbt.getCompound("Rally"));
        this.outward = nbt.contains("Outward") ? Direction.byId(nbt.getInt("Outward")) : null;
        this.totalWaves = Math.max(1, nbt.getInt("TotalWaves"));
        this.state = State.values()[MathHelper.clamp(nbt.getInt("State"), 0, State.values().length - 1)];
        this.wave = nbt.getInt("Wave");
        this.stateTicks = nbt.getInt("StateTicks");
        this.absentTicks = nbt.getInt("AbsentTicks");
        for (NbtElement element : nbt.getList("Raiders", NbtElement.COMPOUND_TYPE)) {
            NbtCompound raider = (NbtCompound) element;
            this.raiders.put(raider.getUuid("UUID"), new Raider(raider.getFloat("Health"), raider.getFloat("MaxHealth")));
        }
        for (NbtElement element : nbt.getList("Participants", NbtElement.INT_ARRAY_TYPE)) {
            this.participants.add(NbtHelper.toUuid(element));
        }
        if (nbt.contains("WaveSpawn")) {
            this.waveSpawn = NbtHelper.toBlockPos(nbt.getCompound("WaveSpawn"));
        }
    }

    static GoblinRaid fromNbt(NbtCompound nbt) {
        return new GoblinRaid(nbt);
    }

    NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("Id", this.id);
        nbt.putString("Kind", this.kind.id);
        nbt.putLong("Settlement", this.settlementKey);
        nbt.put("Rally", NbtHelper.fromBlockPos(this.rally));
        if (this.outward != null) {
            nbt.putInt("Outward", this.outward.getId());
        }
        nbt.putInt("TotalWaves", this.totalWaves);
        nbt.putInt("State", this.state.ordinal());
        nbt.putInt("Wave", this.wave);
        nbt.putInt("StateTicks", this.stateTicks);
        nbt.putInt("AbsentTicks", this.absentTicks);
        NbtList raiders = new NbtList();
        this.raiders.forEach((uuid, raider) -> {
            NbtCompound tag = new NbtCompound();
            tag.putUuid("UUID", uuid);
            tag.putFloat("Health", raider.health);
            tag.putFloat("MaxHealth", raider.maxHealth);
            raiders.add(tag);
        });
        nbt.put("Raiders", raiders);
        NbtList participants = new NbtList();
        this.participants.forEach(uuid -> participants.add(NbtHelper.fromUuid(uuid)));
        nbt.put("Participants", participants);
        if (this.waveSpawn != null) {
            nbt.put("WaveSpawn", NbtHelper.fromBlockPos(this.waveSpawn));
        }
        return nbt;
    }

    public int getId() {
        return this.id;
    }

    public Settlement.Kind getKind() {
        return this.kind;
    }

    public long getSettlementKey() {
        return this.settlementKey;
    }

    public BlockPos getRally() {
        return this.rally;
    }

    public State getState() {
        return this.state;
    }

    public int getWave() {
        return this.wave;
    }

    public int getTotalWaves() {
        return this.totalWaves;
    }

    public int getRaidersLeft() {
        return this.raiders.size();
    }

    /** Whether this raid still counts {@code uuid} among its war party. */
    public boolean hasRaider(UUID uuid) {
        return this.raiders.containsKey(uuid);
    }

    public boolean isOver() {
        return this.state == State.DONE;
    }

    private Text placeName() {
        return Text.translatable(this.kind.nameKey());
    }

    // ---- Tick ----

    void tick(ServerWorld world) {
        if (this.state == State.DONE) {
            return;
        }
        // Peaceful sweeps the goblins away; that mustn't count as beating them.
        if (world.getDifficulty() == Difficulty.PEACEFUL && this.state != State.VICTORY) {
            this.withdraw(world);
            return;
        }
        List<ServerPlayerEntity> defenders = world.getPlayers(this::isDefender);
        this.updateBarPlayers(world, defenders);
        if (this.state == State.VICTORY) {
            if (--this.stateTicks <= 0) {
                this.end();
            }
            return;
        }
        // Nobody about: the raid stands still (its goblins may not even be loaded) until it's abandoned.
        if (defenders.isEmpty()) {
            if (++this.absentTicks >= ABANDON_TICKS) {
                this.withdraw(world);
            }
            return;
        }
        this.absentTicks = 0;

        if (this.state == State.APPROACHING) {
            this.bar.setName(Text.translatable("raid.dndclasses.goblin.approaching", this.wave + 1, this.totalWaves));
            int delay = this.wave == 0 ? FIRST_WAVE_DELAY : WAVE_DELAY;
            this.bar.setPercent(1.0F - MathHelper.clamp((float) this.stateTicks / delay, 0.0F, 1.0F));
            if (--this.stateTicks <= 0) {
                this.spawnWave(world);
            }
            return;
        }

        // Fighting
        this.stateTicks++;
        this.updateRaiders(world);
        this.bar.setName(Text.translatable("raid.dndclasses.goblin.fighting", this.wave, this.totalWaves, this.placeName()));
        float health = 0.0F;
        float maxHealth = 0.0F;
        for (Raider raider : this.raiders.values()) {
            health += raider.health;
            maxHealth += raider.maxHealth;
        }
        this.bar.setPercent(maxHealth <= 0.0F ? 0.0F : MathHelper.clamp(health / maxHealth, 0.0F, 1.0F));
        if (this.raiders.isEmpty()) {
            if (this.wave >= this.totalWaves) {
                this.win(world);
            } else {
                this.state = State.APPROACHING;
                this.stateTicks = WAVE_DELAY;
                this.waveSpawn = this.findSpawn(world);
                this.blowHorn(world);
            }
        } else if (this.stateTicks % 20 == 0) {
            this.steer(world, defenders);
        }
    }

    private boolean isDefender(ServerPlayerEntity player) {
        return player.isAlive() && !player.isSpectator() && this.inRange(player);
    }

    private boolean inRange(Entity entity) {
        double dx = entity.getX() - (this.rally.getX() + 0.5D);
        double dz = entity.getZ() - (this.rally.getZ() + 0.5D);
        return dx * dx + dz * dz < RADIUS * RADIUS && Math.abs(entity.getY() - this.rally.getY()) < RADIUS;
    }

    private void updateBarPlayers(ServerWorld world, List<ServerPlayerEntity> defenders) {
        for (ServerPlayerEntity player : List.copyOf(this.bar.getPlayers())) {
            if (player.isRemoved() || player.world != world || !defenders.contains(player)) {
                this.removeBarPlayer(player);
            }
        }
        for (ServerPlayerEntity player : defenders) {
            if (!this.bar.getPlayers().contains(player)) {
                this.bar.addPlayer(player);
                if (this.state != State.VICTORY) {
                    BossMusic.send(player, this.bar.getUuid(), ModSounds.MUSIC_GOBLIN_RAID);
                }
            }
            if (!player.isCreative() && !mattonfire.dnd.dm.DungeonMaster.isDm(player)) {
                this.participants.add(player.getUuid());
            }
        }
    }

    private void removeBarPlayer(ServerPlayerEntity player) {
        this.bar.removePlayer(player);
        BossMusic.send(player, this.bar.getUuid(), null);
    }

    /** Takes the bar and music away from everyone; called when the raid ends or is unloaded. */
    void clearBar() {
        for (ServerPlayerEntity player : List.copyOf(this.bar.getPlayers())) {
            this.removeBarPlayer(player);
        }
    }

    private void end() {
        this.state = State.DONE;
        this.clearBar();
    }

    // ---- Waves ----

    private void spawnWave(ServerWorld world) {
        this.wave++;
        this.state = State.FIGHTING;
        this.stateTicks = 0;
        BlockPos spawn = this.waveSpawn != null ? this.waveSpawn : this.findSpawn(world);
        this.waveSpawn = null;
        boolean finalWave = this.wave >= this.totalWaves;
        int warriors = 1 + this.wave + (world.getDifficulty() == Difficulty.HARD ? 1 : 0);
        for (int i = 0; i < warriors; i++) {
            GoblinWarriorEntity goblin = ModEntityTypes.GOBLIN_WARRIOR.create(world);
            if (goblin != null) {
                this.addRaider(world, goblin, spawn);
            }
        }
        if (finalWave) {
            GoblinWarlordEntity warlord = ModEntityTypes.GOBLIN_WARLORD.create(world);
            if (warlord != null) {
                this.addRaider(world, warlord, spawn);
                // Marches on the rally point, and holds it between fights.
                warlord.setGuardPos(this.rally);
            }
        }
        if (this.wave == 1) {
            this.blowHorn(world);
        }
        Text message = finalWave
                ? Text.translatable("raid.dndclasses.goblin.warlord", this.placeName()).formatted(Formatting.RED)
                : Text.translatable("raid.dndclasses.goblin.wave", this.wave, this.totalWaves, this.placeName()).formatted(Formatting.RED);
        this.message(world, message);
        DnDClasses.LOGGER.info("[GoblinRaid] raid {} wave {}/{} at {} ({} raiders)", this.id, this.wave, this.totalWaves,
                spawn.toShortString(), this.raiders.size());
    }

    private void addRaider(ServerWorld world, HostileEntity goblin, BlockPos spawn) {
        this.place(world, goblin, spawn);
        goblin.initialize(world, world.getLocalDifficulty(goblin.getBlockPos()), SpawnReason.EVENT, null, null);
        goblin.setPersistent();
        goblin.addCommandTag(RAIDER_TAG);
        // Claimed before it's spawned, so the load check in GoblinRaids keeps it.
        this.raiders.put(goblin.getUuid(), new Raider(goblin.getHealth(), goblin.getMaxHealth()));
        world.spawnEntityAndPassengers(goblin);
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, goblin.getX(), goblin.getBodyY(0.5D), goblin.getZ(),
                10, 0.4D, 0.6D, 0.4D, 0.02D);
    }

    /** Finds standing room for a goblin a few blocks round the war party's spawn point. */
    private void place(ServerWorld world, MobEntity goblin, BlockPos spawn) {
        for (int attempt = 0; attempt < 12; attempt++) {
            int dx = attempt == 0 ? 0 : world.random.nextInt(7) - 3;
            int dz = attempt == 0 ? 0 : world.random.nextInt(7) - 3;
            for (int dy = 2; dy >= -3; dy--) {
                BlockPos pos = spawn.add(dx, dy, dz);
                if (!world.getBlockState(pos.down()).isSideSolidFullSquare(world, pos.down(), Direction.UP)) {
                    continue;
                }
                goblin.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                        world.random.nextFloat() * 360.0F, 0.0F);
                if (world.isSpaceEmpty(goblin)) {
                    return;
                }
            }
        }
        goblin.refreshPositionAndAngles(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, 0.0F, 0.0F);
    }

    /**
     * Picks open ground about {@link #SPAWN_DISTANCE} blocks from the rally point (out in front of a
     * fortress gate), in loaded chunks, where the war party gathers.
     */
    private BlockPos findSpawn(ServerWorld world) {
        double baseAngle = this.outward != null
                ? Math.atan2(this.outward.getOffsetZ(), this.outward.getOffsetX())
                : world.random.nextDouble() * Math.PI * 2.0D;
        for (int attempt = 0; attempt < 40; attempt++) {
            double spread = this.outward != null ? Math.PI / 3.0D : Math.PI;
            double angle = attempt == 0 ? baseAngle : baseAngle + (world.random.nextDouble() * 2.0D - 1.0D) * spread;
            double distance = SPAWN_DISTANCE - attempt * 0.4D;
            int x = MathHelper.floor(this.rally.getX() + Math.cos(angle) * distance);
            int z = MathHelper.floor(this.rally.getZ() + Math.sin(angle) * distance);
            BlockPos column = new BlockPos(x, this.rally.getY(), z);
            if (!world.isChunkLoaded(column)) {
                continue;
            }
            BlockPos top = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, column);
            if (Math.abs(top.getY() - this.rally.getY()) > 20 || !world.getFluidState(top.down()).isEmpty()
                    || !world.getBlockState(top.down()).isSideSolidFullSquare(world, top.down(), Direction.UP)) {
                continue;
            }
            return top;
        }
        return this.rally;
    }

    /** The raid horn, heard from the direction the war party is coming from. */
    private void blowHorn(ServerWorld world) {
        BlockPos from = this.waveSpawn != null ? this.waveSpawn : this.rally;
        long seed = world.random.nextLong();
        for (ServerPlayerEntity player : world.getPlayers(this::inRange)) {
            Vec3d toward = Vec3d.ofCenter(from).subtract(player.getPos());
            Vec3d dir = toward.lengthSquared() < 1.0E-4D ? Vec3d.ZERO : toward.normalize().multiply(13.0D);
            player.networkHandler.sendPacket(new PlaySoundS2CPacket(SoundEvents.EVENT_RAID_HORN, SoundCategory.NEUTRAL,
                    player.getX() + dir.x, player.getY() + dir.y, player.getZ() + dir.z, 64.0F, 0.8F, seed));
        }
    }

    private void message(ServerWorld world, Text text) {
        for (ServerPlayerEntity player : world.getPlayers(this::inRange)) {
            player.sendMessage(text, false);
        }
    }

    // ---- Raiders ----

    /** Drops dead or vanished raiders, records the health of the rest, and lights up stragglers. */
    private void updateRaiders(ServerWorld world) {
        boolean glow = this.stateTicks > GLOW_AFTER_TICKS && this.raiders.size() <= GLOW_MAX_RAIDERS;
        this.raiders.entrySet().removeIf(entry -> {
            Raider raider = entry.getValue();
            Entity entity = world.getEntity(entry.getKey());
            if (entity == null) {
                return ++raider.missingTicks > MISSING_TICKS;
            }
            raider.missingTicks = 0;
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
                return true;
            }
            raider.health = living.getHealth();
            raider.maxHealth = living.getMaxHealth();
            if (glow && this.stateTicks % 20 == 0) {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 60, 0, false, false));
            }
            return false;
        });
    }

    /**
     * Raiders without a fight pick on the nearest defender (hobbit, dwarf, villager, golem or
     * player), or else march on the rally point. Dwarves turn on the nearest raider.
     */
    private void steer(ServerWorld world, List<ServerPlayerEntity> players) {
        List<MobEntity> goblins = new ArrayList<>();
        for (UUID uuid : this.raiders.keySet()) {
            if (world.getEntity(uuid) instanceof MobEntity mob && mob.isAlive()) {
                goblins.add(mob);
            }
        }
        for (MobEntity goblin : goblins) {
            LivingEntity target = goblin.getTarget();
            if (target != null && target.isAlive()) {
                continue;
            }
            LivingEntity defender = this.nearestDefender(world, goblin, players);
            if (defender != null) {
                goblin.setTarget(defender);
                continue;
            }
            Vec3d rallyPoint = Vec3d.ofBottomCenter(this.rally);
            if (goblin.squaredDistanceTo(rallyPoint) > 36.0D
                    && (goblin.getNavigation().isIdle() || this.stateTicks % 100 == 0)) {
                Vec3d step = goblin instanceof GoblinWarriorEntity warrior
                        ? NoPenaltyTargeting.findTo(warrior, 15, 4, rallyPoint, Math.PI / 2.0D) : null;
                Vec3d to = step != null ? step : rallyPoint;
                goblin.getNavigation().startMovingTo(to.x, to.y, to.z, 1.0D);
            }
        }
        // Dwarves rush out to meet the raiders.
        if (this.kind == Settlement.Kind.FORTRESS && !goblins.isEmpty()) {
            Box area = new Box(this.rally).expand(48.0D, 24.0D, 48.0D);
            for (MountainDwarfEntity dwarf : world.getEntitiesByClass(MountainDwarfEntity.class, area,
                    dwarf -> dwarf.isAlive() && dwarf.getTarget() == null)) {
                MobEntity nearest = null;
                double best = 32.0D * 32.0D;
                for (MobEntity goblin : goblins) {
                    double distance = dwarf.squaredDistanceTo(goblin);
                    if (distance < best) {
                        best = distance;
                        nearest = goblin;
                    }
                }
                if (nearest != null) {
                    dwarf.setTarget(nearest);
                }
            }
        }
    }

    @Nullable
    private LivingEntity nearestDefender(ServerWorld world, MobEntity goblin, List<ServerPlayerEntity> players) {
        LivingEntity nearest = null;
        double best = DEFENDER_RANGE * DEFENDER_RANGE;
        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class,
                goblin.getBoundingBox().expand(DEFENDER_RANGE), entity -> entity.isAlive() && (entity instanceof HobbitEntity
                        || entity instanceof MountainDwarfEntity || entity instanceof MerchantEntity
                        || entity instanceof IronGolemEntity))) {
            double distance = goblin.squaredDistanceTo(entity);
            if (distance < best) {
                best = distance;
                nearest = entity;
            }
        }
        for (ServerPlayerEntity player : players) {
            double distance = goblin.squaredDistanceTo(player);
            // Marked players are the horde's favourite target: they count as half as far away.
            if (TierEffects.isMarked(player, goblin.getType())) {
                distance /= 4.0D;
            }
            if (distance < best && !player.isCreative() && !mattonfire.dnd.dm.DungeonMaster.isDm(player)
                    && TierEffects.goblinMayTarget(goblin, player, false)) {
                best = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    // ---- Outcome ----

    private void win(ServerWorld world) {
        this.state = State.VICTORY;
        this.stateTicks = VICTORY_TICKS;
        this.bar.setName(Text.translatable("raid.dndclasses.goblin.victory", this.placeName()));
        this.bar.setColor(BossBar.Color.GREEN);
        this.bar.setPercent(1.0F);
        for (ServerPlayerEntity player : List.copyOf(this.bar.getPlayers())) {
            BossMusic.send(player, this.bar.getUuid(), null);
        }
        DnDClasses.LOGGER.info("[GoblinRaid] raid {} won by {} players", this.id, this.participants.size());

        // The settlement cheers.
        Box area = new Box(this.rally).expand(64.0D, 32.0D, 64.0D);
        for (LivingEntity folk : world.getEntitiesByClass(LivingEntity.class, area,
                entity -> entity.isAlive() && (entity instanceof HobbitEntity || entity instanceof MountainDwarfEntity))) {
            world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, folk.getX(), folk.getEyeY() + 0.3D, folk.getZ(),
                    8, 0.4D, 0.3D, 0.4D, 0.0D);
            folk.playSound(SoundEvents.ENTITY_VILLAGER_CELEBRATE, 1.0F, folk instanceof HobbitEntity ? 1.35F : 0.7F);
            if (folk.isOnGround()) {
                folk.setVelocity(folk.getVelocity().add(0.0D, 0.42D, 0.0D));
                folk.velocityModified = true;
            }
        }

        Identifier lootId = new Identifier(DnDClasses.MOD_ID, "gameplay/goblin_raid_" + this.kind.id);
        LootTable loot = world.getServer().getLootManager().getTable(lootId);
        Advancement advancement = world.getServer().getAdvancementLoader().get(ADVANCEMENT);
        List<ServerPlayerEntity> defenders = new java.util.ArrayList<>();
        for (UUID uuid : this.participants) {
            if (!(world.getEntity(uuid) instanceof ServerPlayerEntity player) || !player.isAlive()) {
                continue;
            }
            defenders.add(player);
            player.sendMessage(Text.translatable("raid.dndclasses.goblin.reward", this.placeName()).formatted(Formatting.GOLD), false);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HERO_OF_THE_VILLAGE, 20 * 60 * 40, 0, false, false, true));
            for (ItemStack stack : loot.generateLoot(new LootContext.Builder(world)
                    .parameter(LootContextParameters.THIS_ENTITY, player)
                    .parameter(LootContextParameters.ORIGIN, player.getPos())
                    .random(world.random)
                    .build(LootContextTypes.GIFT))) {
                if (!player.giveItemStack(stack)) {
                    player.dropItem(stack, false);
                }
            }
            ExperienceOrbEntity.spawn(world, player.getPos(), 40 + 20 * this.totalWaves);
            mattonfire.dnd.faction.FactionEvents.raidWon(player, world, new Identifier(DnDClasses.MOD_ID, this.kind.id),
                    mattonfire.dnd.entity.ModEntityTypes.GOBLIN_WARRIOR);
            if (this.kind == Settlement.Kind.FORTRESS) {
                SettlementGrudges.forgive(player, this.rally, RADIUS);
            }
            if (advancement != null) {
                AdvancementProgress progress = player.getAdvancementTracker().getProgress(advancement);
                for (String criterion : progress.getUnobtainedCriteria()) {
                    player.getAdvancementTracker().grantCriterion(advancement, criterion);
                }
            }
        }
        mattonfire.dnd.quest.QuestEvents.onRaidWon(world.getServer(), defenders, this.kind.id);
        world.playSound(null, this.rally, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.NEUTRAL, 1.0F, 1.0F);
    }

    /**
     * The war party gives up (or is called off): its goblins leave in a puff of smoke. Goblins that
     * aren't loaded are sent away when they next load (see {@link GoblinRaids}).
     */
    void withdraw(ServerWorld world) {
        for (UUID uuid : this.raiders.keySet()) {
            Entity entity = world.getEntity(uuid);
            if (entity != null && entity.isAlive()) {
                world.spawnParticles(ParticleTypes.LARGE_SMOKE, entity.getX(), entity.getBodyY(0.5D), entity.getZ(),
                        15, 0.4D, 0.6D, 0.4D, 0.02D);
                entity.discard();
            }
        }
        this.raiders.clear();
        for (UUID uuid : this.participants) {
            if (world.getEntity(uuid) instanceof ServerPlayerEntity player) {
                player.sendMessage(Text.translatable("raid.dndclasses.goblin.withdrew", this.placeName()), false);
            }
        }
        DnDClasses.LOGGER.info("[GoblinRaid] raid {} withdrew", this.id);
        this.end();
    }
}
