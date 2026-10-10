package mattonfire.dnd.quest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * One step of a quest: 1-4 objectives, all required. When they're all done, {@code onComplete}
 * runs and the quest moves to the next stage (or finishes after the last).
 *
 * @param title short name ("Trouble on the Road"), or null to use the quest's title
 * @param text  what the Journal says about this step
 */
public record QuestStage(@Nullable Text title, Text text, List<QuestObjective> objectives, List<QuestAction> onComplete) {
    public static final int MAX_OBJECTIVES = 4;

    static QuestStage parse(JsonElement element) {
        JsonObject json = QuestJson.object(element, "stage");
        List<QuestObjective> objectives = QuestJson.list(json, "objectives", QuestObjective::parse);
        if (objectives.isEmpty() || objectives.size() > MAX_OBJECTIVES) {
            throw new IllegalArgumentException("a stage needs 1 to " + MAX_OBJECTIVES + " objectives");
        }
        return new QuestStage(QuestJson.optText(json, "title"), QuestJson.text(json.get("text"), "text"),
                List.copyOf(objectives), List.copyOf(QuestJson.list(json, "on_complete", QuestAction::parse)));
    }
}
