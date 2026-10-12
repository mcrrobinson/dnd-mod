# Ability Scores
Every character has a character sheet: Strength, Dexterity, Constitution, Intelligence, Wisdom and Charisma, each a score with a modifier, plus a proficiency bonus that grows with class level. Your class sets the starting scores and the saves and skills you're trained in. The sheet only changes d20 rolls (lockpicking, persuasion, the attack-roll panel, and later saving throws and other checks). Your hearts, speed and damage still come from your class.

![A level 1 Rogue's lockpick: 10 + 6 (DEX +2, Thieves' Tools expertise +4) against DC 10](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/ability-scores/abilities-lockpick-l1.png)

## How it works
### Scores and modifiers
- Modifier = floor((score - 10) / 2), so 8 is -1, 10 is +0, 15 is +2 and 20 is +5.
- Every class uses the 5e standard array (15, 14, 13, 12, 10, 8), arranged to fit the class.
- A player without a class has 10 in everything and no proficiencies.
- Scores go from 1 to 30. Bonuses that add to a score ([races](../races/races.md) and ability score improvements) can't push it past 20. Items that set a score ("your Strength is 19") and the admin override can.

### Proficiency bonus
| Class level | 0-4 | 5-8 | 9-10 |
|-|-|-|-|
| Proficiency bonus | +2 | +3 | +4 |

A save or skill you're proficient in adds the bonus. Expertise (◆) adds it twice. Half proficiency (½, the Bard's [Jack of All Trades](class-save-features.md) from level 2) adds half, rounded down.

### Ability score improvements
- Reaching class levels **4 and 8** gives **2 ability points** each (4 in total by level 10). They're separate from skill points.
- At an Attunement Table, open the **Character** tab and click the green **+** on a score: 1 point gives +1 to that score.
- The + only shows while you have points and the score's natural value (class + race + earlier improvements) is below 20. The server checks the table (within 8 blocks of the one you opened), the points and the cap again.
- Points are kept per class, like skill points, so switching class and back keeps them. They survive death and relogging. `/dndclass resetprogress` refunds them with the rest of the class's progress.
- Levelling into 4 or 8 says so in chat: "You have 2 ability points: spend them at an Attunement Table (Character tab)."
- Example: a level 10 Rogue with a +2 DEX race reaches DEX 15 + 2 + 2 (level 4) + 1 (level 8) = 20, a +5 modifier.

### Check and save bonuses
- **Skill check:** d20 + the skill's ability modifier + proficiency (× 0, ½, 1 or 2) + flat bonuses.
- **Saving throw:** d20 + ability modifier + proficiency if your class has that save + flat bonuses.
- **Attack bonus** (shown on the crit and fumble panel): the better of STR and DEX modifier, plus proficiency. Attacks still never miss against armour, so it's for display only.
- **Passive score:** 10 + the check bonus, +5 with advantage, -5 with disadvantage.
- **Advantage / disadvantage:** roll two d20s and keep the higher or lower. Any advantage together with any disadvantage gives a straight roll. The HUD shows the dropped die in grey.
- A natural 20 always succeeds and a natural 1 always fails.

### Classes
Scores are STR / DEX / CON / INT / WIS / CHA, with modifiers in brackets. Skills marked ◆ have expertise.

