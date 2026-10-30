package mattonfire.dnd.entity.dragon;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Conditions.Condition;
import mattonfire.dnd.classes.Conditions.Conditions;
import mattonfire.dnd.classes.Registry.ModSounds;
import mattonfire.dnd.classes.SkillChecks.SaveResult;
import mattonfire.dnd.classes.SkillChecks.SavingThrow;
import mattonfire.dnd.entity.DragonPartLayout;
import mattonfire.dnd.entity.LairDragonEntity;
import mattonfire.dnd.entity.MobActions;
import mattonfire.dnd.entity.ai.goal.DragonFlyRandomlyGoal;
import mattonfire.dnd.entity.boss.BossFight;
import mattonfire.dnd.entity.boss.LegendaryCreature;
import mattonfire.dnd.entity.boss.LegendaryResistance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;

/**
 * An Ancient chromatic dragon (design 10, section 5): the endgame boss. Everything the colours share lives here:
 * <ul>
 * <li><b>Size</b>: the Lightning Chaser's skeleton thickened ({@code ancient_dragon.geo.json}) and drawn at
 * {@value #SCALE}x, so a ~46-block wingspan; a 4x4 box with 23 scaled hit shapes, a visibility box as wide as
 * the wings and a 12-chunk tracking range.</li>
 * <li><b>Frightful Presence</b>: at the fight's start, every {@value #FRIGHT_INTERVAL} ticks and on entering its
 * last phase, players within {@value #FRIGHT_RADIUS} who can see it make a WIS save or are Frightened; a success
 * makes them immune for the rest of the fight.</li>
 * <li><b>Breath recharge</b>: a breath spends it; every {@value #RECHARGE_TICKS} ticks it rolls a d6 and recharges
 * on 5-6 (4-6 in its last phase).</li>
 * <li><b>Legendary Resistance</b> ({@link LegendaryResistance}, 3 a day) and <b>legendary actions</b>
 * ({@link LegendaryActions}).</li>
 * <li><b>Lair actions</b> ({@link ChromaticColor#lairActions()}) while the fight is on within
 * {@value #LAIR_RADIUS} blocks of its lair, every 20 / 14 / 10 s by phase.</li>
 * <li><b>Phases</b>: below 60% "The Mountain Answers" (faster lair actions, calls its kobolds), below 30% "Molten
 * Fury" (faster breath recharge, +15% speed, a new Frightful Presence).</li>
 * </ul>
 * It never despawns, never respawns at its lair, and flies home when drawn more than {@value #LEASH} blocks away.
 */
public abstract class AncientDragonEntity extends LairDragonEntity implements LegendaryCreature {
    /** How much bigger than ancient_dragon.geo.json it's drawn (the renderer's withScale). */
    public static final float SCALE = 2.5F;
    /** The Lightning Chaser's hit-shape groups on the thickened model, scaled like the renderer. */
    public static final DragonPartLayout ANCIENT_LAYOUT = partLayout("ancient_dragon").scaled(SCALE);

    public static final double MAX_HEALTH = 700.0;
    public static final double ARMOR = 14.0;
    public static final double ARMOR_TOUGHNESS = 8.0;
    public static final double WALK_SPEED = 0.32;
    public static final double FLY_SPEED = 0.65;
    public static final double BITE_DAMAGE = 20.0;
    public static final int BITE_COOLDOWN = 30;
    public static final double BITE_RANGE = 10.0;
    public static final double FOLLOW_RANGE = 96.0;
    public static final int XP = 500;

    public static final double FRIGHT_RADIUS = 48.0;
    public static final int FRIGHT_INTERVAL = 600;
    public static final int FRIGHT_TICKS = 200;
    public static final int FRIGHT_REPEAT_TICKS = 60;
    public static final int RECHARGE_TICKS = 60;
    public static final int RECHARGE_ON = 5;
    public static final int RECHARGE_ON_FURY = 4;
    public static final double LAIR_RADIUS = 64.0;
    public static final int[] LAIR_INTERVALS = { 400, 280, 200 };
    public static final double LEASH = 96.0;
    /** Wakes when a player comes this close; once fighting it follows them out to its follow range. */
    public static final double WAKE_RANGE = 40.0;
    public static final double BAR_RANGE = 96.0;
    /** After this long without a fight, its Legendary Resistance and the Frightful Presence immunities reset. */
    public static final int QUIET_RESET_TICKS = 1200;
    public static final float FURY_SPEED_BONUS = 0.15F;
    public static final int KOBOLD_STRENGTH_TICKS = 1200;
    public static final int DEATH_TICKS = 60;

