# Factions and Reputation
Hobbits, Mountain Dwarves, the elves of the Sylvan Court, the Dragonsworn of the dragon temples, the Brimstone Covenant of the Nether's sanctums, the Tinkers' Guild of Cogsdeep, the Bloodtusk Horde of the orc strongholds, the drow of the Spider Houses, the duergar of the Grey Clans and goblins remember what you do. Each player has a standing from -1000 to +1000 with each faction, in six tiers from Hostile to Exalted. Killing goblins, defending a village, handing in bounties and trading raise it. Hitting or killing hobbits and dwarves, or being seen at the dwarven hoard, lowers it. Factions are data files, so new ones (the elves, orcs and others of the race settlements) need no code.

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
| Mountain Dwarves (`dndclasses:mountain_dwarves`) | mountain dwarves | dwarven fortresses | 0 (Neutral); Dwarves 150 (Friendly) | goblins -0.5 |
| Sylvan Court (`dndclasses:sylvan_court`) | wood elves, Elf Wardens, the Speaker and the Fletcher | elven enclaves | 0 (Neutral); Elves 150 (Friendly) | goblins -0.5 |
| The Dragonsworn (`dndclasses:dragonsworn`) | Dragonborn Templars, the Elder | dragon temples | 0 (Neutral); Dragonborn 150 (Friendly) | goblins -0.5 |
| The Brimstone Covenant (`dndclasses:brimstone_covenant`) | Tiefling Exiles, the Pactbroker | brimstone sanctums | 0 (Neutral); Tieflings 150 (Friendly) | none |
| The Tinkers' Guild (`dndclasses:tinkers_guild`) | Gnome Tinkers, Clockwork Sentinels | Cogsdeep burrows | 150 (Friendly); Gnomes 450 (Honored) | goblins -0.5 |
| The Bloodtusk Horde (`dndclasses:bloodtusk_horde`) | Orc Warriors, Archers, the Quartermaster and the War Chief (not the Trial's Champion) | orc strongholds | -600 (Hostile); Half-Orcs 0 (Neutral) | hobbits -0.25, dwarves -0.25, Sylvan Court -0.25 |
| The Goblin Horde (`dndclasses:goblins`) | goblin warriors, the Warlord | goblin camps | -600 (Hostile) | hobbits -0.5, dwarves -0.5, Sylvan Court -0.5, Dragonsworn -0.5 |
| The Grey Clans (`dndclasses:duergar_clans`) | [duergar](../mobs/duergar.md): Soldiers, Arbalesters, the Factor and the Taskmaster | [Duergar Holds](../structures/duergar-holds.md) | -250 (Unfriendly); Gnomes -400 (Unfriendly), Dwarves -550 (Hostile) | dwarves -0.5, the drow -0.25, Tinkers' Guild -0.25 |
| The Spider Houses (`dndclasses:drow`) | the [drow](../mobs/drow.md): Warriors, Mages, Priestesses, Elites and the [Matron Mother](../bosses/matron-mother.md) | [Drow Outposts](../structures/drow-outposts.md), the [Drow Spire](../structures/drow-spire.md) | -700 (Hostile); Elves -900 | Sylvan Court -0.5, Grey Clans -0.25 |

- **Sources**:

| Event | Hobbits | Dwarves | Goblins | Cap |
|-|-|-|-|-|
| Kill a goblin (not a boss minion) | +1 | +1 | -3 | hobbits and dwarves +30 a day from kills |
| Kill a Goblin Warlord | +25 | +25 | -50 | none |
| Kill a goblin while goblins are Neutral or better (betrayal) | +1 | +1 | -40 | |
| Win a goblin raid (taking part) | +60 at a village, scaled by its health left | +60 at a fortress, scaled | -40 | none |
| Lose a goblin raid: the settlement falls (taking part) | -40 at a village | -40 at a fortress | | none |
| Claim a minor / major bounty | +8 / +20 | | -4 / -10 (rival) | none |
| Trade with an innkeeper | +1 a trade | | | +15 a day |
| Barter a gold ingot with a dwarf | | +1 | | +15 a day |
| Hit a hobbit / dwarf (not the killing blow) | -10 | -15 | | none |
| Kill a hobbit / innkeeper | -150 / -300 | | | none |
| Kill a dwarf / the Dwarf King | | -120 / -500 | | none |
| Kill a duergar (Grey Clans -60, the Taskmaster -150) | | +30 | | dwarves +30 a day from kills |
| A dwarf sees you open a chest or barrel, or break the hoard | | -40 | | once per 10 s |
| Free a captive from a [Drow Outpost](../structures/drow-outposts.md) pen (hobbit / mountain dwarf; an elf gives the Sylvan Court +40) | +40 | +40 | | none |

- **Rivals** lose a share of *gains* from bounties, trades and (later) quests: a +20 bounty for the hobbits is also -10 for the goblins. Losses under a point carry over per rival, so small gains add up: each +1 trade with the Grey Clans costs the dwarves 0.5, a whole point every second trade. Kills and raid wins already list every faction they touch, so rivals don't add to them, and rival losses never chain.
- **Drow kills**: every [drow](../mobs/drow.md) killed gives the Sylvan Court +30, with no daily cap (an `enemy_kill_overrides` entry for `#dndclasses:faction/drow`), and the Grey Clans +15 (their `enemy_kills`, no daily cap).
- Kills by your tamed pets count as yours.
- **Daily caps** reset when the in-game day changes (`timeOfDay / 24000`).
- **Grudge decay**: each new in-game day, a negative standing with the hobbits or dwarves recovers 5 points, never past 0. Goblin standing doesn't decay.
- **Feedback**: every change shows on the action bar. One or two factions are listed by name ("-3 The Goblin Horde, +1 Mountain Dwarves"). With three or more, only the biggest change is named (largest amount, a loss before a gain on a tie, then the order the event added them) and the rest fold into a count: "-3 The Goblin Horde, +1 with 4 others" for a goblin kill, when the others all moved by the same amount; "-50 The Goblin Horde, and 4 others" (the count in gray) when they differ. `/rep` shows every faction's standing. Crossing into a new tier prints "Your standing with ... is now Friendly." in chat, in the tier's colour, with a sound.
- Reputation is saved with the player and kept on death.

### What the tiers do
Each NPC checks the standing of the player it's dealing with (or looking at) with its own faction, so rep is personal: a party can send its diplomat in. An NPC with no faction, and a player in [DM mode](dungeon-master.md), count as Neutral.

**Hobbits** ([hobbits](../mobs/hobbits.md), [innkeeper and bounty boards](../structures/hobbit-tavern.md))

| Tier | Effect |
|-|-|
| Hostile | Hobbits flee you (8 blocks) and give no gifts. The innkeeper won't trade or pay bounties; bounty boards won't give or take notices ("The notices are not for the likes of you."). |
| Unfriendly | Innkeeper prices +50% (rounded up, at least +1); a hobbit gives a gift every 15 minutes instead of 5. |
| Neutral | As before. |
| Friendly | Prices -10% (rounded). Lookouts: when a goblin raid sets out for a hobbit village you're defending, you hear the horn and get a warning straight away, and the first wave comes 20 s after it instead of 10 s. |
| Honored | Prices -25%; gifts every 2.5 minutes. The innkeeper adds a 15th trade, a round of 3 ale for 1 emerald that comes with a cake on the house, and the first time you open the trades each day hands you that night's Room Key free (so the long rest at the inn costs nothing). |
| Exalted | Prices -40%; gifts every 2.5 minutes. Winning a raid on a hobbit village rolls its victory loot twice for you. |

"Price" is the first thing a trade asks for: emeralds for food and ale, or the produce the innkeeper buys (20 wheat becomes 30 at Unfriendly and 12 at Exalted). A trade never drops below 1.

**Mountain Dwarves** ([mountain dwarves](../mobs/mountain-dwarves.md))

| Tier | Effect |
|-|-|
| Hostile | Every dwarf attacks you on sight. |
| Unfriendly | Half the time a dwarf refuses to barter (the gold stays with you); a dwarf notices you at the hoard from 24 blocks instead of 16. |
| Neutral | As before. |
| Friendly | Barter: 15% chance of a second roll. |
| Honored | Barter: 30% second roll, from the better `gameplay/dwarf_barter_honored` table. Opening a barrel in front of a dwarf no longer counts as theft (chests and gold still do). |
| Exalted | Barter: 50% second roll, from the honored table. Dwarves fight at your side: a dwarf within 16 blocks of you goes for a mob that hit you or that you hit in the last 5 s, or that has you as its target (not players, other dwarves, your own pets or creepers). Sneak + right-click a dwarf holding an unidentified magic item and it identifies it free (anyone else is told the dwarves "only look over magic for a friend of the mountain"). |

**Goblins** ([goblins](../mobs/goblins.md), [raids](goblin-raids.md))

| Tier | Effect |
|-|-|
| Hostile, -800 or lower ("Marked") | Natural raids near you are twice as likely (1 in 10 per check instead of 1 in 20); raiders go for you over other defenders (you count as half as far away). |
| Hostile | Goblins attack on sight, as before. |
| Unfriendly | Goblins (camp, Nether, raiders, the Warlord) leave you alone unless you come within 6 blocks ("parley range") or hit one. |
| Neutral and up | Goblins ignore you unless you hit one. |
| Any | Open (or smash) a goblin camp chief's chest and every goblin within 24 blocks of it comes for you, whether or not it can see you. They keep at it while they fight you and for 30 s after. |

"Hit one" means you hit that goblin, or any goblin in the last 10 seconds; a goblin you've provoked at Neutral stands down 10 seconds after you stop fighting. A goblin that's already fighting you when your standing improves (a quest, `/rep`) stands down within a second.

**Checks.** A Charisma check against a faction member (Persuasion now, quest dialogue later) has its DC shifted by your standing: Hostile +5, Unfriendly +3, Neutral 0, Friendly -2, Honored -4, Exalted -6. Plain villagers belong to no faction, so their DC stays 12 unless a data pack adds a faction for them.

## Where to find it
Anywhere: kill goblins, defend the hobbit village, trade at the Green Dragon. `/rep` shows your standing.

## Commands
- `/rep`: your standing with every faction (anyone).
- `/rep <player>`: someone else's (permission level 2).
- `/rep <player> <faction> set <n>` / `add <n>`: change it (permission level 2). Faction ids tab-complete.
- `/rep <player> <faction> reset`: forget the stored standing, so it reads as the start again (the race's start for a player with one).
- For a player in [DM mode](dungeon-master.md), `/rep` lines that aren't Neutral end in "- DM mode: NPCs treat <player> as Neutral": the value is kept, but tier effects (prices, the innkeeper's Honored round and free room, refusals) wait for `/dm off`.

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
  "raid_lost": -40,
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
| `kill_cap` | Daily cap on `enemy_kills` and `monster_kills` gains (0 = none) |
| `monster_kills` | `{"<matcher>": n}`: gain for killing a creature that isn't in an enemy faction (capped by `kill_cap`). The Mountain Dwarves have `{"dndclasses:rust_monster": 3}` |
| `raid_won` | Gain when a goblin raid on one of `settlements` is won, scaled by the settlement's health left (see [Goblin raids](goblin-raids.md)) |
| `raid_lost` | Change when a goblin raid on one of `settlements` is lost: the settlement's health ran out |
| `raid_defeated` | Change when a raid by this faction's members is beaten |
| `bounties` | `{"minor": n, "major": n}`: gain for a bounty handed in in one of `settlements`; if it's handed in in none, every faction with `bounties` gains |
| `trade` / `trade_cap` | Gain per trade or barter with a member, and its daily cap |
| `theft_witnessed` | Change when a member sees you at the hoard |

The entity tags `dndclasses:faction/<id>` and structure tags `dndclasses:faction/<id>` list the members and settlements of the launch factions. A file with an unknown field, a wrong type or an unknown entity type is logged (`[Factions] Skipping faction ...`) and skipped; the other factions still load.

## Known limitations
- Quest and dialogue unlocks come with the quest tickets.
- The tier perks are built for the hobbits and Mountain Dwarves (and the goblin camp chest for goblins). Other factions, including data-pack ones, get the price, hostility and DC rules but not these perks.
- Any dwarf joins your fights and identifies your items, not only those of a fortress or its King.
- Hobbit gift cooldowns are per hobbit: each gift starts that player's wait, and the next player gets a gift once their own wait has passed since it (or the last one's wait is over).
- Bounty boards outside every faction's settlements credit every faction with a `bounties` reward.

