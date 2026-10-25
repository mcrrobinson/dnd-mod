# Drow Outposts
The [drow](../mobs/drow.md)'s common settlement, on the floor of a wide cavern in the [Underdark](../systems/underdark.md)'s Fungal Forest and Faerzress Hollows. Three to five hollow stalagmite towers stand in a ring, joined at the top by web bridges, round a spider shrine with a Dread-Warded reliquary. A barracks dome and a captive pen stand between them. The drow attack on sight; the captives in the pen are yours to free.

![An outpost from above](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/drow-outposts/drow-outpost-above.png)

## How it works
### Where they generate
| Rule | Value |
|-|-|
| Biomes | `#dndclasses:has_structure/drow_outpost`: Fungal Forest, Faerzress Hollows (checked at the outpost's floor) |
| Structure set | `dndclasses:drow_outposts`: random spread, spacing 20 chunks, separation 8, salt 731900537 |
| Site | an open cavern floor found by `UnderdarkSite` (below): floor between y -220 and -110, flat to within 4 blocks on at least 16 of 25 points 8 blocks apart, with 12 blocks of air over each. If the start chunk's centre isn't one, the centre moved 8 blocks east, west, south or north is tried. No site, no outpost |
| Keeps clear of | Rift Landings: no outpost within 10 chunks of one that really generates |

### Layout
The outpost is about 50 blocks across. All its parts are built round the site's centre:

| Part | Where | What |
|-|-|-|
| **Grounds** | a disc of radius 25 | The cave floor levelled: up to 4 blocks filled with deepslate where it dips (the top block deepslate tiles, cobbled deepslate or Fungal Loam), and air cleared 7 blocks up, falling to 3 at the rim. Water and lava next to the cleared space are sealed with deepslate. Paths of Polished Faerzress and deepslate tiles run from the shrine to every door, with Faerie Fire Lanterns on Drowstone Brick wall posts beside them every 7 blocks. Cobwebs and Bluecaps here and there. Giant zurkhwoods, nightlight towers and Ripplebark don't grow in the chunks an outpost covers or the chunks next to them. |
| **Spider shrine** | the middle, its back (the reliquary) towards the side farthest from the pen and the barracks | An 11×11 floor of Drowstone Bricks edged with Polished Faerzress, a Spider Sigil inlay in front, Faerie Fire Lanterns on posts at the corners. The **spider idol**, 5 high: a Polished Faerzress pedestal, a Drowstone abdomen and head with crying-obsidian eyes, and eight legs of Drowstone Brick walls arching out to the floor; you can walk under the knees. An altar with a lantern in front. |
| **Reliquary** | behind the idol | A 3×3 vault holding the shrine's chest (`chests/drow_shrine`, lock DC 15). Its archway is framed with **Dread Ward** blocks (Medium): anyone in the passage is Frightened, slowed and weakened unless a Paladin is within 6 blocks ([obstacles](../systems/obstacles.md)). |
| **Dart trap** | 7 blocks out in front of the shrine | A hidden pressure plate and a dart launcher in a Drowstone post. A player stepping on it makes a **CON save DC 13 against [Drow Poison](../mobs/drow.md#drow-poison)** and is poisoned on a failure; 3 darts (Challenge tier 2, 1.67 damage each) fly at them. It fires once. Drow and their spiders walk over it without setting it off. A Rogue can spot and disarm it like any trap. |
| **Towers** (3-5) | a ring 19-20 blocks out, evenly spaced with a little jitter, doors facing the shrine | See below. 1-2 of them are lookouts. |
| **Web bridges** | between neighbouring towers' platforms | One block wide, Web Bridge Planks flush with the platforms (all platforms are at the same height), a straight four-connected line from tower to tower through a gap in each parapet. Cobweb handrails on alternate blocks, cobwebs hanging under the sagging middle third, Faerie Fire Lanterns hung under it a quarter of the way from each end. It cuts through rock in its way, with 3 blocks of air over the deck. |
| **Captive pen** | 12 blocks out between the first two towers, facing the shrine | Three cells, each 2 wide and 3 deep, under a Drowstone roof, iron bars between them and across the front. Each cell's gate is two [Chain Cages](../items/underdark-blocks.md) high, **locked**. Chains hang in the cells and Faerie Fire Lanterns stand on the roof. See [Captives](#captives). |
| **Barracks** | 14 blocks out between the next two towers, facing the shrine | A low Drowstone dome, 5 blocks round and 4 high inside, with a band of Polished Faerzress and a Spidersilk Curtain in the door. Four pale grey beds, spidersilk carpet, a Faerie Fire Lantern from the crown, and a weapon rack of two armour stands holding iron swords in black leather. |

### Towers
| Part | What |
|-|-|
| Shape | A hollow stalagmite: a 7×7 shell of Drowstone Bricks round a 5×5 hollow, coated outside in rough dripstone, deepslate, cobbled deepslate and tuff (radius 5.3 at the foot, about a block less at the platform, with noise). Above the platform a single dripstone spike runs up to the tip, 14-24 blocks above the floor. Cobbled deepslate props it up to the cave floor. |
| Door | One wide and two high, a Spidersilk Curtain hung in it (you walk through slowly). |
| Stair | Drowstone Brick stairs spiral up round the wall from beside the door, one block a step, each on a block of its own, to the platform. The middle 3×3 is open, so you step off the stair onto each floor's middle. |
| Floors | Web Bridge Planks at 4 and 8 when the platform is at 12-13, at 5 when it's at 10-11. Each floor and the platform leave the two cells open where the stair climbs through. |
| Platform | At the cavern's headroom less 3, between 10 and 13 blocks up. Drowstone, a Drowstone Brick wall parapet round it, a Faerie Fire Lantern by the spike. |
| Inside | A Faerie Fire Lantern under every floor and under the platform. Arrow slits (1×2) out of the back and both sides on every level. On the floors, spidersilk sleeping mats and barrels on the ring cells clear of the stair, and on the top floor the tower's chest (`chests/drow_outpost_tower`, lock DC 10). |
| Cobweb trap | Each flight of the stair has a 20% chance of cobwebs across two of its steps. |

### Who lives there
All placed when the outpost generates, persistent (they never despawn) and kept near their posts:

| Who | How many | Where |
|-|-|-|
| Drow Warrior | 4-7 | one on each tower's ground floor (within 6 blocks), one on each lookout's platform (within 2) |
| Drow Mage | 1-2 | one in the barracks (within 5), and one on a tower's first floor half the time |
| Drow Priestess | 1 | at the shrine (within 6) |
| Cave spider | 4-8 | 7-14 blocks out round the shrine (within 10) |
| Captives | 1-2 | kneeling in the pen |
| A pen beast | 1 | in the pen's third cell: a spider (`#dndclasses:underdark/pen_beasts`) |

The structure's `spawn_overrides` top the drow up over time anywhere in the outpost, whatever the light: warriors (weight 6, 1-2) and mages (weight 1), while fewer than 12 drow are within 48 blocks (`ModSpawns.canDrowSpawn`). Those natural spawns despawn like any monster.

The outpost is a Spider Houses settlement (`#dndclasses:faction/drow`), so its chests count for the faction's theft rules.

### Captives
Surface folk the drow have taken: a hobbit, a mountain dwarf or a wood elf (each equally likely). The first cell always holds one; the second holds another half the time.
- A captive kneels and doesn't move (no AI), never despawns, and the drow leave it alone.
- Opening its gate frees it: a Rogue picks the lock (the Chain Cage's Thieves' Tools roll, DC 12), or anyone breaks the cage (iron-bar hardness). The two cages of a gate open together.
- A freed captive thanks you in chat, stands up and walks off, and is gone 60 s later.
- You gain **+40** with its folk (`Reputation.Source.RESCUE`): the hobbits for a hobbit, the Mountain Dwarves for a mountain dwarf, the Sylvan Court for an elf. Rivals lose their usual share.
- Any mob can be a captive: it's the `Captive:1b` flag in its NBT.

Opening the third cell's gate lets its spider out.

### Loot
| Chest | Contents |
|-|-|
| `chests/drow_outpost_tower` (lock DC 10) | Spidersilk 2-6; 2-4 of arrows, gold nuggets, string, spider eyes, iron, a Drow Poison Vial; 10% a Map to the Rift; 5% an Uncommon magic item |
| `chests/drow_shrine` (lock DC 15, behind the Dread Ward) | 2-6 gold ingots, 3-8 amethyst shards, 1-2 Drow Poison Vials; 1-3 of gold nuggets, Spidersilk, a gold block, emeralds, an enchanted book; 20% a Rare magic item |

## Where to find it
- In the Underdark's Fungal Forest and Faerzress Hollows, about one per 20×20 chunks where a wide enough cavern is.
- A **Map to a Drow Outpost** ([Underdark maps](../systems/underdark.md)) leads to the nearest one: the duergar factor sells it, and `/underdark map drow_outpost` makes one.
- Quests can point at them with the structure tag `#dndclasses:quest/drow_outpost`.

## Commands
| Command | What it does |
|-|-|
| `/dndhome drowoutpost [shrine\|above\|overview\|pen\|barracks\|tower\|inside\|top\|bridge\|reliquary\|trap]` | Teleports you into the nearest outpost (searching the Underdark below you) and prints its layout |
| `/dndhome drowoutpostsurvey [grid] [spacing]` | Checks the nearest outpost from each point of a grid round you: biome, floor height, pieces, every tower shell, bridge deck and stair step, the pen's locked gates, and no water on the floor (`[DrowOutpostSurvey]` lines) |
| `/dndhome drowoutpostseeds [seeds...]` | The nearest outpost to 0, 0 on each seed and its biome, without making a world (`[DrowOutpostSeeds]` lines) |

## Known limitations
- The design's pen lock was DC 15; the pens use the Chain Cage's own lock, DC 12.
- The bridges' "sag" is only cobwebs hung under the middle: a deck that dipped a block would need a jump to climb out of.
- The design's 3% Piwafwi in the shrine chest waits for the Drow Spire ticket, which adds the item.
- Geodes and plant patches spreading from chunks two or more away can still reach an outpost's rim (in one of 17 surveyed outposts a plant sat on a tower's top step).

## For developers
- `world/underdark/UnderdarkSite` (shared): the cavern finder (`find(context, Rules.SETTLEMENT)`, cached noise columns, `Site(center, floorY, matches, headroom)`), and `inSettlement(world, pos)`, which the giant fungi check so they don't grow through a settlement (structures tagged `#dndclasses:underdark/settlements`: the outposts, and the Duergar Holds and the Drow Spire as optional entries).
- `world/underdark/UnderdarkPiece` (shared): the base piece for Underdark settlements, an `OrientedPiece` with `ground` (level the floor and seal liquids), `clear`, `seal`, `solidify`, `rock`, `noise`, lanterns, `resident` (persistent, kept near its post) and `captive`.
- `world/underdark/UnderdarkGarrisons` (shared): `inside(world, pos, structure)` and `underCap(world, pos, kin, cap)` for settlement spawn rules.
- `world/underdark/Captives` (shared): `bind(mob)`, `freeNear(world, cage, rescuer)`, `free(mob, rescuer)`, the tick; `CaptiveMobMixin` keeps the flag on every mob, `CaptiveKneelMixin` (client) crouches a humanoid mob in the crouching pose. `ChainCageBlock` frees captives when it's opened or broken.
- `world/underdark/drow/`: `DrowOutposts` (registration, keys, loot ids, `PEN_BEASTS` tag), `DrowOutpostStructure` (site and clearance), `DrowOutpostPlanner` (layout), and the pieces `DrowGroundsPiece`, `DrowShrinePiece`, `DrowTowerPiece`, `DrowBridgePiece`, `DrowPenPiece`, `DrowBarracksPiece`.
- Keeping apart is one-sided: `StructureProximity.nearGenerating` runs the other structure's site check, so two structures that each checked the other would recurse forever. The Duergar Holds and the Drow Spire check `drow_outposts`; the outposts don't check them back.
- `ModSpawns.canDrowSpawn` is every drow's spawn rule; `TrapTriggerBlockEntity.setDrowPoison` makes a dart trap a drow one (NBT `DrowPoison:1b`).
- Data: `worldgen/structure/drow_outpost.json`, `worldgen/structure_set/drow_outposts.json`, `tags/worldgen/biome/has_structure/drow_outpost.json`, `tags/worldgen/structure/{drow_outposts,quest/drow_outpost,underdark/settlements}.json` (and the existing `underdark_maps/drow_outpost` and `faction/drow`), `tags/entity_types/underdark/pen_beasts.json`, `loot_tables/chests/{drow_outpost_tower,drow_shrine}.json`.
- Devscripts: `drow-outpost-survey.txt` (9 grid points, each nearest outpost checked), `drow-outpost.txt` (3 seeds, survey, census, screenshots) then `drow-outpost-rejoin-check.txt` (the drow are still there after a rejoin); `drow-captives.txt` (a captive hobbit picked free for +40, walking off and leaving, a broken cage freeing an elf, the drow ignoring a captive, the drow dart trap).
