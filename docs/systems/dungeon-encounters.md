# Dungeon encounters
Monsters in a dungeon don't wait in the rooms: they appear when the party walks in, and how many depends on the party's size, its class levels and the dungeon's Challenge tier. While a fight is on, arcane seals close the room's doorways. The mid-boss room has a champion with a boss bar, and the boss room calls up the boss with its health scaled to the party. Killing the boss clears the dungeon.

The dungeons themselves (layout, tier, rooms, `/dungeon`) are on [Dungeons](../structures/dungeons.md).

## How it works
### Room fights
Every room with a fight (Encounter small, Encounter large, Mid-boss room, Boss room) runs **UNTOUCHED -> ACTIVE -> CLEARED**. Rooms with no fight (antechamber, trap corridor, gate, vault, side rooms) count as cleared as soon as someone walks in, as before. The puzzle room counts as cleared once it's solved (see [Dungeon puzzles and gates](dungeon-puzzles-and-gates.md)).

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

The Goblin Warren's pool:

| Tier | Pool (weight) |
|-|-|
| I | Goblin Warrior 6, spider 8, cave spider 4, wolf 6 |
| II | Goblin Warrior 10, spider 6, cave spider 5 |
| III | Goblin Warrior 12, spider 5, cave spider 6 |
| IV | Goblin Warrior 14, spider 4, cave spider 6 |

Neutral monsters (the Warren's wolves) come in angry at the player nearest the one who walked in.

Encounter monsters drop their normal loot and count for bounties and class XP like any other monster. Monsters a boss summons (the Lich's raised dead) still give nothing.

### Champion (mid-boss)
The mid-boss room spawns its champion in the middle of the room, plus a 5-base budget of minions. The Crypt's champion is the **Ossuary Cube**, a Gelatinous Cube:
- Max health x3 for one player, +0.3 x the base per extra player (4 players: x3.9). The cube's base is 50, so 150 HP solo.
- +25% attack damage, +0.3 knockback resistance.
- A white, notched boss bar for players within 32 blocks.
- It starts with 3 stacks rolled from `minecraft:chests/simple_dungeon` inside it, which it drops when it dies.
- **Split**: the first time it's at or below half health, it bursts and sheds ordinary Gelatinous Cubes beside it: **2** for a party of two or more, **1** for a lone player. The new cubes belong to the room (it stays sealed until they're dead too). It splits only once.

The Goblin Warren's champion is **Grimclaw**, the goblins' war beast, an Owlbear (base 50 HP, so 150 HP solo, with the same champion buffs and bar):
- **Chained**: it appears in the middle of its pen, between the hanging chains, held in its chains (no AI) with its chains rattling. After 3 seconds, or as soon as anything hurts it, it breaks free: the chains clatter, it roars and turns on the nearest player.
- **Fury**: below half health it charges every 4 seconds instead of 6.

### Boss
The boss room spawns its boss on the central dais when the first player is 5 blocks past its walls. The Crypt's boss is the **Lich** (300 HP), and its phylactery appears on the altar of the **reliquary**, the side room through a door in the boss room's wall (about 25 blocks from the dais).
- **The reliquary is part of the fight**: its doorway isn't sealed, players in it count as in the boss room (for the 30 s lock-in and for the kill's participants), and the phylactery and a Lich that reforms there are followed there.
- **The Lich's lair**: a dungeon Lich wanders round the boss room's middle, and if it's ever more than its leash (the distance to the phylactery + 8, at least 28: about 33 blocks) from it, it blinks back. It still blinks to the phylactery to defend it when it's hit. A boss that ends up outside the boss room and reliquary (a blink through a wall) is brought back to the dais instead of calling the fight off. A dungeon without a reliquary (none yet besides the Crypt) puts the phylactery at the boss room's spawn point furthest from you, and the Lich keeps its usual 28-block leash round it.
- **Health**: x(1 + 0.35 per extra player), at most x3.45: one player 300 HP, two 405, four 615. The phylactery (and a Lich that reforms from it) gets the same multiplier.
- Tier III and up: +20% attack damage. Tier IV: heals 15% once when it drops below half health.
- **Lock-in**: the boss room seals when the boss appears. If no living player is in the boss room for 30 seconds, the boss (and its phylactery and minions) vanish quietly with no loot, the seals open and the room waits again: walking back in calls up a fresh boss at full health.
- **Victory**: the Lich's true death (smash the phylactery, then kill the Lich, or smash the phylactery while it reforms) defeats the boss. A Lich whose soul flees to its phylactery isn't dead yet.

The Goblin Warren's boss is the **Goblin Warlord** (250 HP, so 250 solo, 337 for two, 512 for four), called up on the dais of its war hall. It guards the middle of the hall (`setGuardPos`, a 12-block leash) and heads back there after a fight, and still bellows for Goblin Warrior reinforcements; those vanish with it if the fight is called off. The same health scaling, tier bonuses and lock-in apply. Its death clears the dungeon, and (see [Goblin Raids](goblin-raids.md)) calls off goblin raids within 256 blocks for 3 days.

