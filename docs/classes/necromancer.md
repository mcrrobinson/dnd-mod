# Necromancer
A weak melee fighter that the undead leave alone and that raises undead allies to do the fighting.

![A Necromancer with a raised zombie and skeleton](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-flesh-out/necromancer-front.png)

## How it works
| Stat | Necromancer | Vanilla |
|-|-|-|
| Attack damage (base) | 0.5 | 1 |
| Max health | 20 | 20 |

Picking the class drops your current health to 5. Your max health stays at 20, so eat and let it regenerate before you go anywhere. Rejoining and leaving the End keep your health as it was.

**Undead don't attack.** Zombies, skeletons, wither skeletons and other undead monsters won't target you. Spiders, creepers and the rest still will.

**Wither touch.** Every melee hit gives the target Wither I for 5 ticks. That's too short to do real damage, but it does mark the target: killing a withered mob gives you bonus class XP.

**Special (power-up key, full mana): Raise Dead.** A zombie and a skeleton appear beside you. They follow you, attack monsters, and vanish after 10 seconds. They also vanish if their chunk unloads or the server restarts, since they'd come back without their AI.

### Tips
At night the undead are no threat, so most danger comes from creepers and spiders. Your own hits are weak, so let summons, a bow or a sharp sword do the work. Undead mob farms are safe for you to walk through.

### Skill tree
Press **O** to open your skill tree. Class XP gives you a skill point per level, up to level 6 at 1000 XP. Every class gets 2 XP for a hostile kill and 1 XP a minute for playing. Unlock skills from the tree anywhere; change your loadout (one active for the power-up key, two passives) at an Attunement Table.

Necromancers get 1 extra XP for killing a mob that has Wither, 1 XP when Wither you applied finishes a hostile mob, and 3 XP whenever one of their summons kills a hostile mob.

| Skill | Branch | Type | Points | Mana | Effect |
|-|-|-|-|-|-|
| Raise Dead | Root | Active | 0 | 9 | The special above |
| Bone Armor | Bone | Passive | 1 | | +3 armor |
| Skeletal Archers | Bone | Active | 1 | 5 | Two skeletons with bows fight for you for 60 s |
| Grave Pact | Bone | Passive | 1 | | Your summons within 48 blocks get Strength I and Resistance I |
| Life Drain | Blight | Passive | 1 | | Heal 10% of the melee damage you deal |
| Wither Cloud | Blight | Active | 1 | 4 | Leaves a 4-block cloud where you stand for 6 s. Hostile mobs in it get Wither II for 3 s, refreshed every second. It doesn't touch you or your summons |
| Death's Embrace | Blight | Passive | 1 | | Your melee hits give Wither II for 3 s |
| Army of the Dead | Capstone | Active | 2 | 9 | Six undead, alternating zombies with iron swords and skeletons with bows, fight for you for 60 s |

Summons from the skill tree last a full minute, wear fire resistance so daylight doesn't burn them, and drop no gear. Death's Embrace makes the wither on your hits long enough to do damage.

## Commands
- `/dndclass set <player> necromancer` (see [Admin commands](../systems/admin-commands.md)).

## Known limitations
- Without Death's Embrace, the Wither from melee hits is too short to do damage.
- The Raise Dead zombie and skeleton burn in daylight like any other.

## For developers
- Wither: the `AttackEntityCallback` in `DnDClasses`. Undead: `mixin/ActiveTargetGoalMixin`. Summons: `PowerUpEffect.spawnUndead` (lifetime `UNDEAD_LIFETIME_TICKS`) through `SkillHelpers.spawnSummon`, and `Goals/FollowSummonerGoal`.
- Skill tree: `Progression/Classes/NecromancerSkills.java`. All summons join a scoreboard team named after your UUID, which is also how a summon's kill is traced back to you. The team is removed when your last summon is gone.
