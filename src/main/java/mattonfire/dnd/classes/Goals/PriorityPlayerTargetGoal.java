package mattonfire.dnd.classes.Goals;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;

public class PriorityPlayerTargetGoal<T extends LivingEntity> extends ActiveTargetGoal<T> {

    public PriorityPlayerTargetGoal(MobEntity mob, Class<T> targetClass) {
        super(mob, targetClass, true);
    }

    @Override
    protected void findClosestTarget() {
        // If it's a player-based target
        if (this.targetClass == PlayerEntity.class || this.targetClass == ServerPlayerEntity.class) {
            List<ServerPlayerEntity> players = this.mob.getWorld().getEntitiesByClass(
                    ServerPlayerEntity.class,
                    this.getSearchBox(this.getFollowRange()),
                    player -> this.targetPredicate.test(this.mob, player));

            if (players.isEmpty()) {
                this.targetEntity = null;
                return;
            }

            // Prioritize specific players (e.g., username, team, item, etc.)
            ServerPlayerEntity priorityTarget = null;
            for (ServerPlayerEntity player : players) {
                if (isPriorityPlayer(player)) {
                    priorityTarget = player;
                    break; // Stop at first match
                }
            }

            // If no priority players found, use default closest player selection
            if (priorityTarget != null) {
                this.targetEntity = priorityTarget;
            } else {
                this.targetEntity = this.mob.getWorld().getClosestPlayer(this.targetPredicate, this.mob,
                        this.mob.getX(), this.mob.getEyeY(), this.mob.getZ());
            }
        } else {
            // Default behavior for non-player targets
            this.targetEntity = this.mob.getWorld().getClosestEntity(
                    this.mob.getWorld().getEntitiesByClass(this.targetClass,
                            this.getSearchBox(this.getFollowRange()),
                            (livingEntity) -> true),
                    this.targetPredicate,
                    this.mob,
                    this.mob.getX(),
                    this.mob.getEyeY(),
                    this.mob.getZ());
        }
    }

    private boolean isPriorityPlayer(ServerPlayerEntity player) {
        if (player instanceof PlayerEntityExt) {
            PlayerEntityExt playerEntity = (PlayerEntityExt) player;
            return playerEntity.getDndClass() == DndCharacter.FIGHTER;
        }
        return false;
    }
}
