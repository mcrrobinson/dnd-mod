# Class-gated obstacles
Some walls, doors and barriers can only be got past by the right class. Look at one and a hint above the crosshair says what it is, how hard it is and who can deal with it. The right class right-clicks it and rolls a d20 on the [skill check HUD](d20-skill-checks.md). Everyone else has a fallback that costs something, or has to bring the right adventurer.

The first obstacles are the **Lesser** and **Greater Arcane Seals**. More kinds (boulders, holy wards, thornwalls, hidden doors and so on) are planned on the same framework.

![A Wizard's Arcana roll shattering a Lesser Arcane Seal](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/obstacle-framework/obstacle-wizard-open.png)

## How it works
### Arcane Seals
| | Lesser Arcane Seal | Greater Arcane Seal |
|-|-|-|
| Looks like | Translucent violet glyph wall | The same, with a gold rim |
| Check | Arcana | Arcana |
| Who can dispel it | Wizard (primary), Warlock (secondary) | Wizard only |
| Failed roll | 2 magic damage | 2 magic damage |
| Natural 1 | 4 magic damage and Weakness I for 10 seconds; raises an alarm | Same |
| Fallback | Mine it: hardness 50 (like obsidian, about 9.4 s per block with a diamond pickaxe). Each block you break deals 6 magic damage and Weakness I for 30 seconds, and drops nothing | None. It can't be broken in survival |

Nobody can dispel a seal while under the Beholder's Anti-Magic. Seals glow at light level 7 while sealed. TNT, withers and the Ender Dragon can't break them, and pistons can't move them.

### The roll
Right-click a sealed obstacle with an empty main hand. If your class can't attempt it, the action bar says who can ("Lesser Arcane Seal (Hard). A Wizard or Warlock could dispel it.").

The roll is d20 + modifier against the obstacle's DC:

| Tier | DC | Class XP for the solver |
|-|-|-|
| Easy | 10 | 5 |
| Medium | 13 | 10 |
| Hard | 15 | 15 |
| Very Hard | 18 | 25 |

Until ability scores exist, the modifier is a placeholder from the class table: a **primary** class gets +3 plus proficiency, a **secondary** class +1 plus proficiency. Proficiency is +2 at class levels 0-3, +3 at 4-7 and +4 at 8-10. A level 0 Wizard rolls +5 and a level 0 Warlock +3.

- **Success:** the roll shows, and 1.3 seconds (26 ticks) later the whole seal opens: every touching block of the same seal, up to 48. Open seal blocks have no collision and leave a faint broken frame. The solver gets the class XP above.
- **Failure:** the seal's sting (above) and a 1.5 second wait before you can try again.
- **Natural 1:** the fumble sting, a 5 second wait, and an alarm that will wake dormant monsters within 16 blocks once dungeons use it.

A natural 20 always succeeds and a natural 1 always fails.

### Take your time
Sneak and right-click instead to work at it slowly. You need no hostile mob within 16 blocks, and you must stand still for 8 seconds; moving or taking damage breaks your concentration. It then resolves as 20 + your modifier, with no crit, no fumble and no sting. It isn't allowed on Very Hard obstacles. Rolling stays the fast option in a fight.

### Protected volume
Obstacles can't be dug or built round. In survival, nobody can place a block in, or break a non-obstacle block in, the 1-block shell round a sealed obstacle. The action bar says why.

### Creative and spectator
Creative players open obstacles by right-clicking (no roll, no XP) and ignore the protected volume, so DMs can test. Spectators ignore obstacles.

## Where to find it
Nowhere in survival worlds yet: structure placements come in a later ticket. Operators can place seals with `/dndobstacle place`, and creative players can take the seal blocks from the DnD Classes creative tab (hand-placed seals are Medium).

## Commands
Operators (permission level 2):

| Command | What it does |
|-|-|
| `/dndobstacle place <from> <to> <type> <tier> [critical]` | Fills the box with one sealed obstacle group (at most 48 blocks). Types: `dndclasses:lesser_arcane_seal`, `dndclasses:greater_arcane_seal`. Tiers: `easy`, `medium`, `hard`, `very_hard` |
| `/dndobstacle info <pos>` | Type, tier, DC, state, group size and who can attempt it |
| `/dndobstacle open <pos>` | Opens the whole group (no XP) |
| `/dndobstacle reset <pos>` | Reseals the whole group |
| `/dndobstacle settier <pos> <tier>` | Retiers the whole group |
| `/dndobstacle reseal <pos> <ticks>` | The group reseals itself this long after opening (0 = never, the default) |
| `/dndobstacle list [radius]` | Lists obstacle groups near you (default 64 blocks) |

## Configuration
None.

## Known limitations
- Retry timers and take-your-time focus are kept in memory, so they reset when the server restarts.
- The protected volume only stops block items. Buckets can still pour fluids next to a seal.
- Breaking a non-obstacle block in the protected volume is refused by the server, so the client briefly shows the block breaking before it comes back.
- The "Ask <name>" line only knows players whose class the client has been told about.
- The modifier is a placeholder until ability scores land (see For developers).

## For developers
- Package `classes/Obstacles/`:
  - `ObstacleType` (one kind of obstacle: skill, `eligibility(DndCharacter)`, stings, fallback, effects) and `ObstacleTypes`, the registry of types, their blocks and items, and the shared `dndclasses:obstacle` block entity type. `ArcaneSealType` is both seals.
  - `ObstacleBlock` has `state=sealed|open`. `ObstacleBlockEntity` stores `Tier`, `Critical`, `ResealTicks`, `OpenedAt`, `GroupSeed` and a per-type `Data` compound, and syncs to clients for the hint.
  - `ObstacleGroups` flood-fills touching blocks of the same type and `GroupSeed` (cap 48) to open, reseal and retier them together.
  - `ObstacleInteractions`: right-click, retry timers, the 26-tick pending open, take your time and the protected volume.
  - `ObstacleIndex` (`PersistentState`, per dimension): every obstacle block by chunk, filled as block entities load and emptied when the block is removed.
  - `ObstacleEvents.SOLVED` (quests) and `ObstacleEvents.ALARM` (dungeons).
  - `ObstaclePlacer`: the worldgen API. `place`, `doorway` and `box` only write blocks inside the `chunkBox` you pass, like chest loot in `LairPiece`.
- `SkillChecks/SkillModifiers` computes the placeholder modifier from `Eligibility` (`PRIMARY`, `SECONDARY`, `UNTRAINED`, `NONE`) and runs registered `SkillModifierProvider`s, the seam for ability scores, races, subclasses and items. `D20.Skill.ARCANA` labels the roll.
- `Client/Hud/ObstacleHintHud` draws the crosshair hint.
- Textures are made by `tools/obstacle_textures.py`.
- Every roll, sting, solve and broken focus logs an `[Obstacle]` line. `devscripts/obstacle-verify.txt` rolls 40 times on an Easy seal (successes, failures, crits, fumbles) and breaks a focus by moving and by damage.
- `devscripts/obstacle-arcane-seal.txt` covers a Fighter (hint, refusal, protected volume, mining with backlash), a Wizard (roll, take your time, XP, reset), a Warlock (take your time on Medium) and the Greater seal. It uses the DevScript step `mine on|off`, which keeps breaking the block at the crosshair.
