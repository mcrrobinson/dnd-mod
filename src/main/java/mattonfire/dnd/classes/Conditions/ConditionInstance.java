package mattonfire.dnd.classes.Conditions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.Abilities.Ability;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.Text;
import net.minecraft.world.World;

/** Immutable snapshot of one instance; a refresh replaces it. Times are {@code world.getTime()}. */
public record ConditionInstance(Condition condition, String key, @Nullable UUID source, @Nullable Text sourceName,
        long appliedAt, long endsAt /* world time, or -1 */, @Nullable RepeatSave save,
        boolean endsWithSource, boolean transientInstance) {

    /** Ticks left, or -1 when indefinite. */
    public int remainingTicks(World world) {
        if (endsAt < 0) {
            return -1;
        }
        return (int) Math.max(0, endsAt - world.getTime());
    }

    public boolean indefinite() {
        return endsAt < 0;
    }

    /** Whether this ends later than {@code other} (indefinite counts as latest). */
    public boolean endsAfter(ConditionInstance other) {
        if (indefinite()) {
            return !other.indefinite();
        }
        return !other.indefinite() && endsAt > other.endsAt;
    }

    ConditionInstance withSave(@Nullable RepeatSave newSave) {
        return new ConditionInstance(condition, key, source, sourceName, appliedAt, endsAt, newSave, endsWithSource,
                transientInstance);
    }

    /** Same source and key as {@code other}. */
    boolean sameSlot(Condition c, @Nullable UUID otherSource, String otherKey) {
        return condition == c && key.equals(otherKey)
                && (source == null ? otherSource == null : source.equals(otherSource));
    }

    NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("Id", condition.id());
        nbt.putString("Key", key);
        if (source != null) {
            nbt.put("Source", NbtHelper.fromUuid(source));
        }
        if (sourceName != null) {
            nbt.putString("SourceName", Text.Serializer.toJson(sourceName));
        }
        nbt.putLong("Applied", appliedAt);
        nbt.putLong("Ends", endsAt);
        if (save != null) {
            NbtCompound s = new NbtCompound();
            s.putString("Ability", save.ability().shortKey());
            s.putInt("Dc", save.dc());
            s.putInt("Every", save.every());
            s.putLong("Next", save.nextAt());
            s.putString("Label", save.labelKey());
            NbtList tags = new NbtList();
            for (String tag : save.tags()) {
                tags.add(NbtString.of(tag));
            }
            s.put("Tags", tags);
            if (save.escalateTo() != null) {
                s.putString("Escalate", save.escalateTo().id());
                s.putInt("EscalateTicks", save.escalateTicks());
            }
            nbt.put("Save", s);
        }
        if (endsWithSource) {
            nbt.putBoolean("WithSource", true);
        }
        return nbt;
    }

    @Nullable
    static ConditionInstance fromNbt(NbtCompound nbt) {
        Condition condition = Condition.byId(nbt.getString("Id"));
        if (condition == null) {
            return null;
        }
        UUID source = nbt.containsUuid("Source") ? nbt.getUuid("Source") : null;
        Text sourceName = null;
        if (nbt.contains("SourceName", NbtElement.STRING_TYPE)) {
            try {
                sourceName = Text.Serializer.fromJson(nbt.getString("SourceName"));
            } catch (RuntimeException e) {
                sourceName = null;
            }
        }
        RepeatSave save = null;
        if (nbt.contains("Save", NbtElement.COMPOUND_TYPE)) {
            NbtCompound s = nbt.getCompound("Save");
            Ability ability = Ability.byId(s.getString("Ability"));
            if (ability != null) {
                List<String> tags = new ArrayList<>();
                NbtList list = s.getList("Tags", NbtElement.STRING_TYPE);
                for (int i = 0; i < list.size(); i++) {
                    tags.add(list.getString(i));
                }
                save = new RepeatSave(ability, s.getInt("Dc"), Math.max(1, s.getInt("Every")), s.getString("Label"),
                        List.copyOf(tags), Condition.byId(s.getString("Escalate")), s.getInt("EscalateTicks"),
                        s.getLong("Next"));
            }
        }
        String key = nbt.getString("Key");
        return new ConditionInstance(condition, key, source, sourceName, nbt.getLong("Applied"), nbt.getLong("Ends"),
                save, nbt.getBoolean("WithSource"), false);
    }
}
