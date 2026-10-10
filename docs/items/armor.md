# Armor
Fourteen class-themed armor sets. A full set gives a bonus to anyone, boosted when your class matches the set.

![Armor stands wearing, from left: Thief, Assassin, Wizard, Cleric, Blood Hunter, Golden Horns and Holy Armor](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/armor-sets-1.png)
![Armor stands wearing, from left: Knight, Warrior, Prismarine, Wooden, Robe, Steampunk and Wither](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/armor-sets-2.png)

## How it works
- **Rarity:** every piece is an **Uncommon** [magic item](../systems/magic-items.md): its name is green and its tooltip says "Uncommon armor".
- Every piece has the same stats: helmet 3, chestplate 8, leggings 6, boots 3 armor; 3 toughness and 10% knockback resistance each, plus small speed and attack speed bonuses (helmets also +1 luck).
- **Durability, enchanting and repair:** every set has netherite durability and enchantability 15. The GeckoLib sets repair with netherite ingots; the others with a themed item: Prismarine (prismarine shard), Holy Armor (gold ingot), Robe (wool), Steampunk (copper ingot), Warrior (iron ingot), Wither (netherite scrap), Wooden (logs).
- **Full set (all four pieces):** gives its set bonus to anyone.
- **Matching class:** if your class is one the set is made for, every set effect goes up one level and you also get the class effect.

| Set | Set bonus (anyone) | Made for | Boosted bonus (matching class) |
|-|-|-|-|
| **Thief** | Jump Boost I | Rogue | Jump Boost II + Speed I |
| **Assassin** | Jump Boost I | Ranger | Jump Boost II + Night Vision |
| **Wizard** | Jump Boost I | Wizard | Jump Boost II + Fire Resistance |
| **Cleric** | Jump Boost I | Cleric | Jump Boost II + Resistance I |
| **Blood Hunter** | Jump Boost I | Blood Hunter | Jump Boost II + Strength I |
| **Golden Horns** | Jump Boost I | Barbarian, Bard | Jump Boost II + Strength I |
| **Holy Armor** | Jump Boost I | Paladin | Jump Boost II + Fire Resistance |
| **Knight** | Jump Boost I | Fighter | Jump Boost II + Resistance I |
| **Warrior** | Jump Boost I | Barbarian, Fighter | Jump Boost II + Haste I |
| **Prismarine** | Water Breathing | Druid | Water Breathing + Dolphin's Grace |
| **Wooden** | Jump Boost I | Druid | Jump Boost II + Haste I |
| **Robe** | Jump Boost I | Monk, Alchemist | Jump Boost II + Speed I |
| **Steampunk** | Jump Boost I | Artificer | Jump Boost II + Haste I |
| **Wither** | Jump Boost I | Warlock, Necromancer | Jump Boost II + Strength I |

## Where to find it / How to get it
- The mod's creative tab. There are no crafting recipes yet.

## Configuration
`config/dndclasses/dndclasses.json`:
- `applyArmorEffects` (default `true`): turn set bonuses off.
- `applyModificators` (default `true`): apply the pieces' attribute modifiers.
- `showDescriptions` (default `true`) and `descrtiptionsLength` (default 250, 20-1000): tooltip descriptions and their wrap width.

## Known limitations
- The Wither set has no boots in the creative tab (`/give @s dndclasses:wither_boots` works).
- The Thief set's items are named `rogue_*`.
- Prismarine, Holy Armor, Robe, Steampunk, Warrior, Wither and Wooden render as layered vanilla-style armor, not 3D models. Their capes, shoulder pads and gears have textures but no model yet. Holy Armor's layers are a placeholder recoloured from the Warrior set (its own texture is for a model that was never added).

## For developers
- Items: `Items/*ArmorItem` on `Items/lib/FAArmorItem` (GeckoLib armor, `ArmorMaterials.NETHERITE`) or `Items/lib/DndArmorItem` (vanilla `ArmorItem` with `Items/lib/ModArmorMaterials`, rendered by `Client/Render/LayeredArmorRenderer` from `textures/models/armor/<set>/{body,leggings}_{lower,middle,upper}.png`). Set logic: `Items/lib/SetBonusArmor`, `Items/lib/FAArmorEffectHandler`. Devscript: `armor-class-bonus.txt`.
