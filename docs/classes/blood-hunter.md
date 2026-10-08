# Blood Hunter
A sword fighter that is strongest at night and can take over any mob it looks at.

## How it works
**Burning blades.** Every sword hit sets the target on fire for 8 seconds, like Fire Aspect II.

**Night and day.** With a sword in your main hand you deal double damage at night (day time 13000 to 23000) and half damage during the day.

**Swords stay in hand.** You can't throw a sword away, with Q in the world or with Q or a click outside the window in your inventory ("Blood Hunters cannot drop their swords!"). Swords still drop when you die, and creative mode isn't affected.

**Special (power-up key, full mana): Blood Control.** Look at a mob within 30 blocks (not through walls, and not the Ender Dragon or the Wither) to become it for 20 seconds. You teleport to it and take its shape. When the time runs out, or you log off, the mob reappears where you're standing with its original health, name and gear. If there's no valid target, nothing happens and you keep your mana.

### Tips
Do your fighting at night and your mining and building in the day. A good sword with Sharpness matters more for you than for anyone else, because the night bonus doubles it. Blood Control drops you right where the mob was, so it doubles as a 30-block teleport.

### Skill tree
Press **O** to open your skill tree. Class XP gives you a skill point per level, up to level 10 at 2700 XP. Every class gets 2 XP for a hostile kill and 1 XP a minute for playing. Unlock skills from the tree anywhere; change your loadout (one active for the power-up key, two passives) and rank up abilities at an Attunement Table (see [Class progression](../systems/class-progression.md)).

Blood Hunters get 3 extra XP for each hostile mob killed with a sword at night.

| Skill | Branch | Type | Points | Mana | Effect |
|-|-|-|-|-|-|
| Blood Control | Root | Active | 0 | 9 | The special above |
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
- Fire and damage: `DnDClasses` (`AttackEntityCallback`) and `mixin/LivingEntityMixin.modifyDamageAmount`. No sword drops: `Misc/BloodHunterSwords` and the `*SwordDropMixin` mixins. Control: `Misc/BloodHunterControl` (`RANGE`, `DURATION_TICKS`), `Misc/BloodHunterIdentityCompat`.
- Skill tree: `Progression/Classes/BloodHunterSkills.java`. The day/night multiplier is applied in `LivingEntityMixin` after the skill hooks, so Sunshield and Blood Moon scale day damage up first to land on their targets.
