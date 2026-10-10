# Dragon Lairs
The home of a [Lightning Chaser](../mobs/dragons.md), on the very summit of a mountain.

![A dragon lair on a snowy summit: standing stones with lightning rods round the nest, and its Lightning Chaser flying below](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/dragon-lair.png)

## How it works
- The lair sits on the highest point of a jagged, frozen or stony peak, with the mountain falling away on every side.
- It's a ring of standing stones crowned with lightning rods, round a nest of logs and bones heaped with gold and a hoard chest.
- **Hoard chest (`chests/dragon_lair`):** gold, copper, emeralds, diamonds and enchanted diamond gear, sometimes an enchanted golden apple, a trident or a Staff of Lightning.
- Each lair starts with one Lightning Chaser (sometimes two). It never strays more than about 24 blocks before circling back.
- Once it's killed, the lair waits **3 in-game days** and then sends a new one (structure spawn override; never in peaceful or with another Lightning Chaser within 64 blocks). A chaser that left without dying (e.g. in peaceful) is replaced as soon as possible.

## Where to find it / How to get it
- Jagged Peaks, Frozen Peaks, Stony Peaks. Structure set spacing 12 chunks, separation 4, and at least 6 chunks from dwarven fortresses (which share the peaks).

## Commands
- `/locate structure dndclasses:dragon_lair`

## For developers
- `world/gen/lair/` (`DragonLairStructure`, `LairPiece`, `LairRespawns` for the per-lair respawn timer). Devscripts: `dragon-lair-locate.txt`, `dragon-lair-survey.txt`, `lightning-chaser-lair-lookup.txt`.
