# Class-gated obstacles
Some walls, doors and barriers can only be got past by the right class. Look at one and a hint above the crosshair says what it is, how hard it is and who can deal with it. The right class right-clicks it and rolls a d20 on the [skill check HUD](d20-skill-checks.md). Everyone else has a fallback that costs something, or has to bring the right adventurer.

So far there are the **Lesser** and **Greater Arcane Seals** (Wizards, Warlocks) the **physical obstacles** for Barbarians, Fighters and Monks (Collapsed Passages, Heavy Boulders, Portcullises with their winches, Chasms and Crumbling Ledges), and the **divine and death obstacles** for Clerics, Paladins, Necromancers and Blood Hunters (Holy Wards, Desecrated Altars, Dread Wards, Ossuary Gates, Crypt Sleepers and Blood Seals), the **hidden and locked** obstacles for Rangers and Rogues (Hidden Doors, Locked Doors, and the vanilla temple traps as hidden traps), the **nature and song** obstacles for Druids and Bards (Guardian Beasts, Thornwalls and Singing Doors), and the **craft and element** obstacles for Artificers, Alchemists and Warlocks (Clockwork Doors, Alchemical Seals, Miasma Vents and Fire Curtains). More kinds are planned on the same framework.

![A Wizard's Arcana roll shattering a Lesser Arcane Seal](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/obstacle-framework/obstacle-wizard-open.png)

## Catalogue
Every obstacle type, who can roll for it and what anyone else can do. "(only)" marks a class's exclusive: the class is the obstacle's only primary solver. Every class has at least one, and `ObstacleCodexTest` fails the build if one doesn't (see [For developers](#for-developers)). The same list is the **Obstacle Codex** chapter at the back of the [Class Guidebook](class-guidebook.md).

- **Primary**: the classes it's built for, named first in hints. **Secondary**: classes that can also try. **Anyone else**: "May try" means every other class can work it too (a Blood Seal's 8-heart price, searching for a Hidden Door); "Can't try" means they need the fallback.
- **Check** "None" means there's no roll: getting past it is movement (a Monk's leap across a Chasm, a Paladin walking through a Dread Ward) or a plain right-click.

