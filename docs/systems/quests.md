# Quests
Quests are multi-step stories written as data files. A quest has stages, each stage has objectives (slay, find, speak with, gather, deliver, pass a check, defend), and finishing a stage moves the story on. If you're in a party, everyone nearby shares the quest: anyone's kills count for all, and every participant gets the full reward. This page covers the engine and its commands. NPC dialogue, the Journal screen and the launch quest chains come in later tickets; until then quests are started and handed in with `/quest admin`.

![Chat after a party's shared quest: progress, "Quest complete", and both players paid in full](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/quest-engine/quest-lan-host-rewarded.png)

## How it works
- **Quests and stages**: a quest is a list of stages. A stage has 1-4 objectives, all required. When the last one is done, the stage's `on_complete` actions run and the next stage starts. After the last stage the quest is **finished**.
- **Objectives**:

| Type | Counts when |
|-|-|
| `kill` | a participant (or their pet) kills a matching mob, optionally inside a structure (`within_structure`). Boss minions never count. A Lich that flees into its phylactery doesn't count; smashing the phylactery while it reforms counts as killing the Lich. |
| `visit` | a participant stands inside the structure (checked every second) |
| `talk` | a participant speaks with an NPC with the role |
| `collect` | the participant handing in at the quest's giver has the items (`consume` takes them) |
| `deliver` | a participant gives the items to an NPC with the role (always taken; can be handed in a few at a time) |
| `check` | a participant passes a d20 check in dialogue (stub: nothing offers checks yet) |
| `defend` | a goblin raid on that kind of settlement (`hobbit_village`, `dwarven_fortress`) is won with a participant taking part |

- **Quest drops**: a `kill` objective can have a `quest_drop`, an item that drops from matching kills with a chance. It only drops for quest participants, only the killer can pick it up, and it vanishes after 2 minutes, so it can't be farmed outside the quest.
- **One kill, one count per quest**: a kill counts once for each quest the killer is on, for the first matching objective of its current stage that isn't done.
- **Actions** (on accept, on stage complete, and as rewards): give an item, a loot table, a magic item of a rarity, emeralds, XP, class XP, reputation, an explorer map to the nearest structure (`reveal`), a narration line in chat, start another quest, set a flag, grant an advancement.
- **Party sharing**:
  - Starting a quest in a party adds every member who is online, in the same dimension and within 48 blocks. Members already on that quest (or at the limit) are skipped with a message. Members further away get a chat line with a clickable **[Join]** (`/quest join`).
  - Progress from any participant counts for everyone, wherever they are.
  - Leaving the party (or being removed) gives you your own copy of each shared quest at its current progress; the party keeps the original.
  - **Rewards aren't split**: when the quest is finished, each participant has its rewards waiting at the giver (any NPC with the giver's role). A quest with no giver pays out straight away. Rewards wait across restarts and logouts.
  - Stage actions for a participant who is offline run when they next join.
- **Limits**: 8 active quests per player. Quests never expire. A quest is done once unless it's `repeatable` (with a cooldown in days).
- **Feedback**: progress shows on the action bar ("Slay goblins 4/6"); stage completion and the finished quest go to chat with a sound.

## Where to find it
- The sample quest **Trouble on the Road** (`dndclasses:goblin_menace/1`, the first step of The Goblin Menace): slay 6 goblins (+20 Hobbits of the Shire when done); reward 5 emeralds, 30 XP and 50 class XP from the innkeeper.
- Quest state is saved with the world (`data/dndclasses_quests.dat` in the overworld folder).

