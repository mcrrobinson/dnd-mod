# Dwarven Fortresses
Fortresses carved into mountainsides, home to the [Mountain Dwarves](../mobs/mountain-dwarves.md). Each one is laid out differently, and the treasury behind the throne is one of the richest hoards in the mod.

![The gate of a dwarven fortress on a snowy peak](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-flesh-out/dwarven-fortress.png)

## How it works
You find a fortress by its **great gate**: a tall deepslate facade at the foot of a mountainside, with a gold crest, a gilded doorway and a raised portcullis between gold-capped pillars and banners. It opens onto a terrace with braziers and guards.

Behind the gate everything runs straight back into the rock:

1. **The great hall**, a long pillared hall with chandeliers, banners, feasting tables and a red carpet. Four to six dwarves stand about in it. It has two or three doorways down each side.
2. **The throne room**, where the Dwarf King sits on a gold throne on a stepped dais, with guards.
3. **The treasury**, through a narrow door behind the throne, heaped with gold.

Each side doorway leads down a short corridor to a room. A fortress has four or six of them and always has at least one of each kind:

| Room | What's in it | Loot table |
|-|-|-|
| Forge | lava channels, an anvil, smithing tables, a grindstone | `chests/dwarven_fortress_forge`: iron and diamond tools, netherite scrap, Steampunk armor |
| Barracks | bunks and armor stands | `chests/dwarven_fortress_barracks`: weapons, shields, crossbows, Knight and Warrior armor |
| Mead hall | long tables of food | `chests/dwarven_fortress_brewhall`: cooked food, golden apples and carrots |
| Mine | ore, amethyst and an ore cart | `chests/dwarven_fortress_mine`: raw ores, diamonds, lapis, rails, TNT |

The treasury chests use `chests/dwarven_fortress_treasury`: gold blocks, diamonds, enchanted books, enchanted golden apples, Golden Horns and Knight armor, and a Pigstep disc.

The halls are buried in the mountain. Where the rock is too thin, the fortress piles more on top so you never see a room poking out of a hillside. Dwarves spawn in every room and keep turning up inside, and monsters never spawn there.

### Getting the treasure
The dwarves are neutral until you cross them. If you open or break a chest or barrel, or break a gold block, every dwarf within 16 blocks that can see you turns on you. Dwarves that can't see you stay calm. A few ways to deal with that:

- Loot where no dwarf can see you, or lure them away first.
- The chests are locked. A [Rogue](../classes/rogue.md) can pick them (DC 15 in the treasury, DC 10 elsewhere, see [d20 skill checks](../systems/d20-skill-checks.md#lockpicking-rogue)). Anyone else has to break them and loses some of the loot. A picked lock still angers a dwarf that's watching.
- Help the dwarves drive off a [goblin raid](../systems/goblin-raids.md). Every dwarf of that fortress forgives you.

## Where to find it
Meadow, Grove, Snowy Slopes, Jagged Peaks, Frozen Peaks, Stony Peaks, Windswept Hills, Windswept Gravelly Hills and Windswept Forest. Fortresses use a spacing of 16 chunks and a separation of 6, so they're fairly common in the mountains. The gate needs a steep mountainside with the ground in front clear, so not every spot in those biomes gets one.

## Commands
- `/locate structure dndclasses:dwarven_fortress`

## For developers
- `world/gen/fortress/`: `DwarvenFortressStructure` finds the gate site, `FortressPlanner` lays out the gate, hall, throne room, treasury and side rooms (one `*Piece` each), and `CladdingPiece` heaps rock over anything the mountain doesn't cover. `FortressPiece` holds the shared loot ids.
- Data: `worldgen/structure/dwarven_fortress.json` (dwarf spawn override, empty monster list), `worldgen/structure_set/dwarven_fortresses.json`, the `#dndclasses:has_structure/dwarven_fortress` biome tag and `loot_tables/chests/dwarven_fortress_*.json`.
- Grudges: `entity/DwarfGrudges.java` and `MountainDwarfEntity.witness`.
- Devscripts: `dwarven-fortress-locate.txt`, `dwarven-fortress-survey.txt`.
