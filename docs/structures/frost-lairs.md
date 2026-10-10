# Frost Lairs
The home of a [Frost Drake](../mobs/dragons.md), on the summit of a Frozen Peaks mountain. It's the [dragon lair](dragon-lairs.md) remade in snow and ice, and the only place to find a Frost Drake.

![A frost lair on a Frozen Peaks summit: ice spires round a snowy nest, with its Frost Drake](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/feat-frost-drake/frost_lair-corner.png)

## How it works
- The lair sits on the highest point of a Frozen Peaks mountain, with the mountain falling away on every side, like a dragon lair.
- The peak is cut flat and floored with snow, packed ice and a little calcite, with a ring of blue ice where a dragon lair has its runes, and drifts of snow (1-2 layers) across the floor.
- A low rim of packed ice, blue ice and snow runs round the edge, with eight **ice spires** on it: packed ice veined with blue ice, thick at the foot and tipped with blue ice, 6-9 blocks tall. A quarter of them have snapped off at 2-3 blocks.
- In the middle is a nest of spruce logs and bones round a bed of snow, heaped with snow, gold and raw gold blocks and the odd diamond block, and a hoard chest. There's no powder snow, so no traps.
- Where the floor overhangs the slopes it's held up by stone capped with snow.
- **Hoard chest (`chests/frost_lair`):** 5-8 rolls of gold, emeralds, blue ice, diamonds, golden apples, bottles o' enchanting, bones, and enchanted diamond swords and boots. One more roll has a 1 in 8 chance each of an enchanted golden apple or a trident, and a 1 in 4 chance of a [Staff of Ice](../items/staffs.md). It counts as a hard lock for [lockpicking](../systems/d20-skill-checks.md).
- Half of all frost lairs have a [mimic](../mobs/mimic.md) beside the hoard, holding hoard loot.
- Each lair starts with one Frost Drake (30% of lairs have two). It never strays more than about 24 blocks before circling back.
- Once it's killed, the lair waits **3 in-game days** and then sends a new one (structure spawn override; never in peaceful or with another Frost Drake within 64 blocks). A drake that left without dying (e.g. in peaceful) is replaced as soon as possible.

![The frost lair from above](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/feat-frost-drake/frost_lair-top.png)

## Where to find it / How to get it
- Frozen Peaks only. Structure set spacing 12 chunks, separation 4.
- It's never within 6 chunks of a dragon lair or a dwarven fortress that actually generates (both can also be on Frozen Peaks). Dragon lairs still generate on Frozen Peaks too.

## Commands
- `/locate structure dndclasses:frost_lair`

## For developers
- Shares its code with the dragon lair: `world/gen/lair/DragonLairStructure` (registered a second time as the `dndclasses:frost_lair` structure type, `FROST_CODEC`) and `LairPiece` with `Kind.FROST` (saved as `Kind: "frost"` in the piece NBT; pieces without it are dragon lairs). The storm lair draws exactly the same random numbers as before, so half-generated dragon lairs from older versions still match up.
- The 6-chunk distance is checked in `DragonLairStructure.nearOtherStructure`, not with the structure set's `exclusion_zone`: that takes only one other set, and it counts every grid slot of that set, which would rule out over 95% of sites. The check only counts dragon lairs and fortresses that would really generate.
- Data: `worldgen/structure/frost_lair.json`, `worldgen/structure_set/frost_lairs.json`, biome tag `has_structure/frost_lair`, loot table `chests/frost_lair.json`.
- Respawn timer: `LairRespawns`, shared with dragon lairs (keyed by lair centre).
- Devscripts: `frost-lair.txt` (looks over the nearest lair to the dev world's spawn) and `frost-drake.txt` (ends with `/locate`).
