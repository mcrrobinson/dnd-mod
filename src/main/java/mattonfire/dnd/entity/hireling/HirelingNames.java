package mattonfire.dnd.entity.hireling;

import net.minecraft.util.math.random.Random;

/** First names and family names for hirelings, by race. */
public final class HirelingNames {
    private static final String[][] FIRST = {
            // Human
            {"Brannoc", "Aldric", "Maren", "Tobias", "Elsbeth", "Garrick", "Wenna", "Rowan", "Hollis", "Isolde",
                    "Corwin", "Bryony", "Edric", "Linnet", "Osric", "Talia", "Jory", "Merrin", "Hugh", "Annick"},
            // Elf
            {"Aelar", "Thalia", "Ilyndra", "Faelan", "Soveliss", "Naivara", "Erevan", "Sariel", "Lia", "Theren"},
            // Half-Orc
            {"Dench", "Grukka", "Holg", "Ovak", "Shautha", "Thokk", "Volen", "Yevelda", "Krusk", "Baggi"},
            // Tiefling
            {"Akmenos", "Damakos", "Kallista", "Mordai", "Nemeia", "Orianna", "Skamos", "Rieta", "Ekemon", "Leucis"},
            // Dragonborn
            {"Arjhan", "Balasar", "Kava", "Medrash", "Nala", "Pandjed", "Sora", "Torinn", "Thava", "Kriv"},
            // Gnome
            {"Alston", "Bimpnottin", "Dimble", "Ellyjobell", "Fonkin", "Nissa", "Orryn", "Roywyn", "Wrenn", "Zook"}
    };
    private static final String[][] LAST = {
            {"Ashdown", "Blackwater", "Thorne", "Hale", "Marsh", "Fairweather", "Crane", "Holloway", "Stroud",
                    "Wren", "Brightwood", "Cobb", "Harrow", "Penrose"},
            {"Amakiir", "Galanodel", "Liadon", "Meliamne", "Siannodel", "Xiloscient", "Ilphelkiir", "Nailo"},
            {"Bonecleaver", "Ironjaw", "Grimtusk", "Ashfist", "Redmane", "Stonehide", "Skullsplitter"},
            {"Ashborn", "Nightsong", "Cinderwake", "Duskmantle", "Emberveil", "Hollowheart"},
            {"Kerrhylon", "Myastan", "Daardendrian", "Delmirev", "Fenkenkabradon", "Nemmonis", "Yarjerit"},
            {"Beren", "Daergel", "Folkor", "Garrick", "Nackle", "Murnig", "Scheppen", "Turen", "Timbers"}
    };

    private HirelingNames() {
    }

    public static String pick(HirelingRace race, Random random) {
        String[] first = FIRST[race.ordinal()];
        String[] last = LAST[race.ordinal()];
        return first[random.nextInt(first.length)] + " " + last[random.nextInt(last.length)];
    }

    /** A family name for a race, for {@code /hireling admin spawn ... <first name>}. */
    public static String surname(HirelingRace race, Random random) {
        String[] last = LAST[race.ordinal()];
        return last[random.nextInt(last.length)];
    }
}
