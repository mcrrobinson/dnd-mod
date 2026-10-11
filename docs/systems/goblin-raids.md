# Goblin Raids
Goblin war parties raid hobbit villages, dwarven fortresses, elven enclaves and dragon temples in waves, ending with a Goblin Warlord. Players nearby see a raid bar and hear raid music. Defenders who win get loot, XP, Hero of the Village and the **Hold the Line** advancement.

![Two glowing Goblin Warriors marching into a hobbit village under the red goblin raid bar](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/goblin-raid.png)

## How it works
- **Trigger**: at night, every 30 seconds, each survival or adventure player in or within 32 blocks of a village, fortress, enclave or temple has a 1 in 20 chance of a raid setting out for it (1 in 10 for a player the goblins have Marked, standing -800 or lower; see [Factions](factions.md)). Each settlement is raided at most once every 3 in-game days. Raids never start in peaceful.
- **Waves**: 2 on easy, 3 on normal, 4 on hard. Wave *n* has *n*+1 Goblin Warriors, plus one more on hard. A raid horn sounds from the war party's direction 10 seconds before the first wave. At a hobbit village, if a defender is Friendly or better with the hobbits, the lookouts spot the war party early: that player hears the horn and gets a chat warning at once, and the first wave comes 20 seconds later instead of 10. Each wave gathers on open ground about 32 blocks out (in front of the gate at a fortress, down in the valley below a temple's stair). There is a 15 second lull and another horn call between waves. The **Goblin Warlord** leads the final wave: it marches on the village green, fortress gate, enclave Moonwell or temple courtyard and holds it.
- **Fighting**: an idle raider attacks the nearest hobbit, dwarf, villager, iron golem or player within 16 blocks. It skips players the goblins don't mind: Neutral or better unless they've hit a goblin, and Unfriendly ones more than 6 blocks away. A Marked player counts as half as far away, so raiders go for them first. Otherwise it marches on the village green or the gate. Hobbits and Wood Elves flee, and dwarves, Elf Wardens and Dragonborn Templars are sent at the nearest raider. When only the last 2 raiders of a wave are left, they start glowing after 30 seconds.
- **Bar and music**: everyone within 96 blocks sees a red raid bar. It counts down to the next wave, then shows the wave's remaining health. They also hear **Steel on Steel**.
- **Victory**: the settlement cheers. Everyone who took part (in survival or adventure, in range at some point during the raid) gets:
  - Hero of the Village for 40 minutes
  - a reward loot table: emeralds and food at a village; gold, iron, gems or an enchanted axe at a fortress; emeralds, arrows, berries, amethyst, an enchanted book or bow at an enclave (`gameplay/goblin_raid_elven_enclave`); gold, blaze powder, bottles o' enchanting, Fire Resistance, an enchanted book or sword at a temple (`gameplay/goblin_raid_dragon_temple`). All can include the Steel on Steel disc. Every reward table has a 20% chance of an Uncommon magic item (unidentified), and a village's 2% of The Burglar's Ring (see [Magic loot](magic-loot.md)).
  - 40 + 20 × waves XP
  - the Hold the Line advancement
  - reputation: +60 with the settlement's faction and -40 with the goblins (see [Factions](factions.md))
  - at a hobbit village, a defender Exalted with the hobbits gets the loot rolled twice

  At a fortress, enclave or temple, the dwarves, Elf Wardens or Templars also drop any grudge against the defenders, and they rush out to meet the raiders.
- **Defeat**: if no one is within 96 blocks for 2 minutes, the war party withdraws and its goblins vanish.
- **Peaceful**: switching to Peaceful mid-raid makes the war party withdraw, with no rewards.
- Raiders carry the `dndclasses.goblin_raider` tag. One that was unloaded when its raid ended, or that the raid lost track of (unloaded for 10 seconds while defenders are about), vanishes as soon as its chunk loads again, so no war party is left behind.
- **Cleared Goblin Warrens**: when a [Goblin Warren](../structures/dungeons.md#the-goblin-warren) is cleared (its Warlord killed, or `/dungeon clear`), every raid whose rally point is within 256 blocks of the warren is called off ("Word of the Warlord's fall reaches the war party..."; its goblins vanish and its settlement goes back on cooldown), and no natural raid starts on a settlement within 256 blocks for 3 in-game days. Everyone in the warren is told. The server log says `[GoblinRaid] goblin warren <key> cleared at <pos>: called off <n> raid(s) within 256 blocks; no raids there until day <d>`.
- Raids are saved with the world and pick up again on reload (and so are the quiet zones round cleared warrens).

## Where to find it
Stay in or near a hobbit village (`/locate structure dndclasses:hobbit_village`) a dwarven fortress (`/locate structure dndclasses:dwarven_fortress`) an elven enclave (`/locate structure dndclasses:elven_enclave`) or a dragon temple (`/locate structure dndclasses:dragon_temple`) at night.

## Commands
Permission level 2.
- `/goblinraid start`: raid the nearest village, fortress, enclave or temple within about 128 blocks, ignoring the cooldown. If there isn't one, raid the spot you're standing on.
- `/goblinraid start here`: raid the spot you're standing on.
- `/goblinraid stop`: call off the nearest raid.
- `/goblinraid list`: list active raids with their state, wave and the number of raiders left, and the quiet zones round cleared goblin warrens with the minutes they have left.

## Configuration
- `/gamerule dndGoblinRaids false`: stops natural raids. `/goblinraid` still works.

## Known limitations
- The raid doesn't count goblins the Warlord calls in mid-fight. They stay behind as ordinary hostile mobs after a win (they drop nothing, like all boss minions).
- Raiders from before the `dndclasses.goblin_raider` tag existed aren't cleaned up.
- The raid music reuses Steel on Steel.
- Natural raids only check loaded chunks near players.

## For developers
- `entity/raid/GoblinRaids`: a per-world `PersistentState` that holds the trigger, cooldowns and gamerule. Cooldown API: `startCooldown(world, settlementKey)`, `isQuiet(world, pos)`, `quiet(world, center, radius, ticks)` (calls off raids in range and adds a `QuietZone`, saved as `Quiet`), `getQuietZones()`. It listens for `DungeonEvents.CLEARED` on Goblin Warrens (`WARREN_RADIUS` 256, `WARREN_QUIET` 3 days).
- `entity/raid/GoblinRaid`: one raid. It runs the waves, raid bar, music and raider steering, and gives the rewards.
- `entity/raid/Settlement`: finds the nearest village, fortress, enclave or temple and its rally point (the village green, the gate's terrace, the Moonwell's rim, or the middle of the temple courtyard, with raiders coming up the stair side).
- `classes/Commands/GoblinRaidCommand`: the `/goblinraid` command.
- `SettlementGrudges.forgive`: called for fortress defenders after a win. Raid music goes through `BossMusic` and `EventMusic`.
- Data files:
  - loot tables: `data/dndclasses/loot_tables/gameplay/goblin_raid_{hobbit_village,dwarven_fortress,wilds}.json`
  - advancement: `advancements/goblin_raid_defended.json`
  - sound: `music.goblin_raid` in `sounds.json`
- Test: `timeout 300 ./gradlew runClient -PdevScript=devscripts/goblin-raid-check.txt` runs a raid to victory with `/kill`.
