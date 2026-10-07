package mattonfire.dnd.classes.Client.Hud;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Progression.ClassProgress;
import mattonfire.dnd.classes.Progression.ClassTrees;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Progression.SkillNode;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * The current class's skill tree. Opened with the menu key to look at it and
 * spend skill points, or from an Attunement Table ({@code attuning}) where
 * clicking an unlocked skill equips it.
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

    private final boolean attuning;
    private int left;
    private int top;

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

        renderFooter(matrices, progress, centerX);
        super.render(matrices, mouseX, mouseY, delta);

        if (hovered != null) {
            renderOrderedTooltip(matrices, tooltip(progress, hovered), mouseX, mouseY);
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
                .append(Text.literal(active.name() + " (" + active.manaCost() + " mana)")
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

        String hint = attuning ? "Click an unlocked skill to equip or unequip it."
                : "Loadouts are changed at an Attunement Table.";
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
        if (!unlocked) {
            matrices.push();
            matrices.translate(0, 0, 200); // Over the item
            fill(matrices, x, y, x + NODE_SIZE, y + NODE_SIZE, available ? 0x60000000 : 0xB0000000);
            matrices.pop();
        }
    }

    private List<OrderedText> tooltip(ClassProgress progress, SkillNode node) {
        List<OrderedText> lines = new ArrayList<>();
        lines.add(Text.literal(node.name()).formatted(Formatting.BOLD, Formatting.WHITE).asOrderedText());
        lines.add((node.isActive()
                ? Text.literal("Active - " + node.manaCost() + " mana").formatted(Formatting.AQUA)
                : Text.literal("Passive").formatted(Formatting.GREEN)).asOrderedText());
        lines.addAll(textRenderer.wrapLines(Text.literal(node.description()).formatted(Formatting.GRAY), 180));

        Text status;
        boolean equipped = node.isActive() ? progress.activeNode() == node : progress.hasPassive(node.id());
        if (equipped) {
            status = Text.literal(attuning && !node.isActive() ? "Equipped - click to unequip" : "Equipped")
                    .formatted(Formatting.GREEN);
        } else if (progress.isUnlocked(node.id())) {
            status = Text.literal(attuning ? "Click to equip" : "Unlocked - equip at an Attunement Table")
                    .formatted(Formatting.GOLD);
        } else if (progress.canUnlock(node)) {
            status = Text.literal("Click to unlock (" + cost(node) + ")").formatted(Formatting.YELLOW);
        } else if (progress.isReachable(node)) {
            status = Text.literal("Needs " + cost(node)).formatted(Formatting.RED);
        } else {
            List<String> names = node.requires().stream().map(id -> ClassTrees.node(id).name()).toList();
            status = Text.literal("Unlock " + String.join(" or ", names) + " first").formatted(Formatting.DARK_GRAY);
        }
        lines.add(status.asOrderedText());
        return lines;
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
        if (button == 0) {
            ClassProgress progress = ClassProgress.client;
            for (SkillNode node : ClassTrees.get(progress.dndClass)) {
                if (!isOver(node, mouseX, mouseY))
                    continue;
                if (progress.canUnlock(node)) {
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
