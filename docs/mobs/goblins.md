# Goblins
Heavily built goblins that infest Nether Fortresses, led by a [Goblin Warlord](../bosses/goblin-warlord.md).

## How it works
**Goblin Warrior**
- 50 HP, 6 armor, 60% knockback resistance, 9 attack damage, 0.24 speed, follow range 24. Fire immune.
- Slow, telegraphed swing: it raises its club, and the hit lands 10 ticks later only if you're still in reach. It swings at most once every 2 seconds.
- Attacks players and iron golems, and fights back against zombified piglins. Drops 10 XP and no items.

## Where to find it / How to get it
- Spawns in Nether Fortresses alongside blazes and wither skeletons (weight 6, groups of 1-3, in the dark).
- The Goblin Warlord also summons them in waves.

## Commands
- `/summon dndclasses:goblin_warrior`

## For developers
- `entity/GoblinWarriorEntity` (GeckoLib, `attack_controller` for one-shot animations). Fortress spawns: `mixin/SpawnHelperMixin`.
- Devscript: `goblin-warrior.txt`.
