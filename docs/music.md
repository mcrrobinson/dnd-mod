# Music
Custom tracks that play during game events, five music discs, and a short sting when you use your class special.

## How it works
Event music replaces the vanilla pick, in this priority order:

| Event | Track | When |
|-|-|-|
| Boss fight | **Tooth and Claw** | While you can see a boss bar with a fight track (dragons, Magmamuncher Alpha). It loops until the fight ends, then 20 seconds of quiet |
| Low health | Low health loop | Below 25% health while in combat (hurt in the last 10 s or a monster within 12 blocks). It stops above 40% health or after 30 s out of combat |
| Dungeon | **Silent Footsteps** | Within 16 blocks of a mob spawner, or inside a stronghold, mineshaft, ancient city, bastion, mansion, ocean monument or desert/jungle temple |
| Nether Fortress | Nether Fortress loop | Inside a Nether Fortress or within 16 blocks of it (32 up or down) |
| Travelling | **Awake Cart**, mixed with vanilla music | After covering 80 blocks in 30 seconds, until you slow below 30 blocks per 30 seconds |
| Night | **Silent Footsteps**, mixed with vanilla music | At night |

- Vanilla's underwater, menu, credits and End music still win, except over a boss fight.
- **Class stings:** when your special fires, a short (up to 4 s) sting for your class plays and the background music dips to 30% under it.

**Music discs:** Steel on Steel, Awake Cart, Tooth and Claw, Silent Footsteps, Music Box.

## Where to find it / How to get it
- Discs are in the mod's creative tab (Music Box is only via `/give @s dndclasses:music_box_music_disc`).

## Configuration
- Everything uses the Music volume slider, stings included. Discs use Jukebox/Note Blocks.

## For developers
- Event selection: `Client/Music/EventMusic` (hooked through `mixin/MinecraftClientMusicMixin`). Stings and ducking: `Client/Music/MusicStings` + `mixin/SoundSystemMusicDuckMixin`. Server-side dungeon/fortress detection: `Music/DungeonMusic` (tags `dndclasses:music_dungeons`, `music_nether_fortresses`). Boss tracks: `entity/boss/BossMusic`.
- Sounds: `assets/dndclasses/sounds.json`, `Registry/ModSounds`. Devscripts: `event-music.txt`, `music-events.txt`.
