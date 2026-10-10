package mattonfire.dnd.classes.Client.Hud;

import mattonfire.dnd.classes.Rest.Charges;
import mattonfire.dnd.classes.Rest.RestSnapshot;
import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.ClassInfo;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Progression.ClassSkills;
import mattonfire.dnd.classes.Progression.ClassProgress;
import mattonfire.dnd.classes.Progression.ClassTrees;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Progression.Ranks;
import mattonfire.dnd.classes.Progression.SkillNode;
import mattonfire.dnd.classes.Progression.Subclass;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * The current class's skill tree. Opened with the menu key to look at it and
 * spend skill points, or from an Attunement Table ({@code attuning}) where
 * clicking an unlocked skill equips it and right-clicking ranks it up. Classes
 * with a bestiary get a second page listing the creatures they've learned,
 * unlocked there too. A banner over each branch names its subclass; from
 * class level 3, clicking one at a table chooses it (after a confirm).
 */
public class SkillTreeScreen extends Screen {
    private static final int PANEL_WIDTH = 260;
    private static final int PANEL_HEIGHT = 226;
    private static final int NODE_SIZE = 22;
    private static final int COL_SPACING = 80;
    private static final int ROW_SPACING = 32;

    private static final int PANEL_COLOR = 0xE0101018;
    private static final int PANEL_BORDER = 0xFF6B4FA0;
    private static final int GOLD = 0xFFE0B040;
    private static final int GREEN = 0xFF50E070;
    private static final int LINE_OFF = 0xFF404048;
    private static final int PIP_ON = 0xFFE0B040;
    private static final int PIP_OFF = 0xFF505058;

    private static final int TAB_HEIGHT = 11;
    private static final int BESTIARY_TOP = 52;
    private static final int BESTIARY_COLUMNS = 2;
    private static final int BESTIARY_ROWS = 7;
    private static final int CELL_WIDTH = 118;
    private static final int CELL_HEIGHT = 18;

    private static final int HEADER_WIDTH = 96;
    private static final int HEADER_TOP = 48;
    private static final int HEADER_HEIGHT = 32;
    private static final int PADLOCK = 0xFFB0B0B8;

    private final boolean attuning;
    private int left;
    private int top;
    private boolean bestiaryPage;
    private int bestiaryScroll;

    public SkillTreeScreen(boolean attuning) {
        super(Text.literal(attuning ? "Attunement Table" : "Skill Tree"));
        this.attuning = attuning;
    }

    @Override
    protected void init() {
        left = (width - PANEL_WIDTH) / 2;
        top = Math.max(4, (height - PANEL_HEIGHT) / 2);
    }

    private int nodeX(SkillNode node) {
        return left + PANEL_WIDTH / 2 + (node.col() - 1) * COL_SPACING - NODE_SIZE / 2;
    }

    private int nodeY(SkillNode node) {
        return top + 52 + node.row() * ROW_SPACING;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        fill(matrices, left - 1, top - 1, left + PANEL_WIDTH + 1, top + PANEL_HEIGHT + 1, PANEL_BORDER);
        fill(matrices, left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, PANEL_COLOR);

        ClassProgress progress = ClassProgress.client;
        DndCharacter dndClass = progress.dndClass;
        int centerX = left + PANEL_WIDTH / 2;

        if (dndClass == DndCharacter.NONE) {
            drawCenteredTextWithShadow(matrices, textRenderer, "Pick a class first.", centerX,
                    top + PANEL_HEIGHT / 2 - 4, 0xFFFFFF);
            super.render(matrices, mouseX, mouseY, delta);
            return;
        }

        renderHeader(matrices, progress, centerX);

        if (!progress.usesBestiary()) {
            bestiaryPage = false;
        } else {
            renderTabs(matrices, mouseX, mouseY);
            if (bestiaryPage) {
                String hovered = renderBestiary(matrices, progress, mouseX, mouseY);
                renderFooter(matrices, progress, centerX);
                super.render(matrices, mouseX, mouseY, delta);
                if (hovered != null) {
                    renderOrderedTooltip(matrices, bestiaryTooltip(progress, hovered), mouseX, mouseY);
                }
                return;
            }
        }

        List<SkillNode> nodes = ClassTrees.get(dndClass);
        if (nodes.isEmpty()) {
            drawCenteredTextWithShadow(matrices, textRenderer, "This class's skill tree is coming soon.", centerX,
                    top + PANEL_HEIGHT / 2, 0xAAAAAA);
            super.render(matrices, mouseX, mouseY, delta);
            return;
        }

        for (SkillNode node : nodes) {
            for (String requirement : node.requires()) {
                SkillNode from = ClassTrees.node(requirement);
                if (from != null) {
                    boolean lit = progress.isUnlocked(from.id()) && progress.isUnlocked(node.id());
                    drawLink(matrices, from, node, lit ? GOLD : LINE_OFF);
                }
            }
        }

        SkillNode hovered = null;
        for (SkillNode node : nodes) {
            drawNode(matrices, progress, node);
            if (isOver(node, mouseX, mouseY)) {
                hovered = node;
            }
        }

        Subclass hoveredSubclass = null;
        for (Subclass subclass : ClassTrees.subclasses(dndClass)) {
            drawSubclassHeader(matrices, progress, subclass, mouseX, mouseY);
            if (isOverHeader(subclass, mouseX, mouseY)) {
                hoveredSubclass = subclass;
            }
        }

        renderFooter(matrices, progress, centerX);
        super.render(matrices, mouseX, mouseY, delta);

        if (hovered != null) {
            renderOrderedTooltip(matrices, tooltip(progress, hovered), mouseX, mouseY);
        } else if (hoveredSubclass != null) {
            renderOrderedTooltip(matrices, subclassTooltip(progress, hoveredSubclass), mouseX, mouseY);
        }
    }

