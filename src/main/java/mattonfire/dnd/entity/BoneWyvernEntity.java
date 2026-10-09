package mattonfire.dnd.entity;

import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.scoreboard.Team;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/**
 * A small skeletal Wyvern raised by the Necromancer's Raise Dead at its top rank. Same model,
 * animations and parts as the Wyvern, drawn at {@link #SCALE} with a bone texture. It's tamed to the
 * Necromancer, so it fights what they fight and never them or their party, and it also hunts hostile
 * mobs on its own. Its bite withers. It has no loot, XP or boss bar, and is removed like the other
 * summons (see SkillHelpers.spawnSummon).
 */
public class BoneWyvernEntity extends WyvernEntity {
    /** Model size relative to the Wyvern; the renderer draws it at the same scale. */
    public static final float SCALE = 0.35F;
    public static final double BREATH_RANGE = 8.0;
    public static final float BREATH_DAMAGE = 3.0F;
    public static final int BITE_WITHER_TICKS = 60;

    private static final Identifier TEXTURE = new Identifier(DnDClasses.MOD_ID, "textures/entity/wyvern/bone.png");
    private static final DragonPartLayout BONE_LAYOUT = PART_LAYOUT.scaled(SCALE);

    public BoneWyvernEntity(EntityType<? extends TameableEntity> entityType, World world) {
        super(entityType, world, BREATH_RANGE, BREATH_DAMAGE);
    }

    public static DefaultAttributeContainer.Builder createBoneWyvernAttributes() {
        return TameableEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.75)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        // Besides what the owner fights: any hostile mob in sight (never a teammate, see TargetPredicate)
        this.targetSelector.add(5, new ActiveTargetGoal<>(this, LivingEntity.class, 10, true, false,
                entity -> entity instanceof Monster));
    }

    @Override
    public DragonPartLayout getPartLayout() {
        return BONE_LAYOUT;
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
    public double getBiteRange() {
        return 2.5;
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit && target instanceof LivingEntity living) {
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, BITE_WITHER_TICKS, 0), this);
        }
        return hit;
    }

    /**
     * A tamed mob only asks its owner (and reports the owner's team as its own), and the owner isn't on
     * the Necromancer's ally team, so the raised undead on it would count as hostile mobs to bite and
     * flame. The wyvern's own ally team counts too.
     */
    @Override
    public boolean isTeammate(Entity other) {
        Team team = this.world.getScoreboard().getPlayerTeam(this.getEntityName());
        return super.isTeammate(other) || team != null && team.isEqual(other.getScoreboardTeam());
    }

    @Override
    public EntityGroup getGroup() {
        return EntityGroup.UNDEAD;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_SKELETON_DEATH;
    }

    // A summon: no XP and it never despawns on its own
    @Override
    public int getXpToDrop() {
        return 0;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }
}
