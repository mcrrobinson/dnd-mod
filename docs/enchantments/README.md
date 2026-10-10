# Enchantments
The mod's own enchantments. At an enchanting table they only come up for an [Artificer](../classes/artificer.md); everyone else's rolls skip them. The table's hint on each button comes from the same filtered roll, so it always names something you can actually get. Once enchanted, an item works for anyone.

![Each of the mod's enchantments on an item it can go on: Lunge II, Tree Feller, Returning, Smite Dragons V, Invulnerability II, Grid Miner II, Vampiric III and Featherfall III](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/enchantments-overview.png)

| Enchantment | Max level | Goes on | Effect |
|-|-|-|-|
| [Lunge](lunge.md) | II | Swords | Right click to dash forward; 5 s cooldown |
| [Invulnerability](invulnerability.md) | II | Swords | Right click to become invulnerable for 2 s; 20 s cooldown (10 s at II) |
| [Tree Feller](tree-feller.md) | I | Axes | Breaking a log fells the connected logs |
| [Grid Miner](grid-miner.md) | II | Pickaxes and shovels | Breaking a block mines up to 16 (II: 48) connected blocks the tool can mine |
| [Returning](returning.md) | I | Tridents (snowballs, eggs and ender pearls by anvil) | Thrown items go back to the thrower's inventory when they land |
| [Vampiric](vampiric.md) | III | Swords and axes | Melee hits heal you for 10% of the damage dealt per level |
| [Smite Dragons](smite-dragons.md) | V | Swords and axes | +2.5 melee damage per level against dragons |
| [Featherfall](featherfall.md) | III | Boots | Cuts fall damage by 50-90%; slow falling at II-III |

Pages for enchantments in open PRs are listed in the [docs index](../README.md#enchantments).

## For developers
- Registry: `Registry/ModEnchantments`. Artificer-only rolls and item filters: `mixin/EnchantingTableMixin` (replaces the handler's `generateEnchantments`, used for both hint and roll). Devscripts: `enchants-check.txt`, `headless-input-check.txt`.
