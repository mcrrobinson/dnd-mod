# Factions and Reputation
Hobbits, Mountain Dwarves and goblins remember what you do. Each player has a standing from -1000 to +1000 with each faction, in six tiers from Hostile to Exalted. Killing goblins, defending a village, handing in bounties and trading raise it. Hitting or killing hobbits and dwarves, or being seen at the dwarven hoard, lowers it. Factions are data files, so new ones (the elves, orcs and others of the race settlements) need no code.

![The action bar shows "+5 Hobbits of the Shire" and chat shows the standing rising to Friendly, then Honored](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/faction-rep/faction-rep-actionbar.png)

## How it works
- **Tiers**:

| Tier | Range | Colour |
|-|-|-|
| Hostile | -1000 .. -500 | dark red |
| Unfriendly | -499 .. -100 | red |
| Neutral | -99 .. 99 | grey |
| Friendly | 100 .. 399 | green |
| Honored | 400 .. 749 | aqua |
| Exalted | 750 .. 1000 | gold |

- **Launch factions**:

| Faction | Members | Settlements | Start | Rivals |
|-|-|-|-|-|
| Hobbits of the Shire (`dndclasses:hobbits`) | hobbits, innkeepers | hobbit villages | 0 (Neutral) | goblins -0.5 |
| Mountain Dwarves (`dndclasses:mountain_dwarves`) | mountain dwarves | dwarven fortresses | 0 (Neutral) | goblins -0.5 |
| The Goblin Horde (`dndclasses:goblins`) | goblin warriors, the Warlord | goblin camps | -600 (Hostile) | hobbits -0.5, dwarves -0.5 |

- **Sources**:

| Event | Hobbits | Dwarves | Goblins | Cap |
|-|-|-|-|-|
| Kill a goblin (not a boss minion) | +1 | +1 | -3 | hobbits and dwarves +30 a day from kills |
| Kill a Goblin Warlord | +25 | +25 | -50 | none |
| Kill a goblin while goblins are Neutral or better (betrayal) | +1 | +1 | -40 | |
| Win a goblin raid (taking part) | +60 at a village | +60 at a fortress | -40 | none |
| Claim a minor / major bounty | +8 / +20 | | -4 / -10 (rival) | none |
| Trade with an innkeeper | +1 a trade | | | +15 a day |
| Barter a gold ingot with a dwarf | | +1 | | +15 a day |
| Hit a hobbit / dwarf (not the killing blow) | -10 | -15 | | none |
| Kill a hobbit / innkeeper | -150 / -300 | | | none |
| Kill a dwarf / the Dwarf King | | -120 / -500 | | none |
| A dwarf sees you open a chest or barrel, or break the hoard | | -40 | | once per 10 s |

- **Rivals** lose a share of *gains* from bounties, trades and (later) quests: a +20 bounty for the hobbits is also -10 for the goblins. Kills and raid wins already list every faction they touch, so rivals don't add to them, and rival losses never chain.
- Kills by your tamed pets count as yours.
- **Daily caps** reset when the in-game day changes (`timeOfDay / 24000`).
- **Grudge decay**: each new in-game day, a negative standing with the hobbits or dwarves recovers 5 points, never past 0. Goblin standing doesn't decay.
- **Feedback**: every change shows on the action bar ("+1 Hobbits of the Shire, +1 Mountain Dwarves, -3 The Goblin Horde"). Crossing into a new tier prints "Your standing with ... is now Friendly." in chat, in the tier's colour, with a sound.
- Reputation is saved with the player and kept on death.
- This ticket only tracks reputation. Tiers don't change prices, hostility or raids yet.

## Where to find it
Anywhere: kill goblins, defend the hobbit village, trade at the Green Dragon. `/rep` shows your standing.

## Commands
- `/rep`: your standing with every faction (anyone).
- `/rep <player>`: someone else's (permission level 2).
- `/rep <player> <faction> set <n>` / `add <n>`: change it (permission level 2). Faction ids tab-complete.

## Configuration
Each faction is a data file, `data/<namespace>/factions/<id>.json`; a data pack can add or replace them. For example `dndclasses:hobbits`:

