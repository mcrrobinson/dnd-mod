package mattonfire.dnd.quest.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.quest.QuestSync;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * The local player's quests, as last sent by the server ({@code dndclasses:quest_sync}). The
 * Journal screen and tracker HUD read this.
 */
public final class ClientQuests {
    public record Objective(Text description, int progress, int count) {
        public boolean done() {
            return this.progress >= this.count;
        }
    }

    public record Participant(String name, boolean leader) {
    }

    public record Active(int instance, Identifier quest, Text title, Identifier chain, String giver, boolean tracked,
                         int stage, int stages, Text stageTitle, Text stageText, List<Objective> objectives,
                         List<Participant> participants) {
    }

    public record Finished(Identifier quest, Text title, Identifier chain, boolean rewardWaiting) {
    }

    private static volatile List<Active> active = List.of();
    private static volatile List<Finished> finished = List.of();

    private ClientQuests() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(QuestSync.PACKET_ID, (client, handler, buf, sender) -> {
            int count = buf.readVarInt();
            List<Active> readActive = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                int instance = buf.readVarInt();
                Identifier quest = buf.readIdentifier();
                Text title = buf.readText();
                Identifier chain = buf.readIdentifier();
                String giver = buf.readString();
                boolean tracked = buf.readBoolean();
                int stage = buf.readVarInt();
                int stages = buf.readVarInt();
                Text stageTitle = buf.readText();
                Text stageText = buf.readText();
                int objectiveCount = buf.readVarInt();
                List<Objective> objectives = new ArrayList<>(objectiveCount);
                for (int j = 0; j < objectiveCount; j++) {
                    objectives.add(new Objective(buf.readText(), buf.readVarInt(), buf.readVarInt()));
                }
                int participantCount = buf.readVarInt();
                List<Participant> participants = new ArrayList<>(participantCount);
                for (int j = 0; j < participantCount; j++) {
                    participants.add(new Participant(buf.readString(), buf.readBoolean()));
                }
                readActive.add(new Active(instance, quest, title, chain, giver, tracked, stage, stages, stageTitle,
                        stageText, List.copyOf(objectives), List.copyOf(participants)));
            }
            int finishedCount = buf.readVarInt();
            List<Finished> readFinished = new ArrayList<>(finishedCount);
            for (int i = 0; i < finishedCount; i++) {
                readFinished.add(new Finished(buf.readIdentifier(), buf.readText(), buf.readIdentifier(), buf.readBoolean()));
            }
            List<Active> newActive = Collections.unmodifiableList(readActive);
            List<Finished> newFinished = Collections.unmodifiableList(readFinished);
            client.execute(() -> {
                active = newActive;
                finished = newFinished;
                DnDClasses.LOGGER.info("[Quests] client sync: active {}, finished {}", newActive.stream()
                        .map(quest -> quest.quest() + " stage " + (quest.stage() + 1) + " " + quest.objectives().stream()
                                .map(objective -> objective.progress() + "/" + objective.count()).toList()
                                + " with " + quest.participants().stream().map(Participant::name).toList())
                        .toList(), newFinished.stream().map(Finished::quest).toList());
            });
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            active = List.of();
            finished = List.of();
        });
    }

    public static List<Active> active() {
        return active;
    }

    public static List<Finished> finished() {
        return finished;
    }
}
