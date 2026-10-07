# Admin commands
Commands for operators (permission level 2).

## Commands
| Command | What it does |
|-|-|
| `/dndclass get <player>` | Prints the player's class (`none` if they haven't picked one) |
| `/dndclass set <player> <class>` | Switches the player to a class without them dying. `none` clears the class and reopens their class picker |

Class names are lower-case and tab-completed: `barbarian`, `bard`, `cleric`, `druid`, `fighter`, `monk`, `paladin`, `ranger`, `rogue`, `necromancer`, `warlock`, `wizard`, `artificer`, `bloodhunter`, `alchemist`, `none`.

Useful vanilla commands for this mod:
- `/locate structure dndclasses:dragon_lair`, `dndclasses:hobbit_village`, `dndclasses:dwarven_fortress`
- `/summon dndclasses:<mob>` (e.g. `wyvern`, `lightning_chaser`, `goblin_warlord`, `magmamuncher_alpha`)

## For developers
- `Commands/DndClassCommand`. Devscript: `devscripts/dndclass-command.txt`.
