package mattonfire.dnd.classes.Client.Hud;

import java.util.ArrayList;
import java.util.List;

import mattonfire.dnd.classes.ClassInfo;
import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.Abilities.ClassAbilities;
import mattonfire.dnd.classes.Abilities.Contribution;
import mattonfire.dnd.classes.Abilities.Proficiency;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Obstacles.ObstacleCodex;
import mattonfire.dnd.classes.Obstacles.ObstacleType;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.SkillChecks.Eligibility;
import mattonfire.dnd.classes.Race.DragonAncestry;
import mattonfire.dnd.classes.Race.RaceInfo;
import mattonfire.dnd.classes.Race.RaceLifecycle;
import mattonfire.dnd.classes.Progression.ClassProgress;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

/**
 * Builds the class guidebook's pages from {@link ClassInfo} and shows them in
 * the vanilla book screen. Text is laid out to the book's page size, so long
 * entries carry on onto the next page instead of being cut off.
 */
@Environment(EnvType.CLIENT)
public final class ClassGuidebookScreen {
    public static final String POWER_UP_KEY = "key.dnd-classes.power-up";

    // The vanilla book page is 114 pixels wide and 128 tall, with 9 pixel lines.
    private static final int PAGE_WIDTH = 114;
    private static final int PAGE_LINES = 128 / 9;

    private ClassGuidebookScreen() {
    }

    public static void open(PlayerEntity player) {
        DndCharacter character = player instanceof PlayerEntityExt ext ? ext.getDndClass() : null;
        MinecraftClient client = MinecraftClient.getInstance();
        List<Text> pages = buildPages(client.textRenderer, ClassInfo.get(character),
                RaceInfo.get(RaceLifecycle.raceOf(player)), RaceLifecycle.ancestryOf(player));
        client.setScreen(new BookScreen(new BookScreen.Contents() {
            @Override
            public int getPageCount() {
                return pages.size();
            }

            @Override
            public StringVisitable getPageUnchecked(int index) {
                return pages.get(index);
            }
        }));
    }

    static List<Text> buildPages(TextRenderer textRenderer, ClassInfo info, RaceInfo race, DragonAncestry ancestry) {
        Pages pages = new Pages(textRenderer);
        if (info == null) {
            pages.section(
                    Text.translatable("book.dndclasses.class_guidebook.title").formatted(Formatting.BOLD),
                    Text.literal(""),
                    Text.translatable("book.dndclasses.class_guidebook.no_class"));
            for (ClassInfo each : ClassInfo.all().values()) {
                classPages(pages, each, false);
            }
        } else {
            classPages(pages, info, true);
        }
        if (race != null) {
            heritagePage(pages, race, ancestry);
        }
        codexPages(pages, info == null ? null : info.id());
        return pages.finish();
    }

