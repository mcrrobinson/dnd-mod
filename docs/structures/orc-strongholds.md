# Orc Strongholds
Bloodtusk Hold: a walled warcamp in the badlands, with a palisade and gatehouse, the Great Longhouse, a sunken Fighting Pit, a smithy and hide yurts. The Bloodtusk Horde's [orcs](../mobs/orcs.md) live here under their [War Chief](../bosses/orc-war-chief.md). It's the Half-Orc home (see [Racial homes](../races/racial-homes.md)): orcs attack everyone else on sight, and a Half-Orc can win their respect in the Trial of Strength.

## How it works
Every hold is a circle round its Fighting Pit, turned so its gate faces one of the four directions at random. The ground is levelled out to radius 25: everything up to 32 blocks above the floor is cut away and dips are filled with terracotta.

| Piece | What it is |
|-|-|
| Palisade | Radius 22. Dark oak and stripped dark oak logs 4 high, most topped with pointed dripstone stakes, a torch here and there, patched with red terracotta and blackstone. The floor inside is coarse dirt, red sand, packed mud, terracotta and gravel, with a packed-mud road out of the gate. |
| Gatehouse | Two 9-high watchtowers of dark oak, 4 blocks either side of a 5-wide gate. A ladder runs up the inside of each to a 5x5 archer platform with a rail. The gate's lintel is lined with skulls (a wither skeleton skull in the middle) and hung with lanterns. Red war banners with a black skull and border hang on both towers, inside and out. |
| Great Longhouse (hearth) | 21 by 11 at the back of the hold, its door facing the pit. Spruce plank walls on dark oak posts under an A-frame roof of spruce and dark oak stairs. Inside: a campfire in a blackstone fire pit, log benches, four war drums (note blocks on hay), lanterns hung from the ridge, a trophy wall of 12 mob heads on the back wall, and the **War Chief's seat**: a blackstone throne between two gold blocks on a polished blackstone dais, with a gold block, a wither skeleton skull and two war banners behind it. The **hoard** chest stands beside the seat. |
| Fighting Pit | In the middle. Round, radius 7, sunk 3 blocks, a sand floor, mud-brick walls and a dark oak fence round the rim, with mud-brick steps down on the gate side. The **war drum** stands on the far rim: a note block on a hay bale under a red banner. |
| Smithy | On one side of the pit: an open-sided shed (8 by 11) under a dark oak slab roof. A polished blackstone forge wall with a blast furnace and a furnace, a channel of lava behind iron bars, an anvil, a grindstone, a smithing table, the armoury chest and a barrel. |
| Yurts | 3 round hide tents, 9 across and 7 high: walls of brown, white and grey wool and brown terracotta, a conical roof round a spruce pole, the door facing the pit. Each holds a supply chest, two straw beds and a cauldron. |
| Totems and bone heaps | 2-4 between the buildings: a totem pole of logs with a carved face, fence arms and a skull, or a heap of bone blocks round skulls. |

### People
When the hold generates: 6-9 Orc Warriors round the yard (they roam within 18 blocks of the pit), an Orc Archer on each tower, the Orc Quartermaster in the smithy and the War Chief by his seat. Orc Warriors spawn naturally inside the palisade later, up to 12 orcs within 48 blocks (not on Peaceful). No monsters spawn inside a hold's box. See [Orcs](../mobs/orcs.md).

