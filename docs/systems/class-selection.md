# Class selection
Every player picks one of 15 classes. Each class changes your base stats and gives you passives, drawbacks and a [special](mana.md).

## How it works
- The **class picker** opens when you join without a class. Your class is saved with the player (`DndClass` in the player NBT), so rejoining, dying and leaving the End all keep it. It's a 3×5 grid of buttons and can't be closed with Escape.
- Picking a class resets you to vanilla stats (20 health, 1 attack damage, 0.1 speed, 4 attack speed, 0 luck), clears your status effects, then applies the class. Chat shows its pros, cons and special.
- Classes: [Barbarian](../classes/barbarian.md), [Bard](../classes/bard.md), [Cleric](../classes/cleric.md), [Druid](../classes/druid.md), [Fighter](../classes/fighter.md), [Monk](../classes/monk.md), [Paladin](../classes/paladin.md), [Ranger](../classes/ranger.md), [Rogue](../classes/rogue.md), [Necromancer](../classes/necromancer.md), [Warlock](../classes/warlock.md), [Wizard](../classes/wizard.md), [Artificer](../classes/artificer.md), [Blood Hunter](../classes/blood-hunter.md), [Alchemist](../classes/alchemist.md).
- **Character Progress** screen: press **O** to see a per-class progress counter.

## Commands
- `/dndclass get|set` changes a class without dying. See [Admin commands](admin-commands.md).

## Known limitations
- Nothing increases the Character Progress counters yet.

## For developers
- Picker GUI: `Client/Hud/ClassSelectionHud` (LibGui), opened by `SetPlayerClass` when the server sends class 0.
- Server side: `DnDClasses.applyClass`, with per-class stats in `SetClassAttributes`. Join and respawn: `mixin/PlayerManagerMixin` (respawning copies the class to the new player entity and re-applies its stats).
- Class ids: the `DndCharacter` enum (NONE = 0 ... ALCHEMIST = 15).
- Saving: `mixin/PlayerEntityMixin` writes and reads `DndClass` in the player NBT.
- Devscripts: `class-none-fallthrough.txt`, `dndclass-command.txt`, `class-persist-set.txt` then `class-persist-check.txt` (sets a class, then checks it after rejoining).