    private static void classPages(Pages pages, ClassInfo info, boolean yours) {
        List<Text> intro = new ArrayList<>();
        intro.add(Text.literal(info.name()).formatted(Formatting.BOLD, Formatting.DARK_BLUE));
        if (yours) {
            intro.add(Text.translatable("book.dndclasses.class_guidebook.your_class").formatted(Formatting.ITALIC));
            intro.add(Text.literal(""));
            intro.add(Text.translatable("book.dndclasses.class_guidebook.intro"));
        }
        intro.add(Text.literal(""));
        intro.addAll(List.of(bulletList("book.dndclasses.class_guidebook.pros", Formatting.DARK_GREEN, info.pros())));
        pages.section(intro.toArray(Text[]::new));

        pages.section(bulletList("book.dndclasses.class_guidebook.cons", Formatting.DARK_RED, info.cons()));

        List<Text> special = new ArrayList<>(
                List.of(bulletList("book.dndclasses.class_guidebook.special", Formatting.DARK_PURPLE, info.special())));
        special.add(Text.literal(""));
        if (info.specialOnKey()) {
            MutableText key = Text.keybind(POWER_UP_KEY).formatted(Formatting.BOLD, Formatting.DARK_PURPLE);
            special.add(Text.translatable("book.dndclasses.class_guidebook.key", key));
            special.add(Text.literal(""));
            special.add(Text.translatable("book.dndclasses.class_guidebook.rebind").formatted(Formatting.GRAY));
        } else {
            special.add(Text.translatable("book.dndclasses.class_guidebook.passive"));
        }
        pages.section(special.toArray(Text[]::new));

        rolePages(pages, info);

        if (yours) {
            abilitiesPage(pages, info);
        }

        if (!info.subclasses().isEmpty()) {
            pages.section(Text.translatable("book.dndclasses.class_guidebook.subclasses")
                    .formatted(Formatting.BOLD, Formatting.DARK_AQUA),
                    Text.translatable("book.dndclasses.class_guidebook.subclasses_intro",
                            ClassProgress.SUBCLASS_LEVEL));
            for (ClassInfo.SubclassInfo sub : info.subclasses()) {
                List<Text> lines = new ArrayList<>();
                lines.add(Text.literal(sub.name()).formatted(Formatting.BOLD, Formatting.DARK_PURPLE));
                if (yours && ClassProgress.client.dndClass == info.id() && ClassProgress.client.hasSubclass(sub.id())) {
                    lines.add(Text.translatable("book.dndclasses.class_guidebook.your_subclass")
                            .formatted(Formatting.ITALIC, Formatting.DARK_GREEN));
                }
                if (!sub.flavour().isEmpty()) {
                    lines.add(Text.literal(sub.flavour()).formatted(Formatting.ITALIC));
                }
                lines.add(Text.literal(""));
                lines.add(Text.translatable("book.dndclasses.class_guidebook.subclass_feature",
                        Text.literal(sub.featureName()).formatted(Formatting.BOLD)));
                lines.add(Text.literal(sub.featureDescription() + "."));
                pages.section(lines.toArray(Text[]::new));
            }
        }
    }

    /** "Your abilities": the class's starting scores, saves and skills, and how ability points work. */
    private static void abilitiesPage(Pages pages, ClassInfo info) {
        ClassAbilities abilities = info.abilities() == null ? ClassAbilities.NONE : info.abilities();
        List<Text> lines = new ArrayList<>();
        lines.add(Text.translatable("book.dndclasses.class_guidebook.your_abilities")
                .formatted(Formatting.BOLD, Formatting.DARK_AQUA));
        for (Ability a : Ability.values()) {
            int score = abilities.score(a);
            int mod = Ability.modifier(score);
            lines.add(Text.translatable(a.translationKey()).append(" ")
                    .append(Text.literal(score + " (" + (mod < 0 ? "" : "+") + mod + ")").formatted(Formatting.BOLD)));
        }
        lines.add(Text.literal(""));
        List<String> saves = new ArrayList<>();
        for (Ability a : abilities.saves()) {
            saves.add(Text.translatable(a.shortTranslationKey()).getString());
        }
        lines.add(Text.translatable("book.dndclasses.class_guidebook.saves",
                saves.isEmpty() ? "-" : String.join(", ", saves)).formatted(Formatting.DARK_GREEN));
        List<String> skills = new ArrayList<>();
        for (Skill s : Skill.values()) {
            Proficiency p = abilities.proficiency(s);
            if (p != Proficiency.NONE) {
                skills.add(Text.translatable(s.translationKey()).getString() + (p == Proficiency.EXPERTISE ? " ◆" : ""));
            }
        }
        lines.add(Text.translatable("book.dndclasses.class_guidebook.skills",
                skills.isEmpty() ? "-" : String.join(", ", skills)).formatted(Formatting.DARK_PURPLE));
        pages.section(lines.toArray(Text[]::new));
        pages.section(Text.translatable("book.dndclasses.class_guidebook.ability_points",
                ClassProgress.ASI_POINTS_PER_LEVEL, ClassProgress.ASI_LEVELS[0], ClassProgress.ASI_LEVELS[1],
                Contribution.NATURAL_CAP));
    }

