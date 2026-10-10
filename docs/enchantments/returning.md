# Returning

An Artificer enchantment for thrown items. A Returning trident, snowball, egg or ender pearl goes straight back into the thrower's inventory once it lands, so you can keep throwing the same one.

![A Returning trident flying at a husk](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/returning-trident.png)

## How it works
- Max level: I. Rarity: uncommon.
- **Trident:** when it hits a mob or a block, or falls below the bottom of the world, it goes straight back into the thrower's inventory. It doesn't fly back like Loyalty. You hear the trident-return sound. If your inventory is full, it drops at your feet.
- **Snowball, egg, ender pearl:** when it hits something, you get one back. The normal effect still happens: the snowball knocks back, the egg can hatch a chicken and the pearl teleports you.
- Can't be combined with **Loyalty** (it does the same job) or **Riptide** (a Riptide trident can't be thrown).
- In creative, a throw doesn't use up the item, so nothing is given back and you don't get a duplicate.

## How to get it
- **Enchanting table:** only an Artificer can roll it, and only on a trident.
- **Anvil:** put a Returning enchanted book on a trident, snowballs, eggs or ender pearls.

## Commands
`/give @s minecraft:snowball{Enchantments:[{id:"dndclasses:returning",lvl:1}]} 16`

## Known limitations
- It only returns to a living player who threw it and is in the same world. Otherwise the item behaves as in vanilla.
- Splash and lingering potions and bottles o' enchanting can't take it, because it would give endless potions and XP. Any other thrown item that carries the enchantment (for example, given by command) still returns.

## For developers
- `Enchantments/ReturningEnchantment.java`: which items it accepts and which enchantments it conflicts with.
- `Misc/Returning.java`: shared helpers (`hasReturning`, `returnTarget`, `giveBack`).
- `mixin/TridentEntityMixin.java`: hooks the start of `tick`. It returns the trident once `dealtDamage` or `inGround` is set, or once it falls below the world.
- `mixin/ProjectileEntityMixin.java`: hooks the start of `onCollision` for any `ThrownItemEntity`. Each projectile returns once.
- Registered in `Registry/ModEnchantments.java`. It's on the Artificer-only list in `mixin/EnchantingTableMixin.java`.
- Test: `timeout 300 ./gradlew runClient -PdevScript=devscripts/returning-check.txt`
