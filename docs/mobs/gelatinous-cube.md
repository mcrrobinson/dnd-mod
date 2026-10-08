# Gelatinous Cube
A slow, 2-block cube of see-through green jelly that oozes through dark caves. Anything living that it touches sinks into it and is slowly dissolved. It soaks up items lying in its path, keeps them floating where you can see them, and drops them all when it dies.

![A Gelatinous Cube](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-flesh-out/gelatinous_cube.png)

## How it works

| Stat | Value |
|-|-|
| Health | 50 |
| Acid damage | 3 per second |
| Speed | 0.17 |
| Knockback resistance | 100% |
| Follow range | 16 blocks |
| Size | 2 x 2 x 2 blocks |
| XP | 10 |

It hunts players and iron golems and oozes straight into them. It has no swing; all its damage comes from engulfing. It fights back against whatever hurts it. It can't swim, so in water it sinks and crawls along the bottom.

### Engulfing
Anything living whose hitbox overlaps the cube sinks into it instead of being pushed aside. That covers players and mobs, but not creative or spectator players, armor stands or other cubes. While you're inside:

- you have Slowness III, which lasts 1.5 seconds after you get out;
- you're pulled gently towards the middle;
- you take **3 acid damage as soon as you touch it, then every second**. The damage scales with difficulty, goes through shields, and doesn't knock you out of the cube.

The cube wobbles and squelches when it catches something new, and anyone inside sees the world tinted green. The death message is "*X* was dissolved by Gelatinous Cube".

### Absorbing items
Every 5 ticks it soaks up dropped items it's touching, once they can be picked up. It holds up to **6 different stacks** and fills matching stacks first; anything that doesn't fit stays on the floor. The stacks float and turn inside the jelly, so you can see what you'll get. The drops of mobs it dissolves get absorbed too.

Once it has taken an item it never despawns, like a zombie that picked up loot. In peaceful it still leaves, but it drops what it holds first.

New cubes may already hold up to 3 stacks of junk from earlier adventurers:

| Item | Count | Weight |
|-|-|-|
| Bones | 1-3 | 20 |
| Arrows | 2-6 | 15 |
| Rotten flesh | 1-2 | 15 |
| Iron nuggets | 2-7 | 12 |
| Gold nuggets | 2-6 | 10 |
| Torches | 1-4 | 8 |
| Coal | 1-3 | 8 |
| Iron ingots | 1-2 | 5 |
| Emerald | 1 | 3 |
| Worn iron sword | 1 | 3 |
| Worn leather boots | 1 | 3 |
| Skeleton skull | 1 | 1 |
| Golden apple | 1 | 1 |
| Diamond | 1 | 1 |

### Death
It drops everything it absorbed, plus 1-3 slime balls (up to 1 more per Looting level). It slumps where it is rather than toppling over.

### Tips
- It's slow. Back off and hit it with a bow, or hit and step away so you never stay inside it.
- If you get caught, walk out at once. Every second inside is another 3 damage, and a shield won't help.
- Look at what's floating inside before you decide whether the fight is worth it.
- Don't drop items to distract it: it will just eat them.

## Where to find it
It can spawn anywhere in the Overworld (monster, weight 5, always alone), but only:

- in the dark, under the usual monster light rules;
- out of sight of the sky;
- more than 10 blocks below sea level.

So you'll meet it in caves and dungeons, never on the surface. It needs a 2 x 2 x 2 space to spawn. There's also a spawn egg in the mod's creative tab.

## Commands
- `/summon dndclasses:gelatinous_cube` spawns one with random contents.
- `/summon dndclasses:gelatinous_cube ~ ~ ~ {AbsorbedItems:[{id:"minecraft:diamond",Count:1b}]}` spawns one with exactly the given contents.
- `/data get entity @e[type=dndclasses:gelatinous_cube,limit=1,sort=nearest] AbsorbedItems` lists what it holds.

## Configuration
- `mobGriefing`: when false, it doesn't absorb items. Engulfing still works.
- `doMobLoot` only affects the slime balls. Absorbed items always drop.
- The starting contents are a datapack-editable loot table: `dndclasses:gameplay/gelatinous_cube_contents`.

## Known limitations
- The texture is placeholder art.
- The jelly doesn't hide whatever is drawn after it. Without that, an engulfed mob drawn after the cube vanished inside it. The catch is that engulfed mobs show through untinted, as if in front of the jelly. Absorbed items are drawn before the jelly, so they do look tinted.
- Only 6 stacks are kept or shown; there's no inventory beyond that.
- There's no dedicated dungeon spawner or structure. "Dungeons" means the general underground spawn rules above.
- Enchantment glint on absorbed items may be hidden behind the jelly.

## For developers
- `entity/GelatinousCubeEntity.java`: AI, engulfing (`engulfTouching`, `holdInside`, `digest`), item absorption, `AbsorbedItems` NBT and the spawn rule (`canSpawn`). Absorbed stacks are synced through 6 tracked `ItemStack` data slots.
- `client/model/GelatinousCubeModel.java`: the custom render layer: entity-translucent, no culling, colour-only write mask (no depth write).
- `client/renderer/GelatinousCubeRenderer.java`: draws the absorbed items, then flushes the item buffers so they're drawn before the jelly.
- Registration: `ModEntityTypes.GELATINOUS_CUBE`, renderer in `DndClassesClient`, spawns in `ModSpawns`, `ModDamageTypes.GELATINOUS_CUBE_DAMAGE_SOURCE`.
- Data: `data/dndclasses/damage_type/gelatinous_cube.json` (in `minecraft:bypasses_shield`), `loot_tables/entities/gelatinous_cube.json`, `loot_tables/gameplay/gelatinous_cube_contents.json`.
- Assets: `geo/entity/gelatinous_cube.geo.json`, `animations/entity/gelatinous_cube.animation.json` (`idle`, `walk`, `engulf`), `textures/entity/gelatinous_cube.png`.
- Devscript: `devscripts/gelatinous-cube.txt` takes screenshots and checks absorbing a diamond, engulfing a pig and the drops on death:
  `timeout 300 ./gradlew runClient -PdevScript=devscripts/gelatinous-cube.txt`
