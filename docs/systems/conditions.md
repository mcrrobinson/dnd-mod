# Conditions
Monsters, traps and items can leave a creature **Prone, Frightened, Restrained, Blinded, Stunned, Petrified, Poisoned, Charmed, Incapacitated or Paralyzed**. A condition has a source, a length and sometimes a repeat save to shake it off. Your conditions show as small icons right above the [save lane](saving-throws.md), and everyone sees the visible ones (stone, stars, hearts). Conditions aren't potion effects: they don't show in the inventory, and milk ends only Poisoned.

![Stunned icon above the save lane](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/feat-conditions/conditions-hud.png)

## How it works
"Advantage" against a target means a player's melee [attack roll](d20-skill-checks.md) rolls 2d20 and keeps the higher (more crits, fewer fumbles), and a mob's melee hit does x1.25. "Disadvantage" on an attacker means the lower die, or x0.75 for a mob (x0.8 when Poisoned is the only reason). Advantage and disadvantage cancel.

| Condition | Movement | Actions | Saves and checks | Attacks against it | Its own attacks | Visual |
|-|-|-|-|-|-|-|
| **Prone** | Players crawl at -50% speed, no sprint or jump; mobs -50% speed | Normal | Normal | Melee within 3 blocks: advantage. Projectiles x0.75 | Disadvantage | Crawl pose; dust under mobs |
| **Frightened** | Players can't move toward the source while it's within 32 blocks and in sight. Path mobs flee the source | Normal | Ability checks at disadvantage while the source is in sight | Normal | x0.5 melee damage, and attack rolls at disadvantage while the source is in sight | Icon only |
| **Restrained** | Speed 0, no jump | Can attack and use items | DEX saves at disadvantage | Advantage | Disadvantage | None (the grab shows it) |
| **Blinded** | Players get a hidden Blindness; mobs can't target or keep a target past 4 blocks | Normal | Perception and Search at disadvantage; Danger Sense off | Advantage | Disadvantage | Blindness fog |
| **Stunned** | Speed 0, no jump; mobs' AI stops | Incapacitated | STR and DEX saves **auto-fail** | Advantage | Can't attack | Stars over the head |
| **Petrified** | Speed 0, no jump, knockback resistance +1 | Incapacitated; can't heal | STR and DEX auto-fail | Advantage; **all damage x0.5**; can't be poisoned | Can't attack | Grey stone tint; a grey screen edge for the player |
| **Poisoned** | Normal | Normal | Ability checks at disadvantage | Normal | Disadvantage (mobs x0.8) | Icon only |
| **Charmed** | Charmed mobs follow a mob charmer within 12 blocks and fight its target | Can't harm the charmer ("You can't bring yourself to hurt..."); mobs can't target it or its other thralls | Normal | Normal | Normal | Hearts |
| **Incapacitated** | Normal | Can't attack, break or use blocks, use items or entities, fire specials (power-up, Warlock fireballs, breath weapon) or rest | Normal | Normal | Can't attack | Icon only |
| **Paralyzed** | As Stunned | Incapacitated | STR and DEX auto-fail | Advantage; **melee within 2.5 blocks is a critical hit** (mobs x2) | Can't attack | Stars and the blue Freeze tint |

Stunned, Petrified and Paralyzed also count as Incapacitated. More rules:
- **Refresh, not stack.** The same condition from the same source (and key) refreshes when the new one lasts longer; different sources coexist, and the effect applies once.
- **Repeat saves** roll every few seconds from when the condition lands ("Stunned (INT) 14+1=15 vs 14 ✔ ends" on the save lane). A success ends it; a failure keeps it, or **escalates** it (Restrained turning to Petrified). Downed players' repeat saves wait.
- **Standing up.** A Prone player presses jump (at least half a second after falling) to stand, unless held. Mobs stand when it ends.
- **Statues.** Mobs ignore a Petrified creature and drop it as a target within half a second. A mob Petrified for good is a statue: persistent and silent, untouched by any hit except a player's pickaxe, which shatters it (normal loot and kill credit).
- **Charm breaks** when the charmer, or one of its thralls, hurts the charmed creature.
- **Bosses** are immune to Petrified, Charmed and Frightened. Stunned, Paralyzed, Incapacitated, Restrained and Prone last at most 1 s on a boss, and then that condition can't land on it again for 10 s ("shrugs it off").
- **Immunities:** creative and spectator players are immune to everything, undead and anything Petrified to Poisoned, and constructs (iron and snow golems, the Clockwork Sentinel) to Poisoned, Charmed, Frightened, Paralyzed and Petrified. The Gelatinous Cube and the Warden can't be Petrified.
- **Death** ends everything; going **Downed** ends everything but Petrified; respawning starts clean. Conditions last through relogs and chunk unloads. A condition tied to its source ends 5 s after the source dies, leaves or is more than 64 blocks away.

Default lengths when nothing says otherwise: Prone 3 s, Frightened 8 s, Restrained until released, Blinded 5 s, Stunned 3 s, Petrified until ended, Poisoned 10 s, Charmed 10 s, Incapacitated 3 s, Paralyzed 3 s.

