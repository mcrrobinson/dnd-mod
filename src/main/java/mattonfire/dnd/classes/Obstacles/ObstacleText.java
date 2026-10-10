package mattonfire.dnd.classes.Obstacles;

import java.util.List;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Progression.Progression;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/** Text shared by the crosshair hint (client) and the action bar (server). */
public final class ObstacleText {
    private ObstacleText() {
    }

    /** "Lesser Arcane Seal (Hard)". */
    public static MutableText name(ObstacleType type, Tier tier) {
        return Text.translatable(type.translationKey()).append(" (")
                .append(Text.translatable(tier.translationKey())).append(")");
    }

    /** "A Wizard or Warlock" for the type's solvers, primaries first. */
    public static String solverList(ObstacleType type) {
        List<DndCharacter> solvers = type.solvers();
        if (solvers.isEmpty()) {
            return "Nobody";
        }
        StringBuilder out = new StringBuilder();
        String first = Progression.name(solvers.get(0));
        out.append("AEIOU".indexOf(first.charAt(0)) >= 0 ? "An " : "A ");
        for (int i = 0; i < solvers.size(); i++) {
            if (i > 0) {
                out.append(i == solvers.size() - 1 ? " or " : ", ");
            }
            out.append(Progression.name(solvers.get(i)));
        }
        return out.toString();
    }

    /** "A Wizard or Warlock could dispel it". */
    public static MutableText who(ObstacleType type) {
        return Text.translatable("obstacle.dndclasses.who", solverList(type),
                Text.translatable(type.translationKey() + ".verb"));
    }

    /** "You could dispel it: right-click (sneak to take your time)". */
    public static MutableText you(ObstacleType type) {
        return Text.translatable("obstacle.dndclasses.you", Text.translatable(type.translationKey() + ".verb"));
    }

    /** What anyone else can do instead. */
    public static MutableText fallback(ObstacleType type) {
        return Text.translatable(type.translationKey() + ".fallback");
    }

    /** The action-bar line for someone who can't attempt it. */
    public static MutableText cannot(ObstacleType type, Tier tier) {
        return name(type, tier).append(". ").append(who(type));
    }
}
