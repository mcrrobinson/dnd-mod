# Underdark blocks
The [Underdark](../systems/underdark.md)'s natural blocks: zurkhwood stalks and caps, glowing Nightlight caps, Fungal Loam, Faerzress Stone, the small Bluecap and Trillimac mushrooms, and Zurkhwood Planks to build with; and the building blocks the drow and duergar build with (Drowstone, Spidersilk, Greysteel, forge brick, braziers and cages). The natural blocks are in the main **DnD** creative tab, the planks family and the building blocks in **DnD Building Blocks**.

![Zurkhwood Stem and Caps, a Nightlight Cap, Fungal Loam with a Bluecap and a Trillimac, Faerzress Stone, and the Zurkhwood Planks family](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/underdark-dimension/underdark-blocks-showcase.png)

## How it works
| Block | Looks like | Notes |
|-|-|-|
| **Zurkhwood Stem** | a purple-grey ribbed stalk | lies along an axis like a log. Crimson stem's hardness and sound, axe. Doesn't burn. |
| **Zurkhwood Planks** (and stairs, slab, fence, fence gate, door, trapdoor) | dusky lavender-grey boards | a [block family](../dev/building-blocks.md) with crimson planks' settings and sounds: like Nether wood, it doesn't burn |
| **Zurkhwood Cap** | a dusky violet-brown cap with pale specks, tan gills underneath | a huge-mushroom block: its skin shows on faces that touch air, gills on the rest. Brown mushroom block's hardness, axe |
| **Nightlight Cap** | a pale blue-white glowing cap | light level 13. Red mushroom block's hardness, axe |
| **Fungal Loam** | dark blue-grey spongy soil with white spore specks | doesn't spread or turn to dirt. Shovel. Vanilla mushrooms grow on it in any light (it's in `#minecraft:mushroom_grow_block`) |
| **Faerzress Stone** | deepslate veined with glowing violet | light level 5; now and then a violet mote drifts off an open face. Deepslate's hardness, pickaxe. In the Dwarf's [stonecunning](../races/passive-traits.md) tag |
| **Bluecap** | a small glowing blue mushroom | light level 6. Breaks instantly. Stands on Fungal Loam, mycelium, moss, dirt, deepslate, tuff, stone and Faerzress Stone, in any light, and never spreads |
| **Trillimac** | a small grey-tan mushroom | as the Bluecap, without the glow |

Every block drops itself when broken (the caps too, unlike vanilla mushroom blocks).

### Building blocks (drow and duergar)
![The building blocks: the drow's stone, lanterns and silk at the back, the duergar's steel, forge brick, brazier and cages in front](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/underdark-blocks/underdark-building-showcase.png)

| Block | Looks like | Notes |
|-|-|-|
| **Drowstone Bricks** (and stairs, slab, wall, pillar) | black bricks with a faint violet sheen; a fluted pillar | polished blackstone bricks' hardness and sound. The drow towers' stone |
| **Polished Faerzress** (and stairs, slab, wall) | Faerzress Stone ground smooth, its violet vein still glowing | light 5. Polished deepslate's hardness |
| **Spider Sigil Stone** | drowstone carved with an eight-legged sigil, the inlay glowing violet | light 4. Faces you when placed, like glazed terracotta |
| **Faerie Fire Lantern** | a black-iron lantern with a violet flame | light 12. Stands on a block or hangs under one, like a lantern |
| **Spidersilk Block** / **Carpet** | pale silver woven silk | wool's hardness and sound |
| **Spidersilk Curtain** | silk hung like a pane, with gaps | joins up like glass panes. No collision: you walk through it, but inside it you move at about 30% of your speed (a cobweb leaves you about 10%). Spiders and cave spiders (and the drow, once they exist) pass freely |
| **Web Bridge Planks** | a woven silk walkway | the top half of a block, like a top slab, so a bridge is flush with full-block floors. Breaks at once, like scaffolding |
| **Greysteel Plate** (and stairs, slab, trapdoor) | soot-dark steel plates with red-hot rivets | iron block's hardness. The trapdoor only opens to redstone, like iron's |
| **Basalt Forge Bricks** (and stairs, slab, wall) | dark basalt bricks with ember-glow mortar | lies along an axis like polished basalt. The glow is painted on: no light |
| **Duergar Brazier** | a black-iron bowl of glowing coals on a squat foot | light 14, smokes and crackles. Burns anything standing in it for 1 damage a second, except fire-immune mobs and the duergar |
| **Chain Cage** | a gate of black iron bars wrapped in chain | a one-block barred panel, facing the way you faced when you placed it. Opens by hand and swings against its side. A **locked** cage (as the captive pens place it) only opens to a Rogue's lockpick roll, the same Thieves' Tools check as a locked chest (DC 12, the usual retry wait). A picked cage stays unlocked. In creative it opens anyway, and sneak + use with an empty hand locks or unlocks it |
| **Duergar Steel Ingot** (item) | a dark steel ingot with a reddish sheen | 9 make a Greysteel Plate, and a plate breaks back into 9 |