```json
{
  "name": "faction.dndclasses.hobbits",
  "color": "#7FBF4F",
  "members": "#dndclasses:faction/hobbits",
  "settlements": "#dndclasses:faction/hobbits",
  "start": 0,
  "rivals": {"dndclasses:goblins": -0.5},
  "decay_per_day": 5,
  "hit_member": -10,
  "kill_member": -150,
  "kill_member_overrides": {"dndclasses:innkeeper": -300},
  "enemy_kills": {"dndclasses:goblins": 1},
  "enemy_kill_overrides": {"dndclasses:goblin_warlord": 25},
  "kill_cap": 30,
  "raid_won": 60,
  "bounties": {"minor": 8, "major": 20},
  "trade": 1,
  "trade_cap": 15
}
```

| Field | Meaning |
|-|-|
| `name` | Translation key (or plain text) of the name |
| `color` | `#RRGGBB` |
| `members` (required) | Entity type id, entity type tag (`#ns:path`) or command tag (`@tag`) |
| `settlements` | Structure id or structure tag (`#ns:path`): raid wins and bounty claims |
| `start` | Standing before the player has done anything |
| `start_by_race` | `{"<race id>": n}` starting standing by race (used once races land) |
| `rivals` | `{"<faction>": factor}`, factor -1..0: share of each gain they lose |
| `decay_per_day` | Points a negative standing recovers per in-game day |
| `hit_member` / `kill_member` | Change for hitting / killing a member |
| `kill_member_overrides` | `{"<matcher>": n}` for particular members; command tags win over types, types over tags |
| `betrayal` | Kill change used instead while you're Neutral or better with this faction |
| `enemy_kills` | `{"<faction>": n}`: gain for killing a member of that faction (capped by `kill_cap`) |
| `enemy_kill_overrides` | `{"<matcher>": n}`: uncapped gain for particular enemies (bosses) |
| `kill_cap` | Daily cap on `enemy_kills` gains (0 = none) |
| `raid_won` | Gain when a goblin raid on one of `settlements` is won |
| `raid_defeated` | Change when a raid by this faction's members is beaten |
| `bounties` | `{"minor": n, "major": n}`: gain for a bounty handed in in one of `settlements`; if it's handed in in none, every faction with `bounties` gains |
| `trade` / `trade_cap` | Gain per trade or barter with a member, and its daily cap |
| `theft_witnessed` | Change when a member sees you at the hoard |

The entity tags `dndclasses:faction/<id>` and structure tags `dndclasses:faction/<id>` list the members and settlements of the launch factions. A file with an unknown field, a wrong type or an unknown entity type is logged (`[Factions] Skipping faction ...`) and skipped; the other factions still load.

## Known limitations
- Tiers have no effects yet (prices, hostility, raids, quests come in a later ticket).
- No Journal screen yet; the client keeps the synced values for it.
- Bounty boards outside every faction's settlements credit every faction with a `bounties` reward.
- Dungeon Masters (PR #121) still gain and lose reputation until both are merged and `Reputation.ignored` is set.

## For developers
- Code: `mattonfire.dnd.faction`. `Faction` (record + JSON parser), `Factions` (server data reload listener), `ReputationTier`, `Reputation` (API and storage), `FactionEvents` (kill/hit hooks, decay tick, and the calls below), `RepCommand`, `client/ClientReputation`.
- API: `Reputation.get(player, faction)`, `tier(...)`, `factionOf(entity)`, `add(player, faction, delta, Source)`, and `change(player, source).add(...).add(..., Cap, limit).apply()` for one event touching several factions (one action-bar line).
- Hooks in existing code: `FactionEvents.raidWon` (`GoblinRaid.win`), `bountyClaimed` (`BountyRewards.claim`), `traded` (`InnkeeperEntity.trade`, `MountainDwarfEntity` barter), `theftWitnessed` (`MountainDwarfEntity.witness`). The crowned dwarf gets the command tag `dndclasses.role.dwarf_king`.
- Stub hooks for other areas: `Reputation.raceOf` (races, for `start_by_race`) and `Reputation.ignored` (DM mode).
- Storage: player persistent data, `DndReputation` (faction id to value; missing = start) and `DndRepCaps` (`Day`, `DecayDay` and today's capped gains). Copied on respawn by `ClassLifecycle`.
- Sync: S2C `dndclasses:reputation_sync` (count, then id, name key, colour, value per faction) on join, respawn, data pack reload and every change. The client logs `[Reputation] client sync: [...]`.
- Tests: `devscripts/faction-rep.txt` (kills, cap, Warlord, raid, death), `faction-rep-relog.txt`, `faction-rep-sources.txt` (trades, bounties, barter, hoard, King, betrayal, decay).
