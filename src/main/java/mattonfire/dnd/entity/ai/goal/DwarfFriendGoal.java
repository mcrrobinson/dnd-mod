package mattonfire.dnd.entity.ai.goal;

import java.util.EnumSet;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.MountainDwarfEntity;
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
 * Exalted with the dwarves: a dwarf joins the fight of a friend of the mountain within
 * {@link TierEffects#DWARF_ALLY_RANGE} blocks. The foe is a hostile mob (or any mob going for the friend)
 * that hit the friend, that the friend hit, in the last {@link TierEffects#ALLY_FIGHT_TICKS}, or that has
 * the friend as its target. Never another player, a dwarf, the friend's own pet, or a creeper.
 */
public class DwarfFriendGoal extends TrackTargetGoal {
    /** canStart calls between looks round for a friend in a fight (it's asked every other tick). */
    private static final int CHECK_INTERVAL = 5;
    private int cooldown;
    @Nullable
    private LivingEntity foe;
    @Nullable
    private PlayerEntity friend;

    public DwarfFriendGoal(MountainDwarfEntity dwarf) {
        super(dwarf, false);
        this.setControls(EnumSet.of(Control.TARGET));
    }

    @Override
    public boolean canStart() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        this.cooldown = CHECK_INTERVAL;
        double range = TierEffects.DWARF_ALLY_RANGE;
        for (PlayerEntity player : this.mob.world.getEntitiesByClass(PlayerEntity.class,
                this.mob.getBoundingBox().expand(range), p -> p.isAlive() && !p.isSpectator())) {
            if (!TierEffects.dwarfFriend(TierEffects.tierWith(player, this.mob))) {
                continue;
            }
            LivingEntity foe = this.foeOf(player);
            if (foe != null) {
                this.friend = player;
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
                this.mob.getBoundingBox().expand(TierEffects.DWARF_ALLY_RANGE), m -> m.getTarget() == player)) {
            if (this.isFoe(mob, player)) {
                return mob;
            }
        }
        return null;
    }

    private boolean isFoe(LivingEntity entity, PlayerEntity friend) {
        if (!entity.isAlive() || entity instanceof PlayerEntity || entity instanceof MountainDwarfEntity
                || entity instanceof CreeperEntity || entity == this.mob
                || entity instanceof TameableEntity pet && pet.isOwner(friend)
                || this.mob.squaredDistanceTo(entity) > TierEffects.DWARF_ALLY_RANGE * TierEffects.DWARF_ALLY_RANGE) {
            return false;
        }
        return entity instanceof Monster || entity instanceof MobEntity mob && mob.getTarget() == friend;
    }

    @Override
    public void start() {
        this.mob.setTarget(this.foe);
        if (this.foe != null && this.friend != null) {
            DnDClasses.LOGGER.info("[Reputation] {} joins {} against {}", this.mob.getName().getString(),
                    this.friend.getEntityName(), this.foe.getType().getUntranslatedName());
        }
        super.start();
    }
}