    private void renderHeader(MatrixStack matrices, ClassProgress progress, int centerX) {
        int level = progress.level();
        String title = Progression.name(progress.dndClass) + " - Level " + level
                + (level == ClassProgress.MAX_LEVEL ? " (max)" : "");
        drawCenteredTextWithShadow(matrices, textRenderer, title, centerX, top + 6, 0xFFFFFF);

        // XP bar towards the next level.
        int barWidth = 160;
        int barX = centerX - barWidth / 2;
        int barY = top + 19;
        float fraction = 1.0F;
        String xpText = progress.xp + " XP";
        if (level < ClassProgress.MAX_LEVEL) {
            int from = ClassProgress.LEVEL_XP[level];
            int to = ClassProgress.LEVEL_XP[level + 1];
            fraction = (progress.xp - from) / (float) (to - from);
            xpText = progress.xp + " / " + to + " XP";
        }
        fill(matrices, barX, barY, barX + barWidth, barY + 5, 0xFF000000);
        fill(matrices, barX, barY, barX + (int) (barWidth * fraction), barY + 5, 0xFF7FD040);
        drawCenteredTextWithShadow(matrices, textRenderer, xpText, centerX, barY + 8, 0xA0A0A0);

        int points = progress.points();
        drawCenteredTextWithShadow(matrices, textRenderer,
                Text.literal(points + " skill point" + (points == 1 ? "" : "s"))
                        .formatted(points > 0 ? Formatting.YELLOW : Formatting.GRAY),
                centerX, top + 38, 0xFFFFFF);
    }

    private void renderFooter(MatrixStack matrices, ClassProgress progress, int centerX) {
        int y = top + PANEL_HEIGHT - 42;
        fill(matrices, left + 10, y - 4, left + PANEL_WIDTH - 10, y - 3, PANEL_BORDER);

        SkillNode active = progress.activeNode();
        drawTextWithShadow(matrices, textRenderer, Text.literal("Active: ").formatted(Formatting.GRAY)
                .append(Text.literal(active.name() + " (" + ClassSkills.manaCostFor(progress, active) + " mana" + chargeSuffix(active) + ")")
                        .formatted(Formatting.AQUA)),
                left + 12, y, 0xFFFFFF);

        List<String> names = new ArrayList<>();
        for (int i = 0; i < ClassProgress.PASSIVE_SLOTS; i++) {
            SkillNode passive = i < progress.passives.size() ? ClassTrees.node(progress.passives.get(i)) : null;
            names.add(passive == null ? "-" : passive.name());
        }
        drawTextWithShadow(matrices, textRenderer, Text.literal("Passives: ").formatted(Formatting.GRAY)
                .append(Text.literal(String.join(", ", names)).formatted(Formatting.GREEN)),
                left + 12, y + 11, 0xFFFFFF);

        String hint;
        if (bestiaryPage) {
            hint = attuning ? "Click a learned creature to unlock it." : "Creatures are unlocked at an Attunement Table.";
        } else if (progress.canChooseSubclass()) {
            hint = attuning ? "Choose your subclass: click a branch's banner." : "Choose your subclass at an Attunement Table.";
        } else {
            hint = attuning ? "Click to equip, right-click to rank up." : "Loadouts are changed at an Attunement Table.";
        }
        drawCenteredTextWithShadow(matrices, textRenderer, hint, centerX, y + 25, 0x808080);
    }

