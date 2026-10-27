# Underdark monster lairs
Three monsters keep homes in the [Underdark](../systems/underdark.md): a **Basilisk Den** full of petrified drow and duergar, a **Troll Bridge** over a Darklake channel with a troll living under one end, and an **Illithid Colony** of glowing purple chambers where Mind Flayers keep thralls and a larder of captives round a bubbling brine pool. Each lair's monster is placed with it and never despawns.

## How it works
All three generate only in the Underdark, and keep at least 5 chunks away from the Drow Outposts, the Duergar Holds, the Drow Spire and the Rift Landings. The Underdark's giant fungi don't grow through them (they're in `#dndclasses:underdark/settlements`). Nothing else spawns inside a lair (an empty `spawn_overrides` list).

| Lair | Biomes | Spacing / separation (chunks) |
|-|-|-|
| Basilisk Den | Grey Reaches, Faerzress Hollows | 16 / 6 |
| Troll Bridge | Darklake | 14 / 5 |
| Illithid Colony | Faerzress Hollows | 40 / 16 |

### Basilisk Den
A low dome of radius 10 and 7 blocks high, hollowed out of a cave floor between y -220 and -110 (the floor must be flat to 3 blocks over 15 of 25 points 4 apart, with 7 blocks of air).
- The floor is levelled (up to 3 blocks filled) with gravel, tuff, cobbled deepslate and the odd bone block. A 2-block shell of rock closes it in, veined with glowing Faerzress Stone, with a mouth 3 wide on one side.
- **7 petrified victims** stand round the middle, armour stands in stone-grey leather with their arms up against the gaze, posed and coloured like the [Beholder](../bosses/beholder.md)'s statues. Most are drow (an iron sword or a crossbow, no helmet) or duergar (helmed, with a pick or an axe, sometimes a shield); about 1 in 7 is a surface adventurer. **5 Petrified Remains** kneel by the walls, and skulls, bone blocks and webs lie about.
- A **chest** against the back wall holds what the victims carried (`chests/basilisk_den`): worn iron weapons and helmets, crossbows, arrows, spidersilk, Duergar Steel, gold nuggets, emeralds and bones; a Salve of Stone to Flesh 35% of the time, an uncommon magic item 25%.
- One **[Basilisk](../mobs/basilisk.md)**, kept within 8 blocks of the middle.

### Troll Bridge
A Zurkhwood bridge across a channel of the Darklake 5 to 18 blocks wide, found where open water meets a bank on both sides (each within 16 blocks, at most 6 blocks above the water, 7 blocks of air over the middle). Such channels are uncommon, so bridges are rarer than the spacing suggests: one within 1200-1700 blocks of the origin on the seeds tried.
- The deck is 3 wide, at least 4 blocks over the water, on Zurkhwood beams with fence rails and a post every 4 blocks: skulls on the end posts, soul lanterns on the middle ones. Zurkhwood piles go down to the lake bed every 4 blocks. It runs 2 blocks onto one bank and over the nook on the other, then stairs lead down to each bank: 14 to 27 blocks end to end.
- Under one end, the **nook** is dug into the bank at the water's level, 5 wide and 4 deep, open to the channel: gravel, coarse dirt and mud, heaps of bone, skulls and webs, a soul lantern, and the **toll chest** (`chests/troll_toll`): emeralds, gold, iron nuggets, Darklake Pearls, bones and rotten flesh, an uncommon magic item 20% of the time.
- One **[Troll](../mobs/troll.md)** lives in the nook, kept within 10 blocks of it.

### Illithid Colony
Organic, purple-grey chambers swelling out of a wide cavern floor (the settlement site rules: a floor between y -220 and -110, flat to 4 blocks over 16 of 25 points 8 apart, 12 blocks of air).
- Four rough domes run into each other: the **core** (radius 9, 8 high), with the **larder** behind it and the **den** and the **hall** either side (radius 5, 5 high). The walls are 2 blocks of Polished Faerzress, Faerzress Stone, tuff, crying obsidian, amethyst and smooth basalt; sculk spreads over the floor. A tunnel through the core's front wall is the way in, framed with crying obsidian.
- The **brine pool** fills the middle of the core: radius 3.5, 3 deep, with magma under it pulling bubbles down. It's set aside as the arena of a future Elder Brain.
- **2 or 3 [Mind Flayers](../mobs/mind-flayer.md)** (core, den, and half the time the hall), kept within 12 blocks of their spots. They don't call up thralls of their own.
- **3 to 5 thralls**, drow and duergar from `#dndclasses:underdark/thralls` (drow, duergar soldiers and arbalesters; bosses left out). They carry the tag `dndclasses.colony_thrall`, and a Mind Flayer within 24 blocks [Charms](../systems/conditions.md) any of them that isn't Charmed within a second: they follow it and fight its target, the duergar too (settlement folk who are Charmed take their charmer's target over their own rules). If their flayer dies, another flayer of the colony nearby takes them over.
- The **larder**: three cells behind locked Chain Cage gates at its back. The first two always hold a kneeling captive (a hobbit, a mountain dwarf or a wood elf) and the third half the time. Opening a gate frees its captive for reputation with its folk, as in a [Drow Outpost](drow-outposts.md).
- A **chest** in the den (`chests/illithid_colony`): amethyst, experience bottles, enchanted books, ender pearls, Darklake Pearls, gold, emeralds, an echo shard; a rare magic item 30% of the time. The Mind Flayer quest chain after "Shadows Below" is to start here.

