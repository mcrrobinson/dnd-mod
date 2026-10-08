# Alchemist
The brewer: the only class that can brew safely and use the Fast Brewing Stand, but it can't enchant.

## How it works
- **Safe brewing:** when a vanilla brewing stand finishes a brew, it explodes (power 10, like TNT) if the last player to use it wasn't an Alchemist. Stands nobody has used, such as hopper-fed ones, are safe. A death from it hints "Maybe get a alchamist to brew next time...".
- **Fast Brewing Stand:** only Alchemists can open it, and it brews 10× faster. See [Potions and brewing](../items/potions-and-brewing.md).
- **Can't enchant:** enchanting tables refuse, and enchanted books won't apply on an anvil.
- **Special (power-up key, full mana):** every single-effect potion in your main inventory is upgraded to the strongest registered potion with the same effect (e.g. Strength → Strength II). Splash and lingering potions stay splash and lingering. If nothing can be upgraded, the mana is kept.

## Known limitations
- The class blurb mentions random potion backfires. They aren't implemented.

## For developers
- Explosion: `mixin/BrewingStandBlockEntityMixin` (+ `BrewingStandBlockMixin` records the last user). Fast stand: `Blocks/FastBrewingStandBlock`, `Entities/FastBrewingStandBlockEntity`. Enchanting: `mixin/EnchantingTableMixin`, `mixin/AnvilScreenHandlerMixin`.
- Devscripts: `brewing-explosion.txt`, `brewing-use-alchemist.txt`.
