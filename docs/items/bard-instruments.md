# Bard Instruments
The Lute, War Drum and Flute each play a short song. When a [Bard](../classes/bard.md) plays one, every player nearby gets a buff. Anyone can play them, but only a Bard's song gives a buff.

![A Bard's hotbar with the lute, drum and flute](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-flesh-out/hud-bard.png)

## How it works
Right click to play a 4 second song where you stand. Note particles rise above you, and anyone within 32 blocks hears it. The song plays on the **Jukebox/Note Blocks** volume slider, so turn that up if you can't hear it. Background music dips for everyone in earshot while it plays, then comes back, the same as for class special stings.

**As a Bard**, every player within 16 blocks, you included, gets the instrument's buff for 30 seconds. Green sparkles show who got it, and the action bar says "Your song grants *buff* to *n* player(s)". All three instruments then go on a shared 10 second cooldown.

| Instrument | Buff for 30 s |
|-|-|
| Lute | Regeneration I |
| War Drum | Strength I |
| Flute | Speed I |

**As any other class**, the song plays and the action bar says "A pleasant tune, but only a Bard's song inspires". There's no buff, and all three instruments get a 4 second cooldown.

The buff doesn't come from a potion, so Fighters, Paladins and Artificers, who ignore potions, still get it.

### Tips
- The cooldown is shared, but it's shorter than the buff. Play the Lute, Drum and Flute one after another 10 seconds apart and your party keeps all three buffs running.
- Keep all three in your hotbar so you can switch quickly.

## How to get it
Craft them, or take them from the D&D Classes creative tab.

**Lute:** two sticks, three planks and a string.

| | | |
|-|-|-|
| | | Stick |
| Planks | String | Stick |
| Planks | Planks | |

**War Drum:** three leather, five planks and a string.

| | | |
|-|-|-|
| Leather | Leather | Leather |
| Planks | String | Planks |
| Planks | Planks | Planks |

**Flute:** three bamboo in a diagonal line, from top right to bottom left.

| | | |
|-|-|-|
| | | Bamboo |
| | Bamboo | |
| Bamboo | | |

Any kind of planks works.

## Known limitations
- The item art is a 16x16 placeholder.
- Only the Bard check has been tested in a script. Buffing a second player hasn't been tested with two clients yet.

## For developers
- `classes/Items/InstrumentItem.java`: the item logic (`BUFF_RADIUS`, `BUFF_TICKS`, the cooldowns, the Bard check).
- `classes/Music/InstrumentSongs.java`: `SONG_TICKS`, `HEARING_RANGE` and the `dndclasses:instrument_song` packet that tells clients to duck their music. `Client/Music/MusicStings.duck()` handles it on the client.
- Registration: `ModItems` (`LUTE`, `DRUM`, `FLUTE`), `ModSounds` (`instrument.lute|drum|flute`), `sounds.json`. Recipes: `data/dndclasses/recipes/lute.json`, `drum.json`, `flute.json`.
- The songs are synthesized with `python3 tools/music-gen/music_gen.py song_lute song_drum song_flute`.
- Devscript: `devscripts/bard-instruments.txt` (Bard lute gives Regeneration, shared cooldown, Fighter flute gives no buff).
