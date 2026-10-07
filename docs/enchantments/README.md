# Enchantments
The mod's own enchantments. At an enchanting table they only come up for an [Artificer](../classes/artificer.md); everyone else's rolls skip them. Once enchanted, an item works for anyone.

| Enchantment | Max level | Goes on | Effect |
|-|-|-|-|
| [Lunge](lunge.md) | II | Swords | Right click to dash forward; 5 s cooldown |
| [Invulnerability](invulnerability.md) | II | Swords | Right click to become invulnerable for 2 s |
| [Tree Feller](tree-feller.md) | II | Swords (works on axes) | Breaking a log fells the connected logs |
| [Grid Miner](grid-miner.md) | II | Digging tools (works on pickaxes and shovels) | Breaking a block mines the connected blocks the tool can mine |
| [Returning](returning.md) | I | Tridents (snowballs, eggs and ender pearls by anvil) | Thrown items go back to the thrower's inventory when they land |
| [Vampiric](vampiric.md) | III | Swords and axes | Melee hits heal you for 10% of the damage dealt per level |
| [Smite Dragons](smite-dragons.md) | V | Swords and axes | +2.5 melee damage per level against dragons |

Pages for enchantments in open PRs are listed in the [docs index](../README.md#enchantments).

## Known limitations
- Lunge, Invulnerability, Tree Feller and Grid Miner are registered as the same enchantment class (common rarity, levels I-II), so level only matters for Lunge.
- Tree Feller can only be rolled on swords but only works on axes. Getting it on an axe needs commands or NBT.

## For developers
- Registry: `Registry/ModEnchantments`. Artificer-only rolls: `mixin/EnchantingTableMixin` (`getSomeEntries`). Devscript: `headless-input-check.txt`.
