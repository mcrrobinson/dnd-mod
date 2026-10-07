package mattonfire.dnd.entity.ai.goal;

import java.util.EnumSet;
import mattonfire.dnd.entity.OwlbearEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * The Owlbear's charge. With its target a few blocks off and in sight it roars and paws the ground
 * (the wind-up), then barrels in a straight line towards where the target stood. The first thing
 * it hits takes heavy damage and is thrown back; running into a wall instead leaves it stunned.
 * The line is fixed when the charge starts, so stepping aside dodges it.
 */
public class OwlbearChargeGoal extends Goal {
    private static final double MIN_RANGE = 5.0D;
    private static final double MAX_RANGE = 16.0D;
    private static final int WINDUP_TICKS = 20;
    private static final int MAX_CHARGE_TICKS = 30;
    private static final double CHARGE_SPEED = 0.65D;
    private static final int COOLDOWN = 120;
    private static final float DAMAGE_MULTIPLIER = 1.5F;
    private static final double KNOCKBACK = 1.5D;
    private static final int STUN_TICKS = 50;

    private final OwlbearEntity owlbear;
    private long nextChargeTime;
    private int ticks;
    private boolean done;
    private Vec3d direction = Vec3d.ZERO;

    public OwlbearChargeGoal(OwlbearEntity owlbear) {
        this.owlbear = owlbear;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
    }

    @Override
    public boolean canStart() {
        if (this.owlbear.getWorld().getTime() < this.nextChargeTime || this.owlbear.isHugging()
                || this.owlbear.isStunned() || !this.owlbear.isOnGround()) {
            return false;
        }
        LivingEntity target = this.owlbear.getTarget();
        if (target == null || !target.isAlive() || !this.owlbear.getVisibilityCache().canSee(target)) {
            return false;
        }
        double distanceSq = this.owlbear.squaredDistanceTo(target);
        return distanceSq >= MIN_RANGE * MIN_RANGE && distanceSq <= MAX_RANGE * MAX_RANGE
                && Math.abs(target.getY() - this.owlbear.getY()) < 3.0D;
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.done = false;
        this.owlbear.getNavigation().stop();
        this.owlbear.triggerAnim("attack_controller", OwlbearEntity.ROAR);
        this.owlbear.playSound(SoundEvents.ENTITY_POLAR_BEAR_WARNING, 2.0F, 0.6F);
    }

    @Override
    public boolean shouldContinue() {
        return !this.done && !this.owlbear.isHugging() && !this.owlbear.isStunned();
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.ticks++;
        LivingEntity target = this.owlbear.getTarget();
        if (this.ticks <= WINDUP_TICKS) {
            this.windUp(target);
        } else {
            this.charge(target);
        }
    }

    private void windUp(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            this.done = true;
            return;
        }
        this.owlbear.getNavigation().stop();
        this.owlbear.getLookControl().lookAt(target, 30.0F, 30.0F);
        faceTowards(target.getPos());
        if (this.ticks % 5 == 0) {
            // Pawing the ground.
            ServerWorld world = (ServerWorld) this.owlbear.getWorld();
            world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK,
                            world.getBlockState(this.owlbear.getBlockPos().down())),
                    this.owlbear.getX(), this.owlbear.getY() + 0.1D, this.owlbear.getZ(), 8, 0.5D, 0.05D, 0.5D, 0.1D);
        }
        if (this.ticks == WINDUP_TICKS) {
            Vec3d to = target.getPos().subtract(this.owlbear.getPos()).multiply(1.0D, 0.0D, 1.0D);
            if (to.lengthSquared() < 1.0E-4) {
                this.done = true;
                return;
            }
            this.direction = to.normalize();
            this.owlbear.setCharging(true);
            this.owlbear.playSound(SoundEvents.ENTITY_RAVAGER_ROAR, 1.5F, 1.1F);
        }
    }

    private void charge(LivingEntity target) {
        if (this.ticks > WINDUP_TICKS + MAX_CHARGE_TICKS) {
            this.done = true;
            return;
        }
        faceTowards(this.owlbear.getPos().add(this.direction));
        Vec3d velocity = this.owlbear.getVelocity();
        this.owlbear.setVelocity(this.direction.x * CHARGE_SPEED, velocity.y, this.direction.z * CHARGE_SPEED);

        // Anything in its path: the target, or any other living thing that's not an owlbear.
        for (LivingEntity hit : this.owlbear.getWorld().getEntitiesByClass(LivingEntity.class,
                this.owlbear.getBoundingBox().expand(0.4D), e -> e != this.owlbear && e.isAlive()
                        && !(e instanceof OwlbearEntity) && !e.isSpectator()
                        && (e == target || !this.owlbear.isTeammate(e)))) {
            this.ram(hit);
            this.done = true;
            return;
        }

        if (this.owlbear.horizontalCollision && this.ticks > WINDUP_TICKS + 2) {
            // Head first into a tree or wall.
            this.owlbear.playSound(SoundEvents.ENTITY_GENERIC_BIG_FALL, 1.5F, 0.6F);
            ((ServerWorld) this.owlbear.getWorld()).spawnParticles(ParticleTypes.POOF, this.owlbear.getX(),
                    this.owlbear.getEyeY(), this.owlbear.getZ(), 10, 0.5D, 0.3D, 0.5D, 0.02D);
            this.owlbear.stun(STUN_TICKS);
            this.done = true;
        }
    }

    private void ram(LivingEntity hit) {
        float damage = (float) this.owlbear.getAttributeValue(
                net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE) * DAMAGE_MULTIPLIER;
        if (hit.damage(this.owlbear.getDamageSources().mobAttack(this.owlbear), damage)) {
            this.owlbear.applyDamageEffects(this.owlbear, hit);
            hit.takeKnockback(KNOCKBACK, -this.direction.x, -this.direction.z);
            hit.addVelocity(0.0D, 0.4D, 0.0D);
            hit.velocityModified = true;
        }
        this.owlbear.playSound(SoundEvents.ENTITY_RAVAGER_ATTACK, 1.5F, 0.8F);
        this.owlbear.triggerAnim("attack_controller", "swipe");
    }

    private void faceTowards(Vec3d pos) {
        double dx = pos.x - this.owlbear.getX();
        double dz = pos.z - this.owlbear.getZ();
        float yaw = (float) (MathHelper.atan2(dz, dx) * MathHelper.DEGREES_PER_RADIAN) - 90.0F;
        this.owlbear.setYaw(yaw);
        this.owlbear.setBodyYaw(yaw);
        this.owlbear.setHeadYaw(yaw);
    }

    @Override
    public void stop() {
        this.owlbear.setCharging(false);
        this.owlbear.setVelocity(this.owlbear.getVelocity().multiply(0.3D, 1.0D, 0.3D));
        this.nextChargeTime = this.owlbear.getWorld().getTime() + COOLDOWN;
    }
}
