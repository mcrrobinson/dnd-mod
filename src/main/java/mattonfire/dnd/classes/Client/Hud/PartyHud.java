package mattonfire.dnd.classes.Client.Hud;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import mattonfire.dnd.classes.ClassInfo;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PartyRole;
import mattonfire.dnd.classes.Downed.Downed;
import mattonfire.dnd.classes.mixin.BossBarHudAccessor;
import mattonfire.dnd.classes.Party.PartyEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

/**
 * Top-left list of the other party members with their role icon and a health bar each. The server
 * sends the data every half second (see {@link PartyEvents#syncHud}). A Downed member shows
 * "DOWNED" in red, or "STABLE" in yellow, in place of their HP, and their death saves ("●●○ ✕○○",
 * passes and fails) on a pulsing bar. While a boss bar is showing, the rows stop short of it and
 * long names are cut.
 */
public final class PartyHud {
    private record Member(String name, boolean leader, boolean online, float health, float maxHealth,
            float absorption, boolean nearby, int classId, UUID uuid, byte downed, int passes, int fails) {
        boolean isDowned() {
            return online && (downed & Downed.BIT_DOWNED) != 0;
        }

        boolean isStable() {
            return (downed & Downed.BIT_STABLE) != 0;
        }
    }

    private static final int X = 4;
    private static final int Y = 4;
    private static final int BAR_WIDTH = 81;
    private static final int BAR_HEIGHT = 5;
    private static final int ROW_HEIGHT = 18;
    /** Vanilla boss bars are 182 wide, centred. */
    private static final int BOSS_BAR_HALF_WIDTH = 91;
    /** Narrowest the rows get when making room for a boss bar. */
    private static final int MIN_WIDTH = 50;

    private static volatile List<Member> members = List.of();
    private static volatile Set<UUID> memberIds = Set.of();

    private PartyHud() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(PartyEvents.S2C_PARTY_HUD, (client, handler, buf, sender) -> {
            int count = buf.readVarInt();
            List<Member> received = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                received.add(new Member(buf.readString(), buf.readBoolean(), buf.readBoolean(), buf.readFloat(),
                        buf.readFloat(), buf.readFloat(), buf.readBoolean(), buf.readVarInt(), buf.readUuid(),
                        buf.readByte(), buf.readByte(), buf.readByte()));
            }
            members = List.copyOf(received);
            Set<UUID> ids = new HashSet<>();
            for (Member member : received) {
                ids.add(member.uuid());
            }
            memberIds = Set.copyOf(ids);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            members = List.of();
            memberIds = Set.of();
        });
        HudRenderCallback.EVENT.register((matrices, tickDelta) -> render(matrices));
    }

    /** Whether this player is in the local player's party (not counting the local player). */
    public static boolean isMember(UUID uuid) {
        return memberIds.contains(uuid);
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
        // Stop short of the boss bars (top centre); names that don't fit are cut below
        if (!((BossBarHudAccessor) client.inGameHud.getBossBarHud()).dndclasses$getBossBars().isEmpty()) {
            int bossLeft = client.getWindow().getScaledWidth() / 2 - BOSS_BAR_HALF_WIDTH;
            width = Math.max(MIN_WIDTH, Math.min(width, bossLeft - 4 - X));
        }

        int y = Y;
        for (Member member : current) {
            Text status = statusOf(member);
            int icon = roleOf(member.classId()) != null ? PartyRole.ICON_SIZE + 2 : 0;
            String name = trim(text, nameOf(member), width + 2 - text.getWidth(status) - 4 - icon);
            int nameColor = !member.online() ? 0x777777 : member.nearby() ? 0xFFFFFF : 0xAAAAAA;
            PartyRole role = roleOf(member.classId());
            int nameX = X;
            if (role != null) {
                RoleIcon.draw(matrices, X, y, role);
                nameX += PartyRole.ICON_SIZE + 2;
            }
            text.drawWithShadow(matrices, name, nameX, y, nameColor);

            text.drawWithShadow(matrices, status, X + width + 2 - text.getWidth(status), y,
                    member.online() ? 0xFF5555 : 0x555555);

            int barY = y + 10;
            DrawableHelper.fill(matrices, X - 1, barY - 1, X + width + 1, barY + BAR_HEIGHT + 1, 0xC0000000);
            if (member.online() && member.maxHealth() > 0) {
                float fraction = Math.min(1, Math.max(0, member.health() / member.maxHealth()));
                int filled = Math.round(width * fraction);
                if (member.isDowned()) {
                    // A Downed member's bar is a dim red strip that pulses (yellow once stable)
                    float pulse = 0.5f + 0.5f * MathHelper.sin((client.player.age + client.getTickDelta()) * 0.3f);
                    int alpha = 0x60 + (int) (0x9F * pulse);
                    DrawableHelper.fill(matrices, X, barY, X + width, barY + BAR_HEIGHT,
                            alpha << 24 | (member.isStable() ? 0xE0C030 : 0xC02020));
                    if (!member.isStable()) {
                        // The saves sit on the bar line, so the name line stays as narrow as a health row
                        text.drawWithShadow(matrices, savesOf(member), X + 1, barY - 2, 0xFFFFFF);
                    }
                    y += ROW_HEIGHT;
                    continue;
                }
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

    private static Text statusOf(Member member) {
        if (member.isDowned()) {
            if (member.isStable()) {
                return Text.translatable("hud.dndclasses.party.stable").formatted(Formatting.YELLOW, Formatting.BOLD);
            }
            return Text.translatable("hud.dndclasses.party.downed").formatted(Formatting.RED, Formatting.BOLD);
        }
        return Text.literal(member.online()
                ? (member.health() <= 0 ? "dead" : Math.round(member.health()) + "/" + Math.round(member.maxHealth()))
                : "offline");
    }

    /** A Downed member's death saves: "●●○" passes in green, "✕○○" fails in red. */
    private static Text savesOf(Member member) {
        int passes = Math.min(3, member.passes());
        int fails = Math.min(3, member.fails());
        return Text.literal("\u25CF".repeat(passes) + "\u25CB".repeat(3 - passes)).formatted(Formatting.GREEN)
                .append(Text.literal(" " + "\u2715".repeat(fails) + "\u25CB".repeat(3 - fails))
                        .formatted(Formatting.RED));
    }

    /** {@code name}, cut to {@code maxWidth} with "..." if it's wider. */
    private static String trim(TextRenderer text, String name, int maxWidth) {
        if (text.getWidth(name) <= maxWidth) {
            return name;
        }
        String dots = "...";
        return text.trimToWidth(name, Math.max(0, maxWidth - text.getWidth(dots))) + dots;
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
