# Orcs
The Bloodtusk Horde of the [Orc Strongholds](../structures/orc-strongholds.md): Orc Warriors, Orc Archers, the Orc Quartermaster, the Orc Champion of the Trial of Strength, and the [Orc War Chief](../bosses/orc-war-chief.md). They're hostile to almost everyone, but tolerate Half-Orcs.

## How it works
| Orc | Health | Armour | Damage | Speed | Carries | Behaviour |
|-|-|-|-|-|-|-|
| Warrior | 30 | 4 | 7 + weapon | 0.28 | a stone or iron axe; an iron helmet 40% of the time | Roams within 18 blocks of the pit |
| Archer | 22 | 2 | crossbow | 0.27 | a crossbow | Keeps its post on a gate tower, stepping to the platform's edge to see down; loads, holds for 1-2 s and shoots anything it fights within 16 blocks it can see |
| Quartermaster | 30 | 4 | 7 | 0.25 | an iron ingot | Merchant and quest giver (role `orc_quartermaster`); keeps within 4 blocks of the smithy. Never attacks on sight. |
| Champion | 40 | 6 | 8 (an iron axe) | 0.3 | an iron axe and helmet | Only appears for the Trial of Strength and fights only its duellist |
| War Chief | 90 | 8 | 12 | 0.3 | an Orcish Greataxe, a gold helmet | The boss, see [Orc War Chief](../bosses/orc-war-chief.md) |

Orcs are the player model with tusks, 1.1x tall and 1.15x wide (the Champion 1.15 x 1.2, the War Chief 1.25 x 1.3), with six skins from olive to grey-green. Each has a name such as "Grukk Ironjaw", never despawns, and sounds like a deep piglin brute. Warriors, Archers, the Quartermaster and the War Chief are members of the **Bloodtusk Horde** [faction](../systems/factions.md) (the Champion isn't, so the duel's blows cost no standing).

### Who they fight
- Players Hostile with the horde (everyone but Half-Orcs starts there, at -600) on sight within 16 blocks, or 8 for Barbarians.
- Anyone who strikes an orc: it and every orc within 16 blocks turn on them for 30-50 s. Opening the War Chief's hoard where an orc can see does the same.
- Hobbits, dwarves, elves and villagers within 24 blocks of home.
- Never goblins: goblins and orcs are allies and neither targets or hurts the other.
- Never a player in the Trial of Strength (except the Champion), creative or spectator players, or anyone on Peaceful.

Players at Unfriendly or better, players Blood-Sworn at the orc's hold, and their party members within 8 blocks of them are left alone unless they start something. An orc already fighting a player stands down once that player is tolerated.

### Trades
The Quartermaster trades only with Half-Orcs and with players Blood-Sworn at his hold; anyone else gets "&lt;name&gt; grunts and waves you off.". Each trade has 12 uses and restocks every morning. Prices follow your standing with the horde (Unfriendly +50%, Friendly -10%, Honored -25%, Exalted -40%), and Half-Orcs pay 25% less (kin prices).

| Sells | Price |
|-|-|
| Grog (Strength I for 60 s, Nausea for 5 s, 2 hunger) | 2 emeralds |
| [Orcish Greataxe](../items/racial-weapons.md) | 14 emeralds |
| Saddle | 6 emeralds |
| An iron axe enchanted at level 15 | 8 emeralds |
| Half-Orcs only: [Orcish Warplate](../items/armor.md#racial-armor) helmet, chestplate, leggings, boots | 10, 16, 14, 8 emeralds |

| Buys | For |
|-|-|
| 32 rotten flesh, 16 bones, 8 leather or 4 iron ingots | 1 emerald |
| 3 Goblin War Tokens | 2 emeralds |

**Goblin War Tokens** drop from 25% of [Goblin Warriors](goblins.md) killed by a player.

### Drops
| Orc | Drops |
|-|-|
| Warrior | 0-2 rotten flesh, leather and bones; rarely its axe or helmet |
| Archer | 1-4 arrows, 0-1 leather; 8% its crossbow |
| Quartermaster | 1-3 iron ingots, 0-2 emeralds |
| Champion | nothing |

## Where to find it
[Orc Strongholds](../structures/orc-strongholds.md) in the badlands. Spawn eggs for all five are in the creative tab.

## Commands
- `/summon dndclasses:orc_warrior` (also `orc_archer`, `orc_quartermaster`, `orc_champion`, `orc_war_chief`). A summoned orc belongs to the hold it stands in, if any.

## Known limitations
- A summoned Champion outside a Trial fights only those who strike it.
- Orcs wearing a helmet don't show their tusks.

## For developers
- `entity/OrcEntity` (base: variant, name, home, hold key, `GuardAnger` grudges, ally rules), `OrcWarriorEntity`, `OrcArcherEntity` (`CrossbowUser`, its own load-hold-shoot goal), `OrcQuartermasterEntity` (`NpcMerchant` serving Half-Orcs and the Blood-Sworn, `KinStock` with `KinStockOffers.orcStronghold`), `OrcChampionEntity` (`setHealth` clamp while duelling), `OrcWarChiefEntity`, `OrcHostility` (who they fight), `OrcTrades`.
- `client/renderer/RacialHumanoidRenderer` (`orc`, `orcQuartermaster`, `orcChampion`, `orcWarChief`; `Look` now has a separate width, and crossbow arm poses).
- `GoblinWarriorEntity` refuses orc targets and orc damage. Loot: `loot_tables/entities/orc_*.json`, and the war token pool in `goblin_warrior.json`.
