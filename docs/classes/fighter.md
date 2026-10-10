# Fighter
A front-line tank. You hit hard, take hits well and pull monsters off your friends, but you can't use bows or potions.

![A Fighter right after Super Regeneration, hearts refilling from a big hit](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/fighter-super-regeneration.png)

## How it works
| Stat | Fighter | Vanilla |
|-|-|-|
| Max health | 26 (13 hearts) | 20 |
| Attack damage (base) | 6 | 1 |

Picking the class sets your health to 25.

**Attracts mobs.** Monsters that already hunt players switch to the nearest Fighter in range, even if they were chasing someone else. Mobs that only attack when angry, like zombified piglins and polar bears, still only go for Fighters they're angry at. Endermen are left alone.

**No bows or crossbows.** Using one is refused with "Fighters cannot use bows!".

**No potions.** You can't drink or throw potions, and potion buffs from any source (drinking, splash, lingering clouds, tipped arrows) don't apply to you. Harmful potion effects still do, so a witch's splash potion hurts you as normal.

**Special (power-up key, full mana): Super Regeneration.** Regeneration V, about 3 hearts a second, for 4 seconds at rank I, 6 at II, 8 at III and 10 at IV. Rank it up at an Attunement Table (see [Class progression](../systems/class-progression.md#ability-ranks)). It isn't a potion, so it works.

### Tips
In a group, stand in front: the mobs come to you, so your friends can shoot or cast in peace. You have no ranged attack, so carry a shield for skeletons and close in on them. Golden apples and beacons still work for you, since they aren't potions.

### Skill tree
Press **O** to open your skill tree. Class XP gives you a skill point per level, up to level 10 at 2700 XP. Every class gets 2 XP for a hostile kill and 1 XP a minute for playing. Unlock skills from the tree anywhere; change your loadout (one active for the power-up key, two passives) and rank up abilities at an Attunement Table (see [Class progression](../systems/class-progression.md)).

Fighters get 2 extra XP for each hostile mob killed in melee, and 3 more on top if at least 3 hostile mobs (counting the one you killed) were within 6 blocks.

| Skill | Subclass | Type | Points | Mana | Effect |
|-|-|-|-|-|-|
| Super Regeneration | Root | Active | 0 | 9 | Regeneration V for 4 / 6 / 8 / 10 s (ranks I-IV) |
| Improved Critical | Champion | Passive | 1 | | Critical hits (falling, not sprinting) deal 25% more damage |
| Action Surge | Champion | Active | 1 | 4 | Haste II, Speed II and Strength I for 8 s |
| Brawler | Champion | Passive | 1 | | +2 attack damage |
| Defensive Style | Battle Master | Passive | 1 | | +2 armor and +2 armor toughness |
| Riposte | Battle Master | Active | 1 | 3 | For 6 s, anything that hits you in melee takes half that damage back |
| Second Wind | Battle Master | Passive | 1 | | Dropping below 25% health heals 3 hearts, once a minute |
| Indomitable | Capstone | Active | 2 | 9 | Resistance II, Strength II and no knockback for 15 s |

None of these effects come from potions, so your potion ban doesn't block them. Riposte pays off when you're drawing a crowd, which a Fighter does anyway.

### Subclasses
From class level 3, choose one of these two at an Attunement Table (see [Subclasses](../systems/class-progression.md#subclasses)). Your subclass's branch opens fully. The other branch's first node stays open, but the rest of it is sealed.

| Subclass | Branch | Nodes | Feature (free, doesn't use a passive slot) |
|-|-|-|-|
| Champion | Right | Improved Critical (first node, open to both), Action Surge, Brawler | **Superior Critical**: melee attack rolls crit on 18-20 |
| Battle Master | Left | Defensive Style (first node, open to both), Riposte, Second Wind | **Combat Superiority**: blocking a melee hit with a shield restores 1 mana pip (at most once every 5 seconds) |

How the features work:
- **Superior Critical** (Champion): your melee attack roll crits on a natural 18-20 instead of 19-20. It's on your character sheet, so the D20 HUD shows it.
- **Combat Superiority** (Battle Master): blocking a melee hit with a raised shield gives you 1 mana pip, at most once every 5 s. Arrows and other projectiles don't count.

## Commands
- `/dndclass set <player> fighter` (see [Admin commands](../systems/admin-commands.md)).

## For developers
- Mob attraction: `Goals/PriorityPlayerTargetGoal` (attached on entity load). Bow and potion refusal: the `UseItemCallback` in `DnDClasses`. Potion immunity: `PotionImmunity` plus the `Potion*Mixin`, `AreaEffectCloudEntityMixin` and `ArrowEntityMixin`.
- Skill tree: `Progression/Classes/FighterSkills.java`. Riposte reflects as thorns damage, so two riposting Fighters don't bounce it back and forth.
- Devscript: `devscripts/brewing-use-fighter.txt`.