## Commands
| Command | Who | What |
|-|-|-|
| `/quest list` | anyone | your active quests (with progress, party and the tracked one) and finished ones (marking rewards still waiting) |
| `/quest info <quest>` | anyone | the quest's stages, objectives, party and rewards. Ops can look up any loaded quest. |
| `/quest track <quest>` | anyone | the quest the tracker HUD shows (the newest one by default) |
| `/quest abandon <quest>` | anyone | leave the quest; the rest of the party keeps it |
| `/quest join [instance]` | anyone | join a quest a party member started (the newest one if no number) |
| `/quest admin <players> start <quest>` | op | start it (skips requirements; the player's nearby party joins as usual) |
| `/quest admin <players> stage <quest> <n>` | op | jump to stage n with no progress (skipped stages' actions don't run) |
| `/quest admin <players> complete <quest>` | op | finish it now for its whole party; rewards wait at the giver |
| `/quest admin <players> reset <quest>` | op | forget it for the player: active, finished and waiting rewards |
| `/quest admin <players> talk <role>` | op | as if they spoke with an NPC with that role: counts `talk`, hands in `deliver`/`collect`, pays rewards |
| `/quest admin <players> pass <skill>` | op | as if they passed a dialogue check for that skill |

## Configuration
Quests are files in a data pack at `data/<namespace>/quests/<path>.json`; the id is `<namespace>:<path>`. A file with a bad field is logged (`[Quests] Skipping quest ...` with the file and field) and skipped; the rest still load. Text can be a translation key (which falls back to itself, so literal text works too) or a JSON text component.

```json
{
  "title": "quest.dndclasses.goblin_menace.1.title",
  "description": "Optional longer text",
  "chain": "dndclasses:goblin_menace",
  "giver": "innkeeper",
  "requires": {"quests": ["dndclasses:goblin_menace/1"], "rep": {"dndclasses:hobbits": "friendly"}},
  "repeatable": {"cooldown_days": 3},
  "rumour_weight": 0,
  "on_accept": [{"type": "give", "item": "minecraft:compass"}],
  "stages": [
    {
      "title": "Optional stage name",
      "text": "What the Journal says about this step",
      "objectives": [
        {"type": "kill", "entity": "#dndclasses:goblins", "count": 6, "text": "Slay goblins",
         "within_structure": "dndclasses:goblin_camp", "quest_drop": {"item": "minecraft:bone", "chance": 0.25}}
      ],
      "on_complete": [{"type": "rep", "factions": {"dndclasses:hobbits": 20}}]
    }
  ],
  "rewards": [{"type": "emeralds", "count": 5}, {"type": "xp", "amount": 30}, {"type": "class_xp", "amount": 50}]
}
```

- `chain` defaults to the id's folder (`dndclasses:goblin_menace`). `giver` is an NPC role; leave it out to pay rewards at once. `requires` and `rumour_weight` are for dialogue and Rumour notices (later tickets).
- Objective fields: `kill` (`entity` id or `#tag`, `count`, `within_structure`, `quest_drop`), `visit` (`structure` id or `#tag`), `talk` (`role`), `collect` (`item` id or `#tag`, `count`, `consume`, default true), `deliver` (`item`, `count`, `role`), `check` (`skill`, `dc`), `defend` (`settlement`). Every objective takes an optional `text`.
- Actions: `give` (`item`, `count`, `nbt` as SNBT), `loot` (`table`), `magic_item` (`rarity`: `common`, `uncommon`, `rare`, `very_rare`, `legendary`), `emeralds` (`count`), `xp` / `class_xp` (`amount`), `rep` (`factions`: id to delta), `reveal` (`structure`, `radius` in chunks, default 100, `fallback` text), `narrate` (`text`), `start_quest` (`quest`), `set_flag` (`flag`), `advancement` (`advancement`).
- `magic_item` rolls from the loot table `dndclasses:gameplay/quest_reward_<rarity>` until magic items are in.

## Known limitations
- No NPC dialogue yet: nobody offers quests or pays rewards in-game, so quests are started and handed in with `/quest admin` (`start`, `talk`).
- No Journal or tracker HUD yet; the client receives the quest data (`dndclasses:quest_sync`) but only logs it.
- `check` objectives have no d20 roll yet: dialogue will offer them.
- Dev clients get a random player name each launch, so per-player quest state seems to vanish between runs; pass `-PdevUser=<name>` to keep one.
- DMs still get quest progress until the DM toolkit hooks in (`QuestHooks.ignored`).

## For developers
- Package `mattonfire.dnd.quest`:
  - `Quests`: server-data reload listener for `quests/**.json`.
  - `QuestDefinition`, `QuestStage`, `QuestObjective` (sealed: `Kill`, `Visit`, `Talk`, `Collect`, `Deliver`, `Check`, `Defend`) and `QuestAction` (sealed, one record per action) parse the files. `QuestJson` has the shared parsing helpers and id-or-tag matchers.
  - `QuestManager` (`PersistentState` `dndclasses_quests`): instances, per-player finished quests, tracked quest and pending actions (rewards at the giver, missed stage actions). Its API for the next tickets: `whyCantStart`, `start`, `join`, `talkedTo(player, role)` (dialogue calls this), `checkPassed(player, skill)`, `claim`.
  - `QuestEvents`: kill hook (`AFTER_KILLED_OTHER_ENTITY`, pets credit their owner), visit tick, `onRaidWon` (called from `GoblinRaid.win`), `onPartyLeft` (called from `/party leave` and `/party kick`).
  - `QuestSync` (S2C `dndclasses:quest_sync`, debounced to once a second) and `client/ClientQuests` (the data the Journal will read).
  - `QuestHooks`: `magicItem` (set by the magic items ticket) and `ignored` (set to `DungeonMaster::isDm` by the DM toolkit).
- `LichEntity.soulFled()` tells a reforming death from the real one.
- Devscripts: `quest-engine.txt` + `quest-engine-restart.txt` (sample quest, rewards, admin commands, saved progress), `quest-engine-objectives.txt` (every objective and action, using the test data pack in `devscripts/datapacks/quest-engine-test`, which also has two broken files) and `quest-engine-lan-host.txt` / `quest-engine-lan-guest.txt` (party sharing, join, fork). Run them with `-PdevUser=QuestDev`.
