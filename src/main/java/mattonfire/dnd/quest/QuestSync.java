package mattonfire.dnd.quest;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Party.Party;
import mattonfire.dnd.classes.Party.PartyManager;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * S2C {@code dndclasses:quest_sync}: everything the Journal and tracker show, with the text already
 * resolved on the server. Sent on join and (at most once a second) after a change.
 *
 * <pre>
 * varint n, then per active quest:
 *   varint instance, id quest, text title, id chain, string giver ("" for none), bool tracked,
 *   varint stage (0-based), varint stages, text stage title, text stage text,
 *   varint objectives, per objective: text description, varint progress, varint count
 *   varint participants, per participant: string name, bool party leader
 * varint n, per finished quest: id quest, text title, id chain, bool reward waiting
 * </pre>
 */
public final class QuestSync {
    public static final Identifier PACKET_ID = new Identifier(DnDClasses.MOD_ID, "quest_sync");

    private QuestSync() {
    }

    static void send(ServerPlayerEntity player, QuestManager manager) {
        MinecraftServer server = player.getServer();
        UUID uuid = player.getUuid();
        PacketByteBuf buf = PacketByteBufs.create();
        List<QuestInstance> active = manager.instancesOf(uuid).stream()
                .filter(instance -> Quests.get(instance.quest()) != null).toList();
        int tracked = manager.tracked(uuid);
        PartyManager parties = PartyManager.get(server);
        buf.writeVarInt(active.size());
        for (QuestInstance instance : active) {
            QuestDefinition quest = Quests.get(instance.quest());
            QuestStage stage = quest.stage(instance.stage());
            buf.writeVarInt(instance.id());
            buf.writeIdentifier(quest.id());
            buf.writeText(quest.title());
            buf.writeIdentifier(quest.chain());
            buf.writeString(quest.giver() == null ? "" : quest.giver());
            buf.writeBoolean(instance.id() == tracked);
            buf.writeVarInt(instance.stage());
            buf.writeVarInt(quest.stages().size());
            buf.writeText(stage.title() != null ? stage.title() : quest.title());
            buf.writeText(stage.text());
            buf.writeVarInt(stage.objectives().size());
            for (int i = 0; i < stage.objectives().size(); i++) {
                QuestObjective objective = stage.objectives().get(i);
                buf.writeText(objective.description());
                buf.writeVarInt(Math.min(instance.progress(i), objective.count()));
                buf.writeVarInt(objective.count());
            }
            buf.writeVarInt(instance.participants().size());
            for (UUID participant : instance.participants()) {
                Party party = parties.getParty(participant);
                buf.writeString(manager.name(server, participant));
                buf.writeBoolean(party != null && party.isLeader(participant));
            }
        }
        Map<Identifier, Long> finished = manager.finished(uuid);
        List<Identifier> unclaimed = manager.unclaimed(uuid);
        buf.writeVarInt(finished.size());
        for (Identifier questId : finished.keySet()) {
            QuestDefinition quest = Quests.get(questId);
            buf.writeIdentifier(questId);
            buf.writeText(quest != null ? quest.title() : Text.literal(questId.toString()));
            buf.writeIdentifier(quest != null ? quest.chain() : questId);
            buf.writeBoolean(unclaimed.contains(questId));
        }
        ServerPlayNetworking.send(player, PACKET_ID, buf);
    }
}
