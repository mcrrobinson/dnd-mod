# Duergar
The Grey Clans: grey dwarves of the Underdark's Grey Reaches who grow to giant size mid-fight and turn invisible when they're hurt. They're surly but they trade, gold for Underdark goods. Most players start Unfriendly with them; Dwarves start Hostile, the old grudge between the clans.

## How it works
| Duergar | Health | Armour | Damage | Speed | Carries | Special |
|-|-|-|-|-|-|-|
| Soldier | 30 | 6 | 6 | 0.25 | a war pick (iron pickaxe); an iron helmet 30% of the time | Enlarge, Invisibility |
| Arbalester | 24 | 5 | 4 (hand axe) | 0.25 | a crossbow and an iron axe in the off hand | Heavy crossbow: loads, holds for 1-2 s and shoots for 7 at anything it fights within 20 blocks it can see, walking closer otherwise; chops with the axe within 2.5 blocks (once a second). Enlarge, Invisibility |
| Factor | 30 | 4 | 3 | 0.23 | a gold ingot | Merchant (role `duergar_factor`). Never Enlarges; Invisibility |
| Taskmaster | 40 | 7 | 5 (whip, 2.5 blocks more reach) | 0.25 | a whip (a lead) and an iron helmet | Drive. Enlarge, Invisibility |

Duergar are the [mountain dwarf](mountain-dwarves.md) model (0.7 x 1.4 blocks) with grey skins: ash-grey and bald, black or grey beards, soot-dark plate with red-hot rivets. The Factor wears an apron and a gold chain, the Taskmaster a red sash and a spiked iron cap. Each has a name such as "Thrum Ashforge" and sounds like a deep vindicator. They're members of **the Grey Clans** [faction](../systems/factions.md).

