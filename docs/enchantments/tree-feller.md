# Tree Feller
Fell a whole tree by breaking one log.

## How it works
- Breaking a log with a Tree Feller **axe** also breaks every connected log (up, down and the four sides), up to 10 steps from the first one, and drops them as items.
- Works on oak, spruce, birch, jungle, acacia, dark oak, mangrove and cherry logs, and crimson and warped stems.

## Where to find it / How to get it
- Enchanting table, as an [Artificer](../classes/artificer.md), on an axe (or a book).
- Anvil: a Tree Feller book goes on any axe. Other tools refuse it.

## Commands
- `/enchant @s dndclasses:tree_feller` while holding an axe, or `/give @s diamond_axe{Enchantments:[{id:"dndclasses:tree_feller",lvl:1}]}`

## Known limitations
- Stripped logs and wood blocks aren't counted.
- The enchanting table's hint (the "Tree Feller...?" tooltip) comes from vanilla, so it can still name Tree Feller on a pickaxe, shovel or hoe, or for a non-Artificer. The roll you actually get only puts it on axes.

## For developers
- `TreeFeller.java`. `Enchantments/TreeFellerEnchantment` uses the `DIGGER` target (vanilla has no axe-only one) and narrows it to axes in `isAcceptableItem`; `mixin/EnchantingTableMixin` checks both.
