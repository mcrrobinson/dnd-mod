package mattonfire.dnd.entity;

import java.util.EnumSet;
import net.minecraft.entity.CrossbowUser;
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
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * An Orc Archer: 22 health, 2 armour, a crossbow. Keeps watch from the gate towers and shoots whatever the
 * horde fights within {@link #RANGE} blocks: a pillager's load-hold-shoot rhythm, about one bolt every 2-3 s.
 */
public class OrcArcherEntity extends OrcEntity implements CrossbowUser {
    public static final float RANGE = 16.0F;

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

    /** Load, hold a moment, shoot; walk closer when the target is out of range or out of sight. */
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
            boolean sees = this.archer.getVisibilityCache().canSee(target);
            double distance = this.archer.squaredDistanceTo(target);
            boolean inRange = sees && distance <= RANGE * RANGE;
            // Archers posted on a tower hold their post.
            if (inRange || this.archer.getHomeRange() <= 4) {
                this.archer.getNavigation().stop();
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
                    } else if (inRange) {
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
