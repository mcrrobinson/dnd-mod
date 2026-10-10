package mattonfire.dnd.faction;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

/**
 * What a reputation tier does to the NPCs of a faction: prices, gifts, barter, hostility and raids.
 * The numbers live here so every NPC reads them the same way; the NPC classes only ask.
 *
 * <p>Everything is per player: an NPC looks up the standing of the player it's dealing with (or looking
 * at) with its own faction ({@link Reputation#factionOf}). An NPC with no faction, a client-side call
 * or a player in DM mode counts as {@link ReputationTier#NEUTRAL}, i.e. today's behaviour.
 */
public final class TierEffects {
    /** Goblin standing at or below this is "Marked": raids come twice as often and raiders go for you first. */
    public static final int MARKED = -800;
    /** At goblin Unfriendly, camp goblins and raiders only notice you this close (unless provoked). */
    public static final double PARLEY_RANGE = 6.0D;
    /** How long after hitting a member a player still counts as having provoked its kin. */
    public static final int PROVOKED_TICKS = 20 * 10;

    private TierEffects() {
    }

    // ---- Looking up the tier ----

    /** {@code player}'s tier with {@code npc}'s faction (Neutral without one). */
    public static ReputationTier tierWith(PlayerEntity player, Entity npc) {
        return tierWith(player, Reputation.factionOf(npc));
    }

    /** {@code player}'s tier with the faction whose members include {@code type} (Neutral without one). */
    public static ReputationTier tierWith(PlayerEntity player, EntityType<?> type) {
        return tierWith(player, factionOfType(type));
    }

    public static ReputationTier tierWith(PlayerEntity player, @Nullable Faction faction) {
        if (faction == null || !(player instanceof ServerPlayerEntity serverPlayer) || Reputation.ignored.test(serverPlayer)) {
            return ReputationTier.NEUTRAL;
        }
        return Reputation.tier(serverPlayer, faction);
    }

    /** The first faction listing {@code type} as a member, or null. */
    @Nullable
    public static Faction factionOfType(EntityType<?> type) {
        for (Faction faction : Factions.all()) {
            if (faction.members().matchesType(type)) {
                return faction;
            }
        }
        return null;
    }

    /** Whether {@code player} is Marked by the faction of {@code type} (goblins): standing -800 or lower. */
    public static boolean isMarked(PlayerEntity player, EntityType<?> type) {
        Faction faction = factionOfType(type);
        return faction != null && player instanceof ServerPlayerEntity serverPlayer
                && !Reputation.ignored.test(serverPlayer) && Reputation.get(serverPlayer, faction) <= MARKED;
    }

    // ---- Merchant prices ----

    /**
     * The reputation price modifier: how much to add to a trade's first price ({@code base} items, e.g.
     * emeralds) for a customer of {@code tier}. Unfriendly +50% (rounded up, at least +1), Friendly -10%,
     * Honored -25%, Exalted -40% (rounded). The trade itself never drops below 1. Applied with
     * {@code TradeOffer.increaseSpecialPrice}, so other modifiers (e.g. a kin discount) stack on top.
     */
    public static int reputationPriceDelta(ReputationTier tier, int base) {
        return switch (tier) {
            case UNFRIENDLY -> Math.max(1, (int) Math.ceil(base * 0.5D));
            case FRIENDLY -> -(int) Math.round(base * 0.10D);
            case HONORED -> -(int) Math.round(base * 0.25D);
            case EXALTED -> -(int) Math.round(base * 0.40D);
            default -> 0;
        };
    }

    /** Hostile customers get no trade, no bounty payout and no notices. */
    public static boolean refusesService(ReputationTier tier) {
        return tier == ReputationTier.HOSTILE;
    }

    // ---- Hobbits ----

    /** Ticks a hobbit waits before sharing food again after giving to a player of {@code tier}; -1 = never. */
    public static int giftCooldown(ReputationTier tier) {
        return switch (tier) {
            case HOSTILE -> -1;
            case UNFRIENDLY -> 20 * 60 * 15;
            case HONORED, EXALTED -> 20 * 150;
            default -> 20 * 60 * 5;
        };
    }

