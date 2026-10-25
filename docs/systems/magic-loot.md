# Magic loot
Where [magic items](magic-items.md) turn up in the world. Every source rolls on four rarity tables, which mix +1/+2/+3 gear with the named [magic items](../items/magic-items.md). Mod structures, bosses, Mimics, bounties and goblin raid rewards each get an extra magic pool, and so do some vanilla structures (dungeons, mineshafts, temples, strongholds, mansions, bastions, ancient cities and end cities). Random loot comes out unidentified and can be cursed. Bounty rewards come out identified.

## How it works
### The rarity tables
`dndclasses:magic/uncommon`, `magic/rare`, `magic/very_rare` and `magic/legendary` each roll one item. They're unidentified and get the tier's curse chance (Uncommon and Rare 10%, Very Rare 8%, Legendary none). An item with a built-in curse (The Burglar's Ring) is always cursed.

| Table | +N gear (weight) | Named items (weight) | Chance of a named item |
|-|-|-|-|
| Uncommon | +1 iron gear (swords and axes 8, armor 5), +1 diamond gear (3/2), bow 6, crossbow 4, trident 2 | Cloak of Protection 6, Wand of Magic Missiles 2, Immovable Rod, Bag of Holding, Decanter of Endless Water, Periapt of Wound Closure 1 each | 12 in 74 (16%) |
| Rare | +2 iron gear (4/3), +2 diamond gear (6/4), bow 6, crossbow 4, trident 2 | Ring of Protection 6; Staff of Ice, Fire and Lightning 2 each; Amulet of Health, Doss Lute, Warlord's Warhorn, Heart of the Furnace, Storm-Chaser Boots, Wyvern-Scale Cloak, Belt of Owlbear Might, Mace of Disruption, Gorgon's Gaze 1 each | 21 in 81 (26%) |
| Very Rare | +3 diamond gear (6/4), bow 6, crossbow 4, trident 2 | Frostbrand 2, Tome of Clear Thought 2, Phylactery Locket 1, The Burglar's Ring 1 | 6 in 46 (13%) |
| Legendary | +3 netherite gear (6/4), bow 4, crossbow 3, trident 3 | Eye of the Tyrant 6 | 6 in 44 (14%) |

Cloak of Protection and Ring of Protection only come from these tables. The other named items also have their own sources (below and in the [boss drops](../items/magic-items.md#where-to-find-it)).

### Extra pools
Each pool is one roll with the chance shown; "1 roll" means it always rolls. Where a pool lists several tables, the weights pick one.

| Source | Magic pool | Named items |
|-|-|-|
| [Goblin camp](../structures/goblin-camps.md) chest (`chests/goblin_camp`) | 25%: Uncommon | Wand of Magic Missiles 3%, The Burglar's Ring 1% ("stolen from a hobbit") |
| Goblin camp supply barrel (`chests/goblin_camp_supplies`) | 10%: Uncommon | |
| [Dwarven fortress](../structures/dwarven-fortresses.md) forge, mine, barracks, mead hall | 15%: Uncommon (75) / Rare (25) | forge: Immovable Rod 3%; barracks: Periapt of Wound Closure 3%; mead hall: Doss Lute 3% |
| Dwarven fortress treasury | 60%: Uncommon (50) / Rare (45) / Very Rare (5) | Bag of Holding 3%, Amulet of Health 3% |
| [Dragon lair](../structures/dragon-lairs.md) hoard | 1 roll: Rare (70) / Very Rare (30) | Storm-Chaser Boots 6% |
| [Frost lair](../structures/frost-lairs.md) hoard | 1 roll: Rare (70) / Very Rare (30) | Frostbrand 8% |
| [Beholder](../bosses/beholder.md) lair chest | 1 roll: Rare (40) / Very Rare (45) / Legendary (15) | Immovable Rod 4%, Eye of the Tyrant 2% |
| [Dragon Temple](../structures/dragon-temples.md) reliquary (`chests/dragon_temple_reliquary`) | 1 roll: Rare (90) / Very Rare (10) | its Staffs of Fire, Ice and Lightning stay at 4% each |
| Dragon Temple cell chests and storeroom barrels (`chests/dragon_temple_cell`) | 8%: Uncommon | |
| [Orc stronghold](../structures/orc-strongholds.md) yurt chests (`chests/orc_stronghold_yurt`) | 5%: Uncommon | |
| Orc stronghold armoury (`chests/orc_stronghold_armory`) | 15%: Uncommon (75) / Rare (25) | |
| Orc stronghold hoard (`chests/orc_stronghold_hoard`) | 60%: Uncommon (40) / Rare (50) / Very Rare (10) | |
| [Brimstone sanctum](../structures/brimstone-sanctums.md) Refuge and market (`chests/brimstone_sanctum_refuge`, `_market`) | 5%: Uncommon | |
| Brimstone sanctum den (`chests/brimstone_sanctum_den`) | 12%: Uncommon (80) / Rare (20) | |
| Brimstone sanctum offering chest (`chests/brimstone_sanctum_altar`) | 60%: Uncommon (40) / Rare (50) / Very Rare (10) | |
| [Cogsdeep](../structures/cogsdeep.md) Commons, observatory and workshops (`chests/cogsdeep_commons`, `_workshop`) | 5%: Uncommon | |
| Cogsdeep gem room (`chests/cogsdeep_gems`) | 12%: Uncommon (80) / Rare (20) | |
| Cogsdeep clocktower strongbox (`chests/cogsdeep_vault`) | 60%: Uncommon (40) / Rare (50) / Very Rare (10) | Gnomish Repeater 12%, Tinker's Goggles 12% (JSON) |
| [Hobbit village](../structures/hobbit-villages.md) harvest chests | | Decanter of Endless Water 2% |
| [Goblin Warlord](../bosses/goblin-warlord.md), [Magmamuncher Alpha](../bosses/magmamuncher-alpha.md), [Orc War Chief](../bosses/orc-war-chief.md) (player kill) | 50%: Rare | their trophies (boss drops) |
| [Lich](../bosses/lich.md) (final kill), [Beholder](../bosses/beholder.md) (player kill) | 1 roll: Very Rare (80) / Legendary (20) | Tome of Clear Thought 10% each, plus their trophies |
| [Medusa](../bosses/medusa.md) (player kill) | 1 roll: Rare | Gorgon's Gaze 25% (+5% per Looting level) |
| [Mimic](../mobs/mimic.md) (player kill, on top of its chest's loot) | 25%: Uncommon | Bag of Holding 10% |
| [River Pikehorn](../mobs/dragons.md) (player kill) | | Decanter of Endless Water 5% |
| Minor bounty spoils (`gameplay/bounty_minor`) | 10%: Uncommon, **identified** | Wand of Magic Missiles 5% (identified) |
| Major bounty spoils (`gameplay/bounty_major`) | 35%: Uncommon (60) / Rare (40), **identified** | Periapt of Wound Closure 3%, Doss Lute 3% (identified) |
| [Goblin raid](goblin-raids.md) rewards (every kind) | 20%: Uncommon | hobbit village raids: The Burglar's Ring 2% |
| Vanilla dungeon, mineshaft, desert pyramid, jungle temple chests | 8%: Uncommon | dungeon and mineshaft: Wand of Magic Missiles 2% |
| Vanilla stronghold library, woodland mansion, bastion treasure, ancient city chests | 20%: Rare (70) / Very Rare (30) | Scroll of Identify (1-2) 25%; stronghold library: Scroll of Remove Curse 10%, Amulet of Health 4%, Mace of Disruption 3%, Tome of Clear Thought 2% |
| Vanilla end city chests | 15%: Very Rare (85) / Legendary (15) | |
| [Dungeons](../structures/dungeons.md) | the [dungeon loot](dungeon-loot.md) tables pick the tier per room; the Hoard Coffer always gives one | |

- Identified rewards keep their tier and +N but lose any random curse (known goods). A built-in curse stays and is shown.
- Scrolls aren't unidentified: they're plain consumables.
- Monster pools need a player kill, like the other rare drops. The Lich's table drops from its smashed phylactery.

## Where to find it
Open the chests, kill the monsters and finish the bounties and raids above. In vanilla structures generated before this update, chests that haven't been opened yet roll the new pools too (loot is rolled when a chest is first opened).

## Commands
- `/loot give @s loot dndclasses:magic/rare` rolls a rarity table.
- `/loot give @s loot dndclasses:chests/beholder_lair` or `/loot give @s loot minecraft:chests/simple_dungeon` rolls a source with its magic pools.
- `/loot give @s kill @e[type=dndclasses:mimic,limit=1,sort=nearest]` rolls a mob's table as a player kill.
- Devscript: `devscripts/magic-loot-check.txt` rolls the beholder lair 20 times, the vanilla dungeon 30 times, the major bounty, the stronghold library and the Rare table, and logs the inventory with `slots`.

## Configuration
- The rarity tables are generated: edit `tools/magic_tier_tables.py` and run `python3 tools/magic_tier_tables.py` from the repo root.
- The extra pools are in `magic/MagicLoot.java`, except the Mimic's (`loot_tables/entities/mimic_magic.json`).
- A datapack that replaces a vanilla table doesn't get the pools. A datapack can override the rarity tables and `entities/mimic_magic`.

## Known limitations
- The pools are added to the mod's tables in code, so a datapack that replaces one of the mod's chest or mob tables still gets them.
- Chests that were already opened before the update don't refill.

## For developers
- `magic/MagicLoot`: the placement table (`placements()`: `chest`, `entity`, `gameplay`, `vanilla`, each with `tier(...)`, `named(...)` or `plain(...)` entries) and one `LootTableEvents.MODIFY` hook. Tier entries are `minecraft:loot_table` entries pointing at `dndclasses:magic/<tier>`; entity pools add `killed_by_player`. Vanilla tables are only extended when `source.isBuiltin()`.
- `MagicItemLootFunction.identified()` (JSON `{"function": "dndclasses:magic_item", "identified": true}` with no tier) identifies an already-rolled item: a stack that already has a tier keeps it and its +N, and a random curse is lifted. Bounty pools put it on the tier-table entry.
- `MimicEntity.dropLoot` rolls `entities/mimic_magic` after the chest table it holds.
- The boss trophies are still in `magic/items/BossItemDrops`.
- The dungeon tables (`tools/dungeon_loot.py`) reference the rarity tables, so they get the named items without changes.
- The old one-staff tables `chests/staff_of_{fire,ice,lightning}.json` are gone: the staffs are in the Rare table.
