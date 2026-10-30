# Kobolds
Small, scaly, cowardly and clever: kobolds worship dragons and build traps. On their own they run; in a pack they get brave, mob you, stab harder when another kobold is beside you (Pack Tactics), and their slingers loose stones together on a shout. Trapmakers hide snares in the grass mid-fight. They belong to the **Kobold Tribes**, a faction that judges you by dragons: dragon-slayers lose standing, dragon-friends pay tribute in gold.

![Kobolds beside a player](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/kobolds/kobold-scale.png)

## How it works

### The four kobolds
Every kobold is 0.5 x 0.95 blocks (eyes at 0.8), so it fits a 1-block hole; its head reaches a player's hip. It has one of 5 skins (rust, ochre, umber, slate, ash).

| Mob | Id | Health | Armour | Speed | Melee | Special |
|-|-|-|-|-|-|-|
| Kobold | `kobold` | 8 | 2 | 0.33 | spear 3 | Pack Tactics, cowardice |
| Kobold Slinger | `kobold_slinger` | 7 | 1 | 0.33 | dagger 2 | slings a stone (3 damage) at 6-16 blocks every 30 ticks (+-5); joins volleys; backs off when you close in |
| Kobold Trapmaker | `kobold_trapmaker` | 9 | 2 | 0.30 | hook-knife 2 | sets snares in a fight; trades at Friendly or better |
| Kobold Dragonshield | `kobold_dragonshield` | 30 | 7 | 0.30 | spear 5 | the leader: Rally, a shield, fire immunity, knockback resistance 0.3; 0.6 x 1.1, drawn at 1.15x |

- **Pack Tactics**: a kobold's melee hit, and a slinger's stone, has advantage (x1.25) when another living kobold is within 2 blocks of the target. It stacks and cancels with [conditions](../systems/conditions.md) as usual.
- **Sunlight Sensitivity**: in the Overworld by day with sky light 12+ at its eyes, a kobold's attacks have disadvantage (x0.75), cancelling Pack Tactics, and 30% of a slinger's stones fly wide (4x the spread). They don't burn.
- Kobolds never hurt each other, and their stones fly through other kobolds.

### Courage
A kobold is **brave** when any of these holds:
1. a Dragonshield within 12 blocks is alive (or it is one);
2. at least 2 other kobolds are within 8 blocks (a pack of 3);
3. it's at home (in a [warren](../structures/kobold-warrens.md), or a lair once those exist).

Otherwise it's **craven**: it won't close in to melee, keeps 10-14 blocks from its target (slingers still shoot from there), and runs (x1.3 speed, cowering) for 5 s from a player who hits it. Below 30% health any kobold runs for its pack (a warren kobold first dives into the nearest bolt-hole within 20 blocks): towards the nearest kobold further from the threat, or just away, until it's 12 blocks clear.

**Dragon-slayer dread**: for 3 in-game days (72000 ticks) after you kill a Lightning Chaser, a Frost Drake or an Ancient dragon (the `#dndclasses:kobold_dread` tag), kobolds need 4 others round them to be brave against you, and a kobold that sees you shrieks "The wyrm-killer!" (once per 30 s).

### Sling volleys
When a slinger is ready and sees its target it calls a volley: a sharp chirp, and every other slinger within 12 blocks that sees the same target and is within 15 ticks of being ready slings at it 0-6 ticks later. They all restart their cooldown as they shoot. Three slingers do a 9-damage burst, 11.25 with Pack Tactics.

