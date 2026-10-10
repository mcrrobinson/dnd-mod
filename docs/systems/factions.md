# Factions and Reputation
Hobbits, Mountain Dwarves, the elves of the Sylvan Court and goblins remember what you do. Each player has a standing from -1000 to +1000 with each faction, in six tiers from Hostile to Exalted. Killing goblins, defending a village, handing in bounties and trading raise it. Hitting or killing hobbits and dwarves, or being seen at the dwarven hoard, lowers it. Factions are data files, so new ones (the elves, orcs and others of the race settlements) need no code.

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
| Hobbits of the Shire (`dndclasses:hobbits`) | hobbits, innkeepers | hobbit villages | 0 (Neutral); Halflings 150 (Friendly) | goblins -0.5 |
| Mountain Dwarves (`dndclasses:mountain_dwarves`) | mountain dwarves | dwarven fortresses | 0 (Neutral); Dwarves 150, Gnomes 100 (Friendly) | goblins -0.5 |
| Sylvan Court (`dndclasses:sylvan_court`) | wood elves, Elf Wardens, the Speaker and the Fletcher | elven enclaves | 0 (Neutral); Elves 150 (Friendly) | goblins -0.5 |
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

### What the tiers do
Each NPC checks the standing of the player it's dealing with (or looking at) with its own faction, so rep is personal: a party can send its diplomat in. An NPC with no faction, and a player in [DM mode](dungeon-master.md), count as Neutral.

**Hobbits** ([hobbits](../mobs/hobbits.md), [innkeeper and bounty boards](../structures/hobbit-tavern.md))

| Tier | Effect |
|-|-|
| Hostile | Hobbits flee you (8 blocks) and give no gifts. The innkeeper won't trade or pay bounties; bounty boards won't give or take notices ("The notices are not for the likes of you."). |
| Unfriendly | Innkeeper prices +50% (rounded up, at least +1); a hobbit gives a gift every 15 minutes instead of 5. |
| Neutral | As before. |
| Friendly | Prices -10% (rounded). |
| Honored | Prices -25%; gifts every 2.5 minutes. |
| Exalted | Prices -40%; gifts every 2.5 minutes. |

"Price" is the first thing a trade asks for: emeralds for food and ale, or the produce the innkeeper buys (20 wheat becomes 30 at Unfriendly and 12 at Exalted). A trade never drops below 1.

**Mountain Dwarves** ([mountain dwarves](../mobs/mountain-dwarves.md))

| Tier | Effect |
|-|-|
| Hostile | Every dwarf attacks you on sight. |
| Unfriendly | Half the time a dwarf refuses to barter (the gold stays with you); a dwarf notices you at the hoard from 24 blocks instead of 16. |
| Neutral | As before. |
| Friendly | Barter: 15% chance of a second roll. |
| Honored | Barter: 30% second roll, from the better `gameplay/dwarf_barter_honored` table. |
| Exalted | Barter: 50% second roll, from the honored table. |

**Goblins** ([goblins](../mobs/goblins.md), [raids](goblin-raids.md))

| Tier | Effect |
|-|-|
| Hostile, -800 or lower ("Marked") | Natural raids near you are twice as likely (1 in 10 per check instead of 1 in 20); raiders go for you over other defenders (you count as half as far away). |
| Hostile | Goblins attack on sight, as before. |
| Unfriendly | Goblins (camp, Nether, raiders, the Warlord) leave you alone unless you come within 6 blocks ("parley range") or hit one. |
| Neutral and up | Goblins ignore you unless you hit one. |

"Hit one" means you hit that goblin, or any goblin in the last 10 seconds; a goblin you've provoked at Neutral stands down 10 seconds after you stop fighting. A goblin that's already fighting you when your standing improves (a quest, `/rep`) stands down within a second.

