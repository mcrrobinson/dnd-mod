# Cleric
A miner and healer. You dig fast, see in the dark, and can make every mob around you lose interest.

## How it works
You always have **Haste III** and **Night Vision**. They're refreshed before they run low and show no particles, so you never see the Night Vision flicker.

| Stat | Cleric | Vanilla |
|-|-|-|
| Attack damage (base) | 0.67 | 1 |
| Fog | starts at 16, solid at 48 blocks | render distance |

The shorter view is the main cost. You'll spot mobs and landmarks late on the surface, but underground it rarely matters.

**Special (power-up key, full mana): Sanctuary.** For a while no mob can target you, and every half second mobs within 32 blocks that were already chasing you give up. It starts as a short personal escape; ranking it up at an Attunement Table (see [Class progression](../systems/class-progression.md#ability-ranks)) makes it last longer and shares it with your party (see [Party](../systems/party.md)). Party members within the reach when you cast it get Mob Repel for the same time, plus Regeneration.

| Rank | Class level | Duration | Party share | Party Regeneration |
|-|-|-|-|-|
| I | 0 | 6 s | none, only you | none |
| II | 3 | 9 s | within 8 blocks | I for 5 s |
| III | 6 | 12 s | within 12 blocks | I for 8 s |
| IV | 9 | 15 s | within 16 blocks | II for 10 s |

A ring of particles shows the circle: at the party reach, or a small 2-block ring at rank I. Bosses (Wyvern, Lightning Chaser, Lich, Goblin Warlord) and their minions pick targets through the same code as other mobs, so they ignore you too.

### Tips
Sanctuary is an escape button: use it when a cave fight goes wrong and walk out. Early on it only lasts 6 seconds, so start walking straight away. Mobs ignore you, but they still hit anything else, so it won't protect pets or villagers.

### Skill tree
Press **O** to open your skill tree. Class XP gives you a skill point per level, up to level 10 at 2700 XP. Every class gets 2 XP for a hostile kill and 1 XP a minute for playing. Unlock skills from the tree anywhere; change your loadout (one active for the power-up key, two passives) and rank up abilities at an Attunement Table (see [Class progression](../systems/class-progression.md)).

Clerics get 3 extra XP for killing undead, and XP for mining ores outside creative: 1 per ore, or 3 for diamond, emerald and ancient debris.

| Skill | Branch | Type | Points | Mana | Effect |
|-|-|-|-|-|-|
| Sanctuary | Root | Active | 0 | 9 | The special above: 6 / 9 / 12 / 15 s, party share from rank II (ranks I-IV) |
| Preserve Life | Life | Passive | 1 | | Dropping below 30% health gives Regeneration II for 5 s, once a minute |
| Cure Wounds | Life | Active | 1 | 4 | Heals you, other players and your pets within 8 blocks by 4 hearts |
| Smite Undead | Life | Passive | 1 | | 50% more damage to undead |
| Prospector | Forge | Passive | 1 | | 20% chance an ore drops its loot twice. Not with Silk Touch, and the XP orbs aren't doubled |
| Radiance | Forge | Active | 1 | 4 | Everything within 8 blocks except players and your pets glows for 10 s; undead also burn for 5 s and take 4 damage |
| Deep Delver | Forge | Passive | 1 | | Haste IV instead of Haste III |
| Divine Intervention | Capstone | Active | 2 | 9 | Fully heals you, other players and your pets within 10 blocks, puts out fire, clears harmful effects and gives Resistance III for 10 s |

The Forge branch is the mining build: Prospector and Deep Delver together make strip mining much faster. The Life branch suits group play and undead-heavy places like strongholds.

## Commands
- `/dndclass set <player> cleric` (see [Admin commands](../systems/admin-commands.md)).

## For developers
- Passives and the repel circle: `Misc/ClericHandler`. Target blocking: `mixin/MobEntityMixin`. Effect: `ModEffects.MOB_REPEL`. Fog: `mixin/BackgroundRendererMixin`.
- Sanctuary's ranks: `ClericSkills.SANCTUARY` (duration, plus the party text built from the `SANCTUARY_PARTY_*` arrays, read with `sanctuaryPartyReach` and `sanctuaryPartyRegen*`), fired in `Misc/PowerUpEffect` (case `CLERIC`). `ClericHandler` draws the ring at the party reach.
- Skill tree: `Progression/Classes/ClericSkills.java`. Deep Delver is read by `ClericHandler` when it refreshes Haste.
