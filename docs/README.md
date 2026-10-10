# DnD Mod documentation
The detailed reference for the mod, one page per feature, grouped by area. The [main README](../README.md) has the short overview.

Each page follows the same template: summary, How it works, Where to find it, Commands, Configuration, Known limitations, For developers.

## Classes
- [Barbarian](classes/barbarian.md): 40 HP and huge damage, but slow and short-sighted; subclass features (PR #136)
- [Bard](classes/bard.md): ignored by monsters; a built-in lute slot (G) charms animals, which become companions that follow and fight for you; subclass features (PR #136)
- [Cleric](classes/cleric.md): Haste and Night Vision; a circle mobs can't target; subclass features (PR #136)
- [Druid](classes/druid.md): hearts from tamed animals, light regen, Wild Shape into killed and unlocked animals; subclass features (PR #136)
- [Fighter](classes/fighter.md): tough tank that draws mobs; no bows or potions; subclass features (PR #136)
- [Monk](classes/monk.md): triple jump and fast fists; staff or fists only; Flurry Rush blink-strike special; subclass features (PR #136)
- [Paladin](classes/paladin.md): protective auras, Divine Judgment beams and a Circle of Healing; no potions, crafting or brewing; weak in the Nether; subclass features (PR #136)
- [Ranger](classes/ranger.md): instant bow draw and an ammo-free Arrow Storm that scales with its rank; subclass features (PR #136)
- [Rogue](classes/rogue.md): no hunger or poison; turns invisible (longer with each rank) and dodges projectiles with Danger Sense
- [Necromancer](classes/necromancer.md): undead ignore you; Raise Dead ranks up to more, stronger undead and a Bone Wyvern
- [Warlock](classes/warlock.md): empty-hand fireballs, fireproof, fire breath; hurt by water
- [Wizard](classes/wizard.md): 10 HP glass cannon with a self-centred explosion that grows with ranks
- [Artificer](classes/artificer.md): auto-enchants crafted gear, the only one to roll mod enchantments
- [Blood Hunter](classes/blood-hunter.md): burning swords, strong at night, possesses mobs
- [Alchemist](classes/alchemist.md): safe and fast brewing, Transmute potion clouds; can't enchant

## Races
- [Races](races/races.md): the race picker (before the class), 8 races with body sizes, head features, stat modifiers and ability bonuses, `/dndrace` and the `dndRaces` gamerule (body sizes and head features: PR #130)
- [Racial homes](races/racial-homes.md): home settlements for Halflings (Hobbit Villages) and Dwarves (Dwarven Fortresses): welcome, hearth regeneration, 25% kin prices, kin trust, the Dwarf King's audience (PR #135)

## Systems
- [Class selection](systems/class-selection.md): the class picker and what picking a class does
- [Mana and class specials](systems/mana.md): the 9-pip mana bar and the power-up key (Z)
- [Rests and charges](systems/rests.md): charges for major actives, short rests at campfires, long rests, Hit Dice (PR #133)
- [Class progression](systems/class-progression.md): class levels, subclasses chosen at level 3, ability ranks at the Attunement Table and the Bard/Druid bestiary
- [Admin commands](systems/admin-commands.md): `/dndclass get|set`, progress, rests and charges, `/dndrace get|set|list`
- [Party](systems/party.md): group up with other players
- [Party roles](systems/party-roles.md): Tank, Healer, Damage, Support or Utility for every class, in the picker, guidebook and party HUD
- [Ability scores](systems/ability-scores.md): six ability scores, proficiency, saves and skills per class; the sheet behind every d20 roll
- [Saving throws](systems/saving-throws.md): the `SavingThrow` API (exposures, half damage), the compact save lane beside the crosshair and `/dm save|check` (PR #128)
- [D20 skill checks](systems/d20-skill-checks.md): lockpicking, persuasion, Arcana, crits and fumbles (PR #127)
- [Class-gated obstacles](systems/obstacles.md): Arcane Seals only Wizards and Warlocks can dispel, with a crosshair hint and `/dndobstacle`, rolled from the character sheet (PR #127)
- [Class guidebook](systems/class-guidebook.md): in-game guidebook item driven by shared class data
- [Magic items](systems/magic-items.md): rarity tiers and coloured names, +1/+2/+3 weapons and armor, unidentified items, attunement (3 bonds, the table's Items tab) and `/dndmagic` (PR #129)
- [Goblin raids](systems/goblin-raids.md): goblin raids on hobbit villages and dwarven fortresses
- [Factions](systems/factions.md): per-player reputation with hobbits, dwarves and goblins, data-driven factions, `/rep`; tiers change prices, hostility and raids (PR #131)
- [Dungeon Master](systems/dungeon-master.md): `/dm` mode, the veil, data-driven encounters and freeze for running a session
- [Dungeon encounters](systems/dungeon-encounters.md): party-scaled room fights, ward seals, the Ossuary Cube champion and the Lich boss in dungeons (PR #132)

## Mobs
- [Dragons](mobs/dragons.md): Wyvern, Ember Wyvern, Lightning Chaser, Frost Drake and the tameable River Pikehorn
- [Bone Wyvern](mobs/bone-wyvern.md): the Necromancer's small undead dragon from Raise Dead rank V
- [Goblins](mobs/goblins.md): Goblin Warriors in Nether Fortresses and goblin camps
- [Magmamunchers](mobs/magmamunchers.md): fire-proof Nether beasts
- [Hobbits](mobs/hobbits.md): peaceful villagers that share their food
- [Mountain Dwarves](mobs/mountain-dwarves.md): neutral fortress guards that barter for gold
- [Mimic](mobs/mimic.md): a chest that bites; hides in dungeons, dragon lairs and dwarven fortresses
- [Owlbear](mobs/owlbear.md): forest predator that charges and bear-hugs; Druids can take its form
- [Gelatinous Cube](mobs/gelatinous-cube.md): slow jelly cube that engulfs mobs, players and items

## Bosses
- [Boss fights](bosses/boss-fights.md): boss bars, fight music, phases and rewards
- [Goblin Warlord](bosses/goblin-warlord.md): Nether Fortress boss that summons goblin waves
- [Magmamuncher Alpha](bosses/magmamuncher-alpha.md): rare Nether boss with burning bites and fireball volleys
- [Lich](bosses/lich.md): undead caster in stronghold libraries that reforms from its phylactery
- [Beholder](bosses/beholder.md): floating eye tyrant in a deep lair; its anti-magic cone blocks class specials

## Structures
- [Dragon Lairs](structures/dragon-lairs.md): Lightning Chaser nests on mountain summits
- [Frost Lairs](structures/frost-lairs.md): Frost Drake nests on Frozen Peaks summits, the Staff of Ice's source
- [Hobbit Villages](structures/hobbit-villages.md): Shire-style villages full of food
- [Dwarven Fortresses](structures/dwarven-fortresses.md): mountain halls with a Dwarf King and treasury
- [Nether Fortress additions](structures/nether-fortresses.md): goblins, the Warlord and fortress music
- [Goblin Camps](structures/goblin-camps.md): palisaded goblin war camps in forests and plains
- [Hobbit Tavern](structures/hobbit-tavern.md): innkeeper and bounty board
- [Dungeons](structures/dungeons.md): underground multi-room Crypts with a stair, a planned route to a boss and vault, and a Challenge tier

## Enchantments
- [Overview](enchantments/README.md): all enchantments at a glance
- [Lunge](enchantments/lunge.md): right-click to dash forward
- [Invulnerability](enchantments/invulnerability.md): right-click for 2 s of invulnerability, with a cooldown
- [Tree Feller](enchantments/tree-feller.md): fell a whole tree at once
- [Grid Miner](enchantments/grid-miner.md): mine out connected blocks
- [Returning](enchantments/returning.md): thrown tridents, snowballs, eggs and pearls come back
- [Vampiric](enchantments/vampiric.md): melee hits heal you for part of the damage dealt
- [Smite Dragons](enchantments/smite-dragons.md): extra melee damage against dragons
- [Featherfall](enchantments/featherfall.md): boots enchantment that cuts fall damage and slows big drops

## Items
- [Armor](items/armor.md): 14 class sets with full-set and matching-class bonuses
- [Staffs](items/staffs.md): elemental staffs of Fire, Ice and Lightning, and the Monk Staff
- [Potions and brewing](items/potions-and-brewing.md): Fast Brewing Stand, Potion of Freezing, exploding stands
- [Bard instruments](items/bard-instruments.md): lute, drum and flute

## Music
- [Music](music.md): event music, class stings and music discs

## Developers
- [Testing in the dev client](dev/testing.md): DevScript and scripted, hidden test clients
- [Multipart mobs](dev/multipart-mobs.md): MultipartDragon, DragonPartLayout and DragonRenderer
- [Boss framework](dev/boss-framework.md): adding a boss with BossFight
- [Project layout](dev/project-layout.md): where things live in the code
