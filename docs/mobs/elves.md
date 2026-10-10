# Elves
The folk of the [Elven Enclaves](../structures/elven-enclaves.md): Wood Elves, the Elf Wardens who guard them, and two merchants, the Speaker and the Fletcher. They're neutral to players until Sylvan Law is broken.

![The Fletcher, the Speaker, a Wood Elf and an Elf Warden](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/elven-enclave/elves-models.png)

## How it works
| Elf | Health | Speed | Carries | Behaviour |
|-|-|-|-|-|
| Wood Elf | 18 | 0.30 | nothing | Wanders up to 24 blocks from home by day and 8 at night, opens doors, runs from monsters |
| Elf Warden | 24 (2 armour) | 0.32 | a bow (Power I 30% of the time) and an iron sword; green leather tunic and boots | The guard: hunts monsters (not creepers) within 24 blocks of home |
| Speaker | 30 | 0.25 | a book | Merchant; keeps within 4 blocks of the Speaker's Hall |
| Fletcher | 20 | 0.28 | arrows | Merchant; keeps within 4 blocks of the archery glade |

Every elf has a Sindarin-style name such as "Aelar Galanodel", never despawns, and is a member of the **Sylvan Court** [faction](../systems/factions.md).

### Wardens
A Warden fights with its bow from 8-12 blocks off: it closes in when its target is farther or out of sight, backs away when it's nearer, strafes, and draws for 30 ticks (1.5 s) a shot. When a target gets within 3 blocks it switches to its sword. Its arrows can't be picked up and don't hurt other elves.

Wardens leave players alone unless the player hits an elf (every Warden within 16 blocks joins in) or breaks Sylvan Law where an elf can see (see [Elven Enclaves](../structures/elven-enclaves.md#sylvan-law)). They stay angry for 30-50 s. Helping to drive off a goblin raid on the enclave forgives you.

![A Warden drawing on a zombie](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/elven-enclave/warden-bow.png)

### Trades
Each trade has 12 uses and restocks every morning. Elves pay 25% less (kin prices: the first price item's count × 0.75, rounded down, at least 1).

| Merchant | Sells | Buys for 1 emerald |
|-|-|-|
| Fletcher | 16 arrows for 1 emerald; 4 tipped arrows of Slowness or of Poison for 2; a bow with Power I-II for 6; a Power III book for 14 | 32 sticks, 12 feathers, 14 string |
| Speaker | Moonwater for 3; one random enchanted book a day for 12-20; an explorer map to the nearest unexplored dragon or beholder lair for 14; glow berries for 1 | - |

![The Speaker's trades for an Elf, at kin prices](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/elven-enclave/speaker-trades.png)

### Looks
Elves use the player model with slim arms and pointed ears (a 1x3x1 cuboid each side of the head), at full player size. There are six Wood Elf skins (robes in greens, silver and white; Wardens wear their leathers over them) plus one each for the Speaker and the Fletcher.

![Pointed ears](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/elven-enclave/elves-ears.png)

## Where to find it
Only in Elven Enclaves, where they also spawn naturally inside the trees, the glade, the gardens and by the Moonwell (Wood Elves and Wardens, up to 12 elves within 48 blocks).

## Commands
- `/summon dndclasses:wood_elf`, `dndclasses:elf_warden`, `dndclasses:elf_speaker`, `dndclasses:elf_fletcher`, or the spawn eggs in the mod's creative tab.

## Configuration
- `data/dndclasses/factions/sylvan_court.json`: standings (Elves start at 150), hit -15, kill -150 (the Speaker -500), seen breaking Sylvan Law -40, raids won +60.
- `#dndclasses:faction/sylvan_court` lists the elf entity types.

## For developers
- `entity/ElfEntity` (the Wood Elf and base class: variant, name, home, `mayTarget` filter, Warden call for help on being struck), `ElfWardenEntity` (`Angerable`, `RangedAttackMob`, its own `WardenAttackGoal` because `BowAttackGoal` needs a `HostileEntity`), `ElfMerchantEntity` + `ElfTrades`, `SylvanLaw`.
- `client/renderer/RacialHumanoidRenderer`: the racial NPC kit (player model, slim or classic arms, armour, arm poses for held items and drawn bows, optional head features, scale). Elf ears are the `racial_ears` model layer, textured from the skin's spare corner at (56, 0). The races ticket's `RaceFeatures.renderHead` can take the ears over for both players and NPCs once both are in.
- Skins: `python3 tools/elf_skins.py` writes `textures/entity/elf/`.
- `ModSpawns.canElfSpawn`: natural and chunk-generation spawns only inside real enclave pieces, at most 12 elves within 48 blocks.
