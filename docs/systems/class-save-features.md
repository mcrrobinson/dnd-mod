# Class Save Features
Some classes are better at [saving throws](saving-throws.md) than their scores alone say. A Paladin's aura steadies everyone near them, Rogues and Monks dodge breath they'd otherwise take half of, Barbarians see danger coming, Fighters shrug off a failed save, Bards have a little training in everything, and a master Monk is proficient in every save. They're all automatic: you don't pick or equip them, apart from the Paladin's aura, which is a skill tree passive.

## How it works
| Class | Feature | From | Effect |
|-|-|-|-|
| Paladin | Aura of Protection | the Oath of Devotion passive | You and every player within 8 blocks add your CHA modifier (at least +1) to every saving throw. With several Paladins around, the best bonus counts; auras don't stack. A Downed Paladin's aura is down |
| Rogue, Monk | Evasion | level 7 | On a DEX save that halves damage (dragon breath and storms, Dragonborn breath, flame vents, puzzle vents): a success takes **no** damage and a failure takes **half** |
| Barbarian | Danger Sense | level 2 | Advantage on DEX saves, unless you're blinded or in Darkness |
| Barbarian | Rage | while Rage is on | Advantage on STR saves and Athletics checks |
| Fighter | Indomitable | level 9 | When you fail a save, roll it again; the second roll stands. Once every 5 minutes |
| Bard | Jack of All Trades | level 2 | Half your proficiency bonus (rounded down) on every check you aren't trained in, including plain ability checks |
| Monk | Diamond Soul | level 10 | Proficient in all six saving throws |

- The Paladin's CHA 14 gives +2, so a Wizard standing next to them rolls DEX saves at +4 instead of +2. The aura is re-checked once a second, so it appears and drops within a second of walking into or out of range.
- **Evasion** shows on the save lane as `no damage (Evasion)` or `half damage (Evasion)` in place of the attack's usual text. Effects that come with the damage still follow the save as before (a failed save against a Frost Drake still freezes you).
- **Indomitable** shows the failed die as its own row, `Indomitable (CON) 1+3=4 vs 14 ✘ rerolling`, and the second roll arrives in the row below half a second later. It covers every save that shows on your save lane; silent saves don't use it up. The cooldown is kept if you log out, and resets when the server restarts. This is separate from the Fighter tree's active **Indomitable** (Resistance, Strength, no knockback, and a death-save reroll).
- **Jack of All Trades** is the level 2 class feature. The College of Lore tree passive of the same name (+1 heart, 10% more damage) is unchanged.
- **Danger Sense** here is the Barbarian's. The Rogue's Danger Sense (Thief tree active, projectiles fly through you) is a different feature.
- Every feature adds a line to the breakdown in `/dndclass sheet`, for example `Aura of Protection (Steve): save:ALL +2`, `Evasion: feature half-damage DEX saves: ...`, `Rage: adv STR saves and Athletics`.

## Where to find it
On any player of those classes at those levels. Class levels come from XP (`/dndclass xp`); see [class progression](class-progression.md).

## Commands
| Command | What it does |
|-|-|
| `/dndclass sheet <player>` | Lists the save bonuses with each feature's line in the breakdown |
| `/dndclass xp <player> set <xp>` | Level 2 = 120, 7 = 1350, 9 = 2200, 10 = 2700 XP |
| `/dndclass forceroll <player> <n...>` | Rig the next naturals, e.g. `1 12` for a failed save then Indomitable's reroll |
| `/dm save <targets> <ability> <dc>` | Call a save to test with |

## Configuration
None. The numbers are constants in `ClassSaveFeatures` and `PaladinSkills`.

## Known limitations
- Evasion only changes saves whose attack uses `SaveResult.damage` with `halvesDamage()`. A DEX save with its own success rule (a pit: you catch the edge) isn't affected.
- Danger Sense checks blindness when the save is rolled, but the sheet line stays while you're blinded.
- The client's copy of the sheet catches up with the aura and Rage within a second.

## For developers
- `classes/Abilities/ClassSaveFeatures.java` registers everything from `AbilityScores.bootstrap`:
  - `class/save_features` (contributor): Jack of All Trades (`checkProficiencyFloor(HALF)`), Diamond Soul (`saveProficiency` on all six), Danger Sense (`advantage` with a filter that checks Blindness and Darkness at roll time), and `feature(...)` lines for Evasion and Indomitable.
  - `class/save_features_live` (dynamic contributor, rebuilt once a second): Rage (`BarbarianSkills.isRaging`; `rage()` also invalidates the sheet so it applies at once) and the aura (`PaladinSkills.auraSave`, which reads the Paladin's CHA with `AbilityScores.peek` so two Paladins' sheets never rebuild each other).
  - Evasion: a `SavingThrow.ResultHook` that returns `result.withDamageMultipliers(0, 0.5, Text "Evasion")` for a player's half-damage DEX save. Hooks run before the exposure cache, so every hit of one breath agrees.
  - Indomitable: a `SavingThrow.Reroll`. The failed roll is shown under the reroll's name with the detail "rerolling", then `rollDie()` runs again. Logged as `[D20] <player> Indomitable: rerolling save <n> (<outcome>)`.
- New, additive APIs:
  - `SavingThrow.registerResultHook(ResultHook)` and `SavingThrow.registerReroll(Reroll)`.
  - `SaveResult.withDamageMultipliers(onSuccess, onFailure, note)` and `note()`: the lane's detail becomes `no damage (<note>)` / `half damage (<note>)` / `full damage (<note>)`.
  - `Contribution.feature(description, source)`: a breakdown line only (target `feature`).
  - `AbilityScores.peek(player)`: the last built sheet without a rebuild. A dynamic rebuild that changes the sheet now also re-sends it to the client.
- Test script: `devscripts/class-save-features.txt` (single player). The Paladin-next-to-Wizard check needs two clients (see "Testing in the dev client" in `CLAUDE.md`).
