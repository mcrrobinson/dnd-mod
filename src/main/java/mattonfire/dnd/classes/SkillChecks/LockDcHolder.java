package mattonfire.dnd.classes.SkillChecks;

/**
 * A container block entity's lock DC override (chests, barrels...; added by {@code LockableContainerLockDcMixin}).
 * Saved in the block entity's NBT as {@value #NBT_KEY}, so {@code /data get block} shows it and
 * {@code /data merge block ... {DndLockDc:18}} sets it. 0 means no override: {@link Lockpicking#dcFor} decides.
 */
public interface LockDcHolder {
    String NBT_KEY = "DndLockDc";

    int dndclasses$getLockDc();

    void dndclasses$setLockDc(int dc);
}
