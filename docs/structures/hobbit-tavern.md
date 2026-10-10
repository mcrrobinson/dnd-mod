# Hobbit Tavern
Every hobbit village has the Green Dragon inn. Inside, an **innkeeper** sells food and ale and buys produce. On the inn's east wall hang two **bounty boards**, where hobbits pin up jobs: hunts and expeditions that pay emeralds, loot, XP and class XP.

![The innkeeper in front of the Green Dragon's bar, with the bounty boards on the wall to the right](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/hobbit-tavern-innkeeper.png)

## How it works
### Innkeeper
- A hobbit (14 HP) holding a Mug of Ale, named "<name>, Innkeeper". It stays within 4 blocks of the bar by day and within 3 at night.
- Right-click it to open the trade screen. Each trade can be used 12 times, and stock refills each in-game morning.
  - **Sells (for emeralds):**
    - 1 emerald each: 2 ale, 4 bread, 3 cooked chicken, 2 pumpkin pie, 8 cookies or 2 honey bottles
    - 2 emeralds: 1 rabbit stew
    - 3 emeralds: 1 cake
  - **Buys (for 1 emerald):** 20 wheat, 24 potatoes, 22 carrots, 10 brown mushrooms or 6 pumpkins.
- Right-clicking it with a finished bounty notice pays the bounty out.
- **Reputation** ([Factions](../systems/factions.md)): each customer sees prices for their own standing with the hobbits: +50% at Unfriendly (rounded up, at least +1), -10% at Friendly, -25% at Honored, -40% at Exalted, never below 1. A cake costs 5 emeralds at Unfriendly and 2 at Exalted; the innkeeper wants 30 wheat at Unfriendly and 12 at Exalted. A player Hostile with the hobbits gets "<name> won't serve you.": no trades and no bounty payouts.

### Mug of Ale
A drink: 3 hunger, 0.4 saturation, 6 s of Regeneration I, and a 25% chance of 8 s of Nausea. Stacks to 16.

### Bounty board
- Each board has three notices.
- **Take a notice:** right-click it. Which one you get (left, middle or right) depends on where you click, and the paper disappears from the board.
- **Read the board:** sneak-right-click with an empty hand to list today's bounties in chat.
- A player Hostile with the hobbits can read the board but can't take or hand in notices: "The notices are not for the likes of you." The board belongs to the factions whose settlement it's in (any faction that rewards bounties, for a board in the wild).
- **Restocking:** each in-game day, a board posts 3 different bounties, drawn by weight. A taken notice stays gone until the next day, for everyone. A board that's just been hung up starts bare and gets its first notices the next morning, so taking a board down and putting it back doesn't restock it.

![The two bounty boards on the inn's east wall, three notices on each](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/hobbit-tavern-bounty-boards.png)

### Bounty notices
- Notices track progress from anywhere in your inventory.
  - **Hunts** count your kills, including kills with projectiles. Each kill counts towards one notice (the first unfinished one for that creature). Boss minions don't count.
  - **Expeditions** complete when you stand inside the structure. Players are checked once a second. Each structure finishes one notice of each expedition per player, ever (the last 128 are remembered), so carrying a stack of the same notice doesn't pay out several times.
- The tooltip shows the job, progress and reward, and a finished notice glows.
- **Claiming:** right-click a bounty board or an innkeeper with a finished notice. You get emeralds, a roll of spoils, vanilla XP and class XP. An unfinished notice is refused and kept.

| Bounty | Job | Emeralds | Spoils | XP | Class XP | Weight |
|---|---|---|---|---|---|---|
| Goblin Trouble | Slay 10 goblins | 8 | minor | 30 | 50 | 10 |
| The Goblin Warlord | Slay a Goblin Warlord | 16 | major | 60 | 150 | 4 |
| Wyvern Hunt | Slay a wyvern | 12 | major | 50 | 120 | 6 |
| The Storm Dragon | Slay a Lightning Chaser | 24 | major | 100 | 250 | 2 |
| The Frost Drake | Slay a Frost Drake | 24 | major | 100 | 250 | 2 |
| Spiders in the Woods | Slay 8 spiders | 5 | minor | 20 | 30 | 10 |
| The Restless Dead | Slay 15 undead | 6 | minor | 25 | 40 | 10 |
| Brigands on the Road | Defeat 5 illagers | 10 | minor | 30 | 60 | 6 |
| Under the Mountain | Find a dwarven fortress | 10 | major | 40 | 100 | 6 |
| The Dragon's Lair | Find a dragon lair | 14 | major | 50 | 120 | 3 |

What the spoils can contain:
- **Minor spoils** (`gameplay/bounty_minor`): 1-2 rolls of food, ale, iron, gold, arrows or lapis, with a small chance of an enchanted book, an enchanted iron sword or a diamond.
- **Major spoils** (`gameplay/bounty_major`): 2-3 rolls of gold, iron, diamonds, ale, golden apples, enchanted books, enchanted diamond tools or an enchanted iron chestplate, or bottles o' enchanting. There's a 1-in-~90 chance per roll of an enchanted golden apple.

## Where to find it / How to get it
- Run `/locate structure dndclasses:hobbit_village`. Every village generates its inn, unless the terrain has no room for it.
- In the creative tab: Bounty Board, Mug of Ale, Innkeeper Spawn Egg, and one notice per bounty.

## Commands
- Give a specific notice: `/give @s dndclasses:bounty_notice{Bounty:"spiders"}`.
  - Optionally add `Progress:<n>`.
  - IDs: `goblin_trouble`, `goblin_warlord`, `wyvern_hunt`, `storm_dragon`, `frost_drake`, `spiders`, `barrow_wights`, `brigands`, `dwarf_fortress`, `dragon_lair`.
- Restock boards and the innkeeper: `/time add 24000`.

## Known limitations
- Board stock is per board, not per player: the first player to take a notice gets it.
- No custom GUI. You pick a notice by clicking on the board, and you read the board in chat.
- Placeholder art: the innkeeper uses the ordinary hobbit skins.
- Class XP is paid only once class progression is in the mod (see below).

## For developers
- Code: package `mattonfire.dnd.tavern`.
  - `Tavern` registers everything. It's called from `DnDClasses`, and the renderer is registered in `DndClassesClient`.
  - `Bounty` is the bounty list (enum).
  - `BountyBoardBlock` / `BountyBoardBlockEntity` hold the board's daily stock.
  - `BountyNoticeItem` stores NBT `Bounty` and `Progress`.
  - `BountyRewards` tracks kills and structure visits and pays rewards.
  - `AleItem` is the ale.
- Innkeeper: `entity/InnkeeperEntity` (a `HobbitEntity` that implements `Merchant`) and `entity/InnkeeperTrades`.
- Placement: `world/gen/village/InnPiece`. `HobbitVillagePlanner` now always plans the inn.
- Bounty targets are data, as entity type tags:
  - `#dndclasses:goblins`
  - `#dndclasses:wyverns`
  - `#dndclasses:bounty/goblin_warlords`
  - `#dndclasses:bounty/storm_dragons`
  - `#dndclasses:bounty/frost_drakes`
  - `#dndclasses:bounty/spiders`
  - `#dndclasses:bounty/undead`
  - `#dndclasses:bounty/brigands`
- Expedition targets are the structures `dndclasses:dwarven_fortress` and `dndclasses:dragon_lair`.
- Class XP goes through `Progression.addXp`. Finished expeditions are kept in the player's persistent data under `BountyExpeditions`.
  - If the class isn't there, it does nothing.
  - `TODO(class-progression)`: replace the lookup with a direct call once that branch merges.
- Devscript: `devscripts/hobbit-tavern.txt` takes a notice, completes a spider bounty, claims it, and opens the innkeeper's trades.
