# Quests
Quests are multi-step stories written as data files. A quest has stages, each stage has objectives (slay, find, speak with, gather, deliver, pass a check, defend), and finishing a stage moves the story on. If you're in a party, everyone nearby shares the quest: anyone's kills count for all, and every participant gets the full reward. This page covers the engine and its commands. NPCs offer quests, take hand-ins and pay rewards through [NPC dialogue](dialogue.md); the [Journal](quest-journal.md) (J) and the top-right tracker show where you are. Four launch chains send you from village rumours to the mod's bosses, and a fifth, Shadows Below, takes you from the Beholder down into the [Underdark](underdark.md) (see [Launch quest chains](#launch-quest-chains)).

![Chat after a party's shared quest: progress, "Quest complete", and both players paid in full](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/quest-engine/quest-lan-host-rewarded.png)

## How it works
- **Quests and stages**: a quest is a list of stages. A stage has 1-4 objectives, all required. When the last one is done, the stage's `on_complete` actions run and the next stage starts. After the last stage the quest is **finished**.
- **Objectives**:

| Type | Counts when |
|-|-|
| `kill` | a participant (or their pet) kills a matching mob, optionally inside a structure (`within_structure`). Boss minions never count. A Lich that flees into its phylactery doesn't count; smashing the phylactery while it reforms counts as killing the Lich. |
| `visit` | a participant stands inside the structure, or anywhere in the `dimension` (checked every second) |
| `talk` | a participant speaks with an NPC with the role |
| `collect` | the participant handing in at the quest's giver has the items (`consume` takes them) |
| `deliver` | a participant gives the items to an NPC with the role (always taken; can be handed in a few at a time) |
| `check` | a participant passes a d20 check offered in [dialogue](dialogue.md) |
| `defend` | a goblin raid on that kind of settlement (`hobbit_village`, `dwarven_fortress`) is won with a participant taking part |

- **Quest drops**: a `kill` objective can have a `quest_drop`, an item that drops from matching kills with a chance. It only drops for quest participants, only the killer can pick it up, and it vanishes after 2 minutes, so it can't be farmed outside the quest. With a `max` it stops dropping once the killer holds that many (counting any already handed in to a `deliver` objective for the item in the same stage).
- **One kill, one count per quest**: a kill counts once for each quest the killer is on, for the first matching objective of its current stage that isn't done.
- **Actions** (on accept, on stage complete, and as rewards): give an item, a loot table, a magic item of a rarity, emeralds, XP, class XP, reputation, an explorer map to the nearest structure (`reveal`, also in another dimension: see below), a narration line in chat, start another quest, set a flag, grant an advancement.
- **Party sharing**:
  - Starting a quest in a party adds every member who is online, in the same dimension and within 48 blocks. Members already on that quest (or at the limit) are skipped with a message. Members further away get a chat line with a clickable **[Join]** (`/quest join`).
  - Progress from any participant counts for everyone, wherever they are.
  - Leaving the party (or being removed) gives you your own copy of each shared quest at its current progress; the party keeps the original.
  - **Rewards aren't split**: when the quest is finished, each participant has its rewards waiting at the giver (any NPC with the giver's role). A quest with no giver pays out straight away. Rewards wait across restarts and logouts.
  - Stage actions for a participant who is offline run when they next join.
- **Dungeon Masters**: a player in DM mode (`/dm on`) is never added to a quest and their kills, visits and raids don't count.
- **Limits**: 8 active quests per player. Quests never expire. A quest is done once unless it's `repeatable` (with a cooldown in days).
- **Feedback**: progress shows on the action bar ("Slay goblins 4/6"); stage completion and the finished quest go to chat with a sound and a toast. The [Journal](quest-journal.md) and the tracker show the rest.

## Launch quest chains
Rewards come on top of whatever the bosses drop, and every participant gets them in full. "Check" steps are dialogue options rolled on the d20 HUD (the DC shifts with your standing, see [dialogue](dialogue.md)); each can be tried once, and both outcomes move the story on.

**A. The Goblin Menace** (the innkeeper; no requirements)
1. **Trouble on the Road** (`dndclasses:goblin_menace/1`): slay 6 goblins (+20 Hobbits of the Shire). Reward at the innkeeper: 5 emeralds, 30 XP, 50 class XP.
2. **The Warlord's Trail** (`dndclasses:goblin_menace/2`, after Trouble on the Road). On accept: a map to the nearest goblin camp (ask the innkeeper "Where are they coming from?" for another).
   1. *Tracks in the Mud*: slay 3 goblins inside a goblin camp. You get the **Goblin War Orders** and a map to the nearest dwarven fortress.
   2. *Word to the King*: give the War Orders to the Dwarf King.
   3. *The King's Answer*: Persuasion DC 13 with the King. Pass: "the Mountain's blessing", +65 Mountain Dwarves for the one who persuaded him (40 for the blessing plus the 40 alliance bonus, with the 15 from step 4), flag `king_ally`, and step 4 is skipped. Fail: on to step 4.
   4. *The King's Price*: give the King 8 gold ingots (+15 Mountain Dwarves; a pass above gets this too).
   5. *The Warlord's Fall*: slay a Goblin Warlord (any: Nether fortress or war camp). Return to the innkeeper.
   - Reward: 30 emeralds, 120 XP, 300 class XP, +100 Hobbits, +60 Mountain Dwarves, -150 Goblin Horde, an uncommon magic item.

