# NPC dialogue and quest givers
Some NPCs have stories to tell. Right-click the innkeeper, the Dwarf King or a village's Thain (the hobbit elder) and a dialogue panel opens at the bottom of the screen. They greet you according to your standing with their people, offer quests, ask after the ones you're on, take items you were sent to deliver, and pay your rewards. Some replies are d20 checks: talk the innkeeper into paying something up front, see through a liar. When an NPC has nothing to say, right-clicking does what it always did: the innkeeper trades, hobbits share food, dwarves barter for gold.

## How it works
- **Roles.** An NPC with the command tag `dndclasses.role.<role>` is a quest giver for that role. Built in:
  - `innkeeper`: every innkeeper (older innkeepers get the tag when their chunk loads).
  - `dwarf_king`: the crowned dwarf of each Dwarven Fortress.
  - `hobbit_elder`: the Thain ("Thain Brandybuck"), a new hobbit placed by the party tree on each newly generated village green.
  - Any mob can be given a role with `/tag <entity> add dndclasses.role.<role>`, so a DM can make anyone a quest giver.
- **When it opens.** Right-click with an empty hand (the innkeeper also talks while you hold anything other than a bounty notice, a name tag or a spawn egg). The dialogue opens only if the NPC has something to say to you:
  - a quest it gives that you could start now (requirements met, not on it, not finished or off cooldown),
  - a dialogue topic whose requirements you meet,
  - a `talk` objective for its role, items for a `deliver` objective to its role or a `collect` objective of a quest it gives, or
  - a finished quest's reward waiting with it.
  Otherwise the NPC's own interaction runs. **Sneak** + right-click always skips the dialogue.
- **Hostile.** If you're Hostile with the NPC's faction and it has something to say, it won't: "<name> won't speak to you." on the action bar.
- **Opening** counts `talk` objectives, hands in `deliver`/`collect` items and pays rewards for quests this role gives (chat lines as before, plus a line at the top of the page).
- **The first page** shows the greeting for your tier, then: one `[Quest] <title>` option per quest on offer (opens the quest's description with "I'll do it." / "Not now."), the role's topics, "Let's trade." for merchants (innkeepers), and "Goodbye.".
- **Checks.** A check option shows its skill and DC, e.g. `[Persuasion DC 14]`. The DC is the file's DC shifted by your tier with the NPC's faction: Hostile +5, Unfriendly +3, Neutral 0, Friendly -2, Honored -4, Exalted -6 (minimum 1). The roll uses your character sheet and plays on the d20 HUD (drawn over the dialogue panel). A pass also counts any `check` objective for that skill. Each check can be tried once: per quest instance when it belongs to a quest, else once per player. After the try the option disappears.
- Pick a reply by clicking it or pressing its number (1-9). Esc closes. Walking more than 8 blocks away or the NPC dying ends the talk. While you talk, the NPC stops and looks at you. The game doesn't pause.
- The server rebuilds the page before acting on a choice, so an option whose requirements no longer hold (or a forged index) does nothing: "(That's no longer on the table.)".
- Players in DM mode don't get dialogue (they're not quest participants).