    private void drawLink(MatrixStack matrices, SkillNode from, SkillNode to, int color) {
        int x1 = nodeX(from) + NODE_SIZE / 2;
        int y1 = nodeY(from) + NODE_SIZE / 2;
        int x2 = nodeX(to) + NODE_SIZE / 2;
        int y2 = nodeY(to) + NODE_SIZE / 2;
        // Up from the requirement, then across into the node.
        fill(matrices, x1 - 1, Math.min(y1, y2), x1 + 1, Math.max(y1, y2) + 1, color);
        fill(matrices, Math.min(x1, x2), y2 - 1, Math.max(x1, x2) + 1, y2 + 1, color);
    }

    private void drawNode(MatrixStack matrices, ClassProgress progress, SkillNode node) {
        int x = nodeX(node);
        int y = nodeY(node);
        boolean unlocked = progress.isUnlocked(node.id());
        boolean equipped = node.isActive() ? progress.activeNode() == node : progress.hasPassive(node.id());
        boolean available = progress.canUnlock(node);

        int border;
        if (equipped) {
            border = GREEN;
        } else if (unlocked) {
            border = GOLD;
        } else if (available) {
            // Pulse so it's obvious there's something to spend points on.
            float pulse = (float) (Math.sin(System.currentTimeMillis() / 200.0) * 0.5 + 0.5);
            int shade = 0x90 + (int) (pulse * 0x6F);
            border = 0xFF000000 | shade << 16 | shade << 8 | shade;
        } else {
            border = 0xFF303038;
        }

        // Actives get a thicker frame than passives.
        int frame = node.isActive() ? 2 : 1;
        fill(matrices, x - frame, y - frame, x + NODE_SIZE + frame, y + NODE_SIZE + frame, border);
        fill(matrices, x, y, x + NODE_SIZE, y + NODE_SIZE, unlocked ? 0xFF2A2440 : 0xFF18181E);

        ItemStack icon = new ItemStack(Registries.ITEM.get(new Identifier(node.icon())));
        itemRenderer.renderInGui(matrices, icon, x + 3, y + 3);
        matrices.push();
        matrices.translate(0, 0, 200); // Over the item
        if (!unlocked) {
            fill(matrices, x, y, x + NODE_SIZE, y + NODE_SIZE, available ? 0x60000000 : 0xB0000000);
        }
        if (!unlocked && progress.subclassLock(node) == ClassProgress.SubclassLock.OTHER_SUBCLASS) {
            drawPadlock(matrices, x + NODE_SIZE - 7, y + 1);
        }
        int maxRank = node.maxRank();
        if (maxRank > 1) {
            // One pip per rank along the bottom edge, lit up to the current rank.
            int rank = unlocked ? progress.rank(node.id()) : 0;
            int pipsWidth = maxRank * 3 - 1;
            int pipX = x + (NODE_SIZE - pipsWidth) / 2;
            for (int i = 0; i < maxRank; i++) {
                fill(matrices, pipX + i * 3, y + NODE_SIZE - 3, pipX + i * 3 + 2, y + NODE_SIZE - 1,
                        i < rank ? PIP_ON : PIP_OFF);
            }
        }
        matrices.pop();
    }

