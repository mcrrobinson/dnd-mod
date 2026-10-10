# Dungeon Master
An op can run a session as the party's Dungeon Master: step outside the game, hide behind a veil, drop premade encounters where they're looking, and pause mobs and players. Later tickets add roll calls, narration, loot planting, a DM Wand and a DM panel on top of this.

## How it works
### DM mode
`/dm on` makes you a DM; `/dm off` ends it (and lifts the veil). While in DM mode you are outside the game:
- Mobs never target you (`ActiveTargetGoal` drops DMs as targets).
- You take no share of party XP, and don't show on your party members' HUD.
- You never count as a [goblin raid](goblin-raids.md) participant (no reward or advancement), raids don't start because of you, and raiders don't go for you.
- [Boss fights](../bosses/boss-fights.md) don't start or keep going because of you: a boss only counts as fighting when a non-DM player is its target or attacker.
- Your kills earn no [bounty](../structures/hobbit-tavern.md) progress.
- You can't be frozen.

DM mode is saved with the world, so it survives restarts and relogs.

### Veil
`/dm veil [on|off]` (no argument toggles; veiling also turns DM mode on). A veiled DM:
- isn't sent to non-DM clients at all: no model, armour, held items, name tag or particles. Other DMs still see you.
- can fly, takes no damage, makes no sounds (no footsteps) and can't be targeted by mobs.
- can still use blocks, chests and items, unlike spectator mode.

The veil is re-applied every second, so changing game mode doesn't drop it. You still appear in the tab list.

### Encounters
`/dm encounter spawn <id>` drops a premade group of mobs at the block you're looking at (up to 64 blocks away), or at `<pos>`. Each mob is placed on safe ground (a solid floor, two free blocks above it, no fluid) within the encounter's `spread` of that block, made persistent and turned to face the nearest player. Add `frozen` to spawn them already frozen.

Chat reports the result: `Encounter #7 "Goblin Patrol": 3 mobs at 1010, 151, 1010`. Every mob gets the tags `dndclasses.dm_encounter` and `dndclasses.enc.<n>`, so you can pick them out with selectors (`@e[tag=dndclasses.enc.7]`). A Lich's phylactery picks up its Lich's tags.

`/dm encounter clear <n>|all` removes an encounter's mobs with a puff of smoke. They don't die, so nothing drops. `/dm encounter list` lists the encounter files and the live encounters with how many of their mobs are still around (loaded ones only).

`/dm encounter list` counts every loaded entity carrying the encounter's tag, so a Lich court shows its phylactery as an extra.

Encounter mobs give the normal drops, XP and bounty credit, because the DM decides the story. An encounter with `"loot": false` drops nothing, gives no XP and earns no class XP or bounty credit, like boss minions.

The launch set:

| Id | Mobs | Difficulty |
|-|-|-|
| `goblin_patrol` | 2-3 Goblin Warriors | medium |
| `goblin_warband` | 5 Goblin Warriors and a Goblin Warlord | deadly |
| `owlbear_ambush` | 1 Owlbear | medium |
| `owlbear_den` | 2 Owlbears | hard |
| `undead_crypt` | 4 zombies, 2 skeletons, 1 wither skeleton | hard |
| `restless_dead` | 6 zombies | medium |
| `mimic_hoard` | 1 Mimic and 2 treasury chests (`chests/dwarven_fortress_treasury`, un-rolled so a Rogue has to pick them) | medium |
| `gelatinous_corridor` | 1 Gelatinous Cube | hard |
| `wyvern_pair` | 2 Wyverns | deadly |
| `lich_court` | the Lich (which places its phylactery) and 4 skeletons | deadly |

### Freeze
`/dm freeze <targets>`, `/dm freeze radius [r]` (mobs and players within `r` blocks, default 24), `/dm unfreeze <targets>`, `/dm unfreeze radius [r]` and `/dm unfreeze all`.
- **Mobs** stop thinking (`NoAI`) and moving, glow, and hang in place if they were in the air (`NoGravity`). Their old `NoAI`, `NoGravity` and glowing are kept and put back on unfreeze.
- **Players** can't move, jump or fly (held where they were frozen; a `/tp` moves the hold point), attack, break or use blocks, use items or entities, or use their class power. The action bar reads "The Dungeon Master holds the scene." The freeze is saved on the player, so logging out and back in doesn't escape it.
- Frozen mobs and players take no damage, so nobody gets free hits during a pause. Boss bars stay up.
- Frozen entities have the tag `dndclasses.dm_frozen`.

## Where to find it
Server-side commands only; nothing to craft.

## Commands
Ops (permission level 2) can run every `/dm` command. `/dm grant <player>` lets a non-op run all of them except `grant` and `revoke`; `/dm revoke <player>` takes it back (and ends their DM mode).

| Command | What it does |
|-|-|
| `/dm on` / `/dm off` | DM mode for yourself |
| `/dm status` | Lists DMs, veiled DMs and granted players |
| `/dm grant <player>` / `/dm revoke <player>` | (op only) Let a non-op use `/dm` |
| `/dm veil [on\|off]` | Hide from non-DM players; toggles with no argument |
| `/dm encounter list` | Encounter files and live encounters |
| `/dm encounter spawn <id> [<pos>] [frozen]` | Spawn an encounter at the block you look at, or at `pos` |
| `/dm encounter clear <n>\|all` | Remove an encounter's mobs, no drops |
| `/dm freeze <targets>\|radius [r]` | Pause mobs and players |
| `/dm unfreeze <targets>\|radius [r]\|all` | Let them go |