| Class | STR | DEX | CON | INT | WIS | CHA | Saves | Skills |
|-|-|-|-|-|-|-|-|-|
| Barbarian | 15 (+2) | 13 (+1) | 14 (+2) | 8 (-1) | 12 (+1) | 10 (+0) | STR, CON | Athletics, Survival, Intimidation |
| Bard | 8 (-1) | 14 (+2) | 12 (+1) | 10 (+0) | 13 (+1) | 15 (+2) | DEX, CHA | Persuasion◆, Performance◆, Insight, Animal Handling |
| Cleric | 13 (+1) | 10 (+0) | 14 (+2) | 8 (-1) | 15 (+2) | 12 (+1) | WIS, CHA | Insight, Medicine, Religion |
| Druid | 8 (-1) | 13 (+1) | 14 (+2) | 12 (+1) | 15 (+2) | 10 (+0) | INT, WIS | Nature, Animal Handling, Survival, Perception |
| Fighter | 15 (+2) | 12 (+1) | 14 (+2) | 10 (+0) | 13 (+1) | 8 (-1) | STR, CON | Athletics, Perception, Intimidation |
| Monk | 10 (+0) | 15 (+2) | 13 (+1) | 8 (-1) | 14 (+2) | 12 (+1) | STR, DEX | Acrobatics, Athletics, Insight, Stealth |
| Paladin | 15 (+2) | 8 (-1) | 13 (+1) | 10 (+0) | 12 (+1) | 14 (+2) | WIS, CHA | Athletics, Persuasion, Religion |
| Ranger | 12 (+1) | 15 (+2) | 13 (+1) | 10 (+0) | 14 (+2) | 8 (-1) | STR, DEX | Perception, Survival, Stealth, Nature |
| Rogue | 8 (-1) | 15 (+2) | 12 (+1) | 14 (+2) | 13 (+1) | 10 (+0) | DEX, INT | Stealth◆, Thieves' Tools◆, Perception, Acrobatics, Investigation, Sleight of Hand |
| Necromancer | 8 (-1) | 14 (+2) | 10 (+0) | 15 (+2) | 12 (+1) | 13 (+1) | INT, WIS | Arcana, Religion, Intimidation |
| Warlock | 8 (-1) | 14 (+2) | 13 (+1) | 12 (+1) | 10 (+0) | 15 (+2) | WIS, CHA | Arcana, Deception, Intimidation |
| Wizard | 8 (-1) | 14 (+2) | 12 (+1) | 15 (+2) | 13 (+1) | 10 (+0) | INT, WIS | Arcana, Investigation, History |
| Artificer | 10 (+0) | 13 (+1) | 14 (+2) | 15 (+2) | 12 (+1) | 8 (-1) | CON, INT | Arcana, Investigation, Thieves' Tools, Tinker's Tools |
| Blood Hunter | 15 (+2) | 13 (+1) | 14 (+2) | 12 (+1) | 10 (+0) | 8 (-1) | DEX, INT | Athletics, Survival, Insight |
| Alchemist | 8 (-1) | 13 (+1) | 14 (+2) | 15 (+2) | 12 (+1) | 10 (+0) | CON, INT | Arcana, Medicine, Nature, Alchemist's Supplies |
| (no class) | 10 | 10 | 10 | 10 | 10 | 10 | none | none |

Fighters also crit on a natural 19 or 20 (Improved Critical), which now comes from the sheet.

Skills and tools: Acrobatics, Sleight of Hand, Stealth, Thieves' Tools (DEX); Athletics (STR); Arcana, History, Investigation, Nature, Religion, Tinker's Tools, Alchemist's Supplies (INT); Animal Handling, Insight, Medicine, Perception, Survival (WIS); Deception, Intimidation, Performance, Persuasion (CHA).

