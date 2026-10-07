package mattonfire.dnd.entity;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.block.BlockState;
import mattonfire.dnd.classes.Damages.ModDamageTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameRules;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
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

import java.util.List;

/**
 * A slow, translucent cube of acidic jelly that lurks in caves and dungeons. Anything living that it
 * touches is engulfed: pulled towards its middle, slowed and dissolved a little every second. It soaks
 * up items lying in its path (they float about inside it, visible through the jelly) and drops them all
 * when it dies.
 */
public class GelatinousCubeEntity extends HostileEntity implements GeoEntity {
    /** How many different stacks it can hold (and show) at once. */
    public static final int MAX_ABSORBED = 6;
    /** Junk it may already have inside when it spawns. */
    public static final Identifier CONTENTS_LOOT = new Identifier(DnDClasses.MOD_ID, "gameplay/gelatinous_cube_contents");

    /** Ticks between acid hits on something engulfed; the first lands on contact. */
    private static final int DIGEST_INTERVAL = 20;
    private static final int SLOWNESS_AMPLIFIER = 2;
    /** Horizontal pull towards the cube's middle, per tick. */
    private static final double PULL = 0.04D;

    @SuppressWarnings("unchecked")
    private static final TrackedData<ItemStack>[] ABSORBED = new TrackedData[MAX_ABSORBED];

    static {
        for (int i = 0; i < MAX_ABSORBED; i++) {
            ABSORBED[i] = DataTracker.registerData(GelatinousCubeEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
        }
    }

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ENGULF = RawAnimation.begin().thenPlay("engulf");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** Entity id of everything currently engulfed -> ticks it has been inside. */
    private final Int2IntMap engulfed = new Int2IntOpenHashMap();

    public GelatinousCubeEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 10;
    }

