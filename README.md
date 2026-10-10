# Dungeons and Dragons Mod
A Fabric mod for Minecraft 1.19.4 that brings Dungeons & Dragons to the game: fifteen playable classes with their own strengths, weaknesses and special abilities, plus dragons, bosses, new structures, enchantments, armor and music.

The full reference, one page per feature, is in [docs/](docs/README.md).

## Contents
- [Building](#building)
- [Features](#features)
  - [Player Classes](#player-classes)
  - [Mana and Specials](#mana-and-specials)
  - [Class Progression](#class-progression)
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
Building needs JDK 21; the built mod runs on Java 17 or newer, like Minecraft 1.19.4. From the project root, use the Gradle wrapper:

```sh
./gradlew genSources   # decompile Minecraft for your IDE (optional)
./gradlew build        # build the mod jar into build/libs/
./gradlew runClient    # start a development client with the mod
```

On Windows use `gradlew.bat`. For VS Code, `./gradlew vscode` generates the launch configuration. On Debian or Ubuntu, install the JDK with `sudo apt install openjdk-21-jdk`.

See [Testing in the dev client](docs/dev/testing.md) for scripted test runs.

## Features

### Player Classes
Every class has its own strengths, weaknesses and a special ability. Players pick a class when they first join and get a **Class Guidebook** explaining it (see [Class selection](docs/systems/class-selection.md) and [Class guidebook](docs/systems/class-guidebook.md)). Click a class for its full page.

<!-- class-table:start - generated from src/main/resources/data/dndclasses/class_info.json, the same data the guidebook uses. Edit that file and run ./gradlew generateClassReadme -->
| Class | Strengths | Weaknesses | Special ability |
|-|-|-|-|
| **[Barbarian](docs/classes/barbarian.md)** | Greatly increased strength<br>Health to rival a dragon | Limited vision: fog closes in at about 24 blocks<br>Moves very slowly | **Rage**: Strength I for 8 seconds, rising to Strength III for 12 seconds with ranks |
| **[Bard](docs/classes/bard.md)** | Ignored by hostile mobs | Less health | **Animal Friends**: nearby animals you have charmed with an instrument become companions that follow and fight for you |
| **[Cleric](docs/classes/cleric.md)** | Faster mining<br>Night vision | Shorter view distance: fog closes in at about 48 blocks<br>Slightly less attack damage | **Sanctuary**: a circle mobs won't target, lasting 6 to 15 seconds by rank. From rank II, party members within 8 to 16 blocks share it and regenerate |
| **[Druid](docs/classes/druid.md)** | An extra heart for each tamed animal, up to 5<br>Regenerates in light (level 10 or brighter) | Cannot swim<br>Gets hungry in the dark (light level 4 or lower) | **Wild Shape**: become an animal you have killed and unlocked at an Attunement Table for 15 to 30 seconds by rank. Sneak and press the key to choose the form |
| **[Fighter](docs/classes/fighter.md)** | High health<br>High strength<br>Draws aggro: hostile mobs target a Fighter before other players | Cannot use bows or crossbows<br>No beneficial potions: potions can't be drunk and buffs don't apply, but harmful effects still do | **Super Regeneration**: Regeneration V for 4 seconds, up to 10 seconds with ranks |
| **[Monk](docs/classes/monk.md)** | Triple jump<br>Unrivaled attack speed | Reduced damage: 75% unarmored, and less the more armor you wear<br>Can only attack with a staff or bare fists | **Flurry Rush**: blink between the mob you're looking at and hostiles near it for a rapid chain of hits, then blink back. 3 hits on 1 target, up to 10 hits across 5 targets with ranks |
| **[Paladin](docs/classes/paladin.md)** | High health<br>Protective auras for nearby players<br>Unaffected by potions, good or bad (ability effects still apply) | Cannot craft (crafting table, stonecutter, smithing table, loom) or brew potions<br>Very weak in the Nether: half damage and armor, 20% slower | **Divine Judgment**: a beam of holy light strikes the mob you look at within 30 blocks, sets it alight and sends out a shockwave. Undead take 50% more damage. Damage, shockwave and extra beams grow with rank |
| **[Ranger](docs/classes/ranger.md)** | Can zoom in with a bow<br>Draws bows faster | Cannot pick up swords<br>Weak to fire | **Arrow Storm**: hold right click to fire arrows without using ammo. Fire rate, arrow speed and duration grow with rank; no zooming while it runs |
| **[Rogue](docs/classes/rogue.md)** | Immune to poison<br>Never needs to eat: food never drains and you never starve, but food doesn't regenerate health either | Low health | **Vanish**: invisibility for 6 seconds at rank I, up to 15 seconds at rank IV |
| **[Necromancer](docs/classes/necromancer.md)** | Melee hits inflict Wither<br>Undead mobs won't attack you | Slightly less health<br>Much less damage | **Raise Dead**: 2 undead allies for 10 seconds, up to 5 stronger undead and a Bone Wyvern for 20 seconds with ranks |
| **[Warlock](docs/classes/warlock.md)** | Throws fireballs from an empty hand<br>Immune to fire and lava | Reduced damage<br>Hurt by water and rain: 1 damage every 4 seconds, never below 1 heart | **Fire Breath**: hold the key to breathe fire, 8 seconds and 3 blocks at rank I up to 20 seconds and 7 blocks at rank IV |
| **[Wizard](docs/classes/wizard.md)** | Can wield elemental staffs | Greatly reduced health<br>Can't wear armor heavier than iron | **Arcane Explosion**: a 12-block blast with Resistance V for 2 seconds, up to 72 blocks and 5 seconds with ranks |
| **[Artificer](docs/classes/artificer.md)** | Faster movement<br>25% chance for crafted tools, weapons and armor to come out enchanted | Deals 25% less damage<br>Unaffected by potions: can't drink them and is immune to splash, lingering and tipped-arrow effects (ability effects still apply) | **Reinforced Armor**: +8 armor and +4 toughness for 30 seconds |
| **[Blood Hunter](docs/classes/blood-hunter.md)** | Swords always have Fire Aspect<br>Double damage at night | Half damage during the day<br>Can't drop swords (they still drop on death) | **Blood Control**: take control of the mob you look at, 8 seconds within 15 blocks at 60% success, up to 20 seconds, 30 blocks and 100% with ranks. Strong mobs resist more, a failure costs the mana and a heart, and bosses can't be controlled. Requires the Identity mod |
| **[Alchemist](docs/classes/alchemist.md)** | Can brew potions only the Alchemist knows<br>Brewing stands are safe: a stand explodes when its brew finishes if the last player to use it wasn't an Alchemist (stands nobody has used, such as hopper-fed ones, are safe) | Cannot enchant (enchanting table or enchanted books on an anvil) | **Transmute**: throw your held potion (or an unstable brew) as a lingering cloud with stronger effects, buffing allies and harming mobs. Bigger and longer-lasting with ranks |
<!-- class-table:end -->

### Mana and Specials
Every class has a special ability. Fill the 9-pip mana bar (18 seconds) and press **Z** to use it. See [Mana and class specials](docs/systems/mana.md) and [Class selection](docs/systems/class-selection.md); each class has its own page under [docs/classes/](docs/README.md#classes).

### Class Progression
Classes level up to 10 with class XP. Each level gives a skill point to spend on the class skill tree or on ranking up abilities at an Attunement Table. At level 3 you choose a subclass, one of the tree's two branches. Bards and Druids keep a bestiary of the creatures they have killed. See [Class progression](docs/systems/class-progression.md).

### Parties
`/party` lets players group up: shared XP, no friendly fire, a party health HUD and a party-aware Cleric special. See [Party](docs/systems/party.md).

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
- [Dragons](docs/mobs/dragons.md): the Wyvern, Ember Wyvern, Lightning Chaser, Frost Drake and the tameable River Pikehorn. They breathe fire (or frost), have hittable wings and tails, and killing any of them earns **Dragon Slayer**.
- [Goblins](docs/mobs/goblins.md), [Magmamunchers](docs/mobs/magmamunchers.md), [Hobbits](docs/mobs/hobbits.md) and [Mountain Dwarves](docs/mobs/mountain-dwarves.md).
- [Mimic](docs/mobs/mimic.md): a chest that isn't. It bites and grabs whoever opens or hits it, and hides in dungeons, dragon lairs and dwarven fortresses.
- [Owlbear](docs/mobs/owlbear.md): a hostile owl-headed bear in dark and old-growth forests that charges and bear-hugs. Druids who kill one can take its form.
- [Gelatinous Cube](docs/mobs/gelatinous-cube.md): a slow jelly cube in dark caves and dungeons that engulfs whatever it touches and soaks up items.
- Bosses with boss bars, fight music and phases: the [Goblin Warlord](docs/bosses/goblin-warlord.md), the [Magmamuncher Alpha](docs/bosses/magmamuncher-alpha.md), the [Lich](docs/bosses/lich.md) (reforms from its phylactery until that's smashed) and the [Beholder](docs/bosses/beholder.md) (its anti-magic cone blocks class specials). See [Boss fights](docs/bosses/boss-fights.md).

### Structures
- [Dragon Lairs](docs/structures/dragon-lairs.md) on mountain summits (`/locate structure dndclasses:dragon_lair`).
- [Frost Lairs](docs/structures/frost-lairs.md): Frost Drake nests on Frozen Peaks summits (`dndclasses:frost_lair`).
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
