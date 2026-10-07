package mattonfire.dnd.entity;

import java.util.UUID;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MagmamuncherEntity extends TameableEntity implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public MagmamuncherEntity(EntityType<? extends TameableEntity> entityType, World world) {
        super(entityType, world);
        // The type is fireImmune, so fire and lava don't hurt it: no reason to steer around them.
        this.setPathfindingPenalty(PathNodeType.LAVA, 8.0F);
        this.setPathfindingPenalty(PathNodeType.DANGER_FIRE, 0.0F);
        this.setPathfindingPenalty(PathNodeType.DAMAGE_FIRE, 0.0F);
    }

    /** Highest y a natural spawn may stand at: keeps them off the bedrock roof of the Nether. */
    private static final int NETHER_ROOF_Y = 127;

    /**
     * Spawn restriction for the Nether: any solid ground (netherrack, basalt, blackstone, soul soil/sand)
     * or magma (allowed because the type is fireImmune), in any light, but never on top of the Nether roof.
     * Spawners and spawn eggs skip the checks.
     */
    public static boolean canSpawnInNether(EntityType<MagmamuncherEntity> type, ServerWorldAccess world,
                                           SpawnReason reason, BlockPos pos, Random random) {
        return reason == SpawnReason.SPAWNER
                || (pos.getY() < NETHER_ROOF_Y && MobEntity.canMobSpawn(type, world, reason, pos, random));
    }

    /** AnimalEntity prefers bright grass, which made every dark Nether spot fail {@code canSpawn}. */
    @Override
    public float getPathfindingFavor(BlockPos pos, WorldView world) {
        return 0.0F;
    }

    /** Wild ones despawn like other Nether mobs (they're in the MONSTER group); tamed ones never do. */
    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return !this.isTamed();
    }

    @Override
    public boolean cannotDespawn() {
        return super.cannotDespawn() || this.isTamed() || this.isLeashed();
    }

    @Override
    public LivingEntity getOwner() {
        UUID uuid = this.getOwnerUuid();
        return uuid == null ? null : this.world.getPlayerByUuid(uuid);
    }

    @Override
    public net.minecraft.world.EntityView method_48926() {
        return this.world;
    }

    public static DefaultAttributeContainer.Builder createMagmamuncherAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 60.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0D);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new SitGoal(this));
        this.goalSelector.add(3, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.add(4, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 1.0D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(2, new AttackWithOwnerGoal(this));
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<MagmamuncherEntity> controller = new AnimationController<>(this, "controller", 0, this::predicate);
        controller.setSoundKeyframeHandler(event -> {});
        controllers.add(controller);
    }

    private <T extends GeoEntity> PlayState predicate(AnimationState<T> tAnimationState) {
        if (tAnimationState.isMoving()) {
            return tAnimationState.setAndContinue(RawAnimation.begin().thenLoop("walk"));
        }
        return tAnimationState.setAndContinue(RawAnimation.begin().thenLoop("idle"));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
