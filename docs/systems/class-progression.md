# Class progression
Each class levels up to 10 with class XP. Every level is a skill point, spent on unlocking skill tree nodes or on ranking up abilities at an Attunement Table. At class level 3 you choose a **subclass**, one of your tree's two branches. The Bard and Druid also keep a bestiary of creatures they've learned: the Druid by killing them, the Bard by charming them with music.

![An Attunement Table between two bookshelves](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/attunement-table.png)

## How it works
### Levels and points
Class XP, levels and the tree's shape are covered in [Class selection](class-selection.md#class-levels-and-the-skill-tree). In short: level 10 is reached at 2700 XP, each level gives one skill point, and a branch costs 3 points and the capstone 2.

### Subclasses
Each class's two skill-tree branches are its two D&D subclasses: Path of the Berserker or Path of the Totem Warrior, School of Evocation or School of Abjuration, and so on.

| Rule | Value |
|-|-|
| When | From class level **3** (250 XP). Reaching level 3 says "You can now choose a subclass at an Attunement Table." in chat |
| Where | At an Attunement Table only. A gold, pulsing banner over each branch says "Click to choose"; clicking one asks you to confirm |
| Before choosing | Only the root and each branch's **first** (bottom) node can be unlocked. The rest says "Needs a subclass (level 3)" |
| After choosing | Your branch opens fully. The other branch's first node can still be unlocked (a "dabble" node), but its upper nodes are sealed with a padlock and say "Path of the Berserker only" |
| Capstone | Unchanged: it needs a top branch node, so only your subclass reaches it |
| Shared nodes | The Paladin's Circle of Healing, between the branches, is open to either oath ("Any oath") once you've chosen one |
| Subclass feature | Each subclass has a free feature that's always on and doesn't use a passive slot. Hover the banner to read it; each class page has the numbers. A feature that isn't in the game yet says so on its banner |
| Points | A subclass branch costs 3, the capstone 2, the other first node 1 and a special's ranks II-IV 3: 9 of the 10 points at level 10 |
| Per class | Kept per class like the rest of the tree, so switching class and back keeps the choice |
| Changing it | Only an operator for now (`/dndclass subclass <player> none`), later a Tome of Clear Thought. Clearing refunds the subclass's upper nodes, the capstone and their ranks |

Choosing announces it in chat: "Matt the Battle Master Fighter has chosen a subclass: Battle Master." The class guidebook has a page per subclass.

