# Dragonborn
The folk of the [Dragon Temples](../structures/dragon-temples.md): Dragonborn Templars who guard the temple and the Dragonborn Elder who keeps its sanctum. They're neutral to players until temple law is broken.

## How it works
| Dragonborn | Health | Damage | Speed | Carries | Behaviour |
|-|-|-|-|-|-|
| Templar | 34 (4 armour, 0.3 knockback resistance) | 6 + weapon | 0.27 | an iron sword (70%) or a trident, a chainmail shirt | The guard: hunts monsters (not creepers) within 24 blocks of home, and breathes at what it fights. Keeps within 16 blocks of home by day, 10 at night. |
| Elder | 40 | 4 | 0.22 | a blaze rod | Merchant and quest giver; keeps within 4 blocks of the sanctum |

Every Dragonborn has a name such as "Medrash Kerrhylon", never despawns, and is a member of the **Dragonsworn** [faction](../systems/factions.md). Blows and breath from other Dragonborn don't hurt them.

### Templars
- **Melee**: sword or trident, at 1.1x speed.
- **Breath**: when what it's fighting is within 5 blocks and in sight, and the breath is ready, a Templar breathes a 60° cone 5 blocks long towards its eyes. Everything in the cone it may hit (its target, monsters, and players it's angry with; never other Dragonborn, bystanders or creative players) takes 5 damage, halved by a DEX save against DC 12 (players see it on the save lane as "Templar's breath"). Then every 10 s.
- **Colour**: a Templar's scales set its breath, with the same visuals and riders as the [Breath Weapon](../races/breath-weapon.md):

| Scales | Breath | Rider |
|-|-|-|
| Red, gold | Ember (fire) | burning for 4 s (shorter on a save) |
| White | Frost | Freeze for 2 s on a failed save |
| Blue, bronze | Storm (lightning) | Glowing for 2 s |

Templars leave players alone unless the player is Hostile with the Dragonsworn (attacked on sight), hits a Dragonborn (every Templar within 16 blocks joins in) or breaks temple law where a Dragonborn can see (see [Dragon Temples](../structures/dragon-temples.md#temple-law)). They stay angry for 30-50 s.

### Trades
Each trade has 12 uses and restocks every morning. Prices follow your standing with the Dragonsworn (Unfriendly +50%, Friendly -10%, Honored -25%, Exalted -40%); the Elder won't trade with Hostile players. Dragonborn pay 25% less (kin prices).

| Sells | Price |
|-|-|
| Smite Dragons III book | 18 emeralds |
| Potion of Fire Resistance (8:00) | 4 |
| Explorer map to the nearest unexplored dragon lair | 12 |
| Explorer map to the nearest unexplored frost lair | 12 |
| **Dragonborn only:** [Draconic Scale](../items/armor.md#racial-armor) helmet, chestplate, leggings, boots | 12, 20, 18, 10 |

The Draconic Scale is kin stock: only Dragonborn players see those four offers (after the others).

| Buys (the wyvern and drake drops) | For |
|-|-|
| 4 phantom membrane | 3 emeralds |
| 6 magma cream | 4 |
| 8 blaze powder | 4 |
| 2 blue ice | 5 |
| 2 copper blocks | 3 |
| 1 netherite scrap | 8 |
| 1 Ancient Red Scale ([Ancient Red Dragon](../bosses/ancient-red-dragon.md)) | 8 |

### Dialogue
The Elder has the quest-giver role `dragonborn_elder` (`dialogue/dragonborn_elder.json`, with a greeting for each standing tier). Quests whose `giver` is `dragonborn_elder` are offered by him; with nothing to say, right-clicking him opens his trades. See [NPC dialogue](../systems/dialogue.md).

### Looks
The player model with classic arms at 1.05x, with the Dragonborn snout, frill and spines that Dragonborn players get, tinted by breath. Five Templar skins (red, white, blue, bronze and gold scales under a red temple tabard) and the Elder's gold scales under purple robes, from `tools/dragonborn_skins.py`.

## Where to find it
Only in Dragon Temples, where Templars also spawn naturally on the terrace (up to 7 within 48 blocks). Spawn eggs: Dragonborn Templar, Dragonborn Elder.

## Commands
- `/summon dndclasses:dragonborn_templar` and `/summon dndclasses:dragonborn_elder`

## Known limitations
- Templars don't throw their tridents.

## For developers
- `entity/DragonbornEntity` (base, a `SettlementNpcEntity`: variant, `getAncestry`, home and ranges, defence), `DragonbornTemplarEntity` (`GuardAnger`, `BreathGoal`, `breathe`), `DragonbornElderEntity` (`NpcMerchant`, `ELDER_TAG`), `DragonbornTrades`, kin stock through `KinStock` / `KinStockOffers.dragonTemple`, `TempleLaw`.
- The breath visuals reuse `BreathWeapon.sendVisuals` and `BreathWeapon.playSound` (now public and taking any entity). Logs `[DragonTemple] Templar <name> breathed <ancestry> at <target> (<n> hit)`.
- Renderer: `RacialHumanoidRenderer.dragonborn` / `dragonbornElder` (`Look DRAGONBORN`); the head feature uses each NPC's ancestry.
- `ModSpawns.canTemplarSpawn` (in a temple piece, at most 7 within 48 blocks).
