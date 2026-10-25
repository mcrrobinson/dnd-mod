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
import net.minecraft.entity.projectile.PersistentProjectileEntity;
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
 * A Duergar Arbalester: 24 health, 5 armour, a heavy crossbow whose bolts hit for {@link #BOLT_DAMAGE} at up to
 * {@link #RANGE} blocks (a pillager's load-hold-shoot rhythm), and a hand axe in the off hand for 4 damage on
 * anything within {@link #AXE_RANGE} blocks. Enlarges and vanishes like every duergar ({@link DuergarEntity}).
 */
public class DuergarArbalesterEntity extends DuergarEntity implements CrossbowUser {
    public static final float RANGE = 20.0F;
    public static final double AXE_RANGE = 2.5D;
    public static final float BOLT_DAMAGE = 7.0F;
    public static final float BOLT_SPEED = 1.6F;
    public static final int AXE_COOLDOWN = 20;

    public DuergarArbalesterEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createArbalesterAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 24.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 5.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0D)
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
        this.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.IRON_AXE));
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.08F);
        this.setEquipmentDropChance(EquipmentSlot.OFFHAND, 0.05F);
        return data;
    }

    /** Crossbows load bolts from nowhere: the clans keep their arbalesters supplied. */
    @Override
    public ItemStack getProjectileType(ItemStack stack) {
        return stack.getItem() instanceof CrossbowItem ? new ItemStack(Items.ARROW) : super.getProjectileType(stack);
    }

    @Override
    public void attack(LivingEntity target, float pullProgress) {
        this.shoot(this, BOLT_SPEED);
    }

    @Override
    public void setCharging(boolean charging) {
    }

    /** A heavy bolt: {@link #BOLT_DAMAGE} at the crossbow's speed (an arrow's damage scales with its speed). */
    @Override
    public void shoot(LivingEntity target, ItemStack crossbow, ProjectileEntity projectile, float multiShotSpray) {
        if (projectile instanceof PersistentProjectileEntity bolt) {
            bolt.setDamage(BOLT_DAMAGE / (BOLT_SPEED * 1.0D));
            bolt.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
        }
        this.shoot(this, target, projectile, multiShotSpray, BOLT_SPEED);
    }

    @Override
    public void postShoot() {
        this.despawnCounter = 0;
    }

    /** Load, hold a moment, shoot; walk closer when out of range or sight; chop with the axe when it's close. */
    static final class CrossbowGoal extends Goal {
        private enum Stage { UNCHARGED, CHARGING, CHARGED, READY }

        private final DuergarArbalesterEntity archer;
        private Stage stage = Stage.UNCHARGED;
        private int holdTicks;
        private int axeReadyAge;

        CrossbowGoal(DuergarArbalesterEntity archer) {
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
            if (inRange) {
                this.archer.getNavigation().stop();
            } else {
                this.archer.getNavigation().startMovingTo(target, 1.0D);
            }
            this.archer.getLookControl().lookAt(target, 30.0F, 30.0F);
            double axeReach = AXE_RANGE + (this.archer.isEnlarged() ? ENLARGE_REACH : 0.0D);
            if (distance <= axeReach * axeReach && this.archer.age >= this.axeReadyAge) {
                this.axeReadyAge = this.archer.age + AXE_COOLDOWN;
                this.archer.swingHand(Hand.OFF_HAND);
                this.archer.tryAttack(target);
            }
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
