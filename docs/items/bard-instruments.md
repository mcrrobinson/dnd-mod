# Bard Instruments

The **Lute**, **War Drum** and **Flute** each play a short song. When a Bard plays one, every player nearby gets a buff. Anyone else can play them too, but their song gives no buff.

## How it works
- Right click plays a 4-second song where you stand. It can be heard up to 32 blocks away and uses the **Jukebox/Note Blocks** volume slider. Note particles appear above the player.
- Background music dips while the song plays, for every player in earshot, then comes back, the same way it does for class-special stings.
- **Bard:** every player within **16 blocks**, the Bard included, gets the instrument's buff for **30 seconds**. The action bar names the buff and how many players got it. All three instruments then share a **10-second** cooldown.

  | Instrument | Bard buff |
  |-|-|
  | Lute | Regeneration I |
  | War Drum | Strength I |
  | Flute | Speed I |

- **Other classes:** the song plays with the message "only a Bard's song inspires". It gives no buff, and all three instruments get a 4-second cooldown.
- The buff isn't a potion effect, so Fighters, Paladins and Artificers, who ignore potions, still get it.

## How to get it
They are in the D&D Classes creative tab, or you can craft them:

| Item | Recipe (shaped) |
|-|-|
| Lute | `  T` / `PST` / `PP ` (P = any planks, S = string, T = stick) |
| War Drum | `LLL` / `PSP` / `PPP` (L = leather, P = any planks, S = string) |
| Flute | 3 bamboo in a diagonal |

## Known limitations
- The shared cooldown makes a Bard play one song at a time, but they can keep all three buffs running by switching instruments.
- The item art is a placeholder (16x16).
- No test has been run with a second player yet.

## For developers
- `classes/Items/InstrumentItem.java`: the item logic (radius, duration, cooldowns, Bard check).
- `classes/Music/InstrumentSongs.java`: the `dndclasses:instrument_song` packet that tells clients to duck their music. `Client/Music/MusicStings.duck()` handles it on the client.
- Registration: `ModItems` (`LUTE`, `DRUM`, `FLUTE`), `ModSounds` (`instrument.lute|drum|flute`), `sounds.json`.
- The songs are synthesized with `python3 tools/music-gen/music_gen.py song_lute song_drum song_flute`.
- Devscript: `devscripts/bard-instruments.txt` (Bard lute gives Regeneration, shared cooldown, Fighter flute gives no buff).
