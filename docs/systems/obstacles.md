# Class-gated obstacles
Some walls, doors and barriers can only be got past by the right class. Look at one and a hint above the crosshair says what it is, how hard it is and who can deal with it. The right class right-clicks it and rolls a d20 on the [skill check HUD](d20-skill-checks.md). Everyone else has a fallback that costs something, or has to bring the right adventurer.

So far there are the **Lesser** and **Greater Arcane Seals** (Wizards, Warlocks) the **physical obstacles** for Barbarians, Fighters and Monks (Collapsed Passages, Heavy Boulders, Portcullises with their winches, Chasms and Crumbling Ledges), and the **divine and death obstacles** for Clerics, Paladins, Necromancers and Blood Hunters (Holy Wards, Desecrated Altars, Dread Wards, Ossuary Gates, Crypt Sleepers and Blood Seals). More kinds (thornwalls, hidden doors and so on) are planned on the same framework.

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

### Physical obstacles (Barbarians, Fighters, Monks)
| | Collapsed Passage | Heavy Boulder | Portcullis Winch |
|-|-|-|-|
| Looks like | A plug of rubble (grey stones in dark grit) | A mossy granite boulder (paint 2x2x2) | A wooden winch drum with an iron wheel, beside an iron portcullis |
| Check | Athletics | Athletics | Athletics |
| Who | Barbarian, Fighter (primary), Paladin (secondary) | Barbarian only | Anyone; Barbarians, Fighters and Paladins (the Tanks) are its primaries |
| Success | The whole plug clears in a burst of dust | The boulder grinds aside (no collision) | The portcullis rises for 4 seconds (80 ticks), or 5 seconds (100 ticks) for a Tank, then drops |
| Failed roll | Hunger I for 10 s and Slowness I for 3 s | Slowness II for 5 s | Nothing: the winch won't turn |
| Natural 1 | Cave-in: 3 damage; alarm | Crushed: 4 damage; alarm | The handle whips back: 2 damage (no alarm) |
| Fallback | Mine it: hardness 30 (about 5.6 s a block with a diamond pickaxe). Each block mined has a 25% chance to bring gravel down for 3 damage | Can't be mined, but its blast resistance is 6 (like cobblestone): one TNT (or creeper) that breaks any block of it clears the whole boulder, and raises an alarm | Mine through the portcullis bars: hardness 50 per bar (like obsidian) |

- **Rage:** a raging Barbarian rolls Athletics with advantage (two dice, keep the higher). This comes from the [character sheet](ability-scores.md), so it covers every Athletics obstacle.
- **Wedge (Fighters only):** while the portcullis is up, a Fighter who succeeds at the winch again jams it. The gate stays up for good and the winch shows as wedged. Resetting the winch (`/dndobstacle reset`) drops the gate again.
- Only the first raise of a winch earns class XP, so cranking it over and over isn't a way to level. A wedge earns XP once.
- The **Portcullis** itself has no check. Its hint says its winch raises it; right-clicking it just says so. A winch works the nearest portcullis within 6 blocks, unless a structure or `/dndobstacle link` tells it which one.

**Chasm.** A gap with a pit under it, and a **Bridge Winch** on the far side. The chasm's top layer is its retracted bridge: invisible and with no collision, so you fall straight through, but it's a sealed obstacle, so nobody can build across it or pillar up within a block of it. There's no roll: whoever gets across right-clicks the bridge winch (with an empty hand) and the bridge (spruce planks) swings down for everyone. The first lowering earns the winch's tier XP. Chasms are meant to be 7 blocks long: a Monk's triple jump clears that, a sprint jump (about 4 blocks) doesn't. An ender pearl or a Rogue's 12-block Shadowstep also gets across, and falling in means climbing out of the pit the hard way. A bridge winch works the nearest chasm within 8 blocks unless linked.

**Crumbling Ledge.** Ledge blocks that give way under you: each one crumbles 10 ticks (half a second) after a player first stands on it, or 40 ticks (2 seconds) for a Monk, and comes back 100 ticks (5 seconds) later once nothing is in the way. Sneaking doesn't help (standing on it is checked every tick). There's no roll and nothing to break; creative and spectator players don't set it off.

