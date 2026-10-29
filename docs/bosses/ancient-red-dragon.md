# Ancient Red Dragon
The endgame boss: a named, centuries-old red wyrm with a 46-block wingspan. It breathes a 28-block cone of fire, terrifies everyone who sees it, shrugs off three failed saves a day, takes extra turns between its attacks (legendary actions) and turns its own lair against you with magma vents, tremors and volcanic gas. Kill it for **Red Dragon Scale Mail**, a hoard of scales and the **Wyrmslayer** challenge.

![The Ancient Red Dragon next to a player wearing its scale mail (left) and a Lightning Chaser (right)](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/feat-ancient-red-dragon/ancient-red-scale.png)

## How it works
**Stats**: 700 health, 14 armour, 8 toughness, full knockback resistance, walk speed 0.32, flying speed 0.65, follow range 96, 500 XP. Immune to fire. It never despawns. Its entity box is 4x4 blocks, but the model is drawn at 2.5x the Lightning Chaser's thickened skeleton (about 46 blocks wingtip to wingtip, its head about 9-10 blocks up while it flies) and has 23 hit shapes on its wings, neck, head, tail and legs, so you can hit it anywhere on its body (see [Multipart mobs](../dev/multipart-mobs.md)). It's named once, when it spawns: "Kharzanthrax", "Vyrmorrath" and so on, shown on its boss bar as "*Name*, Ancient Red Dragon".

It leaves players alone until one it can see comes within **40 blocks**, then hunts them within 96. It flies in circles 24 blocks out and 16 up, dives to 14 out and 9 up when its breath is ready, and after 20 s in the air (or as soon as you hide under a roof) lands beside you and fights on the ground for 10 s (15 s from its second phase) before taking off again.

| Attack | When | Effect |
|-|-|-|
| **Bite** | within 10 blocks, every 1.5 s | 20 damage |
| **Fire Breath** | breath charged, target within 24 blocks and in sight | 1 s wind-up (it inhales, smoke and embers at its jaws), then a 28-block cone (15 degrees) of flame for 2.5 s. **DEX save DC 18**, once per breath: 7 fire damage per hit and alight 8 s; half damage and alight 3 s on a success |
| **Breath recharge** | every 3 s while spent | Rolls a d6: recharges on **5-6** (**4-6** in Molten Fury). Every fight starts with it charged |
| **Frightful Presence** | the fight's start, every 30 s, entering Molten Fury | Every player within 48 blocks who can see it makes a **WIS save DC 17** or is [Frightened](../systems/conditions.md) of it for 10 s. A success makes you immune for the rest of the fight |

**Legendary actions.** It has 3 points, refilled every 6 s, and spends them between its own attacks at most once a second:

| Action | Cost | When | Effect |
|-|-|-|-|
| **Tail Attack** | 1 | a player behind it (outside its 120-degree front arc) within 14 blocks | 12 damage and a hard knockback |
| **Wing Attack** | 2 | two or more players within 15 blocks, one within 8, or it has just landed | Everything within 15 blocks makes a **DEX save DC 18**: 10 damage, thrown back hard and Prone for 2 s; half damage and a lighter push on a success. Then it beats itself 8 blocks into the air |
| **Detect** | 1 | its target is out of sight | It turns to face them and they glow for 3 s |

**Legendary Resistance (3 a day).** When it fails a save (a class power, a spell, Smite Dragons, anything that rolls a save for it), it can choose to succeed instead: the save lane shows the real roll and "Legendary Resistance", and everyone within 48 blocks sees "*Name* shrugs it off. (Legendary Resistance, 2 left)" on the action bar. It doesn't spend one on saves that only halve damage. The charges come back at dawn, or after a minute with no fight. Studying it (V) says so: "It shrugs off only three spells a day. Make it spend them".

**Lair actions.** While the fight is on and the dragon is within 64 blocks of its lair, every 20 s (14 s in its second phase, 10 s in its last) the lair acts, never the same way twice in a row. Their damage is credited to the dragon.

| Lair action | Effect |
|-|-|
| **Magma Vents** | Under up to 4 players in the lair: 1.5 s of warning (smoke, dripping lava, a glowing ring and a rumble), then a vent erupts 2.5 blocks wide and 6 high. **DEX save DC 15**: 10 damage and alight 5 s; half and 2 s on a success. With `mobGriefing` on, the stone at its centre turns to magma |
| **Tremor** | The lair shakes for 2 s and rocks fall around everyone on the ground. **DEX save DC 15**: 4 damage and Prone for 3 s on a failure, nothing on a success |
| **Volcanic Gas** | 3 clouds 4 blocks wide, for 8 s, near random players. **CON save DC 13** on walking in (once per cloud): Poisoned for 6 s on a failure |

