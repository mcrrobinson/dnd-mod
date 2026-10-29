# Feats
Nine 5e feats that every class can take: Great Weapon Master, Sharpshooter, Lucky, Tough, Mobile, War Caster, Alert, Sentinel and Resilient. You get a free feat at class level 6, and at levels 4 and 8 you can spend your 2 ability points on a feat instead of +2 to your scores. Each feat is wired into the attack rolls, saves, health and movement systems the mod already has. You choose them in a picker on the Character tab at an Attunement Table.

![The feat picker at an Attunement Table](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/feats/feats-picker.png)

## How it works
### When you get feats
| Class level | What you get |
|-|-|
| 4 | 2 ability points, or spend both on one feat |
| 6 | One free feat (it doesn't use ability points) |
| 8 | 2 ability points, or spend both on one feat |

So a level 10 character has 1 to 3 feats.
- A feat is paid for with the free level 6 slot first, then with 2 unspent ability points. Each feat remembers what paid for it, so a refund gives back the right thing.
- Each feat can be taken once. Resilient is the exception: you can take it once for each ability.
- There are no prerequisites. Every class can take every feat, and War Caster has no spellcaster requirement.
- Feats are kept per class, like skill points and ability points, so switching class and back keeps them. They survive death and relogging.

"Heavy weapon" means an item in `#dndclasses:heavy_weapons` (every axe, the trident, the Orcish Greataxe and the Dwarven Waraxe) or any main-hand melee item with an attack speed of 1.0 or less.

### The feats
| Feat | What it does |
|-|-|
| **Great Weapon Master** | **Power Attack** (toggle with **N**): with a heavy weapon, your melee attack rolls fumble on 1-4 (a fumble cancels the swing, as a natural 1 does), and full-strength hits deal **+5 damage**. **Cleave** (always on): a critical hit or a kill with a heavy weapon readies your next swing at once (the attack cooldown is reset, on the server and on your screen) |
| **Sharpshooter** | **Steady Aim** (toggle with **N**): fully drawn bow shots and all crossbow shots deal **x1.4 damage** but spread **2.5 times wider**. **Long Shot** (always on): your arrows deal **x1.25** to anything they hit **24 or more blocks** from where they were loosed. **Past cover** (always on): shields don't block your arrows |
| **Lucky** | **3 luck points**. A failed check, a failed save or a fumbled attack of yours spends one to roll the d20 again; the second roll stands. The action bar says "Lucky: you roll again (2 left)". Points come back on a long rest, or at each dawn when the `dndRests` gamerule is off. Halfling Lucky (natural 1s) and Dark One's Own Luck go first |
| **Tough** | **+1 max health per class level** (+4 at level 4, +6 at level 6, +10 at level 10). It follows your level as you go up |
| **Mobile** | **+10% movement speed**. While **sprinting**, cobwebs, sweet berry bushes, powder snow, soul sand and honey **don't slow you**. **Hit and run**: a mob you hit in melee has **disadvantage** (x0.75 damage) on its melee attacks against you for **2 s** |
| **War Caster** | **Advantage on CON saves**. **Somatic hands**: the Warlock's fireballs also work with a weapon or shield in your main hand. **Opportunity bolt**: when a hostile mob that's targeting you comes within **3 blocks**, a free Magic Missile dart hits it (**2 force damage**, no roll), at most once every **10 s** |
| **Alert** | **First to act**: when a hostile mob first targets you, or a boss fight you're in starts, you get **Speed I and Haste I for 3 s** (at most once every 30 s). **Never caught napping**: your camp watch rolls have **advantage**. **No blind spots**: attacks against you never get advantage from your being Blinded |
| **Sentinel** | **Hold the line**: mobs you hit in melee get **Slowness III for 1 s**. **Guardian**: when a hostile mob within **4 blocks** of you hurts someone else (a player, hireling, pet or villager), your next melee hit on it within **3 s** has **advantage** on the attack roll and **Restrains** it for **1 s** (bosses shrug that off as usual). A marked mob has red sparks at its corners |
| **Resilient** | **+1 to one ability** (capped at 20, like every improvement) and **proficiency in that ability's saving throws**. Once per ability |

Feats are the one place where the sheet's choices change damage and health. The numbers are small and each one costs a choice.

### Power Attack and Steady Aim (N)
**N** flips Power Attack and Steady Aim together. The action bar says "Power Attack: on" (or "Steady Aim", or both). The setting is saved with the player. With neither feat, N tells you so and does nothing.

## Where to find it
- **Character tab** (O, or an Attunement Table): a **Feats** box under the six ability boxes lists your feats with their icons. Hover one for its full rules and what paid for it. The top line shows "Ability points: 2 · Feat slots: 1".
- At an Attunement Table, while you have a free feat slot or 2 ability points, a gold **Choose a feat** button opens the picker.
- **Picker**: a 3x3 grid of feat cards (icon, name, two-line summary; greyed out and "Taken" once you have it). Click one to see its rules and what pays for it ("Paid with your level 6 feat." or "Paid with 2 ability points."), then **Take it**. Resilient asks for an ability first; the ones you've already taken are greyed out. Only a Tome of Clear Thought can undo a feat.
- Taking a feat is announced in chat: "Matt the Fighter has taken the feat Great Weapon Master."
- Level-up chat: at level 6, "You can choose a feat at an Attunement Table (Character tab)."; at 4 and 8 the ability-point message ends "... or take one feat instead."
- **Class guidebook**: a **Feats** chapter after "Your abilities", with when you get feats and every feat's rules (yours marked "taken").
- **Character sheet**: Resilient shows in the score and save breakdowns as `Resilient (CON)`; War Caster and Alert show their advantage; the other feats are listed under **Features**.

### Respec
| Path | What happens to feats |
|-|-|
| [Tome of Clear Thought](../items/magic-items.md#tome-of-clear-thought) | Clears your subclass **and refunds every feat** of your current class (free slots and ability points come back). It's used up if there was a subclass or a feat to clear |
| `/dndclass resetprogress <player>` | Wipes the class's progress, feats included; Tough's and Mobile's bonuses come off |
| `/dndclass feat <player> remove <feat\|all>` | Removes and refunds |
| Class change | Feats belong to the class. The new class's feats (if any) apply instead |

## Commands
| Command | What it does |
|-|-|
| `/dndclass feat <player> add <feat> [ability]` | Grants a feat without a table or points (paid as a free feat; it may go past the slots, and later slots pay it off, as admin unlocks do). Resilient needs an ability |
| `/dndclass feat <player> remove <feat\|all> [ability]` | Removes it (for Resilient, one ability or every one) and refunds |
| `/dndclass feat <player> list` | Prints the feats with what paid for each, free slots and ability points left, Lucky's points and the N toggle. Returns the number of feats |

Feat ids: `great_weapon_master`, `sharpshooter`, `lucky`, `tough`, `mobile`, `war_caster`, `alert`, `sentinel`, `resilient`.

## Known limitations
- The N toggle covers both Power Attack and Steady Aim; you can't have one on and the other off.
- Great Weapon Master's +5 only applies to the full-strength swing that rolled its attack (not to sweep hits on other mobs).
- Mobile's speed bonus multiplies your base speed, so it also stacks with sprinting and Speed effects as vanilla modifiers do.
- Alert's "no blind spots" only removes the Blinded advantage; nothing in the mod gives invisible mobs advantage yet.
- Long Shot measures from where the arrow was loosed to where it hit, not from the shooter's position at the time of the hit.

## For developers
Package `classes/Feats/`:
- `Feat`: the enum (id, name, two summary lines, full description, icon cell). The picker, the Character tab, the guidebook and the command all read it.
- `FeatChoice(feat, ability, source)`: one taken feat. `ClassProgress.feats` is written to NBT `feats` as a list of `{id, ability?, source: "free"|"asi"}` next to `asi`, so every class keeps its own. `ClassProgress.FEAT_LEVEL` = 6, `FEAT_COST` = `ASI_POINTS_PER_LEVEL` (2), `freeFeatSlots()`, `freeFeatSlotsLeft()`, `featsTaken()`, `canAffordFeat()`, `nextFeatSource()`, `hasFeat(feat[, ability])`. `asiPointsSpent()` counts 2 per `asi` feat.
- `Progression.chooseFeat(player, feat, ability, force)` and `Progression.removeFeats(player, feat|null, ability|null)`: save, sync and invalidate the sheet. `[Feats]` log lines record each choice and each effect that fires.
- `Feats`: `has(player, feat)` (both sides; on the client only for the local player), `isHeavy(stack)`, `castingHandFree(player)`, `refresh(player)` (Tough's `GENERIC_MAX_HEALTH` modifier `5f1c0b7e-...` and Mobile's `GENERIC_MOVEMENT_SPEED` MULTIPLY_BASE 0.10 modifier `0c3f8f2a-...`, temporary, re-applied from `Progression.sync` and every second), the sheet contributor `dndclasses:feats`, and persistent data `dndFeat` (`Toggle`, `LuckUsed`, `LuckDay`), copied on respawn with the rest of the player's data.
- `FeatEffect` / `FeatEffects`: the hook shape of `ClassSkills` and `MagicEffect` (`modifyDealtDamage`, `modifyTakenDamage`, `secondTick`, `fastTick` every 5 ticks, `onKill`), called from `ProgressionEvents` after the class and magic item hooks, for the current class's feats only. `FeatEffects.afterAttack` (Cleave) is called from `AttackRolls.endAttack`, and `FeatEffects.onDamage` (Sentinel's Guardian mark) from `ProgressionEvents.modifyDamage`.
- New hooks other features can use:
  - `D20.registerFumbleRange(ToIntBiFunction<LivingEntity, RollKind>)`: a fumble range for some kinds of roll only (Power Attack: ATTACK, 1-4).
  - `ConditionEffects.registerMobAttackMode((attacker, target) -> Advantage)`: adds to a mob melee attack's advantage/disadvantage count (Mobile's hit and run; kobold Pack Tactics can use the same hook).
  - `ConditionEffects.registerNoBlindSpots(Predicate<LivingEntity>)`: targets whose Blinded condition gives attackers no advantage (Alert).
  - `AttackRolls.isFullStrength(player, target)` and `isCriticalSwing(player, target)` for the swing in progress.
