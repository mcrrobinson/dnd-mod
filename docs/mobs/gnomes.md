# Gnomes
The folk of [Cogsdeep](../structures/cogsdeep.md): Gnome Tinkers, who work the benches and keep shop, and the Clockwork Sentinels they built to guard the burrow. They're friendly to players until the Tinkers' law is broken.

![A Gnome Tinker with goggles on his forehead in a workshop, a Clockwork Sentinel behind him](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/cogsdeep/cd-workshop.png)

## How it works
| Mob | Health | Damage | Speed | Behaviour |
|-|-|-|-|-|
| Gnome Tinker | 18 | none | 0.27 | Merchant and quest giver. Wanders within 6-8 blocks of its bench, flees monsters within 8 blocks, never fights. Holds a clock, compass or spyglass. |
| Clockwork Sentinel | 40 (6 armour, 0.6 knockback resistance) | 4-11 (an iron golem's swing, from 8 attack damage) | 0.27 | The guard: walks within 8-12 blocks of its post and hunts monsters within 20 blocks of it (not creepers, not a dormant mimic), flinging them up like an iron golem. |

Both are members of the **Tinkers' Guild** [faction](../systems/factions.md) and never despawn.

- **Defence**: a player who hits a Tinker or a Sentinel brings every Sentinel within 16 blocks down on them. Sentinels also turn on anyone they see break the Tinkers' law (see [Cogsdeep](../structures/cogsdeep.md#the-tinkers-law)) and on anyone Hostile with the Tinkers' Guild, on sight. Like an iron golem, a Sentinel stays on a player while it can reach them; once it loses them the grudge runs out after 20-39 s. It lets go of a player it no longer holds a grudge against.
- **Sentinels and zombies**: zombies (and goblins) go for a Sentinel the way they go for an iron golem, and it fights back.
- **Mending**: right-click a Sentinel with a copper ingot to heal it 10 HP (an iron ingot does nothing).
- **Drops**: a Sentinel drops 3-6 copper ingots (Looting adds up to 1 per level), and sometimes 1-4 redstone or a clock. Tinkers drop nothing.

### Trades
Every Tinker keeps a shop. Each trade has 12 uses and restocks every morning. Prices follow your standing with the Tinkers' Guild (Unfriendly +50%, Friendly -10%, Honored -25%, Exalted -40%); a Tinker won't trade with Hostile players. Gnomes pay 25% less (kin prices).

| Sells | Price |
|-|-|
| a spyglass | 4 emeralds |
| a clock | 4 |
| a compass | 3 |
| 8 redstone dust | 1 |
| 2 redstone repeaters | 2 |
| a comparator | 3 |
| an observer | 3 |
| a piston | 2 |
| a sticky piston | 3 |
| 2 redstone lamps | 2 |
| a hopper | 3 |
| **Gnomes only:** the [Gnomish Repeater](../items/racial-weapons.md#gnomish-repeater), with Quick Charge III and Multishot | 14 |
| **Gnomes only:** [Tinker's Goggles](../items/tinkers-goggles.md) | 10 |

| Buys | For |
|-|-|
| 10 copper ingots | 1 emerald |
| 6 amethyst shards | 1 |
| 3 iron ingots | 1 |
| 12 redstone dust | 1 |

The Gnome-only offers come last: a Human sees 15 trades, a Gnome 17.

### Dialogue
Tinkers have the quest-giver role `gnome_tinker` (`dialogue/gnome_tinker.json`, with a greeting for each standing tier). Quests whose `giver` is `gnome_tinker` are offered by any of them; with nothing to say, right-clicking one opens its trades. See [NPC dialogue](../systems/dialogue.md).

### Looks
- **Tinker**: the player model with classic arms at the Small race scale (0.65, about 1.3 blocks tall), with a big 2 x 3 x 2 nose sticking out under the eyes. Five skins (fair, tan, ruddy, brown and pale; white, ginger, chestnut, black and blond hair, some with beards) in a green, blue, mustard, maroon or teal tunic under a leather apron with a tool belt, and brass goggles pushed up on the forehead. Names are gnomish, with a clan: "Boddynock Cogwhistle the Tinker".
- **Sentinel**: the iron golem's model at 0.75 scale (2 blocks tall), in riveted copper plates going green in patches, banded arms and legs, a clock face on its chest and glowing amber eyes. It sways as it walks and shows the iron golem's cracks as it's hurt.

Both textures come from `tools/cogsdeep_textures.py`.

## Where to find it
Only in Cogsdeep, where both also spawn naturally on the burrow's floor (up to 10 gnomes and Sentinels within 48 blocks). Spawn eggs: Gnome Tinker, Clockwork Sentinel.

## Commands
- `/summon dndclasses:gnome_tinker` and `/summon dndclasses:clockwork_sentinel` (without NBT, so they get a name, a skin and a home where they're summoned)

## Known limitations
- Tinkers don't actually tinker: they stand at their benches and wander.

## For developers
- `entity/GnomeTinkerEntity` (a `SettlementNpcEntity` with an `NpcMerchant`: variants, names, home and range, `setTarget` refuses everything, `defend`, `ROLE_TAG`, one `KinStock` of `KinStockOffers.cogsdeep`), `entity/TinkerTrades`, `entity/ClockworkSentinelEntity` (an `IronGolemEntity` and `SettlementGuard` with its own goals: melee, wander, hunt monsters near home, group revenge; `provoke`, the `setTarget` filter, copper mending), `entity/TinkersLaw`.
- Renderers: `RacialHumanoidRenderer.gnome` (`Look GNOME`, scale 0.65, with a `NoseFeature` drawing `RaceFeatures.renderNose` from the skin's own texture) and `client/renderer/ClockworkSentinelRenderer` (`IronGolemEntityModel`, scale 0.75, crack and emissive eye layers).
- `ModEntityTypes.GNOME_TINKER` (0.4 x 1.25) and `CLOCKWORK_SENTINEL` (1.05 x 2.0), `ModSpawns.canCogsdeepSpawn`, `loot_tables/entities/clockwork_sentinel.json`.
- Devscript: `cogsdeep-check.txt` (trades, standing, a Sentinel against a zombie, the law).