    /** Whether members flee from this player (hobbits at Hostile). */
    public static boolean fleesFrom(ReputationTier tier) {
        return tier == ReputationTier.HOSTILE;
    }

    // ---- Dwarves ----

    /** Every dwarf attacks a Hostile player on sight. */
    public static boolean attacksOnSight(ReputationTier tier) {
        return tier == ReputationTier.HOSTILE;
    }

    /** Unfriendly: half the time a dwarf won't barter (the gold isn't taken). */
    public static boolean refusesBarter(ReputationTier tier, Random random) {
        return tier == ReputationTier.HOSTILE || tier == ReputationTier.UNFRIENDLY && random.nextBoolean();
    }

    /** Chance of a second barter roll: Friendly 15%, Honored 30%, Exalted 50%. */
    public static float secondBarterRollChance(ReputationTier tier) {
        return switch (tier) {
            case FRIENDLY -> 0.15F;
            case HONORED -> 0.30F;
            case EXALTED -> 0.50F;
            default -> 0.0F;
        };
    }

    /** Honored and up barter from the better table ({@code gameplay/dwarf_barter_honored}). */
    public static boolean honoredBarter(ReputationTier tier) {
        return tier.atLeast(ReputationTier.HONORED);
    }

    /** How far away a dwarf notices a player at the hoard: 24 blocks at Unfriendly or worse, else 16. */
    public static double witnessRange(ReputationTier tier) {
        return tier.atLeast(ReputationTier.NEUTRAL) ? 16.0D : 24.0D;
    }

    // ---- Goblins ----

    /**
     * Whether {@code goblin} may go after {@code player}. Hostile: always (as before). Unfriendly: within
     * {@link #PARLEY_RANGE}, or once provoked. Neutral and up: only once provoked. {@code engaged} skips
     * the parley range, for a goblin already fighting the player.
     */
    public static boolean goblinMayTarget(LivingEntity goblin, PlayerEntity player, boolean engaged) {
        ReputationTier tier = tierWith(player, goblin);
        if (tier == ReputationTier.HOSTILE) {
            return true;
        }
        if (provoked(goblin, player)) {
            return true;
        }
        return tier == ReputationTier.UNFRIENDLY
                && (engaged || goblin.squaredDistanceTo(player) <= PARLEY_RANGE * PARLEY_RANGE);
    }

    /** {@code player} hit {@code npc}, or another member of its faction in the last 10 s. */
    public static boolean provoked(LivingEntity npc, PlayerEntity player) {
        if (npc.getAttacker() == player) {
            return true;
        }
        LivingEntity struck = player.getAttacking();
        if (struck == null || player.age - player.getLastAttackTime() >= PROVOKED_TICKS) {
            return false;
        }
        Faction faction = Reputation.factionOf(npc);
        return faction != null ? faction.isMember(struck) : struck.getType() == npc.getType();
    }

    /** Natural raid chance: 1 in this per check. Marked players draw raids twice as often. */
    public static int raidChance(PlayerEntity player, EntityType<?> raider, int base) {
        return isMarked(player, raider) ? Math.max(1, base / 2) : base;
    }

    // ---- Checks ----

    /**
     * DC shift for a Charisma check (Persuasion, dialogue) against a member of a faction: Hostile +5,
     * Unfriendly +3, Neutral 0, Friendly -2, Honored -4, Exalted -6.
     */
    public static int dcShift(ReputationTier tier) {
        return switch (tier) {
            case HOSTILE -> 5;
            case UNFRIENDLY -> 3;
            case NEUTRAL -> 0;
            case FRIENDLY -> -2;
            case HONORED -> -4;
            case EXALTED -> -6;
        };
    }

    public static int dcShift(PlayerEntity player, Entity npc) {
        return dcShift(tierWith(player, npc));
    }
}
