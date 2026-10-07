# Mimic
A chest that isn't. A sleeping mimic looks exactly like a normal chest until someone opens or hits it. Then it bites, grabs the player in its jaws and hops after them. When it dies it drops the loot of the chest it replaced.

## How it works
- **Disguise**: a sleeping mimic is drawn with the real chest texture (`minecraft:textures/entity/chest/normal.png`), so it matches your resource pack. It sits in the middle of a block, faces north, south, east or west, makes no ambient sound, casts no shadow and can't be pushed. It is solid, so you can bump into it and stand on it. Mountain Dwarves ignore a sleeping one.
- **Waking**: right-click it (open it) or hit it. It creaks open with a growl and bites straight away.
- **Bite and grab**: a bite does 6 damage. A bite that lands holds the victim for 1.5 s (30 ticks): they are pulled to its mouth, given Slowness IV and can't jump out. After a grab it waits 3 s before it can grab again.
- **Stats**: 30 HP, 4 armour, 0.6 knockback resistance, speed 0.28, follow range 16. Once awake it hunts nearby players.
- **Going back to sleep**: after 10 s (200 ticks) with no target, it closes its lid, moves back to the middle of its block, faces north, south, east or west again, and is a chest once more.
- **Loot**: it drops the loot table of the chest it replaced, plus 10 XP. A mimic from a spawn egg or `/summon` uses `dndclasses:entities/mimic`, which is `minecraft:chests/simple_dungeon`.

## Where to find it
- **Dungeons (monster rooms)**: 20% of chests are mimics with dungeon loot.
- **[Dragon lairs](../../README.md#dragon-lairs)**: half of all lairs have a second "chest" 2 blocks from the hoard, holding hoard loot (`chests/dragon_lair`).
- **[Dwarven fortresses](../../README.md#dwarven-fortresses)**: 10% of chests (treasury, forge, barracks, mine) are mimics with that room's loot.
- **Creative**: Mimic Spawn Egg.

## Commands
- `/summon dndclasses:mimic ~ ~ ~` spawns a sleeping one. Add `{Dormant:0b}` to spawn it awake, or `{MimicLoot:"<loot table id>"}` to set what it drops.

## Known limitations
- It's a hostile mob, so in peaceful it disappears along with its loot.
- Mimics only appear in newly generated chunks. A fortress or lair whose generation started before this change may lay out slightly differently in its remaining chunks, because the mimic chance uses extra random numbers.

## For developers
- `entity/MimicEntity.java`: the mimic itself (sleep state, waking, bite and grab, going back to sleep, loot override). NBT: `Dormant`, `RestYaw`, `MimicLoot`. Worldgen spawns use `MimicEntity.disguised(...)`.
- `client/MimicTexture.java`: builds the texture at runtime from the vanilla chest texture plus `textures/entity/mimic/mouth.png`, and rebuilds it on every resource reload. `client/model/MimicModel.java` and `client/renderer/MimicRenderer.java` handle the model and rendering (no shadow while asleep).
- `geo/mimic.geo.json` uses the vanilla chest UV layout. The teeth and tongue sit inside the closed chest, so they only show when the lid opens. `animations/mimic.animation.json`: dormant, awake, walk, wake, bite.
- Worldgen: `classes/mixin/DungeonFeatureMixin.java` (redirects the chest's `setLootTable` call), `world/gen/lair/LairPiece.java`, `world/gen/fortress/FortressPiece.java` (`Builder.chest` / `mimic`).
- Mountain Dwarves skip sleeping mimics: see the target check in `MountainDwarfEntity`.
- Devscript: `devscripts/mimic.txt` (disguise, wake, bite, kill).
