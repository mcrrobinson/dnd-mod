# Warlock
A fire caster. You throw fireballs from an empty hand, fire can't hurt you, and water does.

![Fire Breath on a zombie](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-flesh-out/warlock-breath-first.png)

## How it works
| Stat | Warlock | Vanilla |
|-|-|-|
| Attack damage (base) | 0.5 | 1 |

**Fireballs.** Right click with an empty main hand, at the air or at a hostile mob, to throw a small slow fireball like a blaze's. You can throw one a second; the server enforces the cooldown. If your off hand holds something usable (shield, food, potion), right click uses that instead, so keep the off hand empty when you want to cast.

**Fireproof.** You're immune to all fire damage: fire, lava, burning and fireballs, your own included. You can swim through lava and fight inside your own fire.

**Water and rain hurt.** You take 1 damage every 4 seconds while touching water or standing in rain. It never takes you below 1 heart, and it doesn't apply in creative or spectator. It won't kill you, but it stops you healing and leaves you open to anything else.

**Special (power-up key, full mana): Fire Breath.** A beam of fire in front of you sets targets on fire and deals magic damage to anything in it. Rank it up at an Attunement Table (see [Class progression](../systems/class-progression.md#ability-ranks)):

| Rank | Class level | Duration | Reach | Damage | Burn |
|-|-|-|-|-|-|
| I | 0 | 8 s | 3 blocks | 1 | 2 s |
| II | 3 | 12 s | 4 blocks | 2 | 2 s |
| III | 6 | 16 s | 5 blocks | 2 | 4 s |
| IV | 9 | 20 s | 7 blocks | 3 | 4 s |

The damage lands each time a mob's hurt cooldown runs out (about twice a second). Hellfire boosts it once the target is burning, and the longer burn at ranks III and IV keeps it burning between passes. The kills count as yours. Your party members and your own pets are left alone. Only your current dimension is affected.

### Tips
The Nether is your home: lava lakes are shortcuts, and blazes and ghast fireballs can't hurt you. On the surface, carry a roof or stay near shelter when it rains. Cross rivers by bridge rather than swimming.

### Skill tree
Press **O** to open your skill tree. Class XP gives you a skill point per level, up to level 10 at 2700 XP. Every class gets 2 XP for a hostile kill and 1 XP a minute for playing. Unlock skills from the tree anywhere; change your loadout (one active for the power-up key, two passives) and rank up abilities at an Attunement Table (see [Class progression](../systems/class-progression.md)).

Warlocks get 2 extra XP for any kill by fire, and 1 extra for any kill in the Nether.

| Skill | Branch | Type | Points | Mana | Effect |
|-|-|-|-|-|-|
| Fire Breath | Root | Active | 0 | 9 | The special above: 8 / 12 / 16 / 20 s, 3 / 4 / 5 / 7 blocks (ranks I-IV) |
| Hellfire | Fiend | Passive | 1 | | Burning targets take 25% more damage from you |
| Eldritch Blast | Fiend | Active | 1 | 3 | An instant beam that deals 8 magic damage to the first mob in your crosshair within 20 blocks |
| Infernal Fireballs | Fiend | Passive | 1 | | Fireball cooldown halved, to two a second |
| Rain Ward | Great Old One | Passive | 1 | | Water and rain hurt half as much |
| Hex | Great Old One | Active | 1 | 4 | The mob in your crosshair (up to 20 blocks) gets Weakness II, Slowness II and Glowing, and takes 30% more damage from you, for 15 s |
| Dark One's Blessing | Great Old One | Passive | 1 | | Each kill gives 2 absorption hearts, up to 4 |
| Hellgate | Capstone | Active | 2 | 9 | A 6-block ring of fire around where you stand for 10 s. Mobs inside burn for 3 s and take 2 fire damage a second. You get Strength I for the same time |

Hellfire works with your fireballs: the first fireball sets the target alight, and every hit after that is boosted. Eldritch Blast and Hex need a target in your crosshair, not behind blocks. With no target, nothing happens and you keep your mana.

## Commands
- `/dndclass set <player> warlock` (see [Admin commands](../systems/admin-commands.md)).

## For developers
- Fireballs, fire immunity and wet damage: `Warlock.java` (`FIREBALL_COOLDOWN_TICKS`, `WET_DAMAGE_INTERVAL_TICKS`) with `mixin/WarlockFireballMixin` (client). Fire breath: `Warlock.tickFireBreath` (end times in `DnDClasses.WARLOCK_FIREBREATH`, in world time); per-rank values in `WarlockSkills.FIRE_BREATH`, read each tick. `S2C_WARLOCK_FIREBREATH` carries the end time and the reach, and the client draws your own flames that far until that world time.
- Skill tree: `Progression/Classes/WarlockSkills.java`. `Warlock` reads `WarlockSkills.INFERNAL_FIREBALLS` for the cooldown.
- Devscripts: `devscripts/warlock-passive.txt`, `devscripts/warlock-ranks.txt` (Fire Breath ranks).