    /** "Your heritage": the race's summary, ability bonuses, body modifiers and traits. */
    private static void heritagePage(Pages pages, RaceInfo race, DragonAncestry ancestry) {
        List<Text> lines = new ArrayList<>();
        lines.add(Text.literal(RaceLifecycle.displayName(race, ancestry)).formatted(Formatting.BOLD,
                Formatting.DARK_BLUE));
        lines.add(Text.translatable("book.dndclasses.class_guidebook.your_heritage").formatted(Formatting.ITALIC));
        lines.add(Text.literal(""));
        lines.add(Text.literal(race.summary() + "."));
        lines.add(Text.literal(""));
        lines.add(Text.translatable("book.dndclasses.class_guidebook.abilities", race.abilityText())
                .formatted(Formatting.DARK_AQUA));
        List<String> stats = race.stats().describe();
        if (!stats.isEmpty()) {
            lines.add(Text.literal(String.join(", ", stats)).formatted(Formatting.DARK_GREEN));
        }
        pages.section(lines.toArray(Text[]::new));
        pages.section(bulletList("book.dndclasses.class_guidebook.traits", Formatting.DARK_PURPLE, race.traits()));
    }

    /** "Your role" page, then "Obstacles you handle" (bold entries are the ones only this class can handle). */
    private static void rolePages(Pages pages, ClassInfo info) {
        List<Text> role = new ArrayList<>();
        role.add(Text.translatable("book.dndclasses.class_guidebook.role").formatted(Formatting.BOLD, Formatting.DARK_AQUA));
        role.add(Text.translatable("book.dndclasses.class_guidebook.role.primary",
                info.role().text().formatted(Formatting.BOLD)));
        role.add(Text.translatable("book.dndclasses.class_guidebook.role.secondary", info.secondaryRole().text()));
        if (!info.roleBlurb().isEmpty()) {
            role.add(Text.literal(""));
            role.add(Text.literal(ClassInfo.forGame(info.roleBlurb()) + "."));
        }
        role.add(Text.literal(""));
        role.add(Text.translatable(info.role().translationKey() + ".description").formatted(Formatting.GRAY));
        // "In a party": the role's synergy (Party/PartySynergies), flowing onto the next page if needed
        role.add(Text.literal(""));
        role.add(Text.translatable("book.dndclasses.class_guidebook.role.party").formatted(Formatting.BOLD,
                Formatting.DARK_AQUA));
        role.add(Text.translatable(info.role().translationKey() + ".synergy"));
        pages.section(role.toArray(Text[]::new));

        if (!info.obstacles().isEmpty()) {
            List<Text> obstacles = new ArrayList<>(List.of(bulletList("book.dndclasses.class_guidebook.obstacles",
                    Formatting.GOLD, info.obstacles())));
            obstacles.add(Text.literal(""));
            obstacles.add(Text.translatable("book.dndclasses.class_guidebook.obstacles.note").formatted(Formatting.GRAY));
            pages.section(obstacles.toArray(Text[]::new));
        }
    }

    /**
     * The Obstacle Codex: every obstacle type in the registry ({@link ObstacleCodex#entries()}), who can roll for
     * it and the fallback for everyone else. {@code yours} (if any) is picked out in green.
     */
    private static void codexPages(Pages pages, @Nullable DndCharacter yours) {
        pages.section(Text.translatable("book.dndclasses.class_guidebook.codex").formatted(Formatting.BOLD,
                Formatting.DARK_AQUA), Text.literal(""),
                Text.translatable("book.dndclasses.class_guidebook.codex.intro"), Text.literal(""),
                Text.translatable("book.dndclasses.class_guidebook.codex.key").formatted(Formatting.GRAY));
        for (ObstacleType type : ObstacleCodex.entries()) {
            List<Text> lines = new ArrayList<>();
            lines.add(Text.translatable(type.translationKey()).formatted(Formatting.BOLD, Formatting.DARK_PURPLE));
            List<ObstacleType> variants = ObstacleCodex.variants(type);
            if (!variants.isEmpty()) {
                MutableText also = Text.empty();
                for (int i = 0; i < variants.size(); i++) {
                    also.append(i == 0 ? "" : ", ").append(Text.translatable(variants.get(i).translationKey()));
                }
                lines.add(Text.translatable("book.dndclasses.class_guidebook.codex.variants", also)
                        .formatted(Formatting.GRAY, Formatting.ITALIC));
            }
            DndCharacter only = ObstacleCodex.exclusiveSolver(type);
            List<DndCharacter> primary = ObstacleCodex.classes(type, Eligibility.PRIMARY);
            List<DndCharacter> secondary = ObstacleCodex.classes(type, Eligibility.SECONDARY);
            if (only != null) {
                lines.add(Text.translatable("book.dndclasses.class_guidebook.codex.only", classNames(primary, yours)));
            } else if (!primary.isEmpty()) {
                lines.add(Text.translatable("book.dndclasses.class_guidebook.codex.primary", classNames(primary, yours)));
            }
            if (!secondary.isEmpty()) {
                lines.add(Text.translatable("book.dndclasses.class_guidebook.codex.secondary",
                        classNames(secondary, yours)));
            }
            if (primary.isEmpty() && secondary.isEmpty() && type.ownWhoText()) {
                lines.add(Text.translatable(type.translationKey() + ".who"));
            }
            if (ObstacleCodex.anyoneMayTry(type)) {
                lines.add(Text.translatable("book.dndclasses.class_guidebook.codex.anyone"));
            }
            lines.add(Text.translatable("book.dndclasses.class_guidebook.codex.fallback",
                    ObstacleCodex.fallbackSentence(Text.translatable(type.translationKey() + ".fallback").getString()))
                    .formatted(Formatting.DARK_GRAY));
            pages.block(lines.toArray(Text[]::new));
        }
    }

