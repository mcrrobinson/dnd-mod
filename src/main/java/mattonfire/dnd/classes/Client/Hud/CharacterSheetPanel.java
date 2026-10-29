package mattonfire.dnd.classes.Client.Hud;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.Abilities.AbilityIncreases;
import mattonfire.dnd.classes.Abilities.Advantage;
import mattonfire.dnd.classes.Abilities.CharacterSheet;
import mattonfire.dnd.classes.Abilities.Contribution;
import mattonfire.dnd.classes.Abilities.Proficiency;
import mattonfire.dnd.classes.Abilities.RollQuery;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Feats.Feat;
import mattonfire.dnd.classes.Feats.FeatChoice;
import mattonfire.dnd.classes.Progression.ClassProgress;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * The Character tab of {@link SkillTreeScreen}: the synced {@link CharacterSheet#client} as six ability boxes
 * (score, modifier, a gold corner when the save is proficient), saving throws, a scrollable skill list
 * (● proficient, ◆ expertise, ½ half) and passive scores, with the sheet's breakdown in each tooltip. At an
 * Attunement Table with ability points left, "+" buttons on the boxes spend them (C2S
 * {@link AbilityIncreases#C2S_ABILITY_INCREASE}; the server checks everything again). The Feats box under the
 * ability boxes lists the class's feats, and at a table with a feat slot or 2 ability points a "Choose a feat"
 * button opens the {@link FeatPickerScreen}. The passive scores sit under the skills.
 */
final class CharacterSheetPanel {
    private static final int GOLD = 0xFFE0B040;
    private static final int BORDER = 0xFF6B4FA0;
    private static final int BOX_BG = 0xFF1C1A28;
    private static final int BOX_HOVER = 0xFF2C2840;

    private static final int BOX_W = 46;
    private static final int BOX_H = 34;
    private static final int BOX_GAP = 4;
    private static final int BOXES_LEFT = 8;
    private static final int BOXES_TOP = 44;
    private static final int PLUS = 9;

    private static final int RIGHT_LEFT = 110;
    private static final int RIGHT_WIDTH = 142;
    private static final int SAVES_TOP = 44;
    private static final int ROW = 10;
    private static final int SKILLS_TOP = 104;
    private static final int SKILL_ROWS = 7;

    /** Passive scores: three rows under the skills, in the right column. */
    private static final int PASSIVES_TOP = SKILLS_TOP + SKILL_ROWS * ROW + 3;
    private static final Skill[] PASSIVES = { Skill.PERCEPTION, Skill.INSIGHT, Skill.STEALTH };
    /** The Feats box, under the ability boxes. */
    private static final int FEATS_TOP = 162;
    private static final int FEAT_ROWS = 3;
    private static final int FEATURES_TOP = 204;
    static final Identifier FEAT_ICONS = new Identifier(mattonfire.dnd.classes.DnDClasses.MOD_ID,
            "textures/gui/feats.png");
    static final int FEAT_CELL = 16;

    private int skillScroll;

    private static int maxSkillScroll() {
        return Math.max(0, Skill.values().length - SKILL_ROWS);
    }

    private static int boxX(int left, Ability a) {
        return left + BOXES_LEFT + (a.ordinal() % 2) * (BOX_W + BOX_GAP);
    }

    private static int boxY(int top, Ability a) {
        return top + BOXES_TOP + (a.ordinal() / 2) * (BOX_H + BOX_GAP);
    }

    private static boolean over(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Whether a "+" shows on the ability's box. */
    private static boolean canIncrease(boolean attuning, CharacterSheet sheet, Ability a) {
        return attuning && ClassProgress.client.asiPoints() > 0 && AbilityIncreases.belowCap(sheet, a);
    }

    private static String signed(int n) {
        return n < 0 ? Integer.toString(n) : "+" + n;
    }

    /** Draws the tab and returns the hovered element's tooltip, or null. */
    @Nullable
    List<OrderedText> render(MatrixStack matrices, TextRenderer text, int left, int top, int width, int height,
            int mouseX, int mouseY, boolean attuning) {
        CharacterSheet sheet = CharacterSheet.client;
        ClassProgress progress = ClassProgress.client;
        int centerX = left + width / 2;
        List<OrderedText> tooltip = null;

        // Summary: proficiency, attack, crit range, then ability points.
        MutableText summary = Text.literal("Proficiency ").formatted(Formatting.GRAY)
                .append(Text.literal(signed(sheet.proficiencyBonus())).formatted(Formatting.WHITE))
                .append(Text.literal("   Attack ").formatted(Formatting.GRAY))
                .append(Text.literal(signed(sheet.attackBonus())).formatted(Formatting.WHITE))
                .append(Text.literal("   Crit ").formatted(Formatting.GRAY))
                .append(Text.literal(sheet.critRange() + "-20").formatted(Formatting.WHITE));
        DrawableHelper.drawCenteredTextWithShadow(matrices, text, summary, centerX, top + 20, 0xFFFFFF);
        DrawableHelper.drawCenteredTextWithShadow(matrices, text, pointsText(progress, attuning), centerX, top + 31,
                0xFFFFFF);

        // Ability boxes.
        for (Ability a : Ability.values()) {
            int x = boxX(left, a);
            int y = boxY(top, a);
            boolean hover = over(mouseX, mouseY, x, y, BOX_W, BOX_H);
            DrawableHelper.fill(matrices, x - 1, y - 1, x + BOX_W + 1, y + BOX_H + 1, hover ? GOLD : BORDER);
            DrawableHelper.fill(matrices, x, y, x + BOX_W, y + BOX_H, hover ? BOX_HOVER : BOX_BG);
            if (sheet.saveProficiency(a) != Proficiency.NONE) {
                // Gold corner: proficient in this save.
                DrawableHelper.fill(matrices, x + 2, y + 2, x + 6, y + 6, GOLD);
            }
            DrawableHelper.drawCenteredTextWithShadow(matrices, text,
                    Text.translatable(a.shortTranslationKey()).formatted(Formatting.GRAY), x + BOX_W / 2, y + 3,
                    0xFFFFFF);
            matrices.push();
            matrices.translate(x + BOX_W / 2.0, y + 12, 0);
            matrices.scale(1.5F, 1.5F, 1.0F);
            String score = Integer.toString(sheet.score(a));
            text.drawWithShadow(matrices, score, -text.getWidth(score) / 2.0F, 0, 0xFFFFFF);
            matrices.pop();
            int mod = sheet.modifier(a);
            DrawableHelper.drawCenteredTextWithShadow(matrices, text, signed(mod), x + BOX_W / 2, y + 25,
                    mod > 0 ? 0x80E080 : mod < 0 ? 0xE08080 : 0xA0A0A0);

            if (canIncrease(attuning, sheet, a)) {
                int px = x + BOX_W - PLUS - 1;
                int py = y + 1;
                boolean plusHover = over(mouseX, mouseY, px, py, PLUS, PLUS);
                DrawableHelper.fill(matrices, px, py, px + PLUS, py + PLUS, plusHover ? 0xFF70D070 : 0xFF3A8A3A);
                DrawableHelper.fill(matrices, px + 2, py + 4, px + PLUS - 2, py + 5, 0xFFFFFFFF);
                DrawableHelper.fill(matrices, px + 4, py + 2, px + 5, py + PLUS - 2, 0xFFFFFFFF);
            }
            if (hover) {
                tooltip = abilityTooltip(sheet, a, attuning);
            }
        }

        // Saving throws: two columns of three.
        int rightX = left + RIGHT_LEFT;
        text.drawWithShadow(matrices, Text.literal("Saving throws").formatted(Formatting.GRAY), rightX, top + SAVES_TOP,
                0xFFFFFF);
        int colWidth = RIGHT_WIDTH / 2;
        for (Ability a : Ability.values()) {
            int x = rightX + (a.ordinal() % 2) * colWidth;
            int y = top + SAVES_TOP + 11 + (a.ordinal() / 2) * ROW;
            Proficiency prof = sheet.saveProficiency(a);
            boolean hover = over(mouseX, mouseY, x, y - 1, colWidth - 4, ROW);
            if (hover) {
                DrawableHelper.fill(matrices, x - 1, y - 1, x + colWidth - 4, y + ROW - 1, BOX_HOVER);
                tooltip = saveTooltip(sheet, a);
            }
            drawSymbol(matrices, text, prof, x, y);
            text.drawWithShadow(matrices, Text.translatable(a.shortTranslationKey()), x + 8, y,
                    prof == Proficiency.NONE ? 0xA0A0A0 : 0xFFFFFF);
            String total = signed(sheet.save(a)) + advantageMark(sheet.advantage(RollQuery.save(a)));
            text.drawWithShadow(matrices, total, x + colWidth - 6 - text.getWidth(total), y, 0xFFFFFF);
        }

        // Skills, scrollable.
        int sepY = top + SKILLS_TOP - 14;
        DrawableHelper.fill(matrices, rightX, sepY, rightX + RIGHT_WIDTH, sepY + 1, BORDER);
        text.drawWithShadow(matrices, Text.literal("Skills").formatted(Formatting.GRAY), rightX, top + SKILLS_TOP - 11,
                0xFFFFFF);
        skillScroll = Math.min(skillScroll, maxSkillScroll());
        if (maxSkillScroll() > 0) {
            String scroll = (skillScroll + 1) + "-" + Math.min(Skill.values().length, skillScroll + SKILL_ROWS) + "/"
                    + Skill.values().length + " (scroll)";
            text.drawWithShadow(matrices, scroll, rightX + RIGHT_WIDTH - text.getWidth(scroll),
                    top + SKILLS_TOP - 11, 0x606060);
        }
        Skill[] skills = Skill.values();
        for (int row = 0; row < SKILL_ROWS && row + skillScroll < skills.length; row++) {
            Skill s = skills[row + skillScroll];
            int y = top + SKILLS_TOP + row * ROW;
            Proficiency prof = sheet.skillProficiency(s);
            boolean hover = over(mouseX, mouseY, rightX, y - 1, RIGHT_WIDTH, ROW);
            if (hover) {
                DrawableHelper.fill(matrices, rightX - 1, y - 1, rightX + RIGHT_WIDTH, y + ROW - 1, BOX_HOVER);
                tooltip = skillTooltip(sheet, s);
            }
            drawSymbol(matrices, text, prof, rightX, y);
            String total = signed(sheet.check(s));
            int totalX = rightX + RIGHT_WIDTH - 2 - text.getWidth(total);
            String name = text.trimToWidth(Text.translatable(s.translationKey()).getString(), totalX - rightX - 12);
            text.drawWithShadow(matrices, name, rightX + 8, y, prof == Proficiency.NONE ? 0xA0A0A0 : 0xFFFFFF);
            text.drawWithShadow(matrices, total, totalX, y, 0xFFFFFF);
        }

        // Passive scores, under the skills.
        int passiveSep = top + PASSIVES_TOP - 2;
        DrawableHelper.fill(matrices, rightX, passiveSep, rightX + RIGHT_WIDTH, passiveSep + 1, BORDER);
        for (int i = 0; i < PASSIVES.length; i++) {
            Skill s = PASSIVES[i];
            int y = top + PASSIVES_TOP + 1 + i * ROW;
            boolean hover = over(mouseX, mouseY, rightX, y - 1, RIGHT_WIDTH, ROW);
            if (hover) {
                DrawableHelper.fill(matrices, rightX - 1, y - 1, rightX + RIGHT_WIDTH, y + ROW - 1, BOX_HOVER);
                tooltip = passiveTooltip(sheet, s);
            }
            Text label = Text.literal("Passive ").formatted(Formatting.GRAY)
                    .append(Text.translatable(s.translationKey()).formatted(Formatting.WHITE));
            text.drawWithShadow(matrices, label, rightX + 8, y, 0xFFFFFF);
            String value = Integer.toString(sheet.passive(s));
            text.drawWithShadow(matrices, value, rightX + RIGHT_WIDTH - 2 - text.getWidth(value), y, 0xFFFFFF);
        }

        // Feats, under the ability boxes.
        int px = left + BOXES_LEFT;
        List<OrderedText> featTooltip = renderFeats(matrices, text, progress, px, top + FEATS_TOP, mouseX, mouseY,
                attuning);
        if (featTooltip != null) {
            tooltip = featTooltip;
        }

        // Features that work outside the numbers (advantage, Evasion...), listed in a tooltip.
        List<CharacterSheet.Line> features = featureLines(sheet);
        if (!features.isEmpty()) {
            Text label = Text.literal("Features (" + features.size() + ")").formatted(Formatting.AQUA,
                    Formatting.UNDERLINE);
            int y = top + FEATURES_TOP;
            text.drawWithShadow(matrices, label, px, y, 0xFFFFFF);
            if (over(mouseX, mouseY, px, y - 1, text.getWidth(label), ROW)) {
                tooltip = featuresTooltip(features);
            }
        }

        DrawableHelper.drawCenteredTextWithShadow(matrices, text, Text.literal(hint(progress, attuning))
                .formatted(Formatting.DARK_GRAY), centerX, top + height - 11, 0xFFFFFF);
        return tooltip;
    }

    private static Text pointsText(ClassProgress progress, boolean attuning) {
        int points = progress.asiPoints();
        int slots = progress.freeFeatSlotsLeft();
        if (points > 0 || slots > 0) {
            return Text.literal("Ability points: " + points + " \u00b7 Feat slots: " + slots
                    + (attuning ? "" : " (at an Attunement Table)")).formatted(Formatting.YELLOW);
        }
        int level = progress.level();
        if (level < ClassProgress.ASI_LEVELS[0]) {
            return Text.literal("Next: ability points or a feat at level " + ClassProgress.ASI_LEVELS[0])
                    .formatted(Formatting.GRAY);
        }
        if (level < ClassProgress.FEAT_LEVEL) {
            return Text.literal("Next: a free feat at level " + ClassProgress.FEAT_LEVEL).formatted(Formatting.GRAY);
        }
        for (int asiLevel : ClassProgress.ASI_LEVELS) {
            if (level < asiLevel) {
                return Text.literal("Next: ability points or a feat at level " + asiLevel).formatted(Formatting.GRAY);
            }
        }
        return Text.literal("All ability points and feats chosen").formatted(Formatting.GRAY);
    }

    /** Whether the "Choose a feat" button shows: at a table, with a free slot or enough ability points. */
    static boolean canChooseFeat(boolean attuning) {
        return attuning && ClassProgress.client.canAffordFeat();
    }

    private static final String CHOOSE_FEAT = "Choose a feat";

    /** The Feats box; returns the hovered row's tooltip, or null. */
    @Nullable
    private static List<OrderedText> renderFeats(MatrixStack matrices, TextRenderer text, ClassProgress progress,
            int x, int y, int mouseX, int mouseY, boolean attuning) {
        List<OrderedText> tooltip = null;
        int width = 2 * BOX_W + BOX_GAP;
        if (!canChooseFeat(attuning)) {
            text.drawWithShadow(matrices, Text.literal("Feats").formatted(Formatting.GRAY), x, y, 0xFFFFFF);
        }
        if (canChooseFeat(attuning)) {
            // The gold button takes the header's place
            boolean hover = over(mouseX, mouseY, x, y - 2, width, ROW + 1);
            DrawableHelper.fill(matrices, x, y - 2, x + width, y + ROW - 1, hover ? 0xFFF0C860 : GOLD);
            text.draw(matrices, CHOOSE_FEAT, x + (width - text.getWidth(CHOOSE_FEAT)) / 2, y, 0x2A1E08);
            if (hover) {
                tooltip = new ArrayList<>();
                tooltip.add(title("Choose a feat"));
                FeatChoice.Source source = progress.nextFeatSource();
                tooltip.add(gray(source == FeatChoice.Source.FREE ? "Paid with your level " + ClassProgress.FEAT_LEVEL
                        + " feat." : "Costs " + ClassProgress.FEAT_COST + " ability points."));
            }
        }
        List<FeatChoice> feats = progress.feats;
        if (feats.isEmpty()) {
            text.drawWithShadow(matrices, Text.literal(progress.level() >= ClassProgress.ASI_LEVELS[0] ? "None yet"
                    : "From level " + ClassProgress.ASI_LEVELS[0]).formatted(Formatting.DARK_GRAY), x, y + 11,
                    0xFFFFFF);
            return tooltip;
        }
        int shown = feats.size() > FEAT_ROWS ? FEAT_ROWS - 1 : feats.size();
        for (int i = 0; i < shown; i++) {
            FeatChoice choice = feats.get(i);
            int ry = y + 11 + i * ROW;
            boolean hover = over(mouseX, mouseY, x, ry - 1, width, ROW);
            if (hover) {
                DrawableHelper.fill(matrices, x - 1, ry - 1, x + width, ry + ROW - 1, BOX_HOVER);
                tooltip = featTooltip(choice);
            }
            drawFeatIcon(matrices, choice.feat(), x, ry, 8);
            text.drawWithShadow(matrices, text.trimToWidth(choice.label(), width - 11), x + 11, ry, 0xFFFFFF);
        }
        if (shown < feats.size()) {
            int ry = y + 11 + shown * ROW;
            int more = feats.size() - shown;
            text.drawWithShadow(matrices, Text.literal("+" + more + " more").formatted(Formatting.AQUA), x + 11, ry,
                    0xFFFFFF);
            if (over(mouseX, mouseY, x, ry - 1, width, ROW)) {
                tooltip = new ArrayList<>();
                tooltip.add(title("Feats"));
                for (FeatChoice choice : feats) {
                    tooltip.add(gray(choice.label()));
                }
            }
        }
        return tooltip;
    }

    /** One feat icon from {@link #FEAT_ICONS}, at {@code size} pixels. */
    static void drawFeatIcon(MatrixStack matrices, Feat feat, int x, int y, int size) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, FEAT_ICONS);
        DrawableHelper.drawTexture(matrices, x, y, size, size, feat.icon() * FEAT_CELL, 0, FEAT_CELL, FEAT_CELL,
                FEAT_CELL * Feat.values().length, FEAT_CELL);
    }

    static List<OrderedText> featTooltip(FeatChoice choice) {
        List<OrderedText> lines = new ArrayList<>();
        lines.add(title(choice.label()));
        lines.addAll(MinecraftClient.getInstance().textRenderer.wrapLines(
                Text.literal(choice.feat().description()).formatted(Formatting.GRAY), 200));
        lines.add(Text.literal("Paid with: " + choice.paidWith()).formatted(Formatting.DARK_AQUA).asOrderedText());
        return lines;
    }

    private static String hint(ClassProgress progress, boolean attuning) {
        if (canChooseFeat(attuning) && progress.freeFeatSlotsLeft() > 0) {
            return "Choose your level " + ClassProgress.FEAT_LEVEL + " feat: click \"Choose a feat\".";
        }
        if (progress.asiPoints() > 0) {
            return attuning ? "Click + to raise a score (max " + Contribution.NATURAL_CAP + "), or take a feat."
                    : "Ability points are spent at an Attunement Table.";
        }
        return "Hover a number to see where it comes from.";
    }

    private static void drawSymbol(MatrixStack matrices, TextRenderer text, Proficiency prof, int x, int y) {
        if (prof == Proficiency.NONE)
            return;
        int color = switch (prof) {
            case EXPERTISE -> 0x60E0E0;
            case PROFICIENT -> 0xE0B040;
            default -> 0xA0A0A0;
        };
        text.drawWithShadow(matrices, prof.symbol(), x, y, color);
    }

    private static String advantageMark(Advantage advantage) {
        return switch (advantage) {
            case ADVANTAGE -> " A";
            case DISADVANTAGE -> " D";
            case NORMAL -> "";
        };
    }

    // ---- Tooltips ----

    private static OrderedText title(String title) {
        return Text.literal(title).formatted(Formatting.BOLD, Formatting.WHITE).asOrderedText();
    }

    private static OrderedText gray(String line) {
        return Text.literal(line).formatted(Formatting.GRAY).asOrderedText();
    }

    private static void breakdown(List<OrderedText> out, CharacterSheet sheet, String target) {
        for (CharacterSheet.Line line : sheet.linesFor(target)) {
            out.add(Text.literal(line.source() + ": ").formatted(Formatting.GRAY)
                    .append(Text.literal(line.detail()).formatted(Formatting.WHITE)).asOrderedText());
        }
    }

    private static String profText(Proficiency prof, int proficiencyBonus) {
        return switch (prof) {
            case NONE -> "";
            case HALF -> "Half proficiency " + signed(prof.bonus(proficiencyBonus));
            case PROFICIENT -> "Proficient " + signed(prof.bonus(proficiencyBonus));
            case EXPERTISE -> "Expertise " + signed(prof.bonus(proficiencyBonus));
        };
    }

    private static String name(Ability a) {
        return Text.translatable(a.translationKey()).getString();
    }

    private static List<OrderedText> abilityTooltip(CharacterSheet sheet, Ability a, boolean attuning) {
        List<OrderedText> lines = new ArrayList<>();
        lines.add(title(name(a) + " " + sheet.score(a) + " (" + signed(sheet.modifier(a)) + ")"));
        breakdown(lines, sheet, a.shortKey());
        lines.add(gray("Save " + signed(sheet.save(a))
                + (sheet.saveProficiency(a) == Proficiency.NONE ? "" : " (proficient)")));
        if (!AbilityIncreases.belowCap(sheet, a)) {
            lines.add(Text.literal("Improvements can't raise it past " + Contribution.NATURAL_CAP)
                    .formatted(Formatting.DARK_GRAY).asOrderedText());
        } else if (canIncrease(attuning, sheet, a)) {
            lines.add(Text.literal("Click + to raise it by 1").formatted(Formatting.YELLOW).asOrderedText());
        }
        return lines;
    }

    private static List<OrderedText> saveTooltip(CharacterSheet sheet, Ability a) {
        List<OrderedText> lines = new ArrayList<>();
        lines.add(title(Text.translatable(a.saveTranslationKey()).getString() + " " + signed(sheet.save(a))));
        lines.add(gray(a.shortKey() + " modifier " + signed(sheet.modifier(a))));
        String prof = profText(sheet.saveProficiency(a), sheet.proficiencyBonus());
        if (!prof.isEmpty()) {
            lines.add(gray(prof));
        }
        breakdown(lines, sheet, "save:" + a.shortKey());
        breakdown(lines, sheet, "save:ALL");
        Advantage adv = sheet.advantage(RollQuery.save(a));
        if (adv != Advantage.NORMAL) {
            lines.add(Text.literal(adv == Advantage.ADVANTAGE ? "Advantage (A)" : "Disadvantage (D)")
                    .formatted(adv == Advantage.ADVANTAGE ? Formatting.GREEN : Formatting.RED).asOrderedText());
        }
        return lines;
    }

    private static List<OrderedText> skillTooltip(CharacterSheet sheet, Skill s) {
        Ability a = s.ability();
        List<OrderedText> lines = new ArrayList<>();
        lines.add(title(Text.translatable(s.translationKey()).getString() + " (" + a.shortKey() + ") "
                + signed(sheet.check(s))));
        lines.add(gray(a.shortKey() + " modifier " + signed(sheet.modifier(a))));
        String prof = profText(sheet.skillProficiency(s), sheet.proficiencyBonus());
        if (!prof.isEmpty()) {
            lines.add(gray(prof));
        }
        breakdown(lines, sheet, "skill:" + s.name());
        breakdown(lines, sheet, "check:" + a.shortKey());
        breakdown(lines, sheet, "check:ALL");
        return lines;
    }

    private static List<OrderedText> passiveTooltip(CharacterSheet sheet, Skill s) {
        List<OrderedText> lines = new ArrayList<>();
        String name = Text.translatable(s.translationKey()).getString();
        lines.add(title("Passive " + name + " " + sheet.passive(s)));
        lines.add(gray("10 + " + name + " " + signed(sheet.check(s))));
        Advantage adv = sheet.advantage(RollQuery.check(s, CharacterSheet.PASSIVE_TAG));
        if (adv != Advantage.NORMAL) {
            lines.add(Text.literal(adv == Advantage.ADVANTAGE ? "Advantage: +5" : "Disadvantage: -5")
                    .formatted(adv == Advantage.ADVANTAGE ? Formatting.GREEN : Formatting.RED).asOrderedText());
        }
        lines.add(Text.literal(switch (s) {
            case PERCEPTION -> "Spots mimics and hidden things nearby";
            case STEALTH -> "How close mobs notice you while sneaking";
            default -> "Used when you don't roll";
        }).formatted(Formatting.DARK_GRAY).asOrderedText());
        return lines;
    }

    private static List<CharacterSheet.Line> featureLines(CharacterSheet sheet) {
        List<CharacterSheet.Line> out = new ArrayList<>();
        for (CharacterSheet.Line line : sheet.lines()) {
            switch (line.target()) {
                case "feature", "adv", "dis", "reroll", "crit" -> out.add(line);
                default -> {
                }
            }
        }
        return out;
    }

    private static List<OrderedText> featuresTooltip(List<CharacterSheet.Line> features) {
        List<OrderedText> lines = new ArrayList<>();
        lines.add(title("Features"));
        for (CharacterSheet.Line line : features) {
            String prefix = switch (line.target()) {
                case "adv" -> "Advantage: ";
                case "dis" -> "Disadvantage: ";
                default -> "";
            };
            lines.add(Text.literal(line.source() + ": ").formatted(Formatting.GRAY)
                    .append(Text.literal(prefix + line.detail()).formatted(Formatting.WHITE)).asOrderedText());
        }
        return lines;
    }

    // ---- Input ----

    /** Handles a click: a "+" sends an ability increase. Returns whether the click was used. */
    boolean mouseClicked(double mouseX, double mouseY, int button, int left, int top, boolean attuning) {
        if (button != 0)
            return false;
        if (canChooseFeat(attuning)) {
            int x = left + BOXES_LEFT;
            int y = top + FEATS_TOP;
            if (over(mouseX, mouseY, x, y - 2, 2 * BOX_W + BOX_GAP, ROW + 1)) {
                MinecraftClient client = MinecraftClient.getInstance();
                client.setScreen(new FeatPickerScreen(client.currentScreen));
                return true;
            }
        }
        CharacterSheet sheet = CharacterSheet.client;
        for (Ability a : Ability.values()) {
            int px = boxX(left, a) + BOX_W - PLUS - 1;
            int py = boxY(top, a) + 1;
            if (canIncrease(attuning, sheet, a) && over(mouseX, mouseY, px, py, PLUS, PLUS)) {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeString(a.shortKey());
                ClientPlayNetworking.send(AbilityIncreases.C2S_ABILITY_INCREASE, buf);
                return true;
            }
        }
        return false;
    }

    boolean mouseScrolled(double amount) {
        skillScroll = Math.max(0, Math.min(maxSkillScroll(), skillScroll - (int) Math.signum(amount)));
        return true;
    }
}
