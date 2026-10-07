# Dragon Lairs
The home of a [Lightning Chaser](../mobs/dragons.md), on the very summit of a mountain.

## How it works
- The lair sits on the highest point of a jagged, frozen or stony peak, with the mountain falling away on every side.
- It's a ring of standing stones crowned with lightning rods, round a nest of logs and bones heaped with gold and a hoard chest.
- **Hoard chest (`chests/dragon_lair`):** gold, copper, emeralds, diamonds and enchanted diamond gear, sometimes an enchanted golden apple, a trident or a Staff of Lightning.
- Each lair starts with one Lightning Chaser (sometimes two). It never strays more than about 24 blocks before circling back.
- Once it's dead the lair sends a new one now and then (structure spawn override, never with another Lightning Chaser within 64 blocks).

## Where to find it / How to get it
- Jagged Peaks, Frozen Peaks, Stony Peaks. Structure set spacing 12 chunks, separation 4.

## Commands
- `/locate structure dndclasses:dragon_lair`

## For developers
- `world/gen/lair/` (`DragonLairStructure`, `LairPiece`). Devscripts: `dragon-lair-locate.txt`, `dragon-lair-survey.txt`, `lightning-chaser-lair-lookup.txt`.
