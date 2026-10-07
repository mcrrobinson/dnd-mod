# Nether Fortress additions
The mod adds to vanilla Nether Fortresses: goblins, a boss and their own music.

## How it works
- [Goblin Warriors](../mobs/goblins.md) join the fortress spawn pool (weight 6, groups of 1-3).
- A [Goblin Warlord](../bosses/goblin-warlord.md) guards the central bridge crossing of every newly generated fortress.
- A Nether Fortress music loop plays inside a fortress or within 16 blocks of it (32 up or down). See [Music](../music.md).

## Commands
- `/locate structure minecraft:fortress`

## For developers
- `mixin/SpawnHelperMixin`, `mixin/NetherFortressStartMixin`, `Music/DungeonMusic`.
