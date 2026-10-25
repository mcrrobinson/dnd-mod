# Underdark Rifts
The way into the [Underdark](../systems/underdark.md) without commands. A rift is a ragged sinkhole on the surface, ringed with tuff and coarse dirt and lit by lanterns on posts. Ledges spiral down its walls to a chasm deep in the deepslate. At the chasm's far end lies a pool of black, shimmering **Rift Veil**. Stand in it for a second and you fall through the bottom of the world, to a lit **Rift Landing** straight below. A **Rift Anchor** there pulls you back up. Killing a lair [Beholder](../bosses/beholder.md) also tears a rift open in its floor.

![The sinkhole from above](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/underdark-rifts/rift-sinkhole-top.png)

## How it works
### The rift (Overworld)
| Part | What it is |
|-|-|
| Sinkhole | A 7×7 shaft from the chasm floor up to the ground. Its walls are stone and deepslate, with tuff and cobbled deepslate in the top 5 blocks under a 2-block lip of coarse dirt, and a two-block shell behind them. Trees and hillside above the hole are cleared 8 blocks up. Cobbled-deepslate-wall posts with chains and lanterns stand at the four corners of the lip. Glow lichen grows on the walls. |
| Ledges | One-wide ledges, two blocks thick, spiral round the shaft's outer ring of 24 cells. Every 2 cells the ledge rises one block, so one turn of the spiral climbs 12 blocks. You walk and hop the whole way up or down without placing a block. The climb starts on the chasm floor and ends level with the ground. |
| Chasm | A jagged slit 7-9 wide, 21-23 long and about 28 tall (floor at y -58, roof at y -30 to -28). It runs along a random axis through the start chunk's centre, with the sinkhole at one end. A quarter of the wall blocks are [Faerzress Stone](../items/underdark-blocks.md); the rest are deepslate. Glow lichen grows on the side walls, and 9 dripstones hang from the roof. A two-block deepslate shell keeps caves, aquifers, water and lava out. |
| Veil pool | A 3×5 pool of Rift Veil set into the floor at y -58, at the far end from the sinkhole, edged with Faerzress Stone. |
| Rubble ledge | A 4×4 heap of Collapsed Rubble beside the pool (full blocks, corners half height). The anchor brings you up here, facing the pool. |

### The landing (Underdark)
Straight below every rift, in the same chunk, is a Rift Landing. It's a dome 7 blocks in radius and 6 high, cleared over the first open cavern floor found down the centre column from y -80 to y -200. If there's none, it's carved out of the rock at y -150. The floor is paved with deepslate tiles, with a ring and four spokes of Faerzress Stone. In the middle stands a three-block Rift Anchor. Four Nightlight Caps on zurkhwood stems light it, so nothing spawns there. A chest of supplies (`chests/rift_landing`) stands west of the anchor with a lantern beside it. Water round the dome is walled off with deepslate.

### Rift Veil
| Rule | Value |
|-|-|
| Collision | none: you sink into the pool, which sits 2 px below the floor |
| Breaking | unbreakable (hardness -1, blast resistance 3,600,000); pistons can't move it; no item drops |
| Light | 8 |
| Players | 20 ticks (1 s) standing in it drops you into the Underdark (1 tick in creative). Not while the rift cooldown runs |
| Mobs | they won't path into it (`PathNodeType.DAMAGE_OTHER`, `DANGER_OTHER` next to it). A mob pushed in for 20 ticks falls through like a player |
| Items, XP orbs | gone in a puff of particles |

