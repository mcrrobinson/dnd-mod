package mattonfire.dnd.classes.Client.Hud;

import java.util.ArrayList;
import java.util.List;

import mattonfire.dnd.classes.Party.PartyEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Top-left list of the other party members with a health bar each. The server
 * sends the data every half second (see {@link PartyEvents#syncHud}).
 */
public final class PartyHud {
    private record Member(String name, boolean leader, boolean online, float health, float maxHealth,
            float absorption, boolean nearby) {
    }

    private static final int X = 4;
    private static final int Y = 4;
    private static final int BAR_WIDTH = 81;
    private static final int BAR_HEIGHT = 5;
    private static final int ROW_HEIGHT = 18;

    private static volatile List<Member> members = List.of();

    private PartyHud() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(PartyEvents.S2C_PARTY_HUD, (client, handler, buf, sender) -> {
            int count = buf.readVarInt();
            List<Member> received = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                received.add(new Member(buf.readString(), buf.readBoolean(), buf.readBoolean(), buf.readFloat(),
                        buf.readFloat(), buf.readFloat(), buf.readBoolean()));
            }
            members = List.copyOf(received);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> members = List.of());
        HudRenderCallback.EVENT.register((matrices, tickDelta) -> render(matrices));
    }

    private static void render(MatrixStack matrices) {
        MinecraftClient client = MinecraftClient.getInstance();
        List<Member> current = members;
        if (current.isEmpty() || client.player == null || client.options.hudHidden
                || client.options.debugEnabled) {
            return;
        }
        TextRenderer text = client.textRenderer;

        int y = Y;
        for (Member member : current) {
            String name = (member.leader() ? "★ " : "") + member.name();
            int nameColor = !member.online() ? 0x777777 : member.nearby() ? 0xFFFFFF : 0xAAAAAA;
            text.drawWithShadow(matrices, name, X, y, nameColor);

            String status = member.online()
                    ? (member.health() <= 0 ? "dead" : Math.round(member.health()) + "/" + Math.round(member.maxHealth()))
                    : "offline";
            text.drawWithShadow(matrices, status, X + BAR_WIDTH + 2 - text.getWidth(status), y,
                    member.online() ? 0xFF5555 : 0x555555);

            int barY = y + 10;
            DrawableHelper.fill(matrices, X - 1, barY - 1, X + BAR_WIDTH + 1, barY + BAR_HEIGHT + 1, 0xC0000000);
            if (member.online() && member.maxHealth() > 0) {
                float fraction = Math.min(1, Math.max(0, member.health() / member.maxHealth()));
                int filled = Math.round(BAR_WIDTH * fraction);
                DrawableHelper.fill(matrices, X, barY, X + filled, barY + BAR_HEIGHT, healthColor(fraction));

                if (member.absorption() > 0) {
                    int extra = Math.round(BAR_WIDTH * Math.min(1, member.absorption() / member.maxHealth()));
                    DrawableHelper.fill(matrices, X, barY + BAR_HEIGHT - 2, X + extra, barY + BAR_HEIGHT,
                            0xFFFFCC33);
                }
            }
            y += ROW_HEIGHT;
        }
    }

    private static int healthColor(float fraction) {
        if (fraction > 0.5f) {
            return 0xFF3FBF3F;
        }
        if (fraction > 0.25f) {
            return 0xFFE0C030;
        }
        return 0xFFD03030;
    }
}
