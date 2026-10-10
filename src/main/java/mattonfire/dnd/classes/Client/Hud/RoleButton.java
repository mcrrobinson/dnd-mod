package mattonfire.dnd.classes.Client.Hud;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import io.github.cottonmc.cotton.gui.widget.TooltipBuilder;
import io.github.cottonmc.cotton.gui.widget.WButton;
import io.github.cottonmc.cotton.gui.widget.icon.Icon;
import mattonfire.dnd.classes.ClassInfo;
import mattonfire.dnd.classes.PartyRole;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * A class picker button with the class's primary role icon at its right edge,
 * and a tooltip with both roles, the first pro, the special and the obstacle
 * only that class can handle.
 */
@Environment(EnvType.CLIENT)
public class RoleButton extends WButton {
    private static final int TOOLTIP_WIDTH = 200;
    private static final Pattern BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");

    @Nullable
    private final ClassInfo info;

    public RoleButton(Icon icon, Text label, @Nullable ClassInfo info) {
        super(icon, label);
        this.info = info;
    }

    @Override
    public void paint(MatrixStack matrices, int x, int y, int mouseX, int mouseY) {
        super.paint(matrices, x, y, mouseX, mouseY);
        if (info != null) {
            RoleIcon.draw(matrices, x + getWidth() - PartyRole.ICON_SIZE - 3,
                    y + (getHeight() - PartyRole.ICON_SIZE) / 2, info.role());
        }
    }

    @Override
    public void addTooltip(TooltipBuilder tooltip) {
        if (info == null) {
            return;
        }
        tooltip.add(Text.literal(info.name()).formatted(Formatting.BOLD));
        tooltip.add(Text.translatable("gui.dndclasses.class_picker.role", info.role().text(),
                info.secondaryRole().text()).formatted(Formatting.GRAY));
        if (!info.pros().isEmpty()) {
            wrapped(tooltip, Text.literal("+ " + ClassInfo.forGame(info.pros().get(0))).formatted(Formatting.GREEN));
        }
        if (!info.special().isEmpty()) {
            wrapped(tooltip, Text.literal("★ " + ClassInfo.forGame(info.special().get(0)))
                    .formatted(Formatting.LIGHT_PURPLE));
        }
        String exclusive = exclusiveObstacles(info.obstacles());
        if (!exclusive.isEmpty()) {
            wrapped(tooltip, Text.translatable("gui.dndclasses.class_picker.only_you", exclusive)
                    .formatted(Formatting.GOLD));
        }
    }

    /** The bold names in the obstacle list (the ones only this class can handle), joined with commas. */
    static String exclusiveObstacles(List<String> obstacles) {
        StringBuilder names = new StringBuilder();
        for (String obstacle : obstacles) {
            Matcher bold = BOLD.matcher(obstacle);
            if (bold.find()) {
                if (!names.isEmpty()) {
                    names.append(", ");
                }
                names.append(bold.group(1));
            }
        }
        return names.toString();
    }

    private static void wrapped(TooltipBuilder tooltip, Text line) {
        List<OrderedText> lines = MinecraftClient.getInstance().textRenderer.wrapLines(line, TOOLTIP_WIDTH);
        tooltip.add(lines.toArray(OrderedText[]::new));
    }
}
