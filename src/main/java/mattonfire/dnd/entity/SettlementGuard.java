package mattonfire.dnd.entity;

import java.util.function.Predicate;
import mattonfire.dnd.faction.TierEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * A settlement guard (mountain dwarf, Elf Warden, Dragonborn Templar, Tiefling Exile, orc, Clockwork Sentinel):
 * it holds a grudge against a player ({@link Angerable}) when {@link #provoke provoked} by a settlement's law
 * ({@link SettlementGrudges}) or a struck neighbour ({@link SettlementNpcEntity#defend}), and
 * {@link SettlementGrudges#forgive} can drop it.
 */
public interface SettlementGuard extends Angerable {
    /**
     * This guard saw {@code player} wrong its settlement, or strike one of its folk, and turns on them, with a
     * {@link #provokeShout()} if it wasn't angry already.
     */
    default void provoke(PlayerEntity player) {
        if (!this.shouldAngerAt(player)) {
            this.provokeShout();
        }
        this.setAngryAt(player.getUuid());
        this.chooseRandomAngerTime();
        this.setTarget(player);
    }

    /** The guard's shout (or growl, or bell) as it turns on a player. */
    void provokeShout();

    /** Whether {@code guard} is angry at {@code entity} on sight: a player Hostile with its faction. */
    static boolean attacksOnSight(MobEntity guard, LivingEntity entity) {
        return entity instanceof PlayerEntity player && guard.canTarget(player)
                && TierEffects.attacksOnSight(TierEffects.tierWith(player, guard));
    }

    /**
     * The usual guard targets: whoever strikes one of its {@code kin} (the whole group joins in), players it's angry
     * at ({@link #shouldAngerAt}), and {@code prey} (the monsters it hunts).
     */
    static <G extends PathAwareEntity & SettlementGuard> void addTargetGoals(GoalSelector targetSelector, G guard,
            Class<? extends LivingEntity> kin, Predicate<LivingEntity> prey) {
        targetSelector.add(1, new RevengeGoal(guard, kin).setGroupRevenge());
        targetSelector.add(2, new ActiveTargetGoal<>(guard, PlayerEntity.class, 10, true, false, guard::shouldAngerAt));
        targetSelector.add(3, new ActiveTargetGoal<>(guard, MobEntity.class, 5, true, false, prey));
    }

    /** Whom such a guard fights: players who struck it or that it's angry at, and anyone but its own {@code kin}. */
    static <G extends MobEntity & SettlementGuard> boolean mayTarget(G guard, LivingEntity target,
            Class<? extends LivingEntity> kin) {
        if (target instanceof PlayerEntity player) {
            return player == guard.getAttacker() || guard.shouldAngerAt(player);
        }
        return !kin.isInstance(target);
    }
}
