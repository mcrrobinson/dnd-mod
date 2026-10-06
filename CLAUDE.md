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

## Testing in the dev client

- `./gradlew runClient` loads straight into the dev world `run/saves/New World` (`DevScript` auto-join; pick another world with `-PdevWorld=<folder>`). Minecraft 1.19.4 ignores `--quickPlaySingleplayer`, which only exists from 1.20.
- **Scripted, unattended runs**: `timeout 300 ./gradlew runClient -PdevScript=devscripts/<script>.txt`. The client joins, runs the script and quits on its own. Steps are documented in `DevScript.java`:
  - `/command`
  - `wait <ticks>`
  - `screenshot <name>`
  - `hitboxes on|off` (F3+B)
  - `hud on|off` (F1)
  - `closescreen`
  - `quit`

  See `devscripts/dragon-hitboxes.txt` for an example.
- Check results yourself:
  - Read `run/screenshots/<name>.png` with the Read tool.
  - Grep `run/logs/latest.log` for `[DevScript]` steps, and for `[CHAT]` command feedback such as "Unknown or incomplete command".
- Script tips:
  - The class picker opens a couple of seconds after joining: `wait 40` then `closescreen`.
  - For repeatable shots, use absolute coordinates high in the air (e.g. y=150) and freeze mobs with `{NoAI:1b,NoGravity:1b,Rotation:[0f,0f]}`.
  - Hitbox rendering also draws dragon part shapes as green boxes.
- Stopping a client:
  - End scripts with `quit`, or use the pause menu (Save and Quit).
  - Don't `pkill -f <pattern>`: the pattern matches your own shell, which then dies (exit 144).
- Manual fallback for driving a running client:
  - The game runs under XWayland. `xdotool search --name Minecraft` finds the window.
  - `xdotool key slash` + `xdotool type "..."` + `xdotool key Return` runs commands.
  - `xwd -id <window> -out f.xwd && ffmpeg -i f.xwd f.png` captures the window, including menus where F2 doesn't work.

## Reading Minecraft / library code

- Minecraft sources aren't decompiled. Inspect the named jar with `javap -p -c`: `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-*/**/minecraft-merged-*.jar`.
- GeckoLib ships sources: `.gradle/loom-cache/remapped_mods/**/geckolib-fabric-1.19.4-4.2-sources.jar`.
- GeckoLib 4.2 bug: `GeoBone.getLocalSpaceMatrix()` and `getWorldSpaceMatrix()` have an extra identity matrix added (`RenderUtils.translateMatrix`). Subtract it before use (see `DragonRenderer`).

## Repo notes

- `build/` is tracked in git, so builds show changes under it. Don't commit them.
- Big GeckoLib mobs get extra hit shapes through `MultipartDragon`:
  - Implement it and list the model's bone groups in a `DragonPartLayout`.
  - Render the mob with a `DragonRenderer`.
  - Parts follow the animated model on the client and the rest pose on the server.
