# Building blocks
Decorative block families: a material plus its shapes (stairs, slab, wall, ...), registered from one Java call, with their resources written by a Python script. This is the groundwork for the Building Blocks cards, which each add their structure's materials and swap them into the pieces.

The first family is **Runic Deepslate Bricks** (base, stairs, slab and wall), the dwarves' fortress stone: deepslate bricks with gold-inlaid runes. It's craftable (8 deepslate bricks round a gold nugget make 8) and listed in the **DnD Building Blocks** creative tab. The [Dwarven Fortresses](../structures/dwarven-fortresses.md#building-blocks) are built from it, with their other blocks.

## How it works
- A family is declared in `ModBlockFamilies` with `BlockFamily.builder(name, vanillaBlock)`. Every shape copies the vanilla block's settings (hardness, blast resistance, tool requirement, sound, map colour).
- Ids follow vanilla: the base is `<name>`, the shapes `<stem>_<shape>`. The stem is the name minus the trailing "s" for bricks and tiles (`runic_deepslate_bricks` gives `runic_deepslate_brick_stairs`); `.stem(...)` overrides it.
- Shapes:

| Builder call | Id | Block | Textures (`textures/block/`) |
|---|---|---|---|
| (always) | `<name>` | plain block | `<name>` |
| `.axisBase()` | `<name>` | lies along an axis, like a log | `<name>`, `<name>_top` |
| `.stairs()` | `<stem>_stairs` | stairs | the base's |
| `.slab()` | `<stem>_slab` | slab (double slab shows the base) | the base's |
| `.wall()` | `<stem>_wall` | wall | the base's |
| `.pillar()` | `<stem>_pillar` | pillar beside the base, like the quartz pillar | `<stem>_pillar`, `<stem>_pillar_top` |
| `.fence()` | `<stem>_fence` | fence | the base's |
| `.fenceGate(WoodType)` | `<stem>_fence_gate` | fence gate with that wood's sounds | the base's |
| `.door(BlockSetType)` | `<stem>_door` | door, rendered cut out | `<stem>_door_top`, `<stem>_door_bottom`, and `textures/item/<stem>_door` |
| `.trapdoor(BlockSetType)` | `<stem>_trapdoor` | trapdoor, rendered cut out | `<stem>_trapdoor` |

  `.stoneShapes()` is stairs, slab and wall. An iron-like `BlockSetType` (`IRON`) makes a door or trapdoor that only redstone opens.
- Loot: every block drops itself, so mining a family block out of a structure gives the block, no silk touch needed. Slabs drop 2 when double, doors drop from the lower half only, and all of them except slabs need to survive the explosion that broke them, as in vanilla.
- Recipes, for every family: stairs (6 make 4), slab (3 make 6), wall (6 make 6), pillar (2 stacked make 2), fence (4 base and 2 sticks make 3), fence gate (2 base and 4 sticks make 1), door (6 make 3) and trapdoor (6 make 2). Stone families also stonecut 1 base into 1 stairs, 2 slabs, 1 wall or 1 pillar. A family can also have a crafting recipe for its base from vanilla items (`craft` in the script).
- Tags: stone families go in `mineable/pickaxe`, wood families in `mineable/axe`. Shapes go in the vanilla block and item tags for their shape (`stairs`, `slabs`, `walls`, `fences`, `fence_gates`, `doors`, `trapdoors`; the `wooden_*` ones for wood families), so walls and fences join up with vanilla ones.

## Where to find it
- The **DnD Building Blocks** creative tab lists every family, each family's shapes together.
- Survival: craft them, mine them from structures, or buy them from merchants (once a structure card adds the offer, see below).

## For developers
### Adding a family
1. Declare it in `ModBlockFamilies`:
   ```java
   public static final BlockFamily THATCH = BlockFamily.builder("thatch", Blocks.HAY_BLOCK).stairs().slab().build();
   ```
2. Add the same family to `FAMILIES` in `tools/block_families.py` (same name, stem and shapes; `material='wood'` for axe-mined wood, `material='hide'` for axe-mined blocks that aren't wood: plain shape tags, no stonecutter; `tool='hoe'` or `'shovel'` for another mineable tag, and `cut=False` for a stone family that shouldn't stonecut, e.g. thatch; `shape_titles={'trapdoor': 'Tent Canvas Flap'}` to name a shape something other than `<stem title> <Shape>`) and run `python3 tools/block_families.py`. It writes the blockstates, block and item models, loot tables and recipes, adds the lang names and tag entries, and leaves everything else alone, so it's safe to rerun.
3. Draw the textures listed in the table above in the area's texture script (`tools/<area>_block_textures.py`, 16x16 with `pngio`).
4. `./gradlew build`. On startup, `BlockFamily.register()` logs `[BlockFamily] <id> has no generated resources; run tools/block_families.py` for any block whose blockstate or loot table is missing.

The generator's blockstates and models match vanilla's: generating the oak, deepslate brick and quartz families with it gives vanilla's own files for every shape.

### Placing them in structures
`PieceBlocks` (static-imported by the pieces) takes a family:
- `stairs(family, facing, upsideDown)`, `slab(family, top)`, `wall(family)`, `pillar(family, axis)` (the pillar shape, or the base for an `axisBase()` family)
- `door(block, facing, upper)` and `trapdoor(block, facing, top)` for either half of a closed door or trapdoor
- `family.base()`, `family.fenceGate()` etc. for the plain blocks; `facing(block, dir)` turns a fence gate.

`ModPiece.set` marks stairs, walls and fences for post-processing, so they join up with their neighbours once the chunk is done.

### Selling them
Merchant offers are built in each merchant's `*Trades` class (`TieflingTrades.pactbroker`, `ElfTrades`, `DragonbornTrades.elder`, `OrcTrades.quartermaster`, `TinkerTrades.tinker`, `InnkeeperTrades.create`); the dwarves barter from `gameplay/dwarf_barter`. Sell a stack with `NpcTrades.sell(emeralds, family.base(), count)`. Kin pay 25% less automatically (`KinPrices`), as for every offer at a racial home's merchants. Merchants save their offers, so add the new offer to the merchant's `addMissing` too (or give it one, as `ElfTrades` and `InnkeeperTrades` do), or merchants that already exist won't get it.

### Classes and files
- `classes/Registry/BlockFamily.java`: the builder, shapes, ids and registration
- `classes/Registry/ModBlockFamilies.java`: the families
- `classes/Registry/ModItemGroup.java`: `BUILDING_BLOCKS_ITEMGROUP`
- `classes/Client/DndClassesClient.java`: doors and trapdoors on the cutout render layer (`BlockFamily.cutoutBlocks()`)
- `world/gen/PieceBlocks.java`: the placement helpers
- `tools/block_families.py`: the resources; `tools/dwarven_block_textures.py`: the Dwarven Fortresses textures, `tools/dwarven_blocks.py`: the resources of their one-off blocks (`DwarvenBlocks`)
- `tools/goblin_tent_textures.py`: the goblin camp tents' textures (Stretched Hide, Tent Canvas, their flaps, the Tent Stake)
- `tools/lair_block_textures.py`: the Dragon & Frost lairs' textures and the resources of their one-off blocks (`LairBlocks`)
- `devscripts/block-families.txt`: places the sample family's shapes, checks the drops and the stonecutter, and takes screenshots
