package mattonfire.dnd.entity;

import net.minecraft.entity.Entity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;

/**
 * An animation controller whose looping animations run on the world clock instead of from whenever
 * this client started them: animation time = (world time + partial tick) mod length. The server poses
 * dragon parts with the same formula (DragonAnimations.Animation.phase), so the hit shapes it uses
 * flap in step with what players see.
 */
public class DragonAnimationController<T extends Entity & GeoAnimatable> extends AnimationController<T> {
    private float partialTick;

    public DragonAnimationController(T animatable, String name, AnimationStateHandler<T> handler) {
        super(animatable, name, 0, handler);
    }

    @Override
    protected PlayState handleAnimationState(AnimationState<T> state) {
        this.partialTick = state.getPartialTick();
        return super.handleAnimationState(state);
    }

    @Override
    protected double adjustTick(double tick) {
        if (this.animationState == State.RUNNING && this.currentAnimation != null
                && this.currentAnimation.animation().length() > 0) {
            this.shouldResetTick = false;
            double length = this.currentAnimation.animation().length();
            double time = this.animatable.world.getTime() + this.partialTick;
            return length >= Double.MAX_VALUE / 2 ? time : time % length;
        }
        return super.adjustTick(tick);
    }
}
