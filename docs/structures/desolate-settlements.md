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
- **Has lost something.** The Cogsdeep observatory's dome has caved in (see [below](#the-cogsdeep-observatory)); the burrow underneath is only worn like the rest. Each of an elven enclave's rope bridges may have fallen (the chance depends on the level): it snapped somewhere in its middle 60% and each half swung down and hangs from its platform, the deck on edge (Hanging Rope Planks, which can be climbed like a ladder) between its two rails' chains, which hang from the end posts. 1 or 2 planks by the snap broke off; the rest of each half hangs as long as it was, until it hits something (the ground, a canopy, a roof). Under the whole old span lies a trail of what fell, on the first thing below it (the ground, a tree's crown even right under the old deck, a roof, or floating on water): a plank in 75% of its cells (90% where a hanging half reached the ground and lies on along it), and in each rail cell a length of chain (30%), a broken post (15%) or, where a lamp stood, a lantern (35%). Two blocks out to either side of the path, 20% of cells get a plank and 10% a chain (only planks on water). On leaves, 30% of the planks are caught on edge in the crown, and all of them are where the crown reaches right up under the old deck. One fallen bridge in 4 snapped at one end and hangs whole from the other.

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
| Siege of a fortress gate (see below) | 1 | 0.2 | 0.55 | 0.7 |
| Overgrowth | none | 4% flowers | none | flowers 20%, vines 15%, roofs 30%, floor moss 8%, 0.35 trees per chunk |

- Crops are always dead: a dead bush (30%) or nothing, or tall grass and flowers once overgrown. Farmland dries to dirt or coarse dirt.
- Forsaken settlements lose their moss, vines, lichen and sugar cane as well. Snow on a block that's gone goes with it.
- Candles, furnaces and lamps are always out, jack o'lanterns are carved pumpkins, and potted plants are dead or gone (except overgrown).
- **Collapses** only take out what was built, never the ground, trees, ores, containers or obstacles. They're planned per building (each piece of the structure), for one 12 by 12 blocks; others get them by area, up to 12 times as many and down to 0.4 times. A building at least twice that area (a goblin camp is one) has half its breaches anywhere inside it rather than on its sides:
  - **Breaches** are ragged holes knocked through a wall (one of the building's four sides) or its roof.
  - **Holes** come down through the roof and up to 3 floors under it, 1 to 2.5 blocks across, taking each floor's top block and half the time the one under it.
  - **Fallen doorways**: the door is gone and so is most of the lintel over it (80% of the row above the door and the blocks either side, 50% of the row above that), lying in a heap on the threshold.
- **What loses its support comes down.** How much of a building's walls are breached (the band of blocks within 2 of its sides, by its own breaches and any of a bigger piece's that land on it) decides its fate:
  - under 25%: only what was over a breach falls (the wall above it and the roof on it), and the roof next to it falls in in places, less often further away, out to the breach's radius again past it;
  - 25% to 55%: its roof falls in as well, in patches about 4 blocks across: none of it at 25%, all of it at 55%. The walls stand, and so does a centre pole;
  - 55% or more: the whole building is a heap, its walls left 1 or 2 blocks high, with at least 70% of what fell lying on its floor and the pieces piling up.
  - Big pieces (twice the 12 by 12 area or more, like a goblin camp) only lose what's over and next to their breaches. A building with ground or stone over it (a hobbit smial, a fortress hall) is held up.
  - Whatever is left hanging after that, touching nothing that holds it up, falls too: a stretch of roof, a wall top or a pole cap.
- **A dwarven fortress's gate** takes the worst of it, since that's where it was attacked. Its collapse is planned from where the gate's walls really are rather than its box, all scaled by the level's siege number (s, in the table):
  - The doorway is smashed in (chance 0.25 + s): a hole of radius 2.5 + 1.5s to 3.5 + 1.5s through the frame, arch and portcullis, 3 blocks into the passage.
  - 1 + 6s holes (on average) are knocked into the facade, radius 1.5 to 2 + 2s. Each one (chance 0.3 + 0.6s) goes right through the facade and the hall's front wall into the hall, between 2 and 9 blocks up; the rest are craters 1 to 3 blocks deep anywhere on the face. The bricks round each hole, 1.5 blocks out, are cracked (60%).
  - The top is broken off in a ragged line: up to 1 + 9s rows of battlements, cornice and gold band in places, stepping down 2 rows per block back from the face. On top of that, 2s great bites (on average) come out of it, 4 to 10 blocks across and 4 to 4 + 6s rows deep, right back through the facade.
  - Each of the two pillars in front topples with chance 0.1 + 0.8s, breaking 2 to 7 blocks up and lying end to end on the terrace and slope in front, some of it smashed to rubble.
  - The parapet and the lantern posts on the terrace lose blocks with chance 0.55s each.
  - At least half of what all that knocks out is thrown out 1 to 2 + 4s blocks and lies on the ground, and a scree of rubble, slabs and whole blocks of deepslate masonry lies down the slope up to 12 blocks out in front and 6 past each end, thickest at the foot of the wall and under the holes, and in the passage behind a smashed doorway.
  - Banners, lanterns and torches left with nothing to hang from go.
  - The rest of the fortress is inside the mountain and gets the usual collapses.
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
- `/dndhome visit <structure> <piece>` puts you in front of the first piece of that type, looking at it (e.g. `dndclasses:dwarven_gate`).

See [Admin commands](../systems/admin-commands.md).

