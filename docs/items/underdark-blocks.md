# Underdark blocks
The [Underdark](../systems/underdark.md)'s natural blocks: zurkhwood stalks and caps, glowing Nightlight caps, Fungal Loam, Faerzress Stone, the small Bluecap and Trillimac mushrooms, and Zurkhwood Planks to build with. Its plants and lake life: spores that grow giant fungi, Ripplebark shelves and Trillimac Slices to eat, water-storing Barrelstalks, glowing Waterorbs, Faerie Lichen and Spore Drips, and Pearl-Clams with Darklake Pearls. The natural blocks and plants are in the main **DnD** creative tab, the planks family in **DnD Building Blocks**.

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

Every block above drops itself when broken (the caps too, unlike vanilla mushroom blocks), except the Trillimac, which gives Trillimac Slices (below), and the Nightlight Cap, which also drops Nightlight Spores 8% of the time.

![The plants: Ripplebark at ages 0-2 and Faerie Lichen on a wall, a full and an empty Barrelstalk, a Waterorb, a Spore Drip under a Nightlight Cap, a Pearl-Clam, Zurkhwood Gills, both spores and a Trillimac](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/underdark-flora/underdark-flora-showcase.png)

### Plants, fungi and lake life
| Block | Looks like | Notes |
|-|-|-|
| **Zurkhwood Gills** | tan gills | the underside of a giant zurkhwood's cap. Drops **Zurkhwood Spores** 5% of the time (+2% per Fortune level), itself with Silk Touch. Axe or hoe |
| **Zurkhwood Spores** | a puff of violet spore-fluff | a sapling for a giant zurkhwood. Plant it on Fungal Loam or mycelium where the block light is 12 or less (sky light doesn't count). It grows on its own 1 random tick in 7, or with bone meal 45% of the time |
| **Nightlight Spores** | a puff of glowing blue fluff (light 3) | the same for a nightlight tower. Drop from Nightlight Caps (8%) |
| **Bluecap** (bone meal) | | bone meal grows it into a small nightlight tower (stem 6-9 tall) 40% of the time |
| **Ripplebark** | a rippled orange-brown shelf fungus on a wall | hangs on the side of any solid block, like cocoa. Grows through 3 ages (1 random tick in 5, or bone meal). Right-click a grown one to pick 1-2 Ripplebark; it starts again from age 0. Breaking it drops 1, or 2-3 when grown. The Ripplebark item is food (3 hunger, 0.3 saturation) and plants it back on a wall |
| **Barrelstalk** | a bulbous ribbed fungus barrel 2 blocks tall, dark water in its open top | stores water: a glass bottle gets a water bottle, a bucket a water bucket. Either empties it, and it fills again after 6000 ticks (5 minutes). Stands on Fungal Loam, mycelium, gravel, clay, moss, dirt, deepslate and tuff |
| **Waterorb** | a float of pale glowing spheres | light 4. Goes on water like a lily pad |
| **Faerie Lichen** | violet glowing lichen strands | light 9 (glow lichen is 7). Grows on any face and spreads with bone meal like glow lichen; shears to collect |
| **Glowing Spore Drip** | a hanging string of glowing blue spores | light 6. Hangs under a solid block (nightlight caps); breaks if that's removed. Lets a spore fall now and then. Drops **Glow Spores** (Fortune adds more), itself with shears or Silk Touch |
| **Pearl-Clam** | a dark clam shell bedded in grey clay | in the Darklake's lake beds. Drops a **Darklake Pearl**, itself with Silk Touch. Shovel |

| Item | Use |
|-|-|
| **Trillimac Slice** | breaking a Trillimac gives 1-2 (the plant itself with shears or Silk Touch). Food: 2 hunger, 0.1 saturation |
| **Roasted Trillimac** | a Trillimac Slice smelted (furnace, smoker or campfire). Food: 6 hunger, 0.6 saturation |
| **Ripplebark** | food: 3 hunger, 0.3 saturation; places the shelf |
| **Glow Spores** | brew an Awkward Potion into a Potion of Night Vision instead of a golden carrot (see [Potions and brewing](potions-and-brewing.md)) |
| **Darklake Pearl** | a trade good (the duergar buy them, once they're in) |

Trillimac Slices, Ripplebark and Zurkhwood Gills go in a composter.

## Where to find it
- In the [Underdark](../systems/underdark.md#regions): giant zurkhwood (with gills) and nightlight towers (with spore drips), Fungal Loam, Bluecaps, Trillimacs, Ripplebark, Barrelstalks and some Faerie Lichen in the Fungal Forest; Trillimacs and the odd giant zurkhwood in the Mycelial Caverns; Faerzress Stone and lots of Faerie Lichen in the Faerzress Hollows, Bluecaps there too; Waterorbs, seagrass, Pearl-Clams, Barrelstalks, Ripplebark and shore nightlight towers round the Darklake's lakes.
- Recipes: Roasted Trillimac from a Trillimac Slice in a furnace (10 s), smoker (5 s) or campfire (30 s).
- Recipes: 1 Zurkhwood Stem makes 4 Zurkhwood Planks. The planks' stairs, slab, fence, fence gate, door and trapdoor craft like other wood's (see [Building blocks](../dev/building-blocks.md)).
- The creative tabs.

## For developers
- `classes/Registry/UnderdarkBlocks` (one-offs) and `ModBlockFamilies.ZURKHWOOD_PLANKS`. Block classes: `classes/Blocks/underdark/FaerzressStoneBlock`, `UnderdarkFungusBlock`.
- Resources: `tools/underdark_blocks.py` (one-off blocks, their tags and the biome names) and `tools/block_families.py` (the planks family). Textures: `tools/underdark_block_textures.py`.
- The plants are in `classes/Registry/UnderdarkFlora` (kept apart from `UnderdarkBlocks`, which the building-blocks ticket extends). Block classes: `classes/Blocks/underdark/GiantFungusSporesBlock` (spores; `growFeature` swaps a block for a configured feature, put back if it doesn't fit), `BluecapBlock`, `RipplebarkBlock`, `BarrelstalkBlock`, `WaterorbBlock`, `FaerieLichenBlock`, `SporeDripBlock`. Resources: `tools/underdark_flora.py`; textures: `tools/underdark_flora_textures.py`. The giant fungi are features: see [The Underdark](../systems/underdark.md#for-developers).
- Spores and Bluecaps grow the same configured features worldgen places (`underdark/giant_zurkhwood`, `underdark/nightlight_tower`, `underdark/small_nightlight_tower`) and log `[Underdark] grew <feature> at x y z`.
- Devscript `underdark-flora.txt`: a showcase of the plants, their drops, harvesting Ripplebark, a Barrelstalk's bottle and bucket, spores and a Bluecap grown with bone meal, Roasted Trillimac smelted and eaten, Glow Spores brewed into Night Vision, and the Underdark's flora (see [The Underdark](../systems/underdark.md)).
- Devscript `underdark-blocks.txt`: a showcase, `/loot` drops for every block and shape, breaking each natural block with its tool in survival and picking it up, the stem-to-planks recipe in a crafting table, a Bluecap on Fungal Loam in full daylight (and not on sand), and the Nightlight Cap's light in the Underdark (F3).
