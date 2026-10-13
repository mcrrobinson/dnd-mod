package mattonfire.dnd.classes.Client.Hud;

import java.util.ArrayList;
import java.util.List;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Items.racial.TinkersGoggles;
import mattonfire.dnd.classes.Items.racial.TinkersGogglesItem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Tinker's Goggles, client side: while you wear them and look at a container block within
 * {@link TinkersGogglesItem#RANGE}, the client asks the server what's inside ({@link TinkersGoggles}) every
 * {@link #REFRESH_TICKS} ticks and draws the answer to the right of the crosshair: the block's name, then the
 * items in rows of {@value #PER_ROW} with their counts, or "Sealed" for a container nobody has opened yet.
 * Each new answer is logged ({@code [Goggles]}), which the devscripts check.
 */
public final class GogglesHud {
    static final int REFRESH_TICKS = 10;
    static final int PER_ROW = 9;

    @Nullable
    private static BlockPos asked;
    private static int askedAge;
    @Nullable
    private static BlockPos shown;
    private static boolean sealed;
    private static List<ItemStack> contents = List.of();
    private static String lastLogged = "";

    private GogglesHud() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(TinkersGoggles.S2C_CONTENTS, (client, handler, buf, sender) -> {
            BlockPos pos = buf.readBlockPos();
            boolean isSealed = buf.readBoolean();
            int n = buf.readVarInt();
            List<ItemStack> stacks = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                ItemStack stack = buf.readItemStack();
                stack.setCount(buf.readVarInt());
                stacks.add(stack);
            }
            client.execute(() -> answer(pos, isSealed, stacks));
        });
        ClientTickEvents.END_CLIENT_TICK.register(GogglesHud::tick);
        HudRenderCallback.EVENT.register((matrices, tickDelta) -> render(matrices));
    }

    private static void answer(BlockPos pos, boolean isSealed, List<ItemStack> stacks) {
        shown = pos;
        sealed = isSealed;
        contents = stacks;
        StringBuilder line = new StringBuilder(pos.toShortString()).append(": ");
        if (isSealed) {
            line.append("sealed");
        } else {
            line.append(stacks.size()).append(" kinds");
            for (ItemStack stack : stacks) {
                line.append(", ").append(stack.getCount()).append("x ").append(Registries.ITEM.getId(stack.getItem()));
            }
        }
        if (!line.toString().equals(lastLogged)) {
            lastLogged = line.toString();
            DnDClasses.LOGGER.info("[Goggles] {}", lastLogged);
        }
    }

    /** The container block under the crosshair within range, if the player wears the goggles. */
    @Nullable
    private static BlockPos target(MinecraftClient client) {
        if (client.player == null || client.world == null || !TinkersGogglesItem.wears(client.player)) {
            return null;
        }
        HitResult hit = client.crosshairTarget;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            hit = client.player.raycast(TinkersGogglesItem.RANGE, 1.0F, false);
        }
        if (!(hit instanceof BlockHitResult blockHit2) || hit.getType() != HitResult.Type.BLOCK
                || hit.getPos().squaredDistanceTo(client.player.getEyePos()) > TinkersGogglesItem.RANGE * TinkersGogglesItem.RANGE) {
            return null;
        }
        BlockPos pos = blockHit2.getBlockPos();
        return client.world.getBlockEntity(pos) instanceof net.minecraft.inventory.Inventory ? pos : null;
    }

    private static void tick(MinecraftClient client) {
        BlockPos pos = target(client);
        if (pos == null) {
            asked = null;
            return;
        }
        askedAge++;
        if (!pos.equals(asked) || askedAge >= REFRESH_TICKS) {
            asked = pos;
            askedAge = 0;
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBlockPos(pos);
            ClientPlayNetworking.send(TinkersGoggles.C2S_PEEK, buf);
        }
    }

    private static void render(MatrixStack matrices) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != null || client.options.hudHidden || asked == null || !asked.equals(shown)) {
            return;
        }
        TextRenderer text = client.textRenderer;
        int x = client.getWindow().getScaledWidth() / 2 + 12;
        int y = client.getWindow().getScaledHeight() / 2 - 8;
        Text title = client.world.getBlockState(shown).getBlock().getName().formatted(Formatting.GOLD);
        int rows = sealed || contents.isEmpty() ? 0 : (contents.size() + PER_ROW - 1) / PER_ROW;
        int width = Math.max(text.getWidth(title), Math.min(contents.size(), PER_ROW) * 18) + 6;
        int height = 13 + (rows == 0 ? 10 : rows * 18);
        DrawableHelper.fill(matrices, x - 3, y - 3, x + width, y + height, 0xB0101418);
        DrawableHelper.fill(matrices, x - 3, y - 3, x + width, y - 2, 0xFFC08A3A);
        text.drawWithShadow(matrices, title, x, y, 0xFFFFFF);
        if (sealed) {
            text.drawWithShadow(matrices, Text.translatable("hud.dndclasses.goggles.sealed"), x, y + 12, 0xA0A0A0);
            return;
        }
        if (contents.isEmpty()) {
            text.drawWithShadow(matrices, Text.translatable("hud.dndclasses.goggles.empty"), x, y + 12, 0xA0A0A0);
            return;
        }
        for (int i = 0; i < contents.size(); i++) {
            ItemStack stack = contents.get(i);
            int ix = x + (i % PER_ROW) * 18;
            int iy = y + 11 + (i / PER_ROW) * 18;
            client.getItemRenderer().renderInGuiWithOverrides(matrices, stack, ix, iy);
            client.getItemRenderer().renderGuiItemOverlay(matrices, text, stack, ix, iy,
                    stack.getCount() > 1 ? String.valueOf(stack.getCount()) : null);
        }
    }
}
