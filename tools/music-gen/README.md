# music-gen

Synthesizes the class-special stings (`sting_<class>.ogg`) and the low-health / Nether Fortress loops into `src/main/resources/assets/dndclasses/sounds/`. Pure Python standard library plus `ffmpeg` (libvorbis); output is seeded, so it's reproducible.

Regenerate everything with `python3 tools/music-gen/music_gen.py`, or only some sounds with e.g. `python3 tools/music-gen/music_gen.py fighter low_health` (takes about 30 s for all).
