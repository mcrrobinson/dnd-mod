# Dragon Temples
The Shrine of the Three Breaths: a terrace cut into a mountain slope near a dragon lair, reached by a grand stair, with a courtyard of three dragon statues, a sanctum of breath altars, monks' cells, a library and a training yard. [Dragonborn](../mobs/dragonborn.md) live here. Temples are the Dragonborn home (see [Racial homes](../races/racial-homes.md)).

## How it works
Every temple is the same plan, turned to climb into its slope:

| Piece | What it is |
|-|-|
| Terrace | 39 wide and 28 deep, 8-12 blocks above the stair foot. The slope is cut away 13 blocks above the floor and built up beneath it with cobbled deepslate. Polished deepslate floor, a polished blackstone parapet with gilded posts along the open edges, Ember incense braziers at the front corners. |
| Grand stair | 5 wide, one step up per block from the foot to the courtyard: deepslate tile steps, polished blackstone edges, balustrades with a lantern every fourth step on an Ancestor Relief facing the stair (gilded at the foot), Ember incense braziers either side of the foot |
| Courtyard | 21 by 15 at the top of the stair. A gilded blackstone path runs to the sanctum door and parts round the middle statue. Incense-brazier posts at the corners (Ember at the front; Frost and Storm at the back, on their statues' sides), and paper lanterns on lacquered fence posts either side of the path at the stair and at the sanctum door. |
| Statues | Three dragons, each 10 blocks tall on a plinth of their colour's Dragonscale Tiles with gilded corners, sitting up with their wings half spread: **Ember** (red nether brick, magma eyes, red horns) in the middle of the path, **Frost** (quartz, packed ice, snow, blue ice eyes) and **Storm** (lapis, prismarine, sea lantern eyes, lightning-rod horns) either side |
| Sanctum (hearth) | 21 by 13 at the back, cut into the mountain, under a stepped roof of Ember Dragonscale Tiles (three rings of stairs and a flat top) with a Frost stripe over the Frost altar and a Storm stripe over the Storm altar: basalt pillars standing on Ancestor Reliefs, Temple Lacquer beams across between them, soul lanterns, an enchanting table ringed with 26 bookshelves (open at the front), the three breath altars along the back wall under red, light blue and blue banners bearing the gold [Three Breaths](../items/banner-patterns.md), and the reliquary behind the Ember altar |
| Cloister | Down one side, under the same Ember Dragonscale roof: six small rooms off a corridor (4-6 monks' cells with a Temple Lacquer floor, a red bed, a chest and a candle; the rest storerooms with barrels), 12 prayer wheels set into the corridor walls either side of the doors, paper lanterns overhead, lacquered beams across, an Ancestor Relief at each end of the corridor, a cross-hall with the door onto the courtyard, and the library (28 bookshelves in Temple Lacquer frames, two lecterns, a lacquered reading table) |
| Training yard | Down the other side: coarse dirt and gravel, three targets on hay bales, three sparring posts, three armour stands in the Knight set (one complete, two partial), a grindstone, an anvil and a barrel |

![The sanctum's breath altars under their Three Breaths banners](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/racial-banners/temple-sanctum-after.png)

The breath altars, each on a gilded step with its tiles' stairs either side, an incense brazier of its colour at each front corner and Breath-Scorched Stone of its colour on the floor in front (a 3 by 2 patch with a ragged edge):

| Altar | Plinth | On top |
|-|-|-|
| Ember | Ember Dragonscale Tiles | a campfire |
| Frost | Frost Dragonscale Tiles | a soul campfire |
| Storm | Storm Dragonscale Tiles | a lightning rod |

Temples generated before the Dragonscale plinths keep their red nether brick, blue ice and cut copper ones, and those still work as altars.

### Building blocks
The temple is built from its own blocks, all in the **DnD Building Blocks** creative tab. Each drops itself when mined.

| Block | Shapes | What it is | Recipe |
|-|-|-|-|
| Ember, Frost and Storm Dragonscale Tiles | block, stairs, slab | overlapping rounded scales: deep red to orange, pale blue to white, teal to dark blue with yellow glints. Stone (pickaxe), like deepslate tiles. | 8 deepslate tiles round a red, light blue or cyan dye make 8 |
| Temple Lacquer Planks | planks, stairs, slab, fence | glossy red-lacquered boards with black joints. Wood (axe), like dark oak. | 8 dark oak planks round a red dye make 8 |
| Ember-, Frost- and Storm-Scorched Stone | block | basalt with red glaze and glowing cracks, frost rime and pale blue cracks, or branching yellow-white scorch lines. Like smooth basalt. | 8 smooth basalt round a blaze powder, snowball or copper ingot make 8 |
| Ancestor Relief | block that faces you | carved grey-black stone with a dragon's head in profile and a gold eye. Like chiseled deepslate. | 4 chiseled deepslate round a gold nugget make 4 |
| Prayer Wheel | | a red drum with gold script bands between brass caps. Right-click it and it turns for 3 seconds (a creak and a chime; right-clicking again keeps it going). Pickaxe. | copper ingot, red wool, copper ingot in a column |
| Ember, Frost and Storm Incense Brazier | | a dark bronze bowl on legs with embers glowing in its colour, giving off smoke of that colour. Light 13. Doesn't burn or cook. Pickaxe. | 4 copper ingots, a coal and a red, light blue or cyan dye (`ICI` / `IDI`) |
| Paper Lantern | | a cream paper lantern with red caps. Stands on a block or hangs under one (place it against a ceiling), like a lantern. Light 14. | string over paper, torch, paper over paper |
| Sparring Post | block that faces you | a straw-and-rope wrapped post with a padded front. Hitting it doesn't break it: the action bar shows what the swing would deal, e.g. "Sparring Post: 7.0 damage (100% swing)". Sneak to break it. Axe. | white wool, hay bale, oak fence in a column |

The Sparring Post works the damage out as vanilla does against an unarmoured mob: your attack damage (weapon, Strength, class and gear modifiers), the weapon's damage enchantments (Sharpness, not Smite or Bane), how charged the swing is (counted from your last hit on a post) and a jump crit (x1.5). It doesn't count effects that only trigger on hitting a creature (Sneak Attack, smites and other on-hit extras). Holding the button down counts a hit every 5 ticks at most.

The Elder sells 16 Ember, Frost or Storm Dragonscale Tiles for 3 emeralds each (2 for Dragonborn).

### People
5 to 7 Dragonborn Templars when the temple generates (two in the courtyard and sometimes a third at the top of the stair, one at the sanctum door, one in the cloister, one or two in the yard) and the Dragonborn Elder in the sanctum. Templars spawn naturally on the terrace later, up to 7 within 48 blocks. No monsters spawn inside a temple's box. See [Dragonborn](../mobs/dragonborn.md).

### Temple law
Neutral to everyone. Every Dragonborn within 16 blocks (24 if you're Unfriendly or worse with the Dragonsworn) who can see you takes offence, and the Templars turn on you for 30-50 s, if you:

- break a block of a statue, or an altar's plinth, fire or rod (a Dragonscale Tiles block in the sanctum counts as a plinth)
- open the reliquary (any chest in the sanctum)

Each seen offence costs 40 standing with the Dragonsworn. There's no kin trust: Dragonborn are held to the same law. Striking any Dragonborn brings every Templar within 16 blocks down on you. Helping to drive off a goblin raid on the temple forgives you.

### Shrines (Dragonborn only)
- **Blessing of the Ancestors**: right-click the altar of your own ancestry. Once per in-game day you're blessed for 20 minutes: your [Breath Weapon](../races/breath-weapon.md)'s cooldown is halved (60 s becomes 30 s) and it deals +2 damage. Using it again the same day says "Your ancestors have already blessed you today."
- **Rite of Rebirth**: right-click another ancestry's altar while holding 10 diamonds in your main hand. The diamonds are used up and your ancestry becomes the altar's (Ember, Frost or Storm), with its breath and snout colour. Any blessing ends. Without the diamonds the altar tells you the price. This and `/dndrace set` are the only ways to change ancestry.
- Other races get "The altar stays cold to you." and the altar does nothing.

### Loot
| Loot table | Found in | Contents |
|-|-|-|
| `chests/dragon_temple_reliquary` | the reliquary behind the Ember altar | 3-5 of: 2-6 gold ingots, 4-12 gold nuggets, 1-2 diamonds, a Smite Dragons or Protection book, a golden apple, bottles o' enchanting; plus 4% each of the Staff of Fire, Staff of Ice and Staff of Lightning; plus one [magic loot](../systems/magic-loot.md) roll: Rare (90) / Very Rare (10) |
| `chests/dragon_temple_cell` | cell chests and storeroom barrels | bread, mutton, potatoes, dried kelp, candles, books, paper, ink, a book and quill; always 1 emerald; 8% an Uncommon [magic item](../systems/magic-loot.md) |

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

- where a dragon lair or frost lair really generates (its own peak check passes, and its grid slot isn't in the lair set's exclusion zone, e.g. dragon lairs within 6 chunks of a dwarven fortress grid slot) within 10 chunks of the temple's start chunk, so every temple has a lair nearby
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
- Building blocks: the families `EMBER_/FROST_/STORM_DRAGONSCALE_TILES`, `TEMPLE_LACQUER_PLANKS` and `EMBER_/FROST_/STORM_SCORCHED_STONE` in `ModBlockFamilies` (with `dragonscale(kind)` and `scorchedStone(kind)`), and the one-off blocks in `ModTempleBlocks` (`classes/Blocks/Temple/`: `AncestorReliefBlock`, `PrayerWheelBlock` with its `onSpin` hook for a later blessing, `IncenseBrazierBlock`, `PaperLanternBlock`, `SparringPostBlock`). `TemplePiece.roof(...)` builds the stepped tile roofs. Resources: `tools/block_families.py` (families), `tools/temple_blocks.py` (the one-off blocks), `tools/temple_block_textures.py` (every texture). Logs `[TempleBlocks] <player> spun the prayer wheel at ...` and `[SparringPost] <player> hit for <damage> (<charge>% charge)`.
- `TempleShrine`: the altar hook (`UseBlockCallback`), the `BreathWeapon.MODIFY` listener, `isBlessed`, quest hooks `onBlessing` and `onRebirth`. Persistent data: `DndShrineDay`, `DndShrineBlessedUntil`. Logs `[DragonTemple] ...`.
- `world/gen/StructureProximity.nearGenerating(context, set, radius)` / `nearestGenerating`: like `near`, but runs the other structure's site check, so only starts that really generate count.
- Data: `worldgen/structure/dragon_temple.json` (Templars in the `creature` override for pieces, an empty `monster` list for the whole box), `worldgen/structure_set/dragon_temples.json`, `#dndclasses:has_structure/dragon_temple`, `#dndclasses:dragon_temples`, `#dndclasses:dragon_temple_maps/dragon_lair` and `.../frost_lair` (the Elder's map targets), `loot_tables/chests/dragon_temple_*.json`, `loot_tables/gameplay/goblin_raid_dragon_temple.json`, `factions/dragonsworn.json`, `dialogue/dragonborn_elder.json`.
- `RacialHomes.DRAGON_TEMPLE` (hearth: `DragonTempleStructures.SANCTUM`), `Settlement.Kind.TEMPLE` (rally in the courtyard, raiders from the valley), `SettlementGrudges.DRAGONSWORN` (`entity/TempleLaw`), `Lockpicking` (the reliquary at DC 15, cells unlocked).
- Devscripts: `dragon-temple-survey.txt` (both `/locate`s, `/dndhome templesurvey`, screenshots, NPC counts), `dragon-temple-shrine.txt` (blessing, rebirth, the price, other races, Templar breath at a zombie), `dragon-temple-blocks.txt` (close-ups of every building block, the prayer wheel, the sparring post, drops and the Elder's tiles) and `dragon-temple-blocks-survey.txt` (the same shots of an older temple and of a newly generated one).