    public static DefaultAttributeContainer.Builder createGelatinousCubeAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 50.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.17D)
                // Acid damage per second to whatever it has engulfed.
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0D);
    }

    /** Natural spawns: dark, underground, out of sight of the sky. */
    public static boolean canSpawn(EntityType<GelatinousCubeEntity> type, ServerWorldAccess world, SpawnReason reason,
                                   BlockPos pos, Random random) {
        if (!HostileEntity.canSpawnInDark(type, world, reason, pos, random)) {
            return false;
        }
        return reason != SpawnReason.NATURAL || (pos.getY() < world.getSeaLevel() - 10 && !world.isSkyVisible(pos));
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        for (TrackedData<ItemStack> slot : ABSORBED) {
            this.dataTracker.startTracking(slot, ItemStack.EMPTY);
        }
    }

    @Override
    protected void initGoals() {
        // No SwimGoal: it sinks and crawls along the bottom.
        this.goalSelector.add(2, new EngulfTargetGoal(this));
        this.goalSelector.add(5, new WanderAroundGoal(this, 0.8D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, IronGolemEntity.class, true));
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
        // Bits and pieces of earlier adventurers.
        ServerWorld serverWorld = world.toServerWorld();
        LootTable table = serverWorld.getServer().getLootManager().getTable(CONTENTS_LOOT);
        List<ItemStack> loot = table.generateLoot(new LootContext.Builder(serverWorld)
                .random(this.random)
                .build(LootContextTypes.EMPTY));
        for (ItemStack stack : loot) {
            this.absorb(stack);
        }
        return data;
    }

    // ---------------------------------------------------------------- engulfing

    @Override
    public void tickMovement() {
        super.tickMovement();
        if (!this.world.isClient && this.isAlive()) {
            this.engulfTouching();
            if (this.age % 5 == 0 && this.world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
                this.absorbTouchingItems();
            }
        }
    }

    private void engulfTouching() {
        List<LivingEntity> touching = this.world.getEntitiesByClass(LivingEntity.class, this.getBoundingBox(),
                e -> e != this && e.isAlive() && !(e instanceof GelatinousCubeEntity) && !(e instanceof ArmorStandEntity)
                        && EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(e));

        Int2IntMap stillInside = new Int2IntOpenHashMap();
        for (LivingEntity victim : touching) {
            int ticksInside = this.engulfed.getOrDefault(victim.getId(), 0);
            if (ticksInside == 0) {
                this.triggerAnim("engulf_controller", "engulf");
                this.playSound(SoundEvents.ENTITY_SLIME_ATTACK, 1.0F, 0.5F);
            }
            this.holdInside(victim, ticksInside);
            if (ticksInside % DIGEST_INTERVAL == 0) {
                this.digest(victim);
            }
            stillInside.put(victim.getId(), ticksInside + 1);
        }
        this.engulfed.clear();
        this.engulfed.putAll(stillInside);
    }

    /** Slows and gently drags the victim towards the middle, so walking out is a struggle. */
    private void holdInside(LivingEntity victim, int ticksInside) {
        if (ticksInside % 10 == 0 || !victim.hasStatusEffect(StatusEffects.SLOWNESS)) {
            victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 30, SLOWNESS_AMPLIFIER, false, false, true), this);
        }
        Vec3d toMiddle = new Vec3d(this.getX() - victim.getX(), 0.0D, this.getZ() - victim.getZ());
        if (toMiddle.lengthSquared() > 0.01D) {
            victim.addVelocity(toMiddle.normalize().multiply(PULL));
            victim.velocityModified = true;
        }
    }

    private void digest(LivingEntity victim) {
        DamageSource acid = ModDamageTypes.of(this.world, ModDamageTypes.GELATINOUS_CUBE_DAMAGE_SOURCE, this);
        // A hurt with an attacker knocks the victim away; it's meant to stay stuck, so undo that.
        Vec3d velocity = victim.getVelocity();
        if (victim.damage(acid, (float) this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE))) {
            victim.setVelocity(velocity);
            victim.velocityModified = true;
            this.playSound(SoundEvents.ENTITY_SLIME_SQUISH, 0.6F, 0.6F);
        }
    }

    // Things sink into it rather than being shoved aside.

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushAway(Entity entity) {
    }

    @Override
    public void pushAwayFrom(Entity entity) {
    }

    // ---------------------------------------------------------------- absorbed items

    private void absorbTouchingItems() {
        List<ItemEntity> items = this.world.getEntitiesByClass(ItemEntity.class, this.getBoundingBox(),
                item -> item.isAlive() && !item.getStack().isEmpty() && !item.cannotPickup());
        for (ItemEntity item : items) {
            ItemStack stack = item.getStack().copy();
            int before = stack.getCount();
            ItemStack left = this.absorb(stack);
            if (left.getCount() == before) {
                continue;
            }
            this.sendPickup(item, before - left.getCount());
            if (left.isEmpty()) {
                item.discard();
            } else {
                item.setStack(left);
            }
            // Like a zombie that picked up loot, it now keeps what it took.
            this.setPersistent();
            this.playSound(SoundEvents.ENTITY_SLIME_SQUISH_SMALL, 0.8F, 0.6F + this.random.nextFloat() * 0.2F);
        }
    }

    /**
     * Soaks up as much of the stack as fits (topping up matching stacks, then free slots).
     *
     * @return what didn't fit
     */
    public ItemStack absorb(ItemStack stack) {
        for (int i = 0; i < MAX_ABSORBED && !stack.isEmpty(); i++) {
            ItemStack held = this.getAbsorbed(i);
            if (!held.isEmpty() && ItemStack.canCombine(held, stack) && held.getCount() < held.getMaxCount()) {
                int moved = Math.min(stack.getCount(), held.getMaxCount() - held.getCount());
                ItemStack topped = held.copy();
                topped.increment(moved);
                stack.decrement(moved);
                this.dataTracker.set(ABSORBED[i], topped);
            }
        }
        for (int i = 0; i < MAX_ABSORBED && !stack.isEmpty(); i++) {
            if (this.getAbsorbed(i).isEmpty()) {
                this.dataTracker.set(ABSORBED[i], stack.copy());
                stack.setCount(0);
            }
        }
        return stack;
    }

    /** One of the stacks floating inside it (empty if that slot is free). */
    public ItemStack getAbsorbed(int slot) {
        return this.dataTracker.get(ABSORBED[slot]);
    }

    @Override
    protected void dropInventory() {
        super.dropInventory();
        for (int i = 0; i < MAX_ABSORBED; i++) {
            ItemStack stack = this.getAbsorbed(i);
            if (!stack.isEmpty()) {
                this.dropStack(stack.copy(), 1.0F);
                this.dataTracker.set(ABSORBED[i], ItemStack.EMPTY);
            }
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        NbtList list = new NbtList();
        for (int i = 0; i < MAX_ABSORBED; i++) {
            ItemStack stack = this.getAbsorbed(i);
            if (!stack.isEmpty()) {
                list.add(stack.writeNbt(new NbtCompound()));
            }
        }
        nbt.put("AbsorbedItems", list);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("AbsorbedItems", NbtElement.LIST_TYPE)) {
            for (int i = 0; i < MAX_ABSORBED; i++) {
                this.dataTracker.set(ABSORBED[i], ItemStack.EMPTY);
            }
            NbtList list = nbt.getList("AbsorbedItems", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < list.size(); i++) {
                this.absorb(ItemStack.fromNbt(list.getCompound(i)));
            }
        }
    }

    // ---------------------------------------------------------------- sounds

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_SLIME_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_SLIME_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.ENTITY_SLIME_SQUISH, 0.3F, 0.5F);
    }

    @Override
    public float getSoundPitch() {
        // A big, deep wobble.
        return super.getSoundPitch() * 0.6F;
    }

    // ---------------------------------------------------------------- animation

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, this::movementPredicate));
        controllers.add(new AnimationController<GelatinousCubeEntity>(this, "engulf_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("engulf", ENGULF));
    }

    private PlayState movementPredicate(AnimationState<GelatinousCubeEntity> state) {
        return state.setAndContinue(state.isMoving() ? WALK : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    /** Oozes straight into its target; the acid does the damage once it's inside, so there's no swing. */
    private static class EngulfTargetGoal extends MeleeAttackGoal {
        EngulfTargetGoal(GelatinousCubeEntity cube) {
            super(cube, 1.0D, true);
        }

        @Override
        protected void attack(LivingEntity target, double squaredDistance) {
        }
    }
}
