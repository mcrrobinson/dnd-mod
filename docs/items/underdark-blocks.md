# Underdark blocks
The [Underdark](../systems/underdark.md)'s natural blocks: zurkhwood stalks and caps, glowing Nightlight caps, Fungal Loam, Faerzress Stone, the small Bluecap and Trillimac mushrooms, and Zurkhwood Planks to build with. The natural blocks are in the main **DnD** creative tab, the planks family in **DnD Building Blocks**.

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

## Where to find it
- In the [Underdark](../systems/underdark.md#regions): zurkhwood, nightlights, Fungal Loam, Bluecaps and Trillimacs in the Fungal Forest; Trillimacs in the Mycelial Caverns; Faerzress Stone in the Faerzress Hollows' floors and veins; Bluecaps there too.
- Recipes: 1 Zurkhwood Stem makes 4 Zurkhwood Planks. The planks' stairs, slab, fence, fence gate, door and trapdoor craft like other wood's (see [Building blocks](../dev/building-blocks.md)).
- The creative tabs.

## For developers
- `classes/Registry/UnderdarkBlocks` (one-offs) and `ModBlockFamilies.ZURKHWOOD_PLANKS`. Block classes: `classes/Blocks/underdark/FaerzressStoneBlock`, `UnderdarkFungusBlock`.
- Resources: `tools/underdark_blocks.py` (one-off blocks, their tags and the biome names) and `tools/block_families.py` (the planks family). Textures: `tools/underdark_block_textures.py`.
- Later Underdark tickets add their blocks to `UnderdarkBlocks`: the flora ticket (spores, gills, Ripplebark, ...) and the building-blocks ticket (Drowstone, Spidersilk, Greysteel, ...).
- Devscript `underdark-blocks.txt`: a showcase, `/loot` drops for every block and shape, breaking each natural block with its tool in survival and picking it up, the stem-to-planks recipe in a crafting table, a Bluecap on Fungal Loam in full daylight (and not on sand), and the Nightlight Cap's light in the Underdark (F3).
