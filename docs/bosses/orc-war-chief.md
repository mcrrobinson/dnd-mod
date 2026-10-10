# Orc War Chief
The boss of every [Orc Stronghold](../structures/orc-strongholds.md): the hold's chieftain, waiting by his blackstone seat in the Great Longhouse with an Orcish Greataxe.

## How it works
- 90 health, 8 armour, 0.8 knockback resistance, 12 damage (2 base + the Greataxe's 10), 0.3 speed. Never despawns.
- Fights like any [orc](../mobs/orcs.md): he attacks players Hostile with the Bloodtusk Horde on sight (16 blocks, 8 for Barbarians), anyone who strikes an orc, and leaves tolerated players alone.
- **Ground Slam** every 8 s while his target is within 4 blocks: 6 damage (before armour) and a heavy knockback, up and away, to every creature within 4 blocks that he would fight. Orcs, goblins, creative players and tolerated players are spared. Explosion and smoke particles and a ravager's thud.
- **War Cry** when he drops below half health: the bar turns purple, he roars, and every orc within 16 blocks (himself included) gets Strength I for 10 s.
- **Boss bar**: red, notched, for players within 48 blocks while he fights a player, with the dragon fight music.
- Keeps within 6 blocks of his seat when not fighting.

## Where to find it / How to get it
One in the Great Longhouse of every Orc Stronghold, placed when it generates. There's also a spawn egg.

## Rewards
- 3-7 gold ingots and 2-5 leather (more with Looting).
- Killed by a player: 60 XP, 25% (+5%/Looting) an Orcish Greataxe, and 50% a Rare magic item (unidentified, see [Magic loot](../systems/magic-loot.md)).
- Killing him costs 300 standing with the Bloodtusk Horde.

## Commands
- `/summon dndclasses:orc_war_chief`
- `/dndhome stronghold seat` (operators): teleports you in front of his seat.

## For developers
- `entity/OrcWarChiefEntity` (`BossFight` with `.music(ModSounds.MUSIC_DRAGON_FIGHT)` and a 50% phase; `slam()` logs `[OrcStronghold] War Chief <name> slammed: N hit for 6.0`, the War Cry logs too). Loot `loot_tables/entities/orc_war_chief.json`. Role tag `dndclasses.role.orc_war_chief` (dialogue greetings in `dialogue/orc_war_chief.json`).
