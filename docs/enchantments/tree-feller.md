# Tree Feller
Fell a whole tree by breaking one log.

## How it works
- Breaking a log with a Tree Feller **axe** also breaks every connected log (up, down and the four sides), up to 10 steps from the first one, and drops them as items.
- Works on oak, spruce, birch, jungle, acacia, dark oak, mangrove and cherry logs, and crimson and warped stems.

## Where to find it / How to get it
- Enchanting table, as an [Artificer](../classes/artificer.md). It rolls on swords, though.

## Commands
- `/give @s diamond_axe{Enchantments:[{id:"dndclasses:tree_feller",lvl:1}]}` (`/enchant` refuses axes)

## Known limitations
- Its enchanting target is swords, so it can't normally end up on an axe. Stripped logs and wood blocks aren't counted.

## For developers
- `TreeFeller.java`.
