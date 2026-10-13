# Tinker's Goggles
A brass helmet of lenses made by the gnomes of [Cogsdeep](../structures/cogsdeep.md). Wear them and look at a chest, barrel, hopper, furnace or any other container: what's inside shows beside your crosshair.

![Tinker's Goggles on a chest: "Chest" with 8 diamonds and 12 redstone beside the crosshair](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/cogsdeep/cd-goggles-chest.png)

## How it works
- Look at a container block within 6 blocks (a little past arm's reach) while wearing the goggles. A panel to the right of the crosshair shows the block's name and its contents, like items merged into one stack each with their total count, 9 to a row (at most 27 kinds). It refreshes twice a second, so a hopper filling or a furnace smelting updates as you watch.
- Double chests show both halves.
- A container nobody has opened yet (a structure chest whose loot hasn't been rolled) shows "Sealed: nobody has opened it yet". Peeking never rolls the loot, so a locked chest stays locked and a sealed chest stays sealed until someone opens it.
- The server checks you really are looking at the block, so the goggles don't see through walls.
- Any container works: chests, trapped chests, barrels, hoppers, furnaces, blast furnaces, smokers, dispensers, droppers, brewing stands, shulker boxes.
- As armor: a helmet with 1 armor point (light enough for a Wizard), netherite durability, repaired with copper ingots. Made for Gnomes (gold tooltip line), but anyone can wear them and they work for everyone. Uncommon.

## Where to find it
- Gnome Tinkers in Cogsdeep sell them to Gnomes only (kin stock), for 10 emeralds (7 at kin price, less again with a good standing). See [Gnomes](../mobs/gnomes.md#trades).
- 12% in the clocktower strongbox of Cogsdeep (`chests/cogsdeep_vault`).
- `/give @s dndclasses:tinkers_goggles`

## Known limitations
- The panel doesn't show item tooltips, enchantments or damage, only icons and counts.
- Containers that aren't blocks (chest minecarts, donkeys) aren't shown.

## For developers
- `classes/Items/racial/TinkersGogglesItem` (a `DndArmorItem` helmet of `ModArmorMaterials.TINKERS_GOGGLES`; `wears`, `RANGE`, `getProtection` returns 1), drawn by `LayeredArmorRenderer` from `textures/models/armor/tinkers_goggles/body_middle.png` (`tools/cogsdeep_textures.py`).
- Server: `classes/Items/racial/TinkersGoggles` (`C2S_PEEK` with the block, `S2C_CONTENTS` with the block, a sealed flag and the merged stacks; each count is sent apart from its stack, since a merged count can pass a byte).
- Client: `Client/Hud/GogglesHud` (asks every 10 ticks while the crosshair is on a container within range, draws the panel, logs each new answer as `[Goggles] x, y, z: N kinds, ...` or `sealed`).
- Devscript: `cogsdeep-check.txt` (a chest, a furnace, a hopper and a sealed chest).
