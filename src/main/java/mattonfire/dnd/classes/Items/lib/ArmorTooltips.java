package mattonfire.dnd.classes.Items.lib;

import java.util.List;
import java.util.Set;

import mattonfire.dnd.classes.Client.TooltipTextWidth;
import mattonfire.dnd.classes.Config.FAConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import mattonfire.dnd.classes.Race.DndRace;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Word-wrapped armor descriptions ("<item key>.tooltip"), shared by DndArmorItem and FAArmorItem, and the
 * "Made for" races line of racial gear (armor and weapons).
 */
public final class ArmorTooltips {
    /** Rough width of one character, used when there's no font to measure with (dedicated server). */
    private static final int FALLBACK_CHAR_WIDTH = 6;

    private ArmorTooltips() {
    }

    /** Adds "Made for Elves" (or "Made for Dwarves and Gnomes") in gold. Nothing for gear with no races. */
    public static void appendRaces(Set<DndRace> races, List<Text> tooltip) {
        if (races.isEmpty()) {
            return;
        }
        MutableText names = Text.empty();
        int i = 0;
        for (DndRace race : DndRace.values()) {
            if (!races.contains(race)) {
                continue;
            }
            if (i > 0) {
                names.append(i == races.size() - 1 ? Text.translatable("tooltip.dndclasses.racial_gear.and")
                        : Text.literal(", "));
            }
            names.append(Text.translatable("tooltip.dndclasses.racial_gear.race." + race.id()));
            i++;
        }
        tooltip.add(Text.translatable("tooltip.dndclasses.racial_gear.made_for", names).formatted(Formatting.GOLD));
    }

    public static void appendDescription(String itemTranslationKey, List<Text> tooltip) {
        String translatedText = Text.translatable(itemTranslationKey + ".tooltip").getString();

        int maxWidth = FAConfig.getValues().descrtiptionsLength();
        if (maxWidth < 20 || maxWidth > 1000) {
            maxWidth = 250;
        }

        for (String line : translatedText.split("\n")) {
            StringBuilder currentLine = new StringBuilder();

            for (String word : line.split(" ")) {
                // Start a new line if adding this word would go past maxWidth
                if (width(currentLine + word) > maxWidth) {
                    tooltip.add(Text.literal(currentLine.toString()));
                    currentLine = new StringBuilder("§7");
                }

                // Insert a space (and colour code) unless we're at the start of the line
                if (currentLine.length() > 2) {
                    currentLine.append(" ");
                    currentLine.append("§7");
                }

                currentLine.append(word);
            }

            if (currentLine.length() > 0) {
                tooltip.add(Text.literal(currentLine.toString()));
            }
        }
    }

    private static int width(String text) {
        // Item code is common: only touch the client font on the client, so a dedicated server
        // never loads MinecraftClient.
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return TooltipTextWidth.of(text);
        }
        return text.length() * FALLBACK_CHAR_WIDTH;
    }
}
