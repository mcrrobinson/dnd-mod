package mattonfire.dnd.entity;

import mattonfire.dnd.entity.ai.goal.DragonAttackGoal;
import mattonfire.dnd.entity.ai.goal.DragonFlyRandomlyGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.joml.Vector3f;

public class WyvernEntity extends FlyingDragonEntity {
    // The model is ~12 blocks wide but the entity hitbox is only 1.5, so wings, neck, head and tail
    // get their own hittable parts (like the ender dragon), each wrapping a group of model bones.
    // Root bone sits 11px lower on the ground and 34.5px lower in flight (wyvern.animation.json).
    protected static final DragonPartLayout PART_LAYOUT = new DragonPartLayout("wyvern", -11 / 16.0, -34.5 / 16.0)
            .part("body", "front", "back")
            .part("neck_base", "neck1", "neck2")
            .part("neck_mid", "neck3", "neck4")
            .part("neck_top", "neck5")
            .part("head", "head", "jaw_low")
            .part("tail_base", "tail1", "tail2")
            .part("tail_tip", "tail3", "tail4", "tail5")
            .pair("wing", "wing_left", "membrane_shoulder_left", "wing_wart_left", "membrane_wart1_left", "membrane_wart2_left")
            .pair("wing_arm", "shoulder_arm_left", "membrane_shoulder_arm_left")
            .pair("wing_finger1", "wing_finger1_left", "membrane_wing_finger1_left")
            .pair("wing_finger2", "wing_finger2_left", "membrane_wing_finger2_left")
            .pair("wing_finger3", "wing_finger3_left", "membrane_wing_finger3_left", "wing_finger4_left")
            // Legs hang below the main hitbox in flight
            .pair("leg", "leg_left", "leg_mid_left", "leg_ground_left", "feet_left",
                    "feet_finger1_left", "feet_finger2_left", "feet_finger3_left", "feet_finger4_left")
            // Server-side parts follow these, in step with the client (see DragonFlightAnimation)
            .animations(DragonFlightAnimation.NAMES);
    /** DEX save DC against the breath (Wyvern, Ember Wyvern and Bone Wyvern). */
    public static final int BREATH_SAVE_DC = 12;

    public WyvernEntity(EntityType<? extends TameableEntity> entityType, World world) {
        super(entityType, world, PART_LAYOUT, BossBar.Color.RED);
        // Dragon Slayer comes from the advancement's player_killed_entity criterion (#dndclasses:dragons).
    }

    @Override
    protected FireBreath createBreath(DragonPart head, TrackedData<Integer> ticksKey, TrackedData<Vector3f> aimKey) {
        return new FireBreath(this, head, ticksKey, aimKey, this.getBreathRange(), this.getBreathDamage());
    }

    /** How far the flame reaches. Read from the constructor, so variants return a constant. */
    protected double getBreathRange() {
        return 12.0;
    }

    /** The flame's damage. Read from the constructor, so variants return a constant. */
    protected float getBreathDamage() {
        return 4.0F;
    }

    @Override
    protected boolean isBossFightActive() {
        return this.hasBossFight() && !this.isTamed();
    }

    /** Whether a fight shows the boss bar and plays the fight music (not for the common Ember Wyvern). */
    protected boolean hasBossFight() {
        return true;
    }

    /** The model texture; variants override it. */
    public Identifier getTexture() {
        return new Identifier(DnDClasses.MOD_ID, "textures/entity/wyvern/green.png");
    }

    @Override
    public int getBreathSaveDc() {
        return BREATH_SAVE_DC;
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new DragonAttackGoal(this));
        this.goalSelector.add(3, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F, false));
        this.goalSelector.add(4, new DragonFlyRandomlyGoal(this));
        this.goalSelector.add(5, new WanderAroundGoal(this, 1.0D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));
        
        this.targetSelector.add(1, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(2, new AttackWithOwnerGoal(this));
        this.targetSelector.add(3, new RevengeGoal(this));
        // Wild ones hunt players; tamed ones leave everyone alone unless the owner fights them
        this.targetSelector.add(4, new UntamedActiveTargetGoal<>(this, PlayerEntity.class, true, null));
    }

    public static DefaultAttributeContainer.Builder createWyvernAttributes() {
        return TameableEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6)
                // tryAttack reads it; without it the server crashes on the first melee hit
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0);
    }

    // Wild ones despawn far from players like monsters, so they don't pile up over the world
    // (an animal never despawns). Tamed, named, leashed or ridden ones stay; a name tag also
    // makes a mob persistent on its own.
    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return !this.isTamed() && !this.hasCustomName() && !this.isLeashed() && !this.hasPassengers();
    }

    // AnimalEntity gives 1-3 XP whatever experiencePoints says
    @Override
    public int getXpToDrop() {
        return 20;
    }
}
