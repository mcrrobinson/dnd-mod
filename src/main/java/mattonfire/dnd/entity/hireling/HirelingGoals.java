package mattonfire.dnd.entity.hireling;

import java.util.EnumSet;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Downed.Downed;
import mattonfire.dnd.classes.Downed.Stabilise;
import mattonfire.dnd.entity.HirelingEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.TrackTargetGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** The hireling's goals: knocked out, stabilising the Downed, fighting, following, waiting, and the inn. */
public final class HirelingGoals {
    /** Starts following beyond this distance (the {@code BardCompanions} numbers). */
    public static final double FOLLOW_START = 6.0D;
    public static final double TELEPORT_DISTANCE = 24.0D;
    public static final double STUCK_TELEPORT_DISTANCE = 12.0D;
    /** Goes to help a Downed owner or party member this close. */
    public static final double STABILISE_RANGE = 24.0D;
    /** The Sellsword draws its sword when a target is closer than this. */
    public static final double BOW_MIN_RANGE = 4.0D;
    public static final double BOW_MAX_RANGE = 14.0D;
    /** Draw time and pause between arrows: one every 30 ticks. */
    public static final int BOW_DRAW_TICKS = 20;
    public static final int BOW_PAUSE_TICKS = 10;

    private HirelingGoals() {
    }

    /** Lying down: holds every control so nothing else runs. */
    public static final class KnockedOutGoal extends Goal {
        private final HirelingEntity mob;

        public KnockedOutGoal(HirelingEntity mob) {
            this.mob = mob;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
        }

        @Override
        public boolean canStart() {
            return this.mob.isKnockedOut();
        }

        @Override
        public void start() {
            this.mob.getNavigation().stop();
        }