## Where to find it
| Source | Condition |
|-|-|
| [Mimic](../mobs/mimic.md) grab, [Owlbear](../mobs/owlbear.md) bear hug | Restrained while held (struggle with jump) |
| [Gelatinous Cube](../mobs/gelatinous-cube.md) engulf | Restrained while inside; jump to struggle out (DC 12) |
| Owlbear charge, failed STR 13 | Thrown back and Prone 3 s |
| [Lich](../bosses/lich.md) Dread Presence, failed WIS 15 (16 in the last phase) | Frightened of the Lich 8 s, WIS save every 3 s |
| [Beholder](../bosses/beholder.md) fear ray, failed WIS 15 (16 enraged) | Frightened of the Beholder 8 s, WIS save every 3 s; it drives back only those frightened of it |
| [Dread Ward](obstacles.md) | Frightened ("Dread Ward"), 8 s |
| [Warlord's Warhorn](../items/magic-items.md), Paladin Conquering Presence | Goblins and mobs Frightened of the player: they flee |
| [Eye of the Tyrant](../items/magic-items.md) paralysis ray | Paralyzed 3 s |
| Cave spider and witch poison ([data-driven saves](data-driven-saves.md)) | Poisoned for the poison's final length |
| The old `dndclasses:frightened` effect (`/effect`, old saves) | Turned into a sourceless Frightened of the same length |

## Commands
`/dndcondition` (op level 2), also as `/dm condition ...` for Dungeon Masters:

| Command | Effect |
|-|-|
| `/dndcondition apply <targets> <condition> [<ticks>]` | Sourceless; the default length when ticks are left out, `-1` = until removed |
| `/dndcondition apply <targets> <condition> <ticks> from <source>` | With a source entity |
| `/dndcondition apply <targets> <condition> <ticks> save <ability> <dc> <every> [escalate <condition> <ticks>]` | With a repeat save every `<every>` ticks (and what a failure turns it into) |
| `/dndcondition remove <targets> <condition>\|all` | Ends them |
| `/dndcondition list <target>` | Each instance: key, ticks left, source, save |
| `/dndcondition immune <target> <condition>` | Prints the immunity reason; fails when not immune |

The result is the number of targets it landed on, so `execute store success` works.

## Known limitations
- Petrified creatures can't be healed at all (not only natural regeneration), so a Petrified player waits it out or has it removed.
- Class features that interact with conditions (Aura of Courage, Mindless Rage, Stillness of Mind, Cure Wounds, Countercharm, Fey Ancestry) are a follow-up card ("Conditions: class interactions").
- Holds on non-player creatures restrain them but can't be struggled out of (only players struggle).

## For developers
Package `mattonfire.dnd.classes.Conditions`:
- `Conditions`: the API. `Conditions.of(target, Condition.STUNNED).source(mob).ticks(60).repeatSave(entry, 40).apply()` (returns the instance, or null when immune or vetoed); `has`, `mask`, `canAct`, `canMove` on both sides; `all`, `get`, `sourceOf`, `remove`, `removeKey`, `clear`, `isImmune`, `preset` (structure statues) on the server; `registerImmunity` and `registerApplyHook` (`VETO`, `INDEFINITE`). Builder options: `source`, `sourceName`, `key`, `ticks`, `repeatSave`, `saveLabel`, `saveTags`, `escalateOnFailure`, `endsWithSource`, `transientInstance`, `silent`.
- Initial saves are the caller's job: roll `SavingThrow.of(victim, entry)` and apply on a failure. Conditions rolls only the repeat saves (tagged `condition` and the condition id).
- `Condition` (append-only: the ordinal is the sync bit), `ConditionInstance` and `RepeatSave` records, `ConditionEvents.APPLIED` / `ENDED` (`EndReason`).
- `ConditionTicker` (expiry, repeat saves, source gone, `SOURCE_GRACE_TICKS` 100), `ConditionEffects` (the table above, the statue rule, charm, the sheet contributor `dndclasses:conditions`), `ConditionImmunities` (`BOSS_CAP_TICKS` 20, `BOSS_SHRUG_TICKS` 200, tags `#dndclasses:condition_immune/<id>`), `ConditionSync` (S2C `dndclasses:conditions`, C2S `dndclasses:stand_up`), `ConditionCommand`, `ai/ConditionFleeGoal` (added to every path mob on load).
- Mixins: `ConditionsTrackerMixin` (tracked mask `DND$CONDITIONS`, jump, sprint, heal, poison), `ConditionMobMixin` (`tickNewAi` and `tryAttack` while incapacitated), `ConditionMilkMixin`, client `KeyboardInputMixin` (Frightened clamp). Existing hooks: `PlayerEntityMixin.updatePose` (Prone crawl), `LivingEntityRendererMixin` (stone and paralysis tints), `DownedTargetMixin` (mobs ignore Petrified, Blinded and Charmed rules).
- New hooks elsewhere: `SavingThrow.registerPreRoll` (auto-fail and advantage before a save), `AttackRolls.registerMode` (attack roll advantage), `Grapple.hold(..., conditionKey)` (Restrained key), `SaveRules` field `"condition"`.
- Saved under the persistent data key `dndConditions`; times are world time (`/time set` doesn't end them).
- HUD: `Client/Hud/ConditionHud`, icons in `textures/gui/conditions.png` from `tools/condition_icons.py`; `Client/ConditionClient`.
- Logs: `[Conditions] Dev +stunned (Mind Flayer, 60t, save INT 14/40t)`, `-stunned (saved)`, `restrained -> petrified`, `immune to petrified (boss)`, `blocked: attack (Stunned, ...)`.
- Devscripts: `devscripts/conditions.txt` (all of the above), `conditions-relog-set.txt` / `conditions-relog-check.txt` (persistence); the DevScript `conditions` step logs the client's view.