### Racial traits
- **Enlarge** (Soldiers, Arbalesters and the Taskmaster): once per fight, when one first takes damage or its target comes within 6 blocks, it grows to **1.5x** for **15 s**: a 1.05 x 2.1 hitbox, **+4 attack damage**, **+30% knockback resistance** and **+1 block reach**. It grows with a puff of smoke and eases up to size over half a second. It doesn't grow where there's no room.
- **Invisibility**: once per fight, when one drops below half health, it turns **invisible for 6 s** and backs off about 8 blocks. Mobs fighting it lose it as a target, and it picks no target itself for 3 s, then comes back for whoever it was angry at.
- A fight ends 10 s after a duergar last had a target; then Enlarge and Invisibility are ready again.
- **Duergar Resilience**: Poison (and any effect with "poison" in its id, such as the drow's) lasts half as long on them.
- **Sunlight Sensitivity**: under the open sky by day (sky light 12+), Weakness I and Slowness I, refreshed every second. Never in the Underdark, which has no sun.
- **Drive** (the Taskmaster): in a fight, every 20 s, every duergar within 12 blocks (the Taskmaster too) gets Speed I and Strength I for 8 s.

### Who they fight
Duergar are guards, not monsters:
- Players **Hostile** with the Grey Clans, on sight. A Dwarf starts there (-550).
- Anyone who strikes a duergar: it and every duergar within 16 blocks turn on them for 30-50 s. Hitting one costs 10 standing.
- The clans' enemies in `#dndclasses:duergar_enemies`: the [drow](drow.md). Every drow you kill gives +15 with the clans.
- Never each other (no friendly fire), creative or spectator players, or anyone on Peaceful.

Players at Unfriendly or better (most start at -250) are left alone unless they start something.

### The Grey Clans
| | |
|-|-|
| Id | `dndclasses:duergar_clans` |
| Start | -250 (Unfriendly); Gnomes -400 (Unfriendly); Dwarves -550 (Hostile) |
| Rivals | Mountain Dwarves -0.5, the drow -0.25, Tinkers' Guild -0.25: gaining standing with the clans (trading) costs a share with them |
| Hit a duergar | -10 |
| Kill a duergar | -60; the Taskmaster -150 |
| Trade with the Factor | +1 a trade, up to +15 a day |
| A duergar sees a theft in a [Hold](../structures/duergar-holds.md#the-law) | -40 |
| Grudge decay | 5 a day |

Killing a duergar gives **+30 Mountain Dwarves**, within the dwarves' daily cap of +30 from kills (shared with goblin kills), so the first duergar each day counts.

### Trades
The Factor trades with anyone who isn't Hostile with the Grey Clans; a Hostile customer gets "&lt;name&gt; spits at your feet. The Grey Clans don't deal with your kind." on the action bar. Each trade has 12 uses and restocks every morning. Prices follow your standing (Unfriendly +50%, Friendly -10%, Honored -25%, Exalted -40%). There are no kin prices: Dwarves get no discount here.

| Sells | Price |
|-|-|
| 16 Zurkhwood Planks | 3 emeralds |
| 8 Faerzress Stone | 4 emeralds |
| 16 [Basalt Forge Bricks](../items/underdark-blocks.md#building-blocks-drow-and-duergar) | 4 emeralds |
| a Duergar Steel Ingot | 8 emeralds |
| Friendly and up: 2 Greysteel Plate | 12 emeralds |
| Friendly and up: a Potion of Invisibility | 18 emeralds |
| Honored and up: the Duergar War Pick (iron pickaxe, Efficiency II, Sharpness I) | 20 emeralds |

| Buys | For |
|-|-|
| 4 gold ingots | 1 emerald |
| 12 raw iron | 1 emerald |
| 1 [Darklake Pearl](../items/underdark-blocks.md) | 8 emeralds |
| 8 Spidersilk (from the [drow](drow.md)) | 2 emeralds |

So a Human (Unfriendly) pays 5 emeralds for the planks and 6 gold ingots per emerald.

### Drops
| Duergar | Drops |
|-|-|
| Soldier | 0-2 iron nuggets, 0-1 gold nugget; rarely its pick or helmet; 3% (killed by a player, +1% per Looting level) a Duergar Steel Ingot |
| Arbalester | 0-3 arrows, 0-2 iron nuggets; 8% its crossbow, 5% its axe |
| Factor | 1-3 gold nuggets, 0-2 emeralds |
| Taskmaster | 2-4 iron nuggets, 1-3 gold nuggets, 0-1 gold ingot |

## Where to find it
- Patrols of 2-3 Soldiers in the Underdark's **Grey Reaches** (biome tag `#dndclasses:habitat/duergar`), spawning like monsters in the dark (weight 8), at most 16 duergar within 48 blocks. Natural ones despawn like monsters.
- [Duergar Holds](../structures/duergar-holds.md) house all four, persistent: 6-9 soldiers, 2-3 arbalesters, the factor and the Taskmaster.
- The factor has the `duergar_factor` [dialogue](../systems/dialogue.md) role: the [Shadows Below](../systems/quests.md#launch-quest-chains) chain has you win him over and then hunt drow for him (Drow House Sigils).
- Spawn eggs for all four are in the creative tab.

## Commands
- `/summon dndclasses:duergar_soldier` (also `duergar_arbalester`, `duergar_factor`, `duergar_taskmaster`). Summoned duergar stay and keep within 16 blocks of where they appeared.
- `/rep <player> dndclasses:duergar_clans set <value>`

## Known limitations
- They don't glower or shake their heads at players yet; Unfriendly players are simply ignored.
- Duergar Resilience halves poison status effects only; the Poisoned condition lasts its full time.
- Stock from other Underdark tickets shows up only once its item exists (see For developers).

## For developers
- `entity/DuergarEntity` (base on `SettlementNpcEntity` + `GuardAnger`: variant, name, home, Enlarge with the tracked `ENLARGED` flag, `getDimensions` x1.5 and fixed-UUID modifiers `ENLARGE_DAMAGE_ID` / `ENLARGE_KNOCKBACK_ID`, Invisibility retreat, Resilience in `addStatusEffect`, Sunlight Sensitivity, `DuergarMeleeGoal` with the extra reach), `DuergarSoldierEntity`, `DuergarArbalesterEntity` (`CrossbowUser`, its own load-hold-shoot goal, bolt damage set per shot), `DuergarFactorEntity` (`NpcMerchant`, two `KinStock`s gated by tier), `DuergarTaskmasterEntity` (Drive), `DuergarTrades`.
- `entity/DuergarEntities` registers everything (types, attributes, spawn eggs, Study weakness, the Grey Reaches spawn and `canDuergarSpawn`) from one `register()` call in `DnDClasses`, so parallel Underdark tickets touch the shared registries as little as possible.
- `client/renderer/DuergarRenderer` (scales by `DuergarEntity.renderScale`, crossbow arm poses) on `MountainDwarfRenderer.DwarfModel`, now generic. Skins: `tools/duergar_skins.py`.
- Data: `factions/duergar_clans.json`, `tags/entity_types/faction/duergar_clans.json`, `tags/worldgen/structure/faction/duergar_clans.json` (an optional `dndclasses:duergar_hold` entry for T8), `tags/entity_types/duergar_enemies.json` (optional drow entries for T4), the `enemy_kills` entry in `factions/mountain_dwarves.json`, `loot_tables/entities/duergar_*.json`, `dialogue/duergar_factor.json` (greetings by tier, for quests later), `lore/humanoid` for Study.
- **Hooks for other tickets**: `DuergarTrades.LATER_STOCK` offers Darklake Pearls (buys 1 for 8, T3) and Spidersilk (buys 8 for 2, T4), each only once `dndclasses:<id>` is registered; the building-block stock comes from `UnderdarkBuildingStock` (see [Underdark blocks](../items/underdark-blocks.md#for-developers)). Saved factors pick them up on load. The maps to the Rift and to a Drow Outpost wait for those items. A Hold's spawn override can use `DuergarEntities.canDuergarSpawn`.
- `[MobAction] <name> enlarge|invisibility|drive xN -> <target>` at DEBUG level in `debug.log`.
- Devscript: `devscripts/duergar.txt` (Enlarge and its hitbox, Invisibility, hostility by tier, the factor's prices and refusal, kill reputation, sunlight).
