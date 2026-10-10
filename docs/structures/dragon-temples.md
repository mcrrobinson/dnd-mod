# Dragon Temples
The Shrine of the Three Breaths: a terrace cut into a mountain slope near a dragon lair, reached by a grand stair, with a courtyard of three dragon statues, a sanctum of breath altars, monks' cells, a library and a training yard. [Dragonborn](../mobs/dragonborn.md) live here. Temples are the Dragonborn home (see [Racial homes](../races/racial-homes.md)).

## How it works
Every temple is the same plan, turned to climb into its slope:

| Piece | What it is |
|-|-|
| Terrace | 39 wide and 28 deep, 8-12 blocks above the stair foot. The slope is cut away 13 blocks above the floor and built up beneath it with cobbled deepslate. Polished deepslate floor, a polished blackstone parapet with gilded posts along the open edges, braziers at the front corners. |
| Grand stair | 5 wide, one step up per block from the foot to the courtyard: deepslate tile steps, polished blackstone edges, balustrades with a gilded post and lantern every fourth step, braziers either side of the foot |
| Courtyard | 21 by 15 at the top of the stair. A gilded blackstone path runs to the sanctum door and parts round the middle statue. Soul-lantern posts at the corners. |
| Statues | Three dragons, each 10 blocks tall on a gilded plinth, sitting up with their wings half spread: **Ember** (red nether brick, magma eyes, red horns) in the middle of the path, **Frost** (quartz, packed ice, snow, blue ice eyes) and **Storm** (lapis, prismarine, sea lantern eyes, lightning-rod horns) either side |
| Sanctum (hearth) | 21 by 13 at the back, cut into the mountain: pillars of basalt, soul lanterns, an enchanting table ringed with 26 bookshelves (open at the front), the three breath altars along the back wall under red, light blue and blue banners, and the reliquary behind the Ember altar |
| Cloister | Down one side: six small rooms off a corridor (4-6 monks' cells with a red bed, a chest and a candle; the rest storerooms with barrels), a cross-hall with the door onto the courtyard, and the library (about 36 bookshelves, two lecterns, a reading table) |
| Training yard | Down the other side: coarse dirt and gravel, three targets on hay bales, three armour stands in the Knight set (one complete, two partial), a grindstone, an anvil and a barrel |

The breath altars:

| Altar | Plinth | On top |
|-|-|-|
| Ember | red nether brick | a campfire |
| Frost | blue ice | a soul campfire |
| Storm | waxed cut copper | a lightning rod |

### People
5 to 7 Dragonborn Templars when the temple generates (two in the courtyard and sometimes a third at the top of the stair, one at the sanctum door, one in the cloister, one or two in the yard) and the Dragonborn Elder in the sanctum. Templars spawn naturally on the terrace later, up to 7 within 48 blocks. No monsters spawn inside a temple's box. See [Dragonborn](../mobs/dragonborn.md).

### Temple law
Neutral to everyone. Every Dragonborn within 16 blocks (24 if you're Unfriendly or worse with the Dragonsworn) who can see you takes offence, and the Templars turn on you for 30-50 s, if you:

- break a block of a statue, or an altar's plinth, fire or rod
- open the reliquary (any chest in the sanctum)

Each seen offence costs 40 standing with the Dragonsworn. There's no kin trust: Dragonborn are held to the same law. Striking any Dragonborn brings every Templar within 16 blocks down on you. Helping to drive off a goblin raid on the temple forgives you.

### Shrines (Dragonborn only)
- **Blessing of the Ancestors**: right-click the altar of your own ancestry. Once per in-game day you're blessed for 20 minutes: your [Breath Weapon](../races/breath-weapon.md)'s cooldown is halved (60 s becomes 30 s) and it deals +2 damage. Using it again the same day says "Your ancestors have already blessed you today."
- **Rite of Rebirth**: right-click another ancestry's altar while holding 10 diamonds in your main hand. The diamonds are used up and your ancestry becomes the altar's (Ember, Frost or Storm), with its breath and snout colour. Any blessing ends. Without the diamonds the altar tells you the price. This and `/dndrace set` are the only ways to change ancestry.
- Other races get "The altar stays cold to you." and the altar does nothing.

### Loot
| Loot table | Found in | Contents |
|-|-|-|
| `chests/dragon_temple_reliquary` | the reliquary behind the Ember altar | 3-5 of: 2-6 gold ingots, 4-12 gold nuggets, 1-2 diamonds, a Smite Dragons or Protection book, a golden apple, bottles o' enchanting; plus 4% each of the Staff of Fire, Staff of Ice and Staff of Lightning |
| `chests/dragon_temple_cell` | cell chests and storeroom barrels | bread, mutton, potatoes, dried kelp, candles, books, paper, ink, a book and quill; always 1 emerald |

The reliquary is [locked](../systems/d20-skill-checks.md#lockpicking-rogue) at DC 15, and opening it where a Dragonborn can see is an offence. Cell chests aren't locked.

## For your race
Temples are the Dragonborn home. A Dragonborn gets:

- a "Welcome home, Dragonborn" title on arriving
- Regeneration I in the sanctum while no hostile mob is within 16 blocks
- 25% off the Elder's trades
- Kin stock: the Elder sells Dragonborn the [Draconic Scale](../items/armor.md#racial-armor) set (helmet 12, chestplate 20, leggings 18, boots 10 before kin prices); nobody else sees those trades
- the Blessing of the Ancestors and the Rite of Rebirth
- a Friendly (150) start with the Dragonsworn

## Where to find it
Jagged Peaks, Stony Peaks, Frozen Peaks, Snowy Slopes and Groves. Temples use a spacing of 20 chunks and a separation of 7, and only generate:

- where a dragon lair or frost lair really generates (its own peak check passes) within 10 chunks of the temple's start chunk, so every temple has a lair nearby
- at least 4 chunks from any dwarven fortress that really generates
- on a slope: a stair foot with open ground below it, and the ground under the terrace 6-18 blocks higher on average, within 12 blocks above or 14 below the terrace floor everywhere

Goblins sometimes [raid](../systems/goblin-raids.md) temples. The war party climbs up from the valley to the courtyard and the Templars rush out to meet it.

## Commands
- `/locate structure dndclasses:dragon_temple`
- `/dndhome temple [ember|frost|storm]` (operators): teleports you into the nearest temple's sanctum, two blocks in front of that altar and looking at it
- `/dndhome templesurvey [grid] [spacing]` (operators): finds the nearest temple from each point of a grid round you (default 3 x 3, 2000 blocks apart) and checks a dragon or frost lair starts within 10 chunks of it. Each temple logs `[TempleSurvey] temple at X Z: dragon lair N chunks, frost lair M chunks: PASS` (or FAIL), then a summary.
- `/dndhome pieces` lists every piece of the temple you're in.

## Known limitations
- Killing lair dragons doesn't raise standing with the Dragonsworn yet: the dragons aren't a faction.
- The Elder's maps look for a lair within 100 chunks the first time someone opens his trades, which can stall the server for a moment. With no lair in range that map trade is left out.
- Steep mountainsides don't qualify as sites (the climb under the terrace must be at most 18 blocks), so temples sit on the gentler slopes.
- The Ember statue's horns are red nether brick walls: fire coral dies out of water.
- The slope is cut sheer at the back and sides of the terrace, and the foundation is a plain wall of cobbled deepslate down to the ground.

## For developers
- `world/gen/temple/`. `DragonTempleStructure` checks the biome, the lair rule (`StructureProximity.near` on both lair sets first, then `nearGenerating` once a site is found) and the fortress exclusion; `TempleTerrain` finds the site; `DragonTemplePlanner` places the pieces round a `Site` (foot, facing, rise). `TemplePiece` is the shared base (local coordinates facing north, rotated by vanilla, a per-piece seed). Pieces: `TerracePiece`, `GrandStairPiece`, `CourtyardPiece`, `StatuePiece` (one per statue, so a statue is a box), `SanctumPiece` (`altarOf(state)`, `altarTop`, `inFrontOf`, `reliquary`), `CloisterPiece`, `TrainingYardPiece`. `DragonTempleStructures` registers them.
- `TempleShrine`: the altar hook (`UseBlockCallback`), the `BreathWeapon.MODIFY` listener, `isBlessed`, quest hooks `onBlessing` and `onRebirth`. Persistent data: `DndShrineDay`, `DndShrineBlessedUntil`. Logs `[DragonTemple] ...`.
- `world/gen/StructureProximity.nearGenerating(context, set, radius)` / `nearestGenerating`: like `near`, but runs the other structure's site check, so only starts that really generate count.
- Data: `worldgen/structure/dragon_temple.json` (Templars in the `creature` override for pieces, an empty `monster` list for the whole box), `worldgen/structure_set/dragon_temples.json`, `#dndclasses:has_structure/dragon_temple`, `#dndclasses:dragon_temples`, `#dndclasses:dragon_temple_maps/dragon_lair` and `.../frost_lair` (the Elder's map targets), `loot_tables/chests/dragon_temple_*.json`, `loot_tables/gameplay/goblin_raid_dragon_temple.json`, `factions/dragonsworn.json`, `dialogue/dragonborn_elder.json`.
- `RacialHomes.DRAGON_TEMPLE` (hearth: `DragonTempleStructures.SANCTUM`), `Settlement.Kind.TEMPLE` (rally in the courtyard, raiders from the valley), `SettlementGrudges.DRAGONSWORN` (`entity/TempleLaw`), `Lockpicking` (the reliquary at DC 15, cells unlocked).
- Devscripts: `dragon-temple-survey.txt` (both `/locate`s, `/dndhome templesurvey`, screenshots, NPC counts) and `dragon-temple-shrine.txt` (blessing, rebirth, the price, other races, Templar breath at a zombie).