## Known limitations
- Only chunks the structure's box reaches are withered, so ground just past the edge of a settlement stays as it was.
- A tree that a neighbouring chunk grows after the settlement has withered keeps its leaves and the grass under it.
- A fallen bridge's half that hits something judges how much is left to lie there from each column's own drop, not the drop under the platform, so on a slope it can lie a little short or long.
- Rubble only falls within its own chunk, and a fallen doorway's lintel is only taken out on the door's side of a chunk border.
- A fragment that crosses a chunk border is left hanging: each chunk only drops what floats wholly inside it.
- A collapse is aimed at the building's box, not its walls, so one now and then lands in the open and takes out nothing (not the fortress gate's, which knows where its walls are).
- Rubble thrown out of the fortress gate only lands in its own chunk, and the scree stops at the edge of the structure's box, 7 blocks in front of the terrace.
- Water and lava are left alone.

## For developers
- `Desolation` (`world/gen`) holds the `dndclasses:desolate` structure tag, the level each start rolls (`levelOf`, from its start chunk) and the hashed rolls everything uses; `DesolationLevel` holds the numbers above; `Withering` is the per-chunk pass.
- `StructureStartPlacingMixin` calls `Desolation.begin` before a start's pieces are placed in a chunk and `Desolation.end` (the withering) once they're in. While a desolate start is placing, `DesolateSpawnMixin` drops every entity added to a `ChunkRegion` or `ServerWorld` (worldgen and `/place structure`), `ModPiece.Builder.mimic` places a chest instead, `BridgePiece` may fall (logging `[Desolation] bridge of <cells> fell, snapped at <cell>: hangs from <pos> and <pos>` at DEBUG) and `ObservatoryPiece` caves in (`ObservatoryPiece.Cave` plans it from the piece's seed, so every chunk agrees). `DesolateAnimalsMixin` skips the animals vanilla adds to a new chunk (`NoiseChunkGenerator.populateEntities`) when a desolate start reaches into it, since spawn overrides don't cover those.
- A piece that implements `Ruinable` plans its own collapse instead of the breaches and holes aimed at its box: `Withering` hands it a `Ruinable.Ruin` (hashed rolls for that piece, breaches along a line, crumbling a column, a toppled pillar, scree, tidying unsupported blocks), which carves only the part in the chunk. `GatePiece` is the only one so far.
- `DesolateBlocks` registers the Tattered Carpets; `tools/tattered_carpet_textures.py` draws them and writes their resources.
- The structure JSONs are copies of the originals with empty `spawn_overrides` for every spawn group. Each structure set lists the original at weight 4 and the desolate one at weight 1.
- Each withered chunk logs `[Desolation] <structure> (<LEVEL>) at <start>: chunk x z: ...` at DEBUG (in `debug.log`) with the entities dropped, containers emptied, blocks changed and collapses (breaches, holes and fallen doorways).
- **Support** (`Withering.supports` and `fragments`). A real flood fill of what's connected to the ground can't work across chunks in worldgen, so it's done in two deterministic parts:
  - `plannedBreach` records every piece's breaches (centre, radius, face) before checking whether they reach this chunk, so every chunk has the whole plan. Per piece, the share of its wall band (columns within `WALL_BAND` = 2 of its box's sides) inside the radius of a breach (its own wall and inside breaches, and any other piece's that reach into its box) is its wall damage. It sets the piece's fate: local (under `CAVE_DAMAGE` = 0.25), caved (the share of the roof that falls rises from 0 to 1 up to `HEAP_DAMAGE` = 0.55, by `Desolation.patches` noise 4 blocks across) or heaped (0.55 and up). Pieces of `BIG_PIECE` area are always local.
  - Then column by column in this chunk: the column's top run of built blocks (a roof, or a wall and its roof) is found from the top down; if the highest solid block isn't built, the column is held up. The run comes down above a breach's centre if the column is within the breach's ragged radius (0.75 to 1.25 of it), above the 1 or 2 block wall stub in a heap, and wholly if it's a ceiling (open space under it) and rolls under the sag chance (1 at the breach's edge, 0 at `SAG_REACH` = 1 radius past it) or the caved patches. Only that column's blocks are read and written, and what falls is laid with `settle`, which piles heaps up.
  - `fragments` then floods this chunk's slice of the structure (edges and corners connect, so stepped roofs hold together) and drops each group with nothing holding it up: none of it is ground, stone, a natural log or a block entity (but banners and heads), and none of it lies on the slice's edge, where it may carry on into the next chunk. A chunk's blocks are final when it runs, so the result doesn't depend on the order chunks are made in.
  - Each piece that loses blocks logs `[Desolation] piece <n> <class> <centre>: wall damage <share>, <local|caved x|heaped>, <n> blocks fell` at DEBUG, and the chunk line counts the blocks fallen unsupported.
- Devscript: `desolate-collapse.txt` screenshots a desolate orc stronghold at each level.
- Devscript: `desolate-shots.txt` visits every desolate settlement at every level and screenshots it.
- Devscripts: `desolate-shots.txt` visits every desolate settlement at every level and screenshots it; `enclave-bridges.txt` visits an overgrown and a raided enclave for their fallen bridges and `enclave-bridges-close.txt` takes close shots of two of them; `enclave-bridges-2.txt` and `enclave-bridges-canopy.txt` do the same in fresh land, from above and below the canopies, `enclave-bridges-3.txt` and `enclave-bridges-span.txt` look down on whole fallen spans, `enclave-bridges-4.txt` and `enclave-bridges-trail.txt` look straight down on the trails under them, and `enclave-bridges-river.txt` shoots a bridge over a crown and a river; `desolate-observatory.txt` shoots the Cogsdeep observatory at every level, from above, the side, the floor, the gallery and the stairwell (`/dndhome cogsdeep <spot>` finds desolate burrows too).
- Devscript: `desolate-fortress-gate.txt` stands in front of a desolate fortress gate at every level (`/dndhome visit <structure> dndclasses:dwarven_gate`) and screenshots it from the front, close up and the side.