**Phases** (red boss bar, notched, visible within 96 blocks; its own fight music, the Ancient theme):
- **Start** (red): "Thieves. Ash. It is all the same to me." and a Frightful Presence.
- **Below 60%, The Mountain Answers** (purple): "Burn, little things. The mountain itself hates you." Lair actions come faster and every kobold within 64 blocks turns on its foe with Strength I for a minute (the kobolds arrive with the Kobold Tribes; until then nothing answers).
- **Below 30%, Molten Fury** (yellow): "ENOUGH!" A new Frightful Presence, +15% walk and flying speed, its breath recharges on 4-6 and lair actions come every 10 s.
- **Death**: "The hoard... is... mine..." It roars and crashes down over 3 s in fire, smoke and dust.

**Its lair.** Drawn more than 96 blocks from its lair it drops the chase and flies back over it, then takes up the fight again. Unlike the smaller dragons it never respawns at its lair: once it's dead, it's gone. A dragon with no lair (a spawn egg or `/summon`) has no lair actions and never flies home.

**Saving throws** show on the [save lane](../systems/saving-throws.md); every one of them counts as a save against a dragon, so [Red Dragon Scale Mail](../items/magic-items.md#red-dragon-scale-mail) gives advantage on them.

**Rewards** (`entities/ancient_red_dragon`, when a player kills it): 500 XP, **[Red Dragon Scale Mail](../items/magic-items.md#red-dragon-scale-mail)** (always, unidentified), 8-16 **Ancient Red Scales** (+1 per Looting level), 4-8 diamonds, 2-4 netherite scrap, 1-3 gold blocks, a guaranteed roll on the [Very Rare magic pool](../systems/magic-loot.md) (60%, else Legendary), and the **Wyrmslayer** challenge (after Dragon Slayer). It counts for Dragon Slayer too and takes extra damage from [Smite Dragons](../enchantments/smite-dragons.md).

**Ancient Red Scales**: a [Dragonborn](../mobs/dragonborn.md) trader buys one for 8 emeralds; brewing one into an Awkward Potion makes a long Fire Resistance potion; they repair the scale mail.

![Ignrakion rears over a player who just failed the Frightful Presence save](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/feat-ancient-red-dragon/ancient-red-fight-a.png)

![A wing attack: the DEX save fails and the player is thrown back, Prone](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/feat-ancient-red-dragon/ancient-red-fight-b.png)

## Where to find it
- In its lair, the volcanic caldera of the [Ancient dragon lair](../structures/dragon-lairs.md) (coming with that ticket).
- `/summon dndclasses:ancient_red_dragon` or its spawn egg (D&D Classes creative tab) for testing. Give it a lair with NBT: `{Lair:{X:0,Y:100,Z:0}}`.

## Commands
None of its own. `/dndclass forceroll <dragon> 1 1 1` rigs its next saves, so you can watch Legendary Resistance; `/dm save <dragon> con 15` makes it roll one.

## Known limitations
- The lair structure, its hoard and its markers come with the lair ticket; until then an Ancient only has a lair if you give it one by NBT, and `AncientDragonEvents.SLAIN` has no listener.
- Kobolds only answer its call once the Kobold Tribes add mobs to `#dndclasses:faction/kobolds`.
- Its flight is a steered circle, not pathfinding: in tight caves it can scrape along walls until it lands.
- The fire-breath head pose and the wing-attack beat aren't followed by the server's hit shapes (as for every dragon); only the body animation is.
- Tick cost: a 25 s profile of the scripted fight held a steady 20 TPS. Its 23 parts are posed once per animation frame and shared, and its lair checks are once a tick.

## For developers
- `entity/dragon/AncientDragonEntity` (shared by every chromatic colour: size, Frightful Presence, breath recharge, phases, lair actions, going home), `AncientRedDragonEntity`, `ChromaticColor` (bar colour, name syllables, lair actions, lair structure id), `AncientFireBreath`, `LegendaryActions`, `AncientLairActions` / `LairAction`, `AncientSaves`, `AncientDragonNames`, `AncientDragonAttackGoal`.
- `entity/boss/LegendaryCreature` and `LegendaryResistance`: any mob can implement it; the hook turns a fresh failed save into a success through `SaveResult.overruled(...)` and logs `[LegendaryResistance]`.
- `AncientDragonEvents.SLAIN` (lair, `null` without one) and `TREMOR` are the lair ticket's hooks.
- `FireBreath` gained `coneTan()`, `windupTicks()`, `flameTicks()`, `playWindupSound()` and `spawnWindupParticles()` for bigger breaths; `LairDragonEntity.respawnsAtLair()` lets the Ancients opt out of respawning.
- Model and art are generated: `tools/ancient_dragon_model.py` (geo + animation from the Lightning Chaser), `tools/ancient_dragon_texture.py` (texture + glowmask), `tools/boss_item_textures.py` (scale mail and scale), `tools/music-gen/music_gen.py` (`ancient_fight`).
- `devscripts/ancient-red-scale.txt` (size check next to a player and a Lightning Chaser, client vs server hit shapes) and `devscripts/ancient-red.txt` (the fight: Frightful Presence, Legendary Resistance, phases, the kill and its drops). Its special actions log `[MobAction]` to `debug.log`.
