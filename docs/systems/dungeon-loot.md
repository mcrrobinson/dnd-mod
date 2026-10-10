# Dungeon loot
Dungeon rewards scale with the dungeon's Challenge tier. Locked chests along the way hold tier loot. Veterans drop an extra roll. The treasure vault's **Hoard Coffer** gives every player their own roll once the boss is dead, so the party never has to split drops. A cleared dungeon repopulates after 7 in-game days, and a repeat clear pays a smaller "salvage" roll.

## How it works
### Room chests
| Room | Table | What's in it |
|-|-|-|
| Encounter rooms (one chest in a corner) | `dndclasses:chests/dungeon/<theme>_encounter_t<tier>` | 3-5 rolls of supplies: bread, arrows, torches, iron, gold nuggets, emeralds, Healing potions, theme items; from II a tier enchanted book, from III diamonds |
| Secret room | `<theme>_secret_t<tier>` | 4-6 rolls of treasure (below) plus theme items, and a 15/20/25/30% chance (by tier) of a magic item of the tier's lowest vault rarity |
| Side vault | `<theme>_side_vault_t<tier>` | 4-6 rolls of the next tier's treasure, and a 50% chance of a magic item at the next tier's vault rarity |

`<theme>` is `crypt`, `warren` or `ruin`. Theme items: Crypt has bones, rotten flesh, candles, soul torches and name tags. Warren has leather, string, mutton, rabbit hide and saddles. Ruin has raw iron and gold, coal, lanterns and iron nuggets.

**Treasure** (per tier *t*): gold ingots, emeralds, diamonds, Healing (Strong Healing from III), Fire Resistance (Regeneration at I), bottles o' enchanting, golden apples, and enchanted books with treasure enchantments at level 20/25/30/30. Stack sizes grow 50% per tier. Tier IV adds enchanted golden apples and netherite scrap.

All dungeon chests start **locked** (see [D20 checks](d20-skill-checks.md)). The tier in the table name sets the Rogue's lock DC:

| Tier | I | II | III | IV |
|-|-|-|-|-|
| Lock DC | 12 | 13 | 15 | 17 |
| Side vault | 14 | 15 | 17 | 19 |

8% of dungeon chests are Mimics (15% in the Dwarven Ruin), and they hold that chest's loot.

### Veterans
A Veteran (an elite encounter mob) that a player kills drops one extra roll from `dndclasses:gameplay/dungeon_elite_t<tier>`: gold nuggets or ingots, emeralds, arrows, iron, Healing, and from II a diamond. From III there's a 2% (III) or 4% (IV) chance of an Uncommon magic item. Nothing extra drops for mobs killed without a player, or with `doMobLoot` off.

### The Hoard Coffer
A dark-oak, gold-banded coffer on the plinth in the treasure vault. It can't be broken, and it isn't a container, so hoppers can't drain it.
- **Before the boss dies** it stays shut: "The Hoard Coffer is sealed until the dungeon's master falls".
- **First use after a clear**: the player gets `chests/dungeon/vault_t<tier>` straight into their inventory (what doesn't fit drops at their feet). That's 4-6 treasure rolls and **one guaranteed magic item** from the [magic item](magic-items.md) tables. Like other random loot, it's unidentified.

| Tier | Vault magic item |
|-|-|
| I | Uncommon |
| II | Uncommon 70% / Rare 30% |
| III | Rare 70% / Very Rare 30% |
| IV | Very Rare 90% / Legendary 10% |

- **Again in the same clear**: nothing ("You've had your share...").
- **After the dungeon repopulates and is cleared again**: players who already had their roll get `chests/dungeon/vault_salvage` once per clear. That's 2-3 rolls of gold, emeralds, iron, arrows, bottles o' enchanting and Healing. From II it adds a chance of a diamond or an enchanted book, and from III a 25% chance of an Uncommon magic item. Players who never had a roll get the full vault roll.

Claims are stored per dungeon and per player (UUID), so they survive restarts, the coffer's chunk unloading and `/dungeon reset`.

### Repopulation
A cleared dungeon repopulates **7 in-game days** (168000 ticks) after its clear, counted on the game clock or the day clock, whichever is further along (so sleeping and `/time add` count). It also waits until nobody has been inside for **30 s**. Every room goes back to untouched and the boss comes back. Coffer claims stay. `/dungeon reset` repopulates straight away.

## Where to find it
In every dungeon (see [Dungeons](../structures/dungeons.md)): a chest in each encounter room, one in the secret room and the side vault, and the Hoard Coffer in the treasure vault behind the boss room. Dungeons built before this feature still have the old placeholder chests.

## Commands
- `/dungeon info` adds how many players have had a coffer roll and, once cleared, how many ticks until it repopulates.
- `/dungeon reset` repopulates the dungeon now (fires the repopulated event).
- `/dungeon clear` clears it, which opens the coffer.
- Test a table: `/loot give @s loot dndclasses:chests/dungeon/vault_t3`. Run it inside a dungeon for `dndclasses:dungeon_tier` conditions to see the dungeon's tier.
- Place a coffer by hand: `/setblock ~ ~ ~ dndclasses:hoard_coffer`. It finds its dungeon from where it stands.

## Configuration
None. The tables are generated: edit `tools/dungeon_loot.py` and run `python3 tools/dungeon_loot.py` from the repo root.

## Known limitations
- The `theme` bias of `dndclasses:magic_item` isn't used yet. Vault magic items come from the generic `dndclasses:magic/<rarity>` tables.
- Every player gets a guaranteed magic item from the coffer (a balance question for the magic items area).
- The Crypt's Staff of Ice and the Ruin's Staff of Lightning vault chances come with their theme tickets.
- The coffer looks the same whether it's shut or open. Its state is only shown when you use it.

## For developers
- `dungeon/DungeonLoot`: table ids (`chestTable`, `vaultTable`, `salvageTable`, `eliteTable`), `lockDc(table)` (the prefix rule `Lockpicking.dcFor` uses), `roll(...)` and the Veteran drop listener. `register()` also registers the loot condition and the repopulation timer.
- `dungeon/DungeonTierLootCondition`: `{"condition": "dndclasses:dungeon_tier", "min": 2, "max": 4}`. It reads the tier of the recorded dungeon at the roll's `origin` (tier I outside one).
- `dungeon/DungeonState`: `claimCoffer(uuid)` / `cofferClaimFor(uuid)` → `LOCKED`, `FULL`, `SALVAGE` or `NONE`. Saved as `Coffer: [{Player, Clear}]`. A player gets a salvage roll when the clear count has moved on since their last claim. Also `ClearedAtDay`.
- `dungeon/DungeonRegistry`: `repopulatesIn(world, state)`, `repopulate(world, state)` and the timer (checked every 100 ticks). `DungeonEvents.REPOPULATED` fires on every repopulation, so traps and puzzles can re-arm on it.
- `classes/Blocks/HoardCofferBlock` + `HoardCofferBlockEntity` (NBT `StartKey`, `Dungeon`), registered in `ModBlocks`. Worldgen places it through `DungeonPiece.Builder.hoardCoffer`. Rooms pick their chest table with `DungeonPiece.loot("encounter" | "secret" | "side_vault")`.
- Logs: `[DungeonLoot] <player> took a FULL|SALVAGE roll ...`, `[DungeonLoot] Veteran ... dropped ...`, `[Dungeon] ... repopulated`.
- Devscripts: `dungeon-loot-recon.txt` (`/place`s a crypt at -600, 600 and prints its rooms), `dungeon-loot-lan-host.txt` + `dungeon-loot-lan-guest.txt` (two players at the coffer, the salvage roll and the timer, on port 25612).
