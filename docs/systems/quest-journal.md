# Journal and quest tracker
Press **J** to open the Journal. The **Quests** tab lists your quests by chain: the current stage, what's left to do, who's on it with you and what it pays. You can track a quest or abandon it from there. The **Factions** tab shows where you stand with each faction on a bar from Hostile to Exalted. While you play, the quest you're tracking sits in the top-right corner with its objectives, and toasts pop up when a quest starts, a stage is done or a quest is finished.

## How it works
- **Opening**: J (rebindable: Options > Controls > "Journal") opens it when no other screen is open. J or Esc closes it, and Tab switches between the tabs. The game doesn't pause. The Journal remembers the last tab and quest you looked at.
- **Quests tab**:
  - Left: one group per chain (e.g. "The Goblin Menace"). Chains with an active quest come first. Each group lists the active quests, then the finished ones in grey. `»` marks the tracked quest, `✔` a finished quest and `!` (yellow) a finished quest whose reward is still waiting with its giver. Scroll with the mouse wheel.
  - Right: the selected quest's title, giver, description, "Stage n/m" with the stage text, each objective with its progress (`- Slay goblins 2/6`, green with `✔` when done), the party (`★` marks a party leader) and the rewards ("5 emeralds, 30 XP, 50 class XP"). Scroll long text with the wheel.
  - **Track** makes it the tracked quest (`/quest track`). It reads "Tracked" and is greyed when the quest is already tracked.
  - **Abandon** asks first. In a party, only you leave: the rest keep the quest and its progress (`/quest abandon`).
  - The header shows how many quests are active out of 8. With no quests, the tab says where to find some.
  - It updates while open when the server sends new quest data.
- **Factions tab**: one row per faction: a colour swatch and the faction's name, its tier in the tier's colour and your value (e.g. "Friendly +250"), and a bar from -1000 to 1000. The bar fills from 0 to your value in the tier's colour. Ticks mark where Unfriendly (-499), Neutral (-99), Friendly (100), Honored (400) and Exalted (750) begin, and a white mark shows your value. Scroll if there are more factions than fit.
- **Tracker** (top-right): the tracked quest's title (gold) and up to 3 objectives of its current stage with progress. Done objectives turn green. If no quest is marked tracked, it shows the newest. Starting a quest tracks it automatically.
  - It moves down below the vanilla status effect icons when they show, and stays clear of the boss bar in the middle. On a very narrow screen (less than 60 pixels free beside the boss bar) it hides.
  - F1 and F3 hide it, like the party HUD.
- **Toasts** (top-right, like advancements, 5 seconds): "Quest started" with the quest's title, "Stage complete" with the finished stage's name, "Quest complete" with the quest's title. They're silent, because the server already plays the chat sound. The quests you have when you join don't toast, and neither does your own copy of a quest when you leave a party.
- Progress also shows on the action bar ("Slay goblins 2/6") and stage completion goes to chat, as before (see [Quests](quests.md)).

## Where to find it
- Key J. Nothing to craft.
- The sample quest Trouble on the Road (`/quest admin @s start dndclasses:goblin_menace/1`, or from the innkeeper) fills the Quests tab. `/rep @s <faction> set <n>` moves the bars.

## Commands
- None of its own. Track and Abandon send `/quest track <quest>` and `/quest abandon <quest>`, so they behave exactly like the commands. See [Quests](quests.md) and [Factions](factions.md).

## Configuration
- The key is a normal key binding ("Journal" in the DnD Classes category).
- Chain names come from the lang key `quest.<namespace>.<chain path>.title` (e.g. `quest.dndclasses.goblin_menace.title`), or the chain's id in words if there is none.

## Known limitations
- Toasts share the top-right corner with the tracker and draw over it for the few seconds they show (the same as advancements and the effect icons).
- A finished quest shows only its title and whether its reward is waiting, not its stages.
- The tracker shows at most 3 objectives. A stage with 4 shows all of them in the Journal.

## For developers
- `classes/Client/Hud/QuestJournalScreen`: the screen and the J key binding (`key.dnd-classes.journal`, registered in its `register()` from `DndClassesClient`). Rebuilds itself in `tick()` when `ClientQuests` or `ClientReputation` hands out a new list. Logs `[Journal] /quest ...` for the commands it sends.
- `classes/Client/Hud/QuestTrackerHud`: the `HudRenderCallback` tracker; `ClientQuests.tracked()` picks the quest.
- `quest/client/QuestToasts`: compares each `quest_sync` with the previous one (new instance = started, higher stage = stage complete, instance gone and quest in the finished list = finished). Logs `[Quests] toast: <kind> <title>`.
- `quest_sync` now also carries each active quest's description and a rewards summary (`QuestCommand.describe`), after the participants.
- DevScript: `press key.dnd-classes.journal` opens it, `widget Factions` / `widget Quests` switch tabs, `widget <quest title>` selects a quest, `widget Track` / `widget Abandon` press the buttons. `devscripts/quest-journal.txt` runs through all of it with screenshots `journal-*.png`.
