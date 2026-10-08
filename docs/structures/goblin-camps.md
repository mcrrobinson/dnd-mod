# Goblin Camps
Goblin war camps in Overworld forests and plains: a levelled clearing inside a crude palisade, with hide tents round a campfire, a loot chest in the chief's tent, and a garrison of Goblin Warriors. Some camps are led by a Goblin Warlord.

## How it works
- **Clearing**: about 29 blocks across (levelled and cleared out to radius 14, up to 16 blocks high, trees included). Dips are filled with up to 8 blocks of dirt.
- **Palisade** at radius 11: spruce, stripped spruce and oak logs 2-3 high, with stretches of fence, stakes on top, the odd torch and roughly 10% gaps where it's fallen. The gate is on a random side, flanked by 4-high posts topped with skulls, with a 3-wide dirt track leading out.
- **Campfire**: lit, in a 3x3 cobblestone/mossy cobblestone pit, with up to four log seats round it.
- **Tents**: three wool A-frames (7 wide, 5 deep, 4 high; brown, green, grey, lime or black, with patches), one on every side except the gate's, opening towards the fire. Each has a straw bed.
  - The **chief's tent**, opposite the gate, has a red rug, a gold block and the loot chest (`chests/goblin_camp`: iron, gold nuggets and ingots, emeralds, bones, rotten flesh, arrows, leather, cooked mutton, sometimes a saddle, crossbow, golden apple or enchanted iron sword or axe, plus a chance of 1-2 diamonds, a name tag or iron horse armour).
  - The other tents have a 60% chance of a barrel of supplies (`chests/goblin_camp_supplies`: crops, bread, rotten flesh, bones, arrows, string, sticks, coal, leather).
- **Corners**: each gets one of trophy poles with skulls (sometimes a carved pumpkin), a woodpile with a torch, a bone heap with a skull and cobweb, or a crude workshop (crafting table, cauldron, supplies barrel).
- **Garrison**: 3-5 Goblin Warriors, plus a 30% chance of a Goblin Warlord standing in front of the chief's tent with its boss bar and warcry. They never despawn and stay within about 10 blocks of the fire (the Warlord within 12) unless they're chasing something.

## Where to find it
Forest, flower forest, birch forest, old growth birch forest, dark forest, plains and sunflower plains, on dry, fairly level ground. Camps are spread about 28 chunks apart (at least 10), and kept at least 4 chunks from vanilla villages and 8 chunks from where a hobbit village could start (`GoblinCampStructure.nearHobbitVillage`, since a structure set can only exclude one other set).

## Commands
- `/locate structure dndclasses:goblin_camp`
- `/place structure dndclasses:goblin_camp` (only works in an allowed biome)

## Known limitations
- No respawning: the camp is lit, so neither goblins nor vanilla monsters spawn in it later. The starting garrison is the whole population.
- Trees cut at the edge of the clearing can leave a few floating leaves, and a tree grown later by a neighbouring chunk can occasionally reach into the camp (hobbit villages have the same issue).
- Goblin Warriors are strong (50 HP, 9 damage), so a camp is a hard fight early in the game.

## For developers
- `src/main/java/mattonfire/dnd/world/gen/camp/`: `GoblinCampStructures` (registers the structure type and piece), `GoblinCampStructure` (site check), `GoblinCampPiece` (builds the camp from its own seed, in world coordinates, and spawns the goblins).
- Data: `data/dndclasses/worldgen/structure/goblin_camp.json`, `worldgen/structure_set/goblin_camps.json`, `tags/worldgen/biome/has_structure/goblin_camp.json`, `loot_tables/chests/goblin_camp.json` and `goblin_camp_supplies.json`.
- Devscripts: `devscripts/goblin-camp-locate.txt` (finds the nearest camp), `devscripts/goblin-camp-tour.txt` (screenshots the dev world's camp at -408, 1096 and counts the goblins).
