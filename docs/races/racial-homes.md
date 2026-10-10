# Racial homes
Every [race](races.md) has a home settlement. Anyone can visit any settlement, but players of the home race get extra benefits there: a welcome, a hearth that heals, kin prices and kin trust. Halflings are at home in [Hobbit Villages](../structures/hobbit-villages.md) and Dwarves in [Dwarven Fortresses](../structures/dwarven-fortresses.md). Gnomes count the fortresses as home for prices until they get one of their own, and Humans count vanilla villages as home.

![A Halfling arriving at the Green Dragon inn: "Welcome home, Halfling", the welcome basket in the hotbar and Regeneration from the hearth](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/racial-homes/rh-halfling-welcome.png)

## How it works
Once a second the mod checks where each player is. A player is in a settlement while they're inside its overall box, or up to 16 blocks above it, so the lanes and lawns between buildings count too. Spectators get nothing.

### Bonuses for the home race
| Bonus | What it does |
|-|-|
| Welcome | A "Welcome home, &lt;race&gt;" title with the settlement's name. It shows again only after you've been away for 5 minutes. |
| First-visit gift | Some settlements give a gift the first time you visit each one (see below). Each settlement is remembered separately. |
| Hearth | In the hearth building you get Regeneration I, refreshed every 3 s (it lasts 5 s), while no hostile mob is within 16 blocks. |
| Kin prices | The settlement's merchants charge 25% less: the first price item's count × 0.75, rounded down, never below 1. It stacks with your [reputation tier](../systems/factions.md) price change: both are worked out from the base price and added together. |
| Kin trust | The guards let you off some offences (see each settlement). |
| Standing | You start Friendly with the settlement's [faction](../systems/factions.md) instead of Neutral. |

### Halfling: Hobbit Villages
| Bonus | Value |
|-|-|
| Hearth | The Green Dragon inn |
| Kin prices | The innkeeper's trades. For example, rabbit stew costs 1 emerald instead of 2, cake 2 instead of 3, and the innkeeper buys 15 wheat instead of 20. One-emerald trades stay at 1. A Halfling starts Friendly with the Hobbits, whose -10% stacks on top: the wheat trade is then 13. |
| Welcome basket | On your first visit to each village: 1 cake, 4 cookies and 2 Mugs of Ale |
| Hobbit gifts | A Halfling waits 2/5 of the usual wait for their reputation tier between a hobbit's gifts: 2 minutes instead of 5 at Neutral or Friendly, 1 instead of 2.5 at Honored or Exalted, 6 instead of 15 at Unfriendly |
| Kin trust | Hobbits never hold a grudge anyway |
| Standing | Hobbits of the Shire: 150 (Friendly) |

### Dwarf: Dwarven Fortresses
| Bonus | Value |
|-|-|
| Hearth | The great hall |
| Kin prices | A dwarf you hand a gold ingot rolls its barter table twice and gives you the rarer result: the one whose entry has the lower weight in `gameplay/dwarf_barter` |
| Kin trust | You can open the chests in the forge, barracks, mead hall and mine even while dwarves watch. Anywhere else (the treasury, the hall, a chest outside the fortress), and when you break a chest or a gold block, the first offence each in-game day only gets a warning growl: "kin or no, the hoard isn't yours". The next one that day angers them as usual. |
| Audience with the King | Right-click the Dwarf King with an empty hand. Once per in-game day he gives you 3 gold ingots or a random enchanted iron tool (pickaxe, axe, shovel or sword, enchanted at level 15). He won't see you while he's angry with you. |
| Standing | Mountain Dwarves: 150 (Friendly) |

**Gnomes** get the fortress's kin prices (the double barter roll) and start at 100 (Friendly) with the Mountain Dwarves. They get no welcome, hearth or kin trust there.

### Human: vanilla villages
The first time a Human opens a villager's trades, that villager gains 10 `minor_positive` gossip about them, which makes its trades a little cheaper. Each villager does this once per player. Villages have no hearth, welcome or kin trust.

## Where to find it
- [Hobbit Villages](../structures/hobbit-villages.md): `/locate structure dndclasses:hobbit_village`
- [Dwarven Fortresses](../structures/dwarven-fortresses.md): `/locate structure dndclasses:dwarven_fortress`

## Commands
- `/dndhome [player]` (operators): which racial home the player is in, the piece under their feet, and whether it's their own home and hearth.
- `/dndhome pieces` (operators): every piece of the settlement you're standing in, with its box. Use it to find a room such as the forge.
- `/dndrace set <player> <race>` switches race to try the bonuses (see [Races](races.md)).

## Configuration
- The bonuses follow the active race, so with the `dndRaces` gamerule off nobody gets them.
- Starting standings are the factions' `start_by_race` fields in `data/dndclasses/factions/hobbits.json` and `mountain_dwarves.json`.

## Known limitations
- The welcome's 5-minute timer is kept in memory, so you're welcomed again after the server restarts.
- The kin discount applies to every innkeeper, since they're all hobbits, even one summoned outside a village.
- Dwarven kin trust only covers opening chests in the four side rooms. Breaking those chests still counts as an offence.
- Racial gear (kin-only stock) and the other races' settlements are later tickets.

## For developers
- `world/gen/RacialHomes`: the registry. A `Home` has the structure id, home race, extra price-kin races, faction id, hearth piece type and an optional first-visit gift. `homeAt(world, pos)` returns a `Visit` (home plus `StructureStart`, with `pieceAt` and `inHearth`), `ownHomeAt(player)` only for the home race, `isHearth(player)` (a Safe Haven hook for rests), and `homeOfMember(npc)` maps an NPC to its home through `Reputation.factionOf`. A new settlement registers one `Home`.
- `world/gen/HomeBonuses`: the 1 s tick (welcome, first-visit gift, hearth), `giftCooldown` (used by `HobbitEntity`), `kingAudience` (used by `MountainDwarfEntity`, with an `onKingAudience` quest hook) and the Human villager gossip (`UseEntityCallback`, remembered as a `dndclasses.kin_greeted.<uuid>` command tag on the villager).
- `entity/KinPrices`: the kin discount, a separate special-price modifier. `InnkeeperEntity.prepareOffersFor` clears the special prices, then each modifier adds its share, so other modifiers such as reputation tiers stack with it. `KinPrices.barter` is the double roll for barterers. It reads entry weights from the loot table's JSON and logs `[KinPrices] Kin barter for ...`.
- `entity/SettlementGrudges` (was `DwarfGrudges`): per-settlement `Rules` (what's protected, witnesses, provoke, warn, kin-trust `judge`). `MOUNTAIN_DWARVES` keeps the old behaviour for everyone but Dwarves. `opened(player, pos)` is for containers opened another way (picked locks). `dailyWarning` stores the day in `DndKinTrust`. `D20.register()` must still run before `SettlementGrudges.register()`.
- `world/gen/StructureProximity.near(context, set, radius)`: the "is another structure's start nearby" check, taken from `GoblinCampStructure`, for later settlements to reuse.
- Player persistent data: `DndHomeBaskets` (start-chunk longs of visited settlements), `DndKingAudienceDay`, `DndKinTrust`.
- DevScript `slots` also logs a trade screen's offers with their adjusted prices.
- Devscripts: `racial-homes-recon.txt`, `racial-homes-village.txt` and `racial-homes-fortress.txt` (run them on a fresh copy of `run/`, since the daily warning and first visit are saved), plus `mountain-dwarf-grudge.txt`.
