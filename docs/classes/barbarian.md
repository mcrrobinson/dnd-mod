# Barbarian
A slow, short-sighted brawler with twice the normal health and a punch that hurts even without a weapon. The special is inspired by *One Punch Man*.

![A raging Barbarian punching a zombie, with the Strength particles swirling](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/barbarian-rage.png)

## How it works
| Stat | Barbarian | Vanilla |
|-|-|-|
| Max health | 40 (20 hearts) | 20 |
| Attack damage (base) | 6 | 1 |
| Movement speed | 0.08 (20% slower) | 0.1 |

Picking the class sets your health to 25, so you start a little over half full.

Your bare fist does 6 damage, and a weapon adds its own damage on top. The catch is that you can't see far. Fog starts 4 blocks out and is solid at 24, whatever your render distance. Water, lava and Blindness still win when their fog is closer.

**Special (power-up key, full mana): Rage.** Strength that grows with its rank: Strength I for 8 seconds at rank I, Strength II for 10 at II, Strength II for 12 at III and Strength III for 12 at IV. Rank it up at an Attunement Table (see [Class progression](../systems/class-progression.md#ability-ranks)); ranks II, III and IV need class level 3, 6 and 9 and a skill point each.

### Tips
You're slow, so let mobs come to you instead of chasing them. Skeletons can shoot you from inside the fog, so close the gap behind cover. A horse makes up for the walking speed when you travel. Save Rage for a boss or a crowd: each Strength level adds 3 damage to every hit, so a fully ranked Rage adds 9. Fuelled by Rage's 30% bonus applies on top of it while you're below half health. Titan is still the bigger buff: Strength III for 20 seconds with Resistance and Regeneration.

### Skill tree
Press **O** to open your skill tree. Class XP gives you a skill point per level, up to level 10 at 2700 XP. Every class gets 2 XP for a hostile kill and 1 XP a minute for playing. Unlock skills from the tree anywhere; change your loadout (one active for the power-up key, two passives) and rank up abilities at an Attunement Table (see [Class progression](../systems/class-progression.md)).

Barbarians get 3 extra XP for each hostile mob they kill in melee.

| Skill | Subclass | Type | Points | Mana | Effect |
|-|-|-|-|-|-|
| Rage | Root | Active | 0 | 9 | Strength I for 8 s / Strength II for 10 s / Strength II for 12 s / Strength III for 12 s (ranks I-IV) |
| Bloodlust | Path of the Berserker | Passive | 1 | | Heal 1 heart for every kill |
| War Cry | Path of the Berserker | Active | 1 | 3 | Hostile mobs within 8 blocks get Weakness I and Slowness II for 6 s |
| Fuelled by Rage | Path of the Berserker | Passive | 1 | | 30% more damage while below half health |
| Bear Hide | Path of the Totem Warrior | Passive | 1 | | +4 armor |
| Ground Slam | Path of the Totem Warrior | Active | 1 | 5 | 6 damage to everything within 5 blocks except players and your pets, knocking them back and up |
| Unstoppable | Path of the Totem Warrior | Passive | 1 | | +50% knockback resistance, and Slowness is removed every second |
| Titan | Capstone | Active | 2 | 9 | Strength III, Resistance I and Regeneration I for 20 s |

Each branch unlocks in order from the root, and Titan needs the end of either branch. Ground Slam hits villagers and other people's animals too, so check who's standing next to you.

### Subclasses
From class level 3, choose one of these two at an Attunement Table (see [Subclasses](../systems/class-progression.md#subclasses)). Your subclass's branch opens fully. The other branch's first node stays open, but the rest of it is sealed.

| Subclass | Branch | Nodes | Feature (free, doesn't use a passive slot) |
|-|-|-|-|
| Path of the Berserker | Right | Bloodlust (first node, open to both), War Cry, Fuelled by Rage | **Frenzy**: while Rage is active, each melee kill adds 2 seconds to it (up to 6 seconds per Rage) |
| Path of the Totem Warrior | Left | Bear Hide (first node, open to both), Ground Slam, Unstoppable | **Bear Totem Spirit**: while Rage is active you take 15% less damage (not the void, `/kill` or starving) |

How the features work:
- **Frenzy** (Berserker): a melee kill while Rage is on lengthens Rage's Strength by 2 s, up to +6 s per Rage. Kills by arrows or skills don't count. Rage ends early if the Strength is removed (milk).
- **Bear Totem Spirit** (Totem Warrior): while Rage is on, damage you take is multiplied by 0.85. The void, `/kill` and starving aren't reduced.

## Commands
- `/dndclass set <player> barbarian` switches a player to the class. `/dndclass xp`, `unlock` and `equip` handle the skill tree (see [Admin commands](../systems/admin-commands.md)).

## Known limitations
- The fog is drawn on the client only and can't be turned off.

## For developers
- Attributes: `ClassStats`. Fog: `mixin/BackgroundRendererMixin` (`BARBARIAN_FOG_START`/`END`). Special: `Misc/PowerUpEffect` (`BARBARIAN`), with its per-rank values in `BarbarianSkills.RAGE`.
- Skill tree: `Progression/Classes/BarbarianSkills.java`.
- See [Mana and specials](../systems/mana.md) for how the special is triggered.
