# Racial weapons
Four weapons made by the races of the world: the Elven Longbow, the Dwarven Waraxe, the Orcish Greataxe and the Halfling Sling. Anyone can use them. Their tooltip says in gold which race they're made for, and their home settlement sells or trades them (some only to that race, as kin stock).

## How it works
| Weapon | Made for | Stats | Rarity |
|-|-|-|-|
| **Elven Longbow** | Elves | A bow: arrows fly 20% faster (so they also hit about 20% harder), and it has Power I built in, on top of any Power enchantment. 480 durability | Uncommon |
| **Dwarven Waraxe** | Dwarves | 10 attack damage, 0.9 attack speed; +50% damage to goblins (Goblin Warriors and the Goblin Warlord, `#dndclasses:goblins`). Diamond-tier axe | Uncommon |
| **Orcish Greataxe** | Half-Orcs | 11 attack damage, 0.8 attack speed. Diamond-tier axe | Uncommon |
| **Halfling Sling** | Halflings | Right-click to sling one cobblestone from your inventory (offhand first) for 4 damage, then a 0.8 s cooldown. 250 durability, 1 per shot. Without cobblestone: "You need cobblestone to sling." Creative players need none | Common |

### Elven Longbow
- Built-in Power: an unenchanted Longbow shoots like a Power I bow (+1 arrow damage). Enchanted with Power N it shoots like Power N+1 (+0.5 more), so Power V acts as Power VI.
- The speed boost multiplies the draw's arrow speed, so it stacks with a Ranger's quick draw and Arrow Storm, and [+N magic bows](../systems/magic-items.md) add their +10% per +1 on top.
- It takes bow enchantments (Power, Punch, Flame, Infinity, Unbreaking, Mending) at the enchanting table and anvil.

### Dwarven Waraxe
- The +50% applies to melee hits with the Waraxe in your main hand. It's applied before armor, with the other damage bonuses (class passives, magic items).
- It's an axe: it chops wood and strips logs, and takes axe enchantments.

### Orcish Greataxe
The biggest hit of any axe, for the slowest swing.

### Halfling Sling
- The stone is a thrown projectile (`dndclasses:sling_stone`), shown as a flying cobblestone, which breaks into dust on whatever it hits.
- It counts as a thrown hit from you, like a snowball, so it works on mobs and players and knocks them back a little.

## Where to find it
| Weapon | Where |
|-|-|
| Elven Longbow | The Fletcher in an [Elven Enclave](../structures/elven-enclaves.md) sells it to anyone for 16 emeralds (12 for Elves, kin price). 15% in the Heart Tree chest |
| Dwarven Waraxe | Dwarf players get it as a rare extra barter roll (2% per gold ingot) from the dwarves of a [Dwarven Fortress](../structures/dwarven-fortresses.md) |
| Orcish Greataxe | Not sold yet: the Orc Stronghold will sell it for 14 emeralds |
| Halfling Sling | The innkeeper sells it to Halflings only (kin stock), for 6 emeralds (4 at kin price) |

All four are in the mod's creative tab, or `/give @s dndclasses:elven_longbow` (`dwarven_waraxe`, `orcish_greataxe`, `halfling_sling`).

## Commands
None. To compare, `/dndrace set <player> <race>` changes your race (operators).

## Configuration
`showDescriptions` in `config/dndclasses/dndclasses.json` hides the description lines (the "Made for" line always shows).

## Known limitations
- The Orcish Greataxe has no seller until the Orc Stronghold exists.
- The textures are placeholders drawn by `tools/racial_gear_textures.py`.
- The Halfling Sling has no draw animation; it fires on click.

## For developers
- Items in `classes/Items/racial`: `ElvenLongbowItem` (a `BowItem`; `arrowSpeed` and `applyBuiltInPower` are called from `BowItemMixin`, where vanilla shoots the arrow, before `MagicGear.applyArrowBonus`), `RacialAxeItem` (an `AxeItem` with its races; the Waraxe and Greataxe), `HalflingSlingItem` and `entity/SlingStoneEntity` (a `ThrownItemEntity`, rendered by `FlyingItemEntityRenderer`). All implement `Items/lib/RacialGearItem` (`getMatchingRaces()`), and their tooltips use `ArmorTooltips.appendRaces` and `appendDescription`.
- The Waraxe's goblin bonus is `RacialGear.modifyDealtDamage`, called from `ProgressionEvents.modifyDamage`.
- The Longbow's `pull` / `pulling` model predicates are registered in `DndClassesClient` (with the Ranger's quick draw, like the vanilla bow).
- Kin stock: `entity/KinStock` (a merchant's kin-only offers, shown only to the settlement's home race) and `entity/KinStockOffers` (each settlement's list). The Speaker (`ElfMerchantEntity`), innkeeper (`InnkeeperEntity`) and Dragonborn Elder (`DragonbornElderEntity`) use it; dwarves roll `loot_tables/gameplay/dwarf_kin_stock.json` (`MountainDwarfEntity.KIN_STOCK_LOOT`). For the settlements still to come: `KinStockOffers.orcStronghold()` (Orcish Warplate) and `orcishGreataxe()`, `brimstoneSanctum()` (Infernal Regalia) and `cogsdeep()` (empty until the Gnome gear exists).
- Tags: `#dndclasses:rarity/<tier>` and `#dndclasses:racial_gear/<race>` (hooks for the magic items area's rarity and attunement-by-race); magic tiers are registered in `MagicItems.registerDefaults` (unidentified type `sling` for the Sling).
