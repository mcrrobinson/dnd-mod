# Magic items
Items have a D&D rarity, shown by the colour of their name: Common, Uncommon, Rare, Very Rare and Legendary. Generic magic weapons and armor (+1, +2, +3) are the everyday magic loot, and loot from chests and monsters can come out unidentified.

![Tooltips of the Staff of Ice (Rare, Wizard only), a +2 Diamond Sword and an Unidentified Sword](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/magic-foundation/magic-tooltips-weapons.png)

This page covers rarity tiers, item magic data and +N gear. Attunement, identification flows, curses and the named magic items come in later updates.

## How it works
### Rarity tiers
| Tier | Name colour | +N gear bonus |
|-|-|-|
| Common | white `#FFFFFF` | none |
| Uncommon | green `#1EFF00` | +1 |
| Rare | blue `#0070DD` | +2 |
| Very Rare | purple `#A335EE` | +3 |
| Legendary | orange `#FF8000` | +3 (placeholder until the named Legendary items exist) |

- The colour shows everywhere the item's name does: tooltips, the name that pops up over the hotbar, chat hovers and death messages. It replaces vanilla's yellow/aqua/purple rarity colours for these items.
- Under the name, a grey italic line gives the tier and kind, D&D style: `Rare weapon (Wizard only)`, `Uncommon armor`, `Uncommon potion`. Items that need attunement will read `(requires attunement)` or `(requires attunement by a Wizard)`.
- Mundane items (vanilla gear, music discs, spawn eggs, the guidebook, Water/Awkward/Mundane/Thick potions) have no tier and look as before. Enchanting vanilla gear doesn't make it magic.

### Tiers of the existing items
| Item(s) | Tier | Class restriction |
|-|-|-|
| [Staff of Ice, Staff of Lightning, Staff of Fire](../items/staffs.md) | Rare weapon | Wizard only (no attunement, so a Wizard can carry all three) |
| [Monk's Staff](../items/staffs.md#monks-staff) | Common weapon | |
| [Lute, War Drum, Flute](../items/bard-instruments.md) | Common wondrous item | |
| Ale | Common consumable | |
| [The 14 class armor sets](../items/armor.md) (all 56 pieces) | Uncommon armor | |
| [Potion of Freezing, Potion of Arrow Storm](../items/potions-and-brewing.md) | Uncommon potion | |
| Potion of Invulnerability | Rare potion | |
| Vanilla potions with an effect | Common; Uncommon for II or extended ones | |

Splash and lingering potions count the same. Healing II is renamed **Potion of Greater Healing** (and Splash/Lingering Potion of Greater Healing).

### +1 / +2 / +3 gear
Any sword, axe, trident, bow, crossbow or armor piece can be magic gear:

| Gear | Bonus per +1 | At +3 |
|-|-|-|
| Sword, axe, trident | +1 attack damage in the main hand | +3 attack damage |
| Bow, crossbow | +10% arrow damage | +30% |
| Armor piece | +1 armor in its slot | +3 armor and +1 armor toughness |

- The name gets the bonus in front: "+2 Diamond Sword", "+1 Iron Chestplate". A renamed item keeps its custom name (still coloured).
- The bonus shows as its own blue line in the attribute list, e.g. `+2 Attack Damage` under the sword's base damage.
- A +N melee weapon's bonus is added to the modifier shown on [attack rolls](d20-skill-checks.md) (d20 + class bonus + N). Crits and fumbles are still only on natural 20 and 1.
- No attunement is needed.

### Unidentified items
A stack can be unidentified. It's then named `Unidentified <type>` in its rarity colour (you can sense its power): Unidentified Sword, Axe, Bow, Crossbow, Trident, Helmet, Chestplate, Leggings, Boots, Staff, Potion, Instrument, Wondrous Item, and so on. Its tooltip says "Unidentified. Its magic sleeps." and nothing else about its magic.

While unidentified the item only works as its base item: a +2 sword hits like a plain sword. `/dndmagic identify` wakes it up. Wizards identifying on pickup, the Scroll of Identify and paying for identification come in a later update.

## Where to find it
- The tier loot tables `dndclasses:magic/uncommon`, `magic/rare`, `magic/very_rare` and `magic/legendary` each give one unidentified +N item of that tier (iron/diamond swords, axes and armor, bows, crossbows and tridents; Legendary uses netherite). Nothing references them yet: chests, bosses and dungeons start pulling from them in later updates.
- Try one with `/loot give @s loot dndclasses:magic/rare`.

## Commands
Operator only (permission level 2).

| Command | What it does |
|-|-|
| `/dndmagic give <player> <item> [tier] [plus] [unidentified]` | Gives an identified magic item. `tier` is `common`, `uncommon`, `rare`, `very_rare` or `legendary` (overrides a registered item's tier); `plus` is 0-3 and only applies to weapons and armor (default 0). Add `unidentified` to give it unidentified. Example: `/dndmagic give @s minecraft:diamond_sword rare 2` |
| `/dndmagic identify <player>` | Identifies the item in the player's main hand |
| `/dndmagic info <player>` | Prints the main-hand item's tier, identified flag, +N and raw magic data |

## Known limitations
- The item's true identity is in its NBT, which the client can see, so a modified client could read unidentified items.
- Tipped arrows have no tier.
- Legendary +3 netherite gear stands in for the named Legendary items until they're added.

## For developers
- Package `mattonfire.dnd.magic`:
  - `MagicTier`: the 5 tiers, their colours and default +N.
  - `MagicKind`: the tooltip category (weapon, armor, potion, ring, wand, wondrous item, ...).
  - `MagicItemDef` and `MagicItems`: the Java item table (`registerDefaults()` for the existing items) and `MagicItems.info(stack)`, which resolves a stack's tier, kind, attunement, classes and unidentified type from its def, NBT and item class (potions are tiered by strength).
  - `MagicData`: reads and writes the stack sub-compound `dndclasses_magic` with `tier`, `identified`, `curse`, `curseKnown`, `uuid`, `attunedTo`, `plus` and `charges`. `isDormant` / `activePlus` are where the attunement ticket adds bond, class and anti-magic checks.
  - `MagicGear`: +N attribute modifiers through Fabric's `ModifyItemAttributeModifiersCallback`, the arrow damage multiplier and the attack-roll bonus.
  - `MagicNames`: the coloured / "+N" / "Unidentified" name and the tooltip line.
  - `MagicItemLootFunction`: the loot function `dndclasses:magic_item`.
- `mixin/ItemStackNameMixin` (`getName` RETURN) applies `MagicNames.decorateName`. Vanilla wraps `getName()` in the rarity colour, and the colour set on our text wins.
- The tooltip line is added in `Client/DndClassesClient` through `ItemTooltipCallback`.
- Bow damage: `mixin/BowItemMixin` (wraps `World.spawnEntity` in `onStoppedUsing`); crossbows: `mixin/CrossbowItemMixin` (`createArrow` RETURN). Attack roll: `SkillChecks/AttackRolls`.
- Loot function JSON:
  ```json
  { "function": "dndclasses:magic_item", "tier": "rare", "plus": 2, "identified": false, "curse_chance": 0.1, "theme": "crypt" }
  ```
  All fields are optional. `rarity` is accepted as an alias for `tier`. `plus` defaults to the tier's +N for weapons and armor. `identified` defaults to false. `curse_chance` is read but not rolled yet (curses ticket). `theme` is for dungeon themes and isn't used yet.
- Commands: `Commands/MagicCommand`.
- Devscript: `devscripts/magic-tiers-check.txt` gives one of each, checks `/attribute` damage and screenshots the tooltips with the DevScript `tooltips` step.
