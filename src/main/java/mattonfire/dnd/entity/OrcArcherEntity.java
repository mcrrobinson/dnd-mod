package mattonfire.dnd.entity;

import java.util.EnumSet;
import net.minecraft.entity.CrossbowUser;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * An Orc Archer: 22 health, 2 armour, a crossbow. Keeps watch from the gate towers and shoots whatever the
 * horde fights within {@link #RANGE} blocks: a pillager's load-hold-shoot rhythm, about one bolt every 2-3 s.
 *
 * <p>An archer posted on a tower ({@link #isPosted}) can't see the ground near the tower from the middle of
 * its platform, so it counts a target as seen when some spot of the platform has a clear line to it
 * ({@link #firingSpot}), and steps to that spot (a gap in the rail) to shoot.
 */
public class OrcArcherEntity extends OrcEntity implements CrossbowUser {
    public static final float RANGE = 16.0F;
    /** An archer kept this close to home holds a post (a gate tower) instead of chasing. */
    public static final int POST_RANGE = 4;
    /** How far from its post a posted archer looks for a spot to shoot from. */
    private static final int FIRING_SPOT_RADIUS = 3;

    @Nullable
    private BlockPos firingSpot;
    @Nullable
    private Entity firingSpotTarget;
    private int firingSpotAge = -1;

    public OrcArcherEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createOrcArcherAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 22.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 2.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.27D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0D);
    }

    /** Whether this archer holds a post (its home range is at most {@link #POST_RANGE}). */
    public boolean isPosted() {
        return this.getHome() != null && this.getHomeRange() <= POST_RANGE;
    }

    /** Whether the archer sees the entity from where it stands now. */
    boolean seesFromHere(Entity entity) {
        return super.canSee(entity);
    }

    /** A posted archer also sees what it could shoot from another spot of its post (see {@link #firingSpot}). */
    @Override
    public boolean canSee(Entity entity) {
        return super.canSee(entity) || this.firingSpot(entity) != null;
    }

    /**
     * For a posted archer: the nearest spot within {@link #FIRING_SPOT_RADIUS} blocks of its post, on the
     * post's level, where it could stand and see {@code target} within {@link #RANGE} blocks. Null if none,
     * or if the archer isn't posted. Worked out at most once a tick per target.
     */
    @Nullable
    BlockPos firingSpot(Entity target) {
        if (!this.isPosted() || target.world != this.world) {
            return null;
        }
        if (target != this.firingSpotTarget || this.age != this.firingSpotAge) {
            this.firingSpotTarget = target;
            this.firingSpotAge = this.age;
            this.firingSpot = this.findFiringSpot(target);
        }
        return this.firingSpot;
    }

    @Nullable
    private BlockPos findFiringSpot(Entity target) {
        BlockPos home = this.getHome();
        Vec3d eyes = target.getEyePos();
        double reach = RANGE + FIRING_SPOT_RADIUS + 1;
        if (home == null || Vec3d.ofBottomCenter(home).squaredDistanceTo(eyes) > reach * reach) {
            return null;
        }
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        BlockPos.Mutable spot = new BlockPos.Mutable();
        for (int dx = -FIRING_SPOT_RADIUS; dx <= FIRING_SPOT_RADIUS; dx++) {
            for (int dz = -FIRING_SPOT_RADIUS; dz <= FIRING_SPOT_RADIUS; dz++) {
                spot.set(home.getX() + dx, home.getY(), home.getZ() + dz);
                double distance = this.squaredDistanceTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D);
                if (distance >= bestDistance || !this.canStandAt(spot)) {
                    continue;
                }
                Vec3d from = new Vec3d(spot.getX() + 0.5D, spot.getY() + this.getStandingEyeHeight(), spot.getZ() + 0.5D);
                if (from.squaredDistanceTo(eyes) > RANGE * RANGE) {
                    continue;
                }
                HitResult hit = this.world.raycast(new RaycastContext(from, eyes, RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.NONE, this));
                if (hit.getType() == HitResult.Type.MISS) {
                    best = spot.toImmutable();
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    private boolean canStandAt(BlockPos pos) {
        BlockPos below = pos.down();
        BlockPos head = pos.up();
        return this.world.getBlockState(below).isSideSolidFullSquare(this.world, below, Direction.UP)
                && this.world.getBlockState(pos).getCollisionShape(this.world, pos).isEmpty()
                && this.world.getBlockState(head).getCollisionShape(this.world, head).isEmpty();
    }

    /** Walks to the middle of {@code spot}: by path while it's further than a block, then straight. */
    void stepTo(BlockPos spot) {
        double x = spot.getX() + 0.5D;
        double z = spot.getZ() + 0.5D;
        double dx = x - this.getX();
        double dz = z - this.getZ();
        if (dx * dx + dz * dz > 1.0D) {
            this.getNavigation().startMovingTo(x, spot.getY(), z, 1.0D);
        } else {
            this.getNavigation().stop();
            this.getMoveControl().moveTo(x, spot.getY(), z, 0.6D);
        }
    }

    @Override
    protected void initCombatGoals() {
        this.goalSelector.add(2, new CrossbowGoal(this));
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.08F);
        return data;
    }

    /** Crossbows load arrows from nowhere: orcs carry plenty. */
    @Override
    public ItemStack getProjectileType(ItemStack stack) {
        return stack.getItem() instanceof CrossbowItem ? new ItemStack(Items.ARROW) : super.getProjectileType(stack);
    }

    @Override
    public void attack(LivingEntity target, float pullProgress) {
        this.shoot(this, 1.6F);
    }

    @Override
    public void setCharging(boolean charging) {
    }

    @Override
    public void shoot(LivingEntity target, ItemStack crossbow, ProjectileEntity projectile, float multiShotSpray) {
        this.shoot(this, target, projectile, multiShotSpray, 1.6F);
    }

    @Override
    public void postShoot() {
        this.despawnCounter = 0;
    }

    /**
     * Load, hold a moment, shoot; walk closer when the target is out of range or out of sight. A posted archer
     * doesn't leave its post: it steps to a spot on it that sees the target.
     */
    static final class CrossbowGoal extends Goal {
        private enum Stage { UNCHARGED, CHARGING, CHARGED, READY }

        private final OrcArcherEntity archer;
        private Stage stage = Stage.UNCHARGED;
        private int holdTicks;

        CrossbowGoal(OrcArcherEntity archer) {
            this.archer = archer;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            LivingEntity target = this.archer.getTarget();
            return target != null && target.isAlive() && this.archer.isHolding(Items.CROSSBOW);
        }

        @Override
        public boolean shouldContinue() {
            return this.canStart();
        }

        @Override
        public boolean shouldRunEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            if (this.archer.isUsingItem()) {
                this.archer.clearActiveItem();
            }
            this.stage = Stage.UNCHARGED;
        }

        @Override
        public void tick() {
            LivingEntity target = this.archer.getTarget();
            if (target == null) {
                return;
            }
            boolean sees = this.archer.seesFromHere(target);
            double distance = this.archer.squaredDistanceTo(target);
            boolean inRange = sees && distance <= RANGE * RANGE;
            // Archers posted on a tower hold their post, stepping to the edge to see down.
            BlockPos spot = null;
            if (inRange) {
                this.archer.getNavigation().stop();
            } else if (this.archer.isPosted()) {
                spot = this.archer.firingSpot(target);
                if (spot != null) {
                    this.archer.stepTo(spot);
                } else {
                    this.archer.getNavigation().stop();
                }
            } else {
                this.archer.getNavigation().startMovingTo(target, 1.0D);
            }
            this.archer.getLookControl().lookAt(target, 30.0F, 30.0F);
            ItemStack crossbow = this.archer.getMainHandStack();
            switch (this.stage) {
                case UNCHARGED -> {
                    if (CrossbowItem.isCharged(crossbow)) {
                        this.stage = Stage.CHARGED;
                        this.holdTicks = 10;
                    } else if (inRange || spot != null) {
                        this.archer.setCurrentHand(Hand.MAIN_HAND);
                        this.stage = Stage.CHARGING;
                    }
                }
                case CHARGING -> {
                    if (!this.archer.isUsingItem()) {
                        this.stage = Stage.UNCHARGED;
                    } else if (this.archer.getItemUseTime() >= CrossbowItem.getPullTime(crossbow)) {
                        this.archer.stopUsingItem();
                        this.stage = Stage.CHARGED;
                        this.holdTicks = 15 + this.archer.getRandom().nextInt(20);
                    }
                }
                case CHARGED -> {
                    if (--this.holdTicks <= 0) {
                        this.stage = Stage.READY;
                    }
                }
                case READY -> {
                    if (inRange) {
                        this.archer.attack(target, 1.0F);
                        CrossbowItem.setCharged(crossbow, false);
                        this.stage = Stage.UNCHARGED;
                    }
                }
            }
        }
    }
}
