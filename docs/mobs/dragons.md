# Dragons
Fire-breathing dragons in the Overworld and the Nether: the common Wyvern, the Nether's Ember Wyvern, the mountain-lair Lightning Chaser and the small, tameable River Pikehorn.

## How it works
All dragons can be hit on their wings, neck, head, tail and legs (like the ender dragon), take no fall damage, and breathe fire. A breath starts with a growl and a rear back (12 ticks), then a 12-block cone of flame for 2 seconds. Each hit does 4 damage and sets you alight. The cone slowly swings after its target, so you can dodge it by moving sideways. Dragons can't breathe fire underwater.

| | Wyvern | Ember Wyvern | Lightning Chaser |
|-|-|-|-|
| **Health** | 40 | 26 | 200, 12 armor, 6 toughness, 80% knockback resistance |
| **Bite** | 6 | 7 | 14 |
| **Speed** (walk / fly) | 0.3 / 0.6 | 0.35 / 0.75 | 0.35 / 0.6 |
| **Attacks** | Hunts players on sight. Bites once a second up close, breathes fire from further off, 3 s after the last breath ended | Bites twice a second, breathes fire 2 s after the last breath. Immune to fire and lava | Bites once a second. Every 3 s from range it takes turns breathing fire and calling down three lightning bolts round you (5 damage each and sets you alight). Immune to lightning |
| **Boss fight** | Red boss bar | None | Yellow boss bar |
| **Rewards** | 1-3 leather, 0-2 phantom membranes (+Looting). Killed by a player: 20 XP, 2-5 gold nuggets | 1-3 magma cream, 0-2 blaze powder (+Looting). Killed by a player: 10 XP, 2-6 gold nuggets, 5% (+2%/Looting) netherite scrap | 2-5 phantom membranes, 1-3 copper blocks. Killed by a player: 80 XP, 2-5 diamonds, 15% (+5%/Looting) Staff of Lightning |

- Wyverns and Lightning Chasers only attack once their target has been in sight for half a second (10 ticks).
- **Lightning:** the Lightning Chaser's bolts are its own attack: the kill is credited to it, the damage scales with difficulty, and they don't charge creepers, turn mobs into witches or zombified piglins, or burn dropped items. They only set the ground alight when `mobGriefing` and `doFireTick` are on.
- **Peaceful:** wild Wyverns and Lightning Chasers leave in peaceful, like monsters, and don't spawn there. Tamed ones stay.
- **Tamed dragons** (a Bard's power-up can tame them) never hunt players. They only fight what their owner attacks or what attacks the owner, and never the owner's party, pets, tamed horses or players the owner can't hurt.
- None of them can be bred: wheat does nothing.
- In a fight with a player, a wild Wyvern or Lightning Chaser shows its boss bar to everyone within 64 blocks, and **Tooth and Claw** plays for them until the fight ends (see [Boss fights](../bosses/boss-fights.md)).
- The Ember Wyvern is a charred black and red Wyvern with molten wings and eyes that glow in the dark. It despawns like a monster.
- **River Pikehorn:** a small drake with 20 HP, a 2-damage bite and a 7-block flame doing 2 damage (3 s between breaths). Wild ones keep to themselves, but hit one and its group fights back. Drops 0-2 leather and 0-2 cod (cooked if it died burning), each +Looting.
  - **Taming:** feed it raw cod, salmon or tropical fish, with a 1 in 3 chance per fish (hearts on success, smoke on a miss).
  - A tamed Pikehorn follows you and sits or stands when you right-click it with an empty hand. It bites and breathes fire at whatever you attack or whatever attacks you, and never turns on you.
  - Each raw fish heals it 4 health (2 hearts). It never despawns.
- **Dragon Slayer** (challenge advancement, 100 XP): kill a Wyvern, Ember Wyvern or Lightning Chaser (`#dndclasses:dragon_slayer_targets`; the small Pikehorn doesn't count).

## Where to find it / How to get it
- **Wyvern:** plains, meadows, stony peaks and jagged peaks, on well-lit grass, stone, snow, ice or gravel (weight 10, groups of 1-3).
- **Ember Wyvern:** Nether Wastes, Crimson Forests, Basalt Deltas and Soul Sand Valleys, on solid ground in any light but never on the bedrock roof (monster, weight 4, groups of 1-2, not in peaceful).
- **Lightning Chaser:** only in its [lair](../structures/dragon-lairs.md) on a mountain summit. Once killed, its lair waits 3 in-game days before it sends another.
- Every dragon has a spawn egg in the mod's creative tab.
- **River Pikehorn:** rivers, swamps and mangrove swamps (weight 15, groups of 2-4).

## Commands
- `/summon dndclasses:wyvern`, `ember_wyvern`, `lightning_chaser`, `river_pikehorn`

## For developers
- Entities: `entity/WyvernEntity` (base), `EmberWyvernEntity`, `LightningChaserEntity`, `RiverPikehornEntity`.
- Breath: `FireBreath` + `ai/goal/FireBreathGoal`. Hit parts: see [Multipart mobs](../dev/multipart-mobs.md).
- To add a dragon, put it in `data/dndclasses/tags/entity_types/dragons.json` (Smite Dragons), and in `dragon_slayer_targets.json` if killing it should grant Dragon Slayer.
- Wyvern and Lightning Chaser keep one `BirdNavigation` and `FlightMoveControl` on the ground and in the air. Their attack goals tick every tick (`shouldRunEveryTick`) and re-path every 4-10 ticks. Lair respawn timers: `world/gen/lair/LairRespawns`.
- The Ember Wyvern textures are recoloured from the green Wyvern texture by `tools/ember_wyvern_texture.py`; edit the script and rerun it rather than hand-editing them.
- Devscripts: `dragon-hitboxes.txt`, `dragon-fire-breath-all.txt`, `dragon-fall-damage.txt`, `dragon-slayer-all.txt`, `nether-dragon.txt`, `tameable-pikehorn.txt`.
