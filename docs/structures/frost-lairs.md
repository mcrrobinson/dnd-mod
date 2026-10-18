# Frost Lairs
The home of a [Frost Drake](../mobs/dragons.md), on the summit of a Frozen Peaks mountain. It's the [dragon lair](dragon-lairs.md) remade in snow and ice, and the only place to find a Frost Drake.

![A frost lair on a Frozen Peaks summit: ice spires round a snowy nest, with its Frost Drake](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/feat-frost-drake/frost_lair-corner.png)

## How it works
- The lair sits on the highest point of a Frozen Peaks mountain, with the mountain falling away on every side, like a dragon lair.
- The peak is cut flat and floored with snow, packed ice and a little rimestone, with a ring of blue ice where a dragon lair has its runes, and drifts of snow (1-2 layers) across the floor.
- A low rim of rimestone, packed ice, blue ice and snow runs round the edge, with eight **ice spires** on it: packed ice veined with blue ice on a rimestone footing, thick at the foot and tipped with blue ice, 6-9 blocks tall, with hoarfrost crystals growing on them. A quarter of them have snapped off at 2-3 blocks.
- In the middle is a nest of spruce logs, woven nest and bones round a woven Dragon Nest floor, heaped with gold and raw gold blocks, the odd diamond block, Treasury Coin Piles and the drake's shed scales, and a hoard chest. There's no powder snow, so no traps.
- Where the floor overhangs the slopes it's held up by stone capped with snow.
- **Hoard chest (`chests/frost_lair`):** 5-8 rolls of gold, emeralds, blue ice, diamonds, golden apples, bottles o' enchanting, bones, and enchanted diamond swords and boots. One more roll has a 1 in 8 chance each of an enchanted golden apple or a trident, and a 1 in 4 chance of a [Staff of Ice](../items/staffs.md). Every hoard chest also holds one magic item, Rare (70%) or Very Rare (30%), and 8% of them Frostbrand (see [Magic loot](../systems/magic-loot.md)). It counts as a hard lock for [lockpicking](../systems/d20-skill-checks.md).
- Half of all frost lairs have a [mimic](../mobs/mimic.md) beside the hoard, holding hoard loot.
- **Side cache**: like a dragon lair, a cairn (packed ice, topped with blue ice) against the rim north or south of the nest holds a second hoard chest behind a 2x2x2 **Heavy Boulder** (Hard). A Barbarian heaves it aside; anyone else uses TNT.
- Each lair starts with one Frost Drake (30% of lairs have two). It never strays more than about 24 blocks before circling back.
- Once it's killed, the lair waits **3 in-game days** and then sends a new one (structure spawn override; never in peaceful or with another Frost Drake within 64 blocks). A drake that left without dying (e.g. in peaceful) is replaced as soon as possible.

![The frost lair from above](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/feat-frost-drake/frost_lair-top.png)

### Building blocks
The lair is dressed with its own blocks ([Building blocks](../dev/building-blocks.md)), all in the **DnD Building Blocks** creative tab:

| Block | Where | Notes |
|-|-|-|
| Rimestone | 15% of the rim, 10% of the floor (where calcite was), half the buttress blocks at the foot of each spire | grey stone with frost crystals and pale blue ice in its cracks; 8 stone round blue ice make 8 |
| Rimestone Bricks (stairs, slab, wall) | 30% of the rim, the foot of each ice spire | 4 rimestone make 4, or a stonecutter (bricks, stairs, 2 slabs or a wall from 1 rimestone) |
| Hoarfrost Crystal | on top of 45% of the spires' buttresses, on the spires' sides above them (12% of faces), and the odd one (1 in 10) on the floor round the nest | pale blue-white ice crystals that grow like amethyst on any face, light level 3; drop themselves (pickaxe). A Frost Dragon Scale on blue ice makes 4 |
| Dragon Nest (slab) | the nest floor; 35% of the blocks in the ring of logs, and some of the slabs on top of it | as in a [dragon lair](dragon-lairs.md#building-blocks) |
| Treasury Coin Pile | the hoard: heaps of 3-7 layers among the gold and diamond blocks, and loose 1-3 layer piles round them | the Dwarven Fortresses' block |
| Shed Frost Scales | heaps of 1-4 layers in the hoard, and 1-2 layer piles on about a fifth of the floor just outside the nest (not where snow has drifted) | pale blue scales in layers like snow, which (unlike snow) also lie on ice; each layer drops one **Frost Dragon Scale**, never the block. 3 scales make 3 layers |

Lairs generated before these blocks were added keep their old rim and nest.

## Where to find it
- Frozen Peaks only. Structure set spacing 12 chunks, separation 4.
- It's never within 6 chunks of a dragon lair or a dwarven fortress that actually generates (both can also be on Frozen Peaks). Dragon lairs still generate on Frozen Peaks too.

## Commands
- `/locate structure dndclasses:frost_lair`

## For developers
- Shares its code with the dragon lair: `world/gen/lair/DragonLairStructure` (registered a second time as the `dndclasses:frost_lair` structure type, `FROST_CODEC`) and `LairPiece` with `Kind.FROST` (saved as `Kind: "frost"` in the piece NBT; pieces without it are dragon lairs). The storm lair draws exactly the same random numbers as before, so half-generated dragon lairs from older versions still match up.
- The 6-chunk distance is checked in `DragonLairStructure.nearOtherStructure`, not with the structure set's `exclusion_zone`: that takes only one other set, and it counts every grid slot of that set, which would rule out over 95% of sites. The check only counts dragon lairs and fortresses that would really generate.
- Data: `worldgen/structure/frost_lair.json`, `worldgen/structure_set/frost_lairs.json`, biome tag `has_structure/frost_lair`, loot table `chests/frost_lair.json`.
- Respawn timer: `LairRespawns`, shared with dragon lairs (keyed by lair centre).
- Blocks: see [Dragon Lairs](dragon-lairs.md#for-developers); the frost lair adds the `RIMESTONE` and `RIMESTONE_BRICKS` families. `LairPiece.iceSpire` places the crystals, `roundTheNest` the scales and floor crystals.
- Devscripts: `frost-lair.txt` (looks over the nearest lair to the dev world's spawn) and `frost-drake.txt` (ends with `/locate`).
