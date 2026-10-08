# Dungeons and Dragons Mod

**Documentation:** the full reference, one page per feature, is in [docs/](docs/README.md).

## Table of Contents
- [Building](#building)
- [Features](#features)
  - [Player Classes Overview](#player-classes-overview)
  - [Mana and Specials](#mana-and-specials)
  - [Parties](#parties)
  - [d20 Skill Checks](#d20-skill-checks)
  - [Armor and Items](#armor-and-items)
  - [Enchantments](#enchantments)
  - [Music](#music)
  - [Mobs and Bosses](#mobs-and-bosses)
  - [Structures](#structures)
  - [Goblin Raids](#goblin-raids)
  - [Admin Commands](#admin-commands)
- [Documentation](#documentation)

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

Every player gets a **Class Guidebook** on first join and on class change, explaining their class and its special-ability key. See [docs/systems/class-guidebook.md](docs/systems/class-guidebook.md).

<!-- class-table:start - generated from src/main/resources/data/dndclasses/class_info.json, the same data the guidebook uses. Edit that file and run ./gradlew generateClassReadme -->
| **Class**| **Pros**|**Cons**|**Special Ability**|
|-|-|-|-|
| **Barbarian** | Strength highly buffed, Health rivals dragons | Limited vision (fog closes in at ~24 blocks), Moves very slowly | Strength III for 15 seconds, inspired by *One Punch Man* |
| **Bard** | Invisible to mobs | Less health | Instantly tame tameable animals |
| **Cleric** | High mining speed, Night vision | Shorter viewing distance (fog closes in at ~48 blocks), Slightly reduced attack damage | Circle of ignoring mobs (party members within 16 blocks share it and get Regeneration I for 10s) |
| **Druid** | Gains an extra heart per tamed animal (up to 5), Regenerates in light (level 10+) | Cannot swim, Gets hungry in the dark (light 4 or less) | Transforms into a random animal it has killed (or an Owlbear, once it has killed one) for 30s |
| **Fighter** | High health, High strength, Attracts mobs: hostile mobs prefer a Fighter over other players (done) | Cannot use bows or crossbows (done), No potions: can't use potion items and potion buffs don't apply, harmful potions still do (done) | Super regeneration: Regeneration V for 10s (done) |
| **Monk** | Increased mobility, Increased attack speed | Reduced damage output (75% unarmored, less the more armor you wear), Can only attack with a staff or bare fists | Can triple jump, Unrivaled attack speed |
| **Paladin** | High health, circle of healing, Unaffected by potions, good or bad (ability effects still apply) (done) | Cannot craft anything (crafting, stonecutter, smithing table, loom) or brew potions (done), Very weak in the Nether: half damage and armor, 20% slower (done) | Instantly heals everyone within 10 blocks to full, and party members within 24 blocks to full plus Absorption I for 30s (done) |
| **Ranger** | Can zoom in with bow, faster firing (done) | Cannot pick up swords (done), Weak to fire (done) | Hold right click to spam fire (no ammo consumed) (done) |
| **Rogue** | No poison damage (done), No need to eat: food never drains and no starvation, but no natural regen from food either (done) | Low health (done) | Temporary invisibility (done) |
| **Necromancer** | Wither debuff on melee enemies (done), Undead do not attack (done) | Slightly less health (done), Significantly less damage (done) | Can spawn allied undead to attack enemies (done) |
| **Warlock** | Can throw fireballs with an empty hand (done), Immune to fire and lava (done) | Reduced damage output (done), Hurt by water and rain, 1 damage every 4s but never below 1 heart (done) | Can breathe fire by holding a special key (done) |
| **Wizard** | Can wield elemental staffs (done) | Greatly reduced health (done), Can't wear armor heavier than iron (done) | Creates a massive explosion and becomes invulnerable for a few seconds (done) |
| **Artificer** | Increased movement speed (done), 25% chance for crafted tools, weapons and armor to come out enchanted (done) | Deals 25% less damage (done), Unaffected by potions except abilities: can't drink potions, immune to splash/lingering/tipped-arrow effects (done) | Temporarily buffs all armor: +8 armor, +4 toughness for 30 seconds (done) |
| **Blood Hunter** | Fire aspect applied to all swords (done), Double damage at night (done) | Half damage during the day (done) | Can take control of any mob within 30m for 20 seconds (needs the Identity mod) (done) |
| **Alchemist** | Can craft special potions exclusive to the class (done), Brewing stands don't explode: a vanilla brewing stand explodes when its brew finishes if the last player to use it wasn't an Alchemist (stands nobody has used, e.g. hopper-fed, are safe) (done) | Cannot enchant (enchanting table or enchanted books on an anvil) (done) | Instantly buff all potions to max level (done) |
<!-- class-table:end -->

### Mana and Specials
Every class has a special ability. Fill the 9-pip mana bar (18 seconds) and press **Z** to use it. See [Mana and class specials](docs/systems/mana.md) and [Class selection](docs/systems/class-selection.md); each class has its own page under [docs/classes/](docs/README.md#classes).

### Parties
`/party` lets players group up: shared XP, no friendly fire, a party health HUD and party-aware Paladin/Cleric powers. See [Party](docs/systems/party.md).

### d20 Skill Checks
Some actions roll a d20 plus a class modifier, shown on the HUD with a sound: Rogues pick the locks of dungeon and lair loot chests, Bards persuade villagers for better prices, and melee attacks crit on a natural 20 and fumble on a natural 1. See [D20 skill checks](docs/systems/d20-skill-checks.md).

### Armor and Items
- [Armor](docs/items/armor.md): 14 class-themed sets. A full set gives a bonus to anyone, boosted for the classes it's made for.
- [Staffs](docs/items/staffs.md): Staffs of Fire, Ice and Lightning, and the Monk Staff.
- [Potions and brewing](docs/items/potions-and-brewing.md): the Alchemist's Fast Brewing Stand, the Potion of Freezing, and brewing stands that explode for non-Alchemists.
- [Bard instruments](docs/items/bard-instruments.md): the Lute, War Drum and Flute. When a Bard plays one, every player within 16 blocks gets Regeneration, Strength or Speed for 30 seconds.

### Enchantments
Lunge, Invulnerability, Tree Feller, Grid Miner, Returning, Vampiric, Smite Dragons and Featherfall, which only an Artificer can roll at an enchanting table. See [Enchantments](docs/enchantments/README.md).

### Music
Event music for boss fights, goblin raids, low health, dungeons, Nether Fortresses, travelling and night, a sting when you use your special, five music discs, and a Lich fight theme. See [Music](docs/music.md).

### Mobs and Bosses
- [Dragons](docs/mobs/dragons.md): the Wyvern, Ember Wyvern, Lightning Chaser and the tameable River Pikehorn. They breathe fire, have hittable wings and tails, and killing any of them earns **Dragon Slayer**.
- [Goblins](docs/mobs/goblins.md), [Magmamunchers](docs/mobs/magmamunchers.md), [Hobbits](docs/mobs/hobbits.md) and [Mountain Dwarves](docs/mobs/mountain-dwarves.md).
- [Mimic](docs/mobs/mimic.md): a chest that isn't. It bites and grabs whoever opens or hits it, and hides in dungeons, dragon lairs and dwarven fortresses.
- [Owlbear](docs/mobs/owlbear.md): a hostile owl-headed bear in dark and old-growth forests that charges and bear-hugs. Druids who kill one can take its form.
- [Gelatinous Cube](docs/mobs/gelatinous-cube.md): a slow jelly cube in dark caves and dungeons that engulfs whatever it touches and soaks up items.
- Bosses with boss bars, fight music and phases: the [Goblin Warlord](docs/bosses/goblin-warlord.md), the [Magmamuncher Alpha](docs/bosses/magmamuncher-alpha.md), the [Lich](docs/bosses/lich.md) (reforms from its phylactery until that's smashed) and the [Beholder](docs/bosses/beholder.md) (its anti-magic cone blocks class specials). See [Boss fights](docs/bosses/boss-fights.md).

### Structures
- [Dragon Lairs](docs/structures/dragon-lairs.md) on mountain summits (`/locate structure dndclasses:dragon_lair`).
- [Hobbit Villages](docs/structures/hobbit-villages.md) in plains and meadows (`dndclasses:hobbit_village`).
- [Hobbit Tavern](docs/structures/hobbit-tavern.md): every village inn has an innkeeper who trades food and ale, and a bounty board with daily hunts and expeditions.
- [Goblin Camps](docs/structures/goblin-camps.md): palisaded war camps in forests and plains (`dndclasses:goblin_camp`).
- [Dwarven Fortresses](docs/structures/dwarven-fortresses.md) carved into mountainsides (`dndclasses:dwarven_fortress`).
- [Nether Fortress additions](docs/structures/nether-fortresses.md): goblins and a Warlord in every fortress.
- Beholder Lairs: sealed domed caverns deep in the deepslate, reached by a spiral stair (`dndclasses:beholder_lair`). See [Beholder](docs/bosses/beholder.md).

### Goblin Raids
At night, goblin war parties raid hobbit villages and dwarven fortresses in waves, ending with a Goblin Warlord. Players nearby see a raid bar and hear raid music, and defenders who win are rewarded. Start one by hand with `/goblinraid start`. See [Goblin raids](docs/systems/goblin-raids.md).

### Admin Commands
`/dndclass get <player>` and `/dndclass set <player> <class>` change a class without dying. See [Admin commands](docs/systems/admin-commands.md).

## Documentation
[docs/README.md](docs/README.md) indexes every page: classes, systems, mobs, bosses, structures, enchantments, items, music, and developer notes (testing with DevScript, multipart mobs, the boss framework).
