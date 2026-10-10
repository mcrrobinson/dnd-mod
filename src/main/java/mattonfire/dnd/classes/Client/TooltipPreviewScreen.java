package mattonfire.dnd.classes.Client;

import java.util.ArrayList;
import java.util.List;

import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/**
 * DevScript {@code tooltips}: shows the tooltip of every hotbar item at once, laid out in a grid, so a
 * scripted run can screenshot tooltips without a mouse. Also logs each tooltip's lines.
 */
public class TooltipPreviewScreen extends Screen {
    private final List<ItemStack> stacks = new ArrayList<>();

    public TooltipPreviewScreen(List<ItemStack> stacks) {
        super(Text.literal("Tooltip preview"));
        this.stacks.addAll(stacks);
    }

    @Override
    protected void init() {
        for (int i = 0; i < stacks.size(); i++) {
            List<String> lines = getTooltipFromItem(stacks.get(i)).stream().map(Text::getString).toList();
            DnDClasses.LOGGER.info("[DevScript] tooltip {}: {}", i, String.join(" | ", lines));
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        int x = 4, y = 16, rowHeight = 0;
        for (ItemStack stack : stacks) {
            List<Text> lines = getTooltipFromItem(stack);
            int w = 0;
            for (Text line : lines)
                w = Math.max(w, textRenderer.getWidth(line));
            int h = lines.size() * 10 + 8;
            if (x + w + 24 > width) {
                x = 4;
                y += rowHeight + 8;
                rowHeight = 0;
            }
            // renderTooltip draws 12 px right of and 12 px above the point it's given
            renderTooltip(matrices, stack, x - 12, y + 12);
            x += w + 20;
            rowHeight = Math.max(rowHeight, h);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
