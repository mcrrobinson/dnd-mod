# Desolate settlements
Abandoned copies of the mod's settlements. Nobody lives there any more and the containers are empty. How far gone each one is varies: a settlement sacked days ago, one left in a hurry, one empty for years, or one the forest took back long ago.

## How it works
Six settlements have a desolate twin, built by the same planner and pieces, so it's laid out as differently as the living ones:

| Settlement | Desolate structure |
|-|-|
| [Hobbit Village](hobbit-villages.md) | `dndclasses:desolate_hobbit_village` |
| [Dwarven Fortress](dwarven-fortresses.md) | `dndclasses:desolate_dwarven_fortress` |
| [Elven Enclave](elven-enclaves.md) | `dndclasses:desolate_elven_enclave` |
| [Orc Stronghold](orc-strongholds.md) | `dndclasses:desolate_orc_stronghold` |
| [Cogsdeep](cogsdeep.md) | `dndclasses:desolate_cogsdeep` |
| [Goblin Camp](goblin-camps.md) | `dndclasses:desolate_goblin_camp` |

Dragon Temples and Brimstone Sanctums never go desolate.

Every desolate settlement:
- **Has no one in it.** None of the residents, guards, merchants, bosses, mimics, item frames, armour stands or minecarts are placed, and no mobs or animals spawn inside it, either when its chunks are made or later. Animals can still wander in from outside. A mimic's spot gets a plain chest.
- **Has been looted.** Every chest, barrel, Larder Shelf and other container is empty, beehives have no bees or honey, the gold blocks and coin heaps are gone and so are the cakes on the tables.
- **Has lost something.** The Cogsdeep observatory's dome has caved in (see [below](#the-cogsdeep-observatory)); the burrow underneath is only worn like the rest. Each of an elven enclave's rope bridges may have fallen (the chance depends on the level), leaving a stub of 1 or 2 planks at each end, with planks and rail lying on the ground below.

Then each one rolls a **level**, all four equally likely. The numbers are per block unless they say otherwise:

| | Raided | Abandoned | Forsaken | Overgrown |
|-|-|-|-|-|
| Feel | sacked not long ago | left in a hurry | empty for years | lost long ago |
| Grass worn to dirt (in patches) | 25%, some trampled to path | 8% | 60% | none; 50% of dirt and paths grass over |
| Wild plants dead (dead bush 30%, else gone) | 35% | 5% | 60% | none |
| Leaves left | all | all | 40% | all |
| Torches / lanterns left | 35% / 50% | 30% / 60% | 15% / 40% | 0% / 20% |
| Campfires still burning | 35% | none | none | none |
| Glass broken | 60% | 15% | 35% | 70% |
| Bricks cracked / mossy | 15% / 0% | 8% / 4% | 20% / 5% | 25% / 40% |
| Cobwebs under ceilings | none | 3% | 5% | 10% |
| Breaches per building (radius) | 2.5 (1.8-3.5) | 0.6 (1.2-2.2) | 1.5 (1.5-3) | 2.2 (1.8-3.5) |
| Holes through roof and floors, per building | 0.8 | 0.2 | 0.6 | 1 |
| Doorways fallen in | 50% | 10% | 35% | 60% |
| Knocked-out blocks left as rubble | 60% | 30% | 45% | 50% |
| Carpets tattered / gone | 50% / 10% | 30% / 0% | 70% / 15% | 60% / 40% |
| Doors smashed | 50% | 10% | 30% | 60% |
| Elven bridges fallen (each) | 35% | 10% | 30% | 60% |
| Overgrowth | none | 4% flowers | none | flowers 20%, vines 15%, roofs 30%, floor moss 8%, 0.35 trees per chunk |

