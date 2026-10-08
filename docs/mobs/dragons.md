# Dragons
Fire-breathing dragons in the Overworld and the Nether: the common Wyvern, the Nether's Ember Wyvern, the mountain-lair Lightning Chaser and the small, tameable River Pikehorn.

## How it works
All dragons can be hit on their wings, neck, head, tail and legs (like the ender dragon), take no fall damage, and breathe fire. A breath starts with a growl and a rear back (12 ticks), then a 12-block cone of flame for 2 seconds. Each hit does 4 damage and sets you alight. The cone slowly swings after its target, so you can dodge it by moving sideways. Dragons can't breathe fire underwater.

| | Wyvern | Ember Wyvern | Lightning Chaser |
|-|-|-|-|
| **Health** | 40 | 26 | 200, 12 armor, 6 toughness, 80% knockback resistance |
| **Bite** | 6 | 7 | 14 |
| **Speed** (walk / fly) | 0.3 / 0.6 | 0.35 / 0.75 | 0.35 / 0.6 |
| **Attacks** | Hunts players on sight. Bites once a second up close, breathes fire every 3 s from further off | Bites twice a second, breathes fire every 2 s. Immune to fire and lava | Bites once a second. Every 3 s from range it takes turns breathing fire and calling down three lightning bolts round you. Immune to lightning |
| **Boss fight** | Red boss bar | None | Yellow boss bar |
| **Rewards** | Dragon Slayer | 1-3 magma cream, 0-2 blaze powder (+Looting). Killed by a player: 10 XP, 2-6 gold nuggets, 5% (+2%/Looting) netherite scrap | 2-5 phantom membranes, 1-3 copper blocks. Killed by a player: 80 XP, 2-5 diamonds, 15% (+5%/Looting) Staff of Lightning |

- In a fight with a player, a wild Wyvern or Lightning Chaser shows its boss bar to everyone within 64 blocks, and **Tooth and Claw** plays for them until the fight ends (see [Boss fights](../bosses/boss-fights.md)).
- The Ember Wyvern is a charred black and red Wyvern with molten wings and eyes that glow in the dark. It despawns like a monster.
- **River Pikehorn:** a small drake with 20 HP, a 2-damage bite and a 7-block flame doing 2 damage. Wild ones keep to themselves, but hit one and its group fights back.
  - **Taming:** feed it raw cod, salmon or tropical fish, with a 1 in 3 chance per fish (hearts on success, smoke on a miss).
  - A tamed Pikehorn follows you and sits or stands when you right-click it with an empty hand. It bites and breathes fire at whatever you attack or whatever attacks you, and never turns on you.
  - Each raw fish heals it 4 health (2 hearts). It never despawns.
- **Dragon Slayer** (challenge advancement, 100 XP): kill any dragon in the `#dndclasses:dragons` tag.

## Where to find it / How to get it
- **Wyvern:** plains, meadows, stony peaks and jagged peaks, on well-lit grass, stone, snow, ice or gravel (weight 10, groups of 1-3).
- **Ember Wyvern:** Nether Wastes, Crimson Forests, Basalt Deltas and Soul Sand Valleys, on solid ground in any light but never on the bedrock roof (monster, weight 4, groups of 1-2, not in peaceful). There's also a spawn egg in the mod's creative tab.
- **Lightning Chaser:** only in its [lair](../structures/dragon-lairs.md) on a mountain summit.
- **River Pikehorn:** rivers, swamps and mangrove swamps (weight 15, groups of 2-4).

## Commands
- `/summon dndclasses:wyvern`, `ember_wyvern`, `lightning_chaser`, `river_pikehorn`

## For developers
- Entities: `entity/WyvernEntity` (base), `EmberWyvernEntity`, `LightningChaserEntity`, `RiverPikehornEntity`.
- Breath: `FireBreath` + `ai/goal/FireBreathGoal`. Hit parts: see [Multipart mobs](../dev/multipart-mobs.md).
- To add a dragon, put it in `data/dndclasses/tags/entity_types/dragons.json`.
- The Ember Wyvern textures are recoloured from the green Wyvern texture by `tools/ember_wyvern_texture.py`; edit the script and rerun it rather than hand-editing them.
- Devscripts: `dragon-hitboxes.txt`, `dragon-fire-breath-all.txt`, `dragon-fall-damage.txt`, `dragon-slayer-all.txt`, `nether-dragon.txt`, `tameable-pikehorn.txt`.