### Stance
- **Hostile** to players Hostile with the Bloodtusk Horde, which is everyone but Half-Orcs at first: orcs attack on sight within 16 blocks.
- **Barbarians** are respected: orcs only turn on a Hostile Barbarian within 8 blocks.
- **Tolerated**: players at Unfriendly or better with the horde (Half-Orcs start Neutral), players Blood-Sworn at this hold, and anyone in a [party](../systems/party.md) with a player Blood-Sworn at this hold while within 8 blocks of them.
- **Grudges**: striking an orc turns it and every orc within 16 blocks on you for 30-50 s. Opening the hoard (any chest in the Great Longhouse) where an orc within 16 blocks (24 if you're Unfriendly or worse) can see you does the same and costs 40 standing. There's no kin trust.
- Orcs also attack hobbits, dwarves, elves and villagers within 24 blocks of home. Goblins are their allies: goblins and orcs never target or hurt each other.

### Trial of Strength
For Half-Orcs, and for any player Honored with the Bloodtusk Horde.

1. Step down into the pit and right-click the war drum on its rim.
2. An Orc Champion leaps in on the far side and a one-on-one duel starts. A yellow duel bar ("Trial of Strength: &lt;Champion&gt;") shows the Champion's health to everyone within 32 blocks. The other orcs stand down and watch.
3. The duel is non-lethal: whoever drops to 3 health or below yields. Damage that would take either of you below 3 is stopped at 3, so you can't die in the pit.
4. You also lose if you go more than 12 blocks from the middle of the pit, log out, or the duel runs past 3 minutes.

| Result | What happens |
|-|-|
| Win | You become **Blood-Sworn** at this hold: its orcs tolerate you (and your party within 8 blocks), and its Quartermaster trades with you. You get an Orcish Greataxe, 100 class XP, +100 standing with the Bloodtusk Horde and the advancement **Blood and Thunder**. The title reads "Blood-Sworn". |
| Lose | "You yield". You can try again at this hold the next in-game day. |

The drum refuses with a message if you're not eligible ("Only kin of the horde may take the Trial."), already Blood-Sworn there, beaten there today, not down in the pit, at 4 health or less, or another Trial is under way at the hold. Blood-Sworn is kept per hold: a second stronghold has its own Trial.

### Loot
| Loot table | Found in | Contents |
|-|-|-|
| `chests/orc_stronghold_yurt` | yurt chests | 3-6 of: cooked mutton and beef, rabbit stew, leather, rabbit hide, bones, arrows, iron and gold nuggets, string, Grog |
| `chests/orc_stronghold_armory` | the smithy chest | 3-5 of: iron ingots, iron axe, sword and armour, a chainmail chestplate, crossbows, shields, arrows, coal; plus 8% an [Orcish Greataxe](../items/racial-weapons.md) |
| `chests/orc_stronghold_hoard` | the hoard beside the War Chief's seat | 3-5 of: 1-4 diamonds, 1-3 gold blocks, 2-6 gold ingots, a randomly enchanted book, a saddle, diamond horse armour, 2-6 emeralds; plus 10% a totem of undying |

The hoard is [locked](../systems/d20-skill-checks.md#lockpicking-rogue) at DC 15 and the armoury at DC 10. Yurt chests aren't locked. Each also has an extra [magic loot](../systems/magic-loot.md) pool: yurts 5% Uncommon, the armoury 15% (Uncommon or Rare), the hoard 60% (Uncommon 40, Rare 50, Very Rare 10).

## For your race
Strongholds are the Half-Orc home. A Half-Orc gets:

- a "Welcome home, Half-Orc" title on arriving
- **Rally**: Strength I for 30 s each time you're welcomed (on arriving, and again after 5 minutes away)
- Regeneration I in the Great Longhouse while no hostile mob is within 16 blocks
- 25% off the Quartermaster's trades
- Kin stock: the Quartermaster sells Half-Orcs the [Orcish Warplate](../items/armor.md#racial-armor) set (helmet 10, chestplate 16, leggings 14, boots 8 before kin prices)
- the Trial of Strength
- a Neutral (0) start with the Bloodtusk Horde, so the orcs tolerate you

## Where to find it
Badlands, Wooded Badlands and Eroded Badlands. Strongholds use a spacing of 28 chunks and a separation of 10. A site needs fairly level, dry ground: 32 samples on rings 8, 14, 20 and 24 blocks out may rise or fall at most 4, 6, 8 and 10 blocks from the middle, and at most 5 of them may be wet, too steep or outside the badlands. The floor is the median height of the samples. Strongholds don't take part in [goblin raids](../systems/goblin-raids.md): the goblins are their allies.

## Commands
- `/locate structure dndclasses:orc_stronghold`
- `/dndhome stronghold [pit|seat|hoard|smithy|gate]` (operators): teleports you to that spot of the nearest stronghold. `pit` (the default) puts you on the pit floor looking at the war drum, close enough to `use` it.
- `/dndhome strongholdsurvey [grid] [spacing]` (operators): finds the nearest stronghold from each point of a grid round you (default 3 x 3, 2000 blocks apart) and checks its middle is in the badlands and it has all six kinds of piece. Each logs `[StrongholdSurvey] stronghold at X Y Z: biome ..., N pieces (6 kinds): PASS` (or FAIL), then a summary.
- `/dndhome bloodsworn <players> <true|false>` (operators): sets or clears Blood-Sworn at the stronghold you stand in.
- `/dndhome trialreset <players>` (operators): forgets today's lost Trial at the stronghold you stand in.
- `/dndhome pieces` lists every piece of the stronghold you're in.

## Known limitations
- Only one Trial can run at a hold at a time, and a duel in progress is lost if the server stops (the Champion leaves when the hold loads again).
- Blood-Sworn is per hold, so a Blood-Sworn player's party is only safe near them at that hold.
- The archers on the towers hold their post and only shoot what they can see within 16 blocks.
- A tall hoodoo or mesa wall that reaches past 32 blocks above the floor is cut off flat, and steep edges outside the clearing are left as they are.
- Natural spawns only top up warriors, not archers.

## For developers
- `world/gen/stronghold/`. `OrcStrongholdStructure` checks the site and plans the pieces round the centre with a random gate direction (`plan`). `StrongholdPiece` is the shared base: local (a, y, b) coordinates with the gate to the south, turned with `BlockState.rotate`; every piece carries its hold key (the start chunk, `ChunkPos.toLong`) for the orcs it places. Pieces: `GroundsPiece` (clearing, palisade, totems, warriors), `GatehousePiece`, `LonghousePiece` (`hoard()`, `seat()`), `FightingPitPiece` (`drum()`, `floor(offset)`, `inPit(pos)`), `SmithyPiece`, `YurtPiece`. `OrcStrongholdStructures` registers them. `BloodSworn` is the per-world `PersistentState` (`dndclasses_blood_sworn`) of Blood-Sworn players and lost days per hold.
- `entity/OrcTrial`: the drum (`UseBlockCallback` on the pit's note block), the duel tick and bar, the win and loss, `clampPlayerHealth` (called from `PlayerEntityMixin.applyDamage`) and the quest hook `onTrialWon`. `entity/OrcHostility`: who orcs fight. `entity/OrcLaw`: the hoard grudge (`SettlementGrudges.BLOODTUSK_HORDE`).
- `RacialHomes.ORC_STRONGHOLD` (hearth: `OrcStrongholdStructures.LONGHOUSE`), the Rally in `HomeBonuses.welcome`, `Lockpicking` (hoard DC 15, yurts unlocked), `MagicLoot.placements`, `ModSpawns.canOrcSpawn`.
- Data: `worldgen/structure/orc_stronghold.json` (Orc Warriors in the `creature` override, an empty `monster` list), `worldgen/structure_set/orc_strongholds.json`, `#dndclasses:has_structure/orc_stronghold`, `#dndclasses:orc_strongholds`, `loot_tables/chests/orc_stronghold_*.json`, `factions/bloodtusk_horde.json`, `advancements/blood_and_thunder.json`, `dialogue/orc_quartermaster.json`, `dialogue/orc_war_chief.json`.
- Textures: `tools/orc_stronghold_textures.py` (orc skins, Grog, Goblin War Token).
- Devscripts: `orc-stronghold-survey.txt` and `orc-trial.txt`.
