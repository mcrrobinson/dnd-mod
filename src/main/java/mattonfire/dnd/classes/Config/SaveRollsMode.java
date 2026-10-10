package mattonfire.dnd.classes.Config;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

/** How the client shows saving throws ({@code saveRolls} in the config file). */
public enum SaveRollsMode {
    /** On the big roll panel under the crosshair, when it isn't showing a check. */
    FULL,
    /** Compact rows to the right of the crosshair (default). */
    COMPACT,
    /** Not shown; natural 20s and 1s still play their sound. */
    OFF;

    /** Set at runtime (DevScript {@code saverolls}); wins over the config file until cleared. */
    @Nullable
    private static SaveRollsMode override;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Parses "full", "compact" or "off"; null if unknown. */
    @Nullable
    public static SaveRollsMode byId(@Nullable String id) {
        if (id != null) {
            for (SaveRollsMode mode : values()) {
                if (mode.id().equalsIgnoreCase(id.trim())) {
                    return mode;
                }
            }
        }
        return null;
    }

    /** The mode in effect: the runtime override, else the config file, else COMPACT. */
    public static SaveRollsMode current() {
        if (override != null) {
            return override;
        }
        SaveRollsMode fromConfig = byId(FAConfig.getValues().saveRolls());
        return fromConfig == null ? COMPACT : fromConfig;
    }

    public static void setOverride(@Nullable SaveRollsMode mode) {
        override = mode;
    }
}
