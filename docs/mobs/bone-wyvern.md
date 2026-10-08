# Bone Wyvern
A small skeletal Wyvern that the Necromancer's Raise Dead calls up at rank V. It flies at your side, bites with a withering bite and breathes fire at the monsters around you.

## How it works
It's the [Wyvern](dragons.md) model drawn at 35% size with a bone texture, so it has the same flight, animations and fire breath. Its hit shapes (wings, neck, head, tail and legs) shrink with the model.

| | Bone Wyvern | Wyvern |
|-|-|-|
| Health | 30 | 40 |
| Bite | 5, plus Wither I for 3 s | 6 |
| Speed (walk / fly) | 0.35 / 0.75 | 0.3 / 0.6 |
| Flame | 8 blocks, 3 damage | 12 blocks, 4 damage |
| Bites within | 2.5 blocks | 5 blocks |
| Time between bites | 1 s | 1 s |
| Wait after a breath | 3 s | 3 s |
| Immune to | fall damage, fire, lava | fall damage |
| Boss bar | none | red, 64 blocks |
| XP and drops | none | 20 XP, leather, membranes, gold |

- **Owner:** it's tamed to the Necromancer who raised it. It follows them, fights whatever they attack or whatever attacks them, and on its own hunts any hostile mob it sees. It never targets its owner, their party, their pets or their other undead, and its flame doesn't burn them.
- **Undead:** it counts as undead, so Smite hurts it more and Instant Health harms it.
- **Lifetime:** it lasts as long as the rest of that Raise Dead (20 s at rank V). It's removed then, or when its chunk unloads or the server restarts, like the Necromancer's other summons.
- **One at a time:** casting Raise Dead again while it's alive doesn't raise a second.
- It's on the Necromancer's ally team, so Grave Pact buffs it and its kills give the Necromancer summon kill XP.

## Where to find it
Only from the [Necromancer](../classes/necromancer.md)'s Raise Dead at rank V. It doesn't spawn naturally and has no spawn egg.

## Commands
- `/summon dndclasses:bone_wyvern ~ ~ ~ {Owner:[I;...]}`: an untamed one (without `Owner`) hunts players like a wild Wyvern and isn't removed on a timer.

## Known limitations
- A summoned one without an owner never goes away on its own.

## For developers
- `entity/BoneWyvernEntity` extends `WyvernEntity` (`SCALE` 0.35); `client/renderer/BoneWyvernRenderer` draws the Wyvern model `withScale(SCALE)` and texture `textures/entity/wyvern/bone.png`.
- Parts: `DragonPartLayout.scaled(scale)` copies the Wyvern's layout for the server parts; the client parts follow the scaled bones through `DragonRenderer` with no change. Check with the DevScript `serverhitboxes measure` step (`devscripts/necromancer-ranks.txt`).
- Spawned by `PowerUpEffect.spawnUndead` with `setOwner` and `SkillHelpers.spawnSummon`.