### Going down
1. If a Rift Landing starts in your chunk or one of the 8 round it, you land on its anchor's east side, facing east.
2. Otherwise (the Beholder's rift, or a veil placed by hand), you land on a floor `UnderdarkLanding` finds at your x and z. A Rift Anchor is set up beside it if none is within 12 blocks. If no floor is found, a pocket is carved at y -150 with an anchor.

You get Slow Falling for 5 s, a 1 s Darkness pulse, and the title "The Underdark" with "You fall through the bottom of the world" under it. Then a 5 s cooldown starts (100 ticks of Overworld time, saved in your player data under `dndUnderdark.Cooldown`).

### Going up
Right-click a Rift Anchor in the Underdark. It pulls for 3 s (60 ticks), with a progress bar on the action bar. Moving more than a block or taking damage cancels the pull, so it isn't a panic button mid-fight.
1. If an Underdark Rift starts in your chunk or one of the 8 round it, you arrive on its rubble ledge at the chasm bottom, facing the pool.
2. Otherwise you come up on the Overworld's surface at the same x and z (the nearest dry column within 16 blocks).

The 5 s cooldown starts again. An anchor outside the Underdark does nothing.

### A Beholder's rift
When a Beholder whose home is in a [Beholder lair](../bosses/beholder.md) dies, the 3×3 middle of the floor's great eye (the pupil) becomes Rift Veil, and the iris round it cracks. A roar and a crack sound, and everyone within 64 blocks reads "The floor where the eye floated splits open. Cold air rises from below." This happens on death, so it also works in lairs that generated before rifts existed. Lairs don't line up with landings, so going down there usually lands you on a found floor (with an anchor set beside it) or in a carved pocket.

### Loot
| Loot table | Found in | Contents |
|-|-|-|
| `chests/rift_landing` | the chest at every landing | 8-16 torches, 3-6 bread, 16-32 ladders, and 1-3 of: glow berries, string, iron ingots, arrows, a lantern, bones (or nothing) |

## Where to find it
- Rifts generate on dry land in every Overworld land biome except the deep dark (checked at the chasm's depth, so they never cut into an ancient city), oceans, rivers, beaches and mushroom fields. They're about one per 450×450 blocks (structure set `underdark_rifts`: random spread, spacing 28 chunks, separation 10, salt 731900412).
- A rift stays at least 6 chunks from a Beholder lair and from any racial home (Hobbit Village, Dwarven Fortress, Elven Enclave, Dragon Temple, Orc Stronghold, Cogsdeep).
- `/locate structure dndclasses:underdark_rift`. Rifts only appear in newly generated chunks.
- Every lair Beholder you kill leaves a rift in its floor.

## Commands
| Command | Effect |
|-|-|
| `/dndhome rift [top\|mouth\|floor\|ledge\|pool\|step <k>]` | Teleports you to that spot of the nearest rift (ledge by default) and prints its pool, ledge, mouth and step count |
| `/dndhome riftlanding` | Onto the nearest Rift Landing in the Underdark, beside its anchor and facing it |
| `/dndhome riftlair` | Onto the nearest Beholder lair's floor, 4 blocks east of the pupil, facing it |
| `/dndhome riftsurvey [grid] [spacing]` | Checks the nearest rift to each point of a grid round you: both pieces, a landing in the same chunk below, 15 veil blocks, and every climbing step clear (2 blocks of air over a solid ledge). Logged as `[RiftSurvey]` |
| `/dndhome riftseeds [seeds...]` | For each seed (this world's, 1 and 20261025 by default), the nearest rift to 0, 0 that would generate, without making a world. Logged as `[RiftSeeds]` |

All need permission level 2.

## Known limitations
- No mobs follow you through, and pets stay behind, as with the Nether. Hirelings can follow through the `UnderdarkTravel.AFTER_TRAVEL` event.
- The landing chest has no Underdark map yet. The maps ticket adds one to `chests/rift_landing`.
- Rifts only appear in chunks generated after this update. Lair Beholders killed in older chunks still open rifts.

## For developers
- Structures, in `world/underdark/rift/`: `UnderdarkRiftStructure` (site rules: dry land, no deep dark, proximity via `StructureProximity.nearGenerating`), `RiftChasmPiece`, `RiftSinkholePiece`, `RiftLandingStructure` (column-sample floor search), `RiftLandingPiece`, and `UnderdarkRiftStructures` (types, pieces, keys). Data: `worldgen/structure/{underdark_rift,rift_landing}.json`, `worldgen/structure_set/{underdark_rifts,rift_landings}.json` (the same placement, which keeps every rift over a landing), `tags/worldgen/biome/has_structure/{underdark_rift,rift_landing}.json`.
- Blocks: `classes/Registry/RiftBlocks` (`RIFT_VEIL`, `RIFT_ANCHOR`, the pathfinding registration), `classes/Blocks/underdark/RiftVeilBlock` and `RiftAnchorBlock` (property `top`, the crystal head). Resources from `tools/rift_blocks.py`, textures from `tools/rift_block_textures.py`.
- Travel: `UnderdarkTravel.riftDown(player|entity)`, `riftUp(player)`, `downTarget`, `upTarget`, `onCooldown`. Log lines: `[Underdark] <player> descends at x y z (landing|found|carved)`, `[Underdark] <player> ascends to x y z (rift|surface)`, `[Underdark] <mob> falls through a rift to ...`, `[Underdark] <player> starts an anchor pull at ...` and `... anchor pull is cancelled (moved|hurt)`.
- `UnderdarkRifts`: veil contact (the per-entity tick counter; moves happen at the end of the server tick), the anchor pull, `ensureAnchorNear`, `nearbyPiece`, `onBeholderDeath` (called from `BeholderEntity.onDeath`) and `tearOpen(world, floorCentre)` (`[Underdark] Beholder rift torn open at ...`). `BeholderCavernPiece.getFloorCentre()` gives the pupil.
- `UnderdarkLanding`'s carved pocket now has a Rift Anchor in its north-west corner.
- Devscript: `underdark-rifts.txt` runs the seeds check, screenshots, the survey and step checks, the veil down, the anchor up and both cancels, an item and a zombie at the pool, and a lair Beholder's rift.
