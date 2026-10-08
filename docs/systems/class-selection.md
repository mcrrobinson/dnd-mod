# Class selection
Every player picks one of 15 classes. Each class changes your base stats and gives you passives, drawbacks and a [special](mana.md).

## How it works
- The **class picker** opens when you join without a class. Your class is saved with the player (`DndClass` in the player NBT), so rejoining, dying and leaving the End all keep it. It's a 3×5 grid of buttons and can't be closed with Escape.
- Picking a class resets you to vanilla stats (20 health, 1 attack damage, 0.1 speed, 4 attack speed, 0 luck), clears your status effects, then applies the class and its starting health (Barbarian, Fighter and Paladin 25, Bard 15, Wizard 10, Necromancer 5). Chat shows its pros, cons and special.
- You can only pick once: the server ignores a pick from a player who already has a class. Changing class afterwards needs `/dndclass set`.
- Rejoining, respawning and leaving the End only re-apply the class's base stats. Status effects and health are kept (a death respawns you at full health as usual).
- On death and End exit the mod's per-player data (skill progress, mana, Druid forms) carries over to the new player, so dying doesn't refill mana.
- Classes: [Barbarian](../classes/barbarian.md), [Bard](../classes/bard.md), [Cleric](../classes/cleric.md), [Druid](../classes/druid.md), [Fighter](../classes/fighter.md), [Monk](../classes/monk.md), [Paladin](../classes/paladin.md), [Ranger](../classes/ranger.md), [Rogue](../classes/rogue.md), [Necromancer](../classes/necromancer.md), [Warlock](../classes/warlock.md), [Wizard](../classes/wizard.md), [Artificer](../classes/artificer.md), [Blood Hunter](../classes/blood-hunter.md), [Alchemist](../classes/alchemist.md).
- **Character Progress** screen: press **O** to see a per-class progress counter.

## Commands
- `/dndclass get|set` changes a class without dying. See [Admin commands](admin-commands.md).

## Known limitations
- Nothing increases the Character Progress counters yet.

## For developers
- Picker GUI: `Client/Hud/ClassSelectionHud` (LibGui), opened by `DndClassesClient.handleClassQuery` when the server sends class 0. The client never applies stats itself.
- Server side, all in `ClassLifecycle`:
  - `change` is the only path that picks or switches a class (picker packet, `/dndclass set`): it calls every `ClassSkills.forget`, clears effects, applies the stats, sets the starting health, sends the chat summary and the guidebook.
  - The pick packet runs on the server thread and is rejected unless the player's class is NONE; bad ids are ignored.
  - `JOIN`, `COPY_FROM` and `AFTER_RESPAWN` Fabric events re-apply stats only, copy the class and the whole `mattonfire.dnd.classes` persistent compound, and re-sync the class and mana to the client.
  - `DISCONNECT` drops per-player server state (`ClassSkills.forget`, attack-roll crits, Featherfall, party invites, finished cooldowns).
- Per-class base values and starting health: the table in `ClassStats`.
- Class ids: the `DndCharacter` enum (NONE = 0 ... ALCHEMIST = 15).
- Saving: `mixin/PlayerEntityMixin` writes and reads `DndClass` in the player NBT.
- Devscripts: `class-none-fallthrough.txt`, `dndclass-command.txt`, `class-persist-set.txt` then `class-persist-check.txt` (sets a class, then checks it after rejoining), `apply-path-set.txt` then `apply-path-check.txt` (Necromancer health and a potion effect survive a rejoin; class and stats survive a death).
