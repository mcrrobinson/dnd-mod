package mattonfire.dnd.classes.Downed;

import java.util.Set;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Abilities.AbilityScores;
import mattonfire.dnd.classes.Abilities.Advantage;
import mattonfire.dnd.classes.Abilities.RollKind;
import mattonfire.dnd.classes.Abilities.RollQuery;
import mattonfire.dnd.classes.Rest.DndRules;
import mattonfire.dnd.classes.Rest.RestState;
import mattonfire.dnd.classes.SkillChecks.D20;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Death saving throws: a flat d20 (kind {@link RollKind#DEATH}, no ability modifier) plus any
 * {@link DeathSaveModifier} bonuses (capped at +{@value #MAX_BONUS}) against DC {@value #DC}, shown on the d20
 * HUD. Halfling Lucky and sheet advantage filters that match {@code kind DEATH} apply.
 * <ul>
 * <li>natural 20: back up with 1 HP</li>
 * <li>natural 1: two fails</li>
 * <li>pass / fail otherwise; three passes stabilise, three fails kill</li>
 * </ul>
 * {@link #lastStand}: a solo player's one roll against DC {@value #LAST_STAND_DC}.
 */
public final class DeathSaves {
    public static final int DC = 10;
    public static final int LAST_STAND_DC = 15;
    /** World ticks between Last Stands (10 minutes). */
    public static final int LAST_STAND_COOLDOWN = 12000;
    public static final int MAX_BONUS = 5;

    public static final String LABEL = "skill.dndclasses.death_save";
    public static final String LAST_STAND_LABEL = "skill.dndclasses.last_stand";

    private DeathSaves() {
    }

    /** Rolls (and logs) a death save or Last Stand for the player. Show it with {@link D20#show}. */
    public static D20.Roll roll(ServerPlayerEntity player, String label, int dc, Advantage mode) {
        D20.Builder builder = D20.roll(player).label(label).kind(RollKind.DEATH).dc(dc)
                .rerollNaturalOnes(AbilityScores.sheet(player).rerollNaturalOnes())
                .sheetAdvantage(new RollQuery(RollKind.DEATH, null, null, Set.of("death_save"))).mode(mode);
        int bonus = Math.max(-MAX_BONUS, Math.min(MAX_BONUS, DeathSaveModifier.EVENT.invoker().bonus(player)));
        if (bonus != 0) {
            builder.bonus("skill.dndclasses.death_save.bonus", bonus);
        }
        return builder.roll();
    }

    /** The 6-second save of a Downed, unstable player. */
    static void rollSave(ServerPlayerEntity player, Downed.State state) {
        D20.Roll roll = roll(player, LABEL, DC, DeathSaveModifier.EVENT.invoker().mode(player));
        switch (roll.outcome()) {
            case CRITICAL -> {
                D20.show(player, roll, Text.translatable("skill.dndclasses.death_save.critical"));
                Downed.revive(player, 1.0F);
            }
            case FUMBLE -> {
                Downed.addFails(player, state, 2);
                D20.show(player, roll, tallyText("skill.dndclasses.death_save.fumble", state));
            }
            case SUCCESS -> {
                Downed.addPass(player, state);
                D20.show(player, roll, state.passes >= Downed.PASSES_TO_STABLE || state.stable
                        ? Text.translatable("skill.dndclasses.death_save.stable")
                        : tallyText("skill.dndclasses.death_save.success", state));
            }
            case FAILURE -> {
                Downed.addFails(player, state, 1);
                D20.show(player, roll, tallyText("skill.dndclasses.death_save.failure", state));
            }
        }
        DnDClasses.LOGGER.info("[Downed] {} death save {}: {} passes, {} fails", player.getEntityName(),
                roll.outcome(), state.passes, state.fails);
    }

    private static Text tallyText(String key, Downed.State state) {
        return Text.translatable(key, state.passes, state.fails);
    }

    /**
     * A solo player's Last Stand: one roll against DC 15. Returns true if they're back up at 1 HP (the caller
     * cancels the death), false if they die. Off with {@code dndLastStand false}; once per 10 minutes.
     */
    public static boolean lastStand(ServerPlayerEntity player) {
        if (!player.world.getGameRules().getBoolean(DndRules.LAST_STAND)) {
            return false;
        }
        long now = player.getServer().getOverworld().getTime();
        RestState rest = RestState.get(player);
        if (now < rest.lastStandReadyAt) {
            DnDClasses.LOGGER.info("[Downed] {} has no Last Stand for {} more s", player.getEntityName(),
                    (rest.lastStandReadyAt - now) / 20);
            return false;
        }
        rest.lastStandReadyAt = now + LAST_STAND_COOLDOWN;
        rest.save(player);
        D20.Roll roll = roll(player, LAST_STAND_LABEL, LAST_STAND_DC, DeathSaveModifier.EVENT.invoker().mode(player));
        boolean stood = roll.outcome().succeeded();
        D20.show(player, roll, Text.translatable(stood ? "skill.dndclasses.last_stand.success"
                : "skill.dndclasses.last_stand.failure"));
        DnDClasses.LOGGER.info("[Downed] {} Last Stand {}", player.getEntityName(), stood ? "succeeded" : "failed");
        if (!stood) {
            return false;
        }
        player.setHealth(1.0F);
        player.extinguish();
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 60, 1));
        Downed.standUpEffects(player);
        player.sendMessage(Text.literal("You refuse to fall!").formatted(Formatting.GOLD, Formatting.BOLD), false);
        return true;
    }
}
