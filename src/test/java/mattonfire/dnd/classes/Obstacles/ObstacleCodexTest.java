package mattonfire.dnd.classes.Obstacles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import mattonfire.dnd.classes.ClassInfo;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.SkillChecks.Eligibility;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Keeps the "every class has a job" promise: each class is the only PRIMARY solver of at least one obstacle
 * type. Also checks that class_info.json's obstacle lists and the docs catalogue table match the registry.
 */
class ObstacleCodexTest {
    private static final Path LANG = Path.of("src/main/resources/assets/dndclasses/lang/en_us.json");
    private static final Path DOCS = Path.of("docs/systems/obstacles.md");
    private static final String TABLE_START = "<!-- obstacle-table:start";
    private static final String TABLE_END = "<!-- obstacle-table:end -->";

    private static Map<String, String> lang;

    @BeforeAll
    static void bootstrap() throws IOException {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
        try (Reader reader = Files.newBufferedReader(LANG, StandardCharsets.UTF_8)) {
            lang = new Gson().fromJson(reader, new TypeToken<Map<String, String>>() {
            }.getType());
        }
    }

    @Test
    void everyClassHasAnExclusiveObstacle() {
        List<DndCharacter> missing = ObstacleCodex.classesWithoutExclusive(ObstacleTypes.all());
        assertTrue(missing.isEmpty(), "No obstacle type has these classes as its only PRIMARY solver: " + missing
                + ". Every class needs at least one obstacle only it can handle (see docs/systems/obstacles.md).");
    }

    /** The check above must notice when any one class loses its exclusive obstacles. */
    @Test
    void losingAnExclusiveFailsTheCheck() {
        Map<DndCharacter, List<ObstacleType>> exclusives = ObstacleCodex.exclusives(ObstacleTypes.all());
        for (DndCharacter c : DndCharacter.values()) {
            if (c == DndCharacter.NONE) {
                continue;
            }
            List<ObstacleType> types = new ArrayList<>();
            for (ObstacleType type : ObstacleTypes.all()) {
                types.add(exclusives.getOrDefault(c, List.of()).contains(type) ? new Demoted(type, c) : type);
            }
            assertEquals(List.of(c), ObstacleCodex.classesWithoutExclusive(types),
                    "Demoting " + c + " to SECONDARY on its exclusive obstacles");
        }
    }

    /**
     * class_info.json's obstacle lists (the guidebook's "Obstacles you handle" page) match the registry: each
     * names every obstacle the class may roll for, and its exclusives in bold.
     */
    @Test
    void classInfoObstacleListsMatchTheRegistry() {
        List<String> problems = new ArrayList<>();
        for (DndCharacter c : DndCharacter.values()) {
            if (c == DndCharacter.NONE) {
                continue;
            }
            ClassInfo info = ClassInfo.get(c);
            List<String> bold = new ArrayList<>();
            for (String point : info.obstacles()) {
                String[] parts = point.split("\\*\\*");
                for (int i = 1; i < parts.length; i += 2) {
                    bold.add(parts[i].toLowerCase(Locale.ROOT));
                }
            }
            String all = String.join(" ", info.obstacles()).replace("*", "").toLowerCase(Locale.ROOT);
            for (ObstacleType type : ObstacleCodex.entries()) {
                Eligibility e = type.eligibility(c);
                String name = name(type).toLowerCase(Locale.ROOT);
                String stem = name.substring(0, name.length() - 1);
                if (ObstacleCodex.exclusiveSolver(type) == c
                        && bold.stream().noneMatch(b -> b.startsWith(name) || b.startsWith(stem))) {
                    problems.add(info.name() + " is missing its exclusive **" + name(type) + "** in bold");
                } else if ((e == Eligibility.PRIMARY || e == Eligibility.SECONDARY) && !all.contains(stem)) {
                    problems.add(info.name() + " is missing " + name(type) + " (" + e + ")");
                }
            }
        }
        assertTrue(problems.isEmpty(), "class_info.json obstacle lists are out of date with the obstacle registry:\n"
                + String.join("\n", problems));
    }