**B. The Whispering Library** (`dndclasses:whispering_library/1`; the Thain (`hobbit_elder`) or a Rumour; needs Friendly with the hobbits)
1. *Restless Dead*: slay 20 undead (`#dndclasses:bounty/undead`); each has a 25% chance to drop a **Cursed Bone** for you. Bring 3 Cursed Bones to the Thain (taken).
2. *Reading the Bones*: Arcana DC 12 with the Thain. Pass: a map to the nearest Lich's lair (a stronghold or a [Crypt](../structures/dungeons.md#the-crypt), whichever is closer). Either way: 2 eyes of ender ("follow the eyes").
3. *The Soul Jar*: enter a stronghold or a Crypt (`#dndclasses:quest/lich_lairs`) and smash the Lich's phylactery. Either route works: the stronghold library's Lich or the Crypt Lich in its reliquary.
4. *Lichbane*: destroy the Lich (only its final death counts). Return to the Thain.
   - Smashing the phylactery while the Lich is reforming is its final death, so it counts for both stage 3 and stage 4 at once. The order doesn't matter: smash the jar then kill the Lich, or kill the Lich (it flees to the jar) then smash the jar.
   - Reward: 40 emeralds, 150 XP, 400 class XP, +150 Hobbits, a rare magic item.

**C. Ice and Storm** (`dndclasses:ice_and_storm/1`; the Dwarf King; needs Friendly with the dwarves). On accept: a map to the nearest frost lair.
1. *The Lost Expedition*: find the frost lair.
2. *Cold Fire*: slay a Frost Drake and show the King a trophy (`#dndclasses:quest/frost_drake_trophies`: blue ice or the Staff of Ice; not taken). Then a map to the nearest dragon lair.
3. *Thunder on the Peaks*: slay a Lightning Chaser. Return to the King.
   - Reward: 40 emeralds, 150 XP, 400 class XP, +150 Mountain Dwarves, a rare magic item from the King's armoury (`gameplay/quest_reward_dwarven_rare`: diamond axe, pickaxe, helmet, chestplate or a crossbow).

