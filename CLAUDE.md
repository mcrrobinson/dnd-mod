# dnd-mod

## Project board

Work is tracked on the private GitHub Project "DnD Mod": https://github.com/users/mcrrobinson/projects/10 (owner `mcrrobinson`, number `10`).

- Add items as draft cards: `gh project item-create 10 --owner mcrrobinson --title "..." --body "..."`
- Don't create a BUGS.md or repo issues; the board is the single source of truth.
- Fields:
  - **Status**: Todo, In Progress, Done (new items start as Todo)
  - **Kind**: Epic, Task, Bug
  - **Epic**: Barbarian, Cleric, Druid, Fighter, Monk, Paladin, Rogue, Warlock, Artificer, Blood Hunter, Alchemist, Armor, Enchantments, Music, Mobs, Commands / Admin
- List fields/option IDs with `gh project field-list 10 --owner mcrrobinson`, set values with `gh project item-edit`.

## Working on a ticket or bug

Each ticket gets its own git worktree so multiple Claude sessions can work in parallel, each with its own Minecraft client for testing.

1. Find the card on the board and set its **Status** to In Progress.
2. Create a worktree on a new branch off `main`, named after the card (e.g. `fix/rage-damage`, `feat/monk-flurry`):
   `git worktree add ../dnd-mod-<branch-slug> -b <branch> main`
   Do all further work inside that worktree, not the main checkout.
3. Copy over files git doesn't track, which the build and client need:
   - `libs/*.jar` (if any are still untracked)
   - `run/` (world saves, options, mods), skipping `run/logs`
4. Implement the change and make sure `./gradlew build` passes.
5. Start the client in the background from the worktree (`./gradlew runClient`) so the user can test, and wait for their feedback before calling it done.
6. Once the user is happy, commit, push the branch and open a PR to `main`. Move the card to Done when it's merged.
7. Clean up with `git worktree remove ../dnd-mod-<branch-slug>` after merging.
