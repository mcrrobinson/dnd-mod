package mattonfire.dnd.entity;

import mattonfire.dnd.faction.TierEffects;
import mattonfire.dnd.world.gen.HomeBonuses;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.ai.goal.GoToWalkTargetGoal;
import net.minecraft.entity.ai.goal.LongDoorInteractGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * A peaceful halfling from the hobbit villages. Wanders its village by day, goes home at night,
 * keeps away from monsters and is always snacking. Right-click one with an empty hand and it
 * shares some of its food.
 */
public class HobbitEntity extends PathAwareEntity {
    public static final int VARIANTS = 6;
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(HobbitEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private static final int DAY_RANGE = 24;
    private static final int NIGHT_RANGE = 3;
    private static final float HOSTILE_FLEE_DISTANCE = 8.0F;

    private static final Item[] SNACKS = {
            Items.BREAD, Items.APPLE, Items.COOKIE, Items.PUMPKIN_PIE, Items.BAKED_POTATO,
            Items.CARROT, Items.COOKED_CHICKEN, Items.SWEET_BERRIES, Items.MUSHROOM_STEW, Items.HONEY_BOTTLE
    };
    private static final String[] FIRST_NAMES = {
            "Bilbo", "Frodo", "Samwise", "Meriadoc", "Peregrin", "Rosie", "Lobelia", "Bungo", "Belladonna",
            "Hamfast", "Fredegar", "Lotho", "Primula", "Drogo", "Daisy", "Marigold", "Elanor", "Tolman",
            "Ponto", "Holman", "Poppy", "Odo", "Otho", "Mungo", "Ruby", "Pansy", "Hob", "Wilcome", "Dudo", "Esmeralda"
    };
    private static final String[] SURNAMES = {
            "Baggins", "Took", "Brandybuck", "Gamgee", "Proudfoot", "Bolger", "Cotton", "Boffin", "Bracegirdle",
            "Burrows", "Goodbody", "Hornblower", "Sackville", "Underhill", "Greenhand", "Chubb", "Grubb", "Maggot",
            "Brockhouse", "Twofoot", "Puddifoot", "Sandyman", "Banks", "Longhole"
    };

    @Nullable
    private BlockPos home;
    /** The wait a hobbit without a saved {@code GiftWait} assumes (the Neutral 5 minutes). */
    private static final int DEFAULT_GIFT_WAIT = 20 * 60 * 5;
    /** Ticks left of the wait set by the last gift (that player's wait). */
    private int giftCooldown;
    /** The wait the last gift set, so {@code giftWait - giftCooldown} is the time since it. */
    private int giftWait = DEFAULT_GIFT_WAIT;

    public HobbitEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        ((MobNavigation) this.getNavigation()).setCanPathThroughDoors(true);
        this.getNavigation().setCanSwim(true);
    }

