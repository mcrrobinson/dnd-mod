# Drow
The dark elves of the Spider Houses, who hunt the [Underdark](../systems/underdark.md)'s Fungal Forest and Faerzress Hollows: Warriors with poisoned hand crossbows, Mages who cast Darkness, Faerie Fire and Magic Missile, Priestesses of the Spider Queen who call spiders and dread, and the Elite guards of the Drow Spire. They're hostile to every player, spiders are their allies, and every drow you kill pleases the Sylvan Court.

![A Drow Warrior, Mage, Priestess and Elite](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/feat-drow/drow-lineup.png)

## How it works
| Drow | Health | Armour | Speed | Melee | Special |
|-|-|-|-|-|-|
| **Warrior** | 26 | 4 | 0.30 | scimitar 5 | Hand crossbow at 6-14 blocks: a bolt every 50 ticks for 3 damage and Drow Poison. It draws the scimitar within 4 blocks. One in five carries a shield. |
| **Mage** | 22 | 2 | 0.28 | dagger 3 | Keeps 8-12 blocks off and casts (within 16 blocks, when it sees you) Darkness every 20 s, Faerie Fire every 15 s and Magic Missile every 3 s when nothing else is ready, 1 s apart |
| **Priestess** | 40 | 5 | 0.28 | scourge 6 + Drow Poison | Summon Spiders every 15 s, Healing Word every 10 s, Dread of the Spider Queen every 18 s |
| **Elite** | 40 | 8 | 0.32 | twin scimitars 7 | The Warrior's hand crossbow; Parry; levitates to ledges |

Each drow has a name such as "Zesstra Do'Urden" and one of six skins (Mages and Priestesses two each): obsidian-to-slate skin, white or silver hair, red or violet eyes, black and purple leathers with silver trim. The Mage wears a hooded robe, the Priestess a black robe with a red spider and a silver circlet, the Elite a mantle over a chain shirt. They have elf ears.

### Drow Poison
Bolts and the Priestess's scourge carry Drow Poison: a **CON save, DC 13**, tagged `poison` (a Dwarf has advantage).
- **Failure**: Drow Poison for 6 s (Slowness II and Weakness I, and the screen edges darken) and Poisoned for as long. The failure adds a stack.
- **Success**: Drow Poison for 3 s, no stack.
- **Three stacks within 10 s and you fall asleep** for 3 s: Incapacitated (no attacking, using or breaking), Slowness VI, Mining Fatigue III, and "You slump, drowsy with drow poison". Taking any damage wakes you ("The pain jolts you awake").
- Each drow gets one save from you per second, so one bolt is one save.
- Immune: creative players, Rogues and anything else immune to poison (the Alchemist's Iron Stomach, poison-immunity potions), undead and constructs (they can't be Poisoned), and drow and spiders.

### The Mage's spells
- **Darkness**: a sphere of radius 4 for 8 s, centred on its target's side of the gap between them, so you're inside and the mage isn't. Every 0.5 s everything inside except drow and spiders is **Blinded** (the [condition](../systems/conditions.md): mobs can't keep a target past 4 blocks) and gets Darkness and Blindness for a little over a second, so it wears off soon after you step out. Black smoke marks the edge.
- **Faerie Fire**: violet light outlines its target for 10 s (Glowing, seen through walls) and every hit on it deals **+2 damage**.
- **Magic Missile**: 3 darts of 2 force damage each that always hit (no attack roll), the [Wand of Magic Missiles](../items/magic-items.md)' darts.

### The Priestess
- **Summon Spiders**: 2 cave spiders (tagged `drow_summon`) that fight her target, at most 4 alive. They vanish when she dies.
- **Healing Word**: the most hurt drow within 8 blocks that she can see (herself too) heals 6.
- **Dread of the Spider Queen**: players within 6 blocks make a **WIS save, DC 13** (tagged `fear`) or are **Frightened** of her for 5 s.

### The Elite
- **Parry**: a quarter of the melee hits from the front glance off (a shield clang).
- It levitates up to 4 blocks (Levitation for 1 s) to reach a target 2-4.5 blocks above it and within 6 blocks, once every 10 s.

### Traits
- **Sunlight Sensitivity**: in the Overworld by day, under sky light 12 or more, Weakness I and Slowness I (refreshed every second), and their bolts fly wide (+10 spread).
- **Fey Ancestry**: drow can't be Frightened.
- **Allies**: drow never target or hurt each other or spiders, their bolts fly through them, and spiders and cave spiders never target drow (not even when a drow hits one). Drow also hate surface elves and attack [wood elves and wardens](elves.md) on sight.
- A struck drow calls every idle drow within 16 blocks to the fight.
- A shield (Warriors, 20%) turns half the projectiles that hit from the front.

