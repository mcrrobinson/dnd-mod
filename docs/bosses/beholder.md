# Beholder
A floating eye tyrant that guards a sealed cavern deep in the deepslate. In a fight it switches between an open central eye, whose anti-magic cone stops class specials, and volleys of eye rays from its eight eyestalks. Each eyestalk can be hit on its own.

## How it works
- **Stats:** 250 health, 8 armour, 2 toughness, 80% knockback resistance and a 10-damage bite. It flies, takes no fall damage, never despawns and turns its whole body to face what it looks at.
- **Boss fight:** while it fights a player, a purple boss bar (notched) shows to everyone within 48 blocks and **Tooth and Claw** plays.
- **Gaze phase (6 s, 4 s when enraged):** the central eye is open and casts an **anti-magic cone**: 20 blocks long, 30° either side of its gaze, with line of sight, shown by grey particles. The eye also stays open whenever it isn't fighting. Players in the cone (not creative or spectator) get **Anti-Magic** for 1.5 s, refreshed every 5 ticks. While it lasts:
  - The class power-up (Z) fizzles with an action-bar message and the mana is kept.
  - Warlock fireballs and the Monk double jump don't work.
  - Arrow Storm, Mob Repel, Reinforced Armor and Invulnerable are dispelled when it lands.
  - It moves in to about 2.5 blocks and bites once a second.
- **Ray phase (8 s, 10 s when enraged):** it shuts the central eye and hangs about 9 blocks back and 3 above. Every 1.5 s (0.9 s when enraged) an open eyestalk glows in its colour for 0.8 s, then fires. The ray is aimed where you were when the glow started, so moving dodges it. Rays reach 32 blocks, stop at walls and hit the first creature in the way:

  | Eye | Colour | Effect |
  |-|-|-|
  | Slowing | Blue | 2 magic damage and Slowness III for 6 s |
  | Telekinetic | Violet | Levitation II for 2.5 s |
  | Enervation | Red | 8 magic damage (10 when enraged) |
  | Fear | Yellow | **Frightened** for 8 s and Darkness for 4 s |

  **Frightened** halves your attack damage. While it lasts, the Beholder shoves you back (every 0.5 s) whenever you're within 7 blocks of it.
- **Eyestalks:** each stalk is its own hit shape (F3+B shows them as green boxes). A hit of 3 or more damage shuts that eye for 10 s: it shows an eyelid and can't fire.
- **Enrage below 50% health:** the bar turns red, every eye reopens, two different rays fire at once and the phases change length as noted above.
- **Rewards:** 2-4 eyes of ender and 4-9 amethyst shards (+ Looting). When a player kills it, also 120 XP, 3-6 diamonds, a 30% chance (+5% per Looting level) of a Protection IV book, a Feather Falling IV book or a totem of undying, and the **An Eye for an Eye** advancement (challenge, 150 XP).

## Where to find it
Beholders only live in **Beholder lairs**, which are rare (spacing 40 chunks, separation 16). A lair can generate under any Overworld biome except the deep dark, wherever its stair comes up on dry land.
- **The cavern:** a dome of radius 16 and height 12, floor somewhere between y=-38 and y=-19.
  - It sits in a two-block deepslate shell, so caves, water and lava can't break in.
  - The floor is a great eye of calcite and blackstone round a verdant froglight iris and an obsidian pupil.
  - The walls have crying obsidian, amethyst, dripstone and the odd shroomlight.
  - Five petrified adventurers (grey armour stands, arms raised) stand round the floor.
  - Against the west wall, a hoard of gold, bones and skulls surrounds a chest (`chests/beholder_lair`).
  - The Beholder hovers 5 blocks above the floor's centre.
- **The way in:** a tunnel east to a deepslate spiral-stair shaft that comes up 20 blocks east of the cavern's centre, inside a mossy ring of wall with a soul lantern.
- No other monsters spawn in the lair. Once its Beholder is dead, it now and then sends a new one (natural spawn: a 1-in-20 roll, out of the sky, and no other Beholder within 64 blocks). Silent Footsteps plays inside.

## Commands
- `/locate structure dndclasses:beholder_lair`
- `/summon dndclasses:beholder`, or the Beholder Spawn Egg in creative.

## Known limitations
- The model, texture and effect icons are placeholder art. The eyes don't glow: GeckoLib's glow layer rendered the irises black.
- On the server, eyestalk hit shapes and ray origins use the model's rest pose. On the client they follow the animation.
- The anti-magic cone only affects players.
- The restock rate is a rough guess.
- It hits hard and is deadly without armour.

## For developers
- **Entity:** `entity/BeholderEntity.java`.
  - Gaze and ray cycle, charges and the `Ray` enum.
  - Synced closed-eye bitmask and central-eye-shut flag.
  - A body control that does nothing, plus a vex-style `FloatMoveControl`.
  - Goals are `entity/ai/goal/BeholderAttackGoal.java` and `BeholderDriftGoal.java`.
- **Boss framework:** `BossFight` with `.phase(0.5F, RED, enrage)`, `.xp(120)` and `.advancement(dndclasses:beholder_slayer)`.
- **Hit shapes:** `MultipartDragon` with a `DragonPartLayout` of 4 `pair(...)` eyestalk groups.
  - New hook `MultipartDragon.damagePart(part, source, amount)`. `DragonPart.damage` now calls it; by default it just damages the owner. The Beholder overrides it to shut the stalk that was hit.
- **Effects:** `classes/Effects/AntiMagicEffect.java` (`isSuppressed` / `blocks` helpers) and `FrightenedEffect.java`, registered in `ModEffects`.
  - Checks are in `DnDClasses.sendPowerupPacket`, `Warlock.throwFireball` and `DoubleJumpMixin.canJump`.
- **Client:** `client/model/BeholderModel.java` (body pitch, eyelids) and `client/renderer/BeholderRenderer.java` (a `DragonRenderer`).
  - Assets: `geo/beholder.geo.json`, `animations/beholder.animation.json` and `textures/entity/beholder/beholder.png`.
- **Lair:** `world/gen/beholder/`, which contains `BeholderLairStructure`, `BeholderLairStructures`, `BeholderCavernPiece` and `BeholderShaftPiece`.
  - Data: `worldgen/structure/beholder_lair.json` and `worldgen/structure_set/beholder_lairs.json`.
  - Biome tag: `tags/worldgen/biome/has_structure/beholder_lair.json`.
  - Spawn rule: `ModSpawns.canBeholderSpawn`.
- **Loot and advancement:** `loot_tables/entities/beholder.json`, `loot_tables/chests/beholder_lair.json` and `advancements/beholder_slayer.json`.
- **Devscript:** `devscripts/beholder.txt` takes model and hitbox shots, then visits the dev-world lair at about (-504, 760) and fights in survival.
