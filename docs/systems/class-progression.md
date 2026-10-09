# Class progression
Each class levels up to 10 with class XP. Every level is a skill point, spent on unlocking skill tree nodes or on ranking up abilities at an Attunement Table. The Bard and Druid also keep a bestiary of creatures they've killed.

## How it works
### Levels and points
Class XP, levels and the tree's shape are covered in [Class selection](class-selection.md#class-levels-and-the-skill-tree). In short: level 10 is reached at 2700 XP, each level gives one skill point, and a full tree costs 8 points.

### Ability ranks
Some abilities have more than one rank. Each rank makes the ability stronger, for example a longer duration. The first rank comes with the ability; each rank above it costs **1 skill point** and needs a minimum **class level**:

| Rank | II | III | IV |
|-|-|-|-|
| Class level | 3 | 6 | 9 |

Abilities with more than four ranks set their own levels. Ranks compete with unlocking new nodes: 10 points against 8 for the tree plus 3 for a special's ranks II-IV, so you can't have everything.

- **Where:** ranks are only bought at an Attunement Table. Right-click the table, then **right-click** an unlocked node with ranks left. Left-click still equips it. From the **O** screen the tooltip just says "Rank up at an Attunement Table".
- **On the node:** a row of pips along the bottom of the icon shows the rank, lit up to the current one.
- **In the tooltip:** "Rank II/IV", "Now: ..." with the current values, "Next: ... (level 6, 1 point)", and a line saying what's needed: "Right-click to rank up", "Next rank needs level N", "Next rank needs 1 point" or "Max rank".
- Ranks are kept per class, and survive death, the End and relogging. `/dndclass resetprogress` clears them and refunds their points.

Abilities with ranks so far:

| Class | Ability | Ranks |
|-|-|-|
| Fighter | Super Regeneration | Regeneration V for 4 / 6 / 8 / 10 s |

### Bestiary (Bard and Druid)
- Killing a creature as a Bard or Druid **learns** it ("Learned Cow. Unlock it at an Attunement Table."). The Druid learns animals and wild beasts such as the Owlbear; the Bard learns animals.
- At an Attunement Table, open the **Bestiary** tab (top right of the skill tree) and click a learned creature to **unlock** it. Unlocking is free.
- Each creature has a tier. Unlocking it needs the class's special (Wild Shape or Animal Friends) at that rank or higher. Every creature is tier I for now; the class's special card sets the tiers.
- Green entries are unlocked, gold ones can be unlocked now, and grey ones need a higher rank. Scroll with the mouse wheel when the list is long.
- A Druid's old kill list is turned into learned creatures the first time their progress loads.

## Where to find it
Right-click an Attunement Table (D&D Classes creative tab, recipe in [Class selection](class-selection.md#class-levels-and-the-skill-tree)). Press **O** to look at ranks and the bestiary anywhere.

## Commands
- `/dndclass rank <player> <skill> <n>`: sets a rank, skipping points, level and the table.
- `/dndclass bestiary <player> learn|unlock <entity>`.
- `/dndclass progress <player>` prints ranks, learned and unlocked creatures.

See [Admin commands](admin-commands.md).

## Configuration
None. XP per level is `ClassProgress.LEVEL_XP`; rank costs and default levels are in `Ranks`.

## Known limitations
- Bestiary tiers aren't set yet, and unlocked creatures don't change what Wild Shape or Animal Friends do until their class cards land.
- Only the Fighter's special has ranks so far; the other classes' cards add theirs.

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
- **Server:** `Progression.rankUp` (packet `C2S_RANK_UP`; table, level and point checks), `setRank` (admin), `learn` (from `ProgressionEvents.onKill`), `unlockBestiary` (packet `C2S_BESTIARY_UNLOCK`).
- **Bestiary hooks** on `ClassSkills`: `usesBestiary()`, `learnsFrom(killed)`, `bestiaryRank(type)` (rank of the root special needed).
- **Screen:** `Client/Hud/SkillTreeScreen` (pips, rank tooltip, Tree/Bestiary tabs).
- **Test:** `devscripts/ability-ranks.txt`. DevScript's `skill unlock|equip|rankup|bestiary <id>` sends the screen's packets, and `click <dx> <dy> [button]` clicks the open screen relative to its centre.
