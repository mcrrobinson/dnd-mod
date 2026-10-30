# Kobold Warrens
A kobold warren is a cave mouth cut into a cliff or steep slope of the hills, badlands and stony peaks. A tunnel runs down into the hill to a den, with rooms round it: a red-dragon shrine, an egg nursery, a trap workshop, a larder and the chief's hoard room. Kobold traps guard the way in: a hidden spike strip at the mouth, a rockfall halfway down the tunnel, a dart trap at the shrine and a straw-covered pit in front of the hoard. Trapmakers re-arm them, kobolds scurry through bolt-holes too small for you, and breaking an egg clutch brings the whole warren down on you.

![A kobold warren from outside](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/kobold-warren/kw-overview.png)

## How it works
### Where they go
- **Biomes**: the middle or a corner of the chunk's search square (±16 blocks) must be in the kobolds' habitat, and so must the mouth itself (`#dndclasses:has_structure/kobold_warren`): windswept hills, gravelly hills and forest, stony peaks and the three badlands. Dripstone caves are left out; the warren needs a hillside. These biomes are uncommon, so warrens are too: in testing, 2 of 5 seeds had one within 1500 blocks of 0 0.
- **Spacing**: structure set `dndclasses:kobold_warrens`, spacing 20 chunks, separation 6.
- **The mountainside test**: round the chunk's middle (±16 blocks, every 4), it looks for a column where the ground 10 blocks along one of the four compass directions stands at least 10 blocks higher: a cliff or slope face. The column must be dry, at least sea level + 4, with open ground in front, and the hill must still stand at least 2 blocks above the mouth 20, 30 and 40 blocks in, so the rooms are under it. The mouth faces down the slope and the warren digs into the hill.
- **Clearance**: no warren within 4 chunks of a goblin camp, dwarven fortress, Cogsdeep, hobbit village, orc stronghold or dragon lair.
- **Great warrens**: 1 in 5 (20%), and any within 12 chunks of an Ancient's lair (the `dndclasses:ancient_lairs` structure set, from the Ancient lair card). A great warren adds a guard hall, more kobolds, and its traps are tier 2 instead of tier 1.

### Layout
The warren is built in its own frame (the mouth facing out of the hill):

| Room | Size | What's in it |
|-|-|-|
| Mouth | 5 wide, 4 high | rough stone and coarse-dirt lip, bone piles, two red-dragon banners, a hidden **spike strip** 3 blocks in. A Kobold Slinger keeps watch (plus a Kobold in a great warren) |
| Tunnel | 3 x 3, 12-18 long | drops one block every 3, torches every 6 ("dragon fires"); halfway down, a **rockfall**: a tripline across the tunnel under a 3 x 3 of loose ceiling |
| Den | oval 13 x 9, domed roof 5 high | the hub: a campfire, straw bedding, bones, 3-5 kobolds (60% Kobold, 40% Slinger) and two snares hidden in the straw |
| Guard hall (great only) | 13 x 9 | a second den off the front east side: 4-6 kobolds and a Kobold Trapmaker |
| Shrine (west) | 9 x 7, 6 high | a red terracotta dragon idol with nether-brick horns, coin piles, the **Dragon Tribute Bowl**, two red candles and a kobold. A **dart trap** guards its corridor |
| Nursery (east) | 7 x 7, low and dark | straw floor, 3-6 **Kobold Egg Clutches** and a kobold minding them |
| Workshop (back west) | 7 x 5 | a workbench, a chest (`chests/kobold_workshop`), tripwire hooks, cobwebs and 1-2 Kobold Trapmakers |
| Larder (back east) | 5 x 5 | barrels (`chests/kobold_larder`), a composter, mushrooms, 50% a kobold |
| Chief's room (straight back) | 11 x 9, 5 high | the **hoard chest** (`chests/kobold_hoard`), the Kobold Dragonshield, a Kobold and a Slinger, coin piles, a gold block, two banners. Its corridor has a **straw-covered pit** |
| Bolt-holes | 1 x 1 | den to shrine, larder to chief's room, and tunnel up to the hillside behind a bush |

