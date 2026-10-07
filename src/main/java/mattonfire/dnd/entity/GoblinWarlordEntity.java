package mattonfire.dnd.entity;

import java.util.List;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.GoToWalkTargetGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;

/**
 * Boss of the Nether Fortress: a big, crowned Goblin Warrior that guards the fortress's central
 * crossing and bellows for goblin reinforcements during a fight. Below half health it enrages,
 * hitting harder, moving faster and calling bigger waves.
 */
public class GoblinWarlordEntity extends GoblinWarriorEntity {
    private static final RawAnimation WARCRY = RawAnimation.begin().thenPlay("warcry");

    /** Ticks from starting the warcry to the minions appearing; matches the "warcry" animation. */
    private static final int WARCRY_WIND_UP = 12;
    private static final int SUMMON_COOLDOWN = 300;
    private static final int ENRAGED_SUMMON_COOLDOWN = 200;
    private static final int WAVE_SIZE = 2;
    private static final int ENRAGED_WAVE_SIZE = 3;
    /** No new wave while this many goblins are still fighting nearby. */
    private static final int MAX_MINIONS = 4;
    private static final int ENRAGED_MAX_MINIONS = 6;
    private static final double MINION_RANGE = 24.0;
    /** How far the warlord wanders from the spot it guards when not fighting. */
    private static final int GUARD_RADIUS = 12;

    private static final UUID ENRAGE_SPEED_ID = UUID.fromString("5b0a7f3e-6c1d-4a59-9f8e-2d3c1b7a9e41");
    private static final UUID ENRAGE_DAMAGE_ID = UUID.fromString("a3e4c2d1-8b7f-4e60-b5a9-1c2d3e4f5a62");

    private static final double BOSS_BAR_RANGE = 48.0;
    private final ServerBossBar bossBar = new ServerBossBar(this.getDisplayName(),
            BossBar.Color.YELLOW, BossBar.Style.NOTCHED_10);

    private boolean enraged;
    private long nextSummonTime;
    private long summonTime = -1;
    private int pendingWave;