**Checks.** A Charisma check against a faction member (Persuasion now, quest dialogue later) has its DC shifted by your standing: Hostile +5, Unfriendly +3, Neutral 0, Friendly -2, Honored -4, Exalted -6. Plain villagers belong to no faction, so their DC stays 12 unless a data pack adds a faction for them.

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
| `start_by_race` | `{"<race id>": n}` starting standing by [race](../races/races.md), e.g. `{"dndclasses:dwarf": 100}` |
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
- Not yet: Friendly's earlier raid horn, Honored's extra innkeeper trade and free long rest, Exalted's double raid loot, the Honored barrel exception at the hoard, Exalted dwarves joining your fights and identifying items, and goblin aggro for opening a camp chief's chest. Quest and dialogue unlocks come with the quest tickets.
- Hobbit gift cooldowns are per hobbit: each gift starts that player's wait, and the next player gets a gift once their own wait has passed since it (or the last one's wait is over).
- Bounty boards outside every faction's settlements credit every faction with a `bounties` reward.

## For developers
- Code: `mattonfire.dnd.faction`. `Faction` (record + JSON parser), `Factions` (server data reload listener), `ReputationTier`, `Reputation` (API and storage), `FactionEvents` (kill/hit hooks, decay tick, and the calls below), `RepCommand`, `client/ClientReputation` (read by the [Journal](quest-journal.md)'s Factions tab).
- API: `Reputation.get(player, faction)`, `tier(...)`, `factionOf(entity)`, `add(player, faction, delta, Source)`, and `change(player, source).add(...).add(..., Cap, limit).apply()` for one event touching several factions (one action-bar line).
- Hooks in existing code: `FactionEvents.raidWon` (`GoblinRaid.win`), `bountyClaimed` (`BountyRewards.claim`), `traded` (`InnkeeperEntity.trade`, `MountainDwarfEntity` barter), `theftWitnessed` (`SettlementGrudges.witness`). The crowned dwarf gets the command tag `dndclasses.role.dwarf_king`.
- Hooks: `Reputation.raceOf` gives the player's active race as `dndclasses:<race>` (`RaceLifecycle.activeRaceOf`, null with no race or `dndRaces` off) for `start_by_race`, and `Reputation.ignored` is `DungeonMaster::isDm`, so [Dungeon Masters](dungeon-master.md) don't gain or lose reputation.
- Storage: player persistent data, `DndReputation` (faction id to value; missing = start) and `DndRepCaps` (`Day`, `DecayDay` and today's capped gains). Copied on respawn by `ClassLifecycle`.
- Sync: S2C `dndclasses:reputation_sync` (count, then id, name key, colour, value per faction) on join, respawn, data pack reload and every change. The client logs `[Reputation] client sync: [...]`.
- Tier effects: `TierEffects` holds every number and rule (`tierWith(player, npc|type|faction)`, `reputationPriceDelta`, `giftCooldown`, `refusesBarter`, `secondBarterRollChance`, `honoredBarter`, `witnessRange`, `goblinMayTarget`, `provoked`, `isMarked`, `raidChance`, `dcShift`). The NPCs only ask it: `InnkeeperEntity.applyReputationPrices` (special price per customer, added in `prepareOffersFor` after the prices are cleared and before `KinPrices.apply`, so the two stack; cleared when the customer leaves), `BountyBoardBlock.shuns`, `HobbitEntity` (flee goal, gift cooldown), `MountainDwarfEntity` (`shouldAngerAt`, barter), `SettlementGrudges` (the dwarves' witness range), `GoblinWarriorEntity` (target goal predicate, `setTarget` filter, stand-down check; the Warlord inherits it and rallies its kin through `rallyAgainst`), `GoblinRaid.nearestDefender`, `GoblinRaids.maybeStartRaids`, `Persuasion.dc(player, target[, base])` (the hook for dialogue checks).
- Tests: `devscripts/rep-tiers.txt` (prices, refusals, flee, gifts, dwarf aggro, witness range, goblin targeting, Persuasion DC; the Persuasion part needs a test data pack faction `dndtest:villagers` with `"members": "minecraft:villager"` in the world), `rep-tiers-raid.txt` (raiders at Neutral and Hostile), `rep-tiers-barter.txt` (refusals and second rolls), `devscripts/faction-rep.txt` (kills, cap, Warlord, raid, death), `faction-rep-relog.txt`, `faction-rep-sources.txt` (trades, bounties, barter, hoard, King, betrayal, decay).