        @Override
        public boolean shouldRunEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            this.mob.getNavigation().stop();
            this.mob.setVelocity(0, this.mob.getVelocity().y, 0);
        }
    }

    /**
     * Walks to a Downed owner or party member within {@value #STABILISE_RANGE} blocks and stabilises them after
     * {@link Stabilise#HOLD_TICKS} ticks beside them (a Healer: {@link HirelingAbilities#REVIVIFY_HOLD_TICKS}). A hit interrupts it: it fights for a while, then tries again.
     */
    public static final class StabiliseGoal extends Goal {
        private final HirelingEntity mob;
        private ServerPlayerEntity patient;
        private int held;
        private int lastHurt;
        private int cooldown;

        public StabiliseGoal(HirelingEntity mob) {
            this.mob = mob;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            if (this.cooldown > 0) {
                this.cooldown--;
                return false;
            }
            if (!this.mob.isActive()) {
                return false;
            }
            this.patient = Hirelings.downedToHelp(this.mob, STABILISE_RANGE);
            return this.patient != null;
        }

        @Override
        public boolean shouldContinue() {
            return this.mob.isActive() && this.patient != null && this.patient.isAlive() && Downed.is(this.patient)
                    && !Downed.isStable(this.patient) && this.patient.world == this.mob.world
                    && this.mob.squaredDistanceTo(this.patient) <= (STABILISE_RANGE + 8) * (STABILISE_RANGE + 8);
        }

        @Override
        public void start() {
            this.held = 0;
            this.lastHurt = this.mob.getLastAttackedTime();
            this.mob.setTarget(null);
        }

        @Override
        public void stop() {
            this.patient = null;
            this.held = 0;
        }

        @Override
        public boolean shouldRunEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (this.patient == null) {
                return; // finished or interrupted this tick; shouldContinue ends the goal next
            }
            if (this.mob.getLastAttackedTime() != this.lastHurt) {
                // Hit while helping: fight back, then come back to it.
                this.lastHurt = this.mob.getLastAttackedTime();
                this.held = 0;
                this.cooldown = 40;
                this.patient = null;
                return;
            }
            this.mob.getLookControl().lookAt(this.patient, 30.0F, 30.0F);
            double distance = this.mob.squaredDistanceTo(this.patient);
            if (distance > Stabilise.RANGE * Stabilise.RANGE * 0.5D) {
                this.held = 0;
                if (this.mob.age % 10 == 0) {
                    this.mob.getNavigation().startMovingTo(this.patient, 1.25D);
                }
                return;
            }
            this.mob.getNavigation().stop();
            if (this.held == 0) {
                this.mob.swingHand(Hand.MAIN_HAND);
            }
            if (++this.held >= HirelingAbilities.stabiliseTicks(this.mob)) {
                DnDClasses.LOGGER.info("[Hireling] {} stabilises {} ({} ticks)", this.mob.firstName(),
                        this.patient.getEntityName(), this.held);
                Downed.stabilise(this.patient);
                this.patient.sendMessage(net.minecraft.text.Text.translatable("hireling.dndclasses.stabilised",
                        this.mob.firstName()).formatted(net.minecraft.util.Formatting.YELLOW), false);
                // A Healer with Cure Wounds ready stands them straight up (Revivify).
                HirelingAbilities.revivify(this.mob, this.patient);
                this.patient = null;
            }
        }
    }

    /** Melee: every role but the Sellsword, which only draws its sword when a target is close. */
    public static final class MeleeGoal extends MeleeAttackGoal {
        private final HirelingEntity hireling;

        public MeleeGoal(HirelingEntity mob) {
            super(mob, 1.2D, true);
            this.hireling = mob;
        }

        private boolean fits() {
            if (!this.hireling.isActive()) {
                return false;
            }
            if (this.hireling.role() != HirelingRole.DAMAGE) {
                return true;
            }
            LivingEntity target = this.hireling.getTarget();
            return target != null && this.hireling.squaredDistanceTo(target) < BOW_MIN_RANGE * BOW_MIN_RANGE;
        }

        @Override
        public boolean canStart() {
            return this.fits() && super.canStart();
        }

        @Override
        public boolean shouldContinue() {
            return this.fits() && super.shouldContinue();
        }

        @Override
        public void start() {
            this.hireling.holdForRange(false);
            super.start();
        }
    }

    /** The Sellsword's bow: draws for {@value #BOW_DRAW_TICKS} ticks, an arrow every 30. */
    public static final class BowGoal extends Goal {
        private final HirelingEntity mob;
        private int pause;
        private int seeTime;

        public BowGoal(HirelingEntity mob) {
            this.mob = mob;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            LivingEntity target = this.mob.getTarget();
            return this.mob.isActive() && this.mob.role() == HirelingRole.DAMAGE && target != null && target.isAlive()
                    && this.mob.squaredDistanceTo(target) >= BOW_MIN_RANGE * BOW_MIN_RANGE;
        }

        @Override
        public void start() {
            this.mob.holdForRange(true);
            this.mob.setAttacking(true);
        }

        @Override
        public void stop() {
            this.mob.setAttacking(false);
            this.seeTime = 0;
            this.mob.clearActiveItem();
        }

        @Override
        public boolean shouldRunEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.mob.getTarget();
            if (target == null) {
                return;
            }
            boolean sees = this.mob.getVisibilityCache().canSee(target);
            this.seeTime = sees ? Math.max(0, this.seeTime) + 1 : Math.min(0, this.seeTime) - 1;
            double distance = this.mob.squaredDistanceTo(target);
            if (distance > BOW_MAX_RANGE * BOW_MAX_RANGE || this.seeTime < 0) {
                if (this.mob.age % 10 == 0) {
                    this.mob.getNavigation().startMovingTo(target, 1.1D);
                }
            } else {
                this.mob.getNavigation().stop();
            }
            this.mob.getLookControl().lookAt(target, 30.0F, 30.0F);
            if (this.mob.isUsingItem()) {
                if (!sees && this.seeTime < -60) {
                    this.mob.clearActiveItem();
                } else if (sees && this.mob.getItemUseTime() >= BOW_DRAW_TICKS) {
                    this.mob.clearActiveItem();
                    this.mob.shootArrow(target, BowItem.getPullProgress(this.mob.getItemUseTime()));
                    this.pause = BOW_PAUSE_TICKS;
                }
            } else if (--this.pause <= 0 && this.seeTime >= -60) {
                this.mob.setCurrentHand(Hand.MAIN_HAND);
            }
        }
    }

    /** Walks after its owner, and teleports to them when far behind. */
    public static final class FollowOwnerGoal extends Goal {
        private final HirelingEntity mob;
        private PlayerEntity owner;
        private int repathDelay;

        public FollowOwnerGoal(HirelingEntity mob) {
            this.mob = mob;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            if (!this.mob.isActive() || this.mob.isWaiting()) {
                return false;
            }
            this.owner = this.mob.ownerHere();
            if (this.owner == null || this.owner.isSpectator()) {
                return false;
            }
            double distance = this.mob.squaredDistanceTo(this.owner);
            if (distance > TELEPORT_DISTANCE * TELEPORT_DISTANCE) {
                return true; // even mid-fight: never lose the owner
            }
            return this.mob.getTarget() == null && distance > FOLLOW_START * FOLLOW_START;
        }

        @Override
        public boolean shouldContinue() {
            if (this.owner == null || !this.owner.isAlive() || !this.mob.isActive() || this.mob.isWaiting()
                    || this.owner.world != this.mob.world) {
                return false;
            }
            double keep = this.mob.role().keepDistance;
            return (this.mob.getTarget() == null || this.mob.squaredDistanceTo(this.owner) > TELEPORT_DISTANCE * TELEPORT_DISTANCE)
                    && this.mob.squaredDistanceTo(this.owner) > keep * keep;
        }

        @Override
        public void start() {
            this.repathDelay = 0;
        }

        @Override
        public void stop() {
            this.mob.getNavigation().stop();
            this.owner = null;
        }

        @Override
        public void tick() {
            this.mob.getLookControl().lookAt(this.owner, 10.0F, this.mob.getMaxLookPitchChange());
            double distance = this.mob.squaredDistanceTo(this.owner);
            if (distance > TELEPORT_DISTANCE * TELEPORT_DISTANCE) {
                Hirelings.teleportNear(this.mob, this.owner);
            } else if (--this.repathDelay <= 0) {
                this.repathDelay = this.getTickCount(10);
                if (!this.mob.getNavigation().startMovingTo(this.owner, 1.2D)
                        && distance > STUCK_TELEPORT_DISTANCE * STUCK_TELEPORT_DISTANCE) {
                    Hirelings.teleportNear(this.mob, this.owner);
                }
            }
        }
    }

    /** In Wait mode, walks back to its spot after a fight. */
    public static final class ReturnToPostGoal extends Goal {
        private final HirelingEntity mob;

        public ReturnToPostGoal(HirelingEntity mob) {
            this.mob = mob;
            this.setControls(EnumSet.of(Control.MOVE));
        }

        @Override
        public boolean canStart() {
            BlockPos post = this.mob.post();
            return this.mob.isActive() && this.mob.isWaiting() && post != null && this.mob.getTarget() == null
                    && !this.mob.getBlockPos().isWithinDistance(post, 2.5D);
        }

        @Override
        public boolean shouldContinue() {
            return this.canStart() && !this.mob.getNavigation().isIdle();
        }

        @Override
        public void start() {
            BlockPos post = this.mob.post();
            if (post != null) {
                this.mob.getNavigation().startMovingTo(post.getX() + 0.5D, post.getY(), post.getZ() + 0.5D, 1.0D);
            }
        }
    }

    /** A candidate at the inn mills about near its spot. */
    public static final class WanderInnGoal extends WanderAroundFarGoal {
        private final HirelingEntity hireling;

        public WanderInnGoal(HirelingEntity mob) {
            super(mob, 0.5D);
            this.hireling = mob;
        }

        @Override
        public boolean canStart() {
            return this.hireling.isForHire() && super.canStart();
        }

        @Override
        protected Vec3d getWanderTarget() {
            Vec3d target = super.getWanderTarget();
            // Keep inside the home range (the inn).
            if (target != null && this.hireling.hasPositionTarget()
                    && !this.hireling.isInWalkTargetRange(BlockPos.ofFloored(target))) {
                return null;
            }
            return target;
        }
    }

    /** Fights back, but never its friends. */
    public static final class RevengeTargetGoal extends RevengeGoal {
        private final HirelingEntity hireling;

        public RevengeTargetGoal(HirelingEntity mob) {
            super(mob);
            this.hireling = mob;
        }

        @Override
        public boolean canStart() {
            return this.hireling.isActive() && super.canStart();
        }
    }

    /** Targets what last hurt its owner, or what its owner last hit. */
    public static final class DefendOwnerGoal extends TrackTargetGoal {
        private final HirelingEntity hireling;
        private int lastAttackedTime;
        private int lastAttackTime;
        private LivingEntity found;

        public DefendOwnerGoal(HirelingEntity mob) {
            super(mob, false);
            this.hireling = mob;
            this.setControls(EnumSet.of(Control.TARGET));
        }

        @Override
        public boolean canStart() {
            if (!this.hireling.isActive()) {
                return false;
            }
            PlayerEntity owner = this.hireling.ownerHere();
            if (owner == null) {
                return false;
            }
            LivingEntity attacker = owner.getAttacker();
            if (attacker != null && owner.getLastAttackedTime() != this.lastAttackedTime
                    && this.canTrack(attacker, TargetPredicate.DEFAULT) && this.hireling.canTarget(attacker)) {
                this.lastAttackedTime = owner.getLastAttackedTime();
                this.found = attacker;
                return true;
            }
            LivingEntity attacking = owner.getAttacking();
            if (attacking != null && owner.getLastAttackTime() != this.lastAttackTime
                    && this.canTrack(attacking, TargetPredicate.DEFAULT) && this.hireling.canTarget(attacking)) {
                this.lastAttackTime = owner.getLastAttackTime();
                this.found = attacking;
                return true;
            }
            return false;
        }

        @Override
        public void start() {
            this.mob.setTarget(this.found);
            super.start();
        }
    }

    /** Engages a monster within {@link HirelingEntity#GUARD_RANGE} that targets its owner, a party member or a hireling. */
    public static final class GuardAlliesGoal extends ActiveTargetGoal<MobEntity> {
        private final HirelingEntity hireling;

        public GuardAlliesGoal(HirelingEntity mob) {
            super(mob, MobEntity.class, 5, true, false, null);
            this.hireling = mob;
            this.targetPredicate = TargetPredicate.createAttackable().setBaseMaxDistance(HirelingEntity.GUARD_RANGE)
                    .setPredicate(entity -> entity instanceof Monster && entity instanceof MobEntity monster
                            && monster.getTarget() != null
                            && Hirelings.isFriend(mob.getServer(), mob.ownerUuid(), monster.getTarget()));
        }

        @Override
        public boolean canStart() {
            return this.hireling.isActive() && this.hireling.ownerUuid() != null && super.canStart();
        }
    }

    /** In Wait mode: anything hostile within {@link HirelingEntity#WAIT_GUARD_RANGE}. */
    public static final class WaitGuardGoal extends ActiveTargetGoal<MobEntity> {
        private final HirelingEntity hireling;

        public WaitGuardGoal(HirelingEntity mob) {
            super(mob, MobEntity.class, 5, true, false, null);
            this.hireling = mob;
            this.targetPredicate = TargetPredicate.createAttackable().setBaseMaxDistance(HirelingEntity.WAIT_GUARD_RANGE)
                    .setPredicate(entity -> entity instanceof Monster);
        }

        @Override
        public boolean canStart() {
            return this.hireling.isActive() && this.hireling.isWaiting() && super.canStart();
        }
    }
}
