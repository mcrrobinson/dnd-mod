# Elven Enclaves
Treetop halls round a giant Heart Tree in the birch and flower forests, joined by rope bridges, with a Moonwell, an archery glade and flower gardens on the forest floor. [Elves](../mobs/elves.md) live here. Enclaves are the Elf home (see [Racial homes](../races/racial-homes.md)).

![An elven enclave: the Heart Tree with its deck and the Speaker's Hall, four talans and the rope bridges](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/elven-enclave/enclave-side.png)

## How it works
Every enclave is laid out afresh round its **Heart Tree**:

| Piece | How many | What it is |
|-|-|-|
| Heart Tree | 1, in the middle | A 5x5 trunk of birch wood with stripped-birch inlays, 34 tall, roots flaring 3 blocks at the base and a canopy 9 blocks in radius of birch, azalea and flowering azalea leaves, hung with glow berries and lanterns. A spiral stair of spruce steps winds once round the trunk, past the bridge deck (16 up) to the Speaker's Hall (22 up). |
| Speaker's Hall | 1 | An 11-wide round platform at the top of the stair: birch floor with moss carpet, 12 bookshelves set into the trunk, an enchanting table, a lectern, a loom and the Heart Tree chest. The Speaker keeps to it. It is the elves' hearth. |
| Talan | 3 to 5 | A treetop platform, 7x7, on a giant birch with a 3x3 trunk, on a ring 19-30 blocks from the Heart Tree. A ladder runs up the north face and vines hang from the others. Each is a dwelling (green bedroll and chest), a scout post (cartography table and barrel) or a herbalist's (brewing stand, flower pots and barrel). |
| Rope bridges | one from each talan to the Heart Tree, plus one between neighbouring talans up to 30 blocks apart | Birch slabs one wide with fence handrails, lanterns and hanging chains. Every platform is at the same height (16 above the Heart Tree's foot), so the bridges run level and sag one block in the middle in half-slab steps, so you never have to jump. |
| Moonwell | 1, 14-17 blocks from the Heart Tree | A 5x5 pool two deep over a glass floor lit by sea lanterns, rimmed with calcite and amethyst clusters, a white banner on a post at each corner |
| Archery glade | 1 | Three targets on hay bales, a shooting line, a fletching table and a barrel. The Fletcher keeps to it. |
| Flower garden | 1 or 2 | Woodland flowers, sweet berry bushes and moss paths, with a beehive on a post 40% of the time |

The Heart Tree and talans are built from birch *wood*, never logs, so the forest's own trees are left standing among them. The ground pieces clear the forest's trees off their own plots.

![The canopy from above](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/elven-enclave/enclave-top.png)
![Talans and bridges round the Heart Tree](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/elven-enclave/enclave-bridges.png)
![The Speaker's Hall](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/elven-enclave/enclave-hall.png)

### People
8 to 12 elves live in each enclave when it generates, most of them Elf Wardens: one or two on each talan, one at the foot of the Heart Tree with a Wood Elf, one at the glade, a Wood Elf in each garden and sometimes one at the Moonwell. The Speaker stands in the hall and the Fletcher at the glade. Wood Elves and Wardens spawn naturally inside the pieces later, up to 12 elves within 48 blocks. See [Elves](../mobs/elves.md).

No monsters spawn anywhere inside an enclave's box.

### Sylvan Law
Every elf within 16 blocks who can see you takes offence, and the Wardens turn on you for 30-50 s, if inside an enclave you:

- break a log or leaf block of the Heart Tree or a talan (anything inside their pieces' boxes)
- kill an animal
- use flint and steel or a fire charge (fire, lighting TNT) or empty a lava bucket
- open the Heart Tree chest

Each seen offence also costs 40 standing with the Sylvan Court. Helping to drive off a goblin raid on the enclave forgives you. Striking any elf brings every Warden within 16 blocks down on you too.

### The Moonwell
Fill an empty glass bottle from the Moonwell at night (13000-23000) and an Elf gets **Moonwater**: Regeneration II for 20 s and Night Vision for 2 minutes. Elves can draw it once a night for free. Anyone else only gets water there and has to buy Moonwater from the Speaker. By day the well is just water.

![Drawing Moonwater at night](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/elven-enclave/moonwater.png)

### Loot
| Loot table | Found in | Contents |
|-|-|-|
| `chests/elven_enclave_talan` | talan chests and barrels, the glade's barrel | arrows, string, feathers, bows, green leather tunics and boots, books, glow berries, sweet berries, 1-3 emeralds, amethyst; 5% an enchanted book, 3% a golden apple |
| `chests/elven_enclave_heart` | the Heart Tree chest in the Speaker's Hall | 2-3 of: 1-2 diamonds, enchanted books (Power, Infinity or Feather Falling), golden apples, amethyst; 15% a bow enchanted at level 20 |

Talan chests aren't locked. The Heart Tree chest is [locked](../systems/d20-skill-checks.md#lockpicking-rogue) at DC 15, and opening it where an elf can see is an offence.

## For your race
Enclaves are the Elf home. An Elf gets:

- a "Welcome home, Elf" title on arriving
- Regeneration I in the Speaker's Hall while no hostile mob is within 16 blocks
- 25% off the Speaker's and the Fletcher's trades
- free Moonwater once a night
- kin trust: the first tree-block offence each in-game day is forgiven with "The Warden frowns at you."
- a Friendly (150) start with the Sylvan Court

## Where to find it
Birch Forest, Old Growth Birch Forest and Flower Forest. Enclaves use a spacing of 32 chunks and a separation of 12, stay at least 6 chunks from vanilla villages and 8 chunks from goblin camps (camps keep the same distance from enclaves). The middle must be dry, with at most 4 of 24 samples out to radius 24 wet, out of the forest or more than 6 blocks above or below it.

Goblins sometimes [raid](../systems/goblin-raids.md) enclaves. The war party rallies at the Moonwell and the Wardens rush out to meet it.

## Commands
- `/locate structure dndclasses:elven_enclave`
- `/dndhome pieces` lists every piece of the enclave you're in.

## Known limitations
- The Elven Longbow and Elven Chain (racial gear) aren't in yet: the Fletcher and Speaker don't sell them and the Heart Tree chest has an enchanted bow in their place.
- The Speaker's map looks for a lair within 100 chunks the first time someone opens their trades, which can stall the server for a moment. With no lair in range the map trade is left out.
- Moonwater is a vanilla potion with its own effects, colour and name, so it brews and stacks like any potion.
- The forest's own trees are left in place, so a birch can grow through a bridge's rail or poke out of a canopy.
- Elves are always neutral; reputation tiers don't change how they treat you yet.

## For developers
- `world/gen/enclave/`. `ElvenEnclaveStructure` finds the site (`EnclaveTerrain`), `ElvenEnclavePlanner` places the pieces and plans the bridges, `EnclavePiece` is the shared base (local coordinates round an origin, no rotation, a per-piece seed and a position hash `noise` so every chunk builds the same slice), and each part is a piece: `HeartTreePiece`, `SpeakersHallPiece`, `TalanPiece`, `BridgePiece`, `MoonwellPiece`, `ArcheryGladePiece`, `EnclaveGardenPiece`, `EnclaveGroundsPiece` (lava to water). `ElvenEnclaveStructures` registers them; `TREES` lists the pieces Sylvan Law protects.
- `Moonwell`: the bottle hook (`UseItemCallback`), `moonwater()`, night tracked as `DndMoonwaterNight` in persistent data.
- Data: `worldgen/structure/elven_enclave.json` (elves in the `creature` override for pieces, an empty `monster` list for the whole box), `worldgen/structure_set/elven_enclaves.json`, `#dndclasses:has_structure/elven_enclave`, `#dndclasses:elf_speaker_maps` (the map's targets), `loot_tables/chests/elven_enclave_*.json`, `factions/sylvan_court.json`.
- `RacialHomes.ELVEN_ENCLAVE` (hearth: `ElvenEnclaveStructures.HALL`), `Settlement.Kind.ENCLAVE`, `SettlementGrudges.SYLVAN_COURT` (`entity/SylvanLaw`), `Lockpicking` (the heart table at DC 15, talan chests unlocked), `GoblinCampStructure` (keeps 8 chunks from enclaves).
- Devscripts: `elven-enclave-locate.txt`, `elven-enclave-survey.txt` (close-up screenshots), `elven-enclave-elves.txt` (Wardens, Sylvan Law, trades, Moonwell, hearth, spawns, raids) and `elven-enclave-models.txt`. The last three use an "Enclave A" world: a fresh `level.dat` with seed 1111.
