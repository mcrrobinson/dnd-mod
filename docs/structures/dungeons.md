# Dungeons
Underground adventure sites for a party: an entrance on the surface, a spiral stair down, and 12-14 rooms on a planned route to a boss and its treasure vault. Each dungeon has a name ("The Barrow of Ashmoor") and a Challenge tier (I-IV) that rises with distance from spawn.

This page covers the framework and the three themes: the **Crypt**, the **Goblin Warren** and the **Dwarven Ruin**. Room fights, seals, the champion and the boss are on [Dungeon encounters](../systems/dungeon-encounters.md); loot and the Hoard Coffer are on [Dungeon loot](../systems/dungeon-loot.md); traps are on [Dungeon traps](../systems/dungeon-traps.md); the puzzle and class gates are on [Dungeon puzzles and gates](../systems/dungeon-puzzles-and-gates.md).

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
| 9 | Boss room, 2x2 cells, with a central dais (the Ruin: a dome) | 31x31 | 12 |
| 10 | Treasure vault (the Hoard Coffer) | 13x13 | 5 |

- **Side rooms**: every dungeon has a **secret room** (9x9, one chest) behind a bricked-up doorway (cracked stone bricks in a Crypt, packed coarse dirt in a Warren, cracked deepslate bricks in a Ruin) in an antechamber, encounter, puzzle or mid-boss room. Anyone can break through. Half of dungeons also get a **side vault** (11x11, one chest, behind a Wizard-only Greater Arcane Seal). A Crypt always has a **reliquary** (13x13, inner height 6) through a door in the boss room's wall, where the Lich keeps its phylactery (see [The Crypt](#the-crypt)). So a Crypt has 12-14 rooms, and a Warren or a Ruin (no reliquary) 11-13.
- **Corridors**: 3 wide and 4 high, joining neighbouring rooms' 3x3 doorways across the gaps between cells.
- **Shell**: every room and corridor fills its whole box. Walls, floor and ceiling are 2 blocks thick, with an outer skin of deepslate tiles, so caves, aquifers and lava can't flood in. Floors are propped up with cobbled deepslate (up to 16 blocks) over caves.
- **Depth**: the floor is 20 blocks below the lowest of 25 ground samples over the grid (or below sea level, if that's lower), so even the boss room's roof stays at least 6 blocks under the surface. The Dwarven Ruin goes deeper: its floor is at a random level between y -30 and 0 (fixed by its position), or lower if the ground calls for it. Only the entrance shows (for the Crypt, a barrow; for the Goblin Warren, a cave mouth; for the Dwarven Ruin, a collapsed gatehouse: see below).
- **Crypt palette**: stone bricks (some mossy or cracked), polished andesite floors, polished deepslate trim, chiselled stone accents, soul lanterns.
- **Chests**: one in each encounter room, the secret room and the side vault, with locked tier loot (`dndclasses:chests/dungeon/<theme>_<room>_t<tier>`); 8% of them are Mimics. See [Dungeon loot](../systems/dungeon-loot.md).
- **No natural monster spawns** inside (`spawn_overrides.monster` is empty): every monster will come from an encounter.
- **Music**: inside a dungeon you hear the dungeon track, Silent Footsteps (all dungeons are in `#dndclasses:music_dungeons`).

### The Crypt
The first theme: a barrow over a buried tomb, home of the Ossuary Cube and the Lich.
- **Entrance**: a **barrow**, a grassy burial mound (radius about 9, 4-5 blocks high) heaped over the top of the stair and roofed over it. A passage 3 wide and 3 high runs into one side of the mound: stone-floored and stone-walled, under a stone lintel for its inner 4 blocks, with two tall chiselled door stones and a slab capstone at its mouth and an open cut beyond. A soul lantern hangs where it meets the stair. Nine weathered standing stones ring the mound 12 blocks out, each on the ground where it stands; a quarter have toppled, and about a third carry a skull. The mound only fills air, plants and water, so it never fills the shaft or carves into a hill.
- **Names**: "The `<place>` of `<name>`" with 6 places (Barrow, Crypt, Tomb, Ossuary, Sepulchre, Catacombs) and 16 names (Ashmoor, Vell, the Grey King, Hollowmere, Saint Corvin, the Drowned Lord, Blackfen, the Last Watch, Morrow Fen, the Weeping Saint, Old Harrowgate, the Ashen Queen, Wyrmstone, the Pale Brothers, Duskwold, the Hollow Crown), picked from the dungeon's position.
- **Dressing** (placed only into air, so it never covers a doorway, chest, trap or ward, and drawn from its own random, so it never changes a room's layout):

| Room | Props |
|-|-|
| Antechamber | sarcophagi along the walls (half the 8 spots), with candles; cobwebs |
| Encounter rooms | bone piles in the corners (not the chest's), candles by the columns, cobwebs |
| Trap corridor | cobwebs along the top of the hall |
| Gate room, vault, side vault | lit candles in the corners, cobwebs |
| Puzzle room | candles under the corner murals, cobwebs |
| Mid-boss room (the Ossuary) | bone piles in every corner and along the walls, skulls strewn off the door lines, cobwebs |
| Boss room (the Lich's tomb) | sarcophagi along the middle of each wall (3 in 4 of the 8 spots), candles on the dais corners, bone piles, skulls, cobwebs |
| Secret room | a bone pile in one corner |
| Reliquary | a chiselled deepslate altar with polished deepslate steps, four columns, four soul lanterns, candles on the diagonals, bone piles in the corners, skulls |

A sarcophagus is two polished deepslate blocks under a deepslate tile slab lid, with candles at its corner end half the time. A bone pile is a bone block on its side, sometimes stacked, often with a skull.

- **The reliquary**: the room off the boss room where the **phylactery** appears, on the altar in the middle (one block up), when the Lich is called up. It's about 25 blocks from the boss's dais, through a short corridor, so it's inside the Lich's leash; its doorway stays open while the boss room is sealed. See [Dungeon encounters](../systems/dungeon-encounters.md#boss).
- **Vault**: at III and IV the Hoard Coffer's full roll has a 15% chance of a Staff of Ice on top (see [Dungeon loot](../systems/dungeon-loot.md)).

### The Goblin Warren
The second theme: the goblins' home base, dug into the rock behind their camps and raids. Home of Grimclaw, their chained owlbear, and the Goblin Warlord.
- **Palette**: rough rock walls (cobblestone, stone, mossy cobblestone, andesite, tuff), trodden earth floors (coarse dirt, packed mud, rooted dirt, some cobblestone and dirt), spruce-log timber shoring along the foot of the walls, round the doorways and as pillars, stripped spruce accents, plain lanterns, cobblestone stairs down the shaft. The waterproof deepslate-tile skin is the same as the Crypt's.
- **Entrance**: a **cave mouth**. A rocky knoll the size of the barrow's mound (stone, cobblestone, dirt, with grass, coarse dirt and mossy stone on top, a block higher or lower here and there) is heaped over the top of the stair and roofed over it. A tunnel 3 wide and 3 high runs into one side: earth-floored and walled and roofed with rock for its inner 4 blocks, then a **timber frame** at its mouth (two spruce-log posts under a log lintel, a skull on each post) and an open cut beyond. A lantern hangs where it meets the stair. A crude **palisade** of stakes (spruce, oak and stripped spruce logs 2-3 high, some fence, a few topped with a stake or a torch, the goblin camp's palisade) rings the knoll 12 blocks out, with gaps, and a gateway where the trail comes in: two tall gate posts, each with a skull. Four **goblin totems** stand inside it, two either side of the trail and two round the back of the knoll: a spruce log and a stripped log with stake arms, under a carved pumpkin facing down the trail (or a skull).
- **Names**: "The `<place>` of `<name>`" with 4 places (Warren, Den, Burrows, Pits) and 8 names (Gutrot, the Red Fang, Skarnak, the Broken Tooth, Mudgrub, the Rat King, Grimtusk, the Howling Hill).
- **Dressing** (as for the Crypt: only into air, its own random; a refuse pit only turns plain warren floor with air over it into mud):

| Room | Props |
|-|-|
| Antechamber (the sleeping den) | hide lean-tos along the walls (3 in 5 of the 8 spots), cobwebs |
| Encounter rooms | in the corners (not the chest's): a straw bedroll, stores (barrels or a composter of slops), a heap of gnawed bones or a skull totem; a refuse pit by one of the columns; cobwebs |
| Trap corridor | cobwebs along the top of the hall |
| Gate room | skull totems in the corners, cobwebs |
| Puzzle room | lanterns on the floor under the corner murals, cobwebs |
| Mid-boss room (Grimclaw's pen) | four chains hanging from the ceiling round the middle, where Grimclaw is chained; bone piles in the corners; hay, stores and bones along the walls; a refuse pit; cobwebs |
| Boss room (the Warlord's war hall) | cages and hide lean-tos along the middle of each wall (4 in 5 of the 8 spots, half of them cages), skull totems at the dais corners, four lit fire pits under the hanging lanterns, bone piles in the corners, refuse pits, cobwebs |
| Vault, side vault | stores in the corners, cobwebs |
| Secret room | a straw bedroll in one corner |

A hide lean-to is two blocks along a wall and two deep: a hay bale and a rug under hides three blocks up by the wall, and a stake and hides two blocks up in front, in the goblin camp's tent colours. A cage is a 3x3 of iron bars two high under spruce slabs, with a skull or bones inside. A skull totem is a skull on a two-high spruce stake. A refuse pit is 2x2 of mud and soul soil.

- **Monsters**: Goblin Warriors, spiders and cave spiders, and wolves at I (they come in angry). The mid-boss is **Grimclaw**, an Owlbear champion that starts in chains, and the boss is the **Goblin Warlord** (see [Dungeon encounters](../systems/dungeon-encounters.md)).
- **Vault**: the Hoard Coffer's full roll adds the Warlord's war chest on top: one roll of the goblin camp chief's chest (see [Dungeon loot](../systems/dungeon-loot.md)).
- **Raids**: clearing a Warren calls off any goblin raid within 256 blocks and keeps natural raids away from settlements there for 3 in-game days (see [Goblin Raids](../systems/goblin-raids.md)).

### The Dwarven Ruin
The third theme: the hold that dug too deep, far under the mountains. Home of the Mimic Matriarch and the Beholder.
- **Palette**: the dwarven fortress's stone gone to ruin. Deepslate brick walls, a quarter of them cracked, patched with deepslate tiles, cobbled deepslate and a little mossy cobblestone; polished deepslate floors with tiles, cracked tiles, cobbled deepslate and tuff; polished blackstone brick trim; chiselled deepslate accents; polished basalt pillars; plain lanterns; deepslate brick stairs down the shaft. The waterproof skin is the same deepslate tiles.
- **Depth**: the floor is between y -30 and 0, so the stair down from a mountainside can be well over 100 steps.
- **Entrance**: a **collapsed dwarven gatehouse**. A 13x13 hall of polished blackstone and deepslate bricks (a glint of gilded blackstone here and there) stands round the top of the stair, paved and footed down to the ground, cleared of rock 8 blocks up. Its walls are 5 high with gaps knocked in them (about a third of the wall columns are lower); four corner towers stand 7-8 high, and one or two of them have fallen to stumps; a ledge of slabs is all that's left of the roof. The **great doorway** (3 wide, 4 high) stands between two polished basalt pillars with chiselled capitals, under a chiselled lintel broken off on one side (the fallen piece lies in front). A paved approach runs out from it, flanked by two headless statues of dwarf lords. Rubble heaps and fallen pillar drums lie all round, off the approach.
- **Names**: "The `<place>` of `<name>`" with 4 places (Lost Hold, Fallen Halls, Deep Gate, Sunken Forge) and 8 names (Khazbarum, King Durn, Ironvein, the Silent Anvil, Grundholm, the Deep Kings, Barazinbar, Stonehelm).
- **Dressing** (as for the other themes: only into air, its own random; a broken pillar only takes out the pillar's own blocks):

| Room | Props |
|-|-|
| Antechamber (the gate guards' hall) | cracked anvils, barrels and rubble along the walls (3 in 5 of the 8 spots), cobwebs |
| Encounter rooms | rubble heaps in the corners (not the chest's), half the time one of the four columns broken (its shaft gone, a stump on the floor, the capital hanging from the ceiling, a drum lying beside it), cobwebs |
| Trap corridor | cobwebs along the top of the hall |
| Gate room | rubble in the corners, cobwebs |
| Puzzle room | lanterns on the floor under the corner murals, cobwebs |
| Mid-boss room (the Mimic Matriarch's gallery) | eight chest spots round the walls, two on each wall clear of the doorway: five real chests of gallery loot, three left empty for the mimics; a red carpet in the middle; cobwebs |
| Boss room (the Beholder's dome) | see below |
| Vault, side vault | gilded blackstone in the corners, cobwebs |
| Secret room | a dwarf's bones in one corner and their anvil in the opposite one |

- **The dome**: the boss room is cut out of solid rock as a dome of radius 14, the room's full 12 blocks high in the middle and curving down to 4 at its rim; the corners of the 31x31 room are solid. Each doorway leads in through a short passage 3 wide and 4 high. Inside: the dais in the middle, four lanterns hanging from the curve, a ring of eight polished basalt pillars 11 blocks out (about 2 in 5 broken), four headless dwarf statues round the dais (a quarter fallen) and rubble round the edge.
- **Monsters**: Dwarven Revenants and Bowmen (skeletons in the hold's old gear), cave spiders, Gelatinous Cubes and Mimics. The mid-boss is the **Mimic Matriarch**, hiding in its gallery with two more mimics, and the boss is the **Beholder**, which stays in its dome (see [Dungeon encounters](../systems/dungeon-encounters.md)). The Ruin is always tier II or higher, and 15% of its other chests are Mimics.
- **Vault**: the Hoard Coffer's full roll adds a 15% chance of a **Staff of Lightning** and a 25% chance of a dwarven heirloom (see [Dungeon loot](../systems/dungeon-loot.md)).
- **The Mountain Dwarves**: clearing a Ruin reclaims the dwarves' lost hold. Every participant gains at least 60 standing with the Mountain Dwarves, enough to lift a negative standing back to Neutral, and every dwarf of the nearest [dwarven fortress](dwarven-fortresses.md) within 512 blocks drops its grudge against them (see [Mountain Dwarves](../mobs/mountain-dwarves.md)).

### Challenge tier
`tier = 1 + floor(distance / 1500)` from (0, 0), clamped to I-IV. A quarter of dungeons roll one tier higher or lower. Crypts stop at III unless they're at least 4500 blocks out. Dwarven Ruins start at II. The tier is fixed when the dungeon generates.

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
Crypts generate under plains, sunflower plains, meadows, forests, flower forests, taiga, dark forests, snowy plains and snowy taiga, about 28 chunks apart (at least 10).

Goblin Warrens generate under forests, flower forests, birch and old growth birch forests, old growth pine and spruce taiga, taiga, plains, sunflower plains, savannas and savanna plateaus, about 32 chunks apart (at least 12). They also keep 10 chunks clear of Crypts.

Dwarven Ruins generate under the dwarven fortress's mountain biomes (meadows, groves, snowy slopes, jagged, frozen and stony peaks, windswept hills, windswept gravelly hills and windswept forests) and stony shores, about 36 chunks apart (at least 14). They keep 10 chunks clear of Crypts and Goblin Warrens.

For all of them, the entrance has to be on dry land, and at most 2 of the 25 ground samples may be under water. They keep 8 chunks clear of hobbit villages, dwarven fortresses and Beholder lairs.

## Commands
- `/locate structure #dndclasses:dungeons` (or `dndclasses:crypt`, `dndclasses:goblin_warren`, `dndclasses:dwarven_ruin`)
- `/place structure dndclasses:crypt` (or `dndclasses:goblin_warren`, `dndclasses:dwarven_ruin`) builds one at your chunk, if the ground there passes the checks above ("Failed to place structure" otherwise).
- `/dungeon info`: the dungeon you're in or standing over: name, theme, tier, entrance, floor level, bounds, whether it's cleared, and every room with its state. Op level 2, like the rest.
- `/dungeon clear`: marks it cleared (boss defeated, every room cleared).
- `/dungeon reset`: repopulates it now: every room back to untouched and the boss available again (Hoard Coffer claims stay).
- `/dungeon tier <1-4>`: changes its tier.
- `/dungeon trigger <room>`: starts that room's fight now (see [Dungeon encounters](../systems/dungeon-encounters.md)).
- `/dungeon cutaway`: for screenshots: removes everything from 3 blocks above the floor up to the sky over the dungeon. Destructive.

## Known limitations
- A dungeon made with `/place structure` plays no dungeon music (vanilla `/place` doesn't record a structure start), but its wards and `/dungeon` commands work.
- The Ruin's music is the generic dungeon track until the dungeon music ticket.
- A Dwarf's stonecunning doesn't find the Ruin's secret doors yet (a hook for the races area); anyone can break the cracked bricks.
- Dwarves of the fortress that aren't loaded when a Ruin is cleared keep any grudge they held (a dwarf's anger runs out on its own within a minute).
- On a very steep slope the gatehouse hall is level with the top of the stair, so a downhill side stands on a footing up to 8 blocks deep and an uphill side is cut into the hill.
- The Warren's music is the generic dungeon track until the dungeon music ticket.
- The Warren's barrels are empty props (only the encounter chest holds loot).
- Trees can grow on the barrow mound, and on a steep slope the mound and its passage follow the ground at the top of the stair, so one side can sit in the hillside or stand proud of it.
- The Crypt's, the Warren's and the Ruin's room screenshots for the docs are still to be taken in-game.

## For developers
- `src/main/java/mattonfire/dnd/world/gen/dungeon/`:
  - `DungeonStructures` registers the `dndclasses:dungeon` structure type and the pieces.
  - `DungeonStructure` (codec field `theme`; keep-clear check, plus `otherDungeons(theme)`: the other themes' sets a theme keeps 10 chunks from, checked one way only so two dungeons never check each other) and `DungeonTheme` (palette, tier limits, name word counts). `DungeonTheme.Palette` is the Crypt's; `WarrenPalette` and `RuinPalette` override it (`stairs()` gives the shaft's steps). `deep()` (the Ruin) puts the floor at `DEEP_MIN_Y..DEEP_MAX_Y` (-30..0, from a random seeded by the world seed and start chunk, in `DungeonPlanner.Terrain.findSite`).
  - `DungeonPlanner`: site check (`Terrain`), tier, the grid walk, boss/vault/reliquary/branch placement and the pieces (`Layout`). `DungeonTheme.hasReliquary()` (the Crypt) makes the reliquary required: an attempt with no free cell beside the boss room is retried.
  - `DungeonPiece`: the base, with its `Builder` (`shell`, `foundation`, `doorway`, `chest`/`mimic`, `ward`, `spawnPoint`, `column`, `light`). Local coordinates from the box's north-west corner, y = 0 at floor level, no rotation.
  - Pieces: `StairPiece`, `EntrancePiece` (the Crypt's barrow, the Warren's cave mouth, the Ruin's gatehouse; a plain stone ring for other themes), `DungeonCorridorPiece`, `AntechamberPiece`, `EncounterRoomPiece`, `TrapCorridorPiece`, `GateRoomPiece`, `PuzzleRoomPiece`, `ChampionRoomPiece`, `BossRoomPiece`, `VaultPiece` (also the side vault), `SecretRoomPiece`, `ReliquaryPiece` (`phylacterySpot(box, floorY)`).
  - `CryptDressing`: the Crypt's props (`sarcophagus`, `wallSarcophagi`, `bonePile`, `candles`, `skull`, `cornerCobwebs`), called by each room for the Crypt only. They place through `Builder.setIfAir` and draw from `Builder.dressing`, a random separate from the piece's own.
  - `RuinDressing`: the Ruin's props (`rubble`, `anvil`, `wallProp`, `wallProps`, `brokenColumn`, `statue`, `hoardScraps`), the same way.
  - `ChampionRoomPiece.gallerySpots(box, floorY)` (the Ruin's 8 gallery spots, `GALLERY_MIMICS` left empty) and `Builder.plainChest` (a chest that's never a mimic); `BossRoomPiece` builds the Ruin's dome (`DOME_RADIUS`, `domeCeiling`, `domeCentre`).
  - `WarrenDressing`: the Warren's props (`leanTo`, `wallProps`, `cage`, `bedroll`, `stores`, `totem`, `cornerProp`, `chain`, `refusePit`, `firePit`), the same way; it reuses `CryptDressing`'s bone piles, skulls and cobwebs.
- `src/main/java/mattonfire/dnd/dungeon/`:
  - `DungeonRegistry` (PersistentState `dndclasses_dungeons`): `find`, `containing`, `roomAt`, `isInsideUncleared`, `nearest`, `clear`. Entries are created lazily from the structure start or from a ward.
  - `DungeonState` (keyed by `StructureStart.getPos().toLong()`), `RoomRole`, `RoomState`.
  - `DungeonEvents`: `ENTERED`, `ROOM_CLEARED`, `BOSS_DEFEATED`, `CLEARED`. `ENTERED` and `ROOM_CLEARED` fire from wards; `BOSS_DEFEATED` from the boss's death (`DungeonCombat`); `CLEARED` from `DungeonRegistry.clear` (the boss's death or `/dungeon clear`).
- `classes/Blocks/DungeonWardBlock` + `DungeonWardBlockEntity` (NBT: `StartKey`, `RoomId`, `Role`, `Box`, `SpawnPoints`, and `Dungeon`, the whole plan, so a ward can record its dungeon without a structure start).
- Doorway seals are `dndclasses:arcane_seal` (`classes/Blocks/ArcaneSealBlock`), placed by the wards during fights; gate obstacles come from the obstacle framework (`classes/Obstacles/`, placed with `Builder.obstacle`).
- `classes/Commands/DungeonCommand`.
- Data: `worldgen/structure/{crypt,goblin_warren,dwarven_ruin}.json`, `worldgen/structure_set/{crypts,goblin_warrens,dwarven_ruins}.json`, `tags/worldgen/biome/has_structure/{crypt,goblin_warren,dwarven_ruin}.json`, `tags/worldgen/structure/dungeons.json`.
- `entity/DwarfGrudges`: the `DungeonEvents.CLEARED` listener for the Ruin (`REPUTATION` 60, `FORTRESS_RANGE` 512, `FORTRESS_RADIUS` 128; `nearestFortress` uses `#dndclasses:faction/mountain_dwarves`), through `SettlementGrudges.forgive(player, area)`.
- Devscripts: `devscripts/dungeon-place.txt` (walks into a natural crypt in survival, then overhead cutaway shots of it and of a `/place`d one; use a scratch run dir), `dungeon-info.txt` (prints `/dungeon info` again, to check it survived a restart), `dungeon-recon.txt` (tries `/place` at a few spots), `dungeon-crypt.txt` (a fresh `/place`d Crypt: the barrow from outside, then the cut-away layout with its reliquary), `dungeon-warren.txt` (the same for a Goblin Warren: the cave mouth, then the cut-away layout), `dungeon-ruin.txt` (the same for a Dwarven Ruin: the gatehouse, then the cut-away layout with its dome).
