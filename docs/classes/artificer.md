# Artificer
A tinkerer that enchants as it crafts and is the only class that can roll the mod's [enchantments](../enchantments/README.md).

![An Artificer with Arcane Armor up: four armor points with no armor worn](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/artificer-arcane-armor.png)

## How it works
- **Speed:** 0.12 movement speed (20% faster than vanilla).
- **Damage:** 25% less damage (a ×0.75 attack damage modifier).
- **Auto-enchant:** crafted tools, weapons, armor, bows, crossbows, tridents and fishing rods have a 25% chance to come out enchanted, as if from an enchanting table at level 5-15. Enchantments that don't work on the item (Tree Feller off axes, Grid Miner off pickaxes and shovels) are dropped from the roll.
- **Mod enchantments:** only Artificers can get Lunge, Invulnerability, Tree Feller and Grid Miner from an enchanting table.
- **Unaffected by potions:** you can't drink potions (splash and lingering can still be thrown at others). Potion effects from any potion source never apply to you. Ability effects still work.
- **Special (power-up key, full mana): reinforced armor.** +8 armor and +4 armor toughness for 30 seconds.

### Subclasses
From class level 3, choose one of these two at an Attunement Table (see [Subclasses](../systems/class-progression.md#subclasses)). Your subclass's branch opens fully. The other branch's first node stays open, but the rest of it is sealed.

| Subclass | Branch | Nodes | Feature (free, doesn't use a passive slot) |
|-|-|-|-|
| Armorer | Right | Reinforced Plating (first node, open to both), Repair Field, Thorned Plating | **Power Armor**: Arcane Armor lasts 45 seconds instead of 30 and adds 0.5 knockback resistance |
| Battle Smith | Left | Tinkerer (first node, open to both), Steel Defender, Overclock | **Battle Ready**: +2 melee damage with any identified magic weapon |

How the features work:
- **Power Armor** (Armorer): Arcane Armor lasts 45 s instead of 30 s. While it's on (Mechanical Titan's included) you have +0.5 knockback resistance.
- **Battle Ready** (Battle Smith): +2 damage on your melee hits with a magic weapon whose magic is awake (a weapon with a [magic tier](../systems/magic-items.md), +N gear included, that's identified). Unidentified weapons and mundane ones get nothing.

## For developers
- `Misc/ArtificerDamage`, `Misc/ArtificerCrafting` (+ `mixin/CraftingResultSlotMixin`, `CraftingQuickMoveMixin`), `mixin/EnchantingTableMixin`, `Effects/ArmorBuffEffect`.