    /** "Wizard, Warlock", with {@code yours} bold and green. */
    private static Text classNames(List<DndCharacter> classes, @Nullable DndCharacter yours) {
        MutableText out = Text.empty();
        for (int i = 0; i < classes.size(); i++) {
            DndCharacter c = classes.get(i);
            MutableText name = Text.literal(Progression.name(c));
            out.append(i == 0 ? "" : ", ").append(c == yours ? name.formatted(Formatting.BOLD, Formatting.DARK_GREEN)
                    : name.formatted(Formatting.BOLD));
        }
        return out;
    }

    private static Text[] bulletList(String headerKey, Formatting color, List<String> points) {
        Text[] lines = new Text[points.size() + 1];
        lines[0] = Text.translatable(headerKey).formatted(Formatting.BOLD, color);
        for (int i = 0; i < points.size(); i++) {
            lines[i + 1] = Text.literal("• ").append(withBold(points.get(i)));
        }
        return lines;
    }

    /** In-game text for a point, keeping its leading **bold** name bold. */
    private static Text withBold(String point) {
        String text = point.replace(" (done)", "").replace("(done)", "").trim();
        if (text.startsWith("**")) {
            int end = text.indexOf("**", 2);
            if (end > 2) {
                return Text.empty().append(Text.literal(text.substring(2, end)).formatted(Formatting.BOLD))
                        .append(text.substring(end + 2).replace("*", ""));
            }
        }
        return Text.literal(ClassInfo.forGame(text));
    }

    /** Packs paragraphs onto pages, starting each section on a fresh page. */
    private static final class Pages {
        private final TextRenderer textRenderer;
        private final List<Text> pages = new ArrayList<>();
        private MutableText current;
        private int lines;

        Pages(TextRenderer textRenderer) {
            this.textRenderer = textRenderer;
        }

        void section(Text... paragraphs) {
            newPage();
            for (Text paragraph : paragraphs) {
                int height = Math.max(1, textRenderer.wrapLines(paragraph, PAGE_WIDTH).size());
                if (lines > 0 && lines + height > PAGE_LINES) {
                    newPage();
                }
                if (lines > 0) {
                    current.append("\n");
                }
                current.append(paragraph);
                lines += height;
            }
        }

        /**
         * Paragraphs kept together on one page (moving to a fresh page if they don't fit), after a blank line
         * when the page already has something on it. For short entries packed several to a page.
         */
        void block(Text... paragraphs) {
            int height = 0;
            for (Text paragraph : paragraphs) {
                height += Math.max(1, textRenderer.wrapLines(paragraph, PAGE_WIDTH).size());
            }
            if (lines > 0 && lines + 1 + height > PAGE_LINES) {
                newPage();
            }
            if (lines > 0) {
                current.append("\n");
                lines++;
            }
            for (Text paragraph : paragraphs) {
                if (lines > 0) {
                    current.append("\n");
                }
                current.append(paragraph);
                lines += Math.max(1, textRenderer.wrapLines(paragraph, PAGE_WIDTH).size());
            }
        }

        private void newPage() {
            if (current != null && lines > 0) {
                pages.add(current);
            }
            current = Text.empty();
            lines = 0;
        }

        List<Text> finish() {
            newPage();
            return pages;
        }
    }
}
