# Grid Miner
Mine out a whole area by breaking one block.

## How it works
- Breaking a block with a Grid Miner **pickaxe or shovel** also breaks the connected blocks the tool is right for, nearest first. Connected means the six faces plus the edge diagonals.
  - Level I: up to 16 extra blocks, at most 2 steps from the first block.
  - Level II: up to 48 extra blocks, at most 4 steps away.
- Each extra block is mined as if you broke it yourself: Fortune and Silk Touch apply, each block costs durability (it stops when the tool breaks), creative mode drops nothing, and protection/claim mods can refuse blocks.

## Where to find it / How to get it
- Enchanting table, as an [Artificer](../classes/artificer.md). Only on pickaxes and shovels.

## Commands
- `/enchant @s dndclasses:grid_miner 1`

## Known limitations
- Bedrock and other unbreakable blocks are skipped; any other block the tool is right for counts, including ores and obsidian.

## For developers
- `GridMiner.java` (`findConnected`, `breakAsPlayer` via `ServerPlayerInteractionManager.tryBreakBlock`, shared with Tree Feller), `Enchantments/GridMinerEnchantment`. Devscript: `devscripts/enchants-check.txt`.
