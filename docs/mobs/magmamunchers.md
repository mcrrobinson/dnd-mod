# Magmamunchers
Big, slow lava beasts of the Nether. They leave you alone unless you start it, but a herd that's been provoked hits hard. A rare, hostile [Magmamuncher Alpha](../bosses/magmamuncher-alpha.md) is a boss.

![A Magmamuncher](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-flesh-out/magmamuncher.png)

## How it works

| Stat | Value |
|-|-|
| Health | 60 |
| Attack damage | 6 |
| Speed | 0.2 |
| XP | 10 |

Fire and lava don't hurt it, and it walks straight across lava rather than going round.

A wild Magmamuncher wanders and never starts a fight. Hit one and it fights back, and so do the other Magmamunchers nearby. Wild ones despawn like other Nether mobs.

It drops 0-2 magma cream (up to 1 more per Looting level) and 1-3 basalt.

### Taming
There's no food that tames a Magmamuncher, but the [Bard](../classes/bard.md)'s power-up tames every untamed tameable mob within 10 blocks, and Magmamunchers count. A tamed one follows you, attacks what you attack and defends you, and never despawns. You can't make it sit.

### Tips
- Check what's around before you swing at one in the Nether: one hit pulls in the whole group.
- It's slow at 0.2 speed, so you can outrun it on foot.

## Where to find it
- Basalt Deltas and Nether Wastes (monster group, weight 8, groups of 1-3).
- It spawns on any solid ground or magma, in any light, but never on top of the Nether roof.
- An enraged Magmamuncher Alpha calls two to its side.
- Spawn egg in the mod's creative tab.

## Commands
- `/summon dndclasses:magmamuncher`

## Known limitations
- Apart from the Bard's power-up there's no way to tame one.

## For developers
- `entity/MagmamuncherEntity`: a `TameableEntity` with sit, follow-owner and defend-owner goals, and group revenge (`RevengeGoal.setGroupRevenge()`). Spawn rule: `canSpawnInNether`.
- Spawns: `world/gen/ModSpawns`. Loot: `loot_tables/entities/magmamuncher.json`.
- Devscripts: `magmamuncher-nether-spawns.txt`, `magmamuncher-fire-immune.txt`.
