# Monk
A quick martial artist with the fastest attacks in the mod and a triple jump. You fight with a staff or your fists, and armor makes you weaker.

## How it works
| Stat | Monk | Vanilla |
|-|-|-|
| Movement speed | 0.12 (20% faster) | 0.1 |
| Attack speed | 6 | 4 |

**Triple jump.** Press jump in mid-air while falling for up to two extra jumps. They reset when you land or climb. Each extra jump resets your fall distance, so fall damage counts from the top of the last jump. You can't air-jump while wearing a usable elytra, gliding, riding, in water or levitating. This is always on and doesn't use mana.

**Staff or fists only.** Attacking with anything other than the [Monk Staff](../items/staffs.md#monk-staff) or an empty hand is cancelled, with "Monks can only fight with a staff or bare fists!".

**Armor penalty.** Your damage is multiplied by `0.75 / (1 + armor / 10)`:

| Armor points | Damage |
|-|-|
| 0 | 75% |
| 10 | 37.5% |
| 20 (full diamond) | 25% |

**Special (power-up key, full mana): Ki Surge.** Speed II, Haste II and Jump Boost II for 15 seconds.

### Tips
Fight unarmored or in light armor, and lean on speed and the triple jump to avoid hits. A late triple jump is also a way to cancel a long fall: jump just before landing. With Ki Surge's Jump Boost you can clear walls and reach ledges you couldn't otherwise.

### Skill tree
Press **O** to open your skill tree. Class XP gives you a skill point per level, up to level 6 at 1000 XP. Every class gets 2 XP for a hostile kill and 1 XP a minute for playing. Unlock skills from the tree anywhere; change your loadout (one active for the power-up key, two passives) at an Attunement Table.

Monks get 3 extra XP for each hostile mob killed with a fist or the Monk Staff.

| Skill | Branch | Type | Points | Mana | Effect |
|-|-|-|-|-|-|
| Ki Surge | Root | Active | 0 | 9 | Speed II, Haste II and Jump Boost II for 15 s |
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
- Skill tree: `Progression/Classes/MonkSkills.java`. Ki Surge lives there too (`MonkSkills.kiSurge`), called from `PowerUpEffect`.