    private List<OrderedText> tooltip(ClassProgress progress, SkillNode node) {
        List<OrderedText> lines = new ArrayList<>();
        lines.add(Text.literal(node.name()).formatted(Formatting.BOLD, Formatting.WHITE).asOrderedText());
        lines.add((node.isActive()
                ? Text.literal("Active - " + ClassSkills.manaCostFor(progress, node) + " mana").formatted(Formatting.AQUA)
                : Text.literal("Passive").formatted(Formatting.GREEN)).asOrderedText());
        Text chargeLine = chargeLine(node);
        if (chargeLine != null) {
            lines.add(chargeLine.asOrderedText());
        }
        lines.addAll(textRenderer.wrapLines(Text.literal(node.description()).formatted(Formatting.GRAY), 180));
        Subclass owner = ClassTrees.subclassOf(node.id());
        if (owner != null) {
            lines.add(Text.literal(owner.name()).formatted(Formatting.LIGHT_PURPLE).asOrderedText());
        } else if (ClassTrees.isShared(node)) {
            lines.add(Text.literal("Any " + subclassTerm(progress)).formatted(Formatting.LIGHT_PURPLE).asOrderedText());
        }
        addRankLines(lines, progress, node);

        Text status;
        boolean equipped = node.isActive() ? progress.activeNode() == node : progress.hasPassive(node.id());
        if (equipped) {
            status = Text.literal(attuning && !node.isActive() ? "Equipped - click to unequip" : "Equipped")
                    .formatted(Formatting.GREEN);
        } else if (progress.isUnlocked(node.id())) {
            status = Text.literal(attuning ? "Click to equip" : "Unlocked - equip at an Attunement Table")
                    .formatted(Formatting.GOLD);
        } else if (progress.subclassLock(node) == ClassProgress.SubclassLock.NEEDS_SUBCLASS) {
            String term = subclassTerm(progress);
            String article = "aeiou".indexOf(term.charAt(0)) >= 0 ? "an " : "a ";
            status = Text.literal("Needs " + article + term + " (level " + ClassProgress.SUBCLASS_LEVEL + ")")
                    .formatted(Formatting.RED);
        } else if (progress.subclassLock(node) == ClassProgress.SubclassLock.OTHER_SUBCLASS) {
            status = Text.literal(owner.name() + " only").formatted(Formatting.RED);
        } else if (progress.canUnlock(node)) {
            status = Text.literal("Click to unlock (" + cost(node) + ")").formatted(Formatting.YELLOW);
        } else if (progress.isReachable(node)) {
            status = Text.literal("Needs " + cost(node)).formatted(Formatting.RED);
        } else {
            List<String> names = node.requires().stream().map(id -> ClassTrees.node(id).name()).toList();
            status = Text.literal("Unlock " + String.join(" or ", names) + " first").formatted(Formatting.DARK_GRAY);
        }
        lines.add(status.asOrderedText());

        Text rankStatus = rankStatus(progress, node);
        if (rankStatus != null) {
            lines.add(rankStatus.asOrderedText());
        }
        return lines;
    }

    // ---- Subclasses ----

    private int headerX(Subclass subclass) {
        return left + PANEL_WIDTH / 2 + (subclass.col() - 1) * COL_SPACING - HEADER_WIDTH / 2;
    }

    private boolean isOverHeader(Subclass subclass, double mouseX, double mouseY) {
        int x = headerX(subclass);
        return mouseX >= x && mouseX < x + HEADER_WIDTH && mouseY >= top + HEADER_TOP
                && mouseY < top + HEADER_TOP + HEADER_HEIGHT;
    }

    private static String subclassTerm(ClassProgress progress) {
        ClassInfo info = ClassInfo.get(progress.dndClass);
        return info == null ? "subclass" : info.subclassTerm();
    }

