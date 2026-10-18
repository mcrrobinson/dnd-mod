# Adventuring blocks
Blocks that aren't tied to one race's home but give the mod's systems a physical form: **Bedrolls** you sleep in for a [long rest](../systems/rests.md) without a bed, the **Adventurer's Signpost** that names a [dungeon](../structures/dungeons.md) and its Challenge tier at its entrance, the wall-hung **Wanted Poster**, and **Tavern Tables and Benches** in four woods. They're listed in the **DnD Building Blocks** creative tab.

![Tavern tables and benches](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/shared-blocks/shared-blocks-furniture.png)

## How it works
### Bedrolls
A canvas sleeping bag laid out on the ground, two blocks long like a bed: a foot with two leather ties and a head with the blanket turned down under a pillow. Four colours: the red **Bedroll** and the **Green**, **Blue** and **Brown Bedroll**.

- **Sleep in it like a bed**: right-click it at night (or in a thunderstorm). It gives the same [long rest](../systems/rests.md#long-rests-in-beds) as a bed (20 s asleep, or the night skipped while you're fully asleep), with the same refusals.
- **It never sets your spawn point.** No "Respawn point set", and dying sends you back to your old bed or the world spawn.
- You lie on it, 3 pixels off the ground, not floating at bed height.
- It doesn't bounce you like a bed, and in the Nether or the End it says "You can't sleep here" instead of exploding.
- **Making camp** works with any colour: right-click a lit campfire with a Bedroll at night for a [party camp](../systems/rests.md#party-camps). Used anywhere else, the Bedroll item lays the bedroll down.
- Mined in one hit with anything, like a bed; it drops itself from its head half. Burns like wool. Stacks to 1.

### Adventurer's Signpost
A wooden post with an arrow-shaped board. Its text is written on both faces of the board, like sign text, shrunk to fit (never bigger than a sign's), and right-clicking it shows the whole text on the action bar.

- **At a dungeon's entrance** it names the dungeon on one line and its Challenge tier on the next in red: "The Barrow of Ashmoor / Challenge II". The arrow points to the way in. Each time someone reads it, it checks the dungeon again, so a tier changed with `/dungeon tier` shows up, and once the boss is dead it adds "Cleared" in green.
- **Your own signpost**: rename the item in an anvil and the board says that name. Placed, its arrow points the way you're looking. A blank one says "A blank signpost. Name it in an anvil before you put it up."
- Axe, oak fence hardness (strength 1.0), drops itself, burns.

### Wanted Poster
Yellowed parchment pinned to a wall, with torn edges, a red WANTED header and a crude sketch of one of four villains, picked at random when it's put up (the block's `sketch` state, 0-3). Right-click it to read it on the action bar:

| Sketch | Reads |
|-|-|
| 0, a goblin | "WANTED: Skarnak the goblin chief, for raiding the Bywater road. Reward: 40 gold." |
| 1, an orc | "WANTED, dead or alive: Grimtusk One-Eye, orc raider. Reward: 75 gold." |
| 2, a hooded highwayman | "WANTED: Black Ned, highwayman and horse thief. Reward: 30 gold." |
| 3, an owlbear | "WANTED: the Owlbear of Hollowmere Wood. Reward: 100 gold and a free pint." |

It's dressing, the wall-hung cousin of the [bounty notice](../structures/hobbit-tavern.md): it isn't a bounty and gives no reward. It hangs on the face of a solid block like a ladder, has no collision, and drops off when its wall goes. Strength 0.3, axe, drops itself, burns.

### Tavern Tables and Benches
Heavy plank furniture in **Oak**, **Spruce**, **Dark Oak** and **Heartwood**, using the wood's planks texture and copying its hardness and sounds.

- **Table**: a full-size 3-pixel top on four square legs, 16 pixels high, so a candle, a cake or an item frame stands on it. Tables beside each other join into one long table (of any wood): a leg only stands at a corner where neither side next to it is joined.
- **Bench**: a 10-pixel-wide seat at 9 pixels high on two board legs, running across the way you look when you place it (so it lines up beside the table in front of you). Benches end to end on the same line join into one, with legs only at its ends. You can't sit on them yet.

Every block here drops itself when mined.

![A named signpost and a dungeon one](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/shared-blocks/shared-blocks-signpost-a.png)
![The four Wanted Posters](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/shared-blocks/shared-blocks-poster.png)
![Bedrolls in all four colours](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/shared-blocks/shared-blocks-bedrolls.png)

## Where to find it
| Block | Where it's placed | Recipe |
|-|-|-|
| Bedroll (red) | - | 3 wool over 3 leather (unchanged), or any Bedroll and red dye |
| Green, Blue, Brown Bedroll | brown in [goblin camp](../structures/goblin-camps.md) tents and [orc yurts](../structures/orc-strongholds.md) (two each), green on [elven talans](../structures/elven-enclaves.md) | any Bedroll and green, blue or brown dye |
| Adventurer's Signpost | beside every [dungeon](../structures/dungeons.md) entrance: past the barrow's passage, outside the warren's palisade gate, by the ruin's gatehouse approach, outside the ring of stones | 3 planks over 2 sticks (in the middle column) |
| Wanted Poster | the [Green Dragon](../structures/hobbit-tavern.md) (over the bounty board and by the door), the [dwarven fortress](../structures/dwarven-fortresses.md) gate (either side of the door), the [elven](../structures/elven-enclaves.md) Speaker's Hall (on the trunk over the lectern) | paper, red dye and charcoal (shapeless) make 2 |
| Tavern Table | the Green Dragon (spruce), the [dwarven](../structures/dwarven-fortresses.md) mead hall (dark oak, three wide), the [orc](../structures/orc-strongholds.md) Great Longhouse (dark oak) | 3 of the wood's slabs over 2 of its planks (at the sides) |
| Tavern Bench | the Green Dragon (oak), the mead hall (dark oak), the Great Longhouse (spruce, round the fire and behind the tables) | 3 of the wood's slabs over 2 sticks (at the sides) make 2 |

Structures generated before this keep their old straw beds, stair chairs, slab tables and log benches, and old dungeons have no signpost.

## Known limitations
- No sitting on benches.
- A signpost only knows its dungeon's live tier once someone has read it after the server has seen the dungeon (the registry learns about a dungeon when a player first goes in); until then it shows the tier it was generated with.
- Signposts aren't placed outside settlements or along roads yet: there are no roads, and settlements have nothing to point at.
- Bedrolls aren't in any mineable tag (like beds): they break in one hit with anything.

## For developers
- `classes/Registry/AdventuringBlocks.java`: every block and item, the `adventurers_signpost` block entity type, flammability, the creative-tab entries, the client cutout list, and the `EntitySleepEvents.ALLOW_SETTING_SPAWN` listener that keeps bedrolls from setting a spawn point. `TABLES` and `BENCHES` map wood name to block.
- Block classes in `classes/Blocks/adventuring/`:
  - `BedrollBlock` extends `BedBlock` (so vanilla's sleeping, wake-up position and occupancy all apply) with a model instead of the bed's block entity, flat shapes, no bounce and no Nether explosion. `foot(facing)` / `head(facing)` give the states for structure pieces. `SLEEP_DROP` (6/16 block) is how much lower the sleeper lies: `mixin/BedrollSleepMixin` moves them down after `LivingEntity.setPositionInBed`.
  - `classes/Rest/BedrollItem` is now a `BedItem`: a lit campfire makes camp (`PartyCamp.use`), anything else places the block. `PartyCamp.BEDROLL` is `AdventuringBlocks.BEDROLL_ITEM`; `CampfireRest` accepts any `BedrollItem`.
  - `SignpostBlock` (`FACING` = the way the arrow points) and `SignpostBlockEntity`: `lines` (text components, synced to the client), `setDungeon(startKey, name, tier)` and `refresh(world)`, which re-reads `DungeonRegistry`. NBT: `Lines` (JSON text strings), `Dungeon` (start key). `client/renderer/SignpostBlockEntityRenderer` draws the lines (wrapped at 90 font pixels, scaled to fit 8x6 board pixels, at most 1/96 block per pixel).
  - `WantedPosterBlock` (`FACING`, `SKETCH` 0-3; `poster(facing, sketch)`).
  - `TavernFurnitureBlock` (the `north/east/south/west` join properties, with rotate and mirror), `TavernTableBlock`, `TavernBenchBlock` (`AXIS`; `along(axis)`). `PieceBlocks.joinsNeighbours` includes them, so pieces get them joined up by post-processing like fences.
- Structures: `EntrancePiece.signpost` (every theme; the name from `DungeonState.name(startKey, theme)`, the tier from `DungeonState.tierText(tier)`), `InnPiece.furnish`, `BrewhallPiece`, `GatePiece`, `LonghousePiece`, `YurtPiece`, `GoblinCampPiece` (tents), `TalanPiece` (dwellings), `SpeakersHallPiece`.
- Resources: `tools/adventuring_block_textures.py` writes the textures (bedrolls, recoloured Bedroll icons, signpost, posters), the blockstates (the tables and benches are multipart: legs on unjoined corners and ends), models (`template_tavern_*` with a `planks` texture), loot (bedrolls from the head half), recipes, the `dndclasses:bedrolls` item tag, the axe tag and lang names.
- Devscripts: `shared-blocks.txt` (placing and joining tables and benches by hand, a named signpost, reading posters, a poster dropping off its wall, the drops, bedroll placement, sleeping in one: rest, height, no spawn point). For fresh structures in the dev world: `shared-blocks-locate.txt` (finds them far out), `shared-blocks-recon.txt` (generates them and logs their pieces and dungeon entrances), `shared-blocks-survey.txt`, `shared-blocks-closeups.txt` and `shared-blocks-signposts.txt` (screenshots, and reading each dungeon's signpost).
