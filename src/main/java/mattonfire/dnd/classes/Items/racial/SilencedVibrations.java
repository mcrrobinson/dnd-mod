package mattonfire.dnd.classes.Items.racial;

/**
 * Implemented on every entity (by {@code ElvenChainVibrationMixin}): an entity that makes no sculk vibrations
 * until a game time, such as the drops of a block a sneaking Elf in the Elven Chain broke.
 */
public interface SilencedVibrations {
    void dnd$silenceVibrationsUntil(long gameTime);

    /** Whether {@link #dnd$silenceVibrationsUntil} still holds. */
    boolean dnd$vibrationsSilenced();
}