Encounter ids tab-complete. Ids from this mod can be written without the namespace (`goblin_patrol`); others need it (`mypack:bandits`).

## Configuration
### Encounter files
Encounters are data: `data/<namespace>/encounters/<id>.json`, so a datapack can add its own, and `/reload` picks up changes. The same format is meant for anything else that drops a group of mobs (dungeon rooms), through `EncounterSpawner.spawn`.

```json
{
  "name": "Goblin Patrol",
  "difficulty": "medium",
  "spread": 3,
  "loot": true,
  "spawns": [
    {"entity": "dndclasses:goblin_warrior", "count": [2, 3]},
    {"entity": "dndclasses:goblin_warlord", "chance": 0.2, "nbt": "{CustomName:'\"Grub\"'}", "tags": ["patrol.leader"]}
  ],
  "chests": [
    {"loot_table": "dndclasses:chests/dwarven_fortress_treasury", "count": 1}
  ]
}
```

| Field | Default | Meaning |
|-|-|-|
| `name` | the file name | Shown in chat |
| `difficulty` | `medium` | `easy`, `medium`, `hard` or `deadly`. Informational for now |
| `spread` | 3 | How far (in blocks, each way) from the centre mobs and chests may be placed |
| `loot` | `true` | `false`: no drops or XP, no class XP and no bounty credit |
| `spawns` | required | One entry per kind of mob |
| `spawns[].entity` | required | Entity id |
| `spawns[].count` | 1 | A number, or `[min, max]` |
| `spawns[].chance` | 1.0 | Chance (0-1) that this entry spawns at all |
| `spawns[].nbt` | none | Extra entity NBT, as an SNBT string (`"{PersistenceRequired:1b}"`) or a JSON object |
| `spawns[].initialize` | `true` | Run the vanilla spawn setup (random gear, variants), as `/summon` does. Set `false` when `nbt` sets the gear |
| `spawns[].tags` | none | Extra command tags |
| `chests` | none | Chests placed near the centre with an un-rolled loot table (locked for [Lockpicking](d20-skill-checks.md)) |

A file that fails to parse is skipped with an error in the server log (`[Encounters] couldn't load ...`).

## Known limitations
- Frozen mobs that run their own movement code instead of AI goals (some flying dragons and bosses) may still act; `NoAI` covers vanilla-style mobs and the goblins, owlbears and cubes.
- `unfreeze all` and `encounter clear` only reach loaded entities. Mobs in unloaded chunks stay frozen (or stay put) until you go back.
- Chests from an encounter stay after `clear`.
- A veiled DM is still listed in the tab list, and mobs still bump into them on the server.
- DMs gain and lose no [faction reputation](factions.md). The quest hook (no quest progress for DMs) arrives with quests.

## For developers
- Package `mattonfire.dnd.dm`:
  - `DungeonMaster`: `PersistentState` (`dndclasses_dm`) with the DM, granted and veiled sets and the live encounters. Static `isDm(entity)`, `isVeiled(entity)`, `isGranted(entity)`, `canUse(source)`. Use `isDm` to leave DMs out of any new player-counting system.
  - `DmCommand`: `/dm`, registered in `DnDClasses`.
  - `DmVeil`: veil abilities, damage cancel and `refreshTracking`. `classes/mixin/DmEntityTrackerMixin` stops `ThreadedAnvilChunkStorage.EntityTracker` tracking a veiled DM for non-DM viewers; `ThreadedAnvilChunkStorageAccessor` + `DmTracked` re-run tracking when the veil changes.
  - `DmFreeze`: freeze/unfreeze, Fabric attack/use/break callbacks, the player hold tick; the power-up packet in `DnDClasses` checks `DmFreeze.isFrozen`.
  - `encounter/Encounter` (parsed file), `encounter/Encounters` (reload listener, `get(id)`, `all()`), `encounter/EncounterSpawner` (`spawn(world, encounter, center, tags)` returns the mobs and chests; `NO_LOOT_TAG`).
- `"loot": false` mobs get `dndclasses.no_loot`; `BossMinions.givesNothing` covers them and boss minions, and is checked in `BossMinionDropMixin`, `BountyRewards` and `ProgressionEvents`.
- DM exclusions: `ActiveTargetGoalMixin`, `PartyEvents.shareXp` and `syncHud`, `BossFight.isFightingPlayer`, `GoblinRaids.maybeStartRaids`, `GoblinRaid` participants and target picking, `BountyRewards.onKill`.
- Devscripts: `devscripts/dm-toolkit-host.txt` + `dm-toolkit-guest.txt` (two-client LAN test on port 25617), then `devscripts/dm-freeze-rejoin.txt`. Full verification: `dm-verify-host.txt` with `dm-verify-guest.txt` and `dm-verify-guest-rejoin.txt` (port 25641, guest run with `-PdevUser=DmGuest`), and `dm-verify-encounters.txt` on its own.
