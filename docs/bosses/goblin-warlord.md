# Goblin Warlord
The goblins' war leader: a big, crowned Goblin Warrior that bellows for reinforcements. One guards every Nether Fortress's central crossing, about 1 in 3 goblin camps has one, it leads the last wave of every goblin raid, and it's the boss of the Goblin Warren dungeon.

![The Goblin Warlord, crowned, between two Goblin Warriors](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/goblin-warlord.png)

## How it works
- 250 HP, 12 armor, 4 toughness, 90% knockback resistance, 15 attack damage, 0.25 speed. Fire immune and never despawns. Uses the [Goblin Warrior](../mobs/goblins.md)'s slow two-second swing.
- **Warcry:** while fighting, it stops, roars and summons a wave of 2 [Goblin Warriors](../mobs/goblins.md) every 15 seconds. It won't call a wave while 4 or more goblins are already fighting within 24 blocks. Summoned goblins drop no loot or XP and give no class XP or bounty progress.
- **Rally:** hurting it sends idle goblins nearby after you.
- **Enrage (below 50% health):** the bar turns red, it gains +25% speed and +5 attack damage, and it calls a wave straight away. After that, waves of 3 come every 10 seconds, up to 6 goblins.
- Stays within 12 blocks of its post when not fighting.
- **Boss bar:** yellow, notched, for players within 48 blocks. There's no fight music (the Nether Fortress track keeps playing).

![The Goblin Warlord enraged: its boss bar has turned red](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/goblin-warlord-enraged.png)

## Where to find it
- **Nether Fortresses:** one stands in the middle of the bridge crossing that every Nether Fortress grows from. It's placed when the fortress generates, so only newly generated fortresses have one. A fortress generated on Peaceful gets an invisible marker instead, which raises the Warlord the first time it's loaded on a higher difficulty.
- **[Goblin camps](../structures/goblin-camps.md):** 30% of camps are led by a Warlord, which guards the camp's centre.
- **[Goblin raids](../systems/goblin-raids.md):** the final wave of every raid brings a Warlord with its warriors. It marches on the settlement's rally point and holds it.
- **[Goblin Warrens](../structures/dungeons.md):** the dungeon's boss, in its war hall.
- There's also a spawn egg.

## Rewards
- 4-9 gold ingots (+0-2 per Looting level).
- Killed by a player: 60 XP, 1-3 diamonds, 50% (+10%/Looting) netherite scrap, and 25% (+5%/Looting) a randomly enchanted golden helmet.
- Killed by a player: 35% the [Warlord's Warhorn](../items/magic-items.md#warlords-warhorn) (Rare, unidentified), and 50% a Rare magic item (unidentified, see [Magic loot](../systems/magic-loot.md)).

## Commands
- `/summon dndclasses:goblin_warlord`

## For developers
- `entity/GoblinWarlordEntity`, placement in `mixin/NetherFortressStartMixin` through `entity/boss/StructureBosses` (Peaceful markers), in camps by `world/gen/camp/GoblinCampPiece` (`WARLORD_CHANCE` 0.3) and in raids by `entity/raid/GoblinRaid` (final wave), loot `loot_tables/entities/goblin_warlord.json`.
- Devscripts: `goblin-warlord.txt`, `goblin-warlord-fortress.txt`.
