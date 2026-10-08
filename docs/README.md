# DnD Mod documentation
The detailed reference for the mod, one page per feature, grouped by area. The [main README](../README.md) has the short overview. Entries marked "(PR #n)" are in open pull requests and their pages arrive when those merge.

Each page follows the same template: summary, How it works, Where to find it, Commands, Configuration, Known limitations, For developers.

## Classes
- [Barbarian](classes/barbarian.md): 40 HP and huge damage, but slow and short-sighted
- [Bard](classes/bard.md): ignored by monsters; animals fight for you
- [Cleric](classes/cleric.md): Haste and Night Vision; a circle mobs can't target
- [Druid](classes/druid.md): hearts from tamed animals, light regen, animal forms
- [Fighter](classes/fighter.md): tough tank that draws mobs; no bows or potions
- [Monk](classes/monk.md): triple jump and fast fists; staff or fists only
- [Paladin](classes/paladin.md): heals the party; no potions, crafting or brewing; weak in the Nether
- [Ranger](classes/ranger.md): instant bow draw and an ammo-free Arrow Storm that scales with its rank
- [Rogue](classes/rogue.md): no hunger or poison; turns invisible
- [Necromancer](classes/necromancer.md): undead ignore you; summon undead allies
- [Warlock](classes/warlock.md): empty-hand fireballs, fireproof, fire breath; hurt by water
- [Wizard](classes/wizard.md): 10 HP glass cannon with a self-centred explosion
- [Artificer](classes/artificer.md): auto-enchants crafted gear, the only one to roll mod enchantments
- [Blood Hunter](classes/blood-hunter.md): burning swords, strong at night, possesses mobs
- [Alchemist](classes/alchemist.md): safe and fast brewing, upgrades potions; can't enchant

## Systems
- [Class selection](systems/class-selection.md): the class picker and what picking a class does
- [Mana and class specials](systems/mana.md): the 9-pip mana bar and the power-up key (Z)
- [Class progression](systems/class-progression.md): class levels, ability ranks at the Attunement Table and the Bard/Druid bestiary
- [Admin commands](systems/admin-commands.md): `/dndclass get|set`
- [Party](systems/party.md): group up with other players
- [D20 skill checks](systems/d20-skill-checks.md): lockpicking, persuasion, crits and fumbles
- [Class guidebook](systems/class-guidebook.md): in-game guidebook item driven by shared class data
- [Goblin raids](systems/goblin-raids.md): goblin raids on hobbit villages and dwarven fortresses

## Mobs
- [Dragons](mobs/dragons.md): Wyvern, Ember Wyvern, Lightning Chaser and the tameable River Pikehorn
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
- [Hobbit Villages](structures/hobbit-villages.md): Shire-style villages full of food
- [Dwarven Fortresses](structures/dwarven-fortresses.md): mountain halls with a Dwarf King and treasury
- [Nether Fortress additions](structures/nether-fortresses.md): goblins, the Warlord and fortress music
- [Goblin Camps](structures/goblin-camps.md): palisaded goblin war camps in forests and plains
- [Hobbit Tavern](structures/hobbit-tavern.md): innkeeper and bounty board

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
