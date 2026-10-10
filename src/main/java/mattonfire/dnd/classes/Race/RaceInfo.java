package mattonfire.dnd.classes.Race;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;

import mattonfire.dnd.classes.ClassInfo;
import mattonfire.dnd.classes.DnDClasses;

/**
 * Each race's text, ability bonuses and stat modifiers, loaded from {@code data/dndclasses/race_info.json}.
 * The race picker, the pick chat message, the guidebook's heritage page, {@link RaceStats} and
 * {@link RaceAbilityBonuses} all read it, and {@code ./gradlew generateRaceTable} writes the table in
 * {@code docs/races/races.md} from it.
 */
public record RaceInfo(DndRace id, String name, String icon, String summary, Map<String, Integer> abilityBonuses,
        String size, double scale, Stats stats, List<String> traits) {

    public static final String RESOURCE = "/data/" + DnDClasses.MOD_ID + "/race_info.json";
    /** Ability keys area 4's {@code Ability} enum is expected to match. */
    public static final List<String> ABILITIES = List.of("STR", "DEX", "CON", "INT", "WIS", "CHA");

    /**
     * Attribute modifiers on top of the class's base values. speed is a fraction of base movement speed,
     * maxHealth is in health points, knockbackResistance 0-1, attackRange in blocks.
     */
    public record Stats(double speed, double maxHealth, double knockbackResistance, double attackRange) {
        public static final Stats NONE = new Stats(0, 0, 0, 0);

        /** Short phrases for text, e.g. "Speed -8%", "+1 heart". Empty when nothing changes. */
        public List<String> describe() {
            List<String> lines = new ArrayList<>();
            if (speed != 0) {
                lines.add("Speed " + signed(Math.round(speed * 100)) + "%");
            }
            if (maxHealth != 0) {
                double hearts = maxHealth / 2.0;
                lines.add("Max health " + signed(hearts) + (Math.abs(hearts) == 1 ? " heart" : " hearts"));
            }
            if (knockbackResistance != 0) {
                lines.add("Knockback resistance " + signed(Math.round(knockbackResistance * 100)) + "%");
            }
            if (attackRange != 0) {
                lines.add("Attack reach " + signed(attackRange) + " blocks");
            }
            return lines;
        }
    }

    private static Map<DndRace, RaceInfo> byRace;

    private record Entry(String id, String name, String icon, String summary, Map<String, Integer> abilityBonuses,
            String size, Double scale, Map<String, Double> stats, List<String> traits) {
    }

    private record Root(List<Entry> races) {
    }

    /** Info for a race, or null for {@link DndRace#NONE}. */
    @Nullable
    public static RaceInfo get(@Nullable DndRace race) {
        return race == null ? null : all().get(race);
    }

    /** Every race in picker order. */
    public static synchronized Map<DndRace, RaceInfo> all() {
        if (byRace == null) {
            byRace = load();
        }
        return byRace;
    }

    /** "DEX +2, WIS +1", or "+1 to all six". */
    public String abilityText() {
        if (abilityBonuses.size() == ABILITIES.size()
                && abilityBonuses.values().stream().distinct().count() == 1) {
            return signed(abilityBonuses.values().iterator().next()) + " to all six";
        }
        List<String> parts = new ArrayList<>();
        abilityBonuses.forEach((ability, n) -> parts.add(ability + " " + signed(n)));
        return String.join(", ", parts);
    }

    public List<String> traitsForGame() {
        return traits.stream().map(ClassInfo::forGame).toList();
    }

    private static Map<DndRace, RaceInfo> load() {
        Map<DndRace, RaceInfo> map = new EnumMap<>(DndRace.class);
        try (InputStream in = RaceInfo.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                DnDClasses.LOGGER.error("Missing race info resource " + RESOURCE);
                return map;
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                Root root = new Gson().fromJson(reader, Root.class);
                for (Entry e : root.races()) {
                    DndRace race = DndRace.valueOf(e.id());
                    map.put(race, new RaceInfo(race, e.name(), e.icon(), e.summary() == null ? "" : e.summary(),
                            bonuses(e), e.size() == null ? "Medium" : e.size(),
                            e.scale() == null ? 1.0 : e.scale(), stats(e.stats()),
                            e.traits() == null ? Collections.emptyList() : e.traits()));
                }
            }
        } catch (Exception e) {
            DnDClasses.LOGGER.error("Couldn't load race info from " + RESOURCE, e);
        }
        return map;
    }

    private static Map<String, Integer> bonuses(Entry e) {
        Map<String, Integer> bonuses = new LinkedHashMap<>();
        if (e.abilityBonuses() != null) {
            e.abilityBonuses().forEach((ability, n) -> {
                if (ABILITIES.contains(ability)) {
                    bonuses.put(ability, n);
                } else {
                    DnDClasses.LOGGER.error("race_info.json: {} has unknown ability {}", e.id(), ability);
                }
            });
        }
        return Collections.unmodifiableMap(bonuses);
    }

    private static Stats stats(@Nullable Map<String, Double> stats) {
        if (stats == null) {
            return Stats.NONE;
        }
        Set<String> known = Set.of("speed", "maxHealth", "knockbackResistance", "attackRange");
        for (String key : stats.keySet()) {
            if (!known.contains(key)) {
                DnDClasses.LOGGER.error("race_info.json: unknown stat {}", key);
            }
        }
        return new Stats(stats.getOrDefault("speed", 0.0), stats.getOrDefault("maxHealth", 0.0),
                stats.getOrDefault("knockbackResistance", 0.0), stats.getOrDefault("attackRange", 0.0));
    }

    private static String signed(double v) {
        String s = v == Math.rint(v) ? Long.toString(Math.round(v)) : Double.toString(v);
        return v > 0 ? "+" + s : s;
    }
}
