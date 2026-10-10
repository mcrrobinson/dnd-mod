package mattonfire.dnd.quest.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Toasts (top-right, like advancements) when a quest starts, a stage is completed or a quest is finished.
 * Worked out on the client by comparing one {@code quest_sync} with the next, so the server's chat lines
 * and sounds stay as they are and the toasts make no sound of their own.
 */
public final class QuestToasts {
    private QuestToasts() {
    }

    static void onSync(MinecraftClient client, List<ClientQuests.Active> before, List<ClientQuests.Active> after,
                       List<ClientQuests.Finished> finished) {
        for (ClientQuests.Active quest : after) {
            ClientQuests.Active old = find(before, quest.instance());
            if (old == null) {
                // A fork (leaving the party) gets a new instance of a quest you already had: no toast for that.
                if (before.stream().noneMatch(other -> other.quest().equals(quest.quest()))) {
                    show(client, Kind.STARTED, quest.title());
                }
            } else if (quest.stage() > old.stage()) {
                show(client, Kind.STAGE, old.stageTitle());
            }
        }
        for (ClientQuests.Active old : before) {
            if (find(after, old.instance()) == null
                    && after.stream().noneMatch(quest -> quest.quest().equals(old.quest()))
                    && finished.stream().anyMatch(done -> done.quest().equals(old.quest()))) {
                show(client, Kind.FINISHED, old.title());
            }
        }
    }

    private static ClientQuests.Active find(List<ClientQuests.Active> list, int instance) {
        for (ClientQuests.Active quest : list) {
            if (quest.instance() == instance) {
                return quest;
            }
        }
        return null;
    }

    private static void show(MinecraftClient client, Kind kind, Text title) {
        DnDClasses.LOGGER.info("[Quests] toast: {} {}", kind, title.getString());
        client.getToastManager().add(new QuestToast(kind, title));
    }

    enum Kind {
        STARTED("quest.dndclasses.toast.started", 0xFFFF55),
        STAGE("quest.dndclasses.toast.stage", 0x55FF55),
        FINISHED("quest.dndclasses.toast.finished", 0xFFAA00);

        final String key;
        final int color;

        Kind(String key, int color) {
            this.key = key;
            this.color = color;
        }
    }

    /** A 160x32 toast: a book icon, the kind ("Quest started") and the quest or stage title. */
    private record QuestToast(Kind kind, Text title) implements Toast {
        private static final long DURATION = 5000L;

        @Override
        public Visibility draw(MatrixStack matrices, ToastManager manager, long startTime) {
            RenderSystem.setShaderTexture(0, TEXTURE);
            ToastManager.drawTexture(matrices, 0, 0, 0, 0, this.getWidth(), this.getHeight());
            var text = manager.getClient().textRenderer;
            text.draw(matrices, Text.translatable(this.kind.key), 30, 7, this.kind.color);
            Text line = this.title.copy().formatted(Formatting.WHITE);
            String plain = line.getString();
            int room = this.getWidth() - 30 - 6;
            if (text.getWidth(plain) > room) {
                plain = text.trimToWidth(plain, room - text.getWidth("...")) + "...";
                line = Text.literal(plain);
            }
            text.draw(matrices, line, 30, 18, 0xFFFFFF);
            manager.getClient().getItemRenderer().renderInGui(matrices,
                    new ItemStack(this.kind == Kind.FINISHED ? Items.EMERALD : Items.WRITABLE_BOOK), 8, 8);
            return startTime >= DURATION * manager.getNotificationDisplayTimeMultiplier()
                    ? Visibility.HIDE : Visibility.SHOW;
        }
    }
}
