# Hobbit building blocks
The Shire's own building materials and kitchen clutter: golden thatch, half-timbered wattle and daub, cut turf, a stone arch for round doors, a sooty hearth, pipe-weed, and two blocks you can use: the **Larder Shelf** (a 9-slot food store) and the **Ale Cask** (draw a Mug of Ale with a glass bottle). [Hobbit Villages](../structures/hobbit-villages.md) and the Green Dragon inn are built from them.

![The hobbit building blocks](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/hobbit-blocks/hobbit-blocks-left.png)

## How it works
### Materials
| Block | Shapes | Tool | Like | Looks |
|---|---|---|---|---|
| Thatch | stairs, slab | hoe | hay bale | golden straw in uneven bundles, frayed at the bottom |
| Mossy Thatch | stairs, slab | hoe | hay bale | thatch with green-grey moss in its top third |
| Wattle and Daub | | pickaxe | mud bricks | cream plaster, woven wattle showing faintly through |
| Timbered Wattle and Daub | | pickaxe | mud bricks | wattle and daub with a dark oak beam along the top and down the left side, so rows of it make a timber frame |
| Smial Turf Wall | | shovel | dirt (grass sounds) | a grass band over packed earth with roots and pebbles; grass on top, dirt underneath |
| Round Door Frame Stone | stairs, slab | pickaxe | smooth stone | smooth pale stone with a lighter rim |
| Round Door Keystone | | pickaxe | smooth stone | frame stone with a wedge-shaped keystone |
| Hearth Stone | | pickaxe | bricks | red-brown brick, soot-blackened in the middle, an orange glow in the lower joints |
| Pipe-weed Bundle | (lies along an axis, like a hay bale) | hoe | hay bale | dried brown-green leaves tied with two bands of twine |

- Hardness, blast resistance and sounds are copied from the block in the "Like" column.
- Thatch, Mossy Thatch (all shapes) and Pipe-weed Bundles burn like hay bales (catch 60, spread 20).
- Smial Turf Wall counts as dirt (`#minecraft:dirt`): grass, flowers and saplings grow on it.
- Every block drops itself, no silk touch needed.

### Larder Shelf
- Dark oak shelves with a wheel of cheese, two jars of preserves and a hanging ham on the front; it faces you when placed.
- Right-click to open it like a barrel: **9 slots**. Hobbit structures fill it from the hobbit food loot tables when it's first opened.
- Breaking it drops what's inside. Comparators read how full it is.

### Ale Cask
- An oak barrel on its side with iron hoops, a painted green dragon on its head and a brass tap.
- Right-click it with a **glass bottle** to draw a **Mug of Ale** ([the innkeeper's ale](../structures/hobbit-tavern.md#mug-of-ale)). The bottle is used up.
- A cask holds **4 mugs**. Each random tick (about once every 68 s at the default `randomTickSpeed` of 3) has a 1 in 4 chance to add a mug back, so a dry cask refills about 4 times a day and is full again in roughly an in-game day.
- Right-click it with anything else for its level on the action bar: "N mugs of ale left. Draw one with a glass bottle." or "The cask is dry. It fills again in a few hours."
- Comparators read its level (0 to 15).

### Hanging Herbs and Onion String
- Bundles of herbs, or onions on a braided twine, drawn like a vine (no collision). They break instantly and burn.
- Place them under the underside of a block or under another bundle. They drop when the block they hang from goes.

## Where to find it
- **Structures**: every block is placed in [Hobbit Villages](../structures/hobbit-villages.md): the Green Dragon's roof, walls, door, hearth and bar, the smials' fronts, doors, hearths and kitchens, and the market stalls. Mine them out; they drop themselves.
- **The innkeeper** sells 8 Thatch for 1 emerald ([Hobbit Tavern](../structures/hobbit-tavern.md)).
- **The DnD Building Blocks** creative tab.
- **Crafting**:

| Result | Recipe |
|---|---|
| 4 Thatch | 4 wheat (2x2), or 1 hay bale (shapeless) |
| 1 Mossy Thatch | Thatch + vine, or Thatch + moss block (shapeless) |
| 2 Wattle and Daub | 2 sticks and 2 clay balls, crosswise in a 2x2 |
| 4 Timbered Wattle and Daub | 4 Wattle and Daub round a dark oak log (plus shape) |
| 2 Smial Turf Wall | grass over 2 dirt (column) |
| 4 Round Door Frame Stone | 4 smooth stone (2x2); it also stonecuts into stairs (1) and slabs (2) |
| 2 Round Door Keystone | 2 Round Door Frame Stone (column) |
| 4 Hearth Stone | 4 bricks round a charcoal (plus shape) |
| 1 Pipe-weed Bundle | 8 ferns round a string |
| 1 Larder Shelf | 4 dark oak slabs round a barrel (plus shape) |
| 1 Ale Cask | iron ingot, barrel, iron ingot (row) |
| 2 Hanging Herbs | string over 2 grass (column) |
| 2 Onion String | string over 2 potatoes (column) |

Stairs (6 make 4) and slabs (3 make 6) use the usual recipes ([Building blocks](../dev/building-blocks.md)).

## Known limitations
- Hobbits don't use the Larder Shelf yet: their food-sharing is unchanged. It's there for a later "share their food" behaviour to hook into.

## For developers
- Families (`classes/Registry/ModBlockFamilies.java`, "Hobbit Villages" block): `THATCH`, `MOSSY_THATCH`, `WATTLE_AND_DAUB`, `TIMBERED_WATTLE_AND_DAUB`, `SMIAL_TURF_WALL`, `ROUND_DOOR_FRAME_STONE`, `ROUND_DOOR_KEYSTONE`, `HEARTH_STONE`, `PIPEWEED_BUNDLE`. Their resources come from `tools/block_families.py` (thatch and pipe-weed use its `tool='hoe', cut=False` options, turf `tool='shovel'`).
- One-off blocks: `classes/Blocks/hobbit/` (`HobbitBlocks` registers them, the flammability and their creative-tab entries; `LarderShelfBlock` + `LarderShelfBlockEntity`, `AleCaskBlock` with `MAX_MUGS` and `REFILL_ODDS`, `HangingBundleBlock`). Resources: `tools/hobbit_blocks.py` (models, blockstates, loot, the extra recipes, tags, lang).
- Textures: `tools/hobbit_block_textures.py` (`--preview DIR` also writes 16x previews).
- Structure helpers in `HobbitPiece`: `thatch(x, y, z)` (Mossy Thatch for about 1 block in 5, hashed from the position and the piece seed, so it uses no random draws), `Builder.roundDoor` (the stone arch), `Builder.larder` and `Builder.herbs`.
- Devscripts: `hobbit-blocks.txt` (close-ups, the cask, the larder, herbs, drops, recipes, the thatch trade), `hobbit-blocks-village.txt` (a freshly generated village).
