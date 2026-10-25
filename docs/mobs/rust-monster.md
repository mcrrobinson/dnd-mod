# Rust Monster
A low, armadillo-like cave beast with a rust-orange shell, two long feathery antennae and a propeller of fins on its tail. Its antennae rust iron, chain and gold gear, metal weapons that hit it rust too, and it stops whatever it's doing to eat dropped iron. Dwarves hate it.

![A Rust Monster](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/feat-rust-monster/rust-monster-front.png)

## How it works

| Stat | Value |
|-|-|
| Health | 32 |
| Armor | 6 (chitin) |
| Bite | 4 |
| Speed | 0.30 |
| Follow range | 20 blocks |
| Size | 1.2 x 1.0 blocks |
| XP | 12 |

It only hunts players who carry **corrodible metal**: something from `#dndclasses:corrodible` worn or held, or more than 8 metal items (corrodible gear, iron or gold ingots, nuggets, raw iron or gold) in the inventory. A player in leather, wood, diamond or netherite is left alone, unless they hurt it first.

### Antenna touch
Every 2 seconds (40 ticks) that its target is within 2.5 blocks, it brushes them with its antennae: a **DEX saving throw, DC 12**. On a failure one random corrodible piece of armour rusts one step; with no corrodible armour on, a corrodible item in the main or off hand does instead. Mobs save silently with +1. A creative or spectator player is never touched.

### Weapons that hit it
A player's melee hit with a corrodible main-hand item rusts that item one step **50%** of the time, with no save. Arrows, thrown tridents and other projectiles are safe, and so is the damage from anything that isn't a direct swing.

### Corrosion
A corrodible item carries a corrosion level from 0 to 3 (NBT `dndCorrosion`). One step of rust does the first of these that applies:

1. **+N gear loses one point of its +N** ("The rust eats the enchantment on your +2 Iron Sword"). Corrosion levels only start once it's +0.
2. **At 3/3 it crumbles away** ("Your Iron Chestplate crumbles to rust"), with the item-break sound.
3. Otherwise its **corrosion level goes up by one** and it loses **15% of its max durability**. If that wears it out it crumbles instead.

Each level costs:

- **Armour:** -1 armor in its slot.
- **Swords, axes, pickaxes, shovels and hoes:** -1 attack damage in the main hand.
- **Shears, shields and horse armour:** only the durability.

The tooltip shows the damage in orange: "Corroded (2/3): -2 armor". An **attuned** item is immune. Named magic items (boss drops, wondrous items) aren't metal gear and never corrode.

**Repair:** repairing the item on an anvil with its repair material, or combining it with another of the same item, clears all of its corrosion. Renaming it doesn't, and neither does a grindstone. See [Armor](../items/armor.md#corrosion).

What corrodes (`#dndclasses:corrodible`):

- iron, chainmail and golden armour;
- iron and golden swords, axes, pickaxes, shovels and hoes;
- shears, shields, iron and golden horse armour;
- the Knight, Warrior, Holy Armor, Golden Horns and Steampunk class sets;
- the racial Dwarven Plate and Orcish Warplate, the Dwarven Waraxe and the Orcish Greataxe.

Diamond, netherite, leather, wood, stone and the other mod sets are safe.

### It eats metal
It notices dropped items from `#dndclasses:rust_monster_food` (everything corrodible, plus iron and gold ingots, nuggets, raw iron and raw gold) within 12 blocks that it can see, even mid-fight. It walks over and eats the whole stack in 1 second (20 ticks), crunching, and:

- heals **4 health per item** eaten, up to its maximum;
- is **sated for 10 seconds** (200 ticks): it drops its target and won't pick a new one, even someone who hits it.

Throwing it a few iron nuggets is the counterplay: it buys time to walk away or take off your armour.

