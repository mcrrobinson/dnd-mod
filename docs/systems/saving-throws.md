# Saving Throws
Saving throws let you resist monster attacks, traps and spells: a d20 plus your save bonus from the [character sheet](ability-scores.md) against a DC. Saves show as small rows beside the crosshair, so they never take the big roll panel away from a lockpick or a persuasion check, and repeated saves stack into one row with a counter instead of filling the screen.

![A lockpick on the big panel while a DEX save (passed) and a CON save (failed) show on the save lane](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/saving-throws/saves-with-lockpick.png)

## How it works
### The roll
- d20 + ability modifier + proficiency (if your class has that save) + flat bonuses from the sheet, against the DC.
- A natural 20 always succeeds and a natural 1 always fails. This is a house rule (5e has none for saves), kept so a save is never hopeless.
- **Advantage / disadvantage:** roll two d20s and keep the higher or lower. They cancel out. Sources on the sheet (race, items, class features) apply when they match the save's ability or tags.
- Lucky rerolls (Halfling) reroll a natural 1 once.
- On a success, the attack decides what happens: half damage, half the effect's duration, or no effect at all.
- **Pets, villagers and mobs** save too, with a flat +1 and nothing shown on anyone's HUD.
- **Creative and spectator** players roll but see nothing.

### One roll per exposure
A save is rolled once per *exposure* (one breath, one ray, one grab), not on every damage tick. The result is kept for the exposure's window, so all four hits of a dragon's breath use the same outcome.

### The save lane
- Rows sit to the right of the crosshair, stacking downward. Each is a small d20 that spins for 6 ticks, then one line: `DEX save 13+4=17 vs 14 ✔ half damage`.
- A row stays for 2 seconds (40 ticks), then fades over half a second. At most 3 rows show; a new one pushes out the oldest.
- A save with the same name and outcome as a row still showing adds to its counter (`×4`) and refreshes it, instead of adding a row.
- Natural 20s say `Nat 20`, natural 1s `Nat 1`. With advantage the dropped die shows grey in brackets: `17(5 adv)+4=21`.
- Success and failure sounds play at 40% volume. Natural 20s and 1s play at full volume.
- On a narrow screen the detail ("half damage") is left out so the numbers and the counter still fit.
- The server sends at most one save to a player every 10 ticks. Saves in between still apply, and arrive together as one row's counter: ten saves in the same second are two packets and one `×10` row.

![Ten saves in one tick: one row, ×10](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/saving-throws/saves-merge.png)

## Where to find it
Monsters and traps call saves as their attacks get them. So far: [dragon breath and storms](../mobs/dragons.md#saving-throws) (DEX). Beholder rays, the Lich, grabs and generic poison are planned. A DM can call one at any time with `/dm save`.

## Commands
| Command | What it does |
|-|-|
| `/dm save <targets> <ability> <dc> [adv\|dis] [silent]` | Each target rolls a save. Players see it on the save lane; mobs roll +1 silently. Results come back in chat |
| `/dm check <players> <skill> <dc> [adv\|dis] [silent]` | A skill check on the big d20 panel |
| `/dndclass forceroll <player> <n...>` | Rig the next d20 naturals, for tests |

Abilities are `str`, `dex`, `con`, `int`, `wis`, `cha` (or the full name). Skills tab-complete (`stealth`, `thieves_tools`...). `silent` rolls and logs without showing the player anything.

For command blocks and functions, a roll nobody succeeds at makes the command fail:
- `execute store success score @s ok run dm save @s dex 14` stores 1 or 0.
- `execute store result ...` stores the total for one target, or how many targets succeeded for several (0 on a failure).

See [Dungeon Master](dungeon-master.md) for who can use `/dm`.

## Configuration
`saveRolls` in `config/dndclasses/dndclasses.json` (client side; added with its default on first launch):

| Value | Effect |
|-|-|
| `compact` (default) | Rows beside the crosshair |
| `full` | Saves use the big roll panel, like a lockpick, unless it's showing a check (then they fall back to a row) |
| `off` | Nothing is drawn. Natural 20s and 1s still play their sound |

## Known limitations
- Only dragons call the API so far; the other attack saves (Beholder, Lich, grabs, poison) are separate tickets.
- The server's 10-tick throttle and merge only combine saves with the same name and outcome. Different saves arriving together go out one per 10 ticks.
- In `full` mode a DM-called save with no effect text shows a trailing dash after the outcome on the big panel.

## For developers
- `classes/SkillChecks/SavingThrow.java`: the builder.
  ```java
  SaveResult r = SavingThrow.of(victim, Ability.DEX, 14)
          .label("save.dndclasses.fire_breath")  // lane shows "Fire breath (DEX)"; default "DEX save"
          .source(dragon)
          .exposure(dragon, "breath", 60)        // one roll per breath (attacker UUID + tag), reused for 60 ticks
          .halvesDamage()                        // detail "half damage"/"full damage"
          .tags("fire")                          // for advantage filters on the sheet
          .roll();
  victim.damage(source, r.damage(4.0F));
  if (r.failed()) { /* the effect a success avoids */ }
  ```
  - Other builder options: `.dc(n)`, `.advantage()`, `.disadvantage()`, `.advantageIf(b)`, `.mode(Advantage)`, `.bonus(text, n)`, `.rerollNaturalOnes()`, `.onSuccess(text)` / `.onFailure(text)` (the words after the tick), `.silent()`, `.secret()`, `.exposure(String key, tag, ticks)` for non-entity sources such as a trap's position.
  - Players roll through `SkillCheck.save` (sheet bonus, sheet advantage, Lucky). Non-players roll `D20.roll(entity)` with `NON_PLAYER_BONUS` (+1) and `Display.SILENT`.
  - The exposure cache is keyed by target UUID, then attacker UUID + tag, by world time. It's pruned every 200 ticks and cleared for a player on disconnect (`SavingThrow.forget`, from `ClassLifecycle`).
- `SaveResult`: `succeeded()`, `failed()`, `outcome()`, `roll()`, `fresh()` (false when it came from the exposure cache), `damage(full)`, `duration(ticks)` (half on a success), `withDamageMultipliers(onSuccess, onFailure)` for Evasion-style features.
- `MobSaveInfo.register(type, labelKey, ability, dc, halvesDamage, effectKey)` returns an `Entry`; roll it with `SavingThrow.of(target, entry)` so the creature's listing and its attack use the same numbers. `MobSaveInfo.of(type)` lists them (for the Study card).
- Every `SAVE_LANE` roll sent with `D20.show` goes through `SavingThrow.send`: the 10-tick throttle and the merge by label + ability + outcome. The `dndclasses:d20_roll` packet has a trailing VarInt count after the detail text (1 for ordinary rolls).
- Logs: each roll is a `[D20] <name> <label> <natural> <+mod> = <total> vs DC <dc> -> <outcome>` line; each save packet is a `[D20] save lane -> <player>: <label> <outcome> x<count>` line.
- Client: `Client/Hud/SaveLaneHud.java` draws the rows; `DiceRollHud` routes SAVE_LANE packets to it (or to its own panel in `full` mode). `Config/SaveRollsMode` reads `saveRolls`; the DevScript step `saverolls full|compact|off|config` overrides it for a run.
- Commands: `dm/DmRolls.java`.
- Test script: `devscripts/saving-throws.txt`.
