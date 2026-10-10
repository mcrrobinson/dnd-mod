# Dungeon puzzles and gates
Every dungeon's route passes a **class-check gate** and a **rune-pillar puzzle**. The gate bars the way with something one or two classes deal with best: an arcane seal for Wizards and Warlocks, a locked door for Rogues, a cave-in anyone can dig through. Any party can get past it, though some take the hard way. In the puzzle room, four rune pillars must be turned to the glyphs painted on the walls behind them before the far doorway opens. A wrong answer sets the floor vents roaring. Half of dungeons also have a **side vault** behind a strict gate that only a Wizard can open.

The dungeons themselves are on [Dungeons](../structures/dungeons.md), the fights on [Dungeon encounters](dungeon-encounters.md), and the obstacle blocks on [Obstacles](obstacles.md).

## How it works
### The class-check gate (room 5)
A wall runs across the 11x11 gate room with a 3-wide gateway in the middle. The gateway is barred by one of these, chosen at random per dungeon:

| Gate | Looks like | Who gets through |
|-|-|-|
| Arcane seal | a violet Lesser Arcane Seal filling the 3x3 gateway | a Wizard (or a Warlock) dispels it with an Arcana check (see [Obstacles](obstacles.md)). Anyone can mine it instead (obsidian-hard), taking 6 magic damage and Weakness per block |
| Rubble | gravel along the bottom, cobblestone and mossy cobblestone above | anyone with a pickaxe or shovel |
| Locked door | an iron door in the middle of the gateway, walled in either side | its "key" is a lever in a locked chest against the side wall on the near side. A Rogue picks the lock (the tier's lock DC, 12/13/15/17); anyone else smashes the chest open, which can ruin some of what's inside. The chest holds 3 separate levers so one survives. Put a lever on either wall block next to the door (or on the door's own wall) and flip it |

- The **planner check** (`DungeonPlanner.gateFor`): a gate on the main path must let at least two classes through or have a bypass anyone can use (`GateKind.mainPathSafe()`). The Greater Arcane Seal (Wizards only, unbreakable) fails that, so it's only used for side vaults.
- Arcane seals on the main path use obstacle tier Medium (DC 13) at Challenge I-II, Hard (DC 15) at III and Very Hard (DC 18) at IV. They're marked critical (they have a solo fallback).

### The side vault's gate
The side vault's doorway (inner wall layer, 3x3) is filled with a **Greater Arcane Seal**: Wizards only, no mining. Its obstacle tier is one step above the main path's (Medium at I, Hard at II, Very Hard at III-IV). The chest inside has side-vault loot at the tier's lock DC +2 (see [Dungeon loot](dungeon-loot.md)).

### The rune-pillar puzzle (room 6)
- **Pillars**: one in each corner of the 15x15 room, 3 blocks in from the middle on both axes: a trim base, a **Rune Pillar** block (`dndclasses:rune_block`) and an accent cap. West pillars face east and east pillars face west.
- **Turning**: use a rune to turn it to the next glyph: Sun, Moon, Eye, Flame, Crown, Skull, then back to Sun. The new glyph shows on the action bar.
- **Murals**: the two walls of each corner carry a 3x3 terracotta picture (3-5 blocks up, running from the corner towards the middle of each wall) of the glyph that corner's pillar must show. Each glyph has its own colour and shape, and the rune faces use the same pattern.
  - Sun: orange plus. Moon: light blue crescent. Eye: lime diamond. Flame: red flame. Crown: yellow crown. Skull: white skull.
- **Defaced mural**: one set pillar's murals are hacked away (rough cobblestone and mossy cobblestone). The written book on the **lectern** in the middle of the room has a riddle naming that glyph:
  - Sun: "I rise each morning and die each dusk..."
  - Moon: "I wax and wane but never eat..."
  - Eye: "I see all yet never weep..."
  - Flame: "Feed me and I live, give me drink and I die."
  - Crown: "Kings wear me, yet I have never had a head of my own."
  - Skull: "Every one of you carries me..."
- **Studying**: sneak-use a rune with an empty hand to study the murals. That rolls an Investigation or Arcana check (whichever bonus is better) against the trap DC (12/13/15/17). On a success the action bar shows the defaced glyph. Each player gets one try per reset.
- **Free pillar**: at Challenge I one pillar takes any glyph. Its murals are bare black terracotta. From Challenge II all four are set. The four answer glyphs are all different.
- **Solving**: as soon as all four pillars are right, the far doorway's ward seals (`dndclasses:arcane_seal`, there since worldgen) vanish. The runes lock ("The runes are set") and the room counts as cleared: `ROOM_CLEARED` fires and everyone in the room gets the cleared-room class XP (10 x tier).
- **Wrong answer**: if all four pillars have been turned within 5 s of each other, then left alone for 2 s, and they're still wrong, the four **flame vents** (in the floor between each pillar and the middle) flare for 1.5 s. Everyone in the room takes 4 / 6 / 8 / 10 trap damage by tier and burns for 3 s; a DEX save against the trap DC halves the damage and stops the burning. The pillars then **shuffle** to random glyphs. The answer stays the same. Turns more than 10 s old are forgotten.
- The room doesn't count as cleared just by walking in, unlike other rooms with no fight. `/dungeon clear` (or the boss's death) opens it.

### Resets
When the dungeon is reset (`/dungeon reset`, or natural repopulation; see [Dungeon loot](dungeon-loot.md)), within half a second of the room's chunks being loaded:
- The puzzle is unsolved again: the exit reseals, the pillars shuffle, everyone can study again, and the lectern gets a fresh riddle book if it was taken.
- The gate is rebuilt: arcane seals reseal (mined blocks come back), rubble refills, and the iron door shuts. Levers, redstone torches, buttons and the like placed next to the door's wall are broken off (they drop), and the key chest is restocked if it was emptied (or put back if it was broken).

Nothing is put back in a block someone is standing in.

## Where to find it
Rooms 5 (gate) and 6 (puzzle) on every dungeon's main path, and the side vault in half of dungeons. Only dungeons generated after this change have them. Older gate rooms keep their open gateway and older puzzle rooms their empty plinths.

## Commands
- `/dungeon info`: under the puzzle room, the puzzle state (solved or not, the runes now, the answer clockwise from the north-west pillar, the defaced pillar and glyph, wrong answers). Under the gate room and side vault, the gate kind, whether it's open, and who can get through.
- `/dungeon puzzle goto <0-3>`: (testing) teleports you in front of a pillar (0 = north-west, then clockwise), facing it.
- `/dungeon puzzle prime right|wrong`: (testing) turns every rune one glyph short of the answer (or of a wrong answer), so a single use on each pillar solves the puzzle (or burns).
- `/dungeon reset`: resets the puzzle and rebuilds the gates (see above).
- `devscripts/dungeon-puzzle.txt` places a fresh crypt and runs a wrong answer, a solve, a reset and a study.

## Configuration
None.

## Known limitations
- Only Area 1's Arcane Seals exist as obstacle blocks. The rubble and locked door are plain vanilla blocks with no class check (a Barbarian digs rubble like anyone; a lever placed anywhere next to the door's wall works). A Rogue lock, holy seal or rubble obstacle can replace them once Area 1 adds them (`GateKind`).
- The side vault's only strict gate is the Greater Arcane Seal, so it's Wizard-only.
- Anyone can still dig round a gate through the dungeon's walls.
- Puzzle variants (brazier order, weight plates) aren't done yet.
- The Investigation/Arcana check is the merged ability-check API; there's no separate hint if nobody in the party can read the riddle.

