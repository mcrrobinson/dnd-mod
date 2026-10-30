package mattonfire.dnd.entity.kobold.ai;

import java.util.EnumSet;
import mattonfire.dnd.entity.kobold.KoboldSlingerEntity;
import mattonfire.dnd.entity.kobold.KoboldVolleys;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

/**
 * A Kobold Slinger's fight: it keeps its target {@value #BRAVE_MIN}-{@value #RANGE} blocks off while brave
 * (backing off to about {@value #BACK_OFF} when it closes in), or {@link KoboldKeepDistanceGoal#MIN}-
 * {@link KoboldKeepDistanceGoal#MAX} while craven, and slings whenever it's ready and sees the target, calling a
 * volley ({@link KoboldVolleys}). Cornered (under 2 blocks) and brave, it stabs with its dagger.
 */
public class KoboldSlingGoal extends Goal {
    public static final double BRAVE_MIN = 6.0D;
    public static final double BACK_OFF = 8.0D;
    public static final double RANGE = 16.0D;
    private final KoboldSlingerEntity slinger;
    private int repath;
    private int stabCooldown;

    public KoboldSlingGoal(KoboldSlingerEntity slinger) {
        this.slinger = slinger;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        LivingEntity target = this.slinger.getTarget();
        return target != null && target.isAlive() && this.slinger.scaredOf() == null;
    }

    @Override
    public void start() {
        this.repath = 0;
    }

    @Override
    public void stop() {
        this.slinger.getNavigation().stop();
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.slinger.getTarget();
        if (target == null) {
            return;
        }
        this.slinger.getLookControl().lookAt(target, 30.0F, 30.0F);
        double distance = this.slinger.distanceTo(target);
        boolean sees = this.slinger.getVisibilityCache().canSee(target);
        boolean brave = this.slinger.wantsMelee();
        double min = brave ? BRAVE_MIN : KoboldKeepDistanceGoal.MIN;
        double max = brave ? RANGE : KoboldKeepDistanceGoal.MAX;

        if (--this.stabCooldown <= 0 && brave && distance < 2.0D) {
            this.stabCooldown = 20;
            this.slinger.swingHand(Hand.MAIN_HAND);
            this.slinger.triggerAnim("action_controller", "attack");
            this.slinger.tryAttack(target);
        }
        if (--this.repath <= 0) {
            this.repath = 10;
            if (distance < min) {
                Vec3d away = NoPenaltyTargeting.findFrom(this.slinger, (int) (brave ? BACK_OFF : min) + 4, 5, target.getPos());
                if (away != null) {
                    this.slinger.getNavigation().startMovingTo(away.x, away.y, away.z, 1.2D);
                }
            } else if (distance > max || !sees) {
                this.slinger.getNavigation().startMovingTo(target, 1.0D);
            } else {
                this.slinger.getNavigation().stop();
            }
        }
        if (sees && distance <= RANGE && this.slinger.isReady()) {
            KoboldVolleys.call(this.slinger, target);
        }
    }
}
