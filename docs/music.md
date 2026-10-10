# Music
Custom tracks that play during game events, five music discs, and a short sting when you use your class special.

![A jukebox playing the Steel on Steel disc, with the Awake Cart, Tooth and Claw, Silent Footsteps and Music Box discs in item frames behind it](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/music-jukebox.png)

## How it works
### Event music
The mod picks the background music from what you're doing, in this order. The first event that applies wins.

| Event | Track | When |
|-|-|-|
| Boss fight | **Tooth and Claw** | While you can see the boss bar of a Wyvern, Lightning Chaser, Magmamuncher Alpha or Beholder. It loops until the fight ends, then 20 seconds of quiet |
| Lich fight | Lich theme | The same, during a fight with the [Lich](bosses/lich.md) |
| Goblin raid | **Steel on Steel** | Within 96 blocks of a goblin raid, until it's won or lost (see [Goblin raids](systems/goblin-raids.md)) |
| Low health | Low health loop | Below 25% health while in combat, meaning you were hurt in the last 10 seconds or a monster is within 12 blocks. It stops above 40% health or after 30 seconds out of combat |
| Dungeon | **Silent Footsteps** | Within 16 blocks of a mob spawner, or inside a stronghold, mineshaft, ancient city, bastion, woodland mansion, ocean monument, desert or jungle temple, or [Beholder lair](bosses/beholder.md) |
| Nether Fortress | Nether Fortress loop | Inside a Nether Fortress or within 16 blocks of it (32 up or down) |
| Travelling | **Awake Cart**, mixed with vanilla music | After you cover 80 blocks in 30 seconds, until you slow to under 30 blocks per 30 seconds. Teleports don't count |
| Night | **Silent Footsteps**, mixed with vanilla music | From dusk (time 13000) to dawn (23000) |

Vanilla's underwater, menu, credits and End music still take priority over everything except a boss fight. The [Goblin Warlord](bosses/goblin-warlord.md) has a boss bar but no fight track.

### Class stings
When your special fires, a short sting for your class plays (4 seconds at most) and the background music dips to 30% under it, then fades back over about a second. [Bard instrument](items/bard-instruments.md) songs duck the music the same way for everyone in earshot.

### Music discs
| Disc | Length | How to get it |
|-|-|-|
| Steel on Steel | 1:38 | 15% chance in the reward for beating a goblin raid, or creative tab |
| Awake Cart | 1:28 | Creative tab |
| Tooth and Claw | 1:45 | Creative tab |
| Silent Footsteps | 2:50 | Creative tab |
| Music Box | 0:16 | `/give @s dndclasses:music_box_music_disc` only |

All five play in a jukebox and give a comparator signal of 6.

## Configuration
Event music and stings follow the **Music** volume slider. Discs and instrument songs follow **Jukebox/Note Blocks**. Turn Music down to 0 to switch event music off.

## Known limitations
- Only Steel on Steel has a survival source.
- The Lich theme has no disc.

## For developers
- Event selection: `Client/Music/EventMusic`, hooked through `mixin/MinecraftClientMusicMixin`. Stings and ducking: `Client/Music/MusicStings` and `mixin/SoundSystemMusicDuckMixin`.
- Server-side dungeon and fortress detection: `Music/DungeonMusic` (structure tags `dndclasses:music_dungeons` and `music_nether_fortresses`, checked every 40 ticks).
- Boss tracks: `entity/boss/BossMusic` sends the track for a boss bar. A boss gets one through `BossFight.music(...)`; goblin raids send `MUSIC_GOBLIN_RAID` directly.
- Sounds: `assets/dndclasses/sounds.json`, `Registry/ModSounds`. Discs: `Registry/ModItems` and the `#minecraft:music_discs` tag.
- The tracks are made with `tools/music-gen/` (see its README).
- Devscripts: `event-music.txt`, `music-events.txt`.