    public GoblinWarlordEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 60;
    }

    public static DefaultAttributeContainer.Builder createGoblinWarlordAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 250.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 12.0D)
                .add(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, 4.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.9D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 15.0D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 1.5D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        // Heads back to the spot it guards (see setGuardPos) once the fight is over.
        this.goalSelector.add(4, new GoToWalkTargetGoal(this, 1.0D));
    }

    /** Keeps the warlord near {@code pos} when it isn't chasing anything. */
    public void setGuardPos(BlockPos pos) {
        this.setPositionTarget(pos, GUARD_RADIUS);
    }

    @Override
    protected AnimationController<GoblinWarriorEntity> createActionController() {
        return super.createActionController().triggerableAnim("warcry", WARCRY);
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        long now = this.world.getTime();
        LivingEntity target = this.getTarget();

        if (!this.enraged && this.getHealth() < this.getMaxHealth() / 2.0F) {
            this.enrage();
            // Answer the first big wound with a wave straight away.
            this.nextSummonTime = now;
        }

        if (this.summonTime >= 0) {
            // Stand still and roar until the wave arrives.
            this.getNavigation().stop();
            if (target != null) {
                this.getLookControl().lookAt(target, 30.0F, 30.0F);
            }
            if (now >= this.summonTime) {
                this.summonTime = -1;
                this.summonWave(target);
            }
        } else if (target != null && target.isAlive() && now >= this.nextSummonTime
                && this.nearbyGoblins().size() < (this.enraged ? ENRAGED_MAX_MINIONS : MAX_MINIONS)) {
            this.pendingWave = this.enraged ? ENRAGED_WAVE_SIZE : WAVE_SIZE;
            this.summonTime = now + WARCRY_WIND_UP;
            this.nextSummonTime = now + (this.enraged ? ENRAGED_SUMMON_COOLDOWN : SUMMON_COOLDOWN);
            this.triggerAnim("attack_controller", "warcry");
            this.playSound(SoundEvents.ENTITY_RAVAGER_ROAR, 2.0F, 1.3F);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.world.isClient) {
            this.updateBossBar();
        }
    }

    private void enrage() {
        this.enraged = true;
        addModifier(this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED),
                new EntityAttributeModifier(ENRAGE_SPEED_ID, "Warlord enrage speed", 0.25D,
                        EntityAttributeModifier.Operation.MULTIPLY_BASE));
        addModifier(this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE),
                new EntityAttributeModifier(ENRAGE_DAMAGE_ID, "Warlord enrage damage", 5.0D,
                        EntityAttributeModifier.Operation.ADDITION));
        this.playSound(SoundEvents.ENTITY_PIGLIN_BRUTE_ANGRY, 2.0F, 0.8F);
        ((ServerWorld) this.world).spawnParticles(ParticleTypes.ANGRY_VILLAGER,
                this.getX(), this.getBodyY(0.9D), this.getZ(), 6, 0.6D, 0.4D, 0.6D, 0.0D);
    }

    private static void addModifier(EntityAttributeInstance attribute, EntityAttributeModifier modifier) {
        if (attribute != null && !attribute.hasModifier(modifier)) {
            attribute.addPersistentModifier(modifier);
        }
    }

    /** Living Goblin Warriors around the warlord, whether it summoned them or they live here. */
    private List<GoblinWarriorEntity> nearbyGoblins() {
        return this.world.getEntitiesByClass(GoblinWarriorEntity.class,
                this.getBoundingBox().expand(MINION_RANGE),
                goblin -> goblin.isAlive() && !(goblin instanceof GoblinWarlordEntity));
    }

    private void summonWave(LivingEntity target) {
        ServerWorld world = (ServerWorld) this.world;
        for (int i = 0; i < this.pendingWave; i++) {
            GoblinWarriorEntity minion = ModEntityTypes.GOBLIN_WARRIOR.create(world);
            if (minion == null || !this.placeMinion(minion)) {
                continue;
            }
            minion.initialize(world, world.getLocalDifficulty(minion.getBlockPos()), SpawnReason.MOB_SUMMONED, null, null);
            if (target != null && target.isAlive()) {
                minion.setTarget(target);
            }
            world.spawnEntityAndPassengers(minion);
            world.spawnParticles(ParticleTypes.LARGE_SMOKE, minion.getX(), minion.getBodyY(0.5D), minion.getZ(),
                    15, 0.4D, 0.6D, 0.4D, 0.02D);
            world.spawnParticles(ParticleTypes.FLAME, minion.getX(), minion.getY() + 0.1D, minion.getZ(),
                    10, 0.4D, 0.1D, 0.4D, 0.02D);
        }
        this.world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_EVOKER_CAST_SPELL,
                SoundCategory.HOSTILE, 1.5F, 0.6F);
    }

    /** Finds standing room for a minion a few blocks around the warlord. */
    private boolean placeMinion(GoblinWarriorEntity minion) {
        for (int attempt = 0; attempt < 12; attempt++) {
            float angle = this.random.nextFloat() * MathHelper.TAU;
            double distance = 2.5D + this.random.nextDouble() * 2.0D;
            BlockPos column = BlockPos.ofFloored(this.getX() + MathHelper.cos(angle) * distance, this.getY(),
                    this.getZ() + MathHelper.sin(angle) * distance);
            for (int dy = 1; dy >= -2; dy--) {
                BlockPos pos = column.up(dy);
                if (!this.world.getBlockState(pos.down()).isSideSolidFullSquare(this.world, pos.down(), Direction.UP)) {
                    continue;
                }
                minion.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                        this.random.nextFloat() * 360.0F, 0.0F);
                if (this.world.isSpaceEmpty(minion)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean damaged = super.damage(source, amount);
        // Idle goblins nearby rally to their warlord.
        if (damaged && !this.world.isClient && source.getAttacker() instanceof LivingEntity attacker
                && !(attacker instanceof GoblinWarriorEntity)) {
            for (GoblinWarriorEntity goblin : this.nearbyGoblins()) {
                if (goblin.getTarget() == null) {
                    goblin.setTarget(attacker);
                }
            }
        }
        return damaged;
    }

    @Override
    public boolean isTeammate(Entity other) {
        return other instanceof GoblinWarriorEntity || super.isTeammate(other);
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    @Override
    public float getSoundPitch() {
        // Deeper than its squeaky warriors.
        return (this.random.nextFloat() - this.random.nextFloat()) * 0.1F + 0.85F;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("Enraged", this.enraged);
        if (this.hasPositionTarget()) {
            nbt.put("GuardPos", NbtHelper.fromBlockPos(this.getPositionTarget()));
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.enraged = nbt.getBoolean("Enraged");
        if (nbt.contains("GuardPos")) {
            this.setGuardPos(NbtHelper.toBlockPos(nbt.getCompound("GuardPos")));
        }
        if (this.hasCustomName()) {
            this.bossBar.setName(this.getDisplayName());
        }
    }

    // Boss bar for players near a warlord that is fighting a player, like the Wyvern's.

    private boolean isFightingPlayer() {
        return this.isAlive() && (this.getTarget() instanceof PlayerEntity || this.getAttacker() instanceof PlayerEntity);
    }

    private boolean inBossBarRange(ServerPlayerEntity player) {
        return player.world == this.world && player.isAlive() && !player.isSpectator()
                && player.squaredDistanceTo(this) < BOSS_BAR_RANGE * BOSS_BAR_RANGE;
    }

    private void updateBossBar() {
        this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());
        this.bossBar.setName(this.getDisplayName());
        this.bossBar.setColor(this.enraged ? BossBar.Color.RED : BossBar.Color.YELLOW);

        boolean fighting = this.isFightingPlayer();
        for (ServerPlayerEntity player : List.copyOf(this.bossBar.getPlayers())) {
            if (!fighting || !this.inBossBarRange(player)) {
                this.bossBar.removePlayer(player);
            }
        }
        if (fighting) {
            for (ServerPlayerEntity player : ((ServerWorld) this.world).getPlayers(this::inBossBarRange)) {
                this.bossBar.addPlayer(player);
            }
        }
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        this.bossBar.clearPlayers();
    }
}
