package mattonfire.dnd.classes.Client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

/**
 * Client-only text measuring for tooltips built in common item code. Only call it after checking
 * that this is the client (see ArmorTooltips), so a dedicated server never loads MinecraftClient.
 */
@Environment(EnvType.CLIENT)
public final class TooltipTextWidth {
    private TooltipTextWidth() {
    }

    public static int of(String text) {
        return MinecraftClient.getInstance().textRenderer.getWidth(text);
    }
}
