# Artificer
A tinkerer that enchants as it crafts and is the only class that can roll the mod's [enchantments](../enchantments/README.md).

## How it works
- **Speed:** 0.12 movement speed (20% faster than vanilla).
- **Damage:** 25% less damage (a ×0.75 attack damage modifier).
- **Auto-enchant:** crafted tools, weapons, armor, bows, crossbows, tridents and fishing rods have a 25% chance to come out enchanted, as if from an enchanting table at level 5-15.
- **Mod enchantments:** only Artificers can get Lunge, Invulnerability, Tree Feller and Grid Miner from an enchanting table.
- **Unaffected by potions:** you can't drink potions (splash and lingering can still be thrown at others). Potion effects from any potion source never apply to you. Ability effects still work.
- **Special (power-up key, full mana): reinforced armor.** +8 armor and +4 armor toughness for 30 seconds.

## For developers
- `Misc/ArtificerDamage`, `Misc/ArtificerCrafting` (+ `mixin/CraftingResultSlotMixin`, `CraftingQuickMoveMixin`), `mixin/EnchantingTableMixin`, `Effects/ArmorBuffEffect`.
