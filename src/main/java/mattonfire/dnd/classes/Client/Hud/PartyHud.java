package mattonfire.dnd.classes.Client.Hud;

import java.util.ArrayList;
import java.util.List;

import mattonfire.dnd.classes.ClassInfo;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PartyRole;
import mattonfire.dnd.classes.Party.PartyEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Top-left list of the other party members with their role icon and a health bar each. The server
 * sends the data every half second (see {@link PartyEvents#syncHud}).
 */
public final class PartyHud {
    private record Member(String name, boolean leader, boolean online, float health, float maxHealth,
            float absorption, boolean nearby, int classId) {
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
                        buf.readFloat(), buf.readFloat(), buf.readBoolean(), buf.readVarInt()));
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

        // Rows share one width, wide enough that no name runs into its health text.
        int width = BAR_WIDTH;
        for (Member member : current) {
            int icon = roleOf(member.classId()) != null ? PartyRole.ICON_SIZE + 2 : 0;
            width = Math.max(width, icon + text.getWidth(nameOf(member)) + 6 + text.getWidth(statusOf(member)) - 2);
        }

        int y = Y;
        for (Member member : current) {
            String name = nameOf(member);
            int nameColor = !member.online() ? 0x777777 : member.nearby() ? 0xFFFFFF : 0xAAAAAA;
            PartyRole role = roleOf(member.classId());
            int nameX = X;
            if (role != null) {
                RoleIcon.draw(matrices, X, y, role);
                nameX += PartyRole.ICON_SIZE + 2;
            }
            text.drawWithShadow(matrices, name, nameX, y, nameColor);

            String status = statusOf(member);
            text.drawWithShadow(matrices, status, X + width + 2 - text.getWidth(status), y,
                    member.online() ? 0xFF5555 : 0x555555);

            int barY = y + 10;
            DrawableHelper.fill(matrices, X - 1, barY - 1, X + width + 1, barY + BAR_HEIGHT + 1, 0xC0000000);
            if (member.online() && member.maxHealth() > 0) {
                float fraction = Math.min(1, Math.max(0, member.health() / member.maxHealth()));
                int filled = Math.round(width * fraction);
                DrawableHelper.fill(matrices, X, barY, X + filled, barY + BAR_HEIGHT, healthColor(fraction));

                if (member.absorption() > 0) {
                    int extra = Math.round(width * Math.min(1, member.absorption() / member.maxHealth()));
                    DrawableHelper.fill(matrices, X, barY + BAR_HEIGHT - 2, X + extra, barY + BAR_HEIGHT,
                            0xFFFFCC33);
                }
            }
            y += ROW_HEIGHT;
        }
    }

    private static String nameOf(Member member) {
        return (member.leader() ? "★ " : "") + member.name();
    }

    private static String statusOf(Member member) {
        return member.online()
                ? (member.health() <= 0 ? "dead" : Math.round(member.health()) + "/" + Math.round(member.maxHealth()))
                : "offline";
    }

    /** Primary role of a class id from the packet, or null for no class. */
    private static PartyRole roleOf(int classId) {
        if (classId <= 0) {
            return null;
        }
        try {
            ClassInfo info = ClassInfo.get(DndCharacter.fromValue(classId));
            return info == null ? null : info.role();
        } catch (IllegalArgumentException e) {
            return null;
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