Every warren kobold is persistent (it doesn't despawn) and keeps to its room: it wanders back if it strays more than 5-20 blocks from where it was placed (20 for the workshop's Trapmakers, who go out to fix traps). The warren is their **home**, so they're always brave inside it (see [Kobolds](../mobs/kobolds.md#courage)), and Unfriendly players are fair game there.

Room walls are left as the hill's own rock; only fluids and sand or gravel next to a room are sealed with stone.

### The traps
Warren traps are kobold traps (`Faction: dndclasses:kobolds` in the trigger), using the [dungeon trap](../systems/dungeon-traps.md) rules for spotting, saves (the `trap` tag) and disarming, with these differences:
- They **spare** kobolds, players at **Honored** or better with the Kobold Tribes, and the drow's allies.
- They **re-arm** without a dungeon reset: a Kobold Trapmaker walks to a spent or disarmed trap within 24 blocks (out of combat), works on it for 3 s and re-arms it. A spent (not disarmed) trap with any kobold within 16 blocks also re-arms itself 2 minutes (2400 ticks) after it went off.
- Tier 1 in a normal warren, tier 2 in a great one: DC 12 or 13.

| Trap | Where | What you see | Effect | DEX save |
|-|-|-|-|-|
| Spikes | the mouth, 3 in | two coarse-dirt tiles (a buried trigger beside them) | spikes shoot up for 2 s: 3 + tier trap damage and Slowness II for 3 s. The strip re-arms itself 10 s later unless it was disarmed | half damage, no slowness |
| Rockfall | halfway down the tunnel | a thin tripline (`kobold_tripline`) across the floor; cracked loose ceiling (`loose_ceiling`) 4 above | a rumble and dust for 0.5 s, then the 3 x 3 ceiling falls: 4 + 2 x tier trap damage and **Prone** for 2 s to everyone under it. Rubble is left on the floor until it re-arms | half damage, not Prone |
| Dart | the shrine corridor | a stone tile in the floor, a launcher in the wall | as the dungeon dart trap | no poison |
| Pit | the chief's corridor | straw-lidded crumbling floor (3 x 2) | as the dungeon pit: a 4-deep pit onto pointed dripstone, a ladder up the far wall | you catch the edge |

A Rogue (or Artificer) disarms a spike strip, a tripline or the straw by sneak-using it with an empty hand, as in dungeons.

### The warren law
- **The hoard chest** is locked, like most structure chests ([lockpicking](../systems/d20-skill-checks.md)): a Rogue picks it, anyone else smashes it open. Opening or smashing it where a kobold within 24 blocks can see you turns every kobold of the warren (within 48 blocks of the witness) on you, whatever your standing, and costs `theft_witnessed` (-15) with the Kobold Tribes.
- **Egg clutches**: breaking a Kobold Egg Clutch costs **30** standing with the Kobold Tribes whether a kobold sees it or not ("the warren will know"). If one does, it shrieks and the warren comes for you as above: "The kobolds shriek! The whole warren comes for you.". Clutches drop nothing.
- Creative and spectator players and the DM are exempt.

### Bolt-holes and running
Bolt-holes are 1-block tunnels a 0.95-tall kobold fits through and a player doesn't. A kobold below 30% health runs for the nearest bolt-hole within 20 blocks before it runs for its pack.

