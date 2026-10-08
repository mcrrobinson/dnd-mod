package mattonfire.dnd.classes.Misc;

import java.util.EnumSet;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.Party.PartyManager;
import mattonfire.dnd.classes.mixin.FoxEntityInvoker;
import mattonfire.dnd.classes.mixin.MobEntityAccessor;
import mattonfire.dnd.entity.boss.Boss;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.TrackTargetGoal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HoglinEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * The Bard's Animal Friends companions. Wolves, cats and parrots are tamed the
 * vanilla way; everything else (foxes, goats, bees, polar bears, golems,
 * hoglins...) can't be, so every companion gets the same goals instead: follow
 * the Bard, fight hostile mobs and whatever attacks the Bard or the Bard
 * attacks, and never target or hurt the Bard, their party or their other
 * companions.
 *
 * The owner is kept in a command tag ({@link #TAG_PREFIX} + uuid), which is
 * saved with the mob, and the goals are added again whenever it loads.
 */
public final class BardCompanions {
    public static final String TAG_PREFIX = "dnd_bard_companion:";

    /** Starts following beyond this distance, and teleports to the Bard beyond {@link #TELEPORT_DISTANCE}. */
    private static final double FOLLOW_DISTANCE = 6;
    private static final double TELEPORT_DISTANCE = 24;

    private BardCompanions() {
    }

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof PathAwareEntity mob && ownerOf(mob) != null) {
                addGoals(mob);
            }
        });
        // Companions can't hurt their Bard, the Bard's party or each other (goat rams, stray spit).
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            Entity attacker = source.getAttacker();
            UUID owner = attacker == null ? null : ownerOf(attacker);
            return owner == null || !isFriendOf(owner, entity, attacker.getWorld());
        });
    }

    /** The Bard who owns this companion, or null if it isn't one. */
    @Nullable
    public static UUID ownerOf(Entity entity) {
        for (String tag : entity.getCommandTags()) {
            if (tag.startsWith(TAG_PREFIX)) {
                try {
                    return UUID.fromString(tag.substring(TAG_PREFIX.length()));
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        }
        return null;
    }

    public static boolean isCompanion(Entity entity) {
        return ownerOf(entity) != null;
    }

    public static boolean isCompanionOf(Entity entity, PlayerEntity player) {
        return player.getUuid().equals(ownerOf(entity));
    }

    /** Companions the player has in their world right now (loaded ones only). */
    public static int count(ServerWorld world, PlayerEntity player) {
        int count = 0;
        for (Entity entity : world.iterateEntities()) {
            if (entity.isAlive() && isCompanionOf(entity, player)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Whether the entity is on the Bard's side: the Bard, a party member, one of
     * the Bard's companions or pets.
     */
    public static boolean isFriendOf(UUID owner, @Nullable Entity entity, net.minecraft.world.World world) {
        if (entity == null)
            return false;
        if (entity instanceof ProjectileEntity projectile) {
            entity = projectile.getOwner();
            if (entity == null)
                return false;
        }
        if (owner.equals(entity.getUuid()) || owner.equals(ownerOf(entity))
                || entity instanceof Tameable tameable && owner.equals(tameable.getOwnerUuid())) {
            return true;
        }
        PlayerEntity bard = world.getPlayerByUuid(owner);
        return bard != null && PartyManager.areInSameParty(bard, entity);
    }

    /** Whether a companion may target this entity; called from {@code MobEntityMixin.setTarget}. */
    public static boolean blocksTarget(MobEntity mob, @Nullable LivingEntity target) {
        if (target == null)
            return false;
        UUID owner = ownerOf(mob);
        return owner != null && isFriendOf(owner, target, mob.getWorld());
    }

    /** Makes the mob the player's companion. The caller checks the type and the cap. */
    public static void adopt(PathAwareEntity mob, PlayerEntity player) {
        if (mob instanceof TameableEntity tameable && !tameable.isTamed()) {
            tameable.setOwner(player);
        }
        if (mob instanceof FoxEntity fox) {
            ((FoxEntityInvoker) fox).invokeAddTrustedUuid(player.getUuid());
        }
        if (mob instanceof IronGolemEntity golem) {
            golem.setPlayerCreated(true);
        }
        if (mob instanceof HoglinEntity hoglin) {
            // Hoglins turn into zoglins outside the Nether.
            hoglin.setImmuneToZombification(true);
        }
        mob.addCommandTag(TAG_PREFIX + player.getUuidAsString());
        mob.setPersistent();
        mob.setTarget(null);
        if (mob.getBrain().hasMemoryModule(MemoryModuleType.ATTACK_TARGET)) {
            mob.getBrain().forget(MemoryModuleType.ATTACK_TARGET);
        }
        addGoals(mob);
    }

    private static void addGoals(PathAwareEntity mob) {
        GoalSelector goals = ((MobEntityAccessor) mob).getGoalSelector();
        GoalSelector targets = ((MobEntityAccessor) mob).getTargetSelector();
        if (targets.getGoals().stream().anyMatch(goal -> goal.getGoal() instanceof RallyGoal))
            return;

        // A bee's own attack stings once and kills it; companions bite instead.
        if (mob instanceof BeeEntity) {
            goals.clear(goal -> goal instanceof MeleeAttackGoal);
        }
        boolean attacks = goals.getGoals().stream().anyMatch(goal -> goal.getGoal() instanceof MeleeAttackGoal
                || goal.getGoal() instanceof net.minecraft.entity.ai.goal.AttackGoal
                || goal.getGoal() instanceof net.minecraft.entity.ai.goal.ProjectileAttackGoal);
        if (!attacks && mob.getAttributes().hasAttribute(EntityAttributes.GENERIC_ATTACK_DAMAGE)) {
            goals.add(1, new CompanionAttackGoal(mob));
        }
        goals.add(0, new GuardGoal(mob));
        if (!(mob instanceof TameableEntity)) {
            // Tamed wolves, cats and parrots already follow (and sit) the vanilla way.
            goals.add(2, new FollowOwnerGoal(mob));
        }
        targets.add(1, new DefendOwnerGoal(mob));
        targets.add(2, new RallyGoal(mob));
    }

    @Nullable
    private static PlayerEntity owner(MobEntity mob) {
        UUID owner = ownerOf(mob);
        return owner == null ? null : mob.getWorld().getPlayerByUuid(owner);
    }

    /** Targets hostile mobs, except creepers, bosses and other Bards' companions (hoglins are Monsters). */
    private static class RallyGoal extends ActiveTargetGoal<LivingEntity> {
        RallyGoal(MobEntity mob) {
            super(mob, LivingEntity.class, 10, true, false,
                    e -> e instanceof Monster && !(e instanceof CreeperEntity) && !(e instanceof Boss)
                            && !isCompanion(e));
        }
    }

    /** Targets whatever last hurt the Bard, or whatever the Bard last hit. */
    private static class DefendOwnerGoal extends TrackTargetGoal {
        private int lastAttackedTime;
        private int lastAttackTime;
        private LivingEntity found;

        DefendOwnerGoal(MobEntity mob) {
            super(mob, false);
            setControls(EnumSet.of(Control.TARGET));
        }

        @Override
        public boolean canStart() {
            PlayerEntity owner = owner(mob);
            if (owner == null)
                return false;
            LivingEntity attacker = owner.getAttacker();
            if (attacker != null && owner.getLastAttackedTime() != lastAttackedTime
                    && canTrack(attacker, TargetPredicate.DEFAULT) && !blocksTarget(mob, attacker)) {
                lastAttackedTime = owner.getLastAttackedTime();
                found = attacker;
                return true;
            }
            LivingEntity attacking = owner.getAttacking();
            if (attacking != null && owner.getLastAttackTime() != lastAttackTime
                    && canTrack(attacking, TargetPredicate.DEFAULT) && !blocksTarget(mob, attacking)) {
                lastAttackTime = owner.getLastAttackTime();
                found = attacking;
                return true;
            }
            return false;
        }

        @Override
        public void start() {
            mob.setTarget(found);
            super.start();
        }
    }

    /** Walks after the Bard, and teleports to them when far behind. */
    private static class FollowOwnerGoal extends Goal {
        private final PathAwareEntity mob;
        private PlayerEntity owner;

        FollowOwnerGoal(PathAwareEntity mob) {
            this.mob = mob;
            setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            owner = owner(mob);
            return owner != null && !owner.isSpectator() && mob.getTarget() == null
                    && mob.squaredDistanceTo(owner) > FOLLOW_DISTANCE * FOLLOW_DISTANCE;
        }

        @Override
        public boolean shouldContinue() {
            return owner != null && owner.isAlive() && mob.getTarget() == null
                    && mob.squaredDistanceTo(owner) > 3 * 3;
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
            owner = null;
        }

        @Override
        public void tick() {
            mob.getLookControl().lookAt(owner, 10.0F, mob.getMaxLookPitchChange());
            if (mob.squaredDistanceTo(owner) > TELEPORT_DISTANCE * TELEPORT_DISTANCE) {
                BlockPos pos = owner.getBlockPos();
                mob.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, mob.getYaw(),
                        mob.getPitch());
                mob.getNavigation().stop();
            } else if (mob.age % 10 == 0) {
                if (mob.getBrain().hasMemoryModule(MemoryModuleType.WALK_TARGET)) {
                    mob.getBrain().forget(MemoryModuleType.WALK_TARGET);
                }
                mob.getNavigation().startMovingTo(owner, 1.2);
            }
        }
    }

    /** Melee for companions with no attack of their own (goats, hoglins, bees). */
    private static class CompanionAttackGoal extends MeleeAttackGoal {
        CompanionAttackGoal(PathAwareEntity mob) {
            super(mob, 1.25, true);
        }

        @Override
        protected void attack(LivingEntity target, double squaredDistance) {
            if (squaredDistance <= getSquaredMaxAttackDistance(target) && isCooledDown()) {
                resetCooldown();
                mob.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                if (mob instanceof BeeEntity) {
                    target.damage(mob.getDamageSources().mobAttack(mob),
                            (float) mob.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
                } else {
                    mob.tryAttack(target);
                }
            }
        }
    }

    /**
     * Brain-driven mobs (hoglins, goats) pick targets without {@code setTarget},
     * so their brain's target is checked here every tick as well.
     */
    private static class GuardGoal extends Goal {
        private final MobEntity mob;

        GuardGoal(MobEntity mob) {
            this.mob = mob;
        }

        @Override
        public boolean canStart() {
            return true;
        }

        @Override
        public boolean shouldRunEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (blocksTarget(mob, mob.getTarget())) {
                mob.setTarget(null);
            }
            Brain<?> brain = mob.getBrain();
            if (brain.hasMemoryModule(MemoryModuleType.ATTACK_TARGET)) {
                brain.getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET)
                        .filter(target -> blocksTarget(mob, target))
                        .ifPresent(target -> brain.forget(MemoryModuleType.ATTACK_TARGET));
            }
        }
    }
}
