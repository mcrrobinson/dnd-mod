# Dwarven Fortresses
Luxurious fortresses carved into mountainsides, home to the [Mountain Dwarves](../mobs/mountain-dwarves.md). Each one is laid out differently.

## How it works
- **The great gate:** a towering deepslate facade with a gold crest, gilded doorway and raised portcullis, flanked by gold-capped pillars and banners. It opens onto a terrace with braziers and guards.
- **The great hall:** a long pillared hall with chandeliers, banners, feasting tables and a red carpet, with two or three doorways down each side.
- **The throne room**, where the Dwarf King sits on a gold throne on a stepped dais. Behind the throne is the **treasury**, heaped with gold.
- **Side rooms:** forges with lava channels, barracks with bunks and armor stands, mead halls with long tables of food, and working mines with ore, amethyst and an ore cart. Every fortress has a forge and barracks.
- The halls are buried in the mountain. Where the rock is too thin, the fortress piles more on top.
- Dwarves live in every room and keep turning up inside. Monsters never spawn there.
- **Loot:** `chests/dwarven_fortress_treasury`, `_forge`, `_barracks`, `_brewhall` and `_mine`. Opening chests in sight of a dwarf angers them.

## Where to find it / How to get it
- Meadow, Grove, Snowy Slopes, Jagged/Frozen/Stony Peaks, Windswept Hills/Gravelly Hills/Forest. Spacing 16 chunks, separation 6.

## Commands
- `/locate structure dndclasses:dwarven_fortress`

## For developers
- `world/gen/fortress/` (`FortressPlanner`, one `*Piece` per room, `CladdingPiece` for the extra rock). Devscripts: `dwarven-fortress-locate.txt`, `dwarven-fortress-survey.txt`.
