# Hobbit Villages
Shire-style villages full of food, laid out differently every time. [Hobbits](../mobs/hobbits.md) live here, and every village has an inn with a bounty board.

![A hobbit village](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-flesh-out/hobbit-north.png)

## How it works
Every village grows out from a **party green** in the middle: a big party tree ringed by a path, with long tables of cakes and plates, a striped pavilion, ale and a bonfire. Everything else is dropped at random spots around the green, facing it, and joined to the ring path by a winding lane lit with lamp posts.

| Building or plot | How many |
|-|-|
| The Green Dragon inn | always 1 |
| Smials (hobbit holes) | 4 to 7 |
| Vegetable gardens | 1 or 2 |
| Orchard | 70% chance of 1 |
| Market stalls | 1 or 2 |
| Pond | 60% chance of 1 |

Plots only go on dry, fairly level ground, so a village on hilly land can come out with fewer buildings than it rolled.

**Smials** are dug into grassy hills, with a round door (green, red or yellow), round windows, a chimney and a fenced front garden. Inside you'll find a fireplace, a kitchen and a table laid with food. Bigger smials add a pantry stacked with barrels and a bedroom.

**The Green Dragon** has a thatched roof, a sign out front ("Ales & Suppers"), a bar backed by ale casks and tables of food. The innkeeper stands behind the bar and the bounty board hangs on the wall. See [Hobbit Tavern](hobbit-tavern.md).

### Loot
The barrels and chests are full of food, and none of them are [locked](../systems/d20-skill-checks.md#lockpicking-rogue), so anyone can open them.

| Loot table | Found in | Highlights |
|-|-|-|
| `chests/hobbit_pantry` | smials, the inn, the green, market stalls | bread, pies, stews, cake, golden carrots |
| `chests/hobbit_larder` | smials | cooked meat and fish, cake, golden apples, emeralds, a clock |
| `chests/hobbit_harvest` | gardens, orchards, ponds, market stalls, the green | crops, seeds, eggs, honeycomb, bone meal |
| `chests/hobbit_ale` | the inn, the green | potions, honey bottles, pies, rabbit stew |

The plates and hams in item frames can be taken too.

Taking food is safe: the hobbits don't mind. They're peaceful and even share food with you.

## For your race
Hobbit Villages are the Halfling home (see [Racial homes](../races/racial-homes.md)). A Halfling gets:

- a "Welcome home, Halfling" title on arriving, and a Shire welcome basket (1 cake, 4 cookies, 2 Mugs of Ale) on the first visit to each village
- Regeneration I in the Green Dragon inn while no hostile mob is within 16 blocks
- 25% off the innkeeper's trades
- hobbit gifts every 2 minutes instead of 5 (2/5 of the wait for your reputation tier)
- a Friendly (150) start with the Hobbits of the Shire

## Where to find it
Plains, Sunflower Plains and Meadow. Villages use a spacing of 24 chunks and a separation of 8, and stay at least 6 chunks away from vanilla villages. Hobbits spawn inside the village (1 or 2 at a time).

Goblins sometimes [raid](../systems/goblin-raids.md) hobbit villages.

## Commands
- `/locate structure dndclasses:hobbit_village`

## For developers
- `world/gen/village/`. `HobbitVillageStructure` finds the site, `HobbitVillagePlanner` builds the wish list and places the plots and lanes, and each building is a `*Piece` (`SmialPiece`, `InnPiece`, `VillageGreenPiece` and so on). `HobbitPiece` holds the shared loot ids.
- Data: `worldgen/structure/hobbit_village.json`, `worldgen/structure_set/hobbit_villages.json`, the `#dndclasses:has_structure/hobbit_village` biome tag and `loot_tables/chests/hobbit_*.json`.
- Devscripts: `hobbit-locate.txt`, `hobbit-village-tour.txt`.
