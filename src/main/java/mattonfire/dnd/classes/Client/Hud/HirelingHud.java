package mattonfire.dnd.classes.Client.Hud;

import java.util.ArrayList;
import java.util.List;

import mattonfire.dnd.classes.PartyRole;
import mattonfire.dnd.entity.hireling.HirelingRole;
import mattonfire.dnd.entity.hireling.Hirelings;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

/**
 * The owner's hirelings, one compact row each under the party HUD (shown without a party too): role icon, name,
 * a health bar, a heart with loyalty (gold when Devoted), days left in the purse, and Waiting / Knocked out /
 * Away tags. Fed by {@link Hirelings#S2C_HUD} every 10 ticks.
 */
@Environment(EnvType.CLIENT)
public final class HirelingHud {
    /** One hireling from the packet; entity id -1 when it isn't in the client's world. */
    public record Row(int entityId, String name, HirelingRole role, float health, float maxHealth, int loyalty,
                      int daysPaid, byte flags) {
        boolean has(byte flag) {
            return (this.flags & flag) != 0;
        }
    }

    private static final int X = 4;
    private static final int WIDTH = 120;
    private static final int ROW_HEIGHT = 15;
    private static final int BAR_HEIGHT = 3;
    /** Space between the party rows and these. */
    private static final int GAP = 2;

    private static volatile List<Row> rows = List.of();

    private HirelingHud() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(Hirelings.S2C_HUD, (client, handler, buf, sender) -> {
            int count = buf.readVarInt();
            List<Row> received = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                received.add(new Row(buf.readVarInt(), buf.readString(), HirelingRole.byOrdinal(buf.readVarInt()),
                        buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readVarInt(), buf.readByte()));
            }
            rows = List.copyOf(received);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> rows = List.of());
        HudRenderCallback.EVENT.register((matrices, tickDelta) -> render(matrices));
    }

    /** The hirelings the server last sent. */
    public static List<Row> rows() {
        return rows;
    }

    /**
     * Scout: one of your own conscious Utility hirelings (a Guide) is loaded in your world within {@code radius}
     * blocks of you. Read from the HUD packet's roles and entity ids.
     */
    public static boolean roleNear(MinecraftClient client, net.minecraft.entity.player.PlayerEntity self,
                                   HirelingRole role, double radius) {
        if (client.world == null) {
            return false;
        }
        for (Row row : rows) {
            if (row.role() != role || row.entityId() < 0 || row.has(Hirelings.HUD_KNOCKED_OUT)) {
                continue;
            }
            net.minecraft.entity.Entity entity = client.world.getEntityById(row.entityId());
            if (entity != null && entity.squaredDistanceTo(self) <= radius * radius) {
                return true;
            }
        }
        return false;
    }

    private static void render(MatrixStack matrices) {
        MinecraftClient client = MinecraftClient.getInstance();
        List<Row> current = rows;
        if (current.isEmpty() || client.player == null || client.options.hudHidden || client.options.debugEnabled) {
            return;
        }
        TextRenderer text = client.textRenderer;
        int y = PartyHud.bottom() + (PartyHud.inParty() ? GAP : 0);
        for (Row row : current) {
            PartyRole role = row.role().partyRole;
            RoleIcon.draw(matrices, X, y, role);
            int nameX = X + PartyRole.ICON_SIZE + 2;

            // Right side: loyalty heart, purse days and a tag.
            Text loyalty = Text.literal("❤" + row.loyalty()).styled(s -> s.withColor(row.has(Hirelings.HUD_DEVOTED) ? 0xFFC83C : 0xFF6B6B));
            String days = " " + row.daysPaid() + "d";
            int rightWidth = text.getWidth(loyalty) + text.getWidth(days);
            int right = X + WIDTH;
            text.drawWithShadow(matrices, loyalty, right - rightWidth, y, 0xFFFFFF);
            text.drawWithShadow(matrices, days, right - text.getWidth(days), y, row.daysPaid() > 0 ? 0xE8C14A : 0xFF5555);

            String tag = row.has(Hirelings.HUD_KNOCKED_OUT) ? "KO" : row.has(Hirelings.HUD_AWAY) ? "away"
                    : row.has(Hirelings.HUD_WAITING) ? "wait" : "";
            int tagWidth = tag.isEmpty() ? 0 : text.getWidth(tag) + 3;
            String name = row.name().contains(" ") ? row.name().substring(0, row.name().indexOf(' ')) : row.name();
            int nameRoom = WIDTH - (nameX - X) - rightWidth - tagWidth - 3;
            if (text.getWidth(name) > nameRoom) {
                name = text.trimToWidth(name, Math.max(0, nameRoom - text.getWidth("..."))) + "...";
            }
            text.drawWithShadow(matrices, name, nameX, y, row.has(Hirelings.HUD_AWAY) ? 0xAAAAAA : 0xFFFFFF);
            if (!tag.isEmpty()) {
                text.drawWithShadow(matrices, tag, nameX + text.getWidth(name) + 3, y,
                        row.has(Hirelings.HUD_KNOCKED_OUT) ? 0xFF5555 : 0x8FD3FF);
            }

            int barY = y + 10;
            DrawableHelper.fill(matrices, X - 1, barY - 1, X + WIDTH + 1, barY + BAR_HEIGHT + 1, 0xC0000000);
            float fraction = row.maxHealth() > 0 ? Math.min(1, Math.max(0, row.health() / row.maxHealth())) : 0;
            int color = row.has(Hirelings.HUD_KNOCKED_OUT) ? 0xFF7A2020 : fraction > 0.5f ? 0xFF3FBF3F
                    : fraction > 0.25f ? 0xFFE0C030 : 0xFFD03030;
            DrawableHelper.fill(matrices, X, barY, X + Math.round(WIDTH * fraction), barY + BAR_HEIGHT, color);
            y += ROW_HEIGHT;
        }
    }
}
