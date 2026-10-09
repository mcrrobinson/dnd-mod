# Goblin Warlord
The boss of every Nether Fortress: a big, crowned Goblin Warrior that guards the fortress's central crossing and bellows for reinforcements.

![The Goblin Warlord, crowned, between two Goblin Warriors](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/goblin-warlord.png)

## How it works
- 250 HP, 12 armor, 4 toughness, 90% knockback resistance, 15 attack damage, 0.25 speed. Fire immune and never despawns. Uses the [Goblin Warrior](../mobs/goblins.md)'s slow two-second swing.
- **Warcry:** while fighting, it stops, roars and summons a wave of 2 [Goblin Warriors](../mobs/goblins.md) every 15 seconds. It won't call a wave while 4 or more goblins are already fighting within 24 blocks. Summoned goblins drop no loot or XP and give no class XP or bounty progress.
- **Rally:** hurting it sends idle goblins nearby after you.
- **Enrage (below 50% health):** the bar turns red, it gains +25% speed and +5 attack damage, and it calls a wave straight away. After that, waves of 3 come every 10 seconds, up to 6 goblins.
- Stays within 12 blocks of its post when not fighting.
- **Boss bar:** yellow, notched, for players within 48 blocks. There's no fight music (the Nether Fortress track keeps playing).

![The Goblin Warlord enraged: its boss bar has turned red](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/goblin-warlord-enraged.png)

## Where to find it / How to get it
- One stands in the middle of the bridge crossing that every Nether Fortress grows from. It's placed when the fortress generates, so only newly generated fortresses have one. A fortress generated on Peaceful gets an invisible marker instead, which raises the Warlord the first time it's loaded on a higher difficulty. There's also a spawn egg.

## Rewards
- 4-9 gold ingots (+0-2 per Looting level).
- Killed by a player: 60 XP, 1-3 diamonds, 50% (+10%/Looting) netherite scrap, and 25% (+5%/Looting) a randomly enchanted golden helmet.

## Commands
- `/summon dndclasses:goblin_warlord`

## For developers
- `entity/GoblinWarlordEntity`, placement in `mixin/NetherFortressStartMixin` through `entity/boss/StructureBosses` (Peaceful markers), loot `loot_tables/entities/goblin_warlord.json`.
- Devscripts: `goblin-warlord.txt`, `goblin-warlord-fortress.txt`.
