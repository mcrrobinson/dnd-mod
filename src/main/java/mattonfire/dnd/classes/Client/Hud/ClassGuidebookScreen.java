package mattonfire.dnd.classes.Client.Hud;

import java.util.ArrayList;
import java.util.List;

import mattonfire.dnd.classes.ClassInfo;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
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
        List<Text> pages = buildPages(client.textRenderer, ClassInfo.get(character));
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

    static List<Text> buildPages(TextRenderer textRenderer, ClassInfo info) {
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
        pages.section(role.toArray(Text[]::new));

        if (!info.obstacles().isEmpty()) {
            List<Text> obstacles = new ArrayList<>(List.of(bulletList("book.dndclasses.class_guidebook.obstacles",
                    Formatting.GOLD, info.obstacles())));
            obstacles.add(Text.literal(""));
            obstacles.add(Text.translatable("book.dndclasses.class_guidebook.obstacles.note").formatted(Formatting.GRAY));
            pages.section(obstacles.toArray(Text[]::new));
        }
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
