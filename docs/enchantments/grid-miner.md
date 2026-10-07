# Grid Miner
Mine out a whole area by breaking one block.

## How it works
- Breaking a block with a Grid Miner **pickaxe or shovel** also breaks every connected block the tool is right for, up to 10 steps from the first block. Connected means the six faces plus the edge diagonals. The blocks drop as items.

## Where to find it / How to get it
- Enchanting table, as an [Artificer](../classes/artificer.md). Target: digging tools.

## Commands
- `/enchant @s dndclasses:grid_miner 1`

## Known limitations
- In solid stone it can clear a very large area in one go. It doesn't use extra durability for the extra blocks.

## For developers
- `GridMiner.java`.