## For developers
- Blocks: `classes/Blocks/RuneBlock` (`dndclasses:rune_block`, `FACING` + `GLYPH` 0-5, nested `Glyph` enum with the mural colours and 3x3 patterns), registered in `ModBlocks` with a block item. Textures `rune_<glyph>.png` are generated by `tools/rune_textures.py`.
- Data in the room's ward (`DungeonWardBlockEntity`, NBT `Puzzle` / `Gate`):
  - `dungeon/PuzzleState`: `Solution` (4 glyphs, -1 = free), `Defaced`, `Pillars`, `Seal`, `Vents`, `Lectern`, `Solved`, `ResetsSeen`, `WrongAnswers`, `Studied`.
  - `dungeon/GateState`: `Kind`, `Blocks`, `Door`, `KeyChest`, `GroupSeed`, `Tier`, `ResetsSeen`.
- Runtime: `dungeon/PuzzleRooms` (rune use, study, the 10-tick check, solve / wrong / reset, the riddle book) and `dungeon/DungeonGates` (rebuild on reset, key table, obstacle tier, `/dungeon info` text). The ward calls both every 10 ticks, with or without players inside. Resets are spotted through `DungeonState.resets()`, like traps.
- `dungeon/GateKind`: `ARCANE_SEAL`, `GREATER_SEAL`, `RUBBLE`, `LOCKED_DOOR`, with their classes, bypass and `mainPathSafe()`; `GateKind.pick(role, random)`.
- Worldgen: `GateRoomPiece`, `PuzzleRoomPiece`, and `VaultPiece` for the side vault. `DungeonPiece.Builder` gained `obstacle(...)` (through `Obstacles.ObstaclePlacer`), `doorwayColumns(door, depthIn)`, and `puzzle` / `gate` fields that `placeWard` hands to the ward.
- Loot: `chests/dungeon/gate_key_t1..4` (3 levers + junk; locked at the tier's DC through `DungeonLoot.lockDc`).
