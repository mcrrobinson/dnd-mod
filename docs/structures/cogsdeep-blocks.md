# Cogsdeep building blocks
The gnome burrow's own materials: warm riveted brass, small rounded Gnomish Bricks, turning cogwork and gears, copper pipework, glowing gems in brass settings, a clock face that keeps the real time, a blueprint table and a telescope lens. They replace the cut copper, observers, redstone lamps, stone bricks and polished andesite that used to stand in for gnome machinery in [Cogsdeep](cogsdeep.md), and they're all in the **DnD Building Blocks** creative tab.

![The Cogsdeep blocks on show: brass, gnomish bricks, cogwork, gearwall, glowcrystals, the clockface, the blueprint table, the telescope lens and pipework](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/cogsdeep-blocks/cogsdeep-blocks-showcase.png)

## How it works
| Block | Looks | Shapes | Properties |
|-|-|-|-|
| Riveted Brass | four warm brass plates with rivets along their edges and a scuffed sheen | block | cut copper's hardness (3), blast resistance (6), copper sound, pickaxe; gold on maps |
| Cut Riveted Brass | the same brass cut into eight small plates | block, stairs, slab | as Riveted Brass |
| Gnomish Bricks | small, rounded pale-grey bricks, 16 to a face (twice vanilla's) | block, stairs, slab, wall | stone bricks' (1.5, 6, pickaxe) |
| Cogwork Panel | a brass panel with two meshing cogs turning behind a cut-out (8 frames, a tooth every 16 ticks) | block | as Riveted Brass |
| Gearwall | one big four-holed gear filling the face, turning a quarter turn every 40 ticks; rows of them make a mechanism | block | as Riveted Brass |
| Pipework | copper pipes through brass joints | joins up like a fence | 2 hardness, not a full block; joins other pipework on every side and the solid face of a block on the four horizontal sides, so a run ends in a wall |
| Glowcrystal (red, blue, green, amber) | a faceted gem in a brass setting | block | light 12, amethyst block's hardness (1.5) and chime, pickaxe |
| Clockface | a cream clock face in a brass rim; the hour hand shows the in-game time | block, faces you when placed | as Riveted Brass. Twelve hour states on a twelve-hour dial (dawn, time 0, is six o'clock). It ticks just after each in-game hour (every 1000 ticks) to move the hand; a random tick starts that for clocks a structure placed and puts the hand right after `/time set`. |
| Blueprint Table | a wooden table with a blue blueprint pinned on top, a drawer and brass drafting tools | block, faces you when placed | crafting table's (2.5, axe). Using it opens a crafting grid titled "Blueprint Table". |
| Telescope Lens | the end of a brass telescope tube, 10 pixels across, with a glass lens | points away from the face you put it on | 2 hardness, not a full block |

Every block drops itself, so mining one out of a burrow gives the block, no silk touch needed (the slab drops 2 when double). Breaking a Cogwork Panel, Gearwall, Clockface, Glowcrystal, Pipework, Blueprint Table or Telescope Lens inside Cogsdeep where a gnome or Sentinel can see is an offence under the [Tinkers' law](cogsdeep.md#the-tinkers-law), as for the vanilla contraptions.

### In Cogsdeep
| Where | What changed |
|-|-|
| Every room's walls, the observatory tower, the stair | Gnomish Bricks instead of stone bricks (a few tuff and mossy stone bricks remain); the stair's pillar, steps and floor ring are Gnomish Bricks and Gnomish Brick Stairs instead of polished andesite and stone brick stairs; the door's posts are Gnomish Brick Walls |
| Observatory | a Riveted Brass footing and a Cut Riveted Brass course under the dome (the inner frame) instead of polished andesite; a Cut Riveted Brass floor ring instead of cut copper; the telescope's last rod is a Telescope Lens poking out of the slit |
| Workshops | a band of Cut Riveted Brass round the walls under the ceiling; a Cogwork Panel in the wall above each bench; Cogwork Panels instead of observers in the contraptions and the machine islands; Glowcrystals instead of redstone lamps; Riveted Brass and Cut Riveted Brass machine housings instead of copper blocks and polished andesite; two runs of Pipework along the ceiling; a Blueprint Table in each hall |
| Commons | eight Glowcrystal lamps in the walls, two runs of Pipework along the ceiling, a Gnomish Brick ring round the stair's foot |
| Clocktower | a Gnomish Brick floor; the great clock face's rim in Cut Riveted Brass; a Clockface at its hub keeping the real time (instead of a clock in an item frame); three rows of Gearwall along both side walls (instead of grindstones); a Riveted Brass pendulum bob |
| Gem room | a Glowcrystal set into the wall over each polishing bench, and two flanking the gem chest instead of amethyst blocks |

## Where to find it
- **Cogsdeep**: mine them out of a burrow (mind the Sentinels).
- **Gnome Tinkers** sell 8 Riveted Brass for 2 emeralds, 16 Gnomish Bricks for 2 and 4 Cogwork Panels for 3 (Gnomes pay 25% less; see [Gnomes](../mobs/gnomes.md#trades)).
- **Crafting**:

| Recipe | Makes |
|-|-|
| 5 copper ingots and 4 gold nuggets in a checker | 4 Riveted Brass |
| 4 Riveted Brass in a square (or 1 in a stonecutter) | 4 Cut Riveted Brass (1) |
| 4 stone bricks in a square | 4 Gnomish Bricks |
| stairs, slabs and walls as vanilla (6 make 4 stairs, 3 make 6 slabs, 6 make 6 walls), or in a stonecutter | Cut Riveted Brass and Gnomish Brick shapes |
| 4 Riveted Brass round a redstone dust | 4 Cogwork Panels |
| 4 Riveted Brass round an iron ingot | 4 Gearwalls |
| copper ingot, gold nugget, copper ingot in a row | 4 Pipework |
| glowstone, Riveted Brass and a red, blue, green or orange dye (shapeless) | 2 Glowcrystals of that colour |
| 4 Riveted Brass round a clock | 1 Clockface |
| crafting table, paper and blue dye (shapeless) | 1 Blueprint Table |
| Riveted Brass and a glass pane (shapeless) | 1 Telescope Lens |

## Known limitations
- The Clockface has an hour hand only; the minute hand always points to twelve.
- The Blueprint Table is a crafting grid for now. It's the hook for gnome tinkering (Tinker commissions, Artificer schematics), which isn't built yet.
- Burrows generated before this change keep their old blocks.

## For developers
- `classes/Registry/ModBlockFamilies`: `RIVETED_BRASS` (base only), `CUT_RIVETED_BRASS` (stairs, slab) and `GNOMISH_BRICKS` (stone shapes), under `// --- Cogsdeep ---`. The brass families use `CogsdeepBlocks.brass()` (cut copper's settings, gold map colour).
- `classes/Registry/CogsdeepBlocks`: the one-off blocks (`COGWORK_PANEL`, `GEARWALL`, `PIPEWORK`, `GLOWCRYSTALS` by colour, `CLOCKFACE`, `BLUEPRINT_TABLE`, `TELESCOPE_LENS`), their items and their place in the building-blocks tab; `register()` is called from `DnDClasses` after the families.
- Block classes in `classes/Blocks/`: `ClockfaceBlock` (`HOUR`, `hourOf(world)`), `PipeworkBlock` (a `ConnectingBlock`, `connectsTo`), `BlueprintTableBlock` (`createScreenHandlerFactory` is the hook to open another screen), `TelescopeLensBlock`.
- Structure: `CogsdeepPiece.masonry` (Gnomish Bricks), `glowcrystal(builder)` (a random colour), `Builder.pipes` (a straight run, joined up by chunk post-processing: `joinsNeighbours` includes `PipeworkBlock`); the pieces in `world/gen/burrow/`.
- `entity/TinkerTrades` (`addBuildingBlocks`, `addMissing` for saved Tinkers, called from `GnomeTinkerEntity.readCustomDataFromNbt`); `entity/TinkersLaw.isClockwork`.
- Resources: `tools/block_families.py` (the three families), `tools/cogsdeep_blocks.py` (the one-off blocks' blockstates, models, loot, recipes, tags and names), `tools/cogsdeep_block_textures.py` (every texture; the Cogwork Panel and Gearwall are 8-frame strips with a `.mcmeta`).
- Devscripts: `cogsdeep-blocks.txt` (a showcase with close-ups and a night shot, pipework joining up, the Clockface moving to 3 at 9000, drops, recipes, the Blueprint Table's grid, the Tinker's Riveted Brass offer) and `cogsdeep-survey.txt` (the burrow's rooms).
