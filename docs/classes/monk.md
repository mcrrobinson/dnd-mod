# Monk
A quick martial artist with the fastest attacks in the mod and a triple jump. You fight with a staff or your fists, and armor makes you weaker.

## How it works
| Stat | Monk | Vanilla |
|-|-|-|
| Movement speed | 0.12 (20% faster) | 0.1 |
| Attack speed | 6 | 4 |

**Triple jump.** Press jump in mid-air while falling for up to two extra jumps. They reset when you land or climb. Each extra jump resets your fall distance, so fall damage counts from the top of the last jump. You can't air-jump while wearing a usable elytra, gliding, riding, in water or levitating. This is always on and doesn't use mana.

**Staff or fists only.** Attacking with anything other than the [Monk Staff](../items/staffs.md#monk-staff) or an empty hand is cancelled, with "Monks can only fight with a staff or bare fists!". Craft the staff from 3 sticks in a diagonal line.

**Armor penalty.** Your damage is multiplied by `0.75 / (1 + armor / 10)`:

| Armor points | Damage |
|-|-|
| 0 | 75% |
| 10 | 37.5% |
| 20 (full diamond) | 25% |

**Special (power-up key, full mana): Flurry Rush.** Look at a mob within 16 blocks and press the power-up key. You blink next to it and land a rapid chain of hits, one every 3 ticks (0.15 s), appearing on a different side of the target each time. From rank II the chain jumps on to the nearest hostile mobs within 8 blocks of the first target. After the last hit you blink back to where you started, facing the way you were.

- The hits are split across the targets in a row, so each target takes its hits one after another (Flurry of Blows can combo on them). The first target gets any extra hit. With fewer hostiles nearby than the rank allows, the ones there take all the hits.
- Each hit is a melee hit from you with a fixed damage per hit. The armor penalty applies, relative to unarmored: unarmored is the full amount, 10 armor half.
- With the Monk Staff or an empty hand, the hits count as Monk strikes, so Flurry of Blows adds its 50% on every 3rd hit on the same target, a charged Quivering Palm goes off on the first hit, and kills give the Monk kill XP.
- You take no damage while the rush is running, except damage that bypasses invulnerability (`/kill`, the void).
- If a target dies, its remaining hits are lost and the chain moves on. If you can't stand on any side of a target, you strike from where you are.
- With nothing in the crosshair you get "No target in sight" and keep your mana.
- Particles and sound: a cloud and mirror sound where you leave and come back, an end-rod trail along each blink, a sweep and crits on each hit with the hit sound rising in pitch, and an explosion puff and crit sound on the last hit.

Raise its rank at an Attunement Table (see [Class progression](../systems/class-progression.md)):

| Rank | Class level | Hits | Targets | Damage per hit |
|-|-|-|-|-|
| I | 1 | 3 | 1 | 3 |
| II | 3 | 5 | 2 | 3 |
| III | 6 | 7 | 3 | 4 |
| IV | 9 | 10 | 5 | 4 |

### Tips
Fight unarmored or in light armor, and lean on speed and the triple jump to avoid hits. A late triple jump is also a way to cancel a long fall: jump just before landing. Flurry Rush is a safe way into a crowd: you can't be hurt during it and you end where you started, so start it from somewhere out of reach. Fire Quivering Palm first to add its 20 damage to the rush's first hit.

### Skill tree
Press **O** to open your skill tree. Class XP gives you a skill point per level, up to level 10 at 2700 XP. Every class gets 2 XP for a hostile kill and 1 XP a minute for playing. Unlock skills from the tree anywhere; change your loadout (one active for the power-up key, two passives) and rank up abilities at an Attunement Table (see [Class progression](../systems/class-progression.md)).

Monks get 3 extra XP for each hostile mob killed with a fist or the Monk Staff.

| Skill | Branch | Type | Points | Mana | Effect |
|-|-|-|-|-|-|
| Flurry Rush | Root | Active | 0 | 9 | Blink-strike chain on the mob you're looking at and hostiles near it (see above) |
| Flurry of Blows | Open Hand | Passive | 1 | | Every 3rd hit in a row on the same target deals 50% more. The combo breaks if you wait more than 3 s between hits |
| Stunning Strike | Open Hand | Active | 1 | 3 | Hostile mobs within 4 blocks get Slowness IV and Weakness II for 4 s |
| Deflect Missiles | Open Hand | Passive | 1 | | 50% chance to take no damage from a projectile |
| Slow Fall | Way of the Wind | Passive | 1 | | 75% less fall damage |
| Step of the Wind | Way of the Wind | Active | 1 | 3 | Dash about 8 blocks the way you're looking, with no fall damage from the dash |
| Unarmored Movement | Way of the Wind | Passive | 1 | | 15% faster while you aren't wearing a chestplate |
| Quivering Palm | Capstone | Active | 2 | 9 | Your next melee hit within 10 s deals 20 extra damage |

Quivering Palm's bonus is added before armor, and it's spent on your next strike even if that hit is on a weak mob, so line it up on the target you want. Flurry of Blows and Quivering Palm only count hits with a fist or the Monk Staff.

## Commands
- `/dndclass set <player> monk` (see [Admin commands](../systems/admin-commands.md)).

## For developers
- Rules and damage modifier: `MonkHandler`. Jump: `mixin/DoubleJumpMixin` (client) does the jump and sends an empty `doublejump` packet. `MonkHandler.onDoubleJump` checks it on the server (Monk, not flying/riding/in water/levitating/anti-magic, at most 2 air jumps until the server sees you land or climb), resets the fall distance and sends the cloud effect to players tracking you.
- Skill tree: `Progression/Classes/MonkSkills.java`. Flurry Rush lives there too: `MonkSkills.flurryRush` (called from `PowerUpEffect` case `MONK`) picks the targets and starts a `Rush`, which a server tick handler plays out a hit at a time. Its per-rank numbers are `MonkSkills.FLURRY_RUSH` (`Ranks`). Test: `devscripts/monk-ranks.txt`.