### Clearing the dungeon
When the boss dies the dungeon is **cleared**: every room is marked cleared and its seals open, everyone inside sees a "Dungeon cleared" title with the dungeon's name and hears a fanfare, and gets the advancement **Delver** ("Defeat a dungeon's boss and clear the dungeon"). `/dungeon clear` does the same.

## Where to find it
In every dungeon (the Crypt and the Goblin Warren so far); see [Dungeons](../structures/dungeons.md).

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
               "absorbed_loot": "minecraft:chests/simple_dungeon", "absorbed_stacks": 3,
               "chained": false},
  "tiers": {
    "1": [{"entity": "minecraft:zombie", "weight": 10}, {"entity": "minecraft:skeleton", "weight": 8}],
    "3": [{"entity": "minecraft:wither_skeleton", "weight": 4, "threat": 3}]
  }
}
```
A tier with no list uses the nearest lower tier. `boss` is `lich` or `goblin_warlord`. A `chained` champion starts held in its chains (Grimclaw).

## Known limitations
- The Crypt and the Goblin Warren have pools, champions and bosses. The Dwarven Ruin comes with its own ticket.
- A chained champion that's still chained when the server restarts breaks free as soon as its room's ward ticks again.
- The Ossuary Cube's stacks still come from the vanilla dungeon chest table, not the tier's elite table.
- Saving throws against the Lich's spells, and its Dread Presence, come in a later ticket.
- Seals block the way out of a fight too, so a party can only leave a sealed room by winning, dying or teleporting.
- Arrival sounds, the champion bar and the clear fanfare are vanilla sounds until the dungeon music ticket.

## For developers
- `dungeon/EncounterBuilder`: budget (`budget`, `roomBase`, `partyMult`, `levelMult`, `difficultyMult`), `plan` (weighted fill and Veterans), `partyFor`, `spawn` (through the DM toolkit's `EncounterSpawner`, one mob per spawn point), `makeVeteran`, `rouse` (an `Angerable` pool mob, such as a wolf, is made angry at the nearest player). Tags: `dndclasses.dungeon` and `dndclasses.dungeon=<startKey>/<roomId>` on every mob, `dndclasses.dungeon_elite` on Veterans.
- `dungeon/DungeonEncounterPools`: the reload listener for `dungeon_encounters/*.json`, reusing `Encounter.parseSpawn`.
- `dungeon/DungeonChampion`: champion modifiers and its `ServerBossBar`, ticked by the ward (tag `dndclasses.dungeon_champion`); `tickChained` (a `chained` champion: NoAI and tag `dndclasses.dungeon_champion_chained` until `CHAIN_TICKS` (60) pass or it's hurt); an Owlbear champion gets `OwlbearEntity.FURY_TAG` (`chargeCooldown()` 80 ticks below half health, 120 otherwise); `splitIfHurt` (the Ossuary Cube's split, once: tag `dndclasses.dungeon_champion_split`; `SPLIT_AT`, `SPLIT_COUNT`, `SPLIT_COUNT_SOLO`).
- Boss area: the boss room's ward treats the dungeon's `RELIQUARY` rooms (from the plan) as part of its room for players, mob tracking and minion clean-up, and doesn't seal a doorway that leads into one (`area`, `leadsToAnnex`). `DungeonWardBlockEntity.reliquarySpot(dungeon)` and `lairLeash(lair, phylactery)`; `DungeonCombat.playersInBossArea`.
- `LichEntity.setLair(centre, leash)` / `getLair()` (NBT `Lair`, `LairLeash`): home and leash round a lair instead of the phylactery. The phylactery hands the lair on to a Lich that reforms from it (NBT `LichLair`, `LichLairLeash`).
- `dungeon/DungeonCombat`: the `AFTER_DEATH` listener (tells the room's ward a mob died; a boss's true death fires `DungeonEvents.BOSS_DEFEATED` and clears the dungeon) and the `CLEARED` listener (title, sound, `dndclasses:delver`). Boss tag: `dndclasses.dungeon_boss`.
- `classes/Blocks/DungeonWardBlockEntity`: the state machine. NBT adds `Engaged`, `Mobs` (UUIDs), `Seals` (doorway blocks, found at the start of each fight: blocks of the inner wall ring open right through the outer ring, up to doorway height), `LastSeen` and `PartySize`.
- `classes/Blocks/ArcaneSealBlock` (`dndclasses:arcane_seal`, in `ModBlocks`). Not the class-gate obstacles `lesser_arcane_seal`/`greater_arcane_seal`, which a Wizard can dispel.
- `entity/boss/StructureBosses.spawnBoss(world, pos, yaw, BossType, healthMult, phylacterySpot, extras)` and `scaleHealth` (persistent `generic.max_health` modifier, safe to call again). `LichEntity.hasFledToPhylactery()`; a reformed Lich copies its phylactery's `dndclasses.dungeon*` tags.
- Devscripts: `dungeon-encounter.txt` (solo: encounter, seals, bounty credit, champion, boss lock-in and despawn, boss kill and clear, Peaceful), `dungeon-encounter-lan-host.txt` + `dungeon-encounter-lan-guest.txt` (two players: the budget and the Lich's 405 HP), `dungeon-encounter-recon.txt` (lists the dev world crypt's rooms).