    /** The banner over a branch: the subclass's name, and its feature or how to choose it. */
    private void drawSubclassHeader(MatrixStack matrices, ClassProgress progress, Subclass subclass, int mouseX,
            int mouseY) {
        int x = headerX(subclass);
        int y = top + HEADER_TOP;
        boolean chosen = progress.hasSubclass(subclass.id());
        boolean other = !chosen && !progress.subclass.isEmpty();
        boolean choosable = progress.canChooseSubclass();

        int border;
        Text status;
        int nameColor;
        if (chosen) {
            border = GREEN;
            nameColor = 0xFFE0B040;
            status = Text.literal(subclass.featureName()).formatted(Formatting.AQUA);
        } else if (other) {
            border = 0xFF303038;
            nameColor = 0x606060;
            status = Text.literal("Sealed").formatted(Formatting.DARK_GRAY);
        } else if (choosable) {
            float pulse = (float) (Math.sin(System.currentTimeMillis() / 200.0) * 0.5 + 0.5);
            int r = 0x90 + (int) (pulse * 0x50);
            int g = 0x70 + (int) (pulse * 0x40);
            border = 0xFF000000 | r << 16 | g << 8 | 0x30;
            nameColor = 0xFFE0B040;
            status = Text.literal(attuning ? "Click to choose" : "Choose at a table").formatted(Formatting.YELLOW);
        } else {
            border = 0xFF404048;
            nameColor = 0xA0A0A0;
            status = Text.literal("Level " + ClassProgress.SUBCLASS_LEVEL).formatted(Formatting.GRAY);
        }
        boolean hover = isOverHeader(subclass, mouseX, mouseY);
        fill(matrices, x - 1, y - 1, x + HEADER_WIDTH + 1, y + HEADER_HEIGHT + 1, border);
        fill(matrices, x, y, x + HEADER_WIDTH, y + HEADER_HEIGHT, hover ? 0xFF262636 : chosen ? 0xFF201C30 : 0xFF14141A);

        List<OrderedText> name = textRenderer.wrapLines(Text.literal(subclass.name()), HEADER_WIDTH - 4);
        if (name.size() > 2) {
            name = name.subList(0, 2);
        }
        int lines = name.size() + 1;
        int textY = y + (HEADER_HEIGHT - lines * 9) / 2 + 1;
        int centerX = x + HEADER_WIDTH / 2;
        for (OrderedText line : name) {
            drawCenteredTextWithShadow(matrices, textRenderer, line, centerX, textY, nameColor);
            textY += 9;
        }
        drawCenteredTextWithShadow(matrices, textRenderer, status.asOrderedText(), centerX, textY, 0xFFFFFF);
    }

    private List<OrderedText> subclassTooltip(ClassProgress progress, Subclass subclass) {
        List<OrderedText> lines = new ArrayList<>();
        lines.add(Text.literal(subclass.name()).formatted(Formatting.BOLD, Formatting.WHITE).asOrderedText());
        if (!subclass.flavour().isEmpty()) {
            lines.addAll(textRenderer.wrapLines(Text.literal(subclass.flavour())
                    .formatted(Formatting.ITALIC, Formatting.GRAY), 200));
        }
        lines.add(Text.literal("Subclass feature: " + subclass.featureName()).formatted(Formatting.AQUA)
                .asOrderedText());
        lines.addAll(textRenderer.wrapLines(Text.literal(subclass.featureDescription())
                .formatted(Formatting.GRAY), 200));
        if (!subclass.featureReady()) {
            lines.add(Text.literal("(not active yet)").formatted(Formatting.DARK_GRAY).asOrderedText());
        }
        lines.add(Text.literal("Free, and doesn't use a passive slot.").formatted(Formatting.DARK_AQUA)
                .asOrderedText());

        Text status;
        if (progress.hasSubclass(subclass.id())) {
            status = Text.literal("Your " + subclassTerm(progress)).formatted(Formatting.GREEN);
        } else if (!progress.subclass.isEmpty()) {
            status = Text.literal("Sealed: you chose " + progress.subclass().name()).formatted(Formatting.DARK_GRAY);
        } else if (progress.level() < ClassProgress.SUBCLASS_LEVEL) {
            status = Text.literal("Choose at class level " + ClassProgress.SUBCLASS_LEVEL)
                    .formatted(Formatting.RED);
        } else if (attuning) {
            status = Text.literal("Click to choose. The other branch's upper nodes will be sealed.")
                    .formatted(Formatting.YELLOW);
        } else {
            status = Text.literal("Choose at an Attunement Table").formatted(Formatting.GOLD);
        }
        lines.addAll(textRenderer.wrapLines(status, 200));
        return lines;
    }

    /** A little padlock, 6 pixels wide, for nodes only the other subclass can have. */
    private static void drawPadlock(MatrixStack matrices, int x, int y) {
        // Shackle
        fill(matrices, x + 1, y, x + 5, y + 1, PADLOCK);
        fill(matrices, x + 1, y, x + 2, y + 3, PADLOCK);
        fill(matrices, x + 4, y, x + 5, y + 3, PADLOCK);
        // Body
        fill(matrices, x, y + 3, x + 6, y + 8, PADLOCK);
        fill(matrices, x + 2, y + 5, x + 4, y + 6, 0xFF303038);
    }

