# Testing in the dev client
How to check a change in a real Minecraft client without anyone at the keyboard. The full reference is the "Testing in the dev client" section of [CLAUDE.md](../../CLAUDE.md#testing-in-the-dev-client). This page is a summary.

![An enchanting table loaded and read by a scripted, hidden-window DevScript run (slot and button steps)](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/testing-devscript-enchant.png)

## How it works
- `./gradlew runClient` loads straight into `run/saves/New World` (pick another with `-PdevWorld=<folder>`).
- **Scripted runs:** `timeout 300 ./gradlew runClient -PdevScript=devscripts/<script>.txt` joins the world, runs the script one step per tick and quits. Scripted clients use a hidden window that never takes focus.
- Pass `-PdevHidden=false` to watch a scripted run, or `-PdevHidden` to hide a plain `runClient`.
- **Script steps:** `/command`, `wait <ticks>`, `waitchat <text>`, `screenshot <name>`, `hud on|off`, `clearchat`, `hitboxes on|off`, `serverhitboxes on|off|measure`, `look <yaw> <pitch>`, `use`, `attack`, `mine on|off` (keep breaking the block at the crosshair), `hotbar <0-8>`, `press <key>`, `holdkey <key> on|off` (holds any key binding, by translation key), `sneak on|off`, `walk on|off` or `forward on|off` (holds the walk key), `fov <30-110>`, `sizes` (logs every player's pose, hitbox and eye height), `holduse on|off`, `perspective first|back|front`, `slot`, `button`, `rename`, `click <dx> <dy>`, `hover <dx> <dy>` (moves the cursor so a screen draws its tooltip), `widget <label>` (presses a screen button), `page <n|last>` (turns a book), `skill unlock|equip|rankup|bestiary|subclass <id>`, `escape`, `racepicker on|off`, `racepick <race> [ancestry]`, `slots`, `tooltips [hotbar slots]` (shows hotbar item tooltips for a screenshot), `respawn`, `closescreen` and `quit`. They're documented at the top of `Client/DevScript.java`.
- **Results:** read `<run dir>/screenshots/<name>.png`, and grep `<run dir>/logs/latest.log` for `[DevScript]` and `[CHAT]` lines.
- **Several clients / LAN:** give each one its own run directory (`-PdevRunDir=run-2`). A second player can join with `-PdevServer=localhost:25599` after the host runs `/publish false survival 25599`. To keep the two scripts in step, one sends `/say step2` (or `/me step2`, which non-ops can use) and the other waits with `waitchat step2`. `-PdevUser=<name>` fixes a client's player name, and so its UUID, so a guest that quits and rejoins comes back as the same player.

## Existing devscripts
`devscripts/` has one or more scripts per feature (named after it). Each script starts with `#` comment lines saying what it checks, how to run it and what to expect in the log or screenshots; keep that header when adding one. Good templates:
- `headless-input-check.txt`: enchant a sword and attack a zombie.
- `dragon-hitboxes.txt`: frozen mob plus hitbox screenshots.
- `lan-host-check.txt` / `lan-guest-check.txt`: two clients.

## Known limitations
- Minecraft 1.19.4 ignores `--quickPlaySingleplayer`; DevScript's auto-join does that job.
- Action-bar messages aren't logged, so take a screenshot right after the action instead.
- Looking straight down right after a teleport renders a dark frame. Use side views, or wait a few ticks before the screenshot.
- Don't drive clients with xdotool or other OS input.

## For developers
- `Client/DevScript.java`, `mixin/HiddenWindowMixin`, and the `runClient` setup in `build.gradle`.
