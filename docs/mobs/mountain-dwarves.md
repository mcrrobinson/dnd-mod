# Mountain Dwarves
Short, broad, bearded dwarves in mail shirts who guard the [dwarven fortresses](../structures/dwarven-fortresses.md). They're neutral, like iron golems: they leave you alone and fight the monsters in their halls, until you hit one or touch their gold.

![A Mountain Dwarf](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-flesh-out/mountain_dwarf.png)

## How it works

| Stat | Value |
|-|-|
| Health | 32 (64 for the Dwarf King) |
| Armor | 4, plus whatever it wears |
| Knockback resistance | 50% |
| Speed | 0.27 |
| Base attack damage | 1, plus its weapon |
| XP | 8 |

Each dwarf gets one of six looks and a dwarven name, like "Gimli Stonebeard". It never despawns and never picks up items. It can open doors.

| Weapon | Chance |
|-|-|
| Iron axe | 50% |
| Golden axe | 20% |
| Iron pickaxe | 15% |
| Diamond axe | 10% |
| Netherite axe | 5% |

It wears an iron helmet 45% of the time, a golden one 30%, a chainmail one 10%, and nothing on its head otherwise. One in five also wears an iron chestplate.

### Who it fights
A dwarf stays within 32 blocks of the spot it spawned and hunts monsters in that area, except creepers. A sleeping [Mimic](mimic.md) passes for a chest, so dwarves ignore it until it wakes.

It turns on a player for three reasons:

- **You hit it.** It stays angry for 30 to 50 seconds. Its kin nearby join in, and for 10 seconds after you hit a dwarf any other dwarf counts you as an enemy.
- **You touch the hoard.** Open or break a chest (trapped ones too) or a barrel, or break a block of gold, where a dwarf within 16 blocks can see you, and every dwarf that saw it turns on you with a growl. Sneaking with an item in your hand places the item instead of opening the chest, so it doesn't count. Creative and spectator players are never noticed. Players Unfriendly or worse with the dwarves are noticed from 24 blocks.
- **You're Hostile with the dwarves** ([Factions](../systems/factions.md)): every dwarf attacks you on sight.

Help drive off a [goblin raid](../systems/goblin-raids.md) on the fortress and every dwarf within 96 blocks forgives you.

### Bartering
Right-click a dwarf with a gold ingot and it takes the ingot and hands you something from the mountain's depths. It shakes its head instead if it's fighting, angry with you, or traded in the last 2 seconds.

Your standing with the dwarves changes bartering:

| Tier | Barter |
|-|-|
| Hostile | Never (it attacks you) |
| Unfriendly | Refuses half the time and the gold stays with you |
| Neutral | As below |
| Friendly | 15% chance of a second roll |
| Honored | 30% second roll, from the honored table |
| Exalted | 50% second roll, from the honored table |

The honored table (`gameplay/dwarf_barter_honored`) is the table below with diamonds at weight 6, plus an iron chestplate enchanted at levels 10-25 (weight 3) and a common magic iron pickaxe (3) or iron helmet (2).

| Item | Count | Weight |
|-|-|-|
| Raw iron | 2-5 | 30 |
| Raw copper | 4-10 | 25 |
| Coal | 4-12 | 25 |
| Lapis lazuli | 3-8 | 15 |
| Redstone | 4-10 | 15 |
| Amethyst shard | 2-6 | 12 |
| Polished deepslate | 8-16 | 10 |
| Lantern | 1-3 | 8 |
| Emerald | 1-2 | 6 |
| Iron pickaxe, randomly enchanted | 1 | 4 |
| Iron axe, randomly enchanted | 1 | 3 |
| Diamond | 1 | 2 |

The weights add up to 155, so a diamond comes up about once in 78 trades.

### The Dwarf King
Each fortress has a king on the throne. He's named "King" plus a dwarven name, has 64 health, and wears a golden crown and golden chestplate with a netherite axe in hand. The crown always drops when he dies.

### Drops
0-2 raw iron (up to 1 more per Looting level) and 1-5 gold nuggets (up to 2 more per Looting level). Killed by a player, there's also an 8% chance (+2% per Looting level) of a gem: an emerald three times out of four, otherwise a diamond.

### Tips
- Want the treasury? Loot it when no dwarf can see you, or be ready to fight the whole hall.
- Bring gold ingots. Bartering is the safe way to get lapis, redstone and the odd diamond out of a fortress.
- If you've angered a fortress, defending it from a goblin raid clears the grudge.

## Where to find it
Mountain Dwarves only spawn inside dwarven fortresses, including the dark halls, and stop once there are 24 within 64 blocks. The king is placed when the fortress generates. There's also a spawn egg in the mod's creative tab.

## Commands
- `/summon dndclasses:mountain_dwarf`

## For developers
- `entity/MountainDwarfEntity`: stats, equipment (`equip`), names, the king (`crown`), bartering and anger. It's `Angerable`; `setTarget` refuses players it has no grudge against, since the mod adds player-targeting goals to every mob.
- `entity/DwarfGrudges`: the chest, barrel and gold block hooks (`witness`) and `forgive`, which `entity/raid/GoblinRaid` calls when a fortress raid is won.
- Spawn cap: `ModSpawns.canDwarfSpawn`. Loot: `loot_tables/entities/mountain_dwarf.json`, `loot_tables/gameplay/dwarf_barter.json` and `dwarf_barter_honored.json`.
- Reputation: `shouldAngerAt` is also true for players Hostile with the dwarves; barter refusal, second roll and table, and the witness range come from `faction/TierEffects`.
- Devscripts: `mountain-dwarf-behaviour.txt`, `mountain-dwarf-grudge.txt`, `mountain-dwarf-lineup.txt`.
