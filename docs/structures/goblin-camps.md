# Goblin Camps
Goblin war camps in Overworld forests and plains: a levelled clearing inside a crude palisade, with hide and canvas tents round a campfire, a loot chest in the chief's tent, and a garrison of Goblin Warriors. Some camps are led by a Goblin Warlord.

![A goblin camp from the air: palisade, three tents and the campfire in a forest clearing](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/goblin-camp.png)

## How it works
- **Clearing**: about 29 blocks across (levelled and cleared out to radius 14, up to 16 blocks high, trees included). Dips are filled with up to 8 blocks of dirt.
- **Palisade** at radius 11: spruce, stripped spruce and oak logs 2-3 high, with stretches of Rope-and-Bone Fence, rope-and-bone stakes on top, the odd torch and roughly 10% gaps where it's fallen. About 1 post in 5 is spruce with a Glyph-daubed Spruce Log in the middle. The gate is on a random side, flanked by 4-high posts (the middle log daubed with a war mark) topped with skulls and hung outside with black banners bearing the lime [Goblin Skull](../items/banner-patterns.md), with a 3-wide dirt track leading out.
- **Campfire**: lit, in a 3x3 cobblestone/mossy cobblestone pit, with up to four log seats round it.
- **Tents**: three, one on every side except the gate's, their doors facing the fire. Each is a real tent: a spruce ridge pole capped with cloth, a steep roof of stairs that sags into slabs out to a low side wall, a back wall and a front with a 3-wide, 2-high doorway whose flaps (open Stretched Hide or Tent Canvas Flaps) are tied back to the sides. The ridge pole pokes out of the back, an upright pole holds up its front end, and Tent Stakes either side and behind run guy ropes up to the walls. The chief's is of Stretched Hide; the others are Stretched Hide or Tent Canvas (2 in 5 canvas), some with patches of Patchwork Hide on their walls. Each has a brown [Bedroll](../items/adventuring-blocks.md#bedrolls) at the back (a straw bed in camps generated before it).

| Tent | Size (wide x deep, ridge height) | Shape |
|-|-|-|
| Chief's | 9 x 5, ridge 4.5 high, plus a 1-deep porch | ridge, two rows of stairs, a sagging slab, a 1-high wall with a slab eave; the porch carries the roof out over the door on two corner poles, and its ridge pole pokes out past the porch |
| A-frame | 7 x 4 or 5, ridge 3.5 high | ridge, one row of stairs, a sagging slab, a 1-high wall with a slab eave |
| Wall tent | 7 x 4 or 5, ridge 4.5 high | ridge, two rows of stairs on 2-high walls: roomier inside |

  The tents were blocky hide boxes before; camps generated then keep them, and a camp half-generated across the change can have a tent that's half old, half new at a chunk border. The rest of the camp still lines up.
  - The **chief's tent**, opposite the gate, has a floor of Scrap-lashed Planks, a red rug, a lantern hung from the ridge pole, a gold block, the War Order Board on the back wall and, under it, the loot chest (`chests/goblin_camp`: iron, gold nuggets and ingots, emeralds, bones, rotten flesh, arrows, leather, cooked mutton, sometimes a saddle, crossbow, golden apple or enchanted iron sword or axe, plus a chance of 1-2 diamonds, a name tag or iron horse armour). It also has a 25% chance of an Uncommon magic item, 3% of a Wand of Magic Missiles and 1% of The Burglar's Ring (see [Magic loot](../systems/magic-loot.md)).
  - Opening (or smashing) the chief's chest sets the whole camp on you, whatever your standing with the goblins: every goblin within 24 blocks comes for you, seen or not, and keeps at it while it fights you and for 30 s after ([Factions](../systems/factions.md)).
  - The other tents have a 60% chance of a barrel of supplies (`chests/goblin_camp_supplies`: crops, bread, rotten flesh, bones, arrows, string, sticks, coal, leather), and a 10% chance of an Uncommon magic item.
- **Corners**: each gets one of trophy poles with skulls (sometimes a carved pumpkin), a woodpile with a torch, a hoard of Goblin Junk Piles round a bone block with a skull and cobweb, or a crude workshop (crafting table, cauldron, supplies barrel). Every corner but the hoard also has a junk pile at its edge.
- **Guard wolves**: two wolves are chained to a Guardian Beast post just in front of the chief's tent, beside its ridge pole. They growl at anyone within 7 blocks and attack anyone within 3 of the post. A Druid (Animal Handling) or a Bard (Performance) calms them for 10 minutes with a DC 10 check (Easy); anyone else fights them, and once both are dead the post stays open. It's the camp's one critical obstacle, so the fight is always a way past ([Class-gated obstacles](../systems/obstacles.md)).
- **Garrison**: 3-5 Goblin Warriors, plus a 30% chance of a Goblin Warlord standing in front of the chief's tent with its boss bar and warcry. A camp with a Warlord flies his standard from the end of the chief's tent's ridge pole, over the porch: a black Goblin Skull banner with a red border. They never despawn and stay within about 10 blocks of the fire (the Warlord within 12) unless they're chasing something, walking back after a fight. Each one saves its camp's fire (`CampCenter`, the Warlord `GuardPos`), so the leash survives chunk reloads and restarts.
  - Camps generated before `CampCenter` existed lost the leash on their first reload. Their warriors pick it up again the first time they load: a persistent Goblin Warrior with no leash (not a raider, Warlord or boss minion) joins the camp whose fire is within 64 blocks, if its chunks are loaded.

- **Building blocks**: the camp's own blocks, a messier, cheaper copy of the orcs' (all in the DnD Building Blocks creative tab; see [Building blocks](../dev/building-blocks.md)):

| Block | Looks like | Notes |
|-|-|-|
| Patchwork Hide, Patchwork Hide Carpet | brown, dirty green and grey hide and cloth scraps sewn together with big stitches, one ripped sackcloth patch | wool: shears mine it fast, it burns, it blocks vibrations. Craft brown, green and grey wool and a leather into 4; 2 hide make 3 carpet |
| Scrap-lashed Planks, Stairs, Slab | boards of different woods nailed together at angles, rusty nails, a diagonal brace and a rope lashing | a wood family (axe). Spruce planks, 2 oak planks and string make 3 |
| Goblin Junk Pile | a low heap (12 px) of broken blades, a cracked pot, bones and a rusty helmet | shovel. Mining it drops 1-2 rolls of `gameplay/goblin_junk`: 1-3 iron nuggets (30), 1-2 bones (30), 1-2 string (25), rotten flesh (10) or an emerald (3); with silk touch, the pile itself. No recipe |
| Crude Glyph Daub | cobblestone smeared with a goblin war mark: a red skull, a black fist, a yellow arrow or a red stick-figure Warlord, picked by position | pickaxe. Cobblestone and red dye make 1 |
| Glyph-daubed Spruce Log | the same marks on spruce bark (the fist in yellow) | a log (axe, any axis). A spruce log and red dye make 1 |
| Rope-and-Bone Fence | a spruce post with a bone lashed on and a small skull on top, rope rails | a wooden fence: joins up with other wooden fences. 4 spruce fences, a bone and string make 4 |
| Stretched Hide, Stairs, Slab, Flap | one big tan rawhide stretched taut, faintly mottled, with a laced seam down one edge; the Flap (a trapdoor) adds a laced hem, a thong tie and ragged corners | the tents' walls and roofs. Axe; wool's hardness and sound, it burns. 3 leather and a stick make 2. The flap opens by hand with a leathery rustle |
| Tent Canvas, Stairs, Slab, Flap | grubby, sun-bleached canvas with a fine weave, soft stains and a sewn seam; the Flap adds a stitched hem, a rope tie and a frayed corner | the same, for the canvas tents. 2 white wool and 2 string make 2 |
| Tent Stake | a short spruce stake with a guy rope running up from it to each block beside it with a solid side (a tent wall), like a fence joins up | axe. Only the stake has collision. String over a stick makes 2 |
| War Order Board | a crude plank board with scraps of paper pinned by daggers, hung on a wall | axe. Right-click reads today's orders in chat: one of 8, picked by the board's position and the day, so each board shows its own and they change every morning. 3 spruce planks, 2 paper and an iron nugget make 1 |

![The camp gate: skull-topped posts, the dirt track and the chief's tent across the fire](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/goblin-camp-gate.png)

![The gate posts hung with Goblin Skull banners](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/racial-banners/camp-gate-after-v2.png)
![A Warlord's standard on the chief's tent: a black Goblin Skull banner with a red border](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/racial-banners/warlord-standard-v2.png)

## Where to find it
Mine the building blocks out of a camp (each drops itself, except the junk pile's junk) or craft them. Goblins don't trade, so no merchant sells them.

Camps generate in forest, flower forest, birch forest, old growth birch forest, dark forest, plains and sunflower plains, on dry, fairly level ground. Camps are spread about 28 chunks apart (at least 10), and kept at least 4 chunks from vanilla villages and 8 chunks from where a hobbit village could start (`GoblinCampStructure.nearHobbitVillage`, since a structure set can only exclude one other set).

## Commands
- `/locate structure dndclasses:goblin_camp`
- `/place structure dndclasses:goblin_camp` (only works in an allowed biome)

## Known limitations
- No respawning: the camp is lit, so neither goblins nor vanilla monsters spawn in it later. The starting garrison is the whole population.
- Trees cut at the edge of the clearing can leave a few floating leaves, and a tree grown later by a neighbouring chunk can occasionally reach into the camp (hobbit villages have the same issue).
- Goblin Warriors are strong (50 HP, 9 damage), so a camp is a hard fight early in the game.

## For developers
- Building blocks: `classes/Registry/GoblinBlocks.java` (the one-off blocks), `ModBlockFamilies.SCRAP_LASHED_PLANKS`, `STRETCHED_HIDE` and `TENT_CANVAS` (their flaps use the `TENT_FLAP` block set: leather sounds), `classes/Blocks/JunkPileBlock.java`, `WarOrderBoardBlock.java` and `TentStakeBlock.java`; textures from `tools/goblin_block_textures.py` and `tools/goblin_tent_textures.py`, the families' resources from `tools/block_families.py`, the rest hand-written. The camp swaps them in by index (`HIDES` keeps its length, the palisade reuses its draws), so camps generated before them still match across chunk borders.
- `src/main/java/mattonfire/dnd/world/gen/camp/`: `GoblinCampStructures` (registers the structure type and piece), `GoblinCampStructure` (site check), `GoblinCampPiece` (builds the camp from its own seed, in world coordinates, and spawns the goblins). Tents are built by `tent()` from a `TentShape` (the roof pieces of each column out from the ridge: `A_FRAME`, `WALL_TENT`, `CHIEF_TENT`). It still draws the randoms the old box tents drew, in the same order, and picks the tent's shape and depth from its own random, so the rest of a camp matches across chunk borders with camps generated before. The Guardian Beast post and its wolves come last, from `StructureObstacles.guardianPost`, so camps generated before it still match across chunk borders.
- Data: `data/dndclasses/worldgen/structure/goblin_camp.json`, `worldgen/structure_set/goblin_camps.json`, `tags/worldgen/biome/has_structure/goblin_camp.json`, `loot_tables/chests/goblin_camp.json` and `goblin_camp_supplies.json`.
- Devscripts: `devscripts/goblin-camp-locate.txt` (finds the nearest camp), `devscripts/goblin-camp-leash.txt` (logs a fresh camp's warrior positions before and after leaving and coming back), `devscripts/goblin-camp-tour.txt` (screenshots the dev world's camp at -408, 1096 and counts the goblins), `devscripts/goblin-blocks.txt` (the building blocks on a platform, their drops, recipes and the board's orders), `goblin-blocks-recon.txt` and `goblin-blocks-survey.txt` (`/place`s a camp and a Warren in a scratch run dir and shoots them next to the dev world's older camp), `goblin-tents-blocks.txt` (the tent blocks on a platform, a stake roping itself to a wall, their drops), `goblin-tents-locate.txt`, `goblin-tents-survey.txt` and `goblin-tents-closeup.txt` (three fresh camps far out in the dev world: from above and the sides, then each tent's door and wall and inside the chief's tent; scratch run dir).
