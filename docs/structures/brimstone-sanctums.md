# Brimstone Sanctums
The exiles' chapel-town in the Nether: a walled round platform of basalt and blackstone on pillars above the crimson forests and soul sand valleys, with the Pact Hall, the Brass Market, an Alchemist's den and the Refuge with its respawn anchors. [Tiefling Exiles and the Pactbroker](../mobs/tieflings.md) live here. Sanctums are the Tiefling home (see [Racial homes](../races/racial-homes.md)).

![A Brimstone Sanctum from above: the Pact Hall's crimson roof at the back, the Refuge on the left, the three market stalls on the right and the wall round it all](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/brimstone-sanctums/seedB-top.png)

## How it works
Every sanctum is the same plan, turned so its gate faces one of the four directions (local layout with the gate to the south):

| Piece | What it is |
|-|-|
| Platform | A disc of radius 18, two blocks thick (blackstone, polished blackstone, smooth basalt, a few gilded and cracked blocks on top; basalt under it), with everything up to 15 blocks above it cut away. Gaps of up to 3 blocks under it are filled solid; elsewhere it stands on basalt pillars every 6 blocks, down to the Nether floor. A polished blackstone road runs from the gate to the Pact Hall, with a soul campfire brazier either side. |
| Gate and wall | A polished blackstone brick wall 5 high (4 of bricks, a wall course on top) round radius 17, with chiselled posts and soul lanterns every 45°, and a three-wide gate between chiselled pillars under a raised iron-bar portcullis |
| Pact Hall (hearth) | At the back: an 11 by 12 nave of crimson planks between polished blackstone brick piers, red glass windows, a gabled crimson roof with a blackstone ridge. Inside: crimson pews either side of the aisle, two soul-fire chandeliers, an altar of crying obsidian on a gilded step with red and black candles, a lectern before it, and behind it the offering chest between crying obsidian pillars. The Pactbroker keeps to the hall. |
| Brass Market | Three stalls down the east side: crimson fence posts, an orange, yellow or red wool awning with a gold block on top, a hanging soul lantern, a blackstone counter facing the square and two barrels of wares (one filled from the market table) |
| Alchemist's den | West of the square: a squat blackstone workshop with a nether brick floor and roof and iron-bar windows. Brewing stands, a water cauldron and a cauldron along the back wall, a brewing stand on a magma block, barrels, a crafting table and the den's chest. Outside its south wall, a row of nether wart on soul sand. |
| Refuge | South-west of the square: a dormitory of blackstone and crimson planks with 4 respawn anchors along the back wall, a red carpet before each, a chest and a barrel. Beds explode in the Nether, so the anchors are the beds. They're generated empty. |

### People
4 to 6 Tiefling Exiles round the square when the sanctum generates, and the Pactbroker in the Pact Hall. Exiles spawn naturally on the platform later, up to 8 Tieflings within 48 blocks. No monsters spawn inside a sanctum's box. Piglins and zombified piglins wander in as anywhere else: the Exiles don't hunt them, so the sanctum stays neutral ground. See [Tieflings](../mobs/tieflings.md).

### Sanctum law
Neutral to everyone. Every Tiefling within 16 blocks (24 if you're Unfriendly or worse with the Brimstone Covenant) who can see you takes offence, and the Exiles turn on you for 30-50 s, if you open or break a chest or barrel anywhere in the sanctum. Each seen offence costs 40 standing with the Brimstone Covenant. Striking any Tiefling brings every Exile within 16 blocks down on you, and sets you alight (Hellish Rebuke).

A Tiefling's first offence each in-game day is let off with "The exiles look away, this once." (kin trust).

### The Refuge's anchors
Anyone may charge a Refuge anchor with glowstone and bind to it, as with any respawn anchor. A Tiefling binds for free: using an empty Refuge anchor fills it to 4 charges first ("The anchor kindles for you: 4 charges."), then the anchor sets their spawn. After that, the anchor they're bound to refills to 4 charges once every in-game day for them, whenever they use it, die, or come near it (it's checked every 10 s while its chunk is loaded). Used up the same day, it says "The anchor has given you its charges today."

### Loot
Every sanctum table also rolls 4% for an ancient debris and 5% for one piece of the Wither set (helmet, chestplate, leggings or boots).

| Loot table | Found in | Contents |
|-|-|-|
| `chests/brimstone_sanctum_altar` | the offering chest behind the Pact Hall altar | 4-6 of: 2-6 gold ingots, 6-16 quartz, 2-6 blaze powder, an enchanted book, 1-3 crying obsidian, a gold block, 1-2 ghast tears, 2-5 emeralds; plus a 60% [magic loot](../systems/magic-loot.md) roll: Uncommon (40) / Rare (50) / Very Rare (10) |
| `chests/brimstone_sanctum_den` | the den's chest | 3-6 of: gold, gold nuggets, quartz, blaze powder, an enchanted book, nether wart, glass bottles, magma cream, fermented spider eyes, glowstone dust, a Potion of Fire Resistance, a ghast tear; 12% magic loot: Uncommon (80) / Rare (20) |
| `chests/brimstone_sanctum_market` | one barrel per stall | 3-5 of: gold, gold nuggets, quartz, blaze powder, an enchanted book, leather, string, crimson fungus, magma cream, emeralds; 5% an Uncommon magic item |
| `chests/brimstone_sanctum_refuge` | the Refuge chest | 2-5 of: gold, gold nuggets, quartz, blaze powder, an enchanted book, cooked porkchops, 1-4 glowstone (to charge an anchor), red candles, crimson fungus; 5% an Uncommon magic item |

The offering chest is [locked](../systems/d20-skill-checks.md#lockpicking-rogue) at DC 15 and the den's chest at DC 10; the Refuge chest isn't locked. Opening any of them where a Tiefling can see is an offence.

## For your race
Sanctums are the Tiefling home. A Tiefling gets:

- a "Welcome home, Tiefling" title on arriving
- Regeneration I in the Pact Hall while no hostile mob is within 16 blocks
- **fire immunity** anywhere in the sanctum: no damage from fire, lava, burning or magma, and you're put out
- free binding to a Refuge anchor, which refills every day
- 25% off the Pactbroker's trades
- Kin stock: the Pactbroker sells Tieflings the [Infernal Regalia](../items/armor.md#racial-armor) set (helmet 12, chestplate 20, leggings 18, boots 10 before kin prices); nobody else sees those trades
- kin trust (one warning a day)
- a Friendly (150) start with the Brimstone Covenant

## Where to find it
Crimson Forests and Soul Sand Valleys in the Nether. Sanctums use a spacing of 20 chunks and a separation of 7. The Nether has a roof, so heightmaps are no use; the site check reads the noise columns instead:

- the middle column needs a floor between y 40 and 100 with 18 blocks of open air above it (up to 3 floors are tried, lowest first)
- 56 columns on rings of 5, 10, 14 and 18 blocks round it must all have solid ground within 19 blocks below the platform, with no lava on the way and none of it at or below y 39, so a sanctum never stands over the lava sea
- the same columns must have nothing solid 6 to 14 blocks above the platform (the buildings' height), so it never clips the ceiling; lower bumps are cut away
- at most 5 of the columns may be outside a sanctum biome

## Commands
Run these in the Nether (e.g. `/execute in minecraft:the_nether run ...`).

- `/locate structure dndclasses:brimstone_sanctum`
- `/dndhome sanctum [hall|offering|anchor|market|den|gate|top|side]` (operators): teleports you to that spot of the nearest sanctum, looking at it. `top` is as high above the square as the cavern allows; `side` is 40 blocks off to one side.
- `/dndhome sanctumsurvey [grid] [spacing]` (operators): finds the nearest sanctum from each point of a grid round you (default 3 x 3, 1500 blocks apart), generates it, and checks it's in a crimson forest or soul sand valley with all 5 piece kinds, that no lava lies under any platform column (through its pillars, 24 blocks down), and that rock sits right above the cleared space over at most 15% of the platform. Each sanctum logs `[SanctumSurvey] sanctum at X Y Z: biome ..., 7 pieces (5 kinds), lava under 0 of 1085 columns, cut ceiling over N: PASS` (or FAIL), then a summary.
- `/dndhome pieces` lists every piece of the sanctum you're in.

## Known limitations
- Huge fungi grow after the sanctum is built, so a crimson or warped fungus next to the wall may hang its cap over it.
- The cleared space is a flat-topped cylinder: where the cavern roof dips to within 15 blocks of the platform near the wall, it's cut flat.
- Goblins don't raid sanctums, and there are no Covenant bounties or quests yet.
- Hellish Resistance (half fire damage everywhere, see [Passive racial traits](../races/passive-traits.md)) still applies outside; inside a sanctum a Tiefling is fully immune.

## For developers
- `world/gen/sanctum/`. `BrimstoneSanctumStructure` finds the site with `NetherColumns` (`ChunkGenerator.getColumnSample`, cached per column: `solid`, `clear`, `groundBelow`, `biomeAllowed`) and places the pieces (`plan`). `BrimstonePiece` is the shared base, laid out like the Orc Stronghold's (local a/b round the centre, y 0 the platform top, states written for a south gate and rotated, a per-piece seed, the sanctum key), with the loot ids and a `Builder` (`foundation`, `inChunkGap`, `container`, `chandelier`, `tiefling`). Pieces: `PlatformPiece` (platform, pillars, wall, gate, braziers, Exiles), `PactHallPiece` (`altar`, `offering`, `broker`), `MarketPiece` (one per stall), `DenPiece`, `RefugePiece` (`anchors`, `beforeAnchor`). `BrimstoneSanctumStructures` registers them.
- `world/gen/sanctum/SanctumRefuge`: the Refuge anchor (a `UseBlockCallback` before vanilla's anchor use, `AFTER_DEATH`, and a 200-tick check) and fire immunity (`ALLOW_DAMAGE` on `#minecraft:is_fire`). Persistent data: `DndSanctumAnchorDay`. Logs `[BrimstoneSanctum] Refilled Refuge anchor ...`.
- Data: `worldgen/structure/brimstone_sanctum.json` (step `surface_structures`, Exiles in the `creature` override for pieces, an empty `monster` list for the whole box), `worldgen/structure_set/brimstone_sanctums.json` (salt 1468203759), `#dndclasses:has_structure/brimstone_sanctum`, `#dndclasses:brimstone_sanctums`, `#dndclasses:faction/brimstone_covenant` (structure and entity tags), `loot_tables/chests/brimstone_sanctum_*.json`, `factions/brimstone_covenant.json`, `dialogue/tiefling_pactbroker.json`.
- `RacialHomes.BRIMSTONE_SANCTUM` (hearth: `BrimstoneSanctumStructures.PACT_HALL`), `SettlementGrudges.BRIMSTONE_COVENANT` (`entity/SanctumLaw`), `Lockpicking` (the offering chest at DC 15, the Refuge chest unlocked), `MagicLoot`, `ModSpawns.canExileSpawn` (on the platform inside the wall, at most 8 Tieflings within 48 blocks).
- Commands: `classes/Commands/SanctumCommands` (`/dndhome sanctum`, `/dndhome sanctumsurvey`).
- Devscripts: `brimstone-sanctum-survey.txt` (`/locate`, `/dndhome sanctumsurvey`, aerial, side and close-up screenshots, NPC counts; run it with `-PdevWorld=` on other seeds) and `brimstone-sanctum-check.txt` (the anchor's free binding and next-day refill, fire immunity in and out of the sanctum, the Pactbroker's Warlock, Alchemist and Tiefling stock, Hellish Rebuke and the fireball).
