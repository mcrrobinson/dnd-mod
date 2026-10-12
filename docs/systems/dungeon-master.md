# Dungeon Master
An op can run a session as the party's Dungeon Master: step outside the game, hide behind a veil, drop premade encounters where they're looking, and pause mobs and players. It can also call for rolls that pop up on the players' d20 HUD, narrate, speak through NPCs, set the music, and hand out or plant treasure. The DM Wand puts the common actions on right-click, and the DM panel (key K) puts them on buttons.

## How it works
### DM mode
`/dm on` makes you a DM; `/dm off` ends it (and lifts the veil). While in DM mode you are outside the game:
- Mobs never target you (`ActiveTargetGoal` drops DMs as targets).
- You take no share of party XP, and don't show on your party members' HUD.
- You never count as a [goblin raid](goblin-raids.md) participant (no reward or advancement), raids don't start because of you, and raiders don't go for you.
- [Boss fights](../bosses/boss-fights.md) don't start or keep going because of you: a boss only counts as fighting when a non-DM player is its target or attacker.
- Your kills earn no [bounty](../structures/hobbit-tavern.md) progress.
- You can't be frozen.

DM mode is saved with the world, so it survives restarts and relogs.

### Veil
`/dm veil [on|off]` (no argument toggles; veiling also turns DM mode on). A veiled DM:
- isn't sent to non-DM clients at all: no model, armour, held items, name tag or particles. Other DMs still see you.
- can fly, takes no damage, makes no sounds (no footsteps) and can't be targeted by mobs.
- can still use blocks, chests and items, unlike spectator mode.

The veil is re-applied every second, so changing game mode doesn't drop it. You still appear in the tab list.

### Encounters
`/dm encounter spawn <id>` drops a premade group of mobs at the block you're looking at (up to 64 blocks away), or at `<pos>`. Each mob is placed on safe ground (a solid floor, two free blocks above it, no fluid) within the encounter's `spread` of that block, made persistent and turned to face the nearest player. Add `frozen` to spawn them already frozen.

Chat reports the result: `Encounter #7 "Goblin Patrol": 3 mobs at 1010, 151, 1010`. Every mob gets the tags `dndclasses.dm_encounter` and `dndclasses.enc.<n>`, so you can pick them out with selectors (`@e[tag=dndclasses.enc.7]`). A Lich's phylactery picks up its Lich's tags.

`/dm encounter clear <n>|all` removes an encounter's mobs with a puff of smoke. They don't die, so nothing drops. `/dm encounter list` lists the encounter files and the live encounters with how many of their mobs are still around (loaded ones only).

`/dm encounter list` counts every loaded entity carrying the encounter's tag, so a Lich court shows its phylactery as an extra.

Encounter mobs give the normal drops, XP and bounty credit, because the DM decides the story. An encounter with `"loot": false` drops nothing, gives no XP and earns no class XP or bounty credit, like boss minions.

The launch set:

| Id | Mobs | Difficulty |
|-|-|-|
| `goblin_patrol` | 2-3 Goblin Warriors | medium |
| `goblin_warband` | 5 Goblin Warriors and a Goblin Warlord | deadly |
| `owlbear_ambush` | 1 Owlbear | medium |
| `owlbear_den` | 2 Owlbears | hard |
| `undead_crypt` | 4 zombies, 2 skeletons, 1 wither skeleton | hard |
| `restless_dead` | 6 zombies | medium |
| `mimic_hoard` | 1 Mimic and 2 treasury chests (`chests/dwarven_fortress_treasury`, un-rolled so a Rogue has to pick them) | medium |
| `gelatinous_corridor` | 1 Gelatinous Cube | hard |
| `wyvern_pair` | 2 Wyverns | deadly |
| `lich_court` | the Lich (which places its phylactery) and 4 skeletons | deadly |

### Calling for rolls
`/dm save @a dex 14` makes every player roll a Dexterity save: it pops up on their [save lane](saving-throws.md) and each result comes back to you in chat (`Player: Dexterity Save 13 +4 = 17 vs DC 14 -> SUCCESS`). `/dm check @p stealth 15` does the same with a skill check on the big d20 panel. Add `adv` or `dis` for advantage or disadvantage, and `silent` to roll without showing the players. Mobs can be targets of `/dm save` too (flat +1, nothing shown). For command blocks: a roll nobody succeeds at makes the command fail, so `execute store success` gives 1 or 0, and `execute store result` gives the total (one target) or how many succeeded.

