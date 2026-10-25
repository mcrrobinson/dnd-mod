package mattonfire.dnd.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Registry.ModSounds;
import mattonfire.dnd.classes.SkillChecks.SaveResult;
import mattonfire.dnd.entity.ai.goal.CasterMovementGoal;
import mattonfire.dnd.entity.ai.Blink;
import mattonfire.dnd.entity.boss.Boss;
import mattonfire.dnd.entity.boss.BossFight;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
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
import net.minecraft.util.math.BlockPos;
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
    /** A consecrated Desecrated Altar within this many blocks of the Lich weakens its raised dead... */
    public static final int CONSECRATED_RADIUS = 24;
    /** ...to this share of their health. */
    public static final float CONSECRATED_MINION_HEALTH = 0.5F;
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
    /** The Lich blinks back to its phylactery when lured further away than this. */
    private static final double LEASH_RANGE = 28.0D;
    private static final int HOME_RADIUS = 12;

    private static final UUID DESPERATE_SPEED_ID = UUID.fromString("0f6c3a5e-2b7d-4e1a-9c84-5d1e7a3b9f20");

    private enum PhylacteryState { NONE, LINKED, DESTROYED }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final BossFight bossFight;
    private final LichSpells spells = new LichSpells(this);

    @Nullable
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
    /**
     * A lair the Lich keeps to, or null: a dungeon's boss room ({@link #setLair}). With a lair, home
     * is the lair rather than the phylactery, and the leash is measured from it.
     */
    @Nullable
    private BlockPos lair;
    private double lairLeash = LEASH_RANGE;

    private boolean greeted;
    private long lastDreadPresence = -1L;
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
        this.tickLairLeash(now);

        this.spells.tick(world, this.getTarget(), now);
    }

    /** Finds standing room for {@code entity} between {@code min} and {@code max} blocks around {@code center}. */
    boolean placeNear(Entity entity, BlockPos center, double min, double max) {
        return StandingRoom.around(this.world, entity, this.random, center.getX() + 0.5D, center.getY(),
                center.getZ() + 0.5D, min, max, 16, 2, 3, true);
    }

    /** True while it's winding up a spell. */
    public boolean isCasting() {
        return this.spells.isCasting();
    }

    boolean isDesperate() {
        return this.bossFight.getPhase() >= 2;
    }

    // ---------------------------------------------------------------- phases

    private void enterRaisePhase() {
        this.speak("phase_raise");
        this.dreadPresence();
        // Answer the wound with a wave straight away.
        this.spells.readyNow(LichSpells.Spell.RAISE_DEAD, this.world.getTime());
        ((ServerWorld) this.world).spawnParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getBodyY(0.6D),
                this.getZ(), 20, 0.8D, 1.0D, 0.8D, 0.02D);
    }

    /** In its last phase (below 30% health) its save DCs are one higher. */
    public boolean isLastPhase() {
        return this.bossFight.getPhase() >= 2;
    }

    /**
     * Dread Presence: every player within {@value BossSaves#DREAD_RADIUS} blocks makes a WIS save
     * (DC 15, 16 in the last phase) or is Frightened for 8 seconds. Necromancers, its rivals, have
     * advantage. Runs as it passes 60% and 30% health and when it reforms; one save per player per
     * trigger, even when a huge hit passes both thresholds at once.
     */
    public void dreadPresence() {
        if (!(this.world instanceof ServerWorld world) || world.getTime() == this.lastDreadPresence) {
            return; // Both thresholds passed by one hit: one Dread Presence
        }
        this.lastDreadPresence = world.getTime();
        broadcast(world, this, Text.translatable("chat.dndclasses.lich.dread_presence", this.getDisplayName())
                .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC));
        world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE,
                SoundCategory.HOSTILE, 1.5F, 0.6F);
        for (int i = 0; i < 64; i++) {
            float angle = i / 64.0F * MathHelper.TAU;
            world.spawnParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getBodyY(0.5D), this.getZ(), 0,
                    MathHelper.cos(angle), 0.02D, MathHelper.sin(angle), 0.6D);
        }
        world.spawnParticles(ParticleTypes.SQUID_INK, this.getX(), this.getBodyY(0.5D), this.getZ(),
                30, 0.6D, 1.0D, 0.6D, 0.05D);
        boolean lastPhase = this.isLastPhase();
        for (ServerPlayerEntity player : world.getPlayers(p -> p.isAlive() && !p.isCreative() && !p.isSpectator()
                && p.squaredDistanceTo(this) <= BossSaves.DREAD_RADIUS * BossSaves.DREAD_RADIUS)) {
            SaveResult save = BossSaves.save(player, this, BossSaves.DREAD_PRESENCE, lastPhase, "dread",
                    BossSaves.DREAD_WINDOW)
                    .tags("fear")
                    .advantageIf(isNecromancer(player))
                    .onFailure(Text.translatable("save.dndclasses.effect.dread_presence.failure"))
                    .roll();
            if (save.fresh() && save.failed()) {
                mattonfire.dnd.classes.Conditions.Conditions.of(player, mattonfire.dnd.classes.Conditions.Condition.FRIGHTENED).source(this).key("dread")
                        .ticks(BossSaves.DREAD_FRIGHTENED_TICKS)
                        .saveLabel(BossSaves.DREAD_PRESENCE.labelKey())
                        .repeatSave(BossSaves.DREAD_PRESENCE.ability(),
                                lastPhase ? BossSaves.LAST_PHASE_DC : BossSaves.DREAD_PRESENCE.dc(),
                                BossSaves.FEAR_REPEAT_TICKS)
                        .saveTags("fear").apply();
            }
        }
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
        this.spells.readyNow(LichSpells.Spell.FROST_NOVA, this.world.getTime());
        this.dreadPresence();
    }

    // ---------------------------------------------------------------- blinking

    /** Teleports to a random spot {@code min}-{@code max} blocks around {@code center}. */
    private boolean blink(BlockPos center, double min, double max) {
        return Blink.blink(this, center, min, max, null, ParticleTypes.SOUL);
    }

    // ---------------------------------------------------------------- phylactery

    /** True once this (dead) Lich's soul has fled to its phylactery to reform: not a true death. */
    public boolean hasFledToPhylactery() {
        return this.soulFled;
    }

    /** Sets where the phylactery goes when the Lich first ticks (used by structure generation). */
    public void setPhylacterySpot(BlockPos pos) {
        this.phylacteryPos = pos;
        this.setPositionTarget(this.lair != null ? this.lair : pos, HOME_RADIUS);
    }

    /**
     * Gives the Lich a lair to keep to (a dungeon's boss room, whose reliquary holds the phylactery):
     * it wanders round {@code centre}, and whenever it's more than {@code leash} blocks from it, it
     * blinks back. Without a lair it keeps to its phylactery within {@value #LEASH_RANGE} blocks.
     */
    public void setLair(BlockPos centre, double leash) {
        this.lair = centre.toImmutable();
        this.lairLeash = leash;
        this.setPositionTarget(this.lair, HOME_RADIUS);
    }

    @Nullable
    public BlockPos getLair() {
        return this.lair;
    }

    public double getLairLeash() {
        return this.lairLeash;
    }

    /** The lair's leash: lured further than its reach from the lair, the Lich blinks home. */
    private void tickLairLeash(long now) {
        if (this.lair != null && this.age % 20 == 0 && !this.spells.isCasting() && now >= this.nextBlink
                && Math.sqrt(this.getBlockPos().getSquaredDistance(this.lair)) > this.lairLeash) {
            this.nextBlink = now + 100;
            this.blink(this.lair, 2.0D, 5.0D);
        }
    }

    /** Links this Lich to an existing phylactery (a reformed Lich), so it doesn't make a new one. */
    public void linkPhylactery(PhylacteryEntity phylactery) {
        this.phylacteryState = PhylacteryState.LINKED;
        this.phylacteryUuid = phylactery.getUuid();
        this.phylacteryPos = phylactery.getBlockPos();
        this.setPositionTarget(this.lair != null ? this.lair : this.phylacteryPos, HOME_RADIUS);
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
        this.raisePedestal(world, phylactery);
        this.linkPhylactery(phylactery);
        // Encounter tags carry over, so /dm encounter clear and "loot": false cover the phylactery too.
        this.getCommandTags().stream().filter(tag -> !tag.equals(mattonfire.dnd.dm.DmFreeze.TAG))
                .forEach(phylactery::addCommandTag);
        world.spawnEntity(phylactery);
    }

    /**
     * Sets a Phylactery Pedestal under a new phylactery that isn't already on one (the Crypt's reliquary has
     * its own): in the block the phylactery stands in, if that's air and there's room for it one block up,
     * and mob griefing is on.
     */
    private void raisePedestal(ServerWorld world, PhylacteryEntity phylactery) {
        BlockPos feet = BlockPos.ofFloored(phylactery.getX(), phylactery.getY() + 0.01D, phylactery.getZ());
        if (world.getBlockState(feet.down()).isOf(mattonfire.dnd.classes.Registry.CryptBlocks.PHYLACTERY_PEDESTAL)
                || !world.getGameRules().getBoolean(net.minecraft.world.GameRules.DO_MOB_GRIEFING)
                || !world.getBlockState(feet).isAir()) {
            return;
        }
        net.minecraft.util.math.Box raised = phylactery.getBoundingBox().offset(0.0D, feet.getY() + 1.0D - phylactery.getY(), 0.0D);
        if (!world.isSpaceEmpty(phylactery, raised)) {
            return;
        }
        world.setBlockState(feet, mattonfire.dnd.classes.Registry.CryptBlocks.PHYLACTERY_PEDESTAL.getDefaultState());
        phylactery.refreshPositionAndAngles(phylactery.getX(), feet.getY() + 1.0D, phylactery.getZ(),
                phylactery.getYaw(), 0.0F);
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
        if (this.lair == null && this.age % 20 == 0 && distance > LEASH_RANGE && !this.spells.isCasting() && now >= this.nextBlink) {
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
            this.spells.interrupt();
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
        for (MobEntity minion : this.spells.minions(world)) {
            world.spawnParticles(ParticleTypes.SOUL, minion.getX(), minion.getBodyY(0.5D), minion.getZ(),
                    8, 0.3D, 0.5D, 0.3D, 0.02D);
            minion.kill();
        }
    }

    /**
     * True once the Lich has died but its soul fled to the phylactery (it will reform): not a real
     * kill, so quests don't count it.
     */
    public boolean soulFled() {
        return this.soulFled;
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
        if (this.lair != null) {
            nbt.put("Lair", NbtHelper.fromBlockPos(this.lair));
            nbt.putDouble("LairLeash", this.lairLeash);
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
        if (nbt.contains("Lair")) {
            this.setLair(NbtHelper.toBlockPos(nbt.getCompound("Lair")),
                    nbt.contains("LairLeash") ? nbt.getDouble("LairLeash") : LEASH_RANGE);
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
}
