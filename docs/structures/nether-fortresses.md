# Nether Fortress additions
The mod doesn't add a new Nether structure. It changes vanilla Nether Fortresses: goblins move in, a boss guards the central bridge and the fortress gets its own music.

## How it works
[Goblin Warriors](../mobs/goblins.md) join the fortress spawn pool alongside blazes, wither skeletons and the rest, with weight 6 in groups of 1 to 3. Expect them anywhere in the fortress.

Every newly generated fortress gets one [Goblin Warlord](../bosses/goblin-warlord.md), standing in the middle of the bridge crossing that the fortress grows from. That crossing is a big four-way bridge junction. The Warlord is a boss with its own boss bar, so clear the warriors around it before you pull it, and don't fight it near an edge.

A Nether Fortress music loop plays while you're inside a fortress or within 16 blocks of it horizontally and 32 blocks up or down. See [Music](../music.md).

## Where to find it
Anywhere vanilla puts a Nether Fortress. Fortresses generated before you installed the mod don't get a Warlord, but goblins can still spawn in them because the spawn pool is checked live.

## Commands
- `/locate structure minecraft:fortress`

## Known limitations
- Only one Warlord per fortress, placed at generation. Once it's dead, it doesn't come back.

## For developers
- `mixin/SpawnHelperMixin` adds the Goblin Warrior entry to the fortress spawn pool.
- `mixin/NetherFortressStartMixin` places the Warlord in `NetherFortressGenerator.BridgeCrossing`, the way ocean monuments place their elder guardians.
- `Music/DungeonMusic` sends the `music_near_fortress` packet that switches the music, checking every 40 ticks.
