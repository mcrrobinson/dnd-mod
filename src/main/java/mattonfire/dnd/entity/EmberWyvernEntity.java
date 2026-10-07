package mattonfire.dnd.entity;

import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

/**
 * A Nether wyvern: same model, parts and fire breath as the Wyvern, but a glass cannon. Low health,
 * bites twice as often, breathes fire more often and flies faster. Fire and lava immune (the entity
 * type is fireImmune). A common monster, so no boss bar or fight music, and it despawns like one.
 */
public class EmberWyvernEntity extends WyvernEntity {
    private static final Identifier TEXTURE = new Identifier(DnDClasses.MOD_ID, "textures/entity/wyvern/ember.png");
    /** The Nether roof is bedrock at y=127; nothing should spawn on top of it. */
    private static final int NETHER_ROOF_Y = 127;

    public EmberWyvernEntity(EntityType<? extends TameableEntity> entityType, World world) {
        super(entityType, world);
        // Like a blaze: lava is fine to cross, fire is no danger
        this.setPathfindingPenalty(PathNodeType.LAVA, 8.0F);
        this.setPathfindingPenalty(PathNodeType.DANGER_FIRE, 0.0F);
        this.setPathfindingPenalty(PathNodeType.DAMAGE_FIRE, 0.0F);
    }

    public static DefaultAttributeContainer.Builder createEmberWyvernAttributes() {
        return TameableEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 26.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.75)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 7.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
    }

    /** Natural spawns: solid ground below y=127 (not the Nether roof), any light, not in peaceful. */
    public static boolean canSpawn(EntityType<EmberWyvernEntity> type, ServerWorldAccess world, SpawnReason reason,
                                   BlockPos pos, Random random) {
        return world.getDifficulty() != Difficulty.PEACEFUL
                && (reason != SpawnReason.NATURAL || pos.getY() < NETHER_ROOF_Y)
                && MobEntity.canMobSpawn(type, world, reason, pos, random);
    }

    // AnimalEntity favours grass or bright light, and MobEntity.canSpawn rejects spots it scores below
    // zero: that would rule out most of the dim Nether. Every spot is as good as any other.
    @Override
    public float getPathfindingFavor(BlockPos pos, WorldView world) {
        return 0.0F;
    }

    @Override
    public Identifier getTexture() {
        return TEXTURE;
    }

    @Override
    protected boolean hasBossFight() {
        return false;
    }

    @Override
    public int getMeleeCooldown() {
        return 10;
    }

    @Override
    public int getBreathCooldown() {
        return 40;
    }

    @Override
    public int getXpToDrop() {
        return 10;
    }

    // A monster, not an animal: wild ones despawn far from players and leave in peaceful
    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return !this.isTamed();
    }

    @Override
    protected boolean isDisallowedInPeaceful() {
        return !this.isTamed();
    }
}
