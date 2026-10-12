# Class Guidebook
A book that explains your current class: its pros, its cons, its special ability and the key that fires it. You get one on your first join, whenever your class changes and when you respawn without one. The text comes from the same data as the README class table, so the two always match.

![The first page of a Warlock's guidebook](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-flesh-out/guidebook.png)

## How it works
Right click the book to open it. It reads your **current** class each time you open it, so the same book stays correct after a class change.

| Page | Contents |
|-|-|
| 1 | Class name and pros |
| 2 | Cons |
| 3 | Special ability, and the key Power Up is bound to (`Z` by default) with where to rebind it (Options > Controls > Key Binds > D&D Classes) |
| 4 | Your role: primary and secondary [party role](party-roles.md), what your class does in a party and what the role is for |
| 5 | Obstacles you handle. Bold ones are your class's speciality (the obstacles it's the only primary solver of) |
| 6-7 | Your abilities: your class's starting scores (with modifiers), saving throws and skills (◆ expertise), then how ability points work at levels 4 and 8 ([Ability scores](ability-scores.md#ability-score-improvements)) |

If you have a [race](../races/races.md), two "Your heritage" pages follow at the back: the race (and Dragonborn ancestry), its summary, ability bonuses and stat modifiers, then its traits.

The **Obstacle Codex** chapter closes the book: an intro page, then every [class-gated obstacle](obstacles.md#catalogue), a few to a page. Each entry gives its name (and its look-alike variants), its **Specialist** (the one class it's built for) or **Best** classes, the classes that can **Also** try, whether anyone else can work it, and **Otherwise**: the fallback for everyone else. Your class is picked out in green. It's built from the obstacle registry, so a new obstacle shows up in it without editing the book.

![The Obstacle Codex in a Fighter's guidebook](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/obstacle-codex/codex-portcullis.png)

Long entries carry over onto the next page instead of being cut off. If you haven't picked a class yet, the book describes every class.

Picking a class prints the same pros, cons and special in chat.

## Where to find it
You get one on your first join, on every class change (the picker or `/dndclass set`), and when you respawn or leave the End with a class, unless you already carry one. Chat tells you when it arrives. If your inventory is full, it drops at your feet.

To make another, craft a book and a lapis lazuli together in any shape. It's also in the D&D Classes creative tab. It stacks to 1.

## Known limitations
- The pages only exist on clients with the mod installed. It isn't a vanilla written book.
- If you die without keepInventory, the book drops with the rest of your inventory and you respawn with a new one, so you end up with a spare once you collect your things.

## For developers
- The data lives in `src/main/resources/data/dndclasses/class_info.json`: one entry per class with `id` (a `DndCharacter` name), `name`, `pros`, `cons`, `special`, `specialOnKey`, and the [party role](party-roles.md) fields `role`, `secondaryRole`, `roleBlurb` and `obstacles`.
  - A trailing ` (done)` is a dev status marker. It shows in the README only; the game strips it, along with markdown `*`.
- The README "Player Classes" table between the `class-table:start` and `class-table:end` markers is generated from this JSON. Edit the JSON, never the table, then run `./gradlew generateClassReadme`. `checkClassReadme` runs as part of `check` (and so `build`) and fails if the table is stale.
- Key files:
  - `ClassInfo.java` loads the JSON.
  - `Items/ClassGuidebookItem.java` is the item. It opens the book through a client hook.
  - `Items/ClassGuidebook.java` gives the book on join and on class change. It's called from `ClassLifecycle.change`, respawn and the JOIN event.
  - `Client/Hud/ClassGuidebookScreen.java` builds the pages and splits them to the vanilla page size of 114x128 px. `codexPages` builds the Obstacle Codex from `Obstacles/ObstacleCodex.entries()`; `Pages.block` keeps one entry on one page.
  - `ClassLifecycle.sendIntro` prints the chat summary.
- Recipe: `data/dndclasses/recipes/class_guidebook.json`.
- Devscript: `devscripts/class-guidebook.txt` checks that a class change gives the book, a second change doesn't add another, and right click opens it.