**D. The Eye Below** (`dndclasses:eye_below/1`; the Dwarf King; needs Honored with the dwarves and The Warlord's Trail or Ice and Storm finished)
1. *Silent Picks*: Insight DC 14 with the King. Pass: he admits his own miners broke into the cavern (flag `truth`, +25 Mountain Dwarves). Either way: a map to the nearest beholder lair (searching 150 chunks).
2. *An Eye for an Eye*: slay the Beholder. Return to the King.
   - Reward: 64 emeralds, 250 XP, 600 class XP, +200 Mountain Dwarves, a very rare magic item.

**E. Shadows Below** (the Dwarf King, then the duergar factor, then the Elf Speaker; after The Eye Below). The chain that takes you through the [Underdark](underdark.md).
1. **What the Eye Guarded** (`dndclasses:shadows_below/1`, the Dwarf King; needs The Eye Below finished; also a Rumour). "The eye sat on that hole for a hundred years. Now something's coming up it." On accept: a map to the nearest [Underdark Rift](../structures/underdark-rifts.md).
   1. *The Rift*: go down into the Underdark (any way: a rift, the Beholder's torn lair floor, `/underdark go`). "The dark swallows the sound of your steps." Then a map to the nearest [Duergar Hold](../structures/duergar-holds.md) (searching 150 chunks).
   2. *Grey Kin*: find a Duergar Hold.
   3. *A Word with the Factor*: three dialogue topics on the [duergar factor](../mobs/duergar.md), one try each; any pass moves on:
      - Persuasion DC 15 (Dwarves roll with disadvantage): +300 Grey Clans.
      - Intimidation DC 14: -50 Grey Clans, +25 Mountain Dwarves.
      - Deception DC 16: no standing change.
      - Fail all three and a fourth topic, "What will it take?", moves on for -25 Grey Clans.
      - Then a map to the nearest [Drow Outpost](../structures/drow-outposts.md).
   - Reward at the King: 32 emeralds, 200 XP, 400 class XP, +100 Mountain Dwarves.
2. **Silk and Poison** (`dndclasses:shadows_below/2`, the duergar factor). On accept: a map to the nearest Drow Outpost.
   1. *Raid the Raiders*: slay 8 drow (`#dndclasses:faction/drow`) inside a Drow Outpost, and bring 3 **Drow House Sigils** to the factor. Drow you kill at an outpost while on this step drop a sigil 50% of the time until you have 3 (counting any you've handed in). Sigils can be handed in a few at a time.
   - Reward at the factor: 24 emeralds, 500 class XP, +150 Grey Clans, +100 Sylvan Court, a rare magic item.
3. **The Matron's Web** (`dndclasses:shadows_below/3`, the [Elf Speaker](../structures/elven-enclaves.md)). The Dwarf King points you there ("The elves will want a word about the drow. Can't stand them myself.").
   1. *The Speaker's Plea*: Insight DC 14 with the Speaker. Pass: the Speaker admits the Matron's house was elven once (flag `truth`, +25 Sylvan Court). Either way: the [Drow Spire](../structures/drow-spire.md) is revealed (searching 250 chunks of the Underdark below you). The Speaker is on the surface, so you get a **Rift Map** (below).
   2. *The Spire*: find the Drow Spire.
   3. *The Matron*: slay the [Matron Mother](../bosses/matron-mother.md). "Among her letters, tribute paid to something that does not sleep. 'The Brain hungers,' the last one says." You get the **Shadows Below** advancement. Return to the Speaker.
   - Reward: 64 emeralds, 300 XP, 800 class XP, +200 Sylvan Court, +100 Mountain Dwarves, a very rare magic item. Elves get +100 Sylvan Court more.
   - The hook for a later Mind Flayer chain: finishing this quest (`requires.quests` `dndclasses:shadows_below/3`) or the `dndclasses:underdark/matron_slain` advancement.

**Maps to the Underdark**: a `reveal` with `"dimension"` searches that dimension below your x/z. Standing in it, you get the usual map. Anywhere else the chat says where it lies ("Duergar Hold lies below you, in the Underdark, under x 1200, z -340."), and for the Underdark you get a **Rift Map**: a map to the nearest [Underdark Rift](../structures/underdark-rifts.md) (rifts land at the same x/z) with the target's name and x/z in its lore and NBT (`dndclassesRiftTarget`: `Name`, `X`, `Z`). No rift within 100 chunks: just the chat line.

Every map step has a dialogue topic with the giver that hands out another map while you're on that step. If there's no such structure in range, the NPC says so and you're left to find it yourself.

**Rumours**: a bounty board's third notice is a Rumour 20% of days. It starts a quest with a `rumour_weight` you could take now (The Whispering Library 2, Trouble on the Road 1, Ice and Storm 1, What the Eye Guarded 1 once The Eye Below is done). See [Hobbit Tavern](../structures/hobbit-tavern.md#bounty-board).

### Quest items
| Item | From | Use |
|-|-|-|
| Goblin War Orders | chain A, step 2.1 | given to the Dwarf King |
| Cursed Bone | chain B, undead you kill while on the quest (25%) | 3 handed to the Thain |
| Drow House Sigil | chain E, step 2.1: drow killed at a Drow Outpost (50%, until you have 3) | 3 handed to the duergar factor |
| Rumour (`quest_lead`) | a bounty board's Rumour notice | right-click to start its quest (NBT `Quest`) |

## Where to find it
- Chains A and B start in Hobbit Villages (innkeeper, Thain), C, D and E in Dwarven Fortresses (the Dwarf King). Chain E goes on in a Duergar Hold (the factor in its Trade Hall) and an Elven Enclave (the Speaker).
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

- `requires.any_quests`: at least one of these must be finished (The Eye Below uses it).
- `chain` defaults to the id's folder (`dndclasses:goblin_menace`). `giver` is an NPC role; leave it out to pay rewards at once. `requires` decides what dialogue offers; `rumour_weight` is the quest's weight on bounty boards' Rumour notices (0: never).
- Objective fields: `kill` (`entity` id or `#tag`, `count`, `within_structure`, `quest_drop`: `item`, `chance`, optional `max`), `visit` (`structure` id or `#tag`, or `dimension`, e.g. `dndclasses:underdark`), `talk` (`role`), `collect` (`item` id or `#tag`, `count`, `consume`, default true), `deliver` (`item`, `count`, `role`), `check` (`skill`, `dc`), `defend` (`settlement`). Every objective takes an optional `text`.
- Actions: `give` (`item`, `count`, `nbt` as SNBT), `loot` (`table`), `magic_item` (`rarity`: `common`, `uncommon`, `rare`, `very_rare`, `legendary`), `emeralds` (`count`), `xp` / `class_xp` (`amount`), `rep` (`factions`: id to delta; optional `race`, e.g. `elf`: only players of that race get it), `reveal` (`structure`, `radius` in chunks, default 100, `fallback` text, optional `dimension`: search there below the player, a Rift Map from outside the Underdark), `narrate` (`text`), `start_quest` (`quest`), `set_flag` (`flag`), `advancement` (`advancement`), `complete_stage` (`stage`, 1-based, optional: finish the current stage as if its objectives were done, running its `on_complete`; with `stage`, only if the quest is on that stage. Dialogue uses it so a failed check still moves the story on).
- `magic_item` rolls from the loot table `dndclasses:gameplay/quest_reward_<rarity>`: a weapon or armour piece made a magic item of that rarity (identified) with the `dndclasses:magic_item` loot function. Edit those tables to change the pool.

## Known limitations
- The Whispering Library pays out at the Thain, who only lives in villages generated after the dialogue update. In an older village, make a hobbit the elder: `/tag @e[type=dndclasses:hobbit,limit=1,sort=nearest] add dndclasses.role.hobbit_elder`.
- Dialogue check outcomes (the King's blessing, the Insight bonus) go to the player who rolled; stage rewards go to every participant.
- A quest's `check` objective counts any pass of that skill in dialogue, not just the quest's own option (no launch quests overlap).
- A Dwarf starts Hostile with the Grey Clans, and the factor won't talk to a Hostile player, so a Dwarf needs to raise their standing to Unfriendly (or take the Grey Clans' reward from another route) before Shadows Below step 1.3.
- Rewards can't depend on flags, so the King's alliance bonus in chain A is paid when he's persuaded rather than at the end.
- The "King's armoury" (Honored dwarf barter) from the design is the Honored tier effect in [Factions](factions.md), not something Ice and Storm unlocks on its own.
- Dev clients get a random player name each launch, so per-player quest state seems to vanish between runs; pass `-PdevUser=<name>` to keep one.

## For developers
- Package `mattonfire.dnd.quest`:
  - `Quests`: server-data reload listener for `quests/**.json`.
  - `QuestDefinition`, `QuestStage`, `QuestObjective` (sealed: `Kill`, `Visit` (structure or dimension), `Talk`, `Collect`, `Deliver`, `Check`, `Defend`) and `QuestAction` (sealed, one record per action) parse the files. `QuestJson` has the shared parsing helpers and id-or-tag matchers.
  - `QuestManager` (`PersistentState` `dndclasses_quests`): instances, per-player finished quests, tracked quest and pending actions (rewards at the giver, missed stage actions). Its API for the next tickets: `whyCantStart`, `start`, `join`, `talkedTo(player, role)` (dialogue calls this; returns a `TalkResult`), `hasBusinessWith`, `checkPassed(player, skill)`, `claim`.
  - `QuestEvents`: kill hook (`AFTER_KILLED_OTHER_ENTITY`, pets credit their owner), visit tick, `onRaidWon` (called from `GoblinRaid.win`), `onPartyLeft` (called from `/party leave` and `/party kick`).
  - `QuestSync` (S2C `dndclasses:quest_sync`, debounced to once a second) and `client/ClientQuests` (the data the [Journal](quest-journal.md) and tracker read).
  - `QuestHooks`: `magicItem` (set by the magic items ticket) and `ignored` (`DungeonMaster::isDm`: DMs in DM mode never join quests or make progress).
- `LichEntity.soulFled()` tells a reforming death from the real one.
- Devscripts: `quest-engine.txt` + `quest-engine-restart.txt` (sample quest, rewards, admin commands, saved progress), `quest-engine-objectives.txt` (every objective and action, using the test data pack in `devscripts/datapacks/quest-engine-test`, which also has two broken files) and `quest-engine-lan-host.txt` / `quest-engine-lan-guest.txt` (party sharing, join, fork). Run them with `-PdevUser=QuestDev`.
- Launch chains: one devscript each, using `/quest admin ... stage` shortcuts, frozen bosses and `/damage ... by @s` for kill steps: `quest-chain-goblin-menace.txt`, `quest-chain-whispering-library.txt`, `quest-chain-ice-and-storm.txt`, `quest-chain-eye-below.txt`, `shadows-below.txt` (real Holds, Outposts and Spire from `/dndhome`, `/underdark go` for the descent) and `shadows-below-factor.txt` (the factor's dialogue topics); Rumours: `quest-rumour.txt`.
- Quest items: `QuestItem` (named item with a lore line) and `QuestItems` (`goblin_war_orders`, `cursed_bone`, `drow_house_sigil`, `quest_lead`; textures from `tools/quest_item_textures.py` for the sigil); the Rumour is `tavern/QuestLeadItem`.
