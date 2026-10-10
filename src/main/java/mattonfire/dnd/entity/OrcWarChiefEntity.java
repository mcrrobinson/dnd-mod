package mattonfire.dnd.entity;

import java.util.List;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.ModItems;
import mattonfire.dnd.classes.Registry.ModSounds;
import mattonfire.dnd.entity.boss.Boss;
import mattonfire.dnd.entity.boss.BossFight;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The Orc War Chief, boss of a stronghold (role {@code orc_war_chief}), waiting by his seat in the Great
 * Longhouse: 90 health, 8 armour, 12 damage (an Orcish Greataxe), speed 0.3. Uses {@link BossFight}: a red bar
 * and the fight music while he fights a player.
 * <ul>
 * <li><b>Ground Slam</b> every {@link #SLAM_COOLDOWN} ticks while his target is within {@link #SLAM_RADIUS}
 * blocks: {@link #SLAM_DAMAGE} damage and a heavy knockback to everything round him he'd fight (never orcs,
 * goblins or tolerated players).</li>
 * <li><b>War Cry</b> on dropping below half health: Strength I for 10 s to every orc within 16 blocks, himself
 * included.</li>
 * </ul>
 */
public class OrcWarChiefEntity extends OrcEntity implements Boss {
    public static final float SLAM_DAMAGE = 6.0F;
    public static final double SLAM_RADIUS = 4.0D;
    public static final int SLAM_COOLDOWN = 8 * 20;
    public static final double WAR_CRY_RANGE = 16.0D;
    public static final int WAR_CRY_DURATION = 10 * 20;
    public static final String ROLE_TAG = "dndclasses.role.orc_war_chief";

    private final BossFight bossFight;
    private int slamReadyAge = SLAM_COOLDOWN;

    public OrcWarChiefEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.bossFight = new BossFight(this, BossBar.Color.RED, BossBar.Style.NOTCHED_10)
                .range(48.0D)
                .music(ModSounds.MUSIC_DRAGON_FIGHT)
                .phase(0.5F, BossBar.Color.PURPLE, this::warCry)
                .xp(60);
    }

    public static DefaultAttributeContainer.Builder createOrcWarChiefAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 90.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 8.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3D)
                // + the Greataxe's 10 = 12
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0D);
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
        if (this.getCustomName() != null) {
            this.setCustomName(Text.translatable("entity.dndclasses.orc_war_chief.named", this.getCustomName()));
        }
        this.addCommandTag(ROLE_TAG);
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(ModItems.ORCISH_GREATAXE));
        this.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setEquipmentDropChance(slot, 0.0F);
        }
        return data;
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive() && this.age >= this.slamReadyAge
                && this.squaredDistanceTo(target) <= SLAM_RADIUS * SLAM_RADIUS) {
            this.slam();
        }
    }

    /** Ground Slam: everything he'd fight within {@link #SLAM_RADIUS} takes {@link #SLAM_DAMAGE} and is thrown back. */
    public void slam() {
        if (!(this.world instanceof ServerWorld world)) {
            return;
        }
        this.slamReadyAge = this.age + SLAM_COOLDOWN;
        this.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        List<LivingEntity> hit = world.getEntitiesByClass(LivingEntity.class, new Box(this.getBlockPos()).expand(SLAM_RADIUS),
                e -> e != this && e.isAlive() && !e.isSpectator() && this.squaredDistanceTo(e) <= SLAM_RADIUS * SLAM_RADIUS
                        && this.slamHits(e));
        DamageSource source = world.getDamageSources().mobAttack(this);
        for (LivingEntity victim : hit) {
            if (victim.damage(source, SLAM_DAMAGE)) {
                Vec3d away = victim.getPos().subtract(this.getPos()).multiply(1.0D, 0.0D, 1.0D);
                if (away.lengthSquared() < 1.0E-4D) {
                    away = new Vec3d(1.0D, 0.0D, 0.0D);
                }
                away = away.normalize();
                victim.takeKnockback(1.5D, -away.x, -away.z);
                victim.addVelocity(0.0D, 0.4D, 0.0D);
                victim.velocityModified = true;
            }
        }
        world.spawnParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.2D, this.getZ(), 6, 1.5D, 0.1D, 1.5D, 0.0D);
        world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY() + 0.1D, this.getZ(), 12, 2.0D, 0.1D, 2.0D, 0.01D);
        world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_RAVAGER_STUNNED, SoundCategory.HOSTILE, 1.5F, 0.7F);
        DnDClasses.LOGGER.info("[OrcStronghold] War Chief {} slammed: {} hit for {}", this.getEntityName(), hit.size(), SLAM_DAMAGE);
    }

    private boolean slamHits(LivingEntity entity) {
        if (OrcHostility.isAlly(entity)) {
            return false;
        }
        if (entity instanceof PlayerEntity player) {
            return !player.isCreative() && this.mayTarget(player, true);
        }
        return true;
    }

    /** War Cry: Strength I for 10 s to every orc within 16 blocks. */
    private void warCry() {
        if (!(this.world instanceof ServerWorld world)) {
            return;
        }
        List<OrcEntity> orcs = world.getEntitiesByClass(OrcEntity.class, this.getBoundingBox().expand(WAR_CRY_RANGE), Entity::isAlive);
        for (OrcEntity orc : orcs) {
            orc.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, WAR_CRY_DURATION, 0), this);
        }
        world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 2.0F, 0.8F);
        world.spawnParticles(ParticleTypes.ANGRY_VILLAGER, this.getX(), this.getBodyY(0.9D), this.getZ(), 8, 0.6D, 0.4D, 0.6D, 0.0D);
        DnDClasses.LOGGER.info("[OrcStronghold] War Chief {} War Cry: Strength I to {} orcs", this.getEntityName(), orcs.size());
    }

    @Override
    public void tick() {
        super.tick();
        this.bossFight.tick();
    }

    @Override
    public BossFight getBossFight() {
        return this.bossFight;
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    @Override
    public float getSoundPitch() {
        return super.getSoundPitch() * 0.85F;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        this.bossFight.writeNbt(nbt);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.bossFight.readNbt(nbt);
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

    @Override
    public void onDeath(DamageSource source) {
        this.bossFight.onDeath();
        super.onDeath(source);
    }
}
