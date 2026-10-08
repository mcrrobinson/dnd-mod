# Goblins
Heavily built goblins with clubs. They infest Nether Fortresses, garrison goblin camps and march on settlements in raids, led by the [Goblin Warlord](../bosses/goblin-warlord.md).

![A Goblin Warrior](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-flesh-out/goblin_warrior.png)

## How it works
The **Goblin Warrior** is slow but tough and hits hard.

| Stat | Value |
|-|-|
| Health | 50 |
| Armor | 6 |
| Knockback resistance | 60% |
| Attack damage | 9 |
| Speed | 0.24 |
| Follow range | 24 blocks |
| XP | 10 |

It's immune to fire and lava. It hunts players and iron golems, and it fights back against zombified piglins that hit it.

Its swing is telegraphed. When you're in reach it raises its club, and the hit lands 10 ticks (half a second) later, but only if you're still in reach. After that it can't swing again for 2 seconds.

It drops 0-3 gold nuggets and 0-1 leather, each with up to 1 more per Looting level.

### Tips
- Watch for the raised club and step back. If you're out of reach when the half second is up, the swing misses.
- Hit it in the 2 seconds after a swing, then back off before the next one.
- Fire is no use against it, so leave Fire Aspect and flame staffs at home.

## Where to find it
- **Nether Fortresses:** added to the fortress spawn pool with blazes and wither skeletons (weight 6, groups of 1-3, in the dark). See [Nether Fortress additions](../structures/nether-fortresses.md).
- **[Goblin Camps](../structures/goblin-camps.md)** in Overworld forests and plains.
- **[Goblin raids](../systems/goblin-raids.md)** on hobbit villages and dwarven fortresses.
- The Goblin Warlord summons them in waves.
- Spawn egg in the mod's creative tab.

## Commands
- `/summon dndclasses:goblin_warrior`

## For developers
- `entity/GoblinWarriorEntity`: stats and the slow swing (`SlowMeleeAttackGoal`, `ATTACK_INTERVAL` 40 ticks, `WIND_UP` 10 ticks). GeckoLib, with an `attack_controller` for one-shot animations that the Warlord reuses.
- Fortress spawns: `classes/mixin/SpawnHelperMixin`.
- Loot: `loot_tables/entities/goblin_warrior.json`.
- Devscript: `goblin-warrior.txt`.
