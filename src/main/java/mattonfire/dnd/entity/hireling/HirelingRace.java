package mattonfire.dnd.entity.hireling;

import java.util.Locale;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

/**
 * Who answers the innkeeper's call for adventurers: Humans (40%), and Elves, Half-Orcs, Tieflings, Dragonborn
 * and Gnomes (12% each). {@link #skins} is how many skins the renderer has for the race.
 */
public enum HirelingRace {
    HUMAN(40, 6),
    ELF(12, 6),
    HALF_ORC(12, 6),
    TIEFLING(12, 5),
    DRAGONBORN(12, 5),
    GNOME(12, 5);

    public final int weight;
    public final int skins;

    HirelingRace(int weight, int skins) {
        this.weight = weight;
        this.skins = skins;
    }

    public String id() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public MutableText text() {
        return Text.translatable("hireling.dndclasses.race." + this.id());
    }

    public static HirelingRace pick(Random random) {
        int total = 0;
        for (HirelingRace race : values()) {
            total += race.weight;
        }
        int roll = random.nextInt(total);
        for (HirelingRace race : values()) {
            roll -= race.weight;
            if (roll < 0) {
                return race;
            }
        }
        return HUMAN;
    }

    @Nullable
    public static HirelingRace byId(String id) {
        for (HirelingRace race : values()) {
            if (race.id().equalsIgnoreCase(id) || race.name().replace("_", "").equalsIgnoreCase(id)) {
                return race;
            }
        }
        return null;
    }

    public static HirelingRace byOrdinal(int ordinal) {
        HirelingRace[] values = values();
        return values[Math.floorMod(ordinal, values.length)];
    }
}
