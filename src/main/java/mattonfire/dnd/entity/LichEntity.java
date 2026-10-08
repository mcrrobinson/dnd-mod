package mattonfire.dnd.entity;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Registry.ModSounds;
import mattonfire.dnd.entity.boss.Boss;
import mattonfire.dnd.entity.boss.BossFight;
import mattonfire.dnd.entity.boss.BossMinions;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A late-game undead caster boss. It keeps its distance and casts: wither bolts, a ray of frost
 * that freezes and slows, a frost nova when cornered, and waves of raised undead. Its soul is kept
 * in a {@link PhylacteryEntity} placed nearby on its first tick: while that stands, killing the
 * Lich only sends it back to reform there (no loot), so the phylactery has to be smashed first.
 *
 * <p>Phases: below 60% health it raises stronger undead (strays and wither skeletons); below 30%
 * it gets desperate: faster spells, triple wither bolts and blinking away from melee.
 *
 * <p>Necromancers are its rivals: it goes for them first, its undead ignore their usual truce
 * with Necromancers, it taunts them, and Necromancers deal extra damage to it and its phylactery.
 */
public class LichEntity extends HostileEntity implements GeoEntity, Boss {
    public static final String MINION_TAG = "dndclasses.lich_minion";
    public static final Identifier LOOT_TABLE = new Identifier(DnDClasses.MOD_ID, "entities/lich");
    public static final Identifier ADVANCEMENT = new Identifier(DnDClasses.MOD_ID, "lichbane");
    public static final int XP = 150;
    /** Damage multiplier for Necromancer players hitting the Lich or its phylactery. */
    public static final float NECROMANCER_DAMAGE_BONUS = 1.5F;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation CAST = RawAnimation.begin().thenPlay("cast");
    private static final RawAnimation SUMMON = RawAnimation.begin().thenPlay("summon");
    private static final RawAnimation NOVA = RawAnimation.begin().thenPlay("nova");

    private static final double SPEECH_RANGE = 48.0D;
    private static final double MINION_RANGE = 32.0D;
    private static final double NOVA_RADIUS = 5.0D;
    private static final double FROST_RANGE = 20.0D;
    /** The Lich blinks back to its phylactery when lured further away than this. */
    private static final double LEASH_RANGE = 28.0D;
    private static final int HOME_RADIUS = 12;

    private static final UUID DESPERATE_SPEED_ID = UUID.fromString("0f6c3a5e-2b7d-4e1a-9c84-5d1e7a3b9f20");

    private enum Spell {
        WITHER_BOLT(8, 50, "cast"),
        RAY_OF_FROST(12, 90, "cast"),
        FROST_NOVA(10, 140, "nova"),
        RAISE_DEAD(24, 400, "summon");

        final int windUp;
        final int cooldown;
        final String animation;

        Spell(int windUp, int cooldown, String animation) {
            this.windUp = windUp;
            this.cooldown = cooldown;
            this.animation = animation;
        }
    }

    private enum PhylacteryState { NONE, LINKED, DESTROYED }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final BossFight bossFight;

    private final long[] nextCast = new long[Spell.values().length];
    @Nullable
    private Spell casting;
    private long castTime;
    private long nextBlink;
    private long nextDefend;

    private PhylacteryState phylacteryState = PhylacteryState.NONE;
    @Nullable
    private UUID phylacteryUuid;
    /** Where the phylactery stands (or, before it exists, should be placed). */
    @Nullable
    private BlockPos phylacteryPos;
    /** True when the Lich died but its soul fled to the phylactery: no loot, XP or advancement. */
    private boolean soulFled;

    private boolean greeted;
    private final Set<UUID> taunted = new HashSet<>();

