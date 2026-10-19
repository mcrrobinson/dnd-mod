# Dwarven Fortresses
Fortresses carved into mountainsides, home to the [Mountain Dwarves](../mobs/mountain-dwarves.md). Each one is laid out differently, and the treasury behind the throne is one of the richest hoards in the mod.

![The gate of a dwarven fortress on a snowy peak](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/dwarven-blocks/fortress2-gate.png)

## How it works
You find a fortress by its **great gate**: a tall facade of Runic Deepslate Bricks at the foot of a mountainside, with the Clan Crest over a chiseled runic doorway and a raised portcullis between gold-veined pillars and banners bearing the gold [Clan Anvil](../items/banner-patterns.md). Two [Wanted Posters](../items/adventuring-blocks.md#wanted-poster) hang on the facade either side of the door. It opens onto a terrace with braziers, guards and a **Waystone**: right-click it and it names the hold ("Here stands Ironfist Hold") and tells you your standing with the Mountain Dwarves.

Behind the gate everything runs straight back into the rock:

1. **The great hall**, a long hall of gold-veined pillars with chandeliers, Clan Anvil banners (each bay in one of the hall's two colours, the anvil in the other), feasting tables, a red carpet and ancestor statues set into the walls. Four to six dwarves stand about in it. It has two or three doorways down each side, each under a clan crest.
2. **The throne room**, where the Dwarf King sits on a gold throne on a stepped dais under the clan crest, between mithral grilles, with his ancestors looking on and guards.
3. **The treasury**, through a narrow door behind the throne, behind a mithral grille, heaped with coins.

![The great hall's Clan Anvil banners, red anvils on gold and gold on red](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/racial-banners/fortress-hall-after.png)

Each side doorway leads down a short corridor to a room. A fortress has four or six of them and always has at least one of each kind:

| Room | What's in it | Loot table |
|-|-|-|
| Forge | Forge Brick chimney breast and floor round the furnaces, anvils and hearth, lava channels, an anvil, smithing tables, a grindstone | `chests/dwarven_fortress_forge`: iron and diamond tools, netherite scrap, Steampunk armor |
| Barracks | bunks and armor stands | `chests/dwarven_fortress_barracks`: weapons, shields, crossbows, Knight and Warrior armor |
| Mead hall | a long dark oak [Tavern Table](../items/adventuring-blocks.md#tavern-tables-and-benches) (three wide) of food between dark oak Tavern Benches, a forge-brick hearth between walls of ale kegs | `chests/dwarven_fortress_brewhall`: cooked food, golden apples and carrots |
| Mine | ore, amethyst and an ore cart | `chests/dwarven_fortress_mine`: raw ores, diamonds, lapis, rails, TNT |

The treasury chests use `chests/dwarven_fortress_treasury`: gold blocks, diamonds, enchanted books, enchanted golden apples, Golden Horns and Knight armor, and a Pigstep disc.

Magic items ([Magic loot](../systems/magic-loot.md)): each forge, barracks, mead hall and mine chest has a 15% chance of an Uncommon (75) or Rare (25) magic item, plus 3% of an Immovable Rod (forge), Periapt of Wound Closure (barracks) or Doss Lute (mead hall). Treasury chests have a 60% chance of an Uncommon (50), Rare (45) or Very Rare (5) item, plus 3% each of a Bag of Holding and an Amulet of Health. They're unidentified.

The halls are buried in the mountain. Where the rock is too thin, the fortress piles more on top so you never see a room poking out of a hillside. Dwarves spawn in every room and keep turning up inside, and monsters never spawn there.

### Getting the treasure
The dwarves are neutral until you cross them. If you open or break a chest or barrel, or break a gold block or a Treasury Coin Pile, every dwarf within 16 blocks that can see you turns on you. Dwarves that can't see you stay calm. A few ways to deal with that:

- Loot where no dwarf can see you, or lure them away first.
- The chests are locked. A [Rogue](../classes/rogue.md) can pick them (DC 15 in the treasury, DC 10 elsewhere, see [d20 skill checks](../systems/d20-skill-checks.md#lockpicking-rogue)). Anyone else has to break them and loses some of the loot. A picked lock still angers a dwarf that's watching.
- Help the dwarves drive off a [goblin raid](../systems/goblin-raids.md). Every dwarf of that fortress forgives you.

Some treasure is behind [class-gated obstacles](../systems/obstacles.md) (fortresses generated after they were added):

- The treasury doorway is a **Clockwork Door** (Hard, DC 15). An Artificer, or a Rogue, repairs it open with an iron ingot and a redstone dust per try. Anyone else smashes it (hardness 50), and the dwarves who see it take offence.
- Each **mine** has an ore pocket in a back corner: a mine chest in a hollow studded with ore, behind a 2x2x2 **Collapsed Passage** (Medium, DC 13). Barbarians and Fighters clear it with Athletics; anyone can dig through it.
- Each **forge** has a barrel of reagents in a back corner, at the end of the lava channel, sealed with an **Alchemical Seal** (Medium). An Alchemist dissolves it with a potion; breaking it ruins some of the contents. The barrel uses `chests/dwarven_fortress_reagents`: blaze powder, nether wart, glowstone, redstone, gunpowder, fermented spider eyes, magma cream, glistering melon, golden carrots, glass bottles, the odd ghast tear and potions of Fire Resistance, Healing and Swiftness.

### Building blocks
The fortresses are built from their own stone ([Building blocks](../dev/building-blocks.md)), all in the **DnD Building Blocks** creative tab:

| Block | Where | Notes |
|-|-|-|
| Runic Deepslate Bricks (stairs, slab, wall) | walls of every room (about 64% of the masonry, mixed with plain, cracked and tiled deepslate), base courses, floor runners, the gate's parapet, the dais | Dwarves also barter 8-16 of them for a gold ingot (weight 8) |
| Chiseled Runic Deepslate Bricks | door frames, pillar bases and caps, the gate's string course | 2 runic slabs stacked, or a stonecutter |
| Gold-Veined Pillar | the great hall's and throne room's pillars and the gate pillars | lies along an axis like a log; 2 polished deepslate round a gold ingot make 3 |
| Clan Crest | over the gate, above the throne, over the great hall's doorways | turns to face you; a gold ingot on chiseled runic brick |
| Ancestor Statue | in the great hall's walls between the pillars (bays without a doorway), flanking the throne | turns to face you; stonecut from polished deepslate |
| Forge Bricks (stairs, slab) | the forge's chimney breast, hood, hearth and the floor round them and the anvils; the mead hall's hearth | light level 3; 8 bricks round a magma block make 8 |
| Ale Keg Wall | the mead hall's back wall | turns to face you; 3 barrels stacked make 3 |
| Treasury Coin Pile | the treasury's heaps (full piles with part-piles on top, the odd gold or emerald block) | 1-8 layers like snow; drops 1-3 gold nuggets per layer, never itself (shovel); the dwarves guard it like gold blocks |
| Mithral Lattice | the treasury's grille, either side of the throne | a pane; 8 iron bars round a diamond make 8 |
| Runic Column | posts along the terrace parapet (with a lantern on top), either side of the treasury's amethyst | thinner than a block and chamfered; a stack gets a stepped base at the bottom and a capital at the top, a single one both, and the parts follow the blocks above and below as you build. Stonecut 1 from Runic Deepslate Bricks |
| Runic Corbel | under the gate's cornice and arrow slits, the great hall's wall lamps, the throne room's gold bands, the barracks' weapon shelf | a stepped bracket jutting out of a wall (placed on a wall's side it juts out of it); a lantern can hang from it. Stonecut 1 from Runic Deepslate Bricks |
| Runic Deepslate Brick Vertical Slab | buttresses up the gate facade's corners, pilasters either side of the mead hall's hearth | half a block on its edge, against the block you place it on. Stonecut 2 from Runic Deepslate Bricks, or 3 bricks in a column make 6 |
| Waystone | on the terrace beside the approach | two blocks tall, light level 5; right-click for the hold's name and your Mountain Dwarves tier. The clan is one of 16 names picked from the stone's position, so it never changes. Runic bricks round glowstone dust |

The stonework is broken up with shaped blocks, so the rooms aren't all flat walls and square openings:

| Where | Shapes |
|-|-|
| Gate | the facade's foot slopes out onto the terrace in runic stairs (a battered plinth), vertical-slab buttresses up its corners, corbels under the cornice and the arrow slits, runic wall crenellations along the top, runic column posts on the parapet, a stepped arch over the passage |
| Pillars (gate, great hall, throne room) | runic stairs round the foot and upside-down stairs round the gold capital, on every side but the wall's (so the side aisles stay two wide) |
| Great hall | stepped arches (a runic lintel with slabs and the capitals' stairs under it) from pillar to pillar down both arcades; wall lamps hang from corbels |
| Doorways | every room doorway has stepped corners: two steps on the five-wide ones, one on the three-wide ones |
| Throne room | stepped arches between the pillars, corbels under the gold bands, runic wall balustrades along the sides of the dais |
| Forge | a sloping forge-brick top on the hood over the furnaces, a stepped pyramid hood over the great hearth, table-height forge-brick slab counters between the anvils |
| Mead hall | the chimney breast tapers in forge-brick stairs above the hearth, between runic pilasters |
| Barracks | corbels under the weapon shelf |
| Mine | dark oak braces from the props to the cap beams |
| Treasury | runic columns either side of the amethyst |

Fortresses generated before these blocks were added keep their old blackstone and gilded blackstone, and ones generated before the shapes keep their flat walls.

## For your race
Dwarven Fortresses are the Dwarf home (see [Racial homes](../races/racial-homes.md)). A Dwarf gets:

- a "Welcome home, Dwarf" title on arriving
- Regeneration I in the great hall while no hostile mob is within 16 blocks
- the rarer of two rolls when bartering gold with a dwarf
- kin trust: opening chests in the forge, barracks, mead hall and mine doesn't anger the dwarves. The treasury, gold blocks and broken chests still do, but the first offence each in-game day only gets a warning growl.
- an audience with the Dwarf King (right-click him with an empty hand), once per in-game day: 3 gold ingots or an enchanted iron tool
- a Friendly (150) start with the Mountain Dwarves

## Where to find it
Meadow, Grove, Snowy Slopes, Jagged Peaks, Frozen Peaks, Stony Peaks, Windswept Hills, Windswept Gravelly Hills and Windswept Forest. Fortresses use a spacing of 16 chunks and a separation of 6, so they're fairly common in the mountains. The gate needs a steep mountainside with the ground in front clear, so not every spot in those biomes gets one.

## Commands
- `/locate structure dndclasses:dwarven_fortress`

## For developers
- `world/gen/fortress/`: `DwarvenFortressStructure` finds the gate site, `FortressPlanner` lays out the gate, hall, throne room, treasury and side rooms (one `*Piece` each), and `CladdingPiece` heaps rock over anything the mountain doesn't cover. `FortressPiece` holds the shared loot ids and `Builder.obstacle(...)`, which places one obstacle group in local coordinates. Each piece places its obstacles last in `build`, so the earlier random draws (and half-generated fortresses from before) don't change.
- Data: `worldgen/structure/dwarven_fortress.json` (dwarf spawn override, empty monster list), `worldgen/structure_set/dwarven_fortresses.json`, the `#dndclasses:has_structure/dwarven_fortress` biome tag and `loot_tables/chests/dwarven_fortress_*.json`.
- Grudges: `entity/SettlementGrudges.java` (the `MOUNTAIN_DWARVES` rules, including Dwarf kin trust) and `MountainDwarfEntity.provoke`.
- Building blocks: `FortressPiece.masonry`, `RUNIC`, `FORGE_BRICKS`, `chiseledRunic()`, `Builder.column` (pillar with stepped foot and capital; pass the sides to leave plain), `runicColumn`, `corbel`, `archTop` (stepped top corners of an opening; `doorway` uses it), `crest` and `ancestor`. The new shapes' classes are `RunicColumnBlock`, `RunicCorbelBlock` and `VerticalSlabBlock` in `classes/Blocks/dwarven/`. Families in `ModBlockFamilies` (Dwarven Fortresses section) with resources from `tools/block_families.py`; the one-off blocks in `classes/Registry/DwarvenBlocks.java` (`classes/Blocks/dwarven/`: `CarvedFaceBlock`, `CoinPileBlock`, `WaystoneBlock`) with resources from `tools/dwarven_blocks.py`; every texture from `tools/dwarven_block_textures.py`.
- Devscripts: `dwarven-fortress-locate.txt`, `dwarven-fortress-survey.txt`, `dwarven-blocks.txt` (the blocks: drops, recipes, the waystone, the coin grudge, close-ups), `dwarven-fortress-shapes.txt` (screenshots of every room of one freshly generated fortress and walks through the gate, the hall, the side rooms and up the dais; found with `dwarven-fortress-shapes-locate.txt` and `-recon.txt`), `dwarven-fortress-shapes-blocks.txt` (the new shapes' states, drops and recipes).
