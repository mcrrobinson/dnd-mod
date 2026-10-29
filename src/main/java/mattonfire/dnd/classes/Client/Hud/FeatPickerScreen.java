package mattonfire.dnd.classes.Client.Hud;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.Abilities.AbilityIncreases;
import mattonfire.dnd.classes.Abilities.CharacterSheet;
import mattonfire.dnd.classes.Feats.Feat;
import mattonfire.dnd.classes.Feats.FeatChoice;
import mattonfire.dnd.classes.Feats.FeatPackets;
import mattonfire.dnd.classes.Progression.ClassProgress;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The feat picker, opened from the Character tab's "Choose a feat" button at an Attunement Table: a 3x3 grid of feat
 * cards (icon, name, two lines of summary; greyed "Taken" when owned). Clicking one opens a confirm panel saying
 * what pays for it; Resilient first asks for an ability. Confirming sends {@link FeatPackets#C2S_CHOOSE_FEAT}, and
 * the server checks the table, the slot or points and the feat again.
 */
@Environment(EnvType.CLIENT)
public class FeatPickerScreen extends Screen {
    private static final int PANEL_COLOR = 0xE0101018;
    private static final int BORDER = 0xFF6B4FA0;
    private static final int GOLD = 0xFFE0B040;
    private static final int CARD_BG = 0xFF1C1A28;
    private static final int CARD_HOVER = 0xFF2C2840;
    private static final int CARD_TAKEN = 0xFF141218;

    private static final int COLUMNS = 3;
    private static final int CARD_H = 44;
    private static final int GAP = 4;
    private static final int HEADER = 30;
    private static final int FOOTER = 16;

    @Nullable
    private final Screen parent;
    private int cardW;
    private int left;
    private int top;
    private int panelW;
    private int panelH;

    /** The feat being confirmed, or null on the grid. */
    @Nullable
    private Feat selected;
    /** Resilient's ability, once picked. */
    @Nullable
    private Ability ability;

    public FeatPickerScreen(@Nullable Screen parent) {
        super(Text.literal("Choose a feat"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        cardW = Math.min(136, (width - 16 - (COLUMNS - 1) * GAP) / COLUMNS);
        panelW = COLUMNS * cardW + (COLUMNS - 1) * GAP + 12;
        int rows = (Feat.values().length + COLUMNS - 1) / COLUMNS;
        panelH = HEADER + rows * CARD_H + (rows - 1) * GAP + FOOTER;
        left = (width - panelW) / 2;
        top = Math.max(2, (height - panelH) / 2);
        clearChildren();
        if (selected == null) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> close())
                    .dimensions(left + panelW - 46, top + 4, 40, 14).build());
            return;
        }
        int cx = width / 2;
        int y = buttonsY();
        if (selected.perAbility() && ability == null) {
            // Resilient: pick an ability first; the ones already taken are disabled
            int bw = 40;
            int x0 = cx - (Ability.values().length * (bw + 4) - 4) / 2;
            ClassProgress progress = ClassProgress.client;
            for (Ability a : Ability.values()) {
                ButtonWidget button = ButtonWidget.builder(Text.translatable(a.shortTranslationKey()), b -> {
                    ability = a;
                    init();
                }).dimensions(x0 + a.ordinal() * (bw + 4), y - 26, bw, 20).build();
                button.active = !progress.hasFeat(selected, a);
                addDrawableChild(button);
            }
            addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> back())
                    .dimensions(cx - 40, y, 80, 20).build());
            return;
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Take it").formatted(Formatting.GOLD), b -> confirm())
                .dimensions(cx - 84, y, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> back())
                .dimensions(cx + 4, y, 80, 20).build());
    }

    /** Top of the confirm panel's buttons. */
    private int buttonsY() {
        return top + panelH - FOOTER - 26;
    }

    private void back() {
        selected = null;
        ability = null;
        init();
    }

    private void confirm() {
        if (selected == null)
            return;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(selected.id());
        buf.writeString(ability == null ? "" : ability.shortKey());
        ClientPlayNetworking.send(FeatPackets.C2S_CHOOSE_FEAT, buf);
        close();
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    private int cardX(int index) {
        return left + 6 + (index % COLUMNS) * (cardW + GAP);
    }

    private int cardY(int index) {
        return top + HEADER + (index / COLUMNS) * (CARD_H + GAP);
    }

    private static boolean over(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        fill(matrices, left - 1, top - 1, left + panelW + 1, top + panelH + 1, BORDER);
        fill(matrices, left, top, left + panelW, top + panelH, PANEL_COLOR);
        ClassProgress progress = ClassProgress.client;

        drawCenteredTextWithShadow(matrices, textRenderer, Text.literal("Choose a feat").formatted(Formatting.GOLD,
                Formatting.BOLD), width / 2, top + 6, 0xFFFFFF);
        drawCenteredTextWithShadow(matrices, textRenderer, paymentLine(progress), width / 2, top + 17, 0xFFFFFF);

        List<OrderedText> tooltip = null;
        if (selected == null) {
            Feat[] feats = Feat.values();
            for (int i = 0; i < feats.length; i++) {
                Feat feat = feats[i];
                int x = cardX(i);
                int y = cardY(i);
                boolean taken = !progress.featAvailable(feat, null);
                boolean hover = !taken && over(mouseX, mouseY, x, y, cardW, CARD_H);
                fill(matrices, x - 1, y - 1, x + cardW + 1, y + CARD_H + 1, hover ? GOLD : BORDER);
                fill(matrices, x, y, x + cardW, y + CARD_H, taken ? CARD_TAKEN : hover ? CARD_HOVER : CARD_BG);
                CharacterSheetPanel.drawFeatIcon(matrices, feat, x + 4, y + 4, 16);
                if (taken) {
                    fill(matrices, x + 4, y + 4, x + 20, y + 20, 0xA0101018);
                }
                textRenderer.drawWithShadow(matrices, textRenderer.trimToWidth(feat.displayName(), cardW - 28),
                        x + 24, y + 5, taken ? 0x707070 : 0xFFFFFF);
                if (taken) {
                    textRenderer.drawWithShadow(matrices, "Taken", x + 24, y + 14, 0x60A060);
                } else if (feat.perAbility() && progress.hasFeat(feat, null)) {
                    textRenderer.drawWithShadow(matrices, "Again", x + 24, y + 14, 0xA0A060);
                }
                int summaryColor = taken ? 0x505050 : 0xA0A0A0;
                textRenderer.draw(matrices, textRenderer.trimToWidth(feat.summary1(), cardW - 8), x + 4, y + 24,
                        summaryColor);
                textRenderer.draw(matrices, textRenderer.trimToWidth(feat.summary2(), cardW - 8), x + 4, y + 34,
                        summaryColor);
                if (over(mouseX, mouseY, x, y, cardW, CARD_H)) {
                    tooltip = describe(feat);
                }
            }
            drawCenteredTextWithShadow(matrices, textRenderer, Text.literal("Click a feat to take it. Hover for the"
                    + " full rules.").formatted(Formatting.DARK_GRAY), width / 2, top + panelH - 12, 0xFFFFFF);
        } else {
            renderConfirm(matrices, progress);
        }
        super.render(matrices, mouseX, mouseY, delta);
        if (tooltip != null) {
            renderOrderedTooltip(matrices, tooltip, mouseX, mouseY);
        }
    }

    private Text paymentLine(ClassProgress progress) {
        FeatChoice.Source source = progress.nextFeatSource();
        if (source == FeatChoice.Source.FREE)
            return Text.literal("Paid with your level " + ClassProgress.FEAT_LEVEL + " feat").formatted(
                    Formatting.YELLOW);
        if (source == FeatChoice.Source.ASI)
            return Text.literal("Costs " + ClassProgress.FEAT_COST + " of your " + progress.asiPoints()
                    + " ability points").formatted(Formatting.YELLOW);
        return Text.literal("You need a feat slot (level " + ClassProgress.FEAT_LEVEL + ") or "
                + ClassProgress.FEAT_COST + " ability points").formatted(Formatting.RED);
    }

    private void renderConfirm(MatrixStack matrices, ClassProgress progress) {
        int cx = width / 2;
        int boxW = Math.min(panelW - 24, 280);
        int boxTop = top + HEADER + 4;
        int boxBottom = top + panelH - 6;
        fill(matrices, cx - boxW / 2 - 1, boxTop - 1, cx + boxW / 2 + 1, boxBottom + 1, GOLD);
        fill(matrices, cx - boxW / 2, boxTop, cx + boxW / 2, boxBottom, CARD_BG);
        CharacterSheetPanel.drawFeatIcon(matrices, selected, cx - boxW / 2 + 6, boxTop + 6, 16);
        String name = selected.displayName() + (ability == null ? "" : " (" + ability.shortKey() + ")");
        int y = boxTop + 10;
        if (selected.perAbility() && ability == null) {
            drawCenteredTextWithShadow(matrices, textRenderer, Text.literal(selected.displayName() + ": which ability?")
                    .formatted(Formatting.WHITE, Formatting.BOLD), cx, y, 0xFFFFFF);
        } else {
            drawCenteredTextWithShadow(matrices, textRenderer, Text.literal("Take ").formatted(Formatting.WHITE)
                    .append(Text.literal(name).formatted(Formatting.GOLD, Formatting.BOLD)).append("?"), cx, y,
                    0xFFFFFF);
        }
        y += 14;
        for (OrderedText line : textRenderer.wrapLines(Text.literal(selected.description())
                .formatted(Formatting.GRAY), boxW - 16)) {
            if (y > buttonsY() - 36)
                break;
            textRenderer.draw(matrices, line, cx - boxW / 2 + 8, y, 0xFFFFFF);
            y += 9;
        }
        if (!selected.perAbility() || ability != null) {
            FeatChoice.Source source = progress.nextFeatSource();
            String paid = source == FeatChoice.Source.ASI ? "Paid with " + ClassProgress.FEAT_COST + " ability points."
                    : "Paid with your level " + ClassProgress.FEAT_LEVEL + " feat.";
            if (ability != null && !AbilityIncreases.belowCap(CharacterSheet.client, ability)) {
                paid += " " + ability.shortKey() + " is already " + 20 + ": only the saves improve.";
            }
            int by = buttonsY();
            drawCenteredTextWithShadow(matrices, textRenderer, Text.literal(paid).formatted(Formatting.YELLOW), cx,
                    by - 22, 0xFFFFFF);
            drawCenteredTextWithShadow(matrices, textRenderer, Text.literal("Only a Tome of Clear Thought can undo"
                    + " this.").formatted(Formatting.DARK_GRAY), cx, by - 11, 0xFFFFFF);
        }
    }

    private List<OrderedText> describe(Feat feat) {
        List<OrderedText> lines = new ArrayList<>();
        lines.add(Text.literal(feat.displayName()).formatted(Formatting.BOLD, Formatting.WHITE).asOrderedText());
        lines.addAll(textRenderer.wrapLines(Text.literal(feat.description()).formatted(Formatting.GRAY), 220));
        return lines;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (selected == null && button == 0) {
            ClassProgress progress = ClassProgress.client;
            Feat[] feats = Feat.values();
            for (int i = 0; i < feats.length; i++) {
                if (over(mouseX, mouseY, cardX(i), cardY(i), cardW, CARD_H)
                        && progress.featAvailable(feats[i], null) && progress.canAffordFeat()) {
                    selected = feats[i];
                    ability = null;
                    init();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