    @Test
    void docsTableMatchesTheRegistry() throws IOException {
        String page = Files.readString(DOCS, StandardCharsets.UTF_8);
        int startAt = page.indexOf(TABLE_START);
        int endAt = page.indexOf(TABLE_END);
        if (startAt < 0 || endAt < startAt) {
            fail(DOCS + " is missing the " + TABLE_START + " ... " + TABLE_END + " markers");
        }
        int afterStart = page.indexOf('\n', startAt) + 1;
        String expected = page.substring(0, afterStart) + table() + page.substring(endAt);
        if (Boolean.getBoolean("dnd.writeObstacleTable")) {
            Files.writeString(DOCS, expected, StandardCharsets.UTF_8);
            return;
        }
        assertEquals(expected, page, DOCS + " obstacle table is out of date with the obstacle registry. "
                + "Run ./gradlew generateObstacleTable");
    }

    @Test
    void codexMergesOnlyLookAlikes() {
        assertTrue(ObstacleCodex.entries().contains(ObstacleTypes.HIDDEN_DOOR));
        assertFalse(ObstacleCodex.entries().contains(ObstacleTypes.HIDDEN_DEEPSLATE_DOOR));
        assertTrue(ObstacleCodex.entries().contains(ObstacleTypes.LESSER_ARCANE_SEAL));
        assertTrue(ObstacleCodex.entries().contains(ObstacleTypes.GREATER_ARCANE_SEAL));
    }

    private static String table() {
        StringBuilder out = new StringBuilder();
        out.append("| Obstacle | Check | Primary | Secondary | Anyone else | Fallback |\n");
        out.append("|-|-|-|-|-|-|\n");
        for (ObstacleType type : ObstacleCodex.entries()) {
            List<String> variants = ObstacleCodex.variants(type).stream().map(ObstacleCodexTest::name).toList();
            String obstacle = "**" + name(type) + "**"
                    + (variants.isEmpty() ? "" : "<br>(also " + String.join(", ", variants) + ")")
                    + "<br>`" + type.id() + "`";
            DndCharacter only = ObstacleCodex.exclusiveSolver(type);
            String primary = only != null ? "**" + Progression.name(only) + "** (only)"
                    : names(ObstacleCodex.classes(type, Eligibility.PRIMARY));
            String secondary = names(ObstacleCodex.classes(type, Eligibility.SECONDARY));
            String anyone = ObstacleCodex.anyoneMayTry(type) ? "May try"
                    : ObstacleCodex.classes(type, Eligibility.NONE).isEmpty() ? "-" : "Can't try";
            out.append("| ").append(obstacle).append(" | ").append(check(type)).append(" | ").append(primary)
                    .append(" | ").append(secondary).append(" | ").append(anyone).append(" | ")
                    .append(ObstacleCodex.fallbackSentence(text(type.translationKey() + ".fallback"))).append(" |\n");
        }
        out.append('\n');
        out.append("| Class | Only they can handle |\n");
        out.append("|-|-|\n");
        Map<DndCharacter, List<ObstacleType>> exclusives = ObstacleCodex.exclusives(ObstacleCodex.entries());
        for (DndCharacter c : DndCharacter.values()) {
            if (c == DndCharacter.NONE) {
                continue;
            }
            out.append("| ").append(Progression.name(c)).append(" | ")
                    .append(exclusives.getOrDefault(c, List.of()).stream().map(ObstacleCodexTest::name)
                            .collect(Collectors.joining(", ")))
                    .append(" |\n");
        }
        return out.toString();
    }

    private static String check(ObstacleType type) {
        if (type.rollLabel() != null) {
            return text(type.rollLabel());
        }
        Skill skill = type.skill();
        return skill == null ? "None" : text(skill.translationKey());
    }

    private static String names(List<DndCharacter> classes) {
        return classes.isEmpty() ? "-" : classes.stream().map(Progression::name).collect(Collectors.joining(", "));
    }

    private static String name(ObstacleType type) {
        return text(type.translationKey());
    }

    private static String text(String key) {
        String value = lang.get(key);
        if (value == null) {
            fail("Missing lang key " + key);
        }
        return value.replace("|", "\\|");
    }

    /** {@code type}, but with {@code demoted} only a SECONDARY solver. */
    private static final class Demoted extends ObstacleType {
        private final ObstacleType type;
        private final DndCharacter demoted;

        Demoted(ObstacleType type, DndCharacter demoted) {
            super(new Identifier(type.id().getNamespace(), type.id().getPath() + "_demoted"));
            this.type = type;
            this.demoted = demoted;
        }

        @Override
        public Skill skill() {
            return type.skill();
        }

        @Override
        public Eligibility eligibility(DndCharacter dndClass) {
            return dndClass == demoted ? Eligibility.SECONDARY : type.eligibility(dndClass);
        }

        @Override
        public boolean breakable() {
            return type.breakable();
        }
    }
}
