# Admin commands
Commands for operators (permission level 2) to manage classes, class progress, races, goblin raids and Dungeon Master sessions.

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
| `/dndclass progress <player>` | Prints level, XP, unspent points, the subclass, unlocked skills, the equipped active, the passives, skill ranks and, for the Bard and Druid, the learned and unlocked bestiary |
| `/dndclass xp <player> add <amount>` | Adds class XP, announcing any level-up as normal |
| `/dndclass xp <player> set <amount>` | Sets class XP. If it drops below what their skills cost, they have 0 points until later levels cover it |
| `/dndclass unlock <player> <skill>` | Unlocks a skill without needing points or the node below it. Its cost still counts, so later levels pay it off before giving new points |
| `/dndclass equip <player> <skill>` | Equips an unlocked skill without an Attunement Table. Run it again on an equipped passive to unequip it |
| `/dndclass rank <player> <skill> <n>` | Sets an unlocked skill's rank without points, level or an Attunement Table. `n` is from 1 to the skill's max rank. Ranks still count toward points spent, as with `unlock` |
| `/dndclass bestiary <player> learn <entity>` | Adds a creature to the class's bestiary as if they'd killed it (Bard and Druid only) |
| `/dndclass bestiary <player> unlock <entity>` | Learns and unlocks a creature without a table or the special's rank |
| `/dndclass subclass <player> <id\|none>` | Sets the subclass of the player's current class (e.g. `barbarian.berserker`), skipping the level and the Attunement Table; replacing another subclass refunds it first. `none` clears it and refunds the subclass's upper nodes, the capstone and their ranks |
| `/dndclass resetprogress <player>` | Wipes XP, unlocks, ranks and the bestiary for the current class, refunding every point |
| `/dndclass rest <player> short\|long` | Gives the player a short or long rest's benefits, ignoring its limits (it still counts towards them). See [Rests and charges](rests.md) |
| `/dndclass charges <player> [n]` | Prints charges, recharge group, Hit Dice and short rests left; `n` sets the charges (capped at the class's max) |
| `/dndclass hitdice <player> [n]` | Prints the same; `n` sets the Hit Dice left (capped at the pool) |
| `/dndclass sheet <player>` | Prints the player's [character sheet](ability-scores.md): scores, saves, skills and passives |
| `/dndclass score <player> <ability> <1-30>\|clear` | Overrides one ability score (or clears the override), for testing |
| `/dndclass forceroll <player> <n...>\|clear` | Rigs the player's next d20 naturals, for tests |

Skill ids look like `barbarian.war_cry` and tab-complete from the player's class tree; entity ids look like `minecraft:cow`. `unlock`, `equip`, `rank`, `bestiary` and `xp` print the progress line afterwards, so you can check the result straight away. See [Class progression](class-progression.md) for ranks and the bestiary.

### Races
| Command | What it does |
|-|-|
| `/dndrace get <player>` | Prints the player's race (and Dragonborn ancestry), or `none` |
| `/dndrace set <player> <race> [ancestry]` | Changes the race: removes the old race's modifiers, applies the new ones and prints the race summary. `none` clears the race and reopens the race picker |
| `/dndrace list` | Lists online players with their race and class |

Race names tab-complete: `human`, `elf`, `dwarf`, `halfling`, `gnome`, `halforc`, `tiefling`, `dragonborn`, `none`. Dragonborn need an ancestry: `ember`, `frost` or `storm`. `/gamerule dndRaces false` turns races off (no prompt, no modifiers). See [Races](../races/races.md).

### Magic items
See [Magic items](magic-items.md#commands): `/dndmagic give|identify|info`.

### Goblin raids
| Command | What it does |
|-|-|
| `/goblinraid start` | Starts a raid on the nearest hobbit village or dwarven fortress, within 128 blocks, or where you stand if there is none |
| `/goblinraid start here` | Starts a raid where you stand |
| `/goblinraid stop` | Calls off the raid nearest to you |
| `/goblinraid list` | Lists running raids |

See [Goblin raids](goblin-raids.md) for the details.

### Dungeon Master
`/dm on|off`, `/dm veil`, `/dm encounter spawn|list|clear` and `/dm freeze|unfreeze` and `/dm check|save` let an op run a session: hide from the players, drop premade encounters and pause the scene. `/dm grant <player>` opens them to a non-op. See [Dungeon Master](dungeon-master.md).

### Useful vanilla commands
- `/locate structure dndclasses:dragon_lair`, `dndclasses:hobbit_village`, `dndclasses:dwarven_fortress`, `dndclasses:goblin_camp`, `dndclasses:beholder_lair`
- `/summon dndclasses:<mob>`, for example `wyvern`, `lightning_chaser`, `goblin_warlord`, `magmamuncher_alpha`, `owlbear`, `mimic`
- `/give @s dndclasses:attunement_table` to skip the recipe while testing skills

## For developers
- `Commands/DndClassCommand`, `Commands/DndRaceCommand` and `Commands/GoblinRaidCommand`, registered in `DnDClasses`. `/dndrace set` calls `RaceLifecycle.change`.
- `unlock` and `equip` call `Progression.unlock` and `Progression.equip` with `force = true`; `rank` calls `Progression.setRank`; `bestiary` calls `Progression.learn` and `Progression.unlockBestiary(..., true)`.
- `sheet`, `score` and `forceroll` use `AbilityScores.sheet`, `AbilityScores.setOverride` and `D20.force`.
- Devscripts: `devscripts/dndclass-command.txt`, `devscripts/ability-ranks.txt`, `devscripts/ability-scores.txt`, `devscripts/rests-charges.txt`, `devscripts/race-pick.txt`.
