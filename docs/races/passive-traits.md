# Passive racial traits
The racial traits that are always on: Darkvision, the racial resistances, Elf Keen Senses, Trance and Fey Ancestry, Dwarf Dwarven Resilience and Stonecunning, Halfling Second Breakfast, Gnome Gnome Cunning and Tinker, and Human Ambitious and Diplomat. Like every racial trait, they need the `dndRaces` gamerule on. The dice and combat traits are on their own page: [d20 and combat traits](combat-traits.md).

![Darkvision: the same sealed room as an Elf and as a Human](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/passive-racial-traits/darkvision-elf.png)

## How it works
### By race
| Race | Trait | What it does |
|-|-|-|
| Human | **Ambitious** | +10% class XP from every source. Fractions carry over, so two 5 XP awards make 11. |
| Human | **Diplomat** | +2 on Persuasion, Deception and Intimidation checks. The first time a Human opens a villager's trades, it gains 10 `minor_positive` gossip (a small discount; see [Racial homes](racial-homes.md#human-vanilla-villages)). |
| Elf | **Darkvision** | See below. |
| Elf | **Keen Senses** | While you sneak, hostile mobs within 12 blocks get a white outline, through walls. Only you see it. |
| Elf | **Trance** | You never need sleep, so phantoms never spawn for you. You can still sleep to skip the night. |
| Elf | **Fey Ancestry** | Advantage on saves against being charmed: saves tagged `charm` and repeat saves against the [Charmed](../systems/conditions.md) condition. Immunity to magical sleep is waiting for something that casts it. |
| Dwarf | **Darkvision** | See below. |
| Dwarf | **Dwarven Resilience** | Poison you get lasts half as long, and its ticks deal 0.5 damage instead of 1. Advantage on saving throws against poison (saves tagged `poison`, such as a needle trap's). |
| Dwarf | **Stonecunning** | Mines stone, deepslate, tuff and ores 20% faster (`#dndclasses:stonecunning`). While you sneak in a dungeon, its bricked-up hidden doorways within 8 blocks are outlined in gold dust (stonework ones: cracked stone bricks or cracked deepslate bricks, `#dndclasses:secret_door`; the Goblin Warren's packed-earth tunnels don't count). It also finds [Hidden Door obstacles](../systems/obstacles.md) within 8 blocks: once a second, while you sneak, each one still hidden from you is checked against your passive Perception or Investigation (the better one) + 5 for advantage, and if that meets its DC you find it as a Ranger would (it's revealed to your party and you earn its XP, once per reset). Advantage on the camp watch underground. |
| Halfling | **Second Breakfast** | Each food you eat restores 1 extra hunger point (bread: 6 instead of 5). |
| Gnome | **Darkvision** | See below. |
| Gnome | **Gnome Cunning** | 30% less damage from magic (`#dndclasses:magic_damage`: vanilla magic and indirect magic, so poison ticks, harming and Evoker fangs, plus the Wizard's explosion and Hellish Rebuke). Advantage on INT, WIS and CHA saves against magic (saves tagged `magic`). |
| Gnome | **Tinker** | Crafting a redstone component (repeater, comparator, observer, piston, sticky piston, dispenser, dropper, hopper or any rail; `#dndclasses:tinker_crafts`) has a 25% chance per craft to make one extra, which goes into your inventory. Shift-click crafting rolls for every craft. |
| Half-Orc | **Darkvision** | See below. |
| Tiefling | **Darkvision** | See below. |
| Tiefling | **Hellish Resistance** | Half damage from fire, lava and burning (`#minecraft:is_fire`). A Warlock is already immune, so there's nothing to stack. |
| Dragonborn | **Draconic Resistance** | Half damage from your ancestry's element: Ember fire (`#minecraft:is_fire`), Frost freezing (`#minecraft:is_freezing`: powder snow and frost breath), Storm lightning (`#minecraft:is_lightning`). A Frost Dragonborn's Freeze effect also lasts half as long. The full [Draconic Scale](../items/armor.md) set makes you immune instead. |

### Darkvision
Elves, Dwarves, Gnomes, Half-Orcs and Tieflings see in the dark. Your client draws the world as if you had night vision at 15% strength, so an unlit cave is dim but readable, and torches still matter. A Night Vision potion (or the Cleric's) still gives full brightness. It's only on your own screen: other players see the world as their own race does.

| Elf | Human |
|-|-|
| ![Elf](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/passive-racial-traits/darkvision-elf.png) | ![Human](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/passive-racial-traits/darkvision-human.png) |

### Resistances in detail
- Resistances apply before armour, enchantments and the Resistance effect, and stack with them.
- `/damage @s 4 minecraft:in_fire` takes 4 health from a Human and 2 from a Tiefling or an Ember Dragonborn; `/damage @s 4 minecraft:magic` takes 2.8 from a Gnome.
- A 20 s poison lasts 10 s on a Dwarf, and over 5 s it takes 2 health instead of 4. A Bard is still healed by poison instead.

### Stonecunning's hidden doorways
![A sneaking Dwarf sees the bricked-up doorway](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/passive-racial-traits/stonecunning-dwarf.png)

The outline is drawn on both faces of the bricked-up wall every half second while you sneak, and only for you. It stops once the doorway is broken open.

A [Hidden Door obstacle](../systems/obstacles.md) is found instead of outlined: "Your stonecunning picks out a seam in the stonework: a hidden door!", after which it gets the gold box every finder sees. A Dwarf with WIS 10 and no Perception proficiency has a passive 10 + 5 = 15, so they find Easy (DC 10), Medium (13) and Hard (15) doors, but not Very Hard (18) ones.

## Where to find it
Pick the race. The traits are listed in the race picker's tooltips, in the chat summary when you pick, and on the "Your heritage" page of the Class Guidebook. `/dndclass sheet` lists Diplomat's bonuses and the advantages under the race's name.

## Commands
- `/dndrace traits <player>` prints the player's break speed on deepslate and the hidden stone doorways within 128 blocks. See [Admin commands](../systems/admin-commands.md#races).
- `/dndclass sheet <player>` shows Diplomat and the advantage lines.

## Configuration
`/gamerule dndRaces false` turns every trait off. Numbers are constants in `Race/RacePassives`.

## Known limitations
- No saving throw is tagged `magic` yet, so Gnome Cunning's save advantage waits for spells that roll saves.
- Darkvision is a brightness boost, not black-and-white sight, and it doesn't stack with night vision.
- Keen Senses uses the normal glow outline, so it's white unless the mob is on a team.
- The Stonecunning outline only covers dungeon hidden doorways, not other cracked stonework.

## For developers
- `classes/Race/RacePassives` holds every passive trait and its numbers, and registers itself from `RaceLifecycle.register()`:
  - A character sheet contributor `dndclasses:race_passives`: Diplomat's `skillBonus`es, Dwarf advantage on `allSaves().and(tag("poison"))` and on `tag(CampWatch.TAG_WATCH).and(tag(CampWatch.TAG_UNDERGROUND))`, and Gnome advantage on INT/WIS/CHA saves tagged `magic` (`RacePassives.MAGIC_TAG`). Tag a spell's save with `magic` to give it to Gnomes.
  - A server tick (every 10 ticks): Trance resets `Stats.TIME_SINCE_REST` for Elves each second; a sneaking Dwarf's nearby hidden doorways are outlined with dust sent only to them. Every 20 ticks a sneaking Dwarf also checks the Hidden Door obstacles round them (`RacePassives.stonecunningDoors`, through `ObstacleIndex` and `HiddenDoorType.reveal` with `How.STONECUNNING`).
  - `raceOf(player)` reads the active race on the server and the synced body race on the client, so client-side traits (Darkvision, Keen Senses, break speed prediction) need no packets.
- Mixins:
  - `RacePassivesLivingMixin`: `resist` on the `applyDamage` amount in `LivingEntity.damage`, and `shortenEffect` on the effect passed to `addStatusEffect(StatusEffectInstance, Entity)`.
  - `RacePoisonMixin`: a Dwarf's poison tick in `StatusEffect.applyUpdateEffect`.
  - `RacePassivesPlayerMixin`: Stonecunning in `getBlockBreakingSpeed`, Second Breakfast at the head of `eatFood` (`HungerManager.add(1, 0)`).
  - `CraftingResultSlotMixin`: Tinker at the head of `CraftingResultSlot.onCrafted(ItemStack)`, which runs once per craft with the crafted stack (the result inventory's last recipe is already cleared by then on a shift-click).
  - Client: `DarkvisionLightmapMixin` raises local 8 of `LightmapTextureManager.update` (the night vision strength) to `DARKVISION_STRENGTH`; `KeenSensesOutlineMixin` answers `MinecraftClient.hasOutline`.
  - `Progression.addXp` calls `ambitiousXp`; the carried tenths are `ambitiousCarry` in the persistent `dndRace` compound.
- Hidden doorways: `DungeonPiece.hiddenDoorways()` gives each hidden door's bricked-up blocks from the structure start. A dungeon made with `/place structure` has no start, so `RacePassives` falls back to the recorded rooms (`DungeonRegistry.find`) and finds the doorways by shape: inner-wall columns of `#dndclasses:secret_door` with air in the outer skin behind them.
- Tags: `data/dndclasses/tags/damage_type/magic_damage.json`, `items/tinker_crafts.json`, `blocks/stonecunning.json`, `blocks/secret_door.json`.
- Devscripts: `devscripts/race-passive-traits.txt` (every trait, with printed results and screenshots), `race-darkvision.txt`, `stonecunning-secret-doors.txt` then `stonecunning-secret-doors-2.txt` (in a scratch run dir), and `stonecunning-hidden-door.txt` (Hidden Door obstacles).
