# Dungeon encounters
Monsters in a dungeon don't wait in the rooms: they appear when the party walks in, and how many depends on the party's size, its class levels and the dungeon's Challenge tier. While a fight is on, arcane seals close the room's doorways. The mid-boss room has a champion with a boss bar, and the boss room calls up the boss with its health scaled to the party. Killing the boss clears the dungeon.

The dungeons themselves (layout, tier, rooms, `/dungeon`) are on [Dungeons](../structures/dungeons.md).

## How it works
### Room fights
Every room with a fight (Encounter small, Encounter large, Mid-boss room, Boss room) runs **UNTOUCHED -> ACTIVE -> CLEARED**. Rooms with no fight (antechamber, trap corridor, gate, puzzle, vault, side rooms) count as cleared as soon as someone walks in, as before.

1. **Trigger**: the room's ward checks every 10 ticks. The first survival/adventure player past the 2-block walls starts the fight; in the boss room you have to be 5 blocks further in. Nothing happens on Peaceful, and the room stays UNTOUCHED.
2. **Spawn**: the encounter appears at the room's spawn points, furthest from you first, with a puff of smoke, a rattle and an evoker's summoning sound.
3. **Seal**: every doorway of the room fills with **Ward Seals** (`dndclasses:arcane_seal`): translucent, solid, light passes through, and nothing can break, push or blow them up. A doorway block with someone standing in it gets sealed as soon as they step out. "The ward seals the doorways!" shows on the action bar.
4. **Clear**: when every monster of the room is dead, the seals vanish, "Room cleared" shows on the action bar and everyone in the room gets `10 x tier` class XP, shared with their party through the usual party XP split.
5. **Abandon**: if nobody is in an ACTIVE room for 60 seconds (everyone died, or teleported out), its monsters vanish quietly (no drops), the seals open and the room goes back to UNTOUCHED, ready for a fresh fight. A monster that leaves the room or goes missing for 2 seconds no longer counts. Switching to Peaceful mid-fight does the same.

### Party size and level
- **Party size `n`** (1-8): the triggering player, every survival/adventure player inside the dungeon, and the triggering player's party members within 48 blocks. Dungeon Masters, creative players and spectators don't count.
- **Average level `L`**: the players' class levels; a player with no class, or level 0, counts as 1.

### Budget
`budget = room base x tier x party x level x difficulty`

| Room | Base |
|-|-|
| Encounter (small) | 6 |
| Encounter (large) | 10 |
| Mid-boss room (minions, besides the champion) | 5 |

| Tier | I | II | III | IV |
|-|-|-|-|-|
| Tier multiplier | 1.0 | 1.4 | 1.9 | 2.5 |
| Veteran chance | 0% | 10% | 25% | 40% |

| Party size | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 |
|-|-|-|-|-|-|-|-|-|
| Party multiplier | 1.0 | 1.6 | 2.1 | 2.6 | 3.0 | 3.4 | 3.8 | 4.2 |

- Level multiplier: `0.8 + 0.05 x L` (level 1: 0.85, level 5: 1.05, level 10: 1.3).
- Difficulty: Easy x0.75, Normal x1, Hard x1.25.
- Example: one level-1 player in a Tier I small room: 6 x 1 x 1 x 0.85 = 5.1 threat. Two players: 8.16.

The budget is filled with weighted picks from the theme's pool for the tier; each pick has to fit what's left, except the first, so a room always has at least 1 monster. At most 12 monsters per room (16 in the mid-boss room, champion included).

| Monster | Threat |
|-|-|
| Zombie, husk, drowned, spider, cave spider, wolf | 1 |
| Skeleton, stray | 1.5 |
| Witch | 2 |
| Wither skeleton, vindicator, Goblin Warrior, Mimic | 3 |
| Gelatinous Cube | 4 |
| Owlbear | 6 |

**Veterans**: each pick may become a "Veteran <name>" (if its 1.5x cost fits): +50% max health and +25% attack damage, and it glows for 2 seconds when it appears.

**Crypt pool**:

| Tier | Monsters (weight) |
|-|-|
| I | zombie 10, husk 3, skeleton 8, stray 2 |
| II | zombie 8, husk 4, skeleton 8, stray 4, Gelatinous Cube 2 |
| III | zombie 6, husk 4, skeleton 7, stray 5, wither skeleton 4, Gelatinous Cube 3 |
| IV | zombie 4, husk 4, skeleton 6, stray 6, wither skeleton 6, Gelatinous Cube 4 |