    public LichEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.bossFight = new BossFight(this, BossBar.Color.PURPLE, BossBar.Style.NOTCHED_10)
                .range(SPEECH_RANGE)
                .music(ModSounds.MUSIC_LICH_FIGHT)
                .phase(0.6F, BossBar.Color.BLUE, this::enterRaisePhase)
                .phase(0.3F, BossBar.Color.RED, this::enterDesperatePhase)
                .xp(XP)
                .advancement(ADVANCEMENT);
    }

    public static DefaultAttributeContainer.Builder createLichAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 300.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 10.0D)
                .add(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, 4.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.26D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0D);
    }

    /** Stops the Lich and its phylactery and minions from hurting each other (stray bolts, blasts and arrows). */
    public static void registerEvents() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
                !(isLichSide(entity) && isLichSide(source.getAttacker())));
    }

    // ---------------------------------------------------------------- helpers

    public static boolean isLichSide(@Nullable Entity entity) {
        return entity instanceof LichEntity || entity instanceof PhylacteryEntity
                || entity != null && entity.getCommandTags().contains(MINION_TAG);
    }

    /** The Lich and its raised dead don't honour the undead's truce with Necromancers. */
    public static boolean defiesNecromancers(MobEntity mob) {
        return mob instanceof LichEntity || mob.getCommandTags().contains(MINION_TAG);
    }

    public static boolean isNecromancer(@Nullable Entity entity) {
        return entity instanceof PlayerEntityExt ext && ext.getDndClass() == DndCharacter.NECROMANCER;
    }

    static Text speech(Text speaker, String line, Object... args) {
        return Text.translatable("chat.dndclasses.lich.speech", speaker,
                Text.translatable("entity.dndclasses.lich.say." + line, args).formatted(Formatting.ITALIC))
                .formatted(Formatting.DARK_PURPLE);
    }

    static void broadcast(ServerWorld world, Entity from, Text message) {
        for (ServerPlayerEntity player : world.getPlayers(p -> p.squaredDistanceTo(from) < SPEECH_RANGE * SPEECH_RANGE)) {
            player.sendMessage(message, false);
        }
    }

    /** Says a line (entity.dndclasses.lich.say.&lt;line&gt;) to every player nearby. */
    public void speak(String line, Object... args) {
        if (this.world instanceof ServerWorld world) {
            broadcast(world, this, speech(this.getDisplayName(), line, args));
        }
    }

    // ---------------------------------------------------------------- goals

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new CasterMovementGoal(this));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.6D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 12.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this).setGroupRevenge());
        // Rivals first: a Necromancer in sight is always the Lich's chosen target.
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false,
                LichEntity::isNecromancer));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.add(4, new ActiveTargetGoal<>(this, IronGolemEntity.class, true));
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        LivingEntity previous = this.getTarget();
        super.setTarget(target);
        if (this.world.isClient || target == previous || !(target instanceof PlayerEntity player)) {
            return;
        }
        if (isNecromancer(player)) {
            if (this.taunted.add(player.getUuid())) {
                this.speak(this.random.nextBoolean() ? "necromancer" : "necromancer_alt", player.getDisplayName());
                this.greeted = true;
            }
        } else if (!this.greeted) {
            this.greeted = true;
            this.speak("greet");
        }
    }

    // ---------------------------------------------------------------- ticking

    @Override
    public void tick() {
        super.tick();
        this.bossFight.tick();
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        ServerWorld world = (ServerWorld) this.world;
        long now = world.getTime();

        if (this.phylacteryState == PhylacteryState.NONE) {
            this.createPhylactery(world);
        }
        PhylacteryEntity phylactery = this.findPhylactery();
        if (phylactery != null) {
            this.tickPhylacteryLink(world, phylactery, now);
        }

        LivingEntity target = this.getTarget();
        if (this.casting != null) {
            this.getNavigation().stop();
            if (target != null) {
                this.getLookControl().lookAt(target, 30.0F, 30.0F);
            }
            world.spawnParticles(this.casting == Spell.WITHER_BOLT || this.casting == Spell.RAISE_DEAD
                            ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SNOWFLAKE,
                    this.getX(), this.getBodyY(0.9D), this.getZ(), 2, 0.4D, 0.4D, 0.4D, 0.02D);
            if (now >= this.castTime) {
                Spell spell = this.casting;
                this.casting = null;
                this.cast(world, spell, target);
            }
            return;
        }
        if (target == null || !target.isAlive()) {
            return;
        }
        Spell spell = this.chooseSpell(world, target, now);
        if (spell != null) {
            this.casting = spell;
            this.castTime = now + spell.windUp;
            this.nextCast[spell.ordinal()] = now + this.cooldown(spell);
            this.triggerAnim("action", spell.animation);
            this.playSound(spell == Spell.RAISE_DEAD ? SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON
                    : SoundEvents.ENTITY_EVOKER_PREPARE_ATTACK, 2.0F, 0.7F);
        }
    }

    private int cooldown(Spell spell) {
        int cooldown = spell.cooldown;
        if (spell == Spell.RAISE_DEAD && this.bossFight.getPhase() >= 1) {
            cooldown = 300;
        }
        return this.isDesperate() ? cooldown * 3 / 5 : cooldown;
    }

    @Nullable
    private Spell chooseSpell(ServerWorld world, LivingEntity target, long now) {
        double distance = this.distanceTo(target);
        boolean canSee = this.getVisibilityCache().canSee(target);
        if (this.ready(Spell.FROST_NOVA, now) && !this.enemiesWithin(NOVA_RADIUS - 1.0D).isEmpty()) {
            return Spell.FROST_NOVA;
        }
        if (this.ready(Spell.RAISE_DEAD, now) && this.minions(world).size() < this.maxMinions()) {
            return Spell.RAISE_DEAD;
        }
        if (canSee && this.ready(Spell.RAY_OF_FROST, now) && distance <= FROST_RANGE) {
            return Spell.RAY_OF_FROST;
        }
        if (canSee && this.ready(Spell.WITHER_BOLT, now) && distance <= 32.0D) {
            return Spell.WITHER_BOLT;
        }
        return null;
    }

    private boolean ready(Spell spell, long now) {
        return now >= this.nextCast[spell.ordinal()];
    }

    public boolean isCasting() {
        return this.casting != null;
    }

    private boolean isDesperate() {
        return this.bossFight.getPhase() >= 2;
    }

    // ---------------------------------------------------------------- spells

    private void cast(ServerWorld world, Spell spell, @Nullable LivingEntity target) {
        switch (spell) {
            case WITHER_BOLT -> this.castWitherBolt(world, target);
            case RAY_OF_FROST -> this.castRayOfFrost(world, target);
            case FROST_NOVA -> this.castFrostNova(world);
            case RAISE_DEAD -> this.castRaiseDead(world, target);
        }
    }

    private Vec3d staffTip() {
        float yaw = this.bodyYaw * MathHelper.RADIANS_PER_DEGREE;
        // The staff is held to the Lich's right, a little forward.
        double side = -0.4D;
        double forward = 0.3D;
        return new Vec3d(this.getX() - MathHelper.sin(yaw) * forward + MathHelper.cos(yaw) * side,
                this.getY() + 2.2D,
                this.getZ() + MathHelper.cos(yaw) * forward + MathHelper.sin(yaw) * side);
    }

    private void castWitherBolt(ServerWorld world, @Nullable LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return;
        }
        Vec3d tip = this.staffTip();
        int bolts = this.isDesperate() ? 3 : 1;
        for (int i = 0; i < bolts; i++) {
            double spread = (i - (bolts - 1) / 2.0D) * 1.5D;
            double dx = target.getX() - tip.x + spread * MathHelper.cos(this.bodyYaw * MathHelper.RADIANS_PER_DEGREE);
            double dy = target.getBodyY(0.5D) - tip.y;
            double dz = target.getZ() - tip.z + spread * MathHelper.sin(this.bodyYaw * MathHelper.RADIANS_PER_DEGREE);
            WitherSkullEntity skull = new LichBoltEntity(world, this, dx, dy, dz);
            skull.setOwner(this);
            skull.setPos(tip.x, tip.y, tip.z);
            if (this.isDesperate() && i == 1) {
                skull.setCharged(true);
            }
            world.spawnEntity(skull);
        }
        world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_WITHER_SHOOT,
                SoundCategory.HOSTILE, 1.0F, 0.8F);
    }

    private void castRayOfFrost(ServerWorld world, @Nullable LivingEntity target) {
        if (target == null || !target.isAlive() || !this.getVisibilityCache().canSee(target)) {
            return;
        }
        Vec3d tip = this.staffTip();
        Vec3d end = new Vec3d(target.getX(), target.getBodyY(0.5D), target.getZ());
        Vec3d step = end.subtract(tip);
        int points = Math.max(4, (int) (step.length() * 3));
        for (int i = 0; i <= points; i++) {
            Vec3d p = tip.add(step.multiply(i / (double) points));
            world.spawnParticles(ParticleTypes.SNOWFLAKE, p.x, p.y, p.z, 2, 0.05D, 0.05D, 0.05D, 0.0D);
        }
        world.spawnParticles(ParticleTypes.ITEM_SNOWBALL, end.x, end.y, end.z, 12, 0.3D, 0.4D, 0.3D, 0.05D);
        world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_PLAYER_HURT_FREEZE,
                SoundCategory.HOSTILE, 1.5F, 0.8F);
        world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BLOCK_GLASS_BREAK,
                SoundCategory.HOSTILE, 0.8F, 1.6F);
        this.chill(target, 7.0F, 100, 2);
    }

    private void castFrostNova(ServerWorld world) {
        for (int i = 0; i < 48; i++) {
            float angle = i / 48.0F * MathHelper.TAU;
            world.spawnParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY() + 0.3D, this.getZ(), 0,
                    MathHelper.cos(angle), 0.05D, MathHelper.sin(angle), 0.5D);
        }
        world.spawnParticles(ParticleTypes.ITEM_SNOWBALL, this.getX(), this.getBodyY(0.5D), this.getZ(),
                40, 1.5D, 0.6D, 1.5D, 0.1D);
        world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BLOCK_POWDER_SNOW_BREAK,
                SoundCategory.HOSTILE, 2.0F, 0.5F);
        world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_EVOKER_CAST_SPELL,
                SoundCategory.HOSTILE, 1.5F, 0.6F);
        for (LivingEntity victim : this.enemiesWithin(NOVA_RADIUS)) {
            this.chill(victim, 8.0F, 120, 3);
            victim.takeKnockback(1.6D, this.getX() - victim.getX(), this.getZ() - victim.getZ());
            victim.velocityModified = true;
        }
    }

    /** Frost damage plus slowness and freezing (the powder snow frost overlay and its chip damage). */
    private void chill(LivingEntity victim, float damage, int slowTicks, int slowAmplifier) {
        victim.damage(this.getDamageSources().indirectMagic(this, this), damage);
        victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, slowTicks, slowAmplifier), this);
        if (victim.canFreeze()) {
            victim.setFrozenTicks(Math.max(victim.getFrozenTicks(), victim.getMinFreezeDamageTicks() + slowTicks * 2));
        }
    }

    private List<LivingEntity> enemiesWithin(double radius) {
        return this.world.getEntitiesByClass(LivingEntity.class, this.getBoundingBox().expand(radius),
                e -> e.isAlive() && !isLichSide(e) && this.squaredDistanceTo(e) <= radius * radius
                        && (e instanceof PlayerEntity player ? !player.isCreative() && !player.isSpectator()
                        : e == this.getTarget() || e instanceof IronGolemEntity));
    }

    private int maxMinions() {
        return switch (this.bossFight.getPhase()) {
            case 0 -> 4;
            case 1 -> 6;
            default -> 8;
        };
    }

    private List<MobEntity> minions(ServerWorld world) {
        return world.getEntitiesByClass(MobEntity.class, this.getBoundingBox().expand(MINION_RANGE),
                e -> e.isAlive() && e.getCommandTags().contains(MINION_TAG));
    }

    private void castRaiseDead(ServerWorld world, @Nullable LivingEntity target) {
        int wave = 2 + this.bossFight.getPhase();
        for (int i = 0; i < wave; i++) {
            EntityType<? extends MobEntity> type = this.pickMinionType(i);
            MobEntity minion = type.create(world);
            if (minion == null || !this.placeNear(minion, this.getBlockPos(), 2.0D, 4.0D)) {
                continue;
            }
            minion.initialize(world, world.getLocalDifficulty(minion.getBlockPos()), SpawnReason.MOB_SUMMONED, null, null);
            minion.addCommandTag(MINION_TAG);
            BossMinions.mark(minion);
            if (target != null && target.isAlive()) {
                minion.setTarget(target);
            }
            world.spawnEntityAndPassengers(minion);
            world.spawnParticles(ParticleTypes.SOUL, minion.getX(), minion.getY() + 0.2D, minion.getZ(),
                    15, 0.4D, 0.2D, 0.4D, 0.03D);
            world.spawnParticles(ParticleTypes.LARGE_SMOKE, minion.getX(), minion.getBodyY(0.5D), minion.getZ(),
                    10, 0.3D, 0.6D, 0.3D, 0.01D);
        }
        world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_EVOKER_CAST_SPELL,
                SoundCategory.HOSTILE, 2.0F, 0.5F);
        world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE,
                SoundCategory.HOSTILE, 0.6F, 0.5F);
    }

    private EntityType<? extends MobEntity> pickMinionType(int index) {
        if (this.bossFight.getPhase() >= 1 && this.random.nextInt(3) == 0) {
            return EntityType.WITHER_SKELETON;
        }
        boolean archer = index % 2 == 1;
        if (archer) {
            return this.bossFight.getPhase() >= 1 && this.random.nextBoolean() ? EntityType.STRAY : EntityType.SKELETON;
        }
        return this.random.nextInt(4) == 0 ? EntityType.HUSK : EntityType.ZOMBIE;
    }

    /** Finds standing room for {@code entity} between {@code min} and {@code max} blocks around {@code center}. */
    private boolean placeNear(Entity entity, BlockPos center, double min, double max) {
        for (int attempt = 0; attempt < 16; attempt++) {
            float angle = this.random.nextFloat() * MathHelper.TAU;
            double distance = min + this.random.nextDouble() * (max - min);
            BlockPos column = BlockPos.ofFloored(center.getX() + 0.5D + MathHelper.cos(angle) * distance, center.getY(),
                    center.getZ() + 0.5D + MathHelper.sin(angle) * distance);
            for (int dy = 2; dy >= -3; dy--) {
                BlockPos pos = column.up(dy);
                if (!this.world.getBlockState(pos.down()).isSideSolidFullSquare(this.world, pos.down(), Direction.UP)) {
                    continue;
                }
                entity.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                        this.random.nextFloat() * 360.0F, 0.0F);
                if (this.world.isSpaceEmpty(entity) && !this.world.containsFluid(entity.getBoundingBox())) {
                    return true;
                }
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- phases

    private void enterRaisePhase() {
        this.speak("phase_raise");
        // Answer the wound with a wave straight away.
        this.nextCast[Spell.RAISE_DEAD.ordinal()] = this.world.getTime();
        ((ServerWorld) this.world).spawnParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getBodyY(0.6D),
                this.getZ(), 20, 0.8D, 1.0D, 0.8D, 0.02D);
    }

    private void enterDesperatePhase() {
        this.speak("phase_desperate");
        EntityAttributeInstance speed = this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        EntityAttributeModifier modifier = new EntityAttributeModifier(DESPERATE_SPEED_ID, "Lich desperation speed",
                0.25D, EntityAttributeModifier.Operation.MULTIPLY_BASE);
        if (speed != null && !speed.hasModifier(modifier)) {
            speed.addPersistentModifier(modifier);
        }
        this.playSound(SoundEvents.ENTITY_WITHER_AMBIENT, 2.0F, 0.6F);
        this.nextCast[Spell.FROST_NOVA.ordinal()] = this.world.getTime();
    }

    // ---------------------------------------------------------------- blinking

    /** Teleports to a random spot {@code min}-{@code max} blocks around {@code center}. */
    private boolean blink(BlockPos center, double min, double max) {
        Vec3d from = this.getPos();
        for (int attempt = 0; attempt < 16; attempt++) {
            float angle = this.random.nextFloat() * MathHelper.TAU;
            double distance = min + this.random.nextDouble() * (max - min);
            double x = center.getX() + 0.5D + MathHelper.cos(angle) * distance;
            double z = center.getZ() + 0.5D + MathHelper.sin(angle) * distance;
            double y = center.getY() + this.random.nextInt(5) - 2;
            if (this.teleport(x, y, z, false)) {
                ServerWorld world = (ServerWorld) this.world;
                world.spawnParticles(ParticleTypes.SOUL, from.x, from.y + 1.2D, from.z, 25, 0.3D, 0.8D, 0.3D, 0.02D);
                world.spawnParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 1.2D, this.getZ(),
                        25, 0.3D, 0.8D, 0.3D, 0.02D);
                world.playSound(null, from.x, from.y, from.z, SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE,
                        SoundCategory.HOSTILE, 1.5F, 0.8F);
                this.playSound(SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.5F, 0.8F);
                this.getNavigation().stop();
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- phylactery

    /** Sets where the phylactery goes when the Lich first ticks (used by structure generation). */
    public void setPhylacterySpot(BlockPos pos) {
        this.phylacteryPos = pos;
        this.setPositionTarget(pos, HOME_RADIUS);
    }

    /** Links this Lich to an existing phylactery (a reformed Lich), so it doesn't make a new one. */
    public void linkPhylactery(PhylacteryEntity phylactery) {
        this.phylacteryState = PhylacteryState.LINKED;
        this.phylacteryUuid = phylactery.getUuid();
        this.phylacteryPos = phylactery.getBlockPos();
        this.setPositionTarget(this.phylacteryPos, HOME_RADIUS);
        phylactery.setLich(this);
    }

    private void createPhylactery(ServerWorld world) {
        PhylacteryEntity phylactery = ModEntityTypes.PHYLACTERY.create(world);
        if (phylactery == null) {
            this.phylacteryState = PhylacteryState.DESTROYED;
            return;
        }
        boolean placed = false;
        if (this.phylacteryPos != null) {
            phylactery.refreshPositionAndAngles(this.phylacteryPos.getX() + 0.5D, this.phylacteryPos.getY(),
                    this.phylacteryPos.getZ() + 0.5D, this.random.nextFloat() * 360.0F, 0.0F);
            placed = world.isSpaceEmpty(phylactery);
        }
        if (!placed && !this.placeNear(phylactery, this.getBlockPos(), 4.0D, 7.0D)) {
            phylactery.refreshPositionAndAngles(this.getX(), this.getY(), this.getZ(), 0.0F, 0.0F);
        }
        this.linkPhylactery(phylactery);
        world.spawnEntity(phylactery);
    }

    @Nullable
    private PhylacteryEntity findPhylactery() {
        if (this.phylacteryState == PhylacteryState.LINKED && this.phylacteryUuid != null
                && this.world instanceof ServerWorld world
                && world.getEntity(this.phylacteryUuid) instanceof PhylacteryEntity phylactery && phylactery.isAlive()) {
            return phylactery;
        }
        return null;
    }

    private void tickPhylacteryLink(ServerWorld world, PhylacteryEntity phylactery, long now) {
        this.phylacteryPos = phylactery.getBlockPos();
        double distance = this.distanceTo(phylactery);
        if (this.age % 20 == 0 && this.getHealth() < this.getMaxHealth()) {
            // The phylactery slowly mends the Lich: another reason to smash it.
            this.heal(1.0F);
        }
        if (this.age % 10 == 0 && distance < 24.0D) {
            // A faint thread of souls between the Lich and its phylactery
            Vec3d from = new Vec3d(phylactery.getX(), phylactery.getBodyY(0.8D), phylactery.getZ());
            Vec3d to = new Vec3d(this.getX(), this.getBodyY(0.6D), this.getZ());
            double t = (this.age / 10 % 8) / 8.0D;
            Vec3d p = from.lerp(to, t);
            world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
        if (this.age % 20 == 0 && distance > LEASH_RANGE && this.casting == null && now >= this.nextBlink) {
            this.nextBlink = now + 100;
            this.blink(this.phylacteryPos, 2.0D, 5.0D);
        }
    }

    /** The phylactery was hit: come back to defend it and turn on the attacker. */
    void defendPhylactery(PhylacteryEntity phylactery, LivingEntity attacker) {
        long now = this.world.getTime();
        if (attacker instanceof PlayerEntity player && (player.isCreative() || player.isSpectator())) {
            return;
        }
        this.setTarget(attacker);
        if (now >= this.nextDefend) {
            this.nextDefend = now + 600;
            this.speak("defend");
            if (this.distanceTo(phylactery) > 8.0D && now >= this.nextBlink) {
                this.nextBlink = now + 100;
                this.blink(phylactery.getBlockPos(), 2.0D, 4.0D);
            }
        }
    }

    void onPhylacteryDestroyed(@Nullable Entity attacker) {
        this.phylacteryState = PhylacteryState.DESTROYED;
        this.phylacteryUuid = null;
        this.speak("phylactery_destroyed");
        this.playSound(SoundEvents.ENTITY_WITHER_HURT, 2.0F, 0.5F);
        if (attacker instanceof LivingEntity living && !(attacker instanceof PlayerEntity player && player.isCreative())) {
            this.setTarget(living);
        }
    }

    // ---------------------------------------------------------------- damage and death

    @Override
    public boolean damage(DamageSource source, float amount) {
        Entity attacker = source.getAttacker();
        if (!this.world.isClient && isLichSide(attacker)) {
            return false;
        }
        if (isNecromancer(attacker)) {
            amount *= NECROMANCER_DAMAGE_BONUS;
        }
        boolean damaged = super.damage(source, amount);
        long now = this.world.getTime();
        if (damaged && !this.world.isClient && this.isAlive() && this.isDesperate() && now >= this.nextBlink
                && attacker != null && attacker == source.getSource() && this.squaredDistanceTo(attacker) < 16.0D
                && this.random.nextFloat() < 0.4F) {
            // Cornered and desperate: blink away from melee.
            this.nextBlink = now + 60;
            this.casting = null;
            this.blink(this.getBlockPos(), 6.0D, 10.0D);
        }
        return damaged;
    }

    @Override
    public void onDeath(DamageSource source) {
        if (!this.world.isClient && !this.dead && !this.isRemoved()) {
            PhylacteryEntity phylactery = this.findPhylactery();
            if (phylactery != null) {
                this.soulFled = true;
                this.speak("flee");
                phylactery.beginReform(this);
            } else {
                this.bossFight.onDeath();
                LivingEntity killer = this.getPrimeAdversary();
                if (isNecromancer(killer)) {
                    this.speak("death_necromancer", killer.getDisplayName());
                } else {
                    this.speak("death");
                }
            }
            this.removeMinions((ServerWorld) this.world);
        }
        super.onDeath(source);
    }

    /** The raised dead crumble with their master. */
    private void removeMinions(ServerWorld world) {
        for (MobEntity minion : this.minions(world)) {
            world.spawnParticles(ParticleTypes.SOUL, minion.getX(), minion.getBodyY(0.5D), minion.getZ(),
                    8, 0.3D, 0.5D, 0.3D, 0.02D);
            minion.kill();
        }
    }

    @Override
    protected boolean shouldDropLoot() {
        return !this.soulFled && super.shouldDropLoot();
    }

    @Override
    public boolean shouldDropXp() {
        return !this.soulFled && super.shouldDropXp();
    }

    @Override
    public EntityGroup getGroup() {
        return EntityGroup.UNDEAD;
    }

    @Override
    public boolean canHaveStatusEffect(StatusEffectInstance effect) {
        return effect.getEffectType() != StatusEffects.WITHER && effect.getEffectType() != StatusEffects.POISON
                && super.canHaveStatusEffect(effect);
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    public boolean isTeammate(Entity other) {
        return isLichSide(other) || super.isTeammate(other);
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_WITHER_SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_WITHER_SKELETON_DEATH;
    }

    @Override
    public float getSoundPitch() {
        return (this.random.nextFloat() - this.random.nextFloat()) * 0.1F + 0.6F;
    }

    @Override
    public BossFight getBossFight() {
        return this.bossFight;
    }

    // ---------------------------------------------------------------- saving

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        this.bossFight.writeNbt(nbt);
        nbt.putString("PhylacteryState", this.phylacteryState.name());
        if (this.phylacteryUuid != null) {
            nbt.putUuid("Phylactery", this.phylacteryUuid);
        }
        if (this.phylacteryPos != null) {
            nbt.put("PhylacteryPos", NbtHelper.fromBlockPos(this.phylacteryPos));
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.bossFight.readNbt(nbt);
        if (nbt.getBoolean("NoPhylactery")) {
            // e.g. /summon dndclasses:lich ~ ~ ~ {NoPhylactery:1b} for a Lich that dies for good
            this.phylacteryState = PhylacteryState.DESTROYED;
        } else if (nbt.contains("PhylacteryState")) {
            try {
                this.phylacteryState = PhylacteryState.valueOf(nbt.getString("PhylacteryState"));
            } catch (IllegalArgumentException e) {
                this.phylacteryState = PhylacteryState.NONE;
            }
        }
        this.phylacteryUuid = nbt.containsUuid("Phylactery") ? nbt.getUuid("Phylactery") : null;
        if (nbt.contains("PhylacteryPos")) {
            this.phylacteryPos = NbtHelper.toBlockPos(nbt.getCompound("PhylacteryPos"));
            this.setPositionTarget(this.phylacteryPos, HOME_RADIUS);
        }
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        this.bossFight.onStoppedTrackingBy(player);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        this.bossFight.onRemoved();
    }

    // ---------------------------------------------------------------- animation

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, this::movementPredicate));
        controllers.add(new AnimationController<LichEntity>(this, "action", 0, state -> PlayState.STOP)
                .triggerableAnim("cast", CAST)
                .triggerableAnim("summon", SUMMON)
                .triggerableAnim("nova", NOVA));
    }

    private PlayState movementPredicate(AnimationState<LichEntity> state) {
        return state.setAndContinue(state.isMoving() ? WALK : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    /**
     * A wither skull whose blast hurts but never breaks blocks (the Lich lives in libraries). It has
     * no entity type of its own, so it isn't saved: it would come back as a vanilla wither skull.
     */
    private static class LichBoltEntity extends WitherSkullEntity {
        LichBoltEntity(World world, LivingEntity owner, double dx, double dy, double dz) {
            super(world, owner, dx, dy, dz);
        }

        @Override
        public boolean shouldSave() {
            return false;
        }

        @Override
        protected void onCollision(HitResult hitResult) {
            if (hitResult.getType() == HitResult.Type.ENTITY) {
                this.onEntityHit((EntityHitResult) hitResult);
            } else if (hitResult.getType() == HitResult.Type.BLOCK) {
                this.onBlockHit((BlockHitResult) hitResult);
            }
            if (!this.world.isClient) {
                this.world.createExplosion(this, this.getX(), this.getY(), this.getZ(), 1.0F, false,
                        World.ExplosionSourceType.NONE);
                this.discard();
            }
        }
    }

    /**
     * Keeps a caster's distance: backs off when the target gets within 6 blocks, closes in when it's
     * beyond 14 or out of sight, and otherwise stands its ground to cast.
     */
    private static class CasterMovementGoal extends Goal {
        private final LichEntity lich;
        private int repath;

        CasterMovementGoal(LichEntity lich) {
            this.lich = lich;
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            LivingEntity target = this.lich.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void start() {
            this.repath = 0;
        }

        @Override
        public void stop() {
            this.lich.getNavigation().stop();
        }

        @Override
        public boolean shouldRunEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.lich.getTarget();
            if (target == null) {
                return;
            }
            this.lich.getLookControl().lookAt(target, 30.0F, 30.0F);
            if (this.lich.isCasting() || --this.repath > 0) {
                return;
            }
            this.repath = 10;
            double distance = this.lich.squaredDistanceTo(target);
            boolean canSee = this.lich.getVisibilityCache().canSee(target);
            if (distance < 6.0D * 6.0D) {
                Vec3d away = NoPenaltyTargeting.findFrom(this.lich, 10, 4, target.getPos());
                if (away != null) {
                    this.lich.getNavigation().startMovingTo(away.x, away.y, away.z, 1.2D);
                }
            } else if (distance > 14.0D * 14.0D || !canSee) {
                this.lich.getNavigation().startMovingTo(target, 1.0D);
            } else {
                this.lich.getNavigation().stop();
            }
        }
    }
}
