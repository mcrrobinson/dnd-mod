# Dwarven Ruin & Beholder lair blocks
Building blocks that make the [Dwarven Ruin](../structures/dungeons.md#the-dwarven-ruin) look like a fallen version of the living [dwarven fortresses](../structures/dwarven-fortresses.md), and show the [Beholder](../bosses/beholder.md)'s work in its lair: Fallen Runic Bricks, Collapsed Rubble, Tarnished Gilding, Ray-glazed Stone, Petrified Remains and Eye-Stalk Growths. All of them are in the **DnD Building Blocks** creative tab.

## How it works
| Block | Shapes | Looks like | Mined with | Placed in |
|---|---|---|---|---|
| Cracked Runic Deepslate Bricks | stairs, slab, wall | Runic Deepslate Bricks with cracks through them and chipped corners; half the gold inlay is chipped out, the rest dull | pickaxe (as cracked deepslate bricks) | Ruin walls and corridors, its bricked-up secret doorways, the stair down, rubble slabs, the gatehouse |
| Mossy Runic Deepslate Bricks | stairs, slab, wall | Runic Deepslate Bricks with moss in the mortar and a dull inlay | pickaxe (as deepslate bricks) | Ruin walls and corridors, the gatehouse |
| Tarnished Gilding | lies along an axis, like a log | dark fluted stone with gold leaf flaking off in dull brown patches; the ends are a flaking gold ring | pickaxe (as gilded blackstone) | Ruin pillars, door frames, the gatehouse's doorway pillars and fallen pillar drums |
| Ray-glazed Stone | stairs, slab, wall | smooth glassy grey with a diagonal sheen and faint concentric swirls | pickaxe (as polished deepslate) | the Beholder lair's shaft (walls, floor and spiral stair), tunnel and most of the cavern walls |
| Collapsed Rubble | 1-8 layers | broken stone chunks in grey dust | shovel; 0.6 hardness, gravel sounds | drifts round every Ruin rubble heap and broken pillar, over the gatehouse hall's floor (deepest along its walls) and beside the heaps outside it |
| Petrified Remains | faces 4 ways | a grey stone adventurer kneeling with its arms up, its face frozen in a scream | pickaxe (as stone) | 6 round the Beholder cavern's walls, facing the middle (as well as the 5 armour-stand statues) |
| Eye-Stalk Growth | grows on any face | pink-purple stalks ending in closed eyes; now and then one eye opens for a moment | hoe or hand; 0.2 hardness, no collision | about 6% of the open cells against the Beholder cavern's walls and ceiling |

- **Collapsed Rubble** stacks like snow layers: using rubble on the top of a rubble block adds a layer, up to 8 (a full block). It's solid to its full height (up to 4 layers you walk up it without jumping), never melts, needs a full block or a full stack of rubble under it, and drops one rubble per layer. A single layer gives way to other blocks, as snow does.
- **Eye-Stalk Growths** are placed and broken like amethyst clusters: they point away from the face they're on, pop off when that block goes, and can be waterlogged. An arrow hitting one doesn't chime.
- **Petrified Remains** face the player who places them.
- Every block drops itself (slabs drop 2 when double).
- Cracked Runic Deepslate Bricks are in `#dndclasses:secret_door`, so a sneaking Dwarf's stonecunning outlines the Ruin's bricked-up doorways ([Passive racial traits](../races/passive-traits.md)).

## Where to find it
- Mine them from Dwarven Ruins and Beholder lairs generated after this update (older chunks keep their old blocks).
- Craft:
  - Mossy Runic Deepslate Bricks: Runic Deepslate Bricks beside a moss block.
  - Cracked Runic Deepslate Bricks: smelt Runic Deepslate Bricks.
  - Tarnished Gilding: gilded blackstone over polished deepslate makes 2.
  - Ray-glazed Stone: smelt polished deepslate.
  - Collapsed Rubble: 3 cobbled deepslate in a row make 6 layers.
  - The stairs, slabs and walls from their base block, at a crafting table or a stonecutter, as for any [block family](../dev/building-blocks.md).
- Petrified Remains and Eye-Stalk Growths have no recipe: they only come from Beholder lairs (or the creative tab).

## For developers
- Families: `ModBlockFamilies.CRACKED_RUNIC_DEEPSLATE_BRICKS`, `MOSSY_RUNIC_DEEPSLATE_BRICKS`, `TARNISHED_GILDING` (`axisBase()`) and `RAY_GLAZED_STONE`, with their resources from `tools/block_families.py`.
- One-off blocks: `Registry/RuinBlocks` (`COLLAPSED_RUBBLE`, `PETRIFIED_REMAINS`, `EYE_STALK_GROWTH`, added to the Building Blocks tab after the families), `Blocks/RubbleBlock` (`LAYERS`), `Blocks/PetrifiedRemainsBlock`, `Blocks/EyeStalkBlock` (an `AmethystClusterBlock`, on the cutout layer). Their blockstates, models and loot tables are hand-written; the smelting and rubble recipes too.
- Textures: `tools/ruin_block_textures.py` (reuses the runic brick drawing from `tools/dwarven_block_textures.py`; the eye-stalk is a 3-frame `.mcmeta` animation, the statue's face is the texture's top-left 6x4).
- Placement:
  - `DungeonTheme.RuinPalette` (walls, surface, `secretWall`, `stairs`, `pillar`, and the new `Palette.doorFrame()`, the trim unless a theme overrides it).
  - `RuinDressing.rubbleLayers(n)` and `rubbleDrift`, called from `rubble` and `brokenColumn`; `EntrancePiece` (the gatehouse).
  - `BeholderShaftPiece`, `BeholderCavernPiece` (`wall`, the tunnel, `eyeStalkAt` with `EYE_STALK_CHANCE` 0.06, `remains` with `REMAINS` 6, from its own random so the rest of the cavern keeps its layout).
- Devscripts: `devscripts/ruin-blocks.txt` (every block on a platform, rubble stacking with `use`, drops, recipes, the eye-stalk popping off; screenshots), `devscripts/ruin-blocks-survey.txt` (a `/place`d Ruin's gatehouse and rooms and a `/place`d Beholder lair; use a scratch run dir).