## Where to find it
- In the [Underdark](../systems/underdark.md#regions): zurkhwood, nightlights, Fungal Loam, Bluecaps and Trillimacs in the Fungal Forest; Trillimacs in the Mycelial Caverns; Faerzress Stone in the Faerzress Hollows' floors and veins; Bluecaps there too.
- Recipes: 1 Zurkhwood Stem makes 4 Zurkhwood Planks. The planks' stairs, slab, fence, fence gate, door and trapdoor craft like other wood's (see [Building blocks](../dev/building-blocks.md)).
- Building block recipes:
  - Drowstone Bricks: 4 Faerzress Stone and 4 polished blackstone round a Spidersilk make 8. Drowstone Bricks and a glow ink sac make a Spider Sigil Stone (or stonecut one from Drowstone Bricks).
  - Polished Faerzress: 4 Faerzress Stone make 4, or stonecut Faerzress Stone into Polished Faerzress, its stairs, slabs (2) or wall.
  - Faerie Fire Lantern: 8 iron nuggets round a Faerzress Stone.
  - Spidersilk: 4 make a Spidersilk Block, 2 blocks make 3 carpets, 6 Spidersilk make 3 curtains, 3 Spidersilk over 3 Zurkhwood Planks make 6 Web Bridge Planks.
  - Greysteel Plate: 9 Duergar Steel Ingots. Basalt Forge Bricks: 8 polished basalt round a magma cream make 8.
  - Duergar Brazier: 6 iron ingots round a coal block (`ICI`, `III`, ` I `). Chain Cage: 4 iron bars and 2 chains make 2.
  - The shapes craft and stonecut like every family's (see [Building blocks](../dev/building-blocks.md)).
- Spidersilk drops from the drow. Until they're in the game, the recipes that use it don't load.
- Duergar Steel Ingots come from the duergar: the Duergar Factor sells them (8 emeralds), with Basalt Forge Bricks (16 for 4 emeralds) and, at Friendly, Greysteel Plate (2 for 12 emeralds).
- The creative tabs.

## For developers
- `classes/Registry/UnderdarkBlocks` (one-offs) and `ModBlockFamilies.ZURKHWOOD_PLANKS`. Block classes: `classes/Blocks/underdark/FaerzressStoneBlock`, `UnderdarkFungusBlock`.
- Resources: `tools/underdark_blocks.py` (one-off blocks, their tags and the biome names) and `tools/block_families.py` (the planks family). Textures: `tools/underdark_block_textures.py`.
- Building blocks: the families `ModBlockFamilies.DROWSTONE_BRICKS`, `POLISHED_FAERZRESS`, `GREYSTEEL_PLATE` and `BASALT_FORGE_BRICKS`; the one-offs in `UnderdarkBlocks` (`buildingBlocks()`, `DUERGAR_STEEL_INGOT`). Block classes: `classes/Blocks/underdark/SpidersilkCurtainBlock` (`SLOW`), `WebBridgeBlock`, `DuergarBrazierBlock` (`burns`), `ChainCageBlock` (`LOCK_DC`, the `locked` state). Resources: `tools/underdark_blocks.py` (one-offs and the recipes the family generator can't write) and `tools/block_families.py`; textures `tools/underdark_building_textures.py`.
- Hooks for the drow and duergar:
  - Entity tags `#dndclasses:brazier_immune` (holds `#dndclasses:faction/duergar_clans`, optional) and `#dndclasses:web_walkers` (spiders, cave spiders and `#dndclasses:faction/drow`, optional). The faction tags fill them once those mobs exist.
  - Recipes using `dndclasses:spidersilk` carry a `fabric:registry_contains` load condition, so they load once the drow's Spidersilk item is registered.
  - `entity/UnderdarkBuildingStock.duergarFactor(tier)`: the factor's building-block offers. The duergar factor's offer table and its `addMissing` call it.
  - `Lockpicking.pickLock(player, world, pos, dc, lockedMessage)` picks any block's lock with the chest rules; the cage uses it.
- Later Underdark tickets add their blocks to `UnderdarkBlocks`: the flora ticket (spores, gills, Ripplebark, ...).
- Devscript `underdark-blocks.txt`: a showcase, `/loot` drops for every block and shape, breaking each natural block with its tool in survival and picking it up, the stem-to-planks recipe in a crafting table, a Bluecap on Fungal Loam in full daylight (and not on sand), and the Nightlight Cap's light in the Underdark (F3).
- Devscript `underdark-building-blocks.txt`: a showcase, `/loot` drops, the Faerzress stonecutter, the lantern and Greysteel recipes, the Chain Cage's lock (non-Rogue, rigged fail, rigged pick, creative), the curtain's slow (the `speed` readout) and the brazier burning a zombie but not a blaze (or a husk tagged as a duergar by a local test datapack).
