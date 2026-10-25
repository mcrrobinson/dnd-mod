package mattonfire.dnd.entity;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The Duergar Taskmaster, one per Duergar Hold: 40 health, 7 armour, a whip for 5 damage that reaches
 * {@link #WHIP_REACH} blocks further than a plain swing. In a fight, every {@link #DRIVE_COOLDOWN} ticks it
 * <b>Drives</b> the duergar within {@link #DRIVE_RANGE} blocks (itself too): Speed I and Strength I for
 * {@link #DRIVE_TICKS} ticks. Killing it costs 150 Grey Clans standing.
 */
public class DuergarTaskmasterEntity extends DuergarEntity {
    public static final double WHIP_REACH = 2.5D;
    public static final double DRIVE_RANGE = 12.0D;
    public static final int DRIVE_TICKS = 8 * 20;
    public static final int DRIVE_COOLDOWN = 20 * 20;

    private int driveReadyAge;

    public DuergarTaskmasterEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 15;
    }

    public static DefaultAttributeContainer.Builder createTaskmasterAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 7.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.2D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void initCombatGoals() {
        this.goalSelector.add(2, new DuergarMeleeGoal(this, 1.1D, WHIP_REACH));
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
        if (this.getCustomName() != null) {
            this.setCustomName(Text.translatable("entity.dndclasses.duergar_taskmaster.named", this.getCustomName()));
        }
        // The whip: a lead, coiled in the hand.
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.LEAD));
        this.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setEquipmentDropChance(slot, 0.0F);
        }
        return data;
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (!this.world.isClient && this.getTarget() != null && this.getTarget().isAlive() && this.age >= this.driveReadyAge) {
            this.drive();
        }
    }

    /** Drive: Speed I and Strength I to every duergar within {@link #DRIVE_RANGE} blocks. */
    public void drive() {
        this.driveReadyAge = this.age + DRIVE_COOLDOWN;
        int driven = 0;
        for (DuergarEntity duergar : this.world.getEntitiesByClass(DuergarEntity.class,
                this.getBoundingBox().expand(DRIVE_RANGE), DuergarEntity::isAlive)) {
            duergar.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, DRIVE_TICKS, 0), this);
            duergar.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, DRIVE_TICKS, 0), this);
            driven++;
        }
        this.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        this.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 0.6F);
        this.playSound(SoundEvents.ENTITY_VINDICATOR_CELEBRATE, 1.2F, 0.6F);
        if (this.world instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.ANGRY_VILLAGER, this.getX(), this.getEyeY() + 0.3D, this.getZ(), 3,
                    0.3D, 0.2D, 0.3D, 0.0D);
        }
        MobActions.log(this, "drive x" + driven, this.getTarget());
    }
}