## Where to find it
- Innkeepers: the Green Dragon inn in Hobbit Villages.
- The Thain: on the green of Hobbit Villages generated after this update.
- The Dwarf King: the throne hall of Dwarven Fortresses. (Dwarf players with nothing quest-related to discuss still get the King's audience, see [Racial homes](../races/racial-homes.md).)

## Commands
- `/tag <entity> add dndclasses.role.<role>` / `remove`: make an NPC a quest giver, or stop it.
- `/quest admin <players> talk <role>` and `pass <skill>` still simulate a talk or a passed check without an NPC (see [Quests](quests.md)).

## Configuration
Dialogue is data: `data/<ns>/dialogue/**.json`, one or more files per role. Files naming the same role are merged in id order (greetings: first file wins per tier; topics are appended; nodes are added, a duplicate node id keeps the first and logs a warning), so a quest pack can add topics to the innkeeper without copying its file. A file with a bad field is logged (`[Dialogue] Skipping dialogue <id> ...`) and skipped.

```json
{
  "role": "innkeeper",
  "speaker": "optional; defaults to the NPC's name",
  "greeting": {"default": "...", "unfriendly": "...", "friendly": "...", "honored": "...", "exalted": "..."},
  "topics": [
    {"id": "innkeeper/advance", "text": "Times are hard. Something up front?",
     "requires": {"quest": "dndclasses:goblin_menace/1"},
     "check": {"skill": "persuasion", "dc": 14, "success": "advance_yes", "failure": "advance_no",
               "on_success": [{"type": "emeralds", "count": 3}]}}
  ],
  "nodes": {
    "advance_yes": {"lines": ["\"Oh, very well.\""], "options": [{"text": "Back to business.", "goto": "hub"}]}
  }
}
```
- Text is a translation key (falling back to itself, so literal text works) or a JSON text component, as in quest files. `greeting` can also be a single text.
- Node: `speaker` (optional), `lines` (1-3 texts), `options`. Every node page gets "Goodbye." added at the end. `hub` is the first page; add to it with `topics`.
- Option fields:
  - `text`; `id` (optional, stable key for the one-try rule; defaults to file/node/index).
  - `requires`: `quest` with `state` (`active` default, `finished`, `not_started`, `available`), `stage` (1-based, active only), `flags` / `not_flags` (quest instance flags), `rep` (faction id to minimum tier), `class` (e.g. `rogue`).
  - `goto`: next node (default: back to `hub`); `end: true` closes after the option.
  - `accept`: a quest id to start (the page reports why if it can't).
  - `actions`: quest actions (`give`, `rep`, `reveal`, `set_flag`, ...) run for the player in the context of `requires.quest`, which is required when there are actions.
  - `check`: `skill` (any skill id: `persuasion`, `insight`, `arcana`, `deception`, ...), `dc`, `success` / `failure` nodes, `on_success` / `on_failure` actions.
- Launch files: `innkeeper.json` (tier greetings, "Any word on the goblins?" and a Persuasion DC 14 "advance" of 3 emeralds while Trouble on the Road is active), `dwarf_king.json` and `hobbit_elder.json` (tier greetings). The quest chains themselves are a later ticket.
- A quest is offered by NPCs whose role matches its `giver`.

## Known limitations
- Only villages generated after this update get a Thain. Older villages have no elder (their innkeeper still gives quests).
- An NPC with several roles talks for the first role (alphabetically) that has something to say.
- Hostile NPCs with nothing quest-related to say keep their old behaviour (e.g. the innkeeper's own refusal).
- Check DCs are shifted by the NPC's faction; NPCs without a faction don't shift.
- The innkeeper's dialogue opens for every new player until they take Trouble on the Road; trade from the dialogue ("Let's trade.") or sneak + right-click.

## For developers
- `quest/dialogue/`: `Dialogue` (records and parsing), `Dialogues` (reload listener on `data/<ns>/dialogue/`, after quests), `DialogueManager` (roles, sessions, pages, choices, checks), `DialogueEvents` (`UseEntityCallback`, C2S receiver, tick, disconnect).
- Packets: S2C `dndclasses:dialogue_open` (text speaker, varint n + texts lines, varint n + texts option labels), S2C `dndclasses:dialogue_close`, C2S `dndclasses:dialogue_choose` (varint option index, -1 = closed). The server keeps the shown page per player and re-validates.
- `QuestManager.talkedTo` returns a `TalkResult` (hand-ins, rewarded titles); `hasBusinessWith(player, role)` asks without changing anything; `setFlag(instance, flag)`.
- Client: `classes/Client/Hud/DialogueScreen` (redraws `DiceRollHud` on top of the panel).
- Logs: `[Dialogue] <player> talks to ...`, `sees <role>/<node>: [options]`, `chose <key>`, `is Hostile: ... won't speak`; the client logs each page.
- DevScript: `button <n>` picks reply n (0-based) in a dialogue. `devscripts/npc-dialogue.txt` with the test pack `devscripts/datapacks/dialogue-test` (copy it to `run/saves/New World/datapacks/`).
