package mattonfire.dnd.entity;

import mattonfire.dnd.classes.Party.PartyManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;

/** Shared rules for what a tamed Wyvern or Lightning Chaser may attack. */
final class TamedDragons {
    private TamedDragons() {
    }

    /** Like a wolf's, plus the owner's party: never PvP-blocked players, the owner's pets or tamed horses. */
    static boolean canAttackWithOwner(LivingEntity target, LivingEntity owner) {
        if (target instanceof CreeperEntity || target instanceof GhastEntity) {
            return false;
        }
        if (target instanceof TameableEntity tameable) {
            return !tameable.isTamed() || tameable.getOwner() != owner;
        }
        if (target instanceof PlayerEntity player) {
            if (PartyManager.areInSameParty(player, owner)) {
                return false;
            }
            if (owner instanceof PlayerEntity ownerPlayer && !ownerPlayer.shouldDamagePlayer(player)) {
                return false;
            }
        }
        return !(target instanceof AbstractHorseEntity horse && horse.isTame());
    }

    /** A tamed dragon never turns on its owner or the owner's party, even when one of them hits it. */
    static boolean isFriend(TameableEntity dragon, LivingEntity target) {
        if (!dragon.isTamed()) {
            return false;
        }
        LivingEntity owner = dragon.getOwner();
        return dragon.isOwner(target) || owner != null && PartyManager.areInSameParty(owner, target);
    }
}
