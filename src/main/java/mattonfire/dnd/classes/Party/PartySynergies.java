package mattonfire.dnd.classes.Party;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.ClassInfo;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PartyRole;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Downed.Downed;
import mattonfire.dnd.classes.Obstacles.ObstacleType;
import mattonfire.dnd.classes.SkillChecks.D20;
import mattonfire.dnd.classes.SkillChecks.Eligibility;
import mattonfire.dnd.dm.DungeonMaster;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Role synergies: passive bonuses between members of the same {@code /party}, keyed on each member's
 * primary {@link PartyRole} ({@link ClassInfo#role()}). Solo players, and players with no party nearby, get
 * none of them. Dungeon Masters and Downed members neither give nor get them.
 * <ul>
 * <li><b>Bodyguard</b> (Tank): a hostile mob that hurts a member while within {@value #BODYGUARD_RADIUS}
 * blocks of a party Tank turns on the Tank, {@code 30%} of the time.</li>
 * <li><b>Triage</b> (Healer): a Healer's heals on another member heal {@code +25%} (Regeneration lasts
 * {@code 25%} longer), and holding a Downed member stabilises them {@value #TRIAGE_STABILISE_RATE}x as
 * fast.</li>
 * <li><b>Opportunist</b> (Damage): {@code +15%} damage to a mob whose target is a party Tank.</li>
 * <li><b>Inspire</b> (Support): members within {@value #INSPIRE_RADIUS} blocks of a party Support roll
 * obstacle checks with advantage.</li>
 * <li><b>Scout</b> (Utility): members within {@value #SCOUT_RADIUS} blocks of a party Utility see obstacle
 * hints from {@value #SCOUT_HINT_RANGE} blocks (client side, {@code ObstacleHintHud}).</li>
 * <li><b>Balanced party</b>: 3 / 4 / 5 different primary roles among the members sharing class XP give
 * {@code +10 / 15 / 20%} class XP ({@link PartyEvents#shareXp}).</li>
 * <li><b>Assist</b>: another member within {@value #ASSIST_RADIUS} blocks who could attempt the obstacle
 * themselves (primary or secondary) adds {@value #ASSIST_BONUS} to an obstacle check (at most
 * {@value #ASSIST_MAX} in all), and earns half the solver's class XP.</li>
 * </ul>
 * Every synergy logs a {@code [Party]} line.
 */
public final class PartySynergies {
    public static final double BODYGUARD_RADIUS = 12.0;
    public static final float BODYGUARD_CHANCE = 0.30f;
    public static final float TRIAGE_MULTIPLIER = 1.25f;
    /** Hold ticks a Healer adds per tick when stabilising a Downed member (60 ticks becomes 20). */
    public static final int TRIAGE_STABILISE_RATE = 3;
    public static final float OPPORTUNIST_MULTIPLIER = 1.15f;
    public static final double INSPIRE_RADIUS = 8.0;
    public static final double SCOUT_RADIUS = 16.0;
    public static final double SCOUT_HINT_RANGE = 12.0;
    public static final double ASSIST_RADIUS = 6.0;
    public static final int ASSIST_BONUS = 2;
    public static final int ASSIST_MAX = 2;
    /** Class XP bonus by the number of distinct primary roles: index 3, 4 and 5. */
    private static final double[] BALANCED_BONUS = { 0, 0, 0, 0.10, 0.15, 0.20 };
    /** HUD label of the assist bonus ("+2 assist"). */
    public static final String ASSIST_LABEL = "obstacle.dndclasses.assist";

    private PartySynergies() {
    }

    /** A player's primary role, or null with no class. */
    @Nullable
    public static PartyRole roleOf(PlayerEntity player) {
        DndCharacter dndClass = player instanceof PlayerEntityExt ext ? ext.getDndClass() : null;
        if (dndClass == null || dndClass == DndCharacter.NONE) {
            return null;
        }
        ClassInfo info = ClassInfo.get(dndClass);
        return info == null ? null : info.role();
    }

    /** Whether a member can give or take synergies: not a DM, not Downed. */
    private static boolean active(ServerPlayerEntity player) {
        return !DungeonMaster.isDm(player) && !Downed.is(player);
    }

    /** Party members within {@code radius} of {@code player} who can take part (excluding the player). */
    public static List<ServerPlayerEntity> members(ServerPlayerEntity player, double radius) {
        List<ServerPlayerEntity> nearby = PartyManager.nearbyMembers(player, radius);
        nearby.removeIf(m -> !active(m));
        return nearby;
    }

    private static boolean hasRole(PlayerEntity player, PartyRole role) {
        return roleOf(player) == role;
    }

    // ---- Bodyguard ----

    /**
     * Called when {@code mob} damages {@code victim}: with a party Tank (not the victim) within
     * {@value #BODYGUARD_RADIUS} blocks of the mob, it may switch its target to the nearest such Tank.
     */
    public static void bodyguard(ServerPlayerEntity victim, MobEntity mob) {
        if (!(mob instanceof Monster) || !mob.isAlive() || !active(victim)) {
            return;
        }
        if (mob.getTarget() instanceof ServerPlayerEntity current && current != victim
                && hasRole(current, PartyRole.TANK) && PartyManager.areInSameParty(current, victim)) {
            return; // already on a Tank
        }
        ServerPlayerEntity tank = null;
        double best = BODYGUARD_RADIUS * BODYGUARD_RADIUS;
        for (ServerPlayerEntity member : PartyManager.nearbyMembers(victim, PartyManager.SHARE_RADIUS)) {
            double distance = member.squaredDistanceTo(mob);
            if (active(member) && hasRole(member, PartyRole.TANK) && distance <= best) {
                tank = member;
                best = distance;
            }
        }
        if (tank == null) {
            return;
        }
        float roll = mob.getRandom().nextFloat();
        boolean switched = roll < BODYGUARD_CHANCE;
        DnDClasses.LOGGER.info("[Party] Bodyguard: {} hit {}, {} is {} blocks away, roll {} vs {} -> {}",
                mob.getName().getString(), victim.getEntityName(), tank.getEntityName(),
                String.format("%.1f", Math.sqrt(best)), String.format("%.2f", roll), BODYGUARD_CHANCE,
                switched ? "retarget" : "stays");
        if (switched) {
            mob.setTarget(tank);
            tank.sendMessage(Text.translatable("party.dndclasses.bodyguard", mob.getDisplayName(),
                    victim.getDisplayName()), true);
        }
    }

    // ---- Triage ----

    private static boolean triages(ServerPlayerEntity healer, LivingEntity target) {
        return target instanceof ServerPlayerEntity patient && patient != healer && active(healer)
                && !DungeonMaster.isDm(patient) && hasRole(healer, PartyRole.HEALER)
                && PartyManager.areInSameParty(healer, patient);
    }

    /** A Healer's heal on {@code target}: +25% on another party member. */
    public static float triage(ServerPlayerEntity healer, LivingEntity target, float amount) {
        if (!triages(healer, target)) {
            return amount;
        }
        float boosted = amount * TRIAGE_MULTIPLIER;
        DnDClasses.LOGGER.info("[Party] Triage: {} heals {} for {} instead of {}", healer.getEntityName(),
                target.getEntityName(), boosted, amount);
        return boosted;
    }

    /** A Healer's Regeneration on {@code target}: lasts 25% longer on another party member. */
    public static int triageTicks(ServerPlayerEntity healer, LivingEntity target, int ticks) {
        if (!triages(healer, target)) {
            return ticks;
        }
        int boosted = Math.round(ticks * TRIAGE_MULTIPLIER);
        DnDClasses.LOGGER.info("[Party] Triage: {} gives {} {} ticks of Regeneration instead of {}",
                healer.getEntityName(), target.getEntityName(), boosted, ticks);
        return boosted;
    }

    /** Hold ticks per tick when {@code helper} stabilises {@code target}: faster for a party Healer. */
    public static int stabiliseRate(ServerPlayerEntity helper, ServerPlayerEntity target) {
        return triages(helper, target) ? TRIAGE_STABILISE_RATE : 1;
    }

    // ---- Opportunist ----

    /** A player's hit on {@code target}: +15% for a party Damage dealer when the mob is on a party Tank. */
    public static float opportunist(PlayerEntity attacker, LivingEntity target, float amount) {
        if (!(attacker instanceof ServerPlayerEntity player) || !(target instanceof MobEntity mob)
                || !(mob.getTarget() instanceof ServerPlayerEntity tank) || tank == player
                || !hasRole(player, PartyRole.DAMAGE) || !hasRole(tank, PartyRole.TANK) || !active(player)
                || !active(tank) || !PartyManager.areInSameParty(player, tank)) {
            return amount;
        }
        float boosted = amount * OPPORTUNIST_MULTIPLIER;
        DnDClasses.LOGGER.info("[Party] Opportunist: {} hits {} (on {}) for {} instead of {}",
                player.getEntityName(), mob.getName().getString(), tank.getEntityName(), boosted, amount);
        return boosted;
    }

    // ---- Obstacle checks: Inspire and Assist ----

    /** The party Support within {@value #INSPIRE_RADIUS} blocks inspiring {@code player}, or null. */
    @Nullable
    public static ServerPlayerEntity inspirer(ServerPlayerEntity player) {
        if (!active(player)) {
            return null;
        }
        for (ServerPlayerEntity member : members(player, INSPIRE_RADIUS)) {
            if (hasRole(member, PartyRole.SUPPORT)) {
                return member;
            }
        }
        return null;
    }

    /**
     * Party members within {@value #ASSIST_RADIUS} blocks who could attempt this obstacle themselves (primary or
     * secondary solvers), and so assist the roll.
     */
    public static List<ServerPlayerEntity> assisters(ServerPlayerEntity player, ObstacleType type) {
        List<ServerPlayerEntity> result = new ArrayList<>();
        if (!active(player)) {
            return result;
        }
        for (ServerPlayerEntity member : members(player, ASSIST_RADIUS)) {
            Eligibility eligibility = type.eligibility(D20.classOf(member));
            if (eligibility == Eligibility.PRIMARY || eligibility == Eligibility.SECONDARY) {
                result.add(member);
            }
        }
        return result;
    }

    /** Adds Inspire's advantage and the assist bonus to an obstacle roll. */
    public static D20.Builder obstacleRoll(D20.Builder builder, ServerPlayerEntity player, ObstacleType type) {
        ServerPlayerEntity inspirer = inspirer(player);
        if (inspirer != null) {
            builder.advantage();
            DnDClasses.LOGGER.info("[Party] Inspire: {} rolls {} with advantage ({})", player.getEntityName(),
                    type.id(), inspirer.getEntityName());
        }
        List<ServerPlayerEntity> helpers = assisters(player, type);
        if (!helpers.isEmpty()) {
            int bonus = Math.min(ASSIST_MAX, ASSIST_BONUS * helpers.size());
            builder.bonus(ASSIST_LABEL, bonus);
            DnDClasses.LOGGER.info("[Party] Assist: {} gets +{} on {} from {}", player.getEntityName(), bonus,
                    type.id(), helpers.stream().map(PlayerEntity::getEntityName).toList());
        }
        return builder;
    }

    // ---- Balanced party ----

    /** How many different primary roles there are among {@code sharers}. */
    public static int distinctRoles(List<ServerPlayerEntity> sharers) {
        Set<PartyRole> roles = EnumSet.noneOf(PartyRole.class);
        for (ServerPlayerEntity member : sharers) {
            PartyRole role = roleOf(member);
            if (role != null && active(member)) {
                roles.add(role);
            }
        }
        return roles.size();
    }

    /** The class XP multiplier for that many different roles: 1 + 10 / 15 / 20% for 3 / 4 / 5. */
    public static double balancedMultiplier(int distinctRoles) {
        return 1.0 + BALANCED_BONUS[Math.max(0, Math.min(distinctRoles, BALANCED_BONUS.length - 1))];
    }
}
