# Nether Fortress additions
The mod doesn't add a new Nether structure. It changes vanilla Nether Fortresses: goblins move in, a boss guards the central bridge and the fortress gets its own music.

![The Goblin Warlord and Goblin Warriors on a Nether Fortress bridge](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/nether-fortress-warlord-goblins.png)

## How it works
[Goblin Warriors](../mobs/goblins.md) join the fortress spawn pool alongside blazes, wither skeletons and the rest, with weight 6 in groups of 1 to 3. Expect them anywhere in the fortress.

Every newly generated fortress gets one [Goblin Warlord](../bosses/goblin-warlord.md), standing in the middle of the bridge crossing that the fortress grows from. That crossing is a big four-way bridge junction. The Warlord is a boss with its own boss bar, so clear the warriors around it before you pull it, and don't fight it near an edge.

In one corner of the same crossing, a small nether-brick vault hangs off the bridge. Its doorway, through the bridge railing, is a 2x2x2 **Fire Curtain** (Hard) of soul fire. A Warlock walks straight through it, and so does anyone under Fire Resistance; inside, right-clicking the brazier puts the curtain out for everyone. The chest inside uses `chests/nether_fortress_cache`: blaze rods, gold ingots, nether wart, magma cream, potions of Fire Resistance (8:00), and the odd ghast tear, gilded blackstone or diamond. See [Class-gated obstacles](../systems/obstacles.md).

A Nether Fortress music loop plays while you're inside a fortress or within 16 blocks of it horizontally and 32 blocks up or down. See [Music](../music.md).

## Where to find it
Anywhere vanilla puts a Nether Fortress. Fortresses generated before you installed the mod don't get a Warlord, but goblins can still spawn in them because the spawn pool is checked live.

## Commands
- `/locate structure minecraft:fortress`

## Known limitations
- Only one Warlord per fortress, placed at generation. Once it's dead, it doesn't come back.
- Only fortresses generated after the vault was added have one.

## For developers
- `mixin/SpawnHelperMixin` adds the Goblin Warrior entry to the fortress spawn pool.
- `mixin/NetherFortressStartMixin` places the Warlord in `NetherFortressGenerator.BridgeCrossing`, the way ocean monuments place their elder guardians, and builds the curtain vault (`dnd$placeCache`) in the crossing's empty corner, x and z 1..6 in the piece's local coordinates.
- `Music/DungeonMusic` sends the `music_near_fortress` packet that switches the music, checking every 40 ticks.
