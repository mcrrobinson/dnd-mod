# Dragons
Four fire-breathing dragons: the common Wyvern of the plains and peaks, the Ember Wyvern of the Nether, the Lightning Chaser that nests on mountain summits, and the small River Pikehorn, which you can tame.

![A Wyvern](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-flesh-out/wyvern.png)

## How it works
Every dragon breathes fire. It growls and rears back for 12 ticks, then pours out a cone of flame for 2 seconds. Anything caught in the cone takes fire damage and is set alight for 5 seconds. The flame locks onto you during the wind-up, then only swings slowly after you, so running sideways gets you out of it. A dragon can't breathe fire with its mouth underwater. With `mobGriefing` on, the flame sometimes lights fires where it lands. Wild dragons never burn each other.

The model is much bigger than the entity's hitbox, so the wings, neck, head, tail and legs each have their own hit shape, like the ender dragon. You can hit a dragon anywhere on its body. None of them take fall damage, and none can be bred: wheat does nothing.

### Wyvern, Ember Wyvern and Lightning Chaser

| | Wyvern | Ember Wyvern | Lightning Chaser |
|-|-|-|-|
| Health | 40 | 26 | 200 |
| Armor | 0 | 0 | 12, plus 6 toughness |
| Knockback resistance | 0 | 0 | 80% |
| Bite | 6 | 7 | 14 |
| Speed (walk / fly) | 0.3 / 0.6 | 0.35 / 0.75 | 0.35 / 0.6 |
| Flame | 12 blocks, 4 damage | 12 blocks, 4 damage | 12 blocks, 4 damage |
| Time between bites | 1 s | 0.5 s | 1 s |
| Wait after a breath | 3 s | 2 s | 3 s |
| Immune to | fall damage | fall damage, fire, lava | fall damage, lightning |
| Boss bar | red, 64 blocks | none | yellow, 64 blocks |
| XP (killed by a player) | 20 | 10 | 80 |

All three hunt players on sight. They close in, bite when you're within 5 blocks, and breathe fire when you're 5 to 10 blocks away. They only start attacking once you've been in their sight for half a second.

The **Ember Wyvern** is a charred black and red Wyvern with molten wings and eyes that glow in the dark. It's a glass cannon: low health, but it bites twice as often and breathes more often. It walks through fire and lava without harm. It counts as a common monster, so it has no boss bar and despawns like one.

The **Lightning Chaser** takes turns at range: one fire breath, then a lightning storm, then a breath again. If you're beyond the flame's reach it calls the storm every time. A storm is three bolts, one on you and two that land 1.5 to 4.5 blocks away. Each bolt does 5 damage to everything within 3 blocks and sets it alight for 8 seconds. The bolts are the dragon's own attack: they scale with difficulty and the kill is credited to it. They don't charge creepers, turn mobs into witches or zombified piglins, or burn dropped items. They only set the ground alight when `mobGriefing` and `doFireTick` are both on.

![An Ember Wyvern](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-flesh-out/ember_wyvern.png)

![A Lightning Chaser](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-flesh-out/lightning_chaser.png)

### Drops

| Dragon | Always | Killed by a player |
|-|-|-|
| Wyvern | 1-3 leather, 0-2 phantom membranes | 2-5 gold nuggets |
| Ember Wyvern | 1-3 magma cream, 0-2 blaze powder | 2-6 gold nuggets; 5% chance of a netherite scrap (+2% per Looting level); 2% chance of a [Staff of Fire](../items/staffs.md) (+1% per Looting level) |
| Lightning Chaser | 2-5 phantom membranes, 1-3 copper blocks | 2-5 diamonds; 15% chance of a [Staff of Lightning](../items/staffs.md) (+5% per Looting level) |

Looting adds up to 1 more of each item per level, and up to 2 more phantom membranes from a Lightning Chaser. Copper blocks and gold nuggets don't go up with Looting.

### River Pikehorn
A small river drake with 20 health, 0.25 speed and a 2-damage bite. Its flame reaches 7 blocks and does 2 damage, and it waits 3 seconds between breaths. It breathes fire when its target is 3 to 5 blocks away and bites when it's closer.

Wild Pikehorns keep to themselves. Hit one and the whole group nearby turns on you.

To tame one, right-click it with raw cod, raw salmon or tropical fish. Each fish has a 1 in 3 chance: hearts mean it worked, smoke means try again. A newly tamed Pikehorn sits down. Right-click it with an empty hand to make it stand and follow you, and again to make it sit.

A tamed Pikehorn bites and breathes fire at whatever you attack and whatever attacks you. It never turns on you, even if you hit it, and it won't go after creepers, ghasts, your other pets, tamed horses or players you can't hurt. Feed it a raw fish to heal it 4 health (2 hearts). It never despawns.

