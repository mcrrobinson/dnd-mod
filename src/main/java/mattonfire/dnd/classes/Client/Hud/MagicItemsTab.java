package mattonfire.dnd.classes.Client.Hud;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.magic.Attunement;
import mattonfire.dnd.magic.AttunementSnapshot;
import mattonfire.dnd.magic.ForgeBlessing;
import mattonfire.dnd.magic.MagicData;
import mattonfire.dnd.magic.MagicItems;
import mattonfire.dnd.magic.MagicKind;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The Attunement Table's Magic Items tab, drawn inside {@link SkillTreeScreen}'s panel: the player's bonds
 * (carried or missing, with Unattune / Release), the magic items they carry (with Attune and, for a Forge
 * Cleric, Bless), and the 3 s channel bar. Buttons only send requests; the server checks everything again.
 */
final class MagicItemsTab {
    private static final int BOND_ROW = 12;
    private static final int ITEM_ROW = 18;
    private static final int BUTTON_WIDTH = 46;
    private static final int GOLD = 0xFFE0B040;
    private static final int BORDER = 0xFF6B4FA0;

    /** A button drawn this frame; clicks are matched against the last frame's buttons. */
    private record Button(int x, int y, int w, int h, Runnable action) {
        boolean over(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private final List<Button> buttons = new ArrayList<>();
    private int scroll;
    private int maxScroll;
    private int listTop;
    private int listBottom;

    /** Inventory slots holding magic items worth listing (no potions or drinks), plus blessable gear. */
    private static List<Integer> itemSlots(PlayerEntity player, AttunementSnapshot snapshot) {
        List<Integer> slots = new ArrayList<>();
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty())
                continue;
            MagicItems.Info info = MagicItems.info(stack);
            boolean magic = info != null && info.kind() != MagicKind.POTION && info.kind() != MagicKind.CONSUMABLE;
            boolean blessable = snapshot.forge() == 1 && ForgeBlessing.canBless(player, i, stack);
            if (magic || blessable)
                slots.add(i);
        }
        return slots;
    }