### Examples
| Check | Level 1 | Level 5 | Level 10 |
|-|-|-|-|
| Rogue lockpick (DEX + Thieves' Tools ◆) | +6 | +8 | +10 |
| Bard persuasion (CHA + Persuasion ◆) | +6 | +8 | +10 |
| Barbarian attack bonus (STR + proficiency) | +4 | +5 | +6 |

### Why scores don't change health or damage
Each class's physical feel is already hand-tuned (the Barbarian's 40 HP, the Wizard's 10 HP). Adding CON to health or STR to damage would count the same idea twice, make races and stat items mandatory min-maxing, and fight the attribute modifiers that skills, Wild Shape and armour sets already use. The d20 is where your character sheet matters.

## Where to find it
The **Character** tab of the skill tree screen (O, or an Attunement Table) shows the sheet for every class:
- Top: proficiency bonus, attack bonus, crit range, and your unspent ability points.
- Left: six ability boxes (score, modifier below; a gold corner means you're proficient in that save), with a **+** at a table when you have points. Under them, passive Perception, Insight and Stealth, and a **Features** line listing advantages, Evasion-style features, rerolls and crit range.
- Right: the six saving throws (● proficient, `A`/`D` when you always roll them with advantage or disadvantage), then a scrollable list of every skill and tool (● proficient, ◆ expertise, ½ half).
- Hover any number for its breakdown: every source that changed it (class, race, improvements, items, auras, the admin override).

The class guidebook has a **Your abilities** page with your class's starting scores, saves, skills and how ability points work.

`/dndclass sheet <player>` prints the whole sheet in chat.

## Commands
| Command | What it does |
|-|-|
| `/dndclass sheet <player>` | Prints proficiency bonus, crit range, attack bonus, the six scores and modifiers, saves (● proficient), every skill (● proficient, ◆ expertise) and passive Perception, Insight and Stealth, then any non-class sources. Returns the proficiency bonus |
| `/dndclass score <player> <ability> <1-30>` | Admin override: sets one score outright (for testing race-like bonuses). Saved with the player and kept through death. Returns the new score |
| `/dndclass score <player> <ability> clear` | Removes the override |
| `/dndclass forceroll <entity> <n...>` | Rigs the next d20 naturals of a player or a mob (a mob's saves), in order (for example `20 1 15`). With advantage both dice use the queue |
| `/dndclass forceroll <entity> clear` | Drops any rigged rolls left |

Abilities are `STR`, `DEX`, `CON`, `INT`, `WIS`, `CHA` (any case).

## Configuration
The class table lives in `data/dndclasses/class_info.json` (`abilities`, `saves`, `skills`, `expertise` on each class). The game refuses to start if a class's scores aren't a permutation of the standard array, it has more than two saves, a skill name is unknown or an expertise skill isn't also in its skills.

## Known limitations
- Ability points can only go into scores; there are no feats.
- The tab's `A`/`D` marks and passive scores cover plain saves and passive checks only. Advantage that depends on the situation ("vs poison") is listed under Features instead.
- Most skills aren't used by anything yet. Thieves' Tools (lockpicking), Persuasion, passive Stealth and Perception ([Stealth and Perception](stealth-and-perception.md)) are.
- Rigged rolls are kept in memory until used, cleared or the server restarts.

## For developers
Package `classes/Abilities/`:
- `Ability`, `Skill` (all 18 5e skills plus Thieves' Tools, Tinker's Tools and Alchemist's Supplies, each with its ability), `Proficiency` (NONE, HALF, PROFICIENT, EXPERTISE), `Advantage`, `RollKind` (CHECK, SAVE, ATTACK, DEATH, STUDY, FLAT).
- `ClassAbilities`: one class's array, saves, skills and expertise, parsed and validated from `class_info.json` by `ClassInfo`. `ClassAbilities.validateAll()` runs at startup from `AbilityScores.bootstrap()`.
- `AbilityScores`: the facade. `sheet(player)`, `modifier`, `checkBonus`, `saveBonus`, `passive`, `proficiencyBonus(level)`, `invalidate(player)` and `register(id, contributor[, dynamic])`.
- `AbilityContributor` / `Contribution`: how other features add to a sheet. Built in: `dndclasses:class` (class array, proficiencies, Fighter crit range), `dndclasses:asi` (ability score improvements) and `dndclasses:admin` (`/dndclass score`, persistent data key `dndAbilityOverrides`).
- `AbilityIncreases`: the `asi` contributor, C2S `dndclasses:ability_increase` (the ability's short key) handled by `Progression.increaseAbility(player, ability, force)`, and `naturalScore(sheet, ability)` (base + adds, read from the breakdown lines so the client can use it). `ClassProgress` keeps `increases` (NBT `asi`, a compound of short key to count), `asiPoints()`, `ASI_LEVELS` = {4, 8} and `ASI_POINTS_PER_LEVEL` = 2.
- Character tab: `Client/Hud/CharacterSheetPanel`, drawn by `SkillTreeScreen`. Devscript `skill asi <ability>` sends the + click.

  ```java
  AbilityScores.register(new Identifier("dndclasses", "race"), (player, c) -> {
      c.add(Ability.DEX, 2, "Elf");                                        // capped at 20
      c.proficiency(Skill.PERCEPTION, Proficiency.PROFICIENT, "Elf");
      c.advantage(RollFilter.save(Ability.CON).and(RollFilter.tag("poison")), "vs poison", "Dwarf");
  });
  AbilityScores.invalidate(player); // when the input changes
  ```

  Also `setAtLeast`, `override`, `saveBonus(ability|null, n)`, `skillBonus`, `checkBonus(ability|null, n)`, `saveProficiency`, `checkProficiencyFloor` (Jack of All Trades), `disadvantage`, `rerollNaturalOnes` (Halfling Lucky) and `critRange`. Pass `dynamic = true` for contributors that read changing state (effects, nearby allies); those sheets are rebuilt at most once a second.
- `CharacterSheet`: the built sheet (`score`, `modifier`, `save`, `check`, `abilityCheck`, `passive`, `attackBonus`, `advantage(RollQuery)`, `critRange`, and breakdown `lines`). Sent to the client as S2C `dndclasses:sheet_sync` from `Progression.sync` (join, respawn, level-up, class change, ability increase) and every `invalidate`; the client copy is `CharacterSheet.client`. Advantage filters can't be sent, so the packet carries what they come to for each plain save and each passive check, and the client rebuilds stand-in filters for just those.
- Rolls: see [D20 skill checks](d20-skill-checks.md#for-developers) for `SkillCheck` and the `D20.Roll` packet.
- Devscript: `devscripts/ability-scores.txt` (sheet, lockpicks at level 1 and 5 and with DEX 20, Bard persuasion, a Fighter's 19 crit, all with `forceroll`).
