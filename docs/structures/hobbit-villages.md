# Hobbit Villages
Cozy, Shire-style villages full of food, laid out differently every time. [Hobbits](../mobs/hobbits.md) live here.

## How it works
- **Smials** (hobbit holes) dug into grassy hills, with round green (or red, or yellow) doors, round windows, chimneys and fenced front gardens. Inside: a fireplace, a kitchen and a table laid with food. The bigger ones add a pantry stacked with barrels and a bedroom.
- **The Green Dragon** inn (every village has one) has a thatched roof, a bar backed by ale casks, and tables of food. Behind the bar stands the innkeeper, and a bounty board hangs on the wall (see [Hobbit Tavern](hobbit-tavern.md)).
- **The party green** round the party tree has long tables of cakes and plates, a striped pavilion, ale and a bonfire.
- **Gardens, orchards, market stalls and ponds**, joined by winding lanes with lamp posts.
- **Loot:** barrels and chests are full of food (`chests/hobbit_pantry`, `hobbit_larder`, `hobbit_harvest`, `hobbit_ale`). The plates and hams in item frames can be taken.

## Where to find it / How to get it
- Plains, Sunflower Plains, Meadow. Spacing 24 chunks, separation 8, kept at least 6 chunks from vanilla villages.

## Commands
- `/locate structure dndclasses:hobbit_village`

## For developers
- `world/gen/village/` (`HobbitVillagePlanner` lays out the pieces). Devscripts: `hobbit-locate.txt`, `hobbit-village-tour.txt`.
