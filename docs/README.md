# DnD Mod documentation
The detailed reference for the mod, one page per feature, grouped by area. The [main README](../README.md) has the short overview.

Each feature page follows the same template, in this order:
1. A short summary (no heading), then a screenshot if there is one.
2. `## How it works`: the rules and numbers, taken from the code.
3. `## Where to find it`: how a player gets or meets it (picker, recipe, loot, structure, key).
4. `## Commands`: only if it has any.
5. `## Configuration`: only if it has a gamerule or config option; most features have none.
6. `## Known limitations`: only if there are any.
7. `## For developers`: classes, constants, hooks and devscripts.

Reference pages (this index, [Admin commands](systems/admin-commands.md), the [enchantments overview](enchantments/README.md) and the [Developers](#developers) pages) don't follow it.

## Classes
- [Barbarian](classes/barbarian.md): 40 HP and huge damage, but slow and short-sighted; subclass features
- [Bard](classes/bard.md): ignored by monsters; a built-in lute slot (G) charms animals, which become companions that follow and fight for you; subclass features
- [Cleric](classes/cleric.md): Haste and Night Vision; a circle mobs can't target; subclass features
- [Druid](classes/druid.md): hearts from tamed animals, light regen, Wild Shape into killed and unlocked animals; subclass features
- [Fighter](classes/fighter.md): tough tank that draws mobs; no bows or potions; subclass features
- [Monk](classes/monk.md): triple jump and fast fists; staff or fists only; Flurry Rush blink-strike special; subclass features
- [Paladin](classes/paladin.md): protective auras, Divine Judgment beams and a Circle of Healing; no potions, crafting or brewing; weak in the Nether; subclass features
- [Ranger](classes/ranger.md): instant bow draw and an ammo-free Arrow Storm that scales with its rank; subclass features
- [Rogue](classes/rogue.md): no hunger or poison; turns invisible (longer with each rank) and dodges projectiles with Danger Sense; subclass features
- [Necromancer](classes/necromancer.md): undead ignore you; Raise Dead ranks up to more, stronger undead and a Bone Wyvern; subclass features
- [Warlock](classes/warlock.md): empty-hand fireballs, fireproof, fire breath; hurt by water; subclass features
- [Wizard](classes/wizard.md): 10 HP glass cannon with a self-centred explosion that grows with ranks; subclass features
- [Artificer](classes/artificer.md): auto-enchants crafted gear, the only one to roll mod enchantments; subclass features
- [Blood Hunter](classes/blood-hunter.md): burning swords, strong at night, possesses mobs; subclass features
- [Alchemist](classes/alchemist.md): safe and fast brewing, Transmute potion clouds; can't enchant; subclass features

## Races
- [Races](races/races.md): the race picker, 8 races with body sizes, head features, stat modifiers and ability bonuses, `/dndrace` and the `dndRaces` gamerule
- [Dragonborn Breath Weapon](races/breath-weapon.md): B breathes fire, frost (freezes water) or lightning (arcs) in a 6-block cone, 6 + level/2 damage, DEX save, 60 s cooldown that survives death
- [d20 and combat racial traits](races/combat-traits.md): Halfling Lucky rerolls natural 1s (clover on the HUD), Half-Orc Savage Attacks (x2.5 crits) and Relentless Endurance (1 HP instead of dying, 10 min), Tiefling Hellish Rebuke (melee attackers burn, 20 s)
- [Passive racial traits](races/passive-traits.md): Darkvision, half damage from fire (Tiefling) or your breath element (Dragonborn), Gnome -30% magic, Dwarf half poison and +20% mining with hidden doorways outlined, Elf Keen Senses and Trance, Halfling Second Breakfast, Gnome Tinker, Human +10% XP and +2 social checks
- [Racial homes](races/racial-homes.md): home settlements for Halflings (Hobbit Villages), Dwarves (Dwarven Fortresses), Elves (Elven Enclaves), Dragonborn (Dragon Temples), Half-Orcs (Orc Strongholds), Tieflings (Brimstone Sanctums) and Gnomes (Cogsdeep): welcome, hearth regeneration, 25% kin prices, kin-only racial gear, kin trust, the Dwarf King's audience, the temple's blessing and rebirth, the Rally and the Trial of Strength, sanctum fire immunity and a free daily-refilled respawn anchor

## Systems
- [Class selection](systems/class-selection.md): the class picker and what picking a class does
- [Character creation](systems/character-creation.md): new players pick race and class on one screen with a combined summary (health, speed, ability scores, traits) and one chat intro
- [Mana and class specials](systems/mana.md): the 9-pip mana bar (1 pip every 2 s), the power-up key (Z) and every class's 9-mana root special
- [Rests and charges](systems/rests.md): charges for major actives, short rests sitting at campfires, long rests in beds, tavern rooms and party camps (watch rolls, ambushes), Hit Dice
- [Downed and death saves](systems/death-saves.md): at 0 HP with an ally near you crawl and roll death saves; mobs ignore you, bosses punish party wipes; solo players get one Last Stand roll; allies stabilise, heal or feed you back up; a downed HUD, party status and red ally outlines show who's down
- [Class progression](systems/class-progression.md): class XP sources, subclasses chosen at level 3, ability ranks at the Attunement Table (1 point each, levels 3/6/9), the per-class point budget and the tiered Bard/Druid bestiary
- [Admin commands](systems/admin-commands.md): `/dndclass` (class, progress, XP, ranks, subclass, rests, sheet, Downed), `/dndrace`, `/dndhome`, `/goblinraid`, and pointers to `/dungeon`, `/dndobstacle`, `/dm`, `/party`, `/quest` and `/rep`
- [Party](systems/party.md): `/party` groups of up to 8: shared XP within 48 blocks, no friendly fire, the party HUD, and what else checks the party
- [Party roles](systems/party-roles.md): Tank, Healer, Damage, Support or Utility for every class, in the picker, guidebook and party HUD
- [Party synergies](systems/party-synergies.md): Bodyguard, Triage, Opportunist, Inspire and Scout between party roles, Balanced party XP and +2 assists on obstacle checks
- [Ability scores](systems/ability-scores.md): six ability scores, proficiency, saves and skills per class; the sheet behind every d20 roll; the Character tab and ability points at levels 4 and 8
- [Saving throws](systems/saving-throws.md): the `SavingThrow` API (exposures, half damage), the compact save lane beside the crosshair, STR/DEX saves against grabs with jump-to-struggle escapes, and `/dm save|check`
- [Conditions](systems/conditions.md): Prone, Frightened, Restrained, Blinded, Stunned, Petrified, Poisoned, Charmed, Incapacitated and Paralyzed with sources, lengths and repeat saves; HUD icons above the save lane; grabs Restrain, the Lich and Beholder Frighten, an Owlbear charge knocks Prone; `/dndcondition` (PR #276)
- [Data-driven saves](systems/data-driven-saves.md): datapack JSON saves against effects and damage types: CON vs mob poison and wither (Lich Wither Bolt DC 15), Warden sonic boom and Ender Dragon breath; half duration or damage
- [Class save features](systems/class-save-features.md): Aura of Protection adds CHA to nearby saves, Evasion, Danger Sense, Rage advantage, Indomitable rerolls, Jack of All Trades, Diamond Soul; with conditions: Aura of Courage, Mindless Rage, Stillness of Mind, Stunning Strike stuns, Cure Wounds ends one, Countercharm, Fey Ancestry (PR #277)
- [Stealth and Perception](systems/stealth-and-perception.md): sneaking range shrinks with passive Stealth (heavy armour hurts it), passive Perception spots breathing mimics, Search (V) reveals hidden things; the `Perceivable` API
- [Creature study](systems/creature-study.md): V on a creature rolls Religion, Nature, Arcana or Insight (DC 10/13/15) for a card of its health, armour, save-calling attacks and immunities; a natural 20 adds its weakness; Insight reads villagers and dwarves
- [D20 skill checks](systems/d20-skill-checks.md): lockpicking (chests and doors, with a lock glint for Rogues and Rangers), searching walls, persuasion, Arcana, crits and fumbles
- [Class-gated obstacles](systems/obstacles.md): a generated catalogue of every obstacle and each class's exclusive (enforced by a test), Arcane Seals for Wizards and Warlocks, physical obstacles (Collapsed Passages, Heavy Boulders, Portcullis winches, Chasms, Crumbling Ledges) for Barbarians, Fighters and Monks, and divine and death obstacles (Holy Wards, Desecrated Altars, Dread Wards, Ossuary Gates, Crypt Sleepers, Blood Seals) for Clerics, Paladins, Necromancers and Blood Hunters, Hidden Doors, Locked Doors and temple traps for Rangers and Rogues, Guardian Beasts, Thornwalls and Singing Doors for Druids and Bards, and craft and element obstacles (Clockwork Doors, Alchemical Seals, Miasma Vents, Fire Curtains) for Artificers, Alchemists and Warlocks, with a crosshair hint and `/dndobstacle`, rolled from the character sheet; placed in goblin camps, dwarven fortresses, dragon and frost lairs, Beholder lairs, stronghold libraries and corridors, mineshafts and Nether fortresses
- [Class guidebook](systems/class-guidebook.md): in-game guidebook item driven by shared class data, with an Obstacle Codex chapter built from the obstacle registry
- [Magic items](systems/magic-items.md): rarity tiers and coloured names, +1/+2/+3 weapons and armor, attunement (3 bonds, the table's Items tab), identification, curses and Remove Curse, and `/dndmagic`
- [Magic loot](systems/magic-loot.md): where magic items turn up: the four rarity tables (+N gear and named items) and the extra magic pools in mod chests, bosses, Mimics, bounties, raids and vanilla structures
- [Goblin raids](systems/goblin-raids.md): goblin raids on hobbit villages and dwarven fortresses; the settlement's health, and losing the raid when it falls
- [Factions](systems/factions.md): per-player reputation with hobbits, dwarves and goblins, data-driven factions, `/rep`; tiers change prices, hostility and raids, and higher tiers add perks (raid lookouts and loot, a free room, barrels, dwarf allies)
- [Quests](systems/quests.md): data-driven quest chains with party-shared progress and per-hero rewards, `/quest`; four launch chains (Goblin Warlord, Lich, Frost Drake and Lightning Chaser, Beholder) and Rumour notices on bounty boards
- [Journal and quest tracker](systems/quest-journal.md): the Journal (J) with Quests and Factions tabs, the top-right quest tracker and quest toasts
- [NPC dialogue](systems/dialogue.md): talk to the innkeeper, Dwarf King and hobbit Thain to take quests, hand in and get rewards; tier greetings and d20 checks; data-driven dialogue files
- [Dungeon Master](systems/dungeon-master.md): `/dm` mode, the veil, data-driven encounters, freeze, roll calls on the d20 HUD, narration, NPC voice, mood music, loot planting, the DM Wand and the DM panel (K) for running a session
- [Dungeon encounters](systems/dungeon-encounters.md): party-scaled room fights, ward seals, the Ossuary Cube champion (splits at half health) and the Lich boss leashed to its reliquary
- [Dungeon loot](systems/dungeon-loot.md): tier chests, Veteran drops, the per-player Hoard Coffer and dungeon repopulation
- [Dungeon puzzles and gates](systems/dungeon-puzzles-and-gates.md): the rune-pillar puzzle room, class-check gates on the main path and the Wizard-only side vault
- [Dungeon traps](systems/dungeon-traps.md): dart, flame, pit and poison-needle traps, party-shared spotting, Rogue and Artificer disarming and re-arming on reset

## Mobs
- [Dragons](mobs/dragons.md): Wyvern, Ember Wyvern, Lightning Chaser, Frost Drake and the tameable River Pikehorn; DEX saves against breath and storms
- [Bone Wyvern](mobs/bone-wyvern.md): the Necromancer's small undead dragon from Raise Dead rank V
- [Goblins](mobs/goblins.md): Goblin Warriors in Nether Fortresses and goblin camps
- [Magmamunchers](mobs/magmamunchers.md): fire-proof Nether beasts
- [Hobbits](mobs/hobbits.md): peaceful villagers that share their food
- [Mountain Dwarves](mobs/mountain-dwarves.md): neutral fortress guards that barter for gold
- [Dragonborn](mobs/dragonborn.md): breath-weapon Templars and the Elder merchant of the dragon temples
- [Orcs](mobs/orcs.md): the Bloodtusk Horde's Warriors, crossbow Archers, the Quartermaster merchant and the Trial's Champion; hostile except to Half-Orcs, allied with goblins
- [Tieflings](mobs/tieflings.md): fire-immune Exiles with Hellish Rebuke and fireballs, and the Pactbroker merchant with Tiefling, Warlock and Alchemist stock, of the Brimstone Sanctums
- [Gnomes](mobs/gnomes.md): Small Gnome Tinkers who sell spyglasses, clocks and redstone (and Gnome-only gear), and the copper Clockwork Sentinels that guard Cogsdeep
- [Elves](mobs/elves.md): Wood Elves, bow-armed Elf Wardens and the Speaker and Fletcher merchants of the elven enclaves
- [Mimic](mobs/mimic.md): a chest that bites and grabs (STR save, jump to struggle free); hides in dungeons, dragon lairs and dwarven fortresses
- [Owlbear](mobs/owlbear.md): forest predator that charges and bear-hugs (STR saves, jump to struggle free); Druids can take its form
- [Gelatinous Cube](mobs/gelatinous-cube.md): slow jelly cube that engulfs mobs, players and items (DEX save to slip out)
- [Troll](mobs/troll.md): regenerating swamp and forest brute with a two-claw Rend; fire or acid stops the healing, and it only stays dead if you burn it once it falls (PR #273)
- [Mind Flayer](mobs/mind-flayer.md): deep-cave illithid whose Mind Blast cone stuns (INT save), whose tentacles extract stunned brains, which blinks away and keeps charmed thralls (PR #280)

## Bosses
- [Boss fights](bosses/boss-fights.md): boss bars, fight music, phases, rewards and party wipes for every boss (Warlord, Alpha, Lich, War Chief, Beholder, Medusa, dragons)
- [Goblin Warlord](bosses/goblin-warlord.md): goblin boss that summons waves; guards Nether Fortresses, leads 30% of goblin camps and every raid's last wave, and rules the Goblin Warren
- [Magmamuncher Alpha](bosses/magmamuncher-alpha.md): rare Nether boss with burning bites and fireball volleys
- [Lich](bosses/lich.md): undead caster in stronghold libraries that reforms from its phylactery; CON save vs Frost Nova, WIS save vs its Dread Presence
- [Orc War Chief](bosses/orc-war-chief.md): stronghold boss with a 6-damage Ground Slam every 8 s and a War Cry that gives his orcs Strength at half health
- [Beholder](bosses/beholder.md): floating eye tyrant in a deep lair; its anti-magic cone blocks class specials, and each eye ray allows a DEX/STR/CON/WIS save
- [Medusa](bosses/medusa.md): lair boss of the Gorgon's Temple (1 in 3 desert pyramids and jungle temples); meet her eyes and save (CON 14) or turn to stone, raise a shield to reflect it; poisonous snake bites, longbow volleys, statues of earlier visitors; drops the Gorgon's Gaze (PR #281)

## Structures
- [Dragon Lairs](structures/dragon-lairs.md): Lightning Chaser nests on mountain summits, dressed with fulgurite, storm glass, a woven dragon nest, coin piles and shed scales that drop dragon scales
- [Frost Lairs](structures/frost-lairs.md): Frost Drake nests on Frozen Peaks summits, the Staff of Ice's source; rimestone, hoarfrost crystals and shed frost scales
- [Hobbit Villages](structures/hobbit-villages.md): Shire-style villages full of food, with smials whose hills, cuttings and fronts differ one to the next
- [Dwarven Fortresses](structures/dwarven-fortresses.md): mountain halls of runic brick with a Dwarf King, a coin-heaped treasury and their own building blocks (clan crests, ancestor statues, forge bricks, runic columns and corbels, a waystone naming the hold)
- [Elven Enclaves](structures/elven-enclaves.md): treetop halls round a giant Heart Tree in birch and flower forests, rope bridges, a Moonwell and Sylvan Law; the Elf home
- [Dragon Temples](structures/dragon-temples.md): terraced shrines blended into mountain slopes, only near a dragon or frost lair; a double-roofed sanctum with three breath altars (Blessing of the Ancestors, Rite of Rebirth), a monks' hall, a pagoda library and great carved dragon statues; the Dragonborn home; built from Dragonscale Tiles, Temple Lacquer, Breath-Scorched Stone, Dragonstone, Ancestor Reliefs, prayer wheels, incense braziers, paper lanterns and sparring posts
- [Orc Strongholds](structures/orc-strongholds.md): palisaded badlands warcamps with a gatehouse, Great Longhouse, Fighting Pit, smithy and yurts, built from tanned hide, stake walls and war drums; hostile except to Half-Orcs; the Trial of Strength makes you Blood-Sworn; the Half-Orc home
- [Brimstone Sanctums](structures/brimstone-sanctums.md): walled Nether platforms on pillars in crimson forests and soul sand valleys, never over the lava sea; Pact Hall, Brass Market, Alchemist's den and the Refuge's respawn anchors; the Tiefling home; built of Brimstone, Hellbrass and the sanctum's sigils, script panels, Contract Lectern, vents, sconces and censers
- [Cogsdeep](structures/cogsdeep.md): gnome burrows under forests and meadows: a copper-domed observatory over a spiral stair down to y 11-30, workshops of unpowered contraptions, a clocktower, a gem room and a lush grotto; the Tinkers' law; the Gnome home; the stair pillar dressed in brass and clockwork
- [Cogsdeep building blocks](structures/cogsdeep-blocks.md): Riveted Brass, Gnomish Bricks, animated Cogwork Panels and Gearwalls, Pipework, Glowcrystals, a Clockface that keeps the time, the Blueprint Table and Telescope Lens; swapped into the burrow, sold by Tinkers
- [Nether Fortress additions](structures/nether-fortresses.md): goblins, the Warlord and fortress music
- [Goblin Camps](structures/goblin-camps.md): palisaded goblin war camps in forests and plains with hide and canvas tents (sloped roofs, ridge poles, guy ropes, door flaps); the goblin building blocks (Patchwork Hide, Stretched Hide, Tent Canvas, Tent Stakes, Scrap-lashed Planks, junk piles, glyph daubs, Rope-and-Bone Fence, War Order Board), also used in Goblin Warrens
- [Desolate settlements](structures/desolate-settlements.md): abandoned copies of six settlements, 1 in 5 of each, at one of four levels (raided, abandoned, forsaken, overgrown): no one living there, looted, breached walls, holed floors, fallen doorways and rubble, roofs fallen in and huts in heaps where their walls are breached, fallen elven bridges, a caved-in Cogsdeep dome, Tattered Carpets, overgrowth
- [Hobbit Tavern](structures/hobbit-tavern.md): innkeeper, bounty board (incl. dungeon clearances with Dungeon Maps) and rentable rooms (Room Keys, Well Rested)
- [Dungeons](structures/dungeons.md): underground multi-room Crypts under a barrow, Goblin Warrens behind a cave mouth and deep Dwarven Ruins under a collapsed gatehouse, with a planned route to the Lich, the Goblin Warlord or the Beholder, a vault, theme dressing and a Challenge tier

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
- [Armor](items/armor.md): 14 class sets with full-set and matching-class bonuses; 5 racial sets (Elven Chain, Dwarven Plate, Orcish Warplate, Draconic Scale, Infernal Regalia) with race bonuses, sold as kin stock
- [Racial weapons](items/racial-weapons.md): Elven Longbow (+20% arrow speed, built-in Power I), Dwarven Waraxe (+50% to goblins), Orcish Greataxe, Halfling Sling and Gnomish Repeater (Quick Charge III, Multishot); kin-only trades
- [Tinker's Goggles](items/tinkers-goggles.md): Gnome kin-stock helmet that shows what's inside the chest, barrel, hopper or furnace you look at
- [Staffs](items/staffs.md): elemental staffs of Fire, Ice and Lightning, and the Monk Staff
- [Potions and brewing](items/potions-and-brewing.md): Fast Brewing Stand, Potion of Freezing, exploding stands
- [Bard instruments](items/bard-instruments.md): lute, drum and flute
- [Racial banner patterns](items/banner-patterns.md): eight loom patterns (Bloodtusk, Clan Anvil, Sylvan Leaf, Three Breaths, Infernal Pact, Gnome Cog, Green Dragon, Goblin Skull) hung in their homes' structures, sold by kin merchants, the goblin skull dropped by the Warlord
- [Magic items](items/magic-items.md): Wand of Magic Missiles, Bag of Holding, Decanter of Endless Water, Immovable Rod, Cloak and Ring of Protection, Periapt of Wound Closure, Amulet of Health, Doss Lute, Tome of Clear Thought; the boss and monster drops: Warlord's Warhorn, Heart of the Furnace, Storm-Chaser Boots, Wyvern-Scale Cloak, Belt of Owlbear Might, Mace of Disruption, Frostbrand, Phylactery Locket, The Burglar's Ring, Eye of the Tyrant, Gorgon's Gaze
- [Elven building blocks](items/elven-building-blocks.md): Heartwood, Moonstone, Living Wood, Silverleaf, Woven Rope Planks, Sylvan Glyph Stones, Glimmercaps, Moonpetals and Starglass, built into the Elven Enclaves
- [Hobbit building blocks](items/hobbit-building-blocks.md): Thatch, Mossy Thatch, Wattle and Daub, Smial Turf Wall, Round Door Frame Stone, Hearth Stone, Pipe-weed Bundle; the Larder Shelf (9 slots) and Ale Cask (glass bottle for a Mug of Ale, 4 mugs, refills); hanging herbs and onions
- [Crypt building blocks](items/crypt-blocks.md): Ossuary Bricks, Funerary Relief, Burial Niche Wall, Sarcophagus (some hide a zombie), Grave Candle Cluster, Grave Moss, Lich Sigil Floor and Phylactery Pedestal, built into Crypt dungeons
- [Dwarven Ruin & Beholder lair blocks](items/ruin-blocks.md): Fallen Runic Bricks (cracked, mossy), Collapsed Rubble layers, Tarnished Gilding, Ray-glazed Stone, Petrified Remains and Eye-Stalk Growths, placed in the Ruin and the lair
- [Adventuring blocks](items/adventuring-blocks.md): Bedrolls (a long rest like a bed, no spawn point), the Adventurer's Signpost naming a dungeon and its Challenge tier, Wanted Posters, Tavern Tables and Benches in oak, spruce, dark oak and Heartwood

## Music
- [Music](music.md): event music, dungeon theme loops and clear stings, class stings and music discs

## Developers
- [Testing in the dev client](dev/testing.md): DevScript, scripted hidden test clients, and which log lines land in latest.log or debug.log
- [Multipart mobs](dev/multipart-mobs.md): MultipartDragon, DragonPartLayout and DragonRenderer
- [Boss framework](dev/boss-framework.md): adding a boss with BossFight
- [Project layout](dev/project-layout.md): where things live in the code, and what each dependency is for
- [Obstacles in structures and templates](dev/obstacle-templates.md): placing obstacles from structure code, and the `dndclasses:obstacle_tier` processor for NBT templates and jigsaw pieces
- [Building blocks](dev/building-blocks.md): `BlockFamily` decorative materials (stairs, slab, wall, fence, door, ...) from one call, `tools/block_families.py` for their resources, and the DnD Building Blocks tab
