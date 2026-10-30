package mattonfire.dnd.entity.dragon;

import java.util.EnumSet;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * An Ancient dragon's fight (design 10, 5.10), in three modes:
 * <ul>
 * <li><b>Air</b>: circles its target {@value #CIRCLE_RADIUS} blocks out and {@value #CIRCLE_HEIGHT} up. With its
 * breath charged it comes in lower and closer, and breathes once the target is within {@value #BREATH_START}
 * blocks. After {@value #AIR_TICKS} ticks, or as soon as the target is under a roof, it lands.</li>
 * <li><b>Landing</b>: drops beside the target; touching down counts as "just landed" for its wing attack.</li>
 * <li><b>Ground</b>: closes in and bites within reach, breathes at range when charged, for
 * {@value #GROUND_TICKS} ticks ({@value #GROUND_TICKS_LATER} from its second phase), then takes off again.</li>
 * </ul>
 * Legendary actions ({@link LegendaryActions}) go between its attacks in every mode.
 */
public class AncientDragonAttackGoal extends Goal {
    public static final double CIRCLE_RADIUS = 24.0;
    public static final double CIRCLE_HEIGHT = 16.0;
    public static final double DIVE_RADIUS = 14.0;
    public static final double DIVE_HEIGHT = 9.0;
    /** Starts its breath within this many blocks of the target (the flame reaches 28 from the mouth). */
    public static final double BREATH_START = 24.0;
    public static final int AIR_TICKS = 400;
    public static final int GROUND_TICKS = 200;
    public static final int GROUND_TICKS_LATER = 300;
    private static final int LANDING_TIMEOUT = 160;
    /** No sky within this many blocks above the target: it can't be reached from the air. */
    private static final int ROOF_CHECK = 6;

    private enum Mode { AIR, LANDING, GROUND }

    private final AncientDragonEntity dragon;
    private Mode mode = Mode.AIR;
    private int modeTicks;
    private int cooldown;
    private double circleAngle;
    private boolean justLanded;

    public AncientDragonAttackGoal(AncientDragonEntity dragon) {
        this.dragon = dragon;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        LivingEntity target = this.dragon.getTarget();
        return target != null && target.isAlive() && !this.dragon.isReturningHome();
    }

    @Override
    public void start() {
        this.setMode(this.dragon.isOnGround() ? Mode.GROUND : Mode.AIR);
        this.cooldown = 0;
        LivingEntity target = this.dragon.getTarget();
        if (target != null) {
            Vec3d from = this.dragon.getPos().subtract(target.getPos());
            this.circleAngle = Math.atan2(from.z, from.x);
        }
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    private void setMode(Mode mode) {
        this.mode = mode;
        this.modeTicks = 0;
    }

    @Override
    public void tick() {
        LivingEntity target = this.dragon.getTarget();
        if (target == null) {
            return;
        }
        this.dragon.getLookControl().lookAt(target, 30.0F, 30.0F);
        if (this.dragon.getFireBreath().isBreathing()) {
            // Hold still and keep facing the target while it breathes
            this.dragon.getMoveControl().moveTo(this.dragon.getX(), this.dragon.getY(), this.dragon.getZ(), 0.3);
            return;
        }
        this.modeTicks++;
        if (this.cooldown > 0) {
            this.cooldown--;
        }
        if (this.dragon.legendaryActions().tryUse(this.dragon, this.justLanded)) {
            this.justLanded = false;
            if (this.mode == Mode.GROUND && !this.dragon.isOnGround()) {
                this.setMode(Mode.AIR); // the wing attack threw it into the air
            }
            return;
        }
        this.justLanded = false;

        double distance = this.dragon.distanceTo(target);
        boolean canSee = this.dragon.getVisibilityCache().canSee(target);
        if (this.cooldown <= 0 && distance <= this.dragon.getBiteRange()) {
            this.dragon.tryAttack(target);
            this.cooldown = this.dragon.getMeleeCooldown();
        }
        switch (this.mode) {
            case AIR -> this.air(target, distance, canSee);
            case LANDING -> this.landing(target);
            case GROUND -> this.ground(target, distance, canSee);
        }
    }

    private void air(LivingEntity target, double distance, boolean canSee) {
        if (this.modeTicks >= AIR_TICKS || underRoof(target)) {
            this.setMode(Mode.LANDING);
            return;
        }
        boolean charged = this.dragon.isBreathCharged();
        if (charged && canSee && distance <= BREATH_START && this.cooldown <= 0 && this.dragon.breathe(target)) {
            this.cooldown = 20;
            return;
        }
        this.circleAngle += charged ? 0.02 : 0.035;
        double radius = charged ? DIVE_RADIUS : CIRCLE_RADIUS;
        double height = charged ? DIVE_HEIGHT : CIRCLE_HEIGHT;
        this.dragon.getMoveControl().moveTo(target.getX() + Math.cos(this.circleAngle) * radius,
                target.getY() + height, target.getZ() + Math.sin(this.circleAngle) * radius, 1.0);
    }

    private void landing(LivingEntity target) {
        if (this.dragon.isOnGround() || this.modeTicks >= LANDING_TIMEOUT) {
            this.justLanded = this.dragon.isOnGround();
            this.setMode(Mode.GROUND);
            return;
        }
        // Down beside the target, on the side it's coming from
        Vec3d away = this.dragon.getPos().subtract(target.getPos()).multiply(1, 0, 1);
        away = away.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : away.normalize();
        Vec3d spot = target.getPos().add(away.multiply(8.0));
        this.dragon.getMoveControl().moveTo(spot.x, target.getY() - 1.0, spot.z, 1.0);
    }

    private void ground(LivingEntity target, double distance, boolean canSee) {
        int groundTicks = this.dragon.isMountainPhase() ? GROUND_TICKS_LATER : GROUND_TICKS;
        if (this.modeTicks >= groundTicks && !underRoof(target)) {
            this.dragon.takeOff(1.0);
            this.setMode(Mode.AIR);
            return;
        }
        if (this.dragon.isBreathCharged() && canSee && distance > this.dragon.getBiteRange()
                && distance <= BREATH_START && this.cooldown <= 0 && this.dragon.breathe(target)) {
            this.cooldown = 20;
            return;
        }
        if (distance > this.dragon.getBiteRange() - 2.0) {
            this.dragon.getMoveControl().moveTo(target.getX(), target.getY(), target.getZ(), 0.8);
        }
    }

    /** Something solid within {@value #ROOF_CHECK} blocks above the target's head. */
    private static boolean underRoof(LivingEntity target) {
        BlockPos head = BlockPos.ofFloored(target.getX(), target.getEyeY(), target.getZ());
        for (int i = 1; i <= ROOF_CHECK; i++) {
            BlockPos pos = head.up(i);
            if (target.getWorld().getBlockState(pos).getMaterial().blocksMovement()) {
                return true;
            }
        }
        return false;
    }
}
