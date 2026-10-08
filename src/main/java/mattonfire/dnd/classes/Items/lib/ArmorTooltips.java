package mattonfire.dnd.classes.Items.lib;

import java.util.List;

import mattonfire.dnd.classes.Client.TooltipTextWidth;
import mattonfire.dnd.classes.Config.FAConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;

/** Word-wrapped armor descriptions ("<item key>.tooltip"), shared by DndArmorItem and FAArmorItem. */
final class ArmorTooltips {
    /** Rough width of one character, used when there's no font to measure with (dedicated server). */
    private static final int FALLBACK_CHAR_WIDTH = 6;

    private ArmorTooltips() {
    }

    static void appendDescription(String itemTranslationKey, List<Text> tooltip) {
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
