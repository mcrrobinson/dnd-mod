# Magic items
Items have a D&D rarity, shown by the colour of their name: Common, Uncommon, Rare, Very Rare and Legendary. Generic magic weapons and armor (+1, +2, +3) are the everyday magic loot, and loot from chests and monsters can come out unidentified.

![Tooltips of the Staff of Ice (Rare, Wizard only), a +2 Diamond Sword and an Unidentified Sword](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/magic-foundation/magic-tooltips-weapons.png)

This page covers rarity tiers, item magic data, +N gear and attunement. Identification flows, curses and the named magic items come in later updates.

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
- A +N melee weapon's bonus is added to the modifier shown on [attack rolls](d20-skill-checks.md) (d20 + the sheet's attack bonus + N). Crits and fumbles are still only on natural 20 and 1.
- No attunement is needed.

### Unidentified items
A stack can be unidentified. It's then named `Unidentified <type>` in its rarity colour (you can sense its power): Unidentified Sword, Axe, Bow, Crossbow, Trident, Helmet, Chestplate, Leggings, Boots, Staff, Potion, Instrument, Wondrous Item, and so on. Its tooltip says "Unidentified. Its magic sleeps." and nothing else about its magic.

While unidentified the item only works as its base item: a +2 sword hits like a plain sword. `/dndmagic identify` wakes it up. Wizards identifying on pickup, the Scroll of Identify and paying for identification come in a later update.

### Attunement
Strong magic items only work once you've **attuned** to them: bonded yourself to the item. On anyone else it's just its base item.