- Sharpshooter: `FeatArrows` and the `FeatArrow` duck interface on `PersistentProjectileEntity` (`PersistentProjectileHitMixin`, saved as NBT `dndFeat {flags, x, y, z}`), marked in `BowItemMixin` and `CrossbowItemMixin`; `ShieldArrowMixin` (`LivingEntity.blockedByShield`). Mobile: `EntitySlowMovementMixin` (`Entity.slowMovement` and `getVelocityMultiplier`). Cleave: `LivingEntityAccessor` (`lastAttackedTicks` = 100) and S2C `dndclasses:feat_cleave`.
- Packets: C2S `dndclasses:choose_feat {id, ability or ""}`, C2S `dndclasses:feat_toggle`. Key `key.dnd-classes.feat-toggle` (N) in `Client/FeatKeyClient`.
- UI: `Client/Hud/FeatPickerScreen`, the Feats box in `CharacterSheetPanel` (the passive scores moved under the skill list), the guidebook's `featsPages`. Icons: `textures/gui/feats.png`, 9 cells of 16x16, from `tools/feat_icons.py`.
- Devscript step `skill feat <id>[:<ability>]` sends the picker's confirm. Scripts: `devscripts/feats.txt` (slots, payment, Tough, Resilient, the picker, the Character tab, the guidebook, respec; run at `-PdevWidth=1920 -PdevHeight=1080`), `devscripts/feats-combat.txt` (Great Weapon Master, Mobile, Sentinel, Alert, War Caster, Lucky), `devscripts/feats-sharpshooter.txt` (Steady Aim, Long Shot), `devscripts/feats-relog-set.txt` then `feats-relog-check.txt` (feats, toggle and luck points survive a relog), and `devscripts/feats-shield-host.txt` with `feats-shield-guest.txt` (an arrow past a shielding player, over LAN).
