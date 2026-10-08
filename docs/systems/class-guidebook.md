# Class Guidebook
A book that explains your current class: its pros, its cons, its special ability and the key that fires the special. Every player gets one on their first join and whenever their class changes. The text comes from the same data as the README class table, so the two always match.

## How it works
- Right click the book to open it. The pages are built when you open it, from your **current** class, so one book stays correct after a class change.
  - Page 1: class name and pros. Page 2: cons. Page 3: special ability.
  - The special page shows the key Power Up is actually bound to (`Z` by default) and where to rebind it (Options > Controls > Key Binds > D&D Classes). The special fires when your mana bar is full.
  - The Monk's special is passive, so its page says "Always active, no key needed".
  - Long entries carry onto the next page instead of being cut off.
- If you haven't picked a class yet, the book describes every class.
- Picking a class also prints that class's pros, cons and special in chat, from the same data.

## How to get it
- Given on first join (a player who has never left the world) and on every class change (class menu or `/dndclass set`), unless you already carry one. A chat line tells you when you receive it. If your inventory is full, it drops at your feet.
- Craft one from a book and a lapis lazuli (shapeless).
- In the D&D Classes creative tab.
- Stacks to 1.

## Known limitations
- The pages exist only on clients that have the mod installed. It isn't a vanilla written book.
- If you die without keepInventory, the book drops with the rest of your inventory. You get a new one when you pick your class again.

## For developers
- The data lives in `src/main/resources/data/dndclasses/class_info.json`: one entry per class with `id` (a `DndCharacter` name), `name`, `pros`, `cons`, `special` and `specialOnKey`.
  - A trailing ` (done)` is a dev status marker. It shows in the README only; the game strips it, along with markdown `*`.
- The README "Player Classes" table between the `class-table:start`/`class-table:end` markers is generated from this JSON. Edit the JSON, never the table, then run `./gradlew generateClassReadme`.
  - `checkClassReadme` runs as part of `check` (and so `build`) and fails if the table is stale.
- Key files:
  - `ClassInfo.java` loads the JSON.
  - `Items/ClassGuidebookItem.java` is the item. It opens the book through a client hook.
  - `Items/ClassGuidebook.java` gives the book on join and on class change. It's called from `ClassLifecycle.change`, respawn and the JOIN event.
  - `Client/Hud/ClassGuidebookScreen.java` builds the pages and splits them to the vanilla page size of 114x128 px.
  - `ClassLifecycle.sendIntro` prints the chat summary.
- Devscript: `devscripts/class-guidebook.txt` checks that a class change gives the book, a second change doesn't add another, and right click opens it.
