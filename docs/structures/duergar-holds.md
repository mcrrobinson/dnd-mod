# Duergar Holds
The [duergar](../mobs/duergar.md)'s forge-towns, on the floor of a wide cavern in the [Underdark](../systems/underdark.md)'s Grey Reaches. A 6-high wall of Basalt Forge Bricks with Greysteel battlements closes three sides; the fourth is a lake of lava. Inside are a Forge Hall, a Trade Hall where the factor deals, a mine with captive gnomes, one or two barracks and the Taskmaster's tower with the strongbox. The Grey Clans are Unfriendly to most visitors, not hostile: you can walk in and trade, but they watch their things.

![A hold from a front corner](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/feat-duergar-holds/duergar-hold-view-corner.png)

## How it works
### Where they generate
| Rule | Value |
|-|-|
| Biomes | `#dndclasses:has_structure/duergar_hold`: Grey Reaches (checked at the hold's floor) |
| Structure set | `dndclasses:duergar_holds`: random spread, spacing 24 chunks, separation 10, salt 482915003 |
| Site | an open cavern floor found by `UnderdarkSite` (the same rules as the [Drow Outposts](drow-outposts.md)): floor between y -220 and -110, flat to within 4 blocks on at least 16 of 25 points 8 blocks apart, with 12 blocks of air over each |
| Keeps clear of | Drow Outposts and Rift Landings: no hold within 10 chunks of one that really generates |

### Layout
The hold is 58 blocks square round the site's centre, its gate on a random side and the whole plan mirrored left to right half the time. Seen from the gate:

| Part | Where | What |
|-|-|-|
| **Grounds** | the whole 58×58 square | The cave floor levelled: up to 4 blocks filled with tuff where it dips, air cleared 10 blocks up (falling to 4 at the rim), water and lava round it sealed. Springs in the cave's walls and roof over the hold, up to 40 blocks above the floor and 4 blocks round the square, are stopped (lava to basalt, water to cobbled deepslate), so nothing pours into the hold. Inside the wall the yard is paved with cobbled deepslate, deepslate tiles and tuff. A road of smooth basalt and polished deepslate runs from the gate to the lake, with paths off it to every door. Duergar Braziers on basalt-brick posts line the road every 8 blocks and stand at the lake's corners. |
| **Lava lake** | the back, in the middle | A basin 20 wide and 12 deep of lava, two blocks deep and sunk one block under the floor, walled and floored with basalt down to 5 blocks under the floor, with a ring of magma blocks for a shore and a basalt-brick railing along the yard. Every block round the lava is solid, so it never flows: a hold's survey counts the 480 lava blocks in the basin and none anywhere else. |
| **Wall** | round the land side | Two blocks thick: Basalt Forge Bricks 5 high, a Greysteel band on top of the outer row and Greysteel battlements on every other block above it, stepped buttresses outside. It meets the lake's shore at the back. A solid **tower** 10-11 high stands on each corner: a battered foot, slits, a crown of merlons overhanging on corbels and a brazier on top. |
| **Gatehouse** | middle of the front | 9 wide, 5 deep, 9 high, between two **bastions** (4 wide, 8 high, outer corners cut) that stand a block further out, with machicolations overhanging the gate on corbels and a hood on basalt posts round the arch. The passage is 3 wide and 2 high under a Greysteel lintel, closed by a **portcullis** of three Greysteel trapdoors. They open on redstone, and the hold starts with them open. The **lever room** over the passage holds the lever: flip it and the portcullis drops, flip it back and it rises. A ladder by the yard-side door climbs to the lever room and on through a hatch to the roof, where **2 arbalesters** keep watch. Braziers flank the gate. |
| **Forge Hall** | back, on one side | The biggest building: a hall 13×9 with its corners chamfered, walls 8 high, buttressed, under a stepped roof. The **great stack**, a 3×3 chimney, rises from the middle of the back wall to 14-17 blocks and smokes from campfires on magma; the great furnace burns in its foot, and two thin flues smoke from the side walls. A porch 5 wide with a lean-to roof holds the door in a recessed portal; half the time a **smelting wing** with two more blast furnaces leans against the road side. Inside, two lava channels run in the floor under iron bars (the lava a block down, the bars flush with the floor). A grindstone, two lit blast furnaces, a smithing table and the **forge chest** (`chests/duergar_forge`) stand along the back wall, with three anvils in the middle, a lava cauldron, Duergar Braziers and soul lanterns on chains. |
| **Taskmaster's tower** | back, on the other side, by the lava | 7×7 with chamfered basalt corners, three floors with Greysteel bands, a battered foot and a crown of battlements overhanging on corbels, with pinnacles (braziers on some). Three towers in four have a balcony off the top floor towards the lake with a gibbet cage hanging under it. A ladder climbs the back corner through every floor. Ground floor: the guard room. First floor: the **strongbox** vault, a 2×2 closet behind a **Locked Door** (Hard). Top floor: the Taskmaster's quarters, with windows over the hold and the lava. Half the time an arbalester stands on the roof. |
| **Mine** | in front of the tower, facing the road | A 7×7 mine head with its corners cut, a recessed portal and shuttered windows, over a stairwell. The winding house rises over the stairwell to 8-10 blocks under its own roof, with the winch chain hanging down it. The stair (3 wide, a block down each step) runs 7 steps down under the hold to the **gallery**, 9×9 and 4 high, cut from tuff and deepslate with ore in the walls: basalt props, soul lanterns, a rail line with **two chest minecarts** (`chests/duergar_mine`), heaps of raw iron and raw gold blocks and the **ore chest** (`chests/duergar_mine`). In its side wall is the **slave pen**: see [Captives](#captives). Everything round the stair and the gallery is set solid first, so caves and liquids under the hold can't break in. |
| **Trade Hall** | by the gate, on the tower's side | 11×9 with chamfered corners, 6 high. Three halls in five raise a **counting house** 10 high over the back half, its roof stepping up over the hall's. A portico on two basalt posts shelters the door, and an outside chimney stack smokes from the hearth. A Greysteel counter with the **factor** behind it, barrels of stock (`chests/duergar_trade`: gold nuggets, Darklake Pearls), and the **hearth**: a Duergar Brazier in a brick fireplace with a basalt chimney breast. |
| **Barracks** (1-2) | opposite the Trade Hall | 9×7 and 5 high, back corners cut, under a hipped roof, with a square **watch turret** 9-10 high over one front corner and a ladder inside it. With two barracks the turrets face each other across the alley between them, and a Greysteel **bridge** joins them at the top of the ladders. Four bunks: a black bed under a Greysteel shelf with a grey wool mattress on it. A chest of kit (`chests/duergar_barracks`) and barrels. |

### Architecture
Every building is built from the same kit (`DuergarMasonry`), so the hold reads as one grim, heavy style:
- **Footprints** are boxes with chamfered corners, combined: a taller box over a lower one makes a tower, clerestory or winding house; a lower box beside a taller one is a wing under a lean-to roof.
- **Walls**: Basalt Forge Bricks on a polished blackstone base course, polished basalt at square corners, a trim course 3 up and a Greysteel band at the eaves.
- **Plinth**: a ring of blackstone stairs round every foot, so the walls look battered. Stepped buttresses stand against the long walls.
- **Roofs** overhang the walls by a block on corbels and rise a block per block inward as hipped, stepped roofs. Ridges carry a crest of wall blocks. A roof wider than its steps flattens into a terrace with a parapet.
- **Details**: doors recessed in portals (basalt posts under a blackstone hood, a Greysteel lintel, the arch's corners stepped); windows with hoods, sills and open Greysteel trapdoor shutters; chimneys smoking from campfires on magma; Duergar Braziers outside doors.
- **Variety**: each hold draws one style for all its buildings, from 3 roof materials (deepslate tiles, polished blackstone, Greysteel Plate) × 2 ridge crests (forge brick or blackstone wall) × 2 trim courses (polished deepslate or chiselled blackstone). Each building also rolls its own wings, turrets, chimney heights, balcony and pinnacles. A mirrored hold mirrors every building, so wings and turrets stay on the same side of the road.

| | |
|-|-|
| ![Forge Hall](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/feat-duergar-holds/duergar-hold-view-forge.png) | ![Mine head](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/feat-duergar-holds/duergar-hold-view-mine.png) |
| ![Barracks](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/feat-duergar-holds/duergar-hold-view-barracks.png) | ![Gate](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/feat-duergar-holds/duergar-hold-view-outside.png) |

### Who lives there
All placed when the hold generates, persistent (they never despawn) and each kept near its post (its `Home`):

| Who | How many | Where |
|-|-|-|
| Duergar Soldier | 6-9 | 2 in the forge (within 6), 2 in each barracks (within 8), 1 in the mine gallery (within 6), 1-2 in the yard (within 14) |
| Duergar Arbalester | 2-3 | 2 on the gatehouse roof (within 2), and half the time 1 on the tower roof |
| Duergar Factor | 1 | behind the Trade Hall counter (within 3) |
| Duergar Taskmaster | 1 | the tower's top floor (within 3) |
| Captive gnomes | 1-2 | kneeling in the mine's pen |
| [Rust Monster](../mobs/rust-monster.md) | 1 | got into the mine gallery, by the ore face (within 6) |

The structure's `spawn_overrides` top the garrison up anywhere in the hold, whatever the light: soldiers (weight 6, 1-2 at a time) and arbalesters (weight 1), while fewer than 16 duergar are within 48 blocks (`DuergarEntities.canDuergarSpawn`). Those natural spawns despawn like any monster.

Duergar don't attack at Unfriendly. They do attack anyone Hostile with the Grey Clans (a Dwarf starts there), anyone who strikes one of them, and anyone who breaks the hold's law.

### The law
The hold is a Grey Clans settlement (`#dndclasses:faction/duergar_clans`). Its law is `DuergarLaw`, one of the [settlement grudge rules](../systems/factions.md):

| What you do | Counts when |
|-|-|
| Open or break a chest or barrel in the **Forge Hall**, the **mine** or the **Taskmaster's tower** (the forge chest, the ore chest, the strongbox) | a duergar can see you |
| Break **Greysteel** anywhere in the hold (the roofs, battlements, counter, bunk shelves, portcullis) | a duergar can see you |
| Break the **forge's own things**: anvils, blast furnaces, the smithing table, the grindstone, the lava cauldron, Duergar Braziers, Basalt Forge Bricks or iron bars inside the Forge Hall | a duergar can see you |

- A witness is any duergar within the Grey Clans' witness range (16 blocks at Neutral, 24 at Unfriendly or worse) with a line of sight to you.
- Seen, the guards turn on you and the Grey Clans' standing drops by 40 (`theft_witnessed`, at most once every 10 s). Unseen, nothing happens.
- Striking a duergar brings every duergar within 16 blocks down on you (the duergar's own rule).
- There is no kin trust: no race gets a warning first.
- The Trade Hall's and the barracks' barrels and chests and the chest minecarts aren't watched.

### Captives
The duergar work captured gnomes in their mine. The pen is two cells in the gallery's side wall, each behind a gate two [Chain Cages](../items/underdark-blocks.md) high, **locked**. The first cell always holds a kneeling gnome, the second half the time.
- Opening a gate frees its gnome: a Rogue picks the lock (the Chain Cage's Thieves' Tools roll, DC 12), or anyone breaks the cage.
- A freed gnome thanks you, walks off and is gone 60 s later. You gain **+40 with the Tinkers' Guild** (`Reputation.Source.RESCUE`), and its rivals (the goblins) lose their share.
- It works like the [Drow Outposts](drow-outposts.md#captives)' captives: any mob with `Captive:1b` is one.

### Loot
| Chest | Contents |
|-|-|
| `chests/duergar_forge` (lock DC 10) | 3-6 of iron ingots, raw iron, coal, iron nuggets, gold ingots, Duergar Steel Ingots, a lava bucket; 1-2 of enchanted iron pickaxes and axes, iron tools and armour, chains, an anvil; 25% 1-3 more Duergar Steel Ingots |
| `chests/duergar_mine` (the ore chest, lock DC 10, and the two minecarts) | 3-5 of raw iron, raw gold, coal, iron and gold nuggets, torches, rails, amethyst; sometimes a pickaxe or a diamond |
| `chests/duergar_strongbox` (lock DC 15, behind the Locked Door) | 1-3 gold blocks; 2-4 of gold ingots, emeralds, Duergar Steel Ingots, Darklake Pearls, diamonds, an enchanted book; **20% a Rare** and **5% a Very Rare** magic item |
| `chests/duergar_barracks` (lock DC 10) | rations, nuggets, arrows, a crossbow or an axe, leather; 10% a Map to the Rift |
| `chests/duergar_trade` (barrels, unlocked) | gold nuggets, emeralds, raw iron, Spidersilk, gold ingots, sometimes a Darklake Pearl |

The vault also holds a gold block on the floor.

## Where to find it
- In the Underdark's Grey Reaches, about one per 24×24 chunks where a wide enough cavern is.
- A **Map to a Duergar Hold** ([Underdark maps](../systems/underdark.md)) leads to the nearest one; `/underdark map duergar_hold` makes one.
- Quests can point at them with the structure tag `#dndclasses:quest/duergar_hold`.

## Commands
| Command | What it does |
|-|-|
| `/dndhome duergarhold [gate\|inside\|above\|overview\|corner\|corner2\|outside\|gateview\|forge\|forgeview\|trade\|tradeview\|mine\|mineview\|pen\|tower\|towerview\|vault\|top\|lever\|lake\|barracksview]` | Teleports you into the nearest hold (searching the Underdark below you) and prints its layout: the forge chest, lever, portcullis, pen gate, vault door, strongbox and lava. The `*view`, `corner`, `corner2` and `outside` spots look at the buildings from outside (best in spectator mode). |
| `/dndhome duergarholdsurvey [grid] [spacing]` | Checks the nearest hold from each point of a grid round you: biome, floor height, pieces, the lava (at least 90% of the 480 in the basin, none astray), no water inside the wall, the portcullis open, the pen's locked gates, the vault's Locked Door, the chests and every mine stair step (`[DuergarHoldSurvey]` lines) |
| `/dndhome duergarholdcensus` | Who lives in the nearest hold: duergar by kind, how many are persistent, captives (`[DuergarHoldCensus]`) |
| `/dndhome duergarholdseeds [seeds...]` | The nearest hold to 0, 0 on each seed and its biome, without making a world (`[DuergarHoldSeeds]` lines) |

## Known limitations
- The Grey Reaches' large dripstone and dripstone clusters don't generate within a chunk of a hold (`dndclasses:away_from_duergar_holds`), and the hold stops the springs over it, so neither drops into the basin or pours into the yard. Other features from neighbouring chunks that generate after the hold can still leave a few blocks on the levelled ground, and water from a neighbouring biome can stand on the rim outside the wall.
- Tall parts (the great stack, the tower's crown, the corner towers) can rise into a low cavern roof; they're built through the rock rather than cut free of it.
- The portcullis is one row of trapdoors at head height: closed it stops anyone walking through, but it's only redstone deep (break the lintel's dust and it shuts for good).
- Opening a chest minecart isn't theft: the grudge rules only see blocks.

## For developers
- `world/underdark/duergar/`: `DuergarHolds` (registration, keys, loot ids, the shared `duergar(...)` resident helper), `DuergarHoldStructure` (site and clearance), `DuergarHoldPlanner` (layout constants and the spots), and the pieces `DuergarGroundsPiece` (floor, lake, road, braziers, yard guards; its box is the whole hold), `DuergarWallPiece` (wall, gatehouse, portcullis wiring), `DuergarForgePiece`, `DuergarTradePiece`, `DuergarMinePiece`, `DuergarBarracksPiece`, `DuergarTowerPiece`. All are `UnderdarkPiece`s laid out in local coordinates with the door towards +b. The buildings are `DuergarPiece`s, which can be built mirrored (`Mirror`) and carry the hold's style (`Style`); the barracks also save `Turret` and `Bridge`.
- `DuergarMasonry`: the building kit. A `Mass` is a footprint of cells, each with a wall height, made of chamfered boxes; `build` raises it (foundation, walls, plinth, eaves, roof). The helpers are `portal`, `window`, `buttress`, `chimney` and `openAir` (air with any liquid next to it sealed). The roof's slope comes from each cell's chessboard depth from the edge of its own height.
- `AwayFromHoldsPlacement` (`dndclasses:away_from_duergar_holds`): a placement modifier that drops a feature whose chunk or a neighbouring chunk has a reference to a hold. It's on `underdark/large_dripstone` and `underdark/dripstone_cluster`.
- The portcullis: a floor lever (`powered=true`) beside three redstone dust (power 15, 14, 13) laid on the Greysteel lintel; the dust powers the lintel blocks, which power the trapdoors under them (`open=true, powered=true`, facing east so they stand against each cell's side).
- `entity/DuergarLaw` (`GuardRules<DuergarEntity>`, registered as `SettlementGrudges.GREY_CLANS`): `protects` finds the hold with `getStructureContaining(pos, DuergarHolds.KEY)` and the room with `roomAt(start, pos)` (the building piece whose box holds it).
- `DuergarEntities.canDuergarSpawn` lets duergar spawn in any light inside a hold (`UnderdarkGarrisons.inside`), still capped at 16 within 48 blocks.
- Keeping apart is one-sided (see the [Drow Outposts](drow-outposts.md#for-developers)): the holds check `drow_outposts` and `rift_landings`; nothing may check the holds back.
- Data: `worldgen/structure/duergar_hold.json`, `worldgen/structure_set/duergar_holds.json`, `tags/worldgen/biome/has_structure/duergar_hold.json`, `tags/worldgen/structure/{duergar_holds,quest/duergar_hold}.json` (and the existing optional entries in `underdark_maps/duergar_hold`, `faction/duergar_clans` and `underdark/settlements`), `loot_tables/chests/duergar_{forge,mine,strongbox,barracks,trade}.json`. `Lockpicking` lists the strongbox as a hard (DC 15) table.
- Devscripts: `duergar-hold.txt` (3 seeds, survey before and after 200 ticks, census, the lake checks, the lever, screenshots), `duergar-hold-views.txt` (overviews and every building from outside) then `duergar-hold-law.txt` (trading at Unfriendly, the forge chest seen and unseen, freeing a gnome, the vault door).
