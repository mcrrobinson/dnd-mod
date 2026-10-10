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

**Special (power-up key, full mana): Raise Dead.** Undead rise in a ring around you. They follow you, attack monsters, and vanish when their time is up. They also vanish if their chunk unloads or the server restarts, since they'd come back without their AI. Rank it up at an Attunement Table (see [Class progression](../systems/class-progression.md#ability-ranks)) for more and stronger undead:

| Rank | Class level | Undead | Kinds | Lasts |
|-|-|-|-|-|
| I | 0 | 2 | zombie, skeleton | 10 s |
| II | 3 | 3 | zombie, skeleton, spider | 12 s |
| III | 5 | 4 | husk, stray, spider, zombie | 14 s |
| IV | 7 | 5 | 2 wither skeletons, husk, stray, spider | 16 s |
| V | 10 | 5 | as IV, plus a [Bone Wyvern](../mobs/bone-wyvern.md) | 20 s |

- **Gear:** skeletons and strays carry bows and wither skeletons stone swords. Zombies and husks are unarmed at rank I, carry a stone sword at II and an iron sword from III. Nothing they carry or wear drops.
- **Daylight:** zombies, husks, skeletons, strays and wither skeletons wear a helmet (leather at ranks I-II, chainmail at III, iron from IV), so they don't catch fire in daylight. All of them have Fire Resistance against fire and lava.
- **Targets:** they only go after hostile mobs, never players or villagers. Spiders included.
- **Cap:** you can have at most 10 raised undead at once. Casting again while the last lot is still up only tops them up to 10. There's only ever one Bone Wyvern; casting again while it's alive doesn't raise another.
- Grave Pact, summon kill XP and the ally team apply to all of them, the Bone Wyvern included.

### Tips
At night the undead are no threat, so most danger comes from creepers and spiders. Your own hits are weak, so let summons, a bow or a sharp sword do the work. Undead mob farms are safe for you to walk through.

### Skill tree
Press **O** to open your skill tree. Class XP gives you a skill point per level, up to level 10 at 2700 XP. Every class gets 2 XP for a hostile kill and 1 XP a minute for playing. Unlock skills from the tree anywhere; change your loadout (one active for the power-up key, two passives) and rank up abilities at an Attunement Table (see [Class progression](../systems/class-progression.md)).

Necromancers get 1 extra XP for killing a mob that has Wither, 1 XP when Wither you applied finishes a hostile mob, and 3 XP whenever one of their summons kills a hostile mob.

| Skill | Subclass | Type | Points | Mana | Effect |
|-|-|-|-|-|-|
| Raise Dead | Root | Active | 0 | 9 | The special above: 2 to 5 undead for 10 to 20 s (ranks I-V), plus a Bone Wyvern at V |
| Bone Armor | Bonecaller | Passive | 1 | | +3 armor |
| Skeletal Archers | Bonecaller | Active | 1 | 5 | Two skeletons with bows fight for you for 60 s |
| Grave Pact | Bonecaller | Passive | 1 | | Your summons within 48 blocks get Strength I and Resistance I |
| Life Drain | Plaguebringer | Passive | 1 | | Heal 10% of the melee damage you deal |
| Wither Cloud | Plaguebringer | Active | 1 | 4 | Leaves a 4-block cloud where you stand for 6 s. Hostile mobs in it get Wither II for 3 s, refreshed every second. It doesn't touch you or your summons |
| Death's Embrace | Plaguebringer | Passive | 1 | | Your melee hits give Wither II for 3 s |
| Army of the Dead | Capstone | Active | 2 | 9 | Six undead, alternating zombies with iron swords and skeletons with bows, fight for you for 60 s |

Summons from the skill tree last a full minute, wear fire resistance so daylight doesn't burn them, and drop no gear. Death's Embrace makes the wither on your hits long enough to do damage.

### Subclasses
From class level 3, choose one of these two at an Attunement Table (see [Subclasses](../systems/class-progression.md#subclasses)). Your subclass's branch opens fully. The other branch's first node stays open, but the rest of it is sealed.

| Subclass | Branch | Nodes | Feature (free, doesn't use a passive slot) |
|-|-|-|-|
| Bonecaller | Right | Bone Armor (first node, open to both), Skeletal Archers, Grave Pact | **Undying Servants**: your summons last 25% longer |
| Plaguebringer | Left | Life Drain (first node, open to both), Wither Cloud, Death's Embrace | **Grim Harvest**: killing a mob that has your Wither heals you 2 hearts |

The features aren't active yet; they come with the subclass feature cards.

## Commands
- `/dndclass set <player> necromancer` (see [Admin commands](../systems/admin-commands.md)).

## Known limitations
- Without Death's Embrace, the Wither from melee hits is too short to do damage.
- Raised undead die and drop their normal loot (rotten flesh, bones, string) if something kills them before their time is up.

## For developers
- Wither: the `AttackEntityCallback` in `DnDClasses`. Undead: `mixin/ActiveTargetGoalMixin`. Summons: `PowerUpEffect.spawnUndead` through `SkillHelpers.spawnSummon`, and `Goals/FollowSummonerGoal`. Raise Dead's per-rank numbers are `NecromancerSkills.RAISE_DEAD` and the kinds `NecromancerSkills.raisedUndead(rank)`; raised units carry the tag `dndclasses.raised_dead` (`PowerUpEffect.MAX_RAISED`).
- Test: `devscripts/necromancer-ranks.txt` (counts at rank I and V, the Bone Wyvern's parts, and the despawn).
- Skill tree: `Progression/Classes/NecromancerSkills.java`. All summons join a scoreboard team named after your UUID, which is also how a summon's kill is traced back to you. The team is removed when your last summon is gone.
