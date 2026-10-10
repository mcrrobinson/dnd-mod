# Mimic
A chest that isn't. A sleeping mimic looks exactly like a normal chest until you open it or hit it. Then it bites, grabs you in its jaws and hops after you. When it dies it drops the loot of the chest it replaced.

![A Mimic in its chest disguise](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-flesh-out/mimic.png)

## How it works

| Stat | Value |
|-|-|
| Health | 30 |
| Armor | 4 |
| Knockback resistance | 60% |
| Bite | 6 |
| Speed | 0.28 |
| Follow range | 16 blocks |
| XP | 10 |

**The disguise.** A sleeping mimic is drawn with the real chest texture, so it matches your resource pack. It sits in the middle of a block facing north, south, east or west, makes no ambient sound, casts no shadow and can't be pushed. It's solid, so you can bump into it and stand on it. Mountain Dwarves ignore a sleeping one too.

**Waking.** Right-click it as if to open it, or hit it. It creaks open with a growl, bites straight away and starts hunting you.

**The grab.** A bite that lands holds you for 1.5 seconds. You're pulled to its mouth, slowed (Slowness IV) and can't jump out. Once it lets go it can't grab again for 3 seconds. The grab also ends if you get more than 4 blocks away, for example from knockback.

**Going back to sleep.** After 10 seconds with no target, it closes its lid, shuffles back to the middle of its block, squares up and is a chest again.

**Loot.** It drops the loot table of the chest it replaced, plus 10 XP. One from a spawn egg or `/summon` drops simple dungeon chest loot (`dndclasses:entities/mimic`).

### Tips
- Be suspicious of a chest in a dungeon, a dragon lair or a dwarven fortress. Left-click it before you open it. A real chest starts to break; a mimic takes the hit and wakes up.
- Don't fight one with your back to a wall. Being held at its mouth for 1.5 seconds hurts less if you can step away right after.
- A mimic you run from goes back to sleep after 10 seconds, still holding its loot. You can come back for it.

## Where to find it
- **Dungeons (monster rooms):** 20% of their chests are mimics with dungeon loot.
- **[Dragon lairs](../structures/dragon-lairs.md) and [frost lairs](../structures/frost-lairs.md):** half of all lairs have a second "chest" 2 blocks from the hoard, holding hoard loot (`chests/dragon_lair` or `chests/frost_lair`).
- **[Dwarven fortresses](../structures/dwarven-fortresses.md):** 10% of chests (treasury, forge, barracks, mine) are mimics with that room's loot.
- Mimic Spawn Egg in the mod's creative tab.

## Commands
- `/summon dndclasses:mimic ~ ~ ~` spawns a sleeping one.
- Add `{Dormant:0b}` to spawn it awake, or `{MimicLoot:"<loot table id>"}` to set what it drops.

## Known limitations
- It's a hostile mob, so in peaceful it disappears along with its loot.
- Mimics only appear in newly generated chunks. A fortress or lair whose generation started before mimics were added may lay out slightly differently in its remaining chunks, because the mimic roll uses extra random numbers.

## For developers
- `entity/MimicEntity.java`: sleep state, waking, bite and grab (`GRAB_TICKS` 30, `GRAB_COOLDOWN` 60), going back to sleep (`SETTLE_DELAY` 200) and the loot override. NBT: `Dormant`, `RestYaw`, `MimicLoot`. Worldgen uses `MimicEntity.disguised(...)`.
- `client/MimicTexture.java` builds the texture at runtime from the vanilla chest texture plus `textures/entity/mimic/mouth.png`, and rebuilds it on every resource reload. `client/model/MimicModel.java` and `client/renderer/MimicRenderer.java` do the model and rendering (no shadow while asleep).
- `geo/mimic.geo.json` uses the vanilla chest UV layout. The teeth and tongue sit inside the closed chest, so they only show when the lid opens. `animations/mimic.animation.json`: dormant, awake, walk, wake, bite.
- Worldgen: `classes/mixin/DungeonFeatureMixin.java` (redirects the chest's `setLootTable` call), `world/gen/lair/LairPiece.java` and `world/gen/fortress/FortressPiece.java` (`Builder.chest` / `mimic`), each with its own `MIMIC_CHANCE`.
- Mountain Dwarves skip sleeping mimics: see the target check in `MountainDwarfEntity`.
- Devscript: `devscripts/mimic.txt` (disguise, wake, bite, kill).