It drops 0-2 leather and 0-2 cod (cooked if it died burning), each with up to 1 more per Looting level.

### Tamed dragons
You can't tame a Wyvern, Ember Wyvern or Lightning Chaser in survival, and the Bard's charm skips them. A tamed one (for example from `/summon` with an `Owner` tag) never hunts players. It only fights what its owner attacks or what attacks the owner, and never the owner, the owner's [party](../systems/party.md), the owner's pets, tamed horses, creepers, ghasts or players the owner can't hurt. Tamed dragons stay in peaceful and never show a boss bar.

### Boss fight
While a wild Wyvern or Lightning Chaser is fighting a player, its boss bar shows to everyone within 64 blocks and **Tooth and Claw** plays for them until the fight ends. See [Boss fights](../bosses/boss-fights.md).

### Dragon Slayer
Kill a Wyvern, Ember Wyvern or Lightning Chaser to earn **Dragon Slayer**, a challenge advancement worth 100 XP. The Pikehorn doesn't count. All four dragons take extra damage from [Smite Dragons](../enchantments/smite-dragons.md).

### Tips
- The 5 to 10 block band is where the flame reaches you. Inside 5 blocks the big dragons only bite.
- When the head rears back, step sideways. The flame starts where you stood and turns slowly.
- Fight from water: a dragon with its mouth under the surface can't breathe fire.
- Fire Resistance stops the breath and the burning after it. It doesn't stop the 5 damage from the Lightning Chaser's bolts.
- Against a Lightning Chaser, stay close. In melee range it only bites and never calls the storm.

## Where to find it

| Dragon | Where | Spawn rules |
|-|-|-|
| Wyvern | plains, meadows, stony peaks, jagged peaks | creature, weight 10, groups of 1-3; on grass, stone, snow, packed ice or gravel in daylight, like goats; not in peaceful |
| Ember Wyvern | Nether Wastes, Crimson Forest, Basalt Deltas, Soul Sand Valley | monster, weight 4, groups of 1-2; any solid ground in any light, never on the bedrock roof; not in peaceful |
| Lightning Chaser | only its [Dragon Lair](../structures/dragon-lairs.md) on a mountain summit | never within 64 blocks of another; after one dies, its lair waits 3 in-game days before sending another; not in peaceful |
| River Pikehorn | rivers, swamps, mangrove swamps | creature, weight 15, groups of 2-4; on grass in daylight, like other animals |

Wild Wyverns, Ember Wyverns and Lightning Chasers leave when the game is set to peaceful. Every dragon has a spawn egg in the mod's creative tab.

## Commands
- `/summon dndclasses:wyvern`
- `/summon dndclasses:ember_wyvern`
- `/summon dndclasses:lightning_chaser`
- `/summon dndclasses:river_pikehorn`

## Known limitations
- There's no survival way to tame the three big dragons.

## For developers
- Entities: `entity/WyvernEntity` (the base class; `EmberWyvernEntity` extends it), `LightningChaserEntity` and `RiverPikehornEntity`. Bite and breath timing come from `getMeleeCooldown()` and `getBreathCooldown()`.
- Fire breath: `entity/FireBreath` (wind-up 12 ticks, flame 40 ticks, 10° half-angle) and `ai/goal/FireBreathGoal` (used by the Pikehorn). The Wyvern and Lightning Chaser start their breath from `ai/goal/WyvernAttackGoal` and `LightningChaserAttackGoal`, which tick every tick (`shouldRunEveryTick`) and re-path every 4-10 ticks.
- Tamed dragon rules: `entity/TamedDragons`.
- Hit shapes: see [Multipart mobs](../dev/multipart-mobs.md).
- Wyvern and Lightning Chaser keep one `BirdNavigation` and `FlightMoveControl` on the ground and in the air, so a path survives every landing and take-off.
- Spawns: `world/gen/ModSpawns`. Lair respawn timer: `world/gen/lair/LairRespawns` (`RESPAWN_COOLDOWN`).
- Tags: `data/dndclasses/tags/entity_types/dragons.json` (Smite Dragons) and `dragon_slayer_targets.json` (Dragon Slayer). Add a new dragon to both.
- The Ember Wyvern textures are recoloured from the green Wyvern texture by `tools/ember_wyvern_texture.py`; edit the script and rerun it rather than hand-editing them.
- Devscripts: `dragon-hitboxes.txt`, `dragon-fire-breath-all.txt`, `dragon-fall-damage.txt`, `dragon-slayer-all.txt`, `nether-dragon.txt`, `tameable-pikehorn.txt`.