Encounter monsters drop their normal loot and count for bounties and class XP like any other monster. Monsters a boss summons (the Lich's raised dead) still give nothing.

### Champion (mid-boss)
The mid-boss room spawns its champion in the middle of the room, plus a 5-base budget of minions. The Crypt's champion is the **Ossuary Cube**, a Gelatinous Cube:
- Max health x3 for one player, +0.3 x the base per extra player (4 players: x3.9). The cube's base is 50, so 150 HP solo.
- +25% attack damage, +0.3 knockback resistance.
- A white, notched boss bar for players within 32 blocks.
- It starts with 3 stacks rolled from `minecraft:chests/simple_dungeon` inside it, which it drops when it dies.

### Boss
The boss room spawns its boss on the central dais when the first player is 5 blocks past its walls. The Crypt's boss is the **Lich** (300 HP), with its phylactery at the room's spawn point furthest from you.
- **Health**: x(1 + 0.35 per extra player), at most x3.45: one player 300 HP, two 405, four 615. The phylactery (and a Lich that reforms from it) gets the same multiplier.
- Tier III and up: +20% attack damage. Tier IV: heals 15% once when it drops below half health.
- **Lock-in**: the boss room seals when the boss appears. If no living player is in the boss room for 30 seconds, the boss (and its phylactery and minions) vanish quietly with no loot, the seals open and the room waits again: walking back in calls up a fresh boss at full health.
- **Victory**: the Lich's true death (smash the phylactery, then kill the Lich, or smash the phylactery while it reforms) defeats the boss. A Lich whose soul flees to its phylactery isn't dead yet.

### Clearing the dungeon
When the boss dies the dungeon is **cleared**: every room is marked cleared and its seals open, everyone inside sees a "Dungeon cleared" title with the dungeon's name and hears a fanfare, and gets the advancement **Delver** ("Defeat a dungeon's boss and clear the dungeon"). `/dungeon clear` does the same.

## Where to find it
In every dungeon (only the Crypt so far); see [Dungeons](../structures/dungeons.md).

## Commands
- `/dungeon trigger <room>`: starts that room's fight now, scaled for your party (you don't need to be in the room; its ward has to be loaded). Op level 2.
- `/dungeon info` also shows, for an ACTIVE room, how many monsters are left and how many seal blocks are up.
- `/dungeon reset` and `/dungeon clear` end any running fights: the seals open and leftover monsters vanish.
- To clear a room by hand: `/kill @e[tag=dndclasses.dungeon]`.

## Configuration
Pools are data: `data/<namespace>/dungeon_encounters/<theme>.json` (`/reload` picks up changes). Each entry uses the [encounter spawn format](dungeon-master.md) (`entity`, `nbt`, `initialize`, `tags`) plus `weight` and an optional `threat` override:

```json
{
  "boss": "lich",
  "champion": {"entity": "dndclasses:gelatinous_cube", "name": "dungeon.dndclasses.champion.ossuary_cube",
               "absorbed_loot": "minecraft:chests/simple_dungeon", "absorbed_stacks": 3},
  "tiers": {
    "1": [{"entity": "minecraft:zombie", "weight": 10}, {"entity": "minecraft:skeleton", "weight": 8}],
    "3": [{"entity": "minecraft:wither_skeleton", "weight": 4, "threat": 3}]
  }
}
```
A tier with no list uses the nearest lower tier. `boss` is `lich` or `goblin_warlord`.

## Known limitations
- Only the Crypt has a pool, a champion and a boss. The Goblin Warren and Dwarven Ruin come with their own tickets.
- The Lich's phylactery stands in the boss room for now; the reliquary side room comes with the finished Crypt, as do the Ossuary Cube's split at half health and the Crypt's room dressing.
- Veterans don't yet roll extra loot, and the Ossuary Cube's stacks come from the vanilla dungeon chest table: dungeon loot tables come in a later ticket (Veterans are tagged `dndclasses.dungeon_elite` for it).
- Seals block the way out of a fight too, so a party can only leave a sealed room by winning, dying or teleporting.
- Arrival sounds, the champion bar and the clear fanfare are vanilla sounds until the dungeon music ticket.

## For developers
- `dungeon/EncounterBuilder`: budget (`budget`, `roomBase`, `partyMult`, `levelMult`, `difficultyMult`), `plan` (weighted fill and Veterans), `partyFor`, `spawn` (through the DM toolkit's `EncounterSpawner`, one mob per spawn point), `makeVeteran`. Tags: `dndclasses.dungeon` and `dndclasses.dungeon=<startKey>/<roomId>` on every mob, `dndclasses.dungeon_elite` on Veterans.
- `dungeon/DungeonEncounterPools`: the reload listener for `dungeon_encounters/*.json`, reusing `Encounter.parseSpawn`.
- `dungeon/DungeonChampion`: champion modifiers and its `ServerBossBar`, ticked by the ward (tag `dndclasses.dungeon_champion`).
- `dungeon/DungeonCombat`: the `AFTER_DEATH` listener (tells the room's ward a mob died; a boss's true death fires `DungeonEvents.BOSS_DEFEATED` and clears the dungeon) and the `CLEARED` listener (title, sound, `dndclasses:delver`). Boss tag: `dndclasses.dungeon_boss`.
- `classes/Blocks/DungeonWardBlockEntity`: the state machine. NBT adds `Engaged`, `Mobs` (UUIDs), `Seals` (doorway blocks, found at the start of each fight: blocks of the inner wall ring open right through the outer ring, up to doorway height), `LastSeen` and `PartySize`.
- `classes/Blocks/ArcaneSealBlock` (`dndclasses:arcane_seal`, in `ModBlocks`). Not the class-gate obstacles `lesser_arcane_seal`/`greater_arcane_seal`, which a Wizard can dispel.
- `entity/boss/StructureBosses.spawnBoss(world, pos, yaw, BossType, healthMult, phylacterySpot, extras)` and `scaleHealth` (persistent `generic.max_health` modifier, safe to call again). `LichEntity.hasFledToPhylactery()`; a reformed Lich copies its phylactery's `dndclasses.dungeon*` tags.
- Devscripts: `dungeon-encounter.txt` (solo: encounter, seals, bounty credit, champion, boss lock-in and despawn, boss kill and clear, Peaceful), `dungeon-encounter-lan-host.txt` + `dungeon-encounter-lan-guest.txt` (two players: the budget and the Lich's 405 HP), `dungeon-encounter-recon.txt` (lists the dev world crypt's rooms).
