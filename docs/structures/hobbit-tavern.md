# Hobbit Tavern
Every hobbit village has the Green Dragon inn. Inside, an **innkeeper** sells food and ale, buys produce and rents out rooms in the sleeping loft. On the inn's east wall hang two **bounty boards**, where hobbits pin up jobs: hunts and expeditions that pay emeralds, loot, XP and class XP.

![The innkeeper in front of the Green Dragon's bar, with the bounty boards on the wall to the right](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/hobbit-tavern-innkeeper.png)

## How it works
### Innkeeper
- A hobbit (14 HP) holding a Mug of Ale, named "<name>, Innkeeper". It stays within 4 blocks of the bar by day and within 3 at night.
- Right-click it to open the trade screen. Each trade can be used 12 times, and stock refills each in-game morning.
  - **Sells (for emeralds):**
    - 1 emerald each: 2 ale, 4 bread, 3 cooked chicken, 2 pumpkin pie, 8 cookies or 2 honey bottles
    - 2 emeralds: 1 rabbit stew
    - 3 emeralds: 1 cake, or a **Room Key** (see [Rooms for the night](#rooms-for-the-night))
  - **Buys (for 1 emerald):** 20 wheat, 24 potatoes, 22 carrots, 10 brown mushrooms or 6 pumpkins.
  - **Halflings** pay 25% less (kin prices, see [Racial homes](../races/racial-homes.md)): rabbit stew 1, cake 2, and the innkeeper takes 15 wheat, 18 potatoes, 16 carrots, 7 mushrooms or 4 pumpkins. One-emerald trades stay at 1.
  - **Halflings only** (kin stock): the [Halfling Sling](../items/racial-weapons.md#halfling-sling) for 6 emeralds (4 at kin price), listed after the other trades. Nobody else sees it.
- Right-clicking it with a finished bounty notice pays the bounty out.
- **Reputation** ([Factions](../systems/factions.md)): each customer sees prices for their own standing with the hobbits: +50% at Unfriendly (rounded up, at least +1), -10% at Friendly, -25% at Honored, -40% at Exalted, never below 1. A cake costs 5 emeralds at Unfriendly and 2 at Exalted; the innkeeper wants 30 wheat at Unfriendly and 12 at Exalted. A player Hostile with the hobbits gets "<name> won't serve you.": no trades and no bounty payouts.

### Rooms for the night
- **The loft:** a ladder up the east wall, just inside the door, leads to a sleeping loft under the thatch with **4 green beds**, two nightstands and a lantern.
- **Room Key:** 3 emeralds from the innkeeper (reputation and kin prices apply as to the other trades). It's named "Green Dragon Room Key", doesn't stack, and is **good until the next dawn**; after that its tooltip says "Expired" and it does nothing.
- **Honored with the hobbits:** the innkeeper adds a round of 3 ale for 1 emerald, with a cake on the house, at the end of the trades, and the first time you open the trades each day hands you that night's Room Key free (it counts as your one room that day).
  - **One a day:** after you've bought one, the trade shows as sold out to you until the next day, and the innkeeper says "Your room's already paid for."
- **Sleeping:** with a valid key anywhere in your inventory, any bed inside the inn lets you sleep **at any time of day**, even with monsters nearby.
  - After **10 seconds** asleep you get a **long rest** (full health, mana and charges, Hit Dice back; see [Rests](../systems/rests.md#long-rests-in-beds)). The key is used up.
  - **By day the time isn't skipped**, and you get up when the rest is done. At night the bed also counts towards skipping the night as usual.
  - You wake **Well Rested** for 10 minutes: **one temporary charge** above your max (a gold gem, spent before your others, gone when the effect ends or you die) and **Absorption I**.
- The one-long-rest-a-day rule still applies: if you've already had one today, the key won't let you sleep by day ("You've already had a long rest today.") and you keep it.
- A key does nothing special in a bed outside its inn, and an expired key does nothing.

### Mug of Ale
A drink: 3 hunger, 0.4 saturation, 6 s of Regeneration I, and a 25% chance of 8 s of Nausea. Stacks to 16.

### Bounty board
- Each board has three notices.
- **Take a notice:** right-click it. Which one you get (left, middle or right) depends on where you click, and the paper disappears from the board.
- **Read the board:** sneak-right-click with an empty hand to list today's bounties in chat.
- A player Hostile with the hobbits can read the board but can't take or hand in notices: "The notices are not for the likes of you." The board belongs to the factions whose settlement it's in (any faction that rewards bounties, for a board in the wild).
- **Rumours:** each day there's a 20% chance a board's third (right-hand) notice is a **Rumour** instead of a bounty. Taking it gives a *Rumour* item for a quest you could start now, picked by weight among quests with a `rumour_weight` (launch: *The Whispering Library* 2, *Trouble on the Road* 1, *Ice and Storm* 1). If no such quest is open to you, the Rumour stays up for someone else ("The rumour makes no sense to you yet."). Right-click the Rumour to start its quest without finding its giver; its other requirements (earlier quests, standing) still apply, and a refused Rumour is kept. Reading the board lists it as "A Rumour". See [Quests](../systems/quests.md#launch-quest-chains).
- **Restocking:** each in-game day, a board posts 3 different bounties, drawn by weight. A taken notice stays gone until the next day, for everyone. A board that's just been hung up starts bare and gets its first notices the next morning, so taking a board down and putting it back doesn't restock it.

![The two bounty boards on the inn's east wall, three notices on each](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/hobbit-tavern-bounty-boards.png)

### Bounty notices
- Notices track progress from anywhere in your inventory.
  - **Hunts** count your kills, including kills with projectiles. Each kill counts towards one notice (the first unfinished one for that creature). Boss minions don't count.
  - **Dungeon clearances** send you to a [dungeon](dungeons.md) (see below).
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
| Clear the Barrow | Clear a Crypt | 14 (+25%/tier) | major | 60 | 150 | 4 |
| Smoke out the Warren | Clear a Goblin Warren | 16 (+25%/tier) | major | 70 | 180 | 4 |
| Reclaim the Lost Hold | Clear a Dwarven Ruin | 20 (+25%/tier) | major | 90 | 220 | 3 |

### Dungeon clearances
- A board only posts a clearance if the world has a dungeon structure of that theme (any registered theme works; *Reclaim the Lost Hold* appears once the Dwarven Ruin exists).
- **Taking** one binds the notice to the nearest uncleared dungeon of its theme within 2000 blocks of the board, including ones nobody has found yet, and gives you a **Dungeon Map**: an explorer-style map centred on the entrance with a red X, named "Map to <dungeon name>". If there's none in range, the notice stays up: "Nobody knows of such a place nearby."
- The notice's tooltip shows the dungeon's name and Challenge tier, and the reward for that tier.
- It's **done** when that dungeon is cleared (its boss dies, or `/dungeon clear`) while you're inside it with the notice. Everyone inside with a notice for that dungeon gets credit.
- **Reward:** the emeralds grow by 25% of the base per tier over I, rounded (Clear the Barrow: 14 / 18 / 21 / 25 at tiers I-IV). Spoils, XP and class XP don't change.
- A notice from the creative tab or `/give` isn't bound yet: right-click it in the air to bind it to the nearest dungeon to you and get the map.

What the spoils can contain:
- **Minor spoils** (`gameplay/bounty_minor`): 1-2 rolls of food, ale, iron, gold, arrows or lapis, with a small chance of an enchanted book, an enchanted iron sword or a diamond. Plus a 10% chance of an Uncommon magic item and 5% of a Wand of Magic Missiles, both identified (bounty rewards are known goods, see [Magic loot](../systems/magic-loot.md)).
- **Major spoils** (`gameplay/bounty_major`): 2-3 rolls of gold, iron, diamonds, ale, golden apples, enchanted books, enchanted diamond tools or an enchanted iron chestplate, or bottles o' enchanting. There's a 1-in-~90 chance per roll of an enchanted golden apple. Plus a 35% chance of an Uncommon (60) or Rare (40) magic item, and 3% each of a Periapt of Wound Closure and a Doss Lute, all identified.

## Where to find it
- Run `/locate structure dndclasses:hobbit_village`. Every village generates its inn, unless the terrain has no room for it.
- In the creative tab: Bounty Board, Mug of Ale, Innkeeper Spawn Egg, and one notice per bounty.
- Room Keys come only from an innkeeper (a key that wasn't bought opens nothing).

## Commands
- Give a specific notice: `/give @s dndclasses:bounty_notice{Bounty:"spiders"}`.
- Give a Rumour: `/give @s dndclasses:quest_lead{Quest:"dndclasses:whispering_library/1"}`.
- Force today's Rumour on a board you're looking at: `/data merge block <x y z> {Rumour:1b}` (after the board has posted today's notices).
  - Optionally add `Progress:<n>`.
  - IDs: `goblin_trouble`, `goblin_warlord`, `wyvern_hunt`, `storm_dragon`, `frost_drake`, `spiders`, `barrow_wights`, `brigands`, `dwarf_fortress`, `dragon_lair`, `clear_barrow`, `smoke_warren`, `lost_hold`.
- Restock boards and the innkeeper: `/time add 24000`.
- A Room Key for a test room: `/give @s dndclasses:room_key{Inn:"Test",InnBox:[I;x1,y1,z1,x2,y2,z2],Day:<day>L}`, where `<day>` is `/time query day` (the in-game day the key is good for) and the box holds the beds.
- `/dndclass rest @s allow` forgets today's long rest, to try a room again.

## Known limitations
- Board stock is per board, not per player: the first player to take a notice gets it.
- No custom GUI. You pick a notice by clicking on the board, and you read the board in chat.
- Placeholder art: the innkeeper uses the ordinary hobbit skins.
- Class XP is paid only once class progression is in the mod (see below).
- Inns generated before rooms were added have no loft. Their innkeepers sell keys, which open any bed you place inside the inn.
- Beds aren't reserved: any key holder can use any of the inn's beds.

## For developers
- Code: package `mattonfire.dnd.tavern`.
  - `Tavern` registers everything. It's called from `DnDClasses`, and the renderer is registered in `DndClassesClient`.
  - `Bounty` is the bounty list (enum).
  - `BountyBoardBlock` / `BountyBoardBlockEntity` hold the board's daily stock (NBT `Day`, `Posted`, and `Rumour` for the third slot, `RUMOUR_CHANCE` 0.2).
  - `QuestLeadItem` is the Rumour (`dndclasses:quest_lead`, NBT `Quest` and `Title`); `QuestLeadItem.pick` chooses the quest.
  - `BountyNoticeItem` stores NBT `Bounty` and `Progress`.
  - `BountyRewards` tracks kills and structure visits and pays rewards.
  - `DungeonBounties` binds dungeon clearances (notice NBT `StartKey`, `Dimension`, `DungeonName` as text JSON, `DungeonTier`), completes them on `DungeonEvents.CLEARED` and scales the emeralds (`Bounty.emeraldsFor`). `Bounty.isAvailable(world)` keeps a clearance off the board when no structure of its theme is registered.
  - `AleItem` is the ale.
- Innkeeper: `entity/InnkeeperEntity` (a `HobbitEntity` that implements `Merchant`) and `entity/InnkeeperTrades`.
- Rooms:
  - `tavern/RoomKeyItem`: NBT `Inn` (name), `InnBox` (int[6], the inn's bounding box) and `Day` (`timeOfDay / 24000` when bought). The innkeeper stamps the key in `onSellingItem`, when it appears in the trade output slot.
  - `InnkeeperEntity.setInn(BlockBox, name)` sets the inn its keys open; `InnPiece` passes its bounding box and "Green Dragon". An innkeeper without one (spawn egg, other settlements) uses a box around its home: 8 blocks each way across, 3 down, 8 up. Settlements with their own inns should call `setInn`.
  - One key a day per player: persistent data `dndRoomKeyDay`. The offer is shown disabled to that customer and restored when they leave.
  - `tavern/TavernRooms`: `BedRest.RULE` (`TAVERN`, 200 ticks) for a bed inside a held key's inn, and `EntitySleepEvents` `ALLOW_SLEEP_TIME`, `ALLOW_NEARBY_MONSTERS` and `ALLOW_RESETTING_TIME` (false by day, so the day isn't skipped). `RestEvents.AFTER_REST` with `TAVERN` uses up the key and applies Well Rested.
  - `classes/Effects/WellRestedEffect` (`dndclasses:well_rested`): `RestState.tempCharges` is set to 1 by `TavernRooms` and cleared by the effect's `onRemoved` (and on respawn).
  - Textures: `tools/tavern_room_textures.py`.
  - Devscript: `devscripts/tavern-room.txt`.
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
- Devscript: `devscripts/hobbit-tavern.txt` takes a notice, completes a spider bounty, claims it, and opens the innkeeper's trades. `devscripts/dungeon-bounty.txt` takes *Clear the Barrow* next to the dev world's crypt, checks the map, clears the crypt and claims the reward. `devscripts/quest-rumour.txt` forces a Rumour, takes it and follows it.