    private static boolean carried(PlayerEntity player, UUID uuid) {
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            if (uuid.equals(MagicData.uuid(inventory.getStack(i))))
                return true;
        }
        return false;
    }

    /** Draws the tab and returns the hovered item stack (for its tooltip), or null. */
    @Nullable
    ItemStack render(MatrixStack matrices, TextRenderer text, ItemRenderer items, int left, int top, int width,
            int height, int mouseX, int mouseY) {
        buttons.clear();
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null)
            return null;
        AttunementSnapshot snapshot = AttunementSnapshot.client;
        int centerX = left + width / 2;
        int y = top + 20;

        DrawableHelper.drawCenteredTextWithShadow(matrices, text, Text.literal("Bonds " + snapshot.bonds().size()
                + "/" + snapshot.slots()).formatted(Formatting.LIGHT_PURPLE), centerX, y, 0xFFFFFF);
        y += 12;

        for (int i = 0; i < snapshot.slots(); i++) {
            int rowY = y + i * BOND_ROW;
            DrawableHelper.fill(matrices, left + 8, rowY - 1, left + width - 8, rowY + BOND_ROW - 2, 0xFF18181E);
            if (i >= snapshot.bonds().size()) {
                text.drawWithShadow(matrices, Text.literal("- empty -").formatted(Formatting.DARK_GRAY), left + 12,
                        rowY + 1, 0xFFFFFF);
                continue;
            }
            AttunementSnapshot.Bond bond = snapshot.bonds().get(i);
            boolean carried = carried(player, bond.uuid());
            Text status = bond.cursed() ? Text.literal("cursed").formatted(Formatting.DARK_RED)
                    : carried ? Text.literal("carried").formatted(Formatting.GREEN)
                            : Text.literal("missing").formatted(Formatting.RED);
            int statusX = left + width - 12 - BUTTON_WIDTH - 4 - text.getWidth(status);
            String name = text.trimToWidth(bond.name().getString(), statusX - left - 16);
            text.drawWithShadow(matrices, Text.literal(name).setStyle(bond.name().getStyle()), left + 12, rowY + 1,
                    0xFFFFFF);
            text.drawWithShadow(matrices, status, statusX, rowY + 1, 0xFFFFFF);
            if (!bond.cursed()) {
                UUID uuid = bond.uuid();
                button(matrices, text, left + width - 12 - BUTTON_WIDTH, rowY - 1, carried ? "Unattune" : "Release",
                        mouseX, mouseY, () -> sendUuid(Attunement.C2S_UNATTUNE, uuid));
            }
        }
        y += snapshot.slots() * BOND_ROW + 2;
        DrawableHelper.fill(matrices, left + 10, y, left + width - 10, y + 1, BORDER);
        y += 4;
        text.drawWithShadow(matrices, Text.literal("Carried magic items").formatted(Formatting.GRAY), left + 10, y,
                0xFFFFFF);
        y += 11;

        listTop = y;
        listBottom = top + height - 30;
        List<Integer> slots = itemSlots(player, snapshot);
        int visible = Math.max(1, (listBottom - listTop) / ITEM_ROW);
        maxScroll = Math.max(0, slots.size() - visible);
        scroll = Math.min(scroll, maxScroll);
        ItemStack hovered = null;
        if (slots.isEmpty()) {
            DrawableHelper.drawCenteredTextWithShadow(matrices, text, Text.literal("You carry no magic items.")
                    .formatted(Formatting.DARK_GRAY), centerX, y + 8, 0xFFFFFF);
        }
        for (int row = 0; row < visible && row + scroll < slots.size(); row++) {
            int slot = slots.get(row + scroll);
            ItemStack stack = player.getInventory().getStack(slot);
            int rowY = listTop + row * ITEM_ROW;
            boolean channeling = snapshot.channelSlot() == slot;
            DrawableHelper.fill(matrices, left + 8, rowY, left + width - 8, rowY + ITEM_ROW - 1,
                    channeling ? 0xFF2A2440 : 0xFF18181E);
            items.renderInGui(matrices, stack, left + 10, rowY);

            int right = left + width - 12;
            MagicItems.Info info = MagicItems.info(stack);
            boolean bonded = info != null && player.getUuid().equals(MagicData.attunedTo(stack));
            Text status = null;
            if (channeling) {
                status = Text.literal("attuning...").formatted(Formatting.LIGHT_PURPLE);
            } else if (info != null && info.attunement()) {
                UUID owner = MagicData.attunedTo(stack);
                if (bonded) {
                    status = Text.literal("Attuned").formatted(Formatting.AQUA);
                } else if (owner != null) {
                    status = Text.literal("someone's").formatted(Formatting.GRAY);
                } else if (!Attunement.classAllowed(player, info)) {
                    status = Text.literal("not your class").formatted(Formatting.RED);
                } else {
                    right -= BUTTON_WIDTH;
                    button(matrices, text, right, rowY + 2, "Attune", mouseX, mouseY,
                            () -> sendSlot(Attunement.C2S_ATTUNE, slot));
                    right -= 4;
                }
            } else if (info != null) {
                status = Text.literal(MagicData.isIdentified(stack) ? "no attunement" : "unidentified")
                        .formatted(Formatting.DARK_GRAY);
            }
            if (snapshot.forge() == 1 && ForgeBlessing.canBless(player, slot, stack)) {
                right -= BUTTON_WIDTH;
                button(matrices, text, right, rowY + 2, "Bless +1", mouseX, mouseY,
                        () -> sendSlot(Attunement.C2S_FORGE_BLESS, slot));
                right -= 4;
            }
            if (status != null) {
                right -= text.getWidth(status);
                text.drawWithShadow(matrices, status, right, rowY + 5, 0xFFFFFF);
                right -= 4;
            }
            String name = text.trimToWidth(stack.getName().getString(), right - left - 30);
            text.drawWithShadow(matrices, Text.literal(name).setStyle(stack.getName().getStyle()), left + 28, rowY + 5,
                    0xFFFFFF);
            if (mouseX >= left + 8 && mouseX < left + 28 && mouseY >= rowY && mouseY < rowY + ITEM_ROW - 1) {
                hovered = stack;
            }
        }
        if (maxScroll > 0) {
            text.drawWithShadow(matrices, Text.literal((scroll + 1) + "/" + (maxScroll + 1)).formatted(Formatting.DARK_GRAY),
                    left + width - 30, listTop - 11, 0xFFFFFF);
        }

        // Channel bar, or a hint
        int barY = top + height - 24;
        DrawableHelper.fill(matrices, left + 10, barY - 4, left + width - 10, barY - 3, BORDER);
        float progress = snapshot.channelProgress(System.currentTimeMillis());
        if (progress >= 0) {
            int barX = centerX - 80;
            DrawableHelper.fill(matrices, barX, barY + 1, barX + 160, barY + 6, 0xFF000000);
            DrawableHelper.fill(matrices, barX, barY + 1, barX + (int) (160 * progress), barY + 6, 0xFFB070F0);
            DrawableHelper.drawCenteredTextWithShadow(matrices, text, Text.literal("Attuning... stay near the table")
                    .formatted(Formatting.LIGHT_PURPLE), centerX, barY + 9, 0xFFFFFF);
        } else {
            DrawableHelper.drawCenteredTextWithShadow(matrices, text, Text.literal("Attuning takes 3 s at the table.")
                    .formatted(Formatting.GRAY), centerX, barY, 0xFFFFFF);
            Text second = snapshot.forge() == 1
                    ? Text.literal("Blessing of the Forge: ready").formatted(Formatting.GOLD)
                    : snapshot.forge() == 2 ? Text.literal("Blessing of the Forge: used until a long rest")
                            .formatted(Formatting.DARK_GRAY)
                            : Text.literal("A short rest also attunes one item.").formatted(Formatting.DARK_GRAY);
            DrawableHelper.drawCenteredTextWithShadow(matrices, text, second, centerX, barY + 11, 0xFFFFFF);
        }
        return hovered;
    }

    private void button(MatrixStack matrices, TextRenderer text, int x, int y, String label, int mouseX, int mouseY,
            Runnable action) {
        Button button = new Button(x, y, BUTTON_WIDTH, 11, action);
        boolean hover = button.over(mouseX, mouseY);
        DrawableHelper.fill(matrices, x, y, x + BUTTON_WIDTH, y + 11, hover ? GOLD : BORDER);
        DrawableHelper.fill(matrices, x + 1, y + 1, x + BUTTON_WIDTH - 1, y + 10, hover ? 0xFF3A3020 : 0xFF241C36);
        DrawableHelper.drawCenteredTextWithShadow(matrices, text, Text.literal(label), x + BUTTON_WIDTH / 2, y + 2,
                hover ? 0xFFE080 : 0xE0E0E0);
        buttons.add(button);
    }

    boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0)
            return false;
        for (Button b : buttons) {
            if (b.over(mouseX, mouseY)) {
                b.action().run();
                return true;
            }
        }
        return false;
    }

    boolean mouseScrolled(double amount) {
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(amount)));
        return true;
    }

    private static void sendSlot(net.minecraft.util.Identifier packet, int slot) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(slot);
        ClientPlayNetworking.send(packet, buf);
    }

    private static void sendUuid(net.minecraft.util.Identifier packet, UUID uuid) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeUuid(uuid);
        ClientPlayNetworking.send(packet, buf);
    }
}
