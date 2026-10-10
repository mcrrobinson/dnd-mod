package mattonfire.dnd.classes.Rest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Progression.Progression;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Hit Dice: a pool of one die per class level (at least 1) that short rests
 * spend to heal. A long rest gives half the pool back.
 */
public final class HitDice {
    /**
     * The CON modifier added to each die, from the player's character sheet.
     */
    public static ToIntFunction<ServerPlayerEntity> conModifier =
            player -> mattonfire.dnd.classes.Abilities.AbilityScores.modifier(player,
                    mattonfire.dnd.classes.Abilities.Ability.CON);

    private HitDice() {
    }

    public static int dieSize(DndCharacter dndClass) {
        return switch (dndClass) {
            case BARBARIAN -> 12;
            case FIGHTER, PALADIN, RANGER, BLOODHUNTER -> 10;
            case WIZARD, NECROMANCER -> 6;
            default -> 8;
        };
    }

    /** The pool size: the class level, at least 1; 0 without a class. */
    public static int max(ServerPlayerEntity player) {
        DndCharacter dndClass = Progression.classOf(player);
        if (dndClass == DndCharacter.NONE) {
            return 0;
        }
        return Math.max(1, Progression.get(player, dndClass).level());
    }

    public static int remaining(ServerPlayerEntity player, RestState state) {
        return Math.max(0, max(player) - state.hitDiceSpent);
    }

    /** Sets the dice left, clamped to 0..max. */
    public static void setRemaining(ServerPlayerEntity player, int n) {
        RestState state = RestState.get(player);
        int max = max(player);
        state.hitDiceSpent = max - Math.max(0, Math.min(max, n));
        state.save(player);
        RestSync.sync(player);
    }

    /**
     * Spends dice one at a time until the player is at full health or has spent
     * half the pool (rounded up) this rest, and heals them.
     *
     * @return the chat line, or null without a class
     */
    static Text spendOnShortRest(ServerPlayerEntity player, RestState state) {
        DndCharacter dndClass = Progression.classOf(player);
        int max = max(player);
        if (max == 0) {
            return null;
        }
        int size = dieSize(dndClass);
        int limit = (max + 1) / 2;
        int con = conModifier.applyAsInt(player);
        List<Integer> rolls = new ArrayList<>();
        int healed = 0;
        while (rolls.size() < limit && remaining(player, state) > 0
                && player.getHealth() + healed < player.getMaxHealth()) {
            int roll = 1 + player.getRandom().nextInt(size);
            rolls.add(roll);
            healed += Math.max(1, roll + con);
            state.hitDiceSpent++;
        }
        if (rolls.isEmpty()) {
            return Text.literal(remaining(player, state) > 0 ? "Short rest: already at full health, no Hit Dice spent"
                    : "Short rest: no Hit Dice left").formatted(Formatting.GRAY);
        }
        player.heal(healed);
        String sum = String.join(" + ", rolls.stream().map(String::valueOf).toList());
        String conPart = con == 0 ? "" : (con > 0 ? " + " : " - ") + Math.abs(con) * rolls.size();
        return Text.literal("Short rest: spent " + rolls.size() + " Hit " + (rolls.size() == 1 ? "Die" : "Dice")
                + " (d" + size + "): " + sum + conPart + " = " + healed + " HP").formatted(Formatting.GREEN);
    }

    /** A long rest gives back half the pool (rounded down, at least 1). Returns how many came back. */
    static int restoreOnLongRest(ServerPlayerEntity player, RestState state) {
        int before = state.hitDiceSpent;
        state.hitDiceSpent = Math.max(0, state.hitDiceSpent - Math.max(1, max(player) / 2));
        return before - state.hitDiceSpent;
    }
}
