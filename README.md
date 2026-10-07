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
    - [Mountain Dwarves](#mountain-dwarves)
    - [Beholder](#beholder)
  - [Dragon Lairs](#dragon-lairs)
  - [Beholder Lairs](#beholder-lairs)
  - [Hobbit Villages](#hobbit-villages)
  - [Dwarven Fortresses](#dwarven-fortresses)

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
| **Rogue** | No poison damage (done), No need to eat: food never drains and no starvation, but no natural regen from food either (done) | Low health (done) | Temporary invisibility (done) |
| **Necromancer** | Wither debuff on melee enemies (done), Undead do not attack (done) | Slightly less health (done), Significantly less damage (done) | Can spawn allied undead to attack enemies (done) |
| **Warlock** | Can throw fireballs with an empty hand (done), Immune to fire and lava (done) | Reduced damage output (done), Hurt by water and rain, 1 damage every 4s but never below 1 heart (done) | Can breathe fire by holding a special key (done) |
| **Wizard** | Can wield elemental staffs (done) | Greatly reduced health (done) | Creates a massive explosion and becomes invulnerable for a few seconds (done) |
| **Artificer** | Increased movement speed (done), 25% chance for crafted tools, weapons and armor to come out enchanted (done) | Deals 25% less damage (done), Unaffected by potions except abilities: can't drink potions, immune to splash/lingering/tipped-arrow effects (done) | Temporarily buffs all armor: +8 armor, +4 toughness for 30 seconds (done) |
| **Blood Hunter**| Fire aspect applied to all swords (done), Double damage at night (done) | Half damage during the day (done) | Can take control of any mob within 30m for 20 seconds (needs the Identity mod) (done) |
| **Alchemist** | Can craft special potions exclusive to the class (done), Brewing stands don't explode: a vanilla brewing stand explodes when its brew finishes if the last player to use it wasn't an Alchemist (stands nobody has used, e.g. hopper-fed, are safe) (done) | Cannot enchant (enchanting table or enchanted books on an anvil) (done) | Instantly buff all potions to max level (done) |

### Armor
There is armor for each class, however the armor benefits are increased depending on whether or not you match the class and wearing a full set.

Wearing a full set (all four pieces) gives its set bonus to anyone. If your class is one the set is made for, the bonus is boosted: every set effect goes up one level, and you also get the set's class effect. (done)

| Set | Set bonus (anyone) | Made for | Boosted bonus (matching class) |
|-|-|-|-|
| **Thief** | Jump Boost I | Rogue | Jump Boost II + Speed I |
| **Assassin** | Jump Boost I | Ranger | Jump Boost II + Night Vision |
| **Wizard** | Jump Boost I | Wizard | Jump Boost II + Fire Resistance |
| **Cleric** | Jump Boost I | Cleric | Jump Boost II + Resistance I |
| **Blood Hunter** | Jump Boost I | Blood Hunter | Jump Boost II + Strength I |
| **Golden Horns** | Jump Boost I | Barbarian, Bard | Jump Boost II + Strength I |
| **Holy Armor** | Jump Boost I | Paladin | Jump Boost II + Fire Resistance |
| **Knight** | Jump Boost I | Fighter | Jump Boost II + Resistance I |
| **Warrior** | Jump Boost I | Barbarian, Fighter | Jump Boost II + Haste I |
| **Prismarine** | Water Breathing | Druid | Water Breathing + Dolphin's Grace |
| **Wooden** | Jump Boost I | Druid | Jump Boost II + Haste I |
| **Robe** | Jump Boost I | Monk, Alchemist | Jump Boost II + Speed I |
| **Steampunk** | Jump Boost I | Artificer | Jump Boost II + Haste I |
| **Wither** | Jump Boost I | Warlock, Necromancer | Jump Boost II + Strength I |

Set bonuses can be turned off with `applyArmorEffects` in `config/dndclasses/dndclasses.json`.

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
| **Tooth and Claw** | Straight away when a dragon (or Beholder) fight starts, looping until it ends |
| **Silent Footsteps** | In dungeons (near a mob spawner, or inside a stronghold, mineshaft, ancient city, fortress, bastion, mansion, ocean monument or temple) and at night, mixed with the vanilla music |
| **Awake Cart** | While travelling (about 80 blocks covered in 30 seconds), mixed with the vanilla music |
| **Steel on Steel** | Music disc only |
| **Music Box** | Music disc only |

### Mobs

#### Goblins
- **Goblin Warrior**: A goblin warrior is a strong and tough mob that has a low attack speed but high health. It can be found near the entrance of the Nether Fortress.

#### Dragons
Dragons live in the Overworld and the Nether. Their wings, neck, head, tail and legs can all be hit (like the ender dragon's), they take no fall damage, and they breathe fire: after a growl and a rear back, a 12-block cone of flame for 2 seconds (4 damage a hit and sets you alight) that slowly swings after its target, so you can dodge it by moving sideways. They can't breathe fire underwater.

| | Wyvern | Ember Wyvern | Lightning Chaser |
|-|-|-|-|
| **Where** | Plains, meadows, stony peaks and jagged peaks, on well-lit ground (weight 10, groups of 1-3) | The Nether: Nether Wastes, Crimson Forests, Basalt Deltas and Soul Sand Valleys, on solid ground in any light but never on the bedrock roof (monster, weight 4, groups of 1-2; not in peaceful, and it despawns like a monster) | Only in its lair on a mountain summit (see [Dragon Lairs](#dragon-lairs)) |
| **Health** | 40 | 26 (a glass cannon) | 200, with 12 armour, 6 toughness and 80% knockback resistance |
| **Bite** | 6 damage | 7 damage | 14 damage |
| **Speed** | 0.3 walking, 0.6 flying | 0.35 walking, 0.75 flying | 0.35 walking, 0.6 flying |
| **Attacks** | Hunts players on sight. Bites up close (once a second), breathes fire from further off (every 3 seconds) | Hunts players on sight. Bites twice a second up close, breathes fire from further off (every 2 seconds). Immune to fire and lava | Hunts players on sight. Bites up close (once a second); from further off, every 3 seconds, it takes turns breathing fire and calling down a storm of three lightning bolts round you. Immune to lightning |
| **Boss fight** | Red boss bar | None (no bar or music) | Yellow boss bar |
| **Rewards** | **Dragon Slayer** | 1-3 magma cream and 0-2 blaze powder (+ Looting); when a player kills it, also 10 XP, 2-6 gold nuggets, a 5% chance (+2% per Looting level) of a netherite scrap, and **Dragon Slayer** | 2-5 phantom membranes and 1-3 copper blocks; when a player kills it, also 80 XP, 2-5 diamonds, a 15% chance (+5% per Looting level) of a **Staff of Lightning**, and **Dragon Slayer** |

In a fight with a player, a wild Wyvern or Lightning Chaser shows its boss bar to everyone within 64 blocks and **Tooth and Claw** plays for them until the fight ends. The Ember Wyvern is the Wyvern charred black and red, with molten wings and eyes that glow in the dark.

Killing any dragon (Wyvern, Ember Wyvern, Lightning Chaser or River Pikehorn) earns the **Dragon Slayer** advancement (challenge, 100 XP). Every dragon is in the `#dndclasses:dragons` entity type tag, which the advancement checks, so a new dragon only needs adding there.

- **River Pikehorn**: a small fire-breathing drake (20 HP, 2 damage bite, a 7-block flame doing 2 damage) in rivers, swamps and mangrove swamps (weight 15, groups of 2-4). Wild ones keep to themselves, but hit one and its group fights back. Feed one raw fish (cod, salmon or tropical fish) to tame it: a 1 in 3 chance per fish, with hearts on success and smoke on a miss. A tamed Pikehorn follows you, sits or stands when you right-click it with an empty hand, bites and breathes fire at whatever you attack or whatever attacks you, never turns on you, heals 2 hearts per raw fish and never despawns.

### Dragon Lairs
A Lightning Chaser's lair sits on the very summit of a jagged, frozen or stony peak (the highest point around, with the mountain falling away on every side): a ring of standing stones crowned with lightning rods, round a nest of logs and bones heaped with gold and a hoard chest (`chests/dragon_lair`: gold, copper, emeralds, diamonds, enchanted diamond gear, and sometimes an enchanted golden apple, a trident or a Staff of Lightning). Each lair starts with one Lightning Chaser (sometimes two). It never strays more than about 24 blocks before circling back, and once it's dead the lair sends a new one now and then. Find one with `/locate structure dndclasses:dragon_lair`.

#### Hobbits
- **Hobbit**: A small, peaceful halfling that lives in hobbit villages. Hobbits wander their village by day, head home at night, keep away from monsters and are always nibbling something. Right-click one with an empty hand and it shares some of its food (once every few minutes).

#### Mountain Dwarves
- **Mountain Dwarf**: A short, broad, bearded dwarf in a mail shirt, often with an iron, gold or chainmail helmet, carrying an axe or pickaxe. Neutral, like an iron golem: it leaves players alone and hunts monsters (except creepers) near its home, but hit one and it and its kin nearby fight back for a while. Right-click one with a gold ingot to trade for ores, gems or tools from the deep (`gameplay/dwarf_barter`). Dwarves guard their fortress like piglins guard gold: open a chest or barrel, or break a gold block, where one can see you and they all turn on you.

#### Beholder
A floating eye tyrant (250 health, 8 armour, 80% knockback resistance) that only lives in its lair deep underground (see [Beholder Lairs](#beholder-lairs)). It flies, takes no fall damage and turns its whole body to face what it looks at. Fighting a player, it shows a purple boss bar to everyone within 48 blocks and **Tooth and Claw** plays. The fight alternates between two modes:

- **Gaze** (6 seconds): its great central eye is open and casts an **anti-magic cone** (20 blocks, 30° either side of where it looks, shown by grey motes). Anyone standing in it gets **Anti-Magic** for 1.5 seconds: class specials don't work (the power-up fizzles and keeps your mana, no Warlock fireballs, no Monk double jump), and the magical buffs class powers give (Arrow Storm, Mob Repel, Reinforced Armor, Invulnerable) are dispelled. Meanwhile it closes in and bites (10 damage). Its eye stays open, and the cone up, whenever it isn't fighting.
- **Eye rays** (8 seconds): it shuts the central eye (the cone would smother its own magic too), hangs back about 9 blocks off and fires eye rays from its eight eyestalks, two of each kind. An eye glows in its colour for 0.8 seconds first, and the ray goes where you were standing when it started glowing, so keep moving. Rays stop at walls and hit the first creature in the way:

| Eye | Colour | Effect |
|-|-|-|
| Slowing | Blue | 2 magic damage and Slowness III for 6 seconds |
| Telekinetic | Violet | Levitation II for 2.5 seconds (and the fall after) |
| Enervation | Red | 8 magic damage (10 once it's enraged) |
| Fear | Yellow | **Frightened** for 8 seconds (your attacks do half damage, and it keeps driving you back when you come within 7 blocks) plus Darkness for 4 |

Every eyestalk is its own hit shape (like a dragon's wings). Hit one for 3 or more damage and that eye shuts for 10 seconds, so that ray can't be used. Below half health it **enrages** (red bar): every eye opens again, it fires two rays at a time and more often, and the eye-ray phase lasts longer than the gaze. Drops 2-4 eyes of ender and 4-9 amethyst shards (+ Looting); when a player kills it, also 120 XP, 3-6 diamonds, a 30% chance (+5% per Looting level) of a Protection IV or Feather Falling IV book or a totem of undying, and the **An Eye for an Eye** advancement (challenge, 150 XP).

### Beholder Lairs
A Beholder's lair is a great domed cavern carved into the deepslate (floor somewhere between y=-38 and y=-19) under any Overworld biome except the deep dark, rarer than dragon lairs (about one per 40x40 chunks). The floor is a huge eye laid in calcite and blackstone round a glowing froglight iris and an obsidian pupil; the walls are veined with crying obsidian and amethyst, with dripstone hanging from the ceiling and the odd shroomlight. Petrified adventurers (grey armour stands, arms raised against its gaze) stand round the floor, and against the far wall a hoard of gold, bones and skulls surrounds a chest (`chests/beholder_lair`: gold, amethyst, emeralds, diamonds, ender pearls, enchanted books and diamond gear, and sometimes an enchanted golden apple, a totem of undying or a spyglass). The Beholder floats above the eye. The cavern is sealed in a two-block shell, so caves, water and lava can't break in. A deepslate-brick spiral stair climbs from a tunnel off the cavern to a mossy ring of wall on the surface, 20 blocks east of the cavern's centre. Silent Footsteps plays inside, no other monsters spawn there, and once the Beholder's dead the lair sends a new one now and then. Find one with `/locate structure dndclasses:beholder_lair`.

### Hobbit Villages
Cozy, Shire-style villages that generate in plains, sunflower plains and meadows, laid out differently every time:
- **Smials** (hobbit holes) dug into grassy hills, with round green (or red, or yellow) doors, round windows, chimneys and fenced front gardens. Inside: a fireplace, a kitchen, a table laid with food, and in the bigger ones a pantry stacked with barrels and a bedroom.
- **The Green Dragon** inn with a thatched roof, a bar backed by ale casks, and tables of food.
- **The party green** round the party tree, with long tables of cakes and plates, a striped pavilion, ale and a bonfire.
- **Gardens, orchards, market stalls and ponds**, joined by winding lanes with lamp posts.

Barrels and chests are full of food (`chests/hobbit_pantry`, `hobbit_larder`, `hobbit_harvest`, `hobbit_ale`), and the plates and hams in item frames can be taken. Find one with `/locate structure dndclasses:hobbit_village`.

### Dwarven Fortresses
Luxurious dwarven fortresses carved into mountainsides (meadows, groves, slopes, peaks and windswept hills), laid out differently every time:
- **The great gate**: a towering deepslate facade with a gold crest, gilded doorway and raised portcullis, flanked by gold-capped pillars and banners, opening onto a terrace with braziers and guards.
- **The great hall**: a long pillared hall with chandeliers, banners, feasting tables and a red carpet, with two or three doorways down each side.
- **The throne room**, where the Dwarf King (gold crown, netherite axe, double health) sits on a gold throne on a stepped dais, and behind the throne the **treasury**, heaped with gold.
- **Side rooms**: forges with lava channels, barracks with bunks and armour stands, mead halls with long tables of food, and working mines with ore, amethyst and an ore cart. Every fortress has a forge and barracks.

The halls are buried in the mountain: where the rock is too thin, the fortress piles more on top. Dwarves live in every room and keep turning up inside; monsters never spawn there. Chests and barrels use `chests/dwarven_fortress_treasury`, `_forge`, `_barracks`, `_brewhall` and `_mine`. Find one with `/locate structure dndclasses:dwarven_fortress`.
