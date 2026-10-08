# Magmamunchers
Big, slow, fire-proof lava beasts of the Nether. A rare Alpha is a [boss](../bosses/magmamuncher-alpha.md).

## How it works
- **Magmamuncher:** 60 HP, 6 attack damage, 0.2 speed. Fire and lava immune.
- Wild ones wander and don't start fights, but hit one and it and the Magmamunchers nearby fight back. They despawn like other Nether mobs.
- Drops 0-2 magma cream (+Looting) and 1-3 basalt. 10 XP.

## Where to find it / How to get it
- Basalt Deltas and Nether Wastes (monster, weight 8, groups of 1-3), on solid ground or magma in any light, never on the Nether roof.
- An enraged Magmamuncher Alpha calls two to its side.

## Commands
- Spawn egg: in the mod's creative tab.
- `/summon dndclasses:magmamuncher`

## Known limitations
- It's built on a tameable base (sit, follow owner, defend owner), but there's no way to tame one yet.

## For developers
- `entity/MagmamuncherEntity`. Spawns: `world/gen/ModSpawns`.
- Devscripts: `magmamuncher-nether-spawns.txt`, `magmamuncher-fire-immune.txt`.