### Dwarves
Mountain dwarves attack a Rust Monster on sight. Killing one gains **+3 reputation with the Mountain Dwarves** (the faction's `monster_kills`, counted towards its daily `kill_cap` of 30). See [Factions](../systems/factions.md).

### Death
Drops 3-8 iron nuggets (+0-1 per Looting level), and a 25% chance of raw iron when a player kills it.

### Tips
- Fight it in leather, or bring a stone, diamond or netherite weapon.
- A bow keeps your sword safe and keeps you out of reach of its antennae.
- Carry a handful of iron nuggets to throw at it.
- Rusted gear isn't lost: an anvil and one ingot clears all of it.

## Where to find it
- **Deep caves:** spawns as a monster anywhere in the Overworld (weight 4, 1-2 at a time), but only in the dark, out of sight of the sky and **below y=0**.
- **Dwarven Ruins:** in the dungeon's encounter pool at Challenge tiers 2 and 3 (weight 3) and tier 4 (weight 2), costing 3 threat. See [Dungeon encounters](../systems/dungeon-encounters.md).
- **DM toolkit:** the `rust_monster_nest` encounter (medium, 2 Rust Monsters).
- Its spawn egg is in the mod's creative tab.

## Commands
- `/summon dndclasses:rust_monster`
- `/summon dndclasses:rust_monster ~ ~ ~ {WeaponCorrodeChance:1.0f}`: every metal weapon that hits it rusts (for tests).
- `/item replace entity @s armor.chest with minecraft:iron_chestplate{dndCorrosion:2}` gives pre-rusted gear.
- `/data get entity @s Inventory[{Slot:102b}].tag.dndCorrosion` reads a worn chestplate's level.

## Known limitations
- Placeholder art and vanilla sounds (silverfish, turtle).
- Mobs' worn gear rusts too (a dwarf's, a skeleton's), but mobs with no corrodible gear just make the save for nothing.
- The Underdark spawn hook (`underdark_spawns/rust_monster.json`) is data only until the Underdark tickets read it.

## For developers
- `entity/RustMonsterEntity.java`: stats, goals, `carriesMetal` (the target filter), the antenna touch in `mobTick`, the weapon rust in `damage`, the Sated state (`sate`, NBT `SatedTicks`) and `canSpawn`. NBT `WeaponCorrodeChance` (float, default 0.5) is a test knob.
- Save: `RustMonsterEntity.TOUCH` (`MobSaveInfo` "Antenna touch", DEX 12), shown on the Study card. Study: `#dndclasses:lore/beasts`, weakness "throw it iron and it stops to eat".
- `entity/ai/goal/RustMonsterEatGoal.java`: food search (12 blocks), eating (20 ticks), healing and sating.
- `magic/Corrosion.java`: `level`, `setLevel`, `clear`, `canCorrode`, `corrode(holder, stack, slot)` (returns `IMMUNE`, `PLUS_LOST`, `CORRODED` or `CRUMBLED`), the attribute penalties (`ModifyItemAttributeModifiersCallback`) and `tooltipLine`. Other monsters can call `Corrosion.corrode` too.
- Anvil repair: `classes/mixin/AnvilScreenHandlerMixin.clearCorrosion` (`updateResult` TAIL).
- Dwarves: `MountainDwarfEntity.initGoals` (`ActiveTargetGoal<RustMonsterEntity>`); faction field `monster_kills` in `faction/Faction.java`, read by `FactionEvents.onKill`.
- Logs (INFO): `[Rust] <player> resists`, `[Rust] corrodes <item> (<level>/3)`, `[Rust] eats the +N on <item>`, `[Rust] <item> crumbles`, `[Rust] eats <count> <item>`. The touch and eating are also `[MobAction]` DEBUG lines.
- Model: `client/model/RustMonsterModel.java`, `client/renderer/RustMonsterRenderer.java` (1.2x scale). `geo/entity/rust_monster.geo.json`, `animations/entity/rust_monster.animation.json` (`idle`, `walk`, `antenna_touch`, `eat`) and `textures/entity/rust_monster.png` are generated by `tools/rust_monster_model.py`; edit the script and rerun it.
- Data: `loot_tables/entities/rust_monster.json`, item tags `corrodible` and `rust_monster_food`, `encounters/rust_monster_nest.json`, `dungeon_encounters/dwarven_ruin.json`, `underdark_spawns/rust_monster.json` (weight 4, 1-2, zones `["any"]`), spawns in `world/gen/ModSpawns.java`.
- Devscript: `devscripts/rust-monster.txt` (looks, the touch with forced saves, crumbling, a +2 sword, eating, a dwarf, the anvil).
