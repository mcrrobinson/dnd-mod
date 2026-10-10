package mattonfire.dnd.entity;

import java.util.EnumSet;
import java.util.UUID;
import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LongDoorInteractGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.BowItem;
import net.minecraft.item.DyeableItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * An Elf Warden, the enclave's guard. Neutral to players, like a mountain dwarf: it hunts monsters
 * (except creepers) within {@link #HUNT_RANGE} blocks of home with its bow, keeping 8-12 blocks off and
 * drawing for {@link #DRAW_TICKS} ticks a shot, and switches to its iron sword when one gets within 3
 * blocks. It turns on a player who strikes an elf, or whom it sees breaking Sylvan Law ({@link SylvanLaw}).
 */
public class ElfWardenEntity extends ElfEntity implements Angerable, RangedAttackMob {
    public static final int HUNT_RANGE = 24;
    public static final int DRAW_TICKS = 30;
    /** Forest green, for the Warden's leathers. */
    public static final int LEATHER_COLOR = 0x2F5D2A;
    private static final UniformIntProvider ANGER_TIME = TimeHelper.betweenSeconds(30, 50);
    private static final double MELEE_RANGE = 3.0D;
    private static final double KEEP_MIN = 8.0D;
    private static final double KEEP_MAX = 12.0D;

    private int angerTime;
    @Nullable
    private UUID angryAt;
    private ItemStack bow = new ItemStack(Items.BOW);
    private ItemStack sword = new ItemStack(Items.IRON_SWORD);

    public ElfWardenEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 8;
    }

    public static DefaultAttributeContainer.Builder createElfWardenAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 24.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 2.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.32D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, (double) HUNT_RANGE);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new WardenAttackGoal(this));
        this.goalSelector.add(3, new LongDoorInteractGoal(this, true));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.6D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this, ElfEntity.class).setGroupRevenge());
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false, this::shouldAngerAt));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, MobEntity.class, 5, true, false,
                entity -> entity instanceof Monster && !(entity instanceof CreeperEntity)
                        && !(entity instanceof MimicEntity mimic && mimic.isDormant())
                        && this.nearHome(entity)));
    }

    private boolean nearHome(LivingEntity entity) {
        return this.getHome() == null || entity.getBlockPos().isWithinDistance(this.getHome(), HUNT_RANGE);
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
        this.equip(world.getRandom());
        return data;
    }

    private void equip(Random random) {
        this.bow = new ItemStack(Items.BOW);
        if (random.nextFloat() < 0.3F) {
            this.bow.addEnchantment(Enchantments.POWER, 1);
        }
        this.sword = new ItemStack(Items.IRON_SWORD);
        this.equipStack(EquipmentSlot.MAINHAND, this.bow.copy());
        ItemStack tunic = new ItemStack(Items.LEATHER_CHESTPLATE);
        ((DyeableItem) tunic.getItem()).setColor(tunic, LEATHER_COLOR);
        ItemStack boots = new ItemStack(Items.LEATHER_BOOTS);
        ((DyeableItem) boots.getItem()).setColor(boots, LEATHER_COLOR);
        this.equipStack(EquipmentSlot.CHEST, tunic);
        this.equipStack(EquipmentSlot.FEET, boots);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setEquipmentDropChance(slot, 0.05F);
        }
    }

    /** Draws the bow, or (in melee) the sword. */
    void wield(boolean melee) {
        ItemStack want = melee ? this.sword : this.bow;
        if (!ItemStack.areEqual(this.getMainHandStack(), want) && this.getMainHandStack().getItem() != want.getItem()) {
            this.equipStack(EquipmentSlot.MAINHAND, want.copy());
        }
    }

    @Override
    public void attack(LivingEntity target, float pullProgress) {
        ItemStack arrows = new ItemStack(Items.ARROW);
        PersistentProjectileEntity arrow = ProjectileUtil.createArrowProjectile(this, arrows, pullProgress);
        double dx = target.getX() - this.getX();
        double dy = target.getBodyY(0.3333333333333333D) - arrow.getY();
        double dz = target.getZ() - this.getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        arrow.setVelocity(dx, dy + flat * 0.2D, dz, 1.6F, (float) (12 - this.world.getDifficulty().getId() * 4));
        arrow.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
        this.playSound(SoundEvents.ENTITY_SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.world.spawnEntity(arrow);
    }

    /**
     * This Warden saw {@code player} breaking Sylvan Law, or striking one of its folk, and turns on them,
     * with a shout if it wasn't angry already.
     */
    public void provoke(PlayerEntity player) {
        if (!this.shouldAngerAt(player)) {
            this.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0F, this.getSoundPitch() * 0.8F);
        }
        this.setAngryAt(player.getUuid());
        this.chooseRandomAngerTime();
        this.setTarget(player);
    }

    // Wardens only turn on players who wronged them, and never on their own folk.
    @Override
    protected boolean mayTarget(LivingEntity target) {
        if (target instanceof PlayerEntity player) {
            return player == this.getAttacker() || this.shouldAngerAt(player);
        }
        return !(target instanceof ElfEntity);
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (!this.world.isClient) {
            this.tickAngerLogic((ServerWorld) this.world, true);
        }
    }

    // ---- Angerable ----

    @Override
    public int getAngerTime() {
        return this.angerTime;
    }

    @Override
    public void setAngerTime(int angerTime) {
        this.angerTime = angerTime;
    }

    @Nullable
    @Override
    public UUID getAngryAt() {
        return this.angryAt;
    }

    @Override
    public void setAngryAt(@Nullable UUID angryAt) {
        this.angryAt = angryAt;
    }

    @Override
    public void chooseRandomAngerTime() {
        this.setAngerTime(ANGER_TIME.get(this.random));
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        this.writeAngerToNbt(nbt);
        nbt.put("Bow", this.bow.writeNbt(new NbtCompound()));
        nbt.put("Sword", this.sword.writeNbt(new NbtCompound()));
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.readAngerFromNbt(this.world, nbt);
        if (nbt.contains("Bow")) {
            this.bow = ItemStack.fromNbt(nbt.getCompound("Bow"));
        }
        if (nbt.contains("Sword")) {
            this.sword = ItemStack.fromNbt(nbt.getCompound("Sword"));
        }
    }

    /**
     * The Warden's fight: arrows from 8-12 blocks off (closing in when farther or out of sight, backing off
     * when nearer), the sword when the target gets within 3 blocks.
     */
    static final class WardenAttackGoal extends Goal {
        private final ElfWardenEntity warden;
        private int cooldown;
        private int seeTicks;
        private boolean clockwise;
        private int strafeTicks;

        WardenAttackGoal(ElfWardenEntity warden) {
            this.warden = warden;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            LivingEntity target = this.warden.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean shouldContinue() {
            return this.canStart();
        }

        @Override
        public void start() {
            this.warden.setAttacking(true);
        }

        @Override
        public void stop() {
            this.warden.setAttacking(false);
            this.seeTicks = 0;
            this.warden.clearActiveItem();
            this.warden.getNavigation().stop();
            this.warden.wield(false);
        }

        @Override
        public boolean shouldRunEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.warden.getTarget();
            if (target == null) {
                return;
            }
            double distance = this.warden.squaredDistanceTo(target);
            boolean sees = this.warden.getVisibilityCache().canSee(target);
            if (sees != this.seeTicks > 0) {
                this.seeTicks = 0;
            }
            this.seeTicks += sees ? 1 : -1;
            if (this.cooldown > 0) {
                this.cooldown--;
            }
            this.warden.getLookControl().lookAt(target, 30.0F, 30.0F);

            if (distance < MELEE_RANGE * MELEE_RANGE) {
                this.warden.clearActiveItem();
                this.warden.wield(true);
                this.warden.getNavigation().startMovingTo(target, 1.2D);
                double reach = this.warden.getWidth() * 2.0F * this.warden.getWidth() * 2.0F + target.getWidth();
                if (this.cooldown <= 0 && distance <= reach + 1.0D) {
                    this.warden.swingHand(Hand.MAIN_HAND);
                    this.warden.tryAttack(target);
                    this.cooldown = 20;
                }
                return;
            }
            this.warden.wield(false);
            if (distance > KEEP_MAX * KEEP_MAX || this.seeTicks < 20) {
                this.warden.getNavigation().startMovingTo(target, 1.0D);
                this.strafeTicks = 0;
            } else {
                this.warden.getNavigation().stop();
                if (++this.strafeTicks >= 20) {
                    if (this.warden.getRandom().nextFloat() < 0.3F) {
                        this.clockwise = !this.clockwise;
                    }
                    this.strafeTicks = 0;
                }
                float forward = distance < KEEP_MIN * KEEP_MIN ? -0.5F : 0.0F;
                this.warden.getMoveControl().strafeTo(forward, this.clockwise ? 0.4F : -0.4F);
            }
            if (this.warden.isUsingItem()) {
                if (!sees && this.seeTicks < -60) {
                    this.warden.clearActiveItem();
                } else if (sees && this.warden.getItemUseTime() >= DRAW_TICKS) {
                    int used = this.warden.getItemUseTime();
                    this.warden.clearActiveItem();
                    this.warden.attack(target, BowItem.getPullProgress(used));
                    this.cooldown = 20;
                    DnDClasses.LOGGER.debug("[Elves] {} loosed at {}", this.warden.getEntityName(), target.getEntityName());
                }
            } else if (this.cooldown <= 0 && this.seeTicks >= -60 && this.warden.getMainHandStack().isOf(Items.BOW)) {
                this.warden.setCurrentHand(Hand.MAIN_HAND);
            }
        }
    }
}
