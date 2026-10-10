package mattonfire.dnd.quest;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;

/**
 * One started quest, shared by its participants (a party that accepted it together). Progress from
 * any participant counts for everyone.
 */
public final class QuestInstance {
    private final int id;
    private final Identifier quest;
    private int stage;
    private int[] progress;
    private final Set<String> flags;
    private final LinkedHashSet<UUID> participants;
    private final long startedDay;

    QuestInstance(int id, Identifier quest, int stage, int[] progress, Set<String> flags, LinkedHashSet<UUID> participants,
                  long startedDay) {
        this.id = id;
        this.quest = quest;
        this.stage = stage;
        this.progress = progress;
        this.flags = flags;
        this.participants = participants;
        this.startedDay = startedDay;
    }

    public int id() {
        return this.id;
    }

    public Identifier quest() {
        return this.quest;
    }

    /** Current stage, 0-based. */
    public int stage() {
        return this.stage;
    }

    public Set<String> flags() {
        return this.flags;
    }

    public Set<UUID> participants() {
        return this.participants;
    }

    public long startedDay() {
        return this.startedDay;
    }

    /** Progress of objective {@code index} in the current stage. */
    public int progress(int index) {
        return index < this.progress.length ? this.progress[index] : 0;
    }

    void setProgress(int index, int value) {
        if (index >= this.progress.length) {
            this.progress = Arrays.copyOf(this.progress, index + 1);
        }
        this.progress[index] = value;
    }

    /** Moves to {@code stage} with no progress. */
    void setStage(int stage, int objectives) {
        this.stage = stage;
        this.progress = new int[objectives];
    }

    /** A copy for a participant who leaves the party: same quest, stage, progress and flags. */
    QuestInstance fork(int newId, UUID participant) {
        LinkedHashSet<UUID> only = new LinkedHashSet<>();
        only.add(participant);
        return new QuestInstance(newId, this.quest, this.stage, this.progress.clone(), new LinkedHashSet<>(this.flags),
                only, this.startedDay);
    }

    NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("Id", this.id);
        nbt.putString("Quest", this.quest.toString());
        nbt.putInt("Stage", this.stage);
        nbt.putIntArray("Progress", this.progress);
        NbtList flags = new NbtList();
        this.flags.forEach(flag -> flags.add(NbtString.of(flag)));
        nbt.put("Flags", flags);
        NbtList participants = new NbtList();
        this.participants.forEach(uuid -> participants.add(NbtHelper.fromUuid(uuid)));
        nbt.put("Participants", participants);
        nbt.putLong("Started", this.startedDay);
        return nbt;
    }

    static QuestInstance fromNbt(NbtCompound nbt) {
        Set<String> flags = new LinkedHashSet<>();
        for (NbtElement flag : nbt.getList("Flags", NbtElement.STRING_TYPE)) {
            flags.add(flag.asString());
        }
        LinkedHashSet<UUID> participants = new LinkedHashSet<>();
        for (NbtElement uuid : nbt.getList("Participants", NbtElement.INT_ARRAY_TYPE)) {
            participants.add(NbtHelper.toUuid(uuid));
        }
        return new QuestInstance(nbt.getInt("Id"), new Identifier(nbt.getString("Quest")), nbt.getInt("Stage"),
                nbt.getIntArray("Progress"), flags, participants, nbt.getLong("Started"));
    }
}
