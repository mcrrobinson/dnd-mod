# Underdark blocks
The [Underdark](../systems/underdark.md)'s natural blocks: zurkhwood stalks and caps, glowing Nightlight caps, Fungal Loam, Faerzress Stone, the small Bluecap and Trillimac mushrooms, and Zurkhwood Planks to build with. Its plants and lake life: spores that grow giant fungi, Ripplebark shelves and Trillimac Slices to eat, water-storing Barrelstalks, glowing Waterorbs, Faerie Lichen and Spore Drips, and Pearl-Clams with Darklake Pearls. And the building blocks the drow and duergar build with (Drowstone, Spidersilk, Greysteel, forge brick, braziers and cages). The natural blocks and plants are in the main **DnD** creative tab, the planks family and the building blocks in **DnD Building Blocks**.

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
| **Darklake Pearl** | a trade good: the [Duergar Factor](../mobs/duergar.md#trades) buys them |

Trillimac Slices, Ripplebark and Zurkhwood Gills go in a composter.

### Building blocks (drow and duergar)
![The building blocks: the drow's stone, lanterns and silk at the back, the duergar's steel, forge brick, brazier and cages in front](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/underdark-blocks/underdark-building-showcase.png)

| Block | Looks like | Notes |
|-|-|-|
| **Drowstone Bricks** (and stairs, slab, wall, pillar) | black bricks with a faint violet sheen; a fluted pillar | polished blackstone bricks' hardness and sound. The drow towers' stone |
| **Polished Faerzress** (and stairs, slab, wall) | Faerzress Stone ground smooth, its violet vein still glowing | light 5. Polished deepslate's hardness |
| **Spider Sigil Stone** | drowstone carved with an eight-legged sigil, the inlay glowing violet | light 4. Faces you when placed, like glazed terracotta |
| **Faerie Fire Lantern** | a black-iron lantern with a violet flame | light 12. Stands on a block or hangs under one, like a lantern |
| **Spidersilk Block** / **Carpet** | pale silver woven silk | wool's hardness and sound |
| **Spidersilk Curtain** | silk hung like a pane, with gaps | joins up like glass panes. No collision: you walk through it, but inside it you move at about 30% of your speed (a cobweb leaves you about 10%). Spiders, cave spiders and the [drow](../mobs/drow.md) pass freely |
| **Web Bridge Planks** | a woven silk walkway | the top half of a block, like a top slab, so a bridge is flush with full-block floors. Breaks at once, like scaffolding |
| **Greysteel Plate** (and stairs, slab, trapdoor) | soot-dark steel plates with red-hot rivets | iron block's hardness. The trapdoor only opens to redstone, like iron's |
| **Basalt Forge Bricks** (and stairs, slab, wall) | dark basalt bricks with ember-glow mortar | lies along an axis like polished basalt. The glow is painted on: no light |
| **Duergar Brazier** | a black-iron bowl of glowing coals on a squat foot | light 14, smokes and crackles. Burns anything standing in it for 1 damage a second, except fire-immune mobs and the duergar |
| **Chain Cage** | a gate of black iron bars wrapped in chain | a one-block barred panel, facing the way you faced when you placed it. Opens by hand and swings against its side. A **locked** cage (as the captive pens place it) only opens to a Rogue's lockpick roll, the same Thieves' Tools check as a locked chest (DC 12, the usual retry wait). A picked cage stays unlocked. In creative it opens anyway, and sneak + use with an empty hand locks or unlocks it. Cages stacked on one another with the same facing are one gate: they open, shut and unlock together. Opening or breaking a cage frees the [captives](../structures/drow-outposts.md#captives) behind it |
| **Duergar Steel Ingot** (item) | a dark steel ingot with a reddish sheen | 9 make a Greysteel Plate, and a plate breaks back into 9 |

## Where to find it
- In the [Underdark](../systems/underdark.md#regions): giant zurkhwood (with gills) and nightlight towers (with spore drips), Fungal Loam, Bluecaps, Trillimacs, Ripplebark, Barrelstalks and some Faerie Lichen in the Fungal Forest; Trillimacs and the odd giant zurkhwood in the Mycelial Caverns; Faerzress Stone and lots of Faerie Lichen in the Faerzress Hollows, Bluecaps there too; Waterorbs, seagrass, Pearl-Clams, Barrelstalks, Ripplebark and shore nightlight towers round the Darklake's lakes.
- Recipes: Roasted Trillimac from a Trillimac Slice in a furnace (10 s), smoker (5 s) or campfire (30 s).
- Recipes: 1 Zurkhwood Stem makes 4 Zurkhwood Planks. The planks' stairs, slab, fence, fence gate, door and trapdoor craft like other wood's (see [Building blocks](../dev/building-blocks.md)).
- Building block recipes:
  - Drowstone Bricks: 4 Faerzress Stone and 4 polished blackstone round a Spidersilk make 8. Drowstone Bricks and a glow ink sac make a Spider Sigil Stone (or stonecut one from Drowstone Bricks).
  - Polished Faerzress: 4 Faerzress Stone make 4, or stonecut Faerzress Stone into Polished Faerzress, its stairs, slabs (2) or wall.
  - Faerie Fire Lantern: 8 iron nuggets round a Faerzress Stone.
  - Spidersilk: 4 make a Spidersilk Block, 2 blocks make 3 carpets, 6 Spidersilk make 3 curtains, 3 Spidersilk over 3 Zurkhwood Planks make 6 Web Bridge Planks.
  - Greysteel Plate: 9 Duergar Steel Ingots. Basalt Forge Bricks: 8 polished basalt round a magma cream make 8.
  - Duergar Brazier: 6 iron ingots round a coal block (`ICI`, `III`, ` I `). Chain Cage: 4 iron bars and 2 chains make 2.
  - The shapes craft and stonecut like every family's (see [Building blocks](../dev/building-blocks.md)).
- Spidersilk drops from the [drow](../mobs/drow.md).
- Duergar Steel Ingots come from the [duergar](../mobs/duergar.md): a Duergar Soldier killed by a player drops one 3% of the time (+1% per Looting level), and the Duergar Factor sells them (8 emeralds), with Basalt Forge Bricks (16 for 4 emeralds) and, at Friendly, Greysteel Plate (2 for 12 emeralds).
- The creative tabs.

## For developers
- `classes/Registry/UnderdarkBlocks` (one-offs) and `ModBlockFamilies.ZURKHWOOD_PLANKS`. Block classes: `classes/Blocks/underdark/FaerzressStoneBlock`, `UnderdarkFungusBlock`.
- Resources: `tools/underdark_blocks.py` (one-off blocks, their tags and the biome names) and `tools/block_families.py` (the planks family). Textures: `tools/underdark_block_textures.py`.
- Building blocks: the families `ModBlockFamilies.DROWSTONE_BRICKS`, `POLISHED_FAERZRESS`, `GREYSTEEL_PLATE` and `BASALT_FORGE_BRICKS`; the one-offs in `UnderdarkBlocks` (`buildingBlocks()`, `DUERGAR_STEEL_INGOT`). Block classes: `classes/Blocks/underdark/SpidersilkCurtainBlock` (`SLOW`), `WebBridgeBlock`, `DuergarBrazierBlock` (`burns`), `ChainCageBlock` (`LOCK_DC`, the `locked` state). Resources: `tools/underdark_blocks.py` (one-offs and the recipes the family generator can't write) and `tools/block_families.py`; textures `tools/underdark_building_textures.py`.
- Hooks for the drow and duergar:
  - Entity tags `#dndclasses:brazier_immune` (holds `#dndclasses:faction/duergar_clans`) and `#dndclasses:web_walkers` (spiders, cave spiders and `#dndclasses:faction/drow`).
  - The Spidersilk recipes use the drow's `dndclasses:spidersilk` (`Drow.SPIDERSILK`).
  - `entity/UnderdarkBuildingStock`: the factor's building-block offers, `always()` (forge bricks, steel ingot) and `friendly()` (Greysteel Plate); `duergarFactor(tier)` gives both for a tier. `DuergarTrades.factor()`/`friendly()` and their `addMissing` take them, so saved factors get them on load.
  - The Duergar Soldier's loot table (`loot_tables/entities/duergar_soldier.json`) has the 3% Duergar Steel Ingot pool.
  - `Lockpicking.pickLock(player, world, pos, dc, lockedMessage)` picks any block's lock with the chest rules; the cage uses it.
- The plants are in `classes/Registry/UnderdarkFlora` (kept apart from `UnderdarkBlocks`, which the building-blocks ticket extends). Block classes: `classes/Blocks/underdark/GiantFungusSporesBlock` (spores; `growFeature` swaps a block for a configured feature, put back if it doesn't fit), `BluecapBlock`, `RipplebarkBlock`, `BarrelstalkBlock`, `WaterorbBlock`, `FaerieLichenBlock`, `SporeDripBlock`. Resources: `tools/underdark_flora.py`; textures: `tools/underdark_flora_textures.py`. The giant fungi are features: see [The Underdark](../systems/underdark.md#for-developers).
- Spores and Bluecaps grow the same configured features worldgen places (`underdark/giant_zurkhwood`, `underdark/nightlight_tower`, `underdark/small_nightlight_tower`) and log `[Underdark] grew <feature> at x y z`.
- Devscript `underdark-flora.txt`: a showcase of the plants, their drops, harvesting Ripplebark, a Barrelstalk's bottle and bucket, spores and a Bluecap grown with bone meal, Roasted Trillimac smelted and eaten, Glow Spores brewed into Night Vision, and the Underdark's flora (see [The Underdark](../systems/underdark.md)).
- Devscript `underdark-blocks.txt`: a showcase, `/loot` drops for every block and shape, breaking each natural block with its tool in survival and picking it up, the stem-to-planks recipe in a crafting table, a Bluecap on Fungal Loam in full daylight (and not on sand), and the Nightlight Cap's light in the Underdark (F3).
- Devscript `underdark-building-blocks.txt`: a showcase, `/loot` drops, the Faerzress stonecutter, the lantern and Greysteel recipes, the Chain Cage's lock (non-Rogue, rigged fail, rigged pick, creative), the curtain's slow (the `speed` readout) and the brazier burning a zombie but not a Duergar Soldier (30 health) or a blaze.