## For developers
- Code: `mattonfire.dnd.faction`. `Faction` (record + JSON parser), `Factions` (server data reload listener), `ReputationTier`, `Reputation` (API and storage), `FactionEvents` (kill/hit hooks, decay tick, and the calls below), `RepCommand`, `client/ClientReputation` (read by the [Journal](quest-journal.md)'s Factions tab).
- API: `Reputation.get(player, faction)`, `tier(...)`, `factionOf(entity)`, `add(player, faction, delta, Source)`, and `change(player, source).add(...).add(..., Cap, limit).apply()` for one event touching several factions (one action-bar line).
- Hooks in existing code: `FactionEvents.raidWon` (`GoblinRaid.win`), `bountyClaimed` (`BountyRewards.claim`), `traded` (`InnkeeperEntity.trade`, `MountainDwarfEntity` barter), `theftWitnessed` (`SettlementGrudges.witness`). The crowned dwarf gets the command tag `dndclasses.role.dwarf_king`.
- Hooks: `Reputation.raceOf` gives the player's active race as `dndclasses:<race>` (`RaceLifecycle.activeRaceOf`, null with no race or `dndRaces` off) for `start_by_race`, and `Reputation.ignored` is `DungeonMaster::isDm`, so [Dungeon Masters](dungeon-master.md) don't gain or lose reputation.
- Storage: player persistent data, `DndReputation` (faction id to value; missing = start) and `DndRepCaps` (`Day`, `DecayDay` and today's capped gains). Copied on respawn by `ClassLifecycle`.
- Sync: S2C `dndclasses:reputation_sync` (count, then id, name key, colour, value per faction) on join, respawn, data pack reload and every change. The client logs `[Reputation] client sync: [...]`.
- Tier effects: `TierEffects` holds every number and rule (`tierWith(player, npc|type|faction)`, `reputationPriceDelta`, `giftCooldown`, `refusesBarter`, `secondBarterRollChance`, `honoredBarter`, `witnessRange`, `goblinMayTarget`, `provoked`, `isMarked`, `raidChance`, `dcShift`). The NPCs only ask it: `NpcMerchant.preparePrices` (every settlement merchant: special price per customer, added after the prices are cleared and before `KinPrices.apply`, so the two stack; cleared when the customer leaves), `BountyBoardBlock.shuns`, `HobbitEntity` (flee goal, gift cooldown), `MountainDwarfEntity` (`shouldAngerAt`, barter), `SettlementGrudges` (the dwarves' witness range), `GoblinWarriorEntity` (target goal predicate, `setTarget` filter, stand-down check; the Warlord inherits it and rallies its kin through `rallyAgainst`), `GoblinRaid.nearestDefender`, `GoblinRaids.maybeStartRaids`, `Persuasion.dc(player, target[, base])` (the hook for dialogue checks).
- Higher-tier perks: also in `TierEffects` (`earlyRaidWarning`, `raidLootRolls`, `innkeeperFavours`, `trustedWithBarrels`, `dwarfFriend`, the constants `EARLY_HORN_TICKS`, `DWARF_ALLY_RANGE`, `ALLY_FIGHT_TICKS`). `GoblinRaid.lookouts` (first tick of the countdown with defenders; saved as `FirstWaveDelay`/`LookoutsChecked`) and the loot loop in `GoblinRaid.win`; `InnkeeperEntity` (`honoredOffer`, saved as `HonoredOffer` and restocked daily, shown at the end of the list only to an Honored customer; `giveFreeRoom` stamps a Room Key and calls `TavernRooms.markRented`, so it's one a day like a bought one); `SettlementGrudges` `DwarfRules.judge` (barrels `TRUSTED`); `ai/goal/DwarfFriendGoal` (target goal, priority 2); `MountainDwarfEntity.interactMob` (identification through `Identify.reveal(..., "dwarf")`); `GoblinCampLaw` (a `SettlementGrudges.Rules` for chests inside a `GoblinCampPiece`, witnesses every goblin within 24 blocks) calling `GoblinWarriorEntity.provokeByTheft`, whose grudge counts as provoked in `TierEffects.provoked` through the `faction/Provokable` interface.
- Tests: `devscripts/rep-perks.txt` (innkeeper round and free room, barrels, identification, dwarves joining a fight, the goblin camp chest at the dev world's camp, -408 71 1103), `rep-perks-raid.txt` (lookouts and double loot at the dev world's hobbit village), `devscripts/rep-tiers.txt` (prices, refusals, flee, gifts, dwarf aggro, witness range, goblin targeting, Persuasion DC; the Persuasion part needs a test data pack faction `dndtest:villagers` with `"members": "minecraft:villager"` in the world), `rep-tiers-raid.txt` (raiders at Neutral and Hostile), `rep-tiers-barter.txt` (refusals and second rolls), `innkeeper-honored-dm.txt` (the Honored round after a rented room, and gone in DM mode), `devscripts/faction-rep.txt` (kills, cap, Warlord, raid, death), `faction-rep-relog.txt`, `faction-rep-sources.txt` (trades, bounties, barter, hoard, King, betrayal, decay).
