# Goblin Raids
Goblin war parties raid hobbit villages and dwarven fortresses in waves, ending with a Goblin Warlord. Players nearby see a raid bar and hear raid music. Defenders who win get loot, XP, Hero of the Village and the **Hold the Line** advancement.

![Two glowing Goblin Warriors marching into a hobbit village under the red goblin raid bar](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/goblin-raid.png)

## How it works
- **Trigger**: at night, every 30 seconds, each survival or adventure player in or within 32 blocks of a village or fortress has a 1 in 20 chance of a raid setting out for it. Each settlement is raided at most once every 3 in-game days. Raids never start in peaceful.
- **Waves**: 2 on easy, 3 on normal, 4 on hard. Wave *n* has *n*+1 Goblin Warriors, plus one more on hard. A raid horn sounds from the war party's direction 10 seconds before the first wave. Each wave gathers on open ground about 32 blocks out (in front of the gate at a fortress). There is a 15 second lull and another horn call between waves. The **Goblin Warlord** leads the final wave: it marches on the village green or fortress gate and holds it.
- **Fighting**: an idle raider attacks the nearest hobbit, dwarf, villager, iron golem or player within 16 blocks. Otherwise it marches on the village green or the gate. Hobbits flee, and dwarves are sent at the nearest raider. When only the last 2 raiders of a wave are left, they start glowing after 30 seconds.
- **Bar and music**: everyone within 96 blocks sees a red raid bar. It counts down to the next wave, then shows the wave's remaining health. They also hear **Steel on Steel**.
- **Victory**: the settlement cheers. Everyone who took part (in survival or adventure, in range at some point during the raid) gets:
  - Hero of the Village for 40 minutes
  - a reward loot table: emeralds and food at a village; gold, iron, gems or an enchanted axe at a fortress. Both can include the Steel on Steel disc.
  - 40 + 20 × waves XP
  - the Hold the Line advancement

  At a fortress, the dwarves also drop any grudge against the defenders.
- **Defeat**: if no one is within 96 blocks for 2 minutes, the war party withdraws and its goblins vanish.
- **Peaceful**: switching to Peaceful mid-raid makes the war party withdraw, with no rewards.
- Raiders carry the `dndclasses.goblin_raider` tag. One that was unloaded when its raid ended, or that the raid lost track of (unloaded for 10 seconds while defenders are about), vanishes as soon as its chunk loads again, so no war party is left behind.
- Raids are saved with the world and pick up again on reload.

## Where to find it
Stay in or near a hobbit village (`/locate structure dndclasses:hobbit_village`) or a dwarven fortress (`/locate structure dndclasses:dwarven_fortress`) at night.

## Commands
Permission level 2.
- `/goblinraid start`: raid the nearest village or fortress within about 128 blocks, ignoring the cooldown. If there isn't one, raid the spot you're standing on.
- `/goblinraid start here`: raid the spot you're standing on.
- `/goblinraid stop`: call off the nearest raid.
- `/goblinraid list`: list active raids with their state, wave and the number of raiders left.

## Configuration
- `/gamerule dndGoblinRaids false`: stops natural raids. `/goblinraid` still works.

## Known limitations
- The raid doesn't count goblins the Warlord calls in mid-fight. They stay behind as ordinary hostile mobs after a win (they drop nothing, like all boss minions).
- Raiders from before the `dndclasses.goblin_raider` tag existed aren't cleaned up.
- The raid music reuses Steel on Steel.
- Natural raids only check loaded chunks near players.

## For developers
- `entity/raid/GoblinRaids`: a per-world `PersistentState` that holds the trigger, cooldowns and gamerule.
- `entity/raid/GoblinRaid`: one raid. It runs the waves, raid bar, music and raider steering, and gives the rewards.
- `entity/raid/Settlement`: finds the nearest village or fortress and its rally point (the village green, or the gate's terrace).
- `classes/Commands/GoblinRaidCommand`: the `/goblinraid` command.
- `DwarfGrudges.forgive`: called for fortress defenders after a win. Raid music goes through `BossMusic` and `EventMusic`.
- Data files:
  - loot tables: `data/dndclasses/loot_tables/gameplay/goblin_raid_{hobbit_village,dwarven_fortress,wilds}.json`
  - advancement: `advancements/goblin_raid_defended.json`
  - sound: `music.goblin_raid` in `sounds.json`
- Test: `timeout 300 ./gradlew runClient -PdevScript=devscripts/goblin-raid-check.txt` runs a raid to victory with `/kill`.