    private void confirmSubclass(Subclass subclass) {
        ClassProgress progress = ClassProgress.client;
        List<Subclass> all = ClassTrees.subclasses(progress.dndClass);
        Subclass other = all.stream().filter(s -> s != subclass).findFirst().orElse(subclass);
        String article = "AEIOU".indexOf(subclass.title().charAt(0)) >= 0 ? "an " : "a ";
        Text title = Text.literal("Become " + article + subclass.title() + "?");
        Text message = Text.literal(subclass.name() + " - " + subclass.featureName() + ": "
                + subclass.featureDescription() + ".\n\n" + other.name()
                + "'s upper nodes will be sealed. Only a Tome of Clear Thought can undo this.");
        client.setScreen(new ConfirmScreen(yes -> {
            if (yes) {
                send(Progression.C2S_CHOOSE_SUBCLASS, subclass.id());
            }
            client.setScreen(this);
        }, title, message));
    }

    /** "Costs N charge(s), recharges on a short/long rest", or null if it costs none or rests are off. */
    private static Text chargeLine(SkillNode node) {
        RestSnapshot rest = RestSnapshot.client;
        int cost = Charges.cost(node);
        if (!node.isActive() || cost <= 0 || !rest.enabled() || rest.max() <= 0) {
            return null;
        }
        return Text.literal("Costs " + cost + " charge" + (cost == 1 ? "" : "s") + ", recharges on a "
                + rest.rechargeGroup().label()).formatted(Formatting.LIGHT_PURPLE);
    }

    private static String chargeSuffix(SkillNode node) {
        int cost = Charges.cost(node);
        RestSnapshot rest = RestSnapshot.client;
        return cost > 0 && rest.enabled() && rest.max() > 0 ? ", " + cost + " charge" + (cost == 1 ? "" : "s") : "";
    }

    private void addRankLines(List<OrderedText> lines, ClassProgress progress, SkillNode node) {
        int maxRank = node.maxRank();
        if (maxRank <= 1)
            return;
        int rank = progress.rank(node.id());
        lines.add(Text.literal("Rank " + Ranks.roman(rank) + "/" + Ranks.roman(maxRank))
                .formatted(Formatting.GOLD).asOrderedText());

        Ranks ranks = node.ranks();
        String now = ranks.describe(rank);
        if (!now.isEmpty()) {
            lines.addAll(textRenderer.wrapLines(Text.literal("Now: " + now).formatted(Formatting.WHITE), 180));
        }
        if (rank < maxRank) {
            String next = ranks.describe(rank + 1);
            String cost = "level " + Ranks.levelFor(node.id(), rank + 1) + ", " + Ranks.POINT_COST + " point"
                    + (Ranks.POINT_COST == 1 ? "" : "s");
            lines.addAll(textRenderer.wrapLines(Text.literal("Next: " + (next.isEmpty() ? "" : next + " ")
                    + "(" + cost + ")").formatted(Formatting.DARK_AQUA), 180));
        }
    }

    /** What ranking the node up needs, or null if it has no ranks or isn't unlocked. */
    private Text rankStatus(ClassProgress progress, SkillNode node) {
        if (node.maxRank() <= 1 || !progress.isUnlocked(node.id()))
            return null;
        ClassProgress.RankUp check = progress.canRankUp(node);
        if (check == ClassProgress.RankUp.MAX_RANK)
            return Text.literal("Max rank").formatted(Formatting.GOLD);
        if (check == ClassProgress.RankUp.LEVEL)
            return Text.literal("Next rank needs level " + Ranks.levelFor(node.id(), progress.rank(node.id()) + 1))
                    .formatted(Formatting.RED);
        if (check == ClassProgress.RankUp.POINTS)
            return Text.literal("Next rank needs " + Ranks.POINT_COST + " point").formatted(Formatting.RED);
        return attuning ? Text.literal("Right-click to rank up").formatted(Formatting.YELLOW)
                : Text.literal("Rank up at an Attunement Table").formatted(Formatting.GOLD);
    }

    // ---- Tabs and bestiary ----

    private int tabX(boolean bestiary) {
        return bestiary ? left + PANEL_WIDTH - 4 - textRenderer.getWidth("Bestiary") - 6 : left + 4;
    }

    private int tabWidth(boolean bestiary) {
        return textRenderer.getWidth(bestiary ? "Bestiary" : "Tree") + 6;
    }

    private boolean isOverTab(boolean bestiary, double mouseX, double mouseY) {
        int x = tabX(bestiary);
        return mouseX >= x && mouseX < x + tabWidth(bestiary) && mouseY >= top + 4 && mouseY < top + 4 + TAB_HEIGHT;
    }

