# Creature study
Press **V** while looking at a creature to study it. You roll a Lore check (Religion, Nature or Arcana, by the kind of creature) or Insight for people. On a success a chat card tells you its health, armour, which saving throws its attacks call for and what it's immune to. A natural 20 also gives away its weakness. Once you've studied a kind of creature you know it for good.

The check uses your [ability scores](ability-scores.md) and appears on the main d20 panel ([D20 skill checks](d20-skill-checks.md)). The key is the same Search / Study key as [Stealth and Perception](stealth-and-perception.md).

## How it works
### Which skill
The creature's group decides the skill. The groups come from entity type tags, checked in this order:

| Group | Tag | Skill | Examples |
|-|-|-|-|
| Undead | `#dndclasses:lore/undead` | Religion (INT) | zombies, skeletons, phantoms, Wither, Lich, Bone Wyvern |
| Humanoid | `#dndclasses:lore/humanoid` | Insight (WIS) | goblins, hobbits, dwarves, elves, villagers, illagers, witches, piglins |
| Arcane | `#dndclasses:lore/arcane` | Arcana (INT) | Beholder, Mimic, Gelatinous Cube, wyverns and dragons, endermen, blazes, slimes, Warden |
| Beast | `#dndclasses:lore/beasts` | Nature (INT) | Owlbear, River Pikehorn, Magmamunchers, spiders, wolves, hoglins |

A creature in none of the tags falls back on its kind: the undead group goes to Religion, illagers and traders to Insight, other monsters to Arcana, and everything else (animals) to Nature.

### DC
| Creature | DC |
|-|-|
| Vanilla mobs | 10 |
| This mod's mobs | 13 |
| Bosses (any mod boss, Ender Dragon, Wither, Warden, Elder Guardian) | 15 |

Example: a level 1 Wizard has Arcana +4 (INT 15, proficient), so against a Frost Drake (DC 15) an 11 or better on the die works.

### Results
- **Success:** the panel says "You recall what you know of the ...", and a card appears in chat:
  - Name and the skill used.
  - Health (this one's current and maximum) and armour.
  - Each attack that calls for a save, for example "Frost breath: DEX save DC 14, half damage, no Freeze". These come from the same entries the attacks roll from, so they're always the real numbers. Creatures without such attacks say so.
  - Immunities found on the creature itself: fire, freezing, drowning, poison, wither.
  - The creature type is now **known**. Pressing V at that type again shows the card straight away, with no roll.
- **Natural 20:** as a success, and the card also lists its **weaknesses**, which stay on the card from then on:
  - Frost Drake: fire deals +50% damage.
  - Lich: its soul hides in a phylactery (smash that first or it reforms); Necromancers deal +50% damage to it.
  - Owlbear: while it hugs you, 6 damage breaks its grip.
  - Any undead: potions of Healing hurt it and Harming heals it.
  - Others: "No weakness worth knowing".
- **Failure or natural 1:** you can't study that creature type again for **60 s** ("You can't recall more about the ... yet (Ns)"). Other types are unaffected.

A plain success never shows the weakness, and a known type doesn't roll again, so a natural 20 on the first try is the only way to learn it in normal play.

### Insight on people
When the skill is Insight, the card also shows what you read in that person. It's read fresh each time, even for a known type:
- **Villager:** how it feels about you (fond, warm, indifferent, wary or hostile) with its gossip reputation number, and whether you can still [persuade](d20-skill-checks.md) it today (already tried today, or not at all: a jobless villager, nitwit or child has no trade to be talked into).
- **Mountain Dwarf:** whether it holds a grudge against you.

### What the key does
- Looking at a mob within 16 blocks: Study.
- Looking at a player, an armour stand or nothing: [Search](stealth-and-perception.md#search-v).
- A dormant mimic isn't a creature yet, so it's Searched rather than studied.

## Where to find it
Anywhere, on any mob.

## Commands
- `/dndclass lore <player> list`: the creature types the player knows, marked "(weakness)" if learnt on a natural 20.
- `/dndclass lore <player> forget [entity]`: forgets one type (e.g. `dndclasses:frost_drake`) or all of them, and clears the matching fail cooldowns, so the next V rolls again.
- `/dndclass forceroll <player> <n...>`: rigs the next roll (e.g. `20` to see the weakness).

## Configuration
- Datapack tags `data/dndclasses/tags/entity_types/lore/{undead,humanoid,arcane,beasts}.json` choose each creature's skill.

## Known limitations
- Known creatures are stored per player (persistent data, kept through death). Fail cooldowns are in memory and reset on relog.
- Weaknesses are only written for a few creatures so far.
- The card goes to chat; there's no bestiary page for studied creatures yet.

## For developers
- `classes/SkillChecks/Study`: registered with `Perception.registerTargetHandler` (from `D20.register`). It only handles `MobEntity` targets.
  - `loreOf(target)` returns the `Study.Lore` group (each has its `skill()` and `tag()`), and `dc(target)` the DC.
  - `knowledge(player, type)` returns 0, `KNOWN` (1) or `KNOWN_WEAKNESS` (2). `learn(player, type, level)` raises it. The data is stored in the player's persistent data under `dndKnownCreatures` (entity id to byte).
  - `card(player, target, lore, weakness)` builds the card lines.
  - `registerWeakness(type, Text | target -> Text)` adds a natural 20 line. Built-ins use `FrostDrakeEntity.FIRE_WEAKNESS`, `LichEntity.NECROMANCER_DAMAGE_BONUS` and `OwlbearEntity.HUG_BREAK_DAMAGE`.
  - `registerInsight((player, target) -> List<Text>)` adds Insight lines for humanoids. This is the hook for detect lies and other people-reading features. Built-ins read `VillagerEntity.getReputation`, `Persuasion.triedToday(player, villager)` and `MountainDwarfEntity.hasGrudgeAgainst(player)`.
- The roll is `SkillCheck.builder(player, skill, dc, "study", "<group>")` with kind `RollKind.STUDY`, so sheet advantage filters can target `study` or a group tag.
- The save lines come from `MobSaveInfo.of(type)`: register a creature's saves there and they appear on its card.
- Logs: `[Study] <player> studies <type> with <skill>: <total> vs DC <dc> -> <outcome>` and `[Study] <player> recalls <type> (known N)`, plus the usual `[D20]` line.
