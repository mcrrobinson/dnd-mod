# Admin commands
Commands for operators (permission level 2) to manage classes, class progress and goblin raids.

![Chat after /dndclass set, get, xp add and progress, and /goblinraid list](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/admin-commands-chat.png)

## Commands
### Classes
| Command | What it does |
|-|-|
| `/dndclass get <player>` | Prints the player's class, or `none` if they haven't picked one |
| `/dndclass set <player> <class>` | Switches the player to a class without them dying. `none` clears the class and reopens their class picker |

Class names are lower case and tab-complete: `barbarian`, `bard`, `cleric`, `druid`, `fighter`, `monk`, `paladin`, `ranger`, `rogue`, `necromancer`, `warlock`, `wizard`, `artificer`, `bloodhunter`, `alchemist`, `none`.

`set` works like a fresh pick: it clears the player's status effects, applies the new stats and starting health, prints the class summary and hands out a [Class Guidebook](class-guidebook.md) if they don't have one. Their progress in the old class is kept, so switching back later restores it.

### Class progress
These all act on the player's current class. See [Class selection](class-selection.md#class-levels-and-the-skill-tree) for how levels and skills work.

| Command | What it does |
|-|-|
| `/dndclass progress <player>` | Prints level, XP, unspent points, unlocked skills, the equipped active and the passives |
| `/dndclass xp <player> add <amount>` | Adds class XP, announcing any level-up as normal |
| `/dndclass xp <player> set <amount>` | Sets class XP. Lowering it can leave the player with negative points until they earn them back |
| `/dndclass unlock <player> <skill>` | Unlocks a skill without spending points or needing the node below it |
| `/dndclass equip <player> <skill>` | Equips an unlocked skill without an Attunement Table. Run it again on an equipped passive to unequip it |
| `/dndclass resetprogress <player>` | Wipes XP and unlocks for the current class |

Skill ids look like `barbarian.war_cry` and tab-complete from the player's class tree. `unlock`, `equip` and `xp` print the progress line afterwards, so you can check the result straight away.

### Goblin raids
| Command | What it does |
|-|-|
| `/goblinraid start` | Starts a raid on the nearest hobbit village or dwarven fortress, within 128 blocks, or where you stand if there is none |
| `/goblinraid start here` | Starts a raid where you stand |
| `/goblinraid stop` | Calls off the raid nearest to you |
| `/goblinraid list` | Lists running raids |

See [Goblin raids](goblin-raids.md) for the details.

### Useful vanilla commands
- `/locate structure dndclasses:dragon_lair`, `dndclasses:hobbit_village`, `dndclasses:dwarven_fortress`, `dndclasses:goblin_camp`, `dndclasses:beholder_lair`
- `/summon dndclasses:<mob>`, for example `wyvern`, `lightning_chaser`, `goblin_warlord`, `magmamuncher_alpha`, `owlbear`, `mimic`
- `/give @s dndclasses:attunement_table` to skip the recipe while testing skills

## For developers
- `Commands/DndClassCommand` and `Commands/GoblinRaidCommand`, registered in `DnDClasses`.
- `unlock` and `equip` call `Progression.unlock` and `Progression.equip` with `force = true`.
- Devscript: `devscripts/dndclass-command.txt`.
