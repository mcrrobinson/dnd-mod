# Blood Hunter
A sword fighter that is strongest at night and can take over any mob it looks at.

![Blood Control: the Blood Hunter has taken the shape of a ravager](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/blood-hunter-blood-control.png)

## How it works
**Burning blades.** Every sword hit sets the target on fire for 8 seconds, like Fire Aspect II.

![A Blood Hunter's sword hit setting a zombie on fire at night](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/blood-hunter-burning-blade.png)

**Night and day.** With a sword in your main hand you deal double damage at night (day time 13000 to 23000) and half damage during the day.

**Swords stay in hand.** You can't throw a sword away, with Q in the world or with Q or a click outside the window in your inventory ("Blood Hunters cannot drop their swords!"). Swords still drop when you die, and creative mode isn't affected.

**Special (power-up key, full mana): Blood Control.** Look at a mob in range (not through walls) to become it for a while. You teleport to it and take its shape. When the time runs out, or you log off, the mob reappears where you're standing with its original health, name and gear. If there's no mob in your crosshair, nothing happens and you keep your mana.

Range, duration and success chance grow with the special's rank, which you raise with skill points at an Attunement Table:

| Rank | Class level | Duration | Success | Range |
|-|-|-|-|-|
| I | 0 | 8 s | 60% | 15 blocks |
| II | 3 | 12 s | 75% | 20 blocks |
| III | 6 | 16 s | 90% | 25 blocks |
| IV | 9 | 20 s | 100% | 30 blocks |

- **Strong mobs resist more.** Every point of max health above 20 (a player's) takes 0.25 percentage points off the success chance, at most 30. A zombie (20) has no penalty; an iron golem or ravager (100) has -20, so 80% at rank IV; a Warden has the full -30.
- **Failure.** The mob resists: you get a red "<mob> resists your Blood Control!" message, smoke rises from the mob, and you pay the blood price. The mana is spent and you take a heart of magic damage.
- **Bosses can't be controlled:** the Ender Dragon, the Wither and the mod's bosses (Wyvern, Lightning Chaser, Frost Drake, Lich, Goblin Warlord, Magmamuncher Alpha, Beholder). Aiming at one says "<boss> is too powerful to control." and keeps your mana. Taking one over would remove it from its fight (boss bar, phases, raid) and bring it back as a copy.

### Tips
Do your fighting at night and your mining and building in the day. A good sword with Sharpness matters more for you than for anyone else, because the night bonus doubles it. Blood Control drops you right where the mob was, so it doubles as a teleport (up to 30 blocks at rank IV). Early on, pick weak targets: a failure costs a heart as well as the mana.

### Skill tree
Press **O** to open your skill tree. Class XP gives you a skill point per level, up to level 10 at 2700 XP. Every class gets 2 XP for a hostile kill and 1 XP a minute for playing. Unlock skills from the tree anywhere; change your loadout (one active for the power-up key, two passives) and rank up abilities at an Attunement Table (see [Class progression](../systems/class-progression.md)).

Blood Hunters get 3 extra XP for each hostile mob killed with a sword at night.

| Skill | Branch | Type | Points | Mana | Effect |
|-|-|-|-|-|-|
| Blood Control | Root | Active | 0 | 9 | The special above (4 ranks) |
| Crimson Rite | Blood Curse | Passive | 1 | | Sword hits give Wither I for 2 s |
| Curse of Binding | Blood Curse | Active | 1 | 3 | The mob in your crosshair (up to 20 blocks) is frozen in place and glows for 4 s |
| Hemocraft | Blood Curse | Passive | 1 | | Heal half a heart per sword hit at night |
| Sunshield | Lycan | Passive | 1 | | Swords deal 75% damage in the day instead of 50% |
| Hybrid Transformation | Lycan | Active | 1 | 6 | Strength II, Speed I and Jump Boost II for 15 s |
| Predator | Lycan | Passive | 1 | | At night you get Night Vision, and hostile mobs within 16 blocks glow |
| Blood Moon | Capstone | Active | 2 | 9 | For 20 s, swords deal night damage (double) in the day too, and you heal 20% of the damage you deal |

Sunshield takes most of the sting out of daytime fights. Blood Moon is the strongest capstone in daylight, since it turns your 50% into 200%.

## Commands
- `/dndclass set <player> bloodhunter` (see [Admin commands](../systems/admin-commands.md)).

## Known limitations
- Blood Control needs the optional **Identity** mod.

## For developers
- Test: `devscripts/blood-hunter-ranks.txt` (ranks, boss refusal; needs Identity in `run/mods` or on the classpath).
- Fire and damage: `DnDClasses` (`AttackEntityCallback`) and `mixin/LivingEntityMixin.modifyDamageAmount`. No sword drops: `Misc/BloodHunterSwords` and the `*SwordDropMixin` mixins. Control: `Misc/BloodHunterControl` (`successChance`, strong-mob penalty and blood price constants, boss filter), `Misc/BloodHunterIdentityCompat`. Per-rank duration, success and range: `BloodHunterSkills.BLOOD_CONTROL`.
- Skill tree: `Progression/Classes/BloodHunterSkills.java`. The day/night multiplier is applied in `LivingEntityMixin` after the skill hooks, so Sunshield and Blood Moon scale day damage up first to land on their targets.