### Divine and death obstacles (Clerics, Paladins, Necromancers, Blood Hunters)
| | Holy Ward | Desecrated Altar | Ossuary Gate |
|-|-|-|-|
| Looks like | Pale stone with a gold sunburst glyph and gold rim, glowing (light 8) | A cracked blackstone altar with soul fire on top (light 10) | Stacked bone with a skull keystone |
| Check | Religion | Religion | Intimidation ("command the dead") |
| Who | Cleric (primary), Paladin (secondary) | Cleric only | Necromancer only |
| Success | The whole ward opens | Consecrated (gold cross, light 12): every mob spawner within 12 blocks stops for 10 minutes, and the Cleric and their party within 8 blocks are healed 8 health (4 hearts). After 10 minutes the altar desecrates itself again and the spawners wake; only the first consecration earns XP | The bones part |
| Failed roll | 2 magic damage | Wither I for 5 s | The skull cackles (no harm) |
| Natural 1 | 4 magic damage and Blindness for 3 s; alarm | Wither I, and 2 zombies rise beside it; alarm | 2 skeletons rise; alarm |
| Fallback | Can't be broken. Throw Potions of Healing at it (splash or lingering): each one bursting within 2.5 blocks of a sealed ward counts once, and the 3rd opens it. No XP | Smash it: hardness 5, pickaxe. The spawners stay on and 4 undead (2 zombies, 2 skeletons) rise | Smash it: hardness 2 (like bone), pickaxe. Breaking one block brings the whole gate down; 4 skeletons and 1 wither skeleton rise, and an alarm goes off |

**Dread Ward.** Black-bannered deepslate that fills the passage round it with dread. There's no roll and nothing to click. Its zone is every block within 2 of a sealed Dread Ward block (a 5x5x5 cube round a single block), so a builder paints the ward blocks into an archway's frame and the passage through it is the zone. Once a second, a player in the zone gets **Frightened** (half melee damage), **Slowness IV** and **Weakness II**, each for 1.5 seconds (30 ticks), so it wears off soon after you leave. A **Paladin** is immune, and so is every player within 6 blocks of a Paladin (any player, not just party members). Anyone else can still push through, slowly. It can't be broken; `/dndobstacle open` turns it off.

