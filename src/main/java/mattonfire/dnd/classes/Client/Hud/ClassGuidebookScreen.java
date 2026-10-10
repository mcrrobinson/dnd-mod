package mattonfire.dnd.classes.Client.Hud;

import java.util.ArrayList;
import java.util.List;

import mattonfire.dnd.classes.ClassInfo;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
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

    private static Text[] bulletList(String headerKey, Formatting color, List<String> points) {
        Text[] lines = new Text[points.size() + 1];
        lines[0] = Text.translatable(headerKey).formatted(Formatting.BOLD, color);
        for (int i = 0; i < points.size(); i++) {
            lines[i + 1] = Text.literal("• " + ClassInfo.forGame(points.get(i)));
        }
        return lines;
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
