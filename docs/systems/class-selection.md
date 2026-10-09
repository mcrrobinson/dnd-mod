# Class selection
Every player picks one of 15 classes. Your class sets your base stats and gives you passives, drawbacks, a [special ability](mana.md) and a skill tree.

![The class picker](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-flesh-out/class-picker.png)

## How it works
The class picker opens a couple of seconds after you join a world without a class. It's a grid of 15 buttons, three across and five down, and Escape doesn't close it: you have to pick. You get one pick. After that the server ignores any further picks from you, and only an operator can change your class with [`/dndclass set`](admin-commands.md).

When you pick, the mod resets you to vanilla stats, clears all your status effects, then applies the class. Chat prints the class's pros, cons and special, and you get a [Class Guidebook](class-guidebook.md) if you don't already carry one.

| Class | Max health | Health on pick | Attack damage | Speed | Other |
|-|-|-|-|-|-|
| No class (vanilla) | 20 | kept | 1 | 0.1 | attack speed 4, luck 0 |
| [Barbarian](../classes/barbarian.md) | 40 | 25 | 6 | 0.08 | |
| [Bard](../classes/bard.md) | 15 | 15 | 1 | 0.12 | |
| [Cleric](../classes/cleric.md) | 20 | kept | 0.67 | 0.1 | |
| [Druid](../classes/druid.md) | 20, +1 heart per tamed animal (up to 5) | kept | 1 | 0.1 | |
| [Fighter](../classes/fighter.md) | 26 | 25 | 6 | 0.1 | |
| [Monk](../classes/monk.md) | 20 | kept | 1 | 0.12 | attack speed 6 |
| [Paladin](../classes/paladin.md) | 26 | 25 | 1 | 0.1 | |
| [Ranger](../classes/ranger.md) | 20 | kept | 1 | 0.1 | luck 5 |
| [Rogue](../classes/rogue.md) | 14 | kept | 1 | 0.1 | |
| [Necromancer](../classes/necromancer.md) | 20 | 5 | 0.5 | 0.1 | |
| [Warlock](../classes/warlock.md) | 20 | kept | 0.5 | 0.1 | |
| [Wizard](../classes/wizard.md) | 10 | 10 | 1 | 0.1 | |
| [Artificer](../classes/artificer.md) | 20 | kept | 1 | 0.12 | |
| [Blood Hunter](../classes/blood-hunter.md) | 20 | kept | 1 | 0.1 | |
| [Alchemist](../classes/alchemist.md) | 20 | kept | 1 | 0.1 | |

"Kept" means your current health stays as it was, capped at the new maximum. The Necromancer starts on 5 health and has to heal up to 20, so pick it somewhere safe.

Your class is saved on your player, so it survives logging out, dying and leaving the End. Those only re-apply the class's base stats: your status effects stay, and so does your health (a death respawns you at full health as usual). Your mana, class XP, skill tree and Druid forms also carry over when you die, so dying doesn't refill your mana bar.

![The Warlock skill tree at level 0](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-flesh-out/skill-tree.png)

### Class levels and the skill tree
Each class has its own XP and level, from 0 to 10. You earn class XP by playing:

- 1 XP for every minute you're online (not in spectator).
- 2 XP for every hostile mob you kill, plus class-specific bonuses such as Barbarian melee kills, Cleric ore mining, Alchemist brewing and Artificer crafting.
- Kill XP is split with nearby [party](party.md) members.

| Level | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 |
|-|-|-|-|-|-|-|-|-|-|-|
| Total XP | 40 | 120 | 250 | 450 | 700 | 1000 | 1350 | 1750 | 2200 | 2700 |

Each level gives you one skill point, and chat tells you when you level up. Press **O** to open your skill tree. Every tree has the same shape: the root at the bottom is your class's original special and is always unlocked, a branch of three nodes climbs each side, and a capstone sits at the top. Branch nodes cost 1 point each. The capstone costs 2 and needs the top node of either branch. A full tree costs 8 points, and abilities can also be ranked up for points (see [Class progression](class-progression.md)), so ten levels can't buy everything and you have to choose.

Click a node you can afford to unlock it. A new passive goes straight into an empty passive slot. To swap your active, or to change passives once both slots are full, you need an **Attunement Table**: right click one to open the tree in attunement mode, then click unlocked nodes to equip or unequip them. You hold one active and two passives at a time. Hover a node to see what it does, its mana cost and what it needs.

Craft an Attunement Table from a book, two lapis lazuli, a crafting table and three amethyst shards:

| | | |
|-|-|-|
| | Book | |
| Lapis Lazuli | Crafting Table | Lapis Lazuli |
| Amethyst Shard | Amethyst Shard | Amethyst Shard |

Each class keeps its own progress. If an operator switches you to another class and back, your old level and unlocks are still there.

## Where to find it
The picker appears on its own. The skill tree is on **O** (rebind it under Options > Controls > Key Binds > D&D Classes). The Attunement Table is in the D&D Classes creative tab.

## Commands
`/dndclass get|set` reads or changes a class without the player dying, and `/dndclass xp|unlock|equip|progress|resetprogress` manage levels and skills. See [Admin commands](admin-commands.md).

## Known limitations
- You can't change class yourself. Ask an operator.
- You can only change your loadout while you stay within 8 blocks of the Attunement Table you opened.

## For developers
- Picker GUI: `Client/Hud/ClassSelectionHud` (LibGui), opened by `DndClassesClient.handleClassQuery` when the server sends class 0. The client never applies stats itself.
- Server side, all in `ClassLifecycle`:
  - `change` is the only path that picks or switches a class (picker packet, `/dndclass set`). It calls every `ClassSkills.forget`, clears effects, applies the stats, sets the starting health, and sends the chat summary and the guidebook.
  - The pick packet runs on the server thread and is rejected unless the player's class is NONE. Bad ids are ignored.
  - The `JOIN`, `COPY_FROM` and `AFTER_RESPAWN` Fabric events re-apply stats only, copy the class and the whole `mattonfire.dnd.classes` persistent compound, and re-sync the class and mana to the client.
  - `DISCONNECT` drops per-player server state (`ClassSkills.forget`, attack-roll crits, Featherfall, party invites, finished cooldowns).
- Per-class base values and starting health: the table in `ClassStats`. Class ids: the `DndCharacter` enum (NONE = 0 ... ALCHEMIST = 15). Saving: `mixin/PlayerEntityMixin` writes and reads `DndClass` in the player NBT.
- Progression lives in `Progression/`. `Progression` stores XP and handles the unlock and equip packets, `ClassProgress` holds the level table and the loadout, `ClassTrees` lists every tree, and `ProgressionEvents` hands out kill XP and runs passives once a second. Each class's nodes and skill code are in `Progression/Classes/<Class>Skills.java`. Progress is saved under `dndProgression` in the player's persistent data, one compound per class.
- Skill tree screen: `Client/Hud/SkillTreeScreen`. Attunement Table: `Blocks/AttunementTableBlock`, recipe `data/dndclasses/recipes/attunement_table.json`.
- Devscripts: `class-none-fallthrough.txt`, `dndclass-command.txt`, `class-persist-set.txt` then `class-persist-check.txt` (sets a class, then checks it after rejoining), `apply-path-set.txt` then `apply-path-check.txt` (Necromancer health and a potion effect survive a rejoin; class and stats survive a death).