![The Attunement Table's Items tab: bonds, carried magic items and the Attune button](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/attunement/attune-tab-after.png)

| Rule | Value |
|-|-|
| Bonds | **3** per player. Artificers get 4 from class level 5 and 5 from class level 10 (Magic Item Adept). Bonds are the character's, not the class's |
| Where | the **Items** tab of an Attunement Table, or one item per short rest |
| Time | 3 seconds at the table, with a channel bar and enchanting glyphs. Moving more than 8 blocks from the table, or the item leaving its inventory slot, cancels it |
| Class | an item made for certain classes (`requires attunement by a Cleric or Paladin`) can only be attuned by them. A **Thief** Rogue ignores this (Use Magic Device) |
| Ownership | an item bonded to someone else can't be attuned ("That item is attuned to Steve.") |
| Attuning | identifies the item (and will reveal a curse, once curses exist) |
| Where it must be | weapons and staffs: in the main hand. Armor: worn. Wondrous items (cloaks, rings, amulets, belts, wands): anywhere in your inventory, including the offhand |
| Unattune | free at the table. A cursed bond can't be ended (curses update) |
| Lost items | the bond stays when the item drops on death or goes into a chest, and works again when you pick it up. If it's gone for good, the tab lists the bond as **missing** with a **Release** button |
| Death and relog | bonds are kept |
| Anti-magic | in a Beholder's anti-magic cone every attuned item's magic is off (its bonuses come off at once and return within a second after the effect ends), and you can't attune |

The 4th attune (with 3 slots) is refused: "You can only be attuned to 3 items."

Tooltips show `Attuned` in aqua on your own bonded items, and `Attuned to <player>` in grey on someone else's.

**The Items tab** (only at a table, next to Tree and Bestiary):
- **Bonds N/3**: one row per slot, with the bonded item's name and `carried`, `missing` or `cursed`. **Unattune** (carried) or **Release** (missing) ends the bond.
- **Carried magic items**: every magic item in your inventory except potions and drinks, with **Attune** when you can attune to it, or its state (`Attuned`, `no attunement`, `unidentified`, `not your class`, `someone's`).
- The footer shows the channel bar while attuning.

**Short rest**: finishing a [short rest](rests.md) attunes one item for free: the main hand's, else the offhand's, else the first carried item that needs it and can be attuned ("While you rest, you study your gear.").

### Attunement items
| Item | Tier | Effect while attuned |
|-|-|-|
| Cloak of Protection | Uncommon wondrous item | +1 armor and +1 armor toughness (anywhere in the inventory). The +1 to saving throws comes with the saving throws update |

The full set of named items (Ring of Protection, Frostbrand, ...) comes in a later update; the Cloak is the first.

### Blessing of the Forge
A [Forge Domain Cleric](../classes/cleric.md)'s subclass feature. Once per long rest, at an Attunement Table's Items tab, **Bless +1** makes the weapon in your hand (main or offhand) or a piece of armor you wear +1 (a mundane item becomes a green "+1 Iron Sword"; a +2 item becomes +3; +3 and unidentified items can't be blessed). Chat says `Blessing of the Forge: [Iron Sword] is now +1 until your next long rest.` and the tooltip `Blessing of the Forge: +1 until a long rest`.
- The blessing belongs to the Cleric's rest. The item remembers who blessed it, and once a second any blessed item an online player carries loses the +1 ("The Blessing of the Forge on Iron Sword fades.") if its Cleric is online and has had a long rest since, or isn't a Forge Cleric any more. While the Cleric is offline the item keeps it.
- With the `dndRests` gamerule off there are no long rests: the blessing is once per in-game day and lasts until the next day (an admin `/dndclass rest <player> long` still ends it).

## Where to find it
- The tier loot tables `dndclasses:magic/uncommon`, `magic/rare`, `magic/very_rare` and `magic/legendary` each give one unidentified +N item of that tier (iron/diamond swords, axes and armor, bows, crossbows and tridents; Legendary uses netherite). Nothing references them yet: chests, bosses and dungeons start pulling from them in later updates.
- Try one with `/loot give @s loot dndclasses:magic/rare`.

## Commands
Operator only (permission level 2).

| Command | What it does |
|-|-|
| `/dndmagic give <player> <item> [tier] [plus] [unidentified]` | Gives an identified magic item. `tier` is `common`, `uncommon`, `rare`, `very_rare` or `legendary` (overrides a registered item's tier); `plus` is 0-3 and only applies to weapons and armor (default 0). Add `unidentified` to give it unidentified. Example: `/dndmagic give @s minecraft:diamond_sword rare 2` |
| `/dndmagic identify <player>` | Identifies the item in the player's main hand |
| `/dndmagic info <player>` | Prints the main-hand item's tier, identified flag, +N, whether it's active for the player, and raw magic data |
| `/dndmagic attune <player>` | Attunes the player to their main-hand item, with no table or channel (the slot, class and ownership checks still apply) |
| `/dndmagic release <player> [all\|<n>]` | Ends the bond with the main-hand item, every bond, or bond `n` from `/dndmagic bonds`. Ignores the table and curses |
| `/dndmagic bonds <player>` | Lists the player's bonds (`carried` or `missing`, `cursed`) and slots |

## Known limitations
- The item's true identity is in its NBT, which the client can see, so a modified client could read unidentified items.
- Tipped arrows have no tier.
- Legendary +3 netherite gear stands in for the named Legendary items until they're added.
- Anti-magic turns off attunement items, but not the attribute bonus of +N gear (no attunement), which comes from the item itself and doesn't know who holds it. The +N attack-roll and arrow bonuses also stay on.
- A bond released while its item was away stays written on the item until the owner carries it again (then it's cleared). Until then, someone else can only attune to it while the owner is online.
- Two of the same attunement item don't stack.
- The Cloak of Protection's texture is a placeholder.

## For developers
- Package `mattonfire.dnd.magic`:
  - `MagicTier`: the 5 tiers, their colours and default +N.
  - `MagicKind`: the tooltip category (weapon, armor, potion, ring, wand, wondrous item, ...).
  - `MagicItemDef` and `MagicItems`: the Java item table (`registerDefaults()` for the existing items) and `MagicItems.info(stack)`, which resolves a stack's tier, kind, attunement, classes and unidentified type from its def, NBT and item class (potions are tiered by strength).
  - `MagicData`: reads and writes the stack sub-compound `dndclasses_magic` with `tier`, `identified`, `curse`, `curseKnown`, `uuid` (the bond id), `attunedTo` / `attunedName`, `plus`, `forgeBlessing` and `charges`. `isDormant` (unidentified) and `activePlus` (+N plus a Forge blessing, at most +3) need no player.
  - `Attunement`: the player's bonds, in persistent data under `dndAttunement` (`[{uuid, item, name, cursed}]`; `ClassLifecycle` copies all persistent data on death and End exit). `isActive(player, stack)` = identified, bonded if it needs attunement, class allowed (Thief: `rogue.thief`), no `AntiMagicEffect`. `slots(player)`, `refusal`, `attune`, `release`, the 3 s channel (server tick, `Progression.atAttunementTable`), the short-rest attune on `RestEvents.AFTER_REST`, and the packets `attune_item {slot}`, `unattune_item {uuid}`, `forge_bless {slot}` and `attunement_sync` (`AttunementSnapshot`, sent from `Progression.sync`).
  - `MagicEffect` / `MagicEffects`: per-item effect hooks with the `ClassSkills` shape (`attributeBonuses`, `modifyDealtDamage`, `modifyTakenDamage`, `secondTick`, `onKill`), dispatched to the player's active, in-place items from `ProgressionEvents`, right after the class hooks. Register with `MagicEffects.register(item, effect)`; the Cloak of Protection in `Magic.registerEffects` is the example.
  - `ForgeBlessing`: the Forge Domain Cleric's blessing (`cleric.forge`): `forgeBlessing`, `forgeBy` (the Cleric) and `forgeKey` (`rest:<long rests so far>`, plus `/day:<day>` with rests off) on the item, the used key `dndForgeBlessingUsed` and the long-rest count `dndForgeLongRests` in the Cleric's persistent data, and a once-a-second check that ends stale blessings.
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
- The Items tab: `Client/Hud/MagicItemsTab`, drawn by `SkillTreeScreen` when opened from a table.
- Commands: `Commands/MagicCommand`.
- Devscripts: `devscripts/magic-tiers-check.txt` gives one of each, checks `/attribute` damage and screenshots the tooltips with the DevScript `tooltips` step. `devscripts/attunement.txt` checks attunement end to end (the tab, the cap, anti-magic, death, short rest, the Forge blessing, Thief, Artificer slots), then `devscripts/attunement-relog.txt` checks the bond after a relog.
