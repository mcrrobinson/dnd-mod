package mattonfire.dnd.classes.SkillChecks;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.AbilityScores;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Blocks.TrapTriggerBlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Who notices a dungeon trap: asked every half second for each player near an armed trap, and a "yes" draws
 * the trap's outline for that player only. Swap the rule with {@link #set} (a race with keener senses, a
 * Detect Traps spell, a DM toolkit override).
 *
 * The default: Rogues and Rangers always spot traps; anyone else spots one when their passive Perception
 * (10 + Perception bonus, from the character sheet) meets the trap's DC.
 */
@FunctionalInterface
public interface TrapSense {
    boolean spots(ServerPlayerEntity player, TrapTriggerBlockEntity trap, int dc);

    TrapSense DEFAULT = (player, trap, dc) -> {
        DndCharacter c = D20.classOf(player);
        return c == DndCharacter.ROGUE || c == DndCharacter.RANGER
                || AbilityScores.sheet(player).passive(Skill.PERCEPTION) >= dc;
    };

    static TrapSense get() {
        return Holder.current;
    }

    static void set(TrapSense sense) {
        Holder.current = sense == null ? DEFAULT : sense;
    }

    final class Holder {
        private static volatile TrapSense current = DEFAULT;

        private Holder() {
        }
    }
}
