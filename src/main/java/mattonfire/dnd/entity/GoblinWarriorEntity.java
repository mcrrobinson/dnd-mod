package mattonfire.dnd.entity;

import mattonfire.dnd.faction.TierEffects;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
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

/** A tough Nether Fortress goblin: lots of health, heavy hits, but a slow two-second swing. */
public class GoblinWarriorEntity extends HostileEntity implements GeoEntity, mattonfire.dnd.faction.Provokable {
    /** How long a camp grudge lasts after the goblin last had the thief as its target (30 s). */
    public static final int CAMP_GRUDGE_TICKS = 20 * 30;
    @Nullable
    private java.util.UUID grudge;
    private int grudgeUntil;

    /** How far a camp goblin roams from its camp's fire when it isn't chasing anything. */
    public static final int CAMP_ROAM = 10;
    /** Old-camp fallback: a garrison goblin with no saved camp leashes to a camp whose fire is this close. */
    private static final int CAMP_SEARCH = 64;
    /** The fire of the camp this goblin garrisons (null for raiders, Warlord waves, spawn eggs, ...). Saved. */
    @Nullable
    private BlockPos campCenter;
    private boolean campLookedUp;

    /** Ticks between swings; vanilla melee mobs use 20. */
    private static final int ATTACK_INTERVAL = 40;
    /** Ticks from raising the club to the hit landing; matches the "attack" animation. */
    private static final int WIND_UP = 10;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GoblinWarriorEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 10;
    }

    public static DefaultAttributeContainer.Builder createGoblinWarriorAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 50.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 6.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.6D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.24D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 9.0D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 1.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        // Frightened (the Warlord's Warhorn): ConditionFleeGoal, added to every path mob when it loads
        this.goalSelector.add(2, new SlowMeleeAttackGoal(this, 1.0D));
        // Walks back into range of its position target (a camp goblin's fire, a Warlord's guard spot) after a fight.
        this.goalSelector.add(4, new GoToWalkTargetGoal(this, 1.0D));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.8D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this, ZombifiedPiglinEntity.class));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false,
                player -> TierEffects.goblinMayTarget(this, (PlayerEntity) player, false)));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, IronGolemEntity.class, true));
    }

    /**
     * Goblin standing decides who's fair game: players at goblin Neutral or better are left alone unless
     * they start something, and at Unfriendly only once they come within parley range. This covers every
     * way a goblin picks a target (its goals, raids, the Warlord's waves). Orcs are allies and never targeted.
     */
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        // Allied with the Bloodtusk Horde: goblins never go for orcs.
        if (target instanceof OrcEntity) {
            return;
        }
        if (target instanceof PlayerEntity player && !this.world.isClient
                && target != this.getTarget() && !TierEffects.goblinMayTarget(this, player, false)) {
            return;
        }
        super.setTarget(target);
    }

    /**
     * This goblin saw (or heard) {@code player} open the camp chief's chest: it goes for them whatever their
     * standing, for {@link #CAMP_GRUDGE_TICKS} after it last had them as its target.
     */
    public void provokeByTheft(PlayerEntity player) {
        this.grudge = player.getUuid();
        this.grudgeUntil = this.age + CAMP_GRUDGE_TICKS;
        this.getLookControl().lookAt(player);
        super.setTarget(player);
    }

    @Override
    public boolean provokedBy(PlayerEntity player) {
        return this.grudge != null && this.grudge.equals(player.getUuid()) && this.age < this.grudgeUntil;
    }

    /** Joins a fight someone else started with the horde (the Warlord calling for help): no standing check. */
    void rallyAgainst(LivingEntity attacker) {
        super.setTarget(attacker);
    }

    /**
     * Garrisons this goblin at a camp: it stays within {@link #CAMP_ROAM} blocks of {@code center} (the fire)
     * unless it's chasing something. Saved as {@code CampCenter}, since a mob's position target isn't.
     */
    public void setCampCenter(BlockPos center) {
        this.campCenter = center.toImmutable();
        this.setPositionTarget(this.campCenter, CAMP_ROAM);
    }

    @Nullable
    public BlockPos getCampCenter() {
        return this.campCenter;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.campCenter != null) {
            nbt.put("CampCenter", NbtHelper.fromBlockPos(this.campCenter));
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("CampCenter")) {
            this.setCampCenter(NbtHelper.toBlockPos(nbt.getCompound("CampCenter")));
        }
    }

    /**
     * Camps generated before {@code CampCenter} was saved: their goblins lost the leash on the first reload.
     * A persistent goblin with no leash, that isn't a raider, a Warlord or a boss's minion, joins the camp whose
     * fire is within {@link #CAMP_SEARCH} blocks, if there is one. Checked once per load.
     */
    private void findOldCamp() {
        this.campLookedUp = true;
        if (this.campCenter != null || this.hasPositionTarget() || !this.isPersistent()
                || this instanceof GoblinWarlordEntity
                || this.getCommandTags().contains(mattonfire.dnd.entity.raid.GoblinRaid.RAIDER_TAG)
                || mattonfire.dnd.entity.boss.BossMinions.isMinion(this)
                || !(this.world instanceof ServerWorld serverWorld)) {
            return;
        }
        BlockPos center = GoblinCampLaw.campCenterNear(serverWorld, this.getBlockPos(), CAMP_SEARCH);
        if (center != null) {
            this.setCampCenter(center);
        }
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (!this.campLookedUp) {
            this.findOldCamp();
        }
        // A camp grudge lasts while the fight does, and fades a while after the thief gets away.
        if (this.grudge != null && this.getTarget() instanceof PlayerEntity target && this.provokedBy(target)) {
            this.grudgeUntil = this.age + CAMP_GRUDGE_TICKS;
        }
        // Stand down if the player's standing changed mid-fight (e.g. a quest or /rep made peace).
        if (this.age % 20 == 0 && this.getTarget() instanceof PlayerEntity player
                && !TierEffects.goblinMayTarget(this, player, true)) {
            super.setTarget(null);
        }
    }

    @Override
    public boolean damage(net.minecraft.entity.damage.DamageSource source, float amount) {
        // No friendly fire from the horde's crossbows and axes.
        if (source.getAttacker() instanceof OrcEntity) {
            return false;
        }
        return super.damage(source, amount);
    }

    @Override
    public boolean isFireImmune() {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_PIGLIN_BRUTE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.entity.damage.DamageSource source) {
        return SoundEvents.ENTITY_PIGLIN_BRUTE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_PIGLIN_BRUTE_DEATH;
    }

    @Override
    public float getSoundPitch() {
        // Higher, squeakier than a brute.
        return super.getSoundPitch() * 1.4F;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 4, this::movementPredicate));
        controllers.add(this.createActionController());
    }

    /** One-shot animations started with {@code triggerAnim("attack_controller", name)}. */
    protected AnimationController<GoblinWarriorEntity> createActionController() {
        return new AnimationController<GoblinWarriorEntity>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK);
    }

    private PlayState movementPredicate(AnimationState<GoblinWarriorEntity> state) {
        return state.setAndContinue(state.isMoving() ? WALK : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    /**
     * MeleeAttackGoal with a slow, telegraphed swing: the club is raised first and the hit lands
     * {@link #WIND_UP} ticks later, only if the target is still in reach.
     */
    private static class SlowMeleeAttackGoal extends MeleeAttackGoal {
        private final GoblinWarriorEntity goblin;
        private long nextAttackTime;
        private long hitTime = -1;

        SlowMeleeAttackGoal(GoblinWarriorEntity goblin, double speed) {
            super(goblin, speed, false);
            this.goblin = goblin;
        }

        @Override
        public void stop() {
            super.stop();
            this.hitTime = -1;
        }

        @Override
        protected void attack(LivingEntity target, double squaredDistance) {
            long now = this.goblin.world.getTime();
            boolean inReach = squaredDistance <= this.getSquaredMaxAttackDistance(target);
            if (this.hitTime >= 0) {
                if (now >= this.hitTime) {
                    this.hitTime = -1;
                    if (inReach) {
                        this.goblin.swingHand(Hand.MAIN_HAND);
                        this.goblin.tryAttack(target);
                    }
                }
            } else if (inReach && now >= this.nextAttackTime) {
                this.nextAttackTime = now + ATTACK_INTERVAL;
                this.hitTime = now + WIND_UP;
                this.goblin.triggerAnim("attack_controller", "attack");
            }
        }
    }
}