### Elsewhere in the Underdark
- The monsters also spawn on their own in their habitat biomes: Troll weight 6 (Fungal Forest, Darklake), Basilisk 4 (Grey Reaches, Faerzress Hollows), Mind Flayer 3 with its thralls (Faerzress Hollows), Rust Monster 6, 1-2 at a time (Grey Reaches, Mycelial Caverns).
- Each [Duergar Hold](duergar-holds.md)'s mine gallery has a Rust Monster in it, kept within 6 blocks of the ore face.
- [Drow Outposts](drow-outposts.md) can keep a troll chained in their pen's beast cell.

## Where to find it
- In the Underdark, in the biomes above. `/locate structure dndclasses:basilisk_den` (or `troll_bridge`, `illithid_colony`) from inside the Underdark, or `/dndhome underdarklair <lair>` from anywhere.

## Commands
Operator commands (see [Admin commands](../systems/admin-commands.md)); `<lair>` is `basilisk`, `troll` or `illithid`. They work from anywhere: outside the Underdark they search below your x and z.
- `/dndhome underdarklair <lair> [spot]`: teleports you to the nearest lair of that kind and prints where it is and its biome. Spots: `inside` (default: the den's mouth, the middle of the bridge, the colony's entrance), `above`, `chest`; the bridge also `side` and `nook`, the colony `core` and `larder`.
- `/dndhome underdarklairsurvey <lair> [grid] [spacing]`: from a grid of points round you, checks the nearest lair to each: its biome, and (generating its chunks) the den's floor, remains and chest; the bridge's deck end to end, its length (14-27), the water under it, the open nook and the chest; the colony's pool water and magma, the locked larder gates, the open entrance and the chest. Logged as `[LairSurvey]`.
- `/dndhome underdarklaircensus <lair>`: counts basilisks, trolls, Mind Flayers (and how many monsters are persistent), colony thralls (and how many a flayer holds), captives and statues in the nearest lair (`[LairCensus]`).
- `/dndhome underdarklairseeds <lair> [seed...]`: the nearest lair to 0, 0 for this world's seed and two others, without generating anything (`[LairSeeds]`).

## Known limitations
- A Basilisk is 3.2 blocks wide, too wide for a Drow Outpost's pen cell, so only the troll joins `#dndclasses:underdark/pen_beasts`.
- The Elder Brain isn't in the game yet; the brine pool is just a pool.
- A troll that leaves its nook to chase you can come back up the bank's stairs onto the deck; it isn't tied to the bridge.

## For developers
- Package `mattonfire.dnd.world.underdark.lair`:
  - `UnderdarkLairs`: the `Lair` enum (structure key, set, locate tag), structure and piece types, loot table ids.
  - `UnderdarkLairStructure`: the Underdark-only check and `KEEP_CLEAR_OF` (rift landings, drow outposts, duergar holds, drow spires, `CLEARANCE` 5). One-sided: the settlements don't check the lairs.
  - `BasiliskDenStructure` (`RULES`: y -220..-110, grid 5, spacing 4, 15 matches, tolerance 3, headroom 7) and `BasiliskDenPiece` (`RADIUS` 10, `HEIGHT` 7, `SHELL` 2, `FILL` 3, `STATUES` 7, `REMAINS` 5, `BASILISK_RANGE` 8).
  - `TrollBridgeStructure`: reads the noise columns for water at the sea level less one (a 5 x 5 grid, `STEP` 4, `MIN_DEPTH` 2, `MAX_REACH` 16, `MAX_BANK` 6, `BANK_AIR` 4, `MIN_SPAN` 5, `MAX_SPAN` 18, `HEADROOM` 7, `ONTO_BANK` 2). `TrollBridgePiece` (`POST_EVERY` 4, `NOOK_WIDTH` 5, `NOOK_DEPTH` 4, `TROLL_RANGE` 10; `deckY` = surface + max(4, bank + 1)).
  - `IllithidColonyStructure` (settlement rules) and `IllithidColonyPiece` (`CHAMBERS`, `POOL_RADIUS` 3.5, `POOL_DEPTH` 3, `FLAYER_RANGE` 12, `THRALL_RANGE` 14; `pool()` for a future Elder Brain).
- `world/gen/PetrifiedStatues`: the statue helper shared with `BeholderCavernPiece` (`adventurer`, `statue(..., Victim, Random)` for drow, duergar and adventurers, `remainsFacing`).
- `MindFlayerEntity.COLONY_THRALL_TAG`, `claimColonyThralls()`, `setPendingThralls(int)`. `ConditionEffects.charmerTarget(mob)`; `SettlementNpcEntity.setTarget` lets a Charmed one take its charmer's target.
- Spawn weights in `world/gen/ModSpawns.java` through the `#dndclasses:habitat/*` biome tags; the mine's Rust Monster in `DuergarMinePiece`.
- Data: `worldgen/structure/{basilisk_den,troll_bridge,illithid_colony}.json`, `worldgen/structure_set/{basilisk_dens,troll_bridges,illithid_colonies}.json`, `tags/worldgen/biome/has_structure/<lair>.json`, `tags/worldgen/structure/<plural>.json`, `loot_tables/chests/{basilisk_den,troll_toll,illithid_colony}.json`, `tags/entity_types/underdark/{thralls,pen_beasts}.json`.
- Commands: `classes/Commands/LairCommands.java`.
- Devscripts: `devscripts/underdark-lair-seeds.txt` (where they are), `devscripts/underdark-lairs.txt` (teleports, surveys, censuses, screenshots, thralls holding), `devscripts/colony-thralls.txt` (a tagged drow and duergar charmed by a flayer, then both going for the player it targets).