### Loot
| Chest | Contents |
|-|-|
| Hoard (`chests/kobold_hoard`) | 4-12 gold nuggets, 1-3 gold ingots, 0-4 copper ingots, 1-3 Kobold Trinkets, 0-2 Kobold Scales, 40% an emerald; plus 20% a magic item (85% Uncommon, 15% Rare) and 2% a pair of [Sending Stones](../items/magic-items.md#sending-stones) |
| Workshop (`chests/kobold_workshop`) | 2-6 string, 1-3 tripwire hooks, 2-5 iron nuggets, 1-3 Kobold Snares, 0-8 cobblestone, 20% a Kobold Sling |
| Larder (`chests/kobold_larder`) | 2-4 rolls of rotten flesh, mutton, eggs, potatoes and brown mushrooms, and a honey bottle |

## Where to find it
`/locate structure dndclasses:kobold_warren`, or look for a dark cave mouth with red banners in a hillside of the windswept hills, stony peaks or badlands. Warrens are in the `#dndclasses:faction/kobolds` structure tag, so the [Kobold Tribes](../mobs/kobolds.md#the-kobold-tribes-dndclasseskobolds) count them as theirs. Kobolds also spawn round warrens like other monsters at night.

## Commands
- `/dndhome koboldwarren [mouth|spikes|rockfall|den|guard|shrine|nursery|workshop|larder|pit|chief|hole|overview]`: teleports you to that spot of the nearest warren, looking at it, and prints the triggers of its traps, the hoard chest and an egg clutch.
- `/dndhome koboldwarrensurvey [grid] [spacing]`: from a grid round you (default 3 x 3, 1500 apart), checks each nearest warren's mouth is on a slope, dry, above sea level + 4 and has all 9 piece kinds (`[WarrenSurvey]` lines and a summary).
- `/dndhome koboldwarrenseeds [seeds]`: the nearest warren to 0 0 on each seed, within 1500 blocks, without making a world.
- `/dndhome koboldtrap <rockfall|spikes|pit|dart> [tier]`: builds a kobold test trap 3 blocks in front of you on a stone strip and prints its trigger.

## Known limitations
- The mountainside test tries the four compass directions, not eight.
- At Honored the traps stay quiet under you, but no kobold hisses "Not there, friend!".
- The shrine's dart launcher has the dungeon look.
- Great warrens near Ancient lairs only happen once the Ancient lair card adds the `dndclasses:ancient_lairs` structure set.
- The trap registry the Trapmakers use is in memory: after a restart, a spent trap is found again when its chunk ticks.

## For developers
- Package `mattonfire.dnd.world.gen.warren`: `KoboldWarrens` (registration, piece types, egg clutch, law, tags, home and refuge hooks), `KoboldWarrenStructure` (site finder), `KoboldWarrenPlanner` (layout), `WarrenPiece` (base and builder), the pieces `WarrenMouthPiece`, `WarrenTunnelPiece`, `WarrenDenPiece`, `WarrenShrinePiece`, `WarrenNurseryPiece`, `WarrenWorkshopPiece`, `WarrenLarderPiece`, `WarrenChiefPiece`, `BoltHolePiece`, plus `KoboldWarren` (lookups: `contains`, `pieceAt`, `refuge`), `KoboldWarrenLaw` (`SettlementGrudges.Rules`) and `KoboldTrapRigs` (trap setup and test rigs).
- Trap blocks in `classes/Blocks`: `SpikeTrapBlock`, `LooseCeilingBlock`, `KoboldTriplineBlock`, `KoboldEggClutchBlock`; `CrumblingFloorBlock` has a `covering` (stone/straw). New `TrapKind`s `ROCKFALL` and `SPIKES`. Trigger NBT adds `Faction`, `Buried`, `Covering`, `Rubble`, `SpentTime`, `RockfallAt`, `ImpactAt`, `SpikesUntil`.
- Kobold side: `entity/kobold/KoboldTraps` (spent kobold traps), `ai/KoboldRearmGoal` (Trapmakers), `KoboldCourage.registerRefuge` / `refugeOf` (used by `KoboldFleeGoal`).
- Hooks for other cards: the `dndclasses:ancient_lairs` structure set (great warrens), `KoboldWarren.contains` for "at home".
- Logs: `[Trap] rockfall hits ...`, `[Trap] spikes hit ...`, `[Trap] kobold rearm <kind> at <pos> (Kobold Trapmaker|kobolds nearby)`, `[Grudges] <player> broke a kobold egg clutch ...`.
- Assets: `tools/kobold_warren_textures.py`.
- Devscripts: `devscripts/kobold-traps.txt` (rockfall, spikes, disarm, re-arming, Honored, pit, egg clutch) and `devscripts/kobold-warren.txt` (seeds, survey, shots of every room, the hoard law).