### The Spider Houses
The drow are the **Spider Houses** [faction](../systems/factions.md) (`dndclasses:drow`):
- Everyone starts **Hostile** at -700, and Elves at -900 ("they remember"). Drow attack any player below Neutral on sight, so in practice everyone. Only grudge decay (5 a day) moves it up, so it takes about 120 days without a drow kill to reach Neutral.
- Hitting a drow: -10. Killing one: -60, a Priestess -120.
- **Every drow you kill gives +30 with the Sylvan Court** (no daily cap). The Grey Clans' share comes with the duergar.
- Rivals: Sylvan Court -0.5 and the Grey Clans -0.25 of any gain with the Spider Houses.

### Drops
| Drow | Drops |
|-|-|
| All | 0-2 Spidersilk, 0-3 arrows; 5% (killed by a player) a **Drow Poison Vial**, a splash potion of Drow Poison (6 s) |
| Priestess | also 2% (killed by a player) a Rare magic item |

Their weapons and gear never drop.

## Where to find it
- The Underdark's **Fungal Forest** and **Faerzress Hollows** (`#dndclasses:habitat/drow`), in the dark: Warriors (weight 20, groups of 2-4), Mages (5, alone) and Priestesses (2, alone). Natural spawns despawn like any monster; drow placed by a structure (the Drow Outposts and the Drow Spire, later tickets) stay.
- Spawn eggs for all four are in the creative tab, with Spidersilk.

## Commands
- `/summon dndclasses:drow_warrior` (also `drow_mage`, `drow_priestess`, `drow_elite`). Summon without NBT and `/data merge` afterwards: NBT skips the gear and the name.
- `/rep <player> dndclasses:drow` shows the standing; `... reset` forgets it (back to the start).

## Known limitations
- The Drow Poison Vial is a plain splash potion: it has no save, for players or mobs.
- Faerie Fire adds +2 damage instead of advantage on attacks against the target.
- The sleep is Incapacitated, not Unconscious (there's no Unconscious condition): attacks against a sleeper have no advantage.

## For developers
- Package `mattonfire.dnd.entity.drow`:
  - `Drow`: every registration in one place (entity types, the `drow_poison` and `faerie_fire` effects, the `drow_poison` potion, Spidersilk and the spawn eggs, attributes, spawns, Study weaknesses), called from `DnDClasses` after the potions. Tags `FACTION_TAG` (`#dndclasses:faction/drow`) and `THRALLS_TAG` (`#dndclasses:underdark/thralls`, which the Mind Flayer's thrall tag includes).
  - `DrowEntity` (base: variant, names, targeting, allies, shield, Sunlight Sensitivity, hand crossbow `shootBolt`), `DrowWarriorEntity`, `DrowMageEntity` (`KeepDistanceGoal`), `DrowPriestessEntity`, `DrowEliteEntity`, `HandCrossbowGoal`, `DrowBoltEntity`.
  - `DrowPoison` (the effect, `afflict`, stacks, `sleep` and `wake`), `DrowSaves` (`MobSaveInfo` entries for each drow's poison and the Dread), `DrowDarkness` (zones ticked per world), `FaerieFire`, `DrowAllies`, `DrowHostility`.
- `magic/MagicMissiles.fire(world, caster, target, darts, dartDamage)`: the darts, shared with `WandOfMagicMissilesItem`.
- Mixins: `SpiderTargetMixin` (on `MobEntity.setTarget`: spiders drop a drow target), `FaerieFireDamageMixin` (on `LivingEntity.damage`).
- Client: `client/drow/DrowClient` (renderers with `RacialHumanoidRenderer` and the `DROW` look, the bolt as an arrow, the poison vignette at 40%).
- Data: `factions/drow.json`, `tags/entity_types/faction/drow.json`, `tags/entity_types/underdark/thralls.json`, `tags/worldgen/structure/faction/drow.json` (optional `drow_outpost` and `drow_spire`), the drow in `condition_immune/frightened.json` and `lore/humanoid.json`, `enemy_kills` and an uncapped override for drow in `factions/sylvan_court.json`, `loot_tables/entities/drow_*.json`.
- Textures: `tools/drow_skins.py` (skins), `tools/drow_icons.py` (Spidersilk, the effect icons).
- Hooks for later tickets: the Grey Clans add `"dndclasses:drow": 15` to their `enemy_kills`; the Drow Outposts and the Spire tag their structures into `faction/drow` and spawn drow with `SpawnReason.STRUCTURE` (persistent); the Matron Mother (`dndclasses:drow_matron`) is already an optional member of `faction/drow`; her ticket adds her `kill_member_overrides` entry (-400) once the type exists (an unknown type there makes the whole faction fail to load).
- Logs: `[Drow] <name> drow poison stack n/3`, `falls asleep`, `hurt by <damage> (n) while asleep`, `wakes up`, `Darkness at x y z`, `summons spiders (n alive)`; `[MobAction]` (debug.log) for `hand crossbow`, `darkness`, `faerie fire`, `magic missile`, `summon spiders`, `healing word`, `dread`, `parry`, `levitate`.
- Devscript: `devscripts/drow.txt`.
