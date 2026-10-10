# Races
Every player picks a **race** before their class: Human, Elf, Dwarf, Halfling, Gnome, Half-Orc, Tiefling or Dragonborn. A race is a small layer on top of your class. For now it changes a few body stats and adds its ability score bonuses; its traits arrive in later updates.

![The race picker](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/races/race-picker.png)

## How it works
### Picking
- **New player**: the race picker opens when you join, and the [class picker](../systems/class-selection.md) opens as soon as you've picked a race.
- **Existing player** (you have a class but no race): only the race picker opens, and chat says "Choose your heritage. Your class is unchanged."
- The picker is a grid of 8 races, two across. Hover a race to see its summary, ability bonuses, stat modifiers and traits. Escape doesn't close it: you have to pick.
- Picking **Dragonborn** swaps the grid for the three draconic ancestries (Ember, Frost, Storm) and a Back button.
- You get one pick. The server ignores any later pick, and only an operator can change your race with [`/dndrace set`](../systems/admin-commands.md#races).
- When you pick, chat prints the race's summary, stat modifiers, ability bonuses and traits, and your [Class Guidebook](../systems/class-guidebook.md) gains a "Your heritage" page at the back.

### The races
<!-- race-table:start - generated from src/main/resources/data/dndclasses/race_info.json, the same data the picker, chat intro and RaceStats use. Edit that file and run ./gradlew generateRaceTable -->
| Race | Ability bonuses | Size | Stat modifiers | Traits (planned) |
|-|-|-|-|-|
| **Human** | +1 to all six | Medium (1.0) | none | **Ambitious**: +10% class XP from every source<br>**Diplomat**: +2 on Persuasion and other social checks |
| **Elf** | DEX +2, WIS +1 | Medium (1.0) | Speed +5% | **Darkvision**: caves are dim but readable<br>**Keen Senses**: while sneaking, see hostile mobs within 12 blocks through walls<br>**Trance**: never need sleep, so phantoms never come for you |
| **Dwarf** | CON +2, STR +1 | Stocky (0.85) | Speed -8%<br>Max health +1 heart<br>Knockback resistance +20% | **Darkvision**: caves are dim but readable<br>**Dwarven Resilience**: poison lasts half as long and hurts half as much<br>**Stonecunning**: mine stone, deepslate and ores 20% faster |
| **Halfling** | DEX +2, CHA +1 | Small (0.65) | Attack reach -0.5 blocks | **Lucky**: a natural 1 on a d20 is rerolled once<br>**Small**: sneak through one-block gaps<br>**Second Breakfast**: food restores one extra hunger point |
| **Gnome** | INT +2, DEX +1 | Small (0.65) | Attack reach -0.5 blocks | **Darkvision**: caves are dim but readable<br>**Gnome Cunning**: 30% less damage from magic<br>**Tinker**: crafting a redstone component has a 25% chance to make one extra |
| **Half-Orc** | STR +2, CON +1 | Medium (1.0) | none | **Darkvision**: caves are dim but readable<br>**Relentless Endurance**: once every 10 minutes, a killing blow leaves you at 1 HP instead<br>**Savage Attacks**: critical hits deal x2.5 instead of x2 |
| **Tiefling** | CHA +2, INT +1 | Medium (1.0) | none | **Darkvision**: caves are dim but readable<br>**Hellish Resistance**: half damage from fire, lava and burning<br>**Hellish Rebuke**: a melee attacker is set on fire (20 second cooldown) |
| **Dragonborn** | STR +2, CHA +1 | Tall (1.05) | none | **Draconic Ancestry**: Ember (fire), Frost or Storm (lightning)<br>**Breath Weapon**: breathe your ancestry's element in a cone (R key)<br>**Draconic Resistance**: half damage from your ancestry's element |
<!-- race-table:end -->

- **Stat modifiers** are live now. They're attribute modifiers on top of your class's base values, so a race and a class never overwrite each other, and switching class keeps them. Speed is a percentage of your class's base speed: a Barbarian Dwarf walks at 0.08 × 0.92 = 0.0736.
- **Ability bonuses** are added to your [ability scores](../systems/ability-scores.md); `/dndclass sheet` lists them under the race's name.
- **Traits** and **size** are planned. They're listed so you can choose, but they do nothing yet.

### Saving
Your race and ancestry are saved on your player, so they survive logging out, dying and leaving the End. Join and respawn only re-apply the race's modifiers, so they never stack.

### Homes
Halflings and Dwarves have a home settlement where they get a welcome, a healing hearth, kin prices and kin trust: see [Racial homes](racial-homes.md).

## Where to find it
The race picker opens on its own. Your race is on the last page of the Class Guidebook.

## Commands
`/dndrace get|set|list` (operators). See [Admin commands](../systems/admin-commands.md#races).

## Configuration
`/gamerule dndRaces false` turns races off: nobody is prompted, and no race modifiers apply (saved races are kept and come back if you turn it on again). Turning it back on prompts every online player without a race. Default: `true`.

## Known limitations
- Only the stat modifiers and ability bonuses work. Traits, body sizes and the Dragonborn Breath Weapon come in later tickets.
- You can't change race yourself. Ask an operator.

## For developers
- Package `classes/Race/`:
  - `DndRace` (NONE 0, HUMAN 1, ELF 2, DWARF 3, HALFLING 4, GNOME 5, HALFORC 6, TIEFLING 7, DRAGONBORN 8) and `DragonAncestry` (NONE 0, EMBER 1, FROST 2, STORM 3). The ids are stable: NBT, DataTracker and packets use them.
  - `RaceInfo` loads `data/dndclasses/race_info.json`: name, icon, summary, `abilityBonuses` (STR/DEX/CON/INT/WIS/CHA), size and `scale`, `stats` and traits. `./gradlew generateRaceTable` writes the table above from it, and `check` (so `build`) fails if the table is stale.
  - `RaceLifecycle` is the one server path. `change` (picker and `/dndrace set`) swaps the modifiers, prints the intro and sends `dndclasses:approve_race_pick`. The pick packet `dndclasses:race_pick` (race varint, ancestry varint) is ignored unless the race is NONE and `dndRaces` is on, and Dragonborn must come with an ancestry. `ClassLifecycle` calls its `onJoin` (before the class query, so the race picker opens first), `copy` (COPY_FROM) and `afterRespawn` (before the class's health is set, so a Dwarf's extra heart counts) hooks.
  - Use `RaceLifecycle.activeRaceOf(player)` for traits: it's NONE while `dndRaces` is off. `raceOf` is the saved race.
  - `RaceStats.apply` removes then adds persistent modifiers with fixed UUIDs (`5a1d7c2e-3b0f-4e11-9a6e-7d2c1f0e5b01` speed `MULTIPLY_BASE`, `...02` max health, `...03` knockback resistance, `...04` `reach-entity-attributes:attack_range`, all `ADDITION` apart from speed), then caps health.
  - `RaceAbilityBonuses` is the hook for ability scores: `forPlayer(player)` returns the active race's bonuses, `CONTRIBUTOR_ID` is `dndclasses:race`, and `register()` (called from `RaceLifecycle.register()`) adds the bonuses to the sheet and sets `onChange`, which runs on every race change, to `AbilityScores.invalidate`.
- Storage: `mixin/PlayerEntityMixin` saves `DndRace` and `DndAncestry` ints in the player NBT, next to `DndClass`. The live value is a DataTracker byte (race in the low 4 bits, ancestry in the high 4) so every client knows every player's race, which racial sizes will need. `PlayerEntityExt.getDndRace/getDragonAncestry/setDndRace`; the setter also calls `calculateDimensions()`.
- Client: `Client/PickerFlow` handles the race and class queries and decides which picker to show (race first). `Client/Hud/RaceSelectionHud` is the LibGui picker.
- DevScript: while a script runs the race picker stays shut, because the dev-world player has a class but no race and every script would stall. `racepicker on` lets it open; `racepick <race> [ancestry]` sends a pick packet like clicking a button. Devscript: `devscripts/race-pick.txt`.
