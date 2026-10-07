# Mountain Dwarves
Short, broad, bearded dwarves in mail shirts that guard the [dwarven fortresses](../structures/dwarven-fortresses.md).

## How it works
- 32 HP, 4 armor, 50% knockback resistance, 0.27 speed. Often wears an iron, gold or chainmail helmet and carries an axe or pickaxe. Six looks, and each gets a dwarven name.
- **Neutral, like an iron golem:** it leaves players alone and hunts monsters (except creepers) within 32 blocks of its home.
- **Grudges:** hit a dwarf and it stays angry for 30-50 seconds. Any of its kin that see it count you as an enemy for 10 seconds.
- **Guarding the hoard:** open a chest or barrel, or break a gold block, where a dwarf within 16 blocks can see you, and they all turn on you.
- **Bartering:** right-click one with a gold ingot to trade for ores, gems or tools (`gameplay/dwarf_barter`, every 2 seconds at most).
- **Dwarf King:** each fortress has one in the throne room, with a crown, a netherite axe and 64 HP.
- Drops 0-2 raw iron and 1-5 gold nuggets (+Looting). Killed by a player, 8% (+2%/Looting) of an emerald or diamond. 8 XP.

## Where to find it / How to get it
- Only inside dwarven fortresses (at most 24 within 64 blocks), in the dark halls too. There's also a spawn egg.

## For developers
- `entity/MountainDwarfEntity`, `entity/DwarfGrudges`.
- Devscripts: `mountain-dwarf-behaviour.txt`, `mountain-dwarf-grudge.txt`, `mountain-dwarf-lineup.txt`.
