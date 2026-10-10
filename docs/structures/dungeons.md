# Dungeons
Underground adventure sites for a party: an entrance on the surface, a spiral stair down, and 11-13 rooms on a planned route to a boss and its treasure vault. Each dungeon has a name ("The Barrow of Ashmoor") and a Challenge tier (I-IV) that rises with distance from spawn.

This page covers the framework and the first theme, the **Crypt**. Room fights, seals, the champion and the boss are on [Dungeon encounters](../systems/dungeon-encounters.md); loot and the Hoard Coffer are on [Dungeon loot](../systems/dungeon-loot.md); traps are on [Dungeon traps](../systems/dungeon-traps.md); the puzzle and class gates are on [Dungeon puzzles and gates](../systems/dungeon-puzzles-and-gates.md).

## How it works
- **Layout**: a 7x7 grid of 16x16 cells (112x112 blocks, one chunk per cell) centred on the structure's start chunk. The entrance is the middle cell; a random walk from it lays out the main path, in this order:

| # | Room | Outer size (blocks) | Inner height |
|-|-|-|-|
| 0 | Entrance: 7x7 spiral-stair shaft from the surface | 7x7 | to the surface |
| 1 | Antechamber (safe room, cold campfire) | 13x13 | 5 |
| 2 | Encounter (small) | 13x13 | 6 |
| 3 | Trap corridor (walked straight through, 3-5 [traps](../systems/dungeon-traps.md)) | 7x16 | 4 |
| 4 | Encounter (large) | 15x15 | 7 |
| 5 | Class-check gate (wall across the room, its 3-wide gateway barred by an arcane seal, rubble or a locked door: see [puzzles and gates](../systems/dungeon-puzzles-and-gates.md)) | 11x11 | 5 |
| 6 | Puzzle room (four rune pillars, murals and a riddle; the far doorway is sealed until it's solved) | 15x15 | 7 |
| 7 | Encounter (small), in half of dungeons | 13x13 | 6 |
| 8 | Mid-boss room | 15x15 | 8 |
| 9 | Boss room, 2x2 cells, with a central dais | 31x31 | 12 |
| 10 | Treasure vault (the Hoard Coffer) | 13x13 | 5 |

- **Side rooms**: every dungeon has a **secret room** (9x9, one chest) behind a bricked-up doorway of cracked stone bricks in an antechamber, encounter, puzzle or mid-boss room. Anyone can break through. Half of dungeons also get a **side vault** (11x11, one chest, behind a Wizard-only Greater Arcane Seal). So a dungeon has 11-13 rooms.
- **Corridors**: 3 wide and 4 high, joining neighbouring rooms' 3x3 doorways across the gaps between cells.
- **Shell**: every room and corridor fills its whole box. Walls, floor and ceiling are 2 blocks thick, with an outer skin of deepslate tiles, so caves, aquifers and lava can't flood in. Floors are propped up with cobbled deepslate (up to 16 blocks) over caves.
- **Depth**: the floor is 20 blocks below the lowest of 25 ground samples over the grid (or below sea level, if that's lower), so even the boss room's roof stays at least 6 blocks under the surface. Only the entrance shows: a ring of mossy standing stones (some fallen, some with skulls) round the shaft top.
- **Crypt palette**: stone bricks (some mossy or cracked), polished andesite floors, polished deepslate trim, chiselled stone accents, soul lanterns.
- **Chests**: one in each encounter room, the secret room and the side vault, with locked tier loot (`dndclasses:chests/dungeon/<theme>_<room>_t<tier>`); 8% of them are Mimics. See [Dungeon loot](../systems/dungeon-loot.md).
- **No natural monster spawns** inside (`spawn_overrides.monster` is empty): every monster will come from an encounter.
- **Music**: inside a dungeon you hear the dungeon track, Silent Footsteps (all dungeons are in `#dndclasses:music_dungeons`).

### Challenge tier
`tier = 1 + floor(distance / 1500)` from (0, 0), clamped to I-IV. A quarter of dungeons roll one tier higher or lower. Crypts stop at III unless they're at least 4500 blocks out. The tier is fixed when the dungeon generates.

| Tier | Distance from spawn |
|-|-|
| I | under 1500 |
| II | 1500-3000 |
| III | 3000-4500 |
| IV | 4500+ |

### Rooms and wards
Each room has an invisible, unbreakable **Dungeon Ward** under the middle of its floor. Every 10 ticks it looks for survival or adventure players in its room. The first time it sees a player in the dungeon, it shows the dungeon's name and tier as a title. Rooms without a fight count as cleared as soon as a player walks in (UNTOUCHED -> CLEARED); fighting rooms spawn a party-scaled encounter and seal their doorways until it's beaten (UNTOUCHED -> ACTIVE -> CLEARED, see [Dungeon encounters](../systems/dungeon-encounters.md)).

The server remembers each dungeon (theme, tier, name, room states, whether the boss is dead, when it was cleared and how often, who has been inside) in `data/dndclasses_dungeons.dat` in the world folder, so it survives restarts.

## Where to find it
Crypts generate under plains, sunflower plains, meadows, forests, flower forests, taiga, dark forests, snowy plains and snowy taiga, about 28 chunks apart (at least 10). The entrance has to be on dry land, and at most 2 of the 25 ground samples may be under water. They keep 8 chunks clear of hobbit villages, dwarven fortresses and Beholder lairs.

## Commands
- `/locate structure #dndclasses:dungeons` (or `dndclasses:crypt`)
- `/place structure dndclasses:crypt` builds one at your chunk, if the ground there passes the checks above ("Failed to place structure" otherwise).
- `/dungeon info`: the dungeon you're in or standing over: name, theme, tier, entrance, floor level, bounds, whether it's cleared, and every room with its state. Op level 2, like the rest.
- `/dungeon clear`: marks it cleared (boss defeated, every room cleared).
- `/dungeon reset`: repopulates it now: every room back to untouched and the boss available again (Hoard Coffer claims stay).
- `/dungeon tier <1-4>`: changes its tier.
- `/dungeon trigger <room>`: starts that room's fight now (see [Dungeon encounters](../systems/dungeon-encounters.md)).
- `/dungeon cutaway`: for screenshots: removes everything from 3 blocks above the floor up to the sky over the dungeon. Destructive.

## Known limitations
- A dungeon made with `/place structure` plays no dungeon music (vanilla `/place` doesn't record a structure start), but its wards and `/dungeon` commands work.
- Only the Crypt theme exists. The Goblin Warren and Dwarven Ruin are placeholders in `DungeonTheme`.
- Trees growing over the entrance can leave a canopy over the shaft (trunks are cleared out of it up to 7 blocks above the ground).

## For developers
- `src/main/java/mattonfire/dnd/world/gen/dungeon/`:
  - `DungeonStructures` registers the `dndclasses:dungeon` structure type and the pieces.
  - `DungeonStructure` (codec field `theme`; keep-clear check) and `DungeonTheme` (palette, tier limits, name word counts).
  - `DungeonPlanner`: site check (`Terrain`), tier, the grid walk, boss/vault/branch placement and the pieces (`Layout`).
  - `DungeonPiece`: the base, with its `Builder` (`shell`, `foundation`, `doorway`, `chest`/`mimic`, `ward`, `spawnPoint`, `column`, `light`). Local coordinates from the box's north-west corner, y = 0 at floor level, no rotation.
  - Pieces: `StairPiece`, `EntrancePiece`, `DungeonCorridorPiece`, `AntechamberPiece`, `EncounterRoomPiece`, `TrapCorridorPiece`, `GateRoomPiece`, `PuzzleRoomPiece`, `ChampionRoomPiece`, `BossRoomPiece`, `VaultPiece` (also the side vault), `SecretRoomPiece`.
- `src/main/java/mattonfire/dnd/dungeon/`:
  - `DungeonRegistry` (PersistentState `dndclasses_dungeons`): `find`, `containing`, `roomAt`, `isInsideUncleared`, `nearest`, `clear`. Entries are created lazily from the structure start or from a ward.
  - `DungeonState` (keyed by `StructureStart.getPos().toLong()`), `RoomRole`, `RoomState`.
  - `DungeonEvents`: `ENTERED`, `ROOM_CLEARED`, `BOSS_DEFEATED`, `CLEARED`. `ENTERED` and `ROOM_CLEARED` fire from wards; `BOSS_DEFEATED` from the boss's death (`DungeonCombat`); `CLEARED` from `DungeonRegistry.clear` (the boss's death or `/dungeon clear`).
- `classes/Blocks/DungeonWardBlock` + `DungeonWardBlockEntity` (NBT: `StartKey`, `RoomId`, `Role`, `Box`, `SpawnPoints`, and `Dungeon`, the whole plan, so a ward can record its dungeon without a structure start).
- Doorway seals are `dndclasses:arcane_seal` (`classes/Blocks/ArcaneSealBlock`), placed by the wards during fights; gate obstacles come from the obstacle framework (`classes/Obstacles/`, placed with `Builder.obstacle`).
- `classes/Commands/DungeonCommand`.
- Data: `worldgen/structure/crypt.json`, `worldgen/structure_set/crypts.json`, `tags/worldgen/biome/has_structure/crypt.json`, `tags/worldgen/structure/dungeons.json`.
- Devscripts: `devscripts/dungeon-place.txt` (walks into a natural crypt in survival, then overhead cutaway shots of it and of a `/place`d one; use a scratch run dir), `dungeon-info.txt` (prints `/dungeon info` again, to check it survived a restart), `dungeon-recon.txt` (tries `/place` at a few spots).
