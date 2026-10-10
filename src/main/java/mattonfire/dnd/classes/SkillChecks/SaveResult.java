package mattonfire.dnd.classes.SkillChecks;

import org.jetbrains.annotations.Nullable;

import net.minecraft.text.Text;

/**
 * What came of a {@link SavingThrow}. Use it to scale what the attack does:
 *
 * <pre>{@code
 * float dmg = result.damage(4.0F);          // half on a success if the save halvesDamage()
 * int burn = result.duration(100);          // half the ticks on a success
 * if (result.failed()) { applyFreeze(); }   // effects a success avoids entirely
 * }</pre>
 *
 * A result from the exposure cache ({@link #fresh()} false) is the same outcome as the first roll of that
 * exposure, so every hit of one breath agrees.
 */
public final class SaveResult {
    private final D20.Roll roll;
    private final boolean halvesDamage;
    private final boolean fresh;
    private final float successDamage;
    private final float failureDamage;
    /** The feature that changed the damage multipliers (Evasion), shown on the save lane. */
    @Nullable
    private final Text note;

    SaveResult(D20.Roll roll, boolean halvesDamage, boolean fresh) {
        this(roll, halvesDamage, fresh, 0.5f, 1.0f, null);
    }

    private SaveResult(D20.Roll roll, boolean halvesDamage, boolean fresh, float successDamage, float failureDamage,
            @Nullable Text note) {
        this.roll = roll;
        this.halvesDamage = halvesDamage;
        this.fresh = fresh;
        this.successDamage = successDamage;
        this.failureDamage = failureDamage;
        this.note = note;
    }

    /** The d20 behind it. */
    public D20.Roll roll() {
        return roll;
    }

    public D20.Outcome outcome() {
        return roll.outcome();
    }

    /** SUCCESS or CRITICAL (natural 20). */
    public boolean succeeded() {
        return roll.outcome().succeeded();
    }

    public boolean failed() {
        return !succeeded();
    }

    /** False when this came from the exposure cache rather than a new roll. */
    public boolean fresh() {
        return fresh;
    }

    public boolean halvesDamage() {
        return halvesDamage;
    }

    /**
     * Damage after the save: for a {@code halvesDamage()} save, half on a success and full on a failure (or what
     * {@link #withDamageMultipliers} set); other saves don't change damage.
     */
    public float damage(float full) {
        if (!halvesDamage) {
            return full;
        }
        return full * (succeeded() ? successDamage : failureDamage);
    }

    /** An effect's duration after the save: half the ticks on a success, all of them on a failure. */
    public int duration(int ticks) {
        return succeeded() ? ticks / 2 : ticks;
    }

    /**
     * The same result with other damage multipliers, for features that change what a half-damage save does
     * (Evasion: 0 on a success, 0.5 on a failure).
     */
    public SaveResult withDamageMultipliers(float onSuccess, float onFailure) {
        return new SaveResult(roll, halvesDamage, fresh, onSuccess, onFailure, note);
    }

    /**
     * As {@link #withDamageMultipliers(float, float)}, naming the feature responsible: the save lane then says
     * "no damage (Evasion)" instead of the attack's own success text.
     */
    public SaveResult withDamageMultipliers(float onSuccess, float onFailure, Text note) {
        return new SaveResult(roll, halvesDamage, fresh, onSuccess, onFailure, note);
    }

    /** The feature that changed this save's damage, or null. */
    @Nullable
    public Text note() {
        return note;
    }

    SaveResult cached() {
        return new SaveResult(roll, halvesDamage, false, successDamage, failureDamage, note);
    }
}