### The Dragonshield
- **Rally**: while it lives, kobolds within 12 blocks are always brave and have Speed I.
- Its shield stops 50% of projectiles that come at it from the front.
- Permanent Fire Resistance (the dragon's blessing).

### Snares
A Kobold Snare is a loop of cord under leaf litter, 1/16 of a block high, with no collision. It can't hang in the air and can be waterlogged.
- **In a fight** a Trapmaker hides one every 20 s on the floor on the straight line between it and its target, 3-6 blocks from the target. Each Trapmaker has at most 3 out; they fall apart after 60 s.
- **Stepping in one** (anything but a kobold, and for a player's snare anyone but its owner and their [party](../systems/party.md)): 2 trap damage and a **DEX save, DC 12** (tag `trap`). A failure is **Restrained** for up to 5 s, with a STR 12 save every second to break free. The snare is used up either way.
- Like dungeon traps, Rogues and Rangers always see snares, anyone else when their passive Perception is 12+, or after a Search (V) finds one: spotters get an orange outline, only for them.
- A snare you place yourself spares you and your party and never falls apart; breaking it gives it back. A kobold's snare drops nothing.

### The Kobold Tribes (`dndclasses:kobolds`)
| | |
|-|-|
| Members | the four kobolds (`#dndclasses:faction/kobolds`) |
| Start | -300 (Unfriendly); Dragonborn 0 (Neutral, "they smell the wyrm in your blood"); Gnomes -600 (Hostile) |
| Rivals | Tinkers' Guild -0.25, Dragonsworn -0.25 |
| Hit / kill a kobold | -3 / -5 (a Dragonshield -25); -30 while you're Neutral or better (betrayal) |
| Kill a dragon | Wyvern or Ember Wyvern -5, Lightning Chaser or Frost Drake -60, an Ancient -400 (uncapped) |
| Tribute | a gold ingot +2, a gold block +18, a Kobold Trinket +1 in a Dragon Tribute Bowl, 40 a day |
| Trade | +1 a trade with a Trapmaker, 15 a day |
| Decay | a negative standing recovers 5 a day |

The Dragonsworn honour whoever slays a chromatic tyrant: killing an Ancient is +100 with them.

**Tribute**: use a gold ingot, a gold block or a Kobold Trinket on a Dragon Tribute Bowl. The item is used up: "The kobolds chitter approvingly. (+2 The Kobold Tribes)". At the daily cap the bowl refuses it: "The bowl is heaped. The wyrm is content for today."

**What the tiers do**

| Tier | Effect |
|-|-|
| Hostile | Kobolds go for you on sight (16 blocks): brave ones close in, craven ones harass from range. |
| Unfriendly | Only at home (a warren or lair), within 6 blocks (parley range), or once provoked. |
| Neutral | They ignore you unless provoked. |
| Friendly | Trapmakers trade. |
| Honored | [Warren traps](../structures/kobold-warrens.md#the-traps) don't go off under you. |
| Exalted | Kobolds within 16 blocks join your fights (any mob that hit you, that you hit in the last 5 s, or that has you as its target; never players, kobolds, your pets or creepers). The Ancient's lair guards let you pass. |

"Provoked" means you hit that kobold or any kobold in the last 10 s, or hit a kobold within 16 blocks of it in the last 30 s (the pack holds a grudge). Lair guards (command tag `dndclasses.lair_guard`) treat everyone below Exalted as Hostile.

### Items
| Item | What it is |
|-|-|
| Kobold Sling | Anyone can use it: right-click slings a cobblestone from your inventory, 3 damage, 1 s cooldown, 120 uses. A Common magic weapon (sling), like the Halfling Sling |
| Kobold Scale | 4 make 1 leather (shapeless) |
| Kobold Trinket | One of 12 odds and ends (a dragon's milk tooth on a string, a tiny brass bell with no clapper, a shard of red eggshell, a jar of shiny pebbles, a gnome's ear (dried), a copper ring bent into a heart, a map scratched on bark leading nowhere, a whistle shaped like a wyrmling, a single gold coin bitten, a lizard-bone dice pair, a scrap of a knight's tabard, a smoky glass bead), shown in its tooltip. Gnome Tinkers buy one for 1 emerald; a Tribute Bowl takes them |
| Kobold Snare | The snare block above |
| Dragon Tribute Bowl | A gold-rimmed basalt bowl: 2 gold ingots over 3 smooth basalt |

### Loot
- Kobold: 0-1 Kobold Scale, 0-1 arrow, 8% Kobold Trinket (player kill; +2% per Looting level).
- Kobold Slinger: 0-3 cobblestone, 8% Kobold Sling (+2% per Looting level), 5% Trinket.
- Kobold Trapmaker: 0-2 string, 20% a tripwire hook, 15% 1-2 Kobold Snares, 5% Trinket.
- Kobold Dragonshield: 1-3 Kobold Scales, 30% Trinket, 3-8 gold nuggets, and a 15% Uncommon magic item (player kill).
- Looting adds to the counts as usual.

### Trapmaker trades
At Friendly or better (worse, it hisses and won't open). 12 uses each, restocked every morning; [reputation prices](../systems/factions.md) apply.

| Sells | Price |
|-|-|
| 4 Kobold Snares | 3 emeralds |
| Kobold Sling | 4 emeralds |
| 32 cobblestone ("sling stones") | 1 emerald |
| Kobold Trinket | 2 emeralds |
| 2 tripwire hooks | 1 emerald |

| Buys | Pays |
|-|-|
| 3 gold ingots | 1 emerald |
| 16 rotten flesh | 1 emerald |

### Study
Kobolds are humanoids ([Insight](../systems/creature-study.md)). A natural 20 adds "alone, a kobold loses its nerve and runs; in bright sun it fights badly" (and for the Dragonshield, "while it lives the pack is brave; kill the leader and they scatter"). The Trapmaker's card lists the snare's DEX 12 save. Kobolds have darkvision (flavour only).

## Where to find it
Packs roam the windswept hills, gravelly hills and forest, stony peaks, the three badlands and dripstone caves (`#dndclasses:habitat/kobold`), in the dark like other monsters (spawn weight 8). Each natural kobold brings 2-4 more round it (50% Kobold, 35% Slinger, 15% Trapmaker) and a 10% chance of a Dragonshield leading them. Natural kobolds despawn like other monsters. Spawn eggs for all four are in the DnD tab. [Kobold Warrens](../structures/kobold-warrens.md) in hillsides of the same biomes hold a den, a shrine with a Tribute Bowl, a nursery, a trap workshop and the chief's hoard, guarded by kobold traps.

## Known limitations
- Kobold sounds are a fox's, pitched up (the volley chirp is a fox screech); there are no kobold sound files yet.
- Kobold weapons (spear, sling, hook-knife, shield) are part of the model, not held items.
- Dragon-slayer dread is per player and doesn't spread to their party. `/rep <player> dndclasses:kobolds reset` clears it too.
- Lairs (another card) register their home through `KoboldCourage.registerHome`; warrens already do.

## For developers
- Package `mattonfire.dnd.entity.kobold`: `Kobolds` (registration, tags, items, blocks, spawns, hooks), `KoboldEntity` (base: variant, courage cache, grudges, scared state, animations), `KoboldWarriorEntity`, `KoboldSlingerEntity`, `KoboldTrapmakerEntity` (`SettlementMerchant`), `KoboldDragonshieldEntity`, `KoboldCourage` (bravery, Pack Tactics, Sunlight Sensitivity), `KoboldVolleys`, `KoboldSnares`, `KoboldHostility`, `KoboldTribute` (tribute, dread), `KoboldTrades`, `KoboldPacks`, `KoboldSaves`, `ai/` goals.
- Blocks: `classes/Blocks/KoboldSnareBlock` + `KoboldSnareBlockEntity` (owner, `PlayerSnare`, `ExpiresAt`; `Perceivable`), `TributeBowlBlock`.
- `ConditionEffects.registerMobAttackMode(name, (attacker, target) -> Advantage)`: each answer adds one advantage or disadvantage to a mob's melee (on top of conditions) and projectiles (alone). Logged as `[Conditions] Kobold -> Iron Golem: pack tactics +adv => ADVANTAGE x1.25 (3.75 damage)`.
- Factions: `monster_kill_overrides` (uncapped, may be negative, checked before `monster_kills`), `Reputation.Source.TRIBUTE` (rivals react), `Reputation.Cap.TRIBUTE`. `FactionAllyGoal` generalises the dwarves' `DwarfFriendGoal`.
- `SlingStoneEntity` takes its damage (`Damage` in NBT); `SlingItems` holds the slings' shared throw.
- Hooks for other cards: entity tags `#dndclasses:faction/kobolds` and `#dndclasses:lair_guards/{warrior,slinger,trapmaker,leader}`, `KoboldCourage.registerHome(Predicate<KoboldEntity>)`, `KoboldHostility.isLairGuard` / `sparedByTraps` / `LAIR_GUARD_TAG`, `KoboldSnares.place(world, pos, kobold)`, `KoboldEntity.setHome`, the structure tag `#dndclasses:faction/kobolds` (empty until warrens), the `#dndclasses:ancient_dragons` entity tag (optional in `kobold_dread`), and 1-3 Kobold Trinkets added to `chests/ancient_red_hoard` by id.
- Logs: `[Kobold]` lines (slings, volleys, snares, tribute, running, dread) at INFO; `[MobAction] Kobold Slinger volley (3 slingers) -> Dev` and slings/snares at DEBUG.
- Assets: `tools/kobold_model.py` (geo, animations idle/walk/attack/sling/cower; asserts the 15.2 px = 0.95 block height), `tools/kobold_skins.py` (5 skins), `tools/kobold_item_textures.py`.
- Devscripts: `devscripts/kobold-scale.txt` (size beside a player, hitboxes) and `devscripts/kobolds.txt` (Pack Tactics, sunlight, volleys, fleeing, snares, tribute, dragon kill, Dragonborn start, trades).
