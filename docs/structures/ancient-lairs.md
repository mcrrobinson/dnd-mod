# Ancient dragon lairs
The home of an [Ancient Red Dragon](../bosses/ancient-red-dragon.md): a volcanic caldera raised on a hill or mesa, with the dragon asleep on a basalt perch above its crater and its hoard in a domed hall under it. Kobolds keep the stair. It's the only place an Ancient comes from, and once its dragon is dead the lair stays empty for good.

![An Ancient Red's caldera from the air, the dragon on its perch](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/feat-ancient-dragon-lair/ancient_lair-air.png)

## How it works
### The caldera
- A cone of blackstone, basalt, smooth basalt and tuff, 44 blocks in radius at its foot, rising at least 24 blocks above the ground round it to a rough rim at radius 30. On a hill that's already higher, the cone is cut down into the hilltop instead. Seams of magma run down its sides.
- The **crater floor** (radius 26) sits 6 blocks below the rim: blackstone and basalt with cracks of magma, piles of **Shed Red Scales** (1 in 20 floor blocks) and charred bones.
- **Three lava pools**, radius 2-4, sunk two deep into the floor and ringed with a raised basalt kerb, so the lava stays put.
- **4-6 obsidian spires** (2x2, 6-12 high, a touch of crying obsidian, tapering to a point).
- **The Perch**: a 9x9 plateau on a 14-high basalt pillar, standing on a blackstone-brick plinth 15 blocks from the middle, on the far side from the mouth. Red scale piles lie on it. This is where the dragon rests.
- **The mouth**: a 7-wide gash through the rim on the lowest side, with a stair running down the cone's slope to the ground.
- **The dragon**: when the lair's chunk loads, a marker on the perch raises its Ancient Red (named, with its lair set to the caldera), facing the mouth, unless the lair is slain. It goes back to its perch when it has nothing to fight.

![The crater floor and the Perch, with the player for scale](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/feat-ancient-dragon-lair/ancient_lair-scale-crater.png)

### The stair and the kobold quarters
- A door in the perch's plinth opens on a stair, 3 wide and 3 high, that winds round a core under the perch and comes out into the hoard hall 20 blocks down. The dragon is too big to follow.
- Two traps on the stair, tier 3 (DC 15, see [Dungeon traps](../systems/dungeon-traps.md)): a **dart trap** on the first landing, shooting out of the wall, and a covered **pit** (4 deep, with a ladder) on the far side.
- **The barracks** (9x7x4), off the second landing: hay beds along the walls and a weapon rack.
- **The trap workshop** (7x5x4), off the third landing: benches, barrels, cobwebs and a `chests/kobold_workshop` chest (4-7 rolls of string, arrows, cobblestone, iron and gold nuggets, flint, tripwire hooks and pointed dripstone; the Kobold Warrens' version of the table replaces this one).

![The kobold barracks](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/feat-ancient-dragon-lair/ancient_lair-barracks.png)

### The hoard hall
- A dome of polished blackstone, radius 18 and 14 high, on six fluted basalt pillars with amethyst growing on them, lit by hanging lanterns.
- Treasury Coin Piles over about 70% of the floor, heaps of gold and raw gold blocks set with an emerald or diamond, the dragon's sleeping hollow in the middle, and **4-6 fallen heroes** (armour stands in gold or chain armour, slumped among the coins with a skull or bones beside them).
- **Four hoard chests** (`chests/ancient_red_hoard`): 8-24 gold ingots, 1-3 gold blocks, 2-6 diamonds, 2-6 emeralds, 1-4 amethyst shards, a 30% chance of an enchanted golden apple and a 20% chance of a netherite ingot, plus one magic item: Rare (50%), Very Rare (40%) or Legendary (10%).
- **The Hoard Coffer** (`chests/ancient_red_hoard_coffer`) on a gold dais behind the hollow: 2-4 gold blocks, 4-8 diamonds and a magic item, Very Rare or Legendary (50/50), and a 1 in 4 chance of a **Tome of Clear Thought** (see [Magic loot](../systems/magic-loot.md)).
- All five are hard locks (DC 15) for [lockpicking](../systems/d20-skill-checks.md).

![The hoard hall](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/feat-ancient-dragon-lair/ancient_lair-hall.png)

### The hoard law
Open a chest in the hoard hall (a picked lock counts) or break one while its dragon lives, and it wakes. Everyone within 96 blocks reads *"Somewhere far above, something vast opens one eye."*, the dragon (anywhere within 128 blocks) takes the thief as its target and its Frightful Presence goes off at once. There's no warning and no trust to lose first. Creative and spectator players don't count.

### Kobold guards
The lair leaves 12 guard posts: six on the crater floor (warriors, slingers and a trapmaker), four in the barracks and two at the foot of the stair. Each is an invisible marker that raises a kobold once the Kobold Tribes exist (see For developers). Until then they wait, so lairs made before the kobolds arrive still get them later.

### Slain for good
When an Ancient with a lair dies, the lair is marked slain (logged `[Lair] <name> is slain`). A slain lair never raises its dragon again: its perch marker is dropped when the chunk loads, and the dragon's own drops are the only reward. The hoard stays where it was.

When the dragon's Tremor lair action goes off, dust shakes down from the hall's dome onto anyone inside.

## Where to find it
- On high ground in badlands (all three kinds), savanna plateaus, windswept savannas, windswept hills (and gravelly hills), stony peaks and jagged peaks.
- Structure set `ancient_lairs`: spacing 32 chunks, separation 12. Each grid slot looks for an allowed biome within 48 blocks of its middle, then the highest ground within 20 blocks of that. The site has to stand at least 20 above sea level, with the ground round the cone's foot at least 6 lower on average (a rise, not a pit), and be dry.
- Never within 10 chunks of an orc stronghold, dragon or frost lair, dwarven fortress or dragon temple that really generates.
- On 5 of 6 test seeds (the dev world, 1, 42, 1234 and 20261025; not 7) there's one within 3000 blocks of 0, 0 (`ancient-lair-seeds.txt`).
- The [Red Dragon Scale Mail](../bosses/ancient-red-dragon.md)'s dragon sense points to the nearest one (within 3000 blocks).

## Commands
- `/locate structure dndclasses:ancient_red_lair`
- `/dndhome ancientlair [spot]`: teleports you to a spot of the nearest lair (`mouth`, `air`, `crater`, `perch`, `door`, `dart`, `pit`, `barracks`, `workshop`, `hall`, `chest`, `coffer`) and prints its layout.
- `/dndhome ancientlairsurvey [grid] [spacing]`: checks the nearest lair to each grid point: heights, slain flag, lava and water in the caldera, the stair, chests, dragon and guard posts (`[AncientLairSurvey]`).
- `/dndhome ancientlairseeds [seed...]`: the nearest lair to 0, 0 within 3000 blocks on each seed, without generating anything (`[AncientLairSeeds]`).
- `/dndhome ancientlairreset`: forgets that the nearest lair's dragon was slain and puts a new one on its perch (tests).

## Known limitations
- Only the Red has a lair. The structure type takes a `color`, but the other colours have no dragon yet.
- The guard posts stay empty until the Kobold Tribes fill `#dndclasses:lair_guards/<role>`.
- A slain dragon's hoard is never refilled.
- `#dndclasses:boss_lairs` only lists this lair here. The Deck of Many Things ticket creates that tag with the other boss lairs (`replace: false`, so the two lists merge); keep both entries if they meet in one file.

## For developers
- `world/gen/ancient/`: `AncientLairStructures` (registers the `ancient_lair` type and the `ancient_caldera` and `ancient_hoard_hall` pieces), `AncientLairStructure` (site choice; `REJECTS` counts why sites were turned down, for the seeds check), `AncientLairLayout` (sizes and the lair's frame: `at`, `stair`, `perchTop`, `hallCenter`), `CalderaPiece`, `HoardHallPiece`, `AncientLairs` (the slain state, `PersistentState` `dndclasses_ancient_lairs`, keyed by lair centre), `AncientHoardLaw` (a `SettlementGrudges.Rules`), `AncientLairGuards`.
- The dragon: `StructureBosses.placeAncient` leaves a marker tagged `dndclasses.pending_boss.ancient.<color>` and `dndclasses.lair=x,y,z`, raised by `spawnAncient` unless `AncientLairs.isSlain`. `AncientDragonEntity` has a `Perch` (NBT) it flies back to, and `wakeForHoard(player)` (logs `[MobAction] <name> hoard_wake`).
- Hooks it listens to: `AncientDragonEvents.SLAIN` (marks the lair slain) and `TREMOR` (the dust).
- **Kobold guards (for the Kobold Tribes ticket)**: markers tagged `dndclasses.lair_guard_post` with `data:{Role:"warrior"|"slinger"|"trapmaker"|"leader", Lair:{X,Y,Z}}`. As a post loads, and every 200 ticks, if the entity type tag `#dndclasses:lair_guards/<role>` has anything in it and the difficulty isn't Peaceful, a random type from it takes the post: it's tagged `dndclasses.lair_guard`, made persistent, kept within 12 blocks, and the marker goes (`[LairGuards] a <type> (<role>) takes the guard post at ...`). Adding kobolds to those tags is all it takes.
- Tags: `#dndclasses:ancient_dragon_lairs` (every colour's lair; the scale mail looks for it), `#dndclasses:boss_lairs`, biome tag `has_structure/ancient_red_lair`.
- Data: `worldgen/structure/ancient_red_lair.json` (`"color": "red"`), `worldgen/structure_set/ancient_lairs.json`, loot tables `chests/ancient_red_hoard`, `chests/ancient_red_hoard_coffer`, `chests/kobold_workshop`.
- The Shed Red Scales block (`LairBlocks.RED_SCALE_PILE`, `red_scale_pile`) is generated by `tools/lair_block_textures.py`; each layer drops 0-1 Ancient Red Scale.
- Devscripts: `ancient-lair-seeds.txt` (three seeds and the dev world), `ancient-lair.txt` (looks over the dev world's lair, near 1088 -2492, and checks the lava after 200 ticks), `ancient-lair-hoard.txt` (scale, guard posts with the test datapack `devscripts/datapacks/lair-guards-test`, dragon sense, the hoard law, the kill) and `ancient-lair-rejoin.txt` (the slain lair stays empty, then resets it).
