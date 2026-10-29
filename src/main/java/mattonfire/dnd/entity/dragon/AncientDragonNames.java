package mattonfire.dnd.entity.dragon;

import net.minecraft.util.math.random.Random;

/**
 * Names for Ancient dragons (design 10, 5.12): one syllable from each of its colour's three tables, so a Red is
 * "Kharzanthrax" or "Vyrmorrath". Given once, as the entity's custom name, on its first tick.
 */
public final class AncientDragonNames {
    private AncientDragonNames() {
    }

    public static String roll(ChromaticColor color, Random random) {
        StringBuilder name = new StringBuilder();
        for (String[] table : color.syllables()) {
            name.append(table[random.nextInt(table.length)]);
        }
        return name.toString();
    }
}
