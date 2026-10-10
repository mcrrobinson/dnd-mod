# Dungeons
Underground adventure sites for a party: an entrance on the surface, a spiral stair down, and 12-14 rooms on a planned route to a boss and its treasure vault. Each dungeon has a name ("The Barrow of Ashmoor") and a Challenge tier (I-IV) that rises with distance from spawn.

This page covers the framework and the themes so far, the **Crypt** and the **Goblin Warren**. Room fights, seals, the champion and the boss are on [Dungeon encounters](../systems/dungeon-encounters.md); loot and the Hoard Coffer are on [Dungeon loot](../systems/dungeon-loot.md); traps are on [Dungeon traps](../systems/dungeon-traps.md); the puzzle and class gates are on [Dungeon puzzles and gates](../systems/dungeon-puzzles-and-gates.md).

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

- **Side rooms**: every dungeon has a **secret room** (9x9, one chest) behind a bricked-up doorway (cracked stone bricks in a Crypt, packed coarse dirt in a Warren) in an antechamber, encounter, puzzle or mid-boss room. Anyone can break through. Half of dungeons also get a **side vault** (11x11, one chest, behind a Wizard-only Greater Arcane Seal). A Crypt always has a **reliquary** (13x13, inner height 6) through a door in the boss room's wall, where the Lich keeps its phylactery (see [The Crypt](#the-crypt)). So a Crypt has 12-14 rooms, and a Warren (no reliquary) 11-13.
- **Corridors**: 3 wide and 4 high, joining neighbouring rooms' 3x3 doorways across the gaps between cells.
- **Shell**: every room and corridor fills its whole box. Walls, floor and ceiling are 2 blocks thick, with an outer skin of deepslate tiles, so caves, aquifers and lava can't flood in. Floors are propped up with cobbled deepslate (up to 16 blocks) over caves.
- **Depth**: the floor is 20 blocks below the lowest of 25 ground samples over the grid (or below sea level, if that's lower), so even the boss room's roof stays at least 6 blocks under the surface. Only the entrance shows (for the Crypt, a barrow; for the Goblin Warren, a cave mouth: see below).
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
Tavern bounty boards post **dungeon clearances** (*Clear the Barrow*, *Smoke out the Warren*, *Reclaim the Lost Hold*). Taking one gives a **Dungeon Map** to the nearest uncleared dungeon of that theme within 2000 blocks, and the notice is done once you're inside when that dungeon is cleared. See [Hobbit Tavern](hobbit-tavern.md#dungeon-clearances).

Crypts generate under plains, sunflower plains, meadows, forests, flower forests, taiga, dark forests, snowy plains and snowy taiga, about 28 chunks apart (at least 10).

Goblin Warrens generate under forests, flower forests, birch and old growth birch forests, old growth pine and spruce taiga, taiga, plains, sunflower plains, savannas and savanna plateaus, about 32 chunks apart (at least 12). They also keep 10 chunks clear of Crypts.

For both, the entrance has to be on dry land, and at most 2 of the 25 ground samples may be under water. They keep 8 chunks clear of hobbit villages, dwarven fortresses and Beholder lairs.

## Commands
- `/locate structure #dndclasses:dungeons` (or `dndclasses:crypt`, `dndclasses:goblin_warren`)
- `/place structure dndclasses:crypt` (or `dndclasses:goblin_warren`) builds one at your chunk, if the ground there passes the checks above ("Failed to place structure" otherwise).
- `/dungeon info`: the dungeon you're in or standing over: name, theme, tier, entrance, floor level, bounds, whether it's cleared, and every room with its state. Op level 2, like the rest.
- `/dungeon clear`: marks it cleared (boss defeated, every room cleared).
- `/dungeon reset`: repopulates it now: every room back to untouched and the boss available again (Hoard Coffer claims stay).
- `/dungeon tier <1-4>`: changes its tier.
- `/dungeon trigger <room>`: starts that room's fight now (see [Dungeon encounters](../systems/dungeon-encounters.md)).
- `/dungeon cutaway`: for screenshots: removes everything from 3 blocks above the floor up to the sky over the dungeon. Destructive.

## Known limitations
- A dungeon made with `/place structure` plays no dungeon music (vanilla `/place` doesn't record a structure start), but its wards and `/dungeon` commands work.
- The Dwarven Ruin is still a placeholder in `DungeonTheme` (Crypt palette, no structure JSON).
- The Warren's music is the generic dungeon track until the dungeon music ticket.
- The Warren's barrels are empty props (only the encounter chest holds loot).
- Trees can grow on the barrow mound, and on a steep slope the mound and its passage follow the ground at the top of the stair, so one side can sit in the hillside or stand proud of it.
- The Crypt's and the Warren's room screenshots for the docs are still to be taken in-game.

## For developers
- `src/main/java/mattonfire/dnd/world/gen/dungeon/`:
  - `DungeonStructures` registers the `dndclasses:dungeon` structure type and the pieces.
  - `DungeonStructure` (codec field `theme`; keep-clear check, plus `otherDungeons(theme)`: the other themes' sets a theme keeps 10 chunks from, checked one way only so two dungeons never check each other) and `DungeonTheme` (palette, tier limits, name word counts). `DungeonTheme.Palette` is the Crypt's; `WarrenPalette` overrides it (`stairs()` gives the shaft's steps).
  - `DungeonPlanner`: site check (`Terrain`), tier, the grid walk, boss/vault/reliquary/branch placement and the pieces (`Layout`). `DungeonTheme.hasReliquary()` (the Crypt) makes the reliquary required: an attempt with no free cell beside the boss room is retried.
  - `DungeonPiece`: the base, with its `Builder` (`shell`, `foundation`, `doorway`, `chest`/`mimic`, `ward`, `spawnPoint`, `column`, `light`). Local coordinates from the box's north-west corner, y = 0 at floor level, no rotation.
  - Pieces: `StairPiece`, `EntrancePiece` (the Crypt's barrow, the Warren's cave mouth; a plain stone ring for other themes), `DungeonCorridorPiece`, `AntechamberPiece`, `EncounterRoomPiece`, `TrapCorridorPiece`, `GateRoomPiece`, `PuzzleRoomPiece`, `ChampionRoomPiece`, `BossRoomPiece`, `VaultPiece` (also the side vault), `SecretRoomPiece`, `ReliquaryPiece` (`phylacterySpot(box, floorY)`).
  - `CryptDressing`: the Crypt's props (`sarcophagus`, `wallSarcophagi`, `bonePile`, `candles`, `skull`, `cornerCobwebs`), called by each room for the Crypt only. They place through `Builder.setIfAir` and draw from `Builder.dressing`, a random separate from the piece's own.
  - `WarrenDressing`: the Warren's props (`leanTo`, `wallProps`, `cage`, `bedroll`, `stores`, `totem`, `cornerProp`, `chain`, `refusePit`, `firePit`), the same way; it reuses `CryptDressing`'s bone piles, skulls and cobwebs.
- `src/main/java/mattonfire/dnd/dungeon/`:
  - `DungeonRegistry` (PersistentState `dndclasses_dungeons`): `find`, `containing`, `roomAt`, `isInsideUncleared`, `nearest`, `clear`. Entries are created lazily from the structure start or from a ward. The static `nearest(world, pos, theme, unclearedOnly, maxDistance)` also finds dungeons nobody has visited: it runs the structure locator over the dungeon structures of that theme (`structuresOf(world, theme)`, any `DungeonStructure` in the registry), loads only the start chunk's structure start and records it. If the nearest one is cleared, it looks again past already-referenced starts.
  - `DungeonMaps.create(world, dungeon)`: the Dungeon Map (vanilla filled map, scale 2, `RED_X` decoration at the entrance, NBT `DungeonStartKey`). For bounties and quests.
  - `DungeonState` (keyed by `StructureStart.getPos().toLong()`), `RoomRole`, `RoomState`.
  - `DungeonEvents`: `ENTERED`, `ROOM_CLEARED`, `BOSS_DEFEATED`, `CLEARED`. `ENTERED` and `ROOM_CLEARED` fire from wards; `BOSS_DEFEATED` from the boss's death (`DungeonCombat`); `CLEARED` from `DungeonRegistry.clear` (the boss's death or `/dungeon clear`).
- `classes/Blocks/DungeonWardBlock` + `DungeonWardBlockEntity` (NBT: `StartKey`, `RoomId`, `Role`, `Box`, `SpawnPoints`, and `Dungeon`, the whole plan, so a ward can record its dungeon without a structure start).
- Doorway seals are `dndclasses:arcane_seal` (`classes/Blocks/ArcaneSealBlock`), placed by the wards during fights; gate obstacles come from the obstacle framework (`classes/Obstacles/`, placed with `Builder.obstacle`).
- `classes/Commands/DungeonCommand`.
- Data: `worldgen/structure/{crypt,goblin_warren}.json`, `worldgen/structure_set/{crypts,goblin_warrens}.json`, `tags/worldgen/biome/has_structure/{crypt,goblin_warren}.json`, `tags/worldgen/structure/dungeons.json`.
- Devscripts: `devscripts/dungeon-place.txt` (walks into a natural crypt in survival, then overhead cutaway shots of it and of a `/place`d one; use a scratch run dir), `dungeon-info.txt` (prints `/dungeon info` again, to check it survived a restart), `dungeon-recon.txt` (tries `/place` at a few spots), `dungeon-crypt.txt` (a fresh `/place`d Crypt: the barrow from outside, then the cut-away layout with its reliquary), `dungeon-warren.txt` (the same for a Goblin Warren: the cave mouth, then the cut-away layout).
