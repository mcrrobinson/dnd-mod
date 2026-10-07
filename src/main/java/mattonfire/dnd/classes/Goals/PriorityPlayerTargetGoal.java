package mattonfire.dnd.classes.Goals;

import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.mixin.ActiveTargetGoalAccessor;
import mattonfire.dnd.classes.mixin.MobEntityAccessor;

/**
 * Fighter "attracts mobs": mobs that would already target players switch to
 * the nearest Fighter in range. Only targets Fighters; other players are left
 * to the mob's own target goals.
 */
public class PriorityPlayerTargetGoal extends ActiveTargetGoal<PlayerEntity> {

    // Runs ahead of vanilla target goals (revenge/player goals sit at 1-2), so
    // a mob already chasing someone else switches to the Fighter.
    private static final int PRIORITY = 0;

    public PriorityPlayerTargetGoal(MobEntity mob, TargetPredicate template) {
        super(mob, PlayerEntity.class, true);
        // Reuse the mob's own player predicate so e.g. zombified piglins and
        // polar bears only go for Fighters they are already angry at.
        this.targetPredicate = template;
    }

    /**
     * Adds the goal to mobs that already have a player-targeting goal. Passive
     * mobs (animals, villagers, iron golems...) have none and are skipped.
     */
    public static void attach(MobEntity mob) {
        if (mob instanceof EndermanEntity) {
            return; // Endermen only aggro when stared at
        }
        GoalSelector targetSelector = ((MobEntityAccessor) mob).getTargetSelector();
        TargetPredicate template = null;
        for (PrioritizedGoal prioritized : targetSelector.getGoals()) {
            if (prioritized.getGoal() instanceof PriorityPlayerTargetGoal) {
                return; // Already attached
            }
            if (template == null && prioritized.getGoal() instanceof ActiveTargetGoal<?> goal
                    && ((ActiveTargetGoalAccessor) goal).getTargetClass().isAssignableFrom(PlayerEntity.class)) {
                template = ((ActiveTargetGoalAccessor) goal).getTargetPredicate();
            }
        }
        if (template != null) {
            targetSelector.add(PRIORITY, new PriorityPlayerTargetGoal(mob, template));
        }
    }

    @Override
    protected void findClosestTarget() {
        this.targetEntity = this.mob.getWorld().getClosestEntity(
                this.mob.getWorld().getEntitiesByClass(PlayerEntity.class,
                        this.getSearchBox(this.getFollowRange()),
                        PriorityPlayerTargetGoal::isFighter),
                this.targetPredicate,
                this.mob,
                this.mob.getX(),
                this.mob.getEyeY(),
                this.mob.getZ());
    }

    private static boolean isFighter(PlayerEntity player) {
        return player instanceof PlayerEntityExt ext && ext.getDndClass() == DndCharacter.FIGHTER;
    }
}
