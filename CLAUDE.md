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
- **Scripted, unattended runs**: `timeout 300 ./gradlew runClient -PdevScript=devscripts/<script>.txt`. The client joins, runs the script and quits on its own.
  - Scripted runs use a **hidden window that never takes keyboard focus**, so they run in the background while the user keeps working. Pass `-PdevHidden=false` to show it, or `-PdevHidden` to hide a plain `runClient`.
  - Don't drive clients with xdotool or other OS input: it steals the user's focus and drops keystrokes. Use the DevScript input steps instead.
  - Two players: the first client runs `/publish false survival 25599` (wait for `Started serving on` in its log), then start a second client with `-PdevRunDir=run-2 -PdevServer=localhost:25599`; it joins that world instead of opening its own (see `devscripts/lan-host-check.txt` and `lan-guest-check.txt`).
  - Several clients can run at once. Each one needs its own run directory, because a world can only be open once: copy `run/` to `run-2/` (skip `run/logs`) and pass `-PdevRunDir=run-2`. Each client uses about 2-3 GB of RAM, so check `free -g` first.
- DevScript steps (documented in `DevScript.java`):
  - `/command`, `wait <ticks>`, `screenshot <name>`, `hitboxes on|off` (F3+B), `hud on|off` (F1), `closescreen`, `quit`
  - Input, simulated inside the game: `look <yaw> <pitch>`, `use` (right click), `attack` (left click), `hotbar <0-8>`, `press <key binding translation key>` (e.g. `key.dnd-classes.power-up`), `perspective first|back|front` (F5)
  - Screens: `slot <index> [action] [button]` clicks a slot of the open screen (`pickup` by default, `quick_move` = shift-click, `swap <0-8>` = number key, `throw` = Q), `button <id>` (e.g. enchanting option 0-2), `rename <text>` (anvil), `slots` logs every non-empty slot so you can check results in the log.

  See `devscripts/headless-input-check.txt` (enchants a sword and attacks a zombie) and `devscripts/dragon-hitboxes.txt`.
- Check results yourself:
  - Read `<run dir>/screenshots/<name>.png` with the Read tool.
  - Grep `<run dir>/logs/latest.log` for `[DevScript]` steps and `slots` output, and for `[CHAT]` command feedback such as "Unknown or incomplete command". Prefer commands that print results: `/data get entity ...`, `/attribute @s ... get`, `/execute if block|entity ...` ("Test passed/failed").
- Script tips:
  - The class picker opens a couple of seconds after joining: `wait 40` then `closescreen`.
  - For repeatable shots, use absolute coordinates high in the air (e.g. y=150) and freeze mobs with `{NoAI:1b,NoGravity:1b,Rotation:[0f,0f]}`.
  - Aim with `/tp @s X Y Z <yaw> <pitch>` or `look`, with the target block or mob about 2 blocks ahead; `use` and `attack` act on whatever the crosshair hits.
  - Slot indexes depend on the screen: container slots first, then the 27 inventory slots, then the 9 hotbar slots (e.g. the enchanting table is 0 item, 1 lapis, 2-28 inventory, 29-37 hotbar). Run `slots` to see them.
  - Action-bar messages aren't logged; take a screenshot right after the action.
  - Hitbox rendering also draws dragon part shapes as green boxes.
- Stopping a client:
  - End scripts with `quit`. To stop one early, find its PID with `ps -eo pid,args | grep "[j]ava.*<worktree>"` and `kill` it.
  - Don't `pkill -f <pattern>`: the pattern matches your own shell, which then dies (exit 144).

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
