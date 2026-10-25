# The Underdark
A sunless world of huge caverns under the Overworld's bedrock, from y -256 to -65, lit only by what grows there. It's its own dimension (`dndclasses:underdark`) at the same x and z as the Overworld above, with five regions: the Fungal Forest, the Mycelial Caverns, the Darklake, the Faerzress Hollows and the Grey Reaches. For now you get there with `/underdark go`; the Underdark Rifts that take players down without commands, the giant fungi, the drow and duergar and their settlements come with the later Underdark tickets.

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
| **Fungal Forest** | blue-grey fog; zurkhwood mushrooms with flat violet caps and glowing Nightlight caps over spongy Fungal Loam | Fungal Loam / deepslate | zurkhwood ×6 and nightlights ×3 per chunk (small stand-ins until the giant fungi come), Bluecap patches ×4, Trillimac patches ×2, glow lichen ×40 | drifting blue spores |
| **Mycelial Caverns** | dim violet fog; red and brown giant mushrooms on mycelium | mycelium over dirt / deepslate | huge red and brown mushrooms ×3 each, small mushrooms ×3 each, Trillimac ×3, glow lichen ×20 | mycelium spores |
| **Darklake** | black-blue fog and water; the lowest caverns, flooded | gravel, clay below the waterline (+2) / deepslate | extra lake pools (clay-lined), water springs ×20, ceiling moss ×24, clay pools with dripleaves ×16, glow lichen ×60; glow squid and axolotls | dripping water |
| **Faerzress Hollows** | violet fog; walls veined with glowing faerzress | deepslate with Faerzress Stone in about a third of it / deepslate | faerzress veins ×8 (size 33), amethyst geodes (1 chunk in 12), Bluecaps ×2, glow lichen ×120 | portal motes |
| **Grey Reaches** | ash-grey fog; barren tuff and basalt, dripstone and lava | tuff, basalt in patches / tuff | magma patches ×6, lava springs ×20, large dripstone ×12, dripstone clusters ×8 | white ash |

Every biome spawns spiders (weight 100, 1-3), cave spiders (40), zombies (60), skeletons (60), creepers (20), endermen (4) and witches (4), and bats. Mod mobs come with later tickets, through the habitat biome tags (see For developers).

### Night all the time
The Underdark's clock is fixed at midnight. The mod's night checks (`GameTime.isNight`) read 18000 there whatever the Overworld's time, so a [Blood Hunter](../classes/blood-hunter.md) has night strength the whole time they're below. Vanilla's own day checks see a fixed-time dimension as neither day nor night, like the Nether.

### Getting there and back
- `/underdark go` takes you down at your x and z. It looks for a dry, solid floor with two blocks of air above it between y -75 and -250, in your column and then in rings out to 8 blocks. It prefers a floor in an open cavern (6 blocks of air above), then the one nearest y -90; a cramped floor in your own column is the fallback. If there's no floor at all, it carves a 5×5×3 pocket at y -150 with a deepslate-tile floor and a lantern, sealing any water or lava round it.
- It remembers where you came from, and `/underdark back` returns you there (or to the world spawn if nothing is remembered).
- Dying in the Underdark respawns you at your bed or the world spawn, as usual. Logging out there logs you back in there.
- The Underdark is a separate world: you can't dig down into it from the Overworld. Pets stay behind, as with the Nether.

## Where to find it
For now only through `/underdark go` and `/underdark biome` (ops), or `/execute in dndclasses:underdark run tp @s x y z`. The Underdark Rifts ticket adds sinkholes in the Overworld that drop players in.

## Commands
`/underdark` needs permission level 2.

| Command | What it does |
|-|-|
| `/underdark go [player]` | Down into the Underdark at the same x and z, on a found floor or a carved pocket; remembers where you were |
| `/underdark back [player]` | Back to the remembered spot, or to the world spawn |
| `/underdark biome <biome>` | Teleports you to the nearest Underdark biome of that kind (`fungal_forest`, `mycelial_caverns`, `darklake`, `faerzress_hollows`, `grey_reaches`), up to 48 blocks past its near edge, on a safe floor, facing the most open way |
| `/underdark survey [radius]` | Samples the noise every 4 blocks round you (radius 128 by default) and prints each biome's share, floors per column, the median floor y, the share of floors under water, the bedrock at the floor and the roof of the chunk you're over, the cost in ms per column, and the climate noises' mean and spread (logged as `[Underdark] survey ...`) |

## Known limitations
- The zurkhwood and nightlight are vanilla huge-mushroom shapes (4-7 tall) until the flora ticket brings the giant fungi.
- No way in or out without commands until the Underdark Rifts ticket.
- No Underdark music yet: the mod's event music only plays in the Overworld.

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
- `UnderdarkLanding.findOrMake(world, x, z)`: the floor search and carved pocket. `UnderdarkTravel`: `descend`, `sendTo`, `back` and `teleport`, with the return spot in the player's persistent data under `dndUnderdark.Return`. `UnderdarkTravel.AFTER_TRAVEL` (player, from, to) fires after every trip in or out, so followers such as hirelings can come along. Log lines: `[Underdark] <player> descends at x y z (found|carved)` and `[Underdark] <player> returns to ...`.
- `UnderdarkSurvey` backs `/underdark survey`.
- Client: `client/UnderdarkEffects` (no sky or clouds, biome fog unchanged, darkened lightmap), registered for the dimension type's `effects` id `dndclasses:underdark`.
- A world with a data-pack dimension counts as "experimental" in vanilla, which would show the Experimental Settings warning on every load (and stall the DevScript auto-join). `IntegratedServerLoaderMixin` skips the warning when every non-stable registry entry is the mod's own (`ExperimentalWarning.onlyOurs`); another pack's experimental content still warns.
- Existing worlds: the dimension is added to a world made before it when the world loads (checked on the dev world, made before the Underdark existed).
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
- Devscripts: `underdark-biomes.txt` (go, every biome located and screenshotted, the survey, back, death, and monsters in a dark room but not a lit one), `underdark-blocks.txt` (see [Underdark blocks](../items/underdark-blocks.md)), `underdark-survey.txt` (the survey at six spots), and `underdark-rejoin-set.txt` then `underdark-rejoin-check.txt` (logging out in the Underdark logs you back in there).
