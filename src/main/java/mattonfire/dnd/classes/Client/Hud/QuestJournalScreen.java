package mattonfire.dnd.classes.Client.Hud;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.faction.ReputationTier;
import mattonfire.dnd.faction.client.ClientReputation;
import mattonfire.dnd.quest.client.ClientQuests;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * The Journal (key J): a Quests tab (quests grouped by chain, active first and finished greyed, with the
 * selected quest's stage, objectives, party and rewards, and Track / Abandon buttons) and a Factions tab
 * (a bar from -1000 to 1000 per faction with the tier boundaries ticked). Reads {@link ClientQuests} and
 * {@link ClientReputation} and rebuilds when a sync arrives while it's open. Track and Abandon run
 * {@code /quest track|abandon}, so the server checks them like the commands. Doesn't pause the game.
 */
public class QuestJournalScreen extends Screen {
    public static final String KEY = "key.dnd-classes.journal";
    private static KeyBinding journalKey;

    private static final int PANEL_MAX_WIDTH = 400;
    private static final int PANEL_MAX_HEIGHT = 250;
    private static final int PAD = 8;
    private static final int LINE = 10;
    private static final int TAB_HEIGHT = 16;
    private static final int LIST_WIDTH = 130;
    private static final int ENTRY_HEIGHT = 12;
    private static final int FACTION_ROW = 28;
    private static final int BACKGROUND = 0xE0141018;
    private static final int BORDER = 0xFFB08A3E;
    private static final int DIVIDER = 0x60B08A3E;
    private static final int SELECTED = 0x50B08A3E;
    private static final int HOVER = 0x30FFFFFF;

    public enum Tab { QUESTS, FACTIONS }

    /** Last tab and selection, kept between openings. */
    private static Tab lastTab = Tab.QUESTS;
    private static Identifier lastSelected;
    private static boolean lastSelectedFinished;

    private Tab tab = lastTab;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int contentTop;

    // What the screen was built from: a new sync rebuilds it.
    private List<ClientQuests.Active> builtActive;
    private List<ClientQuests.Finished> builtFinished;
    private List<ClientReputation.Entry> builtReputation;

    private final List<QuestEntry> entries = new ArrayList<>();
    private final List<HeaderRow> headers = new ArrayList<>();
    private int listScroll;
    private int listHeight;
    private final List<OrderedText> detail = new ArrayList<>();
    private int detailScroll;
    private int factionScroll;

    public QuestJournalScreen() {
        super(Text.translatable("journal.dndclasses.title"));
    }

    public static void register() {
        journalKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(KEY, InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J,
                "category.dnd-classes.dnd-classes"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (journalKey.wasPressed()) {
                if (client.currentScreen == null && client.player != null) {
                    client.setScreen(new QuestJournalScreen());
                }
            }
        });
    }

    @Override
    protected void init() {
        this.builtActive = ClientQuests.active();
        this.builtFinished = ClientQuests.finished();
        this.builtReputation = ClientReputation.entries();
        this.panelWidth = Math.min(PANEL_MAX_WIDTH, this.width - 16);
        this.panelHeight = Math.min(PANEL_MAX_HEIGHT, this.height - 16);
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;
        this.contentTop = this.panelY + PAD + LINE + 4 + TAB_HEIGHT + 4;

        int tabX = this.panelX + PAD;
        int tabY = this.panelY + PAD + LINE + 4;
        ButtonWidget quests = ButtonWidget.builder(Text.translatable("journal.dndclasses.tab.quests"),
                button -> this.setTab(Tab.QUESTS)).dimensions(tabX, tabY, 70, TAB_HEIGHT).build();
        ButtonWidget factions = ButtonWidget.builder(Text.translatable("journal.dndclasses.tab.factions"),
                button -> this.setTab(Tab.FACTIONS)).dimensions(tabX + 74, tabY, 70, TAB_HEIGHT).build();
        quests.active = this.tab != Tab.QUESTS;
        factions.active = this.tab != Tab.FACTIONS;
        this.addDrawableChild(quests);
        this.addDrawableChild(factions);

        this.entries.clear();
        this.headers.clear();
        this.detail.clear();
        if (this.tab == Tab.QUESTS) {
            this.initQuests();
        }
    }

    private void setTab(Tab tab) {
        this.tab = tab;
        lastTab = tab;
        this.clearAndInit();
    }

    // ---- Quests tab ----

    private void initQuests() {
        // Group by chain: chains with active quests first, in the order the server sent them
        Map<Identifier, List<QuestEntry>> chains = new LinkedHashMap<>();
        for (ClientQuests.Active quest : this.builtActive) {
            chains.computeIfAbsent(quest.chain(), chain -> new ArrayList<>()).add(new QuestEntry(quest, null));
        }
        for (ClientQuests.Finished quest : this.builtFinished) {
            chains.computeIfAbsent(quest.chain(), chain -> new ArrayList<>()).add(new QuestEntry(null, quest));
        }

        QuestEntry selected = null;
        for (Map.Entry<Identifier, List<QuestEntry>> chain : chains.entrySet()) {
            this.headers.add(new HeaderRow(chainTitle(chain.getKey()), this.headers.size() + this.entries.size()));
            for (QuestEntry entry : chain.getValue()) {
                entry.row = this.headers.size() + this.entries.size();
                this.entries.add(entry);
                if (entry.quest().equals(lastSelected) && entry.isFinished() == lastSelectedFinished) {
                    selected = entry;
                }
            }
        }
        if (selected == null && !this.entries.isEmpty()) {
            // The tracked quest, else the first one
            selected = this.entries.stream().filter(entry -> entry.active != null && entry.active.tracked())
                    .findFirst().orElse(this.entries.get(0));
        }
        this.listHeight = this.panelY + this.panelHeight - PAD - this.contentTop;
        int rows = this.headers.size() + this.entries.size();
        this.listScroll = Math.max(0, Math.min(this.listScroll, rows - this.listHeight / ENTRY_HEIGHT));
        for (QuestEntry entry : this.entries) {
            this.addDrawableChild(entry);
        }
        this.layoutList();
        this.select(selected);
    }

    private void layoutList() {
        int visibleRows = this.listHeight / ENTRY_HEIGHT;
        for (QuestEntry entry : this.entries) {
            int row = entry.row - this.listScroll;
            entry.setX(this.panelX + PAD);
            entry.setY(this.contentTop + row * ENTRY_HEIGHT);
            entry.visible = row >= 0 && row < visibleRows;
        }
    }

    private QuestEntry selectedEntry() {
        for (QuestEntry entry : this.entries) {
            if (entry.selected) {
                return entry;
            }
        }
        return null;
    }

    private void select(QuestEntry entry) {
        // Drop the old detail buttons
        this.children().stream().filter(child -> child instanceof DetailButton).toList().forEach(this::remove);
        for (QuestEntry each : this.entries) {
            each.selected = each == entry;
        }
        this.detail.clear();
        this.detailScroll = 0;
        if (entry == null) {
            return;
        }
        lastSelected = entry.quest();
        lastSelectedFinished = entry.isFinished();
        int x = this.detailX();
        int width = this.detailWidth();
        if (entry.active != null) {
            this.buildDetail(entry.active, width);
            int buttonY = this.panelY + this.panelHeight - PAD - 20;
            ClientQuests.Active quest = entry.active;
            DetailButton track = new DetailButton(x, buttonY, 70, Text.translatable(
                    quest.tracked() ? "journal.dndclasses.tracked" : "journal.dndclasses.track"),
                    () -> this.run("quest track " + quest.quest()));
            track.active = !quest.tracked();
            DetailButton abandon = new DetailButton(x + width - 70, buttonY, 70,
                    Text.translatable("journal.dndclasses.abandon"), () -> this.confirmAbandon(quest));
            this.addDrawableChild(track);
            this.addDrawableChild(abandon);
        } else {
            this.buildDetail(entry.finished, width);
        }
    }

    private void buildDetail(ClientQuests.Active quest, int width) {
        this.add(quest.title().copy().formatted(Formatting.GOLD, Formatting.BOLD), width);
        if (!quest.giver().isEmpty()) {
            this.add(Text.translatable("quest.dndclasses.info.giver", roleName(quest.giver())).formatted(Formatting.GRAY), width);
        }
        if (!quest.description().getString().isEmpty()) {
            this.add(quest.description().copy().formatted(Formatting.GRAY, Formatting.ITALIC), width);
        }
        this.gap();
        this.add(Text.translatable("quest.dndclasses.info.stage", quest.stage() + 1, quest.stages(), quest.stageTitle())
                .formatted(Formatting.YELLOW), width);
        if (!quest.stageText().getString().isEmpty()) {
            this.add(quest.stageText().copy().formatted(Formatting.WHITE), width);
        }
        for (ClientQuests.Objective objective : quest.objectives()) {
            MutableText line = Text.literal(objective.done() ? "✔ " : "- ")
                    .append(objective.description().copy())
                    .append(" " + objective.progress() + "/" + objective.count());
            this.add(line.formatted(objective.done() ? Formatting.GREEN : Formatting.WHITE), width);
        }
        this.gap();
        MutableText party = Text.translatable("quest.dndclasses.info.participants").formatted(Formatting.AQUA);
        boolean first = true;
        for (ClientQuests.Participant participant : quest.participants()) {
            party.append(Text.literal((first ? " " : ", ") + (participant.leader() ? "★" : "") + participant.name())
                    .formatted(Formatting.WHITE));
            first = false;
        }
        this.add(party, width);
        if (!quest.rewards().getString().isEmpty()) {
            this.add(Text.translatable("quest.dndclasses.info.rewards").formatted(Formatting.GOLD)
                    .append(" ").append(quest.rewards().copy().formatted(Formatting.WHITE)), width);
        }
    }

    private void buildDetail(ClientQuests.Finished quest, int width) {
        this.add(quest.title().copy().formatted(Formatting.GRAY, Formatting.BOLD), width);
        this.add(Text.translatable("journal.dndclasses.finished").formatted(Formatting.GREEN), width);
        if (quest.rewardWaiting()) {
            this.gap();
            this.add(Text.translatable("journal.dndclasses.reward_waiting").formatted(Formatting.YELLOW), width);
        }
    }

    private void add(Text text, int width) {
        this.detail.addAll(this.textRenderer.wrapLines(text, width));
    }

    private void gap() {
        this.detail.add(OrderedText.EMPTY);
    }

    private void confirmAbandon(ClientQuests.Active quest) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new ConfirmScreen(yes -> {
            if (yes) {
                this.run("quest abandon " + quest.quest());
            }
            client.setScreen(this);
        }, Text.translatable("journal.dndclasses.abandon.title", quest.title()),
                Text.translatable(quest.participants().size() > 1 ? "journal.dndclasses.abandon.party"
                        : "journal.dndclasses.abandon.solo"),
                Text.translatable("journal.dndclasses.abandon"), Text.translatable("gui.cancel")));
    }

    private void run(String command) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() != null) {
            DnDClasses.LOGGER.info("[Journal] /{}", command);
            client.getNetworkHandler().sendCommand(command);
        }
    }

    private int detailX() {
        return this.panelX + PAD + LIST_WIDTH + PAD;
    }

    private int detailWidth() {
        return this.panelX + this.panelWidth - PAD - this.detailX();
    }

    /** "The Goblin Menace" from {@code quest.dndclasses.goblin_menace.title}, else the path in words. */
    static Text chainTitle(Identifier chain) {
        String path = chain.getPath();
        String words = path.substring(path.lastIndexOf('/') + 1).replace('_', ' ');
        words = words.isEmpty() ? path : Character.toUpperCase(words.charAt(0)) + words.substring(1);
        return Text.translatableWithFallback("quest." + chain.getNamespace() + "." + path.replace('/', '.') + ".title", words);
    }

    static Text roleName(String role) {
        return Text.translatableWithFallback("quest.dndclasses.role." + role, role.replace('_', ' '));
    }

    // ---- Rendering ----

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        int x0 = this.panelX;
        int y0 = this.panelY;
        int x1 = x0 + this.panelWidth;
        int y1 = y0 + this.panelHeight;
        fill(matrices, x0, y0, x1, y1, BACKGROUND);
        fill(matrices, x0, y0, x1, y0 + 1, BORDER);
        fill(matrices, x0, y1 - 1, x1, y1, BORDER);
        fill(matrices, x0, y0, x0 + 1, y1, BORDER);
        fill(matrices, x1 - 1, y0, x1, y1, BORDER);
        this.textRenderer.drawWithShadow(matrices, this.title.copy().formatted(Formatting.GOLD, Formatting.BOLD),
                x0 + PAD, y0 + PAD, 0xFFFFFF);
        fill(matrices, x0 + PAD, this.contentTop - 3, x1 - PAD, this.contentTop - 2, DIVIDER);

        if (this.tab == Tab.QUESTS) {
            this.renderQuests(matrices);
        } else {
            this.renderFactions(matrices, mouseX, mouseY);
        }
        super.render(matrices, mouseX, mouseY, delta);
    }

    private void renderQuests(MatrixStack matrices) {
        int count = this.builtActive.size();
        String active = count + "/" + mattonfire.dnd.quest.QuestDefinition.MAX_ACTIVE;
        this.textRenderer.draw(matrices, Text.translatable("journal.dndclasses.active", active),
                this.panelX + this.panelWidth - PAD - this.textRenderer.getWidth(
                        Text.translatable("journal.dndclasses.active", active)), this.panelY + PAD, 0xAAAAAA);
        if (this.entries.isEmpty()) {
            List<OrderedText> lines = this.textRenderer.wrapLines(Text.translatable("journal.dndclasses.no_quests"),
                    this.panelWidth - 2 * PAD);
            int y = this.contentTop + 4;
            for (OrderedText line : lines) {
                this.textRenderer.draw(matrices, line, this.panelX + PAD, y, 0xAAAAAA);
                y += LINE;
            }
            return;
        }
        int dividerX = this.panelX + PAD + LIST_WIDTH + PAD / 2;
        fill(matrices, dividerX, this.contentTop, dividerX + 1, this.panelY + this.panelHeight - PAD, DIVIDER);
        int visibleRows = this.listHeight / ENTRY_HEIGHT;
        for (HeaderRow header : this.headers) {
            int row = header.row - this.listScroll;
            if (row >= 0 && row < visibleRows) {
                Text title = QuestTrackerHud.fit(this.textRenderer, header.title.copy().formatted(Formatting.GOLD),
                        LIST_WIDTH);
                this.textRenderer.drawWithShadow(matrices, title, this.panelX + PAD,
                        this.contentTop + row * ENTRY_HEIGHT + 2, 0xFFFFFF);
            }
        }
        // Scroll hints
        if (this.listScroll > 0) {
            this.textRenderer.draw(matrices, "▲", dividerX - 8, this.contentTop, 0x888888);
        }
        if (this.headers.size() + this.entries.size() - this.listScroll > visibleRows) {
            this.textRenderer.draw(matrices, "▼", dividerX - 8, this.panelY + this.panelHeight - PAD - 8, 0x888888);
        }

        int x = this.detailX();
        int top = this.contentTop;
        int bottom = this.panelY + this.panelHeight - PAD - (this.selectedEntry() != null
                && this.selectedEntry().active != null ? 24 : 0);
        int lines = (bottom - top) / LINE;
        this.detailScroll = Math.max(0, Math.min(this.detailScroll, this.detail.size() - lines));
        for (int i = 0; i < lines && i + this.detailScroll < this.detail.size(); i++) {
            this.textRenderer.drawWithShadow(matrices, this.detail.get(i + this.detailScroll), x, top + i * LINE, 0xFFFFFF);
        }
        if (this.detail.size() - this.detailScroll > lines) {
            this.textRenderer.draw(matrices, "▼", this.panelX + this.panelWidth - PAD - 6, bottom - 8, 0x888888);
        }
    }

    private void renderFactions(MatrixStack matrices, int mouseX, int mouseY) {
        List<ClientReputation.Entry> factions = this.builtReputation;
        if (factions.isEmpty()) {
            this.textRenderer.draw(matrices, Text.translatable("journal.dndclasses.no_factions"), this.panelX + PAD,
                    this.contentTop + 4, 0xAAAAAA);
            return;
        }
        int left = this.panelX + PAD;
        int right = this.panelX + this.panelWidth - PAD;
        int visible = (this.panelY + this.panelHeight - PAD - this.contentTop) / FACTION_ROW;
        this.factionScroll = Math.max(0, Math.min(this.factionScroll, factions.size() - visible));
        for (int i = 0; i < visible && i + this.factionScroll < factions.size(); i++) {
            ClientReputation.Entry faction = factions.get(i + this.factionScroll);
            int y = this.contentTop + i * FACTION_ROW;
            ReputationTier tier = faction.tier();
            // Colour swatch, name, then the tier and value on the right
            fill(matrices, left, y + 1, left + 7, y + 8, 0xFF000000 | faction.color());
            this.textRenderer.drawWithShadow(matrices, Text.translatable(faction.nameKey()), left + 11, y, 0xFFFFFF);
            Text standing = tier.displayName().append(Text.literal(" " + signed(faction.value())).formatted(Formatting.GRAY));
            this.textRenderer.drawWithShadow(matrices, standing, right - this.textRenderer.getWidth(standing), y, 0xFFFFFF);
            this.drawBar(matrices, left, right, y + 12, faction.value(), tier);
        }
        if (this.factionScroll + visible < factions.size()) {
            this.textRenderer.draw(matrices, "▼", right - 6, this.panelY + this.panelHeight - PAD - 8, 0x888888);
        }
    }

    /** A bar from -1000 to 1000: dark track, filled from 0 to the value in the tier's colour, ticks between tiers. */
    private void drawBar(MatrixStack matrices, int left, int right, int y, int value, ReputationTier tier) {
        int height = 6;
        fill(matrices, left - 1, y - 1, right + 1, y + height + 1, 0xFF000000);
        fill(matrices, left, y, right, y + height, 0xFF2A2A2A);
        int zero = barX(left, right, 0);
        int at = barX(left, right, value);
        Integer color = tier.color.getColorValue();
        int fillColor = 0xFF000000 | (color == null ? 0xAAAAAA : color);
        if (at >= zero) {
            fill(matrices, zero, y, Math.max(at, zero + 1), y + height, fillColor);
        } else {
            fill(matrices, at, y, zero, y + height, fillColor);
        }
        // Tier boundaries (where Unfriendly, Neutral, Friendly, Honored and Exalted begin)
        for (ReputationTier each : ReputationTier.values()) {
            if (each.ordinal() == 0) {
                continue;
            }
            int tick = barX(left, right, each.min);
            fill(matrices, tick, y - 2, tick + 1, y + height + 2, 0xFFD0D0D0);
        }
        // The player's mark
        fill(matrices, at - 1, y - 3, at + 1, y + height + 3, 0xFFFFFFFF);
    }

    private static int barX(int left, int right, int value) {
        int clamped = Math.max(-1000, Math.min(1000, value));
        return left + Math.round((right - 1 - left) * (clamped + 1000) / 2000.0f);
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : String.valueOf(value);
    }

    // ---- Input ----

    @Override
    public void tick() {
        if (ClientQuests.active() != this.builtActive || ClientQuests.finished() != this.builtFinished
                || ClientReputation.entries() != this.builtReputation) {
            this.clearAndInit();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int step = amount > 0 ? -1 : 1;
        if (this.tab == Tab.FACTIONS) {
            this.factionScroll = Math.max(0, this.factionScroll + step);
            return true;
        }
        if (mouseX < this.detailX() - PAD / 2) {
            int rows = this.headers.size() + this.entries.size();
            this.listScroll = Math.max(0, Math.min(this.listScroll + step, rows - this.listHeight / ENTRY_HEIGHT));
            this.layoutList();
        } else {
            this.detailScroll = Math.max(0, this.detailScroll + step);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (journalKey != null && journalKey.matchesKey(keyCode, scanCode)) {
            this.close();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            this.setTab(this.tab == Tab.QUESTS ? Tab.FACTIONS : Tab.QUESTS);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    public Tab tab() {
        return this.tab;
    }

    // ---- Widgets ----

    private record HeaderRow(Text title, int row) {
    }

    /** A quest in the left list; its label is the quest's title, so DevScript's {@code widget <title>} selects it. */
    private class QuestEntry extends PressableWidget {
        final ClientQuests.Active active;
        final ClientQuests.Finished finished;
        int row;
        boolean selected;

        QuestEntry(ClientQuests.Active active, ClientQuests.Finished finished) {
            super(0, 0, LIST_WIDTH, ENTRY_HEIGHT, active != null ? active.title() : finished.title());
            this.active = active;
            this.finished = finished;
        }

        Identifier quest() {
            return this.active != null ? this.active.quest() : this.finished.quest();
        }

        boolean isFinished() {
            return this.active == null;
        }

        @Override
        public void onPress() {
            QuestJournalScreen.this.select(this);
        }

        @Override
        public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            if (this.selected) {
                fill(matrices, this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, SELECTED);
            } else if (this.isHovered()) {
                fill(matrices, this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, HOVER);
            }
            String prefix = this.active != null ? (this.active.tracked() ? "» " : "  ")
                    : (this.finished.rewardWaiting() ? "! " : "✔ ");
            int color = this.active != null ? 0xFFFFFF : this.finished.rewardWaiting() ? 0xFFFF55 : 0x808080;
            Text label = QuestTrackerHud.fit(QuestJournalScreen.this.textRenderer,
                    Text.literal(prefix).append(this.getMessage()), this.width - 6);
            QuestJournalScreen.this.textRenderer.drawWithShadow(matrices, label, this.getX() + 4, this.getY() + 2, color);
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
            builder.put(NarrationPart.TITLE, this.getMessage());
        }
    }

    /** Track / Abandon, removed when the selection changes. */
    private static class DetailButton extends ButtonWidget {
        DetailButton(int x, int y, int width, Text message, Runnable action) {
            super(x, y, width, 20, message, button -> action.run(), DEFAULT_NARRATION_SUPPLIER);
        }
    }
}
