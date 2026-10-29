package mattonfire.dnd.classes.Blocks;

import java.util.Locale;

/**
 * What a dungeon trap does when it's set off. Every trap has one {@link TrapTriggerBlock} that holds its
 * state ({@link TrapTriggerBlockEntity}); the other blocks of a trap are linked to it.
 */
public enum TrapKind {
    /** A hidden pressure tile and a {@link DartLauncherBlock} in the wall: 3 darts, Poison I unless you save. */
    DART,
    /** A row of {@link FlameVentBlock}s round the trigger: fire damage, a DEX save halves it. */
    FLAME,
    /** A trigger tile in front of {@link CrumblingFloorBlock}s over a dripstone pit: a DEX save catches the edge. */
    PIT,
    /** Hidden under a locked chest: a failed lockpick or a smashed chest jabs you (CON save halves the poison). */
    NEEDLE,
    /**
     * A kobold tripline ({@link KoboldTriplineBlock}) across a tunnel under a 3 x 3 of {@link LooseCeilingBlock}: a
     * rumble, then rocks fall on everyone under it (DEX save: half, no Prone), leaving rubble on the floor.
     */
    ROCKFALL,
    /** A strip of {@link SpikeTrapBlock}s that look like the floor: iron spikes shoot up (DEX save: half, no Slowness). */
    SPIKES;

    public String id() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "trap.dndclasses." + this.id();
    }

    public static TrapKind byId(String id) {
        for (TrapKind kind : values()) {
            if (kind.id().equals(id)) {
                return kind;
            }
        }
        return DART;
    }

    /** The trap DC for a challenge tier: 12 / 13 / 15 / 17 (as for locks). */
    public static int dc(int tier) {
        return switch (Math.max(1, Math.min(4, tier))) {
            case 1 -> 12;
            case 2 -> 13;
            case 3 -> 15;
            default -> 17;
        };
    }
}
