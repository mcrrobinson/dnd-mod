# Beholder
A floating eye tyrant that guards a sealed cavern deep in the deepslate. In a fight it switches between two phases: an open central eye whose anti-magic cone shuts down class specials, and volleys of eye rays from its eight eyestalks. You can hit each eyestalk on its own to shut it.

![A Beholder](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-flesh-out/beholder.png)

## How it works

| Stat | Value |
|-|-|
| Health | 250 |
| Armor | 8, plus 2 toughness |
| Knockback resistance | 80% |
| Bite | 10 |
| Speed (walk / fly) | 0.25 / 0.5 |
| Follow range | 32 blocks |
| XP | 120 |

It flies, takes no fall damage, never despawns and turns its whole body to face what it's looking at. While it fights a player, a purple, notched boss bar shows to everyone within 48 blocks and **Tooth and Claw** plays.

### Gaze phase
The gaze phase lasts 6 seconds (4 when enraged). The central eye is open and casts an **anti-magic cone** 20 blocks long and 30° either side of where it's looking, blocked by walls, and shown by grey particles. The eye also stays open whenever it isn't fighting.

Players in the cone (not creative or spectator) get **Anti-Magic** for 1.5 seconds, refreshed every 5 ticks. While you have it:

- the class power-up (Z) fizzles with an action-bar message, and you keep your mana;
- Warlock fireballs and the Monk's double jump don't work;
- Arrow Storm, Mob Repel, Reinforced Armor and Invulnerable are removed the moment it lands.

During this phase it moves in to about 2.5 blocks and bites once a second.

### Ray phase
The ray phase lasts 8 seconds (10 when enraged). It shuts the central eye and hangs about 9 blocks back and 3 above you. Every 1.5 seconds (0.9 when enraged) one open eyestalk glows in its colour for 0.8 seconds, then fires. The ray goes where you were when the glow started, so moving dodges it. Rays reach 32 blocks, stop at walls and hit the first creature in the way.

| Eye | Colour | Effect |
|-|-|-|
| Slowing | Blue | 2 magic damage and Slowness III for 6 s |
| Telekinetic | Violet | Levitation II for 2.5 s |
| Enervation | Red | 8 magic damage (10 when enraged) |
| Fear | Yellow | **Frightened** for 8 s and Darkness for 4 s |

**Frightened** halves your attack damage. While it lasts, the Beholder shoves you back every half second whenever you're within 7 blocks of it.

### Eyestalks
Each stalk has its own hit shape (F3+B shows them as green boxes). A hit of 3 or more damage shuts that eye for 10 seconds: an eyelid closes over it and it can't fire.

### Enrage
Below 50% health the bar turns red, every eye reopens, two different rays fire at once, and the phases change length as listed above.

### Rewards

| Drop | Count | Condition |
|-|-|-|
| Eyes of ender | 2-4 (+0-1 per Looting level) | always |
| Amethyst shards | 4-9 (+0-2 per Looting level) | always |
| Diamonds | 3-6 | killed by a player |
| Protection IV book, Feather Falling IV book or totem of undying | 1 | killed by a player, 30% chance (+5% per Looting level) |

The player credited with the kill also gets **An Eye for an Eye**, a challenge advancement worth 150 XP.

### Tips
- Use your class power-up before the fight or during the ray phase, when the central eye is shut. In the gaze phase it just fizzles.
- Shut the eyes. Any hit of 3 damage or more on a stalk closes it for 10 seconds. An arrow from a fully drawn bow does it, so a bow is a good way to strip its rays from range.
- The red Enervation eye hurts most. Shut it first.
- Keep moving sideways in the ray phase. Each ray aims at where you stood when its eye started to glow.
- Bring Slow Falling or a water bucket for the Telekinetic ray, and good armour: it hits hard and is deadly without it.

## Where to find it
Beholders only live in **Beholder lairs**, which are rare (spacing 40 chunks, separation 16). A lair can generate under any Overworld biome except the deep dark, wherever its stair comes up on dry land.

**The cavern** is a dome of radius 16 and height 12, with its floor somewhere between y=-38 and y=-19. A two-block deepslate shell keeps caves, water and lava out. The floor is a great eye of calcite and blackstone round a verdant froglight iris and an obsidian pupil, and the walls have crying obsidian, amethyst, dripstone and the odd shroomlight. Five petrified adventurers (grey armour stands, arms raised) stand round the floor. Against the west wall, a hoard of gold, bones and skulls surrounds a chest (`chests/beholder_lair`). The Beholder hovers 5 blocks above the middle of the floor.

**The way in** is a deepslate spiral stair that comes up 20 blocks east of the cavern's centre, inside a mossy ring of wall with a soul lantern, and a tunnel from its foot to the cavern.

No other monsters spawn in the lair, and Silent Footsteps plays inside. Once its Beholder is dead, the lair now and then sends a new one: a 1 in 20 roll on each natural spawn, out of sight of the sky, with no other Beholder within 64 blocks.

## Commands
- `/locate structure dndclasses:beholder_lair`
- `/summon dndclasses:beholder`, or the Beholder Spawn Egg in creative.

## Known limitations
- The model, texture and effect icons are placeholder art. The eyes don't glow: GeckoLib's glow layer rendered the irises black.
- On the server, eyestalk hit shapes and ray origins use the model's rest pose. On the client they follow the animation.
- The anti-magic cone only affects players.
- The restock rate is a rough guess.

## For developers
- **Entity:** `entity/BeholderEntity.java`.
  - The gaze and ray cycle, ray charges and the `Ray` enum.
  - A synced closed-eye bitmask and central-eye-shut flag.
  - A body control that does nothing, plus a vex-style `FloatMoveControl`.
  - Goals: `entity/ai/goal/BeholderAttackGoal.java` and `BeholderDriftGoal.java`.
- **Boss framework:** `BossFight` with `.phase(0.5F, RED, enrage)`, `.xp(120)` and `.advancement(dndclasses:beholder_slayer)`.
- **Hit shapes:** `MultipartDragon` with a `DragonPartLayout` of 4 `pair(...)` eyestalk groups.
  - `MultipartDragon.damagePart(part, source, amount)` is the hook `DragonPart.damage` calls; by default it just damages the owner. The Beholder overrides it to shut the stalk that was hit.
- **Effects:** `classes/Effects/AntiMagicEffect.java` (`isSuppressed` / `blocks` helpers) and `FrightenedEffect.java`, registered in `ModEffects`.
  - The checks are in `DnDClasses.sendPowerupPacket`, `Warlock.throwFireball` and `DoubleJumpMixin.canJump`.
- **Client:** `client/model/BeholderModel.java` (body pitch, eyelids) and `client/renderer/BeholderRenderer.java` (a `DragonRenderer`).
  - Assets: `geo/beholder.geo.json`, `animations/beholder.animation.json` and `textures/entity/beholder/beholder.png`.
- **Lair:** `world/gen/beholder/`, with `BeholderLairStructure`, `BeholderLairStructures`, `BeholderCavernPiece` and `BeholderShaftPiece`.
  - Data: `worldgen/structure/beholder_lair.json` and `worldgen/structure_set/beholder_lairs.json`.
  - Biome tag: `tags/worldgen/biome/has_structure/beholder_lair.json`.
  - Spawn rule: `ModSpawns.canBeholderSpawn`.
- **Loot and advancement:** `loot_tables/entities/beholder.json`, `loot_tables/chests/beholder_lair.json` and `advancements/beholder_slayer.json`.
- **Devscript:** `devscripts/beholder.txt` takes model and hitbox shots, then visits the dev-world lair at about (-504, 760) and fights in survival.