- Crops are always dead: a dead bush (30%) or nothing, or tall grass and flowers once overgrown. Farmland dries to dirt or coarse dirt.
- Forsaken settlements lose their moss, vines, lichen and sugar cane as well. Snow on a block that's gone goes with it.
- Candles, furnaces and lamps are always out, jack o'lanterns are carved pumpkins, and potted plants are dead or gone (except overgrown).
- **Collapses** only take out what was built, never the ground, trees, ores, containers or obstacles. They're planned per building (each piece of the structure), for one 12 by 12 blocks; others get them by area, up to 12 times as many and down to 0.4 times. A building at least twice that area (a goblin camp is one) has half its breaches anywhere inside it rather than on its sides:
  - **Breaches** are ragged holes knocked through a wall (one of the building's four sides) or its roof.
  - **Holes** come down through the roof and up to 3 floors under it, 1 to 2.5 blocks across, taking each floor's top block and half the time the one under it.
  - **Fallen doorways**: the door is gone and so is most of the lintel over it (80% of the row above the door and the blocks either side, 50% of the row above that), lying in a heap on the threshold.
- Some of what a collapse knocks out lies on the ground below, scattered up to a block sideways: as a slab of the same material, a few layers of [Collapsed Rubble](dungeons.md) for stone, or the block itself.
- **Tattered Carpets** replace the vanilla carpets: the same colour, worn, stained and with holes torn through. There's one for each dye colour, and they can be mined and placed.
- **Overgrowth**: flowers and tall grass on open grass, vines hanging down walls (each vine 75% likely to carry on down), moss carpet on floors, and roofs with moss carpet or grassed-over soil (and the odd flower). Oak and birch trees grow up on open ground inside.

### The Cogsdeep observatory
Its dome has fallen in, and what's left stands only where something still holds it up:
- The dome is split into 10 sectors round the middle. Each keeps its copper up to a rim a few rows above the walls, blended into its neighbours and a block higher or lower here and there, so the crown is always gone and the rim is jagged. A few blocks of what's left are knocked out too.
- Some sectors have **slumped in**: no copper left over them at all, the wall's top course (and often the row under it, windows and all) broken off, and a heap of copper up to 3 high against the wall below. There's always at least one. A window beside a broken stretch comes down too (its top course stood on the glass). The wall never breaks over the door or the ladder.
- **Nothing floats.** Going up row by row from the walls, a block of copper stays only if it stands on the wall, the copper below or the gallery, or reaches at most 2 blocks along its row to one that does.
- The **telescope** has slid off true, its eyepiece down on the gallery, or has been knocked off its tripod and lies across the gallery with its lens on the floor below. The dome's lightning rod lies on the floor.
- **Fallen copper** (cut copper blocks, slabs, stairs lying at any angle and Collapsed Rubble) covers the floor and some of the gallery. It's kept off the stairwell's rail and the way in from the door, so the stair down to the burrow stays open.
- The withering's breaches and holes leave the observatory alone: they would leave bits of the rim hanging.

| | Raided | Abandoned | Forsaken | Overgrown |
|-|-|-|-|-|
| Rim (rows of copper left above the walls, per sector) | 0-3 | 2-4 | 1-3 | 0-3 |
| Sectors slumped in (at least one) | 25% | only the one | 20% | 30% |
| Wall top rows lost under a slump | 1-2 | 1 | 1-2 | 1-2 |
| Copper left knocked out (per block) | 12% | 5% | 10% | 15% |
| Floor cells with copper (outside a heap; half that on the gallery) | 55% | 40% | 50% | 50% |
| Telescope only askew (else knocked off) | 25% | 75% | 45% | 20% |
| Copper a stage more weathered | none | none | 60% | 60% |

The withering covers the structure's box and up to 24 blocks above it (pieces like the hobbit party tree build higher than their boxes), and fades in over the outer 6 blocks, so it doesn't stop in a straight line.

A desolate settlement isn't a home: it gives none of the [racial home](../races/racial-homes.md) bonuses, isn't raided by [goblins](../systems/goblin-raids.md) and doesn't count for [faction](../systems/factions.md) or quest checks. Obstacles and puzzle blocks are kept.

## Where to find it
1 in 5 settlements of each of the six kinds is the desolate one. It shares the living settlement's structure set, so it turns up in the same biomes and spacing and is kept apart from the others in the same way. Find one with `/locate structure dndclasses:desolate_<name>`.

## Commands
- `/dndhome desolation <raided|abandoned|forsaken|overgrown|random>` gives every desolate settlement generated from now on that level, for testing. It isn't saved, and `random` (the default) rolls it again.
- `/dndhome visit <structure>` teleports you over the nearest start of any structure.

See [Admin commands](../systems/admin-commands.md).

## Known limitations
- Only chunks the structure's box reaches are withered, so ground just past the edge of a settlement stays as it was.
- A tree that a neighbouring chunk grows after the settlement has withered keeps its leaves and the grass under it.
- Rubble only falls within its own chunk, and a fallen doorway's lintel is only taken out on the door's side of a chunk border.
- A collapse is aimed at the building's box, not its walls, so one now and then lands in the open and takes out nothing.
- Water and lava are left alone.

## For developers
- `Desolation` (`world/gen`) holds the `dndclasses:desolate` structure tag, the level each start rolls (`levelOf`, from its start chunk) and the hashed rolls everything uses; `DesolationLevel` holds the numbers above; `Withering` is the per-chunk pass.
- `StructureStartPlacingMixin` calls `Desolation.begin` before a start's pieces are placed in a chunk and `Desolation.end` (the withering) once they're in. While a desolate start is placing, `DesolateSpawnMixin` drops every entity added to a `ChunkRegion` or `ServerWorld` (worldgen and `/place structure`), `ModPiece.Builder.mimic` places a chest instead, `BridgePiece` may fall and `ObservatoryPiece` caves in (`ObservatoryPiece.Cave` plans it from the piece's seed, so every chunk agrees). `DesolateAnimalsMixin` skips the animals vanilla adds to a new chunk (`NoiseChunkGenerator.populateEntities`) when a desolate start reaches into it, since spawn overrides don't cover those.
- `DesolateBlocks` registers the Tattered Carpets; `tools/tattered_carpet_textures.py` draws them and writes their resources.
- The structure JSONs are copies of the originals with empty `spawn_overrides` for every spawn group. Each structure set lists the original at weight 4 and the desolate one at weight 1.
- Each withered chunk logs `[Desolation] <structure> (<LEVEL>) at <start>: chunk x z: ...` at DEBUG (in `debug.log`) with the entities dropped, containers emptied, blocks changed and collapses (breaches, holes and fallen doorways).
- Devscripts: `desolate-shots.txt` visits every desolate settlement at every level and screenshots it; `desolate-observatory.txt` shoots the Cogsdeep observatory at every level, from above, the side, the floor, the gallery and the stairwell (`/dndhome cogsdeep <spot>` finds desolate burrows too).
