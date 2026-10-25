# The Underdark
A sunless world of huge caverns under the Overworld's bedrock, from y -256 to -65, lit only by what grows there. It's its own dimension (`dndclasses:underdark`) at the same x and z as the Overworld above, with five regions: the Fungal Forest, the Mycelial Caverns, the Darklake, the Faerzress Hollows and the Grey Reaches. Its forests are giant zurkhwood and glowing nightlight towers, its lakes are misty and full of glowing Waterorbs and Pearl-Clams, and it has its own music. You get there through [Underdark Rifts](../structures/underdark-rifts.md): sinkholes in the Overworld down to a pool of Rift Veil that drops you in, and Rift Anchors that pull you back up. The drow and duergar and their settlements come with the later Underdark tickets.

![The Fungal Forest with Night Vision: Fungal Loam, Trillimacs and a zurkhwood stalk](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/underdark-dimension/underdark-fungal_forest-nightvision.png)

| Fungal Forest | Faerzress Hollows | Grey Reaches |
|-|-|-|
| ![Fungal Forest](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/underdark-dimension/underdark-fungal_forest.png) | ![Faerzress Hollows](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/underdark-dimension/underdark-faerzress_hollows.png) | ![Grey Reaches](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/underdark-dimension/underdark-grey_reaches.png) |

The three small shots are what a player with Darkvision sees (an Elf, brightness at maximum).

## How it works
### The dimension
| Setting | Value | Why |
|-|-|-|
| Height | y -256 to -65 (192 blocks) | under the Overworld's bedrock floor (-64) |
| Sky | none: a bedrock roof, no sky light, no clouds, no sun | like the Nether |
| Ambient light | 0.05 (the Nether is 0.1) | dark enough that light sources matter; [Darkvision](../races/passive-traits.md) reads well |
| Time | always midnight (fixed time 18000) | clocks and compasses spin; night-only features are always on (see below) |
| Beds | work, set your spawn and give long rests; they don't skip the Overworld's night | respawn anchors don't work |
| Monster spawning | Overworld rules: block light 0 and light level 0-7 | lit areas are safe |
| Coordinates | 1:1 with the Overworld | the Underdark under a spot is at the same x and z |
| Raids, piglin safety, ultrawarm | none | |