![A Barbarian's tree at level 3 at an Attunement Table, with a "Click to choose" banner over each branch](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/subclasses/subclass-choose.png)

![After choosing the Path of the Totem Warrior: its banner shows the feature, the Berserker branch is sealed with padlocks](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/subclasses/subclass-chosen.png)

**Saves from before subclasses.** The first time a class's progress loads without a subclass:
1. If it owns upper nodes in only one branch, that branch becomes the subclass.
2. If it owns upper nodes in both, the branch with more points in it wins (tie: the branch of the equipped active, else the right branch). The other branch's upper nodes are removed with their ranks and equips, and chat says "Your Barbarian tree now follows the subclass Path of the Berserker. 1 point refunded."
3. Otherwise no subclass is set, and from level 3 you choose one.

All 30 subclasses ("Right" is the branch listed first in the code, column 2):

| Class | Subclass | Id | Branch | Feature |
|-|-|-|-|-|
| Barbarian | Path of the Berserker | `barbarian.berserker` | Right | **Frenzy**: while Rage is active, each melee kill adds 2 seconds to it (up to 6 seconds per Rage) |
| Barbarian | Path of the Totem Warrior | `barbarian.totem_warrior` | Left | **Bear Totem Spirit**: while Rage is active you take 15% less damage |
| Bard | College of Valor | `bard.valor` | Right | **Combat Inspiration**: players who get your instrument buff also get +2 armor for its 30 seconds |
| Bard | College of Lore | `bard.lore` | Left | **Bardic Lore**: +2 to Persuasion checks |
| Cleric | Life Domain | `cleric.life` | Right | **Disciple of Life**: Life Domain healing is 25% stronger |
| Cleric | Forge Domain | `cleric.forge` | Left | **Blessing of the Forge**: once per long rest (once per in-game day with the `dndRests` gamerule off), make one held weapon or worn armor piece +1 (up to +3) until your next long rest, from an Attunement Table's Items tab ([details](magic-items.md#blessing-of-the-forge)) |
| Druid | Circle of the Moon | `druid.moon` | Right | **Primal Strike**: your attacks deal +2 damage in animal form |
| Druid | Circle of the Land | `druid.land` | Left | **Natural Recovery**: +1 mana pip every 30 seconds while standing on grass, leaves or moss |
| Fighter | Champion | `fighter.champion` | Right | **Superior Critical**: melee attack rolls crit on 18-20 |
| Fighter | Battle Master | `fighter.battle_master` | Left | **Combat Superiority**: blocking a melee hit with a shield restores 1 mana pip (at most once every 5 seconds) |
| Monk | Way of the Open Hand | `monk.open_hand` | Right | **Open Hand Technique**: each Flurry Rush hit gives the target Slowness II for 2 seconds and knocks it back a little |
| Monk | Way of the Drunken Master | `monk.drunken_master` | Left | **Tipsy Sway**: with no chestplate on, 15% of melee hits miss you, and drinking Ale gives 2 mana pips |
| Paladin | Oath of Devotion | `paladin.devotion` | Right | **Purity of Spirit**: you're immune to Wither and Poison, and undead deal you 15% less damage |
| Paladin | Oath of Conquest | `paladin.conquest` | Left | **Conquering Presence**: Divine Judgment's shockwave frightens hostile mobs for 3 seconds |
| Ranger | Hunter | `ranger.hunter` | Right | **Colossus Slayer**: your first arrow hit each second on a target below full health deals +3 damage |
| Ranger | Horizon Walker | `ranger.horizon_walker` | Left | **Planar Warrior**: in the Nether and the End you deal 20% more damage and have Speed I |
| Rogue | Assassin | `rogue.assassin` | Right | **Assassinate**: double melee damage to a mob that isn't targeting you, and your first melee swing out of Vanish (while it lasts or up to 5 seconds after) is a critical |
| Rogue | Thief | `rogue.thief` | Left | **Fast Hands**: +3 to lockpicking (Thieves' Tools), and you ignore class restrictions when attuning magic items |
| Necromancer | Bonecaller | `necromancer.bonecaller` | Right | **Undying Servants**: your summons (Raise Dead, Skeletal Archers, Army of the Dead) last 25% longer |
| Necromancer | Plaguebringer | `necromancer.plaguebringer` | Left | **Grim Harvest**: killing a mob that's withering heals you 2 hearts |
| Warlock | The Fiend | `warlock.fiend` | Right | **Dark One's Own Luck**: once every 2 minutes, a failed d20 roll of yours (a check, a save or a fumbled attack) is rolled again |
| Warlock | The Great Old One | `warlock.great_old_one` | Left | **Entropic Ward**: once every 60 seconds, a projectile that would hit you misses |
| Wizard | School of Evocation | `wizard.evocation` | Right | **Sculpt Spells**: your staff blasts, Arcane Explosion and Meteor Swarm don't hurt or knock back party members or their pets |
| Wizard | School of Abjuration | `wizard.abjuration` | Left | **Arcane Ward**: every active you fire gives 2 absorption hearts (up to 4), fading 60 seconds after the last |
| Artificer | Armorer | `artificer.armorer` | Right | **Power Armor**: Arcane Armor lasts 45 seconds instead of 30 and adds 0.5 knockback resistance |
| Artificer | Battle Smith | `artificer.battle_smith` | Left | **Battle Ready**: +2 melee damage with any identified magic weapon |
| Blood Hunter | Order of the Profane Soul | `bloodhunter.profane_soul` | Right | **Rite Focus**: Crimson Rite's bleed lasts 2 seconds longer, and Curse of Binding costs 2 mana instead of 3 |
| Blood Hunter | Order of the Lycan | `bloodhunter.lycan` | Left | **Stalker's Prowess**: +10% movement speed at night, and Hybrid Transformation lasts 20 seconds instead of 15 |
| Alchemist | Mutagenist | `alchemist.mutagenist` | Right | **Mutagen**: drinking any potion also gives Strength I for 10 seconds |
| Alchemist | Transmuter | `alchemist.transmuter` | Left | **Transmuter's Eye**: Healing potions you drink heal 50% more |

Each class page lists its subclasses' nodes.

### Ability ranks
Some abilities have more than one rank. Each rank makes the ability stronger, for example a longer duration. The first rank comes with the ability; each rank above it costs **1 skill point** and needs a minimum **class level**:

| Rank | II | III | IV |
|-|-|-|-|
| Class level | 3 | 6 | 9 |

Abilities with more than four ranks set their own levels. Ranks compete with unlocking new nodes: 10 points against 8 for the tree plus 3 for a special's ranks II-IV, so you can't have everything.

- **Where:** ranks are only bought at an Attunement Table. Right-click the table, then **right-click** an unlocked node with ranks left. Left-click still equips it. From the **O** screen the tooltip just says "Rank up at an Attunement Table".
- **On the node:** a row of pips along the bottom of the icon shows the rank, lit up to the current one.

![The Necromancer's skill tree at an Attunement Table, with rank pips under Raise Dead](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/attunement-skill-tree.png)

- **In the tooltip:** "Rank II/IV", "Now: ..." with the current values, "Next: ... (level 6, 1 point)", and a line saying what's needed: "Right-click to rank up", "Next rank needs level N", "Next rank needs 1 point" or "Max rank".
- Ranks are kept per class, and survive death, the End and relogging. `/dndclass resetprogress` clears them and refunds their points.

Abilities with ranks (values for ranks I / II / III / IV unless noted):

| Class | Ability | Ranks |
|-|-|-|
| Alchemist | Transmute | Radius 3 / 4 / 5 / 6 blocks, cloud 6 / 8 / 10 / 12 s, +1 / +1 / +2 / +2 effect levels |
| Barbarian | Rage | Strength I 8 s / II 10 s / II 12 s / III 12 s |
| Bard | Animal Friends | Radius 10 / 12 / 14 / 16 blocks, 3 / 4 / 5 / 6 companions, animals up to tier I / II / III / IV |
| Blood Hunter | Blood Control | 8 / 12 / 16 / 20 s, 60 / 75 / 90 / 100% success, range 15 / 20 / 25 / 30 blocks |
| Cleric | Sanctuary | 6 / 9 / 12 / 15 s; from rank II the party within 8 / 12 / 16 blocks shares it with Regeneration I 5 s / I 8 s / II 10 s |
| Druid | Wild Shape | 15 / 20 / 25 / 30 s, forms up to tier I / II / III / IV |
| Fighter | Super Regeneration | Regeneration V for 4 / 6 / 8 / 10 s |
| Monk | Flurry Rush | 3 / 5 / 7 / 10 hits across 1 / 2 / 3 / 5 targets, 3 / 3 / 4 / 4 damage per hit |
| Necromancer | Raise Dead | 5 ranks (levels 3 / 5 / 7 / 10): 2 / 3 / 4 / 5 / 5 undead for 10 / 12 / 14 / 16 / 20 s, stronger kinds with rank, plus a Bone Wyvern at rank V |
| Paladin | Divine Judgment | 8 / 12 / 16 / 20 damage, shockwave 3 / 4 / 5 / 6 blocks, 1 / 1 / 2 / 3 beams |
| Ranger | Arrow Storm | 2 / 2.5 / 3.3 / 5 shots/s, arrow speed 100 / 115 / 130 / 150%, 8 / 10 / 12 / 15 s |
| Rogue | Vanish | Invisibility for 6 / 9 / 12 / 15 s |
| Rogue | Danger Sense | Dodges projectiles for 5 / 6 / 7 / 8 s |
| Warlock | Fire Breath | 8 / 12 / 16 / 20 s, reach 3 / 4 / 5 / 7 blocks, 1 / 2 / 2 / 3 damage, burn 2 / 2 / 4 / 4 s |
| Wizard | Arcane Explosion | Radius 12 / 28 / 48 / 72 blocks, Resistance V for 2 / 3 / 4 / 5 s |

### Bestiary (Bard and Druid)
- **Druid:** killing a creature **learns** it ("Learned Cow. Unlock it at an Attunement Table."): animals and wild beasts such as the Owlbear.
- **Bard:** playing an [instrument](../items/bard-instruments.md) **learns** every tiered animal within 8 blocks that isn't tamed or a companion ("Charmed a fox: unlock it at an Attunement Table."). Kills don't teach the Bard anything.
- At an Attunement Table, open the **Bestiary** tab (top right of the skill tree) and click a learned creature to **unlock** it. Unlocking is free.
- Each creature has a tier. Unlocking it needs the class's special (Wild Shape or Animal Friends) at that rank or higher. Every creature is tier I for now; the class's special card sets the tiers.
- Green entries are unlocked, gold ones can be unlocked now, and grey ones need a higher rank. Scroll with the mouse wheel when the list is long.

![A Druid's Bestiary tab: Cow and Wolf unlocked (green), Pig and Fox ready to unlock (gold), Owlbear locked (grey)](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/attunement-bestiary.png)

- A Druid's old kill list is turned into learned creatures the first time their progress loads.

## Where to find it
Right-click an Attunement Table (D&D Classes creative tab, recipe in [Class selection](class-selection.md#class-levels-and-the-skill-tree)). Press **O** to look at ranks and the bestiary anywhere.

## Commands
- `/dndclass rank <player> <skill> <n>`: sets a rank, skipping points, level and the table.
- `/dndclass bestiary <player> learn|unlock <entity>`.
- `/dndclass progress <player>` prints the subclass, ranks, learned and unlocked creatures.
- `/dndclass subclass <player> <id|none>`: sets the subclass of the player's current class, skipping the level and the table (replacing another one refunds it first); `none` clears it and refunds its nodes.

See [Admin commands](admin-commands.md).

## Configuration
None. XP per level is `ClassProgress.LEVEL_XP`; rank costs and default levels are in `Ranks`.

## Known limitations
- Bestiary tiers aren't set yet, and unlocked creatures don't change what Wild Shape or Animal Friends do until their class cards land.
- Only the Fighter's special has ranks so far; the other classes' cards add theirs.
- Subclass features are described but don't do anything yet. The Tome of Clear Thought (respec) doesn't exist yet either.
- `/dndclass unlock` ignores the subclass lock, like it ignores points.

## For developers
- **Storage:** `ClassProgress.ranks` (node id to rank, missing = 1), `learned` and `bestiary` (entity ids), all in `toNbt`/`fromNbt`, so saving, syncing and the death/End copy need nothing extra. `pointsSpent()` counts rank points.
- **Declaring ranks:** one `Ranks` per ability, in a static field of the class's `ClassSkills` subclass:
  ```java
  public static final Ranks SUPER_REGEN = Ranks.of("fighter.super_regen")
          .value("Regeneration", 5, 5, 5, 5)
          .seconds("Duration", 4, 6, 8, 10);
  ```
  Every list has one value per rank, which sets the max rank. `value` is an effect level (roman numerals; read with `amplifier`), `seconds` a duration (read with `ticks`), `amount(name, unit, ...)` and `percent` numbers (`get`, `fraction`), `text` free tooltip text, `ranks(n)` ranks with nothing to show (read `rank`). `levels(...)` overrides the 3/6/9 gates. Readers take the player or a `ClassProgress` and work on both sides, so the effect and the tooltip use the same numbers. `ClassTrees` checks every declaration at startup.
- **Don't** add upgrade nodes or a new `SkillNode.Kind`; a rank belongs to the node. `SkillNode.maxRank()` reads it from `Ranks`.
- **Server:** `Progression.rankUp` (packet `C2S_RANK_UP`; table, level and point checks), `setRank` (admin), `learn` (from `ProgressionEvents.onKill`, and `BardSkills.charmAnimals` via `InstrumentItem`), `unlockBestiary` (packet `C2S_BESTIARY_UNLOCK`).
- **Bestiary hooks** on `ClassSkills`: `usesBestiary()`, `learnsFrom(killed)`, `bestiaryRank(type)` (rank of the root special needed).
- **Subclasses:** `Progression/Subclass` (record). Each `ClassSkills.subclassIds()` gives the right then the left branch's id; `ClassTrees` works out each one's locked nodes from the tree's shape (its column, plus middle nodes that hang only off it, like Danger Sense) and checks every tree at startup. The text (name, title, flavour, feature, `featureReady`) is in `data/dndclasses/class_info.json` (`ClassInfo.SubclassInfo`); `subclassTerm` ("oath") is per class.
- **Storage and rules:** `ClassProgress.subclass` (NBT key `subclass`, "" = none), `SUBCLASS_LEVEL`, `subclassLock(node)` (checked by `canUnlock`, so the server refuses unlock packets for sealed nodes), `hasSubclass(id)`, `removeSubclassNodes`, `migrateSubclass` (run by `Progression.get` when the key is missing).
- **Server:** `Progression.chooseSubclass` (packet `C2S_CHOOSE_SUBCLASS`; level, table and no-subclass checks), `clearSubclass` (for the admin command and the Tome later), `title(player)` ("Matt the Battle Master Fighter").
- **Feature hooks:** check `progress.hasSubclass("wizard.evocation")` in the `ClassSkills` hooks. An `AttributeBonus` whose `skill` is a subclass id is on whenever that subclass is chosen.
- **More hooks for features:** `ClassSkills.manaCost` (a cheaper active, read on both sides), `ClassSkills.afterActivate` (after an active fires), `D20.registerReroll` (a second try at a failed roll) and `AttackRolls.registerAutoCrit` (a guaranteed critical).
- **Screen:** `Client/Hud/SkillTreeScreen` (pips, rank tooltip, Tree/Bestiary tabs, subclass banners, padlocks, `ConfirmScreen`). Guidebook pages: `ClassGuidebookScreen`.
- **Test:** `devscripts/ability-ranks.txt`, `devscripts/subclasses.txt` (the migration step needs a seeded save, see its header), `devscripts/subclasses-verify.txt` (the whole checklist with tooltip screenshots). DevScript's `hover <dx> <dy>` moves the cursor so screens draw tooltips, `widget <label>` presses a screen button such as a confirm dialog's Yes, and `page <n>` turns an open book. DevScript's `skill unlock|equip|rankup|bestiary|subclass <id>` sends the screen's packets, and `click <dx> <dy> [button]` clicks the open screen relative to its centre.
