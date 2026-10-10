# Stealth and Perception
Your character sheet's Stealth and Perception now do something on their own. Sneaking hides you better the stealthier you are, so a Rogue can creep much closer to a zombie than a Wizard can. A perceptive character notices that a "chest" is breathing (a mimic) and spots traps, and anyone can press **V** to Search the area with a Perception roll.

Scores and bonuses come from [Ability scores](ability-scores.md); the roll display is the main d20 panel from [D20 skill checks](d20-skill-checks.md).

## How it works
### Passive scores
A passive score is 10 + the skill's check bonus, +5 with advantage and -5 with disadvantage. Nothing is rolled.

### Stealth (passive)
Vanilla mobs already notice a sneaking player at 0.8 of their follow range. While you sneak, that is also multiplied by a factor from your passive Stealth:

`factor = clamp(1 - 0.04 x (passive Stealth - 10), 0.35, 1.0)`

A chestplate tagged `#dndclasses:heavy_armor` (iron, diamond, netherite, and the Knight, Holy Armor and Warrior sets) gives **disadvantage on Stealth**: -5 passive, and disadvantage on any rolled Stealth check.

| Who (sneaking) | Passive Stealth | Range x | Zombie (35-block follow range) notices at |
|-|-|-|-|
| Wizard L1 | 12 | 0.74 | ~26 blocks |
| Rogue L1 (Stealth◆) | 16 | 0.61 | ~21 |
| Rogue L10 (Stealth◆, +4 proficiency) | 20 | 0.48 | ~17 |
| Fighter L1 in an iron chestplate | 11 - 5 = 6 | 0.80 | ~28 |

Not sneaking, nothing changes. Bards are still ignored by monsters entirely, and the Rogue's Vanish is unchanged.

### Perception (passive): the mimic tell
Every second, a dormant [mimic](../mobs/mimic.md) checks the players within 6 blocks. A player whose passive Perception is at least **13** gets the tell once per mimic: the action bar says "That chest just... breathed.", and from then on their client shows faint puffs over its lid while it pretends to be a chest. Only that player sees them.

| Class (L1) | Passive Perception | Tell? |
|-|-|-|
| Ranger, Druid | 14 | yes |
| Fighter, Rogue | 13 | yes |
| Wizard | 11 | no |

Dungeon traps use the same passive Perception rule (see [Dungeon traps](dungeon-traps.md#spotting)).

### Search (V)
Press **V** (Search / Study, rebindable under D&D Classes in Controls):
- Looking at a creature within 16 blocks: that will be **Study** (a Lore/Insight check, still to come). Until then it Searches too.
- Otherwise you **Search**: a Perception roll on the main d20 panel, with a **10 s cooldown** ("You can search again in Ns.").
  - Everything hidden within **8 blocks** whose DC the total meets is revealed. A natural 20 finds everything in range; a natural 1 finds nothing.
  - **Mimics** (DC 13): a red glow outline for 5 s, "That chest has teeth. It's a mimic!", and the breathing puffs from then on. A dormant mimic counts as hidden, not as a creature to Study.
  - **Armed traps** (their DC, 12/13/15/17 by tier): outlined for you like a passively spotted trap, until the trap is spent, disarmed or re-armed.
  - The panel says "You find nothing hidden nearby.", "You spot something hidden!" or "You spot N hidden things!".
  - Hidden things of the Investigation kind (hidden doors, later) use the better of your Perception and Investigation bonuses.

## Where to find it
Everywhere: sneaking, any dormant mimic, and dungeon trap corridors.

## Commands
- `/dndclass sheet <player>`: the passives line shows passive Perception, Insight and Stealth, and the sneaking range factor (e.g. `Stealth 12 (sneaking: noticed at x0.74 range)`). Heavy armour shows as "Heavy armour: dis Stealth".
- `/dndclass forceroll <player> <n...>`: rigs the next Search roll.
- `/summon dndclasses:mimic ~ ~ ~3` places a dormant mimic to test the tell.

## Configuration
- Datapack tag `data/dndclasses/tags/items/heavy_armor.json`: which chestplates give Stealth disadvantage.
- Key: `key.dnd-classes.study`, default V.

## Known limitations
- No Character tab yet: passives show in `/dndclass sheet` only.
- Study (V on a creature) isn't built yet; the key Searches instead.
- The mimic tell and who found a trap by Search are kept in memory only, so after a restart the tell can come again.
- Only the chestplate counts for heavy armour.
- Stealth only shrinks detection range; it doesn't stop mobs that are already chasing you or that you hit.

## For developers
- `classes/SkillChecks/Stealth`: `factor(passive)`, `factor(player)`, `HEAVY_ARMOR` tag, and the dynamic `dndclasses:heavy_armor` sheet contributor (disadvantage on Stealth checks). Applied by `mixin/StealthMixin` (RETURN of `LivingEntity.getAttackDistanceScalingFactor`, server only, sneaking players).
- `classes/SkillChecks/Perceivable`: anything that can be noticed (`perceptionDc`, `perceptionSkill` PERCEPTION|INVESTIGATION, `perceptionPos`, `hiddenFrom(player)`, `perceive(player, searched)`). `MimicEntity` and `TrapTriggerBlockEntity` implement it; Search finds entities and loaded block entities that implement it within 8 blocks.
- `classes/SkillChecks/PerceptionService`: the single passive rule, `passive(player, skill)` / `noticesPassively(player, skill, dc)` (INVESTIGATION uses the better of the two), `searchBonus`, `passiveCheck(world, thing, range)`, `near(world, pos, range)`, and the registry for things that aren't Perceivable themselves:
  ```java
  PerceptionService.Hidden door = PerceptionService.hide(world, pos, 15, Skill.INVESTIGATION,
          (player, searched) -> openFor(player));
  door.remove(); // once it's gone
  ```
  Registrations get a passive check every 20 ticks within 6 blocks, live in memory and are cleared when the server stops (re-register on load). `TrapSense.DEFAULT` calls `noticesPassively`.
- `classes/SkillChecks/Perception`: the key's server side. C2S `dndclasses:study` (varint entity id or -1), validated (16 blocks, line of sight, not a hidden Perceivable). `registerTargetHandler((player, target) -> handled)` lets Study claim creature presses; unhandled presses Search. `search(player)` rolls `SkillCheck.builder(player, PERCEPTION, 0, "search")` labelled `skill.dndclasses.search`. `mark(player, entity|pos, Mark.BREATH|OUTLINE, ticks)` sends S2C `dndclasses:perceived`.
- Client: `Client/SearchKeyClient` (the V key and a 16-block crosshair raycast, dragon parts map to their dragon), `Client/Render/PerceivedMarks` (breath puffs, block outline dust, the outline set) with `PerceivedOutlineMixin` (`MinecraftClient.hasOutline`) and `PerceivedTeamColorMixin` (red).
- Logs: `[Perception] <player> notices ...` (passive) and `[Perception] <player> searches: <thing> total N vs DC M -> found|missed`, plus the usual `[D20] <player> Search ...` line.
- DevScript: `devscripts/stealth-perception.txt`.
