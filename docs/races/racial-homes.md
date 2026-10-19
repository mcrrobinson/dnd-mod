# Racial homes
Every [race](races.md) has a home settlement. Anyone can visit any settlement, but players of the home race get extra benefits there: a welcome, a hearth that heals, kin prices and kin trust. Halflings are at home in [Hobbit Villages](../structures/hobbit-villages.md), Dwarves in [Dwarven Fortresses](../structures/dwarven-fortresses.md), Elves in [Elven Enclaves](../structures/elven-enclaves.md), Dragonborn in [Dragon Temples](../structures/dragon-temples.md), Half-Orcs in [Orc Strongholds](../structures/orc-strongholds.md), Tieflings in [Brimstone Sanctums](../structures/brimstone-sanctums.md) and Gnomes in [Cogsdeep](../structures/cogsdeep.md). Humans count vanilla villages as home.

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
| Kin stock | Racial gear only the home race can buy (see each settlement). Other players never see these trades. Humans don't get any kin stock from vanilla villages. |
| Kin trust | The guards let you off some offences (see each settlement). |
| Standing | You start Friendly with the settlement's [faction](../systems/factions.md) instead of Neutral (Gnomes start Honored with the Tinkers' Guild). |

### Halfling: Hobbit Villages
| Bonus | Value |
|-|-|
| Hearth | The Green Dragon inn |
| Kin prices | The innkeeper's trades. For example, rabbit stew costs 1 emerald instead of 2, cake 2 instead of 3, and the innkeeper buys 15 wheat instead of 20. One-emerald trades stay at 1. A Halfling starts Friendly with the Hobbits, whose -10% stacks on top: the wheat trade is then 13. |
| Welcome basket | On your first visit to each village: 1 cake, 4 cookies and 2 Mugs of Ale |
| Hobbit gifts | A Halfling waits 2/5 of the usual wait for their reputation tier between a hobbit's gifts: 2 minutes instead of 5 at Neutral or Friendly, 1 instead of 2.5 at Honored or Exalted, 6 instead of 15 at Unfriendly |
| Kin stock | The innkeeper sells Halflings the [Halfling Sling](../items/racial-weapons.md#halfling-sling) for 6 emeralds (4 at kin price) |
| Kin trust | Hobbits never hold a grudge anyway |
| Standing | Hobbits of the Shire: 150 (Friendly) |

### Dwarf: Dwarven Fortresses
| Bonus | Value |
|-|-|
| Hearth | The great hall |
| Kin prices | A dwarf you hand a gold ingot rolls its barter table twice and gives you the rarer result: the one whose entry has the lower weight in `gameplay/dwarf_barter` |
| Kin stock | Every barter also rolls `gameplay/dwarf_kin_stock`: 4% a [Dwarven Plate](../items/armor.md#racial-armor) piece, 2% a [Dwarven Waraxe](../items/racial-weapons.md#dwarven-waraxe) |
| Kin trust | You can open the chests in the forge, barracks, mead hall and mine even while dwarves watch. Anywhere else (the treasury, the hall, a chest outside the fortress), and when you break a chest or a gold block, the first offence each in-game day only gets a warning growl: "kin or no, the hoard isn't yours". The next one that day angers them as usual. |
| Audience with the King | Right-click the Dwarf King with an empty hand. Once per in-game day he gives you 3 gold ingots or a random enchanted iron tool (pickaxe, axe, shovel or sword, enchanted at level 15). He won't see you while he's angry with you. |
| Standing | Mountain Dwarves: 150 (Friendly) |

### Elf: Elven Enclaves
| Bonus | Value |
|-|-|
| Hearth | The Speaker's Hall up in the Heart Tree |
| Kin prices | The Speaker's and the Fletcher's trades. For example, a Power III book costs 10 emeralds instead of 14, a Power bow 4 instead of 6, Moonwater 2 instead of 3, and the Fletcher buys 24 sticks instead of 32. |
| Kin stock | The Speaker sells the [Elven Chain](../items/armor.md#racial-armor) set: helmet 10, chestplate 16, leggings 14, boots 8 emeralds (7, 12, 10 and 6 at kin price). The Fletcher's [Elven Longbow](../items/racial-weapons.md#elven-longbow) is for anyone, 12 instead of 16 for Elves |
| Moonwater | Fill a glass bottle at the Moonwell at night for free Moonwater, once a night |
| Kin trust | The first time each in-game day you break a block of the Heart Tree or a talan where an elf can see, you're let off with "The Warden frowns at you." Killing animals, fire and the Heart Tree chest are never forgiven. |
| Standing | Sylvan Court: 150 (Friendly) |

### Dragonborn: Dragon Temples
| Bonus | Value |
|-|-|
| Hearth | The sanctum with the three breath altars |
| Kin prices | The Elder's trades. For example, the Smite Dragons book costs 13 emeralds instead of 18, a lair map 9 instead of 12, and he buys 6 blaze powder instead of 8. |
| Kin stock | The Elder sells the [Draconic Scale](../items/armor.md#racial-armor) set: helmet 12, chestplate 20, leggings 18, boots 10 emeralds (9, 15, 13 and 7 at kin price) |
| Blessing of the Ancestors | Right-click the altar of your ancestry once per in-game day: Breath Weapon cooldown halved and +2 breath damage for 20 minutes |
| Rite of Rebirth | Right-click another ancestry's altar holding 10 diamonds to take that ancestry |
| Kin trust | None: the temple law holds for Dragonborn too |
| Standing | The Dragonsworn: 150 (Friendly) |

### Half-Orc: Orc Strongholds
| Bonus | Value |
|-|-|
| Hearth | The Great Longhouse |
| Rally | Strength I for 30 s each time you're welcomed |
| Kin prices | The Quartermaster's trades. For example, the Orcish Greataxe costs 10 emeralds instead of 14, a saddle 4 instead of 6, and he buys 24 rotten flesh instead of 32. |
| Kin stock | The Quartermaster sells the [Orcish Warplate](../items/armor.md#racial-armor) set: helmet 10, chestplate 16, leggings 14, boots 8 emeralds (7, 12, 10 and 6 at kin price). He trades with nobody else but players Blood-Sworn at his hold. |
| Trial of Strength | Win a non-lethal duel in the Fighting Pit to become Blood-Sworn at the hold (see [Orc Strongholds](../structures/orc-strongholds.md#trial-of-strength)) |
| Kin trust | None: opening the hoard or striking an orc angers the hold |
| Standing | The Bloodtusk Horde: 0 (Neutral), so the orcs tolerate you; everyone else starts Hostile |

### Tiefling: Brimstone Sanctums
| Bonus | Value |
|-|-|
| Hearth | The Pact Hall |
| Fire immunity | No damage from fire, lava, burning or magma anywhere in the sanctum |
| Refuge anchor | Bind to one of the Refuge's respawn anchors for free; it refills to 4 charges every in-game day for you |
| Kin prices | The Pactbroker's trades. For example, a ghast tear costs 2 emeralds instead of 3, the daily netherite scrap 18 instead of 24, and he buys 15 quartz instead of 20. |
| Kin stock | The Pactbroker sells the [Infernal Regalia](../items/armor.md#racial-armor) set: helmet 12, chestplate 20, leggings 18, boots 10 emeralds (9, 15, 13 and 7 at kin price) |
| Kin trust | The first time each in-game day you open or break a sanctum chest or barrel where a Tiefling can see, you're let off with "The exiles look away, this once." |
| Standing | The Brimstone Covenant: 150 (Friendly) |

### Gnome: Cogsdeep
| Bonus | Value |
|-|-|
| Hearth | The Commons at the foot of the spiral stair |
| Kin prices | Every Gnome Tinker's trades. For example, a spyglass costs 3 emeralds instead of 4, a comparator 2 instead of 3, and a Tinker buys 7 copper ingots instead of 10. A Gnome starts Honored with the Tinkers' Guild, whose -25% stacks on top: the spyglass is then 2. |
| Kin stock | Every Tinker sells Gnomes the [Gnomish Repeater](../items/racial-weapons.md#gnomish-repeater) (a crossbow with Quick Charge III and Multishot) for 14 emeralds and [Tinker's Goggles](../items/tinkers-goggles.md) for 10 (10 and 7 at kin price) |
| Kin trust | The first time each in-game day you open or break a burrow chest or barrel, or break a contraption, where a gnome or Sentinel can see, you're let off with "The Sentinel's gears grind, but it lets it pass." |
| Standing | The Tinkers' Guild: 450 (Honored); everyone else starts Friendly (150) |

Until Cogsdeep existed, Gnomes got the dwarven fortresses' kin prices and a Friendly start with the Mountain Dwarves; that interim home is gone.

### Human: vanilla villages
The first time a Human opens a villager's trades, that villager gains 10 `minor_positive` gossip about them, which makes its trades a little cheaper. Each villager does this once per player. Villages have no hearth, welcome or kin trust.

## Where to find it
- [Hobbit Villages](../structures/hobbit-villages.md): `/locate structure dndclasses:hobbit_village`
- [Dwarven Fortresses](../structures/dwarven-fortresses.md): `/locate structure dndclasses:dwarven_fortress`
- [Elven Enclaves](../structures/elven-enclaves.md): `/locate structure dndclasses:elven_enclave`
- [Dragon Temples](../structures/dragon-temples.md): `/locate structure dndclasses:dragon_temple`
- [Orc Strongholds](../structures/orc-strongholds.md): `/locate structure dndclasses:orc_stronghold`
- [Brimstone Sanctums](../structures/brimstone-sanctums.md) (in the Nether): `/execute in minecraft:the_nether run locate structure dndclasses:brimstone_sanctum`
- [Cogsdeep](../structures/cogsdeep.md): `/locate structure dndclasses:cogsdeep`

## Commands
- `/dndhome [player]` (operators): which racial home the player is in, the piece under their feet, and whether it's their own home and hearth.
- `/dndhome pieces` (operators): every piece of the settlement you're standing in, with its box and facing. Use it to find a room such as the forge.
- `/dndrace set <player> <race>` switches race to try the bonuses (see [Races](races.md)).

## Configuration
- The bonuses follow the active race, so with the `dndRaces` gamerule off nobody gets them.
- Starting standings are the factions' `start` and `start_by_race` fields in `data/dndclasses/factions/*.json` (for example `hobbits.json`, `mountain_dwarves.json`, `tinkers_guild.json`).

## Known limitations
- The welcome's 5-minute timer is kept in memory, so you're welcomed again after the server restarts.
- The kin discount applies to every innkeeper, since they're all hobbits, even one summoned outside a village.
- Dwarven kin trust only covers opening chests in the four side rooms. Breaking those chests still counts as an offence.

## For developers
- `world/gen/RacialHomes`: the registry. A `Home` has the structure id, home race, extra price-kin races (none now that the Gnomes' interim home at the fortresses is gone), faction id, hearth piece type and an optional first-visit gift. `homeAt(world, pos)` returns a `Visit` (home plus `StructureStart`, with `pieceAt` and `inHearth`), `visitOf(world, pos, home)` only if it's that home, `ownHomeAt(player)` only for the home race, `isHearth(player)` (a Safe Haven hook for rests), and `homeOfMember(npc)` maps an NPC to its home through `Reputation.factionOf`. A new settlement registers one `Home`.
- `world/gen/HomeBonuses`: the 1 s tick (welcome, first-visit gift, hearth), `giftCooldown` (used by `HobbitEntity`), `kingAudience` (used by `MountainDwarfEntity`, with an `onKingAudience` quest hook) and the Human villager gossip (`UseEntityCallback`, remembered as a `dndclasses.kin_greeted.<uuid>` command tag on the villager).
- `entity/KinPrices`: the kin discount, a separate special-price modifier. `NpcMerchant.preparePrices` (every settlement merchant) clears the special prices, then each modifier adds its share, so other modifiers such as reputation tiers stack with it. `KinPrices.barter` is the double roll for barterers. It reads entry weights from the loot table's JSON and logs `[KinPrices] Kin barter for ...`.
- `entity/SettlementGrudges` (was `DwarfGrudges`): per-settlement `Rules` (what's protected, witnesses, provoke, warn, kin-trust `judge`); all but the goblin camps' extend `GuardRules` (the faction's folk in witness range who can see you, guards provoked and the rest frowning, `kinWarning` for a home race's daily warning), with faction ids from `faction/FactionIds`. `MOUNTAIN_DWARVES` keeps the old behaviour for everyone but Dwarves; `SYLVAN_COURT` is the enclaves' Sylvan Law (`entity/SylvanLaw`), which adds the `HARM` and `FIRE` offences; `DRAGONSWORN` is the temples' law (`entity/TempleLaw`); `BRIMSTONE_COVENANT` the sanctums' (`entity/SanctumLaw`); `TINKERS_GUILD` Cogsdeep's (`entity/TinkersLaw`). `witness` logs `[Grudges] <rules> <offence> at <pos> by <player>: <n> witness(es), <verdict>`. `forgive` calms every `SettlementGuard`: dwarves, Elf Wardens, Dragonborn Templars, Tiefling Exiles, orcs and Clockwork Sentinels. `opened(player, pos)` is for containers opened another way (picked locks). `dailyWarning` stores the day in `DndKinTrust`. `D20.register()` must still run before `SettlementGrudges.register()`.
- `entity/KinStock`: a merchant's kin-only offers, kept in their own list (NBT `KinOffers`) and appended to the merchant's offers (by its `NpcMerchant`) only while a customer of the home race (`KinStock.isKin`, home race only, not the extra price-kin races) trades. A `KinStock` can also take another audience and NBT key (the Pactbroker's Warlock and Alchemist stock). `entity/KinStockOffers` holds each settlement's list (`cogsdeep` for the Gnome Tinkers). See [Racial weapons](../items/racial-weapons.md#for-developers).
- `world/gen/StructureProximity.near(context, set, radius)`: the "is another structure's start nearby" check, taken from `GoblinCampStructure`, for later settlements to reuse. `nearGenerating` also runs the other structure's site check, so only starts that really generate count (the Dragon Temple's lair rule).
- Player persistent data: `DndHomeBaskets` (start-chunk longs of visited settlements), `DndKingAudienceDay`, `DndKinTrust`.
- DevScript `slots` also logs a trade screen's offers with their adjusted prices.
- Devscripts: `racial-homes-recon.txt`, `racial-homes-village.txt` and `racial-homes-fortress.txt` (run them on a fresh copy of `run/`, since the daily warning and first visit are saved), plus `mountain-dwarf-grudge.txt`.