    public static DefaultAttributeContainer.Builder createHobbitAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 14.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.32D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new EscapeDangerGoal(this, 1.4D));
        this.goalSelector.add(2, new FleeEntityGoal<>(this, HostileEntity.class, 10.0F, 0.9D, 1.3D));
        // Players Hostile with the hobbits are kept at a distance.
        this.goalSelector.add(2, new FleeEntityGoal<>(this, PlayerEntity.class, HOSTILE_FLEE_DISTANCE, 0.9D, 1.3D,
                player -> TierEffects.fleesFrom(TierEffects.tierWith((PlayerEntity) player, this))));
        this.goalSelector.add(3, new LongDoorInteractGoal(this, true));
        this.goalSelector.add(4, new GoToWalkTargetGoal(this, 0.8D));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.6D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(6, new LookAtEntityGoal(this, HobbitEntity.class, 6.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(VARIANT, 0);
    }

    public int getVariant() {
        return this.dataTracker.get(VARIANT);
    }

    /** Ties the hobbit to a spot it wanders around by day and returns to at night. */
    public void setHome(BlockPos home) {
        this.home = home.toImmutable();
        this.updateRange();
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        Random random = world.getRandom();
        this.dataTracker.set(VARIANT, random.nextInt(VARIANTS));
        this.setCustomName(Text.literal(FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " "
                + SURNAMES[random.nextInt(SURNAMES.length)]));
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(SNACKS[random.nextInt(SNACKS.length)]));
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 1.0F);
        this.setPersistent();
        this.setHome(this.getBlockPos());
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }

    // The mod gives every mob a player-targeting goal; hobbits never pick fights.
    @Override
    public void setTarget(@Nullable LivingEntity target) {
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (this.giftCooldown > 0) {
            this.giftCooldown--;
        }
        if (this.age % 100 == 0) {
            this.updateRange();
        }
        if (this.random.nextInt(900) == 0 && !this.getMainHandStack().isEmpty()) {
            this.nibble();
        }
    }

    // Stay near home: the whole village by day, indoors at night.
    private void updateRange() {
        if (this.home != null) {
            this.setPositionTarget(this.home, this.world.isDay() ? this.dayRange() : this.nightRange());
        }
    }

    /** How far from home this hobbit roams by day. */
    protected int dayRange() {
        return DAY_RANGE;
    }

    /** How far from home this hobbit roams at night. */
    protected int nightRange() {
        return NIGHT_RANGE;
    }

    private void nibble() {
        this.playSound(SoundEvents.ENTITY_GENERIC_EAT, 0.6F, this.getSoundPitch());
        if (this.world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, this.getMainHandStack()),
                    this.getX(), this.getEyeY() - 0.1D, this.getZ(), 6, 0.1D, 0.05D, 0.1D, 0.05D);
        }
    }

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (hand != Hand.MAIN_HAND || !player.getStackInHand(hand).isEmpty()) {
            return super.interactMob(player, hand);
        }
        if (this.world.isClient) {
            return ActionResult.SUCCESS;
        }
        this.getLookControl().lookAt(player);
        // The wait before the next gift depends on how the hobbits feel about this player (Hostile gets none),
        // and Halflings wait less. Each asker needs their own wait to have passed since the last gift.
        int tierWait = TierEffects.giftCooldown(TierEffects.tierWith(player, this));
        int cooldown = tierWait < 0 ? tierWait : HomeBonuses.giftCooldown(player, tierWait);
        if (cooldown < 0) {
            player.sendMessage(Text.translatable("entity.dndclasses.hobbit.no_gift", this.getDisplayName())
                    .formatted(net.minecraft.util.Formatting.RED), true);
        }
        if (cooldown < 0 || this.giftCooldown > 0 && this.giftWait - this.giftCooldown < cooldown) {
            this.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0F, this.getSoundPitch());
            return ActionResult.CONSUME;
        }
        this.giftCooldown = cooldown;
        this.giftWait = cooldown;
        ItemStack gift = new ItemStack(SNACKS[this.random.nextInt(SNACKS.length)], 1 + this.random.nextInt(3));
        if (!player.giveItemStack(gift)) {
            player.dropItem(gift, false);
        }
        this.playSound(SoundEvents.ENTITY_VILLAGER_YES, 1.0F, this.getSoundPitch());
        ((ServerWorld) this.world).spawnParticles(ParticleTypes.HEART, this.getX(), this.getEyeY() + 0.4D, this.getZ(),
                3, 0.3D, 0.2D, 0.3D, 0.0D);
        return ActionResult.CONSUME;
    }

    @Override
    public float getSoundPitch() {
        return super.getSoundPitch() * 1.35F;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.entity.damage.DamageSource source) {
        return SoundEvents.ENTITY_VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_VILLAGER_DEATH;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Variant", this.getVariant());
        nbt.putInt("GiftCooldown", this.giftCooldown);
        nbt.putInt("GiftWait", this.giftWait);
        if (this.home != null) {
            nbt.put("Home", NbtHelper.fromBlockPos(this.home));
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.dataTracker.set(VARIANT, Math.floorMod(nbt.getInt("Variant"), VARIANTS));
        this.giftCooldown = nbt.getInt("GiftCooldown");
        this.giftWait = nbt.contains("GiftWait") ? nbt.getInt("GiftWait") : DEFAULT_GIFT_WAIT;
        if (nbt.contains("Home")) {
            this.setHome(NbtHelper.toBlockPos(nbt.getCompound("Home")));
        }
    }
}
