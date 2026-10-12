package mattonfire.dnd.classes.Client.Hud;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.client.ScreenDrawing;
import io.github.cottonmc.cotton.gui.widget.TooltipBuilder;
import io.github.cottonmc.cotton.gui.widget.WButton;
import io.github.cottonmc.cotton.gui.widget.WLabel;
import io.github.cottonmc.cotton.gui.widget.WPlainPanel;
import io.github.cottonmc.cotton.gui.widget.WWidget;
import io.github.cottonmc.cotton.gui.widget.data.Insets;
import io.github.cottonmc.cotton.gui.widget.icon.Icon;
import io.github.cottonmc.cotton.gui.widget.icon.TextureIcon;
import mattonfire.dnd.classes.ClassInfo;
import mattonfire.dnd.classes.ClassStats;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.Abilities.ClassAbilities;
import mattonfire.dnd.classes.Client.PickerFlow;
import mattonfire.dnd.classes.Race.DndRace;
import mattonfire.dnd.classes.Race.DragonAncestry;
import mattonfire.dnd.classes.Race.RaceInfo;
import mattonfire.dnd.classes.Race.RaceLifecycle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Character creation for a new player: the race grid (with the Dragonborn ancestry step) and the class grid on
 * the left, a summary of the combined result on the right (body, ability scores with the racial bonuses, race
 * traits and the class's special) and a Confirm button that sends both picks in one packet, so the server prints
 * one combined intro. Opened by {@link PickerFlow} when the player has neither a race nor a class; a player
 * missing only one of them gets {@link RaceSelectionHud} or {@link ClassSelectionHud}. Escape doesn't close it.
 * <p>
 * Wide screens get labelled buttons; narrow ones (under {@value #WIDE_MIN_WIDTH} scaled pixels) get icon-only
 * buttons with the name in the tooltip, so the screen fits Minecraft's smallest scaled size (320x240).
 */
@Environment(EnvType.CLIENT)
public class CharacterCreationHud extends LightweightGuiDescription {
    static final int WIDE_MIN_WIDTH = 600;
    private static final int COLUMNS = 4;
    private static final int GAP = 2;
    private static final int ROW = 20 + GAP;
    private static final int TOOLTIP_WIDTH = 200;

    /** What the player has clicked so far. Kept across the rebuild for the ancestry step. */
    public static final class Choice {
        DndRace race = DndRace.NONE;
        DragonAncestry ancestry = DragonAncestry.NONE;
        DndCharacter dndClass = DndCharacter.NONE;
        boolean ancestryStep;
        boolean sent;

        boolean complete() {
            return race != DndRace.NONE && race.hasAncestry() == (ancestry != DragonAncestry.NONE)
                    && dndClass != DndCharacter.NONE;
        }
    }

    private record AncestryOption(DragonAncestry ancestry, String icon) {
    }

    private static final AncestryOption[] ANCESTRIES = {
            new AncestryOption(DragonAncestry.EMBER, "minecraft:textures/item/fire_charge.png"),
            new AncestryOption(DragonAncestry.FROST, "minecraft:textures/item/snowball.png"),
            new AncestryOption(DragonAncestry.STORM, "minecraft:textures/item/trident.png"),
    };

    private final Choice choice;
    private final boolean wide;
    private final WButton confirm;

    public CharacterCreationHud() {
        this(new Choice());
    }

    public CharacterCreationHud(Choice choice) {
        this.choice = choice;
        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        this.wide = screenWidth >= WIDE_MIN_WIDTH;

        int buttonWidth = wide ? 98 : 20;
        int gridWidth = COLUMNS * buttonWidth + (COLUMNS - 1) * GAP;
        int summaryWidth = Math.max(140, Math.min(wide ? 230 : 200, screenWidth - gridWidth - 34));
        int summaryX = gridWidth + 8;

        WPlainPanel root = new WPlainPanel();
        root.setInsets(Insets.ROOT_PANEL);
        setRootPanel(root);

        root.add(new WLabel(Text.translatable("gui.dndclasses.character_picker.title")), 0, 0,
                gridWidth + 8 + summaryWidth, 10);

        // Race: 2 rows of 4, or the ancestry step in the same space.
        String raceLabel = !choice.ancestryStep ? "gui.dndclasses.character_picker.race"
                : wide ? "gui.dndclasses.race_picker.ancestry" : "gui.dndclasses.character_picker.ancestry";
        root.add(new WLabel(Text.translatable(raceLabel)), 0, 14, gridWidth, 10);
        int raceY = 26;
        if (choice.ancestryStep) {
            int width = (gridWidth - 2 * GAP) / 3;
            for (int i = 0; i < ANCESTRIES.length; i++) {
                AncestryOption option = ANCESTRIES[i];
                List<Text> tip = List.of(
                        Text.translatable("race.dndclasses.ancestry." + option.ancestry().id())
                                .formatted(Formatting.GOLD),
                        Text.translatable("race.dndclasses.ancestry." + option.ancestry().id() + ".tooltip")
                                .formatted(Formatting.GRAY));
                ChoiceButton button = new ChoiceButton(new TextureIcon(new Identifier(option.icon())),
                        wide ? Text.translatable("race.dndclasses.ancestry." + option.ancestry().id()) : null, tip,
                        () -> choice.ancestry == option.ancestry());
                button.setOnClick(() -> {
                    choice.ancestry = option.ancestry();
                    refresh();
                });
                root.add(button, i * (width + GAP), raceY, width, 20);
            }
            WButton back = new WButton(Text.translatable("gui.back"));
            back.setOnClick(() -> {
                choice.ancestryStep = false;
                reopen();
            });
            root.add(back, 0, raceY + ROW, wide ? buttonWidth : 2 * buttonWidth + GAP, 20);
        } else {
            int i = 0;
            for (RaceInfo info : RaceInfo.all().values()) {
                DndRace race = info.id();
                ChoiceButton button = new ChoiceButton(new TextureIcon(new Identifier(info.icon())),
                        wide ? Text.translatable("race.dndclasses." + race.id()) : null, raceTooltip(info),
                        () -> choice.race == race);
                button.setOnClick(() -> {
                    if (choice.race != race) {
                        choice.ancestry = DragonAncestry.NONE;
                    }
                    choice.race = race;
                    if (race.hasAncestry()) {
                        choice.ancestryStep = true;
                        reopen();
                    } else {
                        refresh();
                    }
                });
                root.add(button, (i % COLUMNS) * (buttonWidth + GAP), raceY + (i / COLUMNS) * ROW, buttonWidth, 20);
                i++;
            }
        }

        // Class: 4 rows of 4.
        int classLabelY = raceY + 2 * ROW + 4;
        root.add(new WLabel(Text.translatable("gui.dndclasses.character_picker.class")), 0, classLabelY, gridWidth,
                10);
        int classY = classLabelY + 12;
        int i = 0;
        for (ClassSelectionHud.ClassOption option : ClassSelectionHud.CLASSES) {
            DndCharacter dndClass = DndCharacter.fromValue(option.classID());
            ClassChoiceButton button = new ClassChoiceButton(new TextureIcon(new Identifier(option.icon())),
                    wide ? Text.translatable("class.dndclasses." + option.key()) : null, ClassInfo.get(dndClass),
                    () -> choice.dndClass == dndClass);
            button.setOnClick(() -> {
                choice.dndClass = dndClass;
                refresh();
            });
            root.add(button, (i % COLUMNS) * (buttonWidth + GAP), classY + (i / COLUMNS) * ROW, buttonWidth, 20);
            i++;
        }
        int bottom = classY + 4 * ROW - GAP;

        root.add(new Summary(choice), summaryX, 14, summaryWidth, bottom - 14);
        confirm = new WButton(Text.translatable("gui.dndclasses.character_picker.confirm"));
        confirm.setOnClick(() -> {
            if (choice.complete() && !choice.sent) {
                choice.sent = true;
                PickerFlow.sendCharacterPick(choice.race, choice.ancestry, choice.dndClass);
                refresh();
            }
        });
        root.add(confirm, summaryX, bottom + 4, summaryWidth, 20);
        if (wide) {
            root.add(new WLabel(Text.translatable("gui.dndclasses.character_picker.hint")),
                    0, bottom + 10, gridWidth, 10);
        }
        refresh();
        root.validate(this);
    }

    public Choice choice() {
        return choice;
    }

    /** Whether this layout has labelled buttons; a resize across {@link #WIDE_MIN_WIDTH} needs a rebuild. */
    public boolean fits(int scaledWidth) {
        return wide == scaledWidth >= WIDE_MIN_WIDTH;
    }

    private void refresh() {
        confirm.setEnabled(choice.complete() && !choice.sent);
    }

    private void reopen() {
        PickerFlow.showCharacterStep(choice);
    }

    static List<Text> raceTooltip(RaceInfo info) {
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

    /** Draws a gold frame around a selected button. */
    static void paintSelected(MatrixStack matrices, int x, int y, int width, int height) {
        int color = 0xFFFFD24A;
        ScreenDrawing.coloredRect(matrices, x - 1, y - 1, width + 2, 1, color);
        ScreenDrawing.coloredRect(matrices, x - 1, y + height, width + 2, 1, color);
        ScreenDrawing.coloredRect(matrices, x - 1, y, 1, height, color);
        ScreenDrawing.coloredRect(matrices, x + width, y, 1, height, color);
    }

    private static void addWrapped(TooltipBuilder builder, List<Text> lines) {
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        for (Text line : lines) {
            builder.add(font.wrapLines(line, TOOLTIP_WIDTH).toArray(OrderedText[]::new));
        }
    }

    /** A race or ancestry button: icon, optional label, a tooltip and a frame while selected. */
    private static final class ChoiceButton extends WButton {
        private final List<Text> tooltip;
        private final BooleanSupplier selected;

        ChoiceButton(Icon icon, Text label, List<Text> tooltip, BooleanSupplier selected) {
            super(icon);
            if (label != null) {
                setLabel(label);
            }
            this.tooltip = tooltip;
            this.selected = selected;
        }

        @Override
        public void paint(MatrixStack matrices, int x, int y, int mouseX, int mouseY) {
            super.paint(matrices, x, y, mouseX, mouseY);
            if (selected.getAsBoolean()) {
                paintSelected(matrices, x, y, getWidth(), getHeight());
            }
        }

        @Override
        public void addTooltip(TooltipBuilder builder) {
            addWrapped(builder, tooltip);
        }
    }

    /** A class button: the class picker's tooltip, its role icon when labelled, and a frame while selected. */
    private static final class ClassChoiceButton extends RoleButton {
        private final BooleanSupplier selected;

        ClassChoiceButton(Icon icon, Text label, ClassInfo info, BooleanSupplier selected) {
            super(icon, label, info, label != null);
            this.selected = selected;
        }

        @Override
        public void paint(MatrixStack matrices, int x, int y, int mouseX, int mouseY) {
            super.paint(matrices, x, y, mouseX, mouseY);
            if (selected.getAsBoolean()) {
                paintSelected(matrices, x, y, getWidth(), getHeight());
            }
        }
    }

    /** The combined result, redrawn from the choice every frame. */
    private static final class Summary extends WWidget {
        private static final int PAD = 4;
        private final Choice choice;

        Summary(Choice choice) {
            this.choice = choice;
        }

        @Override
        public boolean canResize() {
            return true;
        }

        @Override
        public void paint(MatrixStack matrices, int x, int y, int mouseX, int mouseY) {
            ScreenDrawing.coloredRect(matrices, x, y, getWidth(), getHeight(), 0xFF8B8B8B);
            ScreenDrawing.coloredRect(matrices, x + 1, y + 1, getWidth() - 2, getHeight() - 2, 0xFF1E1E1E);
            TextRenderer font = MinecraftClient.getInstance().textRenderer;
            int width = getWidth() - 2 * PAD;
            List<OrderedText> lines = layout(font, width, true);
            if (lines.size() * (font.fontHeight + 1) > getHeight() - 2 * PAD) {
                lines = layout(font, width, false);
            }
            int lineY = y + PAD;
            for (OrderedText line : lines) {
                if (lineY + font.fontHeight > y + getHeight() - PAD) {
                    break;
                }
                font.drawWithShadow(matrices, line, x + PAD, lineY, 0xFFFFFF);
                lineY += font.fontHeight + 1;
            }
        }

        /** The full text (traits and the class's special), for when the panel only fits the names. */
        @Override
        public void addTooltip(TooltipBuilder builder) {
            if (choice.race != DndRace.NONE || choice.dndClass != DndCharacter.NONE) {
                addWrapped(builder, summary(choice, true));
            }
        }

        private List<OrderedText> layout(TextRenderer font, int width, boolean fullTraits) {
            List<Text> texts = summary(choice, fullTraits);
            List<OrderedText> lines = new ArrayList<>();
            for (Text text : texts) {
                lines.addAll(font.wrapLines(text, width));
            }
            return lines;
        }
    }

    /** The summary panel's lines. fullTraits false shows only the trait names. */
    static List<Text> summary(Choice choice, boolean fullTraits) {
        List<Text> lines = new ArrayList<>();
        RaceInfo race = RaceInfo.get(choice.race);
        ClassInfo dndClass = ClassInfo.get(choice.dndClass);
        String raceName = race == null ? "?" : RaceLifecycle.displayName(race, choice.ancestry);
        String className = dndClass == null ? "?" : dndClass.name();
        if (race == null && dndClass == null) {
            lines.add(Text.translatable("gui.dndclasses.character_picker.empty").formatted(Formatting.GRAY));
        } else {
            lines.add(Text.literal(raceName + " " + className).formatted(Formatting.GOLD, Formatting.BOLD));
        }
        if (dndClass != null) {
            lines.add(Text.translatable("gui.dndclasses.class_picker.role", dndClass.role().text(),
                    dndClass.secondaryRole().text()).formatted(Formatting.GRAY));
        }

        // Body: the class's base values with the race's modifiers on top.
        RaceInfo.Stats stats = race == null ? RaceInfo.Stats.NONE : race.stats();
        double hearts = (ClassStats.maxHealth(choice.dndClass) + stats.maxHealth()) / 2.0;
        long speed = Math.round(ClassStats.movementSpeed(choice.dndClass) * (1 + stats.speed())
                / ClassStats.movementSpeed(DndCharacter.NONE) * 100) - 100;
        lines.add(Text.translatable("gui.dndclasses.character_picker.body", number(hearts),
                (speed >= 0 ? "+" : "") + speed).formatted(Formatting.GREEN));
        for (String extra : stats.describe()) {
            if (!extra.startsWith("Speed") && !extra.startsWith("Max health")) {
                lines.add(Text.literal(extra).formatted(Formatting.GREEN));
            }
        }

        // Ability scores: the class's array plus the racial bonuses (aqua where the race adds).
        ClassAbilities abilities = ClassAbilities.of(choice.dndClass);
        Map<String, Integer> bonuses = race == null ? Map.of() : race.abilityBonuses();
        lines.add(Text.translatable("gui.dndclasses.character_picker.abilities").formatted(Formatting.WHITE));
        MutableText row = Text.empty();
        int n = 0;
        for (Ability ability : Ability.values()) {
            int bonus = bonuses.getOrDefault(ability.shortKey(), 0);
            int score = abilities.score(ability) + bonus;
            int mod = Ability.modifier(score);
            row.append(Text.literal(ability.shortKey() + " " + score + " (" + (mod >= 0 ? "+" : "") + mod + ")")
                    .formatted(bonus > 0 ? Formatting.AQUA : Formatting.GRAY));
            if (++n % 3 == 0) {
                lines.add(row);
                row = Text.empty();
            } else {
                row.append(Text.literal("  "));
            }
        }

        if (race != null) {
            lines.add(Text.translatable("gui.dndclasses.race_picker.traits").formatted(Formatting.DARK_PURPLE));
            for (String trait : race.traits()) {
                String text = fullTraits ? ClassInfo.forGame(trait) : traitName(trait);
                lines.add(Text.literal("• " + text).formatted(Formatting.LIGHT_PURPLE));
            }
        }
        if (dndClass != null && !dndClass.special().isEmpty()) {
            lines.add(Text.literal("★ " + (fullTraits ? ClassInfo.forGame(dndClass.special().get(0))
                    : traitName(dndClass.special().get(0)))).formatted(Formatting.YELLOW));
        }
        return lines;
    }

    /** "**Darkvision**: caves are dim" -> "Darkvision". */
    static String traitName(String trait) {
        int start = trait.indexOf("**");
        int end = start < 0 ? -1 : trait.indexOf("**", start + 2);
        return end < 0 ? ClassInfo.forGame(trait) : trait.substring(start + 2, end);
    }

    private static String number(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