### Roll calls
`/dm roll <players> <check> <dc> [advantage] [disadvantage] [secret] [auto]` asks each player for a roll on their [d20 HUD](d20-skill-checks.md#dm-roll-calls):
- `<check>` is `str`, `dex`, `con`, `int`, `wis` or `cha` for a saving throw, any skill id (`perception`, `stealth`, `athletics`, `thieves_tools`, ...) for a skill check, or `flat` for a plain d20. Modifiers come from the player's character sheet (ability modifier, proficiency, Jack of All Trades and so on); saves go through the saving-throw rules, so Indomitable still works.
- Each player sees a prompt with the check, the DC and "[Y] Roll", and has 20 seconds to press Y (the roll key, rebindable). After that the server rolls for them. The client only says "roll now": the server rolls, so nobody can send a result.
- `auto` rolls for everyone at once with no prompt. `secret` hides the DC and the outcome from the players (they see their total); you still see everything. `advantage` / `disadvantage` (or `adv` / `dis`) roll two dice and keep the higher / lower, and the HUD shows both.
- Natural 20 and natural 1 pass and fail automatically, for saves too (as everywhere in the mod).
- Players in DM mode are left out of a call, unless they're the only one picked (so `/dm roll @s dex 14` works for testing).
- Results come back to you in chat as they arrive (`#3 Alice 19 (nat 16 +3) PASS`, with "(timed out)" when the server rolled), then a summary once everyone has rolled: `Dexterity Save DC 14: Alice 19 (nat 16 +3) PASS · Bob 7 (nat 7 +0) FAIL · 1 of 2 failed [Passed] [Failed]`. Clicking **[Passed]** or **[Failed]** puts `/dm apply pass ` or `/dm apply fail ` in your chat box.
- A player who still has a prompt open when a new call reaches them rolls the old one first. A player who logs out before rolling shows as "absent".

`/dm apply <pass|fail|all> <command>` (op only) runs a command as and at each player of the last roll call who passed, failed, or all of them, like `execute as <player> at <player> run <command>`. For example `/dm apply fail effect give @s minecraft:slowness 10`.

### Narration and NPC voice
- `/dm narrate <text>` sends a narration line to every player: gold italics, starting with `✦`, with a soft page-turn sound. Wrap a word in asterisks for bold: `/dm narrate The door groans *open*.`
- `/dm title <title> [| <subtitle>]` shows a title (and subtitle) on every player's screen for about 5 seconds (fade in 10, stay 70, fade out 20 ticks), with a low drum hit. `/dm title Chapter One | The Sunken Crypt`.
- Both go to everyone, unless the text starts with a target selector: `/dm narrate @a[distance=..20] Only you hear it.` Pick a player by name with `@a[name=Alice]` (a bare name would read as the first word of the line).
- `/dm say <entity> <text>` speaks as an NPC: `<Gimli Stonebeard> Mind your step.` in chat for every player, the name in the colour of the NPC's [faction](factions.md) (yellow for anyone outside a faction), and a speech bubble pops up over its head for 3 seconds. Any entity works, including role NPCs and mobs with a custom name.

### Mood music
`/dm mood combat|tavern|dungeon|boss|off` (op only) forces a music track for every player within 96 blocks of you, through the same path as [boss fight music](../music.md): it starts straight away, loops, and plays over anything else.

| Mood | Track |
|-|-|
| `combat` | The goblin raid track (`music.goblin_raid`) |
| `tavern` | The Bard's lute song (`instrument.lute`) |
| `dungeon` | The dungeon pool (`music.dungeon`) |
| `boss` | The dragon fight track (`music.dragon_fight`) |
| `off` | Back to normal music, for every player online |

Setting a new mood replaces the old one for the players in range. A real boss fight that starts during a mood keeps the mood track until `off` (the first track a player got wins).

### Loot
- `/dm loot give <players> <loot table>` rolls a loot table (any table, as a chest would) straight into each player's inventory; what doesn't fit drops at their feet. You see what each player got.
- `/dm loot give <players> magic <rarity>` gives one roll on the [magic item](../items/magic-items.md) table of that rarity: `uncommon`, `rare`, `very_rare` or `legendary` (unidentified, like found loot).
- `/dm loot plant <loot table> [<pos>] [dc <n>]` (op only) places a chest at the block you're looking at (64 blocks), or at `pos`, facing you, with the loot not rolled yet. A `chests/...` table makes it locked for [Lockpicking](d20-skill-checks.md) like any dungeon chest (DC 10, 15 for the hard hoards). `dc <n>` (1-30) sets the lock DC instead, and locks the chest whatever its table: the "we need a Rogue" chest. Only a Rogue can pick it; anyone can still smash it open and ruin part of the loot.
- `/dm loot plant mimic [<loot table>] [<pos>]` (op only) places a dormant [Mimic](../mobs/mimic.md) instead, holding that loot (default `minecraft:chests/simple_dungeon`).

The DC is saved on the chest as `DndLockDc` (`/data get block <pos>` shows it, and `/data merge block <pos> {DndLockDc:18}` sets it on any chest).

### Freeze
`/dm freeze <targets>`, `/dm freeze radius [r]` (mobs and players within `r` blocks, default 24), `/dm unfreeze <targets>`, `/dm unfreeze radius [r]` and `/dm unfreeze all`.
- **Mobs** stop thinking (`NoAI`) and moving, glow, and hang in place if they were in the air (`NoGravity`). Their old `NoAI`, `NoGravity` and glowing are kept and put back on unfreeze.
- **Players** can't move, jump or fly (held where they were frozen; a `/tp` moves the hold point), attack, break or use blocks, use items or entities, or use their class power. The action bar reads "The Dungeon Master holds the scene." The freeze is saved on the player, so logging out and back in doesn't escape it.
- Frozen mobs and players take no damage, so nobody gets free hits during a pause. Boss bars stay up.
- Frozen entities have the tag `dndclasses.dm_frozen`.

### DM Wand
The DM Wand (`dndclasses:dm_wand`, in the D&D Classes creative tab or `/give @s dndclasses:dm_wand`) puts the common DM actions on right-click. It only works in DM mode: anyone else gets "The wand is cold in your hand." on the action bar and nothing happens.

It reaches 64 blocks: the server casts a ray from your eye and takes the first mob or player along it, else the first block. Clicks on blocks and mobs don't open chests or start trades while you hold it.

**Sneak + right-click air** (nothing within 64 blocks) switches mode: Encounter, Freeze, Inspect, Blink, then back. The action bar shows the new mode, and the item's name shows the current one (`DM Wand (Freeze)`). Right-click air without sneaking just shows the mode again.

| Mode | Right-click | Sneak + right-click |
|-|-|-|
| Encounter | Spawns the selected encounter at the block you hit, like `/dm encounter spawn <id> <pos>` (numbered, tagged, listed in `/dm encounter list`) | Selects the next encounter (in id order). The wand remembers it |
| Freeze | A mob or player: freezes it, or unfreezes it if frozen. A block: freezes every mob within 16 blocks of it, or unfreezes them if all were already frozen | Same |
| Inspect | A mob or player: prints its details to you only (see below) | Same |
| Blink | Teleports you onto the block you hit (the first spot with room to stand, up to 8 blocks up) | Toggles the veil, like `/dm veil` |

Inspect shows:
- **Mobs:** HP, current target, frozen or not, encounter number, faction, and all command tags.
- **Players:** class and level, party role, race, HP, frozen or not, reputation tier and value with every faction, and active quests with their stage.

The mode and the selected encounter are stored on the item, so two wands can be set up differently.

### DM panel
Press **K** (rebindable, "DM Panel" under D&D Classes) to open the DM panel, so you don't have to type commands mid-session. It opens only for players who can use `/dm` (ops, and players given `/dm grant`); anyone else gets "Only the Dungeon Master can use the DM panel." in chat. It doesn't pause the game.

- **Toggles** along the top: DM mode, the veil, and Freeze all (24), which freezes everything within 24 blocks of you and then turns into Unfreeze all while anything is frozen. The title line shows how many encounters are live and how many creatures are frozen.
- **Encounters** on the left: every encounter file, coloured by difficulty (medium pale yellow, hard orange, deadly red) with its mob count. Click one, then **Spawn at crosshair** spawns it at the block you're looking at (64 blocks), frozen if **Spawn frozen** is ticked. **Clear all** removes every live encounter.
- **Roll call** on the right: pick the check (d20, the six saves, then the skills), type the DC (1-40), pick Normal, Advantage or Disadvantage, tick **Secret** if you like, then **Call roll**. Everyone not in DM mode gets the [roll prompt](#roll-calls).
- **Narrate**: type a line (`*word*` for bold) and press Enter or **Narrate** to send it to everyone.

The panel has no powers of its own. Each button asks the server, which checks you may use `/dm` and then runs the matching command as you:

| Button | Runs |
|-|-|
| DM mode | `/dm on` / `/dm off` |
| Veil | `/dm veil on` / `/dm veil off` |
| Freeze all / Unfreeze all | `/dm freeze radius` / `/dm unfreeze all` |
| Spawn at crosshair | `/dm encounter spawn <id> [frozen]` |
| Clear all | `/dm encounter clear all` |
| Call roll | `/dm roll @a <check> <dc> [advantage\|disadvantage] [secret]` |
| Narrate | `/dm narrate <text>` |

So the results, chat lines and server log are the same as typing the command, and the command's reply also shows at the bottom of the panel. The check, DC, mode and selected encounter are remembered until you quit the game.

## Where to find it
Server-side commands, the DM Wand (creative tab or `/give`) and the DM panel (K); nothing to craft.

## Commands
Ops (permission level 2) can run every `/dm` command. `/dm grant <player>` lets a non-op run all of them except `grant`, `revoke`, `apply`, `mood` and `loot plant`; `/dm revoke <player>` takes it back (and ends their DM mode).

| Command | What it does |
|-|-|
| `/dm on` / `/dm off` | DM mode for yourself |
| `/dm status` | Lists DMs, veiled DMs and granted players |
| `/dm grant <player>` / `/dm revoke <player>` | (op only) Let a non-op use `/dm` |
| `/dm veil [on\|off]` | Hide from non-DM players; toggles with no argument |
| `/dm encounter list` | Encounter files and live encounters |
| `/dm encounter spawn <id> [<pos>] [frozen]` | Spawn an encounter at the block you look at, or at `pos` |
| `/dm encounter clear <n>\|all` | Remove an encounter's mobs, no drops |
| `/dm freeze <targets>\|radius [r]` | Pause mobs and players |
| `/dm unfreeze <targets>\|radius [r]\|all` | Let them go |
| `/dm check <players> <skill> <dc> [adv\|dis] [silent]` | A skill check on each player's big d20 panel, reported to you |
| `/dm save <targets> <ability> <dc> [adv\|dis] [silent]` | A saving throw on the save lane; mobs roll +1 silently. See [Saving throws](saving-throws.md) |
| `/dm roll <players> <check> <dc> [advantage\|disadvantage] [secret] [auto]` | A roll call: a prompt on each player's d20 HUD, Y to roll, 20 s timeout, results and a summary to you |
| `/dm apply pass\|fail\|all <command>` | (op only) Run a command as and at each player of the last roll call who passed / failed / all |
| `/dm narrate [@selector] <text>` | Narration line in chat, `*bold*`, page-turn sound |
| `/dm title [@selector] <title> [\| <subtitle>]` | Title and subtitle on screen, drum hit |
| `/dm say <entity> <text>` | Speak as an NPC, in its faction's colour, with a speech bubble |
| `/dm mood combat\|tavern\|dungeon\|boss\|off` | (op only) Force a music track within 96 blocks |
| `/dm loot give <players> <loot table>\|magic <rarity>` | Roll a loot table, or a magic item of that rarity, into their inventories |
| `/dm loot plant <loot table> [<pos>] [dc <n>]` | (op only) Plant an unrolled, locked chest; `dc` sets the lock DC |
| `/dm loot plant mimic [<loot table>] [<pos>]` | (op only) Plant a dormant Mimic |

Encounter ids and loot tables tab-complete. Ids from this mod can be written without the namespace (`goblin_patrol`); others need it (`mypack:bandits`).

## Configuration
### Encounter files
Encounters are data: `data/<namespace>/encounters/<id>.json`, so a datapack can add its own, and `/reload` picks up changes. The same format is meant for anything else that drops a group of mobs (dungeon rooms), through `EncounterSpawner.spawn`.

```json
{
  "name": "Goblin Patrol",
  "difficulty": "medium",
  "spread": 3,
  "loot": true,
  "spawns": [
    {"entity": "dndclasses:goblin_warrior", "count": [2, 3]},
    {"entity": "dndclasses:goblin_warlord", "chance": 0.2, "nbt": "{CustomName:'\"Grub\"'}", "tags": ["patrol.leader"]}
  ],
  "chests": [
    {"loot_table": "dndclasses:chests/dwarven_fortress_treasury", "count": 1}
  ]
}
```

| Field | Default | Meaning |
|-|-|-|
| `name` | the file name | Shown in chat |
| `difficulty` | `medium` | `easy`, `medium`, `hard` or `deadly`. Informational for now |
| `spread` | 3 | How far (in blocks, each way) from the centre mobs and chests may be placed |
| `loot` | `true` | `false`: no drops or XP, no class XP and no bounty credit |
| `spawns` | required | One entry per kind of mob |
| `spawns[].entity` | required | Entity id |
| `spawns[].count` | 1 | A number, or `[min, max]` |
| `spawns[].chance` | 1.0 | Chance (0-1) that this entry spawns at all |
| `spawns[].nbt` | none | Extra entity NBT, as an SNBT string (`"{PersistenceRequired:1b}"`) or a JSON object |
| `spawns[].initialize` | `true` | Run the vanilla spawn setup (random gear, variants), as `/summon` does. Set `false` when `nbt` sets the gear |
| `spawns[].tags` | none | Extra command tags |
| `chests` | none | Chests placed near the centre with an un-rolled loot table (locked for [Lockpicking](d20-skill-checks.md)) |

A file that fails to parse is skipped with an error in the server log (`[Encounters] couldn't load ...`).

## Known limitations
- Frozen mobs that run their own movement code instead of AI goals (some flying dragons and bosses) may still act; `NoAI` covers vanilla-style mobs and the goblins, owlbears and cubes.
- `unfreeze all` and `encounter clear` only reach loaded entities. Mobs in unloaded chunks stay frozen (or stay put) until you go back.
- Chests from an encounter stay after `clear`.
- A veiled DM is still listed in the tab list, and mobs still bump into them on the server.
- The last roll call (for `/dm apply`) and open prompts are kept in memory, so a server restart forgets them.
- A player in creative or spectator mode rolls a called save without seeing it (the saving-throw rules hide those); skill checks and flat rolls still show.
- DMs gain and lose no [faction reputation](factions.md), and NPCs treat them as Neutral: no tier prices, perks or refusals (the innkeeper's Honored round, for one) until `/dm off`. `/rep` says so on each non-Neutral line. The quest hook (no quest progress for DMs) arrives with quests.
- Narration, titles and `say` lines are plain text (no colours or links beyond `*bold*`), and aren't translated.
- The DM panel rolls for everyone (`@a`) and narrates to everyone; to pick players, type the command.
- `/dm mood` only reaches players in range when you run it; someone who walks in later doesn't hear it, and a player who relogs loses it.

## For developers
- Package `mattonfire.dnd.dm`:
  - `DungeonMaster`: `PersistentState` (`dndclasses_dm`) with the DM, granted and veiled sets and the live encounters. Static `isDm(entity)`, `isVeiled(entity)`, `isGranted(entity)`, `canUse(source)`. Use `isDm` to leave DMs out of any new player-counting system.
  - `DmCommand`: `/dm`, registered in `DnDClasses`.
  - `DmVeil`: veil abilities, damage cancel and `refreshTracking`. `classes/mixin/DmEntityTrackerMixin` stops `ThreadedAnvilChunkStorage.EntityTracker` tracking a veiled DM for non-DM viewers; `ThreadedAnvilChunkStorageAccessor` + `DmTracked` re-run tracking when the veil changes.
  - `DmRolls`: the `/dm check`, `/dm save`, `/dm roll` and `/dm apply` command trees. `DmRollCalls`: pending calls (one per player), the `dndclasses:roll_prompt` S2C packet (label, caller, DC or 0 when secret, `Advantage`, ticks left, 0 clears it) and the empty `dndclasses:roll_answer` C2S packet, the 400-tick timeout on `END_SERVER_TICK`, the result and summary lines, and `last()` for `/dm apply`. Logs `[DmRolls] call #n ...` lines.
  - `DmNarration`: `/dm narrate`, `title`, `say` and `mood`. Mood tracks go out through `BossMusic.send` under a fixed fake bar UUID (`MOOD_BAR`); the client logs every boss-music packet as `[BossMusic] bar <uuid> track <id>|off`. Lines are logged as `[DM] narrate|title|say|mood ...`. The speech bubble is the `dndclasses:speech_bubble` particle (`SpeechBubbleParticle`, texture from `tools/speech_bubble_texture.py`).
  - `DmLoot`: `/dm loot give|plant`. The lock DC override is `LockDcHolder` (`DndLockDc` in NBT), added to every `LockableContainerBlockEntity` by `classes/mixin/LockableContainerLockDcMixin`; `Lockpicking.dcAt` reads it (both halves of a double chest) and `lockedTableAt` treats a chest with one as locked.
  - `DmFreeze`: freeze/unfreeze, Fabric attack/use/break callbacks, the player hold tick; the power-up packet in `PowerUpRequests` checks `DmFreeze.isFrozen`.
  - `DmWand`: the wand's modes and actions (`use(player, stack)`, `raycast`, `spawnEncounter`, `inspect`), mode and encounter in stack NBT (`DmWandMode`, `DmWandEncounter`), and `UseBlockCallback` / `UseEntityCallback` handlers that claim wand clicks on both sides (the client can't tell who is a DM). `classes/Items/DmWandItem` handles clicks on air and names the item after its mode. Logs `[DmWand] <player> mode <mode>`.
  - `DmPanelPackets`: the DM panel's packets. C2S `dndclasses:dm_panel_open` (empty) and `dndclasses:dm_panel_action` (action name, then string arguments); both check `DungeonMaster.canUse` and log `[DmPanel] refused <action> from <player>` otherwise. `toCommand(action, args)` validates the arguments (known encounter, `Check.parse`, DC 1-40, one-line text of up to 256 characters) and builds the `/dm` command, run with `executeWithPrefix` as the player and logged as `[DmPanel] <player> runs /dm ...`. S2C `dndclasses:dm_panel_state`: open flag, DM mode, veiled, frozen count, encounter files, live encounters and the command's feedback. The output is wrapped in `Capture` to collect the feedback; `classes/mixin/DmPanelFeedbackMixin` unwraps it in `ServerCommandSource.sendToOps`, so the op broadcast and console log match a typed command. Client: `classes/Client/Hud/DmPanelScreen`, key in `DndClassesClient`.
  - `encounter/Encounter` (parsed file), `encounter/Encounters` (reload listener, `get(id)`, `all()`), `encounter/EncounterSpawner` (`spawn(world, encounter, center, tags)` returns the mobs and chests; `NO_LOOT_TAG`).
- `"loot": false` mobs get `dndclasses.no_loot`; `BossMinions.givesNothing` covers them and boss minions, and is checked in `BossMinionDropMixin`, `BountyRewards` and `ProgressionEvents`.
- DM exclusions: `ActiveTargetGoalMixin`, `PartyEvents.shareXp` and `syncHud`, `BossFight.isFightingPlayer`, `GoblinRaids.maybeStartRaids`, `GoblinRaid` participants and target picking, `BountyRewards.onKill`.
- Devscripts: `devscripts/dm-toolkit-host.txt` + `dm-toolkit-guest.txt` (two-client LAN test on port 25617), then `devscripts/dm-freeze-rejoin.txt`. Full verification: `dm-verify-host.txt` with `dm-verify-guest.txt` and `dm-verify-guest-rejoin.txt` (port 25641, guest run with `-PdevUser=DmGuest`), and `dm-verify-encounters.txt` on its own. Wand: `dm-wand.txt` (single player) and `dm-wand-lan-host.txt` + `dm-wand-lan-guest.txt` (port 25644, guest with `-PdevUser=WandGuest`). Roll calls: `dm-roll-calls.txt` (single player: prompt, the Y key, auto, advantage, secret, apply, timeout) and `dm-roll-calls-lan-host.txt` + `dm-roll-calls-lan-guest.txt` (port 25636, guest with `-PdevUser=Guest`). Narration and loot: `dm-narration.txt` (single player) and `dm-narration-lan-host.txt` + `dm-narration-lan-guest.txt` (port 25643, guest with `-PdevUser=Guest`). DM panel: `dm-panel.txt` (single player: every button, then the same commands typed, to compare the log) and `dm-panel-lan-host.txt` + `dm-panel-lan-guest.txt` (port 25652, guest with `-PdevUser=PanelGuest`: refused K and forged packet, then granted, then revoked). DevScript steps for screens like this: `field <n> <text>`, `cycle <label> <value>`, `dmselect <encounter>`, and `dmpanel <action> [args]` to send a raw action packet.