    private void renderTabs(MatrixStack matrices, int mouseX, int mouseY) {
        for (boolean bestiary : new boolean[] { false, true }) {
            int x = tabX(bestiary);
            boolean selected = bestiary == bestiaryPage;
            boolean hover = isOverTab(bestiary, mouseX, mouseY);
            fill(matrices, x, top + 4, x + tabWidth(bestiary), top + 4 + TAB_HEIGHT,
                    selected ? PANEL_BORDER : hover ? 0xFF303040 : 0xFF202028);
            textRenderer.drawWithShadow(matrices, bestiary ? "Bestiary" : "Tree", x + 3, top + 6,
                    selected ? 0xFFFFFF : 0xA0A0A0);
        }
    }

    private List<String> bestiaryEntries(ClassProgress progress) {
        return new ArrayList<>(progress.learned);
    }

    private int maxBestiaryScroll(ClassProgress progress) {
        int rows = (bestiaryEntries(progress).size() + BESTIARY_COLUMNS - 1) / BESTIARY_COLUMNS;
        return Math.max(0, rows - BESTIARY_ROWS);
    }

    private int cellX(int index) {
        return left + (PANEL_WIDTH - BESTIARY_COLUMNS * CELL_WIDTH) / 2 + (index % BESTIARY_COLUMNS) * CELL_WIDTH;
    }

    private int cellY(int index) {
        return top + BESTIARY_TOP + (index / BESTIARY_COLUMNS - bestiaryScroll) * CELL_HEIGHT;
    }

    /** The learned entity under the mouse, or null. */
    private String bestiaryAt(ClassProgress progress, double mouseX, double mouseY) {
        List<String> entries = bestiaryEntries(progress);
        for (int i = 0; i < entries.size(); i++) {
            int row = i / BESTIARY_COLUMNS - bestiaryScroll;
            if (row < 0 || row >= BESTIARY_ROWS)
                continue;
            int x = cellX(i);
            int y = cellY(i);
            if (mouseX >= x && mouseX < x + CELL_WIDTH - 2 && mouseY >= y && mouseY < y + CELL_HEIGHT - 1) {
                return entries.get(i);
            }
        }
        return null;
    }

    /** Draws the learned creatures and returns the hovered one, or null. */
    private String renderBestiary(MatrixStack matrices, ClassProgress progress, int mouseX, int mouseY) {
        List<String> entries = bestiaryEntries(progress);
        int centerX = left + PANEL_WIDTH / 2;
        if (entries.isEmpty()) {
            drawCenteredTextWithShadow(matrices, textRenderer, "Nothing learned yet.", centerX, top + 90, 0xAAAAAA);
            drawCenteredTextWithShadow(matrices, textRenderer, "Kill creatures as this class to learn them.",
                    centerX, top + 102, 0x808080);
            return null;
        }
        bestiaryScroll = Math.min(bestiaryScroll, maxBestiaryScroll(progress));

        for (int i = 0; i < entries.size(); i++) {
            int row = i / BESTIARY_COLUMNS - bestiaryScroll;
            if (row < 0 || row >= BESTIARY_ROWS)
                continue;
            String id = entries.get(i);
            int x = cellX(i);
            int y = cellY(i);
            boolean unlocked = progress.bestiary.contains(id);
            boolean available = progress.canUnlockBestiary(id);
            int background = unlocked ? 0xFF2A2440 : available ? 0xFF2A2A20 : 0xFF18181E;
            if (mouseX >= x && mouseX < x + CELL_WIDTH - 2 && mouseY >= y && mouseY < y + CELL_HEIGHT - 1) {
                background = 0xFF383850;
            }
            fill(matrices, x, y, x + CELL_WIDTH - 2, y + CELL_HEIGHT - 1, background);
            fill(matrices, x, y, x + 1, y + CELL_HEIGHT - 1, unlocked ? GREEN : available ? GOLD : 0xFF303038);

            Item egg = eggFor(id);
            if (egg != null) {
                itemRenderer.renderInGui(matrices, new ItemStack(egg), x + 2, y);
            }
            String name = textRenderer.trimToWidth(Progression.entityName(id), CELL_WIDTH - 24);
            textRenderer.drawWithShadow(matrices, name, x + 20, y + 5,
                    unlocked ? 0xFFFFFF : available ? 0xE0D080 : 0x808080);
        }

        int maxScroll = maxBestiaryScroll(progress);
        if (maxScroll > 0) {
            drawCenteredTextWithShadow(matrices, textRenderer, (bestiaryScroll + 1) + "/" + (maxScroll + 1)
                    + " (scroll)", centerX, top + BESTIARY_TOP + BESTIARY_ROWS * CELL_HEIGHT - 2, 0x606060);
        }
        return bestiaryAt(progress, mouseX, mouseY);
    }

