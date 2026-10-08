# Fighter
A front-line tank. You hit hard, take hits well and pull monsters off your friends, but you can't use bows or potions.

## How it works
| Stat | Fighter | Vanilla |
|-|-|-|
| Max health | 26 (13 hearts) | 20 |
| Attack damage (base) | 6 | 1 |

Picking the class sets your health to 25.

**Attracts mobs.** Monsters that already hunt players switch to the nearest Fighter in range, even if they were chasing someone else. Mobs that only attack when angry, like zombified piglins and polar bears, still only go for Fighters they're angry at. Endermen are left alone.

**No bows or crossbows.** Using one is refused with "Fighters cannot use bows!".

**No potions.** You can't drink or throw potions, and potion buffs from any source (drinking, splash, lingering clouds, tipped arrows) don't apply to you. Harmful potion effects still do, so a witch's splash potion hurts you as normal.

**Special (power-up key, full mana): Super Regeneration.** Regeneration V for 10 seconds, about 2 hearts a second. It isn't a potion, so it works.

### Tips
In a group, stand in front: the mobs come to you, so your friends can shoot or cast in peace. You have no ranged attack, so carry a shield for skeletons and close in on them. Golden apples and beacons still work for you, since they aren't potions.

### Skill tree
Press **O** to open your skill tree. Class XP gives you a skill point per level, up to level 6 at 1000 XP. Every class gets 2 XP for a hostile kill and 1 XP a minute for playing. Unlock skills from the tree anywhere; change your loadout (one active for the power-up key, two passives) at an Attunement Table.

Fighters get 2 extra XP for each hostile mob killed in melee, and 3 more on top if at least 3 hostile mobs (counting the one you killed) were within 6 blocks.

| Skill | Branch | Type | Points | Mana | Effect |
|-|-|-|-|-|-|
| Super Regeneration | Root | Active | 0 | 9 | Regeneration V for 10 s |
| Improved Critical | Champion | Passive | 1 | | Critical hits (falling, not sprinting) deal 25% more damage |
| Action Surge | Champion | Active | 1 | 4 | Haste II, Speed II and Strength I for 8 s |
| Brawler | Champion | Passive | 1 | | +2 attack damage |
| Defensive Style | Battle Master | Passive | 1 | | +2 armor and +2 armor toughness |
| Riposte | Battle Master | Active | 1 | 3 | For 6 s, anything that hits you in melee takes half that damage back |
| Second Wind | Battle Master | Passive | 1 | | Dropping below 25% health heals 3 hearts, once a minute |
| Indomitable | Capstone | Active | 2 | 9 | Resistance II, Strength II and no knockback for 15 s |

None of these effects come from potions, so your potion ban doesn't block them. Riposte pays off when you're drawing a crowd, which a Fighter does anyway.

## Commands
- `/dndclass set <player> fighter` (see [Admin commands](../systems/admin-commands.md)).

## For developers
- Mob attraction: `Goals/PriorityPlayerTargetGoal` (attached on entity load). Bow and potion refusal: the `UseItemCallback` in `DnDClasses`. Potion immunity: `PotionImmunity` plus the `Potion*Mixin`, `AreaEffectCloudEntityMixin` and `ArrowEntityMixin`.
- Skill tree: `Progression/Classes/FighterSkills.java`. Riposte reflects as thorns damage, so two riposting Fighters don't bounce it back and forth.
- Devscript: `devscripts/brewing-use-fighter.txt`.
