package mattonfire.dnd.classes.SkillChecks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import mattonfire.dnd.classes.Abilities.AbilityScores;
import mattonfire.dnd.classes.Abilities.RollKind;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

/**
 * Every full-strength melee swing at a living mob rolls a d20 attack roll. Only the extremes do
 * anything, and only they are shown on the HUD:
 * <ul>
 * <li>Natural 20: a critical hit, doing double damage. Fighters (Improved Critical) crit on 19-20.</li>
 * <li>Natural 1: a fumble; the swing misses completely.</li>
 * </ul>
 * The modifier shown is the sheet's attack bonus (the better of STR and DEX modifier, plus proficiency);
 * attacks don't miss against armour, so it's for display only.
 */
public final class AttackRolls {
    /**
     * Players whose current swing is a critical hit, set just before {@code PlayerEntity.attack} runs,
     * with the target and the player's age then. Another attack callback can still cancel the swing,
     * so the crit only counts for that target in that same tick.
     */
    private static final Map<UUID, Crit> CRITICAL = new HashMap<>();

    private record Crit(int targetId, int age) {
    }

    /** Drops a crit roll that never got used; called on disconnect. */
    public static void forget(UUID player) {
        CRITICAL.remove(player);
    }

    private AttackRolls() {
    }

    static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient || player.isSpectator() || !(entity instanceof LivingEntity)
                    || entity instanceof ArmorStandEntity || !entity.isAttackable()
                    // Only full-strength swings roll, so spam-clicking can't fish for crits
                    || player.getAttackCooldownProgress(0.5f) < 0.9f) {
                return ActionResult.PASS;
            }
            D20.Roll roll = D20.roll(player).label(D20.ATTACK).kind(RollKind.ATTACK)
                    .modifier(attackBonus(player)).critRange(critRange(player)).roll();
            if (roll.outcome() == D20.Outcome.FUMBLE) {
                D20.show(player, roll, Text.translatable("skill.dndclasses.attack.fumble"));
                player.resetLastAttackedTicks();
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, SoundCategory.PLAYERS, 1.0f, 0.8f);
                return ActionResult.FAIL;
            }
            if (roll.outcome() == D20.Outcome.CRITICAL) {
                D20.show(player, roll, Text.translatable("skill.dndclasses.attack.critical"));
                CRITICAL.put(player.getUuid(), new Crit(entity.getId(), player.age));
            }
            return ActionResult.PASS;
        });
    }

    /** The better of STR and DEX modifier, plus proficiency, from the character sheet. */
    public static int attackBonus(PlayerEntity player) {
        return AbilityScores.sheet(player).attackBonus();
    }

    /** The lowest natural roll that crits (20; Fighters 19; subclasses can lower it on the sheet). */
    public static int critRange(PlayerEntity player) {
        return AbilityScores.sheet(player).critRange();
    }

    /** Whether this swing at the target is the one that rolled the crit; drops a stale mark. */
    private static boolean isCritical(PlayerEntity player, Entity target) {
        Crit crit = CRITICAL.get(player.getUuid());
        if (crit == null)
            return false;
        if (crit.age() != player.age) {
            CRITICAL.remove(player.getUuid()); // From a swing that was cancelled
            return false;
        }
        return target != null && crit.targetId() == target.getId();
    }

    public static float criticalDamage(PlayerEntity player, Entity target, float amount) {
        return !player.getWorld().isClient && isCritical(player, target) ? amount * 2.0f : amount;
    }

    public static void endAttack(PlayerEntity player, Entity target) {
        if (player.getWorld().isClient)
            return;
        boolean critical = isCritical(player, target);
        CRITICAL.remove(player.getUuid());
        if (critical) {
            player.addCritParticles(target);
            player.getWorld().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0f, 0.8f);
        }
    }
}