    private static Item eggFor(String entityId) {
        Identifier id = Identifier.tryParse(entityId);
        if (id == null || !Registries.ENTITY_TYPE.containsId(id))
            return null;
        return SpawnEggItem.forEntity(Registries.ENTITY_TYPE.get(id));
    }

    private List<OrderedText> bestiaryTooltip(ClassProgress progress, String id) {
        List<OrderedText> lines = new ArrayList<>();
        lines.add(Text.literal(Progression.entityName(id)).formatted(Formatting.BOLD, Formatting.WHITE)
                .asOrderedText());
        int needed = progress.bestiaryRank(id);
        SkillNode root = ClassTrees.root(progress.dndClass);
        if (needed > 1 && needed != Integer.MAX_VALUE) {
            lines.add(Text.literal("Tier " + Ranks.roman(needed)).formatted(Formatting.GRAY).asOrderedText());
        }

        Text status;
        if (progress.bestiary.contains(id)) {
            status = Text.literal("Unlocked").formatted(Formatting.GREEN);
        } else if (needed > root.maxRank()) {
            status = Text.literal("Can't be unlocked").formatted(Formatting.DARK_GRAY);
        } else if (progress.rank(root.id()) < needed) {
            status = Text.literal("Needs " + root.name() + " rank " + Ranks.roman(needed)).formatted(Formatting.RED);
        } else {
            status = Text.literal(attuning ? "Click to unlock" : "Unlock at an Attunement Table")
                    .formatted(attuning ? Formatting.YELLOW : Formatting.GOLD);
        }
        lines.add(status.asOrderedText());
        return lines;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (bestiaryPage) {
            bestiaryScroll = Math.max(0, Math.min(maxBestiaryScroll(ClassProgress.client),
                    bestiaryScroll - (int) Math.signum(amount)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    private static String cost(SkillNode node) {
        return node.pointCost() + " point" + (node.pointCost() == 1 ? "" : "s");
    }

    private boolean isOver(SkillNode node, double mouseX, double mouseY) {
        int x = nodeX(node);
        int y = nodeY(node);
        return mouseX >= x && mouseX < x + NODE_SIZE && mouseY >= y && mouseY < y + NODE_SIZE;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ClassProgress progress = ClassProgress.client;
        if (button == 0 && progress.usesBestiary()) {
            for (boolean bestiary : new boolean[] { false, true }) {
                if (isOverTab(bestiary, mouseX, mouseY)) {
                    bestiaryPage = bestiary;
                    return true;
                }
            }
        }

        if (bestiaryPage) {
            String id = bestiaryAt(progress, mouseX, mouseY);
            if (button == 0 && id != null && attuning && progress.canUnlockBestiary(id)) {
                send(Progression.C2S_BESTIARY_UNLOCK, id);
            }
            return id != null || super.mouseClicked(mouseX, mouseY, button);
        }

        if (button == 0) {
            for (Subclass subclass : ClassTrees.subclasses(progress.dndClass)) {
                if (isOverHeader(subclass, mouseX, mouseY)) {
                    if (attuning && progress.canChooseSubclass()) {
                        confirmSubclass(subclass);
                    }
                    return true;
                }
            }
        }

        if (button == 0 || button == 1) {
            for (SkillNode node : ClassTrees.get(progress.dndClass)) {
                if (!isOver(node, mouseX, mouseY))
                    continue;
                if (button == 1) {
                    // The server explains a refusal (level, points); only ask for unlocked nodes with ranks left.
                    if (attuning && progress.isUnlocked(node.id()) && progress.rank(node.id()) < node.maxRank()) {
                        send(Progression.C2S_RANK_UP, node.id());
                    }
                } else if (progress.canUnlock(node)) {
                    send(Progression.C2S_UNLOCK, node.id());
                } else if (attuning && progress.isUnlocked(node.id())) {
                    send(Progression.C2S_EQUIP, node.id());
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static void send(Identifier packet, String id) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeString(id);
        ClientPlayNetworking.send(packet, buf);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
