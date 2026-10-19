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
- **Has lost something.** The Cogsdeep observatory's dome has caved in, most of all at the crown, and its copper lies on the floor round the stairwell; the burrow underneath is only worn like the rest. Each of an elven enclave's rope bridges may have fallen (the chance depends on the level): it snapped somewhere in its middle 60% and each half swung down and hangs from its platform, the deck on edge (Hanging Rope Planks, which can be climbed like a ladder) between its two rails' chains, which hang from the end posts. Each half hangs as long as it was until it hits something: the ground, a canopy, a roof. The rest of it lies on top of whatever is under its path, away from the tree (85% of those planks, with a length of chain beside 35% of them), so the fallen bridge stays in one piece. One fallen bridge in 4 snapped at one end and hangs whole from the other.

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
- A fallen bridge's half that hits something judges how much is left to lie there from each column's own drop, not the drop under the platform, so on a slope it can lie a little short or long.
- Rubble only falls within its own chunk, and a fallen doorway's lintel is only taken out on the door's side of a chunk border.
- A collapse is aimed at the building's box, not its walls, so one now and then lands in the open and takes out nothing.
- Water and lava are left alone.

## For developers
- `Desolation` (`world/gen`) holds the `dndclasses:desolate` structure tag, the level each start rolls (`levelOf`, from its start chunk) and the hashed rolls everything uses; `DesolationLevel` holds the numbers above; `Withering` is the per-chunk pass.
- `StructureStartPlacingMixin` calls `Desolation.begin` before a start's pieces are placed in a chunk and `Desolation.end` (the withering) once they're in. While a desolate start is placing, `DesolateSpawnMixin` drops every entity added to a `ChunkRegion` or `ServerWorld` (worldgen and `/place structure`), `ModPiece.Builder.mimic` places a chest instead, `BridgePiece` may fall (logging `[Desolation] bridge of <cells> fell, snapped at <cell>: hangs from <pos> and <pos>` at DEBUG) and `ObservatoryPiece` caves in. `DesolateAnimalsMixin` skips the animals vanilla adds to a new chunk (`NoiseChunkGenerator.populateEntities`) when a desolate start reaches into it, since spawn overrides don't cover those.
- `DesolateBlocks` registers the Tattered Carpets; `tools/tattered_carpet_textures.py` draws them and writes their resources.
- The structure JSONs are copies of the originals with empty `spawn_overrides` for every spawn group. Each structure set lists the original at weight 4 and the desolate one at weight 1.
- Each withered chunk logs `[Desolation] <structure> (<LEVEL>) at <start>: chunk x z: ...` at DEBUG (in `debug.log`) with the entities dropped, containers emptied, blocks changed and collapses (breaches, holes and fallen doorways).
- Devscripts: `desolate-shots.txt` visits every desolate settlement at every level and screenshots it; `enclave-bridges.txt` visits an overgrown and a raided enclave for their fallen bridges and `enclave-bridges-close.txt` takes close shots of two of them; `enclave-bridges-2.txt` and `enclave-bridges-canopy.txt` do the same in fresh land, from above and below the canopies, and `enclave-bridges-3.txt` and `enclave-bridges-span.txt` look down on whole fallen spans.
