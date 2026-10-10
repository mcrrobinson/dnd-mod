package mattonfire.dnd.dungeon;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Obstacles.ObstacleType;
import mattonfire.dnd.classes.Obstacles.ObstacleTypes;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

/**
 * What bars a dungeon's class-check gate (the gate room on the main path, or a side vault's doorway).
 * <ul>
 * <li>{@link #ARCANE_SEAL}: a Lesser Arcane Seal (Area 1 obstacle). Wizards and Warlocks dispel it; anyone
 * can mine through it and take the backlash.</li>
 * <li>{@link #GREATER_SEAL}: a Greater Arcane Seal. Wizards only, no bypass: side vaults only.</li>
 * <li>{@link #RUBBLE}: a cave-in of cobblestone and gravel anyone can dig through (Barbarians and Fighters
 * will feel at home; there's no check yet).</li>
 * <li>{@link #LOCKED_DOOR}: an iron door. Its lever "key" is in a locked chest by the door: a Rogue
 * picks the lock, anyone else smashes the chest (and may break a lever or two).</li>
 * </ul>
 * The main path never depends on one class: a main-path gate must let at least two classes through or
 * have a bypass anyone can use ({@link #mainPathSafe()}, enforced by {@link #pick}).
 */
public enum GateKind {
    ARCANE_SEAL(true, false, DndCharacter.WIZARD, DndCharacter.WARLOCK),
    GREATER_SEAL(false, true, DndCharacter.WIZARD),
    RUBBLE(true, false),
    LOCKED_DOOR(true, false, DndCharacter.ROGUE);

    /** Something anyone can do to get past (mine, dig, smash the key chest). */
    private final boolean bypass;
    /** Only fit for an optional side room. */
    private final boolean strict;
    /** Classes with a dedicated way through; empty means every class alike. */
    private final List<DndCharacter> classes;

    GateKind(boolean bypass, boolean strict, DndCharacter... classes) {
        this.bypass = bypass;
        this.strict = strict;
        this.classes = List.of(classes);
    }

    public boolean bypass() {
        return this.bypass;
    }

    public List<DndCharacter> classes() {
        return this.classes;
    }

    /** At least two classes get through, or anyone can force it. */
    public boolean mainPathSafe() {
        return !this.strict && (this.bypass || this.classes.size() >= 2 || this.classes.isEmpty());
    }

    /** The obstacle block it's built from, or null for the vanilla-block fallbacks. */
    @Nullable
    public ObstacleType obstacle() {
        return switch (this) {
            case ARCANE_SEAL -> ObstacleTypes.LESSER_ARCANE_SEAL;
            case GREATER_SEAL -> ObstacleTypes.GREATER_ARCANE_SEAL;
            default -> null;
        };
    }

    public String id() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public static GateKind byName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return RUBBLE;
        }
    }

    /**
     * The gate for a room: a strict one for a side vault, otherwise one the main path can rely on.
     * Throws if no kind fits, so a broken table fails loudly in testing rather than walling a party in.
     */
    public static GateKind pick(RoomRole role, Random random) {
        List<GateKind> options = Arrays.stream(values())
                .filter(kind -> role == RoomRole.SIDE_VAULT ? kind.strict : kind.mainPathSafe())
                .toList();
        if (options.isEmpty()) {
            throw new IllegalStateException("No dungeon gate kind fits " + role);
        }
        return options.get(random.nextInt(options.size()));
    }
}
