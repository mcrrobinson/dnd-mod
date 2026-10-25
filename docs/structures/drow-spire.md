# Drow Spire
The drow's hanging city, and the seat of the [Matron Mother](../bosses/matron-mother.md). Deep in the [Underdark](../systems/underdark.md)'s Faerzress Hollows, a great dome holds a spire of dripstone and drowstone: a stalagmite rising from the floor, joined at its waist to a stalactite hanging from the roof. Three minor towers stand round it, bridged to its middle with webs, and a spider pit gapes at its foot. Inside, floor above floor: a guard hall, the priestesses' cells, a library, and at the top the Matron's Sanctum. They're rare: about one in every 770 x 770 blocks of the Underdark, and only in the Faerzress Hollows.

![A Drow Spire from the dome's edge: the spire, two of its towers and their web bridges](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/feat-drow-spire/drow-spire-above.png)

## How it works
### Where they generate
| Rule | Value |
|-|-|
| Biomes | `#dndclasses:has_structure/drow_spire`: Faerzress Hollows only (checked at the spire's floor) |
| Structure set | `dndclasses:drow_spires`: random spread, spacing 48 chunks, separation 20, salt 1482093611 |
| Site | an open cavern floor found by `UnderdarkSite`, as the [Drow Outposts](drow-outposts.md)' but with the floor between y -220 and -125 (so the dome fits under the Underdark's roof): flat to within 4 blocks on at least 16 of 25 points 8 blocks apart, 12 blocks of air over each |
| Keeps clear of | Drow Outposts and Rift Landings: no spire within 8 chunks of one that really generates |

The cavern is rarely tall enough, so the spire carves the dome it needs.

### Layout
| Part | Where | What |
|-|-|-|
| **Dome** | round the middle | A dome of radius 28 and height 44, flattened at the top (its height at radius r is 44 x sqrt(1 - (r/28)^4)), cleared to air. The floor under it is levelled (up to 4 blocks filled with deepslate), with veins of Faerzress Stone. A 2-block shell of dripstone, deepslate and tuff closes the dome and its rim wherever the cave was open, water and lava included, so nothing spills in. Six Faerie Fire Lanterns hang from its roof. |
| **Path** | from the spire's door to the dome's edge | Polished Faerzress, 3 wide, with Faerie Fire Lanterns on Drowstone posts either side every 6 blocks |
| **The Spire** | the middle | See below |
| **Minor towers** (3) | 18 blocks out: 60 degrees either side of the spire's front, and straight behind | The [outposts' towers](drow-outposts.md#towers), taller: their platforms are level with the spire's cells (12 up) and their tips 20-28 high. Doors face the spire. The first is a lookout. |
| **Web bridges** (3) | from each tower's platform to the spire | The outposts' bridges, running into the cells through a doorway they cut in the spire's wall |
| **Spider pit** | beside the spire, 13 blocks to one side and 8 back | A 7 x 7 pit, 4 deep, lined with Drowstone Bricks, a low wall round its rim (a gap on the front side), cobwebs in it, and a **cave spider spawner** on its floor: 2 at a time, every 10-25 s, while a player is within 16 blocks, at most 6 nearby, whatever the light |

### The Spire
| Part | What |
|-|-|
| Shape | 8 blocks round at the foot, narrowing to 6.8 at its waist (10 up), widening to 9.5 at the Sanctum (26-35 up), then tapering to a point of 2.5 at the dome's roof (44 up) and on into the rock. The outside is rough dripstone, deepslate, cobbled deepslate and tuff patched with Drowstone Bricks; inside, a 1-2 block shell of Drowstone Bricks. Cobbled deepslate props its foot to the cave floor. |
| Door | In the front: 3 wide, 3 high, three Spidersilk Curtains across it, a Spider Sigil inlay on the floor inside. |
| Windows | Arrow slits two high on every level, a Faerie Fire Lantern standing on each sill, so the spire glows violet from outside. |
| Main stair | Drowstone Brick stairs spiral round the middle (the towers' 16-step ring, 2 blocks out), one block a step, from the hall floor to the Sanctum, through a gap in each floor. It's always open. |
| **Guard hall** (ground, 12 high) | Four lanterns hanging from the ceiling, barrels. 2 Drow Warriors and 2 Drow Elites. |
| **Priestesses' cells** (floor 12) | Three cells parted by Spidersilk Curtains, each with a web bridge's doorway; sleeping mats, a chest (`chests/drow_spire_cells`). 2 Drow Priestesses and 2 Drow Elites. |
| **Library** (floor 19) | Bookshelves three high round the wall, a lantern hanging over the stair, and a lectern holding **"Letters to the Mother"**: a drow agent's letters about the dwarf-roads, the elves' scouts, and something that thinks, deeper down (the "Shadows Below" quest chain's clue). 3 Drow Mages. |
| **Matron's Sanctum** (floor 26, 7 high) | A round room of radius 8: a Polished Faerzress floor half covered in spidersilk, cobwebs round the back edge, four lanterns. At the back a spider-idol throne (a Polished Faerzress seat and back, a Spider Sigil with crying-obsidian eyes) with eight Drowstone legs arching over it from the walls. The [Matron Mother](../bosses/matron-mother.md) stands before it. |
| **Treasure loft** (above the Sanctum) | A side stair runs up along the Sanctum's front wall to a slot in the ceiling, shut by a **Hard Arcane Seal** (Wizard or Warlock, see [obstacles](../systems/obstacles.md)). Above it the loft: a chest (`chests/drow_spire_loft`), a gold block, barrels, lanterns. |

### Who lives there
All placed when the spire generates, persistent and kept near their posts:

| Who | How many | Where |
|-|-|-|
| [Matron Mother](../bosses/matron-mother.md) | 1 | the Sanctum, before her throne |
| Drow Elite | 4 | 2 in the guard hall, 2 in the cells; all come to the Sanctum when she calls them |
| Drow Priestess | 2 | the cells |
| Drow Mage | 3 | the library |
| Drow Warrior | 6 | 2 in the guard hall, one on each tower's ground floor, one on the lookout's platform |
| Cave spider | from the spawner | the spider pit |

The structure's `spawn_overrides` top the drow up as in an outpost: warriors (weight 6, 1-2) and mages (weight 1), whatever the light, while fewer than 12 drow are within 48 blocks (`ModSpawns.canDrowSpawn`). The spire is a Spider Houses settlement (`#dndclasses:faction/drow`) and is in `#dndclasses:quest/drow_spire` for quests.

### Loot
| Chest | What |
|-|-|
| Cells (`chests/drow_spire_cells`) | 2-4 of Spidersilk, gold nuggets, amethyst, candles, a Drow Poison Vial, a book; 10% a roll on the Uncommon (70) / Rare (30) [magic pools](../systems/magic-loot.md) |
| Loft (`chests/drow_spire_loft`) | 4-10 gold ingots, 3-8 emeralds, 2-4 of gold blocks, amethyst, diamonds, Drow Poison Vials, Spidersilk, an enchanted book; always a roll on Rare (60) / Very Rare (40), and 15% a [Piwafwi](../items/magic-items.md#piwafwi) |
| Towers (`chests/drow_outpost_tower`) | as in an outpost |

## Where to find it
- Explore the Faerzress Hollows of the Underdark. `/locate structure dndclasses:drow_spire` from the Underdark.
- `/dndhome drowspire` takes you to the nearest one (see below).

## Commands
- `/dndhome drowspire [sanctum|throne|above|overview|door|hall|cells|library|loftstair|loft|pit|bridge|tower]`: teleports you into the nearest spire (from anywhere: it searches the Underdark below you) and prints its layout.
- `/dndhome drowspiresurvey [grid] [spacing]`: checks the nearest spire from each point of a grid round you: biome and floor height, the pieces, then every step of both stairs, the seal, the Sanctum's floor, every bridge's deck, the lectern's book, the pit spawner and a dry floor (`[DrowSpireSurvey]` lines).
- `/dndhome drowspirecensus`: who lives in the nearest spire (`[DrowSpireCensus]`).
- `/dndhome drowspireseeds [seeds...]`: the nearest spire to 0, 0 on each seed (this world's, 1 and 20261025 by default) and its biome, without making a world (`[DrowSpireSeeds]` lines).

## Known limitations
- Only Underdark chunks generated after this update can hold a spire.
- The web bridges' cobweb handrails also run a block or two into the cells, beside the path.

## For developers
- `world/underdark/drow/DrowSpires.java` (registration: structure type, `CAVERN` and `SPIRE` pieces, `KEY`, `SET`, `TAG`, loot ids), `DrowSpireStructure` (site rules `RULES`, `MAX_FLOOR` -125, `CLEARANCE` 8, `KEEP_CLEAR_OF` the outposts and rift landings; one-sided, see `DrowOutpostStructure`), `DrowSpirePlanner` (`TOWER_RING` 18, `TOWERS`, tips `MIN_TIP` 20 to `MAX_TIP` 28), `DrowSpireCavernPiece` (`RADIUS` 28, `HEIGHT` 44, `SHELL` 2, `FILL` 4, the pit `PIT_*` and `SPAWNER_NBT`), `DrowSpirePiece` (`CELLS` 12, `LIBRARY` 19, `SANCTUM` 26, `LOFT` 34, `LOFT_TOP` 40, `outer(y)`, `WALL` 1.6; the book `lettersToTheMother()` with NBT flag `dndclasses_letters_to_the_mother`; the Matron via `StructureBosses.placeMatron`).
- The minor towers and bridges reuse `DrowTowerPiece` and `DrowBridgePiece` (platform 12; a tower's platform maxes out at 14).
- `classes/Commands/DrowSpireCommands.java`: the `/dndhome` subcommands.
- Data: `worldgen/structure/drow_spire.json`, `worldgen/structure_set/drow_spires.json`, `tags/worldgen/biome/has_structure/drow_spire.json`, `tags/worldgen/structure/drow_spires.json`, `quest/drow_spire.json`, `loot_tables/chests/drow_spire_{cells,loft}.json` (the magic pools are added by `MagicLoot`). The spire is already in `#dndclasses:underdark/settlements` (giant fungi keep out) and `#dndclasses:faction/drow`.
- Devscripts: `devscripts/drow-spire-seeds.txt` (spires on 6 seeds), `devscripts/drow-spire.txt` (this world's nearest spire: survey, census, screenshots of every floor).
