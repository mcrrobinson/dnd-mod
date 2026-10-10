package mattonfire.dnd.entity;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Party.PartyManager;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.faction.ReputationTier;
import mattonfire.dnd.faction.TierEffects;
import mattonfire.dnd.world.gen.stronghold.BloodSworn;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.Difficulty;

/**
 * Who the Bloodtusk Horde fights. Orcs attack players Hostile with the horde (everyone but Half-Orcs starts
 * there) on sight: within {@link #SIGHT_RANGE} blocks, or {@link #BARBARIAN_RANGE} for Barbarians, whom they
 * respect. Players at Unfriendly or better are tolerated, as are players who are Blood-Sworn at the orc's
 * hold and anyone in their party within {@link #PARTY_RANGE} blocks of them. Anyone who strikes an orc is
 * fought regardless (the orcs' anger, {@link OrcEntity#provoke}). A player in the Trial of Strength is left to
 * the Champion. Goblins are allies: neither side targets the other.
 */
public final class OrcHostility {
    public static final double SIGHT_RANGE = 16.0D;
    public static final double BARBARIAN_RANGE = 8.0D;
    /** Party members this close to a Blood-Sworn player are tolerated. */
    public static final double PARTY_RANGE = 8.0D;

    private OrcHostility() {
    }

    /** Goblins and other orcs: never targeted, never hurt. */
    public static boolean isAlly(Entity entity) {
        return entity instanceof OrcEntity || entity instanceof GoblinWarriorEntity;
    }

    /** The horde's enemies, hunted near home: hobbits, dwarves, elves and villagers. */
    public static boolean isHordeEnemy(LivingEntity entity) {
        return entity instanceof HobbitEntity || entity instanceof MountainDwarfEntity || entity instanceof ElfEntity
                || OrcEntity.isVillager(entity);
    }

    /** Whether {@code orc} goes for {@code player} on sight (its target goal). */
    public static boolean attacksOnSight(OrcEntity orc, PlayerEntity player) {
        if (player.isCreative() || player.isSpectator() || orc.world.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        if (orc instanceof OrcChampionEntity champion) {
            return champion.isDuelist(player);
        }
        if (orc instanceof OrcQuartermasterEntity || OrcTrial.isDueling(player) || tolerated(orc, player)) {
            return false;
        }
        double range = isBarbarian(player) ? BARBARIAN_RANGE : SIGHT_RANGE;
        return orc.squaredDistanceTo(player) <= range * range;
    }

    /**
     * Whether {@code orc} may fight {@code player} at all: on sight, or because the player wronged it.
     * {@code engaged} skips the sight range, for an orc already fighting the player.
     */
    public static boolean mayFight(OrcEntity orc, PlayerEntity player, boolean engaged) {
        if (orc instanceof OrcChampionEntity champion) {
            return champion.isDuelist(player) || !champion.inDuel() && (player == orc.getAttacker() || orc.shouldAngerAt(player));
        }
        if (OrcTrial.isDueling(player)) {
            return false;
        }
        if (player == orc.getAttacker() || orc.shouldAngerAt(player)) {
            return true;
        }
        if (engaged && !player.isCreative() && !player.isSpectator() && !(orc instanceof OrcQuartermasterEntity)
                && orc.world.getDifficulty() != Difficulty.PEACEFUL) {
            return !tolerated(orc, player);
        }
        return attacksOnSight(orc, player);
    }

    /** Unfriendly or better with the horde, Blood-Sworn at this hold, or near a Blood-Sworn party member. */
    public static boolean tolerated(OrcEntity orc, PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer) || !(orc.world instanceof ServerWorld world)) {
            return true;
        }
        if (TierEffects.tierWith(player, orc) != ReputationTier.HOSTILE) {
            return true;
        }
        long hold = orc.getHold();
        if (hold == OrcEntity.NO_HOLD) {
            return false;
        }
        BloodSworn sworn = BloodSworn.get(world);
        if (sworn.isSworn(hold, player.getUuid())) {
            return true;
        }
        for (ServerPlayerEntity member : PartyManager.nearbyMembers(serverPlayer, PARTY_RANGE)) {
            if (member != serverPlayer && sworn.isSworn(hold, member.getUuid())) {
                return true;
            }
        }
        return false;
    }

    static boolean isBarbarian(PlayerEntity player) {
        return player instanceof PlayerEntityExt ext && ext.getDndClass() == DndCharacter.BARBARIAN;
    }
}