**Crypt Sleepers.** Dormant undead laid in alcoves: any mob with the command tag `dndclasses.sleeper` and its AI off (`NoAI`). A sleeper wakes for good, and goes for that player, when a living player comes within 5 blocks, when something attacks it (sunlight and drowning don't count), or when an obstacle alarm goes off within 16 blocks. **Deathly Escort:** a Necromancer never wakes them, and nor does any member of the Necromancer's `/party` within 6 blocks of them, so a Necromancer can lead the party past. Creative and spectator players never wake them. Put undead near you to sleep with `/dndobstacle sleep`, or summon one with `{NoAI:1b,Tags:["dndclasses.sleeper"]}`.

**Blood Seal.** A dripping red sigil door (light 4) that opens for a price in blood, not a roll. Right-click it with an empty hand:
- A **Blood Hunter** pays 4 health (2 hearts) and it opens, for class XP.
- Anyone else pays toward 16 health (8 hearts). Up to 3 players (not creative or spectating) within 4 blocks of the clicked block split it: each right-click pays one share, the price over the number of players there rounded up (16 alone, 8 each for two, 6 for three), never more than what's still owed. It opens once all 16 are paid. Paying it earns no XP. Payments so far are kept until the seal reseals.
- It's never lethal: a payment that would take you to 0 health is refused. The price comes straight off your health, so armour, absorption hearts and resistance don't change it. A player with 16 health or less can't pay it alone (a Wizard has 10).

### The roll
Right-click a sealed obstacle with an empty main hand. If your class can't attempt it, the action bar says who can ("Lesser Arcane Seal (Hard). A Wizard or Warlock could dispel it.").

The roll is a check of the obstacle's skill (Arcana for seals, Athletics for the physical obstacles, Religion for Holy Wards and Desecrated Altars, Intimidation for Ossuary Gates) against its DC: d20 + your [character sheet](ability-scores.md) Arcana bonus (INT modifier + proficiency + any flat bonuses), with any advantage the sheet gives obstacle checks:

| Tier | DC | Class XP for the solver |
|-|-|-|
| Easy | 10 | 5 |
| Medium | 13 | 10 |
| Hard | 15 | 15 |
| Very Hard | 18 | 25 |

Your class decides whether you may try (Wizards and Warlocks for a Lesser seal, only Wizards for a Greater one); the sheet decides how good you are. Both classes are proficient in Arcana. With the class scores alone, a Wizard (INT 15) rolls +4 and a Warlock (INT 12) +3 at class levels 0-4, one more at 5-8 (proficiency +3) and two more at 9-10 (+4). Race bonuses and admin overrides on the sheet count too. The crosshair hint shows the same bonus.

- **Success:** the roll shows, and 1.3 seconds (26 ticks) later the whole seal opens: every touching block of the same seal, up to 48. Open seal blocks have no collision and leave a faint broken frame. The solver gets the class XP above.
- **Failure:** the seal's sting (above) and a 1.5 second wait before you can try again.
- **Natural 1:** the fumble sting, a 5 second wait, and an alarm that will wake dormant monsters within 16 blocks once dungeons use it.

A natural 20 always succeeds and a natural 1 always fails.

### Take your time
Sneak and right-click instead to work at it slowly. You need no hostile mob within 16 blocks, and you must stand still for 8 seconds; moving or taking damage breaks your concentration. It then resolves as 20 + your bonus for the skill, with no crit, no fumble and no sting. It isn't allowed on Very Hard obstacles. Rolling stays the fast option in a fight.

### Protected volume
Obstacles can't be dug or built round. In survival, nobody can place a block in, or break a non-obstacle block in, the 1-block shell round a sealed obstacle. The action bar says why.

### Creative and spectator
Creative players open obstacles by right-clicking (no roll, no XP) and ignore the protected volume, so DMs can test. Spectators ignore obstacles.

## Where to find it
- [Dungeon gates](dungeon-puzzles-and-gates.md): a dungeon's main-path gate can be a Collapsed Passage, a Holy Ward or an Ossuary Gate, and its side vault can be sealed by a Heavy Boulder or a Blood Seal (dungeons generated after each change).
- Other structure placements come in a later ticket. Operators can place obstacles with `/dndobstacle place`, and creative players can take the blocks from the DnD Classes creative tab (hand-placed obstacles are Medium). The Chasm Bridge item places the invisible retracted bridge.

## Commands
Operators (permission level 2):

| Command | What it does |
|-|-|
| `/dndobstacle place <from> <to> <type> <tier> [critical]` | Fills the box with one sealed obstacle group (at most 48 blocks). Types: `dndclasses:lesser_arcane_seal`, `dndclasses:greater_arcane_seal`, `dndclasses:collapsed_passage`, `dndclasses:heavy_boulder`, `dndclasses:portcullis`, `dndclasses:portcullis_winch`, `dndclasses:chasm`, `dndclasses:bridge_winch`, `dndclasses:crumbling_ledge`, `dndclasses:holy_ward`, `dndclasses:desecrated_altar`, `dndclasses:dread_ward`, `dndclasses:ossuary_gate`, `dndclasses:blood_seal`. Tiers: `easy`, `medium`, `hard`, `very_hard` |
| `/dndobstacle info <pos>` | Type, tier, DC, state, group size and who can attempt it |
| `/dndobstacle open <pos>` | Opens the whole group (no XP) |
| `/dndobstacle reset <pos>` | Reseals the whole group |
| `/dndobstacle settier <pos> <tier>` | Retiers the whole group |
| `/dndobstacle reseal <pos> <ticks>` | The group reseals itself this long after opening (0 = never, the default) |
| `/dndobstacle link <winch> <target>` | Connects a portcullis winch to a portcullis block, or a bridge winch to a chasm block |
| `/dndobstacle list [radius]` | Lists obstacle groups near you (default 64 blocks) |
| `/dndobstacle sleep [radius]` | Lays every undead mob within the radius (default 8) down as a Crypt Sleeper |
| `/dndobstacle wake [radius]` | Wakes the Crypt Sleepers within the radius (default 8) |

## Configuration
None.

## Known limitations
- Retry timers and take-your-time focus are kept in memory, so they reset when the server restarts.
- The protected volume only stops block items. Buckets can still pour fluids next to a seal.
- Breaking a non-obstacle block in the protected volume is refused by the server, so the client briefly shows the block breaking before it comes back.
- The "Ask <name>" line only knows players whose class the client has been told about.

- Chasm width is set from the jump physics, not measured in game yet: a Monk's triple jump should clear 7 blocks and a sprint jump shouldn't. Speed potions or other movement abilities may change that.
- A Heavy Boulder that "rolls aside" just loses its collision and model; it doesn't move 3 blocks as a block.
- A Crumbling Ledge's reform tick still runs if an operator resets the ledge with `/dndobstacle reset` in the meantime, so it can crumble once more on its own.
- A Desecrated Altar only quiets spawner blocks. Dungeon or structure spawning that doesn't use a spawner block isn't affected.
- Crypt Sleepers have no crosshair hint, and a Bard gets no special way past them yet.
- Dread Wards apply their effects to players only; mobs in the zone aren't frightened.
- A wedged portcullis that an operator reseals directly (rather than through its winch) leaves the winch showing as wedged until the winch is reset too.

## For developers
- Package `classes/Obstacles/`:
  - `ObstacleType` (one kind of obstacle: skill, `eligibility(DndCharacter)`, stings, fallback, effects) and `ObstacleTypes`, the registry of types, their blocks and items, and the shared `dndclasses:obstacle` block entity type. `ArcaneSealType` is both seals.
  - `ObstacleBlock` has `state=sealed|open`. `ObstacleBlockEntity` stores `Tier`, `Critical`, `ResealTicks`, `OpenedAt`, `GroupSeed` and a per-type `Data` compound, and syncs to clients for the hint.
  - `ObstacleGroups` flood-fills touching blocks of the same type and `GroupSeed` (cap 48) to open, reseal and retier them together.
  - `ObstacleInteractions`: right-click, retry timers, the 26-tick pending open, take your time and the protected volume.
  - `ObstacleIndex` (`PersistentState`, per dimension): every obstacle block by chunk, filled as block entities load and emptied when the block is removed.
  - `ObstacleEvents.SOLVED` (quests) and `ObstacleEvents.ALARM` (dungeons).
  - `ObstaclePlacer`: the worldgen API. `place`, `doorway` and `box` only write blocks inside the `chunkBox` you pass, like chest loot in `LairPiece`.
- `ObstacleType.skill()` returns an `Abilities.Skill` (`ARCANA` for the seals). `ObstacleInteractions` rolls `SkillCheck.builder(player, type.skill(), tier.dc, "obstacle")`, and take your time rolls the same builder with `.take(20)` (a fixed natural that is never a crit or a fumble).
- `ObstacleType.eligibility(DndCharacter)` returns `SkillChecks/Eligibility` (`PRIMARY`, `SECONDARY`, `UNTRAINED`, `NONE`). It only decides who may try and the order classes are named in hints; it doesn't change the modifier.
- Anything that should change obstacle rolls (races, subclasses, magic items) adds an `AbilityContributor` with `AbilityScores.register(id, contributor)`; a contributor can grant advantage on rolls tagged `obstacle`.
- `Client/Hud/ObstacleHintHud` draws the crosshair hint.
- Physical obstacle types live in `classes/Obstacles/types/`: `CollapsedPassageType`, `HeavyBoulderType`, `PortcullisType`, `WinchType` (with `PortcullisWinchType` and `BridgeWinchType`; the target's position is `Data.Target` in the winch's block entity), `ChasmType` and `CrumblingLedgeType`. Each family adds a block of fields to `ObstacleTypes`, before the shared block entity type.
- `ObstacleType` hooks a type can override: `hardness()`, `blastResistance()`, `interactive()` (false: right-click passes), `solid(state)` (collision and outline per state), `ownWhoText()` (hint uses `<key>.who`), `solve(world, pos, solver)` (what success does and whether it earns XP; default opens the group), `scheduledTick`, `onExploded` and `registerEvents()`. A type with no skill that a class can try is worked with a right-click and no roll.
- `ObstacleGroups.openFor(world, pos, ticks)` opens a group on a timer (reseal ticks are checked every tick); `ObstaclePlacer.chasm(...)` lays a chasm bridge and `ObstaclePlacer.winch(...)` places a winch linked to its target.
- `dungeon/GateKind` has `COLLAPSED_PASSAGE`, `HOLY_WARD` and `OSSUARY_GATE` (main-path safe) and `HEAVY_BOULDER` and `BLOOD_SEAL` (strict, side vaults).
- Divine and death types: `HolyWardType` (potion count in the group's `Data.Doused`; `PotionEntityMixin` calls `onPotionBurst`), `DesecratedAltarType` (`MobSpawnerLogicMixin` cancels `serverTick` while an open altar is within 12 blocks, found through `ObstacleIndex`; it reopens on a 12000-tick `ObstacleGroups.openFor` timer), `DreadWardType` (per-second `END_WORLD_TICK` aura), `OssuaryGateType`, `BloodSealType` (payments in `Data.Paid`) and `RisenUndead.raise(...)` for undead that rise beside an obstacle. `Obstacles/CryptSleepers` holds the sleeper tag, `sleep(mob)`, `wake(...)` and `undeadAllied(player)`.
- `ObstacleGroups.dataInt` / `setDataInt` keep a per-type number on every block of a group. `ObstacleEvents.SOLVED` fires only for a solve that earns XP (not re-cranking a winch, paying a Blood Seal's fallback price, or re-consecrating an altar).
- Textures are made by `tools/divine_obstacle_textures.py`. `devscripts/obstacle-divine.txt` covers a Cleric, a Fighter's fallbacks, the Dread Ward, a Necromancer, a crypt sleeper and the Blood Seal.
- Textures are made by `tools/physical_obstacle_textures.py`. `devscripts/obstacle-physical.txt` covers a Barbarian, a Fighter, a Monk and a Wizard's solo fallbacks.
- Textures are made by `tools/obstacle_textures.py`.
- Every roll, sting, solve and broken focus logs an `[Obstacle]` line. `devscripts/obstacle-verify.txt` rolls 40 times on an Easy seal (successes, failures, crits, fumbles) and breaks a focus by moving and by damage.
- `devscripts/obstacle-arcane-seal.txt` covers a Fighter (hint, refusal, protected volume, mining with backlash), a Wizard (roll, take your time, XP, reset), a Warlock (take your time on Medium) and the Greater seal. It uses the DevScript step `mine on|off`, which keeps breaking the block at the crosshair.
