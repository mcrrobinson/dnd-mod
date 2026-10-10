package mattonfire.dnd.classes;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;

/**
 * Pros, cons and special ability of each class, loaded from
 * {@code data/dndclasses/class_info.json}. The same file generates the Player
 * Classes table in the README ({@code ./gradlew generateClassReadme}), so the
 * guidebook, the class-pick chat message and the README never drift apart.
 */
public record ClassInfo(DndCharacter id, String name, List<String> pros, List<String> cons, List<String> special,
        boolean specialOnKey, String subclassTerm, List<SubclassInfo> subclasses) {

    public static final String RESOURCE = "/data/" + DnDClasses.MOD_ID + "/class_info.json";

    private static Map<DndCharacter, ClassInfo> byClass;
    private static Map<String, SubclassInfo> bySubclass;

    /**
     * A subclass's text. The rules (which nodes it locks) are in {@link mattonfire.dnd.classes.Progression.Subclass}.
     *
     * @param title        "Berserker Barbarian", as in "Matt the Berserker Barbarian"
     * @param featureReady false while the feature is only described, so the tree can say it isn't active yet
     */
    public record SubclassInfo(String id, String name, String title, String flavour, String featureName,
            String featureDescription, boolean featureReady) {
    }

    private record Entry(String id, String name, List<String> pros, List<String> cons, List<String> special,
            Boolean specialOnKey, String subclassTerm, List<SubclassEntry> subclasses) {
    }

    /** {@code feature} is "**Name**: what it does", like {@code special}. */
    private record SubclassEntry(String id, String name, String title, String flavour, String feature,
            Boolean featureReady) {
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
            Map<String, SubclassInfo> subclasses = new HashMap<>();
            byClass.values().forEach(info -> info.subclasses().forEach(sub -> subclasses.put(sub.id(), sub)));
            bySubclass = subclasses;
        }
        return byClass;
    }

    /** A subclass's text by id ("barbarian.berserker"), or null if class_info.json doesn't have it. */
    @Nullable
    public static SubclassInfo subclass(String id) {
        all();
        return bySubclass.get(id);
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
                    List<SubclassInfo> subclasses = new ArrayList<>();
                    for (SubclassEntry sub : orEmpty(e.subclasses())) {
                        String feature = forGame(sub.feature() == null ? "" : sub.feature());
                        int colon = feature.indexOf(": ");
                        subclasses.add(new SubclassInfo(sub.id(), sub.name(), sub.title(),
                                sub.flavour() == null ? "" : sub.flavour(),
                                colon < 0 ? feature : feature.substring(0, colon),
                                colon < 0 ? "" : feature.substring(colon + 2),
                                sub.featureReady() != null && sub.featureReady()));
                    }
                    map.put(character, new ClassInfo(character, e.name(), orEmpty(e.pros()), orEmpty(e.cons()),
                            orEmpty(e.special()), e.specialOnKey() == null || e.specialOnKey(),
                            e.subclassTerm() == null ? "subclass" : e.subclassTerm(), List.copyOf(subclasses)));
                }
            }
        } catch (Exception e) {
            DnDClasses.LOGGER.error("Couldn't load class info from " + RESOURCE, e);
        }
        return map;
    }

    private static <T> List<T> orEmpty(@Nullable List<T> list) {
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
