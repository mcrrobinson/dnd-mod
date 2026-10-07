# Dungeons and Dragons Mod

## Table of Contents
- [Building](#building)
- [Features](#features)
  - [Player Classes Overview](#player-classes-overview)
  - [Armor](#armor)
  - [Enchantments (made by an Artificer)](#enchantments-made-by-a-artificer)
  - [Music](#music)
  - [Mobs](#mobs)
    - [Goblins](#goblins)
    - [Dragons](#dragons)
    - [Hobbits](#hobbits)
  - [Hobbit Villages](#hobbit-villages)

## Building
Navigate to the root directory of the project and type the following...

1. `gradlew genSources`
2. `gradlew vscode`
3. `gradlew runClient`

That will build the latest edition of Minecraft I have got working with the mod.

### Linux (Debian/Ubuntu)
Install JDK 21, then use the `./gradlew` wrapper:

```sh
sudo apt install openjdk-21-jdk
./gradlew genSources
./gradlew runClient
```

## Features

### Player Classes Overview

Each class in the game comes with its own unique strengths, weaknesses, and special abilities.

| **Class**| **Pros**|**Cons**|**Special Ability**|
|-|-|-|-|
| **Barbarian** | Strength highly buffed, Health rivals dragons | Limited vision (fog closes in at ~24 blocks), Moves very slowly | Inspired by *One Punch Man* |
| **Bard** | Invisible to mobs | Less health | Instantly tame tameable animals |
| **Cleric** | High mining speed, Night vision | Shorter viewing distance (fog closes in at ~48 blocks), Slightly reduced attack damage | Circle of ignoring mobs |
| **Druid** | Gains an extra heart per tamed animal (up to 5), Regenerates in light (level 10+) | Cannot swim, Gets hungry in the dark (light 4 or less) | Transforms into a random animal it has killed for 30s |
| **Fighter** | High health, High strength, Attracts mobs: hostile mobs prefer a Fighter over other players (done) | Cannot use bows or crossbows (done), No potions: can't use potion items and potion buffs don't apply, harmful potions still do (done) | Super regeneration: Regeneration V for 10s (done) |
| **Monk** | Increased mobility, Increased attack speed | Reduced damage output (75% unarmored, less the more armor you wear), Can only attack with a staff or bare fists | Can triple jump, Unrivaled attack speed |
| **Paladin** | High health, circle of healing, Unaffected by potions, good or bad (ability effects still apply) (done) | Cannot craft anything (crafting, stonecutter, smithing table, loom) or brew potions (done), Very weak in the Nether: half damage and armor, 20% slower (done) | Instantly heals everyone within 10 blocks to full (done) |
| **Ranger** | Can zoom in with bow, faster firing (done) | Cannot pick up swords (done), Weak to fire (done) | Hold right click to spam fire (no ammo consumed) (done) |
| **Rogue** | No poison damage (done), No need to eat (to be tested) | Low health (done) | Temporary invisibility (done) |
| **Necromancer** | Wither debuff on melee enemies (done), Undead do not attack (done) | Slightly less health (done), Significantly less damage (done) | Can spawn allied undead to attack enemies (done) |
| **Warlock** | Can throw fireballs with an empty hand (done), Immune to fire and lava (done) | Reduced damage output (done), Hurt by water and rain, 1 damage every 4s but never below 1 heart (done) | Can breathe fire by holding a special key (done) |
| **Wizard** | Can wield elemental staffs (done) | Greatly reduced health (done) | Creates a massive explosion and becomes invulnerable for a few seconds (done) |
| **Artificer** | Increased movement speed (done), 25% chance for crafted tools, weapons and armor to come out enchanted (done) | Deals 25% less damage (done), Unaffected by potions except abilities: can't drink potions, immune to splash/lingering/tipped-arrow effects (done) | Temporarily buffs all armor: +8 armor, +4 toughness for 30 seconds (done) |
| **Blood Hunter**| Fire aspect applied to all swords, Double damage at night (done) | Half damage during the day (done) | Can take control of any mob within 30m |
| **Alchemist** | Can craft special potions exclusive to the class (done), Brewing stands don't explode: a vanilla brewing stand explodes when its brew finishes if the last player to use it wasn't an Alchemist (stands nobody has used, e.g. hopper-fed, are safe) (done) | Cannot enchant (enchanting table or enchanted books on an anvil) (done) | Instantly buff all potions to max level (done) |

### Armor
There is armor for each class, however the armor benefits are increased depending on whether or not you match the class and wearing a full set.

### Enchantments (made by a Artificer)

| Enchantment | Description | Class |
|-|-|-|
| **Lunge** | Press right click on the item to lunge forward | Attack Weapons |
| **Invulnerability** | The player will be invulnerable for a short duration | Attack Weapons |
| **Tree Feller** | Can dismantle trees | Axe |
| **Grid Miner** | Can mine blocks in a grid pattern | Pickaxe & Shovel |

### Music

Custom music plays during different events in the game. There are **five** music tracks, each also available as a music disc:

| Track | When it plays |
|-------|---------------|
| **Tooth and Claw** | Straight away when a dragon fight starts, looping until it ends |
| **Silent Footsteps** | In dungeons (near a mob spawner, or inside a stronghold, mineshaft, ancient city, fortress, bastion, mansion, ocean monument or temple) and at night, mixed with the vanilla music |
| **Awake Cart** | While travelling (about 80 blocks covered in 30 seconds), mixed with the vanilla music |
| **Steel on Steel** | Music disc only |
| **Music Box** | Music disc only |

### Mobs

#### Goblins
- **Goblin Warrior**: A goblin warrior is a strong and tough mob that has a low attack speed but high health. It can be found near the entrance of the Nether Fortress.

#### Dragons 
- **Dragon**: A dragon is a large, green, and powerful creature that can be found in the Nether. It has a high attack speed and low health.

#### Hobbits
- **Hobbit**: A small, peaceful halfling that lives in hobbit villages. Hobbits wander their village by day, head home at night, keep away from monsters and are always nibbling something. Right-click one with an empty hand and it shares some of its food (once every few minutes).

### Hobbit Villages
Cozy, Shire-style villages that generate in plains, sunflower plains and meadows, laid out differently every time:
- **Smials** (hobbit holes) dug into grassy hills, with round green (or red, or yellow) doors, round windows, chimneys and fenced front gardens. Inside: a fireplace, a kitchen, a table laid with food, and in the bigger ones a pantry stacked with barrels and a bedroom.
- **The Green Dragon** inn with a thatched roof, a bar backed by ale casks, and tables of food.
- **The party green** round the party tree, with long tables of cakes and plates, a striped pavilion, ale and a bonfire.
- **Gardens, orchards, market stalls and ponds**, joined by winding lanes with lamp posts.

Barrels and chests are full of food (`chests/hobbit_pantry`, `hobbit_larder`, `hobbit_harvest`, `hobbit_ale`), and the plates and hams in item frames can be taken. Find one with `/locate structure dndclasses:hobbit_village`.
