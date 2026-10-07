# Paladin
A holy tank that heals the whole party. It shrugs off every potion, can't make anything, and the Nether drains it.

## How it works
- **Health:** 26 max. You start at 25.
- **Unaffected by potions:** potion effects from drinking, splash, lingering clouds and tipped arrows never apply, good or bad. You can't drink or throw potions either. Effects from abilities, beacons, food and so on still work.
- **Can't craft:** you can't take results out of the crafting grid, crafting table, stonecutter, loom or smithing table ("Paladins cannot craft items!").
- **Can't brew:** brewing stands (and the Fast Brewing Stand) won't open for you.
- **Weak in the Nether:** while in the Nether you deal half damage, have half armor and move 20% slower. It wears off as soon as you leave.
- **Special (power-up key, full mana):** instantly heals every player within 10 blocks, including you, to full health.

## For developers
- Nether weakness: `Misc/PaladinNetherWeakness`. Crafting: `mixin/SlotMixin`, `mixin/SmithingScreenHandlerMixin`. Brewing: `mixin/BrewingStandBlockMixin`. Potions: `PotionImmunity`.
