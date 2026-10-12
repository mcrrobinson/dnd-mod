# Character creation
A new player picks their race and class together on one screen: the race grid and the class grid side by side, a summary of the combined character, and a Confirm button.

![Character creation](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/combined-picker/cc-dwarf-barbarian-gui3.png)

## How it works
- **Who sees it**: a player with neither a [race](../races/races.md) nor a [class](class-selection.md) gets this screen a moment after joining. A player missing only one of them gets just that half: the race picker if they have a class, the class picker if they have a race (or races are off with the `dndRaces` gamerule).
- **Race** (top left): the 8 races, four across. Hover one for its summary, ability bonuses, stat modifiers and traits. Clicking **Dragonborn** swaps the race grid for the three draconic ancestries (Ember, Frost, Storm) and a Back button; your class choice stays.
- **Class** (bottom left): the 15 classes, four across, with their [party role](party-roles.md) icons and the class picker's tooltips.
- **Summary** (right): the combined result, updated as you click:
  - "Dwarf Barbarian" (a "?" for the half you haven't picked yet) and the class's roles.
  - Health in hearts and speed against vanilla: the class's base values with the race's modifiers on top (e.g. Dwarf Barbarian: 21 hearts, speed -26%), plus any other race stat (knockback resistance, reach).
  - The six [ability scores](ability-scores.md) with modifiers: the class's array plus the racial bonuses, in aqua where the race adds.
  - The race's traits and the class's special. When the full text doesn't fit, it shows the trait names only.
- **Confirm** is greyed out until you have a race (and an ancestry for a Dragonborn) and a class. Clicking it sends both picks at once.
- Escape doesn't close the screen: you have to pick.
- The server applies the race first, then the class (so the class's starting health counts the race's max health), and chat prints one intro for both: "The Dwarf Barbarian", the race's summary, body, ability scores and traits, then the class's pros, cons and special.
- Each half keeps its one-pick rule. After you've picked, the server ignores further picks, and only an operator can change your race or class (`/dndrace set`, `/dndclass set`).
- **Screen size**: at 600 scaled pixels wide or more the buttons have labels. On a narrower screen (a small window or a large GUI scale) the buttons are icons only, the name is in the tooltip, and the screen fits Minecraft's smallest scaled size. Changing the window or GUI scale while it's open switches the layout and keeps your picks.

## Where to find it
It opens on its own for new players. `/dndrace set <player> none` together with `/dndclass set <player> none` reopens it.

## Commands
`/dndrace set <player> none` reopens the race picker and `/dndclass set <player> none` the class picker; with both cleared the player gets this screen. See [Admin commands](admin-commands.md).

## Known limitations
- The ability scores in the summary are your level 1 scores. Later ability increases happen in the Character tab.

## For developers
- Client:
  - `Client/PickerFlow` decides which screen to show from the server's race and class queries: `CharacterCreationHud` when both are needed, else `RaceSelectionHud` or `ClassSelectionHud`. It waits for the first class query on join, so a new player doesn't see the race picker flash first.
  - After Confirm it waits for both the race and the class answer before switching screens (`awaitingAnswers`), so the first answer doesn't open the other half's picker.
  - `Client/Hud/CharacterCreationHud` (LibGui `WPlainPanel`) holds the picks in a `Choice` that survives the rebuild for the ancestry step and for a resize. `CharacterCreationScreen` rebuilds it when the scaled width crosses `WIDE_MIN_WIDTH` (600).
  - The class list is `ClassSelectionHud.CLASSES`; the class buttons are `RoleButton`s (icon-only ones leave out the role icon).
- Server: `CharacterCreation` handles `dndclasses:character_pick` (race varint, ancestry varint, class varint).
  - Each half applies only if the player still needs it: `RaceLifecycle.needsPick` and class NONE. A packet where a needed half is missing or invalid is ignored, and the client gets its race and class back.
  - It calls `RaceLifecycle.change(..., false)` then `ClassLifecycle.change(..., false)` (no intros), then `CharacterCreation.sendIntro` once. The single-pick paths and `/dndrace set` / `/dndclass set` use the same `sendIntro` with one half.
  - The separate `race_pick` and class pick packets still work.
- Numbers in the summary: `ClassStats.maxHealth` / `movementSpeed`, `RaceInfo.stats()`, `ClassAbilities` and `RaceInfo.abilityBonuses()`.
- DevScript: the class picker still opens on its own in scripts, and `closescreen` closes it. Scripts keep the race picker (and so this screen) shut unless they run `racepicker on`. `racepick` and the class-pick packet work as before. Devscript: `devscripts/character-creation.txt` (run with `-PdevWidth=1920 -PdevHeight=1080`; GUI scales 2, 3 and 4).
