# Admin commands
Commands for operators (permission level 2) to manage classes, class progress, races, racial homes, goblin raids, dungeons, obstacles and Dungeon Master sessions, with links to the player commands (`/party`, `/quest`, `/rep`).

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
| `/dndclass unlock <player> <skill>` | Unlocks a skill without needing points, the node below it or the right subclass. Its cost still counts, so later levels pay it off before giving new points |
| `/dndclass equip <player> <skill>` | Equips an unlocked skill without an Attunement Table. Run it again on an equipped passive to unequip it |
| `/dndclass rank <player> <skill> <n>` | Sets an unlocked skill's rank without points, level or an Attunement Table. `n` is from 1 to the skill's max rank. Ranks still count toward points spent, as with `unlock` |
| `/dndclass bestiary <player> learn <entity>` | Adds a creature to the class's bestiary as if they'd killed it (Bard and Druid only) |
| `/dndclass bestiary <player> unlock <entity>` | Learns and unlocks a creature without a table or the special's rank |
| `/dndclass subclass <player> <id\|none>` | Sets the subclass of the player's current class (e.g. `barbarian.berserker`), skipping the level and the Attunement Table; replacing another subclass refunds it first. `none` clears it and refunds the subclass's upper nodes, the capstone and their ranks |
| `/dndclass resetprogress <player>` | Wipes XP, unlocks, ranks, the subclass and the bestiary for the current class, refunding every point |
| `/dndclass mana <player> <pips>` | Sets a player's mana (0-9 pips), for testing actives without waiting for it to regenerate |
| `/dndclass rest <player> short\|long` | Gives the player a short or long rest's benefits, ignoring its limits (it still counts towards them). See [Rests and charges](rests.md) |
| `/dndclass rest <player> allow` | Forgets the player's last long rest, so they can take another today (in a bed, say) |
| `/dndclass charges <player> [n]` | Prints charges, recharge group, Hit Dice and short rests left; `n` sets the charges (capped at the class's max) |
| `/dndclass hitdice <player> [n]` | Prints the same; `n` sets the Hit Dice left (capped at the pool) |
| `/dndclass sheet <player>` | Prints the player's [character sheet](ability-scores.md): scores, saves, skills and passives |
| `/dndclass score <player> <ability> <1-30>\|clear` | Overrides one ability score (or clears the override), for testing |
| `/dndclass forceroll <entity> <n...>\|clear` | Rigs a player's or mob's next d20 naturals, for tests |
| `/dndclass lore <player> list\|forget [entity]` | Lists the creature types the player has studied, or forgets one (or all) so Study rolls again; see [Creature study](creature-study.md) |
| `/dndclass down <player>` | Downs the player (death saves), even with nobody near. See [Downed and death saves](death-saves.md) |
| `/dndclass stabilise <player>` | A Downed player stops rolling and stands up on their own in 30 s |
| `/dndclass revive <player> [hp]` | A Downed player stands up with `hp` HP (default 1) |
| `/dndclass downed <player>` | Prints the death save tally: passes, fails, seconds to the next save or to standing up |
| `/dndclass laststand <player> [ready]` | Prints the Last Stand cooldown; `ready` clears it |

Skill ids look like `barbarian.war_cry` and tab-complete from the player's class tree; entity ids look like `minecraft:cow`. `unlock`, `equip`, `rank`, `bestiary` and `xp` print the progress line afterwards, so you can check the result straight away. See [Class progression](class-progression.md) for ranks and the bestiary.

### Races
| Command | What it does |
|-|-|
| `/dndrace get <player>` | Prints the player's race (and Dragonborn ancestry), or `none` |
| `/dndrace set <player> <race> [ancestry]` | Changes the race: removes the old race's modifiers, applies the new ones and prints the race summary. `none` clears the race and reopens the race picker |
| `/dndrace list` | Lists online players with their race and class |
| `/dndrace cooldowns <player> [reset]` | Prints the racial cooldowns left (Relentless Endurance, Hellish Rebuke, Breath Weapon); `reset` readies them all |
| `/dndrace traits <player>` | Prints what the passive traits come to now: the break speed on deepslate (1.20 for a Dwarf) and the hidden stone doorways of the dungeon within 128 blocks |

Race names tab-complete: `human`, `elf`, `dwarf`, `halfling`, `gnome`, `halforc`, `tiefling`, `dragonborn`, `none`. Dragonborn need an ancestry: `ember`, `frost` or `storm`. `/gamerule dndRaces false` turns races off (no prompt, no modifiers). See [Races](../races/races.md).

### Racial homes
| Command | What it does |
|-|-|
| `/dndhome [player]` | Which [racial home](../races/racial-homes.md) the player is in, the piece under their feet, and whether it's their own home and hearth |
| `/dndhome pieces` | Lists every piece of the settlement you're standing in, with its box and facing (the side its back faces, for village buildings) |
| `/dndhome desolation <raided\|abandoned\|forsaken\|overgrown\|random>` | Gives every [desolate settlement](../structures/desolate-settlements.md) generated from now on that level (`random` rolls it again, the default); for testing, not saved |
| `/dndhome visit <structure>` | Teleports you 200 blocks up over the nearest start of that structure (within 100 chunks), for devscripts that can't read `/locate`'s answer |
| `/dndhome visit <structure> <piece>` | Puts you 20 blocks out in front of the first piece of that type in the nearest start (e.g. `dndclasses:dwarven_gate`), 10 up and looking at it |
| `/dndhome temple [ember\|frost\|storm]` | Teleports you into the nearest [Dragon Temple](../structures/dragon-temples.md)'s sanctum, in front of that breath altar |
| `/dndhome stronghold [pit\|seat\|hoard\|smithy\|gate]` | Teleports you to that spot of the nearest [Orc Stronghold](../structures/orc-strongholds.md); `pit` stands you in the pit looking at the war drum |
| `/dndhome strongholdsurvey [grid] [spacing]` | Checks the nearest stronghold from each point of a grid round you is in the badlands with all its pieces (`[StrongholdSurvey]` lines) |
| `/dndhome bloodsworn <players> <true\|false>` | Sets or clears Blood-Sworn at the stronghold you stand in |
| `/dndhome trialreset <players>` | Lets the players take the Trial of Strength again today at the stronghold you stand in |
| `/dndhome sanctum [hall\|offering\|anchor\|market\|den\|gate\|top\|side]` | Teleports you to that spot of the nearest [Brimstone Sanctum](../structures/brimstone-sanctums.md) (run it in the Nether) |
| `/dndhome sanctumsurvey [grid] [spacing]` | Checks the nearest sanctum from each point of a grid round you is in a sanctum biome with all its pieces, with no lava under it and no cut ceiling (`[SanctumSurvey]` lines; run it in the Nether) |
| `/dndhome cogsdeep [door\|top\|observatory\|ladder\|telescope\|slit\|stair\|foot\|commons\|workshop\|westworkshop\|clocktower\|strongbox\|gems\|grotto]` | Teleports you to that spot of the nearest [Cogsdeep](../structures/cogsdeep.md), living or desolate, looking at it |
| `/dndhome cogsdeepsurvey [grid] [spacing]` | Checks the nearest burrow from each point of a grid round you: biome, pieces, depth, the observatory on the ground, dry rooms and no other structure within 5 chunks (`[CogsdeepSurvey]` lines) |
| `/dndhome rift [top\|mouth\|floor\|ledge\|pool\|step <k>]` | Teleports you to that spot of the nearest [Underdark Rift](../structures/underdark-rifts.md) |
| `/dndhome riftlanding` / `riftlair` | Onto the nearest Rift Landing (in the Underdark), or the nearest Beholder lair's floor by the pupil |
| `/dndhome riftsurvey [grid] [spacing]` | Checks the nearest rift from each point of a grid round you: pieces, the landing below, the veil pool and every climbing step (`[RiftSurvey]` lines) |
| `/dndhome riftseeds [seeds...]` | The nearest rift to 0, 0 on each seed, without making a world (`[RiftSeeds]` lines) |
| `/dndhome drowoutpost [shrine\|above\|overview\|pen\|barracks\|tower\|inside\|top\|bridge\|reliquary\|trap]` | Teleports you into the nearest [Drow Outpost](../structures/drow-outposts.md) (from anywhere: it searches the Underdark below you) and prints its layout |
| `/dndhome drowoutpostsurvey [grid] [spacing]` | Checks the nearest outpost from each point of a grid round you: biome, floor height, pieces, then every tower shell, bridge deck and stair step, the pen's locked gates and a dry floor (`[DrowOutpostSurvey]` lines) |
| `/dndhome drowoutpostseeds [seeds...]` | The nearest outpost to 0, 0 on each seed and its biome, without making a world (`[DrowOutpostSeeds]` lines) |
| `/dndhome templesurvey [grid] [spacing]` | Checks the nearest temple from each point of a grid round you has a dragon or frost lair within 10 chunks (`[TempleSurvey]` lines) |

### The Underdark
| Command | What it does |
|-|-|
| `/underdark go [player]` | Down into the [Underdark](underdark.md) at the same x and z (a found floor or a carved pocket), remembering where you were |
| `/underdark back [player]` | Back to the remembered spot, or to the world spawn |
| `/underdark biome <biome>` | To the nearest Underdark biome of that kind, on a safe floor |
| `/underdark lake [radius]` | To the nearest dry lake shore at the Underdark's waterline, looking over the water |
| `/underdark flora [radius]` | Counts the giant fungi, plants and lake life round you (`[Underdark] flora` lines) |
| `/underdark map <drow_outpost\|duergar_hold\|rift>` | An Underdark explorer map to the nearest such place |
| `/underdark survey [radius]` | Biome shares, floors, water, bedrock and generation cost round you (`[Underdark] survey` lines) |

### Magic items
See [Magic items](magic-items.md#commands): `/dndmagic give|identify|info`.

### Goblin raids
| Command | What it does |
|-|-|
| `/goblinraid start` | Starts a raid on the nearest hobbit village or dwarven fortress, within 128 blocks, or where you stand if there is none |
| `/goblinraid start here` | Starts a raid where you stand |
| `/goblinraid stop` | Calls off the raid nearest to you |
| `/goblinraid list` | Lists running raids |

`/gamerule dndGoblinRaids false` stops raids starting on their own. See [Goblin raids](goblin-raids.md) for the details.

### Hirelings
`/hireling admin spawn <owner> <role> [race] [first name]`, `loyalty <hireling> <0-100>`, `purse <hireling> <nuggets>`, `newday <owner>` (a wage day now), `roster <innkeeper> reroll`, `knockout <hireling>` and `list [owner]`. Players use `/hireling list|hire|pay|follow|wait|dismiss` without op. See [Hirelings](hirelings.md#commands).

### Dungeons and obstacles
- `/dungeon info|clear|reset|tier|trigger|...` inspects and resets the [dungeon](../structures/dungeons.md#commands) you're in; the room, loot, gate and trap tools are on [Dungeon encounters](dungeon-encounters.md#commands), [Dungeon loot](dungeon-loot.md#commands), [Dungeon puzzles and gates](dungeon-puzzles-and-gates.md#commands) and [Dungeon traps](dungeon-traps.md#commands).
- `/dndobstacle place|info|open|reset|settier|reseal|link|traps|list` places and manages [class-gated obstacles](obstacles.md#commands).

### Conditions
- `/dndcondition apply|remove|list|immune` applies and inspects [conditions](conditions.md#commands) (Prone, Stunned, Petrified, ...), with an optional source, repeat save and escalation. Also `/dm condition ...`.

### Player commands
These need no op: `/party` ([Party](party.md#commands)), `/hireling` ([Hirelings](hirelings.md#commands), with op-only `admin` subcommands), `/quest` ([Quests](quests.md#commands), with op-only subcommands) and `/rep` ([Factions](factions.md#commands), with op-only subcommands).

### Dungeon Master
`/dm on|off`, `/dm veil`, `/dm encounter spawn|list|clear` and `/dm freeze|unfreeze`, `/dm check|save`, `/dm condition`, `/dm roll|apply`, `/dm narrate|title|say|mood` and `/dm loot give|plant` let an op run a session: hide from the players, drop premade encounters, pause the scene, call for rolls on the players' d20 HUD, tell the story and hand out or plant treasure. `/dm grant <player>` opens them to a non-op. The DM Wand (`dndclasses:dm_wand`) puts encounters, freeze, inspect and blink on right-click. See [Dungeon Master](dungeon-master.md).

### Useful vanilla commands
- `/execute in dndclasses:underdark run locate biome dndclasses:fungal_forest` (or any Underdark biome), `/execute in dndclasses:underdark run tp @s x y z`
- `/locate structure dndclasses:dragon_lair`, `dndclasses:hobbit_village`, `dndclasses:dwarven_fortress`, `dndclasses:goblin_camp`, `dndclasses:beholder_lair`
- `/summon dndclasses:<mob>`, for example `wyvern`, `lightning_chaser`, `goblin_warlord`, `magmamuncher_alpha`, `owlbear`, `mimic`
- `/give @s dndclasses:attunement_table` to skip the recipe while testing skills

## For developers
- `Commands/DndClassCommand`, `Commands/DndRaceCommand`, `Commands/DndHomeCommand` and `Commands/GoblinRaidCommand`, registered in `DnDClasses`. `/dndrace set` calls `RaceLifecycle.change`.
- `unlock` and `equip` call `Progression.unlock` and `Progression.equip` with `force = true`; `rank` calls `Progression.setRank`; `bestiary` calls `Progression.learn` and `Progression.unlockBestiary(..., true)`.
- `sheet`, `score` and `forceroll` use `AbilityScores.sheet`, `AbilityScores.setOverride` and `D20.force`.
- Devscripts: `devscripts/dndclass-command.txt`, `devscripts/ability-ranks.txt`, `devscripts/ability-scores.txt`, `devscripts/rests-charges.txt`, `devscripts/downed-solo-laststand.txt`, `devscripts/race-pick.txt`.
