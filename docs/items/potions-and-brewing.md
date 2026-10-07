# Potions and brewing
The mod's potions, the Alchemist's Fast Brewing Stand, and the exploding brewing stands.

## How it works
- **Exploding brewing stands:** a vanilla brewing stand explodes (power 10, breaks blocks) when it finishes a brew whose last user wasn't an [Alchemist](../classes/alchemist.md). Unused stands (e.g. hopper-fed) are safe.
- **Fast Brewing Stand:** brews 10× faster than vanilla. Only Alchemists can open it.
- **Potion of Freezing** (`dndclasses:freeze`): Awkward Potion + Ice. Holds the drinker in place for 3 minutes. Wizards are immune.
- **Arrow Storm** and **Invulnerability** potions (3 minutes each) are registered but have no recipe.
- Some classes ignore potions: [Paladin](../classes/paladin.md), [Artificer](../classes/artificer.md) and [Fighter](../classes/fighter.md) (buffs only).

## Where to find it / How to get it
- Fast Brewing Stand: craft a row of blaze rod, brewing stand, blaze rod.

## For developers
- `Blocks/FastBrewingStandBlock`, `Entities/FastBrewingStandBlockEntity`, `Registry/ModPotions`, `mixin/BrewingStandBlockEntityMixin`. Freeze recipe: `DnDClasses.onInitialize`.
- Devscripts: `brewing-explosion.txt`, `brewing-use-alchemist.txt`, `brewing-use-fighter.txt`.
