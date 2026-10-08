# Potions and brewing
The mod's potions, the Alchemist's Fast Brewing Stand, and the exploding brewing stands.

## How it works
- **Exploding brewing stands:** a vanilla brewing stand explodes (power 10) when it finishes a brew that a non-[Alchemist](../classes/alchemist.md) started: a non-Alchemist had the stand open when the brew began, and no Alchemist did. The blast hurts everyone nearby but doesn't break blocks; the stand pops off and drops itself and its contents. Brews started with nobody looking, such as hopper-fed ones, never explode, even if a non-Alchemist opened the stand earlier. A brew that's cancelled (ingredient taken out) doesn't explode.
- **Fast Brewing Stand:** brews 10× faster than vanilla. Only Alchemists can open it; anyone else gets a message. Drops itself when broken (fastest with a pickaxe).
- **Potion of Freezing** (`dndclasses:freeze`): Awkward Potion + Ice. Holds the drinker in place for 3 minutes. Wizards are immune.
- **Arrow Storm** and **Invulnerability** potions (3 minutes each) are registered but have no recipe (`/give @s minecraft:potion{Potion:"dndclasses:arrow_storm"}`).
- Some classes ignore potions: [Paladin](../classes/paladin.md), [Artificer](../classes/artificer.md) and [Fighter](../classes/fighter.md) (buffs only).

## Where to find it / How to get it
- Fast Brewing Stand: craft a row of blaze rod, brewing stand, blaze rod.

## For developers
- `Blocks/FastBrewingStandBlock`, `Entities/FastBrewingStandBlockEntity`, `Registry/ModPotions`, `mixin/BrewingStandBlockEntityMixin` (arms a brew when it starts, explodes it when it finishes; NBT `DndBrewArmed`). Freeze recipe: `DnDClasses.onInitialize`.
- Devscripts: `brewing-explosion.txt`, `brewing-use-alchemist.txt`, `brewing-use-fighter.txt`.
