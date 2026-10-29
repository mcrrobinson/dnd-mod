package mattonfire.dnd.entity.ai.goal;

import java.util.EnumSet;
import java.util.function.Predicate;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.faction.ReputationTier;
import mattonfire.dnd.faction.TierEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.TrackTargetGoal;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

/**
 * A faction member joins the fight of a friend of its faction (a player whose standing passes {@code friend},
 * e.g. Exalted) within {@code range} blocks. The foe is a hostile mob (or any mob going for the friend) that hit
 * the friend, that the friend hit, in the last {@link TierEffects#ALLY_FIGHT_TICKS}, or that has the friend as its
 * target. Never another player, one of the member's own kin ({@code kin}), the friend's own pet, or a creeper.
 * The dwarves ({@link DwarfFriendGoal}) and the kobolds use it.
 */
public class FactionAllyGoal extends TrackTargetGoal {
    /** canStart calls between looks round for a friend in a fight (it's asked every other tick). */
    private static final int CHECK_INTERVAL = 5;
    private final double range;
    private final Predicate<ReputationTier> friend;
    private final Predicate<LivingEntity> kin;
    private int cooldown;
    @Nullable
    private LivingEntity foe;
    @Nullable
    private PlayerEntity friendPlayer;

    public FactionAllyGoal(MobEntity mob, double range, Predicate<ReputationTier> friend, Predicate<LivingEntity> kin) {
        super(mob, false);
        this.range = range;
        this.friend = friend;
        this.kin = kin;
        this.setControls(EnumSet.of(Control.TARGET));
    }

    @Override
    public boolean canStart() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        this.cooldown = CHECK_INTERVAL;
        for (PlayerEntity player : this.mob.world.getEntitiesByClass(PlayerEntity.class,
                this.mob.getBoundingBox().expand(this.range), p -> p.isAlive() && !p.isSpectator())) {
            if (!this.friend.test(TierEffects.tierWith(player, this.mob))) {
                continue;
            }
            LivingEntity foe = this.foeOf(player);
            if (foe != null) {
                this.friendPlayer = player;
                this.foe = foe;
                return true;
            }
        }
        return false;
    }

    @Nullable
    private LivingEntity foeOf(PlayerEntity player) {
        LivingEntity attacker = player.getAttacker();
        if (attacker != null && player.age - player.getLastAttackedTime() < TierEffects.ALLY_FIGHT_TICKS
                && this.isFoe(attacker, player)) {
            return attacker;
        }
        LivingEntity struck = player.getAttacking();
        if (struck != null && player.age - player.getLastAttackTime() < TierEffects.ALLY_FIGHT_TICKS
                && this.isFoe(struck, player)) {
            return struck;
        }
        for (MobEntity mob : this.mob.world.getEntitiesByClass(MobEntity.class,
                this.mob.getBoundingBox().expand(this.range), m -> m.getTarget() == player)) {
            if (this.isFoe(mob, player)) {
                return mob;
            }
        }
        return null;
    }

    private boolean isFoe(LivingEntity entity, PlayerEntity friend) {
        if (!entity.isAlive() || entity instanceof PlayerEntity || this.kin.test(entity)
                || entity instanceof CreeperEntity || entity == this.mob
                || entity instanceof TameableEntity pet && pet.isOwner(friend)
                || this.mob.squaredDistanceTo(entity) > this.range * this.range) {
            return false;
        }
        return entity instanceof Monster || entity instanceof MobEntity mob && mob.getTarget() == friend;
    }

    @Override
    public void start() {
        this.mob.setTarget(this.foe);
        if (this.foe != null && this.friendPlayer != null) {
            DnDClasses.LOGGER.info("[Reputation] {} joins {} against {}", this.mob.getName().getString(),
                    this.friendPlayer.getEntityName(), this.foe.getType().getUntranslatedName());
        }
        super.start();
    }
}
