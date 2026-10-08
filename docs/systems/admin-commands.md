# Admin commands
Commands for operators (permission level 2) to manage classes, class progress and goblin raids.

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
| `/dndclass progress <player>` | Prints level, XP, unspent points, unlocked skills, the equipped active, the passives, skill ranks and, for the Bard and Druid, the learned and unlocked bestiary |
| `/dndclass xp <player> add <amount>` | Adds class XP, announcing any level-up as normal |
| `/dndclass xp <player> set <amount>` | Sets class XP. Lowering it can leave the player with negative points until they earn them back |
| `/dndclass unlock <player> <skill>` | Unlocks a skill without spending points or needing the node below it |
| `/dndclass equip <player> <skill>` | Equips an unlocked skill without an Attunement Table. Run it again on an equipped passive to unequip it |
| `/dndclass rank <player> <skill> <n>` | Sets an unlocked skill's rank without points, level or an Attunement Table. `n` is from 1 to the skill's max rank |
| `/dndclass bestiary <player> learn <entity>` | Adds a creature to the class's bestiary as if they'd killed it (Bard and Druid only) |
| `/dndclass bestiary <player> unlock <entity>` | Learns and unlocks a creature without a table or the special's rank |
| `/dndclass resetprogress <player>` | Wipes XP, unlocks, ranks and the bestiary for the current class, refunding every point |

Skill ids look like `barbarian.war_cry` and tab-complete from the player's class tree; entity ids look like `minecraft:cow`. `unlock`, `equip`, `rank`, `bestiary` and `xp` print the progress line afterwards, so you can check the result straight away. See [Class progression](class-progression.md) for ranks and the bestiary.

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
- `unlock` and `equip` call `Progression.unlock` and `Progression.equip` with `force = true`; `rank` calls `Progression.setRank`; `bestiary` calls `Progression.learn` and `Progression.unlockBestiary(..., true)`.
- Devscripts: `devscripts/dndclass-command.txt`, `devscripts/ability-ranks.txt`.
