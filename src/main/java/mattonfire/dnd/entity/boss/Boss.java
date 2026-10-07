package mattonfire.dnd.entity.boss;

/**
 * A boss mob. Its {@link BossFight} runs the boss bar, fight music, health phases and kill rewards.
 *
 * <p>Bosses extend different vanilla classes, so the fight is a component rather than a base class.
 * A boss creates one in its constructor and forwards these calls to it (server side):
 * <ul>
 *   <li>{@code tick()}: {@link BossFight#tick()}</li>
 *   <li>{@code onStoppedTrackingBy(player)}: {@link BossFight#onStoppedTrackingBy}</li>
 *   <li>{@code remove(reason)}: {@link BossFight#onRemoved()}</li>
 *   <li>{@code onDeath(source)}: {@link BossFight#onDeath()}</li>
 *   <li>{@code writeCustomDataToNbt} / {@code readCustomDataFromNbt}: {@link BossFight#writeNbt} / {@link BossFight#readNbt}</li>
 *   <li>{@code getLootTable()}: {@link BossFight#getLootTable} (only needed with {@link BossFight#lootTable})</li>
 * </ul>
 */
public interface Boss {
    BossFight getBossFight();
}
