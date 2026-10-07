# Boss framework
Shared code behind every boss's bar, music, phases and rewards. For the player's view see [Boss fights](../bosses/boss-fights.md).

## How it works
A boss implements `Boss` and owns a `BossFight` component, built in its constructor:

```java
this.bossFight = new BossFight(this, BossBar.Color.YELLOW, BossBar.Style.NOTCHED_10)
        .range(48.0)                        // bar visible within 48 blocks (default 64)
        .music(ModSounds.MUSIC_DRAGON_FIGHT) // looping fight track for players seeing the bar
        .phase(0.5F, BossBar.Color.RED, this::enrage) // entered once when health < 50%
        .xp(60);                            // optional: .lootTable(id), .advancement(id), .activeWhen(cond)
```

Forward these calls from the mob to the fight: `tick()`, `onStoppedTrackingBy(player)`, `remove(reason)` → `onRemoved()`, `onDeath(source)`, `writeCustomDataToNbt`/`readCustomDataFromNbt` → `writeNbt`/`readNbt`, and `getLootTable()` if you use `.lootTable`.

- The fight counts while the boss targets a player or was hurt by one, and the `activeWhen` condition holds (e.g. not tamed).
- Phases run in order, are saved as `BossPhase` NBT, and don't re-run their callback on load. Put lasting phase effects in persistent attribute modifiers.
- Music goes to clients through the `dndclasses:boss_music` packet (`BossMusic`), and `EventMusic` plays it.
- Advancements should use a `minecraft:impossible` criterion. They go to the player vanilla credits with the kill.

## For developers
- `entity/boss/Boss`, `BossFight`, `BossMusic`. Examples: `GoblinWarlordEntity`, `MagmamuncherAlphaEntity`, `WyvernEntity`. Devscript: `boss-framework.txt`.
