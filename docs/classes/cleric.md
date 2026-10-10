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

**Special (power-up key, full mana): Sanctuary.** For a while no mob can target you, and every half second mobs within 32 blocks that were already chasing you give up. It starts as a short personal escape; ranking it up at an Attunement Table (see [Class progression](../systems/class-progression.md#ability-ranks)) makes it last longer and shares it with your party (see [Party](../systems/party.md)). Party members within the reach when you cast it get Mob Repel for the same time, plus Regeneration.

| Rank | Class level | Duration | Party share | Party Regeneration |
|-|-|-|-|-|
| I | 0 | 6 s | none, only you | none |
| II | 3 | 9 s | within 8 blocks | I for 5 s |
| III | 6 | 12 s | within 12 blocks | I for 8 s |
| IV | 9 | 15 s | within 16 blocks | II for 10 s |

A ring of particles shows the circle: at the party reach, or a small 2-block ring at rank I. Bosses (Wyvern, Lightning Chaser, Frost Drake, Lich, Goblin Warlord) and their minions pick targets through the same code as other mobs, so they ignore you too.

### Tips
Sanctuary is an escape button: use it when a cave fight goes wrong and walk out. Early on it only lasts 6 seconds, so start walking straight away. Mobs ignore you, but they still hit anything else, so it won't protect pets or villagers.

### Skill tree
Press **O** to open your skill tree. Class XP gives you a skill point per level, up to level 10 at 2700 XP. Every class gets 2 XP for a hostile kill and 1 XP a minute for playing. Unlock skills from the tree anywhere; change your loadout (one active for the power-up key, two passives) and rank up abilities at an Attunement Table (see [Class progression](../systems/class-progression.md)).

Clerics get 3 extra XP for killing undead, and XP for mining ores outside creative: 1 per ore, or 3 for diamond, emerald and ancient debris.

| Skill | Subclass | Type | Points | Mana | Effect |
|-|-|-|-|-|-|
| Sanctuary | Root | Active | 0 | 9 | The special above: 6 / 9 / 12 / 15 s, party share from rank II (ranks I-IV) |
| Preserve Life | Life Domain | Passive | 1 | | Dropping below 30% health gives Regeneration II for 5 s, once a minute |
| Cure Wounds | Life Domain | Active | 1 | 4 | Heals you, other players and your pets within 8 blocks by 4 hearts |
| Smite Undead | Life Domain | Passive | 1 | | 50% more damage to undead |
| Prospector | Forge Domain | Passive | 1 | | 20% chance an ore drops its loot twice. Not with Silk Touch, and the XP orbs aren't doubled |
| Radiance | Forge Domain | Active | 1 | 4 | Everything within 8 blocks except players and your pets glows for 10 s; undead also burn for 5 s and take 4 damage |
| Deep Delver | Forge Domain | Passive | 1 | | Haste IV instead of Haste III |
| Divine Intervention | Capstone | Active | 2 | 9 | Fully heals you, other players and your pets within 10 blocks, puts out fire, clears harmful effects and gives Resistance III for 10 s |

The Forge branch is the mining build: Prospector and Deep Delver together make strip mining much faster. The Life branch suits group play and undead-heavy places like strongholds.

### Subclasses
From class level 3, choose one of these two at an Attunement Table (see [Subclasses](../systems/class-progression.md#subclasses)). Your subclass's branch opens fully. The other branch's first node stays open, but the rest of it is sealed.

| Subclass | Branch | Nodes | Feature (free, doesn't use a passive slot) |
|-|-|-|-|
| Life Domain | Right | Preserve Life (first node, open to both), Cure Wounds, Smite Undead | **Disciple of Life**: Life Domain healing is 25% stronger |
| Forge Domain | Left | Prospector (first node, open to both), Radiance, Deep Delver | **Blessing of the Forge**: once per long rest (once per in-game day with the `dndRests` gamerule off), make one held weapon or worn armor piece +1 (up to +3) until your next long rest, from an Attunement Table's Items tab ([details](../systems/magic-items.md#blessing-of-the-forge)) |

How the features work:
- **Disciple of Life** (Life): Cure Wounds heals 5 hearts instead of 4. Preserve Life's Regeneration II lasts 6.25 s instead of 5 s, and Sanctuary's party Regeneration lasts 25% longer (6.25 / 10 / 12.5 s at ranks II-IV). Divine Intervention already heals fully, so it also gives Absorption I (2 hearts) for 10 s.
- **Blessing of the Forge** (Forge): at an Attunement Table, open the Items tab and press **Bless +1** next to a weapon in your hand (main or offhand) or an armor piece you wear. It becomes one better (+1, up to +3; see [Blessing of the Forge](../systems/magic-items.md#blessing-of-the-forge)) until your next long rest. Once per long rest; with the `dndRests` gamerule off, once per in-game day, lasting until the next day. The +1 comes off the item wherever it is once its Cleric has rested (checked once a second while the Cleric is online), or if the Cleric stops being a Forge Cleric. Unidentified items and +3 items can't be blessed.

## Commands
- `/dndclass set <player> cleric` (see [Admin commands](../systems/admin-commands.md)).

## For developers
- Passives and the repel circle: `Misc/ClericHandler`. Target blocking: `mixin/MobEntityMixin`. Effect: `ModEffects.MOB_REPEL`. Fog: `mixin/BackgroundRendererMixin`.
- Sanctuary's ranks: `ClericSkills.SANCTUARY` (duration, plus the party text built from the `SANCTUARY_PARTY_*` arrays, read with `sanctuaryPartyReach` and `sanctuaryPartyRegen*`), fired in `Misc/PowerUpEffect` (case `CLERIC`). `ClericHandler` draws the ring at the party reach.
- Skill tree: `Progression/Classes/ClericSkills.java`. Deep Delver is read by `ClericHandler` when it refreshes Haste.
