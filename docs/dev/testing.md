# Testing in the dev client
How to check a change in a real Minecraft client without anyone at the keyboard. The full reference is the "Testing in the dev client" section of [CLAUDE.md](../../CLAUDE.md#testing-in-the-dev-client). This page is a summary.

## How it works
- `./gradlew runClient` loads straight into `run/saves/New World` (pick another with `-PdevWorld=<folder>`).
- **Scripted runs:** `timeout 300 ./gradlew runClient -PdevScript=devscripts/<script>.txt` joins the world, runs the script one step per tick and quits. Scripted clients use a hidden window that never takes focus.
- **Script steps:** `/command`, `wait <ticks>`, `screenshot <name>`, `look`, `use`, `attack`, `hotbar`, `press <key>`, `slot`, `button`, `rename`, `slots`, `respawn`, `closescreen`, `quit` and more. They're documented in `Client/DevScript.java`.
- **Results:** read `<run dir>/screenshots/<name>.png`, and grep `<run dir>/logs/latest.log` for `[DevScript]` and `[CHAT]` lines.
- **Several clients / LAN:** give each one its own run directory (`-PdevRunDir=run-2`). A second player can join with `-PdevServer=localhost:25599` after the host runs `/publish false survival 25599`.

## Existing devscripts
`devscripts/` has one or more scripts per feature (named after it). Each script starts with `#` comment lines saying what it checks, how to run it and what to expect in the log or screenshots; keep that header when adding one. Good templates:
- `headless-input-check.txt`: enchant a sword and attack a zombie.
- `dragon-hitboxes.txt`: frozen mob plus hitbox screenshots.
- `lan-host-check.txt` / `lan-guest-check.txt`: two clients.

## Known limitations
- Minecraft 1.19.4 ignores `--quickPlaySingleplayer`; DevScript's auto-join does that job.
- Action-bar messages aren't logged, so take a screenshot instead.
- Don't drive clients with xdotool or other OS input.

## For developers
- `Client/DevScript.java`, `mixin/HiddenWindowMixin`, and the `runClient` setup in `build.gradle`.