The terrain comes from vanilla's **Caves** world preset (a ceilinged world of huge Nether-like caverns), moved down 192 blocks:
- The floor and the roof are bedrock, up to 5 blocks thick (thinning out like the Nether's).
- The rock is deepslate. There are no aquifers: every open cell below y -222 is water, so the deepest basins flood into lakes (about a fifth of all open floors are under water).
- The caverns stack two or three deep in most places; the median floor is around y -180.
- No cave carvers: the caverns are the terrain itself, so every floor gets its region's floor blocks.
- No coal or copper (surface fuels). Ores per chunk: iron 10, gold 6 (twice the Overworld's deep gold), diamond 9 small veins in the lower half, redstone 6, lapis 3, plus tuff and gravel blobs.

### Regions
The five biomes are placed by two climate noises, so each region is a patch roughly 100-250 blocks across and all five usually turn up within a few hundred blocks. The climate is split into even fifths: the driest 40% is the Faerzress Hollows and the Grey Reaches, the wetter 60% the Darklake, the Fungal Forest and the Mycelial Caverns (cold to warm), so over a large area each region has about a fifth of the ground.

| Biome | Looks | Floor / ceiling | Features | Ambient particles |
|-|-|-|-|-|
| **Fungal Forest** | blue-grey fog; forests of giant zurkhwood with flat violet caps and glowing nightlight towers over spongy Fungal Loam | Fungal Loam / deepslate | giant zurkhwood ×4 and nightlight towers ×2 per chunk, Ripplebark clusters ×12, Barrelstalks ×1, Bluecap patches ×4, Trillimac patches ×2, glow lichen ×40, Faerie Lichen ×10 | drifting blue spores |
| **Mycelial Caverns** | dim violet fog; red and brown giant mushrooms on mycelium | mycelium over dirt / deepslate | giant zurkhwood ×1, huge red and brown mushrooms ×3 each, small mushrooms ×3 each, Trillimac ×3, glow lichen ×20 | mycelium spores |
| **Darklake** | black-blue fog and water, mist over the lakes; the lowest caverns, flooded | gravel, clay below the waterline (+2) / deepslate | extra lake pools (clay-lined), clay-and-gravel shores ×4, Pearl-Clams (2 tries in 1 chunk in 4), water springs ×20, ceiling moss ×24, clay pools with dripleaves ×16, nightlight towers on the shores ×3 tries, Ripplebark ×6, Barrelstalks ×1, seagrass ×6 patches, Waterorb ×3 patches (up to 6 each), glow lichen ×60; glow squid and axolotls | dripping water |
| **Faerzress Hollows** | violet fog; walls veined with glowing faerzress | deepslate with Faerzress Stone in about a third of it / deepslate | faerzress veins ×8 (size 33), amethyst geodes (1 chunk in 12), Bluecaps ×2, glow lichen ×120, Faerie Lichen ×60 | portal motes |
| **Grey Reaches** | ash-grey fog; barren tuff and basalt, dripstone and lava | tuff, basalt in patches / tuff | magma patches ×6, lava springs ×20, large dripstone ×12, dripstone clusters ×8 | white ash |

Every biome also plays distant cave sounds now and then (vanilla's `ambient.cave` as an additions sound, chance 0.0111 a tick), on top of the usual cave mood sound.

Every biome spawns spiders (weight 100, 1-3), cave spiders (40), zombies (60), skeletons (60), creepers (20), endermen (4) and witches (4), and bats. Mod mobs come with later tickets, through the habitat biome tags (see For developers).

### Giant fungi
The forests are made of two fungi the mod grows itself (the plants they come with are on the [Underdark blocks](../items/underdark-blocks.md) page):

| Fungus | Shape |
|-|-|
| **Giant zurkhwood** | a Zurkhwood Stem 1×1 and 9-14 tall (60%) or 2×2 and 15-24 tall (40%), leaning one block every 6, under a flat Zurkhwood Cap of radius 4-7 with a one-block downturned rim and Zurkhwood Gills underneath (shorter ones get a smaller cap). 0-3 little shelf caps (radius 1-2) stick out of the stem |
| **Nightlight tower** | a 1×1 Zurkhwood Stem 10-22 tall with rings of glowing Nightlight Cap every 4-5 blocks (radius 2 on the lower half, 1 above) and a domed cap of radius 2 on top, with 0-2 Glowing Spore Drips hanging under its caps. A Bluecap grows a small one, 6-9 tall |

Both need 3 blocks of clear air above their cap, and grow shorter under a low ceiling (or not at all where there's room for less than a 5-block zurkhwood stem or a 4-block tower). They stand on Fungal Loam, mycelium, gravel, clay, moss, dirt, deepslate, tuff, stone or Faerzress Stone, and grow through air and small plants but never through rock or water. Zurkhwood and Nightlight Spores grow the same shapes.

### The Darklake's mist
Over the Darklake's water a close mist closes in, like the Nether's thick fog: while your camera is in the Darklake and no more than 6 open blocks above water (`UnderdarkEffects.MIST_HEIGHT`). It lifts as you climb away from the lake.

### Music
Anywhere in the Underdark the mod's event music plays the Underdark track: `music.underdark`, a cold drone in D Phrygian with far-off crystal chimes, a slow deep pulse and dripping water (weight 3), or vanilla's deep dark music (weight 1), every 1-4 minutes (1200-4800 ticks between tracks). A boss fight, low health and a dungeon still play their own music first.

### Explorer maps
Underdark explorer maps lead to the nearest place of their kind (searching 100 chunks), marked on a vanilla explorer-style map:

| Map | Leads to | Marker |
|-|-|-|
| Map to a Drow Outpost | `#dndclasses:underdark_maps/drow_outpost` | red X |
| Map to a Duergar Hold | `#dndclasses:underdark_maps/duergar_hold` | red X |
| Map to the Rift | `#dndclasses:underdark_maps/rift` (a rift landing in the Underdark, a rift in the Overworld) | white target |

The places they lead to come with later tickets (outposts, holds, rifts); the tags already list them as optional entries, so each map starts working when its structure lands. Until then there's nothing to map. The duergar factor sells them and loot can hold them (see For developers).

### Night all the time
The Underdark's clock is fixed at midnight. The mod's night checks (`GameTime.isNight`) read 18000 there whatever the Overworld's time, so a [Blood Hunter](../classes/blood-hunter.md) has night strength the whole time they're below. Vanilla's own day checks see a fixed-time dimension as neither day nor night, like the Nether.

### Getting there and back
- Players find their own way down through [Underdark Rifts](../structures/underdark-rifts.md) (or a slain lair Beholder's floor), and back up with a Rift Anchor. The rest of this section is the admin command.
- `/underdark go` takes you down at your x and z. It looks for a dry, solid floor with two blocks of air above it between y -75 and -250, in your column and then in rings out to 8 blocks. It prefers a floor in an open cavern (6 blocks of air above), then the one nearest y -90; a cramped floor in your own column is the fallback. If there's no floor at all, it carves a 5×5×3 pocket at y -150 with a deepslate-tile floor, a lantern and a three-block Rift Anchor, sealing any water or lava round it.
- It remembers where you came from, and `/underdark back` returns you there (or to the world spawn if nothing is remembered).
- Dying in the Underdark respawns you at your bed or the world spawn, as usual. Logging out there logs you back in there.
- The Underdark is a separate world: you can't dig down into it from the Overworld. Pets stay behind, as with the Nether.

## Where to find it
Through an [Underdark Rift](../structures/underdark-rifts.md) (about one per 450×450 blocks of Overworld land) or a slain lair Beholder's floor. Ops can also use `/underdark go` and `/underdark biome`, or `/execute in dndclasses:underdark run tp @s x y z`.

## Commands
`/underdark` needs permission level 2.

| Command | What it does |
|-|-|
| `/underdark go [player]` | Down into the Underdark at the same x and z, on a found floor or a carved pocket; remembers where you were |
| `/underdark back [player]` | Back to the remembered spot, or to the world spawn |
| `/underdark biome <biome>` | Teleports you to the nearest Underdark biome of that kind (`fungal_forest`, `mycelial_caverns`, `darklake`, `faerzress_hollows`, `grey_reaches`), up to 48 blocks past its near edge, on a safe floor, facing the most open way |
| `/underdark lake [radius]` | Teleports you (in the Underdark) to the nearest dry shore of open water at the waterline within the radius (96 by default), looking out over it. For the mist and lake-life tests and screenshots |
| `/underdark flora [radius]` | Counts the giant fungi's blocks and the plants and lake life in the loaded blocks within the radius (48 by default), logged as `[Underdark] flora ...` |
| `/underdark map <drow_outpost\|duergar_hold\|rift>` | Gives you an explorer map to the nearest such place from where you are, or says there's none |
| `/underdark survey [radius]` | Samples the noise every 4 blocks round you (radius 128 by default) and prints each biome's share, floors per column, the median floor y, the share of floors under water, the bedrock at the floor and the roof of the chunk you're over, the cost in ms per column, and the climate noises' mean and spread (logged as `[Underdark] survey ...`) |

## Known limitations
- The explorer maps to drow outposts and duergar holds have nowhere to lead until those are in.
- Chunks of the Underdark generated before the giant fungi came keep the old small stand-in mushrooms.

## For developers
- Worldgen data: `data/dndclasses/dimension_type/underdark.json`, `dimension/underdark.json`, `worldgen/noise_settings/underdark.json`, `worldgen/biome/*.json`, `worldgen/configured_feature/underdark/*`, `worldgen/placed_feature/underdark/*`, and the biome tags. All of it is written by `tools/underdark_worldgen.py` from the vanilla Caves preset dumped into `tools/underdark/caves.json` (`java -DbundlerMainClass=net.minecraft.data.Main -jar server.jar --server`). The script moves the two `y_clamped_gradient` anchors of the Caves router down 192, swaps the router's climate noises (erosion at xz scale 2 for "temperature", vegetation at xz scale 1 for humidity; the Overworld's are far too large-scale) and replaces its surface rule. The biomes are climate *ranges* cut at the noises' quantiles (`TEMPERATURE_SD`, `HUMIDITY_SD`, measured with `/underdark survey 1024`), not points, so the shares are even by construction. Every biome lists its features in one global order, so the biome source can't hit a feature order cycle. Rerun it after changing anything.
- `world/underdark/Underdark`: the keys (`WORLD`, `DIMENSION_TYPE`, the five biome keys) and `isUnderdark(World)`.
- Biome tags for later tickets' spawns: `#dndclasses:is_underdark` (all five) and `#dndclasses:habitat/{drow,duergar,troll,basilisk,mind_flayer,rust_monster}` (`Underdark.HABITAT_*`). For example `BiomeModifications.addSpawn(BiomeSelectors.tag(Underdark.HABITAT_TROLL), SpawnGroup.MONSTER, TROLL, w, 1, 1)`.

  | Tag | Biomes |
  |-|-|
  | `habitat/drow` | Fungal Forest, Faerzress Hollows |
  | `habitat/duergar` | Grey Reaches |
  | `habitat/troll` | Fungal Forest, Darklake |
  | `habitat/basilisk` | Grey Reaches, Faerzress Hollows |
  | `habitat/mind_flayer` | Faerzress Hollows |
  | `habitat/rust_monster` | Grey Reaches, Mycelial Caverns |
- `UnderdarkLanding.findOrMake(world, x, z)`: the floor search and carved pocket. `UnderdarkTravel`: `descend`, `sendTo`, `back` and `teleport`, with the return spot in the player's persistent data under `dndUnderdark.Return`. `UnderdarkTravel.AFTER_TRAVEL` (player, from, to) fires after every trip in or out, so followers such as hirelings can come along. The rift trips are `riftDown`/`riftUp` (see [Underdark Rifts](../structures/underdark-rifts.md#for-developers)). Log lines: `[Underdark] <player> descends at x y z (landing|found|carved)` and `[Underdark] <player> returns to ...`.
- `UnderdarkSurvey` backs `/underdark survey`.
- Client: `client/UnderdarkEffects` (no sky or clouds, biome fog unchanged, darkened lightmap, thick fog over the Darklake's water: `isMisty`, logging `[Underdark] Darklake mist on|off`), registered for the dimension type's `effects` id `dndclasses:underdark`.
- Giant fungi: `world/underdark/feature/GiantZurkhwoodFeature`, `NightlightTowerFeature` (config `NightlightTowerConfig`: `min_height`, `max_height`), `ShelfFungusFeature` (Ripplebark clusters on walls), registered as `dndclasses:giant_zurkhwood`, `nightlight_tower` and `shelf_fungus` by `UnderdarkFeatures`; `FungusShapes` holds what they stand on and grow through, and dresses the cap blocks' faces. Their configured and placed features (with the lake life: `darklake_shore` discs, `pearl_clam`, `seagrass_patch`, `waterorb_patch`, `barrelstalk_patch`, `faerie_lichen`) are written by `tools/underdark_worldgen.py`, which also adds the biomes' `additions_sound`.
- Music: `EventMusic.Event.UNDERDARK` (checked before the Overworld-only events), sound event `music.underdark` (`ModSounds.MUSIC_UNDERDARK`); the track `sounds/underdark.ogg` comes from `tools/music-gen` (`python3 tools/music-gen/music_gen.py underdark`). Its log line is `[EventMusic] <previous> -> UNDERDARK`.
- Explorer maps: `world/underdark/UnderdarkMaps.create(world, from, kind)` returns a map or null (the duergar factor's trade calls it). Loot tables `dndclasses:underdark/map_to_{drow_outpost,duergar_hold,rift}` do the same with vanilla's `exploration_map` function, for chests such as the rift landing's (`{"type":"minecraft:loot_table","name":"dndclasses:underdark/map_to_rift"}`). The structure tags `data/dndclasses/tags/worldgen/structure/underdark_maps/*.json` list `dndclasses:drow_outpost`, `dndclasses:duergar_hold`, `dndclasses:rift_landing` and `dndclasses:underdark_rift` as optional entries.
- A world with a data-pack dimension counts as "experimental" in vanilla, which would show the Experimental Settings warning on every load (and stall the DevScript auto-join). `IntegratedServerLoaderMixin` skips the warning when every non-stable registry entry is the mod's own (`ExperimentalWarning.onlyOurs`); another pack's experimental content still warns.
- Existing worlds: the dimension is added to a world made before it when the world loads (checked on the dev world, made before the Underdark existed).
- Lakes: vanilla fills every open cell below `min(-54, sea level)` with lava, which in the Underdark (sea level -222) would make every lake lava. `mixin/NoiseChunkGeneratorFluidMixin` makes the fluid below the sea level the settings' default fluid (water) for any noise settings with a sea level below -54 (only the Underdark's). Chunks generated before that fix keep their lava lakes.
- `GameTime.timeOfDay` returns `GameTime.UNDERDARK_TIME` (18000) in the Underdark.
- Measured on the dev world (`/underdark survey`, every 4 blocks):

  | Centre, radius | Fungal Forest | Mycelial Caverns | Darklake | Faerzress Hollows | Grey Reaches | Water | Median floor | ms per column |
  |-|-|-|-|-|-|-|-|-|
  | 8 8, 256 | 24.7% | 18.1% | 17.9% | 20.6% | 18.7% | 21.4% | -174 | 0.15 |
  | 0 0, 1024 | 16.2% | 21.3% | 20.3% | 23.3% | 18.9% | 22.3% | -180 | 0.17 |
  | 2000 2000, 256 | 19.6% | 31.5% | 19.4% | 16.2% | 13.3% | 18.6% | -178 | 0.18 |
  | -3000 1000, 256 | 25.7% | 15.8% | 25.4% | 16.3% | 16.8% | 23.7% | -182 | 0.22 |
  | 5000 -4000, 256 | 19.3% | 17.5% | 44.6% | 6.5% | 12.1% | 24.8% | -185 | 0.21 |
  | -6000 -6000, 256 | 16.2% | 7.3% | 50.6% | 19.7% | 6.1% | 24.5% | -180 | 0.18 |

  Bedrock: the floor's top at y -252 and the roof's bottom at y -69 everywhere. Over a few hundred blocks the climate can drift, so one region can take up to half a 512-block square (the last two rows); over 2000 blocks each has 16-23%. Sampling the noise costs about 0.15-0.22 ms a column (a 16×16 chunk's columns in about 50 ms), about what the Nether costs.
- Devscripts: `underdark-flora.txt` (the plants and their uses, the giant fungi grown with bone meal and found in the Fungal Forest, the Darklake's lake life and mist, the music event and the maps), `underdark-biomes.txt` (go, every biome located and screenshotted, the survey, back, death, and monsters in a dark room but not a lit one), `underdark-blocks.txt` (see [Underdark blocks](../items/underdark-blocks.md)), `underdark-survey.txt` (the survey at six spots), and `underdark-rejoin-set.txt` then `underdark-rejoin-check.txt` (logging out in the Underdark logs you back in there).
