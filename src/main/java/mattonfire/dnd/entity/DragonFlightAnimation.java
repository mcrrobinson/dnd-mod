package mattonfire.dnd.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Vec3d;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;

/**
 * Picks a flying dragon's body animation (idle, walk, fly.idle or fly.straight) on the server and
 * syncs it, so the client plays the same one the server poses the hit parts with.
 */
public final class DragonFlightAnimation {
    public static final String[] NAMES = {"idle", "walk", "fly.idle", "fly.straight"};
    private static final byte IDLE = 0, WALK = 1, FLY_IDLE = 2, FLY_STRAIGHT = 3;
    private static final RawAnimation[] ANIMATIONS = new RawAnimation[NAMES.length];
    /** Ticks a different flight animation must be wanted before it's switched to, so it doesn't flicker. */
    private static final int FLY_SWITCH_DELAY = 20;
    /** GeckoLib's default motion threshold (GeoEntityRenderer.getMotionAnimThreshold). */
    private static final double MOTION_THRESHOLD = 0.015;

    static {
        for (int i = 0; i < NAMES.length; i++) {
            ANIMATIONS[i] = RawAnimation.begin().thenLoop(NAMES[i]);
        }
    }

    private final MobEntity mob;
    private final TrackedData<Byte> data;
    private int switchTimer;

    public DragonFlightAnimation(MobEntity mob, TrackedData<Byte> data) {
        this.mob = mob;
        this.data = data;
    }

    public static void track(DataTracker tracker, TrackedData<Byte> data) {
        tracker.startTracking(data, IDLE);
    }

    /** Call every tick; only the server chooses. */
    public void tick(boolean flying) {
        if (this.mob.world.isClient) {
            return;
        }
        // Moving the way GeckoLib's renderer decides it (velocity and limb swing)
        Vec3d velocity = this.mob.getVelocity();
        boolean moving = Math.abs(velocity.x) + Math.abs(velocity.z) / 2 >= MOTION_THRESHOLD
                && this.mob.limbAnimator.getSpeed() != 0;
        byte current = this.mob.getDataTracker().get(this.data);
        byte desired = flying ? (moving ? FLY_STRAIGHT : FLY_IDLE) : (moving ? WALK : IDLE);
        if (desired == current) {
            this.switchTimer = 0;
            return;
        }
        if (current >= FLY_IDLE && desired >= FLY_IDLE && ++this.switchTimer < FLY_SWITCH_DELAY) {
            return;
        }
        this.switchTimer = 0;
        this.mob.getDataTracker().set(this.data, desired);
    }

    public String current() {
        byte index = this.mob.getDataTracker().get(this.data);
        return NAMES[Math.max(0, Math.min(NAMES.length - 1, index))];
    }

    /** The body animation controller, on the world clock (see DragonAnimationController). */
    public <T extends Entity & GeoAnimatable> AnimationController<T> createController(T animatable) {
        AnimationController<T> controller = new DragonAnimationController<>(animatable, "controller", state -> {
            byte index = this.mob.getDataTracker().get(this.data);
            return state.setAndContinue(ANIMATIONS[Math.max(0, Math.min(ANIMATIONS.length - 1, index))]);
        });
        return controller;
    }
}
