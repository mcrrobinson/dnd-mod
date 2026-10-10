package mattonfire.dnd.classes;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;

import mattonfire.dnd.classes.Abilities.ClassAbilities;

/**
 * Pros, cons and special ability of each class, loaded from
 * {@code data/dndclasses/class_info.json}. The same file generates the Player
 * Classes table in the README ({@code ./gradlew generateClassReadme}), so the
 * guidebook, the class-pick chat message and the README never drift apart.
 */
public record ClassInfo(DndCharacter id, String name, List<String> pros, List<String> cons, List<String> special,
        boolean specialOnKey, @Nullable ClassAbilities abilities) {

    public static final String RESOURCE = "/data/" + DnDClasses.MOD_ID + "/class_info.json";

    private static Map<DndCharacter, ClassInfo> byClass;
    private static final List<String> ABILITY_ERRORS = new ArrayList<>();

    private record Entry(String id, String name, List<String> pros, List<String> cons, List<String> special,
            Boolean specialOnKey, Map<String, Integer> abilities, List<String> saves, List<String> skills,
            List<String> expertise) {
    }

    private record Root(List<Entry> classes) {
    }

    /** Info for a class, or null for {@link DndCharacter#NONE}. */
    @Nullable
    public static ClassInfo get(@Nullable DndCharacter character) {
        return character == null ? null : all().get(character);
    }

    /** Every class in README order. */
    public static synchronized Map<DndCharacter, ClassInfo> all() {
        if (byClass == null) {
            byClass = load();
        }
        return byClass;
    }

    private static Map<DndCharacter, ClassInfo> load() {
        Map<DndCharacter, ClassInfo> map = new EnumMap<>(DndCharacter.class);
        try (InputStream in = ClassInfo.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                DnDClasses.LOGGER.error("Missing class info resource " + RESOURCE);
                return map;
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                Root root = new Gson().fromJson(reader, Root.class);
                for (Entry e : root.classes()) {
                    DndCharacter character = DndCharacter.valueOf(e.id());
                    ClassAbilities abilities = ClassAbilities.parse(e.id(), e.abilities(), e.saves(), e.skills(),
                            e.expertise(), ABILITY_ERRORS);
                    map.put(character, new ClassInfo(character, e.name(), orEmpty(e.pros()), orEmpty(e.cons()),
                            orEmpty(e.special()), e.specialOnKey() == null || e.specialOnKey(), abilities));
                }
            }
        } catch (Exception e) {
            DnDClasses.LOGGER.error("Couldn't load class info from " + RESOURCE, e);
        }
        return map;
    }

    /** Problems found in the ability fields while loading; {@link ClassAbilities#validateAll} reports them. */
    public static synchronized List<String> abilityErrors() {
        all();
        return List.copyOf(ABILITY_ERRORS);
    }

    private static List<String> orEmpty(@Nullable List<String> list) {
        return list == null ? Collections.emptyList() : list;
    }

    /** Drops markdown emphasis, and any leftover "(done)" status marker, for in-game text. */
    public static String forGame(String text) {
        return text.replace(" (done)", "").replace("(done)", "").replace("*", "").trim();
    }

    public static String joinForGame(List<String> points) {
        return points.stream().map(ClassInfo::forGame).collect(Collectors.joining(". ")) + ".";
    }
}
