# Gelatinous Cube
A slow, 2-block cube of translucent green jelly that oozes through dark caves and dungeons. Anything living that it touches sinks into it and is slowly dissolved. It soaks up items lying in its path, keeps them floating where you can see them, and drops them all when it dies.

## How it works
- **Stats:** 50 health, speed 0.17 (slow), full knockback resistance, 10 XP. Its hitbox is 2x2x2 blocks.
- **Hunting:** it targets players and iron golems and oozes straight into them. It has no melee swing; all its damage comes from engulfing. It fights back against whatever hurts it. It doesn't swim, so it sinks and crawls along the bottom.
- **Engulfing:** anything living whose hitbox overlaps the cube sinks in instead of being pushed aside. This covers players and mobs, but not creative or spectator players, armour stands or other cubes. While inside, the victim:
  - has Slowness III (refreshed while inside, lasts 1.5 s after leaving);
  - is gently pulled towards the middle (0.04 blocks/tick);
  - takes **3 acid damage on contact and then every second**. The damage scales with difficulty, goes through shields, and doesn't knock the victim out of the cube. The death message is "*X* was dissolved by Gelatinous Cube".

  The cube wobbles and squelches when it catches something new. Anyone stuck inside sees the world tinted green.
- **Absorbing items:** every 5 ticks it soaks up item entities touching it, once their pickup delay is over. It holds up to **6 different stacks** and tops up matching stacks first; anything that doesn't fit stays on the ground. The stacks float and turn inside the jelly. Once it has taken an item it never despawns, like vanilla mobs that pick up loot. Drops of the mobs it dissolves get absorbed too.
- **Spawn contents:** new cubes may already hold 0-3 rolls of junk from earlier adventurers: bones, arrows, rotten flesh, iron or gold nuggets, torches, coal, iron ingots, and rarely an emerald, a worn iron sword or leather boots, a skeleton skull, a golden apple or a diamond.
- **Death:** it drops everything it absorbed, plus 1-3 slime balls (+ Looting). It slumps where it is rather than toppling over.

## Where to find it
It spawns anywhere in the Overworld (monster, weight 5, always alone), but only:
- in the dark (monster light rules);
- out of sight of the sky;
- more than 10 blocks below sea level.

So it turns up in caves and dungeons, never on the surface. It needs a 2x2x2 space to spawn.

## Commands
- `/summon dndclasses:gelatinous_cube` spawns one with random contents.
- `/summon dndclasses:gelatinous_cube ~ ~ ~ {AbsorbedItems:[{id:"minecraft:diamond",Count:1b}]}` spawns one with exactly the given contents.
- `/data get entity @e[type=dndclasses:gelatinous_cube,limit=1,sort=nearest] AbsorbedItems` lists what it holds.

## Configuration
- `mobGriefing`: when false, it doesn't absorb items. Engulfing still works.
- `doMobLoot` affects only the slime balls. Absorbed items always drop.
- The spawn contents are a datapack-editable loot table: `dndclasses:gameplay/gelatinous_cube_contents`.

## Known limitations
- The texture is placeholder art.
- The jelly doesn't hide whatever is drawn after it. Without that, an engulfed mob drawn after the cube vanished inside it. The catch is that engulfed mobs show through untinted, as if in front of the jelly. Absorbed items are drawn before the jelly, so they do look tinted.
- Only 6 stacks are kept or shown. It has no inventory beyond that.
- There is no dedicated dungeon spawner or structure; "dungeons" means the general underground spawn rules above.
- Enchantment glint on absorbed items may be hidden behind the jelly.

## For developers
- `src/main/java/mattonfire/dnd/entity/GelatinousCubeEntity.java`: AI, engulfing (`engulfTouching`, `holdInside`, `digest`), item absorption, `AbsorbedItems` NBT, spawn rule (`canSpawn`). Absorbed stacks are synced through 6 tracked `ItemStack` data slots.
- `src/main/java/mattonfire/dnd/client/model/GelatinousCubeModel.java`: the custom render layer: entity-translucent, no culling, color-only write mask (no depth write).
- `src/main/java/mattonfire/dnd/client/renderer/GelatinousCubeRenderer.java`: draws the absorbed items, then flushes the item buffers so they're drawn before the jelly.
- Registration: `ModEntityTypes.GELATINOUS_CUBE`, renderer in `DndClassesClient`, spawns in `ModSpawns`, `ModDamageTypes.GELATINOUS_CUBE_DAMAGE_SOURCE`.
- Data: `data/dndclasses/damage_type/gelatinous_cube.json` (in `minecraft:bypasses_shield`), `loot_tables/entities/gelatinous_cube.json`, `loot_tables/gameplay/gelatinous_cube_contents.json`.
- Assets: `geo/entity/gelatinous_cube.geo.json`, `animations/entity/gelatinous_cube.animation.json` (`idle`, `walk`, `engulf`), `textures/entity/gelatinous_cube.png`.
- Devscript: `devscripts/gelatinous-cube.txt` takes screenshots and checks absorbing a diamond, engulfing a pig and drops on death:
  `timeout 300 ./gradlew runClient -PdevScript=devscripts/gelatinous-cube.txt`