    public static final TagKey<EntityType<?>> ANCIENT_DRAGONS = TagKey.of(RegistryKeys.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "ancient_dragons"));
    /** The Kobold Tribes' mobs (the kobolds ticket fills it); in its second phase they answer its call. */
    public static final TagKey<EntityType<?>> KOBOLDS = TagKey.of(RegistryKeys.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "faction/kobolds"));
    public static final Identifier WYRMSLAYER = new Identifier(DnDClasses.MOD_ID, "wyrmslayer");

    private static final UUID FURY_SPEED_ID = UUID.fromString("5b0f3e9a-2c71-4d8e-9a44-0d1f6c7e2a01");
    private static final UUID FURY_FLY_SPEED_ID = UUID.fromString("5b0f3e9a-2c71-4d8e-9a44-0d1f6c7e2a02");

    private final ChromaticColor color;
    private final LegendaryResistance legendaryResistance = new LegendaryResistance(3);
    private final LegendaryActions legendaryActions = new LegendaryActions();
    private final Set<UUID> frightImmune = new HashSet<>();
    private final List<LairAction.Effect> lairEffects = new ArrayList<>();
    private boolean breathCharged = true;
    private int rechargeTimer;
    private int frightTimer;
    private int lairTimer;
    @Nullable
    private LairAction lastLairAction;
    private boolean fightOn;
    private int quietTicks;
    private long lastDay = -1;
    private int wingbeatTimer;
    private boolean returningHome;
    /** Where it rests between fights: its lair's perch (set by the lair), or null. */
    @Nullable
    private BlockPos perch;

    protected AncientDragonEntity(EntityType<? extends TameableEntity> entityType, World world, ChromaticColor color) {
        super(entityType, world, ANCIENT_LAYOUT, color.barColor());
        this.color = color;
        BossFight fight = this.getBossFight()
                .range(BAR_RANGE)
                .music(ModSounds.MUSIC_ANCIENT_FIGHT)
                .xp(XP)
                .advancement(WYRMSLAYER)
                .phase(0.6F, BossBar.Color.PURPLE, this::enterMountainPhase)
                .phase(0.3F, BossBar.Color.YELLOW, this::enterMoltenFury)
                .title(this::barTitle);
        fight.getBar().setStyle(BossBar.Style.NOTCHED_10);
    }

    public static DefaultAttributeContainer.Builder createAncientAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, MAX_HEALTH)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, WALK_SPEED)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, FLY_SPEED)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, BITE_DAMAGE)
                .add(EntityAttributes.GENERIC_ARMOR, ARMOR)
                .add(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, ARMOR_TOUGHNESS)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, FOLLOW_RANGE);
    }

    public ChromaticColor getColor() {
        return this.color;
    }

    @Override
    public LegendaryResistance legendaryResistance() {
        return this.legendaryResistance;
    }

    public LegendaryActions legendaryActions() {
        return this.legendaryActions;
    }

    // ---------------------------------------------------------------- size

    @Override
    public Box getVisibilityBoundingBox() {
        double reach = this.getPartLayout().getReach();
        return this.getBoundingBox().expand(reach, reach * 0.6, reach);
    }

    @Override
    public double getBiteRange() {
        return BITE_RANGE;
    }

    @Override
    public int getMeleeCooldown() {
        return BITE_COOLDOWN;
    }

    @Override
    public boolean hasStorm() {
        return false;
    }

    @Override
    public void shoot(LivingEntity target) {
    }

    @Override
    public int getBreathSaveDc() {
        return AncientSaves.FIRE_BREATH_DC;
    }

    @Override
    protected RegistryKey<Structure> lairStructure() {
        return this.color.lairStructure();
    }

    @Override
    protected boolean respawnsAtLair() {
        return false;
    }

    // It never despawns: it's placed by its lair or summoned
    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    /** Its own kind and the kobolds that serve it. */
    public boolean isAlly(LivingEntity entity) {
        return entity == this || entity.getType().isIn(ANCIENT_DRAGONS) || entity.getType().isIn(KOBOLDS)
                || this.isTeammate(entity);
    }

    @Override
    public boolean canTarget(LivingEntity target) {
        return !this.isAlly(target) && super.canTarget(target);
    }

    // ---------------------------------------------------------------- goals

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new ReturnHomeGoal(this));
        this.goalSelector.add(3, new AncientDragonAttackGoal(this));
        this.goalSelector.add(3, new PerchGoal(this));
        this.goalSelector.add(4, new DragonFlyRandomlyGoal(this));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 16.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        // It wakes when a player comes within WAKE_RANGE and can be seen; the follow range keeps it after them
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false,
                player -> player.squaredDistanceTo(this) <= WAKE_RANGE * WAKE_RANGE));
    }

    /** Its perch, where it rests between fights (the top of its lair's pillar), or null. */
    @Nullable
    public BlockPos getPerch() {
        return this.perch;
    }

    public void setPerch(@Nullable BlockPos perch) {
        this.perch = perch;
    }

    /**
     * Someone opened a chest in its hoard hall (the lair's hoard law): it turns on {@code thief} wherever they are,
     * and its Frightful Presence goes off at once.
     */
    public void wakeForHoard(PlayerEntity thief) {
        if (!this.isAlive() || thief.isCreative() || thief.isSpectator()) {
            return;
        }
        boolean wasAfterThem = this.getTarget() == thief;
        this.returningHome = false;
        this.setTarget(thief);
        MobActions.log(this, "hoard_wake", thief);
        if (!wasAfterThem) {
            this.playSound(ModSounds.ANCIENT_ROAR, 8.0F, 0.7F);
            this.frightfulPresence();
        }
    }

    /** Drawn more than {@value #LEASH} blocks from its lair: flies home, and the fight pauses. */
    public boolean isReturningHome() {
        return this.returningHome;
    }

    // ---------------------------------------------------------------- breath

    public boolean isBreathCharged() {
        return this.breathCharged;
    }

    /** Breathes on the target if the breath is charged; true if it started. */
    public boolean breathe(LivingEntity target) {
        if (!this.breathCharged || this.getFireBreath().isBreathing()) {
            return false;
        }
        this.getFireBreath().start(target);
        if (!this.getFireBreath().isBreathing()) {
            return false;
        }
        this.breathCharged = false;
        this.rechargeTimer = 0;
        MobActions.log(this, "breath", target);
        return true;
    }

    private void rollRecharge() {
        int roll = 1 + this.random.nextInt(6);
        int needed = this.isMoltenFury() ? RECHARGE_ON_FURY : RECHARGE_ON;
        this.breathCharged = roll >= needed;
        MobActions.log(this, "breath_recharge " + roll + " (" + (this.breathCharged ? "charged" : "spent") + ")",
                null);
    }

    // ---------------------------------------------------------------- phases

    /** Phase 1: below 60% health. */
    public boolean isMountainPhase() {
        return this.getBossFight().getPhase() >= 1;
    }

    /** Phase 2: below 30% health. */
    public boolean isMoltenFury() {
        return this.getBossFight().getPhase() >= 2;
    }

    private void enterMountainPhase() {
        MobActions.log(this, "phase_1", this.getTarget());
        this.speak("phase_mountain", Formatting.DARK_PURPLE);
        this.playSound(ModSounds.ANCIENT_ROAR, 6.0F, 0.9F);
        this.callKobolds();
    }

    private void enterMoltenFury() {
        MobActions.log(this, "phase_2", this.getTarget());
        this.speak("phase_fury", Formatting.GOLD);
        this.addSpeedBonus(EntityAttributes.GENERIC_MOVEMENT_SPEED, FURY_SPEED_ID, "Ancient fury speed");
        this.addSpeedBonus(EntityAttributes.GENERIC_FLYING_SPEED, FURY_FLY_SPEED_ID, "Ancient fury flight");
        this.frightImmune.clear();
        this.frightTimer = 0;
        this.frightfulPresence();
    }

    private void addSpeedBonus(EntityAttribute attribute, UUID id, String name) {
        EntityAttributeInstance instance = this.getAttributeInstance(attribute);
        if (instance != null && instance.getModifier(id) == null) {
            instance.addPersistentModifier(new EntityAttributeModifier(id, name, FURY_SPEED_BONUS,
                    EntityAttributeModifier.Operation.MULTIPLY_BASE));
        }
    }

    /** Every kobold within the lair radius turns on its foe, emboldened (Strength I). */
    private void callKobolds() {
        LivingEntity foe = this.getTarget();
        if (foe == null) {
            return;
        }
        for (MobEntity kobold : this.world.getEntitiesByClass(MobEntity.class, this.getBoundingBox().expand(LAIR_RADIUS),
                e -> e.isAlive() && e.getType().isIn(KOBOLDS))) {
            kobold.setTarget(foe);
            kobold.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, KOBOLD_STRENGTH_TICKS, 0), this);
        }
    }

    /** "Kharzanthrax, Ancient Red Dragon ◆◆◆": its name, kind and Legendary Resistances left. */
    private Text barTitle() {
        Text kind = this.getType().getName();
        Text name = this.hasCustomName() ? Text.translatable("boss.dndclasses.ancient_bar.named",
                this.getCustomName(), kind) : kind;
        return Text.translatable("boss.dndclasses.ancient_bar", name, this.legendaryResistance.diamonds());
    }

    /** A line to every player within {@value #BAR_RANGE} blocks: entity.dndclasses.ancient_dragon.say.&lt;line&gt;. */
    public void speak(String line, Formatting color) {
        if (!(this.world instanceof ServerWorld world)) {
            return;
        }
        Text text = Text.translatable("entity.dndclasses.ancient_dragon.say", this.getDisplayName(),
                Text.translatable("entity.dndclasses.ancient_dragon.say." + line)).formatted(color);
        for (ServerPlayerEntity player : world.getPlayers(p -> p.squaredDistanceTo(this) < BAR_RANGE * BAR_RANGE)) {
            player.sendMessage(text, false);
        }
    }

    // ---------------------------------------------------------------- Frightful Presence

    /**
     * Frightful Presence: each player within {@value #FRIGHT_RADIUS} blocks who can see it and hasn't already
     * resisted it this fight makes a WIS save (DC {@value AncientSaves#FRIGHTFUL_PRESENCE_DC}) or is Frightened for
     * {@value #FRIGHT_TICKS} ticks (a repeat save every {@value #FRIGHT_REPEAT_TICKS}). A success makes them immune
     * until the fight ends or it enters its Molten Fury.
     */
    public void frightfulPresence() {
        if (!(this.world instanceof ServerWorld world)) {
            return;
        }
        MobActions.log(this, "frightful_presence", this.getTarget());
        world.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.ANCIENT_ROAR, SoundCategory.HOSTILE,
                6.0F, 1.0F);
        for (int i = 0; i < 64; i++) {
            float a = i / 64.0F * MathHelper.TAU;
            world.spawnParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getBodyY(0.5), this.getZ(), 0,
                    MathHelper.cos(a), 0.02, MathHelper.sin(a), 0.8);
        }
        for (ServerPlayerEntity player : world.getPlayers(p -> p.isAlive() && !p.isCreative() && !p.isSpectator()
                && p.squaredDistanceTo(this) <= FRIGHT_RADIUS * FRIGHT_RADIUS)) {
            if (this.frightImmune.contains(player.getUuid()) || !this.canSee(player)) {
                continue;
            }
            SaveResult save = SavingThrow.of(player, AncientSaves.FRIGHTFUL_PRESENCE)
                    .source(this)
                    .exposure(this, "frightful_presence", 20)
                    .tags("fear", "dragon")
                    .onSuccess(Text.translatable(AncientSaves.FRIGHTFUL_PRESENCE.effectKey()))
                    .onFailure(Text.translatable("save.dndclasses.effect.frightful_presence.failure"))
                    .roll();
            if (!save.fresh()) {
                continue;
            }
            if (save.succeeded()) {
                this.frightImmune.add(player.getUuid());
            } else {
                Conditions.of(player, Condition.FRIGHTENED).source(this).key("frightful_presence")
                        .ticks(FRIGHT_TICKS)
                        .saveLabel(AncientSaves.FRIGHTFUL_PRESENCE.labelKey())
                        .repeatSave(AncientSaves.FRIGHTFUL_PRESENCE.ability(), AncientSaves.FRIGHTFUL_PRESENCE_DC,
                                FRIGHT_REPEAT_TICKS)
                        .saveTags("fear", "dragon").apply();
            }
        }
    }

    // ---------------------------------------------------------------- lair actions

    /** Adds a lair effect that plays out over the next ticks. */
    public void schedule(LairAction.Effect effect) {
        this.lairEffects.add(effect);
    }

    /** Whether it's within {@value #LAIR_RADIUS} blocks of its lair. */
    public boolean isInLair() {
        BlockPos lair = this.getLair();
        return lair != null && this.getPos().isInRange(Vec3d.ofCenter(lair), LAIR_RADIUS);
    }

    private void runLairAction(ServerWorld world) {
        BlockPos lair = this.getLair();
        if (lair == null) {
            return;
        }
        Vec3d centre = Vec3d.ofCenter(lair);
        List<ServerPlayerEntity> inLair = world.getPlayers(p -> p.isAlive() && !p.isSpectator()
                && p.getPos().isInRange(centre, LAIR_RADIUS));
        List<LairAction> actions = new ArrayList<>(this.color.lairActions());
        if (actions.size() > 1 && this.lastLairAction != null) {
            actions.remove(this.lastLairAction);
        }
        LairAction action = actions.get(this.random.nextInt(actions.size()));
        this.lastLairAction = action;
        MobActions.log(this, "lair_" + action.id(), this.getTarget());
        action.run(this, world, inLair);
    }

    private void tickLairEffects(ServerWorld world) {
        Iterator<LairAction.Effect> it = this.lairEffects.iterator();
        while (it.hasNext()) {
            if (it.next().tick(world)) {
                it.remove();
            }
        }
    }

    // ---------------------------------------------------------------- ticking

    /** Beats up into the air (after a wing attack, or to leave the ground). */
    public void takeOff(double lift) {
        this.setVelocity(this.getVelocity().add(0.0, lift, 0.0));
        this.velocityDirty = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.world instanceof ServerWorld world && this.isAlive()) {
            this.serverTick(world);
        }
    }

    private void serverTick(ServerWorld world) {
        if (!this.hasCustomName()) {
            this.setCustomName(Text.literal(AncientDragonNames.roll(this.color, this.random)));
        }
        // Its Legendary Resistance comes back each dawn
        long day = world.getTimeOfDay() / 24000L;
        if (this.lastDay >= 0 && day != this.lastDay) {
            this.legendaryResistance.refill();
        }
        this.lastDay = day;

        if (!this.isOnGround() && ++this.wingbeatTimer >= 24) {
            this.wingbeatTimer = 0;
            world.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.ANCIENT_WINGBEAT,
                    SoundCategory.HOSTILE, 4.0F, 0.9F + this.random.nextFloat() * 0.1F);
        }

        BlockPos lair = this.getLair();
        if (lair != null && !this.returningHome
                && Vec3d.ofBottomCenter(lair).subtract(this.getPos()).horizontalLengthSquared() > LEASH * LEASH) {
            this.returningHome = true;
            this.setTarget(null);
            MobActions.log(this, "return_home", null);
        }

        boolean fighting = this.getBossFight().isFightingPlayer() && !this.returningHome;
        if (fighting && !this.fightOn) {
            this.startFight();
        }
        this.fightOn = fighting;
        this.tickLairEffects(world);
        if (!fighting) {
            if (++this.quietTicks == QUIET_RESET_TICKS) {
                this.legendaryResistance.refill();
                this.legendaryActions.refill();
                this.frightImmune.clear();
            }
            return;
        }
        this.quietTicks = 0;
        this.legendaryActions.tick();
        if (++this.frightTimer >= FRIGHT_INTERVAL) {
            this.frightTimer = 0;
            this.frightfulPresence();
        }
        if (!this.breathCharged && ++this.rechargeTimer >= RECHARGE_TICKS) {
            this.rechargeTimer = 0;
            this.rollRecharge();
        }
        if (this.isInLair()) {
            int interval = LAIR_INTERVALS[Math.min(this.getBossFight().getPhase(), LAIR_INTERVALS.length - 1)];
            if (++this.lairTimer >= interval) {
                this.lairTimer = 0;
                this.runLairAction(world);
            }
        }
    }

    private void startFight() {
        MobActions.log(this, "fight_start", this.getTarget());
        this.breathCharged = true;
        this.frightTimer = 0;
        this.lairTimer = 0;
        if (this.getBossFight().getPhase() == 0) {
            this.speak("start", Formatting.RED);
        }
        this.frightfulPresence();
    }

    // ---------------------------------------------------------------- spawning, death, saving

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
            @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        this.setCustomName(Text.literal(AncientDragonNames.roll(this.color, this.random)));
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }

    @Override
    public void onDeath(DamageSource source) {
        boolean wasAlive = !this.dead;
        super.onDeath(source);
        if (wasAlive && this.world instanceof ServerWorld world) {
            MobActions.log(this, "death", source.getAttacker() instanceof LivingEntity l ? l : null);
            this.speak("death", Formatting.DARK_RED);
            this.playSound(ModSounds.ANCIENT_ROAR, 8.0F, 0.6F);
            AncientDragonEvents.SLAIN.invoker().onSlain(this, world, this.getLair());
        }
    }

    /** The body crashes down over {@value #DEATH_TICKS} ticks in fire and dust. */
    @Override
    protected void updatePostDeath() {
        this.deathTime++;
        if (this.world instanceof ServerWorld world) {
            if (this.deathTime == 1 || this.deathTime == DEATH_TICKS - 5) {
                for (int i = 0; i < 72; i++) {
                    float a = i / 72.0F * MathHelper.TAU;
                    world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY() + 0.3, this.getZ(),
                            0, MathHelper.cos(a), 0.02, MathHelper.sin(a), 0.6);
                }
                world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 2.0, this.getZ(), 1,
                        0, 0, 0, 0);
            }
            if (this.deathTime % 3 == 0) {
                world.spawnParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 3.0, this.getZ(), 30, 5.0, 3.0, 5.0,
                        0.05);
                world.spawnParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 4.0, this.getZ(), 10, 5.0,
                        3.0, 5.0, 0.02);
            }
            if (this.deathTime >= DEATH_TICKS && !this.isRemoved()) {
                world.sendEntityStatus(this, (byte) 60);
                this.remove(Entity.RemovalReason.KILLED);
            }
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        this.legendaryResistance.writeNbt(nbt);
        this.legendaryActions.writeNbt(nbt);
        nbt.putBoolean("BreathCharged", this.breathCharged);
        if (this.perch != null) {
            nbt.put("Perch", NbtHelper.fromBlockPos(this.perch));
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.legendaryResistance.readNbt(nbt);
        this.legendaryActions.readNbt(nbt);
        if (nbt.contains("BreathCharged")) {
            this.breathCharged = nbt.getBoolean("BreathCharged");
        }
        this.perch = nbt.contains("Perch") ? NbtHelper.toBlockPos(nbt.getCompound("Perch")) : null;
    }

    // ---------------------------------------------------------------- going home

    /**
     * Between fights it goes back to its perch and settles there: it flies over the perch, drops onto it and stays
     * put (the wandering flight can't take over while this runs).
     */
    static final class PerchGoal extends Goal {
        private final AncientDragonEntity dragon;

        PerchGoal(AncientDragonEntity dragon) {
            this.dragon = dragon;
            this.setControls(java.util.EnumSet.of(Control.MOVE));
        }

        @Override
        public boolean canStart() {
            return this.dragon.perch != null && this.dragon.getTarget() == null && !this.dragon.returningHome;
        }

        @Override
        public boolean shouldContinue() {
            return this.canStart();
        }

        @Override
        public void start() {
            this.dragon.getNavigation().stop();
        }

        @Override
        public void tick() {
            BlockPos perch = this.dragon.perch;
            if (perch == null) {
                return;
            }
            Vec3d spot = Vec3d.ofBottomCenter(perch);
            Vec3d pos = this.dragon.getPos();
            double flat = spot.subtract(pos).horizontalLength();
            if (flat > 1.5D) {
                // Over the perch first, high enough to clear the crater's rim and spires
                double height = flat > 12.0D ? Math.max(spot.y + 8.0D, pos.y) : spot.y + 2.0D;
                this.dragon.getMoveControl().moveTo(spot.x, height, spot.z, 1.0D);
            } else if (!this.dragon.isOnGround()) {
                this.dragon.getMoveControl().moveTo(spot.x, spot.y, spot.z, 0.6D);
            }
        }

        @Override
        public boolean shouldRunEveryTick() {
            return true;
        }
    }

    /** Flies back over its lair when drawn too far away, then takes up the fight again. */
    static final class ReturnHomeGoal extends Goal {
        private final AncientDragonEntity dragon;

        ReturnHomeGoal(AncientDragonEntity dragon) {
            this.dragon = dragon;
            this.setControls(java.util.EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            return this.dragon.returningHome && this.dragon.getLair() != null;
        }

        @Override
        public void tick() {
            BlockPos lair = this.dragon.getLair();
            if (lair == null) {
                this.dragon.returningHome = false;
                return;
            }
            Vec3d home = Vec3d.ofBottomCenter(lair).add(0.0, 16.0, 0.0);
            this.dragon.setTarget(null);
            this.dragon.getMoveControl().moveTo(home.x, home.y, home.z, 1.0);
            if (home.subtract(this.dragon.getPos()).horizontalLengthSquared() < 24.0 * 24.0) {
                this.dragon.returningHome = false;
            }
        }

        @Override
        public boolean shouldRunEveryTick() {
            return true;
        }
    }
}
