package mattonfire.dnd.classes.Rest;

import mattonfire.dnd.classes.IEntityDataSaver;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * One player's rest data, in the persistent compound under {@value #KEY}.
 * {@code ClassLifecycle} copies it to the new entity on death, so dying refills
 * nothing. Read it with {@link #get}, change the fields, then {@link #save}.
 */
public final class RestState {
    public static final String KEY = "dndRest";

    /** Charges left, 0..{@link Charges#max}. */
    public int charges;
    /** Extra charges above the max (Well Rested), spent first. */
    public int tempCharges;
    public int hitDiceSpent;
    public int shortRestsSinceLong;
    /** Overworld time ({@code World.getTime()}) the last short rest ended; 0 for never. */
    public long lastShortRestEnd;
    /** In-game day ({@code timeOfDay / 24000}) of the last long rest; -1 for never. */
    public long lastLongRestDay = -1;
    /** Ticks alive towards the next trickle charge. */
    public int trickleTicks;
    /** Wizard Arcane Recovery: the first short rest after a long rest gives 2 charges. */
    public boolean wizardRecoveryUsed;
    /** Overworld time the next Last Stand is allowed (death saves). */
    public long lastStandReadyAt;

    /** The player's state. A player who has never had any starts with full charges. */
    public static RestState get(ServerPlayerEntity player) {
        NbtCompound data = ((IEntityDataSaver) player).getPersistentData();
        RestState state = new RestState();
        if (!data.contains(KEY)) {
            state.charges = Charges.max(player);
            return state;
        }
        NbtCompound nbt = data.getCompound(KEY);
        state.charges = nbt.getInt("charges");
        state.tempCharges = nbt.getInt("tempCharges");
        state.hitDiceSpent = nbt.getInt("hitDiceSpent");
        state.shortRestsSinceLong = nbt.getInt("shortRestsSinceLong");
        state.lastShortRestEnd = nbt.getLong("lastShortRestEnd");
        state.lastLongRestDay = nbt.contains("lastLongRestDay") ? nbt.getLong("lastLongRestDay") : -1;
        state.trickleTicks = nbt.getInt("trickleTicks");
        state.wizardRecoveryUsed = nbt.getBoolean("wizardRecoveryUsed");
        state.lastStandReadyAt = nbt.getLong("lastStandReadyAt");
        return state;
    }

    public void save(ServerPlayerEntity player) {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("charges", charges);
        nbt.putInt("tempCharges", tempCharges);
        nbt.putInt("hitDiceSpent", hitDiceSpent);
        nbt.putInt("shortRestsSinceLong", shortRestsSinceLong);
        nbt.putLong("lastShortRestEnd", lastShortRestEnd);
        nbt.putLong("lastLongRestDay", lastLongRestDay);
        nbt.putInt("trickleTicks", trickleTicks);
        nbt.putBoolean("wizardRecoveryUsed", wizardRecoveryUsed);
        nbt.putLong("lastStandReadyAt", lastStandReadyAt);
        ((IEntityDataSaver) player).getPersistentData().put(KEY, nbt);
    }
}
