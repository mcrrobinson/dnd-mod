# Elven building blocks
The Elven Enclaves' own materials: **Heartwood**, the Heart Tree's wood; **Moonstone**, pale stone that holds moonlight; **Living Wood**, bark with leaves growing out of it; **Silverleaf** leaves; **Woven Rope Planks** for the rope bridges; **Sylvan Glyph Stones** marking the edge of Sylvan Law; glowing **Glimmercaps**; **Moonpetals** that open at night; and **Starglass**. They're built into every newly generated [Elven Enclave](../structures/elven-enclaves.md) and listed in the **DnD Building Blocks** creative tab.

![The Heartwood blocks](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/elven-blocks/eb-heartwood.png)

## How it works
| Block | Shapes | What it is | Where in the enclave |
|-|-|-|-|
| Heartwood | Log, Wood, Stripped Log, Planks, Stairs, Slab, Fence, Fence Gate, Door, Trapdoor | Silver-white bark with soft grey-green ridges and gold flecks; swirled rings on the log ends; pale cream planks with a gold-green grain that curves. Birch's hardness and sounds; an axe strips a log. Logs and planks count as logs and planks for vanilla recipes and burn like wood | the Heart Tree's trunk, roots and boughs (Wood, with Stripped Log inlays and beams), its spiral stair (Stairs) and rail (Fence on a Stripped Log stringer), the bridge deck (Planks, Fence), the Speaker's Hall floor |
| Moonstone | Moonstone (Stairs, Slab, Wall), Polished Moonstone, Moonstone Bricks (Stairs, Slab, Wall) | Pale white stone with a cool blue-lilac sheen and a few bright pixels; the bricks are rounded. Calcite's hardness and sounds. Every Moonstone block gives off light level 4, day and night | the Moonwell's rim (Bricks, capped with Polished Moonstone), the edge of the Heart Tree's deck (Polished), a ring inlaid in the Speaker's Hall floor (Bricks) |
| Living Wood | block | Pale bark with small leaves, curling tendrils and tiny flowers growing out of it. Birch wood's hardness | the talans' trunks above their platforms, the Heart Tree's trunk round the Speaker's Hall |
| Silverleaf | leaves | Silver-green leaves with lighter edges and a few glinting pixels; not tinted by the biome. Mined with shears or silk touch, like leaves | the Heart Tree's canopy |
| Woven Rope Plank | slab-thin | Birch slats lashed with rope along both edges, 3 pixels thick, in the top or bottom half of the block like a slab (placed by where you click). The slats lie across the way you face when you place it. Needs nothing under it | the rope bridges' decks |
| Sylvan Glyph Stone | 2 high | A mossy grey standing stone with a pale green leaf glyph that glows (light 6). Placed and broken as one, like a door, and needs solid ground | 2-4 round the edge of the enclave's grounds |
| Glimmercap | plant | A cluster of pale blue mushrooms with glowing caps (light 7). Grows on dirt, grass and moss | round the Heart Tree's roots, the flower gardens |
| Moonpetal | flower | A white flower: a closed bud by day, open silver petals at night (13000-23000), checked every 10 s once a random tick or placing it wakes it up. Makes Night Vision suspicious stew | round the Moonwell, the flower gardens |
| Starglass | block, Pane | Clear glass with a faint blue tint, small white star specks and a thin silver frame. Needs silk touch, like glass | the Speaker's Hall balustrade (Pane) |

Every block drops itself when mined (Silverleaf and Starglass need the tool above, Woven Rope Planks and Glimmercaps need nothing).

Recipes:
- Heartwood Planks: 1 Heartwood Log, Wood or Stripped Log makes 4. Heartwood Wood: 4 logs make 3. The shapes use the usual recipes (stairs, slab, fence, gate, door, trapdoor).
- Moonstone: 8 calcite round an amethyst shard make 8. Polished Moonstone: 4 Moonstone make 4. Moonstone Bricks: 4 Polished Moonstone make 4. The stonecutter cuts Moonstone into any Moonstone, Polished or Brick shape, and Polished Moonstone into bricks.
- Living Wood: a vine over birch wood.
- Woven Rope Plank: 2 string over 3 birch slabs make 6.
- Sylvan Glyph Stone: glow lichen over 2 mossy cobblestone.
- Starglass: 8 glass round glowstone dust make 8; 6 Starglass make 16 panes.

Glimmercaps and Moonpetals have no recipe: pick them in an enclave.

![Moonstone, by night](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/elven-blocks/eb-moonstone-night.png)
![Silverleaf, Woven Rope Planks, a Sylvan Glyph Stone, Glimmercap, Moonpetals shut and open, Starglass](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/elven-blocks/eb-misc.png)

## Where to find it
- Every [Elven Enclave](../structures/elven-enclaves.md) generated from now on is built from them (mining them there breaks [Sylvan Law](../structures/elven-enclaves.md#sylvan-law) like any tree block).
- The Fletcher sells 8 Heartwood Logs for 4 emeralds and the Speaker 8 Moonstone Bricks for 3 (3 and 2 for Elves). Merchants from older worlds get the new offers when they load.
- The recipes above, and the DnD Building Blocks creative tab.

## Known limitations
- Enclaves generated before this update keep their birch and calcite.
- Moonstone glows all the time, not only at night.
- The talans have no walls or windows, so Starglass is only in the Speaker's Hall, and Living Wood is the talans' trunks round their platforms.
- A Moonpetal placed by worldgen only starts following the time of day after its first random tick (a few minutes at most).

## For developers
- `classes/Registry/ModBlockFamilies.java` ("Elven Enclaves"): `HEARTWOOD_LOG`, `HEARTWOOD_WOOD`, `STRIPPED_HEARTWOOD_LOG` (axis bases), `HEARTWOOD_PLANKS` (stem `heartwood`), `MOONSTONE`, `POLISHED_MOONSTONE`, `MOONSTONE_BRICKS` (calcite with luminance 4), `LIVING_WOOD`.
- `classes/Registry/ElvenBlocks.java`: the one-off blocks (`SILVERLEAF`, `WOVEN_ROPE_PLANK`, `SYLVAN_GLYPH_STONE`, `GLIMMERCAP`, `MOONPETAL`, `STARGLASS`, `STARGLASS_PANE`), the axe stripping and flammability, the creative-tab entries and the client render layers (`cutoutBlocks`, `translucentBlocks`). Block classes in `classes/Blocks/elven/`: `WovenRopePlankBlock` (`half`, `axis`), `SylvanGlyphStoneBlock` (`half`, door-like), `GlimmercapBlock`, `MoonpetalBlock` (`open`, `CHECK_TICKS` = 200).
- Resources: the families from `tools/block_families.py`; textures, the one-off blocks' blockstates, models, loot, recipes, lang and tags, and the extra Heartwood/Moonstone recipes from `tools/elven_block_textures.py`.
- Placed by `HeartTreePiece`, `SpeakersHallPiece`, `TalanPiece`, `BridgePiece`, `MoonwellPiece`, `EnclaveGardenPiece` and `EnclaveGroundsPiece` (`glyphSpots`, `GLYPH_INSET`). Sales in `entity/ElfTrades` (`heartwood()`, `moonstone()`, `addMissing`).
- Devscripts: `elven-blocks.txt` (close-ups by day and night, the Moonpetal opening at night, drops, axe stripping, placing the glyph stone, the merchants' offers, a rope-plank bridge), `elven-blocks-survey.txt` (the dev world's enclave before and after).
