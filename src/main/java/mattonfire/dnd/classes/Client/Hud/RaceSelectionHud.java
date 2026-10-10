package mattonfire.dnd.classes.Client.Hud;

import java.util.ArrayList;
import java.util.List;

import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.widget.TooltipBuilder;
import io.github.cottonmc.cotton.gui.widget.WButton;
import io.github.cottonmc.cotton.gui.widget.WGridPanel;
import io.github.cottonmc.cotton.gui.widget.WLabel;
import io.github.cottonmc.cotton.gui.widget.data.Insets;
import io.github.cottonmc.cotton.gui.widget.icon.TextureIcon;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Client.PickerFlow;
import mattonfire.dnd.classes.Race.DndRace;
import mattonfire.dnd.classes.Race.DragonAncestry;
import mattonfire.dnd.classes.Race.RaceInfo;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * The race picker: a 2-column grid of the 8 races, with each race's summary, ability bonuses, body
 * modifiers and traits in its tooltip (from {@link RaceInfo}). Picking Dragonborn swaps the grid for the
 * three ancestries and a Back button. Opened by {@link PickerFlow}; Escape doesn't close it.
 */
@Environment(EnvType.CLIENT)
public class RaceSelectionHud extends LightweightGuiDescription {
    private static final int COLUMNS = 2;
    private static final int BUTTON_CELLS = 7;

    private record AncestryOption(DragonAncestry ancestry, String icon) {
    }

    private static final AncestryOption[] ANCESTRIES = {
            new AncestryOption(DragonAncestry.EMBER, "minecraft:textures/item/fire_charge.png"),
            new AncestryOption(DragonAncestry.FROST, "minecraft:textures/item/snowball.png"),
            new AncestryOption(DragonAncestry.STORM, "minecraft:textures/item/trident.png"),
    };

    /** The race grid. */
    public RaceSelectionHud() {
        this(false);
    }

    /** @param ancestryStep true for the Dragonborn ancestry buttons */
    public RaceSelectionHud(boolean ancestryStep) {
        WGridPanel root = new WGridPanel();
        root.setInsets(Insets.ROOT_PANEL);
        root.setGaps(2, 4);
        setRootPanel(root);
        if (ancestryStep) {
            buildAncestries(root);
        } else {
            buildRaces(root);
        }
        root.validate(this);
    }

    private static void buildRaces(WGridPanel root) {
        root.add(new WLabel(Text.translatable("gui.dndclasses.race_picker.title"), 0xFFFFFF), 0, 0,
                COLUMNS * BUTTON_CELLS, 1);
        int i = 0;
        for (RaceInfo info : RaceInfo.all().values()) {
            WButton button = new TooltipButton(new TextureIcon(new Identifier(info.icon())),
                    Text.translatable("race.dndclasses." + info.id().id()), tooltip(info));
            DndRace race = info.id();
            button.setOnClick(() -> {
                if (race.hasAncestry()) {
                    PickerFlow.showAncestryStep();
                } else {
                    PickerFlow.sendPick(race, DragonAncestry.NONE);
                }
            });
            root.add(button, (i % COLUMNS) * BUTTON_CELLS, 1 + i / COLUMNS, BUTTON_CELLS, 1);
            i++;
        }
        // A player who already has a class keeps it, so only a new player is told the class picker comes next.
        boolean classed = net.minecraft.client.MinecraftClient.getInstance().player instanceof PlayerEntityExt ext
                && ext.getDndClass() != null && ext.getDndClass() != DndCharacter.NONE;
        String hint = classed ? "gui.dndclasses.race_picker.hint_classed" : "gui.dndclasses.race_picker.hint";
        root.add(new WLabel(Text.translatable(hint).formatted(Formatting.GRAY)), 0,
                1 + (i + COLUMNS - 1) / COLUMNS, COLUMNS * BUTTON_CELLS, 1);
    }

    private static void buildAncestries(WGridPanel root) {
        root.add(new WLabel(Text.translatable("gui.dndclasses.race_picker.ancestry"), 0xFFFFFF), 0, 0,
                COLUMNS * BUTTON_CELLS, 1);
        int row = 1;
        for (AncestryOption option : ANCESTRIES) {
            List<Text> tip = List.of(Text.translatable("race.dndclasses.ancestry." + option.ancestry().id()
                    + ".tooltip").formatted(Formatting.GRAY));
            WButton button = new TooltipButton(new TextureIcon(new Identifier(option.icon())),
                    Text.translatable("race.dndclasses.ancestry." + option.ancestry().id()), tip);
            button.setOnClick(() -> PickerFlow.sendPick(DndRace.DRAGONBORN, option.ancestry()));
            root.add(button, BUTTON_CELLS / 2, row++, BUTTON_CELLS, 1);
        }
        WButton back = new WButton(Text.translatable("gui.back"));
        back.setOnClick(PickerFlow::showRaceStep);
        root.add(back, BUTTON_CELLS / 2, row + 1, BUTTON_CELLS, 1);
    }

    private static List<Text> tooltip(RaceInfo info) {
        List<Text> lines = new ArrayList<>();
        lines.add(Text.literal(info.name()).formatted(Formatting.GOLD, Formatting.BOLD));
        lines.add(Text.literal(info.summary()).formatted(Formatting.GRAY, Formatting.ITALIC));
        lines.add(Text.translatable("gui.dndclasses.race_picker.abilities", info.abilityText())
                .formatted(Formatting.AQUA));
        for (String stat : info.stats().describe()) {
            lines.add(Text.literal(stat).formatted(Formatting.GREEN));
        }
        lines.add(Text.translatable("gui.dndclasses.race_picker.traits").formatted(Formatting.DARK_PURPLE));
        for (String trait : info.traitsForGame()) {
            lines.add(Text.literal("• " + trait).formatted(Formatting.LIGHT_PURPLE));
        }
        return lines;
    }

    private static final class TooltipButton extends WButton {
        private final List<Text> tooltip;

        TooltipButton(TextureIcon icon, Text label, List<Text> tooltip) {
            super(icon, label);
            this.tooltip = tooltip;
        }

        @Override
        public void addTooltip(TooltipBuilder builder) {
            builder.add(tooltip.toArray(Text[]::new));
        }
    }
}