<!-- obstacle-table:start - generated from the obstacle registry (ObstacleTypes) and en_us.json by ObstacleCodexTest. Run ./gradlew generateObstacleTable -->
| Obstacle | Check | Primary | Secondary | Anyone else | Fallback |
|-|-|-|-|-|-|
| **Lesser Arcane Seal**<br>`dndclasses:lesser_arcane_seal` | Arcana | **Wizard** (only) | Warlock | Can't try | Anyone can mine it, but it lashes back |
| **Greater Arcane Seal**<br>`dndclasses:greater_arcane_seal` | Arcana | **Wizard** (only) | - | Can't try | It can't be broken by hand |
| **Collapsed Passage**<br>`dndclasses:collapsed_passage` | Athletics | Barbarian, Fighter | Paladin | Can't try | Anyone can dig through it, if the roof holds |
| **Heavy Boulder**<br>`dndclasses:heavy_boulder` | Athletics | **Barbarian** (only) | - | Can't try | It can't be mined, but one TNT would shift it (loudly) |
| **Portcullis**<br>`dndclasses:portcullis` | None | **Fighter** (only) | - | Can't try | Mine through two of its bars (slow) |
| **Portcullis Winch**<br>`dndclasses:portcullis_winch` | Athletics | Barbarian, Fighter, Paladin | - | May try | Mine through two of the portcullis bars (slow) |
| **Chasm**<br>`dndclasses:chasm` | None | **Monk** (only) | - | Can't try | An ender pearl, or climb down and out the hard way |
| **Bridge Winch**<br>`dndclasses:bridge_winch` | None | - | - | May try | Get across the chasm first |
| **Crumbling Ledge**<br>`dndclasses:crumbling_ledge` | None | - | - | Can't try | Keep moving, and don't look down |
| **Holy Ward**<br>`dndclasses:holy_ward` | Religion | **Cleric** (only) | Paladin | Can't try | Douse it with 3 thrown Potions of Healing (holy water) |
| **Desecrated Altar**<br>`dndclasses:desecrated_altar` | Religion | **Cleric** (only) | - | Can't try | Smash it, but the dead will rise and the spawners stay on |
| **Dread Ward**<br>`dndclasses:dread_ward` | None | **Paladin** (only) | - | Can't try | Anyone else can push through, frightened and slow |
| **Ossuary Gate**<br>`dndclasses:ossuary_gate` | Intimidation | **Necromancer** (only) | - | Can't try | Smash it, and fight the bones that rise |
| **Blood Seal**<br>`dndclasses:blood_seal` | None | **Blood Hunter** (only) | - | May try | Anyone can pay 8 hearts, shared by up to 3 players |
| **Hidden Door**<br>(also Hidden Deepslate Door, Hidden Blackstone Door)<br>`dndclasses:hidden_door` | None | **Ranger** (only) | - | May try | Search the walls (sneak + right-click) to find it |
| **Locked Door**<br>`dndclasses:locked_door` | Lockpicking | **Rogue** (only) | - | Can't try | Find its key, or tear the iron door down (slow and loud) |
| **Guardian Beast**<br>`dndclasses:guardian_beast` | Animal Handling | Bard, Druid | - | Can't try | Fight your way past them |
| **Thornwall**<br>`dndclasses:thornwall` | Nature | **Druid** (only) | - | Can't try | Burn it (flint and steel), or cut it with shears or an axe (it bites back) |
| **Singing Door**<br>`dndclasses:singing_door` | Performance | **Bard** (only) | - | Can't try | Touch it to hear its tune, then play it back on the note blocks |
| **Clockwork Door**<br>`dndclasses:clockwork_door` | Tinker's Tools | **Artificer** (only) | Rogue | Can't try | Smash the mechanism, if no dwarf is watching |
| **Alchemical Seal**<br>`dndclasses:alchemical_seal` | Alchemist's Supplies | **Alchemist** (only) | - | Can't try | Break it, and ruin some of what it seals |
| **Miasma Vent**<br>`dndclasses:miasma_vent` | Alchemist's Supplies | **Alchemist** (only) | - | Can't try | Rogues shrug off its poison; anyone else can run through (poison can't kill) |
| **Fire Curtain**<br>`dndclasses:fire_curtain` | None | **Warlock** (only) | - | Can't try | Walk through with Fire Resistance (Transmute covers potion-immune classes) |
| **Fire Curtain Brazier**<br>`dndclasses:fire_curtain_brazier` | None | **Warlock** (only) | - | May try | Get through the fire curtain first |

| Class | Only they can handle |
|-|-|
| Barbarian | Heavy Boulder |
| Bard | Singing Door |
| Cleric | Holy Ward, Desecrated Altar |
| Druid | Thornwall |
| Fighter | Portcullis |
| Monk | Chasm |
| Paladin | Dread Ward |
| Ranger | Hidden Door |
| Rogue | Locked Door |
| Necromancer | Ossuary Gate |
| Warlock | Fire Curtain, Fire Curtain Brazier |
| Wizard | Lesser Arcane Seal, Greater Arcane Seal |
| Artificer | Clockwork Door |
| Blood Hunter | Blood Seal |
| Alchemist | Alchemical Seal, Miasma Vent |
<!-- obstacle-table:end -->

## How it works
### Arcane Seals
| | Lesser Arcane Seal | Greater Arcane Seal |
|-|-|-|
| Looks like | Translucent violet glyph wall | The same, with a gold rim |
| Check | Arcana | Arcana |
| Who can dispel it | Wizard (primary), Warlock (secondary) | Wizard only |
| Failed roll | 2 magic damage | 2 magic damage |
| Natural 1 | 4 magic damage and Weakness I for 10 seconds; raises an alarm | Same |
| Fallback | Mine it: hardness 50 (like obsidian, about 9.4 s per block with a diamond pickaxe). Each block you break deals 6 magic damage and Weakness I for 30 seconds, and drops nothing | None. It can't be broken in survival |

Nobody can dispel a seal while under the Beholder's Anti-Magic. Seals glow at light level 7 while sealed. TNT, withers and the Ender Dragon can't break them, and pistons can't move them.

### Physical obstacles (Barbarians, Fighters, Monks)
| | Collapsed Passage | Heavy Boulder | Portcullis Winch |
|-|-|-|-|
| Looks like | A plug of rubble (grey stones in dark grit) | A mossy granite boulder (paint 2x2x2) | A wooden winch drum with an iron wheel, beside an iron portcullis |
| Check | Athletics | Athletics | Athletics |
| Who | Barbarian, Fighter (primary), Paladin (secondary) | Barbarian only | Anyone; Barbarians, Fighters and Paladins (the Tanks) are its primaries |
| Success | The whole plug clears in a burst of dust | The boulder grinds aside (no collision) | The portcullis rises for 4 seconds (80 ticks), or 5 seconds (100 ticks) for a Tank, then drops |
| Failed roll | Hunger I for 10 s and Slowness I for 3 s | Slowness II for 5 s | Nothing: the winch won't turn |
| Natural 1 | Cave-in: 3 damage; alarm | Crushed: 4 damage; alarm | The handle whips back: 2 damage (no alarm) |
| Fallback | Mine it: hardness 30 (about 5.6 s a block with a diamond pickaxe). Each block mined has a 25% chance to bring gravel down for 3 damage | Can't be mined, but its blast resistance is 6 (like cobblestone): one TNT (or creeper) that breaks any block of it clears the whole boulder, and raises an alarm | Mine through the portcullis bars: hardness 50 per bar (like obsidian) |

- **Rage:** a raging Barbarian rolls Athletics with advantage (two dice, keep the higher). This comes from the [character sheet](ability-scores.md), so it covers every Athletics obstacle.
- **Wedge (Fighters only):** while the portcullis is up, a Fighter who succeeds at the winch again jams it. The gate stays up for good and the winch shows as wedged. Resetting the winch (`/dndobstacle reset`) drops the gate again.
- Only the first raise of a winch earns class XP, so cranking it over and over isn't a way to level. A wedge earns nothing more: the winch gives its XP once, until it's reset (`/dndobstacle reset`).
- The **Portcullis** itself has no check and takes no clicks (right-clicking it does nothing). Its hint says its winch raises it and that a Fighter can wedge it; a Fighter sees that line in green, and anyone else's hint names a Fighter nearby to ask. The wedge makes the Fighter its only primary solver (their exclusive in the catalogue). A winch works the nearest portcullis within 6 blocks, unless a structure or `/dndobstacle link` tells it which one.

**Chasm.** A gap with a pit under it, and a **Bridge Winch** on the far side. The chasm's top layer is its retracted bridge: invisible and with no collision, so you fall straight through, but it's a sealed obstacle, so nobody can build across it or pillar up within a block of it. There's no roll: whoever gets across right-clicks the bridge winch (with an empty hand) and the bridge (spruce planks) swings down for everyone. The first lowering earns the winch's tier XP. Chasms are meant to be 7 blocks long: a Monk's triple jump clears that, a sprint jump (about 4 blocks) doesn't. An ender pearl or a Rogue's 12-block Shadowstep also gets across, and falling in means climbing out of the pit the hard way. A bridge winch works the nearest chasm within 8 blocks unless linked. The Monk is the Chasm's only primary solver (their exclusive in the catalogue); the gap has no outline, so it shows no hint.

**Crumbling Ledge.** Ledge blocks that give way under you: each one crumbles 10 ticks (half a second) after a player first stands on it, or 40 ticks (2 seconds) for a Monk, and comes back 100 ticks (5 seconds) later once nothing is in the way. Sneaking doesn't help (standing on it is checked every tick). There's no roll and nothing to break; creative and spectator players don't set it off.

### Divine and death obstacles (Clerics, Paladins, Necromancers, Blood Hunters)
| | Holy Ward | Desecrated Altar | Ossuary Gate |
|-|-|-|-|
| Looks like | Pale stone with a gold sunburst glyph and gold rim, glowing (light 8) | A cracked blackstone altar with soul fire on top (light 10) | Stacked bone with a skull keystone |
| Check | Religion | Religion | Intimidation ("command the dead") |
| Who | Cleric (primary), Paladin (secondary) | Cleric only | Necromancer only |
| Success | The whole ward opens | Consecrated (gold cross, light 12): every mob spawner within 12 blocks stops for 10 minutes, and the Cleric and their party within 8 blocks are healed 8 health (4 hearts). After 10 minutes the altar desecrates itself again and the spawners wake; only the first consecration earns XP | The bones part |
| Failed roll | 2 magic damage | Wither I for 5 s | The skull cackles (no harm) |
| Natural 1 | 4 magic damage and Blindness for 3 s; alarm | Wither I, and 2 zombies rise beside it; alarm | 2 skeletons rise; alarm |
| Fallback | Can't be broken. Throw Potions of Healing at it (splash or lingering): each one bursting within 2.5 blocks of a sealed ward counts once, and the 3rd opens it. No XP | Smash it: hardness 5, pickaxe. The spawners stay on and 4 undead (2 zombies, 2 skeletons) rise | Smash it: hardness 2 (like bone), pickaxe. Breaking one block brings the whole gate down; 4 skeletons and 1 wither skeleton rise, and an alarm goes off |

**Dread Ward.** Black-bannered deepslate that fills the passage round it with dread. There's no roll and nothing to click. Its zone is every block within 2 of a sealed Dread Ward block (a 5x5x5 cube round a single block), so a builder paints the ward blocks into an archway's frame and the passage through it is the zone. Once a second, a player in the zone gets **Frightened** (half melee damage), **Slowness IV** and **Weakness II**, each for 1.5 seconds (30 ticks), so it wears off soon after you leave. A **Paladin** is immune, and so is every player within 6 blocks of a Paladin (any player, not just party members). Anyone else can still push through, slowly. It can't be broken; `/dndobstacle open` turns it off.

**Crypt Sleepers.** Dormant undead laid in alcoves: any mob with the command tag `dndclasses.sleeper` and its AI off (`NoAI`). A sleeper wakes for good, and goes for that player, when a living player comes within 5 blocks, when something attacks it (sunlight and drowning don't count), or when an obstacle alarm goes off within 16 blocks. **Deathly Escort:** a Necromancer never wakes them, and nor does any member of the Necromancer's `/party` within 6 blocks of them, so a Necromancer can lead the party past. Creative and spectator players never wake them. Put undead near you to sleep with `/dndobstacle sleep`, or summon one with `{NoAI:1b,Tags:["dndclasses.sleeper"]}`.

**Blood Seal.** A dripping red sigil door (light 4) that opens for a price in blood, not a roll. Right-click it with an empty hand:
- A **Blood Hunter** pays 4 health (2 hearts) and it opens, for class XP.
- Anyone else pays toward 16 health (8 hearts). Up to 3 players (not creative or spectating) within 4 blocks of the clicked block split it: each right-click pays one share, the price over the number of players there rounded up (16 alone, 8 each for two, 6 for three), never more than what's still owed. It opens once all 16 are paid. Paying it earns no XP. Payments so far are kept until the seal reseals or is reset.
- It's never lethal: a payment that would take you to 0 health is refused. The price comes straight off your health, so armour, absorption hearts and resistance don't change it. A player with 16 health or less can't pay it alone (a Wizard has 10).

### Nature and song obstacles (Druids, Bards)
| | Guardian Beast | Thornwall | Singing Door |
|-|-|-|-|
| Looks like | A dark wooden tether post with an iron ring and chain (slack once calmed), with wolves, a polar bear or an Owlbear round it | A wall of dark thorned vines with red berries | Stone blocks with a carved singing face (light 3), with note blocks (its keys) within 4 blocks |
| Check | Animal Handling (Druid) or Performance (Bard) | Nature | Performance |
| Who | Druid, Bard (both primary). A Bard holding any instrument (Lute, War Drum, Flute, Doss Lute) gets +2 | Druid only | Bard only, holding an instrument. An attuned Doss Lute (a bound instrument) adds +2 |
| Success | The beasts are calmed for 10 minutes (12000 ticks): they ignore players and step 5 blocks aside from the post. Then they go back on guard. A Bard's natural 20 on a post with an Owlbear makes the Owlbear their companion for a Minecraft day (24000 ticks) | The vines part for 60 seconds (1200 ticks), then regrow | The door opens at once |
| Failed roll | The beasts growl | Thorns: 2 damage | The door hums back off-key |
| Natural 1 | The beasts attack you; alarm | 4 damage and Slowness II for 3 s (no alarm) | The door shrieks; alarm |
| Fallback | Fight them. Once every bound beast is dead the post opens for good (no XP) | Burn it or cut it (below; no XP) | The tune puzzle (below; no XP) |

**Guardian Beast.** The obstacle block is the beasts' tether post. The first time a player comes within 48 blocks, every wolf, polar bear and Owlbear (`#dndclasses:guardian_beasts`) within 8 blocks of it that isn't tamed or a companion is bound to it, with the command tag `dnd_guardian.<x>_<y>_<z>` (so `@e[tag=dnd_guardian.3_150_6]` finds them). While the post is sealed, bound beasts walk back if they stray more than 6 blocks, growl at players within 7 blocks (every 3 s), and attack anyone (not creative or spectating) who comes within 3 blocks of the post. Calm them by right-clicking the post, or one of the beasts, with an empty hand or an instrument. The post itself is always solid; it can't be broken.

**Thornwall.** Fire and blades get anyone through:
- **Burn it:** flint and steel or a fire charge on the wall, or any fire or lava appearing next to it (a fireball, fire breath). It burns for 20 seconds: each second it may set the air round it alight (fire that spreads to nearby wooden props on its own) and sets anyone in or against it on fire for 4 s. Then it's gone until the obstacle is reset.
- **Cut it:** shears or an axe cut one block in 2 seconds (40 ticks) whatever the tool's tier, for 1 thorns damage per block and 1 durability. Nothing else can break it. Cut blocks stay open until the wall regrows after a Druid parts it, or it's reset.

**Singing Door.** Right-clicking it with an empty hand (anyone, Bards too) makes it sing its phrase: 4 notes, each played on one of its keys (a note particle shows which), 12 ticks apart. Play the phrase back on the keys, in order, with no more than 200 ticks between notes, and the door opens. A wrong key plays an off-key note and starts the phrase over. Right- or left-clicking a key of a sealed door only plays it: its pitch doesn't change and it doesn't start breaking (creative players can sneak to edit keys as usual). The keys are the note blocks within 4 blocks of the door (up to 8), numbered in order of x, then z, then y. The phrase is drawn from the door's `GroupSeed` and position the first time it sings, and saved in `Data.Tune` (key numbers), so a DM can set it with `/data merge block <pos> {Data:{Tune:[I;2,0,3,1]}}` on each block. A door with fewer than 2 keys can't sing.

### Hidden and locked (Rangers, Rogues, Artificers)
**Hidden Door.** A door that looks exactly like the wall it's in: stone bricks (`hidden_door`), deepslate bricks (`hidden_deepslate_door`) or polished blackstone bricks (`hidden_blackstone_door`), shaded like a full block. Until you've found it, it's just a wall: no crosshair hint, and right-clicking does nothing. It can't be broken, and it has no protected shell (a refused block would give it away).
- **Rangers notice them passively.** Once a second, every Hidden Door within 6 blocks of a Ranger is checked against the Ranger's passive score: 10 + the better of their Perception and Investigation bonuses. A Ranger at class levels 0-4 has a passive 14 (WIS 14 +2, proficient +2), so they notice Easy (DC 10) and Medium (DC 13) doors on their own, but not Hard (15) or Very Hard (18) ones.
- **Dwarves sense them.** A sneaking Dwarf's [Stonecunning](../races/passive-traits.md) checks every Hidden Door within 8 blocks once a second, like a Ranger's passive check but with +5 (advantage) on the passive score, and finds those whose DC it meets.
- **Anyone can search.** Sneak and right-click the side of any plain full block (a wall, not the floor or ceiling) with both hands empty. That rolls Investigation ("Search the Wall" on the HUD) on a 5 second cooldown, and finds every Hidden Door within 3 blocks of the block you clicked whose DC the total meets (a natural 20 finds them all, a natural 1 nothing). The roll shows whether or not there's a door. Blocks that do something when used (crafting tables, note blocks and the like), and anything with a block entity (chests, trap tiles), don't count as walls. The Search key (V) also finds Hidden Doors within 8 blocks, using the better of Perception and Investigation.
- **Found for the whole party.** A door you find is revealed to you and to every member of your `/party`, and they're told who found it. It's outlined in gold dust for everyone who knows about it (within 16 blocks), and its crosshair hint appears: "Found! Anyone can open it now".
- **Opening.** Anyone who knows about it right-clicks it with an empty hand: no roll. It slides open (no collision).
- **XP:** whoever finds it earns the tier's class XP (Easy 5 to Very Hard 25), once until it's reset. Opening it earns nothing.

**Locked Door.** An iron door with a brass keyhole. Only a **Rogue** can pick it: a Thieves' Tools roll (shown as "Lockpicking", like a locked chest) against the obstacle's tier DC, with the usual take-your-time option.

| | Locked Door |
|-|-|
| Check | Thieves' Tools (the sheet's bonus: DEX modifier + expertise for a Rogue) |
| Who | Rogue only |
| Failed roll | The lock holds |
| Natural 1 | The pick snaps (a 5 second wait); no alarm |
| Fallback | Tear it down: hardness 50 (like obsidian). Breaking one block brings the whole door down, and raises an alarm. A key from elsewhere in the structure is meant to be the other way in, once dungeons and quests hand keys out |

**Lock glint.** Rogues and Rangers see a glint of gold dust on every locked chest within 6 blocks (every half second), so they know which chests are locked before trying them.

**Hidden traps.** The [dungeon traps](dungeon-traps.md) (darts, flame vents, pits) are this ticket's hidden traps: a trap one member of your `/party` has spotted is outlined for all of you, an Artificer can disarm them as well as a Rogue, and disarming one gives up its parts and class XP once per dungeon reset. The **vanilla temple traps** count too:

| | Desert pyramid | Jungle temple |
|-|-|-|
| Trigger | The stone pressure plate in the treasure room | The tripwire hooks and string |
| Payload | The TNT under the floor | The arrow dispensers |
| Spotted by | Rogues and Rangers always; anyone else with a passive Perception of 13 or more; everyone in the party of someone who has spotted it. Within 6 blocks of a trigger, outlined in orange for that player only | Same |
| Disarm | Rogue (primary) or Artificer (secondary): sneak + right-click a trigger with an empty hand. Thieves' Tools against DC 13, "Disarm" on the HUD | Same |
| Success | The TNT turns to sandstone; you get a redstone dust. 10 class XP, once (it stays disarmed) | The dispensers turn to mossy cobblestone (their arrows gone) and the string and hooks come away; you get 2 arrows. 10 class XP, once |
| Failed roll | Nothing; try again after 1.5 s | Same |
| Natural 1 | The TNT is lit (2 second fuse) | Every dispenser looses an arrow at you |

Only temples generated after this change are registered (a mixin on each temple piece's `generate` records their trap blocks as each chunk generates). `/place structure` counts.

### Craft and element (Artificers, Alchemists, Warlocks)
| | Clockwork Door | Alchemical Seal | Miasma Vent |
|-|-|-|-|
| Looks like | A brass plate with a gear and rivets | Translucent amber resin (light 3) over a cache | An iron grate glowing green, breathing gas (light 4) |
| Check | Tinker's Tools | Alchemist's Supplies | Alchemist's Supplies |
| Who | Artificer (primary), Rogue (secondary) | Alchemist only | Alchemist only |
| Each try uses up | 1 iron ingot and 1 redstone dust, from anywhere in your inventory | 1 potion | 1 potion |
| Success | The gears catch and the whole door opens | The resin dissolves | The vent stops for good (the grate stays, dark) |
| Failed roll | Sparks: 1 damage | It fizzes (no harm) | It belches gas at you: Poison II and Nausea for 10 s |
| Natural 1 | The gears jam: nobody can work it for 30 s (600 ticks), and an alarm | Acid: 3 magic damage and Poison I for 5 s; alarm | It bursts over everyone within 5 blocks (Poison II and Nausea for 10 s); alarm |
| Fallback | Smash the mechanism with a pickaxe: hardness 50 (like obsidian). Breaking any block opens the whole door (the broken block stays in place, open, so a reset rebuilds the whole door), but any Mountain Dwarf who can see you takes it as breaking their fortress's things: they turn on you and your standing with them drops (a Dwarf gets one warning a day) | Break it with a pickaxe: hardness 3. The whole seal shatters (the broken block stays in place, open, so a reset puts the whole seal back), and every chest, barrel or other loot container touching it (diagonals too) rolls its loot and loses each stack with a 40% chance, exactly like a smashed locked chest | Run it. Poison can't kill, and Rogues are immune to it |

- **Holding the cost.** Right-click with an empty hand or with what it takes in hand (an iron ingot or redstone dust for the door, any potion for the seal and the vent). The cost is paid as the roll (or a take-your-time focus) starts, so a try that's refused (wait, jammed door, no potion) costs nothing. "Any potion" is any brewed potion, splash or lingering, Awkward Potions included, but not a water bottle. The one in your hands goes first, else the first in your inventory.
- **Glued shut.** While an Alchemical Seal is sealed, a container with the resin on any side can't be opened in survival ("Amber resin glues it shut").
- **The gas.** Once a second, a player whose body is within 5 blocks of a venting Miasma Vent gets Poison II for 3 seconds and Nausea for 7 seconds, so it wears off soon after they leave. Rogues breathe it without harm (no nausea either); an Alchemist with Iron Stomach shrugs off both too. Vents can't be broken.

**Fire Curtain.** A doorway of eternal soul fire (light 10), with a **Fire Curtain Brazier** (light 12) on the far side. There's no roll and nothing to click on the curtain:
- A **Warlock** walks straight through it, unburnt, and right-clicks the brazier with an empty hand: it gutters out, the whole curtain dies for everyone, and the first dousing earns the brazier's tier XP.
- **Fire Resistance** lets anyone else through too (and they can douse the brazier). A Fighter, Paladin or Artificer can't drink or be splashed with the potion, but an Alchemist's **Transmute** cloud of Fire Resistance still covers them, because it's an ability. Creative and spectator players pass, and so do fire-immune mobs.
- To everyone else it's a wall: it stops you, and every half second you're up against it you take 2 fire damage and catch fire for 3 seconds.
- Water doesn't put it out (it isn't a fire block, and water can't flow into it), and it never burns out. It can't be broken or blown up. `/dndobstacle open` puts it out; resetting the brazier relights the brazier and its curtain.
- A brazier works the nearest Fire Curtain within 8 blocks, unless a structure or `/dndobstacle link` tells it which one.

### The roll
Right-click a sealed obstacle with an empty main hand. If your class can't attempt it, the action bar says who can ("Lesser Arcane Seal (Hard). A Wizard or Warlock could dispel it.").

The roll is a check of the obstacle's skill (Arcana for seals, Athletics for the physical obstacles, Religion for Holy Wards and Desecrated Altars, Intimidation for Ossuary Gates, Thieves' Tools for Locked Doors, Animal Handling or Performance for Guardian Beasts, Nature for Thornwalls, Performance for Singing Doors, Tinker's Tools for Clockwork Doors, Alchemist's Supplies for Alchemical Seals and Miasma Vents) against its DC: d20 + your [character sheet](ability-scores.md) Arcana bonus (INT modifier + proficiency + any flat bonuses), with any advantage the sheet gives obstacle checks:

| Tier | DC | Class XP for the solver |
|-|-|-|
| Easy | 10 | 5 |
| Medium | 13 | 10 |
| Hard | 15 | 15 |
| Very Hard | 18 | 25 |

Your class decides whether you may try (Wizards and Warlocks for a Lesser seal, only Wizards for a Greater one); the sheet decides how good you are. Both classes are proficient in Arcana. With the class scores alone, a Wizard (INT 15) rolls +4 and a Warlock (INT 12) +3 at class levels 0-4, one more at 5-8 (proficiency +3) and two more at 9-10 (+4). Race bonuses and admin overrides on the sheet count too. The crosshair hint shows the same bonus.

- **Success:** the roll shows, and 1.3 seconds (26 ticks) later the whole seal opens: every touching block of the same seal, up to 48. Open seal blocks have no collision and leave a faint broken frame. The solver gets the class XP above.
- **XP once per reset:** an obstacle group pays out its class XP only once. If it reseals on a timer (`/dndobstacle reseal`) or is worked again, solving it again earns nothing until it's reset (`/dndobstacle reset`, or a dungeon reset).
- **Failure:** the seal's sting (above) and a 1.5 second wait before you can try again.
- **Natural 1:** the fumble sting, a 5 second wait, and an alarm that will wake dormant monsters within 16 blocks once dungeons use it.

A natural 20 always succeeds and a natural 1 always fails.

### Take your time
Sneak and right-click instead to work at it slowly. You need no hostile mob within 16 blocks, and you must stand still for 8 seconds; moving or taking damage breaks your concentration. It then resolves as 20 + your bonus for the skill, with no crit, no fumble and no sting. It isn't allowed on Very Hard obstacles. Rolling stays the fast option in a fight.

### Protected volume
Obstacles can't be dug or built round. In survival, nobody can place a block in, or break a non-obstacle block in, the 1-block shell round a sealed obstacle. The action bar says why.

### Creative and spectator
Creative players open obstacles by right-clicking (no roll, no XP) and ignore the protected volume, so DMs can test. Spectators ignore obstacles.

## Where to find it
Structures generated after this change have these (only the goblin camp's post is critical):

| Structure | Obstacle | Tier | Behind it |
|-|-|-|-|
| [Goblin camp](../structures/goblin-camps.md) | Guardian Beast post in front of the chief's tent, 2 wolves bound to it | Easy, **critical** | The chief's tent. Fallback: fight the wolves |
| [Dwarven fortress](../structures/dwarven-fortresses.md) treasury | Clockwork Door filling the 3x3 doorway | Hard | The treasury. Smashing it angers the dwarves who see it |
| Dwarven fortress mine | 2x2x2 Collapsed Passage in a back corner | Medium | An ore pocket with a mine chest |
| Dwarven fortress forge | Alchemical Seal on a barrel in a back corner | Medium | Reagents (`chests/dwarven_fortress_reagents`) |
| [Dragon lair](../structures/dragon-lairs.md) and [frost lair](../structures/frost-lairs.md) | 2x2x2 Heavy Boulder in a cairn against the rim, north or south of the nest | Hard | A second hoard chest |
| [Beholder lair](../bosses/beholder.md) | 2x2 Greater Arcane Seal in the tunnel's north wall | Very Hard | A side vault with a hoard chest. The Beholder's Anti-Magic stops the dispel, so clear it first |
| Stronghold library ([Lich](../bosses/lich.md)) | Desecrated Altar in the middle row, 3 blocks from the phylactery spot | Hard | Consecrated, it quiets spawners within 12 blocks and the Lich's raised dead rise at half health |
| [Nether fortress](../structures/nether-fortresses.md) start crossing | 2x2x2 Fire Curtain into a nether-brick vault in one corner, with its brazier inside | Hard | Blaze rods (`chests/nether_fortress_cache`) |
| Vanilla mineshaft corridors (10% of those 2+ segments long) | 3x3x2 Collapsed Passage between two support frames | Easy | The rest of the corridor |
| Vanilla stronghold corridors (15% of the 5x5x7 ones) | 1x2 Hidden Door in a side wall without an exit | Hard | A 5x5 closet with a stronghold corridor chest |

Hobbit villages and the tavern get none.

- [Dungeon gates](dungeon-puzzles-and-gates.md): a dungeon's main-path gate can be a Collapsed Passage, a Holy Ward or an Ossuary Gate, and its side vault can be sealed by a Heavy Boulder or a Blood Seal (dungeons generated after each change).
- Operators can place obstacles with `/dndobstacle place`, and creative players can take the blocks from the DnD Classes creative tab (hand-placed obstacles are Medium). The Chasm Bridge item places the invisible retracted bridge.

## Commands
Operators (permission level 2):

| Command | What it does |
|-|-|
| `/dndobstacle place <from> <to> <type> <tier> [critical]` | Fills the box with one sealed obstacle group (at most 48 blocks). Types: `dndclasses:lesser_arcane_seal`, `dndclasses:greater_arcane_seal`, `dndclasses:collapsed_passage`, `dndclasses:heavy_boulder`, `dndclasses:portcullis`, `dndclasses:portcullis_winch`, `dndclasses:chasm`, `dndclasses:bridge_winch`, `dndclasses:crumbling_ledge`, `dndclasses:holy_ward`, `dndclasses:desecrated_altar`, `dndclasses:dread_ward`, `dndclasses:ossuary_gate`, `dndclasses:blood_seal`, `dndclasses:hidden_door`, `dndclasses:hidden_deepslate_door`, `dndclasses:hidden_blackstone_door`, `dndclasses:locked_door`, `dndclasses:clockwork_door`, `dndclasses:alchemical_seal`, `dndclasses:miasma_vent`, `dndclasses:fire_curtain`, `dndclasses:fire_curtain_brazier`, `dndclasses:guardian_beast`, `dndclasses:thornwall`, `dndclasses:singing_door`. Tiers: `easy`, `medium`, `hard`, `very_hard` |
| `/dndobstacle info <pos>` | Type, tier, DC, state, group size and who can attempt it |
| `/dndobstacle open <pos>` | Opens the whole group (no XP) |
| `/dndobstacle reset <pos>` | Reseals the whole group and clears its progress: a Holy Ward's potion count, a Blood Seal's payments, who has found a Hidden Door, a Guardian Beast post's bound beasts (it binds the beasts round it again), a Thornwall's burn, a jammed Clockwork Door, and the "already earned XP" mark every group carries |
| `/dndobstacle settier <pos> <tier>` | Retiers the whole group |
| `/dndobstacle reseal <pos> <ticks>` | The group reseals itself this long after opening (0 = never, the default) |
| `/dndobstacle link <winch> <target>` | Connects a portcullis winch to a portcullis block, a bridge winch to a chasm block, or a fire curtain brazier to a fire curtain block |
| `/dndobstacle traps` | Lists the vanilla temple traps within 256 blocks (trigger position, armed or disarmed) |
| `/dndobstacle traps tp` | Teleports you to a safe spot beside the nearest temple trap trigger |
| `/dndobstacle list [radius]` | Lists obstacle groups near you (default 64 blocks) |
| `/dndobstacle template <template> <pos> [<tier> \| depth <n>]` | Places a structure template through the `dndclasses:obstacle_tier` processor, retiering its obstacles (see [Obstacles in structures and templates](../dev/obstacle-templates.md)) |
| `/dndobstacle sleep [radius]` | Lays every undead mob within the radius (default 8) down as a Crypt Sleeper |
| `/dndobstacle wake [radius]` | Wakes the Crypt Sleepers within the radius (default 8) |

## Configuration
None.

## Known limitations
- Retry timers and take-your-time focus are kept in memory, so they reset when the server restarts.
- The protected volume only stops block items. Buckets can still pour fluids next to a seal.
- Breaking a non-obstacle block in the protected volume is refused by the server, so the client briefly shows the block breaking before it comes back.
- The "Ask <name>" line only knows players whose class the client has been told about.

- Chasm width is set from the jump physics, not measured in game yet: a Monk's triple jump should clear 7 blocks and a sprint jump shouldn't. Speed potions or other movement abilities may change that.
- A Heavy Boulder that "rolls aside" just loses its collision and model; it doesn't move 3 blocks as a block.
- A Crumbling Ledge's reform tick still runs if an operator resets the ledge with `/dndobstacle reset` in the meantime, so it can crumble once more on its own.
- A Desecrated Altar only quiets spawner blocks. Dungeon or structure spawning that doesn't use a spawner block isn't affected.
- Crypt Sleepers have no crosshair hint, and a Bard gets no special way past them yet.
- Dread Wards apply their effects to players only; mobs in the zone aren't frightened.
- Hidden Doors are unbreakable, so mining at a wall can still give one away if the blocks round it break normally. Who has found a door is kept on the door, so it survives restarts; who has spotted a temple trap is kept in memory and is found again on the next pass.
- A Hidden Door found before you joined a party stays hidden from your new party members until one of them finds it too.
- The wall search uses the character sheet's Investigation bonus, with no extra for Utility characters. Party members do get Inspire and Assist on obstacle rolls ([Party synergies](party-synergies.md)).
- Locked Doors have no key yet: the key item belongs to the dungeon and quest work.
- Temple traps can't be found with the Search key (V), and a jungle temple's two tripwires count as one trap: disarming either disarms both.
- The crosshair hint's Guardian Beast and Singing Door line shows the sheet bonus without the +2 instrument bonus; the roll itself adds it.
- A Singing Door's phrase progress is kept in memory and shared by everyone at the door, so two players playing at once interleave their notes.
- A calmed Guardian Beast ignores players even if one of them hits it; it fights back again once the post reseals.
- An Owlbear won at a post follows the Bard as an Animal Friends companion, outside the companion cap. When its day is up it goes wild again (it's still an Owlbear).
- Thornwalls, Singing Doors, Holy Wards, Dread Wards, Ossuary Gates, Blood Seals, Miasma Vents, Portcullises and Chasms aren't placed in any existing structure; dungeons and `/dndobstacle place` use them.
- Structures generated before this change keep their old layout. One that was half generated gets its obstacles only in the chunks generated since.
- A stronghold corridor's hidden closet is only built where it fits: clear of the stronghold's other rooms and in chunks the corridor itself generates in. Corridors near a chunk corner rarely get one.
- A goblin camp's guard wolves only keep to their post while a player is within 48 blocks of it, so they can wander off in between.
- The walls round a side vault or cache are ordinary blocks: you can dig round an obstacle from further out than its 1-block protected shell.
- Burrow (a crawl tunnel for a Druid in a small Wild Shape form) isn't built: it needs the Identity mod's small forms.
- An Alchemical Seal over a locked loot chest still leaves the chest locked: a Rogue picks it once the resin is gone (or it's smashed open with the usual ruin). Breaking the resin rolls and ruins the cache straight away, so its chest is no longer locked.
- If your Fire Resistance runs out while you stand inside a Fire Curtain, the curtain is solid round you and you may have to push your way out.
- Fire Curtain collision for players is decided on each side from the class, Fire Resistance and game mode both sides know; a Fire Resistance that ends at the very moment you step in can make you rubber-band.
- A wedged portcullis that an operator reseals directly (rather than through its winch) leaves the winch showing as wedged until the winch is reset too.

## For developers
- Package `classes/Obstacles/`:
  - `ObstacleType` (one kind of obstacle: skill, `eligibility(DndCharacter)`, stings, fallback, effects) and `ObstacleTypes`, the registry of types, their blocks and items, and the shared `dndclasses:obstacle` block entity type. `ArcaneSealType` is both seals.
  - `ObstacleBlock` has `state=sealed|open`. `ObstacleBlockEntity` stores `Tier`, `Critical`, `ResealTicks`, `OpenedAt`, `GroupSeed` and a per-type `Data` compound, and syncs to clients for the hint.
  - `ObstacleGroups` flood-fills touching blocks of the same type and `GroupSeed` (cap 48) to open, reseal and retier them together.
  - `ObstacleInteractions`: right-click, retry timers, the 26-tick pending open, take your time and the protected volume.
  - `ObstacleIndex` (`PersistentState`, per dimension): every obstacle block by chunk, filled as block entities load and emptied when the block is removed.
  - `ObstacleEvents.SOLVED` (quests) and `ObstacleEvents.ALARM` (dungeons).
  - `ObstaclePlacer`: the worldgen API. `place`, `doorway` and `box` only write blocks inside the `chunkBox` you pass, like chest loot in `LairPiece`.
  - `StructureObstacles` (position-only group seeds, pre-bound Guardian Beast posts) and `ObstacleTierProcessor` (`dndclasses:obstacle_tier`, for NBT templates and jigsaw pieces). See [Obstacles in structures and templates](../dev/obstacle-templates.md).
  - Placements: `FortressPiece.Builder.obstacle` (`TreasuryPiece`, `MinePiece`, `ForgePiece`), `GoblinCampPiece`, `LairPiece.sideCache`, `BeholderCavernPiece.sideVault`, and the mixins `StrongholdLibraryMixin` (altar), `NetherFortressStartMixin` (curtain vault), `MineshaftCollapseMixin` and `StrongholdHiddenClosetMixin`. `LichEntity.castRaiseDead` asks `DesecratedAltarType.consecratedNear(world, pos, 24)`.
- `ObstacleType.skill()` returns an `Abilities.Skill` (`ARCANA` for the seals). `ObstacleInteractions` rolls `SkillCheck.builder(player, type.skill(), tier.dc, "obstacle")`, and take your time rolls the same builder with `.take(20)` (a fixed natural that is never a crit or a fumble).
- `ObstacleType.eligibility(DndCharacter)` returns `SkillChecks/Eligibility` (`PRIMARY`, `SECONDARY`, `UNTRAINED`, `NONE`). It only decides who may try and the order classes are named in hints; it doesn't change the modifier.
- Anything that should change obstacle rolls (races, subclasses, magic items) adds an `AbilityContributor` with `AbilityScores.register(id, contributor)`; a contributor can grant advantage on rolls tagged `obstacle`.
- `Client/Hud/ObstacleHintHud` draws the crosshair hint.
- Physical obstacle types live in `classes/Obstacles/types/`: `CollapsedPassageType`, `HeavyBoulderType`, `PortcullisType`, `WinchType` (with `PortcullisWinchType` and `BridgeWinchType`; the target's position is `Data.Target` in the winch's block entity), `ChasmType` and `CrumblingLedgeType`. Each family adds a block of fields to `ObstacleTypes`, before the shared block entity type.
- `ObstacleType` hooks a type can override: `hardness()`, `blastResistance()`, `interactive()` (false: right-click passes), `solid(state)` (collision and outline per state), `ownWhoText()` (hint uses `<key>.who`), `solve(world, pos, solver)` (what success does and whether it earns XP; default opens the group), `scheduledTick`, `onExploded` and `registerEvents()`. A type with no skill that a class can try is worked with a right-click and no roll.
- `ObstacleGroups.openFor(world, pos, ticks)` opens a group on a timer (reseal ticks are checked every tick); `ObstaclePlacer.chasm(...)` lays a chasm bridge and `ObstaclePlacer.winch(...)` places a winch linked to its target.
- `dungeon/GateKind` has `COLLAPSED_PASSAGE`, `HOLY_WARD` and `OSSUARY_GATE` (main-path safe) and `HEAVY_BOULDER` and `BLOOD_SEAL` (strict, side vaults).
- Divine and death types: `HolyWardType` (potion count in the group's `Data.Doused`; `PotionEntityMixin` calls `onPotionBurst`), `DesecratedAltarType` (`MobSpawnerLogicMixin` cancels `serverTick` while an open altar is within 12 blocks, found through `ObstacleIndex`; it reopens on a 12000-tick `ObstacleGroups.openFor` timer), `DreadWardType` (per-second `END_WORLD_TICK` aura), `OssuaryGateType`, `BloodSealType` (payments in `Data.Paid`) and `RisenUndead.raise(...)` for undead that rise beside an obstacle. `Obstacles/CryptSleepers` holds the sleeper tag, `sleep(mob)`, `wake(...)` and `undeadAllied(player)`.
- Hidden and locked types: `HiddenDoorType` (one per look; who has found it is `Data.Revealed`, a list of UUID strings, synced to clients for the hint; `reveal(...)` adds the finder's whole party), `LockedDoorType`. `HiddenSearch` is the sneak + right-click wall search, `ObstacleAuras` the per-second Ranger passive check, door outlines and lock glint. `ObstacleBlockEntity` is a `Perceivable`, so a Search (V) finds hidden obstacles. `VanillaTraps` (a per-dimension `PersistentState`) holds the temple traps; `mixin/TempleTrapsMixin` fills it from `DesertTempleGenerator` and `JungleTempleGenerator`.
- New `ObstacleType` hooks: `hiddenFrom(world, pos, player)` (no hint, right-click passes), `disguised()` (shaded like a full block while sealed), `protectsShell()` and `rollLabel()` (the HUD label, e.g. Lockpicking).
- Nature and song types are registered in `Obstacles/NatureSongObstacles` (loaded by `ObstacleTypes` before the shared block entity type) and live in `types/`: `GuardianBeastType` (bound UUIDs in `Data.Beasts`, kills in `Data.Slain`; a per-second `END_WORLD_TICK` guard and calm pass; `UseEntityCallback` routes a right-clicked beast to its post through `ObstacleInteractions.use`; `MobEntityMixin.setTarget` asks `GuardianBeastType.blocksTarget`), `ThornwallType` (seconds of burning left in `Data.BurnLeft`, ticked with scheduled block ticks; cutting cancels the break in `PlayerBlockBreakEvents.BEFORE` and opens the one block) and `SingingDoorType` (keys and phrase; `UseBlockCallback` and `AttackBlockCallback` on note blocks). `BardCompanions.adoptFor(mob, player, ticks)` makes a companion that leaves after `ticks` (command tag `dnd_bard_companion_until:<world time>`).
- `ObstacleType` hooks added for them: `skillFor(player)` (a per-class skill; the hint uses it too), `accepts(player, held)` (which main-hand items work it), `intercept(...)` (handle a click without a roll), `rollExtras(builder, player)` (per-roll bonuses), `onRolled(...)` (sees the natural before the result plays out), `breakingDelta(player, vanilla)` and `neighborChanged(world, pos, from)`.
- Textures are made by `tools/nature_song_obstacle_textures.py`. `devscripts/obstacle-nature-song.txt` covers a Druid and a Bard calming beasts, a Bard's natural 20 winning an Owlbear, a post attacking a Fighter and opening when its wolf dies, a Druid parting a Thornwall (and its regrowth), a Fighter burning and cutting one, a Bard singing a door open and a Fighter playing a door's tune back.
- `ObstacleInteractions.solve` pays XP through `ObstacleGroups.claimReward`, which marks the group's `Data.XpEarned`; `ObstacleGroups.reset` clears it.
- `ObstacleType.opensWhenBroken()` (Clockwork Door, Alchemical Seal): `ObstacleBlock`'s `PlayerBlockBreakEvents.BEFORE` handler runs `onBrokenByHand` (which opens the group) and cancels the break, so the block stays, open, and `ObstacleGroups.reset` reseals it with the rest.
- `ObstacleGroups.dataInt` / `setDataInt` keep a per-type number on every block of a group. `ObstacleEvents.SOLVED` fires only for a solve that earns XP (not re-cranking a winch, paying a Blood Seal's fallback price, or re-consecrating an altar).
- Craft and element types: `ClockworkDoorType` (jam time in `Data.JammedUntil`), `AlchemicalSealType`, `MiasmaVentType` (per-second `END_WORLD_TICK` gas), `FireCurtainType` and `FireBrazierType` (a `WinchType` whose target is the curtain), registered in `types/CraftElementObstacles`, which `ObstacleTypes` loads from a static block before it builds the shared block entity type (each obstacle family can register from its own file this way, with the public `ObstacleTypes.register`, `solidSettings` and `glowSettings`). `types/Reagents` takes the iron, redstone and potions. `SkillChecks/LootRuin` is the shared 40% ruin of a forced container (`Lockpicking` uses it too). `MountainDwarfEntity.witness(player, pos)` has dwarves who can see it judge a broken piece of dwarven work.
- More `ObstacleType` hooks: `accepts(player, held)` (shared with the nature and song types: what the main hand may hold to work it), `payForAttempt(world, pos, player)` (uses up the cost as a roll or focus starts, or refuses) and `collides(state, shapeContext)` (per-entity collision; the Fire Curtain uses it). The crosshair hint shows a click-free obstacle's own text to the class it's for (a Paladin at a Dread Ward, a Warlock at a Fire Curtain).
- Textures are made by `tools/craft_obstacle_textures.py`; the Fire Curtain uses vanilla soul fire models. `devscripts/obstacle-craft-element.txt` covers an Artificer (no parts, a roll, a jam), a Rogue, a Fighter smashing a door in front of a dwarf, an Alchemist's seal (glued chest, no potion, a roll, acid), a Fighter breaking a seal over a loot chest, the Miasma Vent (Fighter, Rogue, Alchemist), and the Fire Curtain (a Fighter stopped and refused his potion, a Warlock walking through and dousing the brazier, and a Fighter under an Alchemist's Transmute walking through).
- Textures are made by `tools/divine_obstacle_textures.py`. `devscripts/obstacle-divine.txt` covers a Cleric, a Fighter's fallbacks, the Dread Ward, a Necromancer, a crypt sleeper and the Blood Seal.
- `Obstacles/ObstacleCodex` reads the registry: `exclusiveSolver(type)` (the only PRIMARY class, if any), `exclusives(types)`, `classesWithoutExclusive(types)`, and `entries()` (every type, minus look-alike `variants(type)` such as the deepslate and blackstone Hidden Doors). The guidebook's Obstacle Codex chapter (`ClassGuidebookScreen.codexPages`) is built from it.
- `src/test/java/.../Obstacles/ObstacleCodexTest` (JUnit through `fabric-loader-junit`, so it bootstraps Minecraft and the real registry; `./gradlew test`, part of `build`):
  - `everyClassHasAnExclusiveObstacle`: every `DndCharacter` but `NONE` is the sole PRIMARY of at least one type. A new class, or a type change that takes a class's last exclusive away, fails the build. `losingAnExclusiveFailsTheCheck` demotes each class in turn to prove the check notices.
  - `classInfoObstacleListsMatchTheRegistry`: each class's `obstacles` list in `class_info.json` names every type it's PRIMARY or SECONDARY for, and its exclusives in bold.
  - `docsTableMatchesTheRegistry`: the catalogue table above (between the `obstacle-table` markers) matches the registry and `en_us.json`. Regenerate it with `./gradlew generateObstacleTable`; don't edit it by hand.
- Textures are made by `tools/physical_obstacle_textures.py`. `devscripts/obstacle-physical.txt` covers a Barbarian, a Fighter, a Monk and a Wizard's solo fallbacks.
- Textures are made by `tools/obstacle_textures.py`. The Locked Door's comes from `tools/locked_door_texture.py`; Hidden Doors use the vanilla brick models.
- `devscripts/obstacle-hidden-locked.txt` covers a Fighter searching for a Hidden Door, a Ranger noticing one, a Locked Door (refused, picked, XP once per reset), an Artificer disarming and fumbling dungeon traps, the lock glint, and a freshly placed desert pyramid's plate spotted by a Ranger and disarmed by a Rogue. `devscripts/obstacle-party-traps-host.txt` and `-guest.txt` check that a found door and a spotted trap are shared with a party member over LAN.
- Every roll, sting, solve and broken focus logs an `[Obstacle]` line. `devscripts/obstacle-verify.txt` rolls 40 times on an Easy seal (successes, failures, crits, fumbles) and breaks a focus by moving and by damage.
- `devscripts/obstacle-arcane-seal.txt` covers a Fighter (hint, refusal, protected volume, mining with backlash), a Wizard (roll, take your time, XP, reset), a Warlock (take your time on Medium) and the Greater seal. It uses the DevScript step `mine on|off`, which keeps breaking the block at the crosshair.
