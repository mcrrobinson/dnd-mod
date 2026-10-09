# Cleric
A miner and healer. You dig fast, see in the dark, and can make every mob around you lose interest.

![A Cleric in Sanctuary: the mobs around ignore them, and the particle ring marks the 16-block circle](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/cleric-sanctuary.png)

## How it works
You always have **Haste III** and **Night Vision**. They're refreshed before they run low and show no particles, so you never see the Night Vision flicker.

| Stat | Cleric | Vanilla |
|-|-|-|
| Attack damage (base) | 0.67 | 1 |
| Fog | starts at 16, solid at 48 blocks | render distance |

The shorter view is the main cost. You'll spot mobs and landmarks late on the surface, but underground it rarely matters.

**Special (power-up key, full mana): Sanctuary.** For 15 seconds no mob can target you. A ring of particles with a 16-block radius shows the circle. Every half second, mobs within 32 blocks that were already chasing you give up. Party members within 16 blocks when you cast it share the circle for 15 seconds and get Regeneration I for 10 seconds (see [Party](../systems/party.md)).

### Tips
Sanctuary is an escape button: use it when a cave fight goes wrong and walk out. Mobs ignore you, but they still hit anything else, so it won't protect pets or villagers.

### Skill tree
Press **O** to open your skill tree. Class XP gives you a skill point per level, up to level 6 at 1000 XP. Every class gets 2 XP for a hostile kill and 1 XP a minute for playing. Unlock skills from the tree anywhere; change your loadout (one active for the power-up key, two passives) at an Attunement Table.

Clerics get 3 extra XP for killing undead, and XP for mining ores outside creative: 1 per ore, or 3 for diamond, emerald and ancient debris.

| Skill | Branch | Type | Points | Mana | Effect |
|-|-|-|-|-|-|
| Sanctuary | Root | Active | 0 | 9 | The special above |
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
- Skill tree: `Progression/Classes/ClericSkills.java`. Deep Delver is read by `ClericHandler` when it refreshes Haste.
